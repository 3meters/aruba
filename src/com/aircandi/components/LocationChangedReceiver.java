package com.aircandi.components;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import com.aircandi.Constants;
import com.aircandi.events.LocationReceivedEvent;


/**
 * This Receiver class is used to listen for Broadcast Intents that announce
 * that a location change has occurred. This is used instead of a LocationListener
 * within an Activity is our only action is to start a service.
 */
@SuppressWarnings("ucd")
public class LocationChangedReceiver extends BroadcastReceiver {

	protected static String	TAG	= "LocationChangedReceiver";

	/**
	 * When a new location is received, extract it from the Intent and use
	 * it to start the service used to update the list of nearby places.
	 * 
	 * This is the Active receiver, used to receive Location updates when
	 * the Activity is visible.
	 */
	@Override
	public void onReceive(Context context, Intent intent) {

		final String locationKey = android.location.LocationManager.KEY_LOCATION_CHANGED;
		final String providerEnabledKey = android.location.LocationManager.KEY_PROVIDER_ENABLED;

		if (intent.hasExtra(providerEnabledKey)) {
			if (!intent.getBooleanExtra(providerEnabledKey, true)) {
				final Intent providerDisabledIntent = new Intent(Constants.ACTIVE_LOCATION_UPDATE_PROVIDER_DISABLED);
				context.sendBroadcast(providerDisabledIntent);
			}
		}

		if (intent.hasExtra(locationKey)) {
			final Location location = (Location) intent.getExtras().get(locationKey);
			if (location != null) {
				Logger.d(this, "Active location received: " + location.getProvider() + " "
						+ "; accuracy: " + location.getAccuracy()
						+ "; lat/lng: " + location.getLatitude() + ","
						+ location.getLongitude());
				BusProvider.getInstance().post(new LocationReceivedEvent(location));
				LocationManager.getInstance().setLocation(location);
			}
		}
	}
}