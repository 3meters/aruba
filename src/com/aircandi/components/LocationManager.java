package com.aircandi.components;

import java.util.Locale;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import com.aircandi.Aircandi;
import com.aircandi.CandiConstants;
import com.aircandi.PlacesConstants;
import com.aircandi.service.objects.Observation;
import com.aircandi.ui.Preferences;
import com.aircandi.utilities.DateUtils;

@SuppressWarnings("ucd")
public class LocationManager {

	private static LocationManager				singletonObject;
	public static Double						RADIUS_EARTH_MILES			= 3958.75;
	public static Double						RADIUS_EARTH_KILOMETERS		= 6371.0;

	private Context								mApplicationContext;
	protected android.location.LocationManager	mLocationManager;

	private Location							mLocation;
	private Boolean								mLocationModeBurstNetwork	= false;
	private Boolean								mLocationModeBurstGps		= false;
	protected PendingIntent						mLocationListenerPendingIntentNetwork;
	protected PendingIntent						mLocationListenerPendingIntentGps;

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

		/* Setup the location update Pending Intents */
		Intent activeIntentNetwork = new Intent(mApplicationContext, LocationChangedReceiver.class);
		mLocationListenerPendingIntentNetwork = PendingIntent.getBroadcast(mApplicationContext, 0, activeIntentNetwork, PendingIntent.FLAG_UPDATE_CURRENT);

		Intent activeIntentGps = new Intent(mApplicationContext, LocationChangedReceiver.class);
		mLocationListenerPendingIntentGps = PendingIntent.getBroadcast(mApplicationContext, 0, activeIntentGps, PendingIntent.FLAG_UPDATE_CURRENT);
	}

	// --------------------------------------------------------------------------------------------
	// Location routines
	// --------------------------------------------------------------------------------------------

	public void lockLocationBurst() {
		/*
		 * This will produce rapid updates across all available providers until we get an acceptable
		 * location fix.
		 */
		mLocationManager.removeUpdates(mLocationListenerPendingIntentNetwork);
		mLocationManager.removeUpdates(mLocationListenerPendingIntentGps);

		setLocation(null);

		if (isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
			Logger.d(this, "Network burst mode started");
			mLocationManager.requestLocationUpdates(android.location.LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListenerPendingIntentNetwork);
			mLocationModeBurstNetwork = true;
		}
		if (isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
			Logger.d(this, "Gps burst mode started");
			mLocationManager.requestLocationUpdates(android.location.LocationManager.GPS_PROVIDER, 0, 0, mLocationListenerPendingIntentGps);
			mLocationModeBurstGps = true;
		}

		Aircandi.mainThreadHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				Logger.d(LocationManager.this, "Burst mode stopped: timed out");
				mLocationManager.removeUpdates(mLocationListenerPendingIntentNetwork);
				mLocationManager.removeUpdates(mLocationListenerPendingIntentGps);
				mLocationModeBurstNetwork = false;
				mLocationModeBurstGps = false;
				Aircandi.mainThreadHandler.removeCallbacks(this);
			}
		}, PlacesConstants.BURST_TIMEOUT);
	}

	public void stopLocationBurst() {
		Logger.d(LocationManager.this, "Burst mode stopped: disabled");
		if (mLocationModeBurstNetwork) {
			mLocationManager.removeUpdates(mLocationListenerPendingIntentNetwork);
		}
		if (mLocationModeBurstGps) {
			mLocationManager.removeUpdates(mLocationListenerPendingIntentGps);
		}
		mLocationModeBurstNetwork = false;
		mLocationModeBurstGps = false;
	}

	public Location getLastKnownLocation() {
		Location location = null;

		Location locationCandidate = mLocationManager.getLastKnownLocation(android.location.LocationManager.PASSIVE_PROVIDER);
		LocationBetterReason reason = LocationBetterReason.None;
		if (locationCandidate != null) {
			reason = isBetterLocation(locationCandidate, location);
			if (reason != LocationBetterReason.None) {
				location = locationCandidate;
			}
		}

		locationCandidate = mLocationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER);
		if (locationCandidate != null) {
			reason = isBetterLocation(locationCandidate, location);
			if (reason != LocationBetterReason.None) {
				location = locationCandidate;
			}
		}

		locationCandidate = mLocationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER);
		if (locationCandidate != null) {
			reason = isBetterLocation(locationCandidate, location);
			if (reason != LocationBetterReason.None) {
				location = locationCandidate;
			}
		}

		if (!isGoodLocation(location)) {
			location = null;
		}

		return location;
	}

	// --------------------------------------------------------------------------------------------
	// Support routines
	// --------------------------------------------------------------------------------------------

	public Observation getObservation() {
		return getObservationForLocation(null);
	}

	public Observation getObservationForLocation(Location location) {

		Location locationTarget = location;

		if (location == null) {
			locationTarget = mLocation;
		}

		if (locationTarget == null || !locationTarget.hasAccuracy()) {
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
			Logger.d(this, "Aircandi not launched normally, refusing location");
			this.initialize(Aircandi.getInstance().getApplicationContext());
			return;
		}
		/*
		 * Updates the reference location only if the submitted location is better than the current
		 * reference location. Notifies any location change listeners.
		 */
		if (location == null) {
			Logger.d(this, "Location cleared");
			mLocation = null;
			BusProvider.getInstance().post(new LocationChangedEvent(mLocation));
		}
		else {
			if (isGoodLocation(location)) {
				LocationBetterReason reason = isBetterLocation(location, mLocation);
				if (reason != LocationBetterReason.None) {
					String message = new String("Location changed:");
					message += " provider: " + location.getProvider();
					message += " accuracy: " + String.valueOf(location.getAccuracy());
					message += " reason: ** " + reason.name().toLowerCase(Locale.US) + " **";
					Logger.d(this, message);

					if (reason == LocationBetterReason.NotNull
							|| reason == LocationBetterReason.Provider
							|| reason == LocationBetterReason.Accuracy) {

						mLocation = location;
						BusProvider.getInstance().post(new LocationChangedEvent(mLocation));

						if (location.getProvider().equals("network") && mLocationModeBurstNetwork) {
							if (location.getAccuracy() <= PlacesConstants.DESIRED_ACCURACY_NETWORK) {
								Logger.d(this, "Network burst mode stopped: desired accuracy reached");
								mLocationModeBurstNetwork = false;
								if (!mLocationModeBurstGps) {
									mLocationManager.removeUpdates(mLocationListenerPendingIntentNetwork);
								}
							}
						}
						else if (location.getProvider().equals("gps") && mLocationModeBurstGps) {
							if (location.getAccuracy() <= PlacesConstants.DESIRED_ACCURACY_GPS) {
								Logger.d(this, "Gps burst mode stopped: desired accuracy reached");
								mLocationModeBurstGps = false;
								if (!mLocationModeBurstNetwork) {
									mLocationManager.removeUpdates(mLocationListenerPendingIntentGps);
								}
							}
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
			debug += mLocation.getProvider().substring(0, 1).toUpperCase(Locale.US);
			if (mLocation.hasAccuracy()) {
				debug += String.format("%.0f", mLocation.getAccuracy());
			}
			return debug;
		}
		else {
			return null;
		}
	}

	private static boolean isGoodLocation(Location location) {
		if (location == null) return false;

		long fixAge = System.currentTimeMillis() - location.getTime();
		float fixAccuracy = location.getAccuracy();
		if ((fixAge <= PlacesConstants.MAXIMUM_AGE && fixAccuracy <= PlacesConstants.MINIMUM_ACCURACY)) {
			return true;
		}
		return false;
	}

	private static LocationBetterReason isBetterLocation(Location locationToEvaluate, Location currentBestLocation) {
		/*
		 * Evaluates based on distance moved, freshness, and accuracy.
		 */

		/* A new location is always better than no location */
		if (currentBestLocation == null) {
			return LocationBetterReason.NotNull;
		}

		/* A good gps location is always better than an excellent network location */
		if (currentBestLocation.getProvider().equals("network") && locationToEvaluate.getProvider().equals("gps")) {
			return LocationBetterReason.Provider;
		}

		/* Do not replace a good gps location with a network location */
		if (currentBestLocation.getProvider().equals("gps") && locationToEvaluate.getProvider().equals("network")) {
			return LocationBetterReason.None;
		}

		/* Check whether the new location fix is newer or older */
		long timeDelta = locationToEvaluate.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > CandiConstants.TIME_TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -CandiConstants.TIME_TWO_MINUTES;
		boolean isNewer = timeDelta > 0;

		/* Check whether the new location fix is more or less accurate */
		int accuracyDelta = (int) (locationToEvaluate.getAccuracy() - currentBestLocation.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 50;

		/* Check if the old and new location are from the same provider */
		boolean isFromSameProvider = LocationManager.isSameProvider(locationToEvaluate.getProvider(), currentBestLocation.getProvider());
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

		/* Check distance moved and adjust for accuracy */
		float distance = currentBestLocation.distanceTo(locationToEvaluate);
		if (distance - locationToEvaluate.getAccuracy() > PlacesConstants.MIN_DISTANCE_UPDATES) {
			return LocationBetterReason.Distance;
		}

		return LocationBetterReason.None;
	}

	public static Boolean hasMoved(Location locationToEvaluate, Location currentBestLocation, Integer minDistance) {
		if (currentBestLocation == null) {
			return true;
		}
		/* Check distance moved and adjust for accuracy */
		float distance = currentBestLocation.distanceTo(locationToEvaluate);
		if (distance >= minDistance) {
			return true;
		}
		//		if (distance - locationToEvaluate.getAccuracy() > PlacesConstants.MIN_DISTANCE_UPDATES) {
		//			return true;
		//		}
		return false;
	}

	private boolean isProviderEnabled(String provider) {
		return (mLocationManager.isProviderEnabled(provider));
	}

	public boolean isLocationAccessEnabled() {
		return (isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) || isProviderEnabled(android.location.LocationManager.GPS_PROVIDER));
	}

	public static float getRadiusForMeters(float meters) {
		float radius = (float) ((meters / 1000) / RADIUS_EARTH_KILOMETERS);
		return radius;
	}

	private static boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

	private enum LocationBetterReason {
		Distance,
		Recency,
		Accuracy,
		Provider,
		NotNull,
		None
	}
}
