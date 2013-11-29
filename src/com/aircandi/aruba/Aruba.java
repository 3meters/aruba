package com.aircandi.aruba;

import com.aircandi.Aircandi;

public class Aruba extends Aircandi {

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	protected void initializeInstance() {
		/*
		 * Handle additional initialization needed by the aircandi app.
		 */
		super.initializeInstance();
	}

	@Override
	public void snapshotPreferences() {
		/*
		 * Handle additional preferences needed by the aircandi app.
		 */
		super.snapshotPreferences();
	}
}
