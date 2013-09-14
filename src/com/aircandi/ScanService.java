package com.aircandi;

import android.app.IntentService;
import android.content.Intent;

import com.aircandi.components.Logger;
import com.aircandi.components.ProximityManager;
import com.aircandi.components.ProximityManager.ScanReason;

public class ScanService extends IntentService {

	public ScanService() {
		super("ScanService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Logger.v(ScanService.this, "Wifi scan service: requesting wifi scan"); //$NON-NLS-1$
		ProximityManager.getInstance().scanForWifi(ScanReason.MONITORING);
	}
}
