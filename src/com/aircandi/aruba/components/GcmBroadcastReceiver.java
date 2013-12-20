package com.aircandi.aruba.components;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.aircandi.R;
import com.aircandi.utilities.Strings;

/**
 * This {@code WakefulBroadcastReceiver} takes care of creating and managing a
 * partial wake lock for your app. It passes off the work of processing the GCM
 * message to an {@code IntentService}, while ensuring that the device does not
 * go back to sleep in the transition. The {@code IntentService} calls
 * {@code GcmBroadcastReceiver.completeWakefulIntent()} when it is ready to
 * release the wake lock.
 */
public class GcmBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String className = getGCMIntentServiceClassName(context);
		GcmIntentService.runIntentInService(context, intent, className);
		setResultCode(Activity.RESULT_OK);
	}

	/**
	 * Gets the class name of the intent service that will handle GCM messages.
	 */
	protected String getGCMIntentServiceClassName(Context context) {
		return Strings.getString(R.string.class_gcm_intent_service);
	}
}
