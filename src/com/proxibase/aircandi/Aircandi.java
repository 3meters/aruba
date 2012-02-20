package com.proxibase.aircandi;

import java.util.Calendar;
import java.util.concurrent.atomic.AtomicBoolean;

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
import android.content.pm.Signature;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.proxibase.aircandi.components.Events;
import com.proxibase.aircandi.components.ImageUtils;
import com.proxibase.aircandi.components.Logger;
import com.proxibase.aircandi.components.Events.EventHandler;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.sdk.android.proxi.consumer.User;

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
																						ReportField.SETTINGS_SECURE }, mode = ReportingInteractionMode.NOTIFICATION, resToastText = R.string.crash_toast_text, resNotifTickerText = R.string.crash_notif_ticker_text, resNotifTitle = R.string.crash_notif_title, resNotifText = R.string.crash_notif_text, resNotifIcon = android.R.drawable.stat_notify_error, resDialogText = R.string.crash_dialog_text, resDialogIcon = android.R.drawable.ic_dialog_info, resDialogTitle = R.string.crash_dialog_title, resDialogCommentPrompt = R.string.crash_dialog_comment_prompt, resDialogOkToast = R.string.crash_dialog_ok_toast, logcatArguments = { "-t",
																																																																																																																																																																																"100",
																																																																																																																																																																																"-v",
																																																																																																																																																																																"long",
																																																																																																																																																																																"ActivityManager:I",
																																																																																																																																																																																"Aircandi:D",
																																																																																																																																																																																"Proxibase:D",
																																																																																																																																																																																"*:S" } /*
																																																																																																																																																																																		 * Filter
																																																																																																																																																																																		 * format
																																																																																																																																																																																		 * :
																																																																																																																																																																																		 * tag
																																																																																																																																																																																		 * :
																																																																																																																																																																																		 * priority
																																																																																																																																																																																		 */
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
	private Location						mCurrentLocation;
	private static Boolean					mIsDebugBuild;
	private Boolean							mLaunchedFromRadar		= false;

	private LocationManager					mLocationManager;
	private LocationListener				mLocationListener;
	private EventHandler					mEventLocationChanged;
	private AtomicBoolean					mLocationScanActive		= new AtomicBoolean(false);
	private Runnable						mLocationScanRunnable;

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

		/* Location */
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		mLocationListener = new LocationListener() {

			@Override
			public void onLocationChanged(Location location) {
				if (Aircandi.isBetterLocation(location, Aircandi.getInstance().getCurrentLocation())) {
					Aircandi.getInstance().setCurrentLocation(location);
					location.setTime(System.currentTimeMillis());
					Events.EventBus.onLocationChanged(location);
				}
			}

			@Override
			public void onProviderDisabled(String provider) {
				ImageUtils.showToastNotification(provider + ": disabled", Toast.LENGTH_SHORT);
			}

			@Override
			public void onProviderEnabled(String provider) {
				ImageUtils.showToastNotification(provider + ": enabled", Toast.LENGTH_SHORT);
			}

			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {
				if (status == LocationProvider.AVAILABLE) {
					ImageUtils.showToastNotification(provider + ": available", Toast.LENGTH_SHORT);
				}
				else if (status == LocationProvider.OUT_OF_SERVICE) {
					ImageUtils.showToastNotification(provider + ": out of service", Toast.LENGTH_SHORT);
				}
				else if (status == LocationProvider.TEMPORARILY_UNAVAILABLE) {
					ImageUtils.showToastNotification(provider + ": temporarily unavailable", Toast.LENGTH_SHORT);
				}
			}
		};

		mLocationScanRunnable = new Runnable() {

			@Override
			public void run() {
				if (mLocationScanActive.get()) {
					Logger.d(this, "Location scan stopped: time limit");
					stopLocationUpdates();
				}
			}
		};
		
		mEventLocationChanged = new EventHandler() {

			@Override
			public void onEvent(Object data) {

				Location location = Aircandi.getInstance().getCurrentLocation();
				if (location.hasAccuracy() && location.getAccuracy() <= 30) {
					Logger.d(this, "Location scan stopped: accurate fix obtained");
					stopLocationUpdates();
					return;
				}
			}
		};
		Events.EventBus.locationChanged.add(mEventLocationChanged);

		/*
		 * We grab the last known location if it is less than two minutes old.
		 */
		Location lastKnownLocationNetwork = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		if (lastKnownLocationNetwork != null && timeSinceLocationInMillis(lastKnownLocationNetwork) >= CandiConstants.LOCATION_EXPIRATION) {
			if (Aircandi.isBetterLocation(lastKnownLocationNetwork, Aircandi.getInstance().getCurrentLocation())) {
				Logger.d(this, "Location stored: last known network location");
				Aircandi.getInstance().setCurrentLocation(lastKnownLocationNetwork);
			}
		}

		Location lastKnownLocationGPS = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (lastKnownLocationGPS != null && timeSinceLocationInMillis(lastKnownLocationGPS) >= CandiConstants.LOCATION_EXPIRATION) {
			if (Aircandi.isBetterLocation(lastKnownLocationGPS, Aircandi.getInstance().getCurrentLocation())) {
				Logger.d(this, "Location stored: last known GPS location");
				Aircandi.getInstance().setCurrentLocation(lastKnownLocationGPS);
			}
		}
	}

	public void startLocationUpdates(int scanDurationInMillis, int locationExpirationInMillis) {

		if (!mLocationScanActive.get()) {

			Location location = Aircandi.getInstance().getCurrentLocation();
			/*
			 * If a provider is disabled, we won't get any updates. If the user enables the provider
			 * during our time window, we will start getting updates.
			 */
			int locationAge = Aircandi.timeSinceLocationInMillis(location);
			if (location != null && locationAge >= locationExpirationInMillis) {
				mLocationScanActive.set(true);
				Logger.d(this, "Location scan started");
				Aircandi.getInstance().setCurrentLocation(null);
				Aircandi.getInstance().getLocationManager().requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,
						Aircandi.getInstance().getLocationListener());
				Aircandi.getInstance().getLocationManager().requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0,
						Aircandi.getInstance().getLocationListener());
				Aircandi.applicationHandler.postDelayed(mLocationScanRunnable, scanDurationInMillis);
			}
			else {
				Logger.d(this, "Location scan skipped: current location still good: " + String.valueOf(locationAge) + "ms");
			}
		}
	}

	public void stopLocationUpdates() {
		mLocationScanActive.set(false);
		Aircandi.applicationHandler.removeCallbacks(mLocationScanRunnable);
		mLocationManager.removeUpdates(Aircandi.getInstance().getLocationListener());
	}

	public static Boolean isDebugBuild(Context context) {
		/*
		 * Checks if this apk was built using the debug certificate
		 */
		if (mIsDebugBuild == null) {
			try {
				mIsDebugBuild = false;
				Signature[] sigs = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES).signatures;
				for (int i = 0; i < sigs.length; i++) {
					if (sigs[i].hashCode() == DEBUG_SIGNATURE_HASH) {
						Logger.d(applicationContext, "This is a debug build!");
						mIsDebugBuild = true;
						break;
					}
				}
			}
			catch (NameNotFoundException exception) {
				exception.printStackTrace();
			}
		}
		return mIsDebugBuild;
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

	/**
	 * Determines whether one Location reading is better than the current Location fix *
	 * 
	 * @param location The new Location that you want to evaluate
	 * @param currentBestLocation The current Location fix, to which you want to compare the new one
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

	public void setCurrentLocation(Location currentLocation) {
		this.mCurrentLocation = currentLocation;
	}

	public Location getCurrentLocation() {
		return mCurrentLocation;
	}

	public void setRebuildingDataModel(Boolean rebuildingDataModel) {
		this.mRebuildingDataModel = rebuildingDataModel;
	}

	public Boolean isRebuildingDataModel() {
		return mRebuildingDataModel;
	}

	public void setLocationManager(LocationManager locationManager) {
		this.mLocationManager = locationManager;
	}

	public LocationManager getLocationManager() {
		return mLocationManager;
	}

	public void setLocationListener(LocationListener locationListener) {
		this.mLocationListener = locationListener;
	}

	public LocationListener getLocationListener() {
		return mLocationListener;
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
