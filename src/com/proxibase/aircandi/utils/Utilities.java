package com.proxibase.aircandi.utils;

import android.util.Log;

import com.proxibase.aircandi.core.CandiConstants;

public class Utilities {

	public static void Log(String tag, String task, String message) {

		if (CandiConstants.MODE_DEBUG)
			Log.d(tag, task + ": " + message);
	}
}