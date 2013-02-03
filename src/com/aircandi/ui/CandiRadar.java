package com.aircandi.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi;
import com.aircandi.CandiConstants;
import com.aircandi.PlacesConstants;
import com.aircandi.R;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.BeaconsLockedEvent;
import com.aircandi.components.BusProvider;
import com.aircandi.components.CommandType;
import com.aircandi.components.EntitiesChangedEvent;
import com.aircandi.components.EntitiesForBeaconsFinishedEvent;
import com.aircandi.components.EntitiesForLocationFinishedEvent;
import com.aircandi.components.EntityChangedEvent;
import com.aircandi.components.Exceptions;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.LocationChangedEvent;
import com.aircandi.components.LocationManager;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.PlacesNearLocationFinishedEvent;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.ProxiExplorer.EntityModel;
import com.aircandi.components.ProxiExplorer.ModelResult;
import com.aircandi.components.ProxiExplorer.ScanReason;
import com.aircandi.components.QueryWifiScanReceivedEvent;
import com.aircandi.components.RadarListAdapter;
import com.aircandi.components.Tracker;
import com.aircandi.components.WifiChangedEvent;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.service.ProxibaseService.RequestListener;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Observation;
import com.aircandi.ui.base.CandiActivity;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;
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
 * get more control over thread pooling but it requires Android version 11/3.0 (we currently target 7/2.1 and higher).
 * AsyncTasks are hard-coded with a low priority and continue their work even if the activity is paused.
 */

/*
 * Bitmap Management
 * 
 * gc calls are evil but necessary sometimes. It forces code exection to stop while
 * the gc makes an explicit garbage pass. Behavior may be a bit different with
 * the introduction of concurrent gc in Gingerbread (v2.3)
 * 
 * Explicit gc calls to free bitmap memory:
 * 
 * - EntityForm: onDestroy.
 * - PictureSearch: onDestroy.
 * - ProfileForm: onDestroy.
 * - SignUpForm: onDestroy.
 * 
 * Explicit bitmap recycling
 * 
 * - Anyplace where a new bitmap has been processed from another bitmap.
 * - Releasing bitmaps when forms are destroyed.
 * - Releasing bitmaps when list items are reused.
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

/*
 * Scan management
 * 
 * There are three cases that trigger scans:
 * 
 * - First run scan: When application is first started, we load the entity model with a full scan. The
 * entity model lives on even if the radar activity is killed.
 * 
 * - User requested scan: (doRefresh) This can be either full or standard.
 * 
 * - Autoscan: Causes another scan to be scheduled as soon as a scan is finished. We also need
 * to handle suspending autoscan when the activity is paused and restarting when resumed.
 * ---Starting: BeaconScanWatcher, onWindowFocusChange
 * ---Stopping: onStop, scanForBeacons
 * 
 * - Fixup scan: These are done because a settings change requires that the UI is rebuilt.
 */

public class CandiRadar extends CandiActivity {

	private Handler				mHandler			= new Handler();

	private Number				mEntityModelRefreshDate;
	private Number				mEntityModelActivityDate;
	private Location			mActiveLocation		= null;
	private Integer				mWifiState			= WifiManager.WIFI_STATE_UNKNOWN;

	private ListView			mList;

	private SoundPool			mSoundPool;
	private int					mNewCandiSoundId;
	private Boolean				mInitialized		= false;

	private List<Entity>		mEntities			= new ArrayList<Entity>();
	private RadarListAdapter	mRadarAdapter;
	private Boolean				mFreshWindow		= false;
	private Boolean				mUpdateCheckNeeded	= false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!isFinishing()) {
			/*
			 * Get setup for location snapshots. Initialize will populate location
			 * with the best of any cached location fixes. A single update will
			 * be launched if the best cached location fix doesn't meet our freshness
			 * and accuracy requirements.
			 */
			LocationManager.getInstance().initialize(getApplicationContext());

			if (!LocationManager.getInstance().isLocationAccessEnabled()) {
				/* We won't continue if location services are disabled */
				startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
				AnimUtils.doOverridePendingTransition(CandiRadar.this, TransitionType.PageToForm);
				finish();
			}

			initialize();

			/*
			 * We alert that wifi isn't enabled. If the user end up enabling wifi,
			 * we will get that event and refresh radar with beacon support.
			 */
			if (NetworkManager.getInstance().isWifiTethered()
					|| (!NetworkManager.getInstance().isWifiEnabled() && !Aircandi.usingEmulator)) {

				showWifiAlertDialog(NetworkManager.getInstance().isWifiTethered()
						? R.string.alert_wifi_tethered
						: R.string.alert_wifi_disabled
						, null);
			}
		}
	}

	private void initialize() {
		/*
		 * Here we initialize activity level state. Only called from
		 * onCreate.
		 */

		/* Save that we've been run once. */
		Aircandi.settingsEditor.putBoolean(PlacesConstants.SP_KEY_RUN_ONCE, true);
		Aircandi.settingsEditor.commit();

		/* Always reset the entity cache */
		ProxiExplorer.getInstance().getEntityModel().removeAllEntities();

		/* Initialize preferences */
		updatePreferences(true);

		/* Other UI references */
		mList = (ListView) findViewById(R.id.radar_list);
		mList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				final Entity entity = mRadarAdapter.getItems().get(position);
				showCandiForm(entity, entity.synthetic);
			}
		});

		/* Adapter snapshots the items in mEntities */
		mRadarAdapter = new RadarListAdapter(this, mEntities);
		mList.setAdapter(mRadarAdapter);

		/* Store sounds */
		mSoundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 100);
		mNewCandiSoundId = mSoundPool.load(this, R.raw.notification_candi_discovered, 1);

		mInitialized = true;
	}

	// --------------------------------------------------------------------------------------------
	// Event bus routines
	// --------------------------------------------------------------------------------------------

	@Subscribe
	@SuppressWarnings("ucd")
	public void onQueryWifiScanReceived(QueryWifiScanReceivedEvent event) {

		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Aircandi.stopwatch1.segmentTime("Wifi scan received event");
				Logger.d(CandiRadar.this, "Query wifi scan received event: locking beacons");
				ProxiExplorer.getInstance().lockBeacons();
			}
		});
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onBeaconsLocked(BeaconsLockedEvent event) {

		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Aircandi.stopwatch1.segmentTime("Beacons locked event");
				Logger.d(CandiRadar.this, "Beacons locked event: get entities for beacons");
				new AsyncTask() {

					@Override
					protected Object doInBackground(Object... params) {
						Thread.currentThread().setName("GetEntitiesForBeacons");
						ProxiExplorer.getInstance().getEntitiesForBeacons();
						return null;
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
				Aircandi.stopwatch1.segmentTime("Entities for beacons finished event");
				Logger.d(CandiRadar.this, "Entities for beacons finished event: get location fix");
				mActiveLocation = null;
				LocationManager.getInstance().lockLocationBurst();
			}
		});
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onLocationChanged(final LocationChangedEvent event) {

		runOnUiThread(new Runnable() {

			@Override
			public void run() {

				Location location = event.location;
				if (location != null) {

					/*
					 * We pass on gps updates that are too small an improvement to warrent
					 * pulling entities from the service again.
					 */
					if (mActiveLocation != null
							&& location.getProvider().equals("gps")
							&& mActiveLocation.getProvider().equals("gps")) {
						int accuracyDelta = (int) (location.getAccuracy() - mActiveLocation.getAccuracy());
						boolean isSignificantlyMoreAccurate = (accuracyDelta < -20);
						if (!isSignificantlyMoreAccurate) {
							return;
						}
					}

					mActiveLocation = location;

					Aircandi.stopwatch1.segmentTime("Location acquired event");
					Logger.d(CandiRadar.this, "Location change event: getting entities for location");
					final Observation observation = LocationManager.getInstance().getObservationForLocation(mActiveLocation);
					if (observation != null) {

						new AsyncTask() {

							@Override
							protected Object doInBackground(Object... params) {
								Thread.currentThread().setName("GetEntitiesForLocation");
								ProxiExplorer.getInstance().getEntitiesForLocation();
								return null;
							}

						}.execute();
					}
				}
			}
		});
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onEntitiesForLocationFinished(EntitiesForLocationFinishedEvent event) {

		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Aircandi.stopwatch1.segmentTime("Entities for location finished event");
				Logger.d(CandiRadar.this, "Entities for location finished event: getting places near location");
				final Observation observation = LocationManager.getInstance().getObservationForLocation(mActiveLocation);
				if (observation != null) {
					new AsyncTask() {

						@Override
						protected Object doInBackground(Object... params) {
							Thread.currentThread().setName("GetPlacesNearLocation");
							ProxiExplorer.getInstance().getPlacesNearLocation(observation);
							return null;
						}

					}.execute();
				}
			}
		});
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onPlacesNearLocationFinished(PlacesNearLocationFinishedEvent event) {

		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Aircandi.stopwatch1.segmentTime("Places near location finished event");
				Aircandi.stopwatch1.stop("Search for places stopped");
				Logger.d(CandiRadar.this, "Places near location finished event: ** all done **");
				mCommon.hideBusy(true);
			}
		});
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onEntitiesChanged(final EntitiesChangedEvent event) {

		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Logger.d(CandiRadar.this, "Entities changed event: updating radar");

				mEntityModelRefreshDate = ProxiExplorer.getInstance().getEntityModel().getLastRefreshDate();
				mEntityModelActivityDate = ProxiExplorer.getInstance().getEntityModel().getLastActivityDate();

				/* Point radar adapter at the updated entities */
				int previousCount = mRadarAdapter.getCount();
				List<Entity> entities = event.entities;
				mRadarAdapter.setItems(entities);
				mRadarAdapter.notifyDataSetChanged();

				/* Add some sparkle */
				if (previousCount == 0 && entities.size() > 0) {
					if (mPrefSoundEffects) {
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

	@Subscribe
	@SuppressWarnings("ucd")
	public void onEntityChanged(final EntityChangedEvent event) {
		Logger.d(CandiRadar.this, "Entities changed event: updating radar");
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Entity entityNew = event.entity;
				entityNew.getDistance(); // To force distance computation
				if (mRadarAdapter.getItems().contains(entityNew)) {
					mRadarAdapter.getItems().set(mRadarAdapter.getItems().indexOf(entityNew), entityNew);
				}
				else {
					mRadarAdapter.getItems().set(0, entityNew);
				}
				mRadarAdapter.notifyDataSetChanged();
			}
		});
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onWifiChanged(final WifiChangedEvent event) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Integer wifiState = event.wifiState;
				if (wifiState == WifiManager.WIFI_STATE_ENABLED || wifiState == WifiManager.WIFI_STATE_DISABLED) {
					Logger.d(this, "Wifi state change, starting place search");
					searchForPlaces();
				}
			}
		});
	}

	// --------------------------------------------------------------------------------------------
	// Entity routines
	// --------------------------------------------------------------------------------------------

	public void doRefresh() {
		Logger.d(this, "Starting refresh");
		Tracker.trackEvent("Radar", "Refresh", "Full", 0);
		searchForPlaces();
	}

	private void searchForPlaces() {

		/* We won't perform a search if location access is disabled */
		if (!LocationManager.getInstance().isLocationAccessEnabled()) {
			startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
			AnimUtils.doOverridePendingTransition(CandiRadar.this, TransitionType.PageToForm);
		}
		else {
			/* Start wifi scan. Once received, the search process continues to the next step. */
			mCommon.showBusy();
			Aircandi.stopwatch1.start("Search for places");
			mWifiState = NetworkManager.getInstance().getWifiState();
			if (NetworkManager.getInstance().isWifiEnabled()) {
				ProxiExplorer.getInstance().scanForWifi(ScanReason.query);
			}
			else {
				mActiveLocation = null;
				LocationManager.getInstance().lockLocationBurst();
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	private void showCandiForm(Entity entity, Boolean upsize) {

		IntentBuilder intentBuilder = new IntentBuilder(this, CandiForm.class)
				.setCommandType(CommandType.View)
				.setEntityId(entity.id)
				.setParentEntityId(entity.parentId)
				.setEntityType(entity.type);

		if (entity.parentId != null) {
			intentBuilder.setCollectionId(entity.getParent().id);
		}

		Intent intent = intentBuilder.create();
		if (upsize) {
			intent.putExtra(CandiConstants.EXTRA_UPSIZE_SYNTHETIC, true);
		}

		startActivity(intent);
		AnimUtils.doOverridePendingTransition(this, TransitionType.RadarToPage);
	}

	@SuppressWarnings("unused")
	private void scrollToTop() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				mList.setSelection(0);
			}
		});
	}

	private Boolean handleUpdateChecks(final RequestListener listener) {

		/* Update check */
		Boolean updateCheckNeeded = ProxiExplorer.getInstance().updateCheckNeeded();
		if (updateCheckNeeded) {

			new AsyncTask() {

				@Override
				protected Object doInBackground(Object... params) {
					Thread.currentThread().setName("CheckForUpdate");
					ModelResult result = ProxiExplorer.getInstance().checkForUpdate();
					return result;
				}

				@Override
				protected void onPostExecute(Object response) {
					ModelResult result = (ModelResult) response;
					if (result.serviceResponse.responseCode == ResponseCode.Success) {
						if (Aircandi.applicationUpdateNeeded) {
							invalidateOptionsMenu();
							showUpdateAlert(listener);
						}
						else {
							listener.onComplete(false);
						}
					}
					else {
						mCommon.handleServiceError(result.serviceResponse, ServiceOperation.CheckUpdate, CandiRadar.this);
					}
				}
			}.execute();
		}
		return updateCheckNeeded;
	}

	// --------------------------------------------------------------------------------------------
	// Location routines
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// System callbacks
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onStart() {
		/*
		 * Called everytime the activity is started or restarted.
		 */
		Logger.d(this, "CandiRadarActivity starting");
		super.onStart();
		if (!mInitialized) return;

		/* Check for location service */
		if (!LocationManager.getInstance().isLocationAccessEnabled()) {
			/* We won't continue if location services are disabled */
			startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
			AnimUtils.doOverridePendingTransition(CandiRadar.this, TransitionType.PageToForm);
			finish();
		}

		if (CandiConstants.DEBUG_TRACE) {
			Debug.startMethodTracing("aircandi", 100000000);
		}

		/* Start listening for events */
		enableEvents();
	}

	@Override
	protected void onStop() {
		/*
		 * Fired when starting another activity and we lose our window.
		 */
		/* Start listening for events */
		disableEvents();

		/* Stop any location burst that might be active */
		LocationManager.getInstance().stopLocationBurst();

		Logger.d(this, "CandiRadarActivity stopped");
		if (CandiConstants.DEBUG_TRACE) {
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

		if (Aircandi.getInstance().getUser() != null
				&& Aircandi.settings.getBoolean(Preferences.PREF_ENABLE_DEV, true)
				&& Aircandi.getInstance().getUser().isDeveloper != null
				&& Aircandi.getInstance().getUser().isDeveloper) {
			mCommon.stopScanService();
		}

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
		if (!mInitialized) return;
		mFreshWindow = true;

		if (Aircandi.getInstance().getUser() != null
				&& Aircandi.settings.getBoolean(Preferences.PREF_ENABLE_DEV, true)
				&& Aircandi.getInstance().getUser().isDeveloper != null
				&& Aircandi.getInstance().getUser().isDeveloper) {
			mCommon.startScanService(CandiConstants.INTERVAL_SCAN_WIFI);
		}
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);

		if (!mInitialized) return;

		if (hasFocus && (mFreshWindow || mUpdateCheckNeeded)) {
			mFreshWindow = false;
			mUpdateCheckNeeded = false;
			if (Aircandi.applicationUpdateRequired) {
				showUpdateAlert(null);
			}
			else {
				/* Check for update */
				mUpdateCheckNeeded = handleUpdateChecks(new RequestListener() {
					@Override
					public void onComplete(Object dialogDisplayed) {
						/*
						 * We don't do anything right now because window focus returning
						 * when dismissing the update dialog will restart the logic
						 * to trigger data updates.
						 */
						if (!(Boolean) dialogDisplayed) {
							manageData();
						}
					}
				});

				if (!mUpdateCheckNeeded) {
					manageData();
				}
			}
		}
	}

	private void manageData() {

		EntityModel entityModel = ProxiExplorer.getInstance().getEntityModel();
		if (mEntityModelRefreshDate == null) {
			/*
			 * Get set everytime onEntitiesChanged gets called. Means
			 * we have never completed even the first search for entities.
			 */
			Logger.d(this, "Start first place search");
			Aircandi.stopwatch1.stop("Aircandi start");
			searchForPlaces();
		}
		else if (mPrefChangeRefreshNeeded) {
			/*
			 * Gets set based on evaluation of pref changes
			 */
			Logger.d(this, "Start place search because of preference change");
			mPrefChangeRefreshNeeded = false;
			searchForPlaces();
		}
		else if (mActiveLocation == null) {
			/*
			 * Gets set everytime we accept a location change in onLocationChange. Means
			 * we didn't get an acceptable fix yet from either the network or gps providers.
			 */
			Logger.d(this, "Start place search because didn't complete location fix");
			LocationManager.getInstance().lockLocationBurst();
		}
		else if (ProxiExplorer.getInstance().refreshNeeded(mActiveLocation)) {
			/*
			 * We check to see if it's been awhile since the last search or if we
			 * can determin the user has moved.
			 */
			Logger.d(this, "Start place search because of staleness or location change");
			searchForPlaces();
		}
		else if (mWifiState != NetworkManager.getInstance().getWifiState()) {
			/*
			 * Changes in wifi state have a big effect on what we can show
			 * for a search.
			 */
			Logger.d(this, "Start place search because wifi state has changed");
			searchForPlaces();
		}
		else if ((entityModel.getLastRefreshDate() != null
				&& entityModel.getLastRefreshDate().longValue() > mEntityModelRefreshDate.longValue())
				|| (entityModel.getLastActivityDate() != null
				&& entityModel.getLastActivityDate().longValue() > mEntityModelActivityDate.longValue())) {
			/*
			 * Everytime we show details for a place, we fetch place details from the service
			 * when in turn get pushed into the cache and activityDate gets tickled.
			 */
			Logger.d(this, "Update radar ui because of detected entity model change");
			mHandler.postDelayed(new Runnable() {

				@Override
				public void run() {
					mCommon.showBusy();
					invalidateOptionsMenu();
					mRadarAdapter.getItems().clear();
					mRadarAdapter.getItems().addAll(ProxiExplorer.getInstance().getEntityModel().getPlaces());
					mRadarAdapter.notifyDataSetChanged();
					mCommon.hideBusy(false);
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
		Tracker.stopSession();

		/* Don't count on this always getting called when this activity is killed */
		try {
			BitmapManager.getInstance().stopBitmapLoaderThread();
		}
		catch (Exception exception) {
			Exceptions.Handle(exception);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
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
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		mCommon.doOptionsItemSelected(menuItem);
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	private void enableEvents() {
		BusProvider.getInstance().register(this);
	}

	private void disableEvents() {
		try {
			BusProvider.getInstance().unregister(this);
		}
		catch (Exception e) {}
	}

	@SuppressWarnings("unused")
	private String getGoogleAnalyticsId() {
		Properties properties = new Properties();

		try {
			properties.load(getClass().getResourceAsStream("/com/aircandi/google_analytics.properties"));
			String analyticsId = properties.getProperty("analyticsId");
			return analyticsId;
		}
		catch (IOException exception) {
			throw new IllegalStateException("Unable to retrieve google analytics id");
		}
	}

	@Override
	protected int getLayoutId() {
		return R.layout.candi_radar;
	}

}