package com.aircandi.ui;

import java.util.Locale;

import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
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

public class AircandiForm extends BaseBrowse {

	private Number					mPauseDate;
	private Fragment				mRadarFragment;
	private static String			TAG_RADAR		= "radar";
	private static String			TAG_CREATED		= "created";
	private static String			TAG_WATCHING	= "watching";
	private static String			TAG_ACTIVITY	= "activity";
	private static String[]			fragmentTags	= { TAG_RADAR, TAG_CREATED, TAG_WATCHING, TAG_ACTIVITY };

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
				showFragment(RadarFragment.class, null);
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

	public void onMoreButtonClick(View view) {
		ActivityFragment fragment = (ActivityFragment) getCurrentFragment();
		fragment.onMoreButtonClick(view);
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

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	protected void showTabs(Boolean visible) {
		if (visible) {
			if (mActionBar.getNavigationMode() != ActionBar.NAVIGATION_MODE_TABS) {
				mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
				if (getSupportActionBar().getTabCount() == 0) {

					addTab(getString(R.string.tab_radar_item)
							, getString(R.string.tab_radar_item)
							, new TabListener<RadarFragment>(this, TAG_RADAR, RadarFragment.class)
							, RadarFragment.class, null);
					addTab(getString(R.string.tab_watching_item)
							, getString(R.string.tab_watching_item)
							, new TabListener<WatchingFragment>(this, TAG_WATCHING, WatchingFragment.class)
							, WatchingFragment.class, null);
					addTab(getString(R.string.tab_created_item)
							, getString(R.string.tab_created_item)
							, new TabListener<CreatedFragment>(this, TAG_CREATED, CreatedFragment.class)
							, CreatedFragment.class, null);
					addTab(getString(R.string.tab_activity_item)
							, getString(R.string.tab_activity_item)
							, new TabListener<ActivityFragment>(this, TAG_ACTIVITY, ActivityFragment.class)
							, ActivityFragment.class, null);
				}
			}
		}
		else {
			if (mActionBar.getNavigationMode() != ActionBar.NAVIGATION_MODE_STANDARD) {
				mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
				getSupportActionBar().removeAllTabs();
			}
		}
	}

	public void addTab(String tag, CharSequence label, TabListener listener, Class<?> clss, Bundle args) {
		ActionBar.Tab tab = getSupportActionBar().newTab()
				.setText(label)
				.setTag(tag)
				.setTabListener(listener);

		getSupportActionBar().addTab(tab, false);
	}

	public void showFragment(Class<?> clazz, FragmentTransaction ft) {
		Fragment fragment = null;
		FragmentTransaction transaction = ft;

		if (clazz.getName().equals("com.aircandi.ui.RadarFragment") && mRadarFragment != null) {
			fragment = mRadarFragment;
		}
		else {
			fragment = Fragment.instantiate(this, clazz.getName(), null);
		}
		if (clazz.getName().equals("com.aircandi.ui.RadarFragment")) {
			mRadarFragment = fragment;
		}

		if (transaction == null) {
			transaction = getSupportFragmentManager().beginTransaction();
			transaction.replace(R.id.fragment_holder, fragment);
			transaction.commit();
		}
		else {
			transaction.add(R.id.fragment_holder, fragment);
		}

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
		FragmentManager fm = getSupportFragmentManager();
		for (int i = 0; i < fragmentTags.length; i++) {
			Fragment fragment = fm.findFragmentByTag(fragmentTags[i]);
			if (fragment != null && fragment.isVisible()) {
				return (BaseFragment) fragment;
			}
		}
		return null;
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

	public static class TabListener<T extends Fragment> implements ActionBar.TabListener {
		private Fragment		mFragment;
		private final Activity	mActivity;
		private final String	mTag;
		private final Class<T>	mClass;

		public TabListener(Activity activity, String tag, Class<T> clss) {
			mActivity = activity;
			mTag = tag;
			mClass = clss;
		}

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			if (mFragment == null) {
				mFragment = Fragment.instantiate(mActivity, mClass.getName());
				ft.add(android.R.id.content, mFragment, mTag);
			}
			else {
				ft.attach(mFragment);
			}
		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			if (mFragment != null) {
				ft.detach(mFragment);
			}
		}

		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
			((AircandiForm)mActivity).getCurrentFragment().onScollToTop();
			/*
			 * User selected the already selected tab. We could use
			 * this gestured to refresh or scroll to the top.
			 */
		}
	}
}