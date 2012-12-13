package com.aircandi.components;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Bundle;

import com.aircandi.Aircandi;
import com.aircandi.Preferences;
import com.aircandi.core.CandiConstants;
import com.aircandi.core.PlacesConstants;
import com.aircandi.location.ILastLocationFinder;
import com.aircandi.location.LocationUpdateRequester;
import com.aircandi.location.PlatformSpecificImplementationFactory;
import com.aircandi.receivers.LocationChangedReceiver;
import com.aircandi.receivers.PassiveLocationChangedReceiver;
import com.aircandi.service.objects.Observation;
import com.aircandi.utilities.DateUtils;

public class LocationManager {

	private static LocationManager				singletonObject;
	public static Integer						MINIMUM_ACCURACY		= 50;
	public static Long							MAXIMUM_AGE				= (long) CandiConstants.TWO_MINUTES;
	public static Double						RADIUS_EARTH_MILES		= 3958.75;
	public static Double						RADIUS_EARTH_KILOMETERS	= 6371.0;

	private Context								mApplicationContext;
	protected PackageManager					mPackageManager;
	protected android.location.LocationManager	mLocationManager;

	protected Criteria							mCriteria;
	private Location							mLocation;
	protected ILastLocationFinder				mLastLocationFinder;
	protected LocationUpdateRequester			mLocationUpdateRequester;
	protected PendingIntent						mLocationListenerPendingIntent;
	protected PendingIntent						mLocationListenerPassivePendingIntent;

	protected LocationListener					mOneShotLastLocationUpdateListener;
	protected LocationListener					mBestInactiveLocationProviderListener;
	protected BroadcastReceiver					mLocProviderDisabledReceiver;

	public static synchronized LocationManager getInstance() {
		if (singletonObject == null) {
			singletonObject = new LocationManager();
		}
		return singletonObject;
	}

	private LocationManager() {}

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
				if (providerDisabled)
					requestLocationUpdates();
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
				requestLocationUpdates();
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

		/* Specify the Criteria to use when requesting location updates while the application is Active */
		mCriteria = new Criteria();
		if (PlacesConstants.USE_GPS_WHEN_ACTIVITY_VISIBLE) {
			mCriteria.setAccuracy(Criteria.ACCURACY_FINE);
		}
		else {
			mCriteria.setPowerRequirement(Criteria.POWER_LOW);
		}

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
	// Location start/stop
	// --------------------------------------------------------------------------------------------

	protected void requestLocationUpdates() {

		/* Normal updates while activity is visible. */
		mLocationUpdateRequester.requestLocationUpdates(PlacesConstants.MAX_TIME
				, PlacesConstants.MAX_DISTANCE
				, mCriteria
				, mLocationListenerPendingIntent);

		/* Passive location updates from 3rd party apps when the Activity isn't visible. */
		mLocationUpdateRequester.requestPassiveLocationUpdates(PlacesConstants.PASSIVE_MAX_TIME
				, PlacesConstants.PASSIVE_MAX_DISTANCE
				, mLocationListenerPassivePendingIntent);

		// Register a receiver that listens for when the provider I'm using has been disabled. 
		IntentFilter intentFilter = new IntentFilter(PlacesConstants.ACTIVE_LOCATION_UPDATE_PROVIDER_DISABLED);
		mApplicationContext.registerReceiver(mLocProviderDisabledReceiver, intentFilter);

		// Register a receiver that listens for when a better provider than I'm using becomes available.
		String bestProvider = mLocationManager.getBestProvider(mCriteria, false);
		String bestAvailableProvider = mLocationManager.getBestProvider(mCriteria, true);

		if (bestProvider != null && !bestProvider.equals(bestAvailableProvider)) {
			mLocationManager.requestLocationUpdates(bestProvider, 0, 0, mBestInactiveLocationProviderListener, mApplicationContext.getMainLooper());
		}
	}

	public void disableLocationUpdates(Boolean isFinishing) {

		mApplicationContext.unregisterReceiver(mLocProviderDisabledReceiver);
		mLocationManager.removeUpdates(mLocationListenerPendingIntent);
		mLocationManager.removeUpdates(mBestInactiveLocationProviderListener);

		if (isFinishing) {
			mLastLocationFinder.cancel();
		}
		if (PlacesConstants.DISABLE_PASSIVE_LOCATION_WHEN_USER_EXIT && isFinishing) {
			mLocationManager.removeUpdates(mLocationListenerPassivePendingIntent);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Location start/stop
	// --------------------------------------------------------------------------------------------

	public void getLastLocation() {
		boolean followLocationChanges = Aircandi.settings.getBoolean(PlacesConstants.SP_KEY_FOLLOW_LOCATION_CHANGES, true);
		getLastLocation(followLocationChanges);
	}

	public void getLastLocation(boolean followLocationChanges) {
		/*
		 * Find the last known location using a LastLocationFinder.
		 */
		new AsyncTask() {
			@Override
			protected Void doInBackground(Object... params) {
				/*
				 * Find the last known location, specifying a required accuracy of within the min distance between
				 * updates and a required latency of the minimum time required between updates.
				 */
				Location lastKnownLocation = mLastLocationFinder.getLastBestLocation(PlacesConstants.MAX_DISTANCE,
						System.currentTimeMillis() - PlacesConstants.MAX_TIME);

				setLocation(lastKnownLocation);
				return null;
			}
		}.execute();

		/* If we have requested location updates, turn them on here. */
		toggleFollowLocationChanges(followLocationChanges);
	}

	public void getLastBestLocation() {}

	protected void toggleFollowLocationChanges(boolean followLocationChanges) {

		/* Save the location update status in shared preferences */
		Aircandi.settingsEditor.putBoolean(PlacesConstants.SP_KEY_FOLLOW_LOCATION_CHANGES, followLocationChanges);
		Aircandi.settingsEditor.commit();

		/* Start or stop listening for location changes */
		if (followLocationChanges) {
			requestLocationUpdates();
		}
		else {
			disableLocationUpdates(false);
		}
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

}
