package com.proxibase.aircandi.controllers;

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
import org.anddev.andengine.entity.modifier.AlphaModifier;
import org.anddev.andengine.entity.modifier.ParallelEntityModifier;
import org.anddev.andengine.entity.modifier.RotationAtModifier;
import org.anddev.andengine.entity.modifier.ScaleAtModifier;
import org.anddev.andengine.entity.modifier.IEntityModifier.IEntityModifierListener;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.opengl.view.RenderSurfaceView;
import org.anddev.andengine.util.modifier.IModifier;
import org.anddev.andengine.util.modifier.ease.EaseCircularIn;
import org.anddev.andengine.util.modifier.ease.EaseCircularOut;
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
import android.view.animation.AnimationUtils;
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

import com.proxibase.aircandi.candi.models.CandiModel;
import com.proxibase.aircandi.candi.models.CandiPatchModel;
import com.proxibase.aircandi.candi.models.IModel;
import com.proxibase.aircandi.candi.models.CandiModel.DisplayExtra;
import com.proxibase.aircandi.candi.presenters.CandiPatchPresenter;
import com.proxibase.aircandi.candi.presenters.CandiPatchPresenter.ICandiListener;
import com.proxibase.aircandi.candi.utils.CandiConstants;
import com.proxibase.aircandi.utils.ImageCache;
import com.proxibase.aircandi.utils.ImageManager;
import com.proxibase.aircandi.utils.Utilities;
import com.proxibase.sdk.android.proxi.consumer.ProxiEntity;
import com.proxibase.sdk.android.proxi.consumer.ProxiExplorer;
import com.proxibase.sdk.android.proxi.consumer.Stream;
import com.proxibase.sdk.android.proxi.consumer.ProxiExplorer.BaseBeaconScanListener;
import com.proxibase.sdk.android.proxi.service.ProxibaseError;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.util.ProxiConstants;

@SuppressWarnings("unused")
public class CandiSearchActivity extends AircandiGameActivity {

	private static String		COMPONENT_NAME				= "CandiSearch";

	private Boolean				mPrefAutoscan				= true;
	private int					mPrefAutoscanInterval		= 5000;
	private int					mPrefTagLevelCutoff			= -100;
	private boolean				mPrefTagsWithEntitiesOnly	= true;
	private DisplayExtra		mPrefDisplayExtras			= DisplayExtra.None;
	private float				mPrefTileScale				= 1.0f;
	private boolean				mPrefTileRotate;
	private boolean				mPrefSoundEffects;

	private Boolean				mReadyToRun					= false;
	private Handler				mHandler					= new Handler();
	private CxMediaPlayer		mSoundEffects;
	private MediaPlayer			mMediaPlayer;

	private ProxiExplorer		mProxiExplorer;
	private List<ProxiEntity>	mProxiEntities;

	private RenderSurfaceView	mCandiPatchSurfaceView;
	private CandiPatchModel		mCandiPatchModel;
	private CandiPatchPresenter	mCandiPatchPresenter;

	protected ImageView			mProgressIndicator;
	protected ImageView			mButtonRefresh;
	private LinearLayout		mCandiContainer;
	private FrameLayout			mCandiSummaryView;
	private FrameLayout			mCandiSearchView;

	private boolean				mCandiSummaryVisible		= false;
	private boolean				mFirstRun					= true;
	private ScreenOrientation	mScreenOrientation			= ScreenOrientation.PORTRAIT;

	private boolean				mUserRefusedWifiEnable		= false;
	private int					mRenderMode					= RenderSurfaceView.RENDERMODE_WHEN_DIRTY;

	/**
	 * Called when the activity is first created.View
	 */

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Utilities.Log(Aircandi.APP_NAME, COMPONENT_NAME, "Activity.onCreate called");
		super.onCreate(savedInstanceState);

		// setContentView is call from parent class and passes the
		// layout specified in getLayoutId();

		initialize();
	}

	private void initialize() {

		// Start normal processing
		mReadyToRun = false;

		// Image cache
		ImageManager.getImageManager().setImageCache(new ImageCache(getApplicationContext(), "aircandi", 100, 16));
		ImageManager.getImageManager().setContext(getApplicationContext());

		// Sound resources
		mMediaPlayer = new MediaPlayer();
		mSoundEffects = new CxMediaPlayer(this);

		// Ui Hookup
		mCandiContainer = (LinearLayout) findViewById(R.id.CandiContainer);
		mCandiSummaryView = (FrameLayout) findViewById(R.id.CandiSummaryView);
		mCandiSearchView = (FrameLayout) findViewById(R.id.CandiSearchView);
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

		// Use a surface format with an Alpha channel
		super.mRenderSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);

		// Make sure the proxi view rendering surface is on top otherwise the
		// surface view cuts a hole through anything above it in the zorder.
		super.mRenderSurfaceView.setZOrderOnTop(true);
		// super.mRenderSurfaceView.setZOrderMediaOverlay(true);

		// TODO: Rendering only when dirty is more battery efficient
		super.mRenderSurfaceView.setRenderMode(mRenderMode);

		// Property settings get overridden once we retrieve preferences
		mCandiPatchSurfaceView = (RenderSurfaceView) findViewById(R.id.ProxiView);
		mCandiPatchSurfaceView.requestFocus();
		mCandiPatchSurfaceView.setFocusableInTouchMode(true);

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
				mMediaPlayer.setDataSource(CandiSearchActivity.this, uri);
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
		if (!mCandiSummaryVisible) {
			if (mCandiPatchModel.getCandiModelFocused().isGrouped() && mCandiPatchModel.getCandiRootPrevious().isSuperRoot())
				mCandiPatchPresenter.navigateModel(mCandiPatchModel.getCandiRootPrevious(), true);
		}
		else {
			mMediaPlayer.stop();
			mMediaPlayer.reset();
			hideCandiSummaryView();
			mCandiSummaryVisible = false;
		}
	}

	public void onRefreshClick(View view) {
		// For this activity, refresh means rescan and reload proxi entity data from the service
		if (mReadyToRun) {
			scanForBeacons(false, true);
		}
	}

	public void onSummaryViewClick(View v) {
		mMediaPlayer.stop();
		mMediaPlayer.reset();
		hideCandiSummaryView();
		mCandiSummaryVisible = false;
	}

	public void onHomeClick(View view) {
		Intent intent = new Intent(this, CandiSearchActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	public void onSearchClick(View view) {
		AircandiUI.showToastNotification(this, "Unimplemented...", Toast.LENGTH_SHORT);
		return;
	}

	// ==========================================================================
	// SCAN AND DISPLAY routines
	// ==========================================================================

	private void scanForBeacons(final Boolean fullUpdate, Boolean showProgress) {

		if (showProgress)
			startTitlebarProgress();

		if (fullUpdate) {
			mCandiPatchPresenter.setFullUpdateInProgress(true);
			mCandiPatchPresenter.mProgressSprite.setVisible(true);
		}

		Utilities.Log(Aircandi.APP_NAME, COMPONENT_NAME, "Calling Proxibase.ProxiExplorer to scan for tags");

		mProxiExplorer.scanForBeacons(fullUpdate, new BaseBeaconScanListener() {

			@Override
			public void onComplete(List<ProxiEntity> entities) {

				Utilities.Log(Aircandi.APP_NAME, COMPONENT_NAME, "Beacon scan results returned from Proxibase.ProxiExplorer");
				Utilities.Log(Aircandi.APP_NAME, COMPONENT_NAME, "Entity count: " + String.valueOf(entities.size()));

				// try {

				// Scanning is complete so change the heading back to normal
				// Show search message if there aren't any current points
				TextView message = (TextView) findViewById(R.id.Tricorder_Message);
				if (message != null) {
					message.setVisibility(View.GONE);
				}

				// Replace the collection
				Utilities.Log(Aircandi.APP_NAME, COMPONENT_NAME, "Refreshing RippleView");

				// Shallow copy so the entities are by value but any object
				// properties like beacon are by ref from the original.
				mProxiEntities = (List<ProxiEntity>) ((ArrayList<ProxiEntity>) entities).clone();

				// Set url for images
				for (ProxiEntity proxiEntity : mProxiEntities)
					proxiEntity.imageUrl = ProxiConstants.URL_PROXIBASE_MEDIA + "3meters_images/" + proxiEntity.pointResourceId;

				// Push the new and updated proxi entities into the system
				mCandiPatchPresenter.updateCandiData(mProxiEntities, fullUpdate);

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
				Utilities.Log(Aircandi.APP_NAME, COMPONENT_NAME, exception.getMessage());
				AircandiUI.showToastNotification(CandiSearchActivity.this, "Network error", Toast.LENGTH_SHORT);
				stopTitlebarProgress();
				exception.printStackTrace();
			}

			@Override
			public void onProxiExplorerError(ProxibaseError error) {
				AircandiUI.showToastNotification(CandiSearchActivity.this, error.getMessage(), Toast.LENGTH_SHORT);
				Utilities.Log(Aircandi.APP_NAME, COMPONENT_NAME, error.getMessage());
				stopTitlebarProgress();
			}
		});
	}

	private void setupSummary(final CandiModel candiModel) {

		new BuildMenuTask().execute(mCandiSummaryView);

		if (candiModel.getProxiEntity().pointResourceId != null && candiModel.getProxiEntity().pointResourceId != "") {
			if (ImageManager.getImageManager().hasImage(candiModel.getProxiEntity().pointResourceId)) {
				Bitmap bitmap = ImageManager.getImageManager().getImage(candiModel.getProxiEntity().pointResourceId);
				((ImageView) mCandiSummaryView.findViewById(R.id.Image)).setImageBitmap(bitmap);
			}
			if (ImageManager.getImageManager().hasImage(candiModel.getProxiEntity().pointResourceId + ".reflection")) {
				Bitmap bitmap = ImageManager.getImageManager().getImage(candiModel.getProxiEntity().pointResourceId + ".reflection");
				((ImageView) mCandiSummaryView.findViewById(R.id.ImageReflection)).setImageBitmap(bitmap);
			}
		}

		((TextView) mCandiSummaryView.findViewById(R.id.Title)).setText(candiModel.getProxiEntity().title);
		((TextView) mCandiSummaryView.findViewById(R.id.Subtitle)).setText(Html.fromHtml(candiModel.getProxiEntity().subtitle));
		((TextView) mCandiSummaryView.findViewById(R.id.Description)).setText(Html.fromHtml(candiModel.getProxiEntity().description));
	}

	private void hideCandiSummaryView() {

		// Find the center of the container
		final float centerX = mCandiContainer.getWidth() / 2.0f;
		final float centerY = mCandiContainer.getHeight() / 2.0f;

		// Create a new 3D rotation with the supplied parameter
		// The animation listener is used to trigger the next animation
		final Rotate3dAnimation rotation = new Rotate3dAnimation(360, 270, centerX, centerY, 310.0f, true);
		rotation.setDuration(750);
		rotation.setFillAfter(true);
		rotation.setInterpolator(new AccelerateInterpolator());

		rotation.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationEnd(Animation animation) {

				mCandiContainer.post(new Runnable() {

					@Override
					public void run() {

						final float centerX = mCandiContainer.getWidth() / 2.0f;
						final float centerY = mCandiContainer.getHeight() / 2.0f;

						mCandiSummaryView.setVisibility(View.GONE);
						mCandiSearchView.setVisibility(View.VISIBLE);
						mCandiPatchSurfaceView.setVisibility(View.VISIBLE);

						float rotationX = mCandiPatchModel.getCandiModelSelected().getZoneCurrent().getCenterX();
						float rotationY = mCandiPatchModel.getCandiModelSelected().getZoneCurrent().getCenterY();

						mEngine.getScene().registerEntityModifier(
								new ParallelEntityModifier(new RotationAtModifier(1f, 90f, 0.0f, rotationX, rotationY, EaseCubicOut.getInstance()),
										new ScaleAtModifier(1f, 0.5f, 1.0f, rotationX, rotationY, EaseCubicOut.getInstance()), new AlphaModifier(1f,
												0.0f, 1.0f, EaseCircularOut.getInstance())));

						// Show context button area
						updateCandiBackButton();

						// Clear the current entity
						mCandiPatchModel.setCandiModelSelected(null);

						// Allow proxi input again
						mCandiPatchPresenter.mIgnoreInput = false;

					}
				});

			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationStart(Animation animation) {
			}
		});

		mCandiContainer.startAnimation(rotation);
	}

	private void showCandiSummary(final CandiModel candi) {

		mCandiPatchModel.setCandiModelSelected(candi);
		mCandiSummaryVisible = true;

		setupSummary(candi);
		float rotationX = mCandiPatchModel.getCandiModelSelected().getZoneCurrent().getCenterX();
		float rotationY = mCandiPatchModel.getCandiModelSelected().getZoneCurrent().getCenterY();

		// setCandiBackButtonVisible(false);
		mEngine.getScene().registerEntityModifier(
				new ParallelEntityModifier(new IEntityModifierListener() {

					@Override
					public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {

						CandiSearchActivity.this.runOnUiThread(new Runnable() {

							public void run() {

								// mEngine.stop();

								final float centerX = mCandiContainer.getWidth() / 2.0f;
								final float centerY = mCandiContainer.getHeight() / 2.0f;

								// Setting to search view to invisible instead of gone doesn't work. The Summary View
								// isn't visible.
								mCandiPatchSurfaceView.setVisibility(View.GONE);
								mCandiSearchView.setVisibility(View.GONE);
								mCandiSummaryView.setVisibility(View.VISIBLE);

								updateCandiBackButton();

								mCandiSummaryView.requestFocus();

								Rotate3dAnimation rotation = new Rotate3dAnimation(270, 360, centerX, centerY, 310.0f, false);

								rotation.setDuration(500);
								rotation.setFillAfter(true);
								rotation.setInterpolator(new DecelerateInterpolator());

								mCandiContainer.startAnimation(rotation);
							}
						});

					}

				}, new RotationAtModifier(1f, 0, 90, rotationX, rotationY, EaseCubicIn.getInstance()), new ScaleAtModifier(1f, 1, 0.3f, rotationX,
						rotationY, EaseCubicIn.getInstance()), new AlphaModifier(1f, 1.0f, 0.0f, EaseCircularIn.getInstance())));

	}

	private TableLayout configureMenus(CandiModel candi, boolean landscape, Context context) {

		Boolean needMoreButton = false;
		if (candi.getProxiEntity().streams.size() > 6)
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
		for (Stream stream : candi.getProxiEntity().streams) {
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

	// ==========================================================================
	// ENGINE routines
	// ==========================================================================

	@Override
	protected int getLayoutID() {
		return R.layout.candi_search;
	}

	@Override
	protected int getRenderSurfaceViewID() {
		return R.id.ProxiView;
	}

	@Override
	public void onLoadComplete() {
		Utilities.Log(Aircandi.APP_NAME, COMPONENT_NAME, "Activity.onLoadComplete called");
	}

	@Override
	public Engine onLoadEngine() {

		Utilities.Log(Aircandi.APP_NAME, COMPONENT_NAME, "onLoadEngine called");
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		int rotation = getWindowManager().getDefaultDisplay().getRotation();
		if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270)
			mScreenOrientation = ScreenOrientation.LANDSCAPE;
		else
			mScreenOrientation = ScreenOrientation.PORTRAIT;

		Camera camera = new Camera(0, 0, dm.widthPixels, dm.heightPixels - CandiConstants.CANDI_TITLEBAR_HEIGHT) {

			@Override
			public void onApplyMatrix(GL10 pGL) {
				// Gets called for every engine update
				mCandiPatchPresenter.setFrustum(pGL);
			}

			@Override
			public void onApplyPositionIndependentMatrix(GL10 pGL) {
				mCandiPatchPresenter.setFrustum(pGL);
			}

			@Override
			public void onApplyCameraSceneMatrix(GL10 pGL) {
				mCandiPatchPresenter.setFrustum(pGL);
			}
		};

		EngineOptions options = new EngineOptions(true, mScreenOrientation, new FillResolutionPolicy(), camera);
		options.getRenderOptions().disableExtensionVertexBufferObjects();
		Engine engine = new Engine(options);

		return engine;
	}

	@Override
	public void onLoadResources() {
		Utilities.Log(Aircandi.APP_NAME, COMPONENT_NAME, "onLoadResources called");
	}

	@Override
	public Scene onLoadScene() {
		/*
		 * Called after Create, Resume->LoadEngine.
		 * CandiPatchPresenter handles scene instantiation and setup
		 */
		Utilities.Log(Aircandi.APP_NAME, COMPONENT_NAME, "onLoadScene called");
		mCandiPatchPresenter = new CandiPatchPresenter(this, this, mEngine, mRenderSurfaceView, mCandiPatchModel);
		Scene scene = mCandiPatchPresenter.initScene();
		mCandiPatchPresenter.mDisplayExtras = this.mPrefDisplayExtras;
		mCandiPatchModel.addObserver(mCandiPatchPresenter);
		mCandiPatchPresenter.setCandiListener(new ICandiListener() {

			@Override
			public void onSelected(IModel candi) {}

			@Override
			public void onSingleTap(CandiModel candi) {
				mCandiPatchPresenter.mIgnoreInput = true;
				showCandiSummary(candi);
			}
		});

		return scene;
	}

	@Override
	public void onGameResumed() {
		/*
		 * Last event to be fired when bringing everything up to speed.
		 */
		Utilities.Log(Aircandi.APP_NAME, COMPONENT_NAME, "Activity.onGameResumed called");
		scanForBeacons(this.mFirstRun, false);
		if (mFirstRun)
			mFirstRun = false;
	}

	@Override
	public void onGamePaused() {
		Utilities.Log(Aircandi.APP_NAME, COMPONENT_NAME, "Activity.onGamePaused called");
	}

	@Override
	public void onUnloadResources() {
		super.onUnloadResources();
		Utilities.Log(Aircandi.APP_NAME, COMPONENT_NAME, "Activity.onUnloadResources called");
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

			mCandiContainer.post(new SwapViews(rotateRight_));
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

			final float centerX = mCandiContainer.getWidth() / 2.0f;
			final float centerY = mCandiContainer.getHeight() / 2.0f;
			Rotate3dAnimation rotation;

			if (rotateRight_) {
				// mCandiPatchLayoutView.setVisibility(View.GONE);
				mCandiSearchView.setVisibility(View.GONE);
				mCandiSummaryView.setVisibility(View.VISIBLE);
				mCandiSummaryView.requestFocus();

				rotation = new Rotate3dAnimation(270, 360, centerX, centerY, 310.0f, false);
			}
			else {
				mCandiSummaryView.setVisibility(View.GONE);
				mCandiSearchView.setVisibility(View.VISIBLE);
				mCandiSearchView.requestFocus();
				// mCandiPatchLayoutView.setVisibility(View.VISIBLE);
				// mCandiPatchLayoutView.requestFocus();

				rotation = new Rotate3dAnimation(90, 0, centerX, centerY, 310.0f, false);
			}

			rotation.setDuration(500);
			rotation.setFillAfter(true);
			rotation.setInterpolator(new DecelerateInterpolator());

			mCandiContainer.startAnimation(rotation);
		}
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
		 * OnResume gets called after OnCreate (always) and whenever the activity is
		 * being brought back to the foreground. Because code in OnCreate could have
		 * determined that we aren't ready to roll, isReadyToRun is used to indicate
		 * that prep work is complete.
		 * This is also called when the user jumps out and back from setting preferences
		 * so we need to refresh the places where they get used.
		 * Game engine is started/restarted in super class.
		 */

		Utilities.Log(Aircandi.APP_NAME, COMPONENT_NAME, "Activity.onResume called");

		if (mProxiExplorer != null)
			mProxiExplorer.onResume();

		if (mReadyToRun) {
			loadPreferences();
			loadPreferencesProxiExplorer();

			if (mCandiPatchModel.getCandiModels().size() > 0) {
				mCandiPatchPresenter.resetTextures();
				mCandiPatchPresenter.navigateModel(mCandiPatchModel.getCandiRootCurrent(), true);
				mEngine.getScene().registerEntityModifier(new AlphaModifier(1f, 0.0f, 1.0f, EaseCircularIn.getInstance()));
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		try {
			Utilities.Log(Aircandi.APP_NAME, COMPONENT_NAME, "Activity.onPause called");

			if (mEngine.getScene() != null)
				mEngine.getScene().setAlpha(0);

			if (mProxiExplorer != null)
				mProxiExplorer.onPause();

			if (mCandiPatchSurfaceView != null)
				mCandiPatchSurfaceView.onPause();

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
			Utilities.Log(Aircandi.APP_NAME, COMPONENT_NAME, "Activity.onDestroy called");
			mProxiExplorer.onDestroy();
			mSoundEffects.Release();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {

		Utilities.Log(Aircandi.APP_NAME, COMPONENT_NAME, "onConfigurationChanged called: " + newConfig.orientation);

		boolean landscape = false;
		Display getOrient = getWindowManager().getDefaultDisplay();
		if (getOrient.getRotation() == Surface.ROTATION_90 || getOrient.getRotation() == Surface.ROTATION_270)
			landscape = true;

		super.onConfigurationChanged(newConfig);
		if (mCandiSummaryVisible) {
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
			this.mPrefTagLevelCutoff = Integer.parseInt(prefs.getString(Preferences.PREF_TAG_LEVEL_CUTOFF, "-100"));
			this.mPrefTagsWithEntitiesOnly = prefs.getBoolean(Preferences.PREF_TAGS_WITH_ENTITIES_ONLY, true);
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
			mProxiExplorer.setPrefTagLevelCutoff(mPrefTagLevelCutoff);
			mProxiExplorer.setPrefTagsWithEntitiesOnly(mPrefTagsWithEntitiesOnly);
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

	@Override
	public void onWindowFocusChanged(final boolean pHasWindowFocus) {
		super.onWindowFocusChanged(pHasWindowFocus);
		Utilities.Log(Aircandi.APP_NAME, COMPONENT_NAME, "Activity.onWindowFocusChanged called");
	}

	public void updateCandiBackButton() {

		boolean visible = (mCandiSummaryVisible || !mCandiPatchModel.getCandiRootCurrent().isSuperRoot());

		if (visible) {
			mContextButton.setText("Back");
			mContextButton.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					if (mCandiSummaryVisible) {
						onSummaryViewClick(v);
					}
					else {
						mCandiPatchPresenter.navigateModel(mCandiPatchModel.getCandiRootPrevious(), true);
					}
				}
			});

			if (mContextButtonState == ContextButtonState.Default) {
				Animation animation = AnimationUtils.loadAnimation(CandiSearchActivity.this, R.anim.fade_in_normal);
				animation.setFillEnabled(true);
				animation.setFillAfter(true);
				animation.setStartOffset(500);
				mContextButton.startAnimation(animation);
			}

			if (mCandiSummaryVisible)
				mContextButtonState = ContextButtonState.HideSummary;
			else
				mContextButtonState = ContextButtonState.NavigateBack;
		}
		else {

			// mContextButton.setText("");
			mContextButton.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					onHomeClick(v);
				}
			});

			if (mContextButtonState != ContextButtonState.Default) {
				Animation animation = AnimationUtils.loadAnimation(CandiSearchActivity.this, R.anim.fade_out_normal);
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
				startActivity(new Intent(this, Preferences.class));
				return (true);
			default :
				return (super.onOptionsItemSelected(item));
		}
	}
}