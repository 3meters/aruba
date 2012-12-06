package com.aircandi;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import android.content.Intent;
import android.content.res.Configuration;
import android.location.Criteria;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi.CandiTask;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.CommandType;
import com.aircandi.components.Events;
import com.aircandi.components.Events.EventHandler;
import com.aircandi.components.Exceptions;
import com.aircandi.components.FontManager;
import com.aircandi.components.GeoLocationManager;
import com.aircandi.components.ImageCache;
import com.aircandi.components.ImageManager;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ResponseDetail;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.ProxiExplorer.EntityModel;
import com.aircandi.components.Tracker;
import com.aircandi.core.CandiConstants;
import com.aircandi.service.ProxibaseService.RequestListener;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Observation;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;
import com.aircandi.utilities.ImageUtils;
import com.aircandi.widgets.BounceScrollView;
import com.aircandi.widgets.CandiView;
import com.aircandi.widgets.FlowLayout;
import com.amazonaws.auth.BasicAWSCredentials;

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

	private Handler						mHandler		= new Handler();
	public static BasicAWSCredentials	mAwsCredentials	= null;

	private Number						mEntityModelRefreshDate;
	private Number						mEntityModelActivityDate;

	private ViewGroup					mProgressRadar;
	private BounceScrollView			mScrollView;

	private SoundPool					mSoundPool;
	private int							mNewCandiSoundId;
	private Boolean						mInitialized	= false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Aircandi.getInstance().setLaunchedFromRadar(true);
		super.onCreate(savedInstanceState);

		if (Build.PRODUCT.contains("sdk")) {
			Aircandi.usingEmulator = true;
		}
		/*
		 * Get setup for location snapshots. Initialize will populate location
		 * with the best of any cached location fixes. A single update will
		 * be launched if the best cached location fix doesn't meet our freshness
		 * and accuracy requirements.
		 */
		GeoLocationManager.getInstance().setContext(getApplicationContext());
		GeoLocationManager.getInstance().initialize();

		if (!GeoLocationManager.getInstance().isLocationAccessEnabled()) {
			/* We won't continue if location services are disabled */
			startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
			AnimUtils.doOverridePendingTransition(CandiRadar.this, TransitionType.CandiPageToForm);
			finish();
		}

		/* Connectivity monitoring */
		NetworkManager.getInstance().setContext(getApplicationContext());
		NetworkManager.getInstance().initialize();

		/* We alert that wifi isn't enabled */
		if (NetworkManager.getInstance().isTethered()
				|| (!NetworkManager.getInstance().isWifiEnabled() && !Aircandi.usingEmulator)) {

			showWifiAlertDialog(NetworkManager.getInstance().isTethered()
					? R.string.alert_wifi_tethered
					: R.string.alert_wifi_disabled
					, new RequestListener() {

						@Override
						public void onComplete() {
							if (Aircandi.firstStartApp) {
								initializeApp();
							}
							initialize();
						}
					});
		}
		else {
			if (Aircandi.firstStartApp) {
				initializeApp();
			}
			initialize();
		}
	}

	private Boolean initializeApp() {
		/*
		 * Here we initialize application level state that will continue
		 * even if this activity is destroyed.
		 */

		/* Proxibase sdk components */
		ProxiExplorer.getInstance().setContext(getApplicationContext());
		ProxiExplorer.getInstance().setUsingEmulator(Aircandi.usingEmulator);
		ProxiExplorer.getInstance().initialize();

		/* Image cache */
		ImageManager.getInstance().setImageCache(new ImageCache(getApplicationContext(), CandiConstants.CACHE_PATH, 100, 16));
		ImageManager.getInstance().setFileCacheOnly(true);
		ImageManager.getInstance().getImageLoader().setWebView((WebView) findViewById(R.id.webview));
		ImageManager.getInstance().setActivity(this);

		/* Auto signin the user */
		mCommon.signinAuto();
		Aircandi.firstStartApp = false;
		return true;

	}

	private void initialize() {
		/*
		 * Here we initialize activity level state. Only called from
		 * onCreate.
		 */

		/* Used by other activities to determine if they were auto launched after a crash */
		Aircandi.getInstance().setLaunchedFromRadar(true);

		/* Initialize preferences */
		updatePreferences(true);

		/* Other UI references */
		mProgressRadar = (ViewGroup) findViewById(R.id.progress);
		mScrollView = (BounceScrollView) findViewById(R.id.scroll_view);
		
		/* Fonts */
		FontManager.getInstance().setTypefaceLight((TextView) findViewById(R.id.button_custom));
		FontManager.getInstance().setTypefaceLight((TextView) findViewById(R.id.button_tune_text));

		/* Store sounds */
		mSoundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 100);
		mNewCandiSoundId = mSoundPool.load(this, R.raw.notification_candi_discovered, 1);

		/* Set the current task so we can handle initial tab selection */
		Aircandi.getInstance().setCandiTask(CandiTask.RadarCandi);

		/* AWS Credentials */
		startGetAWSCredentials();

		mInitialized = true;
	}

	private void draw(Boolean configChange) {
		FlowLayout layout = (FlowLayout) findViewById(R.id.radar_places);
		List<Entity> entities = ProxiExplorer.getInstance().getEntityModel().getRadarPlaces();
		if (configChange) {
			layout.removeAllViews();
		}
		drawLayout(layout, entities, configChange ? false : true);

		layout = (FlowLayout) findViewById(R.id.radar_synthetics);
		entities = ProxiExplorer.getInstance().getEntityModel().getRadarSynthetics();
		if (configChange) {
			layout.removeAllViews();
		}
		drawLayout(layout, entities, false);
	}

	private void drawLayout(FlowLayout layout, List<Entity> entities, Boolean addSparkle) {

		if (entities.size() == 0) {
			layout.removeAllViews();
			return;
		}
		
		/* 
		 * Custom typeface can only be set via code. We are keeping it here
		 * for simplicity even though it would be more efficient to set it
		 * once when the activity is created.
		 */
		if (mCommon.mThemeTone.equals("dark")) {
			FontManager.getInstance().setTypefaceLight((TextView) findViewById(R.id.radar_places_header));
			FontManager.getInstance().setTypefaceLight((TextView) findViewById(R.id.radar_synthetics_header));
		}
		else {
			FontManager.getInstance().setTypefaceRegular((TextView) findViewById(R.id.radar_places_header));
			FontManager.getInstance().setTypefaceRegular((TextView) findViewById(R.id.radar_synthetics_header));
		}

		String badgeColor = null;
		String badgeColorSynthetic = null;

		TypedValue resourceName = new TypedValue();
		if (getTheme().resolveAttribute(R.attr.badgeColor, resourceName, true)) {
			badgeColor = (String) resourceName.coerceToString();
		}
		if (getTheme().resolveAttribute(R.attr.badgeColorSynthetic, resourceName, true)) {
			badgeColorSynthetic = (String) resourceName.coerceToString();
		}

		DisplayMetrics metrics = getResources().getDisplayMetrics();
		View parentView = findViewById(R.id.radar);
		Integer layoutWidthPixels = metrics.widthPixels
				- (parentView.getPaddingLeft() + parentView.getPaddingRight() + layout.getPaddingLeft() + layout.getPaddingRight());

		Integer marginDip = 1;
		Integer marginPixels = ImageUtils.getRawPixels(CandiRadar.this, marginDip);

		Integer superCandiWidthPixels = layoutWidthPixels - (marginPixels * 2);
		Integer candiHeightPixels = (int) superCandiWidthPixels / 3;
		Integer candiWidthPixels = (int) (superCandiWidthPixels - (marginPixels * 6)) / 3;

		/* We need to cap the dimensions so we don't look crazy in landscape orientation */
		int orientation = getResources().getConfiguration().orientation;
		if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
			superCandiWidthPixels = (layoutWidthPixels / 2) - (marginPixels * 2);
			candiHeightPixels = (int) (((float) superCandiWidthPixels) / 2.5f);
			candiWidthPixels = (int) (layoutWidthPixels - (marginPixels * 10)) / 5;
		}
		else {
			candiWidthPixels = (int) (superCandiWidthPixels - (marginPixels * 4)) / 3;
			superCandiWidthPixels = (layoutWidthPixels / 2) - (marginPixels * 2);
			candiHeightPixels = (int) ((((float) superCandiWidthPixels) / 1) * 0.9);
		}

		/*
		 * Removes all views that are not part of the new set of entities
		 */
		for (int i = layout.getChildCount() - 1; i >= 0; i--) {
			View view = layout.getChildAt(i);
			Entity viewEntity = (Entity) view.getTag();
			Boolean match = false;
			for (Entity entity : entities) {
				if (entity.id.equals(viewEntity.id)) {
					match = true;
					break;
				}

			}
			if (!match) {
				layout.removeViewAt(i);
			}
		}

		/*
		 * Insert views for entities that we don't already have a view for
		 */
		int entityIndex = 0;
		Boolean topIsNew = false;
		for (final Entity entity : entities) {
			View viewForEntity = null;
			Integer viewForEntityIndex = 0;
			for (int i = 0; i < layout.getChildCount(); i++) {
				View view = layout.getChildAt(i);
				Entity viewEntity = (Entity) view.getTag();
				if (viewEntity.id.equals(entity.id)) {
					viewForEntity = view;
					viewForEntityIndex = i;
					break;
				}
			}
			if (viewForEntity != null) {
				layout.removeViewAt(viewForEntityIndex);
				layout.addView(viewForEntity, entityIndex);
				((CandiView) viewForEntity).setTag(entity);
				((CandiView) viewForEntity).bindToEntity(entity);
			}
			else {
				if (entityIndex == 0 && !entity.synthetic) {
					topIsNew = true;
				}

				final CandiView candiView = new CandiView(CandiRadar.this);
				candiView.setTag(entity);
				candiView.setClickable(true);
				candiView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

				if (entity.type.equals(CandiConstants.TYPE_CANDI_PLACE) && !entity.synthetic) {
					/*
					 * Service entity
					 */
					FlowLayout.LayoutParams params = new FlowLayout.LayoutParams(superCandiWidthPixels, candiHeightPixels);
					params.setMargins(marginPixels
							, marginPixels
							, marginPixels
							, marginPixels);
					params.setCenterHorizontal(true);
					candiView.setLayoutParams(params);
					candiView.setLayoutId(R.layout.widget_candi_view_radar);
					candiView.setBadgeColorFilter(badgeColor, null, null, "#ffffff");
				}
				else {
					/*
					 * Synthetic entity
					 */
					FlowLayout.LayoutParams params = new FlowLayout.LayoutParams(candiWidthPixels, (int) (candiWidthPixels * .75));
					params.setMargins(marginPixels
							, marginPixels
							, marginPixels
							, marginPixels);
					params.setCenterHorizontal(true);
					candiView.setLayoutParams(params);
					candiView.setLayoutId(R.layout.widget_candi_view_radar_synthetic);
					candiView.setBadgeColorFilter(badgeColorSynthetic, null, null, "#ffffff");
					int colorResId = entity.place.getCategoryColorResId();
					candiView.setBackgroundResource(colorResId);
				}

				candiView.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View view) {
						onCandiClick(view);
					}
				});

				candiView.initialize();
				candiView.bindToEntity(entity);
				layout.addView(candiView);
			}
			entityIndex++;
		}

		if (layout.getId() == R.id.radar_places) {
			AnimUtils.showView(findViewById(R.id.radar_places_header));
		}
		else if (layout.getId() == R.id.radar_synthetics) {
			AnimUtils.showView(findViewById(R.id.radar_synthetics_header));
		}

		mProgressRadar.setVisibility(View.GONE);
		mCommon.hideBusy();

		/* Check for rookies and play a sound */
		if (addSparkle && topIsNew) {
			scrollToTop();
			if (mPrefSoundEffects) {
				mSoundPool.play(mNewCandiSoundId, 0.2f, 0.2f, 1, 0, 1f);
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Event handlers
	// --------------------------------------------------------------------------------------------

	public void onCustomPlaceButtonClick(View view) {
		if (Aircandi.getInstance().getUser() != null) {
			IntentBuilder intentBuilder = new IntentBuilder(this, EntityForm.class)
					.setCommandType(CommandType.New)
					.setEntityId(null)
					.setEntityType(CandiConstants.TYPE_CANDI_PLACE);

			Intent intent = intentBuilder.create();
			startActivity(intent);
			AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
		}
	}

	public void onTuneButtonClick(View view) {
		Intent intent = new Intent(this, CandiTuner.class);
		startActivity(intent);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
	}

	public void onCandiClick(View view) {
		Entity entity = (Entity) view.getTag();
		showCandiForm(entity);
	}

	// --------------------------------------------------------------------------------------------
	// Entity routines
	// --------------------------------------------------------------------------------------------

	public void doRefresh() {
		/*
		 * Only called as the result of a user initiated refresh.
		 */
		if (Aircandi.getInstance().isRadarScanInProgress()) {
			Logger.v(this, "User refresh request ignored because of active scan");
			return;
		}

		Logger.d(this, "Starting refresh");
		Tracker.trackEvent("Radar", "Refresh", "Full", 0);
		searchForPlaces();
	}

	private void searchForPlaces() {
		/*
		 * We won't perform a search if location access is disabled
		 */
		if (!GeoLocationManager.getInstance().isLocationAccessEnabled()) {
			startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
			AnimUtils.doOverridePendingTransition(CandiRadar.this, TransitionType.CandiPageToForm);
		}
		else {
			mCommon.showBusy();
			/*
			 * Start a location update. It may or may not be finished by the
			 * time we use the location information. We want to force this to
			 * be as fresh as possible so we don't accept anything even slightly
			 * stale.
			 */
			Criteria criteria = new Criteria();
			criteria.setAccuracy(Criteria.ACCURACY_COARSE);
			GeoLocationManager.getInstance().ensureLocation(GeoLocationManager.MINIMUM_ACCURACY
					, 0
					, criteria, null);

			new SearchProcess().start();
		}
	}

	private class SearchProcess {

		public SearchProcess() {}

		public void start() {

			new AsyncTask() {

				@Override
				protected void onPreExecute() {
					/*
					 * This are one shot event receivers. As we capture
					 * events we unregister for them with the event bus.
					 */
					synchronized (Events.EventBus.wifiScanReceived) {
						Events.EventBus.wifiScanReceived.add(new EventHandler() {

							@Override
							public void onEvent(Object data) {

								/* Stop listening for this event */
								synchronized (Events.EventBus.wifiScanReceived) {
									Events.EventBus.wifiScanReceived.remove(this);
								}

								Logger.v(CandiRadar.this, "Wifi scan received");
								new AsyncTask() {

									@Override
									protected Object doInBackground(Object... params) {
										if (!Aircandi.applicationUpdateRequired) {
											ProxiExplorer.getInstance().processBeaconsFromScan();
										}
										return null;
									}

								}.execute();
							}
						});
					}

					synchronized (Events.EventBus.entitiesLoaded) {
						Events.EventBus.entitiesLoaded.add(new EventHandler() {

							@Override
							public void onEvent(Object data) {

								/* Stop listening for this event */
								synchronized (Events.EventBus.entitiesLoaded) {
									Events.EventBus.entitiesLoaded.remove(this);
								}

								Logger.d(CandiRadar.this, "Entities loaded from service");
								ServiceResponse serviceResponse = (ServiceResponse) data;

								if (serviceResponse.responseCode == ResponseCode.Success) {

									runOnUiThread(new Runnable() {

										@Override
										public void run() {
											FlowLayout layout = (FlowLayout) findViewById(R.id.radar_places);
											List<Entity> entities = ProxiExplorer.getInstance().getEntityModel().getRadarPlaces();
											drawLayout(layout, entities, true);
											new AsyncTask() {

												@Override
												protected Object doInBackground(Object... params) {

													Observation observation = GeoLocationManager.getInstance().getObservation();
													if (observation != null) {
														ProxiExplorer.getInstance().getPlacesNearLocation(observation);
													}
													return null;
												}

											}.execute();
										}
									});

								}
								else {
									/* We could have failed because an update is required */
									if (serviceResponse.responseDetail == ResponseDetail.UpdateRequired) {
										showUpdateAlert();
									}
									else {
										mCommon.handleServiceError(serviceResponse, ServiceOperation.BeaconScan, CandiRadar.this);
									}
								}
							}
						});
					}

					synchronized (Events.EventBus.syntheticsLoaded) {
						Events.EventBus.syntheticsLoaded.add(new EventHandler() {

							@Override
							public void onEvent(Object data) {

								/* Stop listening for this event */
								synchronized (Events.EventBus.syntheticsLoaded) {
									Events.EventBus.syntheticsLoaded.remove(this);
								}

								Logger.d(CandiRadar.this, "Synthetics loaded from service");
								ServiceResponse serviceResponse = (ServiceResponse) data;

								if (serviceResponse.responseCode == ResponseCode.Success) {

									runOnUiThread(new Runnable() {

										@Override
										public void run() {
											FlowLayout layout = (FlowLayout) findViewById(R.id.radar_synthetics);
											List<Entity> entities = ProxiExplorer.getInstance().getEntityModel().getRadarSynthetics();
											drawLayout(layout, entities, false);
											searchComplete();
										}
									});
									Aircandi.stopwatch.segmentTime("Finished updating radar UI");
									Logger.d(CandiRadar.this, "Full entity update complete");
									Aircandi.fullUpdateComplete = true;
								}
								else {
									mCommon.handleServiceError(serviceResponse, ServiceOperation.BeaconScan, CandiRadar.this);
								}

								Aircandi.getInstance().setRadarScanInProgress(false);
							}
						});
					}
				}

				@Override
				protected Object doInBackground(Object... params) {

					if (!Aircandi.applicationUpdateRequired) {
						if (NetworkManager.getInstance().isWifiEnabled()) {
							ProxiExplorer.getInstance().scanForWifi(null);
						}
						else {
							ProxiExplorer.getInstance().processBeaconsFromScan();
						}
					}
					return null;
				}

			}.execute();
		}

	}

	public void searchComplete() {
		/*
		 * These are finishing steps that make sense if we rebuilt the entity model
		 * or it is just a UI update to sync to the entity model.
		 */

		/* Show something to the user that there aren't any candi nearby. */
		mCommon.hideBusy();
		mProgressRadar.setVisibility(View.GONE);

		/* Show aircandi tips if this is the first time the application has been run */
		if (Aircandi.firstRunApp) {
			Aircandi.settingsEditor.putBoolean(Preferences.SETTING_FIRST_RUN, false);
			Aircandi.settingsEditor.commit();
			Aircandi.firstRunApp = false;
		}

		/* Capture timestamps so we can detect state changes in the entity model */
		mEntityModelRefreshDate = ProxiExplorer.getInstance().getEntityModel().getLastRefreshDate();
		mEntityModelActivityDate = ProxiExplorer.getInstance().getEntityModel().getLastActivityDate();
	}

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	private void showCandiForm(Entity entity) {

		IntentBuilder intentBuilder = new IntentBuilder(this, CandiForm.class)
				.setCommandType(CommandType.View)
				.setEntityId(entity.id)
				.setParentEntityId(entity.parentId)
				.setEntityType(entity.type)
				.setEntityTree(ProxiExplorer.EntityTree.Radar)
				.setEntityLocation(entity.getLocation());

		if (entity.parentId != null) {
			intentBuilder.setCollectionId(entity.getParent().id);
		}

		Intent intent = intentBuilder.create();

		startActivity(intent);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiRadarToCandiForm);
	}

	private void scrollToTop() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				mScrollView.fullScroll(ScrollView.FOCUS_UP);
			}
		});
	}

	// --------------------------------------------------------------------------------------------
	// System callbacks
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onStart() {
		/*
		 * Called everytime the activity is started or restarted.
		 */
		super.onStart();
		if (!mInitialized) return;

		Logger.d(this, "CandiRadarActivity starting");
		if (CandiConstants.DEBUG_TRACE) {
			Debug.startMethodTracing("aircandi", 100000000);
		}

		/* Make sure the right tab is active */
		mCommon.setActiveTab(0);
	}

	@Override
	protected void onStop() {
		/*
		 * Fired when starting another activity and we lose our window.
		 */
		Logger.d(this, "CandiRadarActivity stopped");
		if (CandiConstants.DEBUG_TRACE) {
			Debug.stopMethodTracing();
		}
		super.onStop();

		/* Start thread to check and manage the file cache. */
		ImageManager.getInstance().cleanCacheAsync(getApplicationContext());
	}

	@Override
	protected void onPause() {
		/*
		 * - Fires when we lose focus and have been moved into the background. This will
		 * be followed by onStop if we are not visible. Does not fire if the activity window
		 * loses focus but the activity is still active.
		 */
		Logger.d(this, "CandiRadarActivity paused");

		mCommon.stopScanService();
		super.onPause();
		try {
			NetworkManager.getInstance().onPause();
			GeoLocationManager.getInstance().onPause();
		}
		catch (Exception exception) {
			Exceptions.Handle(exception);
		}
	}

	@Override
	protected void onResume() {
		/*
		 * Lifecycle ordering
		 * (onCreate/onRestart)->onStart->onResume->onAttachedToWindow->onWindowFocusChanged
		 */

		/*
		 * OnResume gets called after OnCreate (always) and whenever the activity is being brought back to the
		 * foreground. Not guaranteed but is usually called just before the activity receives focus. Because code in
		 * OnCreate could have determined that we aren't ready to roll, isReadyToRun is used to indicate that prep work
		 * is complete. This is also called when the user jumps out and back from setting preferences so we need to
		 * refresh the places where they get used. Game engine is started/restarted in BaseGameActivity class if we
		 * currently have the window focus.
		 */
		super.onResume();
		if (!mInitialized) return;

		NetworkManager.getInstance().onResume();
		GeoLocationManager.getInstance().onResume();
		mCommon.startScanService(CandiConstants.INTERVAL_SCAN_RADAR);
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);

		if (!mInitialized) return;
		if (hasFocus) {
			EntityModel entityModel = ProxiExplorer.getInstance().getEntityModel();
			if (mEntityModelRefreshDate == null) {
				Logger.d(this, "Start first beacon scan");
				searchForPlaces();
			}
			else if (mPrefChangeRefreshNeeded) {
				Logger.d(this, "Start beacon scan because of preference change");
				mPrefChangeRefreshNeeded = false;
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
				Logger.d(this, "Update radar because of detected entity model change");
				mHandler.postDelayed(new Runnable() {

					@Override
					public void run() {
						mCommon.hideBusy();
						invalidateOptionsMenu();
						mCommon.showBusy();
						draw(false);
						searchComplete();
					}
				}, 100);
			}
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
			ImageManager.getInstance().getImageLoader().stopLoaderThread();
		}
		catch (Exception exception) {
			Exceptions.Handle(exception);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		draw(true);
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

	private void startGetAWSCredentials() {
		Thread t = new Thread() {

			@Override
			public void run() {
				try {
					Properties properties = new Properties();
					InputStream inputStream = getClass().getResourceAsStream("aws.properties");
					properties.load(inputStream);

					String accessKeyId = properties.getProperty("accessKey");
					String secretKey = properties.getProperty("secretKey");

					if ((accessKeyId == null) || (accessKeyId.equals(""))
							|| (accessKeyId.equals("CHANGEME"))
							|| (secretKey == null)
							|| (secretKey.equals(""))
							|| (secretKey.equals("CHANGEME"))) {
						Logger.e(CandiRadar.this, "Aws Credentials not configured correctly.");
					}
					else {
						mAwsCredentials = new BasicAWSCredentials(properties.getProperty("accessKey"), properties.getProperty("secretKey"));
					}
				}
				catch (Exception exception) {
					Logger.e(CandiRadar.this, exception.getMessage(), exception);
				}
			}
		};
		t.start();
	}

	@SuppressWarnings("unused")
	private String getGoogleAnalyticsId() {
		Properties properties = new Properties();

		try {
			properties.load(getClass().getResourceAsStream("google_analytics.properties"));
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