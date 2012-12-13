package com.aircandi.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import com.aircandi.Aircandi;
import com.aircandi.components.LocationManager;
import com.aircandi.components.Logger;
import com.aircandi.core.PlacesConstants;
import com.aircandi.location.LegacyLastLocationFinder;

/**
 * This Receiver class is used to listen for Broadcast Intents that announce
 * that a location change has occurred while this application isn't visible.
 * 
 * Where possible, this is triggered by a Passive Location listener.
 */
public class PassiveLocationChangedReceiver extends BroadcastReceiver {

	protected static String	TAG	= "PassiveLocationChangedReceiver";

	/**
	 * When a new location is received, extract it from the Intent and use
	 * it to start the Service used to update the list of nearby places.
	 * 
	 * This is the Passive receiver, used to receive Location updates from
	 * third party apps when the Activity is not visible.
	 */
	@Override
	public void onReceive(Context context, Intent intent) {

		String key = android.location.LocationManager.KEY_LOCATION_CHANGED;
		Location location = null;
		Logger.v(this, "Passive location update received");

		if (intent.hasExtra(key)) {
			/*
			 * This update came from Passive provider, so we can extract the location
			 * directly.
			 */
			location = (Location) intent.getExtras().get(key);
		}
		else {
			/*
			 * This update came from a recurring alarm. We need to determine if there
			 * has been a more recent Location received than the last location we used.
			 */

			/* Get the best last location detected from the providers. */
			LegacyLastLocationFinder lastLocationFinder = new LegacyLastLocationFinder(context);
			location = lastLocationFinder.getLastBestLocation(PlacesConstants.MAX_DISTANCE, System.currentTimeMillis() - PlacesConstants.MAX_TIME);

			/* Get the last location we used to get a listing. */
			long lastTime = Aircandi.settings.getLong(PlacesConstants.SP_KEY_LAST_LIST_UPDATE_TIME, Long.MIN_VALUE);
			long lastLat = Aircandi.settings.getLong(PlacesConstants.SP_KEY_LAST_LIST_UPDATE_LAT, Long.MIN_VALUE);
			long lastLng = Aircandi.settings.getLong(PlacesConstants.SP_KEY_LAST_LIST_UPDATE_LNG, Long.MIN_VALUE);

			Location lastLocation = new Location(PlacesConstants.CONSTRUCTED_LOCATION_PROVIDER);
			lastLocation.setLatitude(lastLat);
			lastLocation.setLongitude(lastLng);

			/*
			 * Check if the last location detected from the providers is either too soon, or too close to the last
			 * value we used. If it is within those thresholds we set the location to null to prevent the update
			 * Service being run unnecessarily (and spending battery on data transfers).
			 */
			if ((lastTime > System.currentTimeMillis() - PlacesConstants.MAX_TIME)
					|| (lastLocation.distanceTo(location) < PlacesConstants.MAX_DISTANCE))
				location = null;
		}

		// Start the Service used to find nearby points of interest based on the last detected location.
		if (location != null) {
			Logger.d(this, "Passive location update used: " + location.getProvider() + " "
					+ "; accuracy: " + location.getAccuracy()
					+ "; lat/lng: " + location.getLatitude() + ","
					+ location.getLongitude());

			LocationManager.getInstance().setLocation(location);

			//			Intent updateServiceIntent = new Intent(context, PlacesUpdateService.class);
			//			updateServiceIntent.putExtra(PlacesConstants.EXTRA_KEY_LOCATION, location);
			//			updateServiceIntent.putExtra(PlacesConstants.EXTRA_KEY_RADIUS, PlacesConstants.DEFAULT_RADIUS);
			//			updateServiceIntent.putExtra(PlacesConstants.EXTRA_KEY_FORCEREFRESH, false);
			//			context.startService(updateServiceIntent);
		}
	}
}