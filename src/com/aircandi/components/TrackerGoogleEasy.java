package com.aircandi.components;

import android.app.Activity;
import android.support.v4.app.Fragment;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.service.objects.User;
import com.aircandi.utilities.Type;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.StandardExceptionParser;

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
public class TrackerGoogleEasy extends TrackerBase {

	@Override
	public void activityStart(Activity activity) {
		try {
			User user = Aircandi.getInstance().getCurrentUser();
			if (Constants.TRACKING_ENABLED && user != null && Type.isFalse(user.developer)) {
				EasyTracker.getInstance(Aircandi.applicationContext).activityStart(activity);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void activityStop(Activity activity) {
		try {
			User user = Aircandi.getInstance().getCurrentUser();
			if (Constants.TRACKING_ENABLED && user != null && Type.isFalse(user.developer)) {
				EasyTracker.getInstance(Aircandi.applicationContext).activityStop(activity);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void sendEvent(String category, String action, String target, long value) {
		/*
		 * Arguments should be free of whitespace.
		 */
		try {
			User user = Aircandi.getInstance().getCurrentUser();
			if (Constants.TRACKING_ENABLED && user != null && Type.isFalse(user.developer)) {
				EasyTracker.getInstance(Aircandi.applicationContext).send(MapBuilder.createEvent(category, action, target, value).build());
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void sendTiming(String category, Long timing, String name, String label) {
		/*
		 * Arguments should be free of whitespace.
		 */
		try {
			User user = Aircandi.getInstance().getCurrentUser();
			if (Constants.TRACKING_ENABLED && user != null && Type.isFalse(user.developer)) {
				EasyTracker.getInstance(Aircandi.applicationContext).send(MapBuilder.createTiming(category, timing, name, label).build());
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void sendException(Exception exception) {
		/*
		 * Arguments should be free of whitespace.
		 */
		try {
			User user = Aircandi.getInstance().getCurrentUser();
			if (Constants.TRACKING_ENABLED && user != null && Type.isFalse(user.developer)) {
				EasyTracker.getInstance(Aircandi.applicationContext).send(
						MapBuilder.createException(new StandardExceptionParser(Aircandi.applicationContext, null)
								.getDescription(Thread.currentThread().getName(), exception), false)
								.build());
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void fragmentStart(Fragment fragment) {
		try {
			User user = Aircandi.getInstance().getCurrentUser();
			if (Constants.TRACKING_ENABLED && user != null && Type.isFalse(user.developer)) {
				/*
				 * Screen name as set will be included in all subsequent sends.
				 */
				EasyTracker.getInstance(Aircandi.applicationContext).set(Fields.SCREEN_NAME, fragment.getClass().getSimpleName());
				EasyTracker.getInstance(Aircandi.applicationContext).send(MapBuilder.createAppView().build());
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void applicationStart() {}

	@SuppressWarnings("unused")
	private void startNewSession() {
		try {
			User user = Aircandi.getInstance().getCurrentUser();
			if (Constants.TRACKING_ENABLED && user != null && Type.isFalse(user.developer)) {
				EasyTracker.getInstance(Aircandi.applicationContext).send(MapBuilder
						.createEvent(TrackerCategory.SYSTEM, "session_start", null, null)
						.set(Fields.SESSION_CONTROL, "start")
						.build()
						);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unused")
	private void stopSession() {
		try {
			User user = Aircandi.getInstance().getCurrentUser();
			if (Constants.TRACKING_ENABLED && user != null && Type.isFalse(user.developer)) {
				EasyTracker.getInstance(Aircandi.applicationContext).send(MapBuilder
						.createEvent(TrackerCategory.SYSTEM, "session_end", null, null)
						.set(Fields.SESSION_CONTROL, "end")
						.build()
						);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}
