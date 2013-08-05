package com.aircandi.ui;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.ScanService;
import com.aircandi.beta.R;
import com.aircandi.components.BeaconsLockedEvent;
import com.aircandi.components.BusProvider;
import com.aircandi.components.BusyManager;
import com.aircandi.components.EntitiesChangedEvent;
import com.aircandi.components.EntitiesForBeaconsFinishedEvent;
import com.aircandi.components.EntityManager;
import com.aircandi.components.Exceptions;
import com.aircandi.components.FontManager;
import com.aircandi.components.LocationChangedEvent;
import com.aircandi.components.LocationLockedEvent;
import com.aircandi.components.LocationManager;
import com.aircandi.components.LocationTimeoutEvent;
import com.aircandi.components.Logger;
import com.aircandi.components.MessageEvent;
import com.aircandi.components.MonitoringWifiScanReceivedEvent;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NetworkManager.ConnectedState;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.PlacesNearLocationFinishedEvent;
import com.aircandi.components.ProximityManager;
import com.aircandi.components.ProximityManager.ScanReason;
import com.aircandi.components.ProximityManager.WifiScanResult;
import com.aircandi.components.QueryWifiScanReceivedEvent;
import com.aircandi.components.RadarListAdapter;
import com.aircandi.components.Tracker;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.service.objects.AirLocation;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Place;
import com.aircandi.ui.base.BaseBrowse;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.Routing.Route;
import com.aircandi.utilities.UI;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.squareup.otto.Subscribe;

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
 * Home: Pause->Stop->||Restart->Start->Resume
 * Back: Pause->Stop->Destroyed
 * Other Candi Activity: Pause->Stop||Restart->Start->Resume
 * 
 * Alert Dialog: None
 * Dialog Activity: Pause||Resume
 * Overflow menu: None
 * ProgressIndicator: None
 * 
 * Preferences: Pause->Stop->||Restart->Start->Resume
 * Profile: Pause->Stop->||Restart->Start->Resume
 * 
 * Power off with Aircandi in foreground: Pause->Stop
 * Power on with Aircandi in foreground: Nothing
 * Unlock screen with Aircandi in foreground: Restart->Start->Resume
 */

public class RadarForm extends BaseBrowse {

	private final Handler			mHandler				= new Handler();

	private Number					mEntityModelRefreshDate;
	private Number					mEntityModelActivityDate;
	private Number					mEntityModelBeaconDate;
	private Integer					mEntityModelWifiState	= WifiManager.WIFI_STATE_UNKNOWN;
	private Number					mPauseDate;

	private PullToRefreshListView	mList;
	private TextView				mBeaconIndicator;
	private String					mDebugWifi;
	private String					mDebugLocation			= "--";
	private MenuItem				mMenuItemBeacons;

	private SoundPool				mSoundPool;
	private int						mNewCandiSoundId;
	private Boolean					mInitialized			= false;

	private final List<Entity>		mEntities				= new ArrayList<Entity>();
	private RadarListAdapter		mRadarAdapter;
	private Boolean					mFreshWindow			= false;
	private Boolean					mNavigationShown		= false;

	@Override
	protected void initialize(Bundle savedInstanceState) {

		mBusyManager = new BusyManager(this);

		if (!LocationManager.getInstance().isLocationAccessEnabled()) {
			Routing.route(this, Route.SettingsLocation);
		}

		/* Current navigation view */
		Aircandi.getInstance().setNavigationDrawerCurrentView(RadarForm.class);

		/* Always reset the entity cache */
		EntityManager.getEntityCache().clear();

		/* Other UI references */
		mList = (PullToRefreshListView) findViewById(R.id.radar_list);
		mList.getRefreshableView().setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				final Place entity = (Place) mRadarAdapter.getItems().get(position - 1);
				Bundle extras = null;
				if (entity.synthetic) {
					extras = new Bundle();
					extras.putBoolean(Constants.EXTRA_UPSIZE_SYNTHETIC, true);
				}
				Routing.route(RadarForm.this, Route.Browse, entity, null, extras);
			}
		});

		/* Adapter snapshots the items in mEntities */
		mRadarAdapter = new RadarListAdapter(this, mEntities);
		mList.getRefreshableView().setAdapter(mRadarAdapter);

		/* Refresh listener */
		mList.setOnRefreshListener(new OnRefreshListener<ListView>() {

			@Override
			public void onRefresh(PullToRefreshBase<ListView> refreshView) {
				searchForPlaces();
			}

		});

		mList.getLoadingLayoutProxy().setPullLabel(getString(R.string.refresh_label_pull));
		mList.getLoadingLayoutProxy().setReleaseLabel(getString(R.string.refresh_label_release));
		mList.getLoadingLayoutProxy().setRefreshingLabel(getString(R.string.refresh_scanning_for_location));
		mList.getLoadingLayoutProxy().setLoadingDrawable(null);
		mList.getLoadingLayoutProxy().setTextTypeface(FontManager.getFontRobotoRegular());

		/* Store sounds */
		mSoundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 100);
		mNewCandiSoundId = mSoundPool.load(this, R.raw.notification_candi_discovered, 1);

		/* Check if the device is tethered */
		tetherAlert();

		mInitialized = true;
	}

	// --------------------------------------------------------------------------------------------
	// Bus: beacons
	// --------------------------------------------------------------------------------------------

	@Subscribe
	@SuppressWarnings("ucd")
	public void onQueryWifiScanReceived(final QueryWifiScanReceivedEvent event) {

		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Aircandi.stopwatch1.segmentTime("Wifi scan received event fired");
				Tracker.sendTiming("radar", Aircandi.stopwatch1.getTotalTimeMills(), "wifi_scan_received", null, Aircandi.getInstance().getUser());
				Logger.d(RadarForm.this, "Query wifi scan received event: locking beacons");

				if (event.wifiList != null) {
					ProximityManager.getInstance().lockBeacons();
				}
				else {
					BusProvider.getInstance().post(new EntitiesForBeaconsFinishedEvent());
				}
			}
		});
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onBeaconsLocked(BeaconsLockedEvent event) {

		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Aircandi.stopwatch1.segmentTime("Beacons locked event fired");
				mEntityModelBeaconDate = ProximityManager.getInstance().getLastBeaconLockedDate();
				new AsyncTask() {

					@Override
					protected Object doInBackground(Object... params) {
						Thread.currentThread().setName("GetEntitiesForBeacons");
						final ServiceResponse serviceResponse = ProximityManager.getInstance().getEntitiesByProximity();
						return serviceResponse;
					}

					@Override
					protected void onPostExecute(Object result) {
						final ServiceResponse serviceResponse = (ServiceResponse) result;
						if (serviceResponse.responseCode != ResponseCode.Success) {
							Routing.serviceError(RadarForm.this, serviceResponse);
							mBusyManager.hideBusy();
						}
					}

				}.execute();
			}
		});
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onEntitiesForBeaconsFinished(EntitiesForBeaconsFinishedEvent event) {

		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Aircandi.stopwatch1.segmentTime("Entities for beacons finished event fired");
				Tracker.sendTiming("radar", Aircandi.stopwatch1.getTotalTimeMills(), "entities_for_beacons", null, Aircandi.getInstance().getUser());
				Logger.d(RadarForm.this, "Entities for beacons finished event: ** done **");
				mList.getLoadingLayoutProxy().setRefreshingLabel(getString(R.string.refresh_scanning_for_location));
				mEntityModelWifiState = NetworkManager.getInstance().getWifiState();
			}
		});
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onWifiScanReceived(MonitoringWifiScanReceivedEvent event) {
		updateDevIndicator(event.wifiList, null);
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onWifiQueryReceived(QueryWifiScanReceivedEvent event) {
		updateDevIndicator(event.wifiList, null);
	}

	// --------------------------------------------------------------------------------------------
	// Bus: location
	// --------------------------------------------------------------------------------------------

	@Subscribe
	@SuppressWarnings("ucd")
	public void onLocationTimeout(final LocationTimeoutEvent event) {
		Aircandi.stopwatch2.stop("Location fix attempt aborted: timeout");
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				Logger.d(RadarForm.this, "Location fix attempt aborted: timeout: ** done **");
				if (LocationManager.getInstance().getLocationLocked() == null) {
					/* We only show toast if we timeout without getting any location fix */
					UI.showToastNotification(getString(R.string.error_location_poor), Toast.LENGTH_SHORT);
				}
				mBusyManager.hideBusy();
				mList.onRefreshComplete();
			}
		});
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onLocationChanged(final LocationChangedEvent event) {

		runOnUiThread(new Runnable() {

			@Override
			public void run() {

				final Location locationCandidate = event.location;
				if (locationCandidate != null) {

					final Location locationLocked = LocationManager.getInstance().getLocationLocked();

					if (locationLocked != null) {

						/* We never go from gps provider to network provider */
						if (locationLocked.getProvider().equals("gps") && locationCandidate.getProvider().equals("network")) {
							return;
						}

						/* If gps provider or same provider then look for improved accuracy */
						if (locationLocked.getProvider().equals(locationCandidate.getProvider())) {
							final float accuracyImprovement = locationLocked.getAccuracy() / locationCandidate.getAccuracy();
							boolean isSignificantlyMoreAccurate = (accuracyImprovement >= 1.5);
							if (!isSignificantlyMoreAccurate) {
								return;
							}
						}
					}

					LocationManager.getInstance().setLocationLocked(locationCandidate);
					if (mBusyManager != null) {
						mBusyManager.updateAccuracyIndicator();
					}
					BusProvider.getInstance().post(new LocationLockedEvent(LocationManager.getInstance().getLocationLocked()));

					if (Aircandi.stopwatch2.isStarted()) {
						Aircandi.stopwatch2.stop("Location acquired");
						Tracker.sendTiming("location", Aircandi.stopwatch2.getTotalTimeMills(), "location_acquired", null, Aircandi.getInstance().getUser());
					}

					final AirLocation location = LocationManager.getInstance().getAirLocationLocked();
					if (location != null) {

						mBusyManager.showBusy();

						new AsyncTask() {

							@Override
							protected Object doInBackground(Object... params) {

								Logger.d(RadarForm.this, "Location changed event: getting places near location");
								Thread.currentThread().setName("GetPlacesNearLocation");
								Aircandi.stopwatch2.start("Get places near location");

								final ServiceResponse serviceResponse = ProximityManager.getInstance().getEntitiesNearLocation(location);
								return serviceResponse;
							}

							@Override
							protected void onPostExecute(Object result) {
								final ServiceResponse serviceResponse = (ServiceResponse) result;
								if (serviceResponse.responseCode != ResponseCode.Success) {
									Routing.serviceError(RadarForm.this, serviceResponse);
									mBusyManager.hideBusy();
								}
							}

						}.execute();
					}
					else {
						mBusyManager.hideBusy();
					}
				}
			}
		});
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onPlacesNearLocationFinished(final PlacesNearLocationFinishedEvent event) {
		Aircandi.stopwatch2.stop("Places near location complete");
		Tracker.sendTiming("radar", Aircandi.stopwatch2.getTotalTimeMills(), "places_near_location", null, Aircandi.getInstance().getUser());
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				Logger.d(RadarForm.this, "Places near location finished event: ** done **");
				mBusyManager.hideBusy();
				mList.onRefreshComplete();
			}
		});
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onLocationLocked(LocationLockedEvent event) {
		updateDevIndicator(null, event.location);
	}

	// --------------------------------------------------------------------------------------------
	// Bus: general
	// --------------------------------------------------------------------------------------------

	@Subscribe
	@SuppressWarnings("ucd")
	public void onEntitiesChanged(final EntitiesChangedEvent event) {

		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Logger.d(RadarForm.this, "Entities changed event: updating radar");
				Aircandi.stopwatch1.segmentTime("Entities changed: start radar display");
				Aircandi.stopwatch3.stop("Aircandi initialization finished and got first entities");

				mEntityModelRefreshDate = ProximityManager.getInstance().getLastBeaconLoadDate();
				mEntityModelActivityDate = EntityManager.getEntityCache().getLastActivityDate();

				/* Point radar adapter at the updated entities */
				final int previousCount = mRadarAdapter.getCount();
				final List<Entity> entities = event.entities;
				mRadarAdapter.setItems(entities);
				mRadarAdapter.notifyDataSetChanged();
				Aircandi.stopwatch1.stop("Search for places by beacon complete");

				/* Add some sparkle */
				if (previousCount == 0 && entities.size() > 0) {
					if (!mNavigationShown && mDrawerLayout != null) {
						final Boolean userOpened = Aircandi.settings.getBoolean(Constants.SETTING_NAVIGATION_DRAWER_OPENED_BY_USER, false);
						if (!userOpened) {
							mDrawerLayout.openDrawer(mDrawerView);
						}
						mNavigationShown = true;
					}
					if (Aircandi.settings.getBoolean(Constants.PREF_SOUND_EFFECTS, Constants.PREF_SOUND_EFFECTS_DEFAULT)) {
						new AsyncTask() {

							@Override
							protected Object doInBackground(Object... params) {
								Thread.currentThread().setName("PlaySound");
								mSoundPool.play(mNewCandiSoundId, 0.2f, 0.2f, 0, 0, 1f);
								return null;
							}

						}.execute();
					}
				}
			}
		});

	}

	@Override
	@Subscribe
	@SuppressWarnings("ucd")
	public void onMessage(final MessageEvent event) {
		super.onMessage(event);
		/*
		 * Refreshes radar so newly created place can pop in.
		 */
		if (event.notification.entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					onRefresh();
				}
			});
		}
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	public void onRefresh() {
		/*
		 * This only gets called by a user clicking the refresh button.
		 */
		Logger.d(this, "Starting refresh");
		Tracker.sendEvent("ui_action", "refresh_radar", null, 0, Aircandi.getInstance().getUser());
		mList.setRefreshing();
	}

	@Override
	public void onAdd() {
		if (Aircandi.getInstance().getUser() != null) {
			Routing.route(this, Route.New, null, Constants.SCHEMA_ENTITY_PLACE, null);
		}
	}

	@Override
	@SuppressWarnings("ucd")
	public void onMenuItemClick(View view) {
		Integer id = view.getId();
		if (id != R.id.radar) {
			super.onMenuItemClick(view);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	private void searchForPlaces() {

		/* We won't perform a search if location access is disabled */
		if (!LocationManager.getInstance().isLocationAccessEnabled()) {
			Routing.route(this, Route.SettingsLocation);
		}
		else {
			new AsyncTask() {

				@Override
				protected void onPreExecute() {
					mBusyManager.showBusy();
				}

				@Override
				protected Object doInBackground(Object... params) {

					ConnectedState connectedState = NetworkManager.getInstance().checkConnectedState();
					if (connectedState == ConnectedState.Normal) {
						searchForPlacesByBeacon();

						/* We give the beacon query a bit of a head start */
						mHandler.postDelayed(new Runnable() {

							@Override
							public void run() {
								searchForPlacesByLocation();
							}
						}, 500);
					}
					return connectedState;
				}

				@Override
				protected void onPostExecute(Object result) {
					ConnectedState connectedState = (ConnectedState) result;
					if (connectedState != ConnectedState.Normal) {
						if (Aircandi.stopwatch3.isStarted()) {
							Aircandi.stopwatch3.stop("Aircandi initialization finished: network problem");
						}
						mBusyManager.hideBusy();
						mList.onRefreshComplete();

						if (connectedState == ConnectedState.WalledGarden) {
							Dialogs.alertDialogSimple(RadarForm.this, null, getString(R.string.error_connection_walled_garden));
						}
						else if (connectedState == ConnectedState.None) {
							Dialogs.alertDialogSimple(RadarForm.this, null, getString(R.string.error_connection_none));
						}
					}
				}
			}.execute();
		}
	}

	private void searchForPlacesByBeacon() {
		Aircandi.stopwatch1.start("Search for places by beacon");
		mEntityModelWifiState = NetworkManager.getInstance().getWifiState();
		if (NetworkManager.getInstance().isWifiEnabled()) {
			EntityManager.getEntityCache().removeEntities(Constants.SCHEMA_ENTITY_BEACON, null, null);
			ProximityManager.getInstance().scanForWifi(ScanReason.query);
		}
	}

	private void searchForPlacesByLocation() {
		Aircandi.stopwatch2.start("Lock location");
		LocationManager.getInstance().setLocationLocked(null);
		LocationManager.getInstance().lockLocationBurst();

	}

	// --------------------------------------------------------------------------------------------
	// UI
	// --------------------------------------------------------------------------------------------

	private void tetherAlert() {
		/*
		 * We alert that wifi isn't enabled. If the user ends up enabling wifi,
		 * we will get that event and refresh radar with beacon support.
		 */
		if (NetworkManager.getInstance().isWifiTethered()
				|| (!NetworkManager.getInstance().isWifiEnabled() && !Aircandi.usingEmulator)) {

			Dialogs.wifi(RadarForm.this, NetworkManager.getInstance().isWifiTethered()
					? R.string.alert_wifi_tethered
					: R.string.alert_wifi_disabled
					, null);
		}
	}

	private void doBeaconIndicatorClick() {
		if (mBeaconIndicator != null) {
			final StringBuilder beaconMessage = new StringBuilder(500);
			List<WifiScanResult> wifiList = ProximityManager.getInstance().getWifiList();
			synchronized (wifiList) {
				if (Aircandi.getInstance().getUser() != null
						&& Aircandi.settings.getBoolean(Constants.PREF_ENABLE_DEV, Constants.PREF_ENABLE_DEV_DEFAULT)
						&& Aircandi.getInstance().getUser().developer != null
						&& Aircandi.getInstance().getUser().developer) {
					if (Aircandi.wifiCount > 0) {
						for (WifiScanResult wifi : wifiList) {
							if (!wifi.SSID.equals("candi_feed")) {
								beaconMessage.append(wifi.SSID + ": (" + String.valueOf(wifi.level) + ") " + wifi.BSSID + System.getProperty("line.separator"));
							}
						}
						beaconMessage.append(System.getProperty("line.separator"));
						beaconMessage.append("Wifi fix: " + DateTime.intervalSince(ProximityManager.getInstance().mLastWifiUpdate, DateTime.nowDate()));
					}

					final Location location = LocationManager.getInstance().getLocationLocked();
					if (location != null) {
						final Date fixDate = new Date(location.getTime());
						beaconMessage.append(System.getProperty("line.separator") + "Location fix: " + DateTime.intervalSince(fixDate, DateTime.nowDate()));
						beaconMessage.append(System.getProperty("line.separator") + "Location accuracy: " + String.valueOf(location.getAccuracy()));
						beaconMessage.append(System.getProperty("line.separator") + "Location provider: " + location.getProvider());
					}
					else {
						beaconMessage.append(System.getProperty("line.separator") + "Location fix: none");
					}
				}
				else {
					return;
				}
			}
			Dialogs.alertDialog(R.drawable.ic_launcher
					, getString(R.string.alert_beacons_title)
					, beaconMessage.toString()
					, null
					, this
					, android.R.string.ok
					, null
					, null
					, new
					DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {}
					}, null);
		}
	}

	public void updateDevIndicator(final List<WifiScanResult> scanList, Location location) {

		if (mBeaconIndicator == null) return;

		if (scanList != null) {

			synchronized (scanList) {
				/*
				 * In case we get called from a background thread.
				 */
				runOnUiThread(new Runnable() {

					@Override
					public void run() {

						WifiScanResult wifiStrongest = null;
						int wifiCount = 0;
						for (WifiScanResult wifi : scanList) {
							wifiCount++;
							if (wifiStrongest == null) {
								wifiStrongest = wifi;
							}
							else if (wifi.level > wifiStrongest.level) {
								wifiStrongest = wifi;
							}
						}

						Aircandi.wifiCount = wifiCount;
						mDebugWifi = String.valueOf(wifiCount);
					}
				});

			}
		}

		if (location != null) {
			Location locationLocked = LocationManager.getInstance().getLocationLocked();
			if (locationLocked != null) {
				if (location.getProvider().equals(locationLocked.getProvider()) && (int) location.getAccuracy() == (int) locationLocked.getAccuracy()) {
					mBeaconIndicator.setTextColor(Aircandi.getInstance().getResources().getColor(R.color.accent_blue));
				}
				else {
					if (mThemeTone.equals("dark")) {
						mBeaconIndicator.setTextColor(Aircandi.getInstance().getResources().getColor(R.color.text_dark));
					}
					else {
						mBeaconIndicator.setTextColor(Aircandi.getInstance().getResources().getColor(R.color.text_light));
					}
				}
			}

			String debugLocation = location.getProvider().substring(0, 1).toUpperCase(Locale.ROOT);
			if (location.hasAccuracy()) {
				debugLocation += String.valueOf((int) location.getAccuracy());
			}
			else {
				debugLocation += "--";
			}
			mDebugLocation = debugLocation;
		}

		mBeaconIndicator.setText(mDebugWifi + ":" + mDebugLocation);
	}

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		/*
		 * Setup menu items local to radar.
		 */
		mMenuItemBeacons = menu.findItem(R.id.beacons);
		if (mMenuItemBeacons != null) {

			/* Only show beacon indicator if user is a developer */
			if (!Aircandi.settings.getBoolean(Constants.PREF_ENABLE_DEV, Constants.PREF_ENABLE_DEV_DEFAULT)
					|| Aircandi.getInstance().getUser() == null
					|| Aircandi.getInstance().getUser().developer == null
					|| !Aircandi.getInstance().getUser().developer) {
				mMenuItemBeacons.setVisible(false);
			}
			else {
				mBeaconIndicator = (TextView) mMenuItemBeacons.getActionView().findViewById(R.id.beacon_indicator);
				mMenuItemBeacons.getActionView().findViewById(R.id.beacon_frame).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						doBeaconIndicatorClick();
					}
				});
			}
		}

		MenuItem refresh = menu.findItem(R.id.refresh);
		if (refresh != null) {
			if (mBusyManager != null) {
				mBusyManager.setAccuracyIndicator(refresh.getActionView().findViewById(R.id.accuracy_indicator));
				mBusyManager.setRefreshImage(refresh.getActionView().findViewById(R.id.refresh_image));
				mBusyManager.setRefreshProgress(refresh.getActionView().findViewById(R.id.refresh_progress));
				mBusyManager.updateAccuracyIndicator();
			}

			refresh.getActionView().findViewById(R.id.refresh_frame).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					onRefresh();
				}
			});
		}

		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	protected void onStart() {
		/*
		 * Called everytime the activity is started or restarted.
		 */
		Logger.d(this, "CandiRadarActivity starting");
		super.onStart();

		if (mPrefChangeReloadNeeded) {
			final Intent intent = getIntent();
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			if (Constants.SUPPORTS_HONEYCOMB) {
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
			}

			startActivity(intent);
			finish();
			return;
		}

		if (!mInitialized) return;

		/* Check for location service */
		if (!LocationManager.getInstance().isLocationAccessEnabled()) {
			/* We won't continue if location services are disabled */
			Routing.route(this, Route.SettingsLocation);
			finish();
		}

		if (Constants.DEBUG_TRACE) {
			Debug.startMethodTracing("aircandi", 100000000);
		}

		/* Reset the accuracy indicator */
		if (mBusyManager != null) {
			mBusyManager.updateAccuracyIndicator();
		}
	}

	@Override
	protected void onStop() {
		/*
		 * Fired when starting another activity and we lose our window.
		 */

		/*
		 * Stop any location burst that might be active unless
		 * this activity is being restarted. We do this because there
		 * is a race condition that can stop location burst after it
		 * has been started by the reload.
		 */
		if (!mPrefChangeReloadNeeded) {
			LocationManager.getInstance().stopLocationBurst();
		}

		/* Kill busy */
		mBusyManager.hideBusy();
		mList.onRefreshComplete();

		Logger.d(this, "CandiRadarActivity stopped");
		if (Constants.DEBUG_TRACE) {
			Debug.stopMethodTracing();
		}

		super.onStop();
	}

	@Override
	protected void onPause() {
		/*
		 * - Fires when we lose focus and have been moved into the background. This will
		 * be followed by onStop if we are not visible. Does not fire if the activity window
		 * loses focus but the activity is still active.
		 */
		stopScanService();
		mPauseDate = DateTime.nowDate().getTime();

		super.onPause();
	}

	@Override
	protected void onResume() {
		/*
		 * Lifecycle ordering: (onCreate/onRestart)->onStart->onResume->onAttachedToWindow->onWindowFocusChanged
		 * 
		 * OnResume gets called after OnCreate (always) and whenever the activity is being brought back to the
		 * foreground. Not guaranteed but is usually called just before the activity receives focus.
		 */
		super.onResume();
		Logger.d(this, "onResume called");
		if (!mInitialized || isFinishing()) return;
		mFreshWindow = true;

		/* Run help if it hasn't been run yet */
		//		final Boolean runOnceHelp = Aircandi.settings.getBoolean(Constants.SETTING_RUN_ONCE_HELP_RADAR, false);
		//		if (!runOnceHelp) {
		//			doHelpClick();
		//			return;
		//		}

		if (Aircandi.getInstance().getUser() != null
				&& Aircandi.settings.getBoolean(Constants.PREF_ENABLE_DEV, Constants.PREF_ENABLE_DEV_DEFAULT)
				&& Aircandi.getInstance().getUser().developer != null
				&& Aircandi.getInstance().getUser().developer) {
			startScanService(Constants.INTERVAL_SCAN_WIFI);
		}
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);

		if (!mInitialized || isFinishing()) return;

		if (hasFocus && mFreshWindow) {

			if (mPauseDate != null) {
				final Long interval = DateTime.nowDate().getTime() - mPauseDate.longValue();
				if (interval > Constants.INTERVAL_TETHER_ALERT) {
					tetherAlert();
				}
			}

			manageData();
			mFreshWindow = false;
		}
	}

	private void manageData() {
		/*
		 * Cases that trigger a search
		 * 
		 * - First time radar is run
		 * - Preference change
		 * - Didn't complete location fix before user switched away from radar
		 * - While away, user enabled wifi
		 * - Beacons we used for last fix have changed
		 * - Beacon fix is thirty minutes old or more
		 * 
		 * Cases that trigger a ui refresh
		 * 
		 * - Preference change
		 * - EntityModel has changed since last search
		 */
		if (mEntityModelActivityDate == null) {
			/*
			 * Get set everytime onEntitiesChanged gets called. Means
			 * we have never completed even the first search for entities.
			 */
			Logger.d(this, "Start first place search");
			mList.setRefreshing(false);
		}
		else if (mPrefChangeNewSearchNeeded) {
			/*
			 * Gets set based on evaluation of pref changes
			 */
			Logger.d(this, "Start place search because of preference change");
			mPrefChangeNewSearchNeeded = false;
			mList.setRefreshing();
		}
		else if (mPrefChangeRefreshUiNeeded) {
			/*
			 * Gets set based on evaluation of pref changes
			 */
			Logger.d(this, "Refresh Ui because of preference change");
			mPrefChangeRefreshUiNeeded = false;
			mBusyManager.showBusy();
			invalidateOptionsMenu();
			mRadarAdapter.getItems().clear();
			mRadarAdapter.getItems().addAll(EntityManager.getInstance().getPlaces(null, null));
			mRadarAdapter.notifyDataSetChanged();
			mBusyManager.hideBusy();
		}
		else if (LocationManager.getInstance().getLocationLocked() == null) {
			/*
			 * Gets set everytime we accept a location change in onLocationChange. Means
			 * we didn't get an acceptable fix yet from either the network or gps providers.
			 */
			Logger.d(this, "Start place search because didn't complete location fix");
			LocationManager.getInstance().lockLocationBurst();
		}
		else if (ProximityManager.getInstance().beaconRefreshNeeded(LocationManager.getInstance().getLocationLocked())) {
			/*
			 * We check to see if it's been awhile since the last search.
			 */
			Logger.d(this, "Start place search because of staleness");
			mList.setRefreshing();
		}
		else if (NetworkManager.getInstance().getWifiState() == WifiManager.WIFI_STATE_DISABLED
				&& mEntityModelWifiState == WifiManager.WIFI_STATE_ENABLED) {
			/*
			 * Wifi has been disabled since our last search
			 */
			Integer wifiApState = NetworkManager.getInstance().getWifiApState();
			if (wifiApState == NetworkManager.WIFI_AP_STATE_ENABLED
					|| wifiApState == NetworkManager.WIFI_AP_STATE_ENABLED + 10) {
				Logger.d(this, "Wifi Ap enabled, clearing beacons");
				UI.showToastNotification("Hotspot or tethering enabled", Toast.LENGTH_SHORT);
			}
			else {
				UI.showToastNotification("Wifi disabled", Toast.LENGTH_SHORT);
			}
			ProximityManager.getInstance().getWifiList().clear();
			EntityManager.getEntityCache().removeEntities(Constants.SCHEMA_ENTITY_BEACON, null, null);
			LocationManager.getInstance().setLocationLocked(null);
		}
		else if (NetworkManager.getInstance().getWifiState() == WifiManager.WIFI_STATE_ENABLED
				&& mEntityModelWifiState == WifiManager.WIFI_STATE_DISABLED) {
			/*
			 * Wifi has been enabled since our last search
			 */
			UI.showToastNotification("Wifi enabled", Toast.LENGTH_SHORT);
			mList.setRefreshing();
		}
		else if ((ProximityManager.getInstance().getLastBeaconLockedDate() != null && mEntityModelBeaconDate != null)
				&& (ProximityManager.getInstance().getLastBeaconLockedDate().longValue() > mEntityModelBeaconDate.longValue())) {
			/*
			 * The beacons we are locked to have changed while we were away so we need to
			 * search for new places linked to beacons.
			 */
			Logger.d(this, "Refresh places for beacons because beacon date has changed");
			mEntityModelBeaconDate = ProximityManager.getInstance().getLastBeaconLockedDate();
			new AsyncTask() {

				@Override
				protected Object doInBackground(Object... params) {
					Thread.currentThread().setName("GetEntitiesForBeacons");
					final ServiceResponse serviceResponse = ProximityManager.getInstance().getEntitiesByProximity();
					return serviceResponse;
				}

			}.execute();
		}
		else if ((ProximityManager.getInstance().getLastBeaconLoadDate() != null && mEntityModelRefreshDate != null
				&& ProximityManager.getInstance().getLastBeaconLoadDate().longValue() > mEntityModelRefreshDate.longValue())
				|| (EntityManager.getEntityCache().getLastActivityDate() != null && mEntityModelActivityDate != null
				&& EntityManager.getEntityCache().getLastActivityDate().longValue() > mEntityModelActivityDate.longValue())) {
			/*
			 * Everytime we show details for a place, we fetch place details from the service
			 * when in turn get pushed into the cache and activityDate gets tickled.
			 */
			Logger.d(this, "Update radar ui because of detected entity model change");
			mHandler.postDelayed(new Runnable() {

				@Override
				public void run() {
					mBusyManager.showBusy();
					invalidateOptionsMenu();
					mRadarAdapter.getItems().clear();
					mRadarAdapter.getItems().addAll(EntityManager.getInstance().getPlaces(null, null));
					mRadarAdapter.notifyDataSetChanged();
					mBusyManager.hideBusy();
				}
			}, 100);
		}
	}

	@Override
	protected void onDestroy() {
		/*
		 * The activity is getting destroyed but the application level state
		 * like singletons, statics, etc will continue as long as the application
		 * is running.
		 */
		Logger.d(this, "CandiRadarActivity destroyed");
		super.onDestroy();

		/* This is the only place we manually stop the analytics session. */
		Tracker.stopSession(Aircandi.getInstance().getUser());

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

	@SuppressWarnings("ucd")
	public void startScanService(int scanInterval) {

		/* Start first scan right away */
		Logger.d(this, "Starting wifi scan service");
		final Intent scanIntent = new Intent(Aircandi.applicationContext, ScanService.class);
		startService(scanIntent);

		/* Setup a scanning schedule */
		if (scanInterval > 0) {
			final AlarmManager alarmManager = (AlarmManager) getSystemService(Service.ALARM_SERVICE);
			final PendingIntent pendingIntent = PendingIntent.getService(Aircandi.applicationContext, 0, scanIntent, 0);
			alarmManager.cancel(pendingIntent);
			alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP
					, SystemClock.elapsedRealtime() + scanInterval
					, scanInterval, pendingIntent);
		}
	}

	@SuppressWarnings("ucd")
	public void stopScanService() {
		final AlarmManager alarmManager = (AlarmManager) getSystemService(Service.ALARM_SERVICE);
		final Intent scanIntent = new Intent(Aircandi.applicationContext, ScanService.class);
		final PendingIntent pendingIntent = PendingIntent.getService(Aircandi.applicationContext, 0, scanIntent, 0);
		alarmManager.cancel(pendingIntent);
		Logger.d(this, "Stopped wifi scan service");
	}

	@Override
	protected int getLayoutId() {
		return R.layout.radar_form;
	}

}