package com.aircandi.components.location;

import java.util.List;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

/**
 * Legacy implementation of Last Location Finder for all Android platforms
 * down to Android 1.6.
 * 
 * This class let's you find the "best" (most accurate and timely) previously
 * detected location using whatever providers are available.
 * 
 * Where a timely / accurate previous location is not detected it will
 * return the newest location (where one exists) and setup a one-off
 * location update to find the current location.
 */
public class LegacyLastLocationFinder implements ILastLocationFinder {

	protected static String		TAG	= "PreGingerbreadLastLocationFinder";

	protected LocationListener	mLocationListener;
	protected LocationManager	mLocationManager;
	protected Criteria			mCriteria;
	protected Context			mContext;

	/**
	 * Construct a new Legacy Last Location Finder.
	 * 
	 * @param context
	 *            Context
	 */
	public LegacyLastLocationFinder(Context context) {
		mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		mCriteria = new Criteria();
		/*
		 * Coarse accuracy is specified here to get the fastest possible result.
		 * The calling Activity will likely (or have already) request ongoing
		 * updates using the Fine location provider.
		 */
		mCriteria.setAccuracy(Criteria.ACCURACY_COARSE);
		mContext = context;
	}

	/**
	 * Returns the most accurate and timely previously detected location.
	 * Where the last result is beyond the specified maximum distance or
	 * latency a one-off location update is returned via the {@link LocationListener} specified in
	 * {@link setChangedLocationListener}.
	 * 
	 * @param minDistance
	 *            Minimum distance before we require a location update.
	 * @param minTime
	 *            Minimum time required between location updates.
	 * @return The most accurate and / or timely previously detected location.
	 */
	public Location getLastBestLocation(int minDistance, long minTime) {
		Location bestResult = null;
		float bestAccuracy = Float.MAX_VALUE;
		long bestTime = Long.MAX_VALUE;

		/*
		 * Iterate through all the providers on the system, keeping
		 * note of the most accurate result within the acceptable time limit.
		 * If no result is found within maxTime, return the newest Location.
		 */
		List<String> matchingProviders = mLocationManager.getAllProviders();
		for (String provider : matchingProviders) {
			Location location = mLocationManager.getLastKnownLocation(provider);
			if (location != null) {
				float accuracy = location.getAccuracy();
				long time = location.getTime();

				if ((time > minTime && accuracy < bestAccuracy)) {
					bestResult = location;
					bestAccuracy = accuracy;
					bestTime = time;
				}
				else if (time < minTime && bestAccuracy == Float.MAX_VALUE && time > bestTime) {
					bestResult = location;
					bestTime = time;
				}
			}
		}

		/*
		 * If the best result is beyond the allowed time limit, or the accuracy of the
		 * best result is wider than the acceptable maximum distance, request a single update.
		 * This check simply implements the same conditions we set when requesting regular
		 * location updates every [minTime] and [minDistance].
		 * 
		 * Prior to Gingerbread "one-shot" updates weren't available, so we need to implement
		 * this manually.
		 */
		if (mLocationListener != null && (bestTime < minTime || bestAccuracy > minDistance)) {
			String provider = mLocationManager.getBestProvider(mCriteria, true);
			if (provider != null) {
				mLocationManager.requestLocationUpdates(provider
						, 0
						, 0
						, singleUpdateListener
						, mContext.getMainLooper());
			}
		}

		return bestResult;
	}

	/**
	 * This one-off {@link LocationListener} simply listens for a single location
	 * update before unregistering itself. The one-off location update is returned via the {@link LocationListener}
	 * specified in {@link setChangedLocationListener}.
	 */
	protected LocationListener	singleUpdateListener	= new LocationListener() {
															public void onLocationChanged(Location location) {
																Log.d(TAG, "Single location update received: "
																		+ location.getLatitude()
																		+ ","
																		+ location.getLongitude());
																if (mLocationListener != null && location != null) {
																	mLocationListener.onLocationChanged(location);
																}
																mLocationManager.removeUpdates(singleUpdateListener);
															}

															public void onStatusChanged(String provider, int status, Bundle extras) {}

															public void onProviderEnabled(String provider) {}

															public void onProviderDisabled(String provider) {}
														};

	/**
	 * {@inheritDoc}
	 */
	public void setChangedLocationListener(LocationListener listener) {
		mLocationListener = listener;
	}

	/**
	 * {@inheritDoc}
	 */
	public void cancel() {
		mLocationManager.removeUpdates(singleUpdateListener);
	}
}