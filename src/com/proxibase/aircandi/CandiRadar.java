package com.proxibase.aircandi;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.opengles.GL10;

import org.anddev.andengine.audio.sound.Sound;
import org.anddev.andengine.audio.sound.SoundFactory;
import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.engine.options.EngineOptions.ScreenOrientation;
import org.anddev.andengine.engine.options.WakeLockOptions;
import org.anddev.andengine.engine.options.resolutionpolicy.FillResolutionPolicy;
import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.modifier.AlphaModifier;
import org.anddev.andengine.entity.modifier.IEntityModifier.IEntityModifierListener;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.opengl.texture.Texture;
import org.anddev.andengine.opengl.texture.TextureManager.TextureListener;
import org.anddev.andengine.opengl.view.RenderSurfaceView;
import org.anddev.andengine.util.modifier.IModifier;
import org.anddev.andengine.util.modifier.ease.EaseLinear;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.net.NetworkInfo.State;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.amazonaws.auth.BasicAWSCredentials;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.proxibase.aircandi.Aircandi.CandiTask;
import com.proxibase.aircandi.Preferences.PrefResponse;
import com.proxibase.aircandi.candi.camera.ChaseCamera;
import com.proxibase.aircandi.candi.models.CandiModel;
import com.proxibase.aircandi.candi.models.CandiModel.DisplayExtra;
import com.proxibase.aircandi.candi.models.CandiPatchModel;
import com.proxibase.aircandi.candi.models.CandiPatchModel.Navigation;
import com.proxibase.aircandi.candi.models.IModel;
import com.proxibase.aircandi.candi.presenters.CandiPatchPresenter;
import com.proxibase.aircandi.candi.presenters.CandiPatchPresenter.ICandiListener;
import com.proxibase.aircandi.candi.presenters.CandiPatchPresenter.TextureReset;
import com.proxibase.aircandi.candi.views.ViewAction;
import com.proxibase.aircandi.candi.views.ViewAction.ViewActionType;
import com.proxibase.aircandi.components.AircandiCommon;
import com.proxibase.aircandi.components.AircandiCommon.ServiceOperation;
import com.proxibase.aircandi.components.AnimUtils;
import com.proxibase.aircandi.components.Command;
import com.proxibase.aircandi.components.Command.CommandType;
import com.proxibase.aircandi.components.Events;
import com.proxibase.aircandi.components.Events.EventHandler;
import com.proxibase.aircandi.components.Exceptions;
import com.proxibase.aircandi.components.GeoLocationManager;
import com.proxibase.aircandi.components.ImageCache;
import com.proxibase.aircandi.components.ImageManager;
import com.proxibase.aircandi.components.ImageRequest;
import com.proxibase.aircandi.components.ImageRequestBuilder;
import com.proxibase.aircandi.components.ImageUtils;
import com.proxibase.aircandi.components.IntentBuilder;
import com.proxibase.aircandi.components.Logger;
import com.proxibase.aircandi.components.NetworkManager;
import com.proxibase.aircandi.components.NetworkManager.IConnectivityListener;
import com.proxibase.aircandi.components.NetworkManager.IWifiReadyListener;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.components.ProxiExplorer;
import com.proxibase.aircandi.components.ProxiExplorer.EntityModel;
import com.proxibase.aircandi.components.ProxiExplorer.ScanOptions;
import com.proxibase.aircandi.components.ProxiHandlerManager;
import com.proxibase.aircandi.components.Tracker;
import com.proxibase.aircandi.components.VersionInfo;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.widgets.ActionsWindow;
import com.proxibase.aircandi.widgets.WebImageView;
import com.proxibase.service.ProxiConstants;
import com.proxibase.service.ProxibaseService;
import com.proxibase.service.ProxibaseService.GsonType;
import com.proxibase.service.ProxibaseService.RequestType;
import com.proxibase.service.ProxibaseService.ResponseFormat;
import com.proxibase.service.Query;
import com.proxibase.service.ServiceRequest;
import com.proxibase.service.objects.Entity;

/*
 * Texture Notes - Textures are loaded into hardware using bitmaps (one or more texture sources).
 * 
 * - Textures are unloaded from hardware when the activity loses its window (anything that switches away from this
 * activity and triggers onPause/onStop).
 * 
 * - Textures are reloaded into hardware when the activity regains its windows. The bitmap
 * used as the texture source is used again but won't fail even if the bitmap has been recycled. The andengine code
 * recycles bitmaps by default once they have been used to load a texture into hardware. The sprites just show up as
 * invisible.
 * 
 * - To simplify the reloading of textures, we allow the engine to try and use the recyled bitmap but
 * intervene to detect that the bitmap has been recycled and pull it again from the file cache. We have disabled the
 * memory cache since the hardware is acting like our memory cache. If the bitmap isn't in the file cache, we return
 * null to the engine and start the async process to fetch the bitmap and create a new BitmapTextureSource.
 * 
 * - Texture behavior: Textures get reloaded whenever the activity regains window focus. This causes a pause while the
 * work is being done so we have a workaround to smooth out animations that need it. When we are regaining window focus
 * after having displayed the candi info, we block texture reloading.
 * 
 * - VM limits: I believe that unlike bitmaps allocated on the native heap (Android version < 3.0), opengl textures do
 * not count toward the VM memory limit.
 */

/*
 * Library Notes
 * 
 * - AWS: We are using the minimum libraries: core and S3. We could do the work to call AWS without their
 * libraries which should give us the biggest savings.
 * 
 * - Gson and Guava: We could reduce our size by making the
 * libraries included with Android work instead of pulling in Gson (which in turn has a dependency on Guava).
 */

/*
 * Threading Notes
 * 
 * - AsyncTasks: AsyncTask uses a static internal work queue with a hard-coded limit of 10 elements.
 * Once we have 10 tasks going concurrently, task 11 causes a RejectedExecutionException. ThreadPoolExecutor is a way to
 * get more control over thread pooling but it requires Android version 11/3.0 (we currently target 7/2.1 and higher).
 * AsyncTasks are hard-coded with a low priority and continue their work even if the activity is paused.
 */

public class CandiRadar extends AircandiGameActivity implements TextureListener {

	private boolean						mFirstRun				= true;
	private AtomicBoolean				mFirstWindow			= new AtomicBoolean(true);
	private boolean						mPaused					= false;
	private Boolean						mReadyToRun				= false;
	private Boolean						mFullUpdateSuccess		= false;
	private Boolean						mBeaconScanActive		= false;
	private Handler						mHandler				= new Handler();
	public static BasicAWSCredentials	mAwsCredentials			= null;

	public Boolean						mPrefAutoscan			= false;
	public String						mPrefAutoscanInterval	= "5000";
	public boolean						mPrefDemoMode			= false;
	public boolean						mPrefGlobalBeacons		= true;
	public DisplayExtra					mPrefDisplayExtras		= DisplayExtra.None;
	public boolean						mPrefEntityFencing		= true;
	public boolean						mPrefShowDebug			= false;
	public boolean						mPrefSoundEffects		= true;

	private ProxiHandlerManager			mEntityHandlerManager;

	private CandiPatchModel				mCandiPatchModel;
	private CandiPatchPresenter			mCandiPatchPresenter;
	private RenderSurfaceView			mCandiSurfaceView;

	private ActionsWindow				mActionsWindow;
	private Sound						mCandiAlertSound;
	private ScreenOrientation			mScreenOrientation		= ScreenOrientation.PORTRAIT;
	private PackageReceiver				mPackageReceiver		= new PackageReceiver();
	private boolean						mUsingEmulator			= false;
	private Runnable					mScanRunnable;
	private BeaconScanWatcher			mScanWatcher;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		Logger.i(this, "CandiRadarActivity created");
		Logger.d(this, "Started from radar flag: " + String.valueOf(Aircandi.getInstance().getLaunchedFromRadar()));

		super.onCreate(savedInstanceState);
		if (CandiConstants.DEBUG_TRACE) {
			Debug.startMethodTracing("candi_search", 100000000);
		}

		initialize();
	}

	@Override
	protected void onSetContentView() {
		super.setContentView(getLayoutID());

		mRenderSurfaceView = (RenderSurfaceView) findViewById(getRenderSurfaceViewID());
		mRenderSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // To support transparency

		/*
		 * Triggers a full rendering pass including loading any textures that have been queued up. TODO: We sometimes
		 * get a null exception in updateTextures() line 134 even though we haven't loaded any textures yet.
		 */
		mRenderSurfaceView.setRenderer(mEngine);

		/* Use a surface format with an alpha channel */
		mRenderSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);

		/*
		 * Make sure the surface view is on top otherwise it a hole through anything above it in the zorder.
		 */
		mRenderSurfaceView.setZOrderOnTop(true);

		/* TODO: Rendering only when dirty is more battery efficient */
		mRenderSurfaceView.setRenderMode(RenderSurfaceView.RENDERMODE_WHEN_DIRTY);
	}

	private void initialize() {

		/* Start normal processing */
		mReadyToRun = false;

		/* Initialize preferences */
		updatePreferences();

		/* Setup scan runnable */
		mScanRunnable = new Runnable() {

			@Override
			public void run() {
				if (mCandiPatchModel == null
						|| mCandiPatchModel.getCandiRootCurrent() == null
						|| mCandiPatchModel.getCandiRootCurrent().isSuperRoot()) {
					Logger.i(CandiRadar.this, "Starting scheduled autoscan");
					scanForBeacons(new ScanOptions(false, false));
				}
				else if (mPrefAutoscan) {
					Logger.d(CandiRadar.this, "Scheduling an autoscan in: " + mPrefAutoscanInterval + " ms");
					mHandler.postDelayed(mScanRunnable, Integer.parseInt(mPrefAutoscanInterval));
				}
			}
		};

		/* Debug footer */
		debugSliderShow(mPrefShowDebug);
		if (mPrefShowDebug) {
			updateDebugInfo();
		}

		registerForContextMenu(findViewById(R.id.group_refresh));

		/* Analytics tracker */
		GoogleAnalyticsTracker.getInstance().startNewSession(getAnalyticsId(), this);
		mCommon.track();

		/* Check for emulator */
		if (Build.PRODUCT.equals("google_sdk") || Build.PRODUCT.equals("sdk")) {
			mUsingEmulator = true;
		}

		/* Connectivity monitoring */
		NetworkManager.getInstance().setContext(getApplicationContext());
		NetworkManager.getInstance().initialize();

		/* Proxibase sdk components */
		ProxibaseService.getInstance().isConnectedToNetwork(this);
		ProxiExplorer.getInstance().setContext(this.getApplicationContext());
		ProxiExplorer.getInstance().setUsingEmulator(mUsingEmulator);
		ProxiExplorer.getInstance().initialize();

		/* AWS Credentials */
		startGetCredentials();

		/* Image cache */
		ImageManager.getInstance().setImageCache(new ImageCache(getApplicationContext(), CandiConstants.CACHE_PATH, 100, 16));
		ImageManager.getInstance().setFileCacheOnly(true);
		ImageManager.getInstance().getImageLoader().setWebView((WebView) findViewById(R.id.webview));
		ImageManager.getInstance().setActivity(this);
		ImageManager.getInstance().setDisplayMetrics(getResources().getDisplayMetrics());

		/* Candi patch */
		mCandiPatchModel = new CandiPatchModel();
		mCandiPatchModel.setScreenWidth(ImageManager.getInstance().getDisplayMetrics().widthPixels);
		mCommon.setCandiPatchModel(mCandiPatchModel);

		/* Proxi activities */
		mEntityHandlerManager = new ProxiHandlerManager(this);

		/* Property settings get overridden once we retrieve preferences */
		mCandiSurfaceView = (RenderSurfaceView) findViewById(R.id.view_rendersurface);
		mCandiSurfaceView.requestFocus();
		mCandiSurfaceView.setFocusableInTouchMode(true);

		/* Get setup for location snapshots */
		GeoLocationManager.getInstance().setContext(getApplicationContext());
		GeoLocationManager.getInstance().initialize();

		/*
		 * Initial user login - uses cached user or anonymous and does not call the service
		 */
		verifyUser();

		Aircandi.getInstance().setLaunchedFromRadar(true);

		mReadyToRun = true;
	}

	private void startGetCredentials() {
		Thread t = new Thread() {

			@Override
			public void run() {
				try {
					Properties properties = new Properties();
					properties.load(getClass().getResourceAsStream("AwsCredentials.properties"));

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

	public void onCommandButtonClick(View view) {
		if (mActionsWindow != null) {
			mActionsWindow.dismiss();
		}
		Command command = (Command) view.getTag();
		mCommon.doCommand(command);
	}

	public void onBackPressed() {
		if (mCandiPatchModel != null && mCandiPatchModel.getCandiRootCurrent() != null
				&& mCandiPatchModel.getCandiRootCurrent().getParent() != null) {
			navigateUp();
		}
		else {
			super.onBackPressed();
		}
	}

	public void onParentCandiClick(View view) {
		onBackPressed();
	}

	public void onHomeClick(View view) {
		if (mEngine != null && mEngine.getTextureManager() != null
				&& Aircandi.getInstance().getUser() != null
				&& Aircandi.getInstance().getUser().isDeveloper != null
				&& Aircandi.getInstance().getUser().isDeveloper) {
			for (Texture texture : mEngine.getTextureManager().mTexturesLoaded) {
				Logger.v(this, texture.toString());
			}
		}
	}

	public void onTabClick(View view) {
		if (view.getTag().equals("radar")) {
			if (mCandiPatchModel != null && mCandiPatchModel.getCandiRootCurrent() != null
					&& mCandiPatchModel.getCandiRootCurrent().getParent() != null) {
				mCandiPatchPresenter.renderingActivate();
				mCandiPatchPresenter.navigateModel(mCandiPatchModel.getCandiRootCurrent().getParent(), false, false, Navigation.Up);
				return;
			}
			if (Aircandi.getInstance().getCandiTask() != CandiTask.RadarCandi) {
				mCommon.setActiveTab(view);
				Aircandi.getInstance().setCandiTask(CandiTask.RadarCandi);
				Intent intent = new Intent(this, CandiRadar.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
				startActivity(intent);
				overridePendingTransition(R.anim.fade_in_short, R.anim.fade_out_short);
			}
		}
		else if (view.getTag().equals("mycandi")) {
			if (Aircandi.getInstance().getUser() != null) {
				mCommon.setActiveTab(view);
				Aircandi.getInstance().setCandiTask(CandiTask.MyCandi);
				IntentBuilder intentBuilder = new IntentBuilder(this, CandiList.class);
				Intent intent = intentBuilder.create();
				startActivity(intent);
				overridePendingTransition(R.anim.fade_in_medium, R.anim.hold);
			}
		}
		else if (view.getTag().equals("map")) {
			if (Aircandi.getInstance().getUser() != null) {
				mCommon.setActiveTab(view);
				Aircandi.getInstance().setCandiTask(CandiTask.Map);

				IntentBuilder intentBuilder = new IntentBuilder(this, CandiMap.class);
				Intent intent = intentBuilder.create();
				startActivity(intent);
				overridePendingTransition(R.anim.fade_in_medium, R.anim.hold);
			}
		}
	}

	@SuppressWarnings("deprecation")
	public void onNewCandiClick(View view) {
		if (Aircandi.getInstance().getUser() != null) {
			showDialog(CandiConstants.DIALOG_NEW_CANDI_ID);
		}
	}

	private void onCandiSingleTap(final CandiModel candiModel) {
		/*
		 * This event bubbles up from user interaction with CandiViews. This can
		 * get called from threads other than the main UI thread.
		 * 
		 * The initial handling of candi single tap is handled in
		 * CandiPatchPresenter.doCandiViewSingleTap as a navigate or passed
		 * up to here as a single tap.
		 */
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				doCandiAction(candiModel);
			}
		});
	}

	// --------------------------------------------------------------------------------------------
	// Entity routines
	// --------------------------------------------------------------------------------------------

	private void scanForBeacons(final ScanOptions scanOptions) {
		/*
		 * Everything associated with this call is on the main thread but the UI is still responsive because most of the
		 * UI is being handled by the 2d engine thread.
		 */
		if (mBeaconScanActive) return;

		/* Make sure there aren't any extra runnables waiting to run */
		mHandler.removeCallbacks(mScanRunnable);

		/* Check that wifi is enabled and we have a network connection */
		mBeaconScanActive = true;

		verifyWifi(new IWifiReadyListener() {

			@Override
			public void onWifiReady() {

				/* Time to turn on the progress indicators */
				if (scanOptions.showProgress) {
					mCommon.showProgressDialog(false, null);
					mCommon.showProgressDialog(true, getString(R.string.progress_scanning), CandiRadar.this);
					if (mCandiPatchPresenter != null && !mCandiPatchPresenter.isVisibleEntity()) {
						mCandiPatchPresenter.renderingActivate(30000);
					}
					else {
						mCommon.startTitlebarProgress();
					}
				}

				/*
				 * We track the progress of a full scan because it effects lots of async processes like candi view
				 * management.
				 */
				Aircandi.getInstance().setRebuildingDataModel(scanOptions.fullBuild);

				mScanWatcher = new BeaconScanWatcher();
				mScanWatcher.start(scanOptions);
			}

			@Override
			public void onWifiFailed() {
				mBeaconScanActive = false;
			}
		});
	}

	private class BeaconScanWatcher {

		private ScanOptions	mScanOptions;

		public BeaconScanWatcher() {
			/*
			 * This is a one shot event receiver. As we capture events we unregister for them with the event bus.
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
								if (mScanOptions.fullBuild) {
									ProxiExplorer.getInstance().getEntityModel().getBeacons().clear();
								}
								ProxiExplorer.getInstance().processBeaconsFromScan();
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
							/*
							 * In case the user navigated in while we were doing an autoscan refresh, skip the data
							 * processing, wrap this up, and schedule another scan. Auto scheduled scans will keep
							 * getting skipped and rescheduled as long as the user is drilled in.
							 */
							Boolean atParentLevel = (mCandiPatchModel == null
									|| mCandiPatchModel.getCandiRootCurrent() == null
									|| mCandiPatchModel.getCandiRootCurrent().isSuperRoot());

							if (!mPrefAutoscan || atParentLevel) {
								EntityModel entityModel = ProxiExplorer.getInstance().getEntityModel();
								mCandiPatchPresenter.updateCandiData(entityModel, mScanOptions.fullBuild, false, false);

								/* Check for rookies and play a sound */
								if (mPrefSoundEffects && mCandiPatchPresenter.getEntityModelSnapshot().getRookieHit()) {
									mCandiAlertSound.play();
								}
							}
						}
						Events.EventBus.onBeaconScanComplete(serviceResponse);
					}
				});
			}

			synchronized (Events.EventBus.beaconScanComplete) {
				Events.EventBus.beaconScanComplete.add(new EventHandler() {

					@Override
					public void onEvent(Object data) {

						synchronized (Events.EventBus.beaconScanComplete) {
							Events.EventBus.beaconScanComplete.remove(this);
						}

						final ServiceResponse serviceResponse = (ServiceResponse) data;

						runOnUiThread(new Runnable() {

							@Override
							public void run() {

								if (serviceResponse.responseCode == ResponseCode.Success) {

									/* Wrap-up */
									if (mFirstRun) {
										mFirstRun = false;
									}

									if (mScanOptions.fullBuild && !mFullUpdateSuccess) {
										mFullUpdateSuccess = true;
									}

									/* Add a call to pass along analytics */
									updateDebugInfo();

									mCandiPatchPresenter.setFullUpdateInProgress(false);
									mCommon.stopTitlebarProgress();
									mCommon.showProgressDialog(false, null);

									/*
									 * Schedule the next wifi scan run if autoscan is enabled | The autoscan will pick
									 * up new beacons and changes in visibility of the entities associated with beacons
									 * that are already being tracked. This is meant to be an efficient refresh that can
									 * run continuously without a ton of data traffic. So there won't be any calls to
									 * the data service unless we discover a new beacon.
									 */
									if (mPrefAutoscan) {
										Logger.d(CandiRadar.this, "Scheduling an autoscan in: " + mPrefAutoscanInterval + " ms");
										mHandler.postDelayed(mScanRunnable, Integer.parseInt(mPrefAutoscanInterval));
									}
								}
								else {
									mCommon.handleServiceError(serviceResponse, ServiceOperation.BeaconScan, CandiRadar.this);
								}
								mBeaconScanActive = false;
							}
						});
					}
				});
			}
		}

		public void start(final ScanOptions scanOptions) {

			mScanOptions = scanOptions;
			new AsyncTask() {

				@Override
				protected Object doInBackground(Object... params) {

					/* Make sure we have a user */
					ServiceResponse serviceResponse = verifyUser();

					if (serviceResponse.responseCode == ResponseCode.Success) {

						if (mScanOptions.fullBuild && mCandiPatchPresenter != null) {
							mCandiPatchPresenter.renderingActivate(60000);
							mCandiPatchPresenter.setFullUpdateInProgress(true);
							/*
							 * Quick check for a new version. We continue even if the network call fails.
							 */
							if (mFirstRun) {
								checkForUpdate();
							}
						}
						ProxiExplorer.getInstance().scanForWifi(null);
						return null;
					}
					else {
						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								mCommon.stopTitlebarProgress();
							}
						});
						mCandiPatchPresenter.setFullUpdateInProgress(false);
						mBeaconScanActive = false;
						mCommon.handleServiceError(serviceResponse, ServiceOperation.LinkLookup, CandiRadar.this);
					}
					return null;
				}

			}.execute();
		}
	}

	public void doRefresh(RefreshType refreshType, ScanOptions options) {

		NetworkManager.getInstance().reset();
		if (refreshType == RefreshType.FullBuild || !mFullUpdateSuccess) {
			Logger.i(this, "Starting full build beacon scan");
			Tracker.trackEvent("Radar", "Refresh", "FullBuild", 0);
			scanForBeacons(options == null ? new ScanOptions(true, true) : options);
		}
		else if (refreshType == RefreshType.Standard || refreshType == RefreshType.Autoscan) {

			Logger.i(this, "User action starting standard refresh");
			Tracker.trackEvent("Radar", "Refresh", "Standard", 0);

			/* Track the entity that currently has the focus */
			final CandiModel candiModelFocused = mCandiPatchModel.getCandiModelFocused();
			String idOfEntityToRefresh = null;
			if (candiModelFocused != null && candiModelFocused.getViewStateCurrent().isVisible()) {
				idOfEntityToRefresh = candiModelFocused.getEntity().id;
			}
			/*
			 * This will come back before we really know if the candi we want to refresh textures for is stillaround.
			 */
			scanForBeacons(options == null ? new ScanOptions(false, true) : options);

			/*
			 * Scan could have caused the current candi to go away or be hidden
			 */
			if (idOfEntityToRefresh != null) {
				Entity entity = ProxiExplorer.getInstance().getEntityModel().getEntityById(idOfEntityToRefresh);

				/* We refresh the image if it's using a linkUri */
				if (entity != null && !entity.hidden && entity.linkUri != null && !entity.linkUri.equals("")) {

					/* Refresh candi view texture */
					Logger.v(this, "Update texture: " + candiModelFocused.getTitleText() != null ? candiModelFocused.getTitleText() : "[Untitled]");

					/* Start rendering because updates require that the update thread is running */
					synchronized (candiModelFocused.getViewActions()) {
						candiModelFocused.getViewActions().addFirst(new ViewAction(ViewActionType.UpdateTexturesForce));
					}
					candiModelFocused.setChanged();
					candiModelFocused.update();

					for (IModel childModel : candiModelFocused.getChildren()) {
						CandiModel childCandiModel = (CandiModel) childModel;
						Entity childEntity = childCandiModel.getEntity();
						if (childEntity != null && !childEntity.hidden && childEntity.linkUri != null && !childEntity.linkUri.equals("")) {
							synchronized (childCandiModel.getViewActions()) {
								childCandiModel.getViewActions().addFirst(new ViewAction(ViewActionType.UpdateTexturesForce));
							}
							childCandiModel.setChanged();
							childCandiModel.update();
						}
					}
				}
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	private void doCandiAction(final CandiModel candiModel) {
		/*
		 * When starting from a candi in search view we always target the first CandiInfo view in the flipper.
		 */
		if (candiModel.getEntity().type == CandiConstants.TYPE_CANDI_COMMAND) {
			if (mCandiPatchPresenter != null) {
				mCandiPatchPresenter.mIgnoreInput = false;
			}
			if (candiModel.getEntity().command.type == CommandType.ChunkEntities) {

				new AsyncTask() {

					@Override
					protected void onPreExecute() {
						mCommon.showProgressDialog(false, null);
						mCommon.showProgressDialog(true, getString(R.string.progress_more), CandiRadar.this);
						mCommon.startTitlebarProgress();
					}

					@Override
					protected Object doInBackground(Object... params) {
						ServiceResponse serviceResponse = ProxiExplorer.getInstance().chunkEntities();
						if (serviceResponse.responseCode == ResponseCode.Success) {
							Logger.d(CandiRadar.this, "More entities chunked from service");
							EntityModel entityModel = ProxiExplorer.getInstance().getEntityModel();
							mCandiPatchPresenter.updateCandiData(entityModel, false, true, false);
						}
						return serviceResponse;
					}

					@Override
					protected void onPostExecute(Object response) {
						ServiceResponse serviceResponse = (ServiceResponse) response;
						updateDebugInfo();
						mCommon.stopTitlebarProgress();
						mCommon.showProgressDialog(false, null);
						if (serviceResponse.responseCode != ResponseCode.Success) {
							mCommon.handleServiceError(serviceResponse, ServiceOperation.Chunking, CandiRadar.this);
						}
					}

				}.execute();
			}
			else if (candiModel.getEntity().command.type == CommandType.ChunkChildEntities) {

				new AsyncTask() {

					@Override
					protected void onPreExecute() {
						mCommon.showProgressDialog(false, null);
						mCommon.showProgressDialog(true, getString(R.string.progress_more), CandiRadar.this);
						mCommon.startTitlebarProgress();
					}

					@Override
					protected Object doInBackground(Object... params) {
						ServiceResponse serviceResponse = ProxiExplorer.getInstance().chunkChildEntities(candiModel.getEntity().command.entityParentId);
						if (serviceResponse.responseCode == ResponseCode.Success) {
							Logger.d(CandiRadar.this, "More child entities chunked from service");
							EntityModel entityModel = ProxiExplorer.getInstance().getEntityModel();
							mCandiPatchPresenter.updateCandiData(entityModel, false, true, false);
						}
						return serviceResponse;
					}

					@Override
					protected void onPostExecute(Object response) {
						ServiceResponse serviceResponse = (ServiceResponse) response;
						updateDebugInfo();
						mCommon.stopTitlebarProgress();
						mCommon.showProgressDialog(false, null);
						if (serviceResponse.responseCode != ResponseCode.Success) {
							mCommon.handleServiceError(serviceResponse, ServiceOperation.Chunking, CandiRadar.this);
						}
					}

				}.execute();
			}
		}
		else if (!candiModel.isDeleted()) {
			mCandiPatchModel.setCandiModelSelected(candiModel);
			showCandiForm(candiModel);
		}
	}

	private void navigateUp() {
		mCandiPatchPresenter.renderingActivate();
		mCandiPatchPresenter.navigateModel(mCandiPatchModel.getCandiRootCurrent().getParent(), false, false, Navigation.Up);
		getSupportActionBar().setDisplayHomeAsUpEnabled(false);

		mHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				showParentButton(false, true);
			}
		}, 200);
	}
	
	private void showCandiForm(CandiModel candiModel) {
		Entity entity = mCandiPatchModel.getCandiModelSelected().getEntity();

		IntentBuilder intentBuilder = new IntentBuilder(this, CandiForm.class);
		intentBuilder.setCommand(new Command(CommandType.View));
		intentBuilder.setEntity(entity);
		intentBuilder.setEntityId(entity.id);
		intentBuilder.setEntityType(entity.type);
		if (!entity.root) {
			intentBuilder.setEntityLocation(entity.parent.location);
		}
		Intent intent = intentBuilder.create();

		startActivity(intent);
		overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
	}

	class PackageReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(final Context context, Intent intent) {

			/* This is on the main UI thread */
			if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)) {
				String publicName = mEntityHandlerManager.getPublicName(intent.getData().getEncodedSchemeSpecificPart());
				if (publicName != null) {
					ImageUtils.showToastNotification(publicName + getText(R.string.dialog_install_toast_package_installed),
							Toast.LENGTH_SHORT);
				}
			}
		}
	}

	public void debugSliderShow(final boolean visible) {

		final ViewGroup slider = (ViewGroup) findViewById(R.id.debug_wrapper_radar);
		if (slider != null) {

			int animationResource = visible ? R.anim.fade_in_medium : R.anim.fade_out_medium;
			Animation animation = AnimationUtils.loadAnimation(this, animationResource);
			animation.setFillEnabled(true);
			animation.setFillAfter(true);
			animation.setAnimationListener(new AnimationListener() {

				@Override
				public void onAnimationEnd(Animation animation) {
					if (!visible) {
						slider.setVisibility(View.GONE);
						slider.findViewById(R.id.debug_drawer_radar).setVisibility(View.GONE);
					}
				}

				@Override
				public void onAnimationRepeat(Animation animation) {}

				@Override
				public void onAnimationStart(Animation animation) {
					if (visible) {
						slider.setVisibility(View.INVISIBLE);
						slider.findViewById(R.id.debug_drawer_radar).setVisibility(View.VISIBLE);
					}
				}
			});
			slider.startAnimation(animation);
		}
	}

	public void updateDebugInfo() {

		final ViewGroup slider = (ViewGroup) findViewById(R.id.debug_wrapper_radar);
		if (slider != null) {

			if (mPrefShowDebug) {

				Debug.MemoryInfo memoryInfo = new Debug.MemoryInfo();
				Debug.getMemoryInfo(memoryInfo);

				((RelativeLayout) findViewById(R.id.debug_section_one)).removeAllViews();
				TableLayout table = new TableLayout(this);
				TableLayout.LayoutParams tableLp = new TableLayout.LayoutParams();
				tableLp.setMargins(0, 0, 0, 0);

				TableRow tableRow = (TableRow) getLayoutInflater().inflate(R.layout.temp_tablerow_debug, null);
				((TextView) tableRow.findViewById(R.id.text_label)).setText("Native heap size: ");
				((TextView) tableRow.findViewById(R.id.text_value)).setText(String.format("%2.2f MB",
						(float) ((float) Debug.getNativeHeapSize() / 1048576f)));
				table.addView(tableRow, tableLp);

				tableRow = (TableRow) getLayoutInflater().inflate(R.layout.temp_tablerow_debug, null);
				((TextView) tableRow.findViewById(R.id.text_label)).setText("Native heap alloc: ");
				((TextView) tableRow.findViewById(R.id.text_value)).setText(String.format("%2.2f MB",
						(float) ((float) Debug.getNativeHeapAllocatedSize() / 1048576f)));
				table.addView(tableRow, tableLp);

				tableRow = (TableRow) getLayoutInflater().inflate(R.layout.temp_tablerow_debug, null);
				((TextView) tableRow.findViewById(R.id.text_label)).setText("Native heap free: ");
				((TextView) tableRow.findViewById(R.id.text_value)).setText(String.format("%2.2f MB",
						(float) ((float) Debug.getNativeHeapFreeSize() / 1048576f)));
				table.addView(tableRow, tableLp);

				tableRow = (TableRow) getLayoutInflater().inflate(R.layout.temp_tablerow_debug, null);
				((TextView) tableRow.findViewById(R.id.text_label)).setText("Total pss: ");
				((TextView) tableRow.findViewById(R.id.text_value)).setText(String.format("%2.2f MB",
						(float) ((float) memoryInfo.getTotalPss() / 1024)));
				table.addView(tableRow, tableLp);

				tableRow = (TableRow) getLayoutInflater().inflate(R.layout.temp_tablerow_debug, null);
				((TextView) tableRow.findViewById(R.id.text_label)).setText("Total shared dirty: ");
				((TextView) tableRow.findViewById(R.id.text_value)).setText(String.format("%2.2f MB",
						(float) ((float) memoryInfo.getTotalSharedDirty() / 1024)));
				table.addView(tableRow, tableLp);

				tableRow = (TableRow) getLayoutInflater().inflate(R.layout.temp_tablerow_debug, null);
				((TextView) tableRow.findViewById(R.id.text_label)).setText("Total priv dirty: ");
				((TextView) tableRow.findViewById(R.id.text_value)).setText(String.format("%2.2f MB",
						(float) ((float) memoryInfo.getTotalPrivateDirty() / 1024)));
				table.addView(tableRow, tableLp);

				((RelativeLayout) findViewById(R.id.debug_section_one)).addView(table);

				/*
				 * Monitor textures Global textures: 1 Textures per zone: 1 Textures per candi: 3
				 */

				/* Determine total size of textures loaded */
				int totalTextureSize = 0;
				if (mEngine != null && mEngine.getTextureManager() != null && mEngine.getTextureManager().mTexturesManaged != null) {
					for (Texture texture : mEngine.getTextureManager().mTexturesLoaded) {
						totalTextureSize += (texture.getWidth() * texture.getHeight()) * 4;
					}
				}

				((RelativeLayout) findViewById(R.id.debug_section_two)).removeAllViews();
				table = new TableLayout(this);
				tableLp = new TableLayout.LayoutParams();
				tableLp.setMargins(0, 0, 0, 0);

				int candiPooledCount = 0;
				int candiLoanedCount = 0;
				int zones = 0;
				int globalTexturesCount = 1;

				tableRow = (TableRow) getLayoutInflater().inflate(R.layout.temp_tablerow_debug, null);
				((TextView) tableRow.findViewById(R.id.text_label)).setText("Candi pooled: ");
				if (mCandiPatchPresenter != null && mCandiPatchPresenter.getCandiViewPool() != null) {
					candiPooledCount = mCandiPatchPresenter.getCandiViewPool().pooledCount();
					((TextView) tableRow.findViewById(R.id.text_value))
							.setText(String.valueOf(mCandiPatchPresenter.getCandiViewPool().pooledCount()));
				}
				table.addView(tableRow, tableLp);

				tableRow = (TableRow) getLayoutInflater().inflate(R.layout.temp_tablerow_debug, null);
				((TextView) tableRow.findViewById(R.id.text_label)).setText("Candi loaned: ");
				if (mCandiPatchPresenter != null && mCandiPatchPresenter.getCandiViewPool() != null) {
					candiLoanedCount = mCandiPatchPresenter.getCandiViewPool().loanedCount();
					((TextView) tableRow.findViewById(R.id.text_value))
							.setText(String.valueOf(mCandiPatchPresenter.getCandiViewPool().loanedCount()));
				}
				table.addView(tableRow, tableLp);

				tableRow = (TableRow) getLayoutInflater().inflate(R.layout.temp_tablerow_debug, null);
				((TextView) tableRow.findViewById(R.id.text_label)).setText("Zones: ");
				if (mCandiPatchPresenter != null && mCandiPatchPresenter.getCandiViewPool() != null) {
					((TextView) tableRow.findViewById(R.id.text_value)).setText(String.valueOf(mCandiPatchPresenter.mZoneViews.size()));
					zones = mCandiPatchPresenter.mZoneViews.size();
				}
				table.addView(tableRow, tableLp);

				tableRow = (TableRow) getLayoutInflater().inflate(R.layout.temp_tablerow_debug, null);
				((TextView) tableRow.findViewById(R.id.text_label)).setText("Txts managed: ");
				if (mEngine != null && mEngine.getTextureManager() != null && mEngine.getTextureManager().mTexturesManaged != null) {
					((TextView) tableRow.findViewById(R.id.text_value)).setText(String.valueOf(mEngine.getTextureManager().mTexturesManaged
							.size()));
				}
				table.addView(tableRow, tableLp);

				tableRow = (TableRow) getLayoutInflater().inflate(R.layout.temp_tablerow_debug, null);
				((TextView) tableRow.findViewById(R.id.text_label)).setText("Txts expected: ");
				((TextView) tableRow.findViewById(R.id.text_value)).setText(String.valueOf(((candiPooledCount + candiLoanedCount) * 3) + (zones * 1)
						+ globalTexturesCount));
				table.addView(tableRow, tableLp);

				tableRow = (TableRow) getLayoutInflater().inflate(R.layout.temp_tablerow_debug, null);
				((TextView) tableRow.findViewById(R.id.text_label)).setText("Txts tot size: ");
				((TextView) tableRow.findViewById(R.id.text_value)).setText(String.format("%2.2f MB",
						(float) ((float) totalTextureSize / 1048576)));
				table.addView(tableRow, tableLp);

				((RelativeLayout) findViewById(R.id.debug_section_two)).addView(table);
			}
		}
	}

	public void showNewCandiButton(boolean show) {
		View buttonFrame = (View) findViewById(R.id.button_layer);
		if (show) {
			buttonFrame.setAnimation(null);
			Animation animation = AnimUtils.loadAnimation(R.anim.fade_in_medium);
			animation.setFillEnabled(true);
			animation.setFillAfter(true);
			buttonFrame.startAnimation(animation);
		}
		else {
			buttonFrame.setAnimation(null);
			Animation animation = AnimUtils.loadAnimation(R.anim.fade_out_medium);
			animation.setFillEnabled(true);
			animation.setFillAfter(true);
			buttonFrame.startAnimation(animation);
		}
	}

	public void showParentButton(boolean show, boolean animate) {
		View buttonFrame = (View) findViewById(R.id.parent_layer);
		if (show) {
			if (animate) {
				buttonFrame.setAnimation(null);
				Animation animation = AnimUtils.loadAnimation(R.anim.fade_in_instant);
				animation.setFillEnabled(true);
				animation.setFillAfter(true);
				buttonFrame.startAnimation(animation);
			}
			else {
				buttonFrame.setVisibility(View.VISIBLE);
			}
		}
		else {
			if (animate) {
				WebImageView imageViewParent = (WebImageView) findViewById(R.id.image_parent);
				imageViewParent.clearImage(true, R.anim.parent_button_out);
			}
			else {
				WebImageView imageViewParent = (WebImageView) findViewById(R.id.image_parent);
				imageViewParent.clearImage(false, null);
			}
		}
	}

	public void setParentButtonImage(Entity entity) {
		WebImageView imageViewParent = (WebImageView) findViewById(R.id.image_parent);
		ImageRequestBuilder builder = new ImageRequestBuilder(imageViewParent);
		builder.setImageUri(entity.getMasterImageUri());
		builder.setImageFormat(entity.getMasterImageFormat());
		builder.setLinkZoom(entity.linkZoom);
		builder.setLinkJavascriptEnabled(entity.linkJavascriptEnabled);
		ImageRequest imageRequest = builder.create();
		imageViewParent.setImageRequest(imageRequest);
	}

	// --------------------------------------------------------------------------------------------
	// Connectivity routines
	// --------------------------------------------------------------------------------------------

	private void verifyWifi(final IWifiReadyListener listener) {

		if (!NetworkManager.getInstance().isWifiEnabled() && !ProxiExplorer.getInstance().isUsingEmulator()) {

			showNetworkDialog(true, getString(R.string.dialog_network_message_wifi_notready));
			final Button retryButton = (Button) findViewById(R.id.button_retry);
			final Button cancelButton = (Button) findViewById(R.id.button_cancel);
			final TextView txtMessage = (TextView) findViewById(R.id.retry_message);
			retryButton.setEnabled(false);
			cancelButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					showNetworkDialog(false, "");
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
						if (((View) findViewById(R.id.retry_dialog)).getVisibility() == View.VISIBLE) {
							((CheckBox) findViewById(R.id.wifi_enabled_checkbox)).setChecked(true);
							txtMessage.setText(getString(R.string.dialog_network_message_wifi_ready));
							retryButton.setEnabled(true);
							retryButton.setOnClickListener(new OnClickListener() {

								@Override
								public void onClick(View v) {
									/* Re-enter so we get to the next stage */
									NetworkManager.getInstance().setConnectivityListener(null);
									showNetworkDialog(false, "");
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
						ImageUtils.showToastNotification("Wifi state enabling...", Toast.LENGTH_SHORT);
					}
					else if (wifiState == WifiManager.WIFI_STATE_DISABLING) {
						ImageUtils.showToastNotification("Wifi state disabling...", Toast.LENGTH_SHORT);
					}
					else if (wifiState == WifiManager.WIFI_STATE_DISABLED) {
						((CheckBox) findViewById(R.id.wifi_enabled_checkbox)).setChecked(false);
						txtMessage.setText(getString(R.string.dialog_network_message_wifi_notready));
						retryButton.setEnabled(false);
						ImageUtils.showToastNotification("Wifi state disabled.", Toast.LENGTH_SHORT);
					}
				}
			});

		}
		else {
			if (listener != null) {
				Logger.i(this, "Wifi verified");
				listener.onWifiReady();
			}
		}
	}

	private ServiceResponse verifyUser() {
		if (Aircandi.getInstance().getUser() != null) {
			return new ServiceResponse();
		}
		else {
			mCommon.signinAuto();
			return new ServiceResponse();
		}
	}

	private void showNetworkDialog(boolean visible, String message) {

		if (visible) {
			TextView txtMessage = (TextView) findViewById(R.id.retry_message);
			CheckBox enableWifiCheckBox = (CheckBox) findViewById(R.id.wifi_enabled_checkbox);

			mCandiSurfaceView.setVisibility(View.GONE);

			txtMessage.setText(message);
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
			((View) findViewById(R.id.retry_dialog)).setVisibility(View.VISIBLE);
		}
		else {
			mCandiSurfaceView.setVisibility(View.VISIBLE);
			((View) findViewById(R.id.retry_dialog)).setVisibility(View.GONE);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Dialogs
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("unused")
	private void showInstallDialog(final CandiModel candi) {

		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				final Dialog dialog = new Dialog(CandiRadar.this, R.style.aircandi_theme_dialog);
				final RelativeLayout installDialog = (RelativeLayout) getLayoutInflater().inflate(R.layout.dialog_install, null);
				dialog.setContentView(installDialog, new FrameLayout.LayoutParams(400, 300, Gravity.CENTER));
				dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
				dialog.getWindow().setBackgroundDrawable(getResources().getDrawable(R.drawable.dialog_bg));

				((Button) installDialog.findViewById(R.id.btn_install_ok)).setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						// Intent goToMarket = new
						// Intent(Intent.ACTION_VIEW).setData(Uri.parse(candi.getEntity().entityHandler.code));
						// startActivityForResult(goToMarket,
						// CandiConstants.ACTIVITY_MARKET);
						// dialog.dismiss();
					}
				});
				((Button) installDialog.findViewById(R.id.btn_install_cancel)).setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						dialog.dismiss();
						if (!mEngine.isRunning()) {
							if (mCandiPatchModel.getCandiModels().size() > 0) {
								mCandiPatchPresenter.resetSharedTextures();
								mCandiPatchPresenter.resetTextures(TextureReset.VisibleOnly);
								new Thread(new Runnable() {

									@Override
									public void run() {
										mCandiPatchPresenter.resetTextures(TextureReset.NonVisibleOnly);
									}
								}).start();
							}
						}
					}
				});
				dialog.show();

			}
		});
	}

	@SuppressWarnings("deprecation")
	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == CandiConstants.DIALOG_NEW_CANDI_ID) {
			return mCommon.mIconContextMenu.createMenu(getString(R.string.dialog_new_message), this);
		}
		return super.onCreateDialog(id);
	}

	// --------------------------------------------------------------------------------------------
	// Graphic engine routines
	// --------------------------------------------------------------------------------------------

	@Override
	public Engine onLoadEngine() {

		Logger.d(this, "Initializing animation engine");
		/*
		 * For now, we only support portait orientation. Supporting orientation changes will likely require some fancy
		 * scaling transformation since the game engine locks on the supplied orientation.
		 */
		mScreenOrientation = ScreenOrientation.PORTRAIT;
		DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

		/* Adjusted for density */
		int statusBarHeight = (int) FloatMath.ceil(CandiConstants.ANDROID_STATUSBAR_HEIGHT * displayMetrics.density);
		int titleBarHeight = (int) FloatMath.ceil(CandiConstants.CANDI_TITLEBAR_HEIGHT * displayMetrics.density);
		int tabBarHeight = (int) FloatMath.ceil(CandiConstants.CANDI_TABBAR_HEIGHT * displayMetrics.density);
		int widthPixels = displayMetrics.widthPixels;
		int heightPixels = displayMetrics.heightPixels;

		if (widthPixels > heightPixels) {
			widthPixels = displayMetrics.heightPixels;
			heightPixels = displayMetrics.widthPixels;
		}

		Camera camera = new ChaseCamera(0, 0, widthPixels, heightPixels - (statusBarHeight + titleBarHeight + tabBarHeight)) {

			@Override
			public void onApplySceneMatrix(GL10 pGL) {
				// Gets called for every engine update
				mCandiPatchPresenter.setFrustum(pGL);
			}

			@Override
			public void onApplySceneBackgroundMatrix(GL10 pGL) {
				mCandiPatchPresenter.setFrustum(pGL);
			}

			@Override
			public void onApplyCameraSceneMatrix(GL10 pGL) {
				mCandiPatchPresenter.setFrustum(pGL);
			}
		};

		EngineOptions options = new EngineOptions(false, mScreenOrientation, new FillResolutionPolicy(), camera);
		options.setNeedsSound(true);
		options.setWakeLockOptions(WakeLockOptions.SCREEN_ON);
		Engine engine = new Engine(options);

		return engine;
	}

	@Override
	public void onLoadResources() {
		SoundFactory.setAssetBasePath("sfx/");
		try {
			mCandiAlertSound = SoundFactory.createSoundFromAsset(mEngine.getSoundManager(), this, "notification2.mp3");
		}
		catch (final IOException e) {
			e.printStackTrace();
		}
		mEngine.getTextureManager().setTextureListener(this);
	}

	@Override
	public Scene onLoadScene() {
		/*
		 * Called after Create, Resume->LoadEngine. CandiPatchPresenter handles scene instantiation and setup
		 */
		Logger.d(this, "Loading scene");
		mCandiPatchPresenter = new CandiPatchPresenter(this, this, mEngine, mRenderSurfaceView, mCandiPatchModel);
		mCandiPatchPresenter.setCommon(this.mCommon);
		Scene scene = mCandiPatchPresenter.initializeScene();

		mCandiPatchModel.addObserver(mCandiPatchPresenter);
		mCandiPatchPresenter.setCandiListener(new ICandiListener() {

			@Override
			public void onSelected(IModel candi) {}

			@Override
			public void onSingleTap(CandiModel candi) {
				CandiRadar.this.onCandiSingleTap(candi);
			}
		});

		return scene;
	}

	@Override
	public void onLoadComplete() {}

	@Override
	public void onResumeGame() {
		/*
		 * This gets called anytime the game surface gains window focus. The game engine acquries the wake lock,
		 * restarts the engine, resumes the GLSurfaceView. The engine reloads textures.
		 */
		Logger.d(this, "Starting animation engine");
		if (mReadyToRun) {

			if (mFirstRun) {
				Logger.i(this, "onResumeGame: Starting first run full beacon scan");
				scanForBeacons(new ScanOptions(true, true));
			}
			else {
				PrefResponse prefResponse = updatePreferences();
				if (prefResponse == PrefResponse.Refresh) {
					Logger.i(this, "onResumeGame: Starting full beacon scan because of preference change");
					scanForBeacons(new ScanOptions(true, true));
				}
				else if (prefResponse == PrefResponse.Restart) {
					mCommon.reload();
				}
			}
		}
	}

	@Override
	public void onPauseGame() {
		/*
		 * This gets called anytime the game surface loses window focus is called on the super class. The game engine
		 * releases the wake lock, stops the engine, pauses the GLSurfaceView.
		 */
		Logger.d(this, "Pausing animation engine");
	}

	@Override
	public void onUnloadResources() {
		super.onUnloadResources();
	}

	@Override
	public void onTexturesLoaded(int count) {}

	@Override
	public void onTexturesReady() {
		Logger.i(this, "Textures ready");
	}

	@Override
	protected int getLayoutID() {
		return R.layout.candi_radar;
	}

	@Override
	protected int getRenderSurfaceViewID() {
		return R.id.view_rendersurface;
	}

	@Override
	public void onWindowFocusChanged(final boolean hasWindowFocus) {
		/*
		 * Parent class will trigger pause or resume for the game engine based on hasWindowFocus. We control when this
		 * message get through to prevent unnecessary restarts. We block it if we lost focus becase of a pop up like a
		 * dialog or an android menu (which do not trigger this.onPause which in turns stops the engine).
		 */

		/*
		 * Losing focus: BaseGameActivity will pause the game engine
		 * Gaining focus: BaseGameActivity will resume the
		 * game engine if currently paused First life run we pass through window focus change here instead of onResume
		 * because the engine isn't ready after first run resume. First life resume starts engine.
		 */
		Logger.v(this, hasWindowFocus ? "Activity has window focus" : "Activity lost window focus");
		if (mFirstWindow.get()) {
			super.onWindowFocusChanged(hasWindowFocus);
			mFirstWindow.set(false);
		}
		else {
			/* Restart autoscan if enabled */
			if (hasWindowFocus) {
				if (mPrefAutoscan) {
					mHandler.post(mScanRunnable);
				}
				// else if (mCandiPatchModel != null && mCandiPatchModel.getCandiModels().size() == 0) {
				// mHandler.post(mScanRunnable);
				// }
			}
			else {
				/* Make sure autoscan is stopped if we are not in the foreground */
				mHandler.removeCallbacks(mScanRunnable);
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// System callbacks
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onStart() {
		/*
		 * Only called first time activity is started.
		 */
		Logger.i(this, "CandiRadarActivity starting");
		super.onStart();
	}

	@Override
	protected void onRestart() {
		/*
		 * This only gets called when the activity was stopped and is now coming back.
		 */
		Logger.i(this, "CandiRadarActivity restarting");
		super.onRestart();
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onResume() {
		super.onResume();
		/*
		 * OnResume gets called after OnCreate (always) and whenever the activity is being brought back to the
		 * foreground. Not guaranteed but is usually called just before the activity receives focus. Because code in
		 * OnCreate could have determined that we aren't ready to roll, isReadyToRun is used to indicate that prep work
		 * is complete. This is also called when the user jumps out and back from setting preferences so we need to
		 * refresh the places where they get used. Game engine is started/restarted in BaseGameActivity class if we
		 * currently have the window focus.
		 */

		/*
		 * Logic that should only run if the activity is resuming after having been paused. This used to be in restart
		 * but it wasn't getting called reliably when returning from another activity.
		 */
		if (mPaused) {
			Logger.i(this, "CandiRadarActivity resuming after pause");

			/* Theme might have changed since we were away */
			if (!mCommon.mPrefTheme.equals(Aircandi.settings.getString(Preferences.PREF_THEME, "aircandi_theme_midnight"))) {
				Logger.d(this, "Restarting because of theme change");
				mCommon.mPrefTheme = Aircandi.settings.getString(Preferences.PREF_THEME, "aircandi_theme_midnight");
				mCommon.reload();
			}
			else {
				/*
				 * We control window focus messages that trigger the engine from here.
				 */
				super.onWindowFocusChanged(true);

				/* We run a fake modifier so we can trigger texture loading */
				mEngine.getScene().registerEntityModifier(
						new AlphaModifier(CandiConstants.DURATION_TRANSITIONS_FADE, 0.0f, 1.0f, new IEntityModifierListener() {

							@Override
							public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
								onTexturesReady();
							}

							@Override
							public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {
								mCandiPatchPresenter.renderingActivate();
							}
						}, EaseLinear.getInstance()));

				/*
				 * Entities could have been inserted, updated or deleted
				 * by another activity so refresh our model
				 */
				scanForBeacons(new ScanOptions(false, false));

				/* Restarting autoscan happens when we gain window focus */
			}
			mPaused = false;
		}
		else {
			Logger.i(this, "CandiRadarActivity resuming for first time");
		}

		NetworkManager.getInstance().onResume();
		GeoLocationManager.getInstance().onResume();
		mCommon.doResume();
		mCommon.startScanService();

		/*
		 * CandiPatchPresenter is created in onLoadScene which gets called after the first onResume
		 */
		if (mCandiPatchPresenter != null) {
			mCandiPatchPresenter.mIgnoreInput = false;
		}

		/* Package receiver */
		IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
		filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
		filter.addAction(Intent.ACTION_PACKAGE_INSTALL);
		filter.addAction(Intent.ACTION_PACKAGE_DATA_CLEARED);
		filter.addDataScheme("package");
		registerReceiver(mPackageReceiver, filter);
	}

	@Override
	protected void onPause() {
		/*
		 * - Fires when we lose focus and have been moved into the background. This will
		 * be followed by onStop if we are not visible.
		 * 
		 * - Calling onPause on super will cause the engine to pause if it hasn't already
		 * been paused because of losing window focus.
		 * 
		 * - onPause does not fire if the activity window loses focus but the activity is still active.
		 */
		Logger.i(this, "CandiRadarActivity paused");
		mCommon.stopScanService();
		super.onPause();
		try {
			unregisterReceiver(mPackageReceiver);

			ProxiExplorer.getInstance().onPause();
			NetworkManager.getInstance().onPause();
			GeoLocationManager.getInstance().onPause();

			mCommon.doPause();
			mPaused = true;
		}
		catch (Exception exception) {
			Exceptions.Handle(exception);
		}
	}

	@Override
	protected void onStop() {
		/*
		 * Fired when starting another activity and we lose our window.
		 */
		Logger.i(this, "CandiRadarActivity stopped");
		super.onStop();

		/* We control window focus messages that trigger the engine from here. */
		super.onWindowFocusChanged(false);

		/* Make sure autoscan is stopped if we are not in the foreground */
		mHandler.removeCallbacks(mScanRunnable);

		if (CandiConstants.DEBUG_TRACE) {
			Debug.stopMethodTracing();
		}

		/* Start thread to check and manage the file cache. */
		ImageManager.getInstance().cleanCacheAsync(getApplicationContext());
	}

	@Override
	protected void onDestroy() {
		Logger.i(this, "CandiRadarActivity destroyed");
		super.onDestroy();
		if (mCommon != null) {
			mCommon.doDestroy();
		}

		Tracker.stopSession();

		/*
		 * Don't count on this always getting called when this activity is killed
		 */
		try {
			ProxiExplorer.getInstance().onDestroy();
			ImageManager.getInstance().getImageLoader().stopLoaderThread();
		}
		catch (Exception exception) {
			Exceptions.Handle(exception);
		}
	}

	@Override
	public void onLowMemory() {
		Logger.i(this, "CandiRadarActivity memory is low");
		super.onLowMemory();
	}

	// --------------------------------------------------------------------------------------------
	// Preferences routines
	// --------------------------------------------------------------------------------------------

	public PrefResponse updatePreferences() {

		PrefResponse prefResponse = PrefResponse.None;

		if (mPrefAutoscan != Aircandi.settings.getBoolean(Preferences.PREF_AUTOSCAN, false)) {
			prefResponse = PrefResponse.Change;
			mPrefAutoscan = Aircandi.settings.getBoolean(Preferences.PREF_AUTOSCAN, false);
			if (mPrefAutoscan) {
				mHandler.post(mScanRunnable);
			}
			else {
				mHandler.removeCallbacks(mScanRunnable);
			}
		}
		if (!mPrefAutoscanInterval.equals(Aircandi.settings.getString(Preferences.PREF_AUTOSCAN_INTERVAL, "5000"))) {
			prefResponse = PrefResponse.Change;
			mPrefAutoscanInterval = Aircandi.settings.getString(Preferences.PREF_AUTOSCAN_INTERVAL, "5000");
			if (mPrefAutoscan) {
				mHandler.removeCallbacks(mScanRunnable);
				mHandler.post(mScanRunnable);
			}
		}
		if (!mPrefDisplayExtras.equals(DisplayExtra.valueOf(Aircandi.settings.getString(Preferences.PREF_DISPLAY_EXTRAS, "None")))) {
			prefResponse = PrefResponse.Refresh;
			mPrefDisplayExtras = DisplayExtra.valueOf(Aircandi.settings.getString(Preferences.PREF_DISPLAY_EXTRAS, "None"));
		}
		if (mPrefDemoMode != Aircandi.settings.getBoolean(Preferences.PREF_DEMO_MODE, false)) {
			prefResponse = PrefResponse.Refresh;
			mPrefDemoMode = Aircandi.settings.getBoolean(Preferences.PREF_DEMO_MODE, false);
		}
		if (mPrefGlobalBeacons != Aircandi.settings.getBoolean(Preferences.PREF_GLOBAL_BEACONS, true)) {
			prefResponse = PrefResponse.Refresh;
			mPrefGlobalBeacons = Aircandi.settings.getBoolean(Preferences.PREF_GLOBAL_BEACONS, true);
		}

		if (mPrefShowDebug != Aircandi.settings.getBoolean(Preferences.PREF_SHOW_DEBUG, false)) {
			prefResponse = PrefResponse.Change;
			mPrefShowDebug = Aircandi.settings.getBoolean(Preferences.PREF_SHOW_DEBUG, false);
			debugSliderShow(mPrefShowDebug);
			if (mPrefShowDebug) {
				updateDebugInfo();
			}
		}

		mPrefSoundEffects = Aircandi.settings.getBoolean(Preferences.PREF_SOUND_EFFECTS, true);

		if (!mCommon.mPrefTheme.equals(Aircandi.settings.getString(Preferences.PREF_THEME, "aircandi_theme_midnight"))) {
			prefResponse = PrefResponse.Restart;
			mCommon.mPrefTheme = Aircandi.settings.getString(Preferences.PREF_THEME, "aircandi_theme_midnight");
		}
		return prefResponse;
	}

	// --------------------------------------------------------------------------------------------
	// Context menu routines (refresh commands)
	// --------------------------------------------------------------------------------------------

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);
		menu.setHeaderTitle(getString(R.string.menu_refresh_title));
		menu.add(0, RefreshType.Standard.ordinal(), 1, getString(R.string.menu_refresh_beacon_plus_current));
		menu.add(0, RefreshType.FullBuild.ordinal(), 2, getString(R.string.menu_refresh_all));
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		if (item.getItemId() == RefreshType.Standard.ordinal()) {
			doRefresh(RefreshType.Standard, null);
		}
		else if (item.getItemId() == RefreshType.FullBuild.ordinal()) {
			mFullUpdateSuccess = false;
			doRefresh(RefreshType.FullBuild, null);
		}
		return true;
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
		
		/* Need to cherry pick any menu commands that have contextual handling */
		if (menuItem.getItemId() == R.id.refresh) {
			if (mBeaconScanActive) {
				Logger.v(this, "User refresh request ignored because of active scan");
				return true;
			}

			if (mReadyToRun) {
				mCommon.startTitlebarProgress();
				doRefresh(RefreshType.Standard, null);
			}
			updateDebugInfo();
			return true;			
		}
		else if (menuItem.getItemId() == android.R.id.home) {
			if (mCandiPatchModel != null && mCandiPatchModel.getCandiRootCurrent() != null
					&& mCandiPatchModel.getCandiRootCurrent().getParent() != null) {
				navigateUp();
				return true;
			}
		}
		return mCommon.doOptionsItemSelected(menuItem);
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("unused")
	private void recycleBitmaps() {
		mCommon.recycleImageViewDrawable(R.id.image_public);
		mCommon.recycleImageViewDrawable(R.id.image_public_reflection);
	}

	private void checkForUpdate() {

		Query query = new Query("documents").filter("{\"type\":\"version\",\"target\":\"aircandi\"}");

		ServiceRequest serviceRequest = new ServiceRequest(ProxiConstants.URL_PROXIBASE_SERVICE, query, RequestType.Get, ResponseFormat.Json);
		serviceRequest.setSuppressUI(true);
		ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		/*
		 * We don't error this because it can be safely passed over until the next time.
		 */
		if (serviceResponse.responseCode == ResponseCode.Success) {

			String jsonResponse = (String) serviceResponse.data;
			final VersionInfo versionInfo = (VersionInfo) ProxibaseService.convertJsonToObject(jsonResponse, VersionInfo.class, GsonType.ProxibaseService).data;
			String currentVersionName = Aircandi.getVersionName(this, CandiRadar.class);

			if (!currentVersionName.equals(versionInfo.versionName)) {

				this.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						AircandiCommon.showAlertDialog(R.drawable.icon_app, getString(R.string.alert_upgrade_title),
								getString(R.string.alert_upgrade_body), CandiRadar.this, R.string.alert_upgrade_ok, R.string.alert_upgrade_cancel, new
								DialogInterface.OnClickListener() {

									public void onClick(DialogInterface dialog, int which) {
										if (which == Dialog.BUTTON_POSITIVE) {
											Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
											String updateUri = versionInfo.updateUri != null ? versionInfo.updateUri : CandiConstants.URL_AIRCANDI_UPGRADE;
											intent.setData(Uri.parse(updateUri));
											startActivity(intent);
											overridePendingTransition(R.anim.form_in, R.anim.browse_out);
										}
									}
								});

					}
				});

			}
		}
	}

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