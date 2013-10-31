package com.aircandi.ui.base;

import java.lang.reflect.Field;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi;
import com.aircandi.BuildConfig;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.BusProvider;
import com.aircandi.components.BusyManager;
import com.aircandi.components.EntityManager;
import com.aircandi.components.FontManager;
import com.aircandi.components.Logger;
import com.aircandi.components.MenuManager;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.TrackerBase.TrackerCategory;
import com.aircandi.ui.SplashForm;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.Animate.TransitionType;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.Routing.Route;
import com.aircandi.utilities.UI;
import com.aircandi.utilities.Utilities;

public abstract class BaseActivity extends SherlockFragmentActivity implements BaseDelegate {

	protected ActionBar	mActionBar;
	protected String	mActivityTitle;

	protected Boolean	mPrefChangeNewSearchNeeded	= false;
	protected Boolean	mPrefChangeRefreshUiNeeded	= false;
	protected Boolean	mPrefChangeReloadNeeded		= false;

	public Resources	mResources;
	public BusyManager	mBusyManager;
	protected Handler	mHandler					= new Handler();
	protected Boolean	mFirstDraw					= true;

	/* Theme */
	private String		mPrefTheme;
	private String		mThemeTone;

	/* Menus */
	protected MenuItem	mMenuItemEdit;
	protected MenuItem	mMenuItemAdd;
	protected MenuItem	mMenuItemSignout;
	protected MenuItem	mMenuItemSignin;
	protected MenuItem	mMenuItemProfile;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (Aircandi.LAUNCHED_NORMALLY == null) {
			/*
			 * We restarting after a crash or after being killed by Android
			 */
			super.onCreate(null); // null prevents fragment AUTO creation
			Logger.d(this, "Aircandi not launched normally, routing to splash activity");
			Intent intent = new Intent(this, SplashForm.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			setResultCode(Activity.RESULT_CANCELED);
			startActivity(intent);
			finish();
		}
		else {
			/*
			 * We do all this here so the work is finished before subclasses start
			 * their create/initialize processing.
			 */
			mResources = getResources();
			mBusyManager = new BusyManager(this);
			/*
			 * Theme must be set before contentView is processed.
			 */
			setTheme(isDialog(), isTransparent());
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			super.setContentView(getLayoutId());
			super.onCreate(savedInstanceState);

			/* Stash the action bar */
			mActionBar = getSupportActionBar();

			/* Fonts */
			final Integer titleId = getActionBarTitleId();
			FontManager.getInstance().setTypefaceDefault((TextView) findViewById(titleId));

			/* Theme info */
			final TypedValue resourceName = new TypedValue();
			if (getTheme().resolveAttribute(R.attr.themeTone, resourceName, true)) {
				mThemeTone = (String) resourceName.coerceToString();
			}
		}
	}

	@Override
	public void unpackIntent() {}

	protected void configureActionBar() {
		try {
			ViewConfiguration config = ViewConfiguration.get(this);
			Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
			if (menuKeyField != null) {
				menuKeyField.setAccessible(true);
				menuKeyField.setBoolean(config, false);
			}
		}
		catch (Exception ex) {
			// Ignore
		}
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	public void onRefresh() {}

	public void onAccept() {}

	@Override
	public void onAdd() {}

	@Override
	public void onBackPressed() {
		Routing.route(this, Route.CANCEL);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Logger.d(this, "Configuration changed");
		super.onConfigurationChanged(newConfig);
	}

	@SuppressWarnings("ucd")
	public void onCancelButtonClick(View view) {
		Routing.route(this, Route.CANCEL);
	}

	public void onCancel(Boolean force) {
		setResultCode(Activity.RESULT_CANCELED);
		finish();
		Animate.doOverridePendingTransition(this, TransitionType.PAGE_BACK);
	}

	@Override
	public void onHelp() {}

	@Override
	public void showBusy() {
		showBusy(null, false);
	}

	@Override
	public void showBusy(final Object message, final Boolean actionBarOnly) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (mBusyManager != null) {
					mBusyManager.showBusy(message);
					if (actionBarOnly == null || !actionBarOnly) {
						mBusyManager.startBodyBusyIndicator();
					}
				}
			}
		});
	}

	@Override
	public void showBusyTimed(final Integer duration, final Boolean actionBarOnly) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (mBusyManager != null) {
					mBusyManager.showBusy();
					if (actionBarOnly == null || !actionBarOnly) {
						mBusyManager.startBodyBusyIndicator();
					}
					mHandler.postDelayed(new Runnable() {

						@Override
						public void run() {
							hideBusy();
						}
					}, duration);
				}
			}
		});
	}

	@Override
	public void hideBusy() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (mBusyManager != null) {
					mBusyManager.hideBusy();
					mBusyManager.stopBodyBusyIndicator();
				}
			}
		});
	};

	@Override
	public void onError() {}

	@Override
	public void onLowMemory() {
		Aircandi.tracker.sendEvent(TrackerCategory.SYSTEM, "memory_low", null, Utilities.getMemoryAvailable());
		super.onLowMemory();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		Aircandi.resultCode = Activity.RESULT_OK;
		Aircandi.resultIntent = null;
		super.onActivityResult(requestCode, resultCode, intent);
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

		if (!Aircandi.getInstance().getPrefTestingSelfNotify()
				.equals(Aircandi.settings.getBoolean(Constants.PREF_TESTING_SELF_NOTIFY, Constants.PREF_TESTING_SELF_NOTIFY_DEFAULT))) {
			Logger.d(this, "Pref change: testing self notify");
		}

		if (!Aircandi.getInstance().getPrefEntityFencing()
				.equals(Aircandi.settings.getBoolean(Constants.PREF_ENTITY_FENCING, Constants.PREF_ENTITY_FENCING_DEFAULT))) {
			mPrefChangeNewSearchNeeded = true;
			Logger.d(this, "Pref change: entity fencing");
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

		if (!Aircandi.getInstance().getPrefTestingPlaceProvider().equals(Aircandi.settings.getString(Constants.PREF_PLACE_PROVIDER,
				Constants.PREF_PLACE_PROVIDER_DEFAULT))) {
			mPrefChangeNewSearchNeeded = true;
			Logger.d(this, "Pref change: place provider");
		}

		if (mPrefChangeNewSearchNeeded || mPrefChangeRefreshUiNeeded || mPrefChangeReloadNeeded) {
			Aircandi.getInstance().snapshotPreferences();
		}
	}

	// --------------------------------------------------------------------------------------------
	// UI
	// --------------------------------------------------------------------------------------------

	public void setTheme(Boolean isDialog, Boolean isTransparent) {
		mPrefTheme = Aircandi.settings.getString(Constants.PREF_THEME, Constants.PREF_THEME_DEFAULT);
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

	public void setActivityTitle(String title) {
		mActivityTitle = title;
		mActionBar.setTitle(title);
	}

	public String getActivityTitle() {
		return (String) (mActivityTitle != null ? mActivityTitle : getTitle());
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	public static void signout(final Activity activity, final Boolean silent) {
		Runnable task = new Runnable() {

			@Override
			public void run() {
				new AsyncTask() {

					@Override
					protected void onPreExecute() {
						if (!silent) {
							((BaseActivity) activity).mBusyManager.showBusy(R.string.progress_signing_out);
						}
					}

					@Override
					protected Object doInBackground(Object... params) {
						Thread.currentThread().setName("SignOut");
						final ModelResult result = EntityManager.getInstance().signoutComplete();
						return result;
					}

					@SuppressLint("NewApi")
					@Override
					protected void onPostExecute(Object response) {
						if (!silent) {
							/* Notify interested parties */
							UI.showToastNotification(Aircandi.applicationContext.getString(R.string.toast_signed_out), Toast.LENGTH_SHORT);
							((BaseActivity) activity).hideBusy();
							Routing.route(activity, Route.SPLASH);
						}
					}
				}.execute();
			}
		};

		if (!silent && activity != null) {
			activity.runOnUiThread(task);
		}
	}

	private void clearReferences() {
		Activity currentActivity = Aircandi.getInstance().getCurrentActivity();
		if (currentActivity != null && currentActivity.equals(this))
			Aircandi.getInstance().setCurrentActivity(null);
	}

	public void setResultCode(int resultCode) {
		setResult(resultCode);
		Aircandi.resultCode = resultCode;
	}

	public void setResultCode(int resultCode, Intent intent) {
		setResult(resultCode, intent);
		Aircandi.resultCode = resultCode;
		Aircandi.resultIntent = intent;
	}

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		/*
		 * Android 2.3 or lower: called when user hits the menu button for the first time.
		 * Android 3.0 or higher: called when activity is first started.
		 * 
		 * Behavior might be modified because we are using ABS.
		 */
		Logger.d(this, "Creating options menu");
		return MenuManager.onCreateOptionsMenu(this, menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		/*
		 * Android 2.3 or lower: called every time the user hits the menu button.
		 * Android 3.0 or higher: called when invalidateOptionsMenu is called.
		 * 
		 * Behavior might be modified because we are using ABS.
		 */
		Logger.d(this, "Preparing options menu");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return Routing.route(this, Routing.routeForMenuId(item.getItemId()));
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onRestart() {
		Logger.d(this, "Activity restarting");
		super.onRestart();
		checkForPreferenceChanges();
	}

	@Override
	protected void onStart() {
		/* Called everytime the activity is started or restarted. */
		super.onStart();
		if (!isFinishing()) {
			Logger.d(this, "Activity starting");
			Aircandi.tracker.activityStart(this);

			/* Show current user */
			if (mActionBar != null
					&& Aircandi.getInstance().getCurrentUser() != null
					&& Aircandi.getInstance().getCurrentUser().name != null) {
				mActionBar.setSubtitle(Aircandi.getInstance().getCurrentUser().name.toUpperCase(Locale.US));
			}

			if (mPrefChangeReloadNeeded) {
				final Intent intent = getIntent();
				startActivity(intent);
				finish();
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		Logger.d(this, "Activity resuming");
		BusProvider.getInstance().register(this);
		Aircandi.getInstance().setCurrentActivity(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		Logger.d(this, "Activity pausing");
		BusProvider.getInstance().unregister(this);
		clearReferences();
	}

	@Override
	protected void onStop() {
		Logger.d(this, "Activity stopping");
		super.onStop();
		Aircandi.tracker.activityStop(this);
	}

	@Override
	protected void onDestroy() {
		/* This activity gets destroyed everytime we leave using back or finish(). */
		Logger.d(this, "Activity destroying");
		super.onDestroy();
		clearReferences();
	}

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		final Window window = getWindow();
		window.setFormat(PixelFormat.RGBA_8888);
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------

	public String getThemeTone() {
		return mThemeTone;
	}

	public void setThemeTone(String themeTone) {
		mThemeTone = themeTone;
	}

	public enum ServiceOperation {
		SIGNIN,
		PASSWORD_CHANGE,
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