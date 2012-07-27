package com.proxibase.aircandi.components;

import android.app.Activity;

import com.google.analytics.tracking.android.EasyTracker;

/*
 * Tracker strategy
 * 
 * - Every activity is a page view when initialized.
 * - Page views and events info is dispatched to google service automatically
 * by EasyTracker.
 * 
 * - Select events are tracked
 * - Insert, update, delete entity
 * - User clicks refresh
 * - Insert, update user
 * - Comment created
 * - User signin, signout
 * 
 * More candidates
 * - Preferences modified
 */

public class Tracker {

	public static void trackEvent(String arg0, String arg1, String arg2, long arg3) {
		/*
		 * Arguments should be free of whitespace.
		 */
		try {
			EasyTracker.getTracker().trackEvent(arg0, arg1, arg2, arg3);
		}
		catch (Exception exception) {}
	}

	public static void trackView(String viewName) {
		/*
		 * Arguments should be free of whitespace.
		 */
		try {
			EasyTracker.getTracker().trackView(viewName);
		}
		catch (Exception exception) {}
	}

	public static void dispatch() {
		try {
			EasyTracker.getInstance().dispatch();
		}
		catch (Exception exception) {}
	}

	public static void stopSession() {
		try {
			EasyTracker.getTracker().setStartSession(false);
		}
		catch (Exception exception) {}
	}

	public static void startNewSession() {
		try {
			EasyTracker.getTracker().setStartSession(true);
		}
		catch (Exception exception) {}
	}

	public static void activityStart(Activity activity) {
		try {
			EasyTracker.getInstance().activityStart(activity);
		}
		catch (Exception exception) {}
	}

	public static void activityStop(Activity activity) {
		try {
			EasyTracker.getInstance().activityStop(activity);
		}
		catch (Exception exception) {}
	}
}
