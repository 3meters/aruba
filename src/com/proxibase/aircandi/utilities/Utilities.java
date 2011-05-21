package com.proxibase.aircandi.utilities;

import com.proxibase.aircandi.controller.Aircandi;

import android.util.Log;

public class Utilities {

	public static void Log(String tag, String task, String message) {

		if (Aircandi.MODE_DEBUG)
			Log.d(tag, task + ": " + message);
	}
}