package com.aircandi.components;

import android.app.Activity;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.service.objects.User;
import com.aircandi.utilities.Type;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.MapBuilder;

/*
 * Tracker strategy
 * 
 * - Every activity is a page view when initialized.
 * - Page views and events info is dispatched to google service automatically
 * by EasyTracker.
 * 
 * - Select events are tracked
 * - Insert, update, delete entity
 * - user clicks refresh
 * - Insert, update user
 * - Comment created
 * - user signin, signout
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
			if (Constants.TRACKING_ENABLED && user != null && Type.isFalse(user.developer)) {
				EasyTracker.getInstance(Aircandi.applicationContext).send(MapBuilder.createEvent(category, action, target, value).build());
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
			if (Constants.TRACKING_ENABLED && user != null && Type.isFalse(user.developer)) {
				EasyTracker.getInstance(Aircandi.applicationContext).send(MapBuilder.createTiming(category, timing, name, label).build());
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void stopSession(User user) {
		try {
			if (Constants.TRACKING_ENABLED && user != null && Type.isFalse(user.developer)) {
				EasyTracker.getInstance(Aircandi.applicationContext).set(Fields.SESSION_CONTROL, "end");
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void startNewSession(User user) {
		try {
			if (Constants.TRACKING_ENABLED && user != null && Type.isFalse(user.developer)) {
				EasyTracker.getInstance(Aircandi.applicationContext).set(Fields.SESSION_CONTROL, "start");
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void activityStart(Activity activity, User user) {
		try {
			if (Constants.TRACKING_ENABLED && user != null && Type.isFalse(user.developer)) {
				EasyTracker.getInstance(Aircandi.applicationContext).activityStart(activity);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void activityStop(Activity activity, User user) {
		try {
			if (Constants.TRACKING_ENABLED && user != null && Type.isFalse(user.developer)) {
				EasyTracker.getInstance(Aircandi.applicationContext).activityStop(activity);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static enum Action {
		ENTITY_KICK,
		ENTITY_DELETE

	}
}
