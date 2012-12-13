package com.aircandi.location;

import com.aircandi.core.PlacesConstants;

import android.content.Context;
import android.location.LocationManager;

/**
 * Factory class to create the correct instances
 * of a variety of classes with platform specific
 * implementations.
 * 
 */
public class PlatformSpecificImplementationFactory {

	/**
	 * Create a new LastLocationFinder instance
	 * 
	 * @param context
	 *            Context
	 * @return LastLocationFinder
	 */
	public static ILastLocationFinder getLastLocationFinder(Context context) {
		return PlacesConstants.SUPPORTS_GINGERBREAD ? new GingerbreadLastLocationFinder(context) : new LegacyLastLocationFinder(context);
	}

	/**
	 * Create a new StrictMode instance.
	 * 
	 * @return StrictMode
	 */
	public static IStrictMode getStrictMode() {
		if (PlacesConstants.SUPPORTS_HONEYCOMB)
			return new HoneycombStrictMode();
		else if (PlacesConstants.SUPPORTS_GINGERBREAD)
			return new LegacyStrictMode();
		else
			return null;
	}

	/**
	 * Create a new LocationUpdateRequester
	 * 
	 * @param locationManager
	 *            Location Manager
	 * @return LocationUpdateRequester
	 */
	public static LocationUpdateRequester getLocationUpdateRequester(LocationManager locationManager) {
		return PlacesConstants.SUPPORTS_GINGERBREAD ? new GingerbreadLocationUpdateRequester(locationManager) : new FroyoLocationUpdateRequester(
				locationManager);
	}
}
