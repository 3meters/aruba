package com.aircandi.location;

import com.aircandi.core.PlacesConstants;

import android.app.PendingIntent;
import android.location.LocationManager;

/**
 * Provides support for initiating active and passive location updates
 * optimized for the Froyo release. Includes use of the Passive Location Provider.
 * 
 * Uses broadcast Intents to notify the app of location changes.
 */
public class FroyoLocationUpdateRequester extends LocationUpdateRequester {

	public FroyoLocationUpdateRequester(LocationManager locationManager) {
		super(locationManager);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void requestPassiveLocationUpdates(long minTime, long minDistance, PendingIntent pendingIntent) {
		/*
		 * Froyo introduced the Passive Location Provider, which receives updates whenever a 3rd party app
		 * receives location updates.
		 */
		locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER
				, PlacesConstants.MAX_TIME
				, PlacesConstants.MAX_DISTANCE
				, pendingIntent);
	}
}
