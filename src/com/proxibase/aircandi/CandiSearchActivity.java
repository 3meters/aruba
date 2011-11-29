package com.proxibase.aircandi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.microedition.khronos.opengles.GL10;

import org.acra.ErrorReporter;
import org.anddev.andengine.audio.sound.Sound;
import org.anddev.andengine.audio.sound.SoundFactory;
import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.engine.options.EngineOptions.ScreenOrientation;
import org.anddev.andengine.engine.options.resolutionpolicy.FillResolutionPolicy;
import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.modifier.AlphaModifier;
import org.anddev.andengine.entity.modifier.ParallelEntityModifier;
import org.anddev.andengine.entity.modifier.RotationAtModifier;
import org.anddev.andengine.entity.modifier.IEntityModifier.IEntityModifierListener;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.opengl.view.RenderSurfaceView;
import org.anddev.andengine.util.modifier.IModifier;
import org.anddev.andengine.util.modifier.ease.EaseCircularIn;
import org.anddev.andengine.util.modifier.ease.EaseCircularOut;
import org.anddev.andengine.util.modifier.ease.EaseCubicIn;
import org.anddev.andengine.util.modifier.ease.EaseCubicOut;
import org.anddev.andengine.util.modifier.ease.EaseLinear;
import org.apache.http.client.ClientProtocolException;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.ActivityManager.MemoryInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Animation.AnimationListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SlidingDrawer;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import android.widget.ViewSwitcher;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.amazonaws.auth.BasicAWSCredentials;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.proxibase.aircandi.AircandiActivity.Verb;
import com.proxibase.aircandi.R;
import com.proxibase.aircandi.candi.camera.ChaseCamera;
import com.proxibase.aircandi.candi.models.CandiModel;
import com.proxibase.aircandi.candi.models.CandiPatchModel;
import com.proxibase.aircandi.candi.models.IModel;
import com.proxibase.aircandi.candi.models.CandiModel.DisplayExtra;
import com.proxibase.aircandi.candi.presenters.CandiPatchPresenter;
import com.proxibase.aircandi.candi.presenters.CandiPatchPresenter.ICandiListener;
import com.proxibase.aircandi.candi.presenters.CandiPatchPresenter.TextureReset;
import com.proxibase.aircandi.candi.views.CandiView;
import com.proxibase.aircandi.candi.views.ViewAction;
import com.proxibase.aircandi.candi.views.ViewAction.ViewActionType;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.utils.AnimUtils;
import com.proxibase.aircandi.utils.Exceptions;
import com.proxibase.aircandi.utils.ImageCache;
import com.proxibase.aircandi.utils.ImageManager;
import com.proxibase.aircandi.utils.ImageUtils;
import com.proxibase.aircandi.utils.Logger;
import com.proxibase.aircandi.utils.NetworkManager;
import com.proxibase.aircandi.utils.ProxiHandlerManager;
import com.proxibase.aircandi.utils.Rotate3dAnimation;
import com.proxibase.aircandi.utils.ImageLoader.ImageProfile;
import com.proxibase.aircandi.utils.ImageManager.ImageRequestListener;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest.ImageFormat;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest.ImageShape;
import com.proxibase.aircandi.utils.NetworkManager.IConnectivityListener;
import com.proxibase.aircandi.utils.NetworkManager.IConnectivityReadyListener;
import com.proxibase.aircandi.widgets.QuickContactWindow;
import com.proxibase.aircandi.widgets.ViewPagerIndicator;
import com.proxibase.aircandi.widgets.ViewPagerIndicator.PageInfoProvider;
import com.proxibase.sdk.android.proxi.consumer.Beacon;
import com.proxibase.sdk.android.proxi.consumer.Command;
import com.proxibase.sdk.android.proxi.consumer.EntityProxy;
import com.proxibase.sdk.android.proxi.consumer.ProxiExplorer;
import com.proxibase.sdk.android.proxi.consumer.User;
import com.proxibase.sdk.android.proxi.consumer.ProxiExplorer.IEntityProcessListener;
import com.proxibase.sdk.android.proxi.consumer.ProxiExplorer.Options;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.Query;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException.ProxiErrorCode;

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
public class CandiSearchActivity extends AircandiGameActivity {

	private static String				COMPONENT_NAME				= "CandiSearch";
	private Boolean						mPrefAutoscan				= false;
	private int							mPrefAutoscanInterval		= 5000;
	private boolean						mPrefDemoMode				= false;
	private DisplayExtra				mPrefDisplayExtras			= DisplayExtra.None;
	private boolean						mPrefEntityFencing			= true;
	private boolean						mPrefShowDebug				= false;
	private boolean						mPrefSoundEffects			= true;

	private Boolean						mReadyToRun					= false;
	private Boolean						mFullUpdateSuccess			= false;
	private Handler						mHandler					= new Handler();
	private LayoutInflater				mInflater;
	private Boolean						mCredentialsFound			= false;
	public static BasicAWSCredentials	mAwsCredentials				= null;

	private List<EntityProxy>			mProxiEntities;
	private List<EntityProxy>			mProxiEntitiesFlat;
	private ProxiHandlerManager			mProxiHandlerManager;

	private CandiPatchModel				mCandiPatchModel;
	private CandiPatchPresenter			mCandiPatchPresenter;

	protected ImageView					mProgressIndicator;
	protected ImageView					mButtonRefresh;
	private RenderSurfaceView			mCandiSurfaceView;
	private View						mCandiInfoView;
	private ProgressDialog				mProgressDialog;

	private ViewPager					mCandiPager;
	private ViewPagerIndicator			mCandiPagerIndicator;
	private CandiPagerAdapter			mCandiPagerAdapter;
	private FrameLayout					mSliderWrapperSearch;

	private DisplayMetrics				mDisplayMetrics;

	private ViewFlipper					mCandiFlipper;
	private ViewSwitcher				mViewSwitcher;

	private boolean						mCandiInfoVisible			= false;
	private QuickContactWindow			mQuickContactWindow;
	private boolean						mCandiActivityActive		= false;
	private AnimType					mAnimTypeCandiInfo			= AnimType.RotateCandi;
	private Sound						mCandiAlertSound;
	private Rotate3dAnimation			mRotate3dAnimation;
	private boolean						mFirstRun					= true;
	private boolean						mFirstTimeSliderInfoShow	= true;
	private boolean						mSliderInfoOpen				= true;
	private ScreenOrientation			mScreenOrientation			= ScreenOrientation.PORTRAIT;
	private PackageReceiver				mPackageReceiver			= new PackageReceiver();
	private boolean						mIgnoreInput				= false;
	private boolean						mUsingEmulator				= false;

	private int							mRenderMode;
	protected User						mUser;
	private Runnable					mUserSignedInRunnable;

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

		/* Tools */
		LayoutInflater mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		/* Ui Hookup */
		mCandiFlipper = (ViewFlipper) findViewById(R.id.flipper_candi);

		mViewSwitcher = (ViewSwitcher) findViewById(R.id.view_switcher);
		mViewSwitcher.setInAnimation(this, R.anim.fade_in_short);
		mViewSwitcher.setOutAnimation(this, R.anim.fade_out_short);

		mCandiPager = (ViewPager) findViewById(R.id.pager);
		mCandiPager.setAdapter(new CandiPagerAdapter());
		mCandiPager.setOnPageChangeListener(mCandiPagerIndicator);

		mCandiPagerIndicator = (ViewPagerIndicator) findViewById(R.id.pager_indicator);
		mCandiPagerIndicator.bindToView((View) mInflater.inflate(R.layout.temp_page_indicator, null));
		mCandiPagerIndicator.initialize(1, 2, (PageInfoProvider) mCandiPager.getAdapter());
		mCandiPager.setOnPageChangeListener(mCandiPagerIndicator);

		mProgressDialog = new ProgressDialog(this);

		View view = findViewById(R.id.img_action_button);

		mCandiInfoView = (View) findViewById(R.id.view_candi_info);

		/* Debug footer */

		mPrefShowDebug = Aircandi.settings.getBoolean(Preferences.PREF_SHOW_DEBUG, false);
		mSliderWrapperSearch = (FrameLayout) findViewById(R.id.slider_wrapper_search);
		debugSliderShow(mPrefShowDebug);
		if (mPrefShowDebug) {
			updateDebugInfo();
		}

		registerForContextMenu(findViewById(R.id.group_refresh));

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
		ImageManager.getInstance().setContext(getApplicationContext());

		/* Candi patch */
		mDisplayMetrics = getResources().getDisplayMetrics();
		mCandiPatchModel = new CandiPatchModel();
		mCandiPatchModel.setScreenWidth(mDisplayMetrics.widthPixels);

		/* Proxi activities */
		mProxiHandlerManager = new ProxiHandlerManager(this);

		/* Property settings get overridden once we retrieve preferences */
		mCandiSurfaceView = (RenderSurfaceView) findViewById(R.id.view_rendersurface);
		mCandiSurfaceView.requestFocus();
		mCandiSurfaceView.setFocusableInTouchMode(true);
		mProgressDialog = new ProgressDialog(this);

		/* Animation */
		mRotate3dAnimation = new Rotate3dAnimation();

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
	// Events
	// --------------------------------------------------------------------------------------------

	public void onCommandButtonClick(View view) {

		final Command command = (Command) view.getTag();
		final String commandHandler = "com.proxibase.aircandi." + command.handler;
		String commandName = command.name.toLowerCase();

		if (command.type.toLowerCase().equals("list")) {
			ImageUtils.showToastNotification(this, "Unimplemented...", Toast.LENGTH_SHORT);
			return;
		}

		try {

			if (command.handler.toLowerCase().equals("dialog.new")) {
				showNewCandiDialog();
				return;
			}
			else if (command.handler.toLowerCase().equals("dialog.new")) {
				showNewCandiDialog();
				return;
			}

			if (commandName.toLowerCase().equals("audio")) {
				String audioFile = "";
				// if (command.entityResourceId.contains("hopper_house"))
				// audioFile = "hopper_house_by_the_railroad.mp3";
				// else if (command.entityResourceId.contains("klimt_hope"))
				// audioFile = "klimt_hope.mp3";
				// else if (command.entityResourceId.contains("monet_japanese"))
				// audioFile = "monet_japanese_footbridge.mp3";
				// else if (command.entityResourceId.contains("starry_night"))
				// audioFile = "vangogh_starry_night.mp3";
				// else if (command.entityResourceId.contains("cezanne"))
				// audioFile = "cezanne2.mp3";

				// MediaPlayer mediaPlayer = new MediaPlayer();
				// Uri uri = Uri.parse(Aircandi.URL_AIRCANDI_MEDIA + "audio.3meters.com/signsoflove.mp3");

				Uri uri = Uri.parse("http://dev.aircandi.com/media/" + audioFile);

				// Uri url = Uri.parse("http://dev.aircandi.com/media/cezanne2.mp3");
				// AsyncPlayer player = new AsyncPlayer("Aircandi");
				// player.stop();
				// player.play(Tricorder.this, url, false, AudioManager.STREAM_MUSIC);

			}
			else if (commandName.toLowerCase().equals("video")) {
				// String movieUrl = Aircandi.URL_RIPPLEMEDIA +
				// "video/cezanne.mp3";
				// //String movieUrl =
				// "http://www.youtube.com/watch?v=q2mMSTlgWcU";
				// Intent tostart = new Intent(Intent.ACTION_VIEW);
				// tostart.setDataAndType(Uri.parse(movieUrl), "video/*");
				// startActivity(tostart);
			}
			else {
				String message = getString(R.string.dialog_new_signin) + " " + command.label;
				mUserSignedInRunnable = new Runnable() {

					@Override
					public void run() {
						try {
							Class clazz = Class.forName(commandHandler, false, this.getClass().getClassLoader());
							Logger.i(CandiSearchActivity.this, "Starting activity: " + clazz.toString());
							EntityProxy entityProxy = command.entity;

							if (command.verb.equals("new")) {
								Intent intent = buildIntent(null, command.entity.id, command, command.entity.beacon, mUser, clazz);
								startActivityForResult(intent, CandiConstants.ACTIVITY_ENTITY_HANDLER);
							}
							else {
								Intent intent = buildIntent(entityProxy, 0, command, null, mUser, clazz);
								startActivityForResult(intent, CandiConstants.ACTIVITY_ENTITY_HANDLER);
							}

						}
						catch (ClassNotFoundException exception) {
							exception.printStackTrace();
						}
						finally {
							mUserSignedInRunnable = null;
						}
					}
				};

				if (!command.verb.equals("view")) {
					if (mUser != null && mUser.anonymous) {
						startActivityForResult(new Intent(this, SignInForm.class).putExtra(getString(R.string.EXTRA_MESSAGE),
								message), CandiConstants.ACTIVITY_SIGNIN);
					}
					else {
						mHandler.post(mUserSignedInRunnable);
					}
				}
				else {
					mHandler.post(mUserSignedInRunnable);
				}
			}
		}
		catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		catch (IllegalStateException e) {
			e.printStackTrace();
		}
		catch (SecurityException exception) {
			exception.printStackTrace();
		}
	}

	public void onItemMoreButtonClick(View view) {

		CandiModel candiModel = (CandiModel) view.getTag();
		if (mQuickContactWindow == null) {
			mQuickContactWindow = new QuickContactWindow(this);
		}
		else {
			long dismissInterval = System.currentTimeMillis() - mQuickContactWindow.getActionStripToggleTime();
			if (dismissInterval <= 200) {
				return;
			}
		}

		int[] coordinates = { 0, 0 };

		view.getLocationInWindow(coordinates);
		final Rect rect = new Rect(coordinates[0], coordinates[1], coordinates[0] + view.getWidth(), coordinates[1] + view.getHeight());
		View content = configureActionButtons(candiModel, CandiSearchActivity.this);

		mQuickContactWindow.show(rect, content, view, 0, -13, -5);
	}

	public void onBackPressed() {
		if (!mCandiInfoVisible) {
			if (mCandiPatchModel.getCandiRootCurrent().getParent() != null) {
				mCandiPatchPresenter.renderingActivate();
				mCandiPatchPresenter.navigateModel(mCandiPatchModel.getCandiRootCurrent().getParent(), false);
			}
			else {
				super.onBackPressed();
			}
		}
		else {
			if (mQuickContactWindow != null && mQuickContactWindow.isShowing())
				mQuickContactWindow.dismiss();
			hideCandiInfo(AnimType.RotateCandi, false);
		}
	}

	public void onRefreshClick(View view) {
		/* For this activity, refresh means rescan and reload entity data from the service */
		if (mReadyToRun) {
			doRefresh(RefreshType.BeaconScanPlusCurrent);
		}
		updateDebugInfo();
	}

	private void doRefresh(RefreshType refreshType) {
		if (!mFullUpdateSuccess || refreshType == RefreshType.All) {
			Logger.i(this, "User starting full beacon scan");
			scanForBeacons(new Options(true, false), true);
		}
		else if (refreshType == RefreshType.BeaconScan) {
			Logger.i(this, "User starting lightweight beacon scan");
			scanForBeacons(new Options(false, false), true);
		}
		else if (refreshType == RefreshType.BeaconScanPlusCurrent) {

			Logger.i(this, "User starting lightweight beacon scan");
			final CandiModel candiModelFocused = mCandiPatchModel.getCandiModelFocused();
			final int idOfEntityToRefresh = candiModelFocused.getEntityProxy().id;

			if (!candiModelFocused.getViewStateCurrent().isVisible()) {
				scanForBeacons(new Options(false, false), true);
			}
			else {
				Logger.i(this, "User starting current entity refresh");

				/* Mark the entity as dirty */
				for (EntityProxy entityProxy : ProxiExplorer.getInstance().getEntityProxiesFlat()) {
					if (entityProxy.id.equals(idOfEntityToRefresh)) {
						entityProxy.isDirty = true;
						break;
					}
				}

				/*
				 * This will come back before we really know if the candi we want to
				 * refresh textures for is still around.
				 */

				scanForBeacons(new Options(false, true), true);

				/* Scan could have caused the current candi to go away or be hidden */
				EntityProxy entityProxy = ProxiExplorer.getInstance().getEntityById(idOfEntityToRefresh);
				if (entityProxy != null && !entityProxy.isHidden) {

					/* Refresh candi info */
					if (mCandiInfoVisible && entityProxy.id.equals(mCandiPatchModel.getCandiModelSelected().getEntityProxy().id)) {
						mCandiPager.getAdapter().notifyDataSetChanged();
					}

					/* Refresh candi view texture */
					Logger.v(this, "Update texture: " + candiModelFocused.getTitleText());
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

	public void onInfoViewClick(View v) {
		hideCandiInfo(AnimType.RotateCandi, false);
		mCandiInfoVisible = false;
	}

	public void onHomeClick(View view) {}

	public void onActionsClick(View view) {

		if (mQuickContactWindow == null) {
			mQuickContactWindow = new QuickContactWindow(this);
		}
		else {
			long dismissInterval = System.currentTimeMillis() - mQuickContactWindow.getActionStripToggleTime();
			if (dismissInterval <= 200) {
				return;
			}
		}

		int[] coordinates = { 0, 0 };

		view.getLocationInWindow(coordinates);
		final Rect rect = new Rect(coordinates[0], coordinates[1], coordinates[0] + view.getWidth(), coordinates[1] + view.getHeight());
		View content = configureActionButtons(CandiSearchActivity.this);

		mQuickContactWindow.show(rect, content, view, 0, -10, -11);
	}

	public void onProfileClick(View view) {

		if (mUser.anonymous) {
			mUserSignedInRunnable = null;
			startActivityForResult(new Intent(this, SignInForm.class), CandiConstants.ACTIVITY_SIGNIN);
		}
		else {
			Intent intent = buildIntent(null, 0, new Command("edit"), null, mUser, ProfileForm.class);
			startActivityForResult(intent, CandiConstants.ACTIVITY_PROFILE);
		}
	}

	private void updateDebugInfo() {

		if (mPrefShowDebug) {

			ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
			Debug.MemoryInfo memoryInfo = new Debug.MemoryInfo();
			Debug.getMemoryInfo(memoryInfo);

			((RelativeLayout) findViewById(R.id.slider_content_search)).removeAllViews();
			final TableLayout table = new TableLayout(this);
			TableLayout.LayoutParams tableLp = new TableLayout.LayoutParams();
			tableLp.setMargins(0, 0, 0, 0);

			TableRow tableRow = (TableRow) getLayoutInflater().inflate(R.layout.temp_tablerow_debug, null);
			((TextView) tableRow.findViewById(R.id.txt_label)).setText("Native heap size: ");
			((TextView) tableRow.findViewById(R.id.txt_value)).setText(String.format("%2.2f MB",
					(float) ((float) Debug.getNativeHeapSize() / 1048576f)));
			table.addView(tableRow, tableLp);

			tableRow = (TableRow) getLayoutInflater().inflate(R.layout.temp_tablerow_debug, null);
			((TextView) tableRow.findViewById(R.id.txt_label)).setText("Native heap alloc: ");
			((TextView) tableRow.findViewById(R.id.txt_value)).setText(String.format("%2.2f MB",
					(float) ((float) Debug.getNativeHeapAllocatedSize() / 1048576f)));
			table.addView(tableRow, tableLp);

			tableRow = (TableRow) getLayoutInflater().inflate(R.layout.temp_tablerow_debug, null);
			((TextView) tableRow.findViewById(R.id.txt_label)).setText("Native heap free: ");
			((TextView) tableRow.findViewById(R.id.txt_value)).setText(String.format("%2.2f MB",
					(float) ((float) Debug.getNativeHeapFreeSize() / 1048576f)));
			table.addView(tableRow, tableLp);

			tableRow = (TableRow) getLayoutInflater().inflate(R.layout.temp_tablerow_debug, null);
			((TextView) tableRow.findViewById(R.id.txt_label)).setText("Total pss: ");
			((TextView) tableRow.findViewById(R.id.txt_value)).setText(String.format("%2.2f MB", (float) ((float) memoryInfo.getTotalPss() / 1024)));
			table.addView(tableRow, tableLp);

			tableRow = (TableRow) getLayoutInflater().inflate(R.layout.temp_tablerow_debug, null);
			((TextView) tableRow.findViewById(R.id.txt_label)).setText("Total shared dirty: ");
			((TextView) tableRow.findViewById(R.id.txt_value)).setText(String.format("%2.2f MB",
					(float) ((float) memoryInfo.getTotalSharedDirty() / 1024)));
			table.addView(tableRow, tableLp);

			tableRow = (TableRow) getLayoutInflater().inflate(R.layout.temp_tablerow_debug, null);
			((TextView) tableRow.findViewById(R.id.txt_label)).setText("Total priv dirty: ");
			((TextView) tableRow.findViewById(R.id.txt_value)).setText(String.format("%2.2f MB",
					(float) ((float) memoryInfo.getTotalPrivateDirty() / 1024)));
			table.addView(tableRow, tableLp);

			((RelativeLayout) findViewById(R.id.slider_content_search)).addView(table);
		}
	}

	public void onNewCandiClick(View view) {
		showNewCandiDialog();
	}

	private void doCandiSingleTap(final CandiModel candiModel) {
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
				mCandiPager.setCurrentItem(PagerView.CandiInfo.ordinal());
				showCandiInfo(candiModel, AnimType.RotateCandi);
			}
		});
	}

	// --------------------------------------------------------------------------------------------
	// Entity routines
	// --------------------------------------------------------------------------------------------

	public interface IScanCompleteListener {

		void onScanComplete();
	}

	private void scanForBeacons(final Options options, final boolean showProgress) {

		/* Check that wifi is enabled and we have a network connection */
		verifyConnectivity(new IConnectivityReadyListener() {

			@Override
			public void onConnectivityReady() {

				if (showProgress) {
					startTitlebarProgress(true);
				}

				if (options.refreshAllBeacons && mCandiPatchPresenter != null) {
					mCandiPatchPresenter.renderingActivate(60000);
					mCandiPatchPresenter.setFullUpdateInProgress(true);
					mCandiPatchPresenter.mProgressSprite.setVisible(true);

				}

				ProxiExplorer.getInstance().scanForBeaconsAsync(options, new IEntityProcessListener() {

					@Override
					public void onComplete(final List<EntityProxy> entities) {

						/* We get called back on a background thread */

						runOnUiThread(new Runnable() {

							@Override
							public void run() {

								Logger.d(CandiSearchActivity.this, "Beacon scan results returned from Proxibase.ProxiExplorer");

								if (entities.size() == 0) {
									showNewCandiDialog();
								}
								else {
									mCandiPatchPresenter.renderingActivate(3000);
									doUpdateEntities(entities, options.refreshAllBeacons);

									/* Check for rookies and play a sound */
									if (mPrefSoundEffects)
										for (CandiModel candiModel : mCandiPatchModel.getCandiModels())
											if (candiModel.isRookie() && candiModel.getViewStateNext().isVisible()) {
												mCandiAlertSound.play();
												break;
											}
								}

								/* Wrap-up */
								if (options.refreshAllBeacons && !mFullUpdateSuccess) {
									mFullUpdateSuccess = true;
								}
								updateDebugInfo();
								mCandiPatchPresenter.setFullUpdateInProgress(false);
								mCandiPatchPresenter.mProgressSprite.setVisible(false);
								stopTitlebarProgress();

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
									mHandler.postDelayed(new Runnable() {

										public void run() {
											scanForBeacons(new Options(false, false), false);
										}
									}, mPrefAutoscanInterval);
								}
							}
						});
					}

					@Override
					public void onProxibaseException(ProxibaseException exception) {
						ImageUtils.showToastNotification(CandiSearchActivity.this, exception.getMessage(), Toast.LENGTH_SHORT);
						Logger.d(CandiSearchActivity.this, exception.getMessage());
						stopTitlebarProgress();
					}
				});
			}
		});

	}

	private void doUpdateEntities(List<EntityProxy> freshEntities, boolean fullUpdate) {
		/*
		 * Shallow copy so entities are by value but any object
		 * properties like beacon are by ref from the original.
		 */
		mProxiEntities = (List<EntityProxy>) ((ArrayList<EntityProxy>) freshEntities).clone();

		/* Push the new and updated entities into the system */
		mCandiPatchPresenter.updateCandiData(mProxiEntities, fullUpdate);

		//ImageManager.getInstance().getImageCache().recycleBitmaps();
	}

	private void doEntityUpdate(EntityProxy freshEntity, EntityProxy staleEntity) {
		/*
		 * Replace the entity in our master collection.
		 */
		for (int i = mProxiEntities.size() - 1; i >= 0; i--) {
			EntityProxy entityProxy = mProxiEntities.get(i);
			if (entityProxy.id == staleEntity.id) {
				if (freshEntity == null) {
					mProxiEntities.remove(i);
				}
				else {
					mProxiEntities.set(i, freshEntity);
				}
			}
		}

		/* Push the new and updated entities into the system */
		mCandiPatchPresenter.updateCandiData(mProxiEntities, false);

		//ImageManager.getInstance().getImageCache().recycleBitmaps();
	}

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	private void showCandiInfo(final CandiModel candiModel, AnimType animType) {

		mCandiInfoVisible = true;
		mCandiPatchPresenter.renderingActivate();
		Logger.d(this, "Show candi info: " + candiModel.getTitleText());

		if (animType == AnimType.CrossFadeFlipper) {
			mCandiSurfaceView.setVisibility(View.GONE);
			mCandiFlipper.clearAnimation();
			mCandiFlipper.setInAnimation(this, android.R.anim.slide_in_left);
			mCandiFlipper.setOutAnimation(this, android.R.anim.slide_out_right);
			mCandiFlipper.showNext();
			updateCandiBackButton(null);
			mCandiPatchPresenter.setIgnoreInput(false);
			mIgnoreInput = false;
		}
		if (animType == AnimType.Zoom) {
			Animation animationEnter = AnimationUtils.loadAnimation(CandiSearchActivity.this, R.anim.zoom_enter);
			Animation animationExit = AnimationUtils.loadAnimation(CandiSearchActivity.this, R.anim.zoom_exit);

			mCandiSurfaceView.startAnimation(animationExit);
			hideGLSurfaceView(CandiConstants.DURATION_CANDIINFO_SHOW);
			mCandiInfoView.setVisibility(View.VISIBLE);
			mCandiInfoView.startAnimation(animationEnter);

			updateCandiBackButton(null);
			mCandiPatchPresenter.setIgnoreInput(false);
			mIgnoreInput = false;
		}
		else if (animType == AnimType.CrossFade) {
			mCandiInfoView.setVisibility(View.VISIBLE);
			mCandiInfoView.startAnimation(AnimUtils.loadAnimation(this, R.anim.fade_in_medium));
			hideGLSurfaceView(CandiConstants.DURATION_CANDIINFO_SHOW);
			updateCandiBackButton(null);
			mCandiPatchPresenter.setIgnoreInput(false);
		}
		else if (animType == AnimType.RotateCandi) {
			if (mPrefShowDebug) {
				debugSliderShow(false);
			}
			if (mCandiPatchModel.getCandiModelSelected() != null && mCandiPatchModel.getCandiModelSelected().hasVisibleChildrenCurrent()) {
				mCandiPagerIndicator.setVisibility(View.VISIBLE);
				mCandiPagerIndicator.initialize(1, 2, (PageInfoProvider) mCandiPager.getAdapter());
			}
			else {
				mCandiPagerIndicator.setVisibility(View.GONE);
			}

			float rotationX = mCandiPatchModel.getCandiModelSelected().getZoneStateCurrent().getZone().getViewStateCurrent().getCenterX();
			float rotationY = mCandiPatchModel.getCandiModelSelected().getZoneStateCurrent().getZone().getViewStateCurrent().getCenterY();
			final float duration = CandiConstants.DURATION_CANDIINFO_SHOW;
			float scaleFrom = 1.0f;
			float scaleTo = 0.5f;
			float alphaFrom = 1.0f;
			float alphaTo = 0.0f;
			final IEntity entity = (IEntity) (mAnimTypeCandiInfo == AnimType.RotateCandi ? mCandiPatchPresenter.getCandiViewsHash().get(
					String.valueOf(mCandiPatchModel.getCandiModelSelected().getModelId())) : mEngine.getScene());

			if (mAnimTypeCandiInfo == AnimType.RotateCandi) {
				rotationX = ((CandiConstants.CANDI_VIEW_WIDTH * entity.getScaleX()) * 0.5f);
				rotationY = (CandiConstants.CANDI_VIEW_BODY_HEIGHT * 0.5f);
				scaleTo = 1.3f;
				alphaTo = 0.0f;
				mEngine.getScene().registerEntityModifier(new AlphaModifier(duration, 1.0f, 0.0f, EaseLinear.getInstance()));
			}

			entity.registerEntityModifier(new ParallelEntityModifier(new IEntityModifierListener() {

				@Override
				public void onModifierFinished(IModifier<IEntity> modifier, final IEntity entityModified) {

					mEngine.runOnUpdateThread(new Runnable() {

						@Override
						public void run() {
							entity.clearEntityModifiers();
						}
					});

					CandiSearchActivity.this.runOnUiThread(new Runnable() {

						public void run() {

							/*
							 * mCandiDetailView.setVisibility(View.VISIBLE);
							 * mCandiDetailView.startAnimation(AircandiUI.loadAnimation(CandiSearchActivity.this,
							 * R.anim.fade_in_medium));
							 * hideCandiSurfaceView();
							 * updateCandiBackButton();
							 * mCandiPatchPresenter.setIgnoreInput(false);
							 */
							infoSliderVisible(true, mCandiInfoView.findViewById(R.id.slider_wrapper_info));

							final float centerX = mCandiFlipper.getWidth() / 2.0f;
							final float centerY = mCandiFlipper.getHeight() / 2.0f;

							mRotate3dAnimation.reset();
							mRotate3dAnimation.configure(270, 360, centerX, centerY, 300.0f, false);
							mRotate3dAnimation.setDuration((long) (duration * 1000));
							mRotate3dAnimation.setFillAfter(true);
							mRotate3dAnimation.setInterpolator(new DecelerateInterpolator(3f));

							mRotate3dAnimation.setAnimationListener(new AnimationListener() {

								@Override
								public void onAnimationEnd(Animation animation) {

									if (mFirstTimeSliderInfoShow)
									{
										SlidingDrawer slidingDrawer = (SlidingDrawer) mCandiInfoView.findViewById(R.id.slide_actions_info);
										slidingDrawer.animateOpen();
										mFirstTimeSliderInfoShow = false;
									}

									mRotate3dAnimation.setAnimationListener(null);
								}

								@Override
								public void onAnimationRepeat(Animation animation) {}

								@Override
								public void onAnimationStart(Animation animation) {
									if (!mFirstTimeSliderInfoShow) {
										SlidingDrawer slidingDrawer = (SlidingDrawer) mCandiInfoView.findViewById(R.id.slide_actions_info);
										if (mSliderInfoOpen) {
											slidingDrawer.open();
										}
									}
								}
							});

							mCandiFlipper.clearAnimation();
							mCandiFlipper.setDisplayedChild(1);
							mCandiFlipper.startAnimation(mRotate3dAnimation);

							mCandiSurfaceView.setVisibility(View.GONE);
							mCandiInfoView.setVisibility(View.VISIBLE);

							updateCandiBackButton(null);
							mCandiPatchPresenter.setIgnoreInput(false);
						}
					});

				}

				@Override
				public void onModifierStarted(IModifier<IEntity> modifier, IEntity entity) {}

			}, new RotationAtModifier(duration, 0, 90, rotationX, rotationY, EaseCubicIn.getInstance()), new AlphaModifier(duration, alphaFrom,
					alphaTo, EaseCircularIn.getInstance())));
		}
	}

	private void hideCandiInfo(AnimType animType, final boolean showBusy) {

		if (mIgnoreInput)
			return;

		Logger.d(this, "Hide candi info");
		mCandiPatchPresenter.renderingActivate();
		mIgnoreInput = true;
		mCandiPatchPresenter.setIgnoreInput(true);

		if (animType == AnimType.Fade) {
			mCandiInfoView.setVisibility(View.GONE);
			mCandiInfoView.startAnimation(AnimUtils.loadAnimation(this, R.anim.fade_out_short));
			mCandiSurfaceView.setVisibility(View.VISIBLE);
			mCandiPatchPresenter.setIgnoreInput(false);
			mIgnoreInput = false;
		}
		else if (animType == AnimType.CrossFadeFlipper) {
			mCandiFlipper.showNext();
			mCandiSurfaceView.setVisibility(View.VISIBLE);
			updateCandiBackButton(!mCandiPatchModel.getCandiRootCurrent().isSuperRoot() ? mCandiPatchModel.getCandiRootCurrent().getTitleText()
																						: null);
			mCandiPatchPresenter.setIgnoreInput(false);
			mIgnoreInput = false;
		}
		else if (animType == AnimType.CrossFade) {

			mCandiInfoView.setVisibility(View.GONE);
			mCandiInfoView.startAnimation(AnimUtils.loadAnimation(this, R.anim.fade_out_short));

			/* Sets to Visible and does an alpha fade-in */
			mEngine.runOnUpdateThread(new Runnable() {

				@Override
				public void run() {

					mEngine.getScene().clearEntityModifiers();
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
					}, new AlphaModifier(CandiConstants.DURATION_CANDIINFO_HIDE, 0.0f, 1.0f, EaseLinear.getInstance())));
				}
			});

			mCandiPatchModel.setCandiModelSelected(null);
			updateCandiBackButton(!mCandiPatchModel.getCandiRootCurrent().isSuperRoot() ? mCandiPatchModel.getCandiRootCurrent().getTitleText()
																						: null);
			mCandiPatchPresenter.setIgnoreInput(false);
			CandiSearchActivity.this.mIgnoreInput = false;
		}
		else {

			/* Find the center of the container */
			final float duration = CandiConstants.DURATION_CANDIINFO_HIDE;
			final float centerX = mCandiFlipper.getWidth() / 2.0f;
			final float centerY = mCandiFlipper.getHeight() / 2.0f;

			/*
			 * Create a new 3D rotation with the supplied parameter.
			 * The animation listener is used to trigger the next animation.
			 */
			if (showBusy) {
				mProgressDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
				mProgressDialog.setMessage("Candi processing...");
				mProgressDialog.show();
				mCandiSurfaceView.setVisibility(View.VISIBLE);
			}

			infoSliderVisible(false, mCandiInfoView.findViewById(R.id.slider_wrapper_info));

			mRotate3dAnimation.reset();
			mRotate3dAnimation.configure(360, 270, centerX, centerY, 300.0f, true);
			mRotate3dAnimation.setDuration((long) (duration * 1000));
			mRotate3dAnimation.setFillAfter(true);
			mRotate3dAnimation.setInterpolator(new AccelerateInterpolator());
			mCandiFlipper.clearAnimation();
			mSliderInfoOpen = ((SlidingDrawer) mCandiInfoView.findViewById(R.id.slide_actions_info)).isOpened();

			mRotate3dAnimation.setAnimationListener(new AnimationListener() {

				@Override
				public void onAnimationEnd(Animation animation) {

					CandiSearchActivity.this.runOnUiThread(new Runnable() {

						@Override
						public void run() {

							mCandiFlipper.clearAnimation();
							mCandiInfoView.setVisibility(View.GONE);
							mCandiPagerIndicator.setVisibility(View.GONE);
							mCandiSurfaceView.setVisibility(View.VISIBLE);

							if (mPrefShowDebug) {
								debugSliderShow(true);
							}

							float rotationX = mCandiPatchModel.getCandiModelFocused().getZoneStateCurrent().getZone().getViewStateCurrent()
									.getCenterX();
							float rotationY = mCandiPatchModel.getCandiModelFocused().getZoneStateCurrent().getZone().getViewStateCurrent()
									.getCenterY();
							float scaleFrom = 0.5f;
							float scaleTo = 1.0f;
							float alphaFrom = 0.0f;
							float alphaTo = 1.0f;

							IEntity entity = mEngine.getScene();

							if (mAnimTypeCandiInfo == AnimType.RotateCandi) {
								entity = (IEntity) mCandiPatchPresenter.getCandiViewsHash().get(
										String.valueOf(mCandiPatchModel.getCandiModelFocused().getModelId()));
								rotationX = (CandiConstants.CANDI_VIEW_WIDTH * 0.5f);
								rotationY = (CandiConstants.CANDI_VIEW_BODY_HEIGHT * 0.5f);
								scaleFrom = 1.3f;
								alphaFrom = 0.5f;
								mEngine.getScene().registerEntityModifier(new AlphaModifier(duration, 0.0f, 1.0f, EaseLinear.getInstance()));
							}

							/* Using a scaling modifier is tricky to reverse without shifting coordinates */
							entity.registerEntityModifier(new ParallelEntityModifier(new RotationAtModifier(duration, 90f, 0.0f, rotationX,
									rotationY, EaseCubicOut.getInstance()), new AlphaModifier(duration, alphaFrom, alphaTo, EaseCircularOut
									.getInstance())));

							updateCandiBackButton(!mCandiPatchModel.getCandiRootCurrent().isSuperRoot() ? mCandiPatchModel.getCandiRootCurrent()
									.getTitleText() : null);
							mCandiPatchModel.setCandiModelSelected(null);
							mCandiPatchPresenter.setIgnoreInput(false);
							CandiSearchActivity.this.mIgnoreInput = false;
							if (showBusy) {
								mProgressDialog.dismiss();
							}
						}
					});

				}

				@Override
				public void onAnimationRepeat(Animation animation) {}

				@Override
				public void onAnimationStart(Animation animation) {}
			});

			mCandiFlipper.startAnimation(mRotate3dAnimation);
		}
		mCandiInfoVisible = false;
	}

	private RelativeLayout buildCandiInfo(final CandiModel candiModel, final RelativeLayout candiInfoView) {

		/* Build menus */
		Display getOrient = getWindowManager().getDefaultDisplay();
		boolean landscape = (getOrient.getOrientation() == Surface.ROTATION_90 || getOrient.getOrientation() == Surface.ROTATION_270);
		TableLayout table = configureMenus(mCandiPatchModel.getCandiModelSelected(), landscape, CandiSearchActivity.this);
		if (table != null) {
			RelativeLayout slideContent = (RelativeLayout) candiInfoView.findViewById(R.id.slider_content_info);
			slideContent.removeAllViews();
			slideContent.addView(table);
			((View) candiInfoView.findViewById(R.id.slide_actions_info)).setVisibility(View.VISIBLE);
		}
		else {
			((View) candiInfoView.findViewById(R.id.slide_actions_info)).setVisibility(View.GONE);
		}

		/* Update any UI indicators related to child candies */
		if (candiModel.hasVisibleChildrenCurrent()) {
			((ImageView) candiInfoView.findViewById(R.id.img_forward)).setVisibility(View.VISIBLE);
		}
		else {
			((ImageView) candiInfoView.findViewById(R.id.img_forward)).setVisibility(View.GONE);
		}

		if (candiModel.getEntityProxy().imageUri != null && !candiModel.getEntityProxy().imageUri.equals("")) {
			if (ImageManager.getInstance().hasImage(candiModel.getEntityProxy().imageUri)) {
				Bitmap bitmap = ImageManager.getInstance().getImage(candiModel.getEntityProxy().imageUri);
				((ImageView) candiInfoView.findViewById(R.id.img_public)).setImageBitmap(bitmap);
				if (ImageManager.getInstance().hasImage(candiModel.getEntityProxy().imageUri + ".reflection")) {
					bitmap = ImageManager.getInstance().getImage(candiModel.getEntityProxy().imageUri + ".reflection");
					((ImageView) candiInfoView.findViewById(R.id.img_public_reflection)).setImageBitmap(bitmap);
				}
			}
			else {
				Bitmap zoneBodyBitmap = null;
				try {
					zoneBodyBitmap = ImageManager.getInstance().loadBitmapFromAssets(mCandiPatchPresenter.getStyleTextureBodyZone());
				}
				catch (ProxibaseException exception) {
					Exceptions.Handle(exception);
				}
				((ImageView) candiInfoView.findViewById(R.id.img_public)).setImageBitmap(zoneBodyBitmap);
				((ImageView) candiInfoView.findViewById(R.id.img_public_reflection)).setImageBitmap(null);

				ImageRequest imageRequest = new ImageRequest(candiModel.getBodyImageUri(), ImageShape.Square, candiModel.getBodyImageFormat(),
						candiModel.getEntityProxy().javascriptEnabled,
						CandiConstants.IMAGE_WIDTH_MAX, true, true, true, 1, this, new ImageRequestListener() {

							@Override
							public void onImageReady(final Bitmap bitmap) {
								if (bitmap != null) {
									runOnUiThread(new Runnable() {

										@Override
										public void run() {
											((ImageView) candiInfoView.findViewById(R.id.img_public)).setImageBitmap(bitmap);
											Bitmap bitmapReflection = ImageManager.getInstance().getImage(
													candiModel.getBodyImageUri() + ".reflection");
											if (bitmapReflection != null)
												((ImageView) candiInfoView.findViewById(R.id.img_public_reflection)).setImageBitmap(bitmapReflection);

											Animation animation = AnimationUtils.loadAnimation(CandiSearchActivity.this, R.anim.fade_in_medium);
											animation.setFillEnabled(true);
											animation.setFillAfter(true);
											((ImageView) findViewById(R.id.img_public)).startAnimation(animation);
											((ImageView) findViewById(R.id.img_public_reflection)).startAnimation(animation);
										}
									});
								}
							}
						});

				Logger.v(this, "Fetching Image: " + candiModel.getBodyImageUri());
				ImageManager.getInstance().getImageLoader().fetchImage(imageRequest);
			}
		}

		((TextView) candiInfoView.findViewById(R.id.txt_subtitle)).setText("");
		((TextView) candiInfoView.findViewById(R.id.txt_content)).setText("");

		((TextView) candiInfoView.findViewById(R.id.txt_title)).setText(candiModel.getEntityProxy().title);
		if (candiModel.getEntityProxy().subtitle != null)
			((TextView) candiInfoView.findViewById(R.id.txt_subtitle)).setText(Html.fromHtml(candiModel.getEntityProxy().subtitle));
		if (candiModel.getEntityProxy().description != null)
			((TextView) candiInfoView.findViewById(R.id.txt_content)).setText(Html.fromHtml(candiModel.getEntityProxy().description));

		return candiInfoView;
	}

	private void setImageView(String imageUri, final ImageView imageView) {
		if (imageUri != null && !imageUri.equals("")) {
			if (imageUri.toLowerCase().contains("resource:user_placeholder")) {
				Bitmap bitmap = ImageManager.getInstance().loadBitmapFromResources(R.drawable.user_placeholder);
				imageView.setImageBitmap(bitmap);
				Animation animation = AnimationUtils.loadAnimation(CandiSearchActivity.this, R.anim.fade_in_long);
				animation.setFillEnabled(true);
				animation.setFillAfter(true);
				imageView.startAnimation(animation);
			}
			else if (ImageManager.getInstance().hasImage(imageUri)) {
				Bitmap bitmap = ImageManager.getInstance().getImage(imageUri);
				imageView.setImageBitmap(bitmap);
				Animation animation = AnimationUtils.loadAnimation(CandiSearchActivity.this, R.anim.fade_in_long);
				animation.setFillEnabled(true);
				animation.setFillAfter(true);
				imageView.startAnimation(animation);
			}
			else {
				ImageManager.getInstance().getImageLoader().fetchImageByProfile(ImageProfile.SquareTile, imageUri, new ImageRequestListener() {

					@Override
					public void onImageReady(final Bitmap bitmap) {
						if (bitmap != null) {
							runOnUiThread(new Runnable() {

								@Override
								public void run() {
									imageView.setImageBitmap(bitmap);
									Animation animation = AnimationUtils.loadAnimation(CandiSearchActivity.this, R.anim.fade_in_long);
									animation.setFillEnabled(true);
									animation.setFillAfter(true);
									imageView.startAnimation(animation);
								}
							});
						}
					}
				});

				Logger.v(this, "Fetching user image: " + imageUri);
			}
		}
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

	public void updateCandiBackButton(String backButtonText) {

		boolean visible = (mCandiInfoVisible || !mCandiPatchModel.getCandiRootCurrent().isSuperRoot());

		if (visible) {
			mLogo.setVisibility(View.GONE);
			mContextButton.setText(backButtonText != null ? backButtonText : getString(R.string.search_back_button));
			mContextButton.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					if (mCandiInfoVisible) {
						onInfoViewClick(v);
					}
					else {
						if (mCandiPatchModel.getCandiRootCurrent().getParent() != null)
							mCandiPatchPresenter.navigateModel(mCandiPatchModel.getCandiRootCurrent().getParent(), false);
					}
				}
			});

			if (mContextButtonState == ContextButtonState.Default) {
				mViewSwitcher.setDisplayedChild(1);
			}

			if (mCandiInfoVisible)
				mContextButtonState = ContextButtonState.HideSummary;
			else
				mContextButtonState = ContextButtonState.NavigateBack;
		}
		else {

			mContextButton.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					onHomeClick(v);
				}
			});

			if (mContextButtonState != ContextButtonState.Default) {
				mViewSwitcher.setDisplayedChild(0);
			}
			mContextButtonState = ContextButtonState.Default;
		}
	}

	private TableLayout configureMenus(CandiModel candi, boolean landscape, Context context) {

		Boolean needMoreButton = false;

		if (candi.getEntityProxy().commands == null || candi.getEntityProxy().commands.size() == 0) {
			return null;
		}
		if (candi.getEntityProxy().commands.size() > 6) {
			needMoreButton = true;
		}

		/* Get the table we use for grouping and clear it */
		final TableLayout table = new TableLayout(context);

		/* Make the first row */
		TableRow tableRow = (TableRow) getLayoutInflater().inflate(R.layout.temp_tablerow_commands, null);
		final TableRow.LayoutParams rowLp = new TableRow.LayoutParams();
		rowLp.setMargins(0, 0, 0, 0);
		TableLayout.LayoutParams tableLp;

		/* Loop the streams */
		Integer commandCount = 0;
		RelativeLayout commandButtonContainer;
		for (Command command : candi.getEntityProxy().commands) {
			/*
			 * TODO: This is a temporary hack. The service shouldn't pass commands
			 * that this user doesn't have sufficient permissions for.
			 */
			if (command.name.toLowerCase().contains("edit")) {
				if (candi.getEntityProxy().createdById != null && !candi.getEntityProxy().createdById.toString().equals(mUser.id)) {
					continue;
				}
			}

			/* Make a button and configure it */
			command.entity = candi.getEntityProxy();
			commandButtonContainer = (RelativeLayout) getLayoutInflater().inflate(R.layout.temp_button_command, null);

			final TextView commandButton = (TextView) commandButtonContainer.findViewById(R.id.CommandButton);
			commandButtonContainer.setTag(command);
			if (needMoreButton && commandCount == 5) {
				commandButton.setText("More...");
				commandButton.setTag(command);
			}
			else {
				commandButton.setText(command.labelCustom != null ? command.labelCustom : command.label);
				commandButton.setTag(command);
			}

			/* Add button to row */
			tableRow.addView(commandButtonContainer, rowLp);
			commandCount++;

			/* If we have three in a row then commit it and make a new row */
			int newRow = 2;
			if (landscape) {
				newRow = 4;
			}

			if (commandCount % newRow == 0) {
				tableLp = new TableLayout.LayoutParams();
				tableLp.setMargins(0, 0, 0, 0);
				table.addView(tableRow, tableLp);
				tableRow = (TableRow) getLayoutInflater().inflate(R.layout.temp_tablerow_commands, null);
			}
			else if (commandCount == 6) {
				break;
			}
		}

		/* We might have an uncommited row with stream buttons in it */
		if (commandCount != 3) {
			tableLp = new TableLayout.LayoutParams();
			tableLp.setMargins(0, 3, 0, 3);
			table.addView(tableRow, tableLp);
		}
		return table;
	}

	private View configureActionButtons(Context context) {
		return configureActionButtons(null, context);
	}

	private View configureActionButtons(CandiModel candi, Context context) {

		ViewGroup viewGroup = null;

		if (candi == null) {
			viewGroup = new LinearLayout(context);
			Button commandButton = (Button) getLayoutInflater().inflate(R.layout.temp_actionstrip_button, null);
			commandButton.setText("New");
			Drawable icon = getResources().getDrawable(R.drawable.icon_new2_dark);
			icon.setBounds(0, 0, 30, 30);
			commandButton.setCompoundDrawables(null, icon, null, null);
			Command command = new Command();
			command.verb = "new";
			command.name = "aircandi.newcandi";
			command.type = "action";
			command.handler = "dialog.new";
			commandButton.setTag(command);
			viewGroup.addView(commandButton);

			Button commandButtonCandi = (Button) getLayoutInflater().inflate(R.layout.temp_actionstrip_button, null);
			commandButtonCandi.setText("Candi");
			icon = getResources().getDrawable(R.drawable.logo5_dark);
			icon.setBounds(0, 0, 30, 30);
			commandButtonCandi.setCompoundDrawables(null, icon, null, null);

			Command commandCandi = new Command();
			commandCandi.verb = "view";
			commandCandi.name = "aircandi.candi.view";
			commandCandi.type = "action";
			commandCandi.handler = "CandiList";
			commandButtonCandi.setTag(commandCandi);

			viewGroup.addView(commandButtonCandi);

		}
		else {

			if (candi.getEntityProxy().commands == null || candi.getEntityProxy().commands.size() == 0) {
				return null;
			}
			/* Get the table we use for grouping and clear it */
			viewGroup = new LinearLayout(context);

			/* Loop the commands */
			for (Command command : candi.getEntityProxy().commands) {
				/*
				 * TODO: This is a temporary hack. The service shouldn't pass commands
				 * that this user doesn't have sufficient permissions for.
				 */
				if (command.name.toLowerCase().contains("edit")) {
					if (candi.getEntityProxy().createdById != null && !candi.getEntityProxy().createdById.toString().equals(mUser.id)) {
						continue;
					}
				}

				/* Make a button and configure it */
				command.entity = candi.getEntityProxy();
				Button commandButton = (Button) getLayoutInflater().inflate(R.layout.temp_actionstrip_button, null);
				commandButton.setText(command.label);

				Drawable icon = getResources().getDrawable(R.drawable.icon_new_dark);
				if (command.name.toLowerCase().contains("edit")) {
					icon = getResources().getDrawable(R.drawable.icon_edit_dark);
				}

				icon.setBounds(0, 0, 30, 30);
				commandButton.setCompoundDrawables(null, icon, null, null);

				commandButton.setTag(command);
				viewGroup.addView(commandButton);
			}
		}

		return viewGroup;
	}

	class BuildMenuTask extends AsyncTask<FrameLayout, Void, TableLayout> {

		FrameLayout	frame;

		@Override
		protected TableLayout doInBackground(FrameLayout... params) {

			/* We are on the background thread */
			frame = params[0];
			boolean landscape = false;
			Display getOrient = getWindowManager().getDefaultDisplay();
			if (getOrient.getOrientation() == Surface.ROTATION_90 || getOrient.getOrientation() == Surface.ROTATION_270)
				landscape = true;
			TableLayout table;
			try {
				table = configureMenus(mCandiPatchModel.getCandiModelSelected(), landscape, CandiSearchActivity.this);
				return table;
			}
			catch (Exception exception) {
				exception.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(TableLayout table) {

			/* We are on the UI thread */
			super.onPostExecute(table);
			FrameLayout frame = (FrameLayout) findViewById(R.id.frame_menu);
			frame.removeAllViews();
			if (table != null) {
				frame.addView(table);
			}
		}
	}

	public ScreenOrientation getScreenOrientation() {
		return mScreenOrientation;
	}

	public void setScreenOrientation(ScreenOrientation screenOrientation) {
		mScreenOrientation = screenOrientation;
	}

	class PackageReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(final Context context, Intent intent) {

			/* This is on the main UI thread */
			if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)) {
				String publicName = mProxiHandlerManager.getPublicName(intent.getData().getEncodedSchemeSpecificPart());
				if (publicName != null) {
					ImageUtils.showToastNotification(CandiSearchActivity.this, publicName + getText(R.string.dialog_install_toast_package_installed),
							Toast.LENGTH_SHORT);
				}
			}
		}
	}

	private void checkEntityHandler() {
	// EntityHandler entityHandler = candiModel.getEntityProxy().entityHandler;
	// boolean startedProxiHandler = mProxiHandlerManager.startProxiHandler(entityHandler.action,
	// candiModel.getEntityProxy());
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
	// mProxiAppManager.startProxiHandler("com.aircandi.intent.action.SHOWEntityProxy",
	// candiModel.getEntityProxy());
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

	private void verifyConnectivity(final IConnectivityReadyListener listener) {

		if (!NetworkManager.getInstance().isWifiEnabled() && !ProxiExplorer.getInstance().isUsingEmulator()) {

			showNetworkDialog(true, getString(R.string.dialog_network_message_wifi_notready));
			final Button retryButton = (Button) findViewById(R.id.retry_button);
			final TextView txtMessage = (TextView) findViewById(R.id.retry_message);
			retryButton.setEnabled(false);

			NetworkManager.getInstance().setConnectivityListener(new IConnectivityListener() {

				@Override
				public void onConnectivityStateChanged(State networkInfoState) {
				}

				@Override
				public void onWifiStateChanged(int wifiState) {

					if (wifiState == WifiManager.WIFI_STATE_ENABLED)
					{
						ImageUtils.showToastNotification(CandiSearchActivity.this, "Wifi state enabled.", Toast.LENGTH_SHORT);
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
									verifyConnectivity(listener);

								}
							});

						}
					}
					else if (wifiState == WifiManager.WIFI_STATE_ENABLING) {
						ImageUtils.showToastNotification(CandiSearchActivity.this, "Wifi state enabling...", Toast.LENGTH_SHORT);
					}
					else if (wifiState == WifiManager.WIFI_STATE_DISABLING) {
						ImageUtils.showToastNotification(CandiSearchActivity.this, "Wifi state disabling...", Toast.LENGTH_SHORT);
					}
					else if (wifiState == WifiManager.WIFI_STATE_DISABLED) {
						((CheckBox) findViewById(R.id.wifi_enabled_checkbox)).setChecked(false);
						txtMessage.setText(getString(R.string.dialog_network_message_wifi_notready));
						retryButton.setEnabled(false);
						ImageUtils.showToastNotification(CandiSearchActivity.this, "Wifi state disabled.", Toast.LENGTH_SHORT);
					}
				}
			});
		}
		else if (!NetworkManager.getInstance().isConnected()) {
			/*
			 * For better feedback we show the progress indicator for a few seconds
			 * so it feels like we tried.
			 */
			showNetworkDialog(false, "");
			mCandiPatchPresenter.mProgressSprite.setVisible(true);
			final Button retryButton = (Button) findViewById(R.id.retry_button);
			final TextView txtMessage = (TextView) findViewById(R.id.retry_message);
			mHandler.postDelayed(new Runnable() {

				public void run() {
					NetworkManager.getInstance().setConnectivityListener(new IConnectivityListener() {

						@Override
						public void onConnectivityStateChanged(State networkInfoState) {
							if (networkInfoState == State.CONNECTED)
							{
								if (((View) findViewById(R.id.retry_dialog)).getVisibility() == View.VISIBLE) {
									txtMessage.setText(getString(R.string.dialog_network_message_connection_ready));
									retryButton.setEnabled(true);
									retryButton.setOnClickListener(new OnClickListener() {

										@Override
										public void onClick(View v) {
											/* Re-enter so we get to the next stage */
											NetworkManager.getInstance().setConnectivityListener(null);
											showNetworkDialog(false, "");
											verifyConnectivity(listener);
										}
									});
								}
							}
							else if (networkInfoState == State.DISCONNECTED)
							{
								txtMessage.setText(getString(R.string.dialog_network_message_connection_notready));
								retryButton.setEnabled(false);
								ImageUtils.showToastNotification(CandiSearchActivity.this, "Network disconnected.", Toast.LENGTH_SHORT);
							}
						}

						@Override
						public void onWifiStateChanged(int wifiState) {
							if (wifiState == WifiManager.WIFI_STATE_ENABLING) {
								ImageUtils.showToastNotification(CandiSearchActivity.this, "Wifi state enabling...", Toast.LENGTH_SHORT);
							}
							else if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
								ImageUtils.showToastNotification(CandiSearchActivity.this, "Wifi state enabled.", Toast.LENGTH_SHORT);
							}
							else if (wifiState == WifiManager.WIFI_STATE_DISABLING) {
								ImageUtils.showToastNotification(CandiSearchActivity.this, "Wifi state disabling...", Toast.LENGTH_SHORT);
							}
							else if (wifiState == WifiManager.WIFI_STATE_DISABLED) {
								ImageUtils.showToastNotification(CandiSearchActivity.this, "Wifi state disabled.", Toast.LENGTH_SHORT);
							}
						}
					});
					if (NetworkManager.getInstance().isConnected()) {
						NetworkManager.getInstance().setConnectivityListener(null);
						showNetworkDialog(false, "");
						if (listener != null) {
							Logger.d(CandiSearchActivity.this, "Connectivity verified");
							listener.onConnectivityReady();
						}
					}
					retryButton.setEnabled(false);
					showNetworkDialog(true, getString(R.string.dialog_network_message_connection_notready));
				}
			}, CandiConstants.NETWORK_INTERVAL_PHONEY);
		}
		else if (mUser == null) {

			try {
				/* Keep user signed in */
				String username = Aircandi.settings.getString(Preferences.PREF_USERNAME, null);
				if (username == null) {
					username = "anonymous@3meters.com";
					mUser = ProxibaseService.getInstance().loadUser(username);
					if (mUser != null) {
						mUser.anonymous = true;
					}
				}
				else {
					mUser = ProxibaseService.getInstance().loadUser(username);
					if (mUser != null) {
						Toast.makeText(this, "Signed in as " + mUser.fullname, Toast.LENGTH_SHORT).show();
					}
					else {
						Logger.d(this, "Error resigning in: Previous user does not exist: " + username);
						username = "anonymous@3meters.com";
						mUser = ProxibaseService.getInstance().loadUser(username);
						if (mUser != null) {
							mUser.anonymous = true;
							Aircandi.settingsEditor.putString(Preferences.PREF_USERNAME, null);
							Aircandi.settingsEditor.putString(Preferences.PREF_PASSWORD, null);
							Aircandi.settingsEditor.commit();
						}
					}
				}
			}
			catch (ProxibaseException exception) {
				exception.printStackTrace();
			}

			if (mUser != null) {
				if (findViewById(R.id.img_user) != null) {
					setImageView(mUser.imageUri, (ImageView) findViewById(R.id.img_user));
				}
				ProxiExplorer.getInstance().setUser(mUser);
				if (listener != null) {
					Logger.d(this, "Connectivity and service verified");
					listener.onConnectivityReady();
				}
			}
			else {
				showNetworkDialog(true, getString(R.string.dialog_network_message_service_notready));
				final Button retryButton = (Button) findViewById(R.id.retry_button);
				retryButton.setEnabled(true);
			}
		}
		else {
			if (listener != null) {
				Logger.d(this, "Connectivity and service verified");
				listener.onConnectivityReady();
			}
		}
	}

	private void showNetworkDialog(boolean visible, String message) {

		if (visible) {
			TextView txtMessage = (TextView) findViewById(R.id.retry_message);
			CheckBox enableWifiCheckBox = (CheckBox) findViewById(R.id.wifi_enabled_checkbox);
			Button retryButton = (Button) findViewById(R.id.retry_button);

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

	private void showInstallDialog(final CandiModel candi) {

		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				final Dialog dialog = new Dialog(CandiSearchActivity.this, R.style.aircandi_theme_dialog);
				final RelativeLayout installDialog = (RelativeLayout) getLayoutInflater().inflate(R.layout.temp_dialog_install, null);
				dialog.setContentView(installDialog, new FrameLayout.LayoutParams(400, 300, Gravity.CENTER));
				dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
				dialog.getWindow().setBackgroundDrawable(getResources().getDrawable(R.drawable.dialog_bg));

				((Button) installDialog.findViewById(R.id.btn_install_ok)).setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						Intent goToMarket = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(candi.getEntityProxy().entityHandler.code));
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

		runOnUiThread(new Runnable() {

			@Override
			public void run() {

				final CharSequence[] items = {
												getResources().getString(R.string.dialog_new_album),
												getResources().getString(R.string.dialog_new_topic),
												getResources().getString(R.string.dialog_new_web) };
				AlertDialog.Builder builder = new AlertDialog.Builder(CandiSearchActivity.this);
				builder.setTitle(getResources().getString(R.string.dialog_new_message));
				builder.setCancelable(true);
				builder.setNegativeButton(getResources().getString(R.string.dialog_new_negative), new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {}
				});
				builder.setItems(items, new DialogInterface.OnClickListener() {

					public void onClick(final DialogInterface dialog, int item) {
						final Command command = new Command();
						command.verb = "new";
						String message = null;
						if (item == 0) {
							message = getString(R.string.dialog_new_album_signin);
							mUserSignedInRunnable = new Runnable() {

								@Override
								public void run() {
									Logger.i(CandiSearchActivity.this, "Starting Gallery activity");
									Intent intent = buildIntent(null, 0, command, ProxiExplorer.getInstance().getStrongestBeacon(), mUser,
											AlbumForm.class);
									startActivityForResult(intent, CandiConstants.ACTIVITY_ENTITY_HANDLER);
									dialog.dismiss();
									mUserSignedInRunnable = null;
								}
							};
						}
						else if (item == 1) {
							message = getString(R.string.dialog_new_topic_signin);
							mUserSignedInRunnable = new Runnable() {

								@Override
								public void run() {
									Logger.i(CandiSearchActivity.this, "Starting Topic activity");
									Intent intent = buildIntent(null, 0, command, ProxiExplorer.getInstance().getStrongestBeacon(), mUser,
											ForumForm.class);
									startActivityForResult(intent, CandiConstants.ACTIVITY_ENTITY_HANDLER);
									dialog.dismiss();
									mUserSignedInRunnable = null;
								}
							};
						}
						if (item == 2) {
							message = getString(R.string.dialog_new_web_signin);
							mUserSignedInRunnable = new Runnable() {

								@Override
								public void run() {
									Logger.i(CandiSearchActivity.this, "Starting Web activity");
									Intent intent = buildIntent(null, 0, command, ProxiExplorer.getInstance().getStrongestBeacon(), mUser,
											WebForm.class);
									startActivityForResult(intent, CandiConstants.ACTIVITY_ENTITY_HANDLER);
									dialog.dismiss();
									mUserSignedInRunnable = null;
								}
							};
						}
						if (mUser != null && mUser.anonymous) {
							startActivityForResult(new Intent(CandiSearchActivity.this, SignInForm.class).putExtra(getString(R.string.EXTRA_MESSAGE),
									message), CandiConstants.ACTIVITY_SIGNIN);
						}
						else {
							mHandler.post(mUserSignedInRunnable);
						}
					}
				});
				AlertDialog alert = builder.create();
				alert.show();
			}
		});
	}

	// --------------------------------------------------------------------------------------------
	// Graphic engine routines
	// --------------------------------------------------------------------------------------------

	@Override
	public Engine onLoadEngine() {

		Logger.d(this, "Initializing animation engine");

		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		int rotation = getWindowManager().getDefaultDisplay().getOrientation();

		/*
		 * For now, we only support portait orientation. Supporting orientation
		 * changes will likely require some fancy scaling transformation since the
		 * game engine locks on the supplied orientation.
		 */
		mScreenOrientation = ScreenOrientation.PORTRAIT;

		/* Adjusted for density */
		int statusBarHeight = (int) Math.ceil(CandiConstants.ANDROID_STATUSBAR_HEIGHT * dm.density);
		int titleBarHeight = (int) Math.ceil(CandiConstants.CANDI_TITLEBAR_HEIGHT * dm.density);
		int widthPixels = dm.widthPixels;
		int heightPixels = dm.heightPixels;

		if (widthPixels > heightPixels) {
			widthPixels = dm.heightPixels;
			heightPixels = dm.widthPixels;
		}

		Camera camera = new ChaseCamera(0, 0, widthPixels, heightPixels - (statusBarHeight + titleBarHeight)) {

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
	}

	@Override
	public Scene onLoadScene() {
		/*
		 * Called after Create, Resume->LoadEngine.
		 * CandiPatchPresenter handles scene instantiation and setup
		 */
		mCandiPatchPresenter = new CandiPatchPresenter(this, this, mEngine, mRenderSurfaceView, mCandiPatchModel);
		Scene scene = mCandiPatchPresenter.initializeScene();

		mCandiPatchPresenter.mDisplayExtras = mPrefDisplayExtras;
		mCandiPatchModel.addObserver(mCandiPatchPresenter);
		mCandiPatchPresenter.setCandiListener(new ICandiListener() {

			@Override
			public void onSelected(IModel candi) {}

			@Override
			public void onSingleTap(CandiModel candi) {
				CandiSearchActivity.this.doCandiSingleTap(candi);
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
			boolean prefChangeThatRequiresRefresh = loadPreferences();
			loadPreferencesProxiExplorer();

			if (mFirstRun || prefChangeThatRequiresRefresh) {
				Logger.i(this, "Starting first run full beacon scan");
				scanForBeacons(new Options(mFirstRun, false), false);
				mFirstRun = false;
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
		Logger.d(this, hasWindowFocus ? "Activity has window focus" : "Activity lost window focus");

		//if (!mEngine.isRunning()) {
		Logger.d(this, "Passing onWindowFocusChanged to Andengine");
		if (hasWindowFocus && mCandiInfoVisible) {
			mEngine.setBlockReloadTextures(true);
		}
		else {
			mEngine.setBlockReloadTextures(false);
		}
		super.onWindowFocusChanged(hasWindowFocus);
		//}
	}

	// --------------------------------------------------------------------------------------------
	// Candi Paging routines
	// --------------------------------------------------------------------------------------------

	private class CandiPagerAdapter extends PagerAdapter implements ViewPagerIndicator.PageInfoProvider {

		@Override
		public int getCount() {
			if (mCandiPatchModel.getCandiModelSelected() == null) {
				return 0;
			}
			else {
				return mCandiPatchModel.getCandiModelSelected().hasVisibleChildrenCurrent() ? 2 : 1;
			}
		}

		@Override
		public void startUpdate(View arg0) {}

		@Override
		public Object instantiateItem(View collection, int position) {

			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			if (position == PagerView.CandiInfo.ordinal()) {
				RelativeLayout candiInfoView = (RelativeLayout) inflater.inflate(R.layout.temp_candi_info, null);
				candiInfoView = buildCandiInfo(mCandiPatchModel.getCandiModelSelected(), candiInfoView);
				//mSliderInfo = (SlidingDrawer) candiInfoView.findViewById(R.id.slide_actions_info);
				((ViewPager) collection).addView(candiInfoView, 0);
				return candiInfoView;
			}
			else if (position == PagerView.CandiList.ordinal()) {
				View candiInfoView = (View) inflater.inflate(R.layout.temp_candi_list, null);
				ListView candiListView = (ListView) candiInfoView.findViewById(R.id.list_candi_children);
				ListAdapter adapter = new ListAdapter(CandiSearchActivity.this, R.id.item_content, mCandiPatchModel.getCandiModelSelected()
						.getChildren());
				candiListView.setAdapter(adapter);
				candiListView.setClickable(true);
				((ViewPager) collection).addView(candiInfoView, 0);
				return candiInfoView;
			}
			return null;
		}

		public int getItemPosition(Object object) {
			return POSITION_NONE;
		}

		@Override
		public void destroyItem(View collection, int position, Object view) {
			((ViewPager) collection).removeView((View) view);
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == ((View) object);
		}

		@Override
		public void finishUpdate(View arg0) {}

		@Override
		public Parcelable saveState() {
			return null;
		}

		@Override
		public void restoreState(Parcelable state, ClassLoader loader) {}

		@Override
		public String getTitle(int position) {

			String entityType = null;
			if (mCandiPatchModel != null && mCandiPatchModel.getCandiModelSelected() != null) {
				entityType = mCandiPatchModel.getCandiModelSelected().getEntityProxy().entityType;
				if (position == PagerView.CandiInfo.ordinal()) {
					if (entityType.equals(CandiConstants.TYPE_CANDI_ALBUM)) {
						return "GALLERY";
					}
					else if (entityType.equals(CandiConstants.TYPE_CANDI_FORUM)) {
						return "TOPIC";
					}
					else if (entityType.equals(CandiConstants.TYPE_CANDI_WEB)) {
						return "BOOKMARK";
					}
				}
				else if (position == PagerView.CandiList.ordinal()) {
					if (entityType.equals(CandiConstants.TYPE_CANDI_ALBUM)) {
						return "PHOTOS";
					}
					else if (entityType.equals(CandiConstants.TYPE_CANDI_FORUM)) {
						return "POSTS";

					}
				}
			}
			return null;
		}
	}

	public class ListAdapter extends ArrayAdapter<IModel> {

		@Override
		public IModel getItem(int position) {
			return items.get(position);
		}

		private List<IModel>	items;

		public ListAdapter(Context context, int textViewResourceId, List<IModel> items) {
			super(context, textViewResourceId, items);
			this.items = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			ViewHolder holder;
			CandiModel itemData = (CandiModel) items.get(position);

			if (view == null) {
				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = inflater.inflate(R.layout.temp_listitem_twoline_icon, null);
				holder = new ViewHolder();
				holder.itemIcon = (ImageView) view.findViewById(R.id.item_image);
				holder.itemTitle = (TextView) view.findViewById(R.id.item_title);
				holder.itemSubtitle = (TextView) view.findViewById(R.id.item_subtitle);
				holder.itemBody = (TextView) view.findViewById(R.id.item_content);
				holder.itemActionButton = (View) view.findViewById(R.id.item_action_button);
				view.setTag(holder);
			}
			else {
				holder = (ViewHolder) view.getTag();
			}

			if (itemData != null) {
				EntityProxy entityProxy = (EntityProxy) itemData.getEntityProxy();
				holder.data = itemData;
				if (holder.itemTitle != null) {
					if (entityProxy.title != null && !entityProxy.title.equals("")) {
						holder.itemTitle.setText(entityProxy.title);
					}
					else {
						holder.itemTitle.setVisibility(View.GONE);
					}
				}

				if (holder.itemSubtitle != null) {
					if (entityProxy.subtitle != null && !entityProxy.subtitle.equals("")) {
						holder.itemSubtitle.setText(entityProxy.subtitle);
					}
					else {
						holder.itemSubtitle.setVisibility(View.GONE);
					}
				}

				if (holder.itemBody != null) {
					if (entityProxy.description != null && !entityProxy.description.equals("")) {
						holder.itemBody.setText(entityProxy.description);
					}
					else {
						holder.itemBody.setVisibility(View.GONE);
					}
					// if (itemData.getEntityProxy().createdDate != "") {
					// Date hookupDate = DateUtils.wcfToDate(itemData.getEntityProxy().createdDate);
					// holder.itemBody.setText("Created " + DateUtils.intervalSince(hookupDate, DateUtils.nowDate()));
					// }
					// else
				}

				if (holder.itemIcon != null) {
					Bitmap bitmap = ImageManager.getInstance().getImage(itemData.getEntityProxy().imageUri);
					if (bitmap != null) {
						holder.itemIcon.setImageBitmap(bitmap);
					}
					else {
						// new GetFacebookImageTask().execute(holder); // Will set the picture when finished
					}
				}

				/* Loop the streams */
				boolean activeCommand = false;
				for (Command command : entityProxy.commands) {
					if (command.name.toLowerCase().contains("edit")) {
						if (entityProxy.createdById != null && entityProxy.createdById.toString().equals(mUser.id)) {
							activeCommand = true;
							command.entity = entityProxy;
						}
					}
					else {
						activeCommand = true;
						command.entity = entityProxy;
					}
				}
				if (!activeCommand) {
					holder.itemActionButton.setVisibility(View.GONE);
				}
				else {
					holder.itemActionButton.setVisibility(View.VISIBLE);
					holder.itemActionButton.setTag(itemData);
				}
			}
			return view;
		}

		public boolean areAllItemsEnabled() {
			return false;
		}

		public boolean isEnabled(int position) {
			return false;
		}

	}

	private class ViewHolder {

		public ImageView	itemIcon;
		public TextView		itemTitle;
		public TextView		itemSubtitle;
		public TextView		itemBody;
		public View			itemActionButton;
		public Object		data;
	}

	// --------------------------------------------------------------------------------------------
	// System callbacks
	// --------------------------------------------------------------------------------------------

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
		 * Game engine is started/restarted in super class if we currently have the window focus.
		 */
		Logger.i(this, "CandiSearchActivity resumed");

		ProxiExplorer.getInstance().onResume();
		NetworkManager.getInstance().onResume();

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
			Logger.i(this, "CandiSearchActivity paused");

			unregisterReceiver(mPackageReceiver);

			ProxiExplorer.getInstance().onPause();
			NetworkManager.getInstance().onPause();

			stopTitlebarProgress();
		}
		catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	@Override
	protected void onStop() {
		super.onStop();

		/* Fired when we lose our window. Follows onPause(). */
		if (CandiConstants.DEBUG_TRACE) {
			Debug.stopMethodTracing();
		}
		ImageManager.getInstance().getImageLoader().getImageCache().cleanCacheAsync(getApplicationContext());
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		/* Don't count on this always getting called when this activity is killed */
		try {
			Logger.i(this, "CandiSearchActivity destroyed");
			ProxiExplorer.getInstance().onDestroy();
			ImageManager.getInstance().getImageLoader().stopThread();
			ImageManager.getInstance().getImageLoader().getImageCache().cleanCacheAsync(getApplicationContext());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		/*
		 * Called before onResume. If we are returning from the market app, we
		 * get a zero result code whether the user decided to start an install
		 * or not.
		 */
		Logger.i(this, "Activity result returned to CandiSearchActivity");
		startTitlebarProgress(true);
		if (requestCode == CandiConstants.ACTIVITY_SIGNIN) {
			if (data != null) {
				Bundle extras = data.getExtras();
				if (extras != null) {
					String json = extras.getString(getString(R.string.EXTRA_USER));
					if (json != null && !json.equals("")) {
						mUser = ProxibaseService.getGson(GsonType.Internal).fromJson(json, User.class);
						if (mUser != null) {
							if (findViewById(R.id.img_user) != null) {
								setImageView(mUser.imageUri, (ImageView) findViewById(R.id.img_user));
							}
							Aircandi.settingsEditor.putString(Preferences.PREF_USERNAME, mUser.email);
							Aircandi.settingsEditor.putString(Preferences.PREF_PASSWORD, mUser.password);
							Aircandi.settingsEditor.commit();

							if (mUserSignedInRunnable != null) {
								mHandler.post(mUserSignedInRunnable);
							}
						}
					}
				}
			}
		}
		else if (requestCode == CandiConstants.ACTIVITY_PROFILE) {
			if (resultCode == Activity.RESULT_FIRST_USER) {
				if (data != null) {
					Bundle extras = data.getExtras();
					if (extras != null) {
						String json = extras.getString(getString(R.string.EXTRA_USER));
						if (json != null && !json.equals("")) {
							mUser = ProxibaseService.getGson(GsonType.Internal).fromJson(json, User.class);
							if (mUser != null) {
								if (findViewById(R.id.img_user) != null) {
									setImageView(mUser.imageUri, (ImageView) findViewById(R.id.img_user));
								}
								Aircandi.settingsEditor.putString(Preferences.PREF_USERNAME, mUser.email);
								if (mUser.password != null && mUser.password.length() != 0) {
									Aircandi.settingsEditor.putString(Preferences.PREF_PASSWORD, mUser.password);
								}
								Aircandi.settingsEditor.commit();
							}
						}
					}
				}
			}
		}
		else if (requestCode == CandiConstants.ACTIVITY_ENTITY_HANDLER) {
			if (resultCode == Activity.RESULT_FIRST_USER) {
				if (data != null) {
					Bundle extras = data.getExtras();
					if (extras != null) {
						Verb resultVerb = (Verb) extras.get(getString(R.string.EXTRA_RESULT_VERB));
						String dirtyBeaconId = extras.getString(getString(R.string.EXTRA_BEACON_DIRTY));
						Integer dirtyEntityId = extras.getInt(getString(R.string.EXTRA_ENTITY_DIRTY));

						/* New top level type was inserted: discussion, album, website */
						if (dirtyBeaconId != null && !dirtyBeaconId.equals("")) {
							for (Beacon beacon : ProxiExplorer.getInstance().getBeacons()) {
								if (beacon.id.equals(dirtyBeaconId)) {
									beacon.isDirty = true;
									List<EntityProxy> freshEntityProxies = null;
									try {
										freshEntityProxies = ProxiExplorer.getInstance().refreshDirtyEntities();
									}
									catch (ProxibaseException exception) {
										if (exception.getErrorCode() == ProxiErrorCode.NetworkError) {
											/* TODO: What do we want to do when there is a network error? */
										}
									}
									doUpdateEntities(freshEntityProxies, false);
								}
							}
						}

						/* New child type was inserted or any type was edited or deleted */
						else if (dirtyEntityId != null) {
							for (EntityProxy entityProxy : ProxiExplorer.getInstance().getEntityProxiesFlat()) {
								if (entityProxy.id.equals(dirtyEntityId)) {
									entityProxy.isDirty = true;

									List<EntityProxy> freshEntityProxies = null;
									try {
										/* TODO: We aren't going through the code that verifies connectivity */
										freshEntityProxies = ProxiExplorer.getInstance().refreshDirtyEntities();
									}
									catch (ProxibaseException exception) {
										if (exception.getErrorCode() == ProxiErrorCode.NetworkError) {
											/* TODO: What do we want to do when there is a network error? */
										}
									}
									doUpdateEntities(freshEntityProxies, false);

									EntityProxy freshEntityProxy = ProxiExplorer.getInstance().getEntityById(dirtyEntityId);
									if (freshEntityProxy != null) {
										if (mCandiInfoVisible && freshEntityProxy.id
													.equals(mCandiPatchModel.getCandiModelSelected().getEntityProxy().id)) {
											if (resultVerb == Verb.Edit) {
												mCandiPager.getAdapter().notifyDataSetChanged();
												synchronized (mCandiPatchModel.getCandiModelFocused().getViewActions()) {
													mCandiPatchModel.getCandiModelFocused().getViewActions().addFirst(
															new ViewAction(ViewActionType.UpdateTexturesForce));
												}
												mCandiPatchModel.getCandiModelFocused().setChanged();
												mCandiPatchModel.getCandiModelSelected().update();
											}
										}
									}
									else {
										/* We have a delete */
										if (resultVerb == Verb.Delete) {
											/* With rotation, we get the texture loading glitch */
											hideCandiInfo(AnimType.RotateCandi, true);
											mCandiInfoVisible = false;
										}
									}
								}
							}
						}
					}
				}
			}
			else if (resultCode == Activity.RESULT_OK || resultCode == Activity.RESULT_CANCELED) {
			}
		}
		else if (requestCode == CandiConstants.ACTIVITY_MARKET) {
		}
		stopTitlebarProgress();
		return;
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
		if (mCandiInfoVisible) {
			TableLayout table = configureMenus(mCandiPatchModel.getCandiModelFocused(), landscape, CandiSearchActivity.this);
			FrameLayout frame = (FrameLayout) findViewById(R.id.frame_menu);
			frame.removeAllViews();
			frame.addView(table);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Preferences routines
	// --------------------------------------------------------------------------------------------

	private boolean loadPreferences() {
		boolean prefChangeThatRequiresRefresh = false;

		if (mPrefAutoscan != Aircandi.settings.getBoolean(Preferences.PREF_AUTOSCAN, false)) {
			prefChangeThatRequiresRefresh = true;
			mPrefAutoscan = Aircandi.settings.getBoolean(Preferences.PREF_AUTOSCAN, false);
		}
		if (mPrefDisplayExtras != DisplayExtra.valueOf(Aircandi.settings.getString(Preferences.PREF_DISPLAY_EXTRAS, "None"))) {
			prefChangeThatRequiresRefresh = true;
			mPrefDisplayExtras = DisplayExtra.valueOf(Aircandi.settings.getString(Preferences.PREF_DISPLAY_EXTRAS, "None"));
		}
		if (mPrefDemoMode != Aircandi.settings.getBoolean(Preferences.PREF_DEMO_MODE, false)) {
			prefChangeThatRequiresRefresh = true;
			mPrefDemoMode = Aircandi.settings.getBoolean(Preferences.PREF_DEMO_MODE, false);
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

		if (!mPrefTheme.equals(Aircandi.settings.getString(Preferences.PREF_THEME, "aircandi_theme.blueray"))) {
			prefChangeThatRequiresRefresh = false;
			mPrefTheme = Aircandi.settings.getString(Preferences.PREF_THEME, "aircandi_theme.blueray");
			int themeResourceId = this.getResources().getIdentifier(mPrefTheme, "style", "com.proxibase.aircandi");
			this.setTheme(themeResourceId);
			Intent intent = new Intent(this, CandiSearchActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		}
		return prefChangeThatRequiresRefresh;
	}

	private void loadPreferencesProxiExplorer() {
		ProxiExplorer.getInstance().setPrefEntityFencing(mPrefEntityFencing);
		ProxiExplorer.getInstance().setPrefDemoMode(mPrefDemoMode);
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
		if (mUser != null && !mUser.anonymous) {
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
				Intent intent = buildIntent(null, 0, new Command("edit"), null, mUser, ProfileForm.class);
				startActivityForResult(intent, CandiConstants.ACTIVITY_PROFILE);
				return (true);
			case R.id.signinout :
				if (mUser != null && !mUser.anonymous) {
					showProgressDialog("Signing out...");
					try {
						mUser = ProxibaseService.getInstance().loadUser("anonymous@3meters.com");
						mUser.anonymous = true;

						Aircandi.settingsEditor.putString(Preferences.PREF_USERNAME, null);
						Aircandi.settingsEditor.putString(Preferences.PREF_PASSWORD, null);
						Aircandi.settingsEditor.commit();

						if (findViewById(R.id.img_user) != null) {
							setImageView(mUser.imageUri, (ImageView) findViewById(R.id.img_user));
						}
					}
					catch (ProxibaseException exception) {
						exception.printStackTrace();
					}
					finally {
						mProgressDialog.dismiss();
						Toast.makeText(this, "Signed out.", Toast.LENGTH_SHORT).show();
					}
				}
				else {
					mUserSignedInRunnable = null;
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

	private void showProgressDialog(String message) {
		mProgressDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		mProgressDialog.setMessage(message);
		mProgressDialog.setIndeterminate(true);
		mProgressDialog.show();
	}

	private Intent buildIntent(EntityProxy entityProxy, int parentEntityId, Command command, Beacon beacon, User user, Class<?> clazz) {
		Intent intent = new Intent(CandiSearchActivity.this, clazz);

		/* We want to make sure that any child entities don't get serialized */
		Gson gson = new GsonBuilder().setExclusionStrategies(new ExclusionStrategy() {

			@Override
			public boolean shouldSkipClass(Class<?> clazz) {
				return (clazz == (Class<List<EntityProxy>>) (Class<?>) List.class);
			}

			@Override
			public boolean shouldSkipField(FieldAttributes f) {
				return (f.getDeclaredClass() == (Class<List<EntityProxy>>) (Class<?>) List.class);
			}
		}).create();

		if (command != null) {
			String jsonCommand = gson.toJson(command);
			intent.putExtra(getString(R.string.EXTRA_COMMAND), jsonCommand);
		}

		if (parentEntityId != 0) {
			intent.putExtra(getString(R.string.EXTRA_PARENT_ENTITY_ID), parentEntityId);
		}

		if (beacon != null) {
			String jsonBeacon = gson.toJson(beacon);
			if (!jsonBeacon.equals("")) {
				intent.putExtra(getString(R.string.EXTRA_BEACON), jsonBeacon);
			}
		}

		if (entityProxy != null) {
			String jsonEntityProxy = gson.toJson(entityProxy);
			if (!jsonEntityProxy.equals("")) {
				intent.putExtra(getString(R.string.EXTRA_ENTITY), jsonEntityProxy);
			}
		}

		if (user != null) {
			String jsonUser = ProxibaseService.getGson(GsonType.Internal).toJson(user);
			if (!jsonUser.equals("")) {
				intent.putExtra(getString(R.string.EXTRA_USER), jsonUser);
			}
		}

		return intent;
	}

	private void recycleBitmaps() {
		recycleImageViewDrawable(R.id.img_public);
		recycleImageViewDrawable(R.id.img_public_reflection);
	}

	private void recycleImageViewDrawable(int resourceId) {
		ImageView imageView = ((ImageView) findViewById(resourceId));
		if (imageView.getDrawable() != null) {
			BitmapDrawable bitmapDrawable = (BitmapDrawable) imageView.getDrawable();
			if (bitmapDrawable != null && bitmapDrawable.getBitmap() != null) {
				bitmapDrawable.getBitmap().recycle();
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Misc classes/interfaces/enums
	// --------------------------------------------------------------------------------------------

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
		CandiList
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