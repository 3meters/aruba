package com.aircandi.components;

import android.app.Activity;

import com.aircandi.Constants;
import com.aircandi.service.objects.User;
import com.google.analytics.tracking.android.EasyTracker;

/*
 * Tracker strategy
 * 
 * - Every activity is a page view when initialized.
 * - Page views and events info is dispatched to google SERVICE automatically
 * by EasyTracker.
 * 
 * - Select events are tracked
 * - INSERT, update, delete entity
 * - USER clicks refresh
 * - INSERT, update user
 * - COMMENT created
 * - USER signin, signout
 * 
 * More candidates
 * - Preferences modified
 */

@SuppressWarnings("ucd")
public class Tracker {

	public static void sendEvent(String category, String action, String target, long value, User user) {
		/*
		 * Arguments should be free of whitespace.
		 */
		try {
			if (Constants.TRACKING_ENABLED && user != null && (user.developer == null || !user.developer)) {
				EasyTracker.getTracker().sendEvent(category, action, target, value);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void sendView(String viewName, User user) {
		/*
		 * Arguments should be free of whitespace.
		 */
		try {
			if (Constants.TRACKING_ENABLED && user != null && (user.developer == null || !user.developer)) {
				EasyTracker.getTracker().sendView(viewName);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void sendTiming(String category, Long timing, String name, String label, User user) {
		/*
		 * Arguments should be free of whitespace.
		 */
		try {
			if (Constants.TRACKING_ENABLED && user != null && (user.developer == null || !user.developer)) {
				EasyTracker.getTracker().sendTiming(category, timing, name, label);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void dispatch(User user) {
		try {
			if (Constants.TRACKING_ENABLED && user != null && (user.developer == null || !user.developer)) {
				EasyTracker.getInstance().dispatch();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void stopSession(User user) {
		try {
			if (Constants.TRACKING_ENABLED && user != null && (user.developer == null || !user.developer)) {
				EasyTracker.getTracker().setStartSession(false);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void startNewSession(User user) {
		try {
			if (Constants.TRACKING_ENABLED && user != null && (user.developer == null || !user.developer)) {
				EasyTracker.getTracker().setStartSession(true);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void activityStart(Activity activity, User user) {
		try {
			if (Constants.TRACKING_ENABLED && user != null && (user.developer == null || !user.developer)) {
				EasyTracker.getInstance().activityStart(activity);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void activityStop(Activity activity, User user) {
		try {
			if (Constants.TRACKING_ENABLED && user != null && (user.developer == null || !user.developer)) {
				EasyTracker.getInstance().activityStop(activity);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
