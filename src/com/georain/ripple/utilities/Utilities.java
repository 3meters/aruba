package com.georain.ripple.utilities;

import com.georain.ripple.controller.Ripple;

import android.util.Log;

public class Utilities {

	public static void Log(String tag, String message) {

		if (Ripple.MODE_DEBUG)
			Utilities.Log(tag, message);
	}
}