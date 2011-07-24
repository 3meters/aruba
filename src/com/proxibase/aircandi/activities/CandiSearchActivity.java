package com.proxibase.aircandi.activities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import android.graphics.PixelFormat;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Animation.AnimationListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.proxibase.aircandi.activities.R;
import com.proxibase.aircandi.candi.camera.ChaseCamera;
import com.proxibase.aircandi.candi.models.CandiModel;
import com.proxibase.aircandi.candi.models.CandiPatchModel;
import com.proxibase.aircandi.candi.models.IModel;
import com.proxibase.aircandi.candi.models.CandiModel.DisplayExtra;
import com.proxibase.aircandi.candi.presenters.CandiPatchPresenter;
import com.proxibase.aircandi.candi.presenters.CandiPatchPresenter.ICandiListener;
import com.proxibase.aircandi.candi.presenters.CandiPatchPresenter.TextureReset;
import com.proxibase.aircandi.candi.utils.CandiConstants;
import com.proxibase.aircandi.candi.views.CandiView;
import com.proxibase.aircandi.utils.AircandiUI;
import com.proxibase.aircandi.utils.ImageCache;
import com.proxibase.aircandi.utils.ImageManager;
import com.proxibase.aircandi.utils.ProxiHandlerManager;
import com.proxibase.aircandi.utils.Rotate3dAnimation;
import com.proxibase.aircandi.utils.Utilities;
import com.proxibase.sdk.android.proxi.consumer.Command;
import com.proxibase.sdk.android.proxi.consumer.EntityHandler;
import com.proxibase.sdk.android.proxi.consumer.EntityProxy;
import com.proxibase.sdk.android.proxi.consumer.ProxiExplorer;
import com.proxibase.sdk.android.proxi.consumer.ProxiExplorer.BaseBeaconScanListener;
import com.proxibase.sdk.android.proxi.service.ProxibaseError;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.util.UtilitiesUI;

@SuppressWarnings("unused")
public class CandiSearchActivity extends AircandiGameActivity {

	public enum AnimType {
		CrossFade, RotateScene, RotateCandi
	}

	private static String		COMPONENT_NAME						= "CandiSearch";

	private Boolean				mPrefAutoscan						= true;
	private int					mPrefAutoscanInterval				= 5000;
	private int					mPrefBeaconLevelCutoffUnregistered	= -100;
	private boolean				mPrefEntityFencing					= true;
	private boolean				mPrefBeaconWithEntitiesOnly			= true;
	private DisplayExtra		mPrefDisplayExtras					= DisplayExtra.None;
	private float				mPrefTileScale						= 1.0f;
	private boolean				mPrefTileRotate;
	private boolean				mPrefSoundEffects;

	private Boolean				mReadyToRun							= false;
	private Handler				mHandler							= new Handler();

	private ProxiExplorer		mProxiExplorer;
	private List<EntityProxy>	mProxiEntities;
	private ProxiHandlerManager	mProxiHandlerManager;

	private CandiPatchModel		mCandiPatchModel;
	private CandiPatchPresenter	mCandiPatchPresenter;

	protected ImageView			mProgressIndicator;
	protected ImageView			mButtonRefresh;
	private ViewFlipper			mCandiFlipper;
	private RenderSurfaceView	mCandiSurfaceView;
	private FrameLayout			mCandiDetailView;

	private boolean				mCandiDetailViewVisible				= false;
	private AnimType			mAnimTypeCandiDetail				= AnimType.RotateCandi;
	private Sound				mCandiAlertSound;
	private Rotate3dAnimation	mRotate3dAnimation;
	private boolean				mFirstRun							= true;
	private ScreenOrientation	mScreenOrientation					= ScreenOrientation.PORTRAIT;
	private PackageReceiver		mPackageReceiver					= new PackageReceiver();

	private boolean				mUserRefusedWifiEnable				= false;
	private int					mRenderMode;

	private boolean				mPrefBeaconShowHidden;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, "Activity.onCreate called");
		super.onCreate(savedInstanceState);
		initialize();
	}

	@Override
	protected void onSetContentView() {
		super.setContentView(this.getLayoutID());

		this.mRenderSurfaceView = (RenderSurfaceView) this.findViewById(this.getRenderSurfaceViewID());
		this.mRenderSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // To support transparency
		this.mRenderSurfaceView.setRenderer(this.mEngine);

		// Use a surface format with an Alpha channel
		this.mRenderSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);

		// Make sure the surface view is on top otherwise it a hole through anything above it in the zorder.
		this.mRenderSurfaceView.setZOrderOnTop(true);

		// TODO: Rendering only when dirty is more battery efficient
		this.mRenderSurfaceView.setRenderMode(mRenderMode = RenderSurfaceView.RENDERMODE_CONTINUOUSLY);
	}

	private void initialize() {

		// Start normal processing
		mReadyToRun = false;

		// Image cache
		ImageManager.getImageManager().setImageCache(new ImageCache(getApplicationContext(), CandiConstants.PATH_IMAGECACHE, 100, 16));
		ImageManager.getImageManager().setContext(getApplicationContext());

		// Ui Hookup
		mCandiFlipper = (ViewFlipper) findViewById(R.id.CandiFlipper);
		mCandiFlipper.setInAnimation(this, R.anim.summary_in);
		mCandiFlipper.setOutAnimation(this, R.anim.summary_out);

		mCandiDetailView = (FrameLayout) findViewById(R.id.CandiSummaryView);
		mCandiDetailView.setFocusable(true);

		mProgressIndicator = (ImageView) findViewById(R.id.Application_ProgressIndicator);
		if (mProgressIndicator != null)
			mProgressIndicator.setVisibility(View.INVISIBLE);

		mButtonRefresh = (ImageView) findViewById(R.id.Application_Button_Refresh);
		if (mButtonRefresh != null)
			mButtonRefresh.setVisibility(View.VISIBLE);

		// Proxibase sdk components
		mProxiExplorer = new ProxiExplorer(this);
		if (!mProxiExplorer.isWifiEnabled()) {
			this.wifiAskToEnable();
			if (mUserRefusedWifiEnable)
				return;
		}

		// Candi patch
		mCandiPatchModel = new CandiPatchModel();

		// Proxi activities
		mProxiHandlerManager = new ProxiHandlerManager(this);

		// Property settings get overridden once we retrieve preferences
		mCandiSurfaceView = (RenderSurfaceView) findViewById(R.id.RenderSurfaceView);
		mCandiSurfaceView.requestFocus();
		mCandiSurfaceView.setFocusableInTouchMode(true);

		// Animation
		mRotate3dAnimation = new Rotate3dAnimation();

		// enableOrientationSensor(new IOrientationListener(){
		//
		// @Override
		// public void onOrientationChanged(OrientationData pOrientationData) {
		// Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, "onOrientationChanged called");
		// }});

		mReadyToRun = true;

	}

	// ==========================================================================
	// EVENT routines
	// ==========================================================================

	private void onCommandButtonClick(View view) {

		Command command = (Command) view.getTag();
		String fullyQualifiedClass = "com.proxibase.aircandi.activities." + command.type;
		String commandName = command.name.toLowerCase();

		if (command.type.toLowerCase().equals("list")) {
			AircandiUI.showToastNotification(this, "Unimplemented...", Toast.LENGTH_SHORT);
			return;
		}

		try {
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
				Class clazz = Class.forName(fullyQualifiedClass, false, this.getClass().getClassLoader());
				Intent intent = new Intent(this, clazz);
				String jsonStream = ProxibaseService.getGson(GsonType.ProxibaseService).toJson(command);
				String jsonEntityProxy = ProxibaseService.getGson(GsonType.ProxibaseService).toJson(
						mCandiPatchModel.getCandiModelSelected().getEntityProxy());
				intent.putExtra("Stream", jsonStream);
				intent.putExtra("EntityProxy", jsonEntityProxy);
				startActivity(intent);
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
		if (!mCandiDetailViewVisible) {
			if (mCandiPatchModel.getCandiModelFocused().isGrouped() && mCandiPatchModel.getCandiRootPrevious().isSuperRoot())
				mCandiPatchPresenter.navigateModel(mCandiPatchModel.getCandiRootPrevious(), true);
		}
		else {
			hideCandiDetailView();
			mCandiDetailViewVisible = false;
		}
	}

	public void onRefreshClick(View view) {
		// For this activity, refresh means rescan and reload proxi entity data from the service
		if (mReadyToRun) {
			scanForBeacons(false, true);
		}
	}

	public void onSummaryViewClick(View v) {
		hideCandiDetailView();
		mCandiDetailViewVisible = false;
	}

	public void onHomeClick(View view) {
		Intent intent = new Intent(this, CandiSearchActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	public void onSearchClick(View view) {

		// Logging
		IEntity layer = mEngine.getScene().getChild(CandiConstants.LAYER_CANDI);
		int childCount = layer.getChildCount();
		for (int i = childCount - 1; i >= 0; i--) {
			IEntity child = layer.getChild(i);
			if (child instanceof CandiView) {

				// TODO: Should we null this so the GC can collect them.
				final CandiView candiView = (CandiView) child;
				Utilities.Log(CandiConstants.APP_NAME, "CandiPatchPresenter", "Current Position for " + candiView.getModel().getTitleText()
																				+ ": "
																				+ String.valueOf(candiView.getX())
																				+ ","
																				+ String.valueOf(candiView.getY()));

			}
		}
		AircandiUI.showToastNotification(this, "Debug output has been logged...", Toast.LENGTH_SHORT);
		return;
	}

	// ==========================================================================
	// SCAN AND DISPLAY routines
	// ==========================================================================

	private void scanForBeacons(final Boolean fullUpdate, Boolean showProgress) {

		if (showProgress)
			startTitlebarProgress();

		if (fullUpdate && mCandiPatchPresenter != null) {
			mCandiPatchPresenter.setFullUpdateInProgress(true);
			mCandiPatchPresenter.mProgressSprite.setVisible(true);
		}

		Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, "Calling Proxibase.ProxiExplorer to scan for tags");

		mProxiExplorer.scanForBeacons(fullUpdate, new BaseBeaconScanListener() {

			@Override
			public void onComplete(List<EntityProxy> entities) {

				Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, "Beacon scan results returned from Proxibase.ProxiExplorer");
				Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, "Entity count: " + String.valueOf(entities.size()));

				// try {

				// Scanning is complete so change the heading back to normal
				// Show search message if there aren't any current points
				TextView message = (TextView) findViewById(R.id.Tricorder_Message);
				if (message != null) {
					message.setVisibility(View.GONE);
				}

				// Replace the collection
				Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, "Refreshing RippleView");

				// Shallow copy so the entities are by value but any object
				// properties like beacon are by ref from the original.
				mProxiEntities = (List<EntityProxy>) ((ArrayList<EntityProxy>) entities).clone();
				
				// Special handling for primer entity proxies
				for (EntityProxy entityProxy : mProxiEntities)
					if (entityProxy.entityType.equals("com.proxibase.aircandi.candi.primer")) {
						entityProxy.label = entityProxy.beacon.label;
						entityProxy.title = entityProxy.beacon.label;
						entityProxy.subtitle = "Signal: " + String.valueOf(entityProxy.beacon.levelDb);
					}

				// TODO: Flag beacons this user wants hidden.
				for (EntityProxy entityProxy : mProxiEntities)
					if (entityProxy.beacon.isHidden) {
					}

				// Push the new and updated proxi entities into the system
				mCandiPatchPresenter.updateCandiData(mProxiEntities, fullUpdate);

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

				// Schedule the next wifi scan run
				if (mPrefAutoscan) {
					mHandler.postDelayed(new Runnable() {

						public void run() {
							scanForBeacons(false, false);
						}
					}, mPrefAutoscanInterval);
				}
				// }
				// catch (Exception exception) {
				// AircandiUI.showToastNotification(TricorderAE.this, "Unknown error", Toast.LENGTH_SHORT);
				// }
			}

			public void onIOException(IOException exception) {
				Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, exception.getMessage());
				AircandiUI.showToastNotification(CandiSearchActivity.this, "Network error", Toast.LENGTH_SHORT);
				stopTitlebarProgress();
				exception.printStackTrace();
			}

			@Override
			public void onProxiExplorerError(ProxibaseError error) {
				AircandiUI.showToastNotification(CandiSearchActivity.this, error.getMessage(), Toast.LENGTH_SHORT);
				Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, error.getMessage());
				stopTitlebarProgress();
			}
		});
	}

	private void buildCandiDetailView(final CandiModel candiModel) {

		new BuildMenuTask().execute(mCandiDetailView);

		if (candiModel.getEntityProxy().imageUri != null && candiModel.getEntityProxy().imageUri != "") {
			if (ImageManager.getImageManager().hasImage(candiModel.getEntityProxy().imageUri)) {
				Bitmap bitmap = ImageManager.getImageManager().getImage(candiModel.getEntityProxy().imageUri);
				((ImageView) mCandiDetailView.findViewById(R.id.Image)).setImageBitmap(bitmap);
			}
			if (ImageManager.getImageManager().hasImage(candiModel.getEntityProxy().imageUri + ".reflection")) {
				Bitmap bitmap = ImageManager.getImageManager().getImage(candiModel.getEntityProxy().imageUri + ".reflection");
				((ImageView) mCandiDetailView.findViewById(R.id.ImageReflection)).setImageBitmap(bitmap);
			}
		}

		((TextView) mCandiDetailView.findViewById(R.id.Title)).setText(candiModel.getEntityProxy().title);
		((TextView) mCandiDetailView.findViewById(R.id.Subtitle)).setText(Html.fromHtml(candiModel.getEntityProxy().subtitle));
		((TextView) mCandiDetailView.findViewById(R.id.Description)).setText(Html.fromHtml(candiModel.getEntityProxy().description));
	}

	private void hideCandiSurfaceView() {
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
		}, new AlphaModifier(0.5f, 1.0f, 0.0f, EaseLinear.getInstance())));
	}

	private void showCandiSurfaceView() {
		mCandiSurfaceView.setVisibility(View.VISIBLE);
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
		}, new AlphaModifier(0.5f, 0.0f, 1.0f, EaseLinear.getInstance())));
	}

	private void showCandiDetailView(final CandiModel candiModel) {

		mCandiPatchModel.setCandiModelSelected(candiModel);
		mCandiDetailViewVisible = true;
		buildCandiDetailView(candiModel);

		if (mAnimTypeCandiDetail == AnimType.CrossFade) {
			mCandiDetailView.setVisibility(View.VISIBLE);
			mCandiDetailView.startAnimation(UtilitiesUI.loadAnimation(this, R.anim.fade_in_medium));
			hideCandiSurfaceView();
			updateCandiBackButton();
		}
		else {

			float rotationX = mCandiPatchModel.getCandiModelSelected().getZoneCurrent().getCenterX();
			float rotationY = mCandiPatchModel.getCandiModelSelected().getZoneCurrent().getCenterY();
			final float duration = 0.6f;
			float scaleFrom = 1.0f;
			float scaleTo = 0.5f;
			float alphaFrom = 1.0f;
			float alphaTo = 0.0f;
			IEntity entity = mEngine.getScene();

			if (mAnimTypeCandiDetail == AnimType.RotateCandi) {
				entity = mCandiPatchPresenter.getViewForModel(mCandiPatchModel.getCandiModelSelected());
				rotationX = (CandiConstants.CANDI_VIEW_WIDTH * 0.5f);
				rotationY = (CandiConstants.CANDI_VIEW_BODY_HEIGHT * 0.5f);
				scaleTo = 1.3f;
				alphaTo = 0.5f;
				mEngine.getScene().registerEntityModifier(new AlphaModifier(duration, 1.0f, 0.0f, EaseLinear.getInstance()));
			}

			entity.registerEntityModifier(new ParallelEntityModifier(new IEntityModifierListener() {

				@Override
				public void onModifierFinished(IModifier<IEntity> modifier, final IEntity entityModified) {

					CandiSearchActivity.this.runOnUiThread(new Runnable() {

						public void run() {

							final float centerX = mCandiFlipper.getWidth() / 2.0f;
							final float centerY = mCandiFlipper.getHeight() / 2.0f;

							mRotate3dAnimation.reset();
							mRotate3dAnimation.configure(270, 360, centerX, centerY, 300.0f, false);
							mRotate3dAnimation.setDuration((long) (duration * 1000));
							mRotate3dAnimation.setFillAfter(true);
							mRotate3dAnimation.setInterpolator(new DecelerateInterpolator(3f));
							mRotate3dAnimation.setAnimationListener(null);

							mCandiFlipper.clearAnimation();
							mCandiFlipper.startAnimation(mRotate3dAnimation);

							mCandiSurfaceView.setVisibility(View.GONE);
							mCandiDetailView.setVisibility(View.VISIBLE);
							updateCandiBackButton();
						}
					});

				}

				@Override
				public void onModifierStarted(IModifier<IEntity> modifier, IEntity entity) {}

			}, new RotationAtModifier(duration, 0, 90, rotationX, rotationY, EaseCubicIn.getInstance()), new AlphaModifier(duration, alphaFrom,
					alphaTo, EaseCircularIn.getInstance())));
		}
	}

	private void hideCandiDetailView() {

		if (mAnimTypeCandiDetail == AnimType.CrossFade) {

			mCandiDetailView.setVisibility(View.GONE);
			mCandiDetailView.startAnimation(UtilitiesUI.loadAnimation(this, R.anim.fade_out_medium));
			showCandiSurfaceView();
			mCandiDetailViewVisible = false;
			mCandiPatchModel.setCandiModelSelected(null);
			updateCandiBackButton();
			mCandiPatchPresenter.mIgnoreInput = false;
		}
		else {

			// Find the center of the container
			final float duration = 0.6f;
			final float centerX = mCandiFlipper.getWidth() / 2.0f;
			final float centerY = mCandiFlipper.getHeight() / 2.0f;

			// Create a new 3D rotation with the supplied parameter
			// The animation listener is used to trigger the next animation
			mRotate3dAnimation.reset();
			mRotate3dAnimation.configure(360, 270, centerX, centerY, 300.0f, true);
			mRotate3dAnimation.setDuration((long) (duration * 1000));
			mRotate3dAnimation.setFillAfter(true);
			mRotate3dAnimation.setInterpolator(new AccelerateInterpolator());
			mCandiFlipper.startAnimation(mRotate3dAnimation);

			// final Rotate3dAnimation rotation = new Rotate3dAnimation(360, 270, centerX, centerY, 300.0f, true);
			// rotation.setZAdjustment(Animation.ZORDER_NORMAL);
			// rotation.setDuration((long) (duration * 1000));
			// rotation.setFillAfter(true);
			// rotation.setInterpolator(new AccelerateInterpolator());

			mRotate3dAnimation.setAnimationListener(new AnimationListener() {

				@Override
				public void onAnimationEnd(Animation animation) {

					CandiSearchActivity.this.runOnUiThread(new Runnable() {

						@Override
						public void run() {

							mCandiFlipper.clearAnimation();
							mCandiDetailView.setVisibility(View.GONE);
							mCandiSurfaceView.setVisibility(View.VISIBLE);

							float rotationX = mCandiPatchModel.getCandiModelSelected().getZoneCurrent().getCenterX();
							float rotationY = mCandiPatchModel.getCandiModelSelected().getZoneCurrent().getCenterY();
							float scaleFrom = 0.5f;
							float scaleTo = 1.0f;
							float alphaFrom = 0.0f;
							float alphaTo = 1.0f;

							IEntity entity = mEngine.getScene();

							if (mAnimTypeCandiDetail == AnimType.RotateCandi) {
								entity = mCandiPatchPresenter.getViewForModel(mCandiPatchModel.getCandiModelSelected());
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

							updateCandiBackButton();
							mCandiPatchModel.setCandiModelSelected(null);
							mCandiPatchPresenter.mIgnoreInput = false;
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

	// ==========================================================================
	// SCAN AND DISPLAY routines
	// ==========================================================================

	private void doCandiSingleTap(CandiModel candiModel) {
		EntityHandler entityHandler = candiModel.getEntityProxy().entityHandler;
		boolean startedProxiHandler = mProxiHandlerManager.startProxiHandler(entityHandler.action, candiModel.getEntityProxy());
		if (!startedProxiHandler) {
			if (mProxiHandlerManager.getProxiHandlers().containsKey(entityHandler.action)) {
				EntityHandler proxiHandlerTracked = (EntityHandler) mProxiHandlerManager.getProxiHandlers().get(entityHandler.action);
				if (!proxiHandlerTracked.isSuppressInstallPrompt()) {
					showInstallDialog(candiModel);
					proxiHandlerTracked.setSuppressInstallPrompt(true);
				}
				else {
					// Fall back to our built-in candi viewer
					showCandiDetailView(candiModel);
					// mProxiAppManager.startProxiHandler("com.aircandi.intent.action.SHOWEntityProxy",
					// candiModel.getEntityProxy());
				}
			}
			else {
				showInstallDialog(candiModel);
				entityHandler.setSuppressInstallPrompt(true);
				mProxiHandlerManager.getProxiHandlers().put(entityHandler.action, entityHandler);
			}
		}

	}

	private TableLayout configureMenus(CandiModel candi, boolean landscape, Context context) {

		Boolean needMoreButton = false;
		if (candi.getEntityProxy().commands.size() > 6)
			needMoreButton = true;

		// Get the table we use for grouping and clear it
		final TableLayout table = new TableLayout(context);

		// Make the first row
		TableRow tableRow = (TableRow) this.getLayoutInflater().inflate(R.layout.temp_tablerow_streams, null);
		final TableRow.LayoutParams rowLp = new TableRow.LayoutParams();
		rowLp.setMargins(0, 0, 0, 0);
		TableLayout.LayoutParams tableLp;

		// Loop the streams
		Integer streamCount = 0;
		RelativeLayout streamButtonContainer;
		for (Command command : candi.getEntityProxy().commands) {
			// Make a button and configure it
			streamButtonContainer = (RelativeLayout) this.getLayoutInflater().inflate(R.layout.temp_button_stream, null);

			final TextView streamButton = (TextView) streamButtonContainer.findViewById(R.id.StreamButton);
			final TextView streamBadge = (TextView) streamButtonContainer.findViewById(R.id.StreamBadge);
			streamButtonContainer.setTag(command);
			if (needMoreButton && streamCount == 5) {
				streamButton.setText("More...");
				streamButton.setTag(command);
			}
			else {
				streamButton.setText(command.label);
				streamButton.setTag(command);
				streamBadge.setTag(command);
				streamBadge.setVisibility(View.INVISIBLE);
			}

			// Add button to row
			tableRow.addView(streamButtonContainer, rowLp);
			streamCount++;

			// If we have three in a row then commit it and make a new row
			int newRow = 2;
			if (landscape)
				newRow = 4;

			if (streamCount % newRow == 0) {
				tableLp = new TableLayout.LayoutParams();
				tableLp.setMargins(0, 0, 0, 0);
				table.addView(tableRow, tableLp);
				tableRow = (TableRow) this.getLayoutInflater().inflate(R.layout.temp_tablerow_streams, null);
			}
			else if (streamCount == 6)
				break;
		}

		// We might have an uncommited row with stream buttons in it
		if (streamCount != 3) {
			tableLp = new TableLayout.LayoutParams();
			tableLp.setMargins(0, 3, 0, 3);
			table.addView(tableRow, tableLp);
		}
		return table;
	}

	class BuildMenuTask extends AsyncTask<FrameLayout, Void, TableLayout> {

		FrameLayout	frame;

		@Override
		protected TableLayout doInBackground(FrameLayout... params) {

			// We are on the background thread
			frame = params[0];
			boolean landscape = false;
			Display getOrient = getWindowManager().getDefaultDisplay();
			if (getOrient.getRotation() == Surface.ROTATION_90 || getOrient.getRotation() == Surface.ROTATION_270)
				landscape = true;
			TableLayout table = configureMenus(mCandiPatchModel.getCandiModelSelected(), landscape, CandiSearchActivity.this);

			return table;
		}

		@Override
		protected void onPostExecute(TableLayout table) {

			// We are on the UI thread
			super.onPostExecute(table);
			FrameLayout frame = (FrameLayout) findViewById(R.id.frameMenu);
			frame.removeAllViews();
			frame.addView(table);
		}
	}

	public ScreenOrientation getScreenOrientation() {
		return this.mScreenOrientation;
	}

	public void setScreenOrientation(ScreenOrientation screenOrientation) {
		this.mScreenOrientation = screenOrientation;
	}

	class PackageReceiver extends BroadcastReceiver {

		@Override
		// This is on the main UI thread
		public void onReceive(final Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)) {
				String publicName = mProxiHandlerManager.getPublicName(intent.getData().getEncodedSchemeSpecificPart());
				AircandiUI
						.showToastNotification(CandiSearchActivity.this, publicName + getText(R.string.toast_package_installed), Toast.LENGTH_SHORT);
			}
		}
	}

	// ==========================================================================
	// ENGINE routines
	// ==========================================================================

	@Override
	protected int getLayoutID() {
		return R.layout.candi_search;
	}

	@Override
	protected int getRenderSurfaceViewID() {
		return R.id.RenderSurfaceView;
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
		int rotation = getWindowManager().getDefaultDisplay().getRotation();
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
			this.mCandiAlertSound = SoundFactory.createSoundFromAsset(this.mEngine.getSoundManager(), this, "notification2.mp3");
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
		Scene scene = mCandiPatchPresenter.initScene();
		mCandiPatchPresenter.mDisplayExtras = this.mPrefDisplayExtras;
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

	// ==========================================================================
	// ANIMATION routines
	// ==========================================================================

	/**
	 * Setup a new 3D rotation on the container view.
	 * 
	 * @param position the item that was clicked to show a picture, or -1 to show the list
	 * @param start the start angle at which the rotation must begin
	 * @param end the end angle of the rotation
	 */
	private void applyRotation(View view, float start, float end) {

		boolean rotateRight = (end - start > 0);

		// Find the center of the container
		final float centerX = view.getWidth() / 2.0f;
		final float centerY = view.getHeight() / 2.0f;

		// Create a new 3D rotation with the supplied parameter
		// The animation listener is used to trigger the next animation
		final Rotate3dAnimation rotation = new Rotate3dAnimation(start, end, centerX, centerY, 310.0f, true);
		rotation.setDuration(500);
		rotation.setFillAfter(true);
		rotation.setInterpolator(new AccelerateInterpolator());
		rotation.setAnimationListener(new DisplayNextView(rotateRight));

		view.startAnimation(rotation);
	}

	/**
	 * This class listens for the end of the first half of the animation. It then posts a new action that effectively
	 * swaps the views when the container is rotated 90 degrees and thus invisible.
	 */
	private final class DisplayNextView implements Animation.AnimationListener {

		private final boolean	rotateRight_;

		private DisplayNextView(boolean rotateRight) {

			rotateRight_ = rotateRight;
		}

		public void onAnimationStart(Animation animation) {

		}

		public void onAnimationEnd(Animation animation) {

			mCandiFlipper.post(new SwapViews(rotateRight_));
		}

		public void onAnimationRepeat(Animation animation) {

		}
	}

	/**
	 * This class is responsible for swapping the views and start the second half of the animation.
	 */
	private final class SwapViews implements Runnable {

		private boolean	rotateRight_;

		public SwapViews(boolean rotateRight) {

			rotateRight_ = rotateRight;
		}

		public void run() {

			final float centerX = mCandiFlipper.getWidth() / 2.0f;
			final float centerY = mCandiFlipper.getHeight() / 2.0f;
			Rotate3dAnimation rotation;

			if (rotateRight_) {
				mCandiSurfaceView.setVisibility(View.GONE);
				mCandiDetailView.setVisibility(View.VISIBLE);
				mCandiDetailView.requestFocus();

				rotation = new Rotate3dAnimation(270, 360, centerX, centerY, 310.0f, false);
			}
			else {
				mCandiDetailView.setVisibility(View.GONE);
				mCandiSurfaceView.setVisibility(View.VISIBLE);
				mCandiSurfaceView.requestFocus();

				rotation = new Rotate3dAnimation(90, 0, centerX, centerY, 310.0f, false);
			}

			rotation.setDuration(500);
			rotation.setFillAfter(true);
			rotation.setInterpolator(new DecelerateInterpolator());

			mCandiFlipper.startAnimation(rotation);
		}
	}

	private void showInstallDialog(final CandiModel candi) {

		this.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				final Dialog dialog = new Dialog(CandiSearchActivity.this, R.style.AircandiDialogTheme);
				final RelativeLayout installDialog = (RelativeLayout) getLayoutInflater().inflate(R.layout.temp_dialog_install, null);
				dialog.setContentView(installDialog, new FrameLayout.LayoutParams(400, 300, Gravity.CENTER));
				dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
				dialog.getWindow().setBackgroundDrawable(getResources().getDrawable(R.drawable.dialog_bg));

				((Button) installDialog.findViewById(R.id.InstallButton)).setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						Intent goToMarket = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(candi.getEntityProxy().entityHandler.code));
						startActivityForResult(goToMarket, 99);
						dialog.dismiss();
					}
				});
				((Button) installDialog.findViewById(R.id.CancelButton)).setOnClickListener(new OnClickListener() {

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

	// ==========================================================================
	// WIFI routines
	// ==========================================================================

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
		AircandiUI.showToastNotification(this, "Wifi enabling...", Toast.LENGTH_LONG);
		mProxiExplorer.enableWifi();
	}

	// ==========================================================================
	// MISC routines
	// ==========================================================================

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

		if (mProxiExplorer != null)
			mProxiExplorer.onResume();

		// Package receiver
		IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
		filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
		filter.addAction(Intent.ACTION_PACKAGE_INSTALL);
		filter.addAction(Intent.ACTION_PACKAGE_DATA_CLEARED);
		filter.addDataScheme("package");
		registerReceiver(mPackageReceiver, filter);

		if (mReadyToRun) {
			loadPreferences();
			loadPreferencesProxiExplorer();

			if (mFirstRun) {
				scanForBeacons(this.mFirstRun, false);
				mFirstRun = false;
			}

			if (mCandiPatchModel.getCandiModels().size() > 0) {
				Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, "Resetting textures");
				mEngine.getScene().registerEntityModifier(new AlphaModifier(0.5f, 0.0f, 1.0f, EaseCircularIn.getInstance()));
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

			if (mEngine.getScene() != null)
				mEngine.getScene().setAlpha(0);

			if (mProxiExplorer != null)
				mProxiExplorer.onPause();

			stopTitlebarProgress();
		}
		catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	@Override
	// Don't count on this always getting called when this activity is killed
	protected void onDestroy() {
		super.onDestroy();

		try {
			Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, "Activity.onDestroy called");
			mProxiExplorer.onDestroy();
		}
		catch (Exception e) {
			e.printStackTrace();
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
		if (getOrient.getRotation() == Surface.ROTATION_90 || getOrient.getRotation() == Surface.ROTATION_270)
			landscape = true;

		super.onConfigurationChanged(newConfig);
		if (mCandiDetailViewVisible) {
			TableLayout table = configureMenus(mCandiPatchModel.getCandiModelFocused(), landscape, CandiSearchActivity.this);
			FrameLayout frame = (FrameLayout) findViewById(R.id.frameMenu);
			frame.removeAllViews();
			frame.addView(table);
		}
	}

	private void loadPreferences() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		if (prefs != null) {
			this.mPrefAutoscan = prefs.getBoolean(Preferences.PREF_AUTOSCAN, true);
			this.mPrefAutoscanInterval = Integer.parseInt(prefs.getString(Preferences.PREF_AUTOSCAN_INTERVAL, "5000"));
			this.mPrefEntityFencing = prefs.getBoolean(Preferences.PREF_ENTITY_FENCING, true);

			this.mPrefBeaconLevelCutoffUnregistered = Integer.parseInt(prefs.getString(Preferences.PREF_BEACON_LEVEL_CUTOFF_UNREGISTERED, "-80"));
			this.mPrefBeaconWithEntitiesOnly = prefs.getBoolean(Preferences.PREF_BEACON_WITH_ENTITIES_ONLY, true);
			this.mPrefBeaconShowHidden = prefs.getBoolean(Preferences.PREF_BEACON_SHOW_HIDDEN, false);

			this.mPrefDisplayExtras = DisplayExtra.valueOf(prefs.getString(Preferences.PREF_DISPLAY_EXTRAS, "None"));
			this.mPrefTileScale = Float.parseFloat(prefs.getString(Preferences.PREF_TILE_SCALE, "1.0"));
			this.mPrefTileRotate = prefs.getBoolean(Preferences.PREF_TILE_ROTATE, true);
			this.mPrefSoundEffects = prefs.getBoolean(Preferences.PREF_SOUND_EFFECTS, true);
			if (this.mCandiPatchPresenter != null)
				mCandiPatchPresenter.mDisplayExtras = mPrefDisplayExtras;
		}
	}

	private void loadPreferencesProxiExplorer() {
		if (mProxiExplorer != null) {
			mProxiExplorer.setPrefTagLevelCutoff(mPrefBeaconLevelCutoffUnregistered);
			mProxiExplorer.setPrefTagsWithEntitiesOnly(mPrefBeaconWithEntitiesOnly);
			mProxiExplorer.setPrefEntityFencing(mPrefEntityFencing);
		}
	}

	protected void startTitlebarProgress() {
		if (mProgressIndicator != null) {
			mProgressIndicator.setVisibility(View.VISIBLE);
			mButtonRefresh.setVisibility(View.INVISIBLE);
			mProgressIndicator.bringToFront();
			AnimationDrawable animation = (AnimationDrawable) mProgressIndicator.getBackground();
			animation.start();
			mProgressIndicator.invalidate();
		}
	}

	protected void stopTitlebarProgress() {
		if (mProgressIndicator != null) {
			mProgressIndicator.setAnimation(null);
			mButtonRefresh.setVisibility(View.VISIBLE);
			mButtonRefresh.bringToFront();
			mProgressIndicator.setVisibility(View.INVISIBLE);
		}
	}

	public void updateCandiBackButton() {

		boolean visible = (mCandiDetailViewVisible || !mCandiPatchModel.getCandiRootCurrent().isSuperRoot());

		if (visible) {
			mContextButton.setText("Back");
			mContextButton.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					if (mCandiDetailViewVisible) {
						onSummaryViewClick(v);
					}
					else {
						mCandiPatchPresenter.navigateModel(mCandiPatchModel.getCandiRootPrevious(), true);
					}
				}
			});

			if (mContextButtonState == ContextButtonState.Default) {
				Animation animation = AnimationUtils.loadAnimation(CandiSearchActivity.this, R.anim.fade_in_medium);
				animation.setFillEnabled(true);
				animation.setFillAfter(true);
				animation.setStartOffset(500);
				mContextButton.startAnimation(animation);
			}

			if (mCandiDetailViewVisible)
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
				Animation animation = AnimationUtils.loadAnimation(CandiSearchActivity.this, R.anim.fade_out_medium);
				animation.setFillEnabled(true);
				animation.setFillAfter(true);
				mContextButton.startAnimation(animation);
			}
			mContextButtonState = ContextButtonState.Default;
		}
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

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	/*
	 * Called before onResume. If we are returning from the market app, we
	 * get a zero result code whether the user decided to start an install
	 * or not.
	 */
	}
}