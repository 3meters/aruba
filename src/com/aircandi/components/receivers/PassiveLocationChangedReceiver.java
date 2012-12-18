package com.aircandi.components.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import com.aircandi.components.Logger;
import com.aircandi.components.location.LegacyLastLocationFinder;
import com.aircandi.components.location.LocationManager;
import com.aircandi.components.location.PlacesConstants;

/**
 * This Receiver class is used to listen for Broadcast Intents that announce
 * that a location change has occurred while this application isn't visible.
 */
public class PassiveLocationChangedReceiver extends BroadcastReceiver {

	protected static String	TAG	= "PassiveLocationChangedReceiver";

	/**
	 * When a new location is received, extract it from the Intent and pass
	 * it along to the aircandi LocationManager.
	 * 
	 * This is the Passive receiver, used to receive Location updates from
	 * third party apps when the Activity is not visible.
	 */
	@Override
	public void onReceive(Context context, Intent intent) {

		String locationKey = android.location.LocationManager.KEY_LOCATION_CHANGED;
		Location location = null;

		if (intent.hasExtra(locationKey)) {
			/*
			 * This update came from android Passive provider, so we can extract the location
			 * directly.
			 */
			location = (Location) intent.getExtras().get(locationKey);
		}
		else {
			/*
			 * This update came from a recurring alarm. We need to determine if there
			 * has been a more recent Location received than the last location we used.
			 * 
			 * Get the best last location detected from the providers.
			 */
			LegacyLastLocationFinder lastLocationFinder = new LegacyLastLocationFinder(context);
			location = lastLocationFinder.getLastBestLocation(PlacesConstants.MIN_DISTANCE_UPDATES, System.currentTimeMillis() - PlacesConstants.MIN_TIME_UPDATES);
		}

		// Start the Service used to find nearby points of interest based on the last detected location.
		if (location != null) {
			Logger.d(this, "Passive location received: " + location.getProvider() + " "
					+ "; accuracy: " + location.getAccuracy()
					+ "; lat/lng: " + location.getLatitude() + ","
					+ location.getLongitude());

			LocationManager.getInstance().setLocation(location);
		}
	}
}