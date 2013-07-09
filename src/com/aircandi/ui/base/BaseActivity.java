package com.aircandi.ui.base;

import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.beta.BuildConfig;
import com.aircandi.beta.R;
import com.aircandi.components.BusProvider;
import com.aircandi.components.BusyManager;
import com.aircandi.components.EntityManager;
import com.aircandi.components.FontManager;
import com.aircandi.components.GCMIntentService;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NotificationManager;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.Tracker;
import com.aircandi.ui.Preferences;
import com.aircandi.ui.RadarForm;
import com.aircandi.ui.SplashForm;
import com.aircandi.ui.edit.FeedbackEdit;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.Animate.TransitionType;
import com.aircandi.utilities.UI;
import com.google.android.gcm.GCMRegistrar;

public abstract class BaseActivity extends SherlockActivity {

	protected ActionBar				mActionBar;
	public BusyManager				mBusyManager;
	protected String				mPageName;

	protected Boolean				mMuteColor;
	protected Boolean				mPrefChangeNewSearchNeeded	= false;
	protected Boolean				mPrefChangeRefreshUiNeeded	= false;
	protected Boolean				mPrefChangeReloadNeeded		= false;
	protected static LayoutInflater	mInflater;
	protected static Resources		mResources;

	/* Theme */
	public String					mPrefTheme;
	public String					mThemeTone;
	protected Boolean				mIsDialog;

	/* Menus */
	protected MenuItem				mMenuItemRefresh;
	protected MenuItem				mMenuItemEdit;
	protected MenuItem				mMenuItemDelete;
	protected MenuItem				mMenuItemAdd;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
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
			/*
			 * We do all this here so the work is finished before subclasses start
			 * their create/initialize processing.
			 */
			unpackIntent();

			mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mResources = getResources();
			/*
			 * Theme must be set before contentView is processed.
			 */
			setTheme(isDialog(), isTransparent());
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			super.setContentView(getLayoutId());
			super.onCreate(savedInstanceState);

			/* Stash the action bar */
			mActionBar = getSupportActionBar();
			mPageName = getClass().getSimpleName();

			/* Fonts */
			final Integer titleId = getActionBarTitleId();
			FontManager.getInstance().setTypefaceDefault((TextView) findViewById(titleId));

			/* Make sure we have successfully registered this device with aircandi service */
			/* FIXME: This might be getting called too often */
			NotificationManager.getInstance().registerDeviceWithAircandi();

			/* Theme info */
			final TypedValue resourceName = new TypedValue();
			if (getTheme().resolveAttribute(R.attr.themeTone, resourceName, true)) {
				mThemeTone = (String) resourceName.coerceToString();
			}

			/* Default sizing if this is a dialog */
			if (mIsDialog) {
				setDialogSize(getResources().getConfiguration());
			}
			mMuteColor = android.os.Build.MODEL.toLowerCase(Locale.US).equals("nexus s"); // nexus 4, nexus 7 are others
		}
	}

	protected void unpackIntent() {}

	// --------------------------------------------------------------------------------------------
	// Events routines
	// --------------------------------------------------------------------------------------------

	@Override
	public void onBackPressed() {
		/* Activity is destroyed */
		setResult(Activity.RESULT_CANCELED);
		super.onBackPressed();
		Animate.doOverridePendingTransition(this, TransitionType.PageBack);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Logger.d(this, "Configuration changed");
		if (mIsDialog) {
			setDialogSize(newConfig);
		}
		super.onConfigurationChanged(newConfig);
	}

	public void onCancelButtonClick(View view) {
		setResult(Activity.RESULT_CANCELED);
		finish();
		Animate.doOverridePendingTransition(this, TransitionType.PageBack);
	}

	// --------------------------------------------------------------------------------------------
	// Preferences
	// --------------------------------------------------------------------------------------------

	public void checkForPreferenceChanges() {

		mPrefChangeNewSearchNeeded = false;
		mPrefChangeRefreshUiNeeded = false;
		mPrefChangeReloadNeeded = false;

		/* Common prefs */

		if (!mPrefTheme.equals(Aircandi.settings.getString(Constants.PREF_THEME, Constants.PREF_THEME_DEFAULT))) {
			Logger.d(this, "Pref change: theme, restarting current activity");
			mPrefChangeReloadNeeded = true;
		}

		if (!Aircandi.getInstance().getPrefSearchRadius()
				.equals(Aircandi.settings.getString(Constants.PREF_SEARCH_RADIUS, Constants.PREF_SEARCH_RADIUS_DEFAULT))) {
			mPrefChangeNewSearchNeeded = true;
			Logger.d(this, "Pref change: search radius");
		}

		/* Dev prefs */

		if (!Aircandi.getInstance().getPrefEnableDev()
				.equals(Aircandi.settings.getBoolean(Constants.PREF_ENABLE_DEV, Constants.PREF_ENABLE_DEV_DEFAULT))) {
			mPrefChangeRefreshUiNeeded = true;
			Logger.d(this, "Pref change: dev ui");
		}

		if (!Aircandi.getInstance().getPrefEntityFencing()
				.equals(Aircandi.settings.getBoolean(Constants.PREF_ENTITY_FENCING, Constants.PREF_ENTITY_FENCING_DEFAULT))) {
			mPrefChangeNewSearchNeeded = true;
			Logger.d(this, "Pref change: entity fencing");
		}

		if (!Aircandi.getInstance().getPrefShowPlaceRankScore().equals(Aircandi.settings.getBoolean(Constants.PREF_SHOW_PLACE_RANK_SCORE,
				Constants.PREF_SHOW_PLACE_RANK_SCORE_DEFAULT))) {
			mPrefChangeRefreshUiNeeded = true;
			Logger.d(this, "Pref change: place rank score");
		}

		if (!Aircandi.getInstance().getPrefTestingBeacons()
				.equals(Aircandi.settings.getString(Constants.PREF_TESTING_BEACONS, Constants.PREF_TESTING_BEACONS_DEFAULT))) {
			mPrefChangeNewSearchNeeded = true;
			Logger.d(this, "Pref change: testing beacons");
		}

		if (!Aircandi.getInstance().getPrefTestingLocation().equals(Aircandi.settings
				.getString(Constants.PREF_TESTING_LOCATION, Constants.PREF_TESTING_LOCATION_DEFAULT))) {
			mPrefChangeNewSearchNeeded = true;
			Logger.d(this, "Pref change: testing location");
		}

		if (!Aircandi.getInstance().getPrefTestingPlaceProvider().equals(Aircandi.settings.getString(Constants.PREF_TESTING_PLACE_PROVIDER,
				Constants.PREF_TESTING_PLACE_PROVIDER_DEFAULT))) {
			mPrefChangeNewSearchNeeded = true;
			Logger.d(this, "Pref change: place provider");
		}

		if (mPrefChangeNewSearchNeeded || mPrefChangeRefreshUiNeeded || mPrefChangeReloadNeeded) {
			Aircandi.getInstance().snapshotPreferences();
		}
	}

	// --------------------------------------------------------------------------------------------
	// General Ui routines
	// --------------------------------------------------------------------------------------------

	public void setTheme(Boolean isDialog, Boolean isTransparent) {
		mPrefTheme = Aircandi.settings.getString(Constants.PREF_THEME, Constants.PREF_THEME_DEFAULT);
		mIsDialog = isDialog;
		/*
		 * ActionBarSherlock takes over the title area if version < 4.0 (Ice Cream Sandwich).
		 */
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			requestWindowFeature(Window.FEATURE_NO_TITLE);
		}
		/*
		 * Need to use application context so our app level themes and attributes are available to actionbarsherlock
		 */
		Integer themeId = getApplicationContext().getResources().getIdentifier(mPrefTheme, "style", getPackageName());
		if (isDialog) {
			themeId = R.style.aircandi_theme_dialog_dark;
			if (mPrefTheme.equals("aircandi_theme_snow")) {
				themeId = R.style.aircandi_theme_dialog_light;
			}
		}
		else if (isTransparent) {
			themeId = R.style.aircandi_theme_midnight_transparent;
			if (mPrefTheme.equals("aircandi_theme_snow")) {
				themeId = R.style.aircandi_theme_snow_transparent;
			}
		}

		setTheme(themeId);
	}

	protected Boolean isDialog() {
		return false;
	}

	@SuppressLint("NewApi")
	private void setDialogSize(Configuration newConfig) {

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB_MR2) {
			//			final android.view.WindowManager.LayoutParams params = mActivity.getWindow().getAttributes();
			//			final int height = Math.min(newConfig.screenHeightDp, 450);
			//			final int width = Math.min(newConfig.screenWidthDp, 350);
			//			params.height = ImageUtils.getRawPixels(mActivity, height);
			//			params.width = ImageUtils.getRawPixels(mActivity, width);
			//			mActivity.getWindow().setAttributes(params);
		}
	}

	protected Boolean isTransparent() {
		return false;
	}

	protected int getLayoutId() {
		return 0;
	}

	public int getActionBarTitleId() {
		Integer actionBarTitleId = null;
		try {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
				actionBarTitleId = Class.forName("com.actionbarsherlock.R$id").getField("abs__action_bar_title").getInt(null);
			}
			else {
				// Use reflection to get the actionbar title TextView and set the custom font. May break in updates.
				actionBarTitleId = Class.forName("com.android.internal.R$id").getField("action_bar_title").getInt(null);
			}
		}
		catch (Exception e) {
			if (BuildConfig.DEBUG) {
				e.printStackTrace();
			}
		}
		return actionBarTitleId;
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	public void signout() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				new AsyncTask() {

					@Override
					protected void onPreExecute() {
						if (mBusyManager != null) {
							mBusyManager.showBusy(R.string.progress_signing_out);
						}
					}

					@Override
					protected Object doInBackground(Object... params) {
						Thread.currentThread().setName("SignOut");
						final ModelResult result = EntityManager.getInstance().signout();
						return result;
					}

					@SuppressLint("NewApi")
					@Override
					protected void onPostExecute(Object response) {
						final ModelResult result = (ModelResult) response;
						/* We continue on even if the service call failed. */
						if (result.serviceResponse.responseCode == ResponseCode.Success) {
							Logger.i(this, "User signed out: " + Aircandi.getInstance().getUser().name + " (" + Aircandi.getInstance().getUser().id + ")");
						}
						else {
							Logger.w(this, "User signed out, service call failed: " + Aircandi.getInstance().getUser().id);
						}

						/* Stop the current tracking session. Starts again when a user logs in. */
						Tracker.stopSession(Aircandi.getInstance().getUser());

						/* Clear the user and session that is tied into auto-signin */
						com.aircandi.components.NotificationManager.getInstance().unregisterDeviceWithAircandi(
								GCMRegistrar.getRegistrationId(Aircandi.applicationContext));
						Aircandi.getInstance().setUser(null);

						Aircandi.settingsEditor.putString(Constants.SETTING_USER, null);
						Aircandi.settingsEditor.putString(Constants.SETTING_USER_SESSION, null);
						Aircandi.settingsEditor.commit();

						/* Make sure onPrepareOptionsMenu gets called */
						invalidateOptionsMenu();

						/* Notify interested parties */
						UI.showToastNotification(getString(R.string.toast_signed_out), Toast.LENGTH_SHORT);
						if (mBusyManager != null) {
							mBusyManager.hideBusy();
						}
						final Intent intent = new Intent(BaseActivity.this, SplashForm.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						startActivity(intent);
						finish();
						Animate.doOverridePendingTransition(BaseActivity.this, TransitionType.FormToPage);
					}
				}.execute();

			}
		});
	}

	// --------------------------------------------------------------------------------------------
	// Application menu routines (settings)
	// --------------------------------------------------------------------------------------------

	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		Intent intent = null;

		switch (menuItem.getItemId()) {
			case android.R.id.home:
				onBackPressed();
				return true;
			case R.id.home:
				intent = new Intent(this, RadarForm.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				finish();
				startActivity(intent);
				Animate.doOverridePendingTransition(this, TransitionType.PageToPage);
				return true;
			case R.id.settings:
				startActivityForResult(new Intent(this, Preferences.class), Constants.ACTIVITY_PREFERENCES);
				Animate.doOverridePendingTransition(this, TransitionType.PageToForm);
				return true;
			case R.id.signout:
				Tracker.sendEvent("ui_action", "signout_user", null, 0, Aircandi.getInstance().getUser());
				signout();
				return true;
			case R.id.feedback:
				final IntentBuilder intentBuilder = new IntentBuilder(this, FeedbackEdit.class);
				intent = intentBuilder.create();
				startActivity(intent);
				Animate.doOverridePendingTransition(this, TransitionType.PageToForm);
				return true;
			case R.id.cancel:
				setResult(Activity.RESULT_CANCELED);
				finish();
				Animate.doOverridePendingTransition(this, TransitionType.FormToPage);
				return true;
			case R.id.cancel_help:
				setResult(Activity.RESULT_CANCELED);
				finish();
				Animate.doOverridePendingTransition(this, TransitionType.HelpToPage);
				return true;
			default:
				return true;
		}
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
	protected void onStart() {
		super.onStart();
		Tracker.activityStart(this, Aircandi.getInstance().getUser());
		if (mPrefChangeReloadNeeded) {
			final Intent intent = getIntent();
			startActivity(intent);
			finish();
			return;
		}
	}

	@Override
	protected void onResume() {
		BusProvider.getInstance().register(this);
		synchronized (GCMIntentService.lock) {
			GCMIntentService.currentActivity = this;
		}
		super.onResume();
	}

	@Override
	protected void onPause() {
		BusProvider.getInstance().unregister(this);
		synchronized (GCMIntentService.lock) {
			GCMIntentService.currentActivity = null;
		}
		super.onPause();
	}

	@Override
	protected void onStop() {
		Tracker.activityStop(this, Aircandi.getInstance().getUser());
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		/* This activity gets destroyed everytime we leave using back or finish(). */
		Logger.d(this, "onDestroy called");
		super.onDestroy();
	}

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		final Window window = getWindow();
		window.setFormat(PixelFormat.RGBA_8888);
	}

	// --------------------------------------------------------------------------------------------
	// Inner classes and enums
	// --------------------------------------------------------------------------------------------
	public enum ServiceOperation {
		Signin,
		PasswordChange,
	}

	public static class SimpleTextWatcher implements TextWatcher {

		@Override
		public void afterTextChanged(Editable s) {}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {}
	}
}