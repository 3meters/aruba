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
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;

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
		ReportField.DROPBOX,
		ReportField.LOGCAT,
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
		resDialogOkToast = R.string.crash_dialog_ok_toast,
		logcatArguments = {
				"-t",
				"100",
				"-v",
				"long",
				"ActivityManager:I",
				"Aircandi:D",
				"Proxibase:D",
				"*:S" } // Filter format : tag : priority
)
public class Aircandi extends Application {

	public final static int					DEBUG_SIGNATURE_HASH		= -2026043354;
	public static BasicAWSCredentials		mAwsCredentials				= null;

	private static Aircandi					singletonObject;

	public static SharedPreferences			settings;
	public static SharedPreferences.Editor	settingsEditor;

	public static Context					applicationContext;
	public static Handler					mainThreadHandler;
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

	public static Aircandi getInstance() {
		return singletonObject;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		singletonObject = this;
		singletonObject.initializeInstance();
	}

	protected void initializeInstance() {

		/* The following line triggers the initialization of ACRA */
		ACRA.init(this);

		applicationContext = getApplicationContext();
		mainThreadHandler = new Handler(Looper.getMainLooper());
		stopwatch1 = new Stopwatch("Stopwatch1");
		stopwatch2 = new Stopwatch("Stopwatch2");
		stopwatch3 = new Stopwatch("Stopwatch3");

		/* Make settings available app wide */
		settings = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
		settingsEditor = settings.edit();
	}

	public static String getVersionName(Context context, Class cls) {
		try {
			ComponentName comp = new ComponentName(context, cls);
			PackageInfo pinfo = context.getPackageManager().getPackageInfo(comp.getPackageName(), 0);
			return pinfo.versionName;
		}
		catch (android.content.pm.PackageManager.NameNotFoundException e) {
			return null;
		}
	}

	public void setUser(User user) {
		this.mUser = user;
	}

	public User getUser() {
		return mUser;
	}

	public void setLaunchedNormally(Boolean launchedNormally) {
		this.mLaunchedNormally = launchedNormally;
	}

	public Boolean wasLaunchedNormally() {
		return mLaunchedNormally;
	}

	public static enum CandiTask {
		None, MyCandi, RadarCandi
	}
}
