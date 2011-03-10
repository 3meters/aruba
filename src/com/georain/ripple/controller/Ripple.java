package com.georain.ripple.controller;

import java.util.Calendar;

import android.app.Application;
import android.location.Location;
import android.util.Log;

import com.georain.ripple.model.FriendsFb;
import com.georain.ripple.model.Post;
import com.georain.ripple.model.UserFb;
import com.threemeters.sdk.android.core.Entity;

public class Ripple extends Application {

	public UserFb				currentUserX;
	public FriendsFb			currentFriendsX;
	public Entity				currentEntityX;
	public Post					currentPostX;
	public Location				currentLocation;
	public static final int		TWO_MINUTES				= 1000 * 60 * 2;
	public static final int		FIVE_MINUTES			= 1000 * 60 * 5;
	public static final String	URL_RIPPLESERVICE_ODATA	= "http://dev.georain.com/ripodata.svc/";
	public static final String	URL_RIPPLESERVICE		= "http://dev.georain.com/riplogic.asmx/";
	public static final String	URL_RIPPLEMEDIA			= "https://s3.amazonaws.com/";
	public static final String	URL_RIPPLEZANIA			= "http://devzania.georain.com/";
	public static final String	URL_RIPPLEBLOGSERVICE	= "http://devblog.georain.com/";
	public static final boolean	MODE_DEBUG				= true;


	public Ripple() {

	}


	@Override
	public void onCreate() {

		super.onCreate();
	}

	/**
	 * Callback interface for Ripple async requests.
	 */
	public static interface Listener {

		/**
		 * Called when an AsyncTask completes with the given response. Executed by a background thread: do not update
		 * the UI in this method.
		 */
		public void onComplete(String response);


		public void onException(Exception e);
	}

	public abstract class BaseListener implements Listener {

		public void onException(Exception e) {

			Log.e("Ripple", e.getMessage());
			e.printStackTrace();
		}
	}


	/**
	 * Determines whether one Location reading is better than the current Location fix *
	 * 
	 * @param location The new Location that you want to evaluate * @param currentBestLocation The current Location fix,
	 *            to which you want to compare the new one
	 */
	public static boolean isBetterLocation(Location location, Location currentBestLocation) {

		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
			// If the new location is more than two minutes older, it must be worse
		}
		else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(), currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and accuracy
		if (isMoreAccurate) {
			return true;
		}
		else if (isNewer && !isLessAccurate) {
			return true;
		}
		else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
			return true;
		}
		return false;
	}


	public static long locationFixAge(Location location) {

		Calendar cal = Calendar.getInstance();
		long timeDelta = cal.getTimeInMillis() - location.getTime();
		return timeDelta;
	}


	/** Checks whether two providers are the same */
	private static boolean isSameProvider(String provider1, String provider2) {

		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

}
