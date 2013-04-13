package com.aircandi.ui.base;

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
import com.aircandi.Aircandi;
import com.aircandi.CandiConstants;
import com.aircandi.beta.R;
import com.aircandi.components.AircandiCommon;
import com.aircandi.components.Logger;
import com.aircandi.components.NotificationManager;
import com.aircandi.service.HttpService.RequestListener;
import com.aircandi.service.objects.AirNotification;
import com.aircandi.ui.SplashForm;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;

public abstract class CandiActivity extends SherlockActivity {

	private AircandiCommon	mCommon;
	private AlertDialog		mUpdateAlertDialog;
	private AlertDialog		mWifiAlertDialog;
	protected Boolean		mPrefChangeNewSearchNeeded	= false;
	protected Boolean		mPrefChangeRefreshUiNeeded	= false;
	protected Boolean		mPrefChangeReloadNeeded		= false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (Aircandi.LAUNCHED_NORMALLY == null) {
			/*
			 * We restarting after a crash or after being killed by Android
			 */
			super.onCreate(savedInstanceState);
			Logger.d(this, "Aircandi not launched normally, routing to splash activity");
			Intent intent = new Intent(this, SplashForm.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			setResult(Activity.RESULT_CANCELED);
			startActivity(intent);
			finish();
		}
		else {
			mCommon = new AircandiCommon(this, savedInstanceState);
			mCommon.setTheme(null, false);
			mCommon.unpackIntent();
			setContentView(getLayoutId());
			super.onCreate(savedInstanceState);
			mCommon.initialize();
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
		AirNotification airNotification = new AirNotification();
		airNotification.title = getString(R.string.alert_upgrade_title);
		airNotification.subtitle = getString(Aircandi.applicationUpdateRequired ? R.string.alert_upgrade_required_body : R.string.alert_upgrade_needed_body);
		airNotification.intent = intent;
		airNotification.type = "update";
		NotificationManager.getInstance().showNotification(airNotification, this);
	}

	private void showUpdateAlertDialog(final RequestListener listener) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (mUpdateAlertDialog == null || !mUpdateAlertDialog.isShowing()) {
					mUpdateAlertDialog = AircandiCommon.showAlertDialog(R.drawable.ic_launcher
							, getString(R.string.alert_upgrade_title)
							, getString(Aircandi.applicationUpdateRequired ? R.string.alert_upgrade_required_body
									: R.string.alert_upgrade_needed_body)
							, null
							, CandiActivity.this
							, R.string.alert_upgrade_ok
							, R.string.alert_upgrade_cancel
							, null
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
					mWifiAlertDialog = AircandiCommon.showAlertDialog(R.drawable.ic_launcher
							, getString(R.string.alert_wifi_title)
							, getString(messageResId)
							, null
							, CandiActivity.this
							, R.string.alert_wifi_settings
							, R.string.alert_wifi_cancel
							, null
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

	public void checkForPreferenceChanges() {

		mPrefChangeNewSearchNeeded = false;
		mPrefChangeRefreshUiNeeded = false;
		mPrefChangeReloadNeeded = false;

		/* Common prefs */

		if (!mCommon.mPrefTheme.equals(Aircandi.settings.getString(CandiConstants.PREF_THEME, CandiConstants.PREF_THEME_DEFAULT))) {
			Logger.d(this, "Pref change: theme, restarting current activity");
			mPrefChangeReloadNeeded = true;
		}

		if (!Aircandi.getInstance().getPrefSearchRadius()
				.equals(Aircandi.settings.getString(CandiConstants.PREF_SEARCH_RADIUS, CandiConstants.PREF_SEARCH_RADIUS_DEFAULT))) {
			mPrefChangeNewSearchNeeded = true;
			Logger.d(this, "Pref change: search radius");
		}

		/* Dev prefs */

		if (!Aircandi.getInstance().getPrefEnableDev()
				.equals(Aircandi.settings.getBoolean(CandiConstants.PREF_ENABLE_DEV, CandiConstants.PREF_ENABLE_DEV_DEFAULT))) {
			mPrefChangeRefreshUiNeeded = true;
			Logger.d(this, "Pref change: dev ui");
		}

		if (!Aircandi.getInstance().getPrefEntityFencing()
				.equals(Aircandi.settings.getBoolean(CandiConstants.PREF_ENTITY_FENCING, CandiConstants.PREF_ENTITY_FENCING_DEFAULT))) {
			mPrefChangeNewSearchNeeded = true;
			Logger.d(this, "Pref change: entity fencing");
		}

		if (!Aircandi.getInstance().getPrefShowPlaceRankScore().equals(Aircandi.settings.getBoolean(CandiConstants.PREF_SHOW_PLACE_RANK_SCORE,
				CandiConstants.PREF_SHOW_PLACE_RANK_SCORE_DEFAULT))) {
			mPrefChangeRefreshUiNeeded = true;
			Logger.d(this, "Pref change: place rank score");
		}

		if (!Aircandi.getInstance().getPrefTestingBeacons()
				.equals(Aircandi.settings.getString(CandiConstants.PREF_TESTING_BEACONS, CandiConstants.PREF_TESTING_BEACONS_DEFAULT))) {
			mPrefChangeNewSearchNeeded = true;
			Logger.d(this, "Pref change: testing beacons");
		}

		if (!Aircandi.getInstance().getPrefTestingLocation().equals(Aircandi.settings
				.getString(CandiConstants.PREF_TESTING_LOCATION, CandiConstants.PREF_TESTING_LOCATION_DEFAULT))) {
			mPrefChangeNewSearchNeeded = true;
			Logger.d(this, "Pref change: testing location");
		}

		if (!Aircandi.getInstance().getPrefTestingPlaceProvider().equals(Aircandi.settings.getString(CandiConstants.PREF_TESTING_PLACE_PROVIDER,
				CandiConstants.PREF_TESTING_PLACE_PROVIDER_DEFAULT))) {
			mPrefChangeNewSearchNeeded = true;
			Logger.d(this, "Pref change: place provider");
		}

		if (mPrefChangeNewSearchNeeded || mPrefChangeRefreshUiNeeded || mPrefChangeReloadNeeded) {
			Aircandi.getInstance().snapshotPreferences();
		}
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
		checkForPreferenceChanges();
	}

	@Override
	protected void onResume() {
		mCommon.doResume();
		super.onResume();
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (mCommon != null) {
			mCommon.doStop();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		mCommon.doStart();
		if (mPrefChangeReloadNeeded) {
			final Intent intent = getIntent();
			startActivity(intent);
			finish();
			return;
		}
	}

	@Override
	protected void onPause() {
		if (mCommon != null) {
			mCommon.doPause();
		}
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

	public AircandiCommon getCommon() {
		return mCommon;
	}

	public void setCommon(AircandiCommon common) {
		mCommon = common;
	}

}