package com.proxibase.aircandi.components;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class Tracker {

	public static void trackEvent(String arg0, String arg1, String arg2, int arg3) {
		try {
			GoogleAnalyticsTracker.getInstance().trackEvent(arg0, arg1, arg2, arg3);
		}
		catch (Exception exception) {
		}
	}

	public static void trackPageView(String arg0) {
		try {
			GoogleAnalyticsTracker.getInstance().trackPageView(arg0);
		}
		catch (Exception exception) {
		}
	}

	public static void dispatch() {
		try {
			GoogleAnalyticsTracker.getInstance().dispatch();
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
}
