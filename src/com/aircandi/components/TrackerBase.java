package com.aircandi.components;

import android.app.Activity;
import android.support.v4.app.Fragment;

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
public abstract class TrackerBase implements TrackerDelegate {

	@Override
	public void sendTiming(String category, Long timing, String name, String label) {}

	@Override
	public void sendException(Exception exception) {}

	@Override
	public void sendError(String category, String name) {}

	@Override
	public void activityStop(Activity activity) {}

	@Override
	public void fragmentStart(Fragment fragment) {}

	@Override
	public void enableDeveloper(Boolean enable) {}

	public static class TrackerCategory {
		public static String	UX			= "ux";
		public static String	SYSTEM		= "system";
		public static String	EDIT		= "editing";
		public static String	LINK		= "linking";
		public static String	USER		= "user";
		public static String	PERFORMANCE	= "performance";
	}

}
