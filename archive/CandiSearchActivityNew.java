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
import org.anddev.andengine.entity.modifier.DelayModifier;
import org.anddev.andengine.entity.modifier.ParallelEntityModifier;
import org.anddev.andengine.entity.modifier.SequenceEntityModifier;
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
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Animation.AnimationListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.amazonaws.auth.BasicAWSCredentials;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.proxibase.aircandi.activities.AircandiActivity.Verb;
import com.proxibase.aircandi.activities.CandiSearchActivity.AnimType;
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
import com.proxibase.aircandi.models.BaseEntity.SubType;
import com.proxibase.aircandi.utils.AircandiUI;
import com.proxibase.aircandi.utils.ImageCache;
import com.proxibase.aircandi.utils.ImageManager;
import com.proxibase.aircandi.utils.ProxiHandlerManager;
import com.proxibase.aircandi.utils.Rotate3dAnimation;
import com.proxibase.aircandi.utils.Utilities;
import com.proxibase.aircandi.utils.ImageManager.IImageReadyListener;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest;
import com.proxibase.sdk.android.proxi.consumer.Beacon;
import com.proxibase.sdk.android.proxi.consumer.Command;
import com.proxibase.sdk.android.proxi.consumer.EntityProxy;
import com.proxibase.sdk.android.proxi.consumer.ProxiExplorer;
import com.proxibase.sdk.android.proxi.consumer.User;
import com.proxibase.sdk.android.proxi.consumer.ProxiExplorer.IEntityProcessListener;
import com.proxibase.sdk.android.proxi.service.ProxibaseError;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.Query;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.util.ProxiConstants;

@SuppressWarnings("unused")
public class CandiSearchActivityNew extends AircandiGameActivity {

	public enum AnimType {
		Fade, CrossFade, CrossFadeFlipper, RotateScene, RotateCandi
	}

	private static String				COMPONENT_NAME						= "CandiSearch";
	private static String				USER_AGENT							= "Mozilla/5.0 (Linux; U; Android 2.2.1; fr-ch; A43 Build/FROYO) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1";

	private Boolean						mPrefAutoscan						= true;
	private int							mPrefAutoscanInterval				= 5000;
	private int							mPrefBeaconLevelCutoffUnregistered	= -100;
	private boolean						mPrefEntityFencing					= true;
	private boolean						mPrefBeaconWithEntitiesOnly			= true;
	private DisplayExtra				mPrefDisplayExtras					= DisplayExtra.None;
	private float						mPrefTileScale						= 1.0f;
	private boolean						mPrefTileRotate;
	private boolean						mPrefSoundEffects;

	private Boolean						mReadyToRun							= false;
	private Handler						mHandler							= new Handler();
	private Boolean						mCredentialsFound					= false;
	public static BasicAWSCredentials	mAwsCredentials						= null;

	private List<EntityProxy>			mProxiEntities;
	private List<EntityProxy>			mProxiEntitiesFlat;
	private List<TopicItem>				mTopics								= new ArrayList<TopicItem>();
	private ProxiHandlerManager			mProxiHandlerManager;

	private CandiPatchModel				mCandiPatchModel;
	private CandiPatchPresenter			mCandiPatchPresenter;

	protected ImageView					mProgressIndicator;
	protected ImageView					mButtonRefresh;
	private RenderSurfaceView			mCandiSurfaceView;
	public WebView						mWebView;

	private boolean						mCandiDetailViewVisible				= false;
	private AnimType					mAnimTypeCandiDetail				= AnimType.RotateCandi;
	private Sound						mCandiAlertSound;
	private Rotate3dAnimation			mRotate3dAnimation;
	private boolean						mFirstRun							= true;
	private ScreenOrientation			mScreenOrientation					= ScreenOrientation.PORTRAIT;
	private PackageReceiver				mPackageReceiver					= new PackageReceiver();
	private boolean						mIgnoreInput						= false;

	private boolean						mUserRefusedWifiEnable				= false;
	private int							mRenderMode;
	protected User						mUser;

	private boolean						mPrefBeaconShowHidden;

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

		// Ui Hookup
		mWebView = (WebView) findViewById(R.id.WebView);
		mWebView.getSettings().setUserAgentString(USER_AGENT);
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.setBackgroundColor(0x00000000); // Transparent

		mProgressIndicator = (ImageView) findViewById(R.id.Application_ProgressIndicator);
		if (mProgressIndicator != null)
			mProgressIndicator.setVisibility(View.INVISIBLE);

		mButtonRefresh = (ImageView) findViewById(R.id.Application_Button_Refresh);
		if (mButtonRefresh != null)
			mButtonRefresh.setVisibility(View.VISIBLE);

		// Proxibase sdk components
		ProxiExplorer.getInstance().setContext(this);
		if (!ProxiExplorer.getInstance().isWifiEnabled()) {
			this.wifiAskToEnable();
			if (mUserRefusedWifiEnable)
				return;
		}

		// AWS Credentials
		startGetCredentials();

		// Image cache
		ImageManager.getInstance().setImageCache(new ImageCache(getApplicationContext(), CandiConstants.PATH_IMAGECACHE, 100, 16));
		ImageManager.getInstance().setWebView(mWebView);
		ImageManager.getInstance().setContext(getApplicationContext());

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

		// User
		mUser = loadUser("Jay Massena");

		mReadyToRun = true;
	}

	private User loadUser(String userName) {
		try {
			Query query = new Query("Users").filter("Name eq '" + userName + "'");
			String jsonResponse = ProxibaseService.getInstance().selectUsingQuery(query, User.class, CandiConstants.URL_AIRCANDI_SERVICE_ODATA);
			mUser = (User) ProxibaseService.convertJsonToObject(jsonResponse, User.class);
			return mUser;
		}
		catch (ClientProtocolException exception) {
			exception.printStackTrace();
		}
		catch (IOException exception) {
			exception.printStackTrace();
		}
		return null;
	}

	// ==========================================================================
	// EVENT routines
	// ==========================================================================

	@Override
	public void onBackPressed() {
		if (mCandiPatchModel.getCandiRootCurrent().getParent() != null) {
			mCandiPatchPresenter.navigateModel(mCandiPatchModel.getCandiRootCurrent().getParent());
		}
	}

	public void onRefreshClick(View view) {
		// For this activity, refresh means rescan and reload entity data from the service
		if (mReadyToRun) {
			CandiView candiView = (CandiView) mCandiPatchPresenter.getViewForModel(mCandiPatchModel.getCandiModelFocused());
			candiView.loadBodyTextureSources(false);
			scanForBeacons(false, true);
		}
	}

	public void onHomeClick(View view) {
		Intent intent = new Intent(this, CandiSearchActivityNew.class);
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

	public void onNewCandiClick(View view) {
		showTopicDialog();
	}

	// ==========================================================================
	// ENTITY routines
	// ==========================================================================

	private void scanForBeacons(final Boolean fullUpdate, Boolean showProgress) {

		if (showProgress)
			startTitlebarProgress();

		if (fullUpdate && mCandiPatchPresenter != null) {
			mCandiPatchPresenter.setFullUpdateInProgress(true);
			mCandiPatchPresenter.mProgressSprite.setVisible(true);
		}

		Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, "Calling Proxibase.ProxiExplorer to scan for tags");

		ProxiExplorer.getInstance().scanForBeaconsAsync(fullUpdate, new IEntityProcessListener() {

			@Override
			public void onComplete(List<EntityProxy> entities) {

				Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, "Beacon scan results returned from Proxibase.ProxiExplorer");

				doEntitiesUpdate(entities, fullUpdate);

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
							CandiView candiView = (CandiView) mCandiPatchPresenter.getViewForModel(mCandiPatchModel.getCandiModelFocused());
							if (candiView != null) {
								candiView.loadBodyTextureSources(false);
							}
							scanForBeacons(false, false);
						}
					}, mPrefAutoscanInterval);
				}
			}

			public void onIOException(IOException exception) {
				Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, exception.getMessage());
				AircandiUI.showToastNotification(CandiSearchActivityNew.this, "Network error", Toast.LENGTH_SHORT);
				stopTitlebarProgress();
				exception.printStackTrace();
			}

			@Override
			public void onProxiExplorerError(ProxibaseError error) {
				AircandiUI.showToastNotification(CandiSearchActivityNew.this, error.getMessage(), Toast.LENGTH_SHORT);
				Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, error.getMessage());
				stopTitlebarProgress();
			}
		});
	}

	private void refreshEntities(Boolean showProgress) {

		if (showProgress)
			startTitlebarProgress();

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
				AircandiUI.showToastNotification(CandiSearchActivityNew.this, "Network error", Toast.LENGTH_SHORT);
				stopTitlebarProgress();
				exception.printStackTrace();
			}

			@Override
			public void onProxiExplorerError(ProxibaseError error) {
				AircandiUI.showToastNotification(CandiSearchActivityNew.this, error.getMessage(), Toast.LENGTH_SHORT);
				Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, error.getMessage());
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
	}

	// ==========================================================================
	// UI routines
	// ==========================================================================

	private void showCandiDetailView(final CandiModel candiModel, AnimType animType) {

		final Intent intent = new Intent(this, CandiInfo.class);

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

		if (candiModel.getEntityProxy() != null) {
			String jsonEntityProxy = gson.toJson(candiModel.getEntityProxy());
			if (jsonEntityProxy != "")
				intent.putExtra(getString(R.string.EXTRA_ENTITY), jsonEntityProxy);
		}

		if (mUser != null) {
			String jsonUser = ProxibaseService.getGson(GsonType.Internal).toJson(mUser);
			if (jsonUser != "")
				intent.putExtra(getString(R.string.EXTRA_USER), jsonUser);
		}

		// Any pre-animations we want to perform on the way out
		mCandiPatchModel.setCandiModelSelected(candiModel);
		footerVisible(false);

		if (animType == AnimType.CrossFade) {
			mCandiPatchPresenter.setIgnoreInput(false);
			mIgnoreInput = false;
			startActivityForResult(intent, CandiConstants.ACTIVITY_ENTITY_HANDLER);
			overridePendingTransition(R.anim.fade_in_medium, R.anim.hold);
		}
		else {

			float rotationX = mCandiPatchModel.getCandiModelSelected().getZoneCurrent().getCenterX();
			float rotationY = mCandiPatchModel.getCandiModelSelected().getZoneCurrent().getCenterY();
			final float duration = 0.6f;
			float scaleFrom = 1.0f;
			float scaleTo = 0.5f;
			float alphaFrom = 1.0f;
			float alphaTo = 0.0f;
			final IEntity entity = mAnimTypeCandiDetail == AnimType.RotateCandi ? mCandiPatchPresenter.getViewForModel(mCandiPatchModel
					.getCandiModelSelected()) : mEngine.getScene();

			if (mAnimTypeCandiDetail == AnimType.RotateCandi) {
				rotationX = ((CandiConstants.CANDI_VIEW_WIDTH * entity.getScaleX()) * 0.5f);
				rotationY = (CandiConstants.CANDI_VIEW_BODY_HEIGHT * 0.5f);
				scaleTo = 1.3f;
				alphaTo = 0.0f;
				mEngine.getScene().clearEntityModifiers();
				mEngine.getScene().registerEntityModifier(new AlphaModifier(duration, 1.0f, 0.0f, EaseLinear.getInstance()));
			}

			entity.clearEntityModifiers();
			entity.registerEntityModifier(new ParallelEntityModifier(new IEntityModifierListener() {

				@Override
				public void onModifierFinished(IModifier<IEntity> modifier, final IEntity entityModified) {

					mEngine.runOnUpdateThread(new Runnable() {

						@Override
						public void run() {
						// mEngine.getScene().clearEntityModifiers();
						// entity.clearEntityModifiers();
						}
					});

					CandiSearchActivityNew.this.runOnUiThread(new Runnable() {

						public void run() {
							mCandiPatchPresenter.setIgnoreInput(false);
							mIgnoreInput = false;
							startActivityForResult(intent, CandiConstants.ACTIVITY_ENTITY_HANDLER);
							overridePendingTransition(R.anim.fade_in_medium, R.anim.hold);
						}
					});

				}

				@Override
				public void onModifierStarted(IModifier<IEntity> modifier, IEntity entity) {}

			}, new RotationAtModifier(duration, 0, 90, rotationX, rotationY, EaseCubicIn.getInstance()), new AlphaModifier(duration, alphaFrom,
					alphaTo, EaseCircularIn.getInstance())));
		}

	}

	private void rotateCandi() {
		CandiModel candiModelFocused = mCandiPatchModel.getCandiModelFocused();
		final float duration = 1.0f;
		float rotationX = candiModelFocused.getZoneCurrent().getCenterX();
		float rotationY = candiModelFocused.getZoneCurrent().getCenterY();
		float scaleFrom = 0.5f;
		float scaleTo = 1.0f;
		float alphaFrom = 0.0f;
		float alphaTo = 1.0f;

		IEntity entity = mCandiPatchPresenter.getViewForModel(candiModelFocused);
		rotationX = (CandiConstants.CANDI_VIEW_WIDTH * 0.5f);
		rotationY = (CandiConstants.CANDI_VIEW_BODY_HEIGHT * 0.5f);
		scaleFrom = 1.3f;
		alphaFrom = 0.5f;

		// Using a scaling modifier is tricky to reverse without shifting coordinates
		entity.clearEntityModifiers();
		ParallelEntityModifier entityMod = new ParallelEntityModifier(new RotationAtModifier(duration, 90f, 0.0f, rotationX, rotationY, EaseCubicOut
				.getInstance()), new AlphaModifier(duration, alphaFrom, alphaTo, EaseCircularOut.getInstance()));
		entity.registerEntityModifier(new SequenceEntityModifier(new DelayModifier(1.0f), entityMod));
	}

	private void footerVisible(boolean visible) {
		int animationResource = visible ? R.anim.fade_in_medium : R.anim.fade_out_medium;
		Animation animation = AnimationUtils.loadAnimation(CandiSearchActivityNew.this, animationResource);
		animation.setFillEnabled(true);
		animation.setFillAfter(true);
		((LinearLayout) findViewById(R.id.AppFooter)).startAnimation(animation);
	}

	public void updateCandiBackButton() {

		boolean visible = (!mCandiPatchModel.getCandiRootCurrent().isSuperRoot());

		if (visible) {
			mContextButton.setText("Back");
			mContextButton.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					if (mCandiPatchModel.getCandiRootCurrent().getParent() != null)
						mCandiPatchPresenter.navigateModel(mCandiPatchModel.getCandiRootCurrent().getParent());
				}
			});

			if (mContextButtonState == ContextButtonState.Default) {
				Animation animation = AnimationUtils.loadAnimation(CandiSearchActivityNew.this, R.anim.fade_in_medium);
				animation.setFillEnabled(true);
				animation.setFillAfter(true);
				animation.setStartOffset(500);
				mContextButton.startAnimation(animation);
			}

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
				Animation animation = AnimationUtils.loadAnimation(CandiSearchActivityNew.this, R.anim.fade_out_medium);
				animation.setFillEnabled(true);
				animation.setFillAfter(true);
				mContextButton.startAnimation(animation);
			}
			mContextButtonState = ContextButtonState.Default;
		}
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
		// showCandiDetailView(candiModel, AnimType.CrossFade);
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
				showCandiDetailView(candiModel, AnimType.CrossFadeFlipper);
			}
		});
	}

	class PackageReceiver extends BroadcastReceiver {

		@Override
		// This is on the main UI thread
		public void onReceive(final Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)) {
				String publicName = mProxiHandlerManager.getPublicName(intent.getData().getEncodedSchemeSpecificPart());
				AircandiUI.showToastNotification(CandiSearchActivityNew.this, publicName + getText(R.string.toast_package_installed),
						Toast.LENGTH_SHORT);
			}
		}
	}

	// ==========================================================================
	// ENGINE routines
	// ==========================================================================

	@Override
	protected int getLayoutID() {
		return R.layout.candi_search_new;
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
		Scene scene = mCandiPatchPresenter.initializeScene();
		mCandiPatchPresenter.mDisplayExtras = this.mPrefDisplayExtras;
		mCandiPatchModel.addObserver(mCandiPatchPresenter);
		mCandiPatchPresenter.setCandiListener(new ICandiListener() {

			@Override
			public void onSelected(IModel candi) {}

			@Override
			public void onSingleTap(CandiModel candi) {
				CandiSearchActivityNew.this.doCandiSingleTap(candi);
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

		// mCandiFlipper.post(new SwapViews(rotateRight_));
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

		// final float centerX = mCandiFlipper.getWidth() / 2.0f;
		// final float centerY = mCandiFlipper.getHeight() / 2.0f;
		// Rotate3dAnimation rotation;
		//
		// if (rotateRight_) {
		// mCandiSurfaceView.setVisibility(View.GONE);
		// mCandiDetailView.setVisibility(View.VISIBLE);
		// mCandiDetailView.requestFocus();
		//
		// rotation = new Rotate3dAnimation(270, 360, centerX, centerY, 310.0f, false);
		// }
		// else {
		// mCandiDetailView.setVisibility(View.GONE);
		// mCandiSurfaceView.setVisibility(View.VISIBLE);
		// mCandiSurfaceView.requestFocus();
		//
		// rotation = new Rotate3dAnimation(90, 0, centerX, centerY, 310.0f, false);
		// }
		//
		// rotation.setDuration(500);
		// rotation.setFillAfter(true);
		// rotation.setInterpolator(new DecelerateInterpolator());
		//
		// mCandiFlipper.startAnimation(rotation);
		}
	}

	private void showInstallDialog(final CandiModel candi) {

		this.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				final Dialog dialog = new Dialog(CandiSearchActivityNew.this, R.style.AircandiDialogTheme);
				final RelativeLayout installDialog = (RelativeLayout) getLayoutInflater().inflate(R.layout.temp_dialog_install, null);
				dialog.setContentView(installDialog, new FrameLayout.LayoutParams(400, 300, Gravity.CENTER));
				dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
				dialog.getWindow().setBackgroundDrawable(getResources().getDrawable(R.drawable.dialog_bg));

				((Button) installDialog.findViewById(R.id.InstallButton)).setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						Intent goToMarket = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(candi.getEntityProxy().entityHandler.code));
						startActivityForResult(goToMarket, CandiConstants.ACTIVITY_MARKET);
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

	private void showTopicDialog() {

		this.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				final Dialog dialog = new Dialog(CandiSearchActivityNew.this, R.style.AircandiDialogTheme);
				final RelativeLayout installDialog = (RelativeLayout) getLayoutInflater().inflate(R.layout.temp_dialog_new, null);
				dialog.setContentView(installDialog, new FrameLayout.LayoutParams(dialog.getWindow().getWindowManager().getDefaultDisplay()
						.getWidth() - 40, LayoutParams.FILL_PARENT, Gravity.CENTER));
				dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

				dialog.getWindow().setBackgroundDrawable(getResources().getDrawable(R.drawable.dialog_bg));

				((Button) installDialog.findViewById(R.id.NewTopicButton)).setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {

						Intent intent = buildIntent(Verb.New, null, SubType.Topic, 0, ProxiExplorer.getInstance().getStrongestBeacon(), mUser,
								Post.class);
						startActivityForResult(intent, CandiConstants.ACTIVITY_ENTITY_HANDLER);
						overridePendingTransition(R.anim.fade_in_medium, R.anim.hold);
						dialog.dismiss();
					}
				});

				((Button) installDialog.findViewById(R.id.ExtendTopicButton)).setOnClickListener(new OnClickListener() {

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

	private Intent buildIntent(Verb verb, EntityProxy entityProxy, SubType subType, int parentEntityId, Beacon beacon, User user, Class<?> clazz) {
		Intent intent = new Intent(CandiSearchActivityNew.this, clazz);

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
			if (jsonBeacon != "")
				intent.putExtra(getString(R.string.EXTRA_BEACON), jsonBeacon);
		}

		if (entityProxy != null) {
			String jsonEntityProxy = gson.toJson(entityProxy);
			if (jsonEntityProxy != "")
				intent.putExtra(getString(R.string.EXTRA_ENTITY), jsonEntityProxy);
		}

		if (user != null) {
			String jsonUser = ProxibaseService.getGson(GsonType.Internal).toJson(user);
			if (jsonUser != "")
				intent.putExtra(getString(R.string.EXTRA_USER), jsonUser);
		}

		return intent;
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
		ProxiExplorer.getInstance().enableWifi();
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

		ProxiExplorer.getInstance().onResume();

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
				rotateCandi();
				footerVisible(true);
				mEngine.getScene().registerEntityModifier(new AlphaModifier(1.5f, 0.0f, 1.0f, EaseCircularIn.getInstance()));

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
		super.onDestroy();

		try {
			Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, "Activity.onDestroy called");
			ProxiExplorer.getInstance().onDestroy();
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
		if (getOrient.getOrientation() == Surface.ROTATION_90 || getOrient.getOrientation() == Surface.ROTATION_270)
			landscape = true;

		super.onConfigurationChanged(newConfig);
	}

	private void loadPreferences() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		if (prefs != null) {
			this.mPrefAutoscan = prefs.getBoolean(Preferences.PREF_AUTOSCAN, true);
			this.mPrefAutoscanInterval = Integer.parseInt(prefs.getString(Preferences.PREF_AUTOSCAN_INTERVAL, "5000"));
			this.mPrefEntityFencing = prefs.getBoolean(Preferences.PREF_ENTITY_FENCING, true);


			this.mPrefDisplayExtras = DisplayExtra.valueOf(prefs.getString(Preferences.PREF_DISPLAY_EXTRAS, "None"));
			this.mPrefSoundEffects = prefs.getBoolean(Preferences.PREF_SOUND_EFFECTS, true);
			if (this.mCandiPatchPresenter != null)
				mCandiPatchPresenter.mDisplayExtras = mPrefDisplayExtras;
		}
	}

	private void loadPreferencesProxiExplorer() {
		ProxiExplorer.getInstance().setPrefEntityFencing(mPrefEntityFencing);
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
		if (requestCode == CandiConstants.ACTIVITY_ENTITY_HANDLER) {
			if (resultCode == Activity.RESULT_FIRST_USER) {
				if (data != null) {
					Bundle extras = data.getExtras();
					if (extras != null) {
						String dirtyBeaconId = extras.getString(getString(R.string.EXTRA_BEACON_DIRTY));
						Integer dirtyEntityId = extras.getInt(getString(R.string.EXTRA_ENTITY_DIRTY));
						if (dirtyBeaconId != null && !dirtyBeaconId.equals("")) {
							for (Beacon beacon : ProxiExplorer.getInstance().getBeacons()) {
								if (beacon.id.equals(dirtyBeaconId)) {
									beacon.dirty = true;
									startTitlebarProgress();
									List<EntityProxy> freshEntityProxies = ProxiExplorer.getInstance().refreshEntities();
									doEntitiesUpdate(freshEntityProxies, false);
									stopTitlebarProgress();
								}
							}
						}
						else if (dirtyEntityId != null) {
							for (EntityProxy entityProxy : ProxiExplorer.getInstance().getEntityProxiesFlat()) {
								if (entityProxy.id.equals(dirtyEntityId)) {
									entityProxy.dirty = true;

									startTitlebarProgress();
									List<EntityProxy> freshEntityProxies = ProxiExplorer.getInstance().refreshEntities();
									doEntitiesUpdate(freshEntityProxies, false);
									stopTitlebarProgress();
								}
							}
						}
					}
				}
			}
			mCandiPatchPresenter.resetSharedTextures();
			mCandiPatchPresenter.resetTextures(TextureReset.VisibleOnly);
		}
		else if (requestCode == CandiConstants.ACTIVITY_MARKET) {
		}
	}
}