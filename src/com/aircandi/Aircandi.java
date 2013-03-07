// $codepro.audit.disable fileComment
package com.aircandi;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;

import com.aircandi.beta.R;
import com.aircandi.components.Logger;
import com.aircandi.components.Stopwatch;
import com.aircandi.service.objects.User;
import com.amazonaws.auth.BasicAWSCredentials;

@ReportsCrashes(formKey = "dFBjSFl2eWpOdkF0TlR5ZUlvaDlrUUE6MQ", customReportContent = {
		ReportField.REPORT_ID,
		ReportField.APP_VERSION_CODE,
		ReportField.APP_VERSION_NAME,
		ReportField.PACKAGE_NAME,
		ReportField.FILE_PATH,
		ReportField.PHONE_MODEL,
		ReportField.BRAND,
		ReportField.PRODUCT,
		ReportField.ANDROID_VERSION,
		ReportField.BUILD,
		ReportField.TOTAL_MEM_SIZE,
		ReportField.AVAILABLE_MEM_SIZE,
		ReportField.CUSTOM_DATA,
		ReportField.IS_SILENT,
		ReportField.STACK_TRACE,
		ReportField.INITIAL_CONFIGURATION,
		ReportField.CRASH_CONFIGURATION,
		ReportField.DISPLAY,
		ReportField.USER_COMMENT,
		ReportField.USER_EMAIL,
		ReportField.USER_APP_START_DATE,
		ReportField.USER_CRASH_DATE,
		ReportField.DUMPSYS_MEMINFO,
		ReportField.RADIOLOG,
		ReportField.DEVICE_ID,
		ReportField.INSTALLATION_ID,
		ReportField.DEVICE_FEATURES,
		ReportField.ENVIRONMENT,
		ReportField.SHARED_PREFERENCES,
		ReportField.SETTINGS_SYSTEM,
		ReportField.SETTINGS_SECURE },
		mode = ReportingInteractionMode.NOTIFICATION,
		resToastText = R.string.crash_toast_text,
		resNotifTickerText = R.string.crash_notif_ticker_text,
		resNotifTitle = R.string.crash_notif_title,
		resNotifText = R.string.crash_notif_text,
		resNotifIcon = android.R.drawable.stat_notify_error,
		resDialogText = R.string.crash_dialog_text,
		resDialogIcon = android.R.drawable.ic_dialog_info,
		resDialogTitle = R.string.crash_dialog_title,
		resDialogCommentPrompt = R.string.crash_dialog_comment_prompt,
		resDialogOkToast = R.string.crash_dialog_ok_toast
)
public class Aircandi extends Application {

	public static BasicAWSCredentials		awsCredentials				= null;

	private static Aircandi					singletonObject;

	public static SharedPreferences			settings;
	public static SharedPreferences.Editor	settingsEditor;

	public static Context					applicationContext;
	public static Handler					mainThreadHandler;
	public static PackageManager			packageManager;

	public static DisplayMetrics			displayMetrics;
	public static Stopwatch					stopwatch1;
	public static Stopwatch					stopwatch2;
	public static Stopwatch					stopwatch3;

	public static Boolean					firstStartApp				= true;
	public static Boolean					usingEmulator				= false;
	public static Integer					wifiCount					= 0;

	public static Boolean					applicationUpdateNeeded		= false;
	public static Boolean					applicationUpdateRequired	= false;
	public static String					applicationUpdateUri;
	public static Number					lastApplicationUpdateCheckDate;

	private User							mUser;
	private Boolean							mLaunchedNormally			= false;

	/* Common preferences */
	private String							mPrefTheme;
	private String							mPrefSearchRadius;

	/* Dev preferences */
	private Boolean							mPrefEnableDev;
	private Boolean							mPrefEntityFencing;
	private Boolean							mPrefShowPlaceRankScore;
	private String							mPrefTestingBeacons;
	private String							mPrefTestingLocation;
	private String							mPrefTestingPlaceProvider;

	private boolean							mUsingEmulator				= false;

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

		/* The following line triggers the initialization of ACRA */
		ACRA.init(this);

		applicationContext = getApplicationContext();
		mainThreadHandler = new Handler(Looper.getMainLooper());
		packageManager = applicationContext.getPackageManager();

		stopwatch1 = new Stopwatch("Stopwatch1"); // $codepro.audit.disable stringLiterals
		stopwatch2 = new Stopwatch("Stopwatch2"); // $codepro.audit.disable stringLiterals
		stopwatch3 = new Stopwatch("Stopwatch3"); // $codepro.audit.disable stringLiterals

		/* Make settings available app wide */
		settings = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
		settingsEditor = settings.edit();

		/* Set prefs so we can tell when a change happens that we need to respond to. Theme is set in setTheme(). */
		snapshotPreferences();
	}

	public void snapshotPreferences() {
		mPrefTheme = Aircandi.settings.getString(CandiConstants.PREF_THEME, CandiConstants.PREF_THEME_DEFAULT);
		mPrefSearchRadius = Aircandi.settings.getString(CandiConstants.PREF_SEARCH_RADIUS, CandiConstants.PREF_SEARCH_RADIUS_DEFAULT);
		mPrefEnableDev = Aircandi.settings.getBoolean(CandiConstants.PREF_ENABLE_DEV, CandiConstants.PREF_ENABLE_DEV_DEFAULT);
		mPrefEntityFencing = Aircandi.settings.getBoolean(CandiConstants.PREF_ENTITY_FENCING, CandiConstants.PREF_ENTITY_FENCING_DEFAULT);
		mPrefShowPlaceRankScore = Aircandi.settings.getBoolean(CandiConstants.PREF_SHOW_PLACE_RANK_SCORE, CandiConstants.PREF_SHOW_PLACE_RANK_SCORE_DEFAULT);
		mPrefTestingBeacons = Aircandi.settings.getString(CandiConstants.PREF_TESTING_BEACONS, CandiConstants.PREF_TESTING_BEACONS_DEFAULT);
		mPrefTestingLocation = Aircandi.settings.getString(CandiConstants.PREF_TESTING_LOCATION, CandiConstants.PREF_TESTING_LOCATION_DEFAULT);
		mPrefTestingPlaceProvider = Aircandi.settings.getString(CandiConstants.PREF_TESTING_PLACE_PROVIDER, CandiConstants.PREF_TESTING_PLACE_PROVIDER_DEFAULT);
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

	public void setUser(User user) {
		mUser = user;
	}

	public User getUser() {
		return mUser;
	}

	public void setLaunchedNormally(Boolean launchedNormally) {
		mLaunchedNormally = launchedNormally;
	}

	public Boolean wasLaunchedNormally() {
		return mLaunchedNormally;
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

	public Boolean getPrefShowPlaceRankScore() {
		return mPrefShowPlaceRankScore;
	}

	public String getPrefTestingBeacons() {
		return mPrefTestingBeacons;
	}

	public String getPrefTestingLocation() {
		return mPrefTestingLocation;
	}

	public String getPrefTestingPlaceProvider() {
		return mPrefTestingPlaceProvider;
	}

	public boolean isUsingEmulator() {
		return mUsingEmulator;
	}

	public void setUsingEmulator(boolean usingEmulator) {
		mUsingEmulator = usingEmulator;
	}
}
