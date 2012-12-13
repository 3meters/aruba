package com.aircandi.location;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;

import com.aircandi.Aircandi;
import com.aircandi.components.LocationManager;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.core.PlacesConstants;
import com.aircandi.receivers.ConnectivityChangedReceiver;
import com.aircandi.receivers.LocationChangedReceiver;
import com.aircandi.receivers.PassiveLocationChangedReceiver;
import com.aircandi.service.objects.Observation;

/**
 * Service that requests a list of nearby locations from the underlying web service.
 * TODO Update the URL and XML parsing to correspond with your underlying web service.
 */
public class PlacesUpdateService extends IntentService {

	protected static String			TAG				= "PlacesUpdateService";

	protected SharedPreferences		mPrefs			= Aircandi.settings;
	protected Editor				mPrefsEditor	= Aircandi.settingsEditor;
	protected ConnectivityManager	mConnectivityManager;
	protected boolean				mLowBattery		= false;
	protected boolean				mMobileData		= false;
	protected int					mPrefetchCount	= 0;

	public PlacesUpdateService() {
		super(TAG);
		setIntentRedeliveryMode(false);
	}

	/**
	 * Set the Intent Redelivery mode to true to ensure the Service starts "Sticky"
	 * Defaults to "true" on legacy devices.
	 */
	protected void setIntentRedeliveryMode(boolean enable) {
		setIntentRedelivery(true);
	}

	/**
	 * Returns battery status. True if less than 10% remaining.
	 * 
	 * @param battery
	 *            Battery Intent
	 * @return Battery is low
	 */
	protected boolean getIsLowBattery(Intent battery) {
		float pctLevel = (float) battery.getIntExtra(BatteryManager.EXTRA_LEVEL, 1) /
				battery.getIntExtra(BatteryManager.EXTRA_SCALE, 1);
		return pctLevel < 0.15;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	}

	/**
	 * {@inheritDoc} Checks the battery and connectivity state before removing stale venues
	 * and initiating a server poll for new venues around the specified
	 * location within the given radius.
	 */
	@Override
	protected void onHandleIntent(Intent intent) {
		/*
		 * Check if we're running in the foreground, if not, check if we have permission to do background updates.
		 */
		@SuppressWarnings("deprecation")
		boolean backgroundAllowed = mConnectivityManager.getBackgroundDataSetting();
		boolean inBackground = mPrefs.getBoolean(PlacesConstants.EXTRA_KEY_IN_BACKGROUND, true);

		if (!backgroundAllowed && inBackground) return;

		/* Extract the location and radius around which to conduct our search. */
		Location location = new Location(PlacesConstants.CONSTRUCTED_LOCATION_PROVIDER);
		int radius = PlacesConstants.DEFAULT_RADIUS;

		Bundle extras = intent.getExtras();
		if (intent.hasExtra(PlacesConstants.EXTRA_KEY_LOCATION)) {
			location = (Location) (extras.get(PlacesConstants.EXTRA_KEY_LOCATION));
			radius = extras.getInt(PlacesConstants.EXTRA_KEY_RADIUS, PlacesConstants.DEFAULT_RADIUS);
		}

		/* Check if we're in a low battery situation. */
		IntentFilter batIntentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent battery = registerReceiver(null, batIntentFilter);
		mLowBattery = getIsLowBattery(battery);

		/* Check if we're connected to a data network, and if so - if it's a mobile network. */
		NetworkInfo activeNetwork = mConnectivityManager.getActiveNetworkInfo();
		boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
		mMobileData = activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE;

		/*
		 * If we're not connected, enable the connectivity receiver and disable the location receiver.
		 * There's no point trying to poll the server for updates if we're not connected, and the
		 * connectivity receiver will turn the location-based updates back on once we have a connection.
		 */
		if (!isConnected) {
			PackageManager pm = getPackageManager();

			ComponentName connectivityReceiver = new ComponentName(this, ConnectivityChangedReceiver.class);
			ComponentName locationReceiver = new ComponentName(this, LocationChangedReceiver.class);
			ComponentName passiveLocationReceiver = new ComponentName(this, PassiveLocationChangedReceiver.class);

			pm.setComponentEnabledSetting(connectivityReceiver,
					PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
					PackageManager.DONT_KILL_APP);

			pm.setComponentEnabledSetting(locationReceiver,
					PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);

			pm.setComponentEnabledSetting(passiveLocationReceiver,
					PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);
		}
		else {
			/*
			 * If we are connected check to see if this is a forced update (typically triggered
			 * when the location has changed).
			 */
			boolean doUpdate = intent.getBooleanExtra(PlacesConstants.EXTRA_KEY_FORCEREFRESH, false);

			/*
			 * If it's not a forced update (for example from the Activity being restarted) then
			 * check to see if we've moved far enough, or there's been a long enough delay since
			 * the last update and if so, enforce a new update.
			 */
			if (!doUpdate) {
				/* Retrieve the last update time and place. */
				long lastTime = mPrefs.getLong(PlacesConstants.SP_KEY_LAST_LIST_UPDATE_TIME, Long.MIN_VALUE);
				long lastLat = mPrefs.getLong(PlacesConstants.SP_KEY_LAST_LIST_UPDATE_LAT, Long.MIN_VALUE);
				long lastLng = mPrefs.getLong(PlacesConstants.SP_KEY_LAST_LIST_UPDATE_LNG, Long.MIN_VALUE);

				Location lastLocation = new Location(PlacesConstants.CONSTRUCTED_LOCATION_PROVIDER);
				lastLocation.setLatitude(lastLat);
				lastLocation.setLongitude(lastLng);

				/* If update time and distance bounds have been passed, do an update. */
				if ((lastTime < System.currentTimeMillis() - PlacesConstants.MAX_TIME)
						|| (lastLocation.distanceTo(location) > PlacesConstants.MAX_DISTANCE))
					doUpdate = true;
			}

			if (doUpdate) {
				/* Hit the server for new venues for the current location. */
				refreshPlaces(location, radius);
			}
			else {
				Log.d(TAG, "Place list is fresh: not refreshing");
			}
		}
		Log.d(TAG, "Place list download service complete");
	}

	/**
	 * Polls the underlying service to return a list of places within the specified
	 * radius of the specified Location.
	 * 
	 * @param location
	 *            Location
	 * @param radius
	 *            Radius
	 */
	protected void refreshPlaces(Location location, int radius) {
		// Log to see if we'll be prefetching the details page of each new place.
		if (mMobileData) {
			Log.d(TAG, "Not prefetching due to being on mobile");
		}
		else if (mLowBattery) {
			Log.d(TAG, "Not prefetching due to low battery");
		}

		/* Call aircandi for new places */
		final Observation observation = LocationManager.getInstance().getObservation();
		if (observation != null) {
			ProxiExplorer.getInstance().getPlacesNearLocation(observation);
		}

		// If we haven't yet reached our prefetching limit, and we're either
		// on WiFi or don't have a WiFi-only prefetching restriction, and we
		// either don't have low batter or don't have a low battery prefetching 
		// restriction, then prefetch the details for this newly added place.
		/* Refresh the prefetch count for each new location. */
		mPrefetchCount = 0;
		if ((mPrefetchCount < PlacesConstants.PREFETCH_LIMIT) &&
				(!PlacesConstants.PREFETCH_ON_WIFI_ONLY || !mMobileData) &&
				(!PlacesConstants.DISABLE_PREFETCH_ON_LOW_BATTERY || !mLowBattery)) {
			mPrefetchCount++;

			// Start the PlaceDetailsUpdateService to prefetch the details for this place.
			// As we're prefetching, don't force the refresh if we already have data.
			//			Intent updateServiceIntent = new Intent(this, PlaceDetailsUpdateService.class);
			//			updateServiceIntent.putExtra(PlacesConstants.EXTRA_KEY_REFERENCE, "[foursquare]");
			//			updateServiceIntent.putExtra(PlacesConstants.EXTRA_KEY_ID, "[foursquare_id]");
			//			updateServiceIntent.putExtra(PlacesConstants.EXTRA_KEY_FORCEREFRESH, false);
			//			startService(updateServiceIntent);
		}

	}
}