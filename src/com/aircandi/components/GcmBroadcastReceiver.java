package com.aircandi.components;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class GcmBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		GcmIntentService.runIntentInService(context, intent);
		setResult(Activity.RESULT_OK, null, null);
	}

}
