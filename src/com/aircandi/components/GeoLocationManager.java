package com.aircandi.components;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.anddev.andengine.sensor.location.ILocationListener;
import org.anddev.andengine.sensor.location.LocationProviderStatus;
import org.anddev.andengine.util.constants.TimeConstants;

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
import com.aircandi.components.Events.EventHandler;
import com.aircandi.core.CandiConstants;
import com.aircandi.service.ProxibaseService.RequestListener;
import com.aircandi.service.objects.Observation;
import com.google.android.maps.GeoPoint;

@SuppressWarnings("unused")
public class GeoLocationManager implements LocationListener {

	/*
	 * Current location strategy
	 * 
	 * We only use location when inserting a beacon or entity.
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
	 * 
	 * 
	 * Notes: The engine class has location support but we are not
	 * using it so our location implementation doesn't have a dependency
	 * on the engine.
	 */

	private static GeoLocationManager	singletonObject;
	public static Integer				MINIMUM_ACCURACY	= 50;
	public static Long					MAXIMUM_AGE			= (long) CandiConstants.TWO_MINUTES;

	private ILocationListener			mLocationListener;

	private Context						mContext;
	private Location					mLocation;
	private LocationManager				mLocationManager;
	private Runnable					mLocationScanRunnable;
	protected PendingIntent				mPendingIntentSingleUpdate;
	private EventHandler				mEventLocationChanged;
	private AtomicBoolean				mLocationScanActive	= new AtomicBoolean(false);

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
		ensureLocation(MINIMUM_ACCURACY, MAXIMUM_AGE, criteria, null);
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

	public void ensureLocation(int minAccuracy, long maxTimeAgo, Criteria criteria, final RequestListener listener) {
		/*
		 * Returns the most accurate and timely previously detected location.
		 * When the last result is beyond the specified maximum distance or
		 * latency a one-off location update is returned.
		 */
		Location bestLocation = null;
		float bestAccuracy = Float.MAX_VALUE;
		long bestTime = Long.MIN_VALUE;
		long minTime = System.currentTimeMillis() - maxTimeAgo;

		/*
		 * Iterate through all the providers on the system, keeping note of the
		 * most accurate result within the acceptable time limit. If no result
		 * is found within maxTime, return the newest Location.
		 */
		for (String provider : mLocationManager.getAllProviders()) {

			Location location = mLocationManager.getLastKnownLocation(provider);
			if (location != null) {
				float accuracy = location.getAccuracy();
				long time = location.getTime();
				if ((time > minTime && accuracy < bestAccuracy)) {
					bestLocation = location;
					bestAccuracy = accuracy;
					bestTime = time;
				}
				else if (time < minTime
						&& bestAccuracy == Float.MAX_VALUE
						&& time > bestTime) {
					bestLocation = location;
					bestTime = time;
				}
			}
		}
		/*
		 * If the best result is beyond the allowed time limit, or the accuracy
		 * of the best result is wider than the acceptable maximum distance,
		 * request a single update.
		 */
		if (bestTime < minTime || bestAccuracy > minAccuracy) {
			getSingleLocationUpdate(listener, criteria);
		}
		else {
			if (bestLocation != null) {
				Logger.d(this, "Using cached location");
				if (isBetterLocation(bestLocation, mLocation)) {
					mLocation = bestLocation;
				}
				if (mLocationListener != null && bestLocation != null) {
					mLocationListener.onLocationChanged(bestLocation);
				}
			}
			if (listener != null) {
				listener.onComplete(bestLocation);
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
			if (!locationProvider.equals(LocationManager.NETWORK_PROVIDER) && mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
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
	// Location snapshot routines
	// --------------------------------------------------------------------------------------------

	public Observation getObservation() {

		if (mLocation == null || !mLocation.hasAccuracy()) {
			return null;
		}

		Observation observation = new Observation();

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

	public void getSingleLocationUpdate(final RequestListener listener, Criteria criteria) {

		String provider = mLocationManager.getBestProvider(criteria, true);

		if (provider != null) {
			Logger.d(this, "Starting single location update");

			mLocationManager.requestLocationUpdates(provider, CandiConstants.ONE_SECOND, 0, new LocationListener() {

				@Override
				public void onLocationChanged(Location location) {
					Logger.d(this, "Single location update received: "
							+ location.getLatitude() + ","
							+ location.getLongitude());

					if (location != null && isBetterLocation(location, mLocation)) {
						mLocation = location;
					}

					if (mLocationListener != null && location != null) {
						mLocationListener.onLocationChanged(location);
					}
					mLocationManager.removeUpdates(this);
					if (listener != null) {
						listener.onComplete(location);
					}
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
			if (listener != null) {
				listener.onComplete(null);
			}
		}
	}

	public void cancel() {
		/* call removeUpdates for any registered and active listeners */
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

		private static final long	MINIMUMTRIGGERTIME_DEFAULT		= 1 * TimeConstants.MILLISECONDSPERSECOND;
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

}
