package com.aircandi;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.aircandi.components.AircandiCommon;
import com.aircandi.components.Logger;
import com.aircandi.service.ProxibaseService.RequestListener;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;

public abstract class CandiActivity extends SherlockActivity {

	protected int				mLastResultCode				= Activity.RESULT_OK;
	protected AircandiCommon	mCommon;
	protected AlertDialog		mUpdateAlertDialog;
	protected AlertDialog		mWifiAlertDialog;
	protected Boolean			mPrefChangeRefreshNeeded	= false;

	/* We use these to track whether a preference gets changed */
	public boolean				mPrefDemoMode				= false;
	public boolean				mPrefGlobalBeacons			= true;
	public boolean				mPrefEntityFencing			= true;
	public boolean				mPrefSoundEffects			= true;
	public String				mPrefTestingBeacons			= "natural";
	public String				mPrefTestingLocation		= "natural";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (!Aircandi.getInstance().wasLaunchedNormally()) {
			/* Try to detect case where this is being created after a crash and bail out. */
			super.onCreate(savedInstanceState);
			finish();
		}
		else {
			mCommon = new AircandiCommon(this, savedInstanceState);
			mCommon.setTheme(null, false);
			mCommon.unpackIntent();
			setContentView(getLayoutId());
			super.onCreate(savedInstanceState);
			mCommon.initialize();

			Logger.d(this, "Started from radar flag: " + String.valueOf(Aircandi.getInstance().wasLaunchedNormally()));
		}
	}

	// --------------------------------------------------------------------------------------------
	// Events routines
	// --------------------------------------------------------------------------------------------

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageBack);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Logger.d(this, "Configuration changed");
		mCommon.mConfigChange = true;
		super.onConfigurationChanged(newConfig);
	}

	protected static void setVisibility(View view, Integer visibility) {
		if (view != null) {
			view.setVisibility(visibility);
		}
	}

	public void showUpdateAlert(RequestListener listener) {
		showUpdateNotification();
		showUpdateAlertDialog(listener);
	}

	public void showUpdateNotification() {
		Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
		intent.setData(Uri.parse(Aircandi.applicationUpdateUri));
		mCommon.showNotification(getString(R.string.alert_upgrade_title)
				, getString(Aircandi.applicationUpdateRequired ? R.string.alert_upgrade_required_body : R.string.alert_upgrade_needed_body)
				, this
				, intent
				, CandiConstants.NOTIFICATION_UPDATE);
	}

	public void showUpdateAlertDialog(final RequestListener listener) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (mUpdateAlertDialog == null || !mUpdateAlertDialog.isShowing()) {
					mUpdateAlertDialog = AircandiCommon.showAlertDialog(R.drawable.ic_app
							, getString(R.string.alert_upgrade_title)
							, getString(Aircandi.applicationUpdateRequired ? R.string.alert_upgrade_required_body
									: R.string.alert_upgrade_needed_body)
							, null
							, CandiActivity.this
							, R.string.alert_upgrade_ok
							, R.string.alert_upgrade_cancel
							, new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog, int which) {
									if (which == Dialog.BUTTON_POSITIVE) {
										Logger.d(this, "Update check: navigating to install page");
										Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
										intent.setData(Uri.parse(Aircandi.applicationUpdateUri));
										startActivity(intent);
										AnimUtils.doOverridePendingTransition(CandiActivity.this, TransitionType.CandiPageToForm);
									}
									else if (which == Dialog.BUTTON_NEGATIVE) {
										/*
										 * We don't continue running if user doesn't install a required
										 * update
										 */
										if (Aircandi.applicationUpdateRequired) {
											Logger.d(this, "Update check: user declined");
											finish();
										}
										if (listener != null) {
											listener.onComplete();
										}
									}
								}
							}
							, new DialogInterface.OnCancelListener() {

								@Override
								public void onCancel(DialogInterface dialog) {
									/* Back button can trigger this */
									if (Aircandi.applicationUpdateRequired) {
										Logger.d(this, "Update check: user canceled");
										finish();
									}
									if (listener != null) {
										listener.onComplete();
									}
								}
							});
					mUpdateAlertDialog.setCanceledOnTouchOutside(false);
				}
			}
		});
	}

	public void showWifiAlertDialog(final Integer messageResId, final RequestListener listener) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (mWifiAlertDialog == null || !mWifiAlertDialog.isShowing()) {
					mWifiAlertDialog = AircandiCommon.showAlertDialog(R.drawable.ic_app
							, getString(R.string.alert_wifi_title)
							, getString(messageResId)
							, null
							, CandiActivity.this
							, R.string.alert_wifi_settings
							, R.string.alert_wifi_cancel
							, new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog, int which) {
									if (which == Dialog.BUTTON_POSITIVE) {
										Logger.d(this, "Wifi check: navigating to wifi settings");
										startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
										AnimUtils.doOverridePendingTransition(CandiActivity.this, TransitionType.CandiPageToForm);
									}
									else if (which == Dialog.BUTTON_NEGATIVE) {
										Logger.d(this, "Wifi check: user declined");
										if (listener != null) {
											listener.onComplete();
										}
									}
								}
							}
							, new DialogInterface.OnCancelListener() {

								@Override
								public void onCancel(DialogInterface dialog) {
									/* Back button can trigger this */
									if (listener != null) {
										listener.onComplete();
									}
								}
							});
					mWifiAlertDialog.setCanceledOnTouchOutside(false);
				}
			}
		});
	}

	public void updatePreferences(Boolean firstUpdate) {

		if (!mCommon.mPrefTheme.equals(Aircandi.settings.getString(Preferences.PREF_THEME, CandiConstants.THEME_DEFAULT))) {
			Logger.d(this, "Pref change: theme, restarting current activity");
			mCommon.mPrefTheme = Aircandi.settings.getString(Preferences.PREF_THEME, CandiConstants.THEME_DEFAULT);
			if (!firstUpdate) {
				mCommon.reload();
			}
			return;
		}

		mPrefChangeRefreshNeeded = false;

		if (mPrefTestingBeacons != Aircandi.settings.getString(Preferences.PREF_TESTING_BEACONS, "natural")) {
			mPrefChangeRefreshNeeded = true;
			Logger.d(this, "Pref change: testing beacons");
			mPrefTestingBeacons = Aircandi.settings.getString(Preferences.PREF_TESTING_BEACONS, "natural");
		}

		if (mPrefTestingLocation != Aircandi.settings.getString(Preferences.PREF_TESTING_LOCATION, "natural")) {
			mPrefChangeRefreshNeeded = true;
			Logger.d(this, "Pref change: testing location");
			mPrefTestingLocation = Aircandi.settings.getString(Preferences.PREF_TESTING_LOCATION, "natural");
		}

		if (mPrefGlobalBeacons != Aircandi.settings.getBoolean(Preferences.PREF_GLOBAL_BEACONS, true)) {
			mPrefChangeRefreshNeeded = true;
			Logger.d(this, "Pref change: global beacons");
			mPrefGlobalBeacons = Aircandi.settings.getBoolean(Preferences.PREF_GLOBAL_BEACONS, true);
		}

		if (firstUpdate) {
			mPrefChangeRefreshNeeded = false;
		}

		mPrefSoundEffects = Aircandi.settings.getBoolean(Preferences.PREF_SOUND_EFFECTS, true);
	}

	// --------------------------------------------------------------------------------------------
	// Application menu routines (settings)
	// --------------------------------------------------------------------------------------------

	public boolean onCreateOptionsMenu(Menu menu) {
		mCommon.doCreateOptionsMenu(menu);
		return true;
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		mCommon.doPrepareOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		mCommon.doOptionsItemSelected(item);
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onRestart() {
		Logger.d(this, "CandiActivity restarting");
		super.onRestart();
		updatePreferences(false);
	}

	@Override
	protected void onResume() {
		mCommon.doResume();
		super.onResume();
	}

	@Override
	protected void onStop() {
		super.onStop();
		mCommon.doStop();
	}

	@Override
	protected void onStart() {
		super.onStart();
		mCommon.doStart();
	}

	@Override
	protected void onPause() {
		mCommon.doPause();
		super.onPause();
	}

	protected int getLayoutId() {
		return 0;
	}

	protected void onDestroy() {
		/* This activity gets destroyed everytime we leave using back or finish(). */
		super.onDestroy();
		if (mCommon != null) {
			mCommon.doDestroy();
		}
		Logger.d(this, "CandiActivity destroyed");
	}

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		mCommon.doAttachedToWindow();
	}

}