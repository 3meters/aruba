package com.proxibase.aircandi.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.engine.options.EngineOptions.ScreenOrientation;
import org.anddev.andengine.engine.options.resolutionpolicy.FillResolutionPolicy;
import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.modifier.ParallelEntityModifier;
import org.anddev.andengine.entity.modifier.RotationAtModifier;
import org.anddev.andengine.entity.modifier.ScaleAtModifier;
import org.anddev.andengine.entity.modifier.IEntityModifier.IEntityModifierListener;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.background.ColorBackground;
import org.anddev.andengine.entity.util.FPSLogger;
import org.anddev.andengine.opengl.util.GLHelper;
import org.anddev.andengine.opengl.view.RenderSurfaceView;
import org.anddev.andengine.ui.activity.LayoutGameActivity;
import org.anddev.andengine.util.modifier.IModifier;
import org.anddev.andengine.util.modifier.ease.EaseCubicIn;
import org.anddev.andengine.util.modifier.ease.EaseCubicOut;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Animation.AnimationListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.proxibase.aircandi.proxi.ProxiConstants;
import com.proxibase.aircandi.proxi.ProxiUiHandler;
import com.proxibase.aircandi.proxi.ProxiTile;
import com.proxibase.aircandi.proxi.ProxiUiHandler.OnProxiEntityListener;
import com.proxibase.aircandi.utilities.Utilities;
import com.proxibase.sdk.android.core.BaseBeaconScanListener;
import com.proxibase.sdk.android.core.LinkedTreeList;
import com.proxibase.sdk.android.core.ProxibaseError;
import com.proxibase.sdk.android.core.ProxibaseService;
import com.proxibase.sdk.android.core.Stream;
import com.proxibase.sdk.android.core.TreeList;
import com.proxibase.sdk.android.core.ProxibaseService.GsonType;
import com.proxibase.sdk.android.core.proxi.ProxiEntity;
import com.proxibase.sdk.android.core.proxi.ProxiExplorer;
import com.proxibase.sdk.android.widgets.ImageCache;

@SuppressWarnings("unused")
public class TricorderAE extends LayoutGameActivity {

	private Boolean					mPrefAutoscan				= true;
	private int						mPrefAutoscanInterval		= 5000;
	private int						mPrefTagLevelCutoff			= -100;
	private boolean					mPrefTagsWithEntitiesOnly	= true;
	private DisplayExtra			mPrefDisplayExtras			= DisplayExtra.None;
	private float					mPrefTileScale				= 1.0f;
	private boolean					mPrefTileRotate;
	private boolean					mPrefSoundEffects;

	private List<ProxiEntity>		mProxiEntities;
	private LinkedTreeList			mProxiModel;

	private Boolean					mReadyToRun					= false;
	private Handler					mHandler					= new Handler();
	private CxMediaPlayer			mSoundEffects;
	private MediaPlayer				mMediaPlayer;

	private TreeList<ProxiEntity>	mCurrentRootNode			= null;
	private int						mCurrentRootNodePosition	= 0;
	private ProxiEntity				mSelectedEntity				= null;

	private ProxiExplorer			mProxiExplorer;
	private RenderSurfaceView		mProxiView;

	protected ImageView				mProgressIndicator;
	protected ImageView				mButtonRefresh;
	private LinearLayout			mRippleContainer;
	private FrameLayout				mEntitySummaryView;
	protected ImageCache			mImageCache;
	private boolean					mDetailVisible				= false;

	private boolean					mUserRefusedWifiEnable		= false;
	private ProxiUiHandler			mProxiController;
	private Camera					mCamera;


	/**
	 * Called when the activity is first created.
	 */

	@Override
	public void onCreate(Bundle savedInstanceState) {

		// setContentView is call from parent class and passes the
		// layout specified in getLayoutId();
		super.onCreate(savedInstanceState);
		initialize();
	}


	private void initialize() {

		// Start normal processing
		mReadyToRun = false;

		// Image cache
		mImageCache = new ImageCache(getApplicationContext(), "aircandi", 100, 16);
		mMediaPlayer = new MediaPlayer();
		mSoundEffects = new CxMediaPlayer(this);

		// Ui Hookup
		mRippleContainer = (LinearLayout) findViewById(R.id.RippleContainer);
		mEntitySummaryView = (FrameLayout) findViewById(R.id.EntitySummaryView);
		mProgressIndicator = (ImageView) findViewById(R.id.Application_ProgressIndicator);
		if (mProgressIndicator != null)
			mProgressIndicator.setVisibility(View.INVISIBLE);
		mButtonRefresh = (ImageView) findViewById(R.id.Application_Button_Refresh);
		if (mButtonRefresh != null)
			mButtonRefresh.setVisibility(View.VISIBLE);

		// Ripple sdk components
		mProxiExplorer = new ProxiExplorer(this);

		if (!mProxiExplorer.isWifiEnabled()) {
			this.wifiAskToEnable();
			if (mUserRefusedWifiEnable)
				return;
		}

		// Use a surface format with an Alpha channel
		this.mRenderSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);

		// Make sure the proxi view rendering surface is on top otherwise the
		// surface view cuts a hole through anything above it in the zorder.
		this.mRenderSurfaceView.setZOrderOnTop(true);

		// TODO: Rendering only when dirty is more battery efficient
		this.mRenderSurfaceView.setRenderMode(RenderSurfaceView.RENDERMODE_CONTINUOUSLY);

		// Property settings get overridden once we retrieve preferences
		mProxiView = (RenderSurfaceView) findViewById(R.id.ProxiView);
		mProxiView.requestFocus();
		mProxiView.setFocusableInTouchMode(true);

		mReadyToRun = true;

	}


	// ==========================================================================
	// EVENT routines
	// ==========================================================================

	public void onStreamButtonClick(View view) {

		Stream stream = (Stream) view.getTag();
		String fullyQualifiedClass = "com.proxibase.aircandi.controller." + stream.streamClass;
		String streamName = stream.streamName.toLowerCase();

		if (stream.streamClass.toLowerCase().equals("list")) {
			AircandiUI.showToastNotification(this, "Unimplemented...", Toast.LENGTH_SHORT);
			return;
		}

		try {
			if (streamName.toLowerCase().equals("audio")) {
				String audioFile = "";
				if (stream.entityResourceId.contains("hopper_house"))
					audioFile = "hopper_house_by_the_railroad.mp3";
				else if (stream.entityResourceId.contains("klimt_hope"))
					audioFile = "klimt_hope.mp3";
				else if (stream.entityResourceId.contains("monet_japanese"))
					audioFile = "monet_japanese_footbridge.mp3";
				else if (stream.entityResourceId.contains("starry_night"))
					audioFile = "vangogh_starry_night.mp3";
				else if (stream.entityResourceId.contains("cezanne"))
					audioFile = "cezanne2.mp3";
				// MediaPlayer mediaPlayer = new MediaPlayer();
				// Uri uri = Uri.parse(Aircandi.URL_AIRCANDI_MEDIA + "audio.3meters.com/signsoflove.mp3");

				Uri uri = Uri.parse("http://dev.aircandi.com/media/" + audioFile);
				mMediaPlayer.setDataSource(TricorderAE.this, uri);
				mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
				mMediaPlayer.prepare();
				mMediaPlayer.start();

				// Uri url = Uri.parse("http://dev.aircandi.com/media/cezanne2.mp3");
				// AsyncPlayer player = new AsyncPlayer("Aircandi");
				// player.stop();
				// player.play(Tricorder.this, url, false, AudioManager.STREAM_MUSIC);

			}
			else if (streamName.toLowerCase().equals("video")) {
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
				String jsonStream = ProxibaseService.getGson(GsonType.Internal).toJson(stream);
				intent.putExtra("stream", jsonStream);

				if (streamName.toLowerCase().equals("friends"))
					intent.putExtra("FriendsFilter", "FriendsByPoint");
				else if (streamName.toLowerCase().equals("eggs")) {
					if (stream.itemCount == 0)
						intent.putExtra("TabId", "eggsdrop");
					else
						intent.putExtra("TabId", "egghunt");
				}

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
			// TODO Auto-generated catch block
			exception.printStackTrace();
		}
		catch (IOException exception) {
			// TODO Auto-generated catch block
			exception.printStackTrace();
		}
	}


	@Override
	public void onBackPressed() {

		if (!mDetailVisible) {
		}
		else {
			mMediaPlayer.stop();
			mMediaPlayer.reset();
			applyRotation(mRippleContainer, 360, 270);
			setCurrentEntity(null);
			mDetailVisible = false;
		}
	}


	public void onRefreshClick(View view) {

		// For this activity, refresh means rescan and reload point data from the service
		if (mReadyToRun)
		{
			scanForBeacons(false, true);
		}
	}


	public void onDetailsClick(View v) {

		mMediaPlayer.stop();
		mMediaPlayer.reset();
		hideEntitySummary();
		// applyRotation(mRippleContainer_, 360, 270);
		mDetailVisible = false;
	}


	public void onHomeClick(View view) {

		Intent intent = new Intent(this, TricorderAE.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}


	// ==========================================================================
	// SCAN AND DISPLAY routines
	// ==========================================================================

	private void scanForBeacons(final Boolean fullUpdate, Boolean showProgress) {

		if (showProgress)
			startProgress();

		Utilities.Log(Aircandi.APP_NAME, "Tricorder", "Calling Proxibase.ProxiExplorer to scan for tags");

		mProxiExplorer.scanForBeacons(fullUpdate, new BaseBeaconScanListener() {

			@Override
			public void onComplete(List<ProxiEntity> entities) {

				Utilities.Log(Aircandi.APP_NAME, "Tricorder", "Beacon scan results returned from Proxibase.ProxiExplorer");
				Utilities.Log(Aircandi.APP_NAME, "Tricorder", "Entity count: " + String.valueOf(entities.size()));

				// try {

				// Scanning is complete so change the heading back to normal
				// Show search message if there aren't any current points
				TextView message = (TextView) findViewById(R.id.Tricorder_Message);
				if (message != null) {
					message.setVisibility(View.GONE);
				}

				// Replace the collection
				Utilities.Log(Aircandi.APP_NAME, "Tricorder", "Refreshing RippleView");
				mProxiEntities = (List<ProxiEntity>) ((ArrayList<ProxiEntity>) entities).clone();

				// Generate a tree list that organizes the entities into a hierarchy based on
				// group and sort properties. The tree list is a better match for how we display.
				mProxiModel = mProxiExplorer.processProxiModelFromEntities(mProxiEntities);

				// Update our UI
				mProxiController.setProxiEntities(mProxiEntities);
				mProxiController.refreshProxiView(fullUpdate);
				stopProgress();

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

				Utilities.Log(ProxibaseService.APP_NAME, "Tricorder", exception.getMessage());
				AircandiUI.showToastNotification(TricorderAE.this, "Network error", Toast.LENGTH_SHORT);
				stopProgress();
				exception.printStackTrace();
			}


			@Override
			public void onProxiExplorerError(ProxibaseError error) {

				AircandiUI.showToastNotification(TricorderAE.this, error.getMessage(), Toast.LENGTH_SHORT);
				Utilities.Log(ProxibaseService.APP_NAME, "Tricorder", error.getMessage());
				stopProgress();
			}

		}

		);

		// Show search message if there aren't any current points
		TextView message = (TextView) findViewById(R.id.Tricorder_Message);
		if (message != null) {
			message.setVisibility(View.VISIBLE);
			message.setText("Searching for\ntags...");
		}
	}


	private void clearProxiCandi() {

		// Removing entities can only be done safely on the UpdateThread.
		// Doing it while updating/drawing can cause an exception with a suddenly missing entity.
		this.runOnUpdateThread(new Runnable() {

			@Override
			public void run() {

				for (int i = 0; i < mProxiEntities.size(); i++) {
					((ProxiTile) mProxiEntities.get(i).sprite).detachSelf();
					mProxiEntities.get(i).sprite = null;
				}
			}
		});
	}


	private void setupSummary(final ProxiEntity proxiEntity) {

		new BuildMenuTask().execute(mEntitySummaryView);

		if (proxiEntity.entity.pointResourceId != null && proxiEntity.entity.pointResourceId != "")
			if (mProxiController.getImageManager().hasImage(proxiEntity.entity.pointResourceId)) {
				Bitmap bitmap = mProxiController.getImageManager().getImage(proxiEntity.entity.pointResourceId);
				((ImageView) mEntitySummaryView.findViewById(R.id.Image)).setImageBitmap(bitmap);
			}

		((TextView) mEntitySummaryView.findViewById(R.id.Title)).setText(proxiEntity.entity.title);
		((TextView) mEntitySummaryView.findViewById(R.id.Subtitle)).setText(Html.fromHtml(proxiEntity.entity.subtitle));
		((TextView) mEntitySummaryView.findViewById(R.id.Description)).setText(Html.fromHtml(proxiEntity.entity.description));
	}


	private void hideEntitySummary() {

		// Find the center of the container
		final float centerX = mRippleContainer.getWidth() / 2.0f;
		final float centerY = mRippleContainer.getHeight() / 2.0f;

		// Create a new 3D rotation with the supplied parameter
		// The animation listener is used to trigger the next animation
		final Rotate3dAnimation rotation = new Rotate3dAnimation(360, 270, centerX, centerY, 310.0f, true);
		rotation.setDuration(750);
		rotation.setFillAfter(true);
		rotation.setInterpolator(new AccelerateInterpolator());

		rotation.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationEnd(Animation animation) {

				mRippleContainer.post(new Runnable() {

					@Override
					public void run() {

						final float centerX = mRippleContainer.getWidth() / 2.0f;
						final float centerY = mRippleContainer.getHeight() / 2.0f;

						mEntitySummaryView.setVisibility(View.GONE);
						mProxiView.setVisibility(View.VISIBLE);
						mProxiView.requestFocus();

						mEngine.getScene().registerEntityModifier(
								new ParallelEntityModifier(
										new RotationAtModifier(1f, 90, 0,
												((ProxiTile) getCurrentEntity().sprite).getX() + (((ProxiTile) getCurrentEntity().sprite).mBodySprite
																.getWidthScaled() * 0.5f),
												((ProxiTile) getCurrentEntity().sprite).getY() + (((ProxiTile) getCurrentEntity().sprite).mBodySprite
																.getHeightScaled() * 0.5f), EaseCubicOut.getInstance()), new ScaleAtModifier(1f,
												0.5f, 1f, ((ProxiTile) getCurrentEntity().sprite).getX() + (((ProxiTile) getCurrentEntity().sprite).mBodySprite
																	.getWidthScaled() * 0.5f),
												((ProxiTile) getCurrentEntity().sprite).getY() + (((ProxiTile) getCurrentEntity().sprite).mBodySprite
																.getHeightScaled() * 0.5f), EaseCubicOut.getInstance())));

						// Lastly we clear the current entity
						setCurrentEntity(null);
					}
				});

			}


			@Override
			public void onAnimationRepeat(Animation animation) {

			// TODO Auto-generated method stub

			}


			@Override
			public void onAnimationStart(Animation animation) {

			// TODO Auto-generated method stub

			}
		});

		mRippleContainer.startAnimation(rotation);
	}


	private void showEntitySummary(final ProxiEntity proxiEntity) {

		setCurrentEntity(proxiEntity);
		setupSummary(proxiEntity);

		mEngine.getScene().registerEntityModifier(
				new ParallelEntityModifier(new IEntityModifierListener() {

					@Override
					public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {

						TricorderAE.this.runOnUiThread(new Runnable() {

							public void run() {

								final float centerX = mRippleContainer.getWidth() / 2.0f;
								final float centerY = mRippleContainer.getHeight() / 2.0f;

								mProxiView.setVisibility(View.GONE);
								mEntitySummaryView.setVisibility(View.VISIBLE);
								mEntitySummaryView.requestFocus();

								Rotate3dAnimation rotation = new Rotate3dAnimation(270, 360, centerX, centerY, 310.0f, false);

								rotation.setDuration(500);
								rotation.setFillAfter(true);
								rotation.setInterpolator(new DecelerateInterpolator());

								mRippleContainer.startAnimation(rotation);
							}
						});

					}

				}, new RotationAtModifier(1f, 0, 90,
						((ProxiTile) proxiEntity.sprite).getX() + (((ProxiTile) proxiEntity.sprite).mBodySprite.getWidthScaled() * 0.5f),
						((ProxiTile) proxiEntity.sprite).getY() + (((ProxiTile) proxiEntity.sprite).mBodySprite.getHeightScaled() * 0.5f), EaseCubicIn
								.getInstance()), new ScaleAtModifier(1f, 1, 0.3f,
						((ProxiTile) proxiEntity.sprite).getX() + (((ProxiTile) proxiEntity.sprite).mBodySprite.getWidthScaled() * 0.5f),
						((ProxiTile) proxiEntity.sprite).getY() + (((ProxiTile) proxiEntity.sprite).mBodySprite.getHeightScaled() * 0.5f), EaseCubicIn
								.getInstance())));

	}


	private TableLayout configureMenus(ProxiEntity entity, boolean landscape, Context context) {

		Boolean needMoreButton = false;
		if (entity.entity.streams.size() > 6)
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
		for (Stream stream : entity.entity.streams) {
			// Make a button and configure it
			streamButtonContainer = (RelativeLayout) this.getLayoutInflater().inflate(R.layout.temp_button_stream, null);

			final TextView streamButton = (TextView) streamButtonContainer.findViewById(R.id.StreamButton);
			final TextView streamBadge = (TextView) streamButtonContainer.findViewById(R.id.StreamBadge);
			streamButtonContainer.setTag(stream);
			if (needMoreButton && streamCount == 5) {
				streamButton.setText("More...");
				streamButton.setTag(stream);
			}
			else {
				streamButton.setText(stream.streamLabel);
				streamButton.setTag(stream);
				if (stream.itemCount > 0) {
					if (stream.streamName.toLowerCase().equals("eggs"))
						streamBadge.setVisibility(View.INVISIBLE);
					else {
						streamBadge.setText(String.valueOf(stream.itemCount));
						streamBadge.setTag(stream);
					}
				}
				else
					streamBadge.setVisibility(View.INVISIBLE);
			}
			// streamButton.getBackground().mutate().setColorFilter(getResources().getColor(R.color.button_color_filter),
			// PorterDuff.Mode.MULTIPLY);

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


	// ==========================================================================
	// ENGINE routines
	// ==========================================================================

	@Override
	protected int getLayoutID() {

		return R.layout.tricorder_ae;
	}


	@Override
	protected int getRenderSurfaceViewID() {

		return R.id.ProxiView;
	}


	@Override
	public void onLoadComplete() {

		if (mEngine.getScene() != null) {
			mProxiController.initScene();
			if (mProxiController.mBusySprite != null)
				mProxiController.mBusySprite.setVisible(true);
			
		}

	}


	@Override
	public Engine onLoadEngine() {

		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);

		this.mCamera = new Camera(0, 0, dm.widthPixels, dm.heightPixels) {

			@Override
			public void onApplyMatrix(GL10 pGL) {

				// Gets called for every engine update
				mProxiController.setFrustum(pGL);
			}


			@Override
			public void onApplyPositionIndependentMatrix(GL10 pGL) {

				mProxiController.setFrustum(pGL);
			}


			@Override
			public void onApplyCameraSceneMatrix(GL10 pGL) {

				mProxiController.setFrustum(pGL);
			}

		};

		Engine engine = new Engine(new EngineOptions(true, ScreenOrientation.PORTRAIT, new FillResolutionPolicy(), this.mCamera));

		return engine;

	}


	@Override
	public void onLoadResources() {
		Utilities.Log(ProxibaseService.APP_NAME, "Tricorder", "onLoadResources called");
	}


	@Override
	public Scene onLoadScene() {

		// Called after LoadEngine and Resume.
		// Scene initialization is completed in ProxiController.
		this.mEngine.registerUpdateHandler(new FPSLogger());
		final Scene scene = new Scene(2) {

			@Override
			protected void applyRotation(final GL10 pGL) {

				/* Disable culling so we can see the backside of this sprite. */
				GLHelper.disableCulling(pGL);

				final float rotation = this.mRotation;

				if (rotation != 0) {
					final float rotationCenterX = this.mRotationCenterX;
					final float rotationCenterY = this.mRotationCenterY;

					pGL.glTranslatef(rotationCenterX, rotationCenterY, 0);
					// Note we are applying rotation around the y-axis and not the z-axis anymore!
					pGL.glRotatef(rotation, 0, 1, 0);
					pGL.glTranslatef(-rotationCenterX, -rotationCenterY, 0);
				}
			}

		};

		scene.setBackground(new ColorBackground(0, 0, 0, 0));
		scene.setTouchAreaBindingEnabled(true);

		// Calculate the coordinates for the center of the camera.

		final int centerX = (int) ((this.mCamera.getWidth() - ProxiConstants.PROXI_TILE_WIDTH) / 2);
		final int centerY = (int) ((this.mCamera.getHeight() - ProxiConstants.PROXI_TILE_HEIGHT) / 2);

		// Invisible entity used to scroll

		mProxiController.setCameraTarget(new Rectangle(centerX, centerY, ProxiConstants.PROXI_TILE_WIDTH, ProxiConstants.PROXI_TILE_HEIGHT));
		mProxiController.getCameraTarget().setScaleCenter(centerX, centerY);
		mProxiController.getCameraTarget().setColor(0, 0, 0, 0);
		mProxiController.getCameraTarget().setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		mProxiController.getCameraTarget().setVisible(true);
		scene.getChild(ProxiUiHandler.LAYER_GENERAL).attachChild(mProxiController.getCameraTarget());
		mCamera.setChaseEntity(mProxiController.getCameraTarget());

		return scene;
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

			mRippleContainer.post(new SwapViews(rotateRight_));
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

			final float centerX = mRippleContainer.getWidth() / 2.0f;
			final float centerY = mRippleContainer.getHeight() / 2.0f;
			Rotate3dAnimation rotation;

			if (rotateRight_) {
				mProxiView.setVisibility(View.GONE);
				mEntitySummaryView.setVisibility(View.VISIBLE);
				mEntitySummaryView.requestFocus();

				rotation = new Rotate3dAnimation(270, 360, centerX, centerY, 310.0f, false);
			}
			else {
				mEntitySummaryView.setVisibility(View.GONE);
				mProxiView.setVisibility(View.VISIBLE);
				mProxiView.requestFocus();

				rotation = new Rotate3dAnimation(90, 0, centerX, centerY, 310.0f, false);
			}

			rotation.setDuration(500);
			rotation.setFillAfter(true);
			rotation.setInterpolator(new DecelerateInterpolator());

			mRippleContainer.startAnimation(rotation);
		}
	}


	private void displaySummary(boolean makeVisible, long duration, long delay) {

		final float centerX = mRippleContainer.getWidth() / 2.0f;
		final float centerY = mRippleContainer.getHeight() / 2.0f;
		Rotate3dAnimation rotation;

		if (makeVisible) {
			mProxiView.setVisibility(View.GONE);
			mEntitySummaryView.setVisibility(View.VISIBLE);
			mEntitySummaryView.requestFocus();

			rotation = new Rotate3dAnimation(270, 360, centerX, centerY, 310.0f, false);
		}
		else {
			mEntitySummaryView.setVisibility(View.GONE);
			mProxiView.setVisibility(View.VISIBLE);
			mProxiView.requestFocus();

			rotation = new Rotate3dAnimation(90, 0, centerX, centerY, 310.0f, false);
		}

		rotation.setDuration(duration);
		rotation.setStartOffset(delay);
		rotation.setFillAfter(true);
		rotation.setInterpolator(new DecelerateInterpolator());

		mRippleContainer.startAnimation(rotation);

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

		mProxiExplorer.enableWifi();
		AircandiUI.showToastNotification(this, "Wifi enabling...", Toast.LENGTH_LONG);
	}


	// ==========================================================================
	// MISC routines
	// ==========================================================================

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
			TableLayout table = configureMenus(getCurrentEntity(), landscape, TricorderAE.this);

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

	public static enum DisplayExtra {
		None, Level, Tag, Time
	}


	@Override
	public void onConfigurationChanged(Configuration newConfig) {

		super.onConfigurationChanged(newConfig);
		if (mDetailVisible) {
			boolean landscape = false;
			Display getOrient = getWindowManager().getDefaultDisplay();
			if (getOrient.getRotation() == Surface.ROTATION_90 || getOrient.getRotation() == Surface.ROTATION_270)
				landscape = true;
			TableLayout table = configureMenus(getCurrentEntity(), landscape, TricorderAE.this);
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
			this.mPrefTagLevelCutoff = Integer.parseInt(prefs.getString(Preferences.PREF_TAG_LEVEL_CUTOFF, "-100"));
			this.mPrefTagsWithEntitiesOnly = prefs.getBoolean(Preferences.PREF_TAGS_WITH_ENTITIES_ONLY, true);
			this.mPrefDisplayExtras = DisplayExtra.valueOf(prefs.getString(Preferences.PREF_DISPLAY_EXTRAS, "None"));
			this.mPrefTileScale = Float.parseFloat(prefs.getString(Preferences.PREF_TILE_SCALE, "1.0"));
			this.mPrefTileRotate = prefs.getBoolean(Preferences.PREF_TILE_ROTATE, true);
			this.mPrefSoundEffects = prefs.getBoolean(Preferences.PREF_SOUND_EFFECTS, true);
		}
	}


	private void configureProxiExplorer() {

		if (mProxiExplorer != null) {
			mProxiExplorer.setPrefTagLevelCutoff(mPrefTagLevelCutoff);
			mProxiExplorer.setPrefTagsWithEntitiesOnly(mPrefTagsWithEntitiesOnly);
		}
	}


	protected ProxiEntity getCurrentEntity() {

		return ((Aircandi) getApplicationContext()).currentEntityX;
	}


	protected void setCurrentEntity(ProxiEntity currentEntity) {

		((Aircandi) getApplicationContext()).currentEntityX = currentEntity;
	}


	protected void startProgress() {

		if (mProgressIndicator != null) {
			mProgressIndicator.setVisibility(View.VISIBLE);
			mButtonRefresh.setVisibility(View.INVISIBLE);
			mProgressIndicator.bringToFront();
			AnimationDrawable rippleAnimation = (AnimationDrawable) mProgressIndicator.getBackground();
			rippleAnimation.start();
			mProgressIndicator.invalidate();
		}
	}


	protected void stopProgress() {

		if (mProgressIndicator != null) {
			mProgressIndicator.setAnimation(null);
			mButtonRefresh.setVisibility(View.VISIBLE);
			mButtonRefresh.bringToFront();
			mProgressIndicator.setVisibility(View.INVISIBLE);
		}
	}


	@Override
	// Don't count on this always getting called when this activity is killed
	protected void onDestroy() {

		try {
			super.onDestroy();
			mProxiExplorer.onDestroy();
			mSoundEffects.Release();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}


	@Override
	protected void onPause() {

		try {
			super.onPause();
			if (mProxiExplorer != null)
				mProxiExplorer.onPause();
			if (mProxiView != null)
				mProxiView.onPause();
			stopProgress();
		}
		catch (Exception exception) {
			exception.printStackTrace();
		}
	}


	@Override
	protected void onResume() {

		super.onResume();
		if (mProxiExplorer != null)
			mProxiExplorer.onResume();
		if (mProxiView != null)
			mProxiView.onResume();

		// OnResume gets called after OnCreate (always) and whenever the activity is
		// being brought back to the foreground. Because code in OnCreate could have
		// determined that we aren't ready to roll, isReadyToRun is used to indicate
		// that prep work is complete.

		// This is also called when the user jumps out and back from setting preferences
		// so we need to refresh the places where they get used.

		if (mReadyToRun) {
			loadPreferences();
			configureProxiExplorer();

			// We always try to kick off a scan when radar is started or resumed
//			if (mProxiEntities != null && mProxiEntities.size() > 0)
//				clearProxiCandi();

			// Controller can be created because engine and scene have been instantiated
			if (mProxiController == null) {
				mProxiController = new ProxiUiHandler(this, this, mEngine, mCamera, mProxiEntities, mImageCache);
				mProxiController.setOnProxiEntityListener(new OnProxiEntityListener() {

					@Override
					public void onSelected(ProxiEntity proxiEntity) {

					}


					@Override
					public void onSingleTap(ProxiEntity proxiEntity) {

						showEntitySummary(proxiEntity);
					}
				});
			}
			
			if (mProxiController != null && mProxiController.mBusySprite != null)
				mProxiController.mBusySprite.setVisible(true);

			scanForBeacons(true, false);
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
				startActivity(new Intent(this, Preferences.class));
				return (true);
			default :
				return (super.onOptionsItemSelected(item));
		}
	}

}