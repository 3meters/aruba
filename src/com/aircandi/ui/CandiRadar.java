package com.aircandi.ui;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
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
import com.aircandi.beta.R;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.BeaconsLockedEvent;
import com.aircandi.components.BusProvider;
import com.aircandi.components.CommandType;
import com.aircandi.components.EntitiesChangedEvent;
import com.aircandi.components.EntitiesForBeaconsFinishedEvent;
import com.aircandi.components.EntityModel;
import com.aircandi.components.Exceptions;
import com.aircandi.components.FontManager;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.LocationChangedEvent;
import com.aircandi.components.LocationLockedEvent;
import com.aircandi.components.LocationManager;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NetworkManager.ConnectedState;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.PlacesNearLocationFinishedEvent;
import com.aircandi.components.ProxiManager;
import com.aircandi.components.ProxiManager.ModelResult;
import com.aircandi.components.ProxiManager.ScanReason;
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

public class CandiRadar extends CandiActivity {

	private final Handler			mHandler			= new Handler();

	private Number					mEntityModelRefreshDate;
	private Number					mEntityModelActivityDate;
	private Number					mEntityModelBeaconDate;
	private Integer					mWifiState			= WifiManager.WIFI_STATE_UNKNOWN;

	private PullToRefreshListView	mList;

	private SoundPool				mSoundPool;
	private int						mNewCandiSoundId;
	private Boolean					mInitialized		= false;

	private final List<Entity>		mEntities			= new ArrayList<Entity>();
	private RadarListAdapter		mRadarAdapter;
	private Boolean					mFreshWindow		= false;
	private Boolean					mUpdateCheckNeeded	= false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!isFinishing()) {
			Aircandi.stopwatch4.start("Creating radar");
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
		ProxiManager.getInstance().getEntityModel().removeAllEntities();

		/* Other UI references */
		mList = (PullToRefreshListView) findViewById(R.id.radar_list);
		mList.getRefreshableView().setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				final Entity entity = mRadarAdapter.getItems().get(position);
				showCandiForm(entity, entity.synthetic);
			}
		});

		/* Adapter snapshots the items in mEntities */
		mRadarAdapter = new RadarListAdapter(this, mEntities);
		mList.getRefreshableView().setAdapter(mRadarAdapter);
		
		/* Refresh listener */
		mList.setOnRefreshListener(new OnRefreshListener<ListView>() {

			@Override
			public void onRefresh(PullToRefreshBase<ListView> refreshView) {
				doRefresh();
			}
			
		});	
		
		mList.getLoadingLayoutProxy().setRefreshingLabel("scanning...");
		mList.getLoadingLayoutProxy().setPullLabel("pull to scan...");
		mList.getLoadingLayoutProxy().setReleaseLabel("release to scan...");
		mList.getLoadingLayoutProxy().setLoadingDrawable(null);
		mList.getLoadingLayoutProxy().setTextTypeface(FontManager.getFontRobotoRegular());

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
	public void onQueryWifiScanReceived(final QueryWifiScanReceivedEvent event) {

		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Aircandi.stopwatch1.segmentTime("Wifi scan received event");
				Tracker.sendTiming("radar", Aircandi.stopwatch1.getTotalTimeMills(), "wifi_scan_received", null, Aircandi.getInstance().getUser());
				Logger.d(CandiRadar.this, "Query wifi scan received event: locking beacons");

				if (event.wifiList != null) {
					ProxiManager.getInstance().lockBeacons();
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
				Aircandi.stopwatch1.segmentTime("Beacons locked event");
				mEntityModelBeaconDate = ProxiManager.getInstance().getEntityModel().getLastBeaconLockedDate();
				new AsyncTask() {

					@Override
					protected Object doInBackground(Object... params) {
						Thread.currentThread().setName("GetEntitiesForBeacons");
						final ServiceResponse serviceResponse = ProxiManager.getInstance().getEntitiesForBeacons();
						return serviceResponse;
					}

					@Override
					protected void onPostExecute(Object result) {
						final ServiceResponse serviceResponse = (ServiceResponse) result;
						if (serviceResponse.responseCode != ResponseCode.Success) {
							mCommon.handleServiceError(serviceResponse, ServiceOperation.PlaceSearch);
							mCommon.hideBusy(true);
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
				Aircandi.stopwatch1.stop("Search for places by beacon complete");
				Tracker.sendTiming("radar", Aircandi.stopwatch1.getTotalTimeMills(), "entities_for_beacons", null, Aircandi.getInstance().getUser());
				Logger.d(CandiRadar.this, "Entities for beacons finished event: ** done **");
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
					mCommon.updateAccuracyIndicator(LocationManager.getInstance().getLocationLocked());
					BusProvider.getInstance().post(new LocationLockedEvent(LocationManager.getInstance().getLocationLocked()));

					if (Aircandi.stopwatch2.isStarted()) {
						Aircandi.stopwatch2.stop("Location acquired");
						Tracker.sendTiming("location", Aircandi.stopwatch2.getTotalTimeMills(), "location_acquired", null, Aircandi.getInstance().getUser());
					}

					final Observation observation = LocationManager.getInstance().getObservationLocked();
					if (observation != null) {

						mCommon.showBusy(true);

						new AsyncTask() {

							@Override
							protected Object doInBackground(Object... params) {

								Logger.d(CandiRadar.this, "Location changed event: getting places near location");
								Thread.currentThread().setName("GetPlacesNearLocation");
								Aircandi.stopwatch2.start("Get places near location");

								final ServiceResponse serviceResponse = ProxiManager.getInstance().getPlacesNearLocation(observation);
								return serviceResponse;
							}

							@Override
							protected void onPostExecute(Object result) {
								final ServiceResponse serviceResponse = (ServiceResponse) result;
								if (serviceResponse.responseCode != ResponseCode.Success) {
									mCommon.handleServiceError(serviceResponse, ServiceOperation.PlaceSearch);
									mCommon.hideBusy(true);
								}
							}

						}.execute();
					}
					else {
						mCommon.hideBusy(true);
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
				Logger.d(CandiRadar.this, "Places near location finished event: ** done **");
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
				Aircandi.stopwatch4.stop("Aircandi initialization finished and got first entities");

				mEntityModelRefreshDate = ProxiManager.getInstance().getEntityModel().getLastBeaconRefreshDate();
				mEntityModelActivityDate = ProxiManager.getInstance().getEntityModel().getLastActivityDate();

				/* Point radar adapter at the updated entities */
				final int previousCount = mRadarAdapter.getCount();
				final List<Entity> entities = event.entities;
				mRadarAdapter.setItems(entities);
				mRadarAdapter.notifyDataSetChanged();
				
				/* Clear loading */
				mList.onRefreshComplete();				

				/* Add some sparkle */
				if (previousCount == 0 && entities.size() > 0) {
					if (Aircandi.settings.getBoolean(CandiConstants.PREF_SOUND_EFFECTS, CandiConstants.PREF_SOUND_EFFECTS_DEFAULT)) {
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
	public void onWifiChanged(final WifiChangedEvent event) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				final Integer wifiState = event.wifiState;
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
		/*
		 * This only gets called by a user clicking the refresh button.
		 */
		Logger.d(this, "Starting refresh");
		Tracker.sendEvent("ui_action", "refresh_radar", null, 0, Aircandi.getInstance().getUser());
		searchForPlaces();
	}

	private void searchForPlaces() {

		/* We won't perform a search if location access is disabled */
		if (!LocationManager.getInstance().isLocationAccessEnabled()) {
			startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
			AnimUtils.doOverridePendingTransition(CandiRadar.this, TransitionType.PageToForm);
		}
		else {
			new AsyncTask() {

				@Override
				protected void onPreExecute() {
					mCommon.showBusy(true);
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
						mCommon.hideBusy(true);
						if (connectedState == ConnectedState.WalledGarden) {
							mCommon.showAlertDialogSimple(null, getString(R.string.error_connection_walled_garden));
						}
					}
				}
			}.execute();
		}
	}

	private void searchForPlacesByBeacon() {
		Aircandi.stopwatch1.start("Search for places by beacon");
		mWifiState = NetworkManager.getInstance().getWifiState();
		if (NetworkManager.getInstance().isWifiEnabled()) {
			ProxiManager.getInstance().getEntityModel().clearBeacons();
			ProxiManager.getInstance().scanForWifi(ScanReason.query);
		}
	}

	private void searchForPlacesByLocation() {
		Aircandi.stopwatch2.start("Lock location");
		LocationManager.getInstance().setLocationLocked(null);
		LocationManager.getInstance().lockLocationBurst();
		Location location = LocationManager.getInstance().getLastKnownLocation();
		if (location != null) {
			LocationManager.getInstance().setLocation(location);
		}
	}

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	private void showCandiForm(Entity entity, Boolean upsize) {

		final IntentBuilder intentBuilder = new IntentBuilder(this, CandiForm.class)
				.setCommandType(CommandType.View)
				.setEntityId(entity.id)
				.setParentEntityId(entity.parentId)
				.setEntityType(entity.type);

		if (entity.parentId != null) {
			intentBuilder.setCollectionId(entity.getParent().id);
		}

		final Intent intent = intentBuilder.create();
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
				mList.getRefreshableView().setSelection(0);
			}
		});
	}

	private Boolean handleUpdateChecks(final RequestListener listener) {

		/* Update check */
		final Boolean updateCheckNeeded = ProxiManager.getInstance().updateCheckNeeded();
		if (updateCheckNeeded) {

			new AsyncTask() {

				@Override
				protected Object doInBackground(Object... params) {
					Thread.currentThread().setName("CheckForUpdate");
					final ModelResult result = ProxiManager.getInstance().checkForUpdate();
					return result;
				}

				@Override
				protected void onPostExecute(Object response) {
					final ModelResult result = (ModelResult) response;
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

		if (mPrefChangeReloadNeeded) {
			final Intent intent = getIntent();
			startActivity(intent);
			finish();
			return;
		}

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

		/* Reset the accuracy indicator */
		mCommon.updateAccuracyIndicator(LocationManager.getInstance().getLocationLocked());
	}

	@Override
	protected void onStop() {
		/*
		 * Fired when starting another activity and we lose our window.
		 */
		/* Start listening for events */
		disableEvents();

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
		mCommon.hideBusy(true);

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
		mCommon.stopScanService();

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
				&& Aircandi.settings.getBoolean(CandiConstants.PREF_ENABLE_DEV, CandiConstants.PREF_ENABLE_DEV_DEFAULT)
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
			mUpdateCheckNeeded = false;
			if (Aircandi.applicationUpdateRequired) {
				showUpdateAlert(null);
			}
			else {
				/* Check for update */
				if (NetworkManager.getInstance().getConnectedState() == ConnectedState.Normal) {
					mUpdateCheckNeeded = handleUpdateChecks(new RequestListener() {

						/* Never gets called if we don't have a good network connection */
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
			mFreshWindow = false;
		}
	}

	private void manageData() {

		final EntityModel entityModel = ProxiManager.getInstance().getEntityModel();
		if (mEntityModelActivityDate == null) {
			/*
			 * Get set everytime onEntitiesChanged gets called. Means
			 * we have never completed even the first search for entities.
			 */
			Logger.d(this, "Start first place search");
			searchForPlaces();
		}
		else if (mPrefChangeNewSearchNeeded) {
			/*
			 * Gets set based on evaluation of pref changes
			 */
			Logger.d(this, "Start place search because of preference change");
			mPrefChangeNewSearchNeeded = false;
			searchForPlaces();
		}
		else if (mPrefChangeRefreshUiNeeded) {
			/*
			 * Gets set based on evaluation of pref changes
			 */
			Logger.d(this, "Refresh Ui because of preference change");
			mPrefChangeRefreshUiNeeded = false;
			mCommon.showBusy(true);
			invalidateOptionsMenu();
			mRadarAdapter.getItems().clear();
			mRadarAdapter.getItems().addAll(ProxiManager.getInstance().getEntityModel().getAllPlaces(false));
			mRadarAdapter.notifyDataSetChanged();
			mCommon.hideBusy(true);
		}
		else if (LocationManager.getInstance().getLocationLocked() == null) {
			/*
			 * Gets set everytime we accept a location change in onLocationChange. Means
			 * we didn't get an acceptable fix yet from either the network or gps providers.
			 */
			Logger.d(this, "Start place search because didn't complete location fix");
			LocationManager.getInstance().lockLocationBurst();
		}
		else if (ProxiManager.getInstance().refreshNeeded(LocationManager.getInstance().getLocationLocked())) {
			/*
			 * We check to see if it's been awhile since the last search or if we
			 * can determin the user has moved.
			 */
			Logger.d(this, "Start place search because of staleness or location change");
			searchForPlaces();
		}
		else if (mWifiState != null
				&& NetworkManager.getInstance().getWifiState() != null
				&& !mWifiState.equals(NetworkManager.getInstance().getWifiState())) {
			/*
			 * Changes in wifi state have a big effect on what we can show
			 * for a search.
			 */
			Logger.d(this, "Start place search because wifi state has changed");
			searchForPlaces();
		}
		else if ((entityModel.getLastBeaconLockedDate() != null && mEntityModelBeaconDate != null)
				&& (entityModel.getLastBeaconLockedDate().longValue() > mEntityModelBeaconDate.longValue())) {
			/*
			 * The beacons we are locked to have changed so we need to search for new places linked
			 * to beacons.
			 */
			Logger.d(this, "Refresh places for beacons because beacon date has changed");
			mEntityModelBeaconDate = ProxiManager.getInstance().getEntityModel().getLastBeaconLockedDate();
			new AsyncTask() {

				@Override
				protected Object doInBackground(Object... params) {
					Thread.currentThread().setName("GetEntitiesForBeacons");
					final ServiceResponse serviceResponse = ProxiManager.getInstance().getEntitiesForBeacons();
					return serviceResponse;
				}

			}.execute();
		}
		else if ((entityModel.getLastBeaconRefreshDate() != null && mEntityModelRefreshDate != null
				&& entityModel.getLastBeaconRefreshDate().longValue() > mEntityModelRefreshDate.longValue())
				|| (entityModel.getLastActivityDate() != null && mEntityModelActivityDate != null
				&& entityModel.getLastActivityDate().longValue() > mEntityModelActivityDate.longValue())) {
			/*
			 * Everytime we show details for a place, we fetch place details from the service
			 * when in turn get pushed into the cache and activityDate gets tickled.
			 */
			Logger.d(this, "Update radar ui because of detected entity model change");
			mHandler.postDelayed(new Runnable() {

				@Override
				public void run() {
					mCommon.showBusy(true);
					invalidateOptionsMenu();
					mRadarAdapter.getItems().clear();
					mRadarAdapter.getItems().addAll(ProxiManager.getInstance().getEntityModel().getAllPlaces(false));
					mRadarAdapter.notifyDataSetChanged();
					mCommon.hideBusy(true);
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
		catch (Exception e) {} // $codepro.audit.disable emptyCatchClause
	}

	@Override
	protected int getLayoutId() {
		return R.layout.candi_radar;
	}

}