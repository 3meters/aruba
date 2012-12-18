package com.aircandi.components.location;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

import com.aircandi.Aircandi;
import com.aircandi.CandiConstants;
import com.aircandi.Preferences;
import com.aircandi.components.Events;
import com.aircandi.components.ListPreferenceMultiSelect;
import com.aircandi.components.Logger;
import com.aircandi.components.receivers.LocationChangedReceiver;
import com.aircandi.components.receivers.PassiveLocationChangedReceiver;
import com.aircandi.service.objects.Observation;
import com.aircandi.utilities.DateUtils;

public class LocationManagerOld {

	private static LocationManagerOld				singletonObject;
	public static Double						RADIUS_EARTH_MILES		= 3958.75;
	public static Double						RADIUS_EARTH_KILOMETERS	= 6371.0;

	private Context								mApplicationContext;
	protected PackageManager					mPackageManager;
	protected android.location.LocationManager	mLocationManager;

	private Location							mLocation;
	private Boolean								mLocationModeBurst		= false;
	protected ILastLocationFinder				mLastLocationFinder;
	protected LocationUpdateRequester			mLocationUpdateRequester;
	protected PendingIntent						mLocationListenerPendingIntent;
	protected PendingIntent						mLocationListenerPassivePendingIntent;

	protected LocationListener					mOneShotLastLocationUpdateListener;
	protected LocationListener					mBestInactiveLocationProviderListener;
	protected BroadcastReceiver					mLocProviderDisabledReceiver;

	public static synchronized LocationManagerOld getInstance() {
		if (singletonObject == null) {
			singletonObject = new LocationManagerOld();
		}
		return singletonObject;
	}

	private LocationManagerOld() {}

	public void initialize(Context applicationContext) {

		Logger.d(this, "Initializing the LocationManager");
		mApplicationContext = applicationContext;
		mLocationManager = (android.location.LocationManager) mApplicationContext.getSystemService(Context.LOCATION_SERVICE);
		mPackageManager = mApplicationContext.getPackageManager();

		/*
		 * If the Location Provider we're using to receive location updates is disabled while the
		 * app is running, this Receiver will be notified, allowing us to re-register our Location
		 * Receivers using the best available Location Provider is still available.
		 */
		mLocProviderDisabledReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				boolean providerDisabled = !intent.getBooleanExtra(android.location.LocationManager.KEY_PROVIDER_ENABLED, false);
				/* Re-register the location listeners using the best available Location Provider. */
				if (providerDisabled) {
					enableLocationUpdates();
				}
			}
		};
		/*
		 * If the best Location Provider (usually GPS) is not available when we request location
		 * updates, this listener will be notified if / when it becomes available. It calls
		 * requestLocationUpdates to re-register the location listeners using the better Location
		 * Provider.
		 */
		mBestInactiveLocationProviderListener = new LocationListener() {
			public void onLocationChanged(Location location) {}

			public void onProviderDisabled(String provider) {}

			public void onStatusChanged(String provider, int status, Bundle extras) {}

			public void onProviderEnabled(String provider) {
				/* Re-register the location listeners using the better Location Provider. */
				enableLocationUpdates();
			}
		};
		/*
		 * One-off location listener that receives updates from the {@link LastLocationFinder}.
		 * This is triggered where the last known location is outside the bounds of our maximum
		 * distance and latency.
		 */
		mOneShotLastLocationUpdateListener = new LocationListener() {
			public void onLocationChanged(Location location) {
				setLocation(location);
			}

			public void onProviderDisabled(String provider) {}

			public void onStatusChanged(String provider, int status, Bundle extras) {}

			public void onProviderEnabled(String provider) {}
		};

		/* Setup the location update Pending Intents */
		Intent activeIntent = new Intent(mApplicationContext, LocationChangedReceiver.class);
		mLocationListenerPendingIntent = PendingIntent.getBroadcast(mApplicationContext, 0, activeIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		Intent passiveIntent = new Intent(mApplicationContext, PassiveLocationChangedReceiver.class);
		mLocationListenerPassivePendingIntent = PendingIntent.getBroadcast(mApplicationContext, 0, passiveIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		/*
		 * Instantiate a LastLocationFinder class.
		 * This will be used to find the last known location when the application starts.
		 */
		mLastLocationFinder = PlatformSpecificImplementationFactory.getLastLocationFinder(mApplicationContext);
		mLastLocationFinder.setChangedLocationListener(mOneShotLastLocationUpdateListener);
		/*
		 * Instantiate a Location Update Requester class based on the available platform version.
		 * This will be used to request location updates.
		 */
		mLocationUpdateRequester = PlatformSpecificImplementationFactory.getLocationUpdateRequester(mLocationManager);
	}

	// --------------------------------------------------------------------------------------------
	// Location monitoring
	// --------------------------------------------------------------------------------------------

	public void enableLocationUpdates() {
		/*
		 * This gets called when the activity is in the foreground so
		 * make sure we aren't doing double duty with passive updats.
		 */
		disablePassiveLocationUpdates();

		/* Normal updates while activity is visible. */
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		mLocationUpdateRequester.requestLocationUpdates(PlacesConstants.MIN_TIME_UPDATES
				, PlacesConstants.MIN_DISTANCE_UPDATES
				, criteria
				, mLocationListenerPendingIntent);

		// Register a receiver that listens for when a better provider than I'm using becomes available.
		String bestProvider = mLocationManager.getBestProvider(criteria, false);
		String bestAvailableProvider = mLocationManager.getBestProvider(criteria, true);
		if (bestProvider != null && bestAvailableProvider != null && !bestProvider.equals(bestAvailableProvider)) {
			mLocationManager.requestLocationUpdates(bestProvider, 0, 0, mBestInactiveLocationProviderListener, mApplicationContext.getMainLooper());
		}

		// Register a receiver that listens for when the provider I'm using has been disabled. 
		IntentFilter intentFilter = new IntentFilter(PlacesConstants.ACTIVE_LOCATION_UPDATE_PROVIDER_DISABLED);
		mApplicationContext.registerReceiver(mLocProviderDisabledReceiver, intentFilter);
	}

	public void disableLocationUpdates(Boolean isFinishing) {

		Logger.d(this, "Active updates disabled");
		try {
			mApplicationContext.unregisterReceiver(mLocProviderDisabledReceiver);
		}
		catch (Exception e) {}

		mLocationManager.removeUpdates(mLocationListenerPendingIntent);
		mLocationManager.removeUpdates(mBestInactiveLocationProviderListener);

		if (isFinishing) {
			/* Kill our oneshot if it's active */
			//			mLastLocationFinder.cancel();
			if (PlacesConstants.ENABLE_PASSIVE_LOCATION_WHEN_USER_EXIT) {
				enablePassiveLocationUpdates();
			}
		}
		else {
			if (PlacesConstants.ENABLE_DISABLE_PASSIVE_LOCATION_WHEN_BACKGROUND) {
				enablePassiveLocationUpdates();
			}
		}
	}

	private void enablePassiveLocationUpdates() {
		mLocationUpdateRequester.requestPassiveLocationUpdates(PlacesConstants.MIN_TIME_UPDATES_PASSIVE
				, PlacesConstants.MIN_DISTANCE_UPDATES_PASSIVE
				, mLocationListenerPassivePendingIntent);
	}

	private void disablePassiveLocationUpdates() {
		Logger.d(this, "Passive updates disabled");
		mLocationManager.removeUpdates(mLocationListenerPassivePendingIntent);
	}

	// --------------------------------------------------------------------------------------------
	// Location start/stop
	// --------------------------------------------------------------------------------------------

	public void lockLocation(Boolean forceOneShot) {
		/*
		 * Find the last known location, specifying a required accuracy of within the min distance between
		 * updates and a required latency of the minimum time required between updates.
		 */
		if (forceOneShot || !isGoodLocation(mLocation)) {
			Location lastKnownLocation = mLastLocationFinder.getLastBestLocation(PlacesConstants.MIN_DISTANCE_UPDATES, System.currentTimeMillis()
					- PlacesConstants.MIN_TIME_UPDATES);
			setLocation(lastKnownLocation);
		}
	}

	public void lockLocationBurst() {
		/*
		 * This will produce rapid updates across all available providers until we get an acceptable
		 * location fix.
		 */
		Logger.d(this, "Burst mode started");
		mLocationManager.removeUpdates(mLocationListenerPendingIntent);
		setLocation(null);
		mLocationModeBurst = true;
		if (isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
			mLocationManager.requestLocationUpdates(android.location.LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListenerPendingIntent);
		}
		if (isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
			mLocationManager.requestLocationUpdates(android.location.LocationManager.GPS_PROVIDER, 0, 0, mLocationListenerPendingIntent);
		}

		Aircandi.applicationHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				Logger.d(LocationManagerOld.this, "Burst mode stopped: timed out");
				mLocationManager.removeUpdates(mLocationListenerPendingIntent);
				mLocationModeBurst = false;
			}
		}, PlacesConstants.BURST_TIMEOUT);
	}

	// --------------------------------------------------------------------------------------------
	// Location routines
	// --------------------------------------------------------------------------------------------

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

	public void setLocation(Location location) {
		/*
		 * If aircandi crashes or is stopped, the active receiver can still be running
		 * so we check and switch to passive if appropriate.
		 */
		if (!Aircandi.getInstance().wasLaunchedNormally()) {
			Logger.d(this, "Aircandi not launched normally, switching to passive updates");
			this.initialize(Aircandi.getInstance().getApplicationContext());
			disableLocationUpdates(true);
			return;
		}
		/*
		 * Updates the reference location only if the submitted location is better than the current
		 * reference location. Notifies any location change listeners.
		 */
		if (location == null) {
			Logger.d(this, "Location cleared");
			mLocation = null;
			Events.EventBus.onLocationChanged(mLocation);
		}
		else {
			if (isGoodLocation(location)) {
				LocationBetterReason reason = isBetterLocation(location, mLocation);
				if (reason != LocationBetterReason.None) {
					String message = new String("Location changed:");
					message += " provider: " + location.getProvider();
					message += " accuracy: " + String.valueOf(location.getAccuracy());
					message += " reason: ** " + reason.name().toLowerCase() + " **";
					Logger.d(this, message);

					mLocation = location;
					Events.EventBus.onLocationChanged(mLocation);
					if (mLocationModeBurst) {
						if (location.getAccuracy() <= PlacesConstants.DESIRED_ACCURACY) {
							Logger.d(this, "Burst mode stopped: desired accuracy reached");
							mLocationManager.removeUpdates(mLocationListenerPendingIntent);
							mLocationModeBurst = false;
						}
					}
				}
			}
		}
	}

	public Location getLocation() {
		return mLocation;
	}

	public String getDebugStringForLocation() {
		if (mLocation != null) {
			String debug = "";
			debug += mLocation.getProvider().substring(0, 1).toUpperCase();
			if (mLocation.hasAccuracy()) {
				debug += String.format("%.0f", mLocation.getAccuracy());
			}
			return debug;
		}
		else {
			return null;
		}
	}

	public static boolean isGoodLocation(Location location) {
		if (location == null) return false;

		long fixAge = System.currentTimeMillis() - location.getTime();
		float fixAccuracy = location.getAccuracy();
		if ((fixAge <= PlacesConstants.MAXIMUM_AGE && fixAccuracy <= PlacesConstants.MINIMUM_ACCURACY)) {
			return true;
		}
		return false;
	}

	public static LocationBetterReason isBetterLocation(Location locationToEvaluate, Location currentBestLocation) {
		/*
		 * Evaluates based on distance moved, freshness, and accuracy.
		 */

		/* A new location is always better than no location */
		if (currentBestLocation == null) {
			return LocationBetterReason.NotNull;
		}

		/* Check distance moved and adjust for accuracy */
		float distance = currentBestLocation.distanceTo(locationToEvaluate);
		if (distance - locationToEvaluate.getAccuracy() > PlacesConstants.MIN_DISTANCE_UPDATES) {
			return LocationBetterReason.Distance;
		}

		/* Check whether the new location fix is newer or older */
		long timeDelta = locationToEvaluate.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > CandiConstants.TIME_TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -CandiConstants.TIME_TWO_MINUTES;
		boolean isNewer = timeDelta > 0;

		/*
		 * If it's been more than two minutes since the current location, use
		 * the new location because the user has likely moved
		 */
		if (isSignificantlyNewer) {
			/* If the new location is more than two minutes older, it must be worse */
			return LocationBetterReason.Recency;
		}
		else if (isSignificantlyOlder) {
			return LocationBetterReason.None;
		}

		/* Check whether the new location fix is more or less accurate */
		int accuracyDelta = (int) (locationToEvaluate.getAccuracy() - currentBestLocation.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 50;

		/* Check if the old and new location are from the same provider */
		boolean isFromSameProvider = LocationManagerOld.isSameProvider(locationToEvaluate.getProvider(), currentBestLocation.getProvider());

		/*
		 * Determine location quality using a combination of timeliness and accuracy
		 */
		if (isMoreAccurate) {
			return LocationBetterReason.Accuracy;
		}
		else if (isNewer && !isLessAccurate) {
			return LocationBetterReason.Recency;
		}
		else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
			return LocationBetterReason.Recency;
		}
		return LocationBetterReason.None;
	}

	public static Boolean hasMoved(Location locationToEvaluate, Location currentBestLocation) {
		if (currentBestLocation == null) {
			return true;
		}
		/* Check distance moved and adjust for accuracy */
		float distance = currentBestLocation.distanceTo(locationToEvaluate);
		if (distance >= PlacesConstants.DIST_FIVE_METERS) {
			return true;
		}
		//		if (distance - locationToEvaluate.getAccuracy() > PlacesConstants.MIN_DISTANCE_UPDATES) {
		//			return true;
		//		}
		return false;
	}

	public boolean isProviderEnabled(String provider) {
		return (mLocationManager.isProviderEnabled(provider));
	}

	public boolean isLocationAccessEnabled() {
		return (isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) || isProviderEnabled(android.location.LocationManager.GPS_PROVIDER));
	}

	public static float getRadiusForMeters(float meters) {
		float radius = (float) ((meters / 1000) / RADIUS_EARTH_KILOMETERS);
		return radius;
	}

	static boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

	public enum LocationBetterReason {
		Distance,
		Recency,
		Accuracy,
		NotNull,
		None
	}
}
