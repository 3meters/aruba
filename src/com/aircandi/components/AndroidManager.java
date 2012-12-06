package com.aircandi.components;

import java.util.List;

import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

public class AndroidManager {

	private static AndroidManager	singletonObject;

	public static synchronized AndroidManager getInstance() {
		if (singletonObject == null) {
			singletonObject = new AndroidManager();
		}
		return singletonObject;
	}

	/**
	 * Designed as a singleton. The private Constructor prevents any other class from instantiating.
	 */
	private AndroidManager() {}

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

	public void callFacebookActivity(Context context, String facebookId) {
		Intent intent = findFacebookApp(context);
		if (intent != null) {
			intent.setData(Uri.parse("fb://profile/" + facebookId + "/wall"));
			//intent.setData(Uri.parse("fb://page/" + facebookId));
		}
		else {
			intent = findBrowserApp(context, "http://www.facebook.com/" + facebookId);
			intent.setData(Uri.parse("http://www.facebook.com/" + facebookId));
		}
		context.startActivity(intent);
		AnimUtils.doOverridePendingTransition((Activity) context, TransitionType.CandiPageToAndroidApp);
	}

	public Intent findTwitterApp(Context context) {
		final String[] twitterApps = {
				"com.twitter.android", 			// official
				"com.twidroid", 				// twidroyd
				"com.handmark.tweetcaster", 	// Tweecaster
				"com.thedeck.android" };		// TweetDeck

		Intent tweetIntent = new Intent();
		tweetIntent.setType("text/plain");
		final PackageManager packageManager = context.getPackageManager();
		List<ResolveInfo> list = packageManager.queryIntentActivities(tweetIntent, PackageManager.MATCH_DEFAULT_ONLY);

		for (int i = 0; i < twitterApps.length; i++) {
			for (ResolveInfo resolveInfo : list) {
				String p = resolveInfo.activityInfo.packageName;
				if (p != null && p.startsWith(twitterApps[i])) {
					tweetIntent.setPackage(p);
					return tweetIntent;
				}
			}
		}
		return null;
	}

	public Intent findFacebookApp(Context context) {
		final String[] facebookApps = {
				"com.facebook.katana" };		// Official android app

		Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
		intent.setType("text/plain");
		final PackageManager packageManager = context.getPackageManager();
		List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

		for (int i = 0; i < facebookApps.length; i++) {
			for (ResolveInfo resolveInfo : list) {
				String p = resolveInfo.activityInfo.packageName;
				if (p != null && p.startsWith(facebookApps[i])) {
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
