package com.aircandi;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
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
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.opengl.texture.Texture;
import org.anddev.andengine.opengl.texture.TextureManager.TextureListener;
import org.anddev.andengine.opengl.view.RenderSurfaceView;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.net.NetworkInfo.State;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi.CandiTask;
import com.aircandi.Preferences.PrefResponse;
import com.aircandi.candi.camera.ChaseCamera;
import com.aircandi.candi.models.CandiModel;
import com.aircandi.candi.models.CandiModel.DisplayExtra;
import com.aircandi.candi.models.CandiPatchModel;
import com.aircandi.candi.models.IModel;
import com.aircandi.candi.presenters.CandiPatchPresenter;
import com.aircandi.candi.presenters.CandiPatchPresenter.ICandiListener;
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
import com.aircandi.service.ProxibaseService;
import com.aircandi.service.ProxibaseService.RequestType;
import com.aircandi.service.ProxibaseService.ResponseFormat;
import com.aircandi.service.ProxibaseService.ServiceDataType;
import com.aircandi.service.Query;
import com.aircandi.service.ServiceRequest;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.User;
import com.aircandi.service.objects.VersionInfo;
import com.amazonaws.auth.BasicAWSCredentials;

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
 * - Guava: This library provides MapMaker used to create the image cache.
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
 * - AndEngine: at the end of an updateTextures pass. TextureManager.updateTextures is called everytime
 * the engine is asked to draw a frame by the opengl renderer. System.gc is called if textures were
 * loaded or unloaded during the call.
 * ** Update **: 7/15/12: I have turned off the gc call in updateTextures to see if it is really needed **
 * 
 * - AndEngine: Font.update (don't believe we are using any).
 * 
 * Explicit bitmap recycling
 * 
 * - Anyplace where a new bitmap has been processed from another bitmap.
 * - Releasing bitmaps when forms are destroyed.
 * - Releasing bitmaps when list items are reused.
 * 
 * - AndEngine: after a bitmap has been pushed to hardware
 * - AndEngine: after a placeholder bitmap has been pushed to hardware
 */

/*
 * Lifecycle event sequences from Radar
 * 
 * Alert Dialog: None, focus toggle
 * Dialog Activity: Pause->LoseFocus->||Resume->GainFocus
 * Overflow menu: None, focus toggle
 * ProgressIndicator: None, focus toggle
 * 
 * Preferences: Pause->Stop->||Restart->Start->Resume->GainFocus
 * Profile: Pause->LoseFocus->Stop->||Restart->Start->Resume->GainFocus
 * 
 * Home: Pause->LoseFocus->Stop->||Restart->Start->Resume->GainFocus
 * Back: Pause->LoseFocus->Stop->Destroyed
 * Other Candi Activity: Pause->LoseFocus->Stop||Restart->Start->Resume->GainFocus
 * 
 * Power off with Aircandi in foreground: Pause->LoseFocus->Stop
 * Power on with Aircandi in foreground: Nothing
 * Unlock screen with Aircandi in foreground: Restart->Start->Resume->GainFocus
 * 
 * First Launch: Create->onStart->onResume
 * ->onAttachedToWindow->onWindowFocusChanged
 * ->onLoadEngine->onLoadScene->onResumeGame
 */

/*
 * Game engine management
 * 
 * Changes have been made so andengine is only told to pause/resume using window focus
 * event. We are blocking the pause event to prevent it from dumping all the textures.
 * We allow focus=true to go through on first gain of window focus and focus=false on destroy. The
 * game engine isn't actually working when still up because of our dirty rendering logic.
 */

/*
 * Scan management
 * 
 * There are three cases that trigger scans:
 * 
 * - First run scan: (onGameResume) When application is first started, we load the entity model with a full scan. The
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

@SuppressWarnings("unused")
public class CandiRadar extends AircandiGameActivity implements TextureListener {

	private AtomicBoolean				mFirstWindow			= new AtomicBoolean(true);
	private Boolean						mReadyToRun				= false;
	private Handler						mHandler				= new Handler();
	public static BasicAWSCredentials	mAwsCredentials			= null;

	/* We use these to track whether a preference gets changed */
	public Boolean						mPrefAutoscan			= false;
	public String						mPrefAutoscanInterval	= "5000";
	public boolean						mPrefDemoMode			= false;
	public boolean						mPrefGlobalBeacons		= true;
	public DisplayExtra					mPrefDisplayExtras		= DisplayExtra.None;
	public boolean						mPrefEntityFencing		= true;
	public boolean						mPrefShowDebug			= false;
	public boolean						mPrefSoundEffects		= true;
	public String						mPrefTestingBeacons		= "natural";

	private Number						mEntityModelRefreshDate;
	private Number						mEntityModelActivityDate;
	private User						mEntityModelUser;

	private CandiPatchModel				mCandiPatchModel;
	private CandiPatchPresenter			mCandiPatchPresenter;
	private RenderSurfaceView			mCandiSurfaceView;
	private ViewGroup					mWifiDialog;
	private ViewGroup					mEmptyDialog;

	private float						mRadarWidth;
	private float						mRadarHeight;

	private Sound						mCandiAlertSound;
	private ScreenOrientation			mScreenOrientation		= ScreenOrientation.PORTRAIT;
	private boolean						mUsingEmulator			= false;
	private Runnable					mScanRunnable;
	private Runnable					mScanRunnableWait;
	private ScanOptions					mScanOptions;
	private BeaconScanWatcher			mScanWatcher;
	public EventHandler					mEventScanReceived;
	private AlertDialog					mUpdateAlertDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		if (CandiConstants.DEBUG_TRACE) {
			Debug.startMethodTracing("candi_radar", 100000000);
		}

		initialize();
	}

	@Override
	protected void onSetContentView() {
		super.setContentView(getLayoutID());

		mRenderSurfaceView = (RenderSurfaceView) findViewById(getRenderSurfaceViewID());
		mRenderSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // To support transparency

		/*
		 * Triggers a full rendering pass including loading any textures that have been queued up.
		 * TODO: We sometimes get a null exception in updateTextures() line 134 even though we haven't
		 * loaded any textures yet.
		 */
		mRenderSurfaceView.setRenderer(mEngine);

		/* Use a surface format with an alpha channel */
		mRenderSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);

		/* Make sure the surface view is on top otherwise it a hole through anything above it in the zorder. */
		mRenderSurfaceView.setZOrderOnTop(true);

		/* We use a rendering window to save on battery */
		mRenderSurfaceView.setRenderMode(RenderSurfaceView.RENDERMODE_WHEN_DIRTY);
	}

	private void initialize() {

		/* Start normal processing */
		mReadyToRun = false;

		/* Other UI references */
		mWifiDialog = (ViewGroup) findViewById(R.id.retry_dialog);
		mEmptyDialog = (ViewGroup) findViewById(R.id.empty_dialog);

		/* Restore empty message since this could be a restart because of a theme change */
		if (Aircandi.lastScanEmpty) {
			String helpHtml = getString(Aircandi.wifiCount > 0 ? R.string.help_radar_empty : R.string.help_radar_empty_no_beacons);
			((TextView) findViewById(R.id.text_empty_message)).setText(Html.fromHtml(helpHtml));
			mEmptyDialog.setVisibility(View.VISIBLE);
		}

		/* Initialize preferences */
		updatePreferences();

		/* Setup scan runnable */
		mScanRunnable = new Runnable() {

			@Override
			public void run() {
				/*
				 * Special case to run immediately if the entity model is empty.
				 */
				if (mCandiPatchModel == null
						|| mCandiPatchModel.getCandiRootCurrent() == null
						|| mCandiPatchModel.getCandiRootCurrent().isSuperRoot()) {
					Logger.d(CandiRadar.this, "Starting scheduled autoscan");
					scanForBeacons(new ScanOptions(false, false, null));
				}
				else if (mPrefAutoscan) {
					Logger.d(CandiRadar.this, "Scheduling an autoscan in: " + mPrefAutoscanInterval + " ms");
					mHandler.postDelayed(mScanRunnable, Integer.parseInt(mPrefAutoscanInterval));
				}
			}
		};

		mScanRunnableWait = new Runnable() {

			@Override
			public void run() {
				scanForBeacons(mScanOptions);
			}
		};

		/* Debug footer */
		debugSliderShow(mPrefShowDebug);
		if (mPrefShowDebug && BuildConfig.DEBUG) {
			updateDebugInfo();
		}

		/* Check for emulator */
		if (Build.PRODUCT.equals("google_sdk") || Build.PRODUCT.equals("sdk")) {
			mUsingEmulator = true;
		}

		/* Set the current task so we can handle initial tab selection */
		Aircandi.getInstance().setCandiTask(CandiTask.RadarCandi);

		/* Connectivity monitoring */
		NetworkManager.getInstance().setContext(getApplicationContext());
		NetworkManager.getInstance().initialize();

		/* Proxibase sdk components */
		ProxiExplorer.getInstance().setContext(getApplicationContext());
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

		/* Property settings get overridden once we retrieve preferences */
		mCandiSurfaceView = (RenderSurfaceView) findViewById(R.id.view_rendersurface);
		mCandiSurfaceView.requestFocus();
		mCandiSurfaceView.setFocusableInTouchMode(true);

		/*
		 * Get setup for location snapshots. Initialize will populate location
		 * with the best of any cached location fixes. A single update will
		 * be launched if the best cached location fix doesn't meet our freshness
		 * and accuracy requirements.
		 */
		GeoLocationManager.getInstance().setContext(getApplicationContext());
		GeoLocationManager.getInstance().initialize();

		/* Beacon indicator */
		mEventScanReceived = new EventHandler() {

			@Override
			public void onEvent(Object data) {
				List<WifiScanResult> scanList = (List<WifiScanResult>) data;
				updateRadarHelp(scanList);
			}
		};

		/* Used by other activities to determine if they were auto launched after a crash */
		Aircandi.getInstance().setLaunchedFromRadar(true);

		/* Auto signin the user */
		mCommon.signinAuto();

		mReadyToRun = true;
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

	public void onHelpButtonClick(View view) {
		mCommon.showHelp(R.string.help_radar);
	}

	private void onCandiSingleTap(final CandiModel candiModel) {
		/*
		 * This event bubbles up from user interaction with CandiViews. This can
		 * get called from threads other than the main UI thread.
		 * 
		 * The initial handling of candi single tap is handled in
		 * CandiPatchPresenter.doCandiViewSingleTap as a navigate or passed
		 * up to here as a single tap.
		 * 
		 * Route to here:
		 * 
		 * CandiView.onViewSingleTap (setup when candi view pool allocates)
		 * CandiRadar.onLoadScene.onSingleTap
		 * CandiRadar.onCandiSingleTap
		 */
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				doCandiAction(candiModel);
			}
		});
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {

		if (resultCode != Activity.RESULT_CANCELED) {
			if (requestCode == CandiConstants.ACTIVITY_TEMPLATE_PICK) {
				if (intent != null && intent.getExtras() != null) {

					Bundle extras = intent.getExtras();
					final String entityType = extras.getString(getString(R.string.EXTRA_ENTITY_TYPE));
					if (entityType != null && !entityType.equals("")) {

						String parentId = null;

						if (mCandiPatchModel != null
								&& mCandiPatchModel.getCandiRootCurrent() != null
								&& !mCandiPatchModel.getCandiRootCurrent().isSuperRoot()) {
							CandiModel candiModel = (CandiModel) mCandiPatchModel.getCandiRootCurrent();
							parentId = candiModel.getEntity().id;
						}
						else if (mCommon.mEntityId != null) {
							parentId = mCommon.mEntityId;
						}

						IntentBuilder intentBuilder = new IntentBuilder(this, EntityForm.class);
						intentBuilder.setCommandType(CommandType.New);
						intentBuilder.setEntityId(null); /* Because we are making a new entity */
						intentBuilder.setParentEntityId(parentId);
						intentBuilder.setEntityType(entityType);
						Intent redirectIntent = intentBuilder.create();
						startActivity(redirectIntent);
						AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
					}
				}
			}
		}
		super.onActivityResult(requestCode, resultCode, intent);
	}

	// --------------------------------------------------------------------------------------------
	// Entity routines
	// --------------------------------------------------------------------------------------------

	private void scanForBeacons(final ScanOptions scanOptions) {
		/*
		 * Everything associated with this call is on the main thread but the UI is still responsive because most of the
		 * UI is being handled by the 2d engine thread.
		 */
		if (Aircandi.getInstance().isRadarScanInProgress()
				|| Aircandi.getInstance().isRadarUpdateInProgress()) {
			return;
		}

		/* Make sure there aren't any extra runnables waiting to run */
		mHandler.removeCallbacks(mScanRunnable);

		/* Make sure the engine is running before we do anything else */
		if (mCandiPatchPresenter == null || mEngine == null || !mEngine.isRunning()) {
			Logger.v(this, "Beacon scan: waiting for engine to start...");
			mScanOptions = scanOptions;
			mHandler.removeCallbacks(mScanRunnableWait);
			mHandler.postDelayed(mScanRunnableWait, 1000);
			return;
		}

		Aircandi.getInstance().setRadarScanInProgress(true);

		/* Check that wifi is enabled and we have a network connection */
		verifyWifi(new IWifiReadyListener() {

			@Override
			public void onWifiReady() {

				/* Time to turn on the progress indicators */
				if (scanOptions.showProgress) {
					mCommon.showProgressDialog(true, getString(scanOptions.progressMessageResId), CandiRadar.this);
				}
				else {
					mCommon.showProgressDialog(false, null);
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

									mCandiPatchPresenter.renderingActivate(CandiConstants.INTERVAL_RENDERING_BOOST);
									Aircandi.getInstance().setRadarUpdateInProgress(true);
									mCandiPatchPresenter.setIgnoreInput(true);

									EntityList<Entity> entities = ProxiExplorer.getInstance().getEntityModel().getRadarEntities();
									EntityList<Entity> entitiesCopy = (EntityList<Entity>) entities.copy();
									mCandiPatchPresenter.updateCandiData(entitiesCopy, mScanOptions.fullBuild, false);

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

								mCandiPatchPresenter.renderingActivate(CandiConstants.INTERVAL_RENDERING_DEFAULT);
								Aircandi.getInstance().setRadarUpdateInProgress(false);
								mCandiPatchPresenter.setIgnoreInput(false);
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
		mCandiPatchPresenter.renderingActivate(CandiConstants.INTERVAL_RENDERING_BOOST);
		Aircandi.getInstance().setRadarUpdateInProgress(true);
		mCandiPatchPresenter.setIgnoreInput(true);

		/* We pass a copy to presenter to provide more stability and steady state. */
		EntityList<Entity> entities = ProxiExplorer.getInstance().getEntityModel().getRadarEntities();
		EntityList<Entity> entitiesCopy = entities.copy();
		mCandiPatchPresenter.updateCandiData(entitiesCopy, false, false);

		/* Handles other wrapup tasks */
		updateComplete();

		mCandiPatchPresenter.renderingActivate(CandiConstants.INTERVAL_RENDERING_DEFAULT);
		Aircandi.getInstance().setRadarUpdateInProgress(false);
		mCandiPatchPresenter.setIgnoreInput(false);
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
				mCommon.showProgressDialog(false, null);
				mWifiDialog.setVisibility(View.GONE);
				EntityList<Entity> radarEntities = ProxiExplorer.getInstance().getEntityModel().getRadarEntities();
				((View) findViewById(R.id.button_new_candi)).setVisibility(Aircandi.wifiCount > 0 ? View.VISIBLE : View.GONE);
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
		mCandiPatchPresenter.ensureScrollBoundaries();

		/* Show aircandi tips if this is the first time the application has been run */
		if (Aircandi.firstRunApp) {
			onHelpButtonClick(null);
			Aircandi.settingsEditor.putBoolean(Preferences.SETTING_FIRST_RUN, false);
			Aircandi.settingsEditor.commit();
			Aircandi.firstRunApp = false;
		}

		/* Check for rookies and play a sound */
		if (mPrefSoundEffects && mCandiPatchPresenter.getRookieHit()) {
			mCandiAlertSound.play();
		}

		/* Capture timestamps so we can detect state changes in the entity model */
		mEntityModelRefreshDate = ProxiExplorer.getInstance().getEntityModel().getLastRefreshDate();
		mEntityModelActivityDate = ProxiExplorer.getInstance().getEntityModel().getLastActivityDate();
		mEntityModelUser = Aircandi.getInstance().getUser();

		/* Add a call to pass along analytics */
		if (BuildConfig.DEBUG) {
			updateDebugInfo();
		}

		/*
		 * Schedule the next wifi scan run if autoscan is enabled | The autoscan will pick
		 * up new beacons and changes in visibility of the entities associated with beacons
		 * that are already being tracked. This is meant to be an efficient refresh that can
		 * run continuously without a ton of data traffic. So there won't be any calls to
		 * the data service unless we discover a new beacon.
		 */
		if (mPrefAutoscan) {
			Logger.d(CandiRadar.this, "Scheduling an autoscan in: " + mPrefAutoscanInterval + " ms");
			/* Make sure something isn't already scheduled */
			mHandler.removeCallbacks(mScanRunnable);
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
						((View) findViewById(R.id.button_new_candi)).setVisibility(wifiCount > 0 ? View.VISIBLE : View.GONE);
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

		if (!mReadyToRun) {
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

	private void doCandiAction(final CandiModel candiModel) {
		if (!candiModel.isDeleted()) {
			mCandiPatchModel.setCandiModelSelected(candiModel);
			showCandiForm(candiModel);
		}
	}

	private void showCandiForm(CandiModel candiModel) {
		Entity entity = mCandiPatchModel.getCandiModelSelected().getEntity();

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
			intentBuilder.setEntityLocation(entity.getParent().location);
		}

		Intent intent = intentBuilder.create();

		startActivity(intent);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiRadarToCandiForm);
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
			buttonFrame.startAnimation(AnimUtils.fadeInMedium());
		}
		else {
			buttonFrame.setAnimation(null);
			buttonFrame.startAnimation(AnimUtils.fadeOutMedium());
		}
	}

	// --------------------------------------------------------------------------------------------
	// Connectivity routines
	// --------------------------------------------------------------------------------------------

	private void verifyWifi(final IWifiReadyListener listener) {

		if (NetworkManager.getInstance().isTethered()) {
			mEmptyDialog.setVisibility(View.GONE);
			mCommon.showProgressDialog(false, null);
			showNetworkDialog(true, getString(R.string.dialog_network_message_wifi_tethered), true);
			if (listener != null) {
				Logger.i(this, "Wifi failed: tethered");
				listener.onWifiFailed();
			}

		}
		else if (!NetworkManager.getInstance().isWifiEnabled() && !ProxiExplorer.getInstance().isUsingEmulator()) {

			/* Make sure we are displaying any background message */
			mEmptyDialog.setVisibility(View.GONE);
			mCommon.showProgressDialog(false, null);
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

			mCandiSurfaceView.setVisibility(View.GONE);
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
			mCandiSurfaceView.setVisibility(View.VISIBLE);
			mWifiDialog.setVisibility(View.GONE);
		}
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
		 * 
		 * Zoom Scaling is handled in CandiPatchPresenter.initializeScene()
		 */
		mScreenOrientation = ScreenOrientation.PORTRAIT;
		DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

		/* Adjusted for density */
		int statusBarHeight = (int) FloatMath.ceil((float) CandiConstants.ANDROID_STATUSBAR_HEIGHT * displayMetrics.density);
		int actionBarStackedHeight = (int) FloatMath.ceil((float) (CandiConstants.ANDROID_ACTIONBAR_HEIGHT * 2) * displayMetrics.density);
		int buttonBarHeight = (int) FloatMath.ceil((float) CandiConstants.ANDROID_ACTIONBAR_HEIGHT * displayMetrics.density);

		int widthPixels = displayMetrics.widthPixels;
		int heightPixels = displayMetrics.heightPixels;

		if (widthPixels > heightPixels) {
			widthPixels = displayMetrics.heightPixels;
			heightPixels = displayMetrics.widthPixels;
		}

		Camera camera = new ChaseCamera(0, 0, widthPixels, heightPixels - (actionBarStackedHeight + statusBarHeight + buttonBarHeight)) {

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

		mRadarWidth = camera.getWidth();
		mRadarHeight = camera.getHeight();

		EngineOptions options = new EngineOptions(false, mScreenOrientation, new FillResolutionPolicy(), camera);
		options.setNeedsSound(true);
		options.setWakeLockOptions(WakeLockOptions.SCREEN_ON);
		Engine engine = new Engine(options);

		return engine;
	}

	@Override
	public void onLoadResources() {
		mCandiAlertSound = SoundFactory.createSoundFromResource(mEngine.getSoundManager(), this, R.raw.notification_candi_discovered);
		mCandiAlertSound.setVolume(0.4f);
		mEngine.getTextureManager().setTextureListener(this);
	}

	@Override
	public Scene onLoadScene() {
		/*
		 * Lifecycle ordering
		 * onCreate->onStart->onResume
		 * ->onAttachedToWindow->onWindowFocusChanged
		 * ->onLoadEngine->onLoadScene->onResumeGame
		 * 
		 * CandiPatchPresenter handles scene instantiation and setup
		 */
		Logger.d(this, "Loading scene");
		mCandiPatchPresenter = new CandiPatchPresenter(this, this, mEngine, mRenderSurfaceView, mCandiPatchModel);
		mCommon.setCandiPatchPresenter(mCandiPatchPresenter);
		mCandiPatchPresenter.setRadarHeight(mRadarHeight);
		mCandiPatchPresenter.setRadarWidth(mRadarWidth);

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
	public void onTexturesLoaded(int count) {}

	@Override
	public void onTexturesReady() {
		Logger.d(this, "Textures ready");
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
	public void onAttachedToWindow() {
		Logger.d(this, "CandiRadarActivity attached to window");
		super.onAttachedToWindow();
	}

	// --------------------------------------------------------------------------------------------
	// System callbacks
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onStart() {
		/*
		 * Only called first time activity is started.
		 */
		Logger.d(this, "CandiRadarActivity starting");
		mCommon.doStart();
		super.onStart();
	}

	@Override
	protected void onRestart() {
		/*
		 * This only gets called when the activity was stopped and is now coming back. All the logic
		 * that would normally be here is in onResume() because restart wasn't getting called
		 * reliably when returning from another activity.
		 */
		Logger.i(this, "Activity CandiRadar restarting");
		super.onRestart();

		/* Make sure the right tab is active */
		mCommon.setActiveTab(0);
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
		if (!mFirstWindow.get()) {
			doResume();
		}
	}

	public void doResume() {

		/* Quick check for a new version. */
		final Boolean doUpdateCheck = (Aircandi.lastApplicationUpdateCheckDate == null
				|| (DateUtils.nowDate().getTime() - Aircandi.lastApplicationUpdateCheckDate.longValue()) > CandiConstants.INTERVAL_UPDATE_CHECK);

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				if (doUpdateCheck) {
					mCommon.showProgressDialog(true, getString(R.string.progress_scanning), CandiRadar.this);
				}
			}

			@Override
			protected Object doInBackground(Object... params) {

				ModelResult result = new ModelResult();

				if (doUpdateCheck) {
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

							runOnUiThread(new Runnable() {

								@Override
								public void run() {
									mUpdateAlertDialog = AircandiCommon.showAlertDialog(R.drawable.icon_app
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
							});
						}
					}
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
						mCommon.showProgressDialog(false, null);
						if (mUpdateAlertDialog == null || !mUpdateAlertDialog.isShowing()) {
							mUpdateAlertDialog = AircandiCommon.showAlertDialog(R.drawable.icon_app
									, getString(R.string.alert_upgrade_title)
									, getString(R.string.alert_upgrade_required_body)
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
												/* We don't continue running if user doesn't install a required update */
												Logger.d(CandiRadar.this, "Update check: user declined");
												finish();
											}
										}
									}
									, new DialogInterface.OnCancelListener() {

										@Override
										public void onCancel(DialogInterface dialog) {
											Logger.d(CandiRadar.this, "Update check: user canceled");
											if (Aircandi.applicationUpdateRequired) {
												finish();
											}
										}
									});
							mUpdateAlertDialog.setCanceledOnTouchOutside(false);
						}
						return;
					}
					else {
						/*
						 * Logic that should only run if the activity is resuming after having been paused.
						 * This used to be in restart but it wasn't getting called reliably when returning from another
						 * activity.
						 */
						if (Aircandi.fullUpdateComplete) {
							Logger.d(this, "CandiRadarActivity resuming after pause");

							/*
							 * We could be resuming because of a preference change.
							 * Restart: theme change
							 * Refresh: display extra, demo mode, global beacons
							 */
							PrefResponse prefResponse = updatePreferences();
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
							else {
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
								if (Aircandi.getInstance().getUser() == null || mEntityModelUser == null
										|| !Aircandi.getInstance().getUser().id.equals(mEntityModelUser.id)) {
									Logger.d(this, "CandiRadarActivity detected entity model change because of user change");
									invalidateOptionsMenu();
									scanForBeacons(new ScanOptions(false, true, R.string.progress_updating));
								}
								else if (ProxiExplorer.getInstance().getEntityModel().getLastRefreshDate() != null
										&& (ProxiExplorer.getInstance().getEntityModel().getLastRefreshDate().longValue() > mEntityModelRefreshDate.longValue()
										|| ProxiExplorer.getInstance().getEntityModel().getLastActivityDate().longValue() > mEntityModelActivityDate
												.longValue())) {
									Logger.d(this, "CandiRadarActivity detected entity model change");
									mCommon.showProgressDialog(false, null);
									invalidateOptionsMenu();
									updateRadarOnly();
								}
								else {
									/* We always do a check because the user might have moved */
									if (!Aircandi.returningFromDialog) {
										invalidateOptionsMenu();
										scanForBeacons(new ScanOptions(false, false, null));
									}
								}
							}
							finishResume(false);
						}
						else {
							Logger.d(this, "CandiRadarActivity resuming for first time or still needs first full update");
							finishResume(true);
							scanForBeacons(new ScanOptions(true, true, R.string.progress_scanning));
						}
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

		/*
		 * CandiPatchPresenter is created in onLoadScene which gets called after the first onResume
		 */
		if (mCandiPatchPresenter != null) {
			mCandiPatchPresenter.setIgnoreInput(false);
		}
	}

	@Override
	public void onWindowFocusChanged(final boolean hasWindowFocus) {
		/*
		 * Parent class will trigger pause or resume for the game engine based on hasWindowFocus. We control when this
		 * message get through to prevent unnecessary restarts. We block it if we lost focus becase of a pop up like a
		 * dialog or an android menu (which do not trigger this.onPause which in turns stops the engine).
		 * 
		 * Losing focus: BaseGameActivity will pause the game engine
		 * Gaining focus: BaseGameActivity will resume the game engine if currently paused
		 * 
		 * First life run: we pass through window focus change here instead of onResume because the engine isn't ready
		 * after 'first run' resume.
		 * 
		 * First life resume starts engine.
		 */
		Logger.d(this, hasWindowFocus ? "Activity has window focus" : "Activity lost window focus");
		if (mFirstWindow.get()) {
			/*
			 * This can also be called in onResume or onStop
			 */
			super.onWindowFocusChanged(hasWindowFocus);
			mFirstWindow.set(false);
			doResume();
		}
		else {
			/* Restart autoscan if enabled */
			if (hasWindowFocus) {
				if (mPrefAutoscan) {
					mHandler.removeCallbacks(mScanRunnable);
					mHandler.postDelayed(mScanRunnable, 5000);
				}
			}
			else {
				/* Make sure autoscan is stopped if we are not in the foreground */
				mHandler.removeCallbacks(mScanRunnable);
			}
		}
	}

	@Override
	protected void onPause() {
		/*
		 * - Fires when we lose focus and have been moved into the background. This will
		 * be followed by onStop if we are not visible. Does not fire if the activity window
		 * loses focus but the activity is still active.
		 * 
		 * - Parent class (BaseGameActivity) has been modified so that it doesn't do any
		 * work in it's onPause handler.
		 */
		Logger.d(this, "CandiRadarActivity paused");
		synchronized (Events.EventBus.wifiScanReceived) {
			Events.EventBus.wifiScanReceived.remove(mEventScanReceived);
		}
		mCommon.stopScanService();
		super.onPause();
		try {

			ProxiExplorer.getInstance().onPause();
			NetworkManager.getInstance().onPause();
			GeoLocationManager.getInstance().onPause();

			mCommon.doPause();
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
		Logger.d(this, "CandiRadarActivity stopped");
		mCommon.doStop();
		super.onStop(); /* Goes to Sherlock */

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
		Logger.d(this, "CandiRadarActivity destroyed");
		super.onWindowFocusChanged(false); /* To stop game engine */
		super.onDestroy();
		if (mCommon != null) {
			mCommon.doDestroy();
		}

		/* This is the only place we manually stop the analytics session. */
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
		Logger.w(this, "Memory is reported as low");
		super.onLowMemory();
	}

	// --------------------------------------------------------------------------------------------
	// Preferences routines
	// --------------------------------------------------------------------------------------------

	public PrefResponse updatePreferences() {

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

		if (mPrefShowDebug != Aircandi.settings.getBoolean(Preferences.PREF_SHOW_DEBUG, false)) {
			prefResponse = PrefResponse.Change;
			mPrefShowDebug = Aircandi.settings.getBoolean(Preferences.PREF_SHOW_DEBUG, false);
			if (BuildConfig.DEBUG) {
				debugSliderShow(mPrefShowDebug);
				if (mPrefShowDebug) {
					updateDebugInfo();
				}
			}
		}

		mPrefSoundEffects = Aircandi.settings.getBoolean(Preferences.PREF_SOUND_EFFECTS, true);

		if (!mCommon.mPrefTheme.equals(Aircandi.settings.getString(Preferences.PREF_THEME, CandiConstants.THEME_DEFAULT))) {
			prefResponse = PrefResponse.Restart;
			mCommon.mPrefTheme = Aircandi.settings.getString(Preferences.PREF_THEME, CandiConstants.THEME_DEFAULT);
		}

		return prefResponse;
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

	private void recycleBitmaps() {
		// mCommon.recycleImageViewDrawable(R.id.image_public);
		// mCommon.recycleImageViewDrawable(R.id.image_public_reflection);
	}

	//	private ServiceResponse checkForUpdate() {
	//		ModelResult result = ProxiExplorer.getInstance().getEntityModel().checkForUpdate();
	//
	//		Aircandi.applicationUpdateNeeded = false;
	//		Aircandi.applicationUpdateRequired = false;
	//		Query query = new Query("documents").filter("{\"type\":\"version\",\"target\":\"aircandi\"}");
	//
	//		ServiceRequest serviceRequest = new ServiceRequest()
	//				.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_REST)
	//				.setRequestType(RequestType.Get)
	//				.setQuery(query)
	//				.setSuppressUI(true)
	//				.setResponseFormat(ResponseFormat.Json);
	//
	//		if (!Aircandi.getInstance().getUser().isAnonymous()) {
	//			serviceRequest.setSession(Aircandi.getInstance().getUser().session);
	//		}
	//
	//		ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);
	//
	//		if (serviceResponse.responseCode == ResponseCode.Success) {
	//
	//			String jsonResponse = (String) serviceResponse.data;
	//			final VersionInfo versionInfo = (VersionInfo) ProxibaseService.convertJsonToObjectSmart(jsonResponse, ServiceDataType.VersionInfo).data;
	//			String currentVersionName = Aircandi.getVersionName(this, CandiRadar.class);
	//
	//			if (versionInfo.enabled && !currentVersionName.equals(versionInfo.versionName)) {
	//				Logger.i(CandiRadar.this, "Update check: update needed");
	//				Aircandi.applicationUpdateNeeded = true;
	//				Aircandi.applicationUpdateUri = versionInfo.updateUri != null ? versionInfo.updateUri : CandiConstants.URL_AIRCANDI_UPGRADE;
	//				if (versionInfo.updateRequired) {
	//					Aircandi.applicationUpdateRequired = true;
	//					Logger.i(CandiRadar.this, "Update check: update required");
	//				}
	//			}
	//			Aircandi.lastApplicationUpdateCheckDate = DateUtils.nowDate().getTime();
	//		}
	//		return serviceResponse;
	//	}
	//
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