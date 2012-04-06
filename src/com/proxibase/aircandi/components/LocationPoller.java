package com.proxibase.aircandi.components;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * BroadcastReceiver to be launched by AlarmManager. Simply passes the work over to LocationPollerService, who arranges
 * to make sure the WakeLock stuff is done properly.
 */
public class LocationPoller extends BroadcastReceiver {

	public static final String	EXTRA_ERROR		= "com.proxibase.aircandi.EXTRA_ERROR";
	public static final String	EXTRA_INTENT	= "com.proxibase.aircandi.EXTRA_INTENT";
	public static final String	EXTRA_LOCATION	= "com.proxibase.aircandi.EXTRA_LOCATION";
	public static final String	EXTRA_PROVIDER	= "com.proxibase.aircandi.EXTRA_PROVIDER";
	public static final String	EXTRA_LASTKNOWN	= "com.proxibase.aircandi.EXTRA_LASTKNOWN";

	/**
	 * Standard entry point for a BroadcastReceiver. Delegates the event to LocationPollerService for processing.
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		//LocationPollerService.requestLocation(context, intent);
	}
}