package com.aircandi.components;

import android.content.Context;

import com.google.android.gcm.GCMBroadcastReceiver;

public class GCMAircandiBroadcastReceiver extends GCMBroadcastReceiver {

	@Override
	protected String getGCMIntentServiceClassName(Context context) {
		return "com.aircandi.components.GCMIntentService";
	}

}
