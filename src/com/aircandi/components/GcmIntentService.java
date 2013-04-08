package com.aircandi.components;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

public class GcmIntentService extends IntentService {

	public GcmIntentService(String name) {
		super(name);
	}

	private static PowerManager.WakeLock	sWakeLock;
	private static final Object				LOCK	= GcmIntentService.class;

	static void runIntentInService(Context context, Intent intent) {
		synchronized (LOCK) {
			if (sWakeLock == null) {
				PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
				sWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "my_wakelock");
			}
		}
		sWakeLock.acquire();
		intent.setClassName(context, GcmIntentService.class.getName());
		context.startService(intent);
	}

	@Override
	public final void onHandleIntent(Intent intent) {
		try {
			String action = intent.getAction();
			if (action.equals("com.google.android.c2dm.intent.REGISTRATION")) {
				//handleRegistration(intent);
			}
			else if (action.equals("com.google.android.c2dm.intent.RECEIVE")) {
				//handleMessage(intent);
			}
		}
		finally {
			synchronized (LOCK) {
				sWakeLock.release();
			}
		}
	}
}
