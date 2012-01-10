package com.proxibase.aircandi.components;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class Tracker {

	public static void trackEvent(String arg0, String arg1, String arg2, int arg3) {
		GoogleAnalyticsTracker.getInstance().trackEvent(arg0, arg1, arg2, arg3);
	}

	public static void trackPageView(String arg0) {
		GoogleAnalyticsTracker.getInstance().trackPageView(arg0);
	}

	public static void dispatch() {
		GoogleAnalyticsTracker.getInstance().dispatch();
	}

	public static void stopSession() {
		GoogleAnalyticsTracker.getInstance().stopSession();
	}
}
