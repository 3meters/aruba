package com.aircandi.components.location;

import com.aircandi.components.Logger;

import android.app.PendingIntent;
import android.location.Criteria;
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
	public void requestLocationUpdates(long minTime, long minDistance, Criteria criteria, PendingIntent pendingIntent) {
		// Prior to Gingerbread we needed to find the best provider manually.
		// Note that we aren't monitoring this provider to check if it becomes disabled - this is handled by the calling Activity.
		String provider = locationManager.getBestProvider(criteria, true);
		if (provider != null)
			Logger.d(this, "Active updates enabled");
			locationManager.requestLocationUpdates(provider
					, minTime
					, minDistance
					, pendingIntent);
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
		Logger.d(this, "Passive updates enabled");
		locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER
				, minTime
				, minDistance
				, pendingIntent);
	}
}
