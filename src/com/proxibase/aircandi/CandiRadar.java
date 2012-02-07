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
import org.anddev.andengine.engine.options.WakeLockOptions;
import org.anddev.andengine.engine.options.EngineOptions.ScreenOrientation;
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
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.amazonaws.auth.BasicAWSCredentials;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.proxibase.aircandi.Aircandi.CandiTask;
import com.proxibase.aircandi.Preferences.PrefResponse;
import com.proxibase.aircandi.candi.camera.ChaseCamera;
import com.proxibase.aircandi.candi.models.CandiModel;
import com.proxibase.aircandi.candi.models.CandiPatchModel;
import com.proxibase.aircandi.candi.models.IModel;
import com.proxibase.aircandi.candi.models.CandiModel.DisplayExtra;
import com.proxibase.aircandi.candi.models.CandiPatchModel.Navigation;
import com.proxibase.aircandi.candi.presenters.CandiPatchPresenter;
import com.proxibase.aircandi.candi.presenters.CandiPatchPresenter.ICandiListener;
import com.proxibase.aircandi.candi.presenters.CandiPatchPresenter.TextureReset;
import com.proxibase.aircandi.candi.views.ViewAction;
import com.proxibase.aircandi.candi.views.ViewAction.ViewActionType;
import com.proxibase.aircandi.components.AircandiCommon;
import com.proxibase.aircandi.components.Command;
import com.proxibase.aircandi.components.DateUtils;
import com.proxibase.aircandi.components.Exceptions;
import com.proxibase.aircandi.components.ImageCache;
import com.proxibase.aircandi.components.ImageManager;
import com.proxibase.aircandi.components.ImageUtils;
import com.proxibase.aircandi.components.IntentBuilder;
import com.proxibase.aircandi.components.Logger;
import com.proxibase.aircandi.components.NetworkManager;
import com.proxibase.aircandi.components.ProxiExplorer;
import com.proxibase.aircandi.components.ProxiHandlerManager;
import com.proxibase.aircandi.components.Rotate3dAnimation;
import com.proxibase.aircandi.components.Tracker;
import com.proxibase.aircandi.components.VersionInfo;
import com.proxibase.aircandi.components.AircandiCommon.EventHandler;
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

public class CandiRadar extends AircandiGameActivity implements TextureListener {

	private boolean						mFirstRun				= true;
	private boolean						mPaused					= false;
	private Boolean						mReadyToRun				= false;
	private Boolean						mFullUpdateSuccess		= false;
	private Boolean						mScanActive				= false;
	private Handler						mMyHandler				= new Handler();
	public static BasicAWSCredentials	mAwsCredentials			= null;

	public Boolean						mPrefAutoscan			= false;
	public String						mPrefAutoscanInterval	= "5000";
	public boolean						mPrefDemoMode			= false;
	public boolean						mPrefGlobalBeacons		= true;
	public DisplayExtra					mPrefDisplayExtras		= DisplayExtra.None;
	public boolean						mPrefEntityFencing		= true;
	public boolean						mPrefShowDebug			= false;
	public boolean						mPrefSoundEffects		= true;

	private List<Entity>				mEntities;
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
					scanForBeacons(new Options(false, false), false);
				}
				else if (mPrefAutoscan) {
					Logger.d(this, "Scheduling an autoscan in: " + mPrefAutoscanInterval + " ms");
					mMyHandler.postDelayed(mScanRunnable, Integer.parseInt(mPrefAutoscanInterval));
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

		new Rotate3dAnimation();

		/* Check to see if we already have a signed in user */
		if (findViewById(R.id.image_user) != null && Aircandi.getInstance().getUser() != null) {
			User user = Aircandi.getInstance().getUser();
			mCommon.setUserPicture(user.imageUri, user.linkUri, (WebImageView) findViewById(R.id.image_user));
		}

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
			mCandiPatchPresenter.renderingActivate();
			mCandiPatchPresenter.navigateModel(mCandiPatchModel.getCandiRootCurrent().getParent(), false, false, Navigation.Up);
		}
		else {
			super.onBackPressed();
		}
	}

	public void onHomeClick(View view) {
		if (mEngine != null && mEngine.getTextureManager() != null) {
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
		}
		mCommon.setActiveTab(view);
		if (view.getTag().equals("radar")) {
			Aircandi.getInstance().setCandiTask(CandiTask.RadarCandi);
			Intent intent = new Intent(this, CandiRadar.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			startActivity(intent);
			overridePendingTransition(R.anim.fade_in_short, R.anim.fade_out_short);
		}
		else if (view.getTag().equals("mycandi")) {
			Aircandi.getInstance().setCandiTask(CandiTask.MyCandi);

			IntentBuilder intentBuilder = new IntentBuilder(this, CandiList.class);
			Intent intent = intentBuilder.create();
			startActivity(intent);
			overridePendingTransition(R.anim.fade_in_medium, R.anim.hold);
		}
		else if (view.getTag().equals("map")) {
			Aircandi.getInstance().setCandiTask(CandiTask.Map);

			IntentBuilder intentBuilder = new IntentBuilder(this, CandiMap.class);
			Intent intent = intentBuilder.create();
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
			//			doRefresh(RefreshType.BeaconScanPlusCurrent);
		}
		updateDebugInfo();
	}

	public void onNewCandiClick(View view) {
		showDialog(CandiConstants.DIALOG_NEW_CANDI_ID);
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
				if (!candiModel.isDeleted()) {
					showCandiForm(candiModel);
				}
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

		/* Make sure there aren't any extra runnables waiting to run */
		mMyHandler.removeCallbacks(mScanRunnable);

		/* Check that wifi is enabled and we have a network connection */
		mScanOptions = options;
		mScanActive = true;

		verifyWifi(new IWifiReadyListener() {

			@Override
			public void onWifiReady() {

				/* Time to turn on the progress indicators */
				if (showProgress) {
					mCommon.showProgressDialog(true, "Searching...");
					if (!isVisibleEntity() && mCandiPatchPresenter != null) {
						mCandiPatchPresenter.renderingActivate(30000);
					}
					else {
						mCommon.startTitlebarProgress();
					}
				}

				/* Make sure we have a user */
				ServiceResponse serviceResponse = verifyUser();

				if (serviceResponse.responseCode == ResponseCode.Success) {

					if (mScanOptions.refreshAllBeacons && mCandiPatchPresenter != null) {
						mCandiPatchPresenter.renderingActivate(60000);
						mCandiPatchPresenter.setFullUpdateInProgress(true);
						/*
						 * Quick check for a new version. We continue even if the network
						 * call fails.
						 */
						if (mFirstRun) {
							checkForUpdate();
						}
					}

					/*
					 * We track the progress of a full scan because it effects
					 * lots of async processes like candi view management.
					 */
					Aircandi.getInstance().setRebuildingDataModel(options.refreshAllBeacons);
					BeaconScanWatcher watcher = new BeaconScanWatcher();
					watcher.start(mScanOptions);

					EventBus.beaconScanComplete = new EventHandler() {

						@Override
						public void onEvent(Object data) {

							final ServiceResponse serviceResponse = (ServiceResponse) data;

							runOnUiThread(new Runnable() {

								@Override
								public void run() {

									if (serviceResponse.responseCode == ResponseCode.Success) {

										/* Wrap-up */
										if (mFirstRun) {
											mFirstRun = false;
										}

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
											Logger.d(this, "Scheduling an autoscan in: " + mPrefAutoscanInterval + " ms");
											mMyHandler.postDelayed(mScanRunnable, Integer.parseInt(mPrefAutoscanInterval));
										}
										mScanActive = false;
									}
								}
							});
						}
					};
				}
				else {
					mCandiPatchPresenter.setFullUpdateInProgress(false);
					mCommon.stopTitlebarProgress();
					mCommon.showProgressDialog(false, null);
					mScanActive = false;
				}
			}

			@Override
			public void onWifiFailed() {
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

					if (serviceResponse.responseCode == ResponseCode.Success) {

						/*
						 * In case the user navigated in while we were doing an autoscan refresh, skip
						 * the data processing, wrap this up, and schedule another scan. Auto scheduled
						 * scans will keep getting skipped and rescheduled as long as the user is
						 * drilled in.
						 */

						if (!mPrefAutoscan || (mCandiPatchModel == null
												|| mCandiPatchModel.getCandiRootCurrent() == null
												|| mCandiPatchModel.getCandiRootCurrent().isSuperRoot())) {

							if (mOptions.refreshDirty) {
								serviceResponse = ProxiExplorer.getInstance().refreshDirtyEntities();
							}

							if (serviceResponse.responseCode == ResponseCode.Success) {

								List<Entity> entities = ProxiExplorer.getInstance().mEntities;
								/*
								 * Set discovery time. Our goal is to base it on when the
								 * user first sees it. So found but hidden doesn't count.
								 * Rookie gets set to false on second time around for an entity.
								 */
								boolean rookieHit = false;
								for (Entity entity : entities) {
									/*
									 * We keep bumping the date up until the entity is finally
									 * visible.
									 */
									if (entity.rookie) {
										entity.discoveryTime = DateUtils.nowDate();
										if (!entity.hidden) {
											entity.rookie = false;
											rookieHit = true;
										}
									}
									for (Entity childEntity : entity.children) {
										/*
										 * We keep bumping the date up until the entity is finally
										 * visible.
										 */
										if (childEntity.rookie) {
											childEntity.discoveryTime = DateUtils.nowDate();
											if (!childEntity.hidden) {
												childEntity.rookie = false;
												rookieHit = true;
											}
										}
									}
								}

								/* Check to see if we have any visible entities */
								boolean visibleEntity = false;
								for (Entity entity : entities) {
									if (!entity.hidden) {
										visibleEntity = true;
										break;
									}
								}

								if (visibleEntity) {
									mCandiPatchPresenter.renderingActivate();
									doUpdateEntities(entities, mScanOptions.refreshAllBeacons, false);
									Logger.d(this, "Model updated with entities");
									mCandiPatchPresenter.renderingActivate(5000);
									if (mScanOptions.refreshAllBeacons) {
										mCandiPatchPresenter.manageViewsAsync();
									}

									/* Check for rookies and play a sound */
									if (mPrefSoundEffects && rookieHit) {
										mCandiAlertSound.play();
									}
								}
							}

						}
					}
					EventBus.onBeaconScanComplete(serviceResponse);
				}
			};
		}

		public void start(Options options) {
			mOptions = options;
			new AsyncTask() {

				@Override
				protected void onPreExecute() {}

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
		else if (refreshType == RefreshType.AutoScan) {
			Logger.i(this, "User starting lightweight beacon scan");
			Tracker.trackEvent("Search", "Refresh", "BeaconScan", 0);
			scanForBeacons(new Options(false, false), false);
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
				if (entity != null && !entity.hidden) {

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
			if (!entity.hidden) {
				return true;
			}
		}
		return false;
	}

	private void refreshModel() {

		Logger.d(this, "Deleted entities: " + String.valueOf(ProxiExplorer.getInstance().mEntitiesDeleted.size()));
		Logger.d(this, "Inserted entities: " + String.valueOf(ProxiExplorer.getInstance().mEntitiesInserted.size()));
		Logger.d(this, "Updated entities: " + String.valueOf(ProxiExplorer.getInstance().mEntitiesUpdated.size()));

		new AsyncTask() {

			private boolean	mRefreshNeeded	= false;

			@Override
			protected void onPreExecute() {
			//mCommon.showProgressDialog(true, "Updating...");
			}

			@Override
			protected Object doInBackground(Object... params) {

				if (ProxiExplorer.getInstance().mEntitiesDeleted.size() > 0) {
					Iterator it = ProxiExplorer.getInstance().mEntitiesDeleted.entrySet().iterator();
					while (it.hasNext()) {
						Map.Entry entry = (Map.Entry) it.next();
						Entity entityDeleted = (Entity) entry.getValue();

						CandiModel candiModel = mCandiPatchModel.getCandiModelForEntity(entityDeleted.id);
						if (candiModel != null) {
							candiModel.setDeleted(true);
						}

						ProxiExplorer.getInstance().mEntitiesUpdated.remove(entityDeleted.id);
						if (ProxiExplorer.getInstance().mEntitiesInserted.remove(entityDeleted.id) == null) {
							/*
							 * Because of 'my candi', an entity could be deleted that isn't currently being
							 * tracked in the big model (beacon might not be tracked either).
							 */
							Beacon beacon = ProxiExplorer.getInstance().getBeaconById(entityDeleted.beaconId);
							if (beacon != null) {
								beacon.dirty = true;
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
							beacon.dirty = true;
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
								entity.dirty = true;
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

					Logger.d(this, "Refreshing model");
					ServiceResponse serviceResponse = ProxiExplorer.getInstance().refreshDirtyEntities();
					if (serviceResponse.responseCode == ResponseCode.Success) {
						List<Entity> freshEntityProxies = (List<Entity>) serviceResponse.data;

						@SuppressWarnings("unused")
						boolean rookieHit = false;
						for (Entity entity : freshEntityProxies) {
							/*
							 * We keep bumping the date up until the entity is finally
							 * visible.
							 */
							if (entity.rookie) {
								entity.discoveryTime = DateUtils.nowDate();
								if (!entity.hidden) {
									entity.rookie = false;
									rookieHit = true;
								}
							}
							for (Entity childEntity : entity.children) {
								/*
								 * We keep bumping the date up until the entity is finally
								 * visible.
								 */
								if (childEntity.rookie) {
									childEntity.discoveryTime = DateUtils.nowDate();
									if (!childEntity.hidden) {
										childEntity.rookie = false;
										rookieHit = true;
									}
								}
							}
						}

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
				//mCommon.showProgressDialog(false, null);

				if (serviceResponse.responseCode == ResponseCode.Success) {
					ProxiExplorer.getInstance().mEntitiesInserted.clear();
					ProxiExplorer.getInstance().mEntitiesUpdated.clear();
					ProxiExplorer.getInstance().mEntitiesDeleted.clear();
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
		intentBuilder.setEntityId(entity.id);
		intentBuilder.setEntityType(entity.type);
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
				public void onAnimationRepeat(Animation animation) {
			}

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
				 * Monitor textures
				 * 
				 * Global textures: 1
				 * Textures per zone: 1
				 * Textures per candi: 3
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
				if (mCandiPatchPresenter != null && mCandiPatchPresenter.mCandiViewPool != null) {
					candiPooledCount = mCandiPatchPresenter.mCandiViewPool.pooledCount();
					((TextView) tableRow.findViewById(R.id.text_value)).setText(String.valueOf(mCandiPatchPresenter.mCandiViewPool.pooledCount()));
				}
				table.addView(tableRow, tableLp);

				tableRow = (TableRow) getLayoutInflater().inflate(R.layout.temp_tablerow_debug, null);
				((TextView) tableRow.findViewById(R.id.text_label)).setText("Candi loaned: ");
				if (mCandiPatchPresenter != null && mCandiPatchPresenter.mCandiViewPool != null) {
					candiLoanedCount = mCandiPatchPresenter.mCandiViewPool.loanedCount();
					((TextView) tableRow.findViewById(R.id.text_value))
							.setText(String.valueOf(mCandiPatchPresenter.mCandiViewPool.loanedCount()));
				}
				table.addView(tableRow, tableLp);

				tableRow = (TableRow) getLayoutInflater().inflate(R.layout.temp_tablerow_debug, null);
				((TextView) tableRow.findViewById(R.id.text_label)).setText("Zones: ");
				if (mCandiPatchPresenter != null && mCandiPatchPresenter.mCandiViewPool != null) {
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
				User user = (User) ProxibaseService.convertJsonToObject(jsonResponse, User.class, GsonType.ProxibaseService);
				Aircandi.getInstance().setUser(user);
				if (Aircandi.getInstance().getUser() != null) {
					ImageUtils.showToastNotification("Signed in as " + Aircandi.getInstance().getUser().name, Toast.LENGTH_SHORT);
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
		 * Called after Create, Resume->LoadEngine.
		 * CandiPatchPresenter handles scene instantiation and setup
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
		 * This gets called anytime the game surface gains window focus. The game
		 * engine acquries the wake lock, restarts the engine, resumes the GLSurfaceView.
		 * The engine reloads textures.
		 */
		Logger.d(this, "Starting animation engine");
		if (mReadyToRun) {

			if (mFirstRun) {
				Logger.i(this, "Starting first run full beacon scan");
				scanForBeacons(new Options(true, false), true);
				mFirstRun = false;
			}
			else {
				PrefResponse prefResponse = updatePreferences();
				if (prefResponse == PrefResponse.Refresh) {
					Logger.i(this, "Starting full beacon scan because of preference change");
					scanForBeacons(new Options(true, false), true);
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
		return R.layout.candi_radar;
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
		else {
			/* Restart autoscan if enabled */
			if (hasWindowFocus) {
				if (mPrefAutoscan) {
					mMyHandler.post(mScanRunnable);
				}
			}
			else {
				/* Make sure autoscan is stopped if we are not in the foreground */
				mMyHandler.removeCallbacks(mScanRunnable);
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

		//		/* Theme might have changed since we were away */
		//		if (!mCommon.mPrefTheme.equals(Aircandi.settings.getString(Preferences.PREF_THEME, "aircandi_theme_midnight"))) {
		//			Logger.d(this, "Restarting because of theme change");
		//			mCommon.mPrefTheme = Aircandi.settings.getString(Preferences.PREF_THEME, "aircandi_theme_midnight");
		//			mCommon.reload();
		//		}
		//		else {
		//			/* We control window focus messages that trigger the engine from here. */
		//			super.onWindowFocusChanged(true);
		//			final WebImageView imageUser = (WebImageView) findViewById(R.id.image_user);
		//
		//			/* We run a fake modifier so we can trigger texture loading */
		//			mEngine.getScene().registerEntityModifier(
		//					new AlphaModifier(CandiConstants.DURATION_TRANSITIONS_FADE, 0.0f, 1.0f, new IEntityModifierListener() {
		//
		//						@Override
		//						public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
		//							onTexturesReady();
		//						}
		//
		//						@Override
		//						public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {
		//							mCandiPatchPresenter.renderingActivate();
		//						}
		//					}, EaseLinear.getInstance()));
		//
		//			/* User or user picture could have changed in another activity */
		//			if (imageUser != null && Aircandi.getInstance().getUser() != null) {
		//				mCommon.updateUserPicture();
		//			}
		//
		//			/* Entities could have been inserted, updated or deleted by another activity so refresh our model */
		//			refreshModel();
		//
		//			/* Restart autoscan if enabled */
		//			if (mPrefAutoscan) {
		//				mMyHandler.postDelayed(mScanRunnable, mPrefAutoscanInterval);
		//			}
		//		}
	}

	@Override
	protected void onResume() {
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

		/*
		 * Logic that should only run if the activity is resuming after having been paused.
		 * This used to be in restart but it wasn't getting called reliably when returning
		 * from another activity.
		 */
		if (mPaused) {
			Logger.i(this, "CandiSearchActivity resuming after pause");

			/* Theme might have changed since we were away */
			if (!mCommon.mPrefTheme.equals(Aircandi.settings.getString(Preferences.PREF_THEME, "aircandi_theme_midnight"))) {
				Logger.d(this, "Restarting because of theme change");
				mCommon.mPrefTheme = Aircandi.settings.getString(Preferences.PREF_THEME, "aircandi_theme_midnight");
				mCommon.reload();
			}
			else {
				/* We control window focus messages that trigger the engine from here. */
				super.onWindowFocusChanged(true);
				final WebImageView imageUser = (WebImageView) findViewById(R.id.image_user);

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

				/* User or user picture could have changed in another activity */
				if (imageUser != null && Aircandi.getInstance().getUser() != null) {
					mCommon.updateUserPicture();
				}

				/* Entities could have been inserted, updated or deleted by another activity so refresh our model */
				refreshModel();

				/* Restarting autoscan happens when we gain window focus */
			}
			mPaused = false;
		}
		else {
			Logger.i(this, "CandiSearchActivity resuming for first time");
		}

		ProxiExplorer.getInstance().onResume();
		NetworkManager.getInstance().onResume();
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
		Logger.i(this, "CandiSearchActivity stopped");
		super.onStop();

		/* We control window focus messages that trigger the engine from here. */
		super.onWindowFocusChanged(false);

		/* Make sure autoscan is stopped if we are not in the foreground */
		mMyHandler.removeCallbacks(mScanRunnable);

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

	// --------------------------------------------------------------------------------------------
	// Preferences routines
	// --------------------------------------------------------------------------------------------

	public PrefResponse updatePreferences() {

		PrefResponse prefResponse = PrefResponse.None;

		if (mPrefAutoscan != Aircandi.settings.getBoolean(Preferences.PREF_AUTOSCAN, false)) {
			prefResponse = PrefResponse.Change;
			mPrefAutoscan = Aircandi.settings.getBoolean(Preferences.PREF_AUTOSCAN, false);
			if (mPrefAutoscan) {
				mMyHandler.post(mScanRunnable);
			}
			else {
				mMyHandler.removeCallbacks(mScanRunnable);
			}
		}
		if (!mPrefAutoscanInterval.equals(Aircandi.settings.getString(Preferences.PREF_AUTOSCAN_INTERVAL, "5000"))) {
			prefResponse = PrefResponse.Change;
			mPrefAutoscanInterval = Aircandi.settings.getString(Preferences.PREF_AUTOSCAN_INTERVAL, "5000");
			if (mPrefAutoscan) {
				mMyHandler.removeCallbacks(mScanRunnable);
				mMyHandler.post(mScanRunnable);
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

	// --------------------------------------------------------------------------------------------
	// Application menu routines (settings)
	// --------------------------------------------------------------------------------------------

	public boolean onCreateOptionsMenu(Menu menu) {
		mCommon.doCreateOptionsMenu(menu);
		return true;
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		mCommon.doPrepareOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		mCommon.doOptionsItemSelected(item);
		return true;
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

		Query query = new Query("Documents").filter("Type eq 'version' and Target eq 'aircandi'");
		ServiceRequest serviceRequest = new ServiceRequest(ProxiConstants.URL_AIRCANDI_SERVICE_ODATA, query, RequestType.Get, ResponseFormat.Json);
		serviceRequest.setSuppressUI(true);
		ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (serviceResponse.responseCode == ResponseCode.Success) {

			String jsonResponse = (String) serviceResponse.data;
			VersionInfo versionInfo = (VersionInfo) ProxibaseService.convertJsonToObject(jsonResponse, VersionInfo.class, GsonType.ProxibaseService);
			String currentVersionName = Aircandi.getVersionName(this, CandiRadar.class);

			if (!currentVersionName.equals(versionInfo.versionName)) {

				AircandiCommon.showAlertDialog(R.drawable.icon_app, "New Aircandi version",
						"A newer version of Aircandi is available. Please download and install as soon possible.", this, new
						DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog, int which) {
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
		AutoScan,
		BeaconScan,
		BeaconScanPlusCurrent,
		All
	}

}