package com.aircandi.ui;

import java.util.Locale;

import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.View;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.Exceptions;
import com.aircandi.components.LocationManager;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.service.objects.Shortcut;
import com.aircandi.ui.base.BaseBrowse;
import com.aircandi.ui.base.BaseFragment;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.Routing.Route;

/*
 * Library Notes
 * 
 * - AWS: We are using the minimum libraries: core and S3. We could do the work to call AWS without their
 * libraries which should give us the biggest savings.
 */

/*
 * Threading Notes
 * 
 * - AsyncTasks: AsyncTask uses a static internal work queue with a hard-coded limit of 10 elements.
 * Once we have 10 tasks going concurrently, task 11 causes a RejectedExecutionException. ThreadPoolExecutor is a way to
 * get more control over thread pooling but it requires Android version 11/3.0 (we currently target 8/2.2 and higher).
 * AsyncTasks are hard-coded with a low priority and continue their work even if the activity is paused.
 */

/*
 * Lifecycle event sequences from Radar
 * 
 * First Launch: onCreate->onStart->onResume
 * HOME: Pause->Stop->||Restart->Start->Resume
 * Back: Pause->Stop->Destroyed
 * Other Candi Activity: Pause->Stop||Restart->Start->Resume
 * 
 * Alert Dialog: none
 * Dialog Activity: Pause||Resume
 * Overflow menu: none
 * ProgressIndicator: none
 * 
 * Preferences: Pause->Stop->||Restart->Start->Resume
 * PROFILE: Pause->Stop->||Restart->Start->Resume
 * 
 * Power off with Aircandi in foreground: Pause->Stop
 * Power on with Aircandi in foreground: Nothing
 * Unlock screen with Aircandi in foreground: Restart->Start->Resume
 */

public class AircandiForm extends BaseBrowse implements ActionBar.TabListener {

	private Number					mPauseDate;
	private Fragment				mCurrentFragment;
	private Fragment				mRadarFragment;

	private PullToRefreshAttacher	mPullToRefreshAttacher;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void initialize(Bundle savedInstanceState) {

		if (!LocationManager.getInstance().isLocationAccessEnabled()) {
			Routing.route(this, Route.SETTINGS_LOCATION);
			finish();
			return;
		}

		/* Check if the device is tethered */
		tetherAlert();

		// The attacher should always be created in the Activity's onCreate
		mPullToRefreshAttacher = PullToRefreshAttacher.get(this);
	}

	@Override
	protected void configureActionBar() {
		super.configureActionBar();
		if (mActionBar != null) {

			mActionBar.setDisplayShowTitleEnabled(true);
			mActionBar.setDisplayShowHomeEnabled(true);

			mActionBar.setHomeButtonEnabled(false);
			mActionBar.setDisplayHomeAsUpEnabled(false);

			if (Aircandi.getInstance().getCurrentUser().isAnonymous()) {
				showFragment(RadarFragment.class);
				return;
			}

			showTabs(true);
			mActionBar.selectTab(mActionBar.getTabAt(0));
		}
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("ucd")
	public void onShortcutClick(View view) {
		final Shortcut shortcut = (Shortcut) view.getTag();
		Routing.shortcut(this, shortcut, null, null);
	}

	@Override
	public void onAdd() {
		BaseFragment fragment = getCurrentFragment();
		if (fragment != null) {
			fragment.onAdd();
		}
	}

	@Override
	public void onHelp() {
		BaseFragment fragment = getCurrentFragment();
		if (fragment != null) {
			fragment.onHelp();
		}
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		TabInfo info = (TabInfo) tab.getTag();
		showFragment(info.clss);
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	protected void showTabs(Boolean visible) {
		if (visible) {
			mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
			if (getSupportActionBar().getTabCount() == 0) {
				addTab(getString(R.string.tab_radar_item), getString(R.string.tab_radar_item), RadarFragment.class, null);
				addTab(getString(R.string.tab_watching_item), getString(R.string.tab_watching_item), WatchingFragment.class, null);
				addTab(getString(R.string.tab_created_item), getString(R.string.tab_created_item), CreatedFragment.class, null);
				addTab(getString(R.string.tab_notifications_item), getString(R.string.tab_notifications_item), NewsFragment.class, null);
			}
		}
		else {
			mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
			getSupportActionBar().removeAllTabs();
		}
	}

	public void addTab(String tag, CharSequence label, Class<?> clss, Bundle args) {
		ActionBar.Tab tab = getSupportActionBar().newTab();
		tab.setText(label);
		tab.setTabListener(this);
		getSupportActionBar().addTab(tab, false);
		TabInfo info = new TabInfo(tag, clss, args);
		tab.setTag(info);
	}

	public void showFragment(Class<?> clazz) {
		Fragment fragment = null;
		if (clazz.getName().equals("com.aircandi.ui.RadarFragment") && mRadarFragment != null) {
			fragment = mRadarFragment;
		}
		else {
			fragment = Fragment.instantiate(this, clazz.getName(), null);
		}
		mCurrentFragment = fragment;
		if (clazz.getName().equals("com.aircandi.ui.RadarFragment")) {
			mRadarFragment = fragment;
		}

		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

		/* Replace whatever is in the fragment_container view with this fragment */
		transaction.replace(R.id.fragment_holder, fragment);

		/* Commit the transaction */
		transaction.commit();

		/* Creates call to onPrepareOptionsMenu */
		invalidateOptionsMenu();
	}

	private void tetherAlert() {
		/*
		 * We alert that wifi isn't enabled. If the user ends up enabling wifi,
		 * we will get that event and refresh radar with beacon support.
		 */
		if (NetworkManager.getInstance().isWifiTethered()
				|| (!NetworkManager.getInstance().isWifiEnabled() && !Aircandi.usingEmulator)) {

			Dialogs.wifi(AircandiForm.this, NetworkManager.getInstance().isWifiTethered()
					? R.string.alert_wifi_tethered
					: R.string.alert_wifi_disabled
					, null);
		}
	}

	public BaseFragment getCurrentFragment() {
		return (BaseFragment) mCurrentFragment;
	}

	public PullToRefreshAttacher getPullToRefreshAttacher() {
		return mPullToRefreshAttacher;
	}

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------	

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	@Override
	public void onStart() {
		super.onStart();
		/*
		 * Check for location service everytime we start. We won't continue
		 * if location services are disabled.
		 */
		if (!LocationManager.getInstance().isLocationAccessEnabled()) {
			Routing.route(this, Route.SETTINGS_LOCATION);
			finish();
		}

		/* Show current user */
		if (mActionBar != null
				&& Aircandi.getInstance().getCurrentUser() != null
				&& Aircandi.getInstance().getCurrentUser().name != null) {
			mActionBar.setSubtitle(Aircandi.getInstance().getCurrentUser().name.toUpperCase(Locale.US));
		}

		/* Manage tabs */
		showTabs(!(Aircandi.getInstance().getCurrentUser().isAnonymous()));
	}

	@Override
	protected void onResume() {
		super.onResume();
		/*
		 * Lifecycle ordering: (onCreate/onRestart)->onStart->onResume->onAttachedToWindow->onWindowFocusChanged
		 * 
		 * OnResume gets called after OnCreate (always) and whenever the activity is being brought back to the
		 * foreground. Not guaranteed but is usually called just before the activity receives focus.
		 */
		Aircandi.getInstance().setCurrentPlace(null);
		Logger.v(this, "Setting current place to null");
		if (mPauseDate != null) {
			final Long interval = DateTime.nowDate().getTime() - mPauseDate.longValue();
			if (interval > Constants.INTERVAL_TETHER_ALERT) {
				tetherAlert();
			}
		}
	}

	@Override
	protected void onPause() {
		/*
		 * - Fires when we lose focus and have been moved into the background. This will
		 * be followed by onStop if we are not visible. Does not fire if the activity window
		 * loses focus but the activity is still active.
		 */
		mPauseDate = DateTime.nowDate().getTime();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		/*
		 * The activity is getting destroyed but the application level state
		 * like singletons, statics, etc will continue as long as the application
		 * is running.
		 */
		Logger.d(this, "Destroyed");
		super.onDestroy();

		/* Don't count on this always getting called when this activity is killed */
		try {
			BitmapManager.getInstance().stopBitmapLoaderThread();
		}
		catch (Exception exception) {
			Exceptions.handle(exception);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.aircandi_form;
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("unused")
	private static final class TabInfo {
		private final String	tag;
		private final Class<?>	clss;
		private final Bundle	args;

		TabInfo(String _tag, Class<?> _class, Bundle _args) {
			tag = _tag;
			clss = _class;
			args = _args;
		}
	}
}