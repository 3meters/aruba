package com.aircandi.location;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;

import com.aircandi.Aircandi;
import com.aircandi.core.PlacesConstants;

/**
 * Service that queries the underlying web service to retrieve the full
 * details for the specified place / venue.
 * This Service is called by the {@link PlacesUpdateService} to prefetch details
 * for the nearby venues, or by the {@link PlacesActivity} and {@link PlaceDetailsFragment} to retrieve the details for
 * the selected place.
 * TODO Replace the URL and XML parsing to match the details available from your service.
 */
public class PlaceDetailsUpdateService extends IntentService {

	protected static String			TAG		= "PlaceDetailsIntentService";

	protected ContentResolver		mContentResolver;
	protected SharedPreferences		prefs	= Aircandi.settings;
	protected ConnectivityManager	cm;

	public PlaceDetailsUpdateService() {
		super(TAG);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mContentResolver = getContentResolver();
		cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		prefs = getSharedPreferences(PlacesConstants.SHARED_PREFERENCE_FILE, Context.MODE_PRIVATE);
	}

	/**
	 * {@inheritDoc} Check to see if we already have these details, and if so, whether or not we should update them.
	 * Initiates an update where appropriate.
	 */
	@Override
	protected void onHandleIntent(Intent intent) {
		// Check if we're running in the foreground, if not, check if
		// we have permission to do background updates.
		@SuppressWarnings("deprecation")
		boolean backgroundAllowed = cm.getBackgroundDataSetting();
		boolean inBackground = Aircandi.settings.getBoolean(PlacesConstants.EXTRA_KEY_IN_BACKGROUND, true);

		if (!backgroundAllowed && inBackground) return;

		/*
		 * Do the work.
		 */
	}
}