package com.proxibase.aircandi;

import java.util.Calendar;

import android.app.Application;
import android.location.Location;

import com.proxibase.aircandi.R;
import com.proxibase.aircandi.core.CandiConstants;

import org.acra.*;
import org.acra.annotation.*;
@ReportsCrashes(formKey = "dFBjSFl2eWpOdkF0TlR5ZUlvaDlrUUE6MQ",                 
		customReportContent = {
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
		logcatArguments = { "-t", "100", "-v", "long", "ActivityManager:I", "Aircandi:D", "Proxibase:D", "*:S" } /* Filter format: tag:priority */
)
public class Aircandi extends Application {

	public Location	currentLocation;

	public Aircandi() {}

	@Override
	public void onCreate() {
		/* The following line triggers the initialization of ACRA */
		ACRA.init(this);
		super.onCreate();
	}

	/**
	 * Determines whether one Location reading is better than the current Location fix *
	 * 
	 * @param location The new Location that you want to evaluate * @param currentBestLocation The current Location fix,
	 *            to which you want to compare the new one
	 */
	public static boolean isBetterLocation(Location location, Location currentBestLocation) {

		if (currentBestLocation == null) {
			/* A new location is always better than no location */
			return true;
		}

		/* Check whether the new location fix is newer or older */
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > CandiConstants.TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -CandiConstants.TWO_MINUTES;
		boolean isNewer = timeDelta > 0;

		/*
		 * If it's been more than two minutes since the current location, use the new location
		 * because the user has likely moved
		 */
		if (isSignificantlyNewer) {
			/* If the new location is more than two minutes older, it must be worse */
			return true;
		}
		else if (isSignificantlyOlder) {
			return false;
		}

		/* Check whether the new location fix is more or less accurate */
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		/* Check if the old and new location are from the same provider */
		boolean isFromSameProvider = isSameProvider(location.getProvider(), currentBestLocation.getProvider());

		/* Determine location quality using a combination of timeliness and accuracy */
		if (isMoreAccurate) {
			return true;
		}
		else if (isNewer && !isLessAccurate) {
			return true;
		}
		else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
			return true;
		}
		return false;
	}

	public static long locationFixAge(Location location) {
		Calendar cal = Calendar.getInstance();
		long timeDelta = cal.getTimeInMillis() - location.getTime();
		return timeDelta;
	}

	/** Checks whether two providers are the same */
	private static boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}
}
