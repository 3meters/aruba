package com.aircandi.components;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Environment;

import com.aircandi.Aircandi;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;

@SuppressWarnings("ucd")
public class AndroidManager {

	public static final int			MEDIA_TYPE_IMAGE	= 1;
	public static final int			MEDIA_TYPE_VIDEO	= 2;

	private static AndroidManager	singletonObject;

	public static synchronized AndroidManager getInstance() {
		if (singletonObject == null) {
			singletonObject = new AndroidManager();
		}
		return singletonObject;
	}

	private AndroidManager() {}

	public String getPublicName(String packageName) {

		try {
			ApplicationInfo info = Aircandi.packageManager.getApplicationInfo(packageName, 0);
			String publicName = (String) info.loadLabel(Aircandi.packageManager);
			return publicName;
		}
		catch (NameNotFoundException exception) {
			exception.printStackTrace();
			return null;
		}
	}

	public boolean doesPackageExist(String targetPackage) {
		List<ApplicationInfo> packages;
		packages = Aircandi.packageManager.getInstalledApplications(0);
		for (ApplicationInfo packageInfo : packages) {
			if (packageInfo.packageName.equals(targetPackage)) return true;
		}
		return false;
	}

	public static boolean isIntentAvailable(Context context, String action) {
		final PackageManager pm = context.getPackageManager();
		final Intent intent = new Intent(action);
		List<ResolveInfo> list = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}

	@SuppressWarnings("ucd")
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
		Intent callIntent = new Intent(Intent.ACTION_DIAL, Uri.parse(number));
		context.startActivity(callIntent);
		AnimUtils.doOverridePendingTransition((Activity) context, TransitionType.CandiPageToAndroidApp);
	}

	public void callBrowserActivity(Context context, String uri) {
		Intent intent = findBrowserApp(context, uri);
		if (intent != null) {
			intent.setData(Uri.parse(uri));
			context.startActivity(intent);
		}
		else {
			intent = new Intent(android.content.Intent.ACTION_VIEW);
			intent.setData(Uri.parse(uri));
			context.startActivity(intent);
		}
		AnimUtils.doOverridePendingTransition((Activity) context, TransitionType.CandiPageToAndroidApp);
	}

	@SuppressWarnings("ucd")
	public void callSendActivity(Context context, String placeName, String uri) {
		Intent intent = new Intent(android.content.Intent.ACTION_SEND);
		intent.setType("text/plain");
		String shareBody = "Here is some aircandi info on a cool place I was at! " + uri;
		intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Subject Here");
		intent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
		context.startActivity(intent);
	}

	public void callTwitterActivity(Context context, String twitterHandle) {

		Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
		intent.setData(Uri.parse("https://www.twitter.com/" + twitterHandle));
		context.startActivity(intent);
		AnimUtils.doOverridePendingTransition((Activity) context, TransitionType.CandiPageToAndroidApp);
	}

	public void callFoursquareActivity(Context context, String venueId) {

		Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
		intent.setData(Uri.parse("http://m.foursquare.com/venue/" + venueId));
		context.startActivity(intent);
		AnimUtils.doOverridePendingTransition((Activity) context, TransitionType.CandiPageToAndroidApp);
	}

	public void callOpentableActivity(Context context, String sourceId, String sourceUri) {
		Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
		intent.setData(Uri.parse(sourceUri));
		context.startActivity(intent);
		AnimUtils.doOverridePendingTransition((Activity) context, TransitionType.CandiPageToAndroidApp);
	}

	public void callFacebookActivity(Context context, String facebookId) {
		/*
		 * Calling the facebook app is actually a poorer experience than
		 * calling the web app. The facebook app does not lock on to the profile id.
		 * 
		 * intent.setData(Uri.parse("fb://place/" + facebookId + ""));
		 */
		Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
		intent.setData(Uri.parse("http://www.facebook.com/" + facebookId));
		context.startActivity(intent);
		AnimUtils.doOverridePendingTransition((Activity) context, TransitionType.CandiPageToAndroidApp);
	}

	public void callYelpActivity(Context context, String sourceId, String sourceUri) {
		Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
		intent.setData(Uri.parse(sourceUri));
		context.startActivity(intent);
		AnimUtils.doOverridePendingTransition((Activity) context, TransitionType.CandiPageToAndroidApp);
	}

	public void callGenericActivity(Context context, String sourceId) {
		Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
		intent.setData(Uri.parse(sourceId));
		context.startActivity(intent);
		AnimUtils.doOverridePendingTransition((Activity) context, TransitionType.CandiPageToAndroidApp);
	}

	private Intent findBrowserApp(Context context, String uri) {
		final String[] browserApps = {
				"com.android.browser",
				"com.android.chrome",
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

	@SuppressWarnings("ucd")
	public boolean checkCameraHardware(Context context) {
		/* Check if this device has a camera */
		if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
			return true;
		}
		else {
			return false;
		}
	}

	@SuppressWarnings("ucd")
	public static Uri getOutputMediaFileUri(int type) {
		/* Create a file Uri for saving an image or video */
		return Uri.fromFile(getOutputMediaFile(type));
	}

	public static File getOutputMediaFile(int type) {
		/*
		 * Create a File for saving an image or video
		 * 
		 * To be safe, you should check that the SDCard is mounted
		 * using Environment.getExternalStorageState() before doing this.
		 */
		File mediaStorageDir = null;
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "aircandi");
		}
		else {
			return null;
		}
		/*
		 * This location works best if you want the created images to be shared
		 * between applications and persist after your app has been uninstalled.
		 */

		/* Create the storage directory if it does not exist */
		if (!mediaStorageDir.exists()) {
			if (!mediaStorageDir.mkdirs()) {
				Logger.d(Aircandi.applicationContext, "Failed to create directory for pictures");
				return null;
			}
		}

		/* Create a media file name */
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
		File mediaFile;
		if (type == MEDIA_TYPE_IMAGE) {
			mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
		}
		else if (type == MEDIA_TYPE_VIDEO) {
			mediaFile = new File(mediaStorageDir.getPath() + File.separator + "VID_" + timeStamp + ".mp4");
		}
		else {
			return null;
		}

		return mediaFile;
	}
}
