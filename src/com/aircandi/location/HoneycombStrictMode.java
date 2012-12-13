package com.aircandi.location;

import android.os.StrictMode;

/**
 * Implementation that supports the Strict Mode functionality
 * available Honeycomb.
 */
public class HoneycombStrictMode implements IStrictMode {
	protected static String	TAG	= "HoneycombStrictMode";

	/**
	 * Enable {@link StrictMode} TODO Set your preferred Strict Mode features.
	 */
	public void enableStrictMode() {
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
				.detectDiskReads()
				.detectDiskWrites()
				.detectNetwork()
				.penaltyLog()
				.penaltyFlashScreen()
				.build());
	}
}
