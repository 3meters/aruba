package com.aircandi.components.location;

import com.aircandi.components.Logger;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.location.Criteria;
import android.location.LocationManager;

/**
 * Provides support for initiating active and passive location updates
 * for all Android platforms from Android 1.6.
 * 
 * Uses broadcast Intents to notify the app of location changes.
 */
public class LegacyLocationUpdateRequester extends LocationUpdateRequester {

	protected AlarmManager	alarmManager;

	protected LegacyLocationUpdateRequester(LocationManager locationManager, AlarmManager alarmManager) {
		super(locationManager);
		this.alarmManager = alarmManager;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void requestLocationUpdates(long minTime, long minDistance, Criteria criteria, PendingIntent pendingIntent) {
		/*
		 * Prior to Gingerbread we needed to find the best provider manually.
		 * Note that we aren't monitoring this provider to check if it becomes disabled - this is handled by the calling
		 * Activity.
		 */
		Logger.d(this, "Active updates enabled");
		String provider = locationManager.getBestProvider(criteria, true);
		if (provider != null) {
			locationManager.requestLocationUpdates(provider, minTime, minDistance, pendingIntent);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void requestPassiveLocationUpdates(long minTime, long minDistance, PendingIntent pendingIntent) {
		/*
		 * Pre-Froyo there was no Passive Location Provider, so instead we will set an inexact repeating, non-waking
		 * alarm that will trigger once the minimum time between passive updates has expired. This is potentially more
		 * expensive than simple passive alarms, however the Receiver will ensure we've transitioned beyond the minimum
		 * time and distance before initiating a background nearby loction update.
		 */
		Logger.d(this, "Passive updates enabled: polling");
		alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME
				, System.currentTimeMillis() + minTime
				, minTime
				, pendingIntent);
	}
}
