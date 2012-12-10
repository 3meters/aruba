package com.aircandi.components;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.SystemClock;

import com.aircandi.Aircandi;
import com.aircandi.Preferences;
import com.aircandi.components.Events.EventHandler;
import com.aircandi.core.CandiConstants;
import com.aircandi.service.ProxibaseService.RequestListener;
import com.aircandi.service.objects.Observation;
import com.aircandi.utilities.DateUtils;

@SuppressWarnings("unused")
public class GeoLocationManager implements LocationListener {

	/*
	 * Current location strategy
	 * 
	 * Location is used in the following ways:
	 * 
	 * - Beacons have location info that is continuously improved/updated as needed.
	 * - Linked place entities use location info from the link source.
	 * - Custom place entities use location info from the device.
	 * - Radar shows synthetics based on the current location.
	 * - Radar shows custom/linked place entities based on location if no beacons available
	 * - Tuning without beacons stores location with the tuning action.
	 * 
	 * Location info is created/refreshed at the following points
	 * 
	 * - Initialization of GeoLocationManager (done in Radar onCreate)
	 * - Any radar scan.
	 * - Creation of EntityForm
	 * - Creation of LinkPicker
	 * - Creation of CandiTuner
	 * 
	 * When the entity form is created for a new candi we refresh the current
	 * location using a combination of last known and a single coarse update (if
	 * needed). The coarse update is almost guaranteed to use the network
	 * provider if available and enabled. Last known will only consider gps if
	 * the gps provider is currently enabled even if a good gps fix was
	 * performed before it was disabled.
	 * 
	 * If network is disabled and gps is enabled, a single update request will
	 * take longer to complete.
	 * 
	 * Additional work: If more accuracy is needed, we can launch a fine update
	 * after the coarse update that will be working while the user is completing
	 * the candi form.
	 * 
	 * Altitude and accuracy are in meters.
	 * Bearing is direction of travel in degrees East of true North.
	 * Speed is movement of the device over ground in meters/second.
	 */

	private static GeoLocationManager	singletonObject;
	public static Integer				MINIMUM_ACCURACY			= 50;
	public static Long					MAXIMUM_AGE					= (long) CandiConstants.TWO_MINUTES;
	public static Double				RADIUS_EARTH_MILES			= 3958.75;
	public static Double				RADIUS_EARTH_KILOMETERS		= 6371.0;
	private static double				conversionRatioMetersToFeet	= 3.28083989501;
	private ILocationListener			mLocationListener;

	private Context						mContext;
	private Location					mLocation;
	private LocationManager				mLocationManager;
	private Runnable					mLocationScanRunnable;
	protected PendingIntent				mPendingIntentSingleUpdate;
	private EventHandler				mEventLocationChanged;
	private AtomicBoolean				mLocationScanActive			= new AtomicBoolean(false);

	protected Criteria					mCriteria;

	public static synchronized GeoLocationManager getInstance() {
		if (singletonObject == null) {
			singletonObject = new GeoLocationManager();
		}
		return singletonObject;
	}

	private GeoLocationManager() {}

	public void setContext(Context context) {
		mContext = context;
	}

	public void initialize() {
		/*
		 * Stash the location manager and get the best available cached location. If
		 * a cached location doesn't meet our minimum age and accuracy requirements, a
		 * single location update is requested based on criteria.
		 */
		Logger.d(this, "Initializing the GeoLocationManager");
		mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_COARSE);
		ensureLocation(MINIMUM_ACCURACY, MAXIMUM_AGE, criteria);
	}

	/**
	 * Works to make sure a location is available that meets the accuracy and age
	 * parameters. We always launch a location request that may or may not be used
	 * later depending on whether it ends up to be better than what we have.
	 * 
	 * It's possible to exit this routine without an available location and there
	 * are no quarantees about how long it will take to get a viable location fix.
	 * 
	 * @param minAccuracy
	 * @param maxAge
	 * @param criteria
	 * @param listener
	 */

	public void ensureLocation(int minAccuracy, long maxAge, Criteria criteria) {
		Logger.d(this, "Ensuring location");

		/*
		 * We always start a single shot update while we look
		 * over the last known locations.
		 */
		if (!mLocationScanActive.get()) {
			getSingleLocationUpdate(criteria);
		}
		/*
		 * Iterate through all the providers on the system, keeping note of the
		 * most accurate result within the acceptable time limit. If no result
		 * is found within maxTime, return the newest Location.
		 */
		Location bestLocation = null;
		for (String provider : mLocationManager.getAllProviders()) {
			Location location = mLocationManager.getLastKnownLocation(provider);
			if (location != null) {

				long fixAge = System.currentTimeMillis() - location.getTime();
				float fixAccuracy = location.getAccuracy();

				if ((fixAge <= maxAge && fixAccuracy <= minAccuracy)) {
					if (bestLocation == null || fixAccuracy < bestLocation.getAccuracy()) {
						bestLocation = location;
					}
				}
			}
		}

		/*
		 * If the best result is beyond the allowed time limit, or the accuracy
		 * of the best result is wider than the acceptable maximum distance,
		 * request a single update.
		 */
		if (bestLocation != null) {
			if (isBetterLocation(bestLocation, mLocation)) {
				Logger.d(this, "Using last known location: provider="
						+ bestLocation.getProvider()
						+ " accuracy="
						+ String.valueOf(bestLocation.getAccuracy())
						+ " age="
						+ DateUtils.intervalSince(new Date(bestLocation.getTime()), DateUtils.nowDate()));
				mLocation = bestLocation;
				if (mLocationListener != null) {
					mLocationListener.onLocationChanged(mLocation);
				}
			}
			else {
				Logger.d(this, "Current location is best: provider="
						+ mLocation.getProvider()
						+ " accuracy="
						+ String.valueOf(mLocation.getAccuracy())
						+ " age="
						+ DateUtils.intervalSince(new Date(mLocation.getTime()), DateUtils.nowDate()));
			}
		}
	}

	private void initializeLocation() {
		/*
		 * Look for the best available location fix in the system and use it. This
		 * does not require powering on any hardware to obtain a fix.
		 */
		Location lastKnownLocationNetwork = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		if (lastKnownLocationNetwork != null) {
			if (isBetterLocation(lastKnownLocationNetwork, mLocation)) {
				mLocation = lastKnownLocationNetwork;
			}
		}

		Location lastKnownLocationGPS = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (lastKnownLocationGPS != null) {
			if (isBetterLocation(lastKnownLocationGPS, mLocation)) {
				mLocation = lastKnownLocationGPS;
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Location polling routines
	// --------------------------------------------------------------------------------------------

	private void configurePolling() {
		mLocationScanRunnable = new Runnable() {

			@Override
			public void run() {
				if (mLocationScanActive.get()) {
					Logger.d(GeoLocationManager.this, "Location scan stopped: time limit");
					stopLocationPolling();
				}
			}
		};

		mEventLocationChanged = new EventHandler() {

			@Override
			public void onEvent(Object data) {

				if (mLocation.hasAccuracy() && mLocation.getAccuracy() <= 30) {
					Logger.d(GeoLocationManager.this, "Location scan stopped: accurate fix obtained");
					stopLocationPolling();
					return;
				}
			}
		};

		synchronized (Events.EventBus.locationChanged) {
			Events.EventBus.locationChanged.add(mEventLocationChanged);
		}
	}

	public void startLocationPolling() {

		/* Start first scan right away */
		Logger.d(this, "Starting location polling scan service");
		Intent locationIntent = new Intent(Aircandi.applicationContext, LocationPollingService.class);
		mContext.startService(locationIntent);

		/* Setup a polling schedule */
		if (CandiConstants.LOCATION_POLLING_INTERVAL > 0) {
			AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Service.ALARM_SERVICE);
			PendingIntent pendingIntent = PendingIntent.getService(Aircandi.applicationContext, 0, locationIntent, 0);
			alarmManager.cancel(pendingIntent);
			alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
					SystemClock.elapsedRealtime() + CandiConstants.LOCATION_POLLING_INTERVAL,
					CandiConstants.LOCATION_POLLING_INTERVAL, pendingIntent);
		}
	}

	public void stopLocationPolling() {
		AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Service.ALARM_SERVICE);
		Intent locationIntent = new Intent(Aircandi.applicationContext, LocationPollingService.class);
		PendingIntent pendingIntent = PendingIntent.getService(Aircandi.applicationContext, 0, locationIntent, 0);
		alarmManager.cancel(pendingIntent);
		Logger.d(this, "Stopped location polling service");
	}

	// --------------------------------------------------------------------------------------------
	// Location listener events
	// --------------------------------------------------------------------------------------------

	public Boolean enableLocationSensor(final ILocationListener locationListener, final LocationSensorOptions locationSensorOptions) {
		this.mLocationListener = locationListener;
		final String locationProvider = mLocationManager.getBestProvider(locationSensorOptions, locationSensorOptions.isEnabledOnly());
		if (locationProvider == null) {
			return false;
		}
		else {
			/*
			 * If best provider isn't the network provider then check if
			 * it's enabled and get it going.
			 */
			if (!locationProvider.equals(LocationManager.NETWORK_PROVIDER)
					&& mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
				mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER
						, 1000
						, 0
						, this);
				this.onLocationChanged(mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
			}

			mLocationManager.requestLocationUpdates(locationProvider
					, locationSensorOptions.getMinimumTriggerTime()
					, locationSensorOptions.getMinimumTriggerDistance()
					, this);

			this.onLocationChanged(mLocationManager.getLastKnownLocation(locationProvider));
			return true;
		}
	}

	public void disableLocationSensor() {
		mLocationManager.removeUpdates(this);
	}

	// --------------------------------------------------------------------------------------------
	// Location listener events
	// --------------------------------------------------------------------------------------------

	@Override
	public void onLocationChanged(final Location location) {

		if (location == null) {
			mLocationListener.onLocationLost();
		}
		else {
			if (mLocation == null) {
				mLocation = location;
				mLocationListener.onLocationChanged(mLocation);
			}
			else {
				if (isBetterLocation(location, mLocation)) {
					mLocation = location;
					mLocationListener.onLocationChanged(mLocation);
				}
			}
		}
	}

	@Override
	public void onProviderDisabled(final String pProvider) {
		this.mLocationListener.onLocationProviderDisabled();
	}

	@Override
	public void onProviderEnabled(final String pProvider) {
		this.mLocationListener.onLocationProviderEnabled();
	}

	@Override
	public void onStatusChanged(final String pProvider, final int pStatus, final Bundle pExtras) {
		switch (pStatus) {
			case LocationProvider.AVAILABLE:
				this.mLocationListener.onLocationProviderStatusChanged(LocationProviderStatus.AVAILABLE, pExtras);
				break;
			case LocationProvider.OUT_OF_SERVICE:
				this.mLocationListener.onLocationProviderStatusChanged(LocationProviderStatus.OUT_OF_SERVICE, pExtras);
				break;
			case LocationProvider.TEMPORARILY_UNAVAILABLE:
				this.mLocationListener.onLocationProviderStatusChanged(LocationProviderStatus.TEMPORARILY_UNAVAILABLE, pExtras);
				break;
		}
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	public static int timeSinceLocationInMillis(Location location) {
		if (location == null) {
			return Integer.MAX_VALUE;
		}
		long locationTime = location.getTime();
		long currentTime = System.currentTimeMillis();
		long timeDelta = currentTime - locationTime;
		return (int) timeDelta;
	}

	public static float getRadiusForMeters(float meters) {
		float radius = (float) ((meters / 1000) / RADIUS_EARTH_KILOMETERS);
		return radius;
	}

	// --------------------------------------------------------------------------------------------
	// Location snapshot routines
	// --------------------------------------------------------------------------------------------

	/**
	 * Calculates distance between two point specified by decimal degree lat/lon.
	 * 
	 * @param lat1
	 * @param lng1
	 * @param lat2
	 * @param lng2
	 * @returns distance in meters or yards between the two points
	 */

	public static double distance(double lat1, double lng1, double lat2, double lng2, MeasurementSystem system) {
		double R = RADIUS_EARTH_KILOMETERS;

		/* calculate delta in radians for latitudes and longitudes */
		double dLat = Math.toRadians(lat2 - lat1);
		double dLng = Math.toRadians(lng2 - lng1);

		double a = Math.sin(dLat / 2)
				* Math.sin(dLat / 2)
				+ Math.cos(Math.toRadians(lat1))
				* Math.cos(Math.toRadians(lat2))
				* Math.sin(dLng / 2)
				* Math.sin(dLng / 2);

		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double dist = R * c;
		if (system == MeasurementSystem.Imperial) {
			dist = (dist * conversionRatioMetersToFeet) / 3;
		}

		return (new Float(dist).floatValue() * 1000);
	}

	private static Double toRad(Double value) {
		return value * Math.PI / 180;
	}

	/**
	 * Calculates geodetic distance between two points specified by latitude/longitude using Vincenty inverse formula
	 * for ellipsoids
	 * 
	 * @param lat1
	 *            first point latitude in decimal degrees
	 * @param lng1
	 *            first point longitude in decimal degrees
	 * @param lat2
	 *            second point latitude in decimal degrees
	 * @param lng2
	 *            second point longitude in decimal degrees
	 * @returns distance in meters or yards between points with 5.10<sup>-4</sup> precision
	 * @see <a href="http://www.movable-type.co.uk/scripts/latlong-vincenty.html">Originally posted here</a>
	 */

	public static double distanceVincenty(double lat1, double lng1, double lat2, double lng2, MeasurementSystem system) {

		double a = 6378137, b = 6356752.314245, f = 1 / 298.257223563; // WGS-84 ellipsoid params
		double L = Math.toRadians(lng2 - lng1);
		double U1 = Math.atan((1 - f) * Math.tan(Math.toRadians(lat1)));
		double U2 = Math.atan((1 - f) * Math.tan(Math.toRadians(lat2)));
		double sinU1 = Math.sin(U1), cosU1 = Math.cos(U1);
		double sinU2 = Math.sin(U2), cosU2 = Math.cos(U2);

		double sinLambda, cosLambda, sinSigma, cosSigma, sigma, sinAlpha, cosSqAlpha, cos2SigmaM;
		double lambda = L, lambdaP, iterLimit = 100;
		do {
			sinLambda = Math.sin(lambda);
			cosLambda = Math.cos(lambda);
			sinSigma = Math.sqrt((cosU2 * sinLambda) * (cosU2 * sinLambda)
					+ (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda) * (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda));
			if (sinSigma == 0)
				return 0; // co-incident points
			cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * cosLambda;
			sigma = Math.atan2(sinSigma, cosSigma);
			sinAlpha = cosU1 * cosU2 * sinLambda / sinSigma;
			cosSqAlpha = 1 - sinAlpha * sinAlpha;
			cos2SigmaM = cosSigma - 2 * sinU1 * sinU2 / cosSqAlpha;
			if (Double.isNaN(cos2SigmaM))
				cos2SigmaM = 0; // equatorial line: cosSqAlpha=0 (§6)
			double C = f / 16 * cosSqAlpha * (4 + f * (4 - 3 * cosSqAlpha));
			lambdaP = lambda;
			lambda = L + (1 - C) * f * sinAlpha
					* (sigma + C * sinSigma * (cos2SigmaM + C * cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM)));
		} while (Math.abs(lambda - lambdaP) > 1e-12 && --iterLimit > 0);

		if (iterLimit == 0)
			return Double.NaN; // formula failed to converge

		double uSq = cosSqAlpha * (a * a - b * b) / (b * b);
		double A = 1 + uSq / 16384 * (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)));
		double B = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)));
		double deltaSigma = B
				* sinSigma
				* (cos2SigmaM + B
						/ 4
						* (cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM) - B / 6 * cos2SigmaM
								* (-3 + 4 * sinSigma * sinSigma) * (-3 + 4 * cos2SigmaM * cos2SigmaM)));
		double dist = b * A * (sigma - deltaSigma);
		if (system == MeasurementSystem.Imperial) {
			dist = (dist * conversionRatioMetersToFeet) / 3;
		}
		return dist;
	}

	public boolean isProviderEnabled(String provider) {
		return (mLocationManager.isProviderEnabled(provider));
	}

	public boolean isLocationAccessEnabled() {
		return (isProviderEnabled(LocationManager.NETWORK_PROVIDER) || isProviderEnabled(LocationManager.GPS_PROVIDER));
	}

	public Observation getObservation() {

		if (mLocation == null || !mLocation.hasAccuracy()) {
			return null;
		}

		Observation observation = new Observation();
		if (Aircandi.usingEmulator) {
			observation = new Observation(47.616245, -122.201645); // earls
			observation.time = DateUtils.nowDate().getTime();
			observation.provider = "emulator_lucky";
		}
		else {
			String testingLocation = Aircandi.settings.getString(Preferences.PREF_TESTING_LOCATION, "natural");
			if (ListPreferenceMultiSelect.contains("zoka", testingLocation, null)) {
				observation = new Observation(47.6686489, -122.3320842); // zoka
				observation.time = DateUtils.nowDate().getTime();
				observation.provider = "testing_zoka";
			}
			else if (ListPreferenceMultiSelect.contains("lucky", testingLocation, null)) {
				observation = new Observation(47.616245, -122.201645); // lucky
				observation.time = DateUtils.nowDate().getTime();
				observation.provider = "testing_lucky";
			}
			else {
				observation.latitude = mLocation.getLatitude();
				observation.longitude = mLocation.getLongitude();

				if (mLocation.hasAltitude()) {
					observation.altitude = mLocation.getAltitude();
				}
				if (mLocation.hasAccuracy()) {
					/* In meters. */
					observation.accuracy = mLocation.getAccuracy();
				}
				if (mLocation.hasBearing()) {
					/* Direction of travel in degrees East of true North. */
					observation.bearing = mLocation.getBearing();
				}
				if (mLocation.hasSpeed()) {
					/* Speed of the device over ground in meters/second. */
					observation.speed = mLocation.getSpeed();
				}
				observation.time = mLocation.getTime();
				observation.provider = mLocation.getProvider();
			}
		}

		return observation;
	}

	public static boolean isBetterLocation(Location locationToEvaluate, Location currentBestLocation) {

		/* A new location is always better than no location */
		if (currentBestLocation == null) {
			return true;
		}

		/* Check whether the new location fix is newer or older */
		long timeDelta = locationToEvaluate.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > CandiConstants.TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -CandiConstants.TWO_MINUTES;
		boolean isNewer = timeDelta > 0;

		/*
		 * If it's been more than two minutes since the current location, use
		 * the new location because the user has likely moved
		 */
		if (isSignificantlyNewer) {
			/* If the new location is more than two minutes older, it must be worse */
			return true;
		}
		else if (isSignificantlyOlder) {
			return false;
		}

		/* Check whether the new location fix is more or less accurate */
		int accuracyDelta = (int) (locationToEvaluate.getAccuracy() - currentBestLocation.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		/* Check if the old and new location are from the same provider */
		boolean isFromSameProvider = isSameProvider(locationToEvaluate.getProvider(), currentBestLocation.getProvider());

		/*
		 * Determine location quality using a combination of timeliness and accuracy
		 */
		if (isMoreAccurate) {
			return true;
		}
		else if (isNewer && !isLessAccurate) {
			return true;
		}
		else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
			return true;
		}
		return false;
	}

	private static boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

	public void setLocation(Location location) {
		this.mLocation = location;
	}

	public Location getLocation() {
		return mLocation;
	}

	/**
	 * We listen for a single update and then stop.
	 * 
	 * @param listener
	 * @param criteria
	 */

	public void getSingleLocationUpdate(Criteria criteria) {

		final String provider = mLocationManager.getBestProvider(criteria, true);

		if (provider != null) {
			Logger.d(this, "Starting single location update: " + provider);
			mLocationScanActive.set(true);

			mLocationManager.requestLocationUpdates(provider, CandiConstants.ONE_SECOND, 0, new LocationListener() {

				@Override
				public void onLocationChanged(Location location) {
					mLocationScanActive.set(false);
					Logger.d(this, "Single location update received: " + provider + " "
							+ location.getLatitude() + ","
							+ location.getLongitude());

					if (location != null && isBetterLocation(location, mLocation)) {
						Logger.d(this, "Single location update is better");
						mLocation = location;
						/* Notify interested parties */
						Events.EventBus.onLocationChanged(mLocation);
					}

					if (mLocationListener != null && location != null) {
						mLocationListener.onLocationChanged(location);
					}

					mLocationManager.removeUpdates(this);
				}

				@Override
				public void onProviderDisabled(String provider) {}

				@Override
				public void onProviderEnabled(String provider) {}

				@Override
				public void onStatusChanged(String provider, int status, Bundle extras) {}

			}, mContext.getMainLooper());
		}
		else {
			Logger.d(this, "No location provider available");
		}
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle routines
	// --------------------------------------------------------------------------------------------

	public void onPause() {
		// GeoLocationManager.getInstance().stopLocationPolling();
	}

	public void onResume() {
		// GeoLocationManager.getInstance().startLocationPolling();
	}

	// --------------------------------------------------------------------------------------------
	// Inner classes
	// --------------------------------------------------------------------------------------------

	public ILocationListener getLocationListener() {
		return mLocationListener;
	}

	public static abstract class BaseLocationListener implements ILocationListener {

		public void onLocationProviderStatusChanged(LocationProviderStatus locationProviderStatus, Bundle pBundle) {}

		public void onLocationProviderEnabled() {}

		public void onLocationProviderDisabled() {}

		public void onLocationLost() {}

		public void onLocationChanged(Location location) {}

	}

	public void setLocationListener(ILocationListener locationListener) {
		mLocationListener = locationListener;
	}

	public LocationManager getLocationManager() {
		return mLocationManager;
	}

	public void setLocationManager(LocationManager locationManager) {
		mLocationManager = locationManager;
	}

	public static class LocationSensorOptions extends Criteria {

		private static final long	MINIMUMTRIGGERTIME_DEFAULT		= 1 * CandiConstants.MILLS_PER_SECOND;
		private static final long	MINIMUMTRIGGERDISTANCE_DEFAULT	= 10;
		private boolean				mEnabledOnly					= true;
		private long				mMinimumTriggerTime				= MINIMUMTRIGGERTIME_DEFAULT;
		private long				mMinimumTriggerDistance			= MINIMUMTRIGGERDISTANCE_DEFAULT;

		/**
		 * @see {@link LocationSensorOptions#setAccuracy(int)},
		 *      {@link LocationSensorOptions#setAltitudeRequired(boolean)},
		 *      {@link LocationSensorOptions#setBearingRequired(boolean)},
		 *      {@link LocationSensorOptions#setCostAllowed(boolean)},
		 *      {@link LocationSensorOptions#setEnabledOnly(boolean)},
		 *      {@link LocationSensorOptions#setMinimumTriggerDistance(long)},
		 *      {@link LocationSensorOptions#setMinimumTriggerTime(long)},
		 *      {@link LocationSensorOptions#setPowerRequirement(int)},
		 *      {@link LocationSensorOptions#setSpeedRequired(boolean)}.
		 */
		public LocationSensorOptions() {}

		public LocationSensorOptions(final int pAccuracy
				, final boolean pAltitudeRequired
				, final boolean pBearingRequired
				, final boolean pCostAllowed
				, final int pPowerRequirement
				, final boolean pSpeedRequired
				, final boolean pEnabledOnly
				, final long pMinimumTriggerTime
				, final long pMinimumTriggerDistance) {

			this.mEnabledOnly = pEnabledOnly;
			this.mMinimumTriggerTime = pMinimumTriggerTime;
			this.mMinimumTriggerDistance = pMinimumTriggerDistance;

			this.setAccuracy(pAccuracy);
			this.setAltitudeRequired(pAltitudeRequired);
			this.setBearingRequired(pBearingRequired);
			this.setCostAllowed(pCostAllowed);
			this.setPowerRequirement(pPowerRequirement);
			this.setSpeedRequired(pSpeedRequired);
		}

		public void setEnabledOnly(final boolean pEnabledOnly) {
			this.mEnabledOnly = pEnabledOnly;
		}

		public boolean isEnabledOnly() {
			return this.mEnabledOnly;
		}

		public long getMinimumTriggerTime() {
			return this.mMinimumTriggerTime;
		}

		public void setMinimumTriggerTime(final long pMinimumTriggerTime) {
			this.mMinimumTriggerTime = pMinimumTriggerTime;
		}

		public long getMinimumTriggerDistance() {
			return this.mMinimumTriggerDistance;
		}

		public void setMinimumTriggerDistance(final long pMinimumTriggerDistance) {
			this.mMinimumTriggerDistance = pMinimumTriggerDistance;
		}

	}

	public enum LocationProviderStatus {
		AVAILABLE,
		OUT_OF_SERVICE,
		TEMPORARILY_UNAVAILABLE;
	}

	public static interface ILocationListener {

		public void onLocationProviderEnabled();

		public void onLocationChanged(final Location location);

		public void onLocationLost();

		public void onLocationProviderDisabled();

		public void onLocationProviderStatusChanged(final LocationProviderStatus locationProviderStatus, final Bundle bundle);
	}

	public enum MeasurementSystem {
		Imperial, Metric
	}
}
