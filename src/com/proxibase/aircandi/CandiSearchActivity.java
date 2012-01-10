package com.proxibase.aircandi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.microedition.khronos.opengles.GL10;

import org.anddev.andengine.audio.sound.Sound;
import org.anddev.andengine.audio.sound.SoundFactory;
import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.engine.options.EngineOptions.ScreenOrientation;
import org.anddev.andengine.engine.options.resolutionpolicy.FillResolutionPolicy;
import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.modifier.AlphaModifier;
import org.anddev.andengine.entity.modifier.MoveModifier;
import org.anddev.andengine.entity.modifier.ParallelEntityModifier;
import org.anddev.andengine.entity.modifier.ScaleModifier;
import org.anddev.andengine.entity.modifier.IEntityModifier.IEntityModifierListener;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.opengl.texture.TextureManager.TextureListener;
import org.anddev.andengine.opengl.view.RenderSurfaceView;
import org.anddev.andengine.util.modifier.IModifier;
import org.anddev.andengine.util.modifier.ease.EaseCircularIn;
import org.anddev.andengine.util.modifier.ease.EaseLinear;
import org.anddev.andengine.util.modifier.ease.EaseQuartInOut;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.amazonaws.auth.BasicAWSCredentials;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.proxibase.aircandi.Aircandi.CandiTask;
import com.proxibase.aircandi.candi.camera.ChaseCamera;
import com.proxibase.aircandi.candi.models.CandiModel;
import com.proxibase.aircandi.candi.models.CandiPatchModel;
import com.proxibase.aircandi.candi.models.IModel;
import com.proxibase.aircandi.candi.models.BaseModel.ViewState;
import com.proxibase.aircandi.candi.models.CandiModel.DisplayExtra;
import com.proxibase.aircandi.candi.presenters.CandiPatchPresenter;
import com.proxibase.aircandi.candi.presenters.CandiPatchPresenter.ICandiListener;
import com.proxibase.aircandi.candi.presenters.CandiPatchPresenter.TextureReset;
import com.proxibase.aircandi.candi.views.CandiView;
import com.proxibase.aircandi.candi.views.ViewAction;
import com.proxibase.aircandi.candi.views.ViewAction.ViewActionType;
import com.proxibase.aircandi.components.AircandiCommon;
import com.proxibase.aircandi.components.Command;
import com.proxibase.aircandi.components.Exceptions;
import com.proxibase.aircandi.components.ImageCache;
import com.proxibase.aircandi.components.ImageManager;
import com.proxibase.aircandi.components.ImageUtils;
import com.proxibase.aircandi.components.Logger;
import com.proxibase.aircandi.components.NetworkManager;
import com.proxibase.aircandi.components.ProxiExplorer;
import com.proxibase.aircandi.components.ProxiHandlerManager;
import com.proxibase.aircandi.components.Rotate3dAnimation;
import com.proxibase.aircandi.components.Tracker;
import com.proxibase.aircandi.components.VersionInfo;
import com.proxibase.aircandi.components.AircandiCommon.EventHandler;
import com.proxibase.aircandi.components.AircandiCommon.IntentBuilder;
import com.proxibase.aircandi.components.Command.CommandVerb;
import com.proxibase.aircandi.components.NetworkManager.IConnectivityListener;
import com.proxibase.aircandi.components.NetworkManager.IWifiReadyListener;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.components.ProxiExplorer.Options;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.widgets.ActionsWindow;
import com.proxibase.aircandi.widgets.WebImageView;
import com.proxibase.sdk.android.proxi.consumer.Beacon;
import com.proxibase.sdk.android.proxi.consumer.Entity;
import com.proxibase.sdk.android.proxi.consumer.User;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.Query;
import com.proxibase.sdk.android.proxi.service.ServiceRequest;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;
import com.proxibase.sdk.android.util.ProxiConstants;

/*
 * Texture Notes
 * 
 * - Textures are loaded into hardware using bitmaps (one or more texture sources).
 * 
 * - Textures are unloaded from hardware when the activity loses its window (anything that
 * switches away from this activity and triggers onPause/onStop).
 * 
 * - Textures are reloaded into hardware when the activity regains its windows. The bitmap
 * used as the texture source is used again but won't fail even if the bitmap has
 * been recycled. The andengine code recycles bitmaps by default once they have been used
 * to load a texture into hardware. The sprites just show up as invisible.
 * 
 * - To simplify the reloading of textures, we allow the engine to try and use the recyled
 * bitmap but intervene to detect that the bitmap has been recycled and pull it again from
 * the file cache. We have disabled the memory cache since the hardware is acting like our
 * memory cache. If the bitmap isn't in the file cache, we return null to the engine and
 * start the async process to fetch the bitmap and create a new BitmapTextureSource.
 * 
 * - Texture behavior: Textures get reloaded whenever the activity regains window focus.
 * This causes a pause while the work is being done so we have a workaround to smooth out
 * animations that need it. When we are regaining window focus after having displayed the
 * candi info, we block texture reloading.
 * 
 * - VM limits: I believe that unlike bitmaps allocated on the native heap
 * (Android version < 3.0), opengl textures do not count toward the VM memory limit.
 */

/*
 * Library Notes
 * 
 * - AWS: We are using the minimum libraries: core and S3. We could do the work to call
 * AWS without their libraries which should give us the biggest savings.
 * 
 * - Gson and Guava: We could reduce our size by making the libraries included with Android
 * work instead of pulling in Gson (which in turn has a dependency on Guava).
 */

/*
 * Threading Notes
 * 
 * - AsyncTasks: AsyncTask uses a static internal work queue with a hard-coded limit of 10
 * elements. Once we have 10 tasks going concurrently, task 11 causes a RejectedExecutionException.
 * 
 * ThreadPoolExecutor is a way to get more control over thread pooling but it requires Android
 * version 11/3.0 (we currently target 7/2.1 and higher).
 * 
 * AsyncTasks are hard-coded with a low priority and continue their work even if the activity
 * is paused.
 */

@SuppressWarnings("unused")
public class CandiSearchActivity extends AircandiGameActivity implements TextureListener {

	private static String				COMPONENT_NAME				= "CandiSearch";
	private Boolean						mPrefAutoscan				= false;
	private int							mPrefAutoscanInterval		= 5000;
	private boolean						mPrefDemoMode				= false;
	private boolean						mPrefGlobalBeacons			= true;
	private DisplayExtra				mPrefDisplayExtras			= DisplayExtra.None;
	private boolean						mPrefEntityFencing			= true;
	private boolean						mPrefShowDebug				= false;
	private boolean						mPrefSoundEffects			= true;

	private boolean						mFirstRun					= true;
	private Boolean						mReadyToRun					= false;
	private Boolean						mFullUpdateSuccess			= false;
	private Boolean						mScanActive					= false;
	private Handler						mMyHandler					= new Handler();
	private Boolean						mCredentialsFound			= false;
	public static BasicAWSCredentials	mAwsCredentials				= null;

	private List<Entity>				mEntities;
	private List<Entity>				mEntitiesFlat;
	private ProxiHandlerManager			mEntityHandlerManager;

	private CandiPatchModel				mCandiPatchModel;
	private CandiPatchPresenter			mCandiPatchPresenter;
	private RenderSurfaceView			mCandiSurfaceView;

	private FrameLayout					mSliderWrapperSearch;

	private ActionsWindow				mActionsWindow;
	private boolean						mCandiActivityActive		= false;
	private AnimType					mAnimTypeCandiInfo			= AnimType.RotateCandi;
	private Sound						mCandiAlertSound;
	private Rotate3dAnimation			mRotate3dAnimation;
	private boolean						mFirstTimeSliderInfoShow	= true;
	private boolean						mSliderInfoOpen				= true;
	private ScreenOrientation			mScreenOrientation			= ScreenOrientation.PORTRAIT;
	private PackageReceiver				mPackageReceiver			= new PackageReceiver();
	private boolean						mIgnoreInput				= false;
	private boolean						mUsingEmulator				= false;

	private int							mRenderMode;
	private Options						mScanOptions;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Logger.i(this, "CandiSearchActivity created");
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
		mRenderSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); /* To support transparency */

		/*
		 * Triggers a full rendering pass including loading any textures that have been queued up.
		 * TODO: We sometimes get a null exception in updateTextures() line 134 even though we haven't loaded
		 * any textures yet.
		 */
		mRenderSurfaceView.setRenderer(mEngine);

		/* Use a surface format with an alpha channel */
		mRenderSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);

		/* Make sure the surface view is on top otherwise it a hole through anything above it in the zorder. */
		mRenderSurfaceView.setZOrderOnTop(true);

		/* TODO: Rendering only when dirty is more battery efficient */
		mRenderSurfaceView.setRenderMode(mRenderMode = RenderSurfaceView.RENDERMODE_WHEN_DIRTY);
	}

	private void initialize() {

		/* Start normal processing */
		mReadyToRun = false;

		/* Debug footer */

		mPrefShowDebug = Aircandi.settings.getBoolean(Preferences.PREF_SHOW_DEBUG, false);
		mSliderWrapperSearch = (FrameLayout) findViewById(R.id.slider_wrapper_search);
		debugSliderShow(mPrefShowDebug);
		if (mPrefShowDebug) {
			updateDebugInfo();
		}

		registerForContextMenu(findViewById(R.id.group_refresh));

		/* Analytics tracker */
		GoogleAnalyticsTracker.getInstance().startNewSession(getAnalyticsId(), this);
		Tracker.trackPageView("/SearchHome");

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
		ImageManager.getInstance().getImageCache().setFileCacheOnly(true);
		ImageManager.getInstance().getImageLoader().setWebView((WebView) findViewById(R.id.webview));
		ImageManager.getInstance().setActivity(this);
		ImageManager.getInstance().setDisplayMetrics(getResources().getDisplayMetrics());

		/* Candi patch */
		mCandiPatchModel = new CandiPatchModel();
		mCandiPatchModel.setScreenWidth(ImageManager.getInstance().getDisplayMetrics().widthPixels);

		/* Proxi activities */
		mEntityHandlerManager = new ProxiHandlerManager(this);

		/* Property settings get overridden once we retrieve preferences */
		mCandiSurfaceView = (RenderSurfaceView) findViewById(R.id.view_rendersurface);
		mCandiSurfaceView.requestFocus();
		mCandiSurfaceView.setFocusableInTouchMode(true);

		/* Animation */
		mRotate3dAnimation = new Rotate3dAnimation();

		/* Check to see if we already have a signed in user */
		if (findViewById(R.id.image_user) != null && Aircandi.getInstance().getUser() != null) {
			User user = Aircandi.getInstance().getUser();
			mCommon.setUserPicture(user.imageUri, user.linkUri, (WebImageView) findViewById(R.id.image_user));
		}

		/* Memory info */
		updateDebugInfo();

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
						Logger.e(CandiSearchActivity.this, "Aws Credentials not configured correctly.");
						mCredentialsFound = false;
					}
					else {
						mAwsCredentials = new BasicAWSCredentials(properties.getProperty("accessKey"), properties.getProperty("secretKey"));
						mCredentialsFound = true;
					}
				}
				catch (Exception exception) {
					Logger.e(CandiSearchActivity.this, exception.getMessage(), exception);
					mCredentialsFound = false;
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
			mCandiPatchPresenter.renderingActivate();
			mCandiPatchPresenter.navigateModel(mCandiPatchModel.getCandiRootCurrent().getParent(), false);
		}
		else {
			super.onBackPressed();
		}
	}

	public void onTabClick(View view) {
		if (view.getTag().equals("radar")) {
			if (mCandiPatchModel != null && mCandiPatchModel.getCandiRootCurrent() != null
					&& mCandiPatchModel.getCandiRootCurrent().getParent() != null) {
				mCandiPatchPresenter.renderingActivate();
				mCandiPatchPresenter.navigateModel(mCandiPatchModel.getCandiRootCurrent().getParent(), false);
				return;
			}
		}
		mCommon.setActiveTab(view);
		if (view.getTag().equals("radar")) {
			Aircandi.getInstance().setCandiTask(CandiTask.RadarCandi);

			Intent intent = new Intent(this, CandiSearchActivity.class);
			startActivity(intent);
			overridePendingTransition(R.anim.fade_in_short, R.anim.fade_out_short);
		}
		else if (view.getTag().equals("mycandi")) {
			Aircandi.getInstance().setCandiTask(CandiTask.MyCandi);

			IntentBuilder intentBuilder = new IntentBuilder(this, CandiList.class);
			Intent intent = intentBuilder.create();
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			overridePendingTransition(R.anim.fade_in_medium, R.anim.hold);
		}
	}

	public void onRefreshClick(View view) {
		if (mScanActive) {
			Logger.v(this, "User refresh request ignored because of active scan");
			return;
		}

		/* For this activity, refresh means rescan and reload entity data from the service */
		Tracker.trackEvent("Clicks", "Button", "refresh", 0);

		if (mReadyToRun) {
			doRefresh(RefreshType.BeaconScanPlusCurrent);
		}
		updateDebugInfo();
	}

	private void onCandiSingleTap(final CandiModel candiModel) {
		/*
		 * This event bubbles up from user interaction with CandiViews. This
		 * can get called from threads other than the main UI thread.
		 */
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				/*
				 * When starting from a candi in search view we always target
				 * the first CandiInfo view in the flipper.
				 */
				mCandiPatchModel.setCandiModelSelected(candiModel);
				showCandiForm(candiModel);
			}
		});
	}

	// --------------------------------------------------------------------------------------------
	// Entity routines
	// --------------------------------------------------------------------------------------------

	private void scanForBeacons(final Options options, final boolean showProgress) {

		/*
		 * Everything associated with this call is on the main thread but the
		 * UI is still responsive because most of the UI is being handled
		 * by the 2d engine thread.
		 */

		/* Check that wifi is enabled and we have a network connection */
		mScanOptions = options;
		mScanActive = true;

		verifyWifi(new IWifiReadyListener() {

			@Override
			public void onWifiReady() {

				/* Time to turn on the progress indicators */
				if (showProgress) {
					mCommon.showProgressDialog(true, "Searching...");
					mCandiPatchPresenter.mProgressSprite.setVisible(true);
					if (!isVisibleEntity() && mCandiPatchPresenter != null) {
						mCandiPatchPresenter.renderingActivate(30000);
						//mCandiPatchPresenter.mProgressSprite.setVisible(true);
					}
					else {
						mCommon.startTitlebarProgress();
					}
				}

				/* Make sure we have a user */
				ServiceResponse serviceResponse = verifyUser();

				if (serviceResponse.responseCode != ResponseCode.Success) {
					if (serviceResponse.responseCode == ResponseCode.Unrecoverable) {
						Exceptions.Handle(serviceResponse.exception);
					}
					else {
						mCandiPatchPresenter.renderingActivate(5000);
						mCandiPatchPresenter.mProgressSprite.setVisible(false);
						mCommon.stopTitlebarProgress();
						mScanActive = false;
						return;
					}
				}

				if (mScanOptions.refreshAllBeacons && mCandiPatchPresenter != null) {
					mCandiPatchPresenter.renderingActivate(60000);
					mCandiPatchPresenter.setFullUpdateInProgress(true);
					mCandiPatchPresenter.mProgressSprite.setVisible(true);
					/*
					 * Quick check for a new version. We continue even if the network
					 * call fails.
					 */
					checkForUpdate();
				}

				BeaconScanWatcher watcher = new BeaconScanWatcher();
				watcher.start(mScanOptions);

				EventBus.beaconScanComplete = new EventHandler() {

					@Override
					public void onEvent(Object data) {

						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								/* Wrap-up */
								if (mScanOptions.refreshAllBeacons && !mFullUpdateSuccess) {
									mFullUpdateSuccess = true;
								}

								/* Add a call to pass along analytics */
								Tracker.dispatch();
								updateDebugInfo();
								mCandiPatchPresenter.setFullUpdateInProgress(false);
								mCommon.stopTitlebarProgress();
								mCommon.showProgressDialog(false, null);

								/*
								 * Schedule the next wifi scan run if autoscan is enabled
								 * |
								 * The autoscan will pick up new beacons and changes in visibility
								 * of the entities associated with beacons that are already being tracked.
								 * This is meant to be an efficient refresh that can run continuously without
								 * a ton of data traffic. So there won't be any calls to the data service
								 * unless we discover a new beacon.
								 */
								if (mPrefAutoscan) {
									mMyHandler.postDelayed(new Runnable() {

										public void run() {
											scanForBeacons(new Options(false, false), false);
										}
									}, mPrefAutoscanInterval);
								}
								mScanActive = false;
							}
						});
					}
				};
			}

			@Override
			public void onWifiFailed() {
				mCandiPatchPresenter.mProgressSprite.setVisible(false);
				mScanActive = false;
			}
		});
	}

	public class BeaconScanWatcher {

		private Options	mOptions;

		public BeaconScanWatcher() {

			EventBus.wifiScanReceived = new EventHandler() {

				@Override
				public void onEvent(Object data) {
					Logger.d(this, "Wifi scan received");
					new AsyncTask() {

						@Override
						protected Object doInBackground(Object... params) {
							ProxiExplorer.getInstance().processBeaconsFromScan(mOptions.refreshAllBeacons);
							return null;
						}

						@Override
						protected void onPostExecute(Object result) {}

					}.execute();

				}
			};

			EventBus.entitiesLoaded = new EventHandler() {

				@Override
				public void onEvent(Object data) {
					Logger.d(this, "Entities loaded from service");
					ServiceResponse serviceResponse = (ServiceResponse) data;
					if (serviceResponse.responseCode != ResponseCode.Success) {
						if (serviceResponse.responseCode == ResponseCode.Unrecoverable) {
							Exceptions.Handle(serviceResponse.exception);
						}
						else {
							mCandiPatchPresenter.mProgressSprite.setVisible(false);
							mCandiPatchPresenter.renderingActivate(5000);
							mCommon.stopTitlebarProgress();
							mScanActive = false;
							return;
						}
					}
					else {
						if (mOptions.refreshDirty) {
							serviceResponse = ProxiExplorer.getInstance().refreshDirtyEntities();
							if (serviceResponse.responseCode != ResponseCode.Success) {
								return;
							}
						}
						List<Entity> entities = ProxiExplorer.getInstance().mEntities;

						/* Check to see if we have any visible entities */
						boolean visibleEntity = false;
						for (Entity entity : entities) {
							if (!entity.isHidden) {
								visibleEntity = true;
								break;
							}
						}

						if (!visibleEntity) {
							showNewCandiDialog();
						}
						else {
							mCandiPatchPresenter.renderingActivate();
							doUpdateEntities(entities, mScanOptions.refreshAllBeacons, false);
							Logger.d(this, "Model updated with entities");
							mCandiPatchPresenter.renderingActivate(5000);

							/* Check for rookies and play a sound */
							if (mPrefSoundEffects) {
								for (CandiModel candiModel : mCandiPatchModel.getCandiModels()) {
									if (candiModel.isRookie() && candiModel.getViewStateNext().isVisible()) {
										mCandiAlertSound.play();
										break;
									}
								}
							}
						}

						EventBus.onBeaconScanComplete(null);
					}
				}
			};
		}

		public void start(Options options) {
			mOptions = options;
			new AsyncTask() {

				@Override
				protected Object doInBackground(Object... params) {
					ProxiExplorer.getInstance().scanForBeacons(mOptions);
					return null;
				}

				@Override
				protected void onPostExecute(Object result) {}

			}.execute();
		}
	}

	public static class EventBus {

		public static EventHandler	wifiScanReceived;
		public static EventHandler	entitiesLoaded;
		public static EventHandler	beaconScanComplete;

		public static void onWifiScanReceived(Object data) {
			if (wifiScanReceived != null)
				wifiScanReceived.onEvent(data);
		}

		public static void onEntitiesLoaded(Object data) {
			if (entitiesLoaded != null)
				entitiesLoaded.onEvent(data);
		}

		public static void onBeaconScanComplete(Object data) {
			if (beaconScanComplete != null)
				beaconScanComplete.onEvent(data);
		}
	}

	private void doUpdateEntities(List<Entity> freshEntities, boolean fullUpdate, boolean delayObserverUpdate) {
		/*
		 * Shallow copy so entities are by value but any object
		 * properties like beacon are by ref from the original.
		 */
		mEntities = (List<Entity>) ((ArrayList<Entity>) freshEntities).clone();

		/* Push the new and updated entities into the system */
		mCandiPatchPresenter.updateCandiData(mEntities, fullUpdate, delayObserverUpdate);
	}

	private void doEntityUpdate(Entity freshEntity, Entity staleEntity) {
		/*
		 * Replace the entity in our master collection.
		 */
		for (int i = mEntities.size() - 1; i >= 0; i--) {
			Entity entity = mEntities.get(i);
			if (entity.id == staleEntity.id) {
				if (freshEntity == null) {
					mEntities.remove(i);
				}
				else {
					mEntities.set(i, freshEntity);
				}
			}
		}

		/* Push the new and updated entities into the system */
		mCandiPatchPresenter.updateCandiData(mEntities, false, false);
	}

	private void doRefresh(RefreshType refreshType) {

		NetworkManager.getInstance().reset();
		if (!mFullUpdateSuccess) {
			Logger.i(this, "User starting first beacon scan");
			Tracker.trackEvent("Search", "Refresh", "All", 0);
			scanForBeacons(new Options(true, false), true);
		}
		else if (refreshType == RefreshType.All) {
			Logger.i(this, "User starting full beacon scan");
			Tracker.trackEvent("Search", "Refresh", "All", 0);
			scanForBeacons(new Options(true, false), true);
		}
		else if (refreshType == RefreshType.BeaconScan) {
			Logger.i(this, "User starting lightweight beacon scan");
			Tracker.trackEvent("Search", "Refresh", "BeaconScan", 0);
			scanForBeacons(new Options(false, false), true);
		}
		else if (refreshType == RefreshType.BeaconScanPlusCurrent) {

			Logger.i(this, "User starting lightweight beacon scan");
			Tracker.trackEvent("Search", "Refresh", "BeaconScanPlusCurrent", 0);
			final CandiModel candiModelFocused = mCandiPatchModel.getCandiModelFocused();

			if (candiModelFocused == null || !candiModelFocused.getViewStateCurrent().isVisible()) {
				scanForBeacons(new Options(false, false), true);
			}
			else {
				final int idOfEntityToRefresh = candiModelFocused.getEntity().id;
				Logger.i(this, "User starting current entity refresh");

				/*
				 * This will come back before we really know if the candi we want to
				 * refresh textures for is still around.
				 */
				scanForBeacons(new Options(false, true), true);

				/* Scan could have caused the current candi to go away or be hidden */
				Entity entity = ProxiExplorer.getInstance().getEntityById(idOfEntityToRefresh);
				if (entity != null && !entity.isHidden) {

					/* Refresh candi view texture */
					Logger.v(this, "Update texture: " + candiModelFocused.getTitleText());

					/* Start rendering because updates require that the update thread is running */
					synchronized (candiModelFocused.getViewActions()) {
						candiModelFocused.getViewActions().addFirst(new ViewAction(ViewActionType.UpdateTexturesForce));
					}
					candiModelFocused.setChanged();
					candiModelFocused.update();

					for (IModel childModel : candiModelFocused.getChildren()) {
						CandiModel childCandiModel = (CandiModel) childModel;
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

	private boolean isVisibleEntity() {
		if (mEntities == null || mEntities.size() == 0) {
			return false;
		}
		for (Entity entity : mEntities) {
			if (!entity.isHidden) {
				return true;
			}
		}
		return false;
	}

	private void refreshModel() {

		new AsyncTask() {

			private boolean	mRefreshNeeded	= false;

			@Override
			protected void onPreExecute() {
				mCommon.showProgressDialog(true, "Updating...");
			}

			@Override
			protected Object doInBackground(Object... params) {
				if (ProxiExplorer.getInstance().mEntitiesDeleted.size() > 0) {
					Iterator it = ProxiExplorer.getInstance().mEntitiesDeleted.entrySet().iterator();
					while (it.hasNext()) {
						Map.Entry entry = (Map.Entry) it.next();
						Entity entityDeleted = (Entity) entry.getValue();

						ProxiExplorer.getInstance().mEntitiesUpdated.remove(entityDeleted.id);
						if (ProxiExplorer.getInstance().mEntitiesInserted.remove(entityDeleted.id) == null) {
							/*
							 * Because of 'my candi', an entity could be deleted that isn't currently being
							 * tracked in the big model (beacon might not be tracked either).
							 */
							Beacon beacon = ProxiExplorer.getInstance().getBeaconById(entityDeleted.beaconId);
							if (beacon != null) {
								beacon.isDirty = true;
								mRefreshNeeded = true;
							}
						}
					}
				}

				if (ProxiExplorer.getInstance().mEntitiesInserted.size() > 0) {
					Iterator it = ProxiExplorer.getInstance().mEntitiesInserted.entrySet().iterator();
					while (it.hasNext()) {
						Map.Entry entry = (Map.Entry) it.next();
						Entity entityInserted = (Entity) entry.getValue();

						Beacon beacon = ProxiExplorer.getInstance().getBeaconById(entityInserted.beaconId);
						if (beacon != null) {
							/*
							 * We will end up with the latest version of the entity
							 * even if we also have an update stacked up in EntitiesUpdated.
							 */
							beacon.isDirty = true;
							mRefreshNeeded = true;
						}
						ProxiExplorer.getInstance().mEntitiesUpdated.remove(entityInserted.id);
					}
				}

				if (ProxiExplorer.getInstance().mEntitiesUpdated.size() > 0) {
					Iterator it = ProxiExplorer.getInstance().mEntitiesUpdated.entrySet().iterator();
					while (it.hasNext()) {
						Map.Entry entry = (Map.Entry) it.next();
						Entity entityUpdated = (Entity) entry.getValue();

						for (Entity entity : ProxiExplorer.getInstance().getEntitiesFlat()) {
							if (entity.id.equals(entityUpdated.id)) {
								entity.isDirty = true;
								mRefreshNeeded = true;

								/* Refresh the texture */
								CandiModel candiModel = mCandiPatchModel.getCandiModelForEntity(entity.id);
								synchronized (candiModel.getViewActions()) {
									candiModel.getViewActions().addFirst(new ViewAction(ViewActionType.UpdateTexturesForce));
								}
							}
						}
						ProxiExplorer.getInstance().mEntitiesUpdated.remove(entityUpdated.id);
					}
				}

				if (mRefreshNeeded) {

					ServiceResponse serviceResponse = ProxiExplorer.getInstance().refreshDirtyEntities();
					if (serviceResponse.responseCode == ResponseCode.Success) {
						List<Entity> freshEntityProxies = (List<Entity>) serviceResponse.data;
						doUpdateEntities(freshEntityProxies, false, false);
						mCandiPatchModel.getCandiModelFocused().setChanged();
					}
					return serviceResponse;
				}
				return new ServiceResponse();
			}

			@Override
			protected void onPostExecute(Object result) {
				ServiceResponse serviceResponse = (ServiceResponse) result;
				mCommon.showProgressDialog(false, null);

				if (serviceResponse.responseCode == ResponseCode.Success) {
					ProxiExplorer.getInstance().mEntitiesInserted.clear();
					ProxiExplorer.getInstance().mEntitiesUpdated.clear();
					ProxiExplorer.getInstance().mEntitiesDeleted.clear();
				}
				else if (serviceResponse.responseCode == ResponseCode.Unrecoverable) {
					Exceptions.Handle(serviceResponse.exception);
				}
				else {
					/* TODO: Should we try to pickup the changes on the next attempt at refresh */
				}
			}
		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	private void showCandiForm(CandiModel candiModel) {
		Entity entity = mCandiPatchModel.getCandiModelSelected().getEntity();

		IntentBuilder intentBuilder = new IntentBuilder(this, CandiForm.class);
		intentBuilder.setCommand(new Command(CommandVerb.View));
		intentBuilder.setEntity(entity);
		intentBuilder.setEntityType(entity.entityType);
		Intent intent = intentBuilder.create();

		startActivity(intent);
	}

	private void showCandiFormFancy(final CandiModel candiModel) {

		mCandiPatchPresenter.renderingActivate();

		if (mPrefShowDebug) {
			debugSliderShow(false);
		}

		float scaleFrom = 1.0f;
		float scaleTo = 0.5f;
		float alphaFrom = 1.0f;
		float alphaTo = 0.0f;
		final IEntity candiView = (IEntity) mCandiPatchPresenter.getCandiViewsHash().get(
				String.valueOf(mCandiPatchModel.getCandiModelSelected().getModelId()));

		scaleTo = 1.3f;
		alphaTo = 0.0f;
		//mEngine.getScene().registerEntityModifier(new AlphaModifier(duration, 1.0f, 0.0f, EaseLinear.getInstance()));

		final ViewState viewState = candiModel.getViewStateCurrent();
		float toX = candiModel.getZoneStateCurrent().getZone().getViewStateCurrent().getX() - 115;
		float toY = candiModel.getZoneStateCurrent().getZone().getViewStateCurrent().getY() - 89;

		MoveModifier move = new MoveModifier(1.0f, candiModel.getViewStateCurrent().getX(), toX,
					candiModel.getViewStateCurrent().getY(), toY, EaseQuartInOut.getInstance());

		ScaleModifier scale = new ScaleModifier(1.0f, viewState.getScale(), (80f / (250f * viewState.getScale())), EaseQuartInOut
					.getInstance());

		candiView.registerEntityModifier(new ParallelEntityModifier(new IEntityModifierListener() {

			@Override
			public void onModifierFinished(IModifier<IEntity> modifier, final IEntity entityModified) {

				mEngine.runOnUpdateThread(new Runnable() {

					@Override
					public void run() {
						candiView.clearEntityModifiers();
					}
				});

				CandiSearchActivity.this.runOnUiThread(new Runnable() {

					public void run() {

						mMyHandler.postDelayed(new Runnable() {

							@Override
							public void run() {
								mEngine.getScene().setAlpha(1.0f);
								candiView.setPosition(viewState.getX(), viewState.getY());
								candiView.setScale(viewState.getScale());
							}
						}, 500);

					}
				});

			}

			@Override
			public void onModifierStarted(IModifier<IEntity> modifier, IEntity entityModified) {
				mMyHandler.postDelayed(new Runnable() {

					@Override
					public void run() {
						Entity entity = mCandiPatchModel.getCandiModelSelected().getEntity();

						IntentBuilder intentBuilder = new IntentBuilder(mCommon.mContext, CandiForm.class);
						intentBuilder.setCommand(new Command(CommandVerb.View));
						intentBuilder.setEntity(entity);
						intentBuilder.setEntityType(entity.entityType);
						Intent intent = intentBuilder.create();

						startActivityForResult(intent, CandiConstants.ACTIVITY_CANDI_INFO);
						//overridePendingTransition(R.anim.fade_in_medium, R.anim.hold);
					}
				}, 200);
			}

		}, move, scale, new AlphaModifier(0.5f, alphaFrom, 0.0f, EaseCircularIn.getInstance())));
	}

	private void hideCandiInfo() {

		mCandiPatchPresenter.renderingActivate();
		CandiModel candiModel = mCandiPatchModel.getCandiModelSelected();
		final IEntity candiView = (IEntity) (mAnimTypeCandiInfo == AnimType.RotateCandi ? mCandiPatchPresenter.getCandiViewsHash().get(
				String.valueOf(mCandiPatchModel.getCandiModelSelected().getModelId())) : mEngine.getScene());
		final float duration = CandiConstants.DURATION_CANDIINFO_SHOW;
		final ViewState viewState = candiModel.getViewStateCurrent();
		float fromX = candiView.getX();
		float fromY = candiView.getY();
		float toX = viewState.getX();
		float toY = viewState.getY();

		MoveModifier move = new MoveModifier(duration, fromX, toX, fromY, toY, EaseQuartInOut.getInstance());
		ScaleModifier scale = new ScaleModifier(duration, candiView.getScaleX(), viewState.getScale(), EaseQuartInOut.getInstance());

		candiView.registerEntityModifier(new ParallelEntityModifier(new IEntityModifierListener() {

			@Override
			public void onModifierFinished(IModifier<IEntity> modifier, final IEntity entityModified) {

				mEngine.runOnUpdateThread(new Runnable() {

					@Override
					public void run() {
						candiView.clearEntityModifiers();
					}
				});

			}

			@Override
			public void onModifierStarted(IModifier<IEntity> modifier, IEntity entity) {}

		}, move, scale));

	}

	private void hideGLSurfaceView(float duration) {
		mEngine.getScene().registerEntityModifier(new ParallelEntityModifier(new IEntityModifierListener() {

			@Override
			public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
				CandiSearchActivity.this.runOnUiThread(new Runnable() {

					public void run() {
						mCandiSurfaceView.setVisibility(View.GONE);
					}
				});
			}

			@Override
			public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {}

		}, new AlphaModifier(duration, 1.0f, 0.0f, EaseLinear.getInstance())));
	}

	private void showGLSurfaceView(float duration) {
		mEngine.getScene().registerEntityModifier(new ParallelEntityModifier(new IEntityModifierListener() {

			@Override
			public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {}

			@Override
			public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {
				CandiSearchActivity.this.runOnUiThread(new Runnable() {

					public void run() {
						mCandiSurfaceView.setVisibility(View.VISIBLE);
					}
				});
			}

		}, new AlphaModifier(duration, 0.0f, 1.0f, EaseLinear.getInstance())));
	}

	private void debugSliderShow(final boolean visible) {

		int animationResource = visible ? R.anim.fade_in_medium : R.anim.fade_out_medium;
		Animation animation = AnimationUtils.loadAnimation(CandiSearchActivity.this, animationResource);
		animation.setFillEnabled(true);
		animation.setFillAfter(true);
		animation.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationEnd(Animation animation) {
				if (!visible) {
					mSliderWrapperSearch.setVisibility(View.GONE);
					mSliderWrapperSearch.findViewById(R.id.slider_drawer_search).setVisibility(View.GONE);
				}
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationStart(Animation animation) {
				if (visible) {
					mSliderWrapperSearch.setVisibility(View.INVISIBLE);
					mSliderWrapperSearch.findViewById(R.id.slider_drawer_search).setVisibility(View.VISIBLE);
				}
			}
		});
		mSliderWrapperSearch.startAnimation(animation);
	}

	private void infoSliderVisible(final boolean visible, final View view) {
		if (view == null) {
			return;
		}
		int animationResource = visible ? R.anim.fade_in_medium : R.anim.fade_out_short;
		Animation animation = AnimationUtils.loadAnimation(CandiSearchActivity.this, animationResource);
		animation.setFillEnabled(true);
		animation.setFillAfter(true);
		animation.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationEnd(Animation animation) {
				if (!visible) {
					view.setVisibility(View.GONE);
				}
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationStart(Animation animation) {
				if (visible) {
					view.setVisibility(View.INVISIBLE);
				}
			}
		});
		view.startAnimation(animation);
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

	private void checkEntityHandler() {
	// EntityHandler entityHandler = candiModel.getEntity().entityHandler;
	// boolean startedProxiHandler = mProxiHandlerManager.startProxiHandler(entityHandler.action,
	// candiModel.getEntity());
	// if (!startedProxiHandler) {
	// if (mProxiHandlerManager.getProxiHandlers().containsKey(entityHandler.action)) {
	// EntityHandler proxiHandlerTracked = (EntityHandler)
	// mProxiHandlerManager.getProxiHandlers().get(entityHandler.action);
	// if (!proxiHandlerTracked.isSuppressInstallPrompt()) {
	// showInstallDialog(candiModel);
	// proxiHandlerTracked.setSuppressInstallPrompt(true);
	// }
	// else {
	// // Fall back to our built-in candi viewer
	// showCandiDetailView(candiModel);
	// mProxiAppManager.startProxiHandler("com.aircandi.intent.action.SHOWEntity",
	// candiModel.getEntity());
	// }
	// }
	// else {
	// showInstallDialog(candiModel);
	// entityHandler.setSuppressInstallPrompt(true);
	// mProxiHandlerManager.getProxiHandlers().put(entityHandler.action, entityHandler);
	// }
	// }
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
										Logger.d(this, "Wifi verified");
										listener.onWifiReady();
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
				Logger.d(this, "Wifi verified");
				listener.onWifiReady();
			}
		}
	}

	private ServiceResponse verifyUser() {

		if (Aircandi.getInstance().getUser() != null) {
			return new ServiceResponse();
		}

		/* Keep user signed in */
		ServiceResponse serviceResponse = new ServiceResponse();
		String username = Aircandi.settings.getString(Preferences.PREF_USERNAME, null);
		Logger.i(this, "Signing in...");
		if (username == null) {

			username = "anonymous@3meters.com";
			Query query = new Query("Users").filter("Email eq '" + username + "'");
			serviceResponse = NetworkManager.getInstance().request(
									new ServiceRequest(ProxiConstants.URL_AIRCANDI_SERVICE_ODATA, query, RequestType.Get, ResponseFormat.Json));

			if (serviceResponse.responseCode == ResponseCode.Success) {

				String jsonResponse = (String) serviceResponse.data;
				Aircandi.getInstance().setUser(
						(User) ProxibaseService.convertJsonToObject(jsonResponse, User.class, GsonType.ProxibaseService));
				if (Aircandi.getInstance().getUser() != null) {
					Aircandi.getInstance().getUser().anonymous = true;
				}
			}
		}
		else {

			Query query = new Query("Users").filter("Email eq '" + username + "'");
			serviceResponse = NetworkManager.getInstance().request(
									new ServiceRequest(ProxiConstants.URL_AIRCANDI_SERVICE_ODATA, query, RequestType.Get, ResponseFormat.Json));

			if (serviceResponse.responseCode == ResponseCode.Success) {

				String jsonResponse = (String) serviceResponse.data;
				Aircandi.getInstance().setUser(
						(User) ProxibaseService.convertJsonToObject(jsonResponse, User.class, GsonType.ProxibaseService));
				if (Aircandi.getInstance().getUser() != null) {
					ImageUtils.showToastNotification("Signed in as " + Aircandi.getInstance().getUser().fullname, Toast.LENGTH_SHORT);
				}
				else {

					Logger.d(this, "Error resigning in: Previous user does not exist: " + username);
					username = "anonymous@3meters.com";
					query = new Query("Users").filter("Email eq '" + username + "'");
					serviceResponse = NetworkManager
								.getInstance()
								.request(new ServiceRequest(ProxiConstants.URL_AIRCANDI_SERVICE_ODATA, query, RequestType.Get, ResponseFormat.Json));

					if (serviceResponse.responseCode == ResponseCode.Success) {

						jsonResponse = (String) serviceResponse.data;
						Aircandi.getInstance().setUser(
								(User) ProxibaseService.convertJsonToObject(jsonResponse, User.class, GsonType.ProxibaseService));
						if (Aircandi.getInstance().getUser() != null) {
							Aircandi.getInstance().getUser().anonymous = true;
							Aircandi.settingsEditor.putString(Preferences.PREF_USERNAME, null);
							Aircandi.settingsEditor.putString(Preferences.PREF_PASSWORD, null);
							Aircandi.settingsEditor.commit();
						}
					}
				}
			}
		}

		if (serviceResponse.responseCode == ResponseCode.Success) {
			if (findViewById(R.id.image_user) != null) {
				User user = Aircandi.getInstance().getUser();
				mCommon.setUserPicture(user.imageUri, user.linkUri, (WebImageView) findViewById(R.id.image_user));
			}
		}
		return serviceResponse;
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
						Logger.i(CandiSearchActivity.this, "Enable wifi from activity");
						NetworkManager.getInstance().enableWifi(true);
					}
					else {
						Logger.i(CandiSearchActivity.this, "Disable wifi from activity");
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

	private void showInstallDialog(final CandiModel candi) {

		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				final Dialog dialog = new Dialog(CandiSearchActivity.this, R.style.aircandi_theme_dialog);
				final RelativeLayout installDialog = (RelativeLayout) getLayoutInflater().inflate(R.layout.dialog_install, null);
				dialog.setContentView(installDialog, new FrameLayout.LayoutParams(400, 300, Gravity.CENTER));
				dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
				dialog.getWindow().setBackgroundDrawable(getResources().getDrawable(R.drawable.dialog_bg));

				((Button) installDialog.findViewById(R.id.btn_install_ok)).setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						Intent goToMarket = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(candi.getEntity().entityHandler.code));
						startActivityForResult(goToMarket, CandiConstants.ACTIVITY_MARKET);
						dialog.dismiss();
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

	private void showNewCandiDialog() {

	//		runOnUiThread(new Runnable() {
	//
	//			@Override
	//			public void run() {
	//
	//				final CharSequence[] items = {
	//												getResources().getString(R.string.dialog_new_gallery),
	//												getResources().getString(R.string.dialog_new_topic),
	//												getResources().getString(R.string.dialog_new_web) };
	//				AlertDialog.Builder builder = new AlertDialog.Builder(CandiSearchActivity.this);
	//				builder.setTitle(getResources().getString(R.string.dialog_new_message));
	//				builder.setCancelable(true);
	//				builder.setNegativeButton(getResources().getString(R.string.dialog_new_negative), new DialogInterface.OnClickListener() {
	//
	//					@Override
	//					public void onClick(DialogInterface dialog, int which) {}
	//				});
	//				builder.setItems(items, new DialogInterface.OnClickListener() {
	//
	//					public void onClick(final DialogInterface dialog, int item) {
	//						final Command command = new Command();
	//						command.verb = "new";
	//						String message = null;
	//						if (item == 0) {
	//							message = getString(R.string.dialog_new_gallery_signin);
	//							mUserSignedInRunnable = new Runnable() {
	//
	//								@Override
	//								public void run() {
	//									Logger.i(CandiSearchActivity.this, "Starting Gallery activity");
	//									Intent intent = AircandiManager.buildIntent(mContext, null, 0, false, null, command, CandiTask.None,
	//											ProxiExplorer
	//													.getInstance()
	//													.getStrongestBeacon().id, mUser,
	//											EntityForm.class);
	//									startActivityForResult(intent, CandiConstants.ACTIVITY_ENTITY_HANDLER);
	//									dialog.dismiss();
	//									mUserSignedInRunnable = null;
	//								}
	//							};
	//						}
	//						else if (item == 1) {
	//							message = getString(R.string.dialog_new_topic_signin);
	//							mUserSignedInRunnable = new Runnable() {
	//
	//								@Override
	//								public void run() {
	//									Logger.i(CandiSearchActivity.this, "Starting Topic activity");
	//									Intent intent = AircandiManager.buildIntent(mContext, null, 0, false, null, command, CandiTask.None,
	//											ProxiExplorer
	//													.getInstance()
	//													.getStrongestBeacon().id, mUser,
	//											EntityForm.class);
	//									startActivityForResult(intent, CandiConstants.ACTIVITY_ENTITY_HANDLER);
	//									dialog.dismiss();
	//									mUserSignedInRunnable = null;
	//								}
	//							};
	//						}
	//						if (item == 2) {
	//							message = getString(R.string.dialog_new_web_signin);
	//							mUserSignedInRunnable = new Runnable() {
	//
	//								@Override
	//								public void run() {
	//									Logger.i(CandiSearchActivity.this, "Starting Web activity");
	//									Intent intent = AircandiManager.buildIntent(mContext, null, 0, false, null, command, CandiTask.None,
	//											ProxiExplorer
	//													.getInstance()
	//													.getStrongestBeacon().id, mUser,
	//											EntityForm.class);
	//									startActivityForResult(intent, CandiConstants.ACTIVITY_ENTITY_HANDLER);
	//									dialog.dismiss();
	//									mUserSignedInRunnable = null;
	//								}
	//							};
	//						}
	//						if (mUser != null && mUser.anonymous) {
	//							Intent intent = AircandiManager.buildIntent(mContext, null, 0, false, null, new Command("edit"), CandiTask.None, null,
	//									null,
	//									SignInForm.class);
	//							intent.putExtra(getString(R.string.EXTRA_MESSAGE), message);
	//							startActivityForResult(intent, CandiConstants.ACTIVITY_SIGNIN);
	//
	//							//							startActivityForResult(new Intent(CandiSearchActivity.this, SignInForm.class).putExtra(getString(R.string.EXTRA_MESSAGE),
	//							//									message), CandiConstants.ACTIVITY_SIGNIN);
	//						}
	//						else {
	//							mMyHandler.post(mUserSignedInRunnable);
	//						}
	//					}
	//				});
	//				AlertDialog alert = builder.create();
	//				alert.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
	//				alert.show();
	//			}
	//		});
	}

	// --------------------------------------------------------------------------------------------
	// Graphic engine routines
	// --------------------------------------------------------------------------------------------

	@Override
	public Engine onLoadEngine() {

		Logger.d(this, "Initializing animation engine");

		int rotation = getWindowManager().getDefaultDisplay().getOrientation();

		/*
		 * For now, we only support portait orientation. Supporting orientation
		 * changes will likely require some fancy scaling transformation since the
		 * game engine locks on the supplied orientation.
		 */
		mScreenOrientation = ScreenOrientation.PORTRAIT;
		DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

		/* Adjusted for density */
		int statusBarHeight = (int) Math.ceil(CandiConstants.ANDROID_STATUSBAR_HEIGHT * displayMetrics.density);
		int titleBarHeight = (int) Math.ceil(CandiConstants.CANDI_TITLEBAR_HEIGHT * displayMetrics.density);
		int tabBarHeight = (int) Math.ceil(CandiConstants.CANDI_TABBAR_HEIGHT * displayMetrics.density);
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
		 * Called after Create, Resume->LoadEngine.
		 * CandiPatchPresenter handles scene instantiation and setup
		 */
		Logger.d(this, "Loading scene");
		mCandiPatchPresenter = new CandiPatchPresenter(this, this, mEngine, mRenderSurfaceView, mCandiPatchModel);
		Scene scene = mCandiPatchPresenter.initializeScene();

		mCandiPatchPresenter.mDisplayExtras = mPrefDisplayExtras;
		mCandiPatchModel.addObserver(mCandiPatchPresenter);
		mCandiPatchPresenter.setCandiListener(new ICandiListener() {

			@Override
			public void onSelected(IModel candi) {}

			@Override
			public void onSingleTap(CandiModel candi) {
				CandiSearchActivity.this.onCandiSingleTap(candi);
			}
		});

		return scene;
	}

	@Override
	public void onLoadComplete() {}

	@Override
	public void onResumeGame() {
		/*
		 * This gets called anytime the game surface gains window focus. The game
		 * engine acquries the wake lock, restarts the engine, resumes the GLSurfaceView.
		 * The engine reloads textures.
		 */
		Logger.d(this, "Starting animation engine");
		if (mReadyToRun) {
			PrefResponse prefResponse = loadPreferences();
			loadPreferencesProxiExplorer();

			if (mFirstRun || prefResponse == PrefResponse.Refresh) {
				if (mFirstRun) {
					Logger.i(this, "Starting first run full beacon scan");
				}
				else {
					Logger.i(this, "Starting full beacon scan because of preference change");
				}

				scanForBeacons(new Options(mFirstRun, false), true);
				mFirstRun = false;
			}
			else if (prefResponse == PrefResponse.Restart) {
				restartFirstActivity();
			}
		}
	}

	@Override
	public void onPauseGame() {
		/*
		 * This gets called anytime the game surface loses window focus is
		 * called on the super class. The game engine releases the wake lock, stops the engine, pauses the
		 * GLSurfaceView.
		 */
		Logger.d(this, "Pausing animation engine");
	}

	@Override
	public void onUnloadResources() {
		super.onUnloadResources();
	}

	@Override
	public void onTexturesLoaded(int count) {
		Logger.v("Andengine", "Textures loaded: " + String.valueOf(count));
	}

	@Override
	public void onTexturesReady() {
		Logger.i(this, "Textures ready");
	}

	@Override
	protected int getLayoutID() {
		return R.layout.candi_search;
	}

	@Override
	protected int getRenderSurfaceViewID() {
		return R.id.view_rendersurface;
	}

	@Override
	public void onWindowFocusChanged(final boolean hasWindowFocus) {
		/*
		 * Parent class will trigger pause or resume for the game engine
		 * based on hasWindowFocus.
		 * 
		 * We control when this message get through to prevent unnecessary
		 * restarts. We block it if we lost focus becase of a pop up like a
		 * dialog or an android menu (which do not trigger this.onPause which
		 * in turns stops the engine).
		 */

		/*
		 * Losing focus: BaseGameActivity will pause the game engine
		 * Gaining focus: BaseGameActivity will resume the game engine if currently paused
		 * 
		 * First life run we pass through window focus change here instead of onResume
		 * because the engine isn't ready after first run resume. First life resume
		 * starts engine.
		 */
		Logger.d(this, hasWindowFocus ? "Activity has window focus" : "Activity lost window focus");
		if (mFirstRun) {
			super.onWindowFocusChanged(hasWindowFocus);
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
		Logger.i(this, "CandiSearchActivity starting");
		super.onStart();
	}

	@Override
	protected void onRestart() {
		/*
		 * This only gets called when the activity was stopped and
		 * is now coming back.
		 */
		Logger.i(this, "CandiSearchActivity restarting");
		super.onRestart();

		/* We control window focus messages that trigger the engine from here. */
		super.onWindowFocusChanged(true);

		/* We run a fake modifier so we can trigger texture loading */
		mEngine.getScene().registerEntityModifier(new AlphaModifier(0, 0.0f, 1.0f, new IEntityModifierListener() {

			@Override
			public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
				onTexturesReady();
			}

			@Override
			public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {
				mCandiPatchPresenter.renderingActivate();
			}
		}, EaseLinear.getInstance()));

		/* User or user picture could have changed in another activity */
		if (findViewById(R.id.image_user) != null && Aircandi.getInstance().getUser() != null) {
			User user = Aircandi.getInstance().getUser();
			mCommon.setUserPicture(user.imageUri, user.linkUri, (WebImageView) findViewById(R.id.image_user));
		}

		/* Entities could have been inserted, updated or deleted by another activity so refresh our model */
		refreshModel();
	}

	@Override
	protected void onResume() {
		Logger.i(this, "CandiSearchActivity resumed");
		super.onResume();
		/*
		 * OnResume gets called after OnCreate (always) and whenever the activity is being brought back to the
		 * foreground. Not guaranteed but is usually called just before the activity receives focus.
		 * 
		 * Because code in OnCreate could have determined that we aren't ready to roll, isReadyToRun is used to indicate
		 * that prep work is complete.
		 * 
		 * This is also called when the user jumps out and back from setting preferences
		 * so we need to refresh the places where they get used.
		 * 
		 * Game engine is started/restarted in BaseGameActivity class if we currently have the window focus.
		 */

		ProxiExplorer.getInstance().onResume();
		NetworkManager.getInstance().onResume();
		mIgnoreInput = false;

		/*
		 * CandiPatchPresenter is created in onLoadScene which gets called
		 * after the first onResume
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
		Logger.i(this, "CandiSearchActivity paused");
		super.onPause();
		/*
		 * Fired when we lose focus and have been moved into the background.
		 * This will be followed by onStop if we are not visible..
		 */
		/*
		 * Calling onPause on super will cause the engine to pause if it hasn't already been
		 * paused because of losing window focus. This does not get called if the activity window
		 * loses focus but the activity is still active.
		 */
		try {

			unregisterReceiver(mPackageReceiver);

			ProxiExplorer.getInstance().onPause();
			NetworkManager.getInstance().onPause();

			mCommon.stopTitlebarProgress();
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
		Logger.i(this, "CandiSearchActivity stopped");
		super.onStop();

		/* We control window focus messages that trigger the engine from here. */
		super.onWindowFocusChanged(false);

		if (CandiConstants.DEBUG_TRACE) {
			Debug.stopMethodTracing();
		}

		/* Start thread to check and manage the file cache. */
		ImageManager.getInstance().getImageLoader().getImageCache().cleanCacheAsync(getApplicationContext());
	}

	@Override
	protected void onDestroy() {
		Logger.i(this, "CandiSearchActivity destroyed");
		super.onDestroy();

		Tracker.stopSession();

		/* Don't count on this always getting called when this activity is killed */
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
		Logger.i(this, "CandiSearchActivity memory is low");
		super.onLowMemory();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		/*
		 * Called before onResume. If we are returning from the market app, we
		 * get a zero result code whether the user decided to start an install
		 * or not.
		 */
		Logger.i(this, "Activity result returned to CandiSearchActivity: " + String.valueOf(requestCode));
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		/*
		 * TODO: Not getting called because setRequestedOrientation() is beinging
		 * called in BaseGameActivity.
		 */
		Logger.d(this, "onConfigurationChanged called: " + newConfig.orientation);

		boolean landscape = false;
		Display getOrient = getWindowManager().getDefaultDisplay();
		if (getOrient.getOrientation() == Surface.ROTATION_90 || getOrient.getOrientation() == Surface.ROTATION_270)
			landscape = true;

		super.onConfigurationChanged(newConfig);
	}

	// --------------------------------------------------------------------------------------------
	// Preferences routines
	// --------------------------------------------------------------------------------------------

	private void restartFirstActivity() {
		Intent intent = this.getIntent();
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
		finish();
	}

	private PrefResponse loadPreferences() {

		PrefResponse prefResponse = PrefResponse.None;

		if (mPrefAutoscan != Aircandi.settings.getBoolean(Preferences.PREF_AUTOSCAN, false)) {
			prefResponse = PrefResponse.Refresh;
			mPrefAutoscan = Aircandi.settings.getBoolean(Preferences.PREF_AUTOSCAN, false);
		}
		if (mPrefDisplayExtras != DisplayExtra.valueOf(Aircandi.settings.getString(Preferences.PREF_DISPLAY_EXTRAS, "None"))) {
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
			mPrefShowDebug = Aircandi.settings.getBoolean(Preferences.PREF_SHOW_DEBUG, false);
			debugSliderShow(mPrefShowDebug);
			if (mPrefShowDebug) {
				updateDebugInfo();
			}
		}

		mPrefSoundEffects = Aircandi.settings.getBoolean(Preferences.PREF_SOUND_EFFECTS, true);

		if (mCandiPatchPresenter != null) {
			mCandiPatchPresenter.mDisplayExtras = mPrefDisplayExtras;
		}

		if (!mCommon.mPrefTheme.equals(Aircandi.settings.getString(Preferences.PREF_THEME, "aircandi_theme_midnight"))) {
			prefResponse = PrefResponse.Restart;
			//			mPrefTheme = Aircandi.settings.getString(Preferences.PREF_THEME, "aircandi_theme_midnight");
			//			int themeResourceId = this.getResources().getIdentifier(mPrefTheme, "style", "com.proxibase.aircandi");
			//			this.setTheme(themeResourceId);
		}
		return prefResponse;
	}

	public enum PrefResponse {
		None, Refresh, Restart
	}

	private void loadPreferencesProxiExplorer() {
		ProxiExplorer.getInstance().setPrefEntityFencing(mPrefEntityFencing);
		ProxiExplorer.getInstance().setPrefDemoMode(mPrefDemoMode);
		ProxiExplorer.getInstance().setPrefGlobalBeacons(mPrefGlobalBeacons);
	}

	// --------------------------------------------------------------------------------------------
	// Menu routines (refresh commands)
	// --------------------------------------------------------------------------------------------

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);
		menu.setHeaderTitle(getString(R.string.menu_refresh_title));
		menu.add(0, RefreshType.BeaconScanPlusCurrent.ordinal(), 1, getString(R.string.menu_refresh_beacon_plus_current));
		menu.add(0, RefreshType.All.ordinal(), 2, getString(R.string.menu_refresh_all));
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (item.getItemId() == RefreshType.BeaconScanPlusCurrent.ordinal()) {
			doRefresh(RefreshType.BeaconScanPlusCurrent);
		}
		else if (item.getItemId() == RefreshType.All.ordinal()) {
			mFullUpdateSuccess = false;
			doRefresh(RefreshType.All);
		}
		else {
			return false;
		}
		return true;
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		/* Hide the sign out option if we don't have a current session */
		MenuItem itemOut = menu.findItem(R.id.signinout);
		MenuItem itemProfile = menu.findItem(R.id.profile);
		if (Aircandi.getInstance().getUser() != null && !Aircandi.getInstance().getUser().anonymous) {
			itemOut.setTitle("Sign Out");
			itemProfile.setVisible(true);
		}
		else {
			itemOut.setTitle("Sign In");
			itemProfile.setVisible(false);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.settings :
				startActivityForResult(new Intent(this, Preferences.class), 0);
				return (true);
			case R.id.profile :
				mCommon.doProfileClick(null);
				return (true);
			case R.id.signinout :
				if (Aircandi.getInstance().getUser() != null && !Aircandi.getInstance().getUser().anonymous) {
					mCommon.showProgressDialog(true, "Signing out...");
					Query query = new Query("Users").filter("Email eq 'anonymous@3meters.com'");

					ServiceResponse serviceResponse = NetworkManager.getInstance().request(
							new ServiceRequest(ProxiConstants.URL_AIRCANDI_SERVICE_ODATA, query, RequestType.Get, ResponseFormat.Json));

					if (serviceResponse.responseCode != ResponseCode.Success) {
						return true;
					}

					String jsonResponse = (String) serviceResponse.data;

					Aircandi.getInstance().setUser(
							(User) ProxibaseService.convertJsonToObject(jsonResponse, User.class, GsonType.ProxibaseService));
					Aircandi.getInstance().getUser().anonymous = true;

					Aircandi.settingsEditor.putString(Preferences.PREF_USERNAME, null);
					Aircandi.settingsEditor.putString(Preferences.PREF_PASSWORD, null);
					Aircandi.settingsEditor.commit();

					if (findViewById(R.id.image_user) != null) {
						User user = Aircandi.getInstance().getUser();
						mCommon.setUserPicture(user.imageUri, user.linkUri, (WebImageView) findViewById(R.id.image_user));
					}
					mCommon.showProgressDialog(false, null);
					Toast.makeText(this, "Signed out.", Toast.LENGTH_SHORT).show();
				}
				else {
					startActivityForResult(new Intent(this, SignInForm.class), CandiConstants.ACTIVITY_SIGNIN);
				}
				return (true);
			default :
				return (super.onOptionsItemSelected(item));
		}
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	private void recycleBitmaps() {
		mCommon.recycleImageViewDrawable(R.id.image_public);
		mCommon.recycleImageViewDrawable(R.id.image_public_reflection);
	}

	private void checkForUpdate() {

		Query query = new Query("Versions").filter("Target eq 'aircandi'");
		ServiceRequest serviceRequest = new ServiceRequest(ProxiConstants.URL_AIRCANDI_SERVICE_ODATA, query, RequestType.Get, ResponseFormat.Json);
		serviceRequest.setSuppressUI(true);
		ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (serviceResponse.responseCode != ResponseCode.Success) {
			return;
		}

		String jsonResponse = (String) serviceResponse.data;
		VersionInfo versionInfo = (VersionInfo) ProxibaseService.convertJsonToObject(jsonResponse, VersionInfo.class, GsonType.ProxibaseService);
		String currentVersionName = Aircandi.getVersionName(this, CandiSearchActivity.class);

		if (!currentVersionName.equals(versionInfo.versionName)) {

			AircandiCommon.showAlertDialog(R.drawable.icon_app, "New Aircandi version",
					"A newer version of Aircandi is available. Please download and install as soon possible.", this, new
					DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
						}
					});

		}
	}

	private void updateDebugInfo() {

		if (mPrefShowDebug) {

			ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
			Debug.MemoryInfo memoryInfo = new Debug.MemoryInfo();
			Debug.getMemoryInfo(memoryInfo);

			((RelativeLayout) findViewById(R.id.search_slider_content)).removeAllViews();
			final TableLayout table = new TableLayout(this);
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
			((TextView) tableRow.findViewById(R.id.text_value)).setText(String.format("%2.2f MB", (float) ((float) memoryInfo.getTotalPss() / 1024)));
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

			((RelativeLayout) findViewById(R.id.search_slider_content)).addView(table);
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

	private class Stopwatch {

		{
			Debug.startAllocCounting();
		}
		long	start	= System.nanoTime();

		void stop() {
			long elapsed = (System.nanoTime() - start) / 1000;
			Debug.stopAllocCounting();
			Logger.i(CandiSearchActivity.this, "CandiSearchActivity: " + elapsed + "us, "
																+ Debug.getThreadAllocCount() + " allocations, "
																+ Debug.getThreadAllocSize() + " bytes");
		}
	}

	public enum AnimType {
		Fade,
		CrossFade,
		CrossFadeFlipper,
		RotateScene,
		RotateCandi,
		Zoom
	}

	public enum PagerView {
		CandiInfo,
		CandiList,
		CandiInfoChild
	}

	public enum Theme {
		Blueray,
		Midnight,
		Serene,
		Smolder,
		Snow
	}

	public enum RefreshType {
		BeaconScan,
		BeaconScanPlusCurrent,
		All
	}

}