package com.aircandi;

import net.minidev.json.parser.JSONParser;

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
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;

import com.aircandi.components.StopWatch;
import com.aircandi.components.location.IStrictMode;
import com.aircandi.components.location.PlatformSpecificImplementationFactory;
import com.aircandi.service.objects.User;

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

	private static Aircandi					singletonObject;

	public static SharedPreferences			settings;
	public static SharedPreferences.Editor	settingsEditor;

	public static Context					applicationContext;
	public static Handler					applicationHandler;
	public static DisplayMetrics			displayMetrics;
	public static StopWatch					stopwatch;
	public static JSONParser				parser						= new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE);

	public static Boolean					firstStartApp				= true;
	public static Boolean					usingEmulator				= false;
	public static Integer					wifiCount					= 0;

	public static Boolean					applicationUpdateNeeded		= false;
	public static Boolean					applicationUpdateRequired	= false;
	public static String					applicationUpdateUri;
	public static Number					lastApplicationUpdateCheckDate;

	private User							mUser;
	private CandiTask						mCandiTask					= CandiTask.RadarCandi;
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
		applicationHandler = new Handler();
		stopwatch = new StopWatch();

		/* Make settings available app wide */
		settings = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
		settingsEditor = settings.edit();

		/* Strict mode */
		if (CandiConstants.DEVELOPER_MODE) {
			IStrictMode strictMode = PlatformSpecificImplementationFactory.getStrictMode();
			if (strictMode != null)
				strictMode.enableStrictMode();
		}
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

	public void setCandiTask(CandiTask candiTask) {
		this.mCandiTask = candiTask;
	}

	public CandiTask getCandiTask() {
		return mCandiTask;
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
