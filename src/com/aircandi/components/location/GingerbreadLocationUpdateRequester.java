package com.aircandi.components.location;

import com.aircandi.components.Logger;

import android.app.PendingIntent;
import android.location.Criteria;
import android.location.LocationManager;

/**
 * Provides support for initiating active and passive location updates
 * optimized for the Gingerbread release. Includes use of the Passive Location Provider.
 * 
 * Uses broadcast Intents to notify the app of location changes.
 */
public class GingerbreadLocationUpdateRequester extends FroyoLocationUpdateRequester {

	public GingerbreadLocationUpdateRequester(LocationManager locationManager) {
		super(locationManager);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void requestLocationUpdates(long minTime, long minDistance, Criteria criteria, PendingIntent pendingIntent) {
		/*
		 * Gingerbread supports a location update request that accepts criteria directly.
		 * Note that we aren't monitoring this provider to check if it becomes disabled - this is handled by the calling
		 * Activity.
		 */
		Logger.d(this, "Active updates enabled");
		locationManager.requestLocationUpdates(minTime
				, minDistance
				, criteria
				, pendingIntent);
	}
}
