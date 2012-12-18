package com.aircandi.components.location;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.SystemClock;

import com.aircandi.Aircandi;
import com.aircandi.CandiConstants;
import com.aircandi.Preferences;
import com.aircandi.components.Events;
import com.aircandi.components.Events.EventHandler;
import com.aircandi.components.ListPreferenceMultiSelect;
import com.aircandi.components.Logger;
import com.aircandi.components.location.LocationManager.LocationBetterReason;
import com.aircandi.service.objects.Observation;
import com.aircandi.utilities.DateUtils;

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
	 * ALERT: Some phones use the DummyLocationProvider is the phone is in airport
	 * mode. Also, there will be no location updates. A cached location might
	 * still be around from before airport mode was turned on.
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
	public static Long					MAXIMUM_AGE					= (long) CandiConstants.TIME_TWO_MINUTES;
	public static Double				RADIUS_EARTH_MILES			= 3958.75;
	public static Double				RADIUS_EARTH_KILOMETERS		= 6371.0;
	public static double				conversionRatioMetersToFeet	= 3.28083989501;

	private Context						mApplicationContext;
	private Location					mLocation;
	private LocationManager				mLocationManager;
	protected PackageManager			mPackageManager;
	@SuppressWarnings("unused")
	private Runnable					mLocationScanRunnable;
	protected PendingIntent				mPendingIntentSingleUpdate;
	private EventHandler				mEventLocationChanged;

	protected Criteria					mCriteria;

	public static synchronized GeoLocationManager getInstance() {
		if (singletonObject == null) {
			singletonObject = new GeoLocationManager();
		}
		return singletonObject;
	}

	private GeoLocationManager() {}

	public void initialize(Context applicationContext) {
		Logger.d(this, "Initializing the GeoLocationManager");

		mApplicationContext = applicationContext;
		mLocationManager = (LocationManager) mApplicationContext.getSystemService(Context.LOCATION_SERVICE);
		mPackageManager = mApplicationContext.getPackageManager();
	}

	/**
	 * Works to make sure a location is available that meets the accuracy and age
	 * parameters. We always launch a location request that may or may not be used
	 * later depending on whether it ends up to be better than what we have.
	 * 
	 * It's possible to exit this routine without an available location and there
	 * are no quarantees about how long it will take to get a viable location fix.
	 */
	public void ensureLocation() {
		Logger.d(this, "Ensuring location");
		//		/*
		//		 * We always clear the current location to make sure we aren't keeping around
		//		 * something stale because nothing better is available yet.
		//		 */
		//		setLocation(null);
		//		/*
		//		 * Start getting location updates. We kick off the gps sensor if available
		//		 * but will stop polling when we get a good fix network or gps
		//		 */
		//		if (isLocationAccessEnabled()) {
		//			//			Criteria criteria = new Criteria();
		//			//			criteria.setAccuracy(Criteria.ACCURACY_COARSE);
		//			//			getSingleLocationUpdate(criteria);
		//			LocationSensorOptions options = new LocationSensorOptions();
		//			options.setEnabledOnly(true);
		//			options.setAccuracy(Criteria.ACCURACY_FINE);
		//			enableLocationSensor(options);
		//		}
		//		/*
		//		 * Iterate through all the providers on the system, keeping note of the
		//		 * most accurate result within the acceptable time limit. If no result
		//		 * is found within maxTime, return the newest Location.
		//		 */
		//		Location bestLocation = null;
		//		for (String provider : mLocationManager.getAllProviders()) {
		//			Location location = mLocationManager.getLastKnownLocation(provider);
		//			if (location != null) {
		//				long fixAge = System.currentTimeMillis() - location.getTime();
		//				float fixAccuracy = location.getAccuracy();
		//				if ((fixAge <= MAXIMUM_AGE && fixAccuracy <= MINIMUM_ACCURACY)) {
		//					if (bestLocation == null || fixAccuracy < bestLocation.getAccuracy()) {
		//						bestLocation = location;
		//					}
		//				}
		//			}
		//		}
		//		/*
		//		 * If the best result is beyond the allowed time limit, or the accuracy
		//		 * of the best result is wider than the acceptable maximum distance,
		//		 * request a single update.
		//		 */
		//		if (bestLocation != null) {
		//			Logger.d(this, "Using last known location: provider="
		//					+ bestLocation.getProvider()
		//					+ " accuracy="
		//					+ String.valueOf(bestLocation.getAccuracy())
		//					+ " age="
		//					+ DateUtils.intervalSince(new Date(bestLocation.getTime()), DateUtils.nowDate()));
		//			setLocation(bestLocation);
		//		}
	}

	// --------------------------------------------------------------------------------------------
	// Location listener events
	// --------------------------------------------------------------------------------------------

	public Boolean enableLocationSensor(final LocationSensorOptions options) {

		final String locationProvider = mLocationManager.getBestProvider(options, options.isEnabledOnly());
		/*
		 * If best provider isn't the network provider then check if it's enabled and get it going.
		 */
		if (!locationProvider.equals(LocationManager.NETWORK_PROVIDER) && mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
			mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER
					, CandiConstants.TIME_ONE_SECOND
					, 0
					, this);
		}

		mLocationManager.requestLocationUpdates(locationProvider
				, options.getMinimumTriggerTime()
				, options.getMinimumTriggerDistance()
				, this);

		return true;
	}

	public void disableLocationSensor() {
		mLocationManager.removeUpdates(this);
	}

	@Override
	public void onLocationChanged(final Location location) {
		/*
		 * Sometimes, the location.getTime returned from the OS has an age of perhaps 15-20 seconds, according to
		 * the returned timestamp, although we can tell for certain that it's very old. For example, if the lon/lat fix
		 * is from a position that the handset was on 30 minutes ago!
		 * 
		 * This seems to be caused by the code that collects location info and sends it
		 * to Google to support the network location provider.
		 * 
		 * It seems to happen both from Wifi and network, but not gps. Common workaround
		 * is to look at distance moved between fixes to determine it's a bad time setting.
		 */

		if (location != null) {
			Logger.d(this, "Location update received: " + location.getProvider() + " "
					+ "; accuracy: " + location.getAccuracy()
					+ "; lat/lng: " + location.getLatitude() + ","
					+ location.getLongitude());

			if (mLocation == null) {
				setLocation(location);
			}
			else {
				if (location != null && com.aircandi.components.location.LocationManager.isBetterLocation(location, mLocation) != LocationBetterReason.None) {
					setLocation(location);
					Logger.d(this, "Location update is better: " + location.getProvider());
				}
			}

			long fixAge = System.currentTimeMillis() - location.getTime();
			float fixAccuracy = location.getAccuracy();

			if ((fixAge <= MAXIMUM_AGE && fixAccuracy <= MINIMUM_ACCURACY)) {
				//				Logger.d(this, "Stopped listening for updates");
				//				disableLocationSensor();
			}
		}
	}

	@Override
	public void onProviderDisabled(final String provider) {
		Logger.d(this, "Location provider disabled: " + provider);
	}

	@Override
	public void onProviderEnabled(final String provider) {
		Logger.d(this, "Location provider enabled: " + provider);
	}

	@Override
	public void onStatusChanged(final String provider, final int status, final Bundle extras) {
		switch (status) {
			case LocationProvider.AVAILABLE:
				Logger.d(this, "Location provider available: " + provider);
				break;
			case LocationProvider.OUT_OF_SERVICE:
				Logger.d(this, "Location provider out of service: " + provider);
				break;
			case LocationProvider.TEMPORARILY_UNAVAILABLE:
				Logger.d(this, "Location provider temporarily unavailable: " + provider);
				break;
		}
	}

	// --------------------------------------------------------------------------------------------
	// Location polling routines
	// --------------------------------------------------------------------------------------------

	public void configurePolling() {
		mLocationScanRunnable = new Runnable() {
			@Override
			public void run() {
				Logger.d(GeoLocationManager.this, "Location scan stopped: time limit");
				stopLocationPolling();
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
		mApplicationContext.startService(locationIntent);

		/* Setup a polling schedule */
		if (CandiConstants.LOCATION_POLLING_INTERVAL > 0) {
			AlarmManager alarmManager = (AlarmManager) mApplicationContext.getSystemService(Service.ALARM_SERVICE);
			PendingIntent pendingIntent = PendingIntent.getService(Aircandi.applicationContext, 0, locationIntent, 0);
			alarmManager.cancel(pendingIntent);
			alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
					SystemClock.elapsedRealtime() + CandiConstants.LOCATION_POLLING_INTERVAL,
					CandiConstants.LOCATION_POLLING_INTERVAL, pendingIntent);
		}
	}

	public void stopLocationPolling() {
		AlarmManager alarmManager = (AlarmManager) mApplicationContext.getSystemService(Service.ALARM_SERVICE);
		Intent locationIntent = new Intent(Aircandi.applicationContext, LocationPollingService.class);
		PendingIntent pendingIntent = PendingIntent.getService(Aircandi.applicationContext, 0, locationIntent, 0);
		alarmManager.cancel(pendingIntent);
		Logger.d(this, "Stopped location polling service");
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

	// --------------------------------------------------------------------------------------------
	// Location routines
	// --------------------------------------------------------------------------------------------

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

	static boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

	public void setLocation(Location location) {

		String message = new String("Location updated:");
		if (location == null) {
			message += " none";
		}
		else {
			message += " provider: " + location.getProvider();
			message += " accuracy: " + String.valueOf(location.getAccuracy());
		}
		Logger.d(this, message);

		this.mLocation = location;
		Events.EventBus.onLocationChanged(mLocation);
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

			mLocationManager.requestLocationUpdates(provider, CandiConstants.TIME_ONE_SECOND, 0, new LocationListener() {

				@Override
				public void onLocationChanged(Location location) {
					Logger.d(this, "Single location update received: " + provider + " "
							+ location.getLatitude() + ","
							+ location.getLongitude());

					if (location != null && com.aircandi.components.location.LocationManager.isBetterLocation(location, mLocation) != LocationBetterReason.None) {
						Logger.d(this, "Single location update is better");
						setLocation(location);
					}

					mLocationManager.removeUpdates(this);
				}

				@Override
				public void onProviderDisabled(String provider) {}

				@Override
				public void onProviderEnabled(String provider) {}

				@Override
				public void onStatusChanged(String provider, int status, Bundle extras) {}

			}, mApplicationContext.getMainLooper());
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
		// disableLocationSensor();
	}

	public void onResume() {
		// GeoLocationManager.getInstance().startLocationPolling();
		//		if (isLocationAccessEnabled()) {
		//			LocationSensorOptions options = new LocationSensorOptions();
		//			options.setEnabledOnly(true);
		//			options.setAccuracy(Criteria.ACCURACY_FINE);
		//			enableLocationSensor(options);
		//		}
	}

	// --------------------------------------------------------------------------------------------
	// Inner classes
	// --------------------------------------------------------------------------------------------

	public static class LocationSensorOptions extends Criteria {

		private static final long	MINIMUMTRIGGERTIME_DEFAULT		= CandiConstants.TIME_ONE_SECOND;
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
