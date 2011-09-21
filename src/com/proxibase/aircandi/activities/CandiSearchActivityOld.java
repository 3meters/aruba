package com.proxibase.aircandi.activities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Animation.AnimationListener;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import android.widget.AdapterView.OnItemClickListener;

import com.amazonaws.auth.BasicAWSCredentials;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.proxibase.aircandi.activities.AircandiActivity.Verb;
import com.proxibase.aircandi.candi.camera.ChaseCamera;
import com.proxibase.aircandi.candi.models.CandiModel;
import com.proxibase.aircandi.candi.models.CandiPatchModel;
import com.proxibase.aircandi.candi.models.IModel;
import com.proxibase.aircandi.candi.models.CandiModel.DisplayExtra;
import com.proxibase.aircandi.candi.presenters.CandiPatchPresenter;
import com.proxibase.aircandi.candi.presenters.CandiPatchPresenter.ICandiListener;
import com.proxibase.aircandi.candi.presenters.CandiPatchPresenter.TextureReset;
import com.proxibase.aircandi.candi.views.CandiView;
import com.proxibase.aircandi.candi.views.ZoneView;
import com.proxibase.aircandi.core.AircandiException;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.models.BaseEntity.SubType;
import com.proxibase.aircandi.utils.AnimUtils;
import com.proxibase.aircandi.utils.CandiList;
import com.proxibase.aircandi.utils.ImageCache;
import com.proxibase.aircandi.utils.ImageManager;
import com.proxibase.aircandi.utils.ImageUtils;
import com.proxibase.aircandi.utils.ProxiHandlerManager;
import com.proxibase.aircandi.utils.Rotate3dAnimation;
import com.proxibase.aircandi.utils.Utilities;
import com.proxibase.aircandi.utils.ImageManager.IImageReadyListener;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest;
import com.proxibase.aircandi.widgets.HorizontalListView;
import com.proxibase.sdk.android.proxi.consumer.Beacon;
import com.proxibase.sdk.android.proxi.consumer.Command;
import com.proxibase.sdk.android.proxi.consumer.EntityProxy;
import com.proxibase.sdk.android.proxi.consumer.ProxiExplorer;
import com.proxibase.sdk.android.proxi.consumer.User;
import com.proxibase.sdk.android.proxi.consumer.ProxiExplorer.IEntityProcessListener;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.Query;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException.ProxiErrorCode;

/*
 * Texture Notes
 * 
 * - Textures are loaded into hardware using bitmaps (one or more texture sources).
 * 
 * - Textures are unloaded from hardware when the activity loses its window (anything that
 * 	 switches away from this activity and triggers onPause).
 * 
 * - Textures are reloaded into hardware when the activity regains its windows. The bitmap
 *   used as the texture source is used again but won't fail even if the bitmap has
 *   been recycled. The andengine code recycles bitmaps by default once they have been used
 *   to load a texture into hardware. The sprites just show up as invisible.
 *   
 * - To simplify the reloading of textures, we allow the engine to try and use the recyled 
 *   bitmap but intervene to detect that the bitmap has been recycled and pull it again from
 *   the file cache. We have disabled the memory cache since the hardware is acting like our
 *   memory cache. If the bitmap isn't in the file cache, we return null to the engine and
 *   start the async process to fetch the bitmap and create a new BitmapTextureSource.
 *   
 * - Texture behavior: There is hesitation when transitioning from candi detail to candi
 *   search after returning from another activity because all the textures are getting reloaded
 *   from the file cache. It might be possible to improve this if performance work to control 
 *   how textures are reloaded could prioritize textures that are currently visible to the camera.
 */

/*
 * Library Notes
 * 
 * - AWS: We'd like to link to s3 and core libraries to reduce our footprint but there
 * 	 is a known issue in the AWS SDK for Android 2.1. Check next release to see if we can
 *   make the switch. We could also do the work to call AWS without their libraries which
 *   should give us the biggest savings.
 *   
 * - Gson and Guava: We could reduce our size by making the libraries included with Android
 *   work instead of pulling in Gson (which in turn has a dependency on Guava).
 */

/*
 * Threading Notes
 * 
 * - AsyncTasks: AsyncTask uses a static internal work queue with a hard-coded limit of 10
 *   elements. Once we have 10 tasks going concurrently, task 11 causes a RejectedExecutionException.
 *   
 *   ThreadPoolExecutor is a way to get more control over thread pooling but it requires Android 
 *   version 11/3.0 (we currently target 7/2.1 and higher).
 *   
 *   AsyncTasks are hard-coded with a low priority and continue their work even if the activity 
 *   is paused.
 */

@SuppressWarnings("unused")
public class CandiSearchActivityOld extends AircandiGameActivity {

	public enum AnimType {
		Fade, CrossFade, CrossFadeFlipper, RotateScene, RotateCandi
	}

	public enum FlipperView {
		CandiSearch, CandiInfoOne, CandiInfoTwo
	}

	private static String				COMPONENT_NAME			= "CandiSearch";

	private Boolean						mPrefAutoscan			= false;
	private int							mPrefAutoscanInterval	= 5000;
	private boolean						mPrefDemoMode			= false;
	private DisplayExtra				mPrefDisplayExtras		= DisplayExtra.None;
	private boolean						mPrefEntityFencing		= true;
	private boolean						mPrefShowMemory			= false;
	private boolean						mPrefSoundEffects		= true;

	private Boolean						mReadyToRun				= false;
	private Handler						mHandler				= new Handler();
	private Boolean						mCredentialsFound		= false;
	public static BasicAWSCredentials	mAwsCredentials			= null;

	private List<EntityProxy>			mProxiEntities;
	private List<EntityProxy>			mProxiEntitiesFlat;
	private List<TopicItem>				mTopics					= new ArrayList<TopicItem>();
	private ProxiHandlerManager			mProxiHandlerManager;

	private CandiPatchModel				mCandiPatchModel;
	private CandiPatchPresenter			mCandiPatchPresenter;

	protected ImageView					mProgressIndicator;
	protected ImageView					mButtonRefresh;
	private RenderSurfaceView			mCandiSurfaceView;
	private RelativeLayout				mCandiInfoViewOne;
	private RelativeLayout				mCandiInfoViewTwo;
	private RelativeLayout				mCandiInfoViewCurrent;
	public WebView						mWebView;

	private ViewFlipper					mCandiFlipper;
	private FlipperView					mCandiFlipperCurrentView;

	private DisplayMetrics				mDisplayMetrics;

	private boolean						mCandiInfoVisible		= false;
	private boolean						mCandiActivityActive	= false;
	private AnimType					mAnimTypeCandiInfo		= AnimType.RotateCandi;
	private Sound						mCandiAlertSound;
	private Rotate3dAnimation			mRotate3dAnimation;
	private boolean						mFirstRun				= true;
	private ScreenOrientation			mScreenOrientation		= ScreenOrientation.PORTRAIT;
	private PackageReceiver				mPackageReceiver		= new PackageReceiver();
	private boolean						mIgnoreInput			= false;
	private boolean						mUsingEmulator			= false;

	private boolean						mUserRefusedWifiEnable	= false;
	private int							mRenderMode;
	protected User						mUser;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, "Activity.onCreate called");
		super.onCreate(savedInstanceState);
		initialize();
	}

	@Override
	protected void onSetContentView() {
		super.setContentView(getLayoutID());

		mRenderSurfaceView = (RenderSurfaceView) findViewById(getRenderSurfaceViewID());
		mRenderSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // To support transparency
		mRenderSurfaceView.setRenderer(mEngine);

		// Use a surface format with an Alpha channel
		mRenderSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);

		// Make sure the surface view is on top otherwise it a hole through anything above it in the zorder.
		mRenderSurfaceView.setZOrderOnTop(true);

		// TODO: Rendering only when dirty is more battery efficient
		mRenderSurfaceView.setRenderMode(mRenderMode = RenderSurfaceView.RENDERMODE_CONTINUOUSLY);
	}

	private void initialize() {

		// Start normal processing
		mReadyToRun = false;

		// User
		mUser = loadUser("Jay Massena");

		// Ui Hookup
		mCandiFlipper = (ViewFlipper) findViewById(R.id.flipper_candi);
		mCandiFlipperCurrentView = FlipperView.CandiInfoOne;

		mWebView = (WebView) findViewById(R.id.webview);
		mWebView.getSettings().setUserAgentString(CandiConstants.USER_AGENT);
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.setBackgroundColor(0x00000000); // Transparent

		mCandiInfoViewOne = (RelativeLayout) findViewById(R.id.view_candi_info);
		mCandiInfoViewOne.setFocusable(true);
		mCandiInfoViewTwo = (RelativeLayout) findViewById(R.id.view_candi_info_two);
		mCandiInfoViewTwo.setFocusable(true);

		// Check for emulator
		if (Build.PRODUCT.equals("google_sdk") || Build.PRODUCT.equals("sdk"))
			mUsingEmulator = true;

		// Proxibase sdk components
		ProxiExplorer.getInstance().setContext(this.getApplicationContext());
		ProxiExplorer.getInstance().setUsingEmulator(mUsingEmulator);
		ProxiExplorer.getInstance().setUser(mUser);
		ProxiExplorer.getInstance().initialize();
		if (!ProxiExplorer.getInstance().isUsingEmulator()) {
			if (!ProxiExplorer.getInstance().isWifiEnabled()) {
				wifiAskToEnable();
				if (mUserRefusedWifiEnable)
					return;
			}
		}

		// Display metrics
		mDisplayMetrics = getResources().getDisplayMetrics();

		// AWS Credentials
		startGetCredentials();

		// Image cache
		ImageManager.getInstance().setImageCache(new ImageCache(getApplicationContext(), CandiConstants.PATH_IMAGECACHE, 100, 16));
		ImageManager.getInstance().getImageCache().setFileCacheOnly(true);
		ImageManager.getInstance().setWebView(mWebView);
		ImageManager.getInstance().setContext(getApplicationContext());

		// Candi patch
		mCandiPatchModel = new CandiPatchModel();

		// Proxi activities
		mProxiHandlerManager = new ProxiHandlerManager(this);

		// Property settings get overridden once we retrieve preferences
		mCandiSurfaceView = (RenderSurfaceView) findViewById(R.id.view_rendersurface);
		mCandiSurfaceView.requestFocus();
		mCandiSurfaceView.setFocusableInTouchMode(true);

		// Animation
		mRotate3dAnimation = new Rotate3dAnimation();

		// Memory info
		updateMemoryUsed();

		mReadyToRun = true;
	}

	private User loadUser(String userName) {
		try {
			Query query = new Query("Users").filter("Name eq '" + userName + "'");
			String jsonResponse = (String) ProxibaseService.getInstance().selectUsingQuery(query, CandiConstants.URL_AIRCANDI_SERVICE_ODATA);
			mUser = (User) ProxibaseService.convertJsonToObject(jsonResponse, User.class);
			return mUser;
		}
		catch (ProxibaseException exception) {
			// TODO Auto-generated catch block
			exception.printStackTrace();
		}
		return null;
	}

	// --------------------------------------------------------------------------------------------
	// EVENT routines
	// --------------------------------------------------------------------------------------------

	public void onCommandButtonClick(View view) {

		Command command = (Command) view.getTag();
		String commandHandler = "com.proxibase.aircandi.activities." + command.handler;
		String commandName = command.name.toLowerCase();

		if (command.type.toLowerCase().equals("list")) {
			ImageUtils.showToastNotification(this, "Unimplemented...", Toast.LENGTH_SHORT);
			return;
		}

		try {

			if (command.verb.toLowerCase().equals("edit")) {
				EntityProxy entityProxy = mCandiPatchModel.getCandiModelSelected().getEntityProxy();
				SubType subType = entityProxy.entityType.equals(CandiConstants.TYPE_CANDI_TOPIC) ? SubType.Topic : SubType.Comment;
				Intent intent = buildIntent(Verb.Edit, entityProxy, subType, 0, null, mUser, Post.class);
				startActivityForResult(intent, CandiConstants.ACTIVITY_ENTITY_HANDLER);
				overridePendingTransition(R.anim.fade_in_medium, R.anim.hold);
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
				Class clazz = Class.forName(commandHandler, false, this.getClass().getClassLoader());
				Intent intent = new Intent(this, clazz);

				GsonBuilder gsonb = new GsonBuilder();
				gsonb.setExclusionStrategies(new ExclusionStrategy() {

					@Override
					public boolean shouldSkipClass(Class<?> clazz) {
						return (clazz == (Class<List<EntityProxy>>) (Class<?>) List.class);
					}

					@Override
					public boolean shouldSkipField(FieldAttributes f) {
						return (f.getDeclaredClass() == (Class<List<EntityProxy>>) (Class<?>) List.class);
					}
				});

				Gson gson = gsonb.create();

				String jsonCommand = ProxibaseService.getGson(GsonType.Internal).toJson(command);

				// beacon has a circular ref back to entity proxy so fails when using internal
				String jsonEntityProxy = gson.toJson(mCandiPatchModel.getCandiModelSelected().getEntityProxy());
				String jsonUser = ProxibaseService.getGson(GsonType.Internal).toJson(mUser);
				intent.putExtra("Command", jsonCommand);
				intent.putExtra("EntityProxy", jsonEntityProxy);
				intent.putExtra("User", jsonUser);

				startActivityForResult(intent, CandiConstants.ACTIVITY_ENTITY_HANDLER);
				overridePendingTransition(R.anim.fade_in_medium, R.anim.hold);
			}
		}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
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

	@Override
	public void onBackPressed() {
		if (!mCandiInfoVisible) {
			if (mCandiPatchModel.getCandiRootCurrent().getParent() != null) {
				mCandiPatchPresenter.navigateModel(mCandiPatchModel.getCandiRootCurrent().getParent());
			}
		}
		else {
			hideCandiInfo(AnimType.RotateCandi);
			mCandiInfoVisible = false;
		}
	}

	public void onRefreshClick(View view) {
		// For this activity, refresh means rescan and reload entity data from the service
		if (mReadyToRun) {
			doRefresh();
		}
		updateMemoryUsed();
	}

	private void doRefresh() {
		// For this activity, refresh means rescan and reload entity data from the service
		CandiView candiView = (CandiView) mCandiPatchPresenter.getViewForModel(mCandiPatchModel.getCandiModelFocused());
		if (candiView != null) {
			candiView.loadBodyTextureSources(false, true);
		}
		scanForBeacons(false, true);
	}

	public void onInfoViewClick(View v) {
		hideCandiInfo(AnimType.RotateCandi);
		mCandiInfoVisible = false;
	}

	public void onHomeClick(View view) {
		Intent intent = new Intent(this, CandiSearchActivityOld.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	public void onSearchClick(View view) {

		// Scene shift
		ImageUtils.showToastNotification(this, "Shifting scene...", Toast.LENGTH_SHORT);

		// Logging
		IEntity layer = mEngine.getScene().getChild(CandiConstants.LAYER_CANDI);
		Utilities.Log(CandiConstants.APP_NAME, "CandiPatchPresenter", "Current Position for scene: " + String.valueOf(layer.getX())
																		+ ","
																		+ String.valueOf(layer.getY()));
		int childCount = layer.getChildCount();
		for (int i = childCount - 1; i >= 0; i--) {
			IEntity child = layer.getChild(i);
			if (child instanceof CandiView) {

				child.setPosition(child.getX() + 100, child.getY());

				// TODO: Should we null this so the GC can collect them.
				final CandiView candiView = (CandiView) child;
				Utilities.Log(CandiConstants.APP_NAME, "CandiPatchPresenter", "Current Position for " + candiView.getModel().getTitleText()
																				+ ": "
																				+ String.valueOf(candiView.getX())
																				+ ","
																				+ String.valueOf(candiView.getY()));

			}
			if (child instanceof ZoneView) {
				child.setPosition(child.getX() + 100, child.getY());
			}
		}

		layer = mEngine.getScene().getChild(CandiConstants.LAYER_ZONES);
		childCount = layer.getChildCount();
		for (int i = childCount - 1; i >= 0; i--) {
			IEntity child = layer.getChild(i);
			if (child instanceof ZoneView) {
				child.setPosition(child.getX() + 100, child.getY());
			}
		}

		ImageUtils.showToastNotification(this, "Debug output has been logged...", Toast.LENGTH_SHORT);
		return;
	}

	public void onNewCandiClick(View view) {
		showTopicDialog();
	}

	// --------------------------------------------------------------------------------------------
	// ENTITY routines
	// --------------------------------------------------------------------------------------------

	private void scanForBeacons(final Boolean fullUpdate, Boolean showProgress) {

		if (showProgress)
			startTitlebarProgress(true);

		if (fullUpdate && mCandiPatchPresenter != null) {
			mCandiPatchPresenter.setFullUpdateInProgress(true);
			mCandiPatchPresenter.mProgressSprite.setVisible(true);
		}

		Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, "Calling Proxibase.ProxiExplorer to scan for tags");

		ProxiExplorer.getInstance().scanForBeaconsAsync(fullUpdate, new IEntityProcessListener() {

			@Override
			public void onComplete(List<EntityProxy> entities) {

				Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, "Beacon scan results returned from Proxibase.ProxiExplorer");

				if (entities.size() == 0) {
					showTopicDialog();
				}
				else {
					doEntitiesUpdate(entities, fullUpdate);

					// Check for rookies and play a sound
					if (mPrefSoundEffects)
						for (CandiModel candiModel : mCandiPatchModel.getCandiModels())
							if (candiModel.isRookie() && candiModel.isVisibleNext()) {
								mCandiAlertSound.play();
								break;
							}
				}

				// Wrap-up
				updateMemoryUsed();
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
							scanForBeacons(false, false);
						}
					}, mPrefAutoscanInterval);
				}
			}

			public void onIOException(IOException exception) {
				Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, exception.getMessage());
				ImageUtils.showToastNotification(CandiSearchActivityOld.this, "Network error", Toast.LENGTH_SHORT);
				stopTitlebarProgress();
				exception.printStackTrace();
			}

			@Override
			public void onProxiExplorerError(ProxibaseException exception) {
				ImageUtils.showToastNotification(CandiSearchActivityOld.this, exception.getMessage(), Toast.LENGTH_SHORT);
				Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, exception.getMessage());
				stopTitlebarProgress();
			}
		});
	}

	private void updateMemoryUsed() {
		if (mPrefShowMemory) {
			TextView textView = (TextView) findViewById(R.id.txt_footer);
			if (textView != null) {
				float usedMegs = (float) ((float) Debug.getNativeHeapAllocatedSize() / 1048576f);
				String usedMegsString = String.format("Memory Used: %2.2f MB", usedMegs);
				textView.setText(usedMegsString);
			}
		}
	}

	private void refreshEntities(Boolean showProgress) {

		if (showProgress)
			startTitlebarProgress(true);

		Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, "Calling Proxibase.ProxiExplorer to refresh entities");

		ProxiExplorer.getInstance().refreshEntitiesAsync(new IEntityProcessListener() {

			@Override
			public void onComplete(List<EntityProxy> entities) {

				Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, "Entity refresh results returned from Proxibase.ProxiExplorer");

				doEntitiesUpdate(entities, false);

				// Check for rookies and play a sound
				if (mPrefSoundEffects)
					for (CandiModel candiModel : mCandiPatchModel.getCandiModels())
						if (candiModel.isRookie() && candiModel.isVisibleNext()) {
							mCandiAlertSound.play();
							break;
						}

				// Wrap-up
				mCandiPatchPresenter.mProgressSprite.setVisible(false);
				stopTitlebarProgress();
			}

			public void onIOException(IOException exception) {
				Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, exception.getMessage());
				ImageUtils.showToastNotification(CandiSearchActivityOld.this, "Network error", Toast.LENGTH_SHORT);
				stopTitlebarProgress();
				exception.printStackTrace();
			}

			@Override
			public void onProxiExplorerError(ProxibaseException exception) {
				ImageUtils.showToastNotification(CandiSearchActivityOld.this, exception.getMessage(), Toast.LENGTH_SHORT);
				Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, exception.getMessage());
				stopTitlebarProgress();
			}
		});
	}

	private void doEntitiesUpdate(List<EntityProxy> freshEntities, Boolean fullUpdate) {
		// Shallow copy so entities are by value but any object
		// properties like beacon are by ref from the original.
		mProxiEntities = (List<EntityProxy>) ((ArrayList<EntityProxy>) freshEntities).clone();

		// Push the new and updated entities into the system
		mCandiPatchPresenter.updateCandiData(mProxiEntities, fullUpdate);
		ImageManager.getInstance().getImageCache().recycleBitmaps();
	}

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	private void setCandiInfoViewNext(FlipperView flipperView) {
		mCandiFlipperCurrentView = flipperView;
		if (flipperView == FlipperView.CandiInfoOne) {
			mCandiInfoViewCurrent = mCandiInfoViewOne;
		}
		else if (flipperView == FlipperView.CandiInfoTwo) {
			mCandiInfoViewCurrent = mCandiInfoViewTwo;
		}
	}

	private void showCandiInfo(final CandiModel candiModel, AnimType animType) {

		mCandiInfoVisible = true;

		if (animType == AnimType.CrossFadeFlipper) {
			mCandiSurfaceView.setVisibility(View.GONE);
			mCandiFlipper.clearAnimation();
			mCandiFlipper.setInAnimation(this, android.R.anim.slide_in_left);
			mCandiFlipper.setOutAnimation(this, android.R.anim.slide_out_right);
			mCandiFlipper.setDisplayedChild(mCandiFlipperCurrentView.ordinal());
			updateCandiBackButton(null);
			mCandiPatchPresenter.setIgnoreInput(false);
			mIgnoreInput = false;
		}
		else if (animType == AnimType.CrossFade) {
			mCandiInfoViewCurrent.setVisibility(View.VISIBLE);
			mCandiInfoViewCurrent.startAnimation(AnimUtils.loadAnimation(this, R.anim.fade_in_medium));
			hideGLSurfaceView(CandiConstants.DURATION_CANDIINFO_SHOW);
			updateCandiBackButton(null);
			mCandiPatchPresenter.setIgnoreInput(false);
		}
		else if (animType == AnimType.RotateCandi) {

			footerVisible(false);
			float rotationX = mCandiPatchModel.getCandiModelSelected().getZoneCurrent().getCenterX();
			float rotationY = mCandiPatchModel.getCandiModelSelected().getZoneCurrent().getCenterY();
			final float duration = CandiConstants.DURATION_CANDIINFO_SHOW;
			float scaleFrom = 1.0f;
			float scaleTo = 0.5f;
			float alphaFrom = 1.0f;
			float alphaTo = 0.0f;
			final IEntity entity = mAnimTypeCandiInfo == AnimType.RotateCandi ? mCandiPatchPresenter.getViewForModel(mCandiPatchModel
					.getCandiModelSelected()) : mEngine.getScene();

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

					CandiSearchActivityOld.this.runOnUiThread(new Runnable() {

						public void run() {

							/*
							 * mCandiDetailView.setVisibility(View.VISIBLE);
							 * mCandiDetailView.startAnimation(AircandiUI.loadAnimation(CandiSearchActivity.this,
							 * R.anim.fade_in_medium));
							 * hideCandiSurfaceView();
							 * updateCandiBackButton();
							 * mCandiPatchPresenter.setIgnoreInput(false);
							 */

							final float centerX = mCandiFlipper.getWidth() / 2.0f;
							final float centerY = mCandiFlipper.getHeight() / 2.0f;

							mRotate3dAnimation.reset();
							mRotate3dAnimation.configure(270, 360, centerX, centerY, 300.0f, false);
							mRotate3dAnimation.setDuration((long) (duration * 1000));
							mRotate3dAnimation.setFillAfter(true);
							mRotate3dAnimation.setInterpolator(new DecelerateInterpolator(3f));
							mRotate3dAnimation.setAnimationListener(null);

							mCandiFlipper.clearAnimation();
							mCandiFlipper.setDisplayedChild(1);
							mCandiFlipper.startAnimation(mRotate3dAnimation);

							mCandiSurfaceView.setVisibility(View.GONE);
							mCandiInfoViewCurrent.setVisibility(View.VISIBLE);
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

	private void hideCandiInfo(AnimType animType) {

		if (mIgnoreInput)
			return;

		mIgnoreInput = true;
		mCandiPatchPresenter.setIgnoreInput(true);

		if (animType == AnimType.Fade) {
			mCandiInfoViewCurrent.setVisibility(View.GONE);
			mCandiInfoViewCurrent.startAnimation(AnimUtils.loadAnimation(this, R.anim.fade_out_short));
			// mCandiSurfaceView.setVisibility(View.VISIBLE);
			mCandiPatchPresenter.setIgnoreInput(false);
			mIgnoreInput = false;
		}
		else if (animType == AnimType.CrossFadeFlipper) {
			mCandiFlipper.showNext();
			mCandiSurfaceView.setVisibility(View.VISIBLE);
			updateCandiBackButton(!mCandiPatchModel.getCandiRootCurrent().isSuperRoot()	? mCandiPatchModel.getCandiRootCurrent().getTitleText()
																						: null);
			footerVisible(true);
			mCandiPatchPresenter.setIgnoreInput(false);
			mIgnoreInput = false;
		}
		else if (animType == AnimType.CrossFade) {

			mCandiInfoViewCurrent.setVisibility(View.GONE);
			mCandiInfoViewCurrent.startAnimation(AnimUtils.loadAnimation(this, R.anim.fade_out_short));
			showGLSurfaceView(CandiConstants.DURATION_CANDIINFO_HIDE);
			mEngine.getScene().clearEntityModifiers();
			mEngine.getScene()
					.registerEntityModifier(new AlphaModifier(CandiConstants.DURATION_CANDIINFO_HIDE, 0.0f, 1.0f, EaseLinear.getInstance()));
			mCandiInfoVisible = false;
			mCandiPatchModel.setCandiModelSelected(null);
			updateCandiBackButton(!mCandiPatchModel.getCandiRootCurrent().isSuperRoot()	? mCandiPatchModel.getCandiRootCurrent().getTitleText()
																						: null);
			mCandiPatchPresenter.setIgnoreInput(false);
			footerVisible(true);
			CandiSearchActivityOld.this.mIgnoreInput = false;
		}
		else {

			// Find the center of the container
			final float duration = CandiConstants.DURATION_CANDIINFO_HIDE;
			final float centerX = mCandiFlipper.getWidth() / 2.0f;
			final float centerY = mCandiFlipper.getHeight() / 2.0f;

			// Create a new 3D rotation with the supplied parameter
			// The animation listener is used to trigger the next animation
			mRotate3dAnimation.reset();
			mRotate3dAnimation.configure(360, 270, centerX, centerY, 300.0f, true);
			mRotate3dAnimation.setDuration((long) (duration * 1000));
			mRotate3dAnimation.setFillAfter(true);
			mRotate3dAnimation.setInterpolator(new AccelerateInterpolator());
			mCandiFlipper.clearAnimation();
			mCandiFlipper.startAnimation(mRotate3dAnimation);

			mRotate3dAnimation.setAnimationListener(new AnimationListener() {

				@Override
				public void onAnimationEnd(Animation animation) {

					CandiSearchActivityOld.this.runOnUiThread(new Runnable() {

						@Override
						public void run() {

							mCandiFlipper.clearAnimation();
							mCandiInfoViewCurrent.setVisibility(View.GONE);
							mCandiSurfaceView.setVisibility(View.VISIBLE);
							footerVisible(true);

							float rotationX = mCandiPatchModel.getCandiModelFocused().getZoneCurrent().getCenterX();
							float rotationY = mCandiPatchModel.getCandiModelFocused().getZoneCurrent().getCenterY();
							float scaleFrom = 0.5f;
							float scaleTo = 1.0f;
							float alphaFrom = 0.0f;
							float alphaTo = 1.0f;

							IEntity entity = mEngine.getScene();

							if (mAnimTypeCandiInfo == AnimType.RotateCandi) {
								entity = mCandiPatchPresenter.getViewForModel(mCandiPatchModel.getCandiModelFocused());
								rotationX = (CandiConstants.CANDI_VIEW_WIDTH * 0.5f);
								rotationY = (CandiConstants.CANDI_VIEW_BODY_HEIGHT * 0.5f);
								scaleFrom = 1.3f;
								alphaFrom = 0.5f;
								mEngine.getScene().registerEntityModifier(new AlphaModifier(duration, 0.0f, 1.0f, EaseLinear.getInstance()));
							}

							// Using a scaling modifier is tricky to reverse without shifting coordinates
							entity.registerEntityModifier(new ParallelEntityModifier(new RotationAtModifier(duration, 90f, 0.0f, rotationX,
									rotationY, EaseCubicOut.getInstance()), new AlphaModifier(duration, alphaFrom, alphaTo, EaseCircularOut
									.getInstance())));

							updateCandiBackButton(!mCandiPatchModel.getCandiRootCurrent().isSuperRoot() ? mCandiPatchModel.getCandiRootCurrent()
									.getTitleText() : null);
							mCandiPatchModel.setCandiModelSelected(null);
							mCandiPatchPresenter.setIgnoreInput(false);
							CandiSearchActivityOld.this.mIgnoreInput = false;
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
	}

	private void buildCandiInfo(final CandiModel candiModel) {

		final HorizontalListView candiList = (HorizontalListView) mCandiInfoViewCurrent.findViewById(R.id.list_candi_children);
		candiList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				
				CandiModel candiModel = (CandiModel) candiList.getAdapter().getItem(position);
				mCandiPatchModel.setCandiModelSelected(candiModel);
				
				setCandiInfoViewNext(mCandiFlipperCurrentView == FlipperView.CandiInfoOne ? FlipperView.CandiInfoTwo : FlipperView.CandiInfoOne);
				buildCandiInfo(candiModel);
				showCandiInfo(candiModel, AnimType.CrossFadeFlipper);
			}
		});

		new BuildMenuTask().execute(mCandiInfoViewCurrent);

		if (candiModel.hasVisibleChildrenCurrent()) {

			//((View) mCandiInfoViewCurrent.findViewById(R.id.list_rule)).setVisibility(View.VISIBLE);
			int paddingInPx = ImageUtils.getRawPixelsForDisplayPixels(mDisplayMetrics, 100);
			((RelativeLayout) mCandiInfoViewCurrent.findViewById(R.id.group_candi_content)).setPadding(0, 0, 0, paddingInPx);
			candiList.setVisibility(View.VISIBLE);
			ListAdapter adapter = new ListAdapter(CandiSearchActivityOld.this, R.id.item_content, candiModel.getChildren());
			candiList.setAdapter(adapter);
			candiList.setClickable(true);
		}
		else {
			((RelativeLayout) mCandiInfoViewCurrent.findViewById(R.id.group_candi_content)).setPadding(0, 0, 0, 0);
			//((View) mCandiInfoViewCurrent.findViewById(R.id.list_rule)).setVisibility(View.GONE);
			candiList.setVisibility(View.GONE);
			candiList.clearAnimation();
		}

		if (candiModel.getEntityProxy().imageUri != null && !candiModel.getEntityProxy().imageUri.equals("")) {
			if (ImageManager.getInstance().hasImage(candiModel.getEntityProxy().imageUri)) {
				Bitmap bitmap = ImageManager.getInstance().getImage(candiModel.getEntityProxy().imageUri);
				((ImageView) mCandiInfoViewCurrent.findViewById(R.id.img_public)).setImageBitmap(bitmap);
				if (ImageManager.getInstance().hasImage(candiModel.getEntityProxy().imageUri + ".reflection")) {
					bitmap = ImageManager.getInstance().getImage(candiModel.getEntityProxy().imageUri + ".reflection");
					((ImageView) mCandiInfoViewCurrent.findViewById(R.id.img_public_reflection)).setImageBitmap(bitmap);
				}
			}
			else {
				Bitmap zoneBodyBitmap = ImageManager.loadBitmapFromAssets("gfx/trans_30.png");
				((ImageView) mCandiInfoViewCurrent.findViewById(R.id.img_public)).setImageBitmap(zoneBodyBitmap);
				((ImageView) mCandiInfoViewCurrent.findViewById(R.id.img_public_reflection)).setImageBitmap(null);
				ImageRequest imageRequest = new ImageManager.ImageRequest();
				imageRequest.imageId = candiModel.getBodyImageId();
				imageRequest.imageUri = candiModel.getBodyImageUri();
				imageRequest.imageFormat = candiModel.getBodyImageFormat();
				imageRequest.imageShape = "square";
				imageRequest.widthMinimum = CandiConstants.IMAGE_WIDTH_MAX;
				imageRequest.showReflection = true;
				imageRequest.imageReadyListener = new IImageReadyListener() {

					@Override
					public void onImageReady(Bitmap bitmap) {
						Bitmap bitmapNew = ImageManager.getInstance().getImage(candiModel.getBodyImageUri());
						if (bitmapNew != null) {
							((ImageView) mCandiInfoViewCurrent.findViewById(R.id.img_public)).setImageBitmap(bitmapNew);
							Bitmap bitmapReflection = ImageManager.getInstance().getImage(candiModel.getBodyImageUri() + ".reflection");
							if (bitmapReflection != null)
								((ImageView) mCandiInfoViewCurrent.findViewById(R.id.img_public_reflection)).setImageBitmap(bitmapReflection);

							Animation animation = AnimationUtils.loadAnimation(CandiSearchActivityOld.this, R.anim.fade_in_medium);
							animation.setFillEnabled(true);
							animation.setFillAfter(true);
							((ImageView) findViewById(R.id.img_public)).startAnimation(animation);
							((ImageView) findViewById(R.id.img_public_reflection)).startAnimation(animation);
						}

					}

					@Override
					public void onProxibaseException(ProxibaseException exception) {
						if (exception.getErrorCode() == ProxiErrorCode.OperationFailed) {
							Bitmap zoneBodyBitmap = ImageManager.loadBitmapFromAssets("gfx/placeholder3.png");
							((ImageView) mCandiInfoViewCurrent.findViewById(R.id.img_public)).setImageBitmap(zoneBodyBitmap);
						}
					}
				};
				Utilities.Log(CandiConstants.APP_NAME, "buildCandiDetailView", "Fetching Image: " + candiModel.getBodyImageUri());
				try {
					ImageManager.getInstance().fetchImageAsynch(imageRequest);
				}
				catch (AircandiException exception) {
					// TODO: We might have hit the thread limit for asynctasks
					exception.printStackTrace();
				}
			}
		}

		((TextView) mCandiInfoViewCurrent.findViewById(R.id.txt_subtitle)).setText("");
		((TextView) mCandiInfoViewCurrent.findViewById(R.id.txt_content)).setText("");

		((TextView) mCandiInfoViewCurrent.findViewById(R.id.txt_title)).setText(candiModel.getEntityProxy().title);
		if (candiModel.getEntityProxy().subtitle != null)
			((TextView) mCandiInfoViewCurrent.findViewById(R.id.txt_subtitle)).setText(Html.fromHtml(candiModel.getEntityProxy().subtitle));
		if (candiModel.getEntityProxy().description != null)
			((TextView) mCandiInfoViewCurrent.findViewById(R.id.txt_content)).setText(Html.fromHtml(candiModel.getEntityProxy().description));
	}

	private void hideGLSurfaceView(float duration) {
		mEngine.getScene().registerEntityModifier(new ParallelEntityModifier(new IEntityModifierListener() {

			@Override
			public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
				CandiSearchActivityOld.this.runOnUiThread(new Runnable() {

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
				CandiSearchActivityOld.this.runOnUiThread(new Runnable() {

					public void run() {
						mCandiSurfaceView.setVisibility(View.VISIBLE);
					}
				});
			}
		}, new AlphaModifier(duration, 0.0f, 1.0f, EaseLinear.getInstance())));
	}

	private void footerVisible(boolean visible) {
		int animationResource = visible ? R.anim.fade_in_medium : R.anim.fade_out_medium;
		Animation animation = AnimationUtils.loadAnimation(CandiSearchActivityOld.this, animationResource);
		animation.setFillEnabled(true);
		animation.setFillAfter(true);
		((LinearLayout) findViewById(R.id.activity_footer)).startAnimation(animation);
	}

	public void updateCandiBackButton(String backButtonText) {

		boolean visible = (mCandiInfoVisible || !mCandiPatchModel.getCandiRootCurrent().isSuperRoot());

		if (visible) {
			mContextButton.setText(backButtonText != null ? backButtonText : getString(R.string.search_back_button));
			mContextButton.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					if (mCandiInfoVisible) {
						onInfoViewClick(v);
					}
					else {
						if (mCandiPatchModel.getCandiRootCurrent().getParent() != null)
							mCandiPatchPresenter.navigateModel(mCandiPatchModel.getCandiRootCurrent().getParent());
					}
				}
			});

			if (mContextButtonState == ContextButtonState.Default) {
				Animation animation = AnimationUtils.loadAnimation(CandiSearchActivityOld.this, R.anim.fade_in_short);
				animation.setFillEnabled(true);
				animation.setFillAfter(true);
				animation.setStartOffset(250);
				mContextButton.startAnimation(animation);
				mContextButton.setVisibility(View.VISIBLE);
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
				Animation animation = AnimationUtils.loadAnimation(CandiSearchActivityOld.this, R.anim.fade_out_short);
				animation.setFillEnabled(true);
				animation.setFillAfter(true);
				mContextButton.startAnimation(animation);
			}
			mContextButtonState = ContextButtonState.Default;
		}
	}

	private void showInstallDialog(final CandiModel candi) {

		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				final Dialog dialog = new Dialog(CandiSearchActivityOld.this, R.style.aircandi_theme_dialog);
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

	private void showTopicDialog() {

		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				final Dialog dialog = new Dialog(CandiSearchActivityOld.this, R.style.aircandi_theme_dialog);
				final RelativeLayout installDialog = (RelativeLayout) getLayoutInflater().inflate(R.layout.temp_dialog_new, null);
				dialog.setContentView(installDialog, new FrameLayout.LayoutParams(dialog.getWindow().getWindowManager().getDefaultDisplay()
						.getWidth() - 40, LayoutParams.FILL_PARENT, Gravity.CENTER));
				dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

				dialog.getWindow().setBackgroundDrawable(getResources().getDrawable(R.drawable.dialog_bg));

				((Button) installDialog.findViewById(R.id.btn_new_topic)).setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {

						Intent intent = buildIntent(Verb.New, null, SubType.Topic, 0, ProxiExplorer.getInstance().getStrongestBeacon(), mUser,
								Post.class);
						startActivityForResult(intent, CandiConstants.ACTIVITY_ENTITY_HANDLER);
						overridePendingTransition(R.anim.fade_in_medium, R.anim.hold);
						dialog.dismiss();
					}
				});

				((Button) installDialog.findViewById(R.id.btn_new_comment)).setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {

						// We always use the first candi in a zone
						EntityProxy parentEntityProxy = mCandiPatchModel.getCandiModelFocused().getZoneCurrent().getCandiesCurrent().get(0)
								.getEntityProxy();
						Intent intent = buildIntent(Verb.New, null, SubType.Comment, parentEntityProxy.id, parentEntityProxy.beacon, mUser,
								Post.class);
						startActivityForResult(intent, CandiConstants.ACTIVITY_ENTITY_HANDLER);
						overridePendingTransition(R.anim.fade_in_medium, R.anim.hold);
						dialog.dismiss();
					}
				});
				dialog.show();

			}
		});
	}

	private void doCandiSingleTap(final CandiModel candiModel) {
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

		// This can get called from threads other than the main UI thread
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				/*
				 * When starting from a candi in search view we always target
				 * the first CandiInfo view in the flipper.
				 */
				mCandiPatchModel.setCandiModelSelected(candiModel);
				setCandiInfoViewNext(FlipperView.CandiInfoOne);
				buildCandiInfo(candiModel);
				showCandiInfo(candiModel, AnimType.RotateCandi);
			}
		});
	}

	private TableLayout configureMenus(CandiModel candi, boolean landscape, Context context) {

		Boolean needMoreButton = false;

		if (candi.getEntityProxy().commands == null || candi.getEntityProxy().commands.size() == 0)
			return null;

		if (candi.getEntityProxy().commands.size() > 6)
			needMoreButton = true;

		// Get the table we use for grouping and clear it
		final TableLayout table = new TableLayout(context);

		// Make the first row
		TableRow tableRow = (TableRow) getLayoutInflater().inflate(R.layout.temp_tablerow_commands, null);
		final TableRow.LayoutParams rowLp = new TableRow.LayoutParams();
		rowLp.setMargins(0, 0, 0, 0);
		TableLayout.LayoutParams tableLp;

		// Loop the streams
		Integer commandCount = 0;
		RelativeLayout commandButtonContainer;
		for (Command command : candi.getEntityProxy().commands) {
			/*
			 * TODO: This is a temporary hack. The service shouldn't pass commands
			 * that this user doesn't have sufficient permissions for.
			 */
			if (command.name.toLowerCase().equals("edit"))
				if (candi.getEntityProxy().createdById != null && !candi.getEntityProxy().createdById.equals(mUser.id))
					continue;

			// Make a button and configure it
			commandButtonContainer = (RelativeLayout) getLayoutInflater().inflate(R.layout.temp_button_command, null);

			final TextView commandButton = (TextView) commandButtonContainer.findViewById(R.id.CommandButton);
			final TextView commandBadge = (TextView) commandButtonContainer.findViewById(R.id.CommandBadge);
			commandButtonContainer.setTag(command);
			if (needMoreButton && commandCount == 5) {
				commandButton.setText("More...");
				commandButton.setTag(command);
			}
			else {
				commandButton.setText(command.label);
				commandButton.setTag(command);
				commandBadge.setTag(command);
				commandBadge.setVisibility(View.INVISIBLE);
			}

			// Add button to row
			tableRow.addView(commandButtonContainer, rowLp);
			commandCount++;

			// If we have three in a row then commit it and make a new row
			int newRow = 2;
			if (landscape)
				newRow = 4;

			if (commandCount % newRow == 0) {
				tableLp = new TableLayout.LayoutParams();
				tableLp.setMargins(0, 0, 0, 0);
				table.addView(tableRow, tableLp);
				tableRow = (TableRow) getLayoutInflater().inflate(R.layout.temp_tablerow_commands, null);
			}
			else if (commandCount == 6)
				break;
		}

		// We might have an uncommited row with stream buttons in it
		if (commandCount != 3) {
			tableLp = new TableLayout.LayoutParams();
			tableLp.setMargins(0, 3, 0, 3);
			table.addView(tableRow, tableLp);
		}
		return table;
	}

	class BuildMenuTask extends AsyncTask<RelativeLayout, Void, TableLayout> {

		RelativeLayout	frame;

		@Override
		protected TableLayout doInBackground(RelativeLayout... params) {

			// We are on the background thread
			frame = params[0];
			boolean landscape = false;
			Display getOrient = getWindowManager().getDefaultDisplay();
			if (getOrient.getOrientation() == Surface.ROTATION_90 || getOrient.getOrientation() == Surface.ROTATION_270)
				landscape = true;
			TableLayout table;
			try {
				table = configureMenus(mCandiPatchModel.getCandiModelSelected(), landscape, CandiSearchActivityOld.this);
				return table;
			}
			catch (Exception exception) {
				exception.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(TableLayout table) {

			// We are on the UI thread
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
		// This is on the main UI thread
		public void onReceive(final Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)) {
				String publicName = mProxiHandlerManager.getPublicName(intent.getData().getEncodedSchemeSpecificPart());
				ImageUtils.showToastNotification(CandiSearchActivityOld.this, publicName + getText(R.string.dialog_install_toast_package_installed),
						Toast.LENGTH_SHORT);
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// ENGINE routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutID() {
		return R.layout.candi_search;
	}

	@Override
	protected int getRenderSurfaceViewID() {
		return R.id.view_rendersurface;
	}

	@Override
	public void onLoadComplete() {
		Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, "Activity.onLoadComplete called");
	}

	@Override
	public Engine onLoadEngine() {

		Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, "onLoadEngine called");
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		int rotation = getWindowManager().getDefaultDisplay().getOrientation();
		if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270)
			mScreenOrientation = ScreenOrientation.LANDSCAPE;
		else
			mScreenOrientation = ScreenOrientation.PORTRAIT;

		Camera camera = new ChaseCamera(0, 0, dm.widthPixels, dm.heightPixels - CandiConstants.CANDI_TITLEBAR_HEIGHT) {

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

		// EngineOptions options = new EngineOptions(true, mScreenOrientation, new RatioResolutionPolicy(dm.widthPixels,
		// dm.heightPixels), camera);
		EngineOptions options = new EngineOptions(true, mScreenOrientation, new FillResolutionPolicy(), camera);
		// options.getRenderOptions().disableExtensionVertexBufferObjects();
		options.setNeedsSound(true);
		Engine engine = new Engine(options);

		return engine;
	}

	@Override
	public void onLoadResources() {
		Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, "onLoadResources called");
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
		Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, "onLoadScene called");
		mCandiPatchPresenter = new CandiPatchPresenter(this, this, mEngine, mRenderSurfaceView, mCandiPatchModel);
		Scene scene = mCandiPatchPresenter.initializeScene();
		mCandiPatchPresenter.mDisplayExtras = mPrefDisplayExtras;
		mCandiPatchModel.addObserver(mCandiPatchPresenter);
		mCandiPatchPresenter.setCandiListener(new ICandiListener() {

			@Override
			public void onSelected(IModel candi) {}

			@Override
			public void onSingleTap(CandiModel candi) {
				CandiSearchActivityOld.this.doCandiSingleTap(candi);
			}
		});

		return scene;
	}

	@Override
	public void onResumeGame() {
		/*
		 * This gets called anytime the game surface gains window focus. The game
		 * engine acquries the wake lock, restarts the engine, resumes the GLSurfaceView.
		 * The engine reloads textures.
		 */
		Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, "Activity.onGameResumed called");
	}

	@Override
	public void onPauseGame() {
		/*
		 * This gets called anytime the game surface loses window focus is
		 * called on the super class. The game engine releases the wake lock, stops the engine, pauses the
		 * GLSurfaceView.
		 */
		Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, "Activity.onGamePaused called");
	}

	@Override
	public void onUnloadResources() {
		super.onUnloadResources();
		Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, "Activity.onUnloadResources called");
	}

	@Override
	public void onWindowFocusChanged(final boolean hasWindowFocus) {
		/*
		 * Parent class will trigger pause or resume for the game engine
		 * based on hasWindowFocus.
		 * :
		 * We control when this message get through to prevent unnecessary
		 * restarts. We block it if we lost focus becase of a pop up like a
		 * dialog or an android menu (which do not trigger this.onPause which
		 * in turns stops the engine).
		 */

		Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, "Activity.onWindowFocusChanged called");

		if (!mEngine.isRunning()) {
			Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, "Passing onWindowFocusChanged to Andengine");
			super.onWindowFocusChanged(hasWindowFocus);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Candi List routines
	// --------------------------------------------------------------------------------------------

	private class ListAdapter extends ArrayAdapter<IModel> {

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
				Utilities.Log(CandiConstants.APP_NAME, "CandiList", "Get view (fresh): " + String.valueOf(position)
																	+ " "
																	+ itemData.getEntityProxy().title);
				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = inflater.inflate(R.layout.temp_listitem, null);

				holder = new ViewHolder();
				holder.itemIcon = (ImageView) view.findViewById(R.id.item_image);
				holder.itemTitle = (TextView) view.findViewById(R.id.item_title);
				holder.itemBody = (TextView) view.findViewById(R.id.item_content);
				view.setTag(holder);
			}
			else {
				Utilities.Log(CandiConstants.APP_NAME, "CandiList", "Get view (recycled): " + String.valueOf(position)
																	+ " "
																	+ itemData.getEntityProxy().title);
				holder = (ViewHolder) view.getTag();
			}

			if (itemData != null) {
				holder.data = itemData;
				if (holder.itemTitle != null)
					holder.itemTitle.setText(itemData.getEntityProxy().title);

				if (holder.itemBody != null) {
					// if (itemData.getEntityProxy().createdDate != "") {
					// Date hookupDate = DateUtils.wcfToDate(itemData.getEntityProxy().createdDate);
					// holder.itemBody.setText("Created " + DateUtils.intervalSince(hookupDate, DateUtils.nowDate()));
					// }
					// else
					holder.itemBody.setText(itemData.getEntityProxy().description);
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
			}
			return view;
		}
	}

	private class ViewHolder {

		public ImageView	itemIcon;
		public TextView		itemTitle;
		public TextView		itemBody;
		public Object		data;
	}

	// --------------------------------------------------------------------------------------------
	// WIFI routines
	// --------------------------------------------------------------------------------------------

	private void wifiAskToEnable() {

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {

				switch (which) {
					case DialogInterface.BUTTON_POSITIVE :
						mUserRefusedWifiEnable = false;
						wifiEnable();
						break;
					case DialogInterface.BUTTON_NEGATIVE :
						mUserRefusedWifiEnable = true;
						break;
				}
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setIcon(R.drawable.icon_alert_dialog).setMessage(R.string.alert_dialog_wifidisabled).setPositiveButton(R.string.alert_dialog_yes,
				dialogClickListener).setNegativeButton(R.string.alert_dialog_no, dialogClickListener).show();
	}

	private void wifiEnable() {
		ImageUtils.showToastNotification(this, "Wifi enabling...", Toast.LENGTH_LONG);
		ProxiExplorer.getInstance().enableWifi();
	}

	// --------------------------------------------------------------------------------------------
	// MISC routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onResume() {
		super.onResume();

		/*
		 * OnResume gets called after OnCreate (always) and whenever the activity is being brought back to the
		 * foreground. Not guaranteed but is usually called just before the activity receives focus.
		 * :
		 * Because code in OnCreate could have determined that we aren't ready to roll, isReadyToRun is used to indicate
		 * that prep work is complete.
		 * :
		 * This is also called when the user jumps out and back from setting preferences
		 * so we need to refresh the places where they get used.
		 * :
		 * Game engine is started/restarted in super class if we currently have the window focus.
		 */

		Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, "Activity.onResume called");

		ProxiExplorer.getInstance().onResume();

		// Package receiver
		IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
		filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
		filter.addAction(Intent.ACTION_PACKAGE_INSTALL);
		filter.addAction(Intent.ACTION_PACKAGE_DATA_CLEARED);
		filter.addDataScheme("package");
		registerReceiver(mPackageReceiver, filter);

		if (mReadyToRun) {
			boolean prefChangeThatRequiresRefresh = loadPreferences();
			loadPreferencesProxiExplorer();

			if (mFirstRun || prefChangeThatRequiresRefresh) {
				scanForBeacons(mFirstRun, false);
				mFirstRun = false;
			}

			if (mCandiPatchModel.getCandiModels().size() > 0) {
				// mEngine.getScene().setAlpha(0);
				// mEngine.getScene().registerEntityModifier(new AlphaModifier(0.5f, 0.0f, 1.0f,
				// EaseCircularIn.getInstance()));
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		/*
		 * Calling onPause on super will cause the engine to pause if it hasn't already been
		 * paused because of losing window focus. This does not get called if the activity window
		 * loses focus but the activity is still active.
		 */

		try {
			Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, "Activity.onPause called");

			unregisterReceiver(mPackageReceiver);

			// if (mEngine.getScene() != null)
			// mEngine.getScene().setAlpha(0);

			ProxiExplorer.getInstance().onPause();

			stopTitlebarProgress();
		}
		catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	@Override
	// Don't count on this always getting called when this activity is killed
	protected void onDestroy() {
		recycleBitmaps();
		super.onDestroy();

		try {
			Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, "Activity.onDestroy called");
			ProxiExplorer.getInstance().onDestroy();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Intent buildIntent(Verb verb, EntityProxy entityProxy, SubType subType, int parentEntityId, Beacon beacon, User user, Class<?> clazz) {
		Intent intent = new Intent(CandiSearchActivityOld.this, clazz);

		// We want to make sure that any child entities don't get serialized
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

		if (verb != null)
			intent.putExtra(getString(R.string.EXTRA_VERB), verb);

		if (subType != null)
			intent.putExtra(getString(R.string.EXTRA_SUBTYPE), subType);

		if (parentEntityId != 0)
			intent.putExtra(getString(R.string.EXTRA_PARENT_ENTITY_ID), parentEntityId);

		if (beacon != null) {
			String jsonBeacon = gson.toJson(beacon);
			if (!jsonBeacon.equals(""))
				intent.putExtra(getString(R.string.EXTRA_BEACON), jsonBeacon);
		}

		if (entityProxy != null) {
			String jsonEntityProxy = gson.toJson(entityProxy);
			if (!jsonEntityProxy.equals(""))
				intent.putExtra(getString(R.string.EXTRA_ENTITY), jsonEntityProxy);
		}

		if (user != null) {
			String jsonUser = ProxibaseService.getGson(GsonType.Internal).toJson(user);
			if (!jsonUser.equals(""))
				intent.putExtra(getString(R.string.EXTRA_USER), jsonUser);
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

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		/*
		 * TODO: Not getting called because setRequestedOrientation() is beinging
		 * called in BaseGameActivity
		 */
		Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, "onConfigurationChanged called: " + newConfig.orientation);

		boolean landscape = false;
		Display getOrient = getWindowManager().getDefaultDisplay();
		if (getOrient.getOrientation() == Surface.ROTATION_90 || getOrient.getOrientation() == Surface.ROTATION_270)
			landscape = true;

		super.onConfigurationChanged(newConfig);
		if (mCandiInfoVisible) {
			TableLayout table = configureMenus(mCandiPatchModel.getCandiModelFocused(), landscape, CandiSearchActivityOld.this);
			FrameLayout frame = (FrameLayout) findViewById(R.id.frame_menu);
			frame.removeAllViews();
			frame.addView(table);
		}
	}

	private boolean loadPreferences() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		boolean prefChangeThatRequiresRefresh = false;

		if (prefs != null) {
			if (mPrefAutoscan != prefs.getBoolean(Preferences.PREF_AUTOSCAN, true)) {
				prefChangeThatRequiresRefresh = true;
				mPrefAutoscan = prefs.getBoolean(Preferences.PREF_AUTOSCAN, true);
			}
			if (mPrefDisplayExtras != DisplayExtra.valueOf(prefs.getString(Preferences.PREF_DISPLAY_EXTRAS, "None"))) {
				prefChangeThatRequiresRefresh = true;
				mPrefDisplayExtras = DisplayExtra.valueOf(prefs.getString(Preferences.PREF_DISPLAY_EXTRAS, "None"));
			}
			if (mPrefDemoMode != prefs.getBoolean(Preferences.PREF_DEMO_MODE, false)) {
				prefChangeThatRequiresRefresh = true;
				mPrefDemoMode = prefs.getBoolean(Preferences.PREF_DEMO_MODE, false);
			}
			if (mPrefEntityFencing != prefs.getBoolean(Preferences.PREF_ENTITY_FENCING, true)) {
				prefChangeThatRequiresRefresh = true;
				mPrefEntityFencing = prefs.getBoolean(Preferences.PREF_ENTITY_FENCING, true);
			}

			mPrefShowMemory = prefs.getBoolean(Preferences.PREF_SHOW_MEMORY, true);
			mPrefSoundEffects = prefs.getBoolean(Preferences.PREF_SOUND_EFFECTS, true);

			if (mCandiPatchPresenter != null)
				mCandiPatchPresenter.mDisplayExtras = mPrefDisplayExtras;

			if (mPrefShowMemory) {
				updateMemoryUsed();
			}
			else {
				TextView textView = (TextView) findViewById(R.id.txt_footer);
				textView.setText("");
			}
		}
		return prefChangeThatRequiresRefresh;
	}

	private void loadPreferencesProxiExplorer() {
		ProxiExplorer.getInstance().setPrefEntityFencing(mPrefEntityFencing);
		ProxiExplorer.getInstance().setPrefDemoMode(mPrefDemoMode);
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
						Log.e("AWS", "Aws Credentials not configured correctly.");
						mCredentialsFound = false;
					}
					else {
						mAwsCredentials = new BasicAWSCredentials(properties.getProperty("accessKey"), properties.getProperty("secretKey"));
						mCredentialsFound = true;
					}
				}
				catch (Exception exception) {
					Log.e("Loading AWS Credentials", exception.getMessage());
					mCredentialsFound = false;
				}
			}
		};
		t.start();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		// Hide the sign out option if we don't have a current session
		MenuItem item = menu.findItem(R.id.signout);
		item.setVisible(false);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// Hide the sign out option if we don't have a current session
		MenuItem item = menu.findItem(R.id.signout);
		item.setVisible(false);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.settings :
				startActivityForResult(new Intent(this, Preferences.class), 0);
				return (true);
			default :
				return (super.onOptionsItemSelected(item));
		}
	}

	public class TopicItem {

		public String	title;
		public String	id;

		public TopicItem(String title, String id) {
			this.title = title;
			this.id = id;
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		/*
		 * Called before onResume. If we are returning from the market app, we
		 * get a zero result code whether the user decided to start an install
		 * or not.
		 */
		startTitlebarProgress(true);
		mCandiSurfaceView.setVisibility(View.VISIBLE);
		if (requestCode == CandiConstants.ACTIVITY_ENTITY_HANDLER) {
			if (resultCode == Activity.RESULT_FIRST_USER) {
				if (data != null) {
					Bundle extras = data.getExtras();
					if (extras != null) {
						Verb resultVerb = (Verb) extras.get(getString(R.string.EXTRA_RESULT_VERB));
						String dirtyBeaconId = extras.getString(getString(R.string.EXTRA_BEACON_DIRTY));
						Integer dirtyEntityId = extras.getInt(getString(R.string.EXTRA_ENTITY_DIRTY));
						/*
						 * New topic was inserted
						 */
						if (dirtyBeaconId != null && !dirtyBeaconId.equals("")) {
							for (Beacon beacon : ProxiExplorer.getInstance().getBeacons()) {
								if (beacon.id.equals(dirtyBeaconId)) {
									beacon.isDirty = true;
									List<EntityProxy> freshEntityProxies = ProxiExplorer.getInstance().refreshEntities();
									doEntitiesUpdate(freshEntityProxies, false);
								}
							}
						}
						/*
						 * New comment was inserted or
						 * comment or topic was edited or deleted
						 */
						else if (dirtyEntityId != null) {
							for (EntityProxy entityProxy : ProxiExplorer.getInstance().getEntityProxiesFlat()) {
								if (entityProxy.id.equals(dirtyEntityId)) {
									entityProxy.isDirty = true;

									List<EntityProxy> freshEntityProxies = ProxiExplorer.getInstance().refreshEntities();
									doEntitiesUpdate(freshEntityProxies, false);

									EntityProxy freshEntityProxy = ProxiExplorer.getInstance().getEntityById(dirtyEntityId);
									if (freshEntityProxy != null) {
										if (mCandiInfoVisible && freshEntityProxy.id
													.equals(mCandiPatchModel.getCandiModelSelected().getEntityProxy().id)) {
											if (resultVerb == Verb.Edit) {
												buildCandiInfo(mCandiPatchModel.getCandiModelSelected());
												CandiView candiView = (CandiView) mCandiPatchPresenter.getViewForModel(mCandiPatchModel
														.getCandiModelSelected());
												candiView.loadBodyTextureSources(false, true);
											}
										}
									}
									else {
										// We have a delete
										if (resultVerb == Verb.Delete) {
											hideCandiInfo(AnimType.CrossFade);
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
}