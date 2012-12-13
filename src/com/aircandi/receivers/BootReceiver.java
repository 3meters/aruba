package com.aircandi.receivers;

import com.aircandi.Aircandi;
import com.aircandi.components.Logger;
import com.aircandi.core.PlacesConstants;
import com.aircandi.location.LocationUpdateRequester;
import com.aircandi.location.PlatformSpecificImplementationFactory;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;

/**
 * This Receiver class is designed to listen for system boot.
 * 
 * If the app has been run at least once, the passive location updates should be enabled after a reboot.
 */
public class BootReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		

		SharedPreferences prefs = Aircandi.settings;
		boolean runOnce = prefs.getBoolean(PlacesConstants.SP_KEY_RUN_ONCE, false);

		if (runOnce) {
			LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
			/*
			 * Check the Shared Preferences to see if we are updating location changes.
			 */
			boolean followLocationChanges = prefs.getBoolean(PlacesConstants.SP_KEY_FOLLOW_LOCATION_CHANGES, true);
			if (followLocationChanges) {
				Logger.i(this, "System booted: starting passive location updates");
				/*
				 * Passive location updates from 3rd party apps when the Activity isn't visible.
				 * 
				 * Instantiate a Location Update Requester class based on the available platform version.
				 * This will be used to request location updates.
				 */
				LocationUpdateRequester locationUpdateRequester = PlatformSpecificImplementationFactory.getLocationUpdateRequester(locationManager);
				Intent passiveIntent = new Intent(context, PassiveLocationChangedReceiver.class);
				PendingIntent locationListenerPassivePendingIntent = PendingIntent.getActivity(context, 0, passiveIntent, PendingIntent.FLAG_UPDATE_CURRENT);
				
				locationUpdateRequester.requestPassiveLocationUpdates(PlacesConstants.PASSIVE_MAX_TIME
						, PlacesConstants.PASSIVE_MAX_DISTANCE
						, locationListenerPassivePendingIntent);
			}
		}
		else {
			Logger.i(this, "System booted: first run");
		}
	}
}