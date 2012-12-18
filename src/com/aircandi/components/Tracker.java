package com.aircandi.components;

import android.app.Activity;

import com.aircandi.CandiConstants;
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
			if (CandiConstants.TRACKING_ENABLED) {
				EasyTracker.getTracker().trackEvent(arg0, arg1, arg2, arg3);
			}
		}
		catch (Exception exception) {}
	}

	public static void trackView(String viewName) {
		/*
		 * Arguments should be free of whitespace.
		 */
		try {
			if (CandiConstants.TRACKING_ENABLED) {
				EasyTracker.getTracker().trackView(viewName);
			}
		}
		catch (Exception exception) {}
	}

	public static void dispatch() {
		try {
			if (CandiConstants.TRACKING_ENABLED) {
				EasyTracker.getInstance().dispatch();
			}
		}
		catch (Exception exception) {}
	}

	public static void stopSession() {
		try {
			if (CandiConstants.TRACKING_ENABLED) {
				EasyTracker.getTracker().setStartSession(false);
			}
		}
		catch (Exception exception) {}
	}

	public static void startNewSession() {
		try {
			if (CandiConstants.TRACKING_ENABLED) {
				EasyTracker.getTracker().setStartSession(true);
			}
		}
		catch (Exception exception) {}
	}

	public static void activityStart(Activity activity) {
		try {
			if (CandiConstants.TRACKING_ENABLED) {
				EasyTracker.getInstance().activityStart(activity);
			}
		}
		catch (Exception exception) {}
	}

	public static void activityStop(Activity activity) {
		try {
			if (CandiConstants.TRACKING_ENABLED) {
				EasyTracker.getInstance().activityStop(activity);
			}
		}
		catch (Exception exception) {}
	}
}
