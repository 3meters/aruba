package com.aircandi.aruba;

import com.aircandi.Aircandi;
import com.aircandi.components.DispatchManager;
import com.aircandi.components.StringManager;
import com.aircandi.utilities.Reporting;
import com.google.tagmanager.TagManager.RefreshMode;

public class Aruba extends Aircandi {

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	protected void initializeInstance() {
		/* Must have this so activity rerouting works. */
		Aircandi.applicationContext = getApplicationContext();
		
		if (!DEBUG) {
			Reporting.startCrashReporting(this);
		}
		
		super.initializeInstance();

		/* Inject configuration */
		openContainer(StringManager.getString(R.string.id_container), RefreshMode.STANDARD);
		
		/* Inject dispatch manager */
		Aircandi.dispatch = new DispatchManager();
	}
}
