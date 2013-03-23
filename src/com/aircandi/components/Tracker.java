package com.aircandi.components;

import android.app.Activity;

import com.aircandi.CandiConstants;
import com.aircandi.service.objects.User;
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

@SuppressWarnings("ucd")
public class Tracker {

	public static void sendEvent(String category, String action, String target, long value, User user) {
		/*
		 * Arguments should be free of whitespace.
		 */
		try {
			if (CandiConstants.TRACKING_ENABLED && user != null && (user.isDeveloper == null || !user.isDeveloper)) {
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
			if (CandiConstants.TRACKING_ENABLED && user != null && (user.isDeveloper == null || !user.isDeveloper)) {
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
			if (CandiConstants.TRACKING_ENABLED && user != null && (user.isDeveloper == null || !user.isDeveloper)) {
				EasyTracker.getTracker().sendTiming(category, timing, name, label);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void dispatch(User user) {
		try {
			if (CandiConstants.TRACKING_ENABLED && user != null && (user.isDeveloper == null || !user.isDeveloper)) {
				EasyTracker.getInstance().dispatch();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void stopSession(User user) {
		try {
			if (CandiConstants.TRACKING_ENABLED && user != null && (user.isDeveloper == null || !user.isDeveloper)) {
				EasyTracker.getTracker().setStartSession(false);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void startNewSession(User user) {
		try {
			if (CandiConstants.TRACKING_ENABLED && user != null && (user.isDeveloper == null || !user.isDeveloper)) {
				EasyTracker.getTracker().setStartSession(true);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void activityStart(Activity activity, User user) {
		try {
			if (CandiConstants.TRACKING_ENABLED && user != null && (user.isDeveloper == null || !user.isDeveloper)) {
				EasyTracker.getInstance().activityStart(activity);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void activityStop(Activity activity, User user) {
		try {
			if (CandiConstants.TRACKING_ENABLED && user != null && (user.isDeveloper == null || !user.isDeveloper)) {
				EasyTracker.getInstance().activityStop(activity);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
