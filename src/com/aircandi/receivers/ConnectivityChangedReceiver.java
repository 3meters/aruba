package com.aircandi.receivers;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * This Receiver class is designed to listen for changes in connectivity.
 * 
 * When we lose connectivity the relevant Service classes will automatically
 * disable passive Location updates and queue pending checkins.
 * 
 * This class will restart the checkin service to retry pending checkins
 * and re-enables passive location updates.
 */
public class ConnectivityChangedReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {

		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

		// Check if we are connected to an active data network.
		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

		if (isConnected) {
			PackageManager pm = context.getPackageManager();

			ComponentName connectivityReceiver = new ComponentName(context, ConnectivityChangedReceiver.class);
			ComponentName locationReceiver = new ComponentName(context, LocationChangedReceiver.class);
			ComponentName passiveLocationReceiver = new ComponentName(context, PassiveLocationChangedReceiver.class);

			/*
			 * The default state for this Receiver is disabled. it is only
			 * enabled when a Service disables updates pending connectivity.
			 */
			pm.setComponentEnabledSetting(connectivityReceiver,
					PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
					PackageManager.DONT_KILL_APP);
			/*
			 * The default state for the Location Receiver is enabled. it is only
			 * disabled when a Service disables updates pending connectivity.
			 */
			pm.setComponentEnabledSetting(locationReceiver,
					PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
					PackageManager.DONT_KILL_APP);
			/*
			 * The default state for the Location Receiver is enabled. it is only
			 * disabled when a Service disables updates pending connectivity.
			 */
			pm.setComponentEnabledSetting(passiveLocationReceiver,
					PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
					PackageManager.DONT_KILL_APP);
		}
	}
}