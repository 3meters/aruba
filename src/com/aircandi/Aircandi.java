// $codepro.audit.disable fileComment
package com.aircandi;

import java.util.Locale;
import java.util.UUID;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.DisplayMetrics;

import com.aircandi.components.Logger;
import com.aircandi.components.Stopwatch;
import com.aircandi.components.TrackerDelegate;
import com.aircandi.components.TrackerGoogleEasy;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.User;
import com.aircandi.ui.AircandiForm;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Reporting;
import com.amazonaws.auth.BasicAWSCredentials;
import com.google.analytics.tracking.android.GoogleAnalytics;

public class Aircandi extends Application {

	public static BasicAWSCredentials		awsCredentials					= null;

	private static Aircandi					singletonObject;

	public static SharedPreferences			settings;
	public static SharedPreferences.Editor	settingsEditor;

	public static Context					applicationContext;
	public static Handler					mainThreadHandler;
	public static PackageManager			packageManager;
	public static TrackerDelegate			tracker;
	public static GoogleAnalytics			analytics;

	public static DisplayMetrics			displayMetrics;
	public static SoundPool					soundPool;
	public static Stopwatch					stopwatch1;
	public static Stopwatch					stopwatch2;
	public static Stopwatch					stopwatch3;

	public static Boolean					firstStartApp					= true;
	public static Boolean					usingEmulator					= false;
	public static Integer					wifiCount						= 0;

	public static Boolean					muteColor						= false;

	public static Boolean					applicationUpdateRequired		= false;

	public static Boolean					LAUNCHED_NORMALLY;
	private static String					uniqueId						= null;
	private static Long						uniqueDate						= null;
	private static String					uniqueType						= null;
	private static final String				PREF_UNIQUE_ID					= "PREF_UNIQUE_ID";
	private static final String				PREF_UNIQUE_ID_DATE				= "PREF_UNIQUE_ID_DATE";
	private static final String				PREF_UNIQUE_ID_TYPE				= "PREF_UNIQUE_ID_TYPE";
	public static Intent					resultIntent					= null;
	public static Integer					resultCode						= Activity.RESULT_OK;

	/* Current objects */
	private Entity							mCurrentPlace;
	private Activity						mCurrentActivity;
	private User							mCurrentUser;

	/* Common preferences */
	private String							mPrefTheme;
	private String							mPrefSearchRadius;
	private Class<?>						mNavigationDrawerCurrentView	= AircandiForm.class;

	/* Dev preferences */
	private Boolean							mPrefEnableDev;
	private Boolean							mPrefEntityFencing;
	private Boolean							mPrefTestingSelfNotify;
	private String							mPrefTestingBeacons;
	private String							mPrefTestingLocation;
	private String							mPrefPlaceProvider;

	private boolean							mUsingEmulator					= false;

	public static Aircandi getInstance() {
		return singletonObject;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		singletonObject = this;
		singletonObject.initializeInstance();
	}

	private void initializeInstance() {

		stopwatch1 = new Stopwatch("Stopwatch1"); // $codepro.audit.disable stringLiterals
		stopwatch2 = new Stopwatch("Stopwatch2"); // $codepro.audit.disable stringLiterals
		stopwatch3 = new Stopwatch("Stopwatch3"); // $codepro.audit.disable stringLiterals

		Reporting.startCrashReporting(this);

		applicationContext = getApplicationContext();
		mainThreadHandler = new Handler(Looper.getMainLooper());
		packageManager = applicationContext.getPackageManager();
		soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 100);

		/* Make settings available app wide */
		settings = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
		settingsEditor = settings.edit();

		/* Make sure unique id is initialized */
		Aircandi.getInstallationId();

		/* Color hinting */
		muteColor = android.os.Build.MODEL.toLowerCase(Locale.US).equals("nexus s"); // nexus 4, nexus 7 are others

		/* Setup the analytics tracker */
		tracker = new TrackerGoogleEasy();
		analytics = GoogleAnalytics.getInstance(this);
		tracker.applicationStart();

		/* Set prefs so we can tell when a change happens that we need to respond to. Theme is set in setTheme(). */
		snapshotPreferences();
	}

	public void snapshotPreferences() {
		mPrefTheme = Aircandi.settings.getString(Constants.PREF_THEME, Constants.PREF_THEME_DEFAULT);
		mPrefSearchRadius = Aircandi.settings.getString(Constants.PREF_SEARCH_RADIUS, Constants.PREF_SEARCH_RADIUS_DEFAULT);
		mPrefPlaceProvider = Aircandi.settings.getString(Constants.PREF_PLACE_PROVIDER, Constants.PREF_PLACE_PROVIDER_DEFAULT);
		mPrefEnableDev = Aircandi.settings.getBoolean(Constants.PREF_ENABLE_DEV, Constants.PREF_ENABLE_DEV_DEFAULT);
		mPrefEntityFencing = Aircandi.settings.getBoolean(Constants.PREF_ENTITY_FENCING, Constants.PREF_ENTITY_FENCING_DEFAULT);
		mPrefTestingBeacons = Aircandi.settings.getString(Constants.PREF_TESTING_BEACONS, Constants.PREF_TESTING_BEACONS_DEFAULT);
		mPrefTestingLocation = Aircandi.settings.getString(Constants.PREF_TESTING_LOCATION, Constants.PREF_TESTING_LOCATION_DEFAULT);
		mPrefTestingSelfNotify = Aircandi.settings.getBoolean(Constants.PREF_TESTING_SELF_NOTIFY, Constants.PREF_TESTING_SELF_NOTIFY_DEFAULT);
	}

	public static String getVersionName(Context context, Class cls) {
		try {
			final ComponentName comp = new ComponentName(context, cls);
			final PackageInfo pinfo = context.getPackageManager().getPackageInfo(comp.getPackageName(), 0);
			return pinfo.versionName;
		}
		catch (android.content.pm.PackageManager.NameNotFoundException e) {
			Logger.e(applicationContext, e.getMessage());
			return null;
		}
	}

	public static Integer getVersionCode(Context context, Class cls) {
		try {
			final ComponentName comp = new ComponentName(context, cls);
			final PackageInfo pinfo = context.getPackageManager().getPackageInfo(comp.getPackageName(), 0);
			return pinfo.versionCode;
		}
		catch (android.content.pm.PackageManager.NameNotFoundException e) {
			Logger.e(applicationContext, e.getMessage());
			return null;
		}
	}

	public synchronized static String getInstallType() {
		if (uniqueType == null) {
			getInstallationId();
		}
		return uniqueType;
	}

	public synchronized static Long getInstallDate() {
		if (uniqueDate == null) {
			getInstallationId();
		}
		return uniqueDate;
	}

	public synchronized static String getInstallationId() {
		if (uniqueId == null) {
			uniqueId = Aircandi.settings.getString(PREF_UNIQUE_ID, null);
			uniqueDate = Aircandi.settings.getLong(PREF_UNIQUE_ID_DATE, 0);
			uniqueType = Aircandi.settings.getString(PREF_UNIQUE_ID_TYPE, null);
			if (uniqueId == null || uniqueType == null) {
				if (Build.SERIAL != null) {
					uniqueId = Build.SERIAL;
					uniqueType = Constants.INSTALL_TYPE_SERIAL;
				}
				else if (Settings.Secure.ANDROID_ID != null) {
					uniqueId = Settings.Secure.ANDROID_ID;
					uniqueType = Constants.INSTALL_TYPE_ANDROID_ID;
				}
				else {
					uniqueId = UUID.randomUUID().toString();
					uniqueType = Constants.INSTALL_TYPE_RANDOM;
				}
				uniqueDate = DateTime.nowDate().getTime();
				Aircandi.settingsEditor.putString(PREF_UNIQUE_ID_TYPE, uniqueType);
				Aircandi.settingsEditor.putString(PREF_UNIQUE_ID, uniqueId);
				Aircandi.settingsEditor.putLong(PREF_UNIQUE_ID_DATE, uniqueDate);
				Aircandi.settingsEditor.commit();
			}
		}
		return uniqueId;
	}

	public void setCurrentUser(User user) {
		mCurrentUser = user;
		Reporting.updateCrashUser(user);
	}

	public User getCurrentUser() {
		return mCurrentUser;
	}

	public String getPrefTheme() {
		return mPrefTheme;
	}

	public String getPrefSearchRadius() {
		return mPrefSearchRadius;
	}

	public Boolean getPrefEnableDev() {
		return mPrefEnableDev;
	}

	public Boolean getPrefEntityFencing() {
		return mPrefEntityFencing;
	}

	public String getPrefTestingBeacons() {
		return mPrefTestingBeacons;
	}

	public String getPrefTestingLocation() {
		return mPrefTestingLocation;
	}

	public String getPrefTestingPlaceProvider() {
		return mPrefPlaceProvider;
	}

	public boolean isUsingEmulator() {
		return mUsingEmulator;
	}

	public void setUsingEmulator(boolean usingEmulator) {
		mUsingEmulator = usingEmulator;
	}

	public Class<?> getNavigationDrawerCurrentView() {
		return mNavigationDrawerCurrentView;
	}

	public void setNavigationDrawerCurrentView(Class<?> navigationDrawerCurrentView) {
		mNavigationDrawerCurrentView = navigationDrawerCurrentView;
	}

	public Boolean getPrefTestingSelfNotify() {
		return mPrefTestingSelfNotify;
	}

	public void setPrefTestingSelfNotify(Boolean prefTestingSelfNotify) {
		mPrefTestingSelfNotify = prefTestingSelfNotify;
	}

	public Activity getCurrentActivity() {
		return mCurrentActivity;
	}

	public void setCurrentActivity(Activity currentActivity) {
		mCurrentActivity = currentActivity;
	}

	public Entity getCurrentPlace() {
		return mCurrentPlace;
	}

	public void setCurrentPlace(Entity currentPlace) {
		mCurrentPlace = currentPlace;
	}
}
