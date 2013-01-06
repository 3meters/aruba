package com.aircandi.components;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.BatteryManager;

import com.aircandi.Aircandi;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;

public class AndroidManager {

	private static AndroidManager	singletonObject;

	public static synchronized AndroidManager getInstance() {
		if (singletonObject == null) {
			singletonObject = new AndroidManager();
		}
		return singletonObject;
	}

	private AndroidManager() {}

	protected boolean getIsLowBattery() {
		/*
		 * Returns battery status. True if less than 15% remaining.
		 */
		IntentFilter batIntentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent battery = Aircandi.applicationContext.registerReceiver(null, batIntentFilter);
		float pctLevel = (float) battery.getIntExtra(BatteryManager.EXTRA_LEVEL, 1) /
				battery.getIntExtra(BatteryManager.EXTRA_SCALE, 1);
		return pctLevel < 0.15;
	}

	public void callMapActivity(Context context, String latitude, String longitude, String label) {
		String uri = "geo:" + latitude + "," + longitude + "?q="
				+ latitude
				+ "," + longitude
				+ "(" + label + ")";
		Intent searchAddress = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
		context.startActivity(searchAddress);
		AnimUtils.doOverridePendingTransition((Activity) context, TransitionType.CandiPageToAndroidApp);
	}

	public void callDialerActivity(Context context, String phoneNumber) {
		String number = "tel:" + phoneNumber.trim();
		Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse(number));
		context.startActivity(callIntent);
		AnimUtils.doOverridePendingTransition((Activity) context, TransitionType.CandiPageToAndroidApp);
	}

	public void callBrowserActivity(Context context, String uri) {
		Intent intent = findBrowserApp(context, uri);
		intent.setData(Uri.parse(uri));
		context.startActivity(intent);
		AnimUtils.doOverridePendingTransition((Activity) context, TransitionType.CandiPageToAndroidApp);
	}

	public void callSendActivity(Context context, String placeName, String uri) {
		Intent intent = new Intent(android.content.Intent.ACTION_SEND);
		intent.setType("text/plain");
		String shareBody = "Here is some aircandi info on a cool place I was at! " + uri;
		intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Subject Here");
		intent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
		context.startActivity(intent);
	}

	public void callTwitterActivity(Context context, String twitterHandle) {
		Intent intent = findTwitterApp(context);
		if (intent != null) {
			intent.setData(Uri.parse("https://www.twitter.com/" + twitterHandle));
			context.startActivity(intent);
		}
		AnimUtils.doOverridePendingTransition((Activity) context, TransitionType.CandiPageToAndroidApp);
	}

	public void callFoursquareActivity(Context context, String venueId) {
		/* First try to get the native app so we can go to it directly */
		Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
		intent.setType("text/plain");
		if (intent != null) {
			intent.setData(Uri.parse("http://m.foursquare.com/venue/" + venueId));
			context.startActivity(intent);
		}
		AnimUtils.doOverridePendingTransition((Activity) context, TransitionType.CandiPageToAndroidApp);
	}

	public void callFacebookActivity(Context context, String facebookId) {
		Intent intent = findFacebookApp(context);
		if (intent != null) {
			intent.setData(Uri.parse("fb://profile/" + facebookId + "/wall"));
		}
		else {
			intent = findBrowserApp(context, "http://www.facebook.com/" + facebookId);
			intent.setData(Uri.parse("http://www.facebook.com/" + facebookId));
		}
		context.startActivity(intent);
		AnimUtils.doOverridePendingTransition((Activity) context, TransitionType.CandiPageToAndroidApp);
	}

	public void callYelpActivity(Context context, String sourceId, String sourceUri) {
		Intent intent = findFacebookApp(context);
		if (intent != null) {
			intent.setData(Uri.parse(sourceUri));
			context.startActivity(intent);
		}
		else {
			intent = new Intent(android.content.Intent.ACTION_VIEW);
			intent.addCategory("android.intent.category.DEFAULT");
			if (intent != null) {
				intent.setData(Uri.parse(sourceUri));
				context.startActivity(intent);
			}
		}
		AnimUtils.doOverridePendingTransition((Activity) context, TransitionType.CandiPageToAndroidApp);
	}

	public Intent findTwitterApp(Context context) {
		final String[] apps = {
				"com.twitter.android", 			// official
				"com.twidroid", 				// twidroyd
				"com.handmark.tweetcaster", 	// Tweecaster
				"com.thedeck.android" };		// TweetDeck

		Intent intent = new Intent();
		intent.setType("text/plain");
		final PackageManager pm = context.getPackageManager();
		List<ResolveInfo> list = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

		for (int i = 0; i < apps.length; i++) {
			for (ResolveInfo resolveInfo : list) {
				String p = resolveInfo.activityInfo.packageName;
				if (p != null && p.startsWith(apps[i])) {
					intent.setPackage(p);
					return intent;
				}
			}
		}
		return null;
	}

	public Intent findFoursquareApp(Context context) {
		final String[] apps = {
				"foursquare" };		// official

		Intent intent = new Intent();
		intent.setType("text/plain");
		final PackageManager pm = context.getPackageManager();
		List<ResolveInfo> list = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

		for (int i = 0; i < apps.length; i++) {
			for (ResolveInfo resolveInfo : list) {
				String p = resolveInfo.activityInfo.packageName;
				if (p != null && p.contains(apps[i])) {
					intent.setPackage(p);
					return intent;
				}
			}
		}
		return null;
	}

	public Intent findYelpApp(Context context) {
		final String[] apps = {
				"yelp" };		// official

		Intent intent = new Intent();
		intent.setType("text/plain");
		final PackageManager pm = context.getPackageManager();
		List<ResolveInfo> list = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

		for (int i = 0; i < apps.length; i++) {
			for (ResolveInfo resolveInfo : list) {
				String p = resolveInfo.activityInfo.packageName;
				if (p != null && p.contains(apps[i])) {
					intent.setPackage(p);
					return intent;
				}
			}
		}
		return null;
	}

	public Intent findFacebookApp(Context context) {
		final String[] apps = {
				"com.facebook.katana" };		// Official android app

		Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
		intent.setType("text/plain");
		final PackageManager packageManager = context.getPackageManager();
		List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

		for (int i = 0; i < apps.length; i++) {
			for (ResolveInfo resolveInfo : list) {
				String p = resolveInfo.activityInfo.packageName;
				if (p != null && p.startsWith(apps[i])) {
					intent.setPackage(p);
					return intent;
				}
			}
		}
		return null;
	}

	public Intent findBrowserApp(Context context, String uri) {
		final String[] browserApps = {
				"com.android.browser",
				"com.google.android.browser" };

		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
		final PackageManager packageManager = context.getPackageManager();
		List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

		for (int i = 0; i < browserApps.length; i++) {
			for (ResolveInfo resolveInfo : list) {
				String p = resolveInfo.activityInfo.packageName;
				if (p != null && p.startsWith(browserApps[i])) {
					intent.setPackage(p);
					return intent;
				}
			}
		}
		return null;
	}
}
