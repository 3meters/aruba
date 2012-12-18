package com.aircandi.components.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * BroadcastReceiver to be launched by AlarmManager. Simply passes the work over to LocationPollerService, who arranges
 * to make sure the WakeLock stuff is done properly.
 */
public class LocationPoller extends BroadcastReceiver {

	public static final String	EXTRA_ERROR		= "com.aircandi.EXTRA_ERROR";
	public static final String	EXTRA_INTENT	= "com.aircandi.EXTRA_INTENT";
	public static final String	EXTRA_LOCATION	= "com.aircandi.EXTRA_LOCATION";
	public static final String	EXTRA_PROVIDER	= "com.aircandi.EXTRA_PROVIDER";
	public static final String	EXTRA_LASTKNOWN	= "com.aircandi.EXTRA_LASTKNOWN";

	/**
	 * Standard entry point for a BroadcastReceiver. Delegates the event to LocationPollerService for processing.
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		//LocationPollerService.requestLocation(context, intent);
	}
}