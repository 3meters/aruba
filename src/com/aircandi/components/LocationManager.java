package com.aircandi.components;

import java.util.List;
import java.util.Locale;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.events.LocationChangedEvent;
import com.aircandi.events.LocationTimeoutEvent;
import com.aircandi.service.objects.AirLocation;

@SuppressWarnings("ucd")
public class LocationManager {

	public static Double						RADIUS_EARTH_MILES			= 3958.75;
	public static Double						RADIUS_EARTH_KILOMETERS		= 6371.0;
	public static final float					MetersToMilesConversion		= 0.000621371192237334f;
	public static final float					MetersToFeetConversion		= 3.28084f;
	public static final float					MetersToYardsConversion		= 1.09361f;
	public static final float					FeetToMetersConversion		= 0.3048f;

	private Context								mApplicationContext;
	protected android.location.LocationManager	mLocationManager;

	private Location							mLocationLatest;
	private Location							mLocationLocked;
	private AirLocation							mAirLocationLocked;
	
	private Boolean								mLocationModeBurstNetwork	= false;
	private Boolean								mLocationModeBurstGps		= false;
	protected PendingIntent						mLocationListenerPendingIntent;
	private Runnable							mBurstTimeout;

	private LocationManager() {}

	private static class LocationManagerHolder {
		public static final LocationManager	instance	= new LocationManager();
	}

	public static LocationManager getInstance() {
		return LocationManagerHolder.instance;
	}

	public void initialize(Context applicationContext) {

		Logger.d(this, "Initializing the LocationManager");
		mApplicationContext = applicationContext;
		mLocationManager = (android.location.LocationManager) mApplicationContext.getSystemService(Context.LOCATION_SERVICE);

		/* Setup the location update Pending Intents */
		final Intent activeIntentNetwork = new Intent(mApplicationContext, LocationChangedReceiver.class);
		mLocationListenerPendingIntent = PendingIntent.getBroadcast(mApplicationContext, 0, activeIntentNetwork, PendingIntent.FLAG_CANCEL_CURRENT);

		/* Timeout handler */
		mBurstTimeout = new Runnable() {

			@Override
			public void run() {
				Logger.d(LocationManager.this, "Burst mode stopped: timed OUT");
				Aircandi.mainThreadHandler.removeCallbacks(mBurstTimeout);
				mLocationManager.removeUpdates(mLocationListenerPendingIntent);
				mLocationModeBurstNetwork = false;
				mLocationModeBurstGps = false;
				BusProvider.getInstance().post(new LocationTimeoutEvent());
			}
		};
	}

	// --------------------------------------------------------------------------------------------
	// Location routines
	// --------------------------------------------------------------------------------------------

	public void lockLocationBurst() {
		/*
		 * This will produce rapid updates across all available providers until we get an acceptable
		 * location fix.
		 */
		mLocationManager.removeUpdates(mLocationListenerPendingIntent);

		setLocation(null);

		if (isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
			Logger.d(this, "Network burst mode started");
			mLocationManager.requestLocationUpdates(android.location.LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListenerPendingIntent);
			mLocationModeBurstNetwork = true;
		}
		if (isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
			Logger.d(this, "Gps burst mode started");
			mLocationManager.requestLocationUpdates(android.location.LocationManager.GPS_PROVIDER, 0, 0, mLocationListenerPendingIntent);
			mLocationModeBurstGps = true;
		}

		Aircandi.mainThreadHandler.postDelayed(mBurstTimeout, Constants.BURST_TIMEOUT);
	}

	public void stopLocationBurst() {
		Logger.d(LocationManager.this, "Burst mode stopped: disabled");
		Aircandi.mainThreadHandler.removeCallbacks(mBurstTimeout);
		mLocationManager.removeUpdates(mLocationListenerPendingIntent);
		mLocationModeBurstNetwork = false;
		mLocationModeBurstGps = false;
	}

	public Location getLastKnownLocation() {

		Location location = null;
		LocationBetterReason reason = LocationBetterReason.NONE;
		List<String> providers = mLocationManager.getAllProviders();
		for (String provider : providers) {
			Location locationCandidate = mLocationManager.getLastKnownLocation(provider);
			if (locationCandidate != null) {
				reason = isBetterLocation(locationCandidate, location);
				if (reason != LocationBetterReason.NONE) {
					location = locationCandidate;
				}
			}
		}

		return location;
	}

	// --------------------------------------------------------------------------------------------
	// Support routines
	// --------------------------------------------------------------------------------------------

	private AirLocation getAirLocationForLockedLocation() {

		AirLocation location = new AirLocation();

		if (mLocationLocked == null || !mLocationLocked.hasAccuracy()) {
			return null;
		}

		synchronized (mLocationLocked) {

			if (Aircandi.usingEmulator) {
				location = new AirLocation(47.616245, -122.201645); // earls
				location.provider = "emulator_lucky";
			}
			else {
				final String testingLocation = Aircandi.settings.getString(Constants.PREF_TESTING_LOCATION, "natural");
				if (ListPreferenceMultiSelect.contains("zoka", testingLocation, null)) {
					location = new AirLocation(47.6686489, -122.3320842); // zoka
					location.provider = "testing_zoka";
				}
				else if (ListPreferenceMultiSelect.contains("lucky", testingLocation, null)) {
					location = new AirLocation(47.616245, -122.201645); // lucky
					location.provider = "testing_lucky";
				}
				else {
					location.lat = mLocationLocked.getLatitude();
					location.lng = mLocationLocked.getLongitude();

					if (mLocationLocked.hasAltitude()) {
						location.altitude = mLocationLocked.getAltitude();
					}
					if (mLocationLocked.hasAccuracy()) {
						/* In meters. */
						location.accuracy = mLocationLocked.getAccuracy();
					}
					if (mLocationLocked.hasBearing()) {
						/* Direction of travel IN degrees East of true North. */
						location.bearing = mLocationLocked.getBearing();
					}
					if (mLocationLocked.hasSpeed()) {
						/* Speed of the device over ground IN meters/second. */
						location.speed = mLocationLocked.getSpeed();
					}
					location.provider = mLocationLocked.getProvider();
				}
			}
		}

		return location;
	}

	public void setLocation(Location location) {
		/*
		 * If aircandi crashes or is stopped, the active receiver can still be running
		 * so we check and switch to passive if appropriate.
		 */
		if (Aircandi.LAUNCHED_NORMALLY == null) {
			Logger.d(this, "Aircandi not launched normally, refusing location");
			this.initialize(Aircandi.getInstance().getApplicationContext());
			stopLocationBurst();
			return;
		}
		/*
		 * Updates the reference location only if the submitted location is better than the current
		 * reference location. Notifies any location change listeners.
		 */
		if (location == null) {
			Logger.d(this, "Location cleared");
			mLocationLatest = null;
			BusProvider.getInstance().post(new LocationChangedEvent(mLocationLatest));
		}
		else {
			if (isGoodLocation(location)) {
				final LocationBetterReason reason = isBetterLocation(location, mLocationLatest);
				if (reason != LocationBetterReason.NONE) {
					String message = "Location changed:";
					message += " provider: " + location.getProvider();
					message += " accuracy: " + String.valueOf(location.getAccuracy());
					message += " reason: ** " + reason.name().toLowerCase(Locale.US) + " **";
					Logger.d(this, message);

					if (reason == LocationBetterReason.NOT_NULL
							|| reason == LocationBetterReason.PROVIDER
							|| reason == LocationBetterReason.ACCURACY
							|| reason == LocationBetterReason.RECENCY) {

						mLocationLatest = location;
						BusProvider.getInstance().post(new LocationChangedEvent(mLocationLatest));
					}

					if (location.getProvider().equals("NETWORK") && mLocationModeBurstNetwork) {
						if (location.getAccuracy() <= Constants.DESIRED_ACCURACY_NETWORK) {
							Logger.d(this, "Network burst mode stopped: desired accuracy reached");
							if (mLocationModeBurstGps) {
								mLocationManager.removeUpdates(mLocationListenerPendingIntent);
								mLocationManager.requestLocationUpdates(android.location.LocationManager.GPS_PROVIDER, 0, 0, mLocationListenerPendingIntent);
							}
							else {
								stopLocationBurst();
							}
						}
					}
					else if (location.getProvider().equals("gps") && mLocationModeBurstGps) {
						if (location.getAccuracy() <= Constants.DESIRED_ACCURACY_GPS) {
							Logger.d(this, "Gps burst mode stopped: desired accuracy reached");
							stopLocationBurst();
						}
					}
				}
			}
		}
	}

	public Location getLocation() {
		return mLocationLatest;
	}

	public String getDebugStringForLocation() {
		if (mLocationLatest != null) {
			String debug = "";
			debug += mLocationLatest.getProvider().substring(0, 1).toUpperCase(Locale.US);
			if (mLocationLatest.hasAccuracy()) {
				debug += String.format("%.0f", mLocationLatest.getAccuracy());
			}
			return debug;
		}
		else {
			return null;
		}
	}

	private static boolean isGoodLocation(Location location) {
		if (location == null) return false;

		final long fixAge = System.currentTimeMillis() - location.getTime();
		final float fixAccuracy = location.getAccuracy();
		if ((fixAge <= Constants.MAXIMUM_AGE_PREFERRED && fixAccuracy <= Constants.MINIMUM_ACCURACY)) {
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
			return LocationBetterReason.NOT_NULL;
		}

		/* A good gps location is always better than an excellent NETWORK location */
		if (currentBestLocation.getProvider().equals("NETWORK") && locationToEvaluate.getProvider().equals("gps")) {
			return LocationBetterReason.PROVIDER;
		}

		/* Do not replace a good gps location with a NETWORK location */
		if (currentBestLocation.getProvider().equals("gps") && locationToEvaluate.getProvider().equals("NETWORK")) {
			return LocationBetterReason.NONE;
		}

		/* Check whether the new location fix is more or less accurate */
		final float accuracyImprovement = currentBestLocation.getAccuracy() / locationToEvaluate.getAccuracy();
		boolean isLessAccurate = (accuracyImprovement > 1);
		final boolean isMoreAccurate = (accuracyImprovement < 1);
		boolean isSignificantlyLessAccurate = (accuracyImprovement <= 0.5f);

		/* Check whether the new location fix is newer or older */
		final long timeDelta = locationToEvaluate.getTime() - currentBestLocation.getTime();
		final boolean isSignificantlyNewer = timeDelta > Constants.TIME_TWO_MINUTES;
		final boolean isSignificantlyOlder = timeDelta < -Constants.TIME_TWO_MINUTES;
		final boolean isNewer = timeDelta > 0;

		/* Check if the old and new location are from the same provider */
		final boolean isFromSameProvider = LocationManager.isSameProvider(locationToEvaluate.getProvider(), currentBestLocation.getProvider());

		/* Determine location quality using a combination of timeliness and accuracy */
		if (isMoreAccurate) {
			return LocationBetterReason.ACCURACY;
		}
		else if (isNewer && !isLessAccurate) {
			return LocationBetterReason.RECENCY;
		}
		else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
			return LocationBetterReason.RECENCY;
		}
		/*
		 * If it's been more than two minutes since the current location, use
		 * the new location because the user has likely moved
		 */
		if (isSignificantlyNewer) {
			/* If the new location is more than two minutes older, it must be worse */
			return LocationBetterReason.RECENCY;
		}
		else if (isSignificantlyOlder) {
			return LocationBetterReason.NONE;
		}

		/* Check distance moved and adjust for accuracy */
		final float distance = currentBestLocation.distanceTo(locationToEvaluate);
		if (distance - locationToEvaluate.getAccuracy() > Constants.MIN_DISTANCE_UPDATES) {
			return LocationBetterReason.DISTANCE;
		}

		return LocationBetterReason.NONE;
	}

	public static Boolean hasMoved(Location locationToEvaluate, Location currentBestLocation, Integer minDistance) {
		if (currentBestLocation == null) {
			return true;
		}
		/* Check distance moved and adjust for accuracy */
		final float distance = currentBestLocation.distanceTo(locationToEvaluate);
		if (distance >= minDistance) {
			return true;
		}
		//		if (distance - locationToEvaluate.getAccuracy() > Constants.MIN_DISTANCE_UPDATES) {
		//			return true;
		//		}
		return false;
	}

	private boolean isProviderEnabled(String provider) {
		return mLocationManager.isProviderEnabled(provider);
	}

	public boolean isLocationAccessEnabled() {
		return isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) || isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
	}

	public static float getRadiusForMeters(float meters) {
		final float radius = (float) ((meters / 1000) / RADIUS_EARTH_KILOMETERS);
		return radius;
	}

	private static boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

	public Location getLocationLocked() {
		return mLocationLocked;
	}

	public void setLocationLocked(Location locationLocked) {
		mLocationLocked = locationLocked;
		mAirLocationLocked = getAirLocationForLockedLocation();
	}

	public AirLocation getAirLocationLocked() {
		return mAirLocationLocked;
	}

	private enum LocationBetterReason {
		DISTANCE,
		RECENCY,
		ACCURACY,
		PROVIDER,
		NOT_NULL,
		NONE
	}
}
