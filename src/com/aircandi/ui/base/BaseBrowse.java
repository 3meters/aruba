package com.aircandi.ui.base;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.view.WindowManager;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.beta.R;
import com.aircandi.components.BusyManager;
import com.aircandi.components.MessageEvent;
import com.aircandi.components.NotificationManager;
import com.aircandi.ui.RadarForm;
import com.aircandi.ui.user.UserForm;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.AirTextView;
import com.aircandi.ui.widgets.SectionLayout;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.UI;
import com.squareup.otto.Subscribe;

public abstract class BaseBrowse extends BaseActivity {

	/* Inputs */
	protected Boolean				mForceRefresh	= false;
	protected DrawerLayout			mDrawerLayout;
	protected View					mDrawerView;
	protected ActionBarDrawerToggle	mDrawerToggle;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		if (!isFinishing()) {
			unpackIntent();
			initialize(savedInstanceState);
			configureActionBar();
			configureNavigationDrawer();
			databind(mForceRefresh);
		}
	}

	@Override
	protected void unpackIntent() {
		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mForceRefresh = extras.getBoolean(Constants.EXTRA_REFRESH_FORCE);
		}
	}

	protected void initialize(Bundle savedInstanceState) {
		mBusyManager = new BusyManager(this);
	}

	@Override
	protected void configureActionBar() {
		super.configureActionBar();
		if (mActionBar != null) {
			mActionBar.setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	protected void configureNavigationDrawer() {
		/*
		 * ActionBarDrawerToggle ties together the the proper interactions between
		 * the sliding drawer and the action bar app icon
		 */
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerView = (View) findViewById(R.id.left_drawer);

		if (mDrawerLayout != null) {

			/* Set signed in user info */
			SectionLayout section = (SectionLayout) mDrawerView.findViewById(R.id.section_navigation);

			section.setHeaderTitle(Aircandi.getInstance().getUser().name);
			AirImageView photoView = (AirImageView) section.findViewById(R.id.photo);
			if (photoView != null) {
				UI.drawPhoto(photoView, Aircandi.getInstance().getUser().getPhoto());
			}

			/* Set a custom shadow that overlays the main content when the drawer opens */
			mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

			/* Set the over that covers primary content when drawer is visible */
			mDrawerLayout.setScrimColor(mResources.getColor(R.color.overlay_navigation_drawer_dark));

			mDrawerToggle = new ActionBarDrawerToggle(this
					, mDrawerLayout 					// DrawerLayout object 
					, R.drawable.ic_drawer 				// nav drawer image to replace 'Up' caret 
					, R.string.menu_drawer_open 		// "open drawer" description for accessibility
					, R.string.menu_drawer_close 		// "close drawer" description for accessibility
			) {
				@Override
				public void onDrawerClosed(View view) {
					super.onDrawerClosed(view);
					mActionBar.setTitle(getActivityTitle());
					invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
				}

				@Override
				public void onDrawerOpened(View drawerView) {
					super.onDrawerOpened(drawerView);
					updateNotificationCount();
					mActionBar.setTitle("aircandi");
					invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
				}
			};

			mDrawerLayout.setDrawerListener(mDrawerToggle);
		}
	}

	protected void databind(final Boolean refreshProposed) {}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	public void onRefresh() {
		databind(true); // Called from Routing
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onMessage(final MessageEvent event) {
		/*
		 * Refreshes radar so newly created place can pop in.
		 */
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				/* Set new notification count */
				updateNotificationCount();
			}
		});
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		if (mDrawerToggle != null) {
			mDrawerToggle.syncState();
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// Pass any configuration change to the drawer toggles
		if (mDrawerToggle != null) {
			mDrawerToggle.onConfigurationChanged(newConfig);
		}
	}

	@SuppressWarnings("ucd")
	public void onMenuItemClick(View view) {
		Integer id = view.getId();
		Boolean ignore = false;
		if (id == R.id.radar && this instanceof RadarForm) {
			ignore = true;
		}
		else if (id == R.id.profile && this instanceof UserForm) {
			ignore = true;
		}
		if (!ignore) {
			Routing.route(this, Routing.routeForMenuId(view.getId()));
		}
		mDrawerLayout.closeDrawer(mDrawerView);
	}

	// --------------------------------------------------------------------------------------------
	// UI
	// --------------------------------------------------------------------------------------------

	protected void draw() {}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	public void updateNotificationCount() {
		/* Set new notification count */
		if (mDrawerView != null) {
			Integer newCount = NotificationManager.getInstance().getNewCount();
			((AirTextView) mDrawerView.findViewById(R.id.notifications_count)).setText(String.valueOf(newCount));
		}
	}

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		/*
		 * Android 2.3 or lower: called every time the user hits the menu button.
		 * Android 3.0 or higher: called when invalidateOptionsMenu is called.
		 * 
		 * Behavior might be modified because we are using ABS.
		 */

		/* If the nav drawer is open, hide action items related to the content view */

		if (mDrawerLayout != null) {
			//			boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerView);
			//			for (int i = 0; i < menu.size(); i++) {
			//				menu.getItem(i).setVisible(!drawerOpen);
			//			}
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		/*
		 * The action bar home/up action should open or close the drawer.
		 * ActionBarDrawerToggle will take care of this.
		 */
		if (item.getItemId() == android.R.id.home) {
			if (mDrawerLayout != null) {
				if (mDrawerToggle.isDrawerIndicatorEnabled()) {
					if (mDrawerLayout.isDrawerOpen(mDrawerView)) {
						mDrawerLayout.closeDrawer(mDrawerView);
					}
					else {
						mDrawerLayout.openDrawer(mDrawerView);
						final Boolean userOpened = Aircandi.settings.getBoolean(Constants.SETTING_NAVIGATION_DRAWER_OPENED_BY_USER, false);
						if (!userOpened) {
							Aircandi.settingsEditor.putBoolean(Constants.SETTING_NAVIGATION_DRAWER_OPENED_BY_USER, true);
							Aircandi.settingsEditor.commit();
						}
					}
					return true;
				}
			}
		}
		return super.onOptionsItemSelected(item);
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

}