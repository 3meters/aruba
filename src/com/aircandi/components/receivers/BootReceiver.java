package com.aircandi.components.receivers;

import com.aircandi.Aircandi;
import com.aircandi.components.Logger;
import com.aircandi.components.location.LocationUpdateRequester;
import com.aircandi.components.location.PlacesConstants;
import com.aircandi.components.location.PlatformSpecificImplementationFactory;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;

/**
 * This Receiver class is designed to listen for system boot.
 * 
 * If the app has been run at least once, the passive location updates should be enabled after a reboot. This will run
 * until aircandi is started and passive updates will be actively managed by the app from that point on.
 * 
 * The purpose is to get the fastest location fix by getting started even before aircandi is launched.
 */
public class BootReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {

		SharedPreferences prefs = Aircandi.settings;
		boolean runOnce = prefs.getBoolean(PlacesConstants.SP_KEY_RUN_ONCE, false);

		if (runOnce) {
			LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
			Logger.i(this, "System booted: Passive updates enabled");
			/*
			 * Passive location updates from 3rd party apps when the Activity isn't visible.
			 * 
			 * Instantiate a Location Update Requester class based on the available platform version.
			 * This will be used to request location updates.
			 */
			LocationUpdateRequester locationUpdateRequester = PlatformSpecificImplementationFactory.getLocationUpdateRequester(locationManager);
			Intent passiveIntent = new Intent(context, PassiveLocationChangedReceiver.class);
			PendingIntent locationListenerPassivePendingIntent = PendingIntent.getBroadcast(context, 0, passiveIntent, PendingIntent.FLAG_UPDATE_CURRENT);

			locationUpdateRequester.requestPassiveLocationUpdates(PlacesConstants.MIN_TIME_UPDATES_PASSIVE
					, PlacesConstants.MIN_DISTANCE_UPDATES_PASSIVE
					, locationListenerPassivePendingIntent);
		}
		else {
			Logger.i(this, "System booted: first run");
		}
	}
}