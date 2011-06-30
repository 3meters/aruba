package com.proxibase.aircandi.utils;

import com.proxibase.aircandi.controllers.Aircandi;

import android.util.Log;

public class Utilities {

	public static void Log(String tag, String task, String message) {

		if (Aircandi.MODE_DEBUG)
			Log.d(tag, task + ": " + message);
	}
}