package com.proxibase.aircandi;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.location.Location;
import android.os.Handler;
import android.preference.PreferenceManager;

import com.proxibase.service.objects.User;

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

	private static Aircandi					singletonObject;

	public final static int					DEBUG_SIGNATURE_HASH	= -2026043354;
	public static SharedPreferences			settings;
	public static SharedPreferences.Editor	settingsEditor;
	public static Context					applicationContext;
	public static Handler					applicationHandler;
	private User							mUser;
	private Boolean							mRebuildingDataModel	= false;
	private Boolean							mToolstripOpen			= false;
	private Boolean							mFirstTimeCandiForm		= true;
	private CandiTask						mCandiTask				= CandiTask.RadarCandi;
	private Boolean							mLaunchedFromRadar		= false;
	public static Boolean					firstRunApp				= true;
	public static Boolean					firstRunRadar			= true;
	public static Boolean					runFullScan				= true;
	public static Boolean					runUiUpdate				= false;
	public static Boolean					lastScanEmpty			= false;

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

		/* Make settings available app wide */
		settings = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
		settingsEditor = settings.edit();
		firstRunApp = Aircandi.settings.getBoolean(Preferences.SETTING_FIRST_RUN, true);
	}

	public static int timeSinceLocationInMillis(Location location) {
		if (location == null) {
			return Integer.MAX_VALUE;
		}
		long locationTime = location.getTime();
		long currentTime = System.currentTimeMillis();
		long timeDelta = currentTime - locationTime;
		return (int) timeDelta;
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

	public void setToolstripOpen(Boolean toolstripOpen) {
		this.mToolstripOpen = toolstripOpen;
	}

	public Boolean getToolstripOpen() {
		return mToolstripOpen;
	}

	public void setFirstTimeCandiForm(Boolean firstTimeCandiForm) {
		this.mFirstTimeCandiForm = firstTimeCandiForm;
	}

	public Boolean getFirstTimeCandiForm() {
		return mFirstTimeCandiForm;
	}

	public void setCandiTask(CandiTask candiTask) {
		this.mCandiTask = candiTask;
	}

	public CandiTask getCandiTask() {
		return mCandiTask;
	}

	public void setRebuildingDataModel(Boolean rebuildingDataModel) {
		this.mRebuildingDataModel = rebuildingDataModel;
	}

	public Boolean isRebuildingDataModel() {
		return mRebuildingDataModel;
	}

	public void setLaunchedFromRadar(Boolean launchedFromRadar) {
		this.mLaunchedFromRadar = launchedFromRadar;
	}

	public Boolean getLaunchedFromRadar() {
		return mLaunchedFromRadar;
	}

	public static enum CandiTask {
		None, MyCandi, RadarCandi, Map
	}

}
