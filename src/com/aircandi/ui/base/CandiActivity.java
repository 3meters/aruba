package com.aircandi.ui.base;

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
import com.aircandi.Aircandi;
import com.aircandi.CandiConstants;
import com.aircandi.R;
import com.aircandi.components.AircandiCommon;
import com.aircandi.components.Logger;
import com.aircandi.service.ProxibaseService.RequestListener;
import com.aircandi.ui.Preferences;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;

public abstract class CandiActivity extends SherlockActivity {

	protected AircandiCommon	mCommon;
	private AlertDialog			mUpdateAlertDialog;
	private AlertDialog			mWifiAlertDialog;
	protected Boolean			mPrefChangeNewSearchNeeded	= false;
	protected Boolean			mPrefChangeRefreshUiNeeded	= false;

	public boolean				mPrefSoundEffects			= true;
	private String				mPrefSearchRadius;
	private String				mPrefTestingBeacons;
	private String				mPrefTestingLocation;
	private String				mPrefTestingPlaceProvider;
	private Boolean				mPrefEnableDev;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Logger.d(this, "Launched normally: " + String.valueOf(Aircandi.getInstance().wasLaunchedNormally()));
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

			mPrefSearchRadius = Aircandi.settings.getString(Preferences.PREF_SEARCH_RADIUS, Preferences.PREF_SEARCH_RADIUS_DEFAULT);
			mPrefTestingBeacons = Aircandi.settings.getString(Preferences.PREF_TESTING_BEACONS, Preferences.PREF_TESTING_BEACONS_DEFAULT);
			mPrefTestingLocation = Aircandi.settings.getString(Preferences.PREF_TESTING_LOCATION, Preferences.PREF_TESTING_LOCATION_DEFAULT);
			mPrefTestingPlaceProvider = Aircandi.settings.getString(Preferences.PREF_TESTING_PLACE_PROVIDER, Preferences.PREF_TESTING_PLACE_PROVIDER_DEFAULT);
			mPrefEnableDev = Aircandi.settings.getBoolean(Preferences.PREF_ENABLE_DEV, Preferences.PREF_ENABLE_DEV_DEFAULT);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Events routines
	// --------------------------------------------------------------------------------------------

	@Override
	public void onBackPressed() {
		/* Activity is destroyed */
		super.onBackPressed();
		AnimUtils.doOverridePendingTransition(this, TransitionType.PageBack);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Logger.d(this, "Configuration changed");
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

	private void showUpdateNotification() {
		final Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
		intent.setData(Uri.parse(Aircandi.applicationUpdateUri));
		mCommon.showNotification(getString(R.string.alert_upgrade_title)
				, getString(Aircandi.applicationUpdateRequired ? R.string.alert_upgrade_required_body : R.string.alert_upgrade_needed_body)
				, this
				, intent
				, CandiConstants.NOTIFICATION_UPDATE);
	}

	private void showUpdateAlertDialog(final RequestListener listener) {
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

								@Override
								public void onClick(DialogInterface dialog, int which) {
									if (which == Dialog.BUTTON_POSITIVE) {
										Logger.d(this, "Update check: navigating to install page");
										final Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
										intent.setData(Uri.parse(Aircandi.applicationUpdateUri));
										startActivity(intent);
										AnimUtils.doOverridePendingTransition(CandiActivity.this, TransitionType.PageToSource);
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
											listener.onComplete(true);
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
										listener.onComplete(true);
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

								@Override
								public void onClick(DialogInterface dialog, int which) {
									if (which == Dialog.BUTTON_POSITIVE) {
										Logger.d(this, "Wifi check: navigating to wifi settings");
										startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
										AnimUtils.doOverridePendingTransition(CandiActivity.this, TransitionType.PageToForm);
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

		if (!mCommon.mPrefTheme.equals(Aircandi.settings.getString(Preferences.PREF_THEME, Preferences.PREF_THEME_DEFAULT))) {
			Logger.d(this, "Pref change: theme, restarting current activity");
			mCommon.mPrefTheme = Aircandi.settings.getString(Preferences.PREF_THEME, Preferences.PREF_THEME_DEFAULT);
			if (!firstUpdate) {
				mCommon.reload();
			}
			return;
		}

		mPrefChangeNewSearchNeeded = false;
		mPrefChangeRefreshUiNeeded = false;

		if (!mPrefSearchRadius.equals(Aircandi.settings.getString(Preferences.PREF_SEARCH_RADIUS, Preferences.PREF_SEARCH_RADIUS_DEFAULT))) {
			mPrefChangeNewSearchNeeded = true;
			Logger.d(this, "Pref change: search radius");
			mPrefSearchRadius = Aircandi.settings.getString(Preferences.PREF_SEARCH_RADIUS, Preferences.PREF_SEARCH_RADIUS_DEFAULT);
		}

		/* Dev prefs */

		if (!mPrefTestingBeacons.equals(Aircandi.settings.getString(Preferences.PREF_TESTING_BEACONS, Preferences.PREF_TESTING_BEACONS_DEFAULT))) {
			mPrefChangeNewSearchNeeded = true;
			Logger.d(this, "Pref change: testing beacons");
			mPrefTestingBeacons = Aircandi.settings.getString(Preferences.PREF_TESTING_BEACONS, Preferences.PREF_TESTING_BEACONS_DEFAULT);
		}

		if (!mPrefTestingLocation.equals(Aircandi.settings.getString(Preferences.PREF_TESTING_LOCATION, Preferences.PREF_TESTING_LOCATION_DEFAULT))) {
			mPrefChangeNewSearchNeeded = true;
			Logger.d(this, "Pref change: testing location");
			mPrefTestingLocation = Aircandi.settings.getString(Preferences.PREF_TESTING_LOCATION, Preferences.PREF_TESTING_LOCATION_DEFAULT);
		}

		if (!mPrefTestingPlaceProvider.equals(Aircandi.settings.getString(Preferences.PREF_TESTING_PLACE_PROVIDER,
				Preferences.PREF_TESTING_PLACE_PROVIDER_DEFAULT))) {
			mPrefChangeNewSearchNeeded = true;
			Logger.d(this, "Pref change: place provider");
			mPrefTestingPlaceProvider = Aircandi.settings.getString(Preferences.PREF_TESTING_PLACE_PROVIDER, Preferences.PREF_TESTING_PLACE_PROVIDER_DEFAULT);
		}

		if (!mPrefEnableDev.equals(Aircandi.settings.getBoolean(Preferences.PREF_ENABLE_DEV, Preferences.PREF_ENABLE_DEV_DEFAULT))) {
			mPrefChangeRefreshUiNeeded = true;
			Logger.d(this, "Pref change: dev ui");
			mPrefEnableDev = Aircandi.settings.getBoolean(Preferences.PREF_ENABLE_DEV, Preferences.PREF_ENABLE_DEV_DEFAULT);
		}

		if (firstUpdate) {
			mPrefChangeNewSearchNeeded = false;
			mPrefChangeRefreshUiNeeded = false;
		}

		mPrefSoundEffects = Aircandi.settings.getBoolean(Preferences.PREF_SOUND_EFFECTS, true);
	}

	// --------------------------------------------------------------------------------------------
	// Application menu routines (settings)
	// --------------------------------------------------------------------------------------------

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		mCommon.doCreateOptionsMenu(menu);
		return true;
	}

	@Override
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

	@Override
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