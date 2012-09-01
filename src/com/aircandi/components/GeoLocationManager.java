package com.aircandi.components;

import java.util.Calendar;
import java.util.List;
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
import android.os.Bundle;
import android.os.SystemClock;

import com.aircandi.Aircandi;
import com.aircandi.components.Events.EventHandler;
import com.aircandi.core.CandiConstants;
import com.aircandi.service.ProxibaseService.RequestListener;
import com.aircandi.service.objects.Observation;

@SuppressWarnings("unused")
public class GeoLocationManager {

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
	 */

	private static GeoLocationManager	singletonObject;

	private Context						mContext;
	private Location					mCurrentLocation;
	private LocationManager				mLocationManager;
	private LocationListener			mLocationListener;
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
		mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
		mLocationListener = new LocationListener() {

			@Override
			public void onLocationChanged(Location location) {
				if (isBetterLocation(location, mCurrentLocation)) {
					mCurrentLocation = location;
					location.setTime(System.currentTimeMillis());
					Events.EventBus.onLocationChanged(location);
				}
			}

			@Override
			public void onProviderDisabled(String provider) {}

			@Override
			public void onProviderEnabled(String provider) {}

			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {}
		};
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

				if (mCurrentLocation.hasAccuracy() && mCurrentLocation.getAccuracy() <= 30) {
					Logger.d(GeoLocationManager.this, "Location scan stopped: accurate fix obtained");
					stopLocationPolling();
					return;
				}
			}
		};

		synchronized (Events.EventBus.locationChanged) {
			// Events.EventBus.locationChanged.add(mEventLocationChanged);
		}

		mCriteria = new Criteria();
		// Coarse accuracy is specified here to get the fastest possible result.
		// The calling Activity will likely (or have already) request ongoing
		// updates using the Fine location provider.
		mCriteria.setAccuracy(Criteria.ACCURACY_COARSE);
	}

	// --------------------------------------------------------------------------------------------
	// Location polling routines
	// --------------------------------------------------------------------------------------------

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
	// Location snapshot routines
	// --------------------------------------------------------------------------------------------

	public Observation getObservation() {

		if (mCurrentLocation == null || !mCurrentLocation.hasAccuracy()) {
			return null;
		}

		Observation observation = new Observation();

		observation.latitude = mCurrentLocation.getLatitude();
		observation.longitude = mCurrentLocation.getLongitude();

		if (mCurrentLocation.hasAltitude()) {
			observation.altitude = mCurrentLocation.getAltitude();
		}
		if (mCurrentLocation.hasAccuracy()) {
			/* In meters. */
			observation.accuracy = mCurrentLocation.getAccuracy();
		}
		if (mCurrentLocation.hasBearing()) {
			/* Direction of travel in degrees East of true North. */
			observation.bearing = mCurrentLocation.getBearing();
		}
		if (mCurrentLocation.hasSpeed()) {
			/* Speed of the device over ground in meters/second. */
			observation.speed = mCurrentLocation.getSpeed();
		}

		return observation;
	}

	public static int timeSinceLocationInMillis(Location location) {
		if (location == null) {
			return Integer.MAX_VALUE;
		}
		long locationTime = location.getTime();
		long currentTime = System.currentTimeMillis();
		long timeDelta = currentTime - locationTime;
		return (int) timeDelta;
	}

	public static boolean isBetterLocation(Location locationToEvaluate, Location currentBestLocation) {

		if (currentBestLocation == null) {
			/* A new location is always better than no location */
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
			/*
			 * If the new location is more than two minutes older, it must be
			 * worse
			 */
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
		 * Determine location quality using a combination of timeliness and
		 * accuracy
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

	public static long locationFixAge(Location location) {
		Calendar cal = Calendar.getInstance();
		long timeDelta = cal.getTimeInMillis() - location.getTime();
		return timeDelta;
	}

	private static boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

	public void setCurrentLocation(Location currentLocation) {
		this.mCurrentLocation = currentLocation;
	}

	public Location getCurrentLocation() {
		return mCurrentLocation;
	}

	public void getSingleLocationUpdate(final RequestListener listener, Criteria criteria) {
		String provider = mLocationManager.getBestProvider(criteria, true);
		if (provider != null) {
			mLocationManager.requestLocationUpdates(provider, 0, 0, new LocationListener() {

				@Override
				public void onLocationChanged(Location location) {
					Logger.d(this, "Single location update received: "
							+ location.getLatitude() + ","
							+ location.getLongitude());
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

	public void getLastBestLocation(int minAccuracy, long maxTimeAgo, Criteria criteria, final RequestListener listener) {
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
		List<String> matchingProviders = mLocationManager.getAllProviders();
		for (String provider : matchingProviders) {
			Location location = mLocationManager.getLastKnownLocation(provider);
			if (location != null) {
				float accuracy = location.getAccuracy();
				long time = location.getTime();
				if ((time > minTime && accuracy < bestAccuracy)) {
					bestLocation = location;
					bestAccuracy = accuracy;
					bestTime = time;
				}
				else if (time < minTime && bestAccuracy == Float.MAX_VALUE && time > bestTime) {
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
			if (mLocationListener != null && bestLocation != null) {
				mLocationListener.onLocationChanged(bestLocation);
			}
			if (listener != null) {
				listener.onComplete(bestLocation);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
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
}
