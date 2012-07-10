package com.proxibase.aircandi.components;

import android.content.Context;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

/*
 * Tracker strategy
 * 
 * - Every activity is a page view when initialized.
 * - Page views and events info is dispatched to google service when a page view is created.
 * 	
 * - Select events are tracked
 * 		- Insert, update, delete entity
 * 		- User clicks refresh
 * 		- Insert, update user
 * 		- Comment created
 * 		- User signin, signout
 * 
 * 		More candidates
 * 		- Preferences modified
 */

public class Tracker {

	public static void trackEvent(String arg0, String arg1, String arg2, int arg3) {
		/*
		 * Arguments should be free of whitespace.
		 */
		try {
			//GoogleAnalyticsTracker.getInstance().trackEvent(arg0, arg1, arg2, arg3);
		}
		catch (Exception exception) {
		}
	}

	public static void trackPageView(String arg0) {
		try {
			//GoogleAnalyticsTracker.getInstance().trackPageView(arg0);
		}
		catch (Exception exception) {
		}
	}

	public static void dispatch() {
		try {
			//GoogleAnalyticsTracker.getInstance().dispatch();
		}
		catch (Exception exception) {
		}
	}

	public static void stopSession() {
		try {
			GoogleAnalyticsTracker.getInstance().stopSession();
		}
		catch (Exception exception) {
		}
	}
	public static void startNewSession(String arg0, Context context) {
		try {
			GoogleAnalyticsTracker.getInstance().startNewSession(arg0, context);
		}
		catch (Exception exception) {
		}
	}
}
