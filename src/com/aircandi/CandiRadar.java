package com.aircandi;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.NetworkInfo.State;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi.CandiTask;
import com.aircandi.Preferences.PrefResponse;
import com.aircandi.components.AircandiCommon;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.AnimUtils;
import com.aircandi.components.AnimUtils.TransitionType;
import com.aircandi.components.CommandType;
import com.aircandi.components.DateUtils;
import com.aircandi.components.EntityList;
import com.aircandi.components.Events;
import com.aircandi.components.Events.EventHandler;
import com.aircandi.components.Exceptions;
import com.aircandi.components.GeoLocationManager;
import com.aircandi.components.ImageCache;
import com.aircandi.components.ImageManager;
import com.aircandi.components.ImageUtils;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NetworkManager.IConnectivityListener;
import com.aircandi.components.NetworkManager.IWifiReadyListener;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.ProxiExplorer.ModelResult;
import com.aircandi.components.ProxiExplorer.ScanOptions;
import com.aircandi.components.ProxiExplorer.WifiScanResult;
import com.aircandi.components.Tracker;
import com.aircandi.core.CandiConstants;
import com.aircandi.service.ProxiConstants;
import com.aircandi.service.objects.Beacon;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.GeoLocation;
import com.aircandi.service.objects.User;
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

	private Handler						mHandler				= new Handler();
	public static BasicAWSCredentials	mAwsCredentials			= null;

	/* We use these to track whether a preference gets changed */
	public Boolean						mPrefAutoscan			= false;
	public String						mPrefAutoscanInterval	= "5000";
	public boolean						mPrefDemoMode			= false;
	public boolean						mPrefGlobalBeacons		= true;
	public boolean						mPrefEntityFencing		= true;
	public boolean						mPrefShowDebug			= false;
	public boolean						mPrefSoundEffects		= true;
	public String						mPrefTestingBeacons		= "natural";

	private Number						mEntityModelRefreshDate;
	private Number						mEntityModelActivityDate;
	@SuppressWarnings("unused")
	private User						mEntityModelUser;

	private ViewGroup					mWifiDialog;
	private ViewGroup					mEmptyDialog;
	private ViewGroup					mRadarView;
	private ViewGroup					mProgressRadar;

	private SoundPool					mSoundPool;
	private Runnable					mScanRunnable;
	private BeaconScanWatcher			mScanWatcher;
	public EventHandler					mEventScanReceived;
	private AlertDialog					mUpdateAlertDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (Aircandi.firstStartApp) {
			initializeApp();
		}
		initialize();
	}

	private void initializeApp() {
		/*
		 * Here we initialize application level state that will continue
		 * even if this activity is destroyed.
		 */
		if (Build.PRODUCT.contains("sdk")) {
			Aircandi.usingEmulator = true;
		}

		/* Connectivity monitoring */
		NetworkManager.getInstance().setContext(getApplicationContext());
		NetworkManager.getInstance().initialize();

		/* Proxibase sdk components */
		ProxiExplorer.getInstance().setContext(getApplicationContext());
		ProxiExplorer.getInstance().setUsingEmulator(Aircandi.usingEmulator);
		ProxiExplorer.getInstance().initialize();

		/* Image cache */
		ImageManager.getInstance().setImageCache(new ImageCache(getApplicationContext(), CandiConstants.CACHE_PATH, 100, 16));
		ImageManager.getInstance().setFileCacheOnly(true);
		ImageManager.getInstance().getImageLoader().setWebView((WebView) findViewById(R.id.webview));
		ImageManager.getInstance().setActivity(this);

		/*
		 * Get setup for location snapshots. Initialize will populate location
		 * with the best of any cached location fixes. A single update will
		 * be launched if the best cached location fix doesn't meet our freshness
		 * and accuracy requirements.
		 */
		GeoLocationManager.getInstance().setContext(getApplicationContext());
		GeoLocationManager.getInstance().initialize();

		/* Auto signin the user */
		mCommon.signinAuto();
		Aircandi.firstStartApp = false;
	}

	private void initialize() {
		/*
		 * Here we initialize activity level state.
		 */
		if (CandiConstants.DEBUG_TRACE) {
			Debug.startMethodTracing("aircandi", 100000000);
		}

		/* Used by other activities to determine if they were auto launched after a crash */
		Aircandi.getInstance().setLaunchedFromRadar(true);

		/* Other UI references */
		mWifiDialog = (ViewGroup) findViewById(R.id.retry_dialog);
		mEmptyDialog = (ViewGroup) findViewById(R.id.empty_dialog);
		mRadarView = (ViewGroup) findViewById(R.id.radar);
		mProgressRadar = (ViewGroup) findViewById(R.id.progress);

		/* Restore empty message since this could be a restart because of a theme change */
		if (Aircandi.lastScanEmpty) {
			String helpHtml = getString(Aircandi.wifiCount > 0 ? R.string.help_radar_empty : R.string.help_radar_empty_no_beacons);
			((TextView) findViewById(R.id.text_empty_message)).setText(Html.fromHtml(helpHtml));
			mEmptyDialog.setVisibility(View.VISIBLE);
		}

		/* Store sounds */
		mSoundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 100);

		/* Initialize preferences */
		updatePreferences(false);

		/* Setup scan runnable */
		mScanRunnable = new Runnable() {

			@Override
			public void run() {
				scanForBeacons(new ScanOptions(false, false, null));
			}
		};

		/* Set the current task so we can handle initial tab selection */
		Aircandi.getInstance().setCandiTask(CandiTask.RadarCandi);

		/* AWS Credentials */
		startGetCredentials();

		/* Beacon indicator */
		mEventScanReceived = new EventHandler() {

			@Override
			public void onEvent(Object data) {
				List<WifiScanResult> scanList = (List<WifiScanResult>) data;
				updateRadarHelp(scanList);
			}
		};
	}

	private void startGetCredentials() {
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

	// --------------------------------------------------------------------------------------------
	// Event handlers
	// --------------------------------------------------------------------------------------------

	public void onBackPressed() {
		super.onBackPressed();
	}

	public void onNewCandiButtonClick(View view) {
		if (Aircandi.getInstance().getUser() != null) {
			mCommon.showTemplatePicker(true);
		}
	}

	public void onTuneButtonClick(View view) {
		Beacon beacon = ProxiExplorer.getInstance().getStrongestWifiAsBeacon();
		if (beacon == null && mCommon.mParentId == null) {
			AircandiCommon.showAlertDialog(R.drawable.ic_app
					, "Aircandi beacons"
					, getString(R.string.alert_tuning_radar_beacons_zero)
					, null
					, CandiRadar.this, android.R.string.ok
					, null
					, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {}
					}
					, null);
			return;
		}

		Intent intent = new Intent(this, CandiTuner.class);
		startActivity(intent);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
	}

	public void onHelpButtonClick(View view) {
		mCommon.showHelp(R.string.help_radar);
	}

	public void onCandiClick(View view) {
		Entity entity = (Entity) view.getTag();
		showCandiForm(entity);
	}

	// --------------------------------------------------------------------------------------------
	// Entity routines
	// --------------------------------------------------------------------------------------------

	private void scanForBeacons(final ScanOptions scanOptions) {
		/*
		 * Everything associated with this call is on the main thread but the UI is still responsive because most of the
		 * UI is being handled by the 2d engine thread.
		 */
		if (!Aircandi.getInstance().isRadarScanInProgress() && !Aircandi.getInstance().isRadarUpdateInProgress()) {

			/* Make sure there aren't any extra runnables waiting to run */
			mHandler.removeCallbacks(mScanRunnable);

			Aircandi.getInstance().setRadarScanInProgress(true);

			/* Check that wifi is enabled and we have a network connection */
			verifyWifi(new IWifiReadyListener() {

				@Override
				public void onWifiReady() {

					/* Time to turn on the progress indicators */
					if (scanOptions.showProgress) {
						mCommon.showProgressDialog(getString(scanOptions.progressMessageResId), true, CandiRadar.this);
						mProgressRadar.setVisibility(View.VISIBLE);
					}
					else {
						mCommon.hideProgressDialog();
						mProgressRadar.setVisibility(View.GONE);
					}

					Logger.i(this, "Starting beacon scan, fullBuild = " + String.valueOf(scanOptions.fullBuild));
					mScanWatcher = new BeaconScanWatcher();
					mScanWatcher.start(scanOptions);
				}

				@Override
				public void onWifiFailed() {
					Aircandi.getInstance().setRadarScanInProgress(false);
				}
			});
		}
	}

	private class BeaconScanWatcher {

		private ScanOptions	mScanOptions;

		public BeaconScanWatcher() {}

		public void start(final ScanOptions scanOptions) {

			mScanOptions = scanOptions;

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
											if (mScanOptions.fullBuild) {
												ProxiExplorer.getInstance().getEntityModel().getBeacons().clear();
											}
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

									Aircandi.getInstance().setRadarUpdateInProgress(true);

									EntityList<Entity> entities = ProxiExplorer.getInstance().getEntityModel().getRadarEntities();
									buildRadar(entities);

									updateComplete();
									Aircandi.stopwatch.segmentTime("Finished updating radar UI");

									if (mScanOptions.fullBuild) {
										Logger.d(CandiRadar.this, "Full entity update complete");
										Aircandi.fullUpdateComplete = true;
									}
								}
								else {
									mCommon.handleServiceError(serviceResponse, ServiceOperation.BeaconScan, CandiRadar.this);
								}

								Aircandi.getInstance().setRadarUpdateInProgress(false);
								Aircandi.getInstance().setRadarScanInProgress(false);
							}
						});
					}
				}

				@Override
				protected Object doInBackground(Object... params) {

					if (!Aircandi.applicationUpdateRequired) {
						ProxiExplorer.getInstance().scanForWifi(null);
					}
					return null;
				}

			}.execute();
		}

	}

	private void updateRadarOnly() {
		/*
		 * Used to ensure the UI reflects the current state of the entity model.
		 */
		Aircandi.getInstance().setRadarUpdateInProgress(true);

		/* We pass a copy to presenter to provide more stability and steady state. */
		@SuppressWarnings("unused")
		EntityList<Entity> entities = ProxiExplorer.getInstance().getEntityModel().getRadarEntities();
		//mCandiPatchPresenter.updateCandiData(entitiesCopy, false, false);

		/* Handles other wrapup tasks */
		updateComplete();

		Aircandi.getInstance().setRadarUpdateInProgress(false);
	}

	public void updateComplete() {
		/*
		 * These are finishing steps that make sense if we rebuilt the entity model
		 * or it is just a UI update to sync to the entity model.
		 */

		/* Show something to the user that there aren't any candi nearby. */
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				mCommon.hideProgressDialog();
				mProgressRadar.setVisibility(View.GONE);
				mWifiDialog.setVisibility(View.GONE);
				EntityList<Entity> radarEntities = ProxiExplorer.getInstance().getEntityModel().getRadarEntities();
				((View) findViewById(R.id.button_tune)).setVisibility(Aircandi.wifiCount > 0 ? View.VISIBLE : View.GONE);
				if (radarEntities.size() > 0 || mWifiDialog.getVisibility() == View.VISIBLE) {
					Aircandi.lastScanEmpty = false;
					mEmptyDialog.setVisibility(View.GONE);
				}
				else {
					Aircandi.lastScanEmpty = true;
					String helpHtml = getString(Aircandi.wifiCount > 0 ? R.string.help_radar_empty : R.string.help_radar_empty_no_beacons);
					((TextView) findViewById(R.id.text_empty_message)).setText(Html.fromHtml(helpHtml));
					mEmptyDialog.setVisibility(View.VISIBLE);
				}
			}
		});

		/* Make sure we are at a proper scroll location */
		//		mCandiPatchPresenter.ensureScrollBoundaries();

		/* Show aircandi tips if this is the first time the application has been run */
		if (Aircandi.firstRunApp) {
			onHelpButtonClick(null);
			Aircandi.settingsEditor.putBoolean(Preferences.SETTING_FIRST_RUN, false);
			Aircandi.settingsEditor.commit();
			Aircandi.firstRunApp = false;
		}

		/* Check for rookies and play a sound */
		if (mPrefSoundEffects && ProxiExplorer.getInstance().getEntityModel().getRookieHit()) {
			//mCandiPatchPresenter.scrollToTop();
			ProxiExplorer.getInstance().getEntityModel().setRookieHit(false);
			mSoundPool.play(R.raw.notification_candi_discovered, 0.5f, 0.5f, 1, 0, 1f);
		}

		/* Capture timestamps so we can detect state changes in the entity model */
		mEntityModelRefreshDate = ProxiExplorer.getInstance().getEntityModel().getLastRefreshDate();
		mEntityModelActivityDate = ProxiExplorer.getInstance().getEntityModel().getLastActivityDate();
		mEntityModelUser = Aircandi.getInstance().getUser();

		/*
		 * Schedule the next wifi scan run if autoscan is enabled | The autoscan will pick
		 * up new beacons and changes in visibility of the entities associated with beacons
		 * that are already being tracked. This is meant to be an efficient refresh that can
		 * run continuously without a ton of data traffic. So there won't be any calls to
		 * the data service unless we discover a new beacon.
		 */
		if (mPrefAutoscan) {
			Logger.d(CandiRadar.this, "Scheduling an autoscan in: " + mPrefAutoscanInterval + " ms");
			mHandler.removeCallbacks(mScanRunnable); // Make sure something isn't already scheduled
			mHandler.postDelayed(mScanRunnable, Integer.parseInt(mPrefAutoscanInterval));
		}
	}

	public void updateRadarHelp(final List<WifiScanResult> scanList) {

		/*
		 * If we are showing help then adjust it depending
		 * on whether there are beacons nearby.
		 */
		if (Aircandi.lastScanEmpty && mWifiDialog.getVisibility() != View.VISIBLE) {
			synchronized (scanList) {
				/*
				 * In case we get called from a background thread.
				 */
				runOnUiThread(new Runnable() {

					@Override
					public void run() {

						int wifiCount = 0;
						for (WifiScanResult wifi : scanList) {
							if (wifi.global) {
								continue;
							}
							else {
								wifiCount++;
							}
						}
						String helpHtml = getString(wifiCount > 0 ? R.string.help_radar_empty : R.string.help_radar_empty_no_beacons);
						((TextView) findViewById(R.id.text_empty_message)).setText(Html.fromHtml(helpHtml));
						mEmptyDialog.setVisibility(View.VISIBLE);
						((View) findViewById(R.id.button_tune)).setVisibility(wifiCount > 0 ? View.VISIBLE : View.GONE);
						mEmptyDialog.invalidate();
					}
				});
			}
		}
	}

	public void doRefresh(RefreshType refreshType) {
		/*
		 * Only called as the result of a user initiated refresh.
		 */
		if (Aircandi.getInstance().isRadarScanInProgress() || Aircandi.getInstance().isRadarUpdateInProgress()) {
			Logger.v(this, "User refresh request ignored because of active scan");
			return;
		}

		NetworkManager.getInstance().reset();

		if (!Aircandi.fullUpdateComplete) {
			doResume();
		}
		else {
			if (refreshType == RefreshType.FullBuild) {
				Logger.d(this, "Starting full build beacon scan");
				Tracker.trackEvent("Radar", "Refresh", "FullBuild", 0);
				scanForBeacons(new ScanOptions(true, true, R.string.progress_scanning));
			}
			else if (refreshType == RefreshType.Standard || refreshType == RefreshType.Autoscan) {

				Logger.d(this, "User action starting standard refresh");
				Tracker.trackEvent("Radar", "Refresh", "Standard", 0);
				scanForBeacons(new ScanOptions(false, true, R.string.progress_scanning));
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	private void showCandiForm(Entity entity) {

		IntentBuilder intentBuilder = new IntentBuilder(this, CandiForm.class);
		intentBuilder.setCommandType(CommandType.View);
		intentBuilder.setEntityId(entity.id);
		intentBuilder.setParentEntityId(entity.parentId);
		intentBuilder.setEntityType(entity.type);
		intentBuilder.setEntityTree(ProxiExplorer.EntityTree.Radar);

		if (entity.parentId == null) {
			intentBuilder.setCollectionId(ProxiConstants.ROOT_COLLECTION_ID);
		}
		else {
			intentBuilder.setCollectionId(entity.getParent().id);
			intentBuilder.setEntityLocation(new GeoLocation(entity.getParent().place.location.lat, entity.getParent().place.location.lng));
		}

		Intent intent = intentBuilder.create();

		startActivity(intent);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiRadarToCandiForm);
	}

	private void buildRadar(final List<Entity> entities) {

		final FlowLayout layout = (FlowLayout) findViewById(R.id.radar);

		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				layout.removeAllViews();
			}
		});

		new AsyncTask() {

			@Override
			protected void onPreExecute() {}

			@Override
			protected Object doInBackground(Object... parameters) {

				for (final Entity entity : entities) {
					if (!entity.hidden) {

						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								final CandiView candiView = new CandiView(CandiRadar.this);
								FlowLayout.LayoutParams params = new FlowLayout.LayoutParams();
								if (entity.type.equals(CandiConstants.TYPE_CANDI_PLACE) && !entity.synthetic) {
									params.width = ImageUtils.getPixels(CandiRadar.this, 308);
									params.height = ImageUtils.getPixels(CandiRadar.this, 100);
									params.setMargins(0, 0, ImageUtils.getPixels(CandiRadar.this, 4), ImageUtils.getPixels(CandiRadar.this, 4));
									params.setCenterHorizontal(true);
									candiView.setLayoutParams(params);
									candiView.setLayoutId(R.layout.widget_candi_view_radar);
								}
								else {
									params.width = ImageUtils.getPixels(CandiRadar.this, 100);
									params.height = ImageUtils.getPixels(CandiRadar.this, 100);
									params.setMargins(0, 0, ImageUtils.getPixels(CandiRadar.this, 4), ImageUtils.getPixels(CandiRadar.this, 4));
									params.setCenterHorizontal(true);
									candiView.setLayoutParams(params);
									candiView.setLayoutId(R.layout.widget_candi_view_radar_synthetic);
									//candiView.setColorFilter("#ffc4c3bc", "#ffdddddd");
								}
								candiView.setTag(entity);
								candiView.setClickable(true);
								candiView.setBackgroundResource(R.drawable.button_nooutline_selector);
								int padding = ImageUtils.getPixels(CandiRadar.this, 3);
								candiView.setPadding(padding, padding, padding, padding);
								candiView.setOnClickListener(new OnClickListener() {

									@Override
									public void onClick(View view) {
										onCandiClick(view);
									}
								});
								candiView.initialize();
								candiView.getCandiImage().setBackgroundColor(ImageUtils.hexToColor("#ffc4c3bc"));
								candiView.bindToEntity(entity);
								layout.addView(candiView);

							}
						});
					}
				}

				return null;
			}

			@Override
			protected void onPostExecute(Object modelResult) {}
		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// Connectivity routines
	// --------------------------------------------------------------------------------------------

	private void verifyWifi(final IWifiReadyListener listener) {

		if (NetworkManager.getInstance().isTethered()) {
			mEmptyDialog.setVisibility(View.GONE);
			mCommon.hideProgressDialog();
			mProgressRadar.setVisibility(View.GONE);
			showNetworkDialog(true, getString(R.string.dialog_network_message_wifi_tethered), true);
			if (listener != null) {
				Logger.i(this, "Wifi failed: tethered");
				listener.onWifiFailed();
			}

		}
		else if (!NetworkManager.getInstance().isWifiEnabled() && !ProxiExplorer.getInstance().isUsingEmulator()) {

			/* Make sure we are displaying any background message */
			mEmptyDialog.setVisibility(View.GONE);
			mCommon.hideProgressDialog();
			mProgressRadar.setVisibility(View.GONE);

			showNetworkDialog(true, getString(R.string.dialog_network_message_wifi_notready), false);
			final Button retryButton = (Button) findViewById(R.id.button_retry);
			final Button cancelButton = (Button) findViewById(R.id.button_cancel);
			final TextView txtMessage = (TextView) findViewById(R.id.retry_message);
			retryButton.setEnabled(false);
			cancelButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					showNetworkDialog(false, "", false);
					listener.onWifiFailed();
					return;
				}
			});

			NetworkManager.getInstance().setConnectivityListener(new IConnectivityListener() {

				@Override
				public void onConnectivityStateChanged(State networkInfoState) {}

				@Override
				public void onWifiStateChanged(int wifiState) {

					if (wifiState == WifiManager.WIFI_STATE_ENABLED)
					{
						ImageUtils.showToastNotification("Wifi state enabled.", Toast.LENGTH_SHORT);
						if (mWifiDialog.getVisibility() == View.VISIBLE) {
							((CheckBox) findViewById(R.id.wifi_enabled_checkbox)).setChecked(true);
							txtMessage.setText(getString(R.string.dialog_network_message_wifi_ready));
							retryButton.setEnabled(true);
							retryButton.setOnClickListener(new OnClickListener() {

								@Override
								public void onClick(View v) {
									/* Re-enter so we get to the next stage */
									NetworkManager.getInstance().setConnectivityListener(null);
									showNetworkDialog(false, "", false);
									if (listener != null) {
										Logger.i(this, "Wifi verified");
										listener.onWifiReady();
										NetworkManager.getInstance().setConnectivityListener(null);
									}
								}
							});

						}
					}
					else if (wifiState == WifiManager.WIFI_STATE_ENABLING) {
						ImageUtils.showToastNotification(getString(R.string.toast_wifi_enabling), Toast.LENGTH_SHORT);
					}
					else if (wifiState == WifiManager.WIFI_STATE_DISABLING) {
						ImageUtils.showToastNotification(getString(R.string.toast_wifi_disabling), Toast.LENGTH_SHORT);
					}
					else if (wifiState == WifiManager.WIFI_STATE_DISABLED) {
						((CheckBox) findViewById(R.id.wifi_enabled_checkbox)).setChecked(false);
						txtMessage.setText(getString(R.string.dialog_network_message_wifi_notready));
						retryButton.setEnabled(false);
						ImageUtils.showToastNotification(getString(R.string.toast_wifi_disabled), Toast.LENGTH_SHORT);
					}
				}
			});

		}
		else {
			showNetworkDialog(false, "", false);
			if (listener != null) {
				Logger.i(this, "Wifi verified");
				listener.onWifiReady();
			}
		}
	}

	private void showNetworkDialog(boolean visible, String message, Boolean hideControls) {

		if (visible) {
			TextView txtMessage = (TextView) findViewById(R.id.retry_message);
			CheckBox enableWifiCheckBox = (CheckBox) findViewById(R.id.wifi_enabled_checkbox);
			ViewGroup controls = (ViewGroup) findViewById(R.id.group_buttons);

			mRadarView.setVisibility(View.GONE);
			txtMessage.setText(message);

			if (hideControls) {
				controls.setVisibility(View.GONE);
				enableWifiCheckBox.setVisibility(View.GONE);
				mWifiDialog.setVisibility(View.VISIBLE);
			}
			else {
				controls.setVisibility(View.VISIBLE);
				enableWifiCheckBox.setVisibility(View.VISIBLE);

				boolean isWifiEnabled = NetworkManager.getInstance().isWifiEnabled();
				enableWifiCheckBox.setChecked(isWifiEnabled);

				enableWifiCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						if (isChecked) {
							Logger.i(CandiRadar.this, "Enable wifi from activity");
							NetworkManager.getInstance().enableWifi(true);
						}
						else {
							Logger.i(CandiRadar.this, "Disable wifi from activity");
							NetworkManager.getInstance().enableWifi(false);
						}
					}
				});
				mWifiDialog.setVisibility(View.VISIBLE);
			}

		}
		else {
			mRadarView.setVisibility(View.VISIBLE);
			mWifiDialog.setVisibility(View.GONE);
		}
	}

	@Override
	protected int getLayoutId() {
		return R.layout.candi_radar;
	}

	// --------------------------------------------------------------------------------------------
	// System callbacks
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onStart() {
		/*
		 * Called everytime the activity is started or restarted.
		 */
		Logger.d(this, "CandiRadarActivity starting");
		if (CandiConstants.DEBUG_TRACE) {
			Debug.stopMethodTracing();
		}
		mCommon.doStart();
		super.onStart();

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
		mCommon.doStop();
		super.onStop(); /* Goes to Sherlock */

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

		/* Make sure autoscan is stopped if we are not in the foreground */
		mHandler.removeCallbacks(mScanRunnable);

		synchronized (Events.EventBus.wifiScanReceived) {
			Events.EventBus.wifiScanReceived.remove(mEventScanReceived);
		}
		mCommon.stopScanService();
		super.onPause();
		try {
			NetworkManager.getInstance().onPause();
			GeoLocationManager.getInstance().onPause();
			mCommon.doPause();
		}
		catch (Exception exception) {
			Exceptions.Handle(exception);
		}
	}

	@Override
	protected void onResume() {
		/*
		 * Lifecycle ordering
		 * onCreate->onStart->onResume
		 * ->onAttachedToWindow->onWindowFocusChanged
		 * ->onLoadEngine->onLoadScene->onResumeGame
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
		doResume();
	}

	public void doResume() {

		/* Quick check for a new version. */
		final Boolean doUpdateCheck = updateCheckNeeded();

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				if (doUpdateCheck) {
					mCommon.showProgressDialog(getString(R.string.progress_scanning), true, CandiRadar.this);
					mProgressRadar.setVisibility(View.VISIBLE);
				}
			}

			@Override
			protected Object doInBackground(Object... params) {
				ModelResult result = new ModelResult();
				if (doUpdateCheck) {
					result = checkForUpdate();
				}
				return result.serviceResponse;
			}

			@Override
			protected void onPostExecute(Object result) {
				/*
				 * We get here before the user has made a selection but
				 * we do know if an update is needed and required.
				 */
				ServiceResponse serviceResponse = (ServiceResponse) result;
				if (serviceResponse.responseCode == ResponseCode.Failed) {
					mCommon.handleServiceError(serviceResponse, ServiceOperation.CheckUpdate);
				}
				else {
					if (Aircandi.applicationUpdateRequired) {
						mCommon.hideProgressDialog();
						mProgressRadar.setVisibility(View.GONE);
						showUpdateAlertDialog();
						return;
					}
					/*
					 * Logic that should only run if the activity is resuming after having been paused.
					 * This used to be in restart but it wasn't getting called reliably when returning from another
					 * activity.
					 */
					if (!Aircandi.fullUpdateComplete) {
						Logger.d(this, "CandiRadarActivity resuming for first time or still needs first full update");
						finishResume(true);
						scanForBeacons(new ScanOptions(true, true, R.string.progress_scanning));
					}
					else {
						Logger.d(this, "CandiRadarActivity resuming after pause");
						/*
						 * We could be resuming because of a preference change.
						 * Restart: theme change
						 * Refresh: display extra, demo mode, global beacons
						 */
						if (!updatePreferences(true)) {
							/*
							 * We have to be pretty aggressive about refreshing the UI because
							 * there are lots of actions that could have happened while this activity
							 * was stopped that change what the user would expect to see.
							 * 
							 * - Entity deleted or modified
							 * - Entity children modified
							 * - New comments
							 * - Change in user which effects which candi and UI should be visible.
							 * - User profile could have been updated and we don't catch that.
							 */
							//							if (Aircandi.getInstance().getUser() == null || mEntityModelUser == null
							//									|| !Aircandi.getInstance().getUser().id.equals(mEntityModelUser.id)) {
							//								Logger.d(this, "CandiRadarActivity detected entity model change because of user change");
							//								invalidateOptionsMenu();
							//								scanForBeacons(new ScanOptions(false, true, R.string.progress_updating));
							//							}
							//							else 
							if (ProxiExplorer.getInstance().getEntityModel().getLastRefreshDate() != null
									&& (ProxiExplorer.getInstance().getEntityModel().getLastRefreshDate().longValue() > mEntityModelRefreshDate.longValue()
									|| ProxiExplorer.getInstance().getEntityModel().getLastActivityDate().longValue() > mEntityModelActivityDate
											.longValue())) {
								Logger.d(this, "CandiRadarActivity detected entity model change");
								mCommon.hideProgressDialog();
								invalidateOptionsMenu();
								updateRadarOnly();
							}
							//							else {
							//								/* We always do a check because the user might have moved */
							//								if (!Aircandi.returningFromDialog) {
							//									invalidateOptionsMenu();
							//									scanForBeacons(new ScanOptions(false, false, null));
							//								}
							//							}
						}
						finishResume(false);
					}
				}
			}

		}.execute();
	}

	public void finishResume(Boolean startScan) {

		NetworkManager.getInstance().onResume();
		GeoLocationManager.getInstance().onResume();
		mCommon.doResume();
		synchronized (Events.EventBus.wifiScanReceived) {
			Events.EventBus.wifiScanReceived.add(mEventScanReceived);
		}
		mCommon.startScanService();
		Aircandi.returningFromDialog = false;
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
		if (mCommon != null) {
			mCommon.doDestroy();
		}

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

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	public Boolean updateCheckNeeded() {
		Boolean doUpdateCheck = (Aircandi.lastApplicationUpdateCheckDate == null
				|| (DateUtils.nowDate().getTime() - Aircandi.lastApplicationUpdateCheckDate.longValue()) > CandiConstants.INTERVAL_UPDATE_CHECK);
		return doUpdateCheck;
	}

	public ModelResult checkForUpdate() {
		ModelResult result = new ModelResult();

		result = ProxiExplorer.getInstance().getEntityModel().checkForUpdate();
		if (result.serviceResponse.responseCode == ResponseCode.Success) {
			if (Aircandi.applicationUpdateNeeded) {

				Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
				intent.setData(Uri.parse(Aircandi.applicationUpdateUri));
				mCommon.showNotification(getString(R.string.alert_upgrade_title)
						, getString(Aircandi.applicationUpdateRequired ? R.string.alert_upgrade_required_body : R.string.alert_upgrade_needed_body)
						, CandiRadar.this
						, intent
						, CandiConstants.NOTIFICATION_UPDATE);

				/* Show the update dialog */
				showUpdateAlertDialog();
			}
		}
		return result;
	}

	public void showUpdateAlertDialog() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (mUpdateAlertDialog == null || !mUpdateAlertDialog.isShowing()) {
					mUpdateAlertDialog = AircandiCommon.showAlertDialog(R.drawable.ic_app
							, getString(R.string.alert_upgrade_title)
							, getString(Aircandi.applicationUpdateRequired ? R.string.alert_upgrade_required_body
									: R.string.alert_upgrade_needed_body)
							, null
							, CandiRadar.this
							, R.string.alert_upgrade_ok
							, R.string.alert_upgrade_cancel
							, new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog, int which) {
									if (which == Dialog.BUTTON_POSITIVE) {
										Logger.d(CandiRadar.this, "Update check: navigating to install page");
										Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
										intent.setData(Uri.parse(Aircandi.applicationUpdateUri));
										startActivity(intent);
										AnimUtils.doOverridePendingTransition(CandiRadar.this, TransitionType.CandiPageToForm);
									}
									else if (which == Dialog.BUTTON_NEGATIVE) {
										/*
										 * We don't continue running if user doesn't install a required
										 * update
										 */
										if (Aircandi.applicationUpdateRequired) {
											Logger.d(CandiRadar.this, "Update check: user declined");
											finish();
										}
									}
								}
							}
							, new DialogInterface.OnCancelListener() {

								@Override
								public void onCancel(DialogInterface dialog) {
									if (Aircandi.applicationUpdateRequired) {
										Logger.d(CandiRadar.this, "Update check: user canceled");
										finish();
									}
								}
							});
					mUpdateAlertDialog.setCanceledOnTouchOutside(false);
				}
			}
		});
	}

	// --------------------------------------------------------------------------------------------
	// Preferences routines
	// --------------------------------------------------------------------------------------------

	public Boolean updatePreferences(Boolean okToFixup) {

		PrefResponse prefResponse = PrefResponse.None;
		if (mPrefAutoscan != Aircandi.settings.getBoolean(Preferences.PREF_AUTOSCAN, false)) {
			prefResponse = PrefResponse.Refresh;
			mPrefAutoscan = Aircandi.settings.getBoolean(Preferences.PREF_AUTOSCAN, false);
		}

		if (!mPrefAutoscanInterval.equals(Aircandi.settings.getString(Preferences.PREF_AUTOSCAN_INTERVAL, "5000"))) {
			prefResponse = PrefResponse.Refresh;
			mPrefAutoscanInterval = Aircandi.settings.getString(Preferences.PREF_AUTOSCAN_INTERVAL, "5000");
		}

		if (mPrefTestingBeacons != Aircandi.settings.getString(Preferences.PREF_TESTING_BEACONS, "natural")) {
			prefResponse = PrefResponse.Test;
			mPrefTestingBeacons = Aircandi.settings.getString(Preferences.PREF_TESTING_BEACONS, "natural");
		}

		if (mPrefGlobalBeacons != Aircandi.settings.getBoolean(Preferences.PREF_GLOBAL_BEACONS, true)) {
			prefResponse = PrefResponse.Refresh;
			mPrefGlobalBeacons = Aircandi.settings.getBoolean(Preferences.PREF_GLOBAL_BEACONS, true);
		}

		mPrefSoundEffects = Aircandi.settings.getBoolean(Preferences.PREF_SOUND_EFFECTS, true);

		if (!mCommon.mPrefTheme.equals(Aircandi.settings.getString(Preferences.PREF_THEME, CandiConstants.THEME_DEFAULT))) {
			prefResponse = PrefResponse.Restart;
			mCommon.mPrefTheme = Aircandi.settings.getString(Preferences.PREF_THEME, CandiConstants.THEME_DEFAULT);
		}

		if (okToFixup) {

			if (prefResponse == PrefResponse.Refresh) {
				Logger.d(this, "onResumeGame: Starting UI refresh because of preference change");
				scanForBeacons(new ScanOptions(false, false, R.string.progress_updating));
			}
			else if (prefResponse == PrefResponse.Rebuild) {
				Logger.d(this, "onResumeGame: Starting full beacon scan because of preference change");
				scanForBeacons(new ScanOptions(true, true, R.string.progress_scanning));
			}
			else if (prefResponse == PrefResponse.Restart) {
				/* Example: changing the theme requires recreating the activity */
				Logger.v(this, "Restarting from onResumeGame because of theme change");
				Aircandi.runFullScanOnRadarRestart = true;
				mCommon.reload();
			}
			else if (prefResponse == PrefResponse.Test) {
				/* Used for testing */
				Logger.v(this, "Testing pref changes");
			}
		}

		return (prefResponse != PrefResponse.None);
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

	@SuppressWarnings("unused")
	private void recycleBitmaps() {
		// mCommon.recycleImageViewDrawable(R.id.image_public);
		// mCommon.recycleImageViewDrawable(R.id.image_public_reflection);
	}

	@SuppressWarnings("unused")
	private String getAnalyticsId() {
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

	// --------------------------------------------------------------------------------------------
	// Misc classes/interfaces/enums
	// --------------------------------------------------------------------------------------------

	public interface IScanCompleteListener {

		void onScanComplete();
	}

	public static class SimpleAsyncTask extends AsyncTask<Object, Void, Object> {

		@Override
		protected Object doInBackground(Object... params) {
			return true;
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
		}

		@Override
		protected void onPostExecute(Object response) {}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			super.onProgressUpdate(values);
		}
	}

	public static enum AnimType {
		Fade,
		CrossFade,
		CrossFadeFlipper,
		RotateScene,
		RotateCandi,
		Zoom
	}

	public static enum PagerView {
		CandiInfo,
		CandiList,
		CandiInfoChild
	}

	public static enum Theme {
		Blueray,
		Midnight,
		Serene,
		Smolder,
		Snow
	}

	public static enum RefreshType {
		FullBuild,
		Standard,
		Autoscan
	}

}