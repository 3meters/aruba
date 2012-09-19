package com.aircandi.candi.presenters;

import static org.anddev.andengine.util.constants.Constants.VERTEX_INDEX_X;
import static org.anddev.andengine.util.constants.Constants.VERTEX_INDEX_Y;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.microedition.khronos.opengles.GL10;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.modifier.DelayModifier;
import org.anddev.andengine.entity.modifier.IEntityModifier.IEntityModifierListener;
import org.anddev.andengine.entity.modifier.MoveModifier;
import org.anddev.andengine.entity.modifier.ParallelEntityModifier;
import org.anddev.andengine.entity.modifier.ScaleModifier;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.Scene.IOnSceneTouchListener;
import org.anddev.andengine.entity.scene.background.ColorBackground;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.texture.Texture;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.opengl.texture.region.TextureRegionFactory;
import org.anddev.andengine.opengl.util.GLHelper;
import org.anddev.andengine.opengl.view.RenderSurfaceView;
import org.anddev.andengine.util.modifier.IModifier;
import org.anddev.andengine.util.modifier.ease.EaseQuartInOut;
import org.anddev.andengine.util.pool.GenericPool;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.opengl.GLU;
import android.os.AsyncTask;
import android.util.FloatMath;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import com.aircandi.Aircandi;
import com.aircandi.CandiRadar;
import com.aircandi.Preferences;
import com.aircandi.candi.camera.ChaseCamera;
import com.aircandi.candi.models.CandiModel;
import com.aircandi.candi.models.CandiModelFactory;
import com.aircandi.candi.models.CandiPatchModel;
import com.aircandi.candi.models.IModel;
import com.aircandi.candi.models.ZoneModel;
import com.aircandi.candi.models.BaseModel.ViewState;
import com.aircandi.candi.models.CandiModel.ReasonInactive;
import com.aircandi.candi.models.CandiModel.Transition;
import com.aircandi.candi.models.ZoneModel.ZoneStatus;
import com.aircandi.candi.modifiers.CandiAlphaModifier;
import com.aircandi.candi.sprites.CameraTargetSprite;
import com.aircandi.candi.sprites.CandiAnimatedSprite;
import com.aircandi.candi.sprites.CandiScene;
import com.aircandi.candi.views.CandiView;
import com.aircandi.candi.views.IView;
import com.aircandi.candi.views.ViewAction;
import com.aircandi.candi.views.ZoneView;
import com.aircandi.candi.views.IView.ViewTouchListener;
import com.aircandi.candi.views.ViewAction.ViewActionType;
import com.aircandi.components.BitmapTextureSource;
import com.aircandi.components.CandiArrayList;
import com.aircandi.components.CountDownTimer;
import com.aircandi.components.DateUtils;
import com.aircandi.components.EntityList;
import com.aircandi.components.ImageManager;
import com.aircandi.components.ImageRequestBuilder;
import com.aircandi.components.ImageUtils;
import com.aircandi.components.Logger;
import com.aircandi.components.BitmapTextureSource.IBitmapAdapter;
import com.aircandi.components.ImageRequest.ImageShape;
import com.aircandi.core.CandiConstants;
import com.aircandi.service.objects.Entity;
import com.aircandi.BuildConfig;
import com.aircandi.R;

public class CandiPatchPresenter implements Observer {

	public static float				SCALE_NORMAL			= 1;

	public CandiPatchModel			mCandiPatchModel;
	private HashMap					mCandiViewsActiveHash	= new HashMap();
	public List<ZoneView>			mZoneViews				= new ArrayList<ZoneView>();
	private CandiViewPool			mCandiViewPool;
	private Boolean					mRookieHit				= false;

	private GestureDetector			mGestureDetector;
	public boolean					mIgnoreInput			= false;
	private Context					mContext;
	private Engine					mEngine;
	public CandiAnimatedSprite		mProgressSprite;
	public Rectangle				mHighlight;
	private float					mLastMotionX;
	private float					mLastMotionY;
	private CameraTargetSprite		mCameraTargetSprite;
	private float					mBoundsMinX;
	private float					mBoundsMaxX;
	private float					mBoundsMinY;
	private float					mBoundsMaxY;

	private float					mRadarWidth;
	private float					mRadarHeight;

	public static float				mRadarPaddingLeft;
	public static float				mRadarPaddingRight;
	public static float				mRadarPaddingTop;
	public static float				mRadarPaddingBottom;

	private ChaseCamera				mCamera;
	private Scene					mScene;
	public Bitmap					mBitmapBadgeCollections;
	public Bitmap					mBitmapBadgePosts;

	public Texture					mTexture;
	public TextureRegion			mCandiBodyTextureRegion;
	public TextureRegion			mCandiReflectionTextureRegion;
	public TextureRegion			mZoneBodyTextureRegion;
	public TextureRegion			mZoneReflectionTextureRegion;

	public CandiRadar				mCandiRadarActivity;
	public RenderSurfaceView		mRenderSurfaceView;
	private ManageViewsThread		mManageViewsThread;

	private ICandiListener			mCandiListener;
	public int						mTouchSlopSquare;
	public int						mDoubleTapSlopSquare;
	protected float					mTouchStartX;
	protected float					mTouchStartY;
	protected boolean				mTouchGrabbed;
	private boolean					mRenderingActive		= true;
	private RenderCountDownTimer	mRenderingTimer;

	private Integer					mStyleTextureBodyZoneResId;
	private Integer					mStyleTextureBodyCandiResId;

	private String					mStyleTextColorTitle;
	private Boolean					mStyleTextOutlineTitle	= false;

	// --------------------------------------------------------------------------------------------
	// Initialization
	// --------------------------------------------------------------------------------------------

	public CandiPatchPresenter(Context context, Activity activity, Engine engine, RenderSurfaceView renderSurfaceView, CandiPatchModel candiPatchModel) {

		mCandiPatchModel = candiPatchModel;
		mContext = context;
		mEngine = engine;
		mCamera = (ChaseCamera) engine.getCamera();
		mRenderSurfaceView = renderSurfaceView;
		mCandiRadarActivity = (CandiRadar) activity;
		mGestureDetector = new GestureDetector(mContext, new SingleTapGestureDetector());

		/* Rendering timer */
		mRenderingTimer = new RenderCountDownTimer(CandiConstants.INTERVAL_RENDERING_DEFAULT, 500);

		initialize();
	}

	private void initialize() {

		/* Gestures */
		final ViewConfiguration configuration = ViewConfiguration.get(mContext);

		int touchSlop = configuration.getScaledTouchSlop();
		int doubleTapSlop = configuration.getScaledDoubleTapSlop();

		mTouchSlopSquare = (touchSlop * touchSlop) / 4;
		mDoubleTapSlopSquare = doubleTapSlop * doubleTapSlop;

		/* Pooling */
		mCandiViewPool = new CandiViewPool(0, 1);

		/* Resource references per theme */
		loadStyles();

		/* Textures that get shared across candi and zone views */
		loadTextureSources();

		/* Bitmaps that should be cached for multiple use */
		loadBitmapBadges();
	}

	public Scene initializeScene() {
		Logger.v(this, "initScene called");

		final CandiScene scene = new CandiScene(3) {

			@Override
			protected void applyRotation(final GL10 pGL) {

				/* Disable culling so we can see the backside of this sprite. */
				GLHelper.disableCulling(pGL);

				final float rotation = mRotation;

				if (rotation != 0) {
					final float rotationCenterX = mRotationCenterX;
					final float rotationCenterY = mRotationCenterY;

					pGL.glTranslatef(rotationCenterX, rotationCenterY, 0);

					/*
					 * Note we are applying rotation around the y-axis and not the z-axis anymore!
					 */
					pGL.glRotatef(rotation, 0, 1, 0);
					pGL.glTranslatef(-rotationCenterX, -rotationCenterY, 0);
				}
			}
		};

		scene.setBackground(new ColorBackground(0, 0, 0, 0)); /* Transparent */
		scene.setTouchAreaBindingEnabled(true);

		float contentWidth = (CandiConstants.CANDI_VIEW_WIDTH * CandiConstants.RADAR_STACK_COUNT)
				+ (CandiConstants.CANDI_VIEW_SPACING_VERTICAL * (CandiConstants.RADAR_STACK_COUNT - 1));
		float paddingTotal = getRadarZoomedWidth() - contentWidth;

		mRadarPaddingLeft = paddingTotal * 0.5f;
		mRadarPaddingRight = paddingTotal - mRadarPaddingLeft;
		mRadarPaddingTop = CandiConstants.RADAR_PADDING_TOP;
		mRadarPaddingBottom = CandiConstants.RADAR_PADDING_BOTTOM;

		{
			/* Highlight */
			mHighlight = new Rectangle(0, 0, 260, 260);
			mHighlight.setColor(1.0f, 0.7f, 0f, 1.0f);
			mHighlight.setVisible(false);
			scene.getChild(CandiConstants.LAYER_GENERAL).attachChild(mHighlight);

			/* Invisible entity used to scroll */
			mCameraTargetSprite = new CameraTargetSprite((getRadarZoomedWidth() * 0.5f), (getRadarZoomedHeight() * 0.5f), 0, 0, this);
			mCameraTargetSprite.setColor(1, 0, 0, 0.2f);
			mCameraTargetSprite.setBlendFunction(CandiConstants.GL_BLEND_FUNCTION_SOURCE, CandiConstants.GL_BLEND_FUNCTION_DESTINATION);
			mCameraTargetSprite.setVisible(false);
			mCameraTargetSprite.setScale(CandiConstants.RADAR_ZOOM);
			scene.getChild(CandiConstants.LAYER_GENERAL).attachChild(mCameraTargetSprite);

			/* Tie camera position to target position. */
			mCamera.setChaseEntity(mCameraTargetSprite);

			/* Scene touch handling */
			scene.setOnSceneTouchListener(new IOnSceneTouchListener() {

				@Override
				public boolean onSceneTouchEvent(final Scene pScene, final TouchEvent pSceneTouchEvent) {

					/*
					 * This gets called even when the touch is targeting a candiview.
					 * 
					 * TouchEvent is world coordinates
					 * MotionEvent is screen coordinates
					 */
					renderingActivateBump();
					if (Aircandi.getInstance().isRadarUpdateInProgress() || mIgnoreInput) {
						return true;
					}

					if (CandiConstants.RADAR_SCROLL_HORIZONTAL) {

						final float screenX = pSceneTouchEvent.getMotionEvent().getX();

						/*
						 * Check for a fling or double tap using the gesture detector
						 */
						if (mGestureDetector.onTouchEvent(pSceneTouchEvent.getMotionEvent())) {
							return true;
						}

						if (pSceneTouchEvent.isActionDown()) {
							mLastMotionX = screenX;
							mCameraTargetSprite.clearEntityModifiers();
							return true;
						}
						else if (pSceneTouchEvent.isActionUp()) {
							return true;
						}
						else if (pSceneTouchEvent.isActionMove()) {
							if (mHighlight.isVisible()) {
								mHighlight.setVisible(false);
							}

							float scrollX = mLastMotionX - screenX;
							scrollX /= mCameraTargetSprite.getScaleX();
							float cameraTargetX = mCameraTargetSprite.getX();

							if (Math.abs(scrollX) >= 1) {
								mCameraTargetSprite.setPosition(cameraTargetX + scrollX, mCameraTargetSprite.getY());
							}
							mLastMotionX = screenX;
							return true;
						}
						return false;
					}
					else {
						final float screenY = pSceneTouchEvent.getMotionEvent().getY();

						/*
						 * Check for a fling or double tap using the gesture detector
						 */
						if (mGestureDetector.onTouchEvent(pSceneTouchEvent.getMotionEvent())) {
							return true;
						}

						if (pSceneTouchEvent.isActionDown()) {
							mLastMotionY = screenY;
							mCameraTargetSprite.clearEntityModifiers();
							return true;
						}
						else if (pSceneTouchEvent.isActionUp()) {
							/*
							 * Check to see if we ended up outside a boundary
							 */
							renderingActivateBump();
							float cameraTargetY = mCameraTargetSprite.getY();
							if (cameraTargetY <= mBoundsMinY) {
								mCameraTargetSprite.moveToTop(CandiConstants.DURATION_BOUNCEBACK, CandiConstants.EASE_BOUNCE_BACK, null);
								return false;
							}
							else if (cameraTargetY >= mBoundsMaxY) {
								mCameraTargetSprite.moveToBottom(CandiConstants.DURATION_BOUNCEBACK, CandiConstants.EASE_BOUNCE_BACK, null);
								return false;
							}
							return true;
						}
						else if (pSceneTouchEvent.isActionMove()) {
							if (mHighlight.isVisible()) {
								mHighlight.setVisible(false);
							}

							float scrollY = mLastMotionY - screenY;
							scrollY /= mCameraTargetSprite.getScaleY();
							float cameraTargetY = mCameraTargetSprite.getY();

							if (Math.abs(scrollY) >= 1) {
								mCameraTargetSprite.setPosition(mCameraTargetSprite.getX(), cameraTargetY + scrollY);
							}
							mLastMotionY = screenY;
							return true;
						}
						return false;
					}
				}
			});
		}

		mScene = scene;

		/* Render if dirty */
		mEngine.registerUpdateHandler(new IUpdateHandler() {

			@Override
			public void onUpdate(float pSecondsElapsed) {
				if (mRenderingActive) {
					mRenderSurfaceView.requestRender();
				}
			}

			@Override
			public void reset() {

			}
		});

		return scene;
	}

	private void loadStyles() {
		TypedValue resourceName = new TypedValue();
		if (mContext.getTheme().resolveAttribute(R.attr.textureBodyZone, resourceName, true)) {
			mStyleTextureBodyZoneResId = (Integer) resourceName.resourceId;
		}

		if (mContext.getTheme().resolveAttribute(R.attr.textureBodyCandi, resourceName, true)) {
			mStyleTextureBodyCandiResId = (Integer) resourceName.resourceId;
		}

		if (mContext.getTheme().resolveAttribute(R.attr.textColorRadar, resourceName, true)) {
			mStyleTextColorTitle = (String) resourceName.coerceToString();
		}

		if (mContext.getTheme().resolveAttribute(R.attr.textOutlineRadar, resourceName, true)) {
			mStyleTextOutlineTitle = Boolean.parseBoolean((String) resourceName.coerceToString());
		}
	}

	private void loadBitmapBadges() {
		/*
		 * Much more efficient to do this once and cache it.
		 */
		String resolvedResourceName = ImageManager.getInstance().resolveResourceName("ic_collection_250");
		int resourceId = mCandiRadarActivity.getResources().getIdentifier(resolvedResourceName, "drawable", "com.aircandi");
		mBitmapBadgeCollections = ImageManager.getInstance().loadBitmapFromResources(resourceId);
		mBitmapBadgeCollections = ImageUtils.scaleAndCropBitmap(mBitmapBadgeCollections, CandiConstants.CANDI_VIEW_BADGE_WIDTH, ImageShape.Square);

		resolvedResourceName = ImageManager.getInstance().resolveResourceName("ic_post_v2_250");
		resourceId = mCandiRadarActivity.getResources().getIdentifier(resolvedResourceName, "drawable", "com.aircandi");
		mBitmapBadgePosts = ImageManager.getInstance().loadBitmapFromResources(resourceId);
		mBitmapBadgePosts = ImageUtils.scaleAndCropBitmap(mBitmapBadgePosts, CandiConstants.CANDI_VIEW_BADGE_WIDTH, ImageShape.Square);

	}

	// --------------------------------------------------------------------------------------------
	// Primary
	// --------------------------------------------------------------------------------------------

	public void updateCandiData(EntityList<Entity> entitiesCopy, boolean fullBuild, boolean delayObserverUpdate) {
		/*
		 * Push the new and updated entities into the system. Updates all the models and views.
		 * Callers should pass a copy to protect from any asynch changes to the entity model while we are
		 * updating the dependent candi models and views.
		 * 
		 * We also want copies of the entities so we can look for changes when refreshing
		 * the candi model.
		 */

		/* Check to see if there is a brand new entity in the collection */
		mRookieHit = rookieHit(entitiesCopy);

		/* We want all new discoveries on this pass to get the same date */
		Date discoveryTime = DateUtils.nowDate();

		renderingActivateBump();
		if (fullBuild) {
			/*
			 * Clears all game engine sprites. Clears all touch areas. Clears zone and candi model collections. Reloads
			 * shared textures. Creates new root candi model
			 */
			Logger.d(this, "Starting full build.");
			mCandiViewsActiveHash.clear();
			mZoneViews.clear();
			clearCandiLayer();
			clearZoneLayer();
			mEngine.getScene().clearTouchAreas();
			mCandiPatchModel.reset(); /* Clears zone and candi model collections */
			initialize(); /* Reloads shared textures */
		}
		else {
			Logger.d(this, "Starting standard update.");

			/*
			 * Clear out models that have previously marked for deletion. For models and any children: - Recycles the
			 * candi view if the model currently has one. - Removes associated images from the cache. - Removed model
			 * from primary candi model collection.
			 */
			for (int i = mCandiPatchModel.getCandiModels().size() - 1; i >= 0; i--) {
				if (mCandiPatchModel.getCandiModels().get(i).getReasonInactive() == ReasonInactive.Deleting)
					removeCandiModel(mCandiPatchModel.getCandiModels().get(i));
			}

			/*
			 * Flag for deletion any candi models that don't have an entity anymore
			 */
			for (int i = mCandiPatchModel.getCandiModels().size() - 1; i >= 0; i--) {
				CandiModel candiModel = mCandiPatchModel.getCandiModels().get(i);
				if (!entitiesCopy.containsKey(candiModel.getEntity().id)) {
					candiModel.setReasonInactive(ReasonInactive.Deleting);
				}
			}
		}

		CandiModel candiRootPrev = (CandiModel) mCandiPatchModel.getCandiRootCurrent();
		CandiModel candiRootNext = new CandiModel("0", mCandiPatchModel);
		candiRootNext.setSuperRoot(true);

		/* Strip linkages */
		for (CandiModel candiModel : mCandiPatchModel.getCandiModels()) {
			candiModel.setParent(null);
			candiModel.getChildren().clear();
		}

		/* Make sure each entity has a candi model */
		for (Entity entity : entitiesCopy) {

			CandiModel candiModel = null;
			if (mCandiPatchModel.hasCandiModelForEntity(entity.id)) {
				candiModel = mCandiPatchModel.updateCandiModel(entity, mCandiRadarActivity.mPrefDisplayExtras);
				candiModel.setParent(candiRootNext);
				candiRootNext.getChildren().add(candiModel);
				/*
				 * Jayma: Experimenting with bumping the discovery time everytime a candi
				 * is discovered or re-discovered.
				 */
				if (!candiModel.getViewStateCurrent().isVisible() && candiModel.getViewStateNext().isVisible()) {
					candiModel.getEntity().discoveryTime = discoveryTime;
				}
			}
			else {
				/*
				 * We keep bumping the date up until the entity is finally visible.
				 */
				if (entity.rookie) {
					entity.discoveryTime = discoveryTime;
					if (!entity.hidden) {
						entity.rookie = false;
					}
				}
				candiModel = CandiModelFactory.newCandiModel(entity.id, entity, mCandiPatchModel);
				candiModel.setDisplayExtra(mCandiRadarActivity.mPrefDisplayExtras);
				mCandiPatchModel.addCandiModel(candiModel);
				candiModel.setParent(candiRootNext);
				candiRootNext.getChildren().add(candiModel);
			}
		}

		/* Restore the previous root if it still has visible children */
		boolean foundRoot = false;
		if (candiRootPrev != null) {
			for (CandiModel candiModel : mCandiPatchModel.getCandiModels()) {
				if (candiModel.getModelId() == candiRootPrev.getModelId()) {
					foundRoot = true;
					if (candiModel.hasVisibleChildrenNext()) {
						mCandiPatchModel.setCandiRootNext(candiModel);
					}
					else {
						/* Bounce up to the super root */
						mCandiPatchModel.setCandiRootNext(candiRootNext);
					}
					break;
				}
			}
		}

		/* Stage the updated model tree */
		if (!foundRoot) {
			mCandiPatchModel.setCandiRootNext(candiRootNext);
		}
		Aircandi.stopwatch.segmentTime("Prep work before navigate");


		/* Navigate to make sure we are completely configured */
		navigateModel(mCandiPatchModel.getCandiRootNext(), delayObserverUpdate, fullBuild);
		Aircandi.stopwatch.segmentTime("Navigate complete");

		/* Return to default rendering window */
		Logger.d(this, "Model updated with entities");
	}

	public void navigateModel(IModel candiRootNext, boolean delayObserverUpdate, boolean fullUpdate) {

		Logger.d(this, "Starting model navigation.");

		mCandiPatchModel.setCandiRootNext(candiRootNext);

		/* Need to sort the candi before assigning to zones */
		/* Sort the candi by discovery time then modified time */
		mCandiPatchModel.sortCandiModels(mCandiPatchModel.getCandiRootNext().getChildren());

		/*
		 * Set candi visible state, move candi to inactive zone if appropriate. Set candiModelFocused to null if candi
		 * with current focus will not be visible in next update.
		 */
		mCandiPatchModel.updateVisibilityNext();
		/*
		 * Clear zones and re-assign candi to zones. Swap zones if needed to keep candi with focus in the zone the user
		 * is slotted on. Set visible for zones based on candi count.
		 */
		mCandiPatchModel.updateZonesNext();

		/* Set x, y, scale and alignment for all candi */
		mCandiPatchModel.updatePositionsNext();

		/* Zone titles, candi reflections and collapsed state */
		mCandiPatchModel.updateMiscNext();

		/* Make sure zones have a view */
		for (ZoneModel zoneModel : mCandiPatchModel.getZones()) {
			ensureZoneView(zoneModel);
		}

		Aircandi.stopwatch.segmentTime("Prep work before transitions");
		/* For animations, we need to create views in advance. */
		if (CandiConstants.TRANSITIONS_ACTIVE) {

			/* Clear actions/modifiers from candi models without candi views */
			for (CandiModel candiModel : mCandiPatchModel.getCandiModels()) {
				if (mCandiViewsActiveHash.get(candiModel.getModelIdAsString()) == null) {
					synchronized (candiModel.getViewModifiers()) {
						candiModel.getViewModifiers().clear();
					}
					synchronized (candiModel.getViewActions()) {
						candiModel.getViewActions().clear();
					}
				}
			}

			/* Wait for modifiers of active candi views to finish */
			boolean modifiersFinished = false;
			int attempts = 0;
			while (!modifiersFinished) {
				attempts++;

				if (attempts >= 3) {
					Logger.d(this, "Cleared all modifiers/actions after two attempts to let them finish.");
					/*
					 * Last ditch effort to continue: clear actions/modifiers from candi models no matter what.
					 */
					for (CandiModel candiModel : mCandiPatchModel.getCandiModels()) {
						synchronized (candiModel.getViewModifiers()) {
							candiModel.getViewModifiers().clear();
						}
						synchronized (candiModel.getViewActions()) {
							candiModel.getViewActions().clear();
						}
					}
				}

				boolean modifierHit = false;
				for (CandiModel candiModel : mCandiPatchModel.getCandiModels()) {
					synchronized (candiModel.getViewModifiers()) {
						if (candiModel.getViewModifiers().size() > 0) {
							modifierHit = true;
							break;
						}
					}
				}

				if (!modifierHit) {
					modifiersFinished = true;
				}
				else {
					Logger.v(this, "Still active modifiers: sleeping");
					try {
						Thread.sleep(2000);
					}
					catch (InterruptedException exception) {
						return;
					}
				}
			}
			Aircandi.stopwatch.segmentTime("Modifier management complete");

			manageViews(false, true);

			doTransitionAnimations();
			Aircandi.stopwatch.segmentTime("Transition animations processed");
	}

		/* Trigger epoch observer updates */
		if (!delayObserverUpdate) {
			mCandiPatchModel.update();
		}

		/* Copy next to current across the model and child models */
		mCandiPatchModel.shiftToNext();

		/* Reset camera boundaries */
		setCameraBoundaries(mScene);

		/* Now that all the view entities have updated we can do global operations like z sorting. */
		mEngine.getScene().getChild(CandiConstants.LAYER_CANDI).sortChildren();
		Aircandi.stopwatch.segmentTime("Model update-shift-sort complete");

	}

	private IView ensureZoneView(ZoneModel zoneModel) {

		if (zoneModel.getViewStateNext().isVisible() && zoneModel.countObservers() == 0) {

			final ZoneView zoneView = new ZoneView(zoneModel, CandiPatchPresenter.this);
			zoneView.setTitleTextColor(Color.parseColor(mStyleTextColorTitle));
			zoneView.setTitleTextFillColor(Color.TRANSPARENT);

			/* Link view to the model */
			zoneModel.addObserver(zoneView);
			zoneView.loadHardwareTextures();
			zoneView.initialize();

			/* Track in our collection */
			mZoneViews.add(zoneView);

			/* Bind view to model as observer */
			zoneModel.addObserver(zoneView);

			/* Set initial view position */
			zoneView.setPosition(zoneModel.getViewStateCurrent().getX(), zoneModel.getViewStateCurrent().getY());

			/* Add view to scene (starts out hidden using transparency). */
			mEngine.getScene().getChild(CandiConstants.LAYER_ZONES).attachChild(zoneView);

			return zoneView;
		}
		return null;
	}

	@Override
	public void update(Observable observable, Object data) {}

	public class CandiViewPool extends GenericPool<CandiView> {

		/*
		 * Only used internally
		 */

		public CandiViewPool(int initialSize, int growth) {
			super(initialSize, growth);
		}

		@Override
		protected void onHandleObtainItem(CandiView item) {
			item.setIgnoreUpdate(false);
		}

		@Override
		protected void onHandleRecycleItem(CandiView item) {
			item.reset();
			item.setIgnoreUpdate(true);
			item.setRecycled(true);
		}

		@Override
		protected CandiView onAllocatePoolItem() {

			/*
			 * Create a candi view and do all the configuration that isn't model specific.
			 */
			final CandiView candiView = new CandiView(CandiPatchPresenter.this);

			candiView.setTitleTextColor(Color.parseColor(getStyleTextColorTitle()));
			candiView.setTitleTextFillColor(Color.TRANSPARENT);
			mEngine.getScene().getChild(CandiConstants.LAYER_CANDI).attachChild(candiView);

			mCandiRadarActivity.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					candiView.setGestureDetector(new GestureDetector(mCandiRadarActivity, candiView) {

						@Override
						public boolean onTouchEvent(MotionEvent motionEvent) {
							boolean detectedUp = motionEvent.getAction() == MotionEvent.ACTION_UP;
							if (detectedUp) {
								if (mHighlight.isVisible()) {
									mHighlight.setVisible(false);
								}
							}
							return super.onTouchEvent(motionEvent);
						}
					});
				}
			});

			/*
			 * My touch listener that gets called after the touch
			 * has been examined by the candiview.
			 */
			candiView.setViewTouchListener(new ViewTouchListener() {

				@Override
				public void onViewDoubleTap(IView view) {}

				@Override
				public void onViewLongPress(IView view) {}

				@Override
				public void onViewSingleTap(IView view) {
					if (!mIgnoreInput) {
						mIgnoreInput = true;
						renderingActivateBump();

						final CandiModel candiModel = (CandiModel) candiView.getModel();
						Logger.d(this, "SingleTap triggered: " + candiModel.getEntity().label);

						if (mCandiListener != null) {
							mCandiListener.onSingleTap(candiModel);
						}
					}
				}
			});

			return candiView;
		}

		public int pooledCount() {
			return this.mAvailableItems.size();
		}

		public int loanedCount() {
			return this.getUnrecycledCount();
		}
	}

	public void getCandiViewFromPool(final CandiModel candiModel, boolean localUpdate, boolean useNext) {

		/*
		 * CandiView has been pre-configured except for anything that is model specific.
		 */
		final CandiView candiView = mCandiViewPool.obtainPoolItem();

		if (candiView.isRecycled()) {
			Logger.v(this, "CandiView pulled from pool: " + (candiModel.getTitleText() != null ? candiModel.getTitleText() : "[Untitled]"));
		}
		else {
			Logger.d(this, "CandiView created: " + (candiModel.getTitleText() != null ? candiModel.getTitleText() : "[Untitled]"));
		}

		candiView.setRecycled(false);
		candiView.setModel(candiModel);
		candiView.setCandiPatchPresenter(CandiPatchPresenter.this);
		candiModel.addObserver(candiView);
		mCandiViewsActiveHash.put(String.valueOf(candiModel.getModelId()), candiView);

		/*
		 * Both of these calls cause new textures to be loaded based on the current candi model.
		 */
		if (candiView.isHardwareTexturesInitialized()) {
			/*
			 * How time is spent in this call
			 * 
			 * - Image is pulled from file cache and decoded into a bitmap (90 ms)
			 * - Bitmap is pushed to texture regions
			 * - Animation to fade in the images in the ui is kicked off (500 ms duration)
			 */
			candiView.initializeModel();
		}
		else {
			candiView.loadHardwareTextures();
			candiView.initialize();
		}

		/* Hide zone ui */
		ZoneModel zoneModel = useNext ? candiModel.getZoneStateNext().getZone() : candiModel.getZoneStateCurrent().getZone();
		ZoneStatus zoneStatus = useNext ? candiModel.getZoneStateNext().getStatus() : candiModel.getZoneStateCurrent().getStatus();
		boolean zoneVisible = useNext ? zoneModel.getViewStateNext().isVisible() : zoneModel.getViewStateCurrent().isVisible();

		if (!zoneModel.isInactive() && zoneVisible && zoneStatus == ZoneStatus.Normal) {
			/*
			 * Clear all current modifiers and start fade out.
			 */
			synchronized (zoneModel.getViewModifiers()) {
				zoneModel.getViewModifiers().clear();
				zoneModel.getViewModifiers().addLast(
						new DelayModifier(CandiConstants.DURATION_TRANSITIONS_DELAY));
				zoneModel.getViewModifiers().addLast(
						new CandiAlphaModifier(null, CandiConstants.DURATION_TRANSITIONS_FADE, 1.0f, 0.0f, CandiConstants.EASE_FADE_OUT));

			}
			zoneModel.getViewStateCurrent().setVisible(false);
			zoneModel.setChanged();
			zoneModel.update();
		}

		/*
		 * Using localUpdate means this candiview is not part of a double buffer update so we take complete control of
		 * how the candiview is snapped to the model. If the model has any pre-loaded actions and/or modifiers, we strip
		 * them. We monitor current visible state because transition logic won't fade in if it thinks it is already
		 * supposed to be visible.
		 */

		/*
		 * Defers to navigate for animation if localUpdate = false. Defers to candi for animation if localUpdate = true.
		 * Problem: candi got an animation from first navigation that stacks with the candi self animation. First time
		 * we navigate, all the candi are set so current visible = false.
		 */
		if (localUpdate || candiModel.getViewStateCurrent().isVisible()) {
			if (CandiConstants.TRANSITIONS_ACTIVE) {
				synchronized (candiModel.getViewModifiers()) {
					candiModel.getViewModifiers().clear();
				}
				synchronized (candiModel.getViewActions()) {
					candiModel.getViewActions().clear();
					candiModel.getViewActions().addLast(new ViewAction(ViewActionType.Position));
					candiModel.getViewActions().addLast(new ViewAction(ViewActionType.Scale));
					candiModel.getViewActions().addLast(new ViewAction(ViewActionType.ExpandCollapse));
					candiModel.getViewActions().addLast(new ViewAction(ViewActionType.Visibility));
				}
			}
			candiModel.setChanged();
			candiModel.update();
		}
	}

	public void sendCandiViewToPool(CandiModel candiModel, boolean useNext) {

		String modelId = String.valueOf(candiModel.getModelId());
		CandiView candiView = (CandiView) mCandiViewsActiveHash.get(modelId);
		/*
		 * Show zone ui
		 */
		final ZoneModel zoneModel = candiModel.getZoneStateCurrent().getZone();
		ViewState viewState = useNext ? candiModel.getViewStateNext() : candiModel.getViewStateCurrent();

		if (!zoneModel.isInactive() && viewState.isVisible()) {
			synchronized (zoneModel.getViewModifiers()) {
				zoneModel.getViewModifiers().addLast(
						new CandiAlphaModifier(null, CandiConstants.DURATION_TRANSITIONS_FADE, 0.0f, 1.0f, CandiConstants.EASE_FADE_IN));
			}
			zoneModel.getViewStateCurrent().setVisible(true);
			zoneModel.setChanged();
			zoneModel.notifyObservers();
		}

		candiModel.deleteObservers();
		synchronized (candiModel.getViewModifiers()) {
			candiModel.getViewModifiers().clear();
		}
		synchronized (candiModel.getViewActions()) {
			candiModel.getViewActions().clear();
		}
		mCandiViewsActiveHash.remove(modelId);

		if (candiView != null) {
			Logger.v(this, "CandiView recycled to the pool: " + (candiModel.getTitleText() != null ? candiModel.getTitleText() : "[Untitled]"));
			mCandiViewPool.recyclePoolItem(candiView);
		}
	}

	private class ManageViewsThread extends Thread {

		@Override
		public void run() {
			manageViews(true, false);
		}
	};

	private void manageViews(final boolean localUpdate, final boolean useNext) {
		/*
		 * - Called synchronously from navigate if doing animations else async.
		 * - Called async when finished with scroll or fling.
		 */
		ArrayList<CandiModel> candiModels = new ArrayList<CandiModel>(mCandiPatchModel.getCandiModels());
		int countCandiModels = candiModels.size();

		if (localUpdate) {
			Logger.v(this, "Starting view management pass: async");
		}
		else {
			Logger.v(this, "Starting view management pass");
		}

		/* Recycle views first */
		for (int i = 0; i < countCandiModels; i++) {
			final CandiModel candiModel = (CandiModel) candiModels.get(i);
			ViewState viewStateCurrent = candiModel.getViewStateCurrent();
			ViewState viewStateNext = candiModel.getViewStateNext();

			/*
			 * Keep views that are within the halo current or next to optimize transitions
			 */
			boolean isWithinHalo = viewStateCurrent.isWithinHalo(mCamera);
			if (!isWithinHalo) {
				isWithinHalo = viewStateNext.isWithinHalo(mCamera);
			}

			if (!isWithinHalo && mCandiViewsActiveHash.containsKey(candiModel.getModelIdAsString())) {
				sendCandiViewToPool(candiModel, useNext);
			}
		}
		Aircandi.stopwatch.segmentTime("Manage views: recycling complete");


		if (localUpdate) {
			/*
			 * Allocate views based on UI priority
			 */
			List<CandiModel> candiModelsByPriority = new ArrayList<CandiModel>();

			/* Prioritize based on visibility to the user */
			for (CandiModel candiModel : candiModels) {
				candiModel.setPriority(candiModel.getViewStateNext().isVisibleToCamera(mCamera) ? 1 : 0);
				candiModelsByPriority.add(candiModel);
			}

			/* Sort */
			Collections.sort(candiModelsByPriority, new SortCandiModelsByPriority());

			/* Work */

			for (final CandiModel candiModel : candiModelsByPriority) {
				ViewState viewState = useNext ? candiModel.getViewStateNext() : candiModel.getViewStateCurrent();
				if (viewState.isVisible()) {
					boolean isWithinHalo = viewState.isWithinHalo(mCamera);
					if (isWithinHalo && !mCandiViewsActiveHash.containsKey(candiModel.getModelIdAsString())) {

						AsyncTask task = new AsyncTask() {

							@Override
							protected Object doInBackground(Object... params) {
								getCandiViewFromPool(candiModel, localUpdate, useNext);
								return null;
							}
						};

						/*
						 * We don't want parallel tasks because it slows down the display
						 * of the highest priority candi.
						 */
						task.execute();
					}
				}
			}
			Aircandi.stopwatch.segmentTime("Manage views: views allocated");
		}
		else {
			/*
			 * Allocate views without priority
			 */
			for (CandiModel candiModel : candiModels) {
				ViewState viewState = useNext ? candiModel.getViewStateNext() : candiModel.getViewStateCurrent();
				if (viewState.isVisible()) {
					boolean isWithinHalo = viewState.isWithinHalo(mCamera);
					if (isWithinHalo && !mCandiViewsActiveHash.containsKey(candiModel.getModelIdAsString())) {
						/*
						 * TODO: Are we getting animations from this and from the standard
						 * navigation animation logic later?
						 */
						getCandiViewFromPool(candiModel, localUpdate, useNext);
					}
				}
				Aircandi.stopwatch.segmentTime("Manage views: view allocated");
			}

		}

		/* update debug info */
		if (BuildConfig.DEBUG) {
			mCandiRadarActivity.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (Aircandi.settings.getBoolean(Preferences.PREF_SHOW_DEBUG, false)) {
						mCandiRadarActivity.updateDebugInfo();
					}
				}
			});
			Aircandi.stopwatch.segmentTime("Manage views: debug info updated");
		}
	}

	public float getLastVisibleCandiModelX(boolean useNext) {
		CandiArrayList candiModels = mCandiPatchModel.getCandiModels();
		int countCandiModels = candiModels.size();

		float lastX = 0;
		for (int i = 0; i < countCandiModels; i++) {
			final CandiModel candiModel = (CandiModel) candiModels.get(i);
			ViewState viewState = useNext ? candiModel.getViewStateNext() : candiModel.getViewStateCurrent();
			if (viewState.isVisible() && viewState.getX() > lastX) {
				lastX = viewState.getX();
			}
		}
		return lastX;
	}

	public void manageViewsAsync() {
		mManageViewsThread = new ManageViewsThread();
		mManageViewsThread.setPriority(Thread.MIN_PRIORITY);
		mManageViewsThread.setName("ViewManagerThread");
		mManageViewsThread.start();
	}

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	public boolean rookieHit(EntityList<Entity> entities) {
		boolean rookieHit = false;
		for (Entity entity : entities) {
			if (mCandiPatchModel.hasCandiModelForEntity(entity.id)) {
				CandiModel candiModelManaged = mCandiPatchModel.getCandiModels().getByKey(String.valueOf(entity.id));
				Entity originalEntity = candiModelManaged.getEntity();
				if (originalEntity.hidden && !entity.hidden) {
					rookieHit = true;
				}
			}
			else {
				if (!entity.hidden) {
					rookieHit = true;
					break;
				}
			}
		}
		return rookieHit;
	}

	public void renderingActivateBump() {
		/*
		 * Extends the rendering window using the current window setting.
		 */
		mRenderingActive = true;
		mRenderSurfaceView.requestRender();
		synchronized (mRenderingTimer) {
			mRenderingTimer.cancel();
			mRenderingTimer.start();
		}
	}

	public void renderingActivate(long millisInFuture) {
		synchronized (mRenderingTimer) {
			mRenderingTimer.cancel();
			mRenderingTimer.setMillisInFuture(millisInFuture);
			mRenderingActive = true;
			mRenderSurfaceView.requestRender();
			Logger.v(this, "Rendering activated at " + String.valueOf(millisInFuture));
			mRenderingTimer.start();
		}
	}

	public long getRenderingTimeLeft() {
		return mRenderingTimer.getMillisUntilFinished();
	}

	public void ensureScrollBoundaries() {
		/* Check to see if we are at a boundary */
		float cameraTargetY = mCameraTargetSprite.getY();
		if (cameraTargetY <= mBoundsMinY) {
			renderingActivateBump();
			mCameraTargetSprite.moveToTop(CandiConstants.DURATION_BOUNCEBACK, CandiConstants.EASE_BOUNCE_BACK, null);
		}
		else if (cameraTargetY >= mBoundsMaxY) {
			renderingActivateBump();
			mCameraTargetSprite.moveToBottom(CandiConstants.DURATION_BOUNCEBACK, CandiConstants.EASE_BOUNCE_BACK, null);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Animation
	// --------------------------------------------------------------------------------------------

	private void doTransitionAnimations() {
		/*
		 * Zone transitions The zone might already have a fade out modifier because manageViews() populated it with a
		 * full size candi view.
		 */
		for (ZoneModel zoneModel : mCandiPatchModel.getZones()) {
			synchronized (zoneModel.getViewActions()) {
				zoneModel.getViewActions().addLast(new ViewAction(ViewActionType.Visibility));
			}
		}

		/* Candi transitions */
		for (ZoneModel zone : mCandiPatchModel.getZones()) {
			boolean needDelay = false;
			if (zone.isOccupiedCurrent()) {

				for (CandiModel candiModel : zone.getCandiesCurrent()) {
					synchronized (candiModel.getViewModifiers()) {
						if (candiModel.getViewModifiers().isEmpty()) {

							Transition transition = candiModel.getTransition();
							ViewState viewStateCurrent = candiModel.getViewStateCurrent();
							ViewState viewStateNext = candiModel.getViewStateNext();

							if (transition != Transition.None) {
								Logger.v(this, "Transition From: " + transition.toString()
										+ ": " + (candiModel.getTitleText() != null ? candiModel.getTitleText() : "[Untitled]"));
							}

							if (transition == Transition.Out) {
								candiModel.getViewModifiers().addLast(
										new CandiAlphaModifier(null, CandiConstants.DURATION_TRANSITIONS_FADE, 1.0f, 0.0f,
												CandiConstants.EASE_FADE_OUT));
								needDelay = true;
							}
							else if (transition == Transition.FadeOut) {
								candiModel.getViewModifiers().addLast(
										new CandiAlphaModifier(null, CandiConstants.DURATION_TRANSITIONS_FADE, 1.0f, 0.0f,
												CandiConstants.EASE_FADE_OUT_STRONG));
								needDelay = true;
							}
							else if (transition == Transition.Move || transition == Transition.Shift) {

								if (viewStateCurrent.getScale() == viewStateNext.getScale()) {
									candiModel.getViewModifiers().addLast(
											new MoveModifier(CandiConstants.DURATION_TRANSITIONS_MOVE, viewStateCurrent.getX(), viewStateNext.getX(),
													viewStateCurrent.getY(), viewStateNext.getY(), EaseQuartInOut.getInstance()));
								}
								else {
									candiModel.getViewModifiers().addLast(
											new ParallelEntityModifier(
													new ScaleModifier(CandiConstants.DURATION_TRANSITIONS_MOVE, viewStateCurrent.getScale(),
															viewStateNext.getScale(), EaseQuartInOut.getInstance()),
													new MoveModifier(CandiConstants.DURATION_TRANSITIONS_MOVE, viewStateCurrent.getX(), viewStateNext
															.getX(), viewStateCurrent.getY(), viewStateNext.getY(), EaseQuartInOut.getInstance())));
									synchronized (candiModel.getViewActions()) {
										/* Add visibility */
										candiModel.getViewActions().addLast(new ViewAction(ViewActionType.Visibility));
										candiModel.getViewActions().addLast(new ViewAction(ViewActionType.ExpandCollapseAnim));
										candiModel.getViewActions().addLast(new ViewAction(ViewActionType.ReflectionHideShowAnim));
									}
								}
								if (transition == Transition.Move) {
									needDelay = true;
								}
							}
						}
					}
				}
			}

			if (zone.isOccupiedNext()) {

				for (CandiModel candiModel : zone.getCandiesNext()) {

					synchronized (candiModel.getViewModifiers()) {
						if (candiModel.getViewModifiers().isEmpty()) {

							Transition transition = candiModel.getTransition();
							ViewState viewStateCurrent = candiModel.getViewStateCurrent();
							ViewState viewStateNext = candiModel.getViewStateNext();

							if (transition != Transition.None) {
								Logger.v(this, "Transition To: " + transition.toString()
										+ ": " + (candiModel.getTitleText() != null ? candiModel.getTitleText() : "[Untitled]"));
							}

							if (transition == Transition.FadeIn) {

								if (needDelay) {
									candiModel.getViewModifiers().addLast(new DelayModifier(CandiConstants.DURATION_TRANSITIONS_DELAY));
								}

								candiModel.getViewModifiers().addLast(
										new CandiAlphaModifier(null,
												CandiConstants.DURATION_TRANSITIONS_FADE, 0.0f, 1.0f,
												CandiConstants.EASE_FADE_IN));
								synchronized (candiModel.getViewActions()) {
									candiModel.getViewActions().addLast(new ViewAction(ViewActionType.Position));
									candiModel.getViewActions().addLast(new ViewAction(ViewActionType.Scale));
									candiModel.getViewActions().addLast(new ViewAction(ViewActionType.ExpandCollapse));
									candiModel.getViewActions().addLast(new ViewAction(ViewActionType.ReflectionHideShow));
								}
							}
							else if (transition == Transition.Move) {

								if (viewStateCurrent.getScale() == viewStateNext.getScale()) {
									if (needDelay) {
										candiModel.getViewModifiers().addLast(new DelayModifier(CandiConstants.DURATION_TRANSITIONS_DELAY));
									}
									candiModel.getViewModifiers().addLast(
											new MoveModifier(CandiConstants.DURATION_TRANSITIONS_MOVE, viewStateCurrent.getX(), viewStateNext.getX(),
													viewStateCurrent.getY(), viewStateNext.getY(), EaseQuartInOut.getInstance()));
								}
								else {
									candiModel.getViewModifiers().addLast(
											new ParallelEntityModifier(
													new ScaleModifier(CandiConstants.DURATION_TRANSITIONS_MOVE, viewStateCurrent.getScale(),
															viewStateNext.getScale(), EaseQuartInOut.getInstance()),
													new MoveModifier(CandiConstants.DURATION_TRANSITIONS_MOVE, viewStateCurrent.getX(), viewStateNext
															.getX(), viewStateCurrent.getY(), viewStateNext.getY(), EaseQuartInOut.getInstance())));

									synchronized (candiModel.getViewActions()) {
										candiModel.getViewActions().addLast(new ViewAction(ViewActionType.Visibility));
										candiModel.getViewActions().addLast(new ViewAction(ViewActionType.ExpandCollapseAnim));
										candiModel.getViewActions().addLast(new ViewAction(ViewActionType.ReflectionHideShowAnim));
									}

								}
							}
						}
					}

					/*
					 * If the candi is staying visible and has an image change then refresh texture
					 */
					if (candiModel.getViewStateNext().isVisible() && candiModel.isMasterImageUpdated()) {
						synchronized (candiModel.getViewActions()) {
							candiModel.getViewActions().addFirst(new ViewAction(ViewActionType.UpdateTexturesForce));
							candiModel.setMasterImageUpdated(false);
						}
					}
				}
			}
		}
	}

	private void setCameraBoundaries(Scene scene) {

		/* Find first occupied zone */
		ZoneModel firstOccupiedZone = null;
		for (ZoneModel zoneModel : mCandiPatchModel.getZones()) {
			if (zoneModel.getCandiesCurrent().size() > 0) {
				firstOccupiedZone = zoneModel;
				break;
			}
		}
		/*
		 * We have an occupied zone
		 */
		if (firstOccupiedZone != null) {
			int ranks = (int) FloatMath.ceil((float) mCandiPatchModel.getZonesOccupiedCurrentCount() / (float) CandiConstants.RADAR_STACK_COUNT);
			if (CandiConstants.RADAR_SCROLL_HORIZONTAL) {
				mBoundsMinX = mRadarWidth * 0.5f;
				mBoundsMaxX = (mBoundsMinX + ((ranks - 1) * (CandiConstants.CANDI_VIEW_WIDTH + CandiConstants.CANDI_VIEW_SPACING_VERTICAL)));
			}
			else {
				/*
				 * Start out with bounds that exactly fit the visible radar
				 */
				mBoundsMinY = getRadarZoomedHeight() * 0.5f;
				mBoundsMaxY = mBoundsMinY;

				/*
				 * Measure the content to see if scrolling will be needed
				 */
				float contentHeight = CandiConstants.CANDI_VIEW_HEIGHT * ranks;
				contentHeight += CandiConstants.CANDI_VIEW_SPACING_VERTICAL * (ranks - 1);

				if (contentHeight > getRadarZoomedHeight()) {
					mBoundsMaxY = contentHeight - (getRadarZoomedHeight() * 0.5f);
				}
			}
		}
	}

	public void setFrustum(GL10 pGL) {

		/* Set field of view to 60 degrees */
		float fov_degrees = 60;
		float fov_radians = fov_degrees / 180 * (float) Math.PI;

		/* Set aspect ratio and distance of the screen */
		float aspect = mCamera.getWidth() / mCamera.getHeight();
		float camZ = mCamera.getHeight() / 2 / (float) Math.tan(fov_radians / 2);

		/* Set projection */
		GLHelper.setProjectionIdentityMatrix(pGL);
		GLU.gluPerspective(pGL, fov_degrees, aspect, camZ / 10, camZ * 10);

		/*
		 * The logic here positions the viewer but does not position the camera. That means we could be looking at slot
		 * #5 but the camera is still sitting on slot #2. Touch areas track with the camera so they will be messed up.
		 * So we still set the camera target sprite as the chase entity for the camera. Because the y axis is reversed,
		 * we need to subtract to move down.
		 */
		final float[] centerCoordinates = mCameraTargetSprite.getSceneCenterCoordinates();
		float targetX = centerCoordinates[VERTEX_INDEX_X];
		float targetY = -centerCoordinates[VERTEX_INDEX_Y];

		GLU.gluLookAt(pGL, targetX, targetY, camZ, targetX, targetY, 0, 0, 1, 0); /*
																				 * move camera back
																				 */
		pGL.glScalef(1, -1, 1); /* reverse y-axis */
		pGL.glTranslatef(0f, 0, 0); /* origin at top left */
	}

	// --------------------------------------------------------------------------------------------
	// Textures
	// --------------------------------------------------------------------------------------------

	public void removeCandiModel(CandiModel candiModel) {

		/* Remove associated candi view */
		final CandiView candiView = (CandiView) mCandiViewsActiveHash.get(candiModel.getModelIdAsString());
		mCandiViewsActiveHash.remove(candiModel.getModelIdAsString());
		if (candiView != null) {
			renderingActivateBump();

			/*
			 * Remove associated images from image cache if this candi model is gone for good
			 */
			if (candiModel.isDeleted()) {
				String imageUri = ImageRequestBuilder.getImageUriFromEntity(candiModel.getEntity());
				final String cacheName = ImageManager.getInstance().resolveCacheName(imageUri);
				ImageManager.getInstance().deleteImage(cacheName);
				ImageManager.getInstance().deleteImage(cacheName + ".reflection");
			}

			/* Recycle the candi view */
			if (mCandiViewsActiveHash.containsKey(candiModel.getModelIdAsString())) {
				sendCandiViewToPool(candiModel, false);
			}
		}

		/* Remove parent model */
		mCandiPatchModel.getCandiModels().remove(candiModel); /* Search is done using model id */
		if (mCandiPatchModel.getCandiModelSelected() == candiModel) {
			mCandiPatchModel.setCandiModelSelected(null);
		}
		if (mCandiPatchModel.getCandiRootCurrent() == candiModel) {
			mCandiPatchModel.setCandiRootCurrent(null);
		}
	}

	private void clearCandiLayer() {
		IEntity layer = mEngine.getScene().getChild(CandiConstants.LAYER_CANDI);
		int childCount = layer.getChildCount();
		for (int i = childCount - 1; i >= 0; i--) {
			IEntity child = layer.getChild(i);
			if (child instanceof CandiView) {

				/* TODO: Should we null this so the GC can collect them. */
				final CandiView candiView = (CandiView) child;
				mEngine.runOnUpdateThread(new Runnable() {

					@Override
					public void run() {
						candiView.unloadResources();
						candiView.detachSelf();
					}
				});
			}
		}
	}

	private void clearZoneLayer() {
		IEntity layer = mEngine.getScene().getChild(CandiConstants.LAYER_ZONES);
		int childCount = layer.getChildCount();
		for (int i = childCount - 1; i >= 0; i--) {
			IEntity child = layer.getChild(i);
			if (child instanceof ZoneView) {

				/* TODO: Should we null this so the GC can collect them. */
				final ZoneView zoneView = (ZoneView) child;
				mEngine.runOnUpdateThread(new Runnable() {

					@Override
					public void run() {
						zoneView.unloadResources();
						zoneView.detachSelf();
					}
				});
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Textures
	// --------------------------------------------------------------------------------------------

	public void loadTextureSources() {

		/* Create texture */
		mTexture = new Texture(512, 512, CandiConstants.GL_TEXTURE_OPTION);
		mTexture.setName("Global texture for candi and zone generics");

		/* Textures that are shared by zone views */
		Bitmap candiBodyBitmap = null;
		candiBodyBitmap = ImageManager.getInstance().loadBitmapFromResources(Integer.valueOf(mStyleTextureBodyCandiResId));

		if (candiBodyBitmap != null) {
			candiBodyBitmap = ImageUtils.scaleAndCropBitmap(candiBodyBitmap, CandiConstants.CANDI_VIEW_WIDTH, ImageShape.Square);
			Bitmap candiReflectionBitmap = ImageUtils.makeReflection(candiBodyBitmap, true);

			mCandiBodyTextureRegion = TextureRegionFactory.createFromSource(mTexture, new BitmapTextureSource(candiBodyBitmap, "candi_body_placeholder",
					new IBitmapAdapter() {

						@Override
						public Bitmap reloadBitmap() {
							Bitmap candiBodyBitmap = null;
							candiBodyBitmap = ImageManager.getInstance().loadBitmapFromResources(Integer.valueOf(mStyleTextureBodyCandiResId));
							candiBodyBitmap = ImageUtils.scaleAndCropBitmap(candiBodyBitmap, CandiConstants.CANDI_VIEW_WIDTH, ImageShape.Square);
							return candiBodyBitmap;
						}
					}), 0, 0);

			mCandiReflectionTextureRegion = TextureRegionFactory.createFromSource(mTexture, new BitmapTextureSource(candiReflectionBitmap,
					"candi_body_placeholder_reflection",
					new IBitmapAdapter() {

						@Override
						public Bitmap reloadBitmap() {
							Bitmap candiBodyBitmap = null;
							candiBodyBitmap = ImageManager.getInstance().loadBitmapFromResources(Integer.valueOf(mStyleTextureBodyCandiResId));
							candiBodyBitmap = ImageUtils.scaleAndCropBitmap(candiBodyBitmap, CandiConstants.CANDI_VIEW_WIDTH, ImageShape.Square);
							if (candiBodyBitmap != null) {
								Bitmap candiReflectionBitmap = ImageUtils.makeReflection(candiBodyBitmap, true);
								return candiReflectionBitmap;
							}
							return null;
						}
					}), 0, 256);
		}

		Bitmap zoneBodyBitmap = null;
		zoneBodyBitmap = ImageManager.getInstance().loadBitmapFromResources(Integer.valueOf(mStyleTextureBodyZoneResId));

		if (zoneBodyBitmap != null) {
			zoneBodyBitmap = ImageUtils.scaleAndCropBitmap(zoneBodyBitmap, CandiConstants.CANDI_VIEW_WIDTH, ImageShape.Square);

			Bitmap zoneReflectionBitmap = ImageUtils.makeReflection(zoneBodyBitmap, true);

			mZoneBodyTextureRegion = TextureRegionFactory.createFromSource(mTexture, new BitmapTextureSource(zoneBodyBitmap, "zone_body",
					new IBitmapAdapter() {

						@Override
						public Bitmap reloadBitmap() {
							Bitmap zoneBodyBitmap = null;
							zoneBodyBitmap = ImageManager.getInstance().loadBitmapFromResources(Integer.valueOf(mStyleTextureBodyZoneResId));
							zoneBodyBitmap = ImageUtils.scaleAndCropBitmap(zoneBodyBitmap, CandiConstants.CANDI_VIEW_WIDTH, ImageShape.Square);
							return zoneBodyBitmap;
						}
					}), 256, 0);

			mZoneReflectionTextureRegion = TextureRegionFactory.createFromSource(mTexture, new BitmapTextureSource(zoneReflectionBitmap,
					"zone_body_reflection",
					new IBitmapAdapter() {

						@Override
						public Bitmap reloadBitmap() {
							Bitmap zoneBodyBitmap = null;
							zoneBodyBitmap = ImageManager.getInstance().loadBitmapFromResources(Integer.valueOf(mStyleTextureBodyZoneResId));
							zoneBodyBitmap = ImageUtils.scaleAndCropBitmap(zoneBodyBitmap, CandiConstants.CANDI_VIEW_WIDTH, ImageShape.Square);
							if (zoneBodyBitmap != null) {
								Bitmap zoneReflectionBitmap = ImageUtils.makeReflection(zoneBodyBitmap, true);
								return zoneReflectionBitmap;
							}
							return null;
						}
					}), 256, 256);
		}

		/* Load the texture */
		mEngine.getTextureManager().loadTexture(mTexture);

	}

	// --------------------------------------------------------------------------------------------
	// Utility routines
	// --------------------------------------------------------------------------------------------

	private ZoneModel getNearestZone(float nearestToAxis, boolean requireOccupiedNext) {
		if (mCandiPatchModel.getZonesOccupiedNextCount() == 0) {
			return null;
		}

		int nearestIndex = 0;
		float smallestDistance = 999999;

		for (ZoneModel zone : mCandiPatchModel.getZones()) {
			if (requireOccupiedNext && !zone.isOccupiedNext()) {
				continue;
			}
			if (zone.getCandiesCurrent().size() > 0) {
				float distance = 0;
				if (CandiConstants.RADAR_SCROLL_HORIZONTAL) {
					distance = Math.abs(zone.getViewStateCurrent().getX() - nearestToAxis);
				}
				else {
					distance = Math.abs(zone.getViewStateCurrent().getY() - nearestToAxis);
				}
				if (distance < smallestDistance) {
					nearestIndex = zone.getZoneIndex();
					smallestDistance = distance;
				}
			}
		}

		return mCandiPatchModel.getZones().get(nearestIndex);
	}

	@SuppressWarnings("unused")
	private ZoneModel getNearestCenterZone(float nearestToAxis, boolean requireOccupiedNext) {
		if (mCandiPatchModel.getZonesOccupiedNextCount() == 0) {
			return null;
		}

		int nearestIndex = 0;
		float smallestDistance = 999999;

		for (ZoneModel zone : mCandiPatchModel.getZones()) {
			if (requireOccupiedNext && !zone.isOccupiedNext()) {
				continue;
			}
			if (zone.getCandiesCurrent().size() > 0) {
				float distance = Math.abs(zone.getViewStateCurrent().getX() - nearestToAxis);
				if (distance < smallestDistance) {
					nearestIndex = zone.getZoneIndex();
					smallestDistance = distance;
				}
			}
		}

		return mCandiPatchModel.getZones().get(nearestIndex);
	}

	@SuppressWarnings("unused")
	private ZoneModel getZoneContainsCandiCurrent(CandiModel candiModelTarget) {
		if (mCandiPatchModel.getZonesOccupiedCurrentCount() == 0)
			return null;

		for (ZoneModel zone : mCandiPatchModel.getZones()) {
			if (zone.getCandiesCurrent().contains(candiModelTarget))
				return zone;
		}
		return null;
	}

	@SuppressWarnings("unused")
	private ZoneModel getZoneContainsCandiNext(CandiModel candiModelTarget) {
		if (mCandiPatchModel.getZonesOccupiedNextCount() == 0)
			return null;

		for (ZoneModel zone : mCandiPatchModel.getZones()) {
			if (zone.getCandiesNext().contains(candiModelTarget))
				return zone;
		}
		return null;
	}

	public void shiftScene(float shiftX, float shiftY) {
		mScene.setPosition(shiftX, shiftY);
	}

	// --------------------------------------------------------------------------------------------
	// Setters/Getters
	// --------------------------------------------------------------------------------------------

	public void setCandiListener(ICandiListener listener) {
		mCandiListener = listener;
	}

	public Engine getEngine() {
		return mEngine;
	}

	public void setScene(Scene scene) {
		mScene = scene;
	}

	public Scene getScene() {
		return mScene;
	}

	public boolean isIgnoreInput() {
		return mIgnoreInput;
	}

	public void setIgnoreInput(boolean ignoreInput) {
		mIgnoreInput = ignoreInput;
	}

	public ChaseCamera getCamera() {
		return mCamera;
	}

	public String getStyleTextColorTitle() {
		return mStyleTextColorTitle;
	}

	public HashMap getCandiViewsHash() {
		return mCandiViewsActiveHash;
	}

	public void setContext(Context context) {
		this.mContext = context;
	}

	public Context getContext() {
		return mContext;
	}

	public void setCandiViewPool(CandiViewPool candiViewPool) {
		this.mCandiViewPool = candiViewPool;
	}

	public CandiViewPool getCandiViewPool() {
		return mCandiViewPool;
	}

	public boolean isRenderingActive() {
		return mRenderingActive;
	}

	public Boolean getRookieHit() {
		return mRookieHit;
	}

	public void setRookieHit(Boolean rookieHit) {
		mRookieHit = rookieHit;
	}

	public Boolean getStyleTextOutlineTitle() {
		return mStyleTextOutlineTitle;
	}

	public void setStyleTextOutlineTitle(Boolean styleTextOutlineTitle) {
		mStyleTextOutlineTitle = styleTextOutlineTitle;
	}

	public float getRadarWidth() {
		return mRadarWidth;
	}

	public void setRadarWidth(float radarWidth) {
		mRadarWidth = radarWidth;
	}

	public float getRadarHeight() {
		return mRadarHeight;
	}

	public void setRadarHeight(float radarHeight) {
		mRadarHeight = radarHeight;
	}

	public float getBoundsMinY() {
		return mBoundsMinY;
	}

	public float getBoundsMaxY() {
		return mBoundsMaxY;
	}

	public float getRadarPaddingLeft() {
		return mRadarPaddingLeft;
	}

	public void setRadarPaddingLeft(float radarPaddingLeft) {
		mRadarPaddingLeft = radarPaddingLeft;
	}

	public float getRadarPaddingRight() {
		return mRadarPaddingRight;
	}

	public void setRadarPaddingRight(float radarPaddingRight) {
		mRadarPaddingRight = radarPaddingRight;
	}

	public float getRadarPaddingTop() {
		return mRadarPaddingTop;
	}

	public void setRadarPaddingTop(float radarPaddingTop) {
		mRadarPaddingTop = radarPaddingTop;
	}

	public float getRadarPaddingBottom() {
		return mRadarPaddingBottom;
	}

	public void setRadarPaddingBottom(float radarPaddingBottom) {
		mRadarPaddingBottom = radarPaddingBottom;
	}

	public float getRadarZoomedWidth() {
		return mRadarWidth * (1 / CandiConstants.RADAR_ZOOM);
	}

	public float getRadarZoomedHeight() {
		return mRadarHeight * (1 / CandiConstants.RADAR_ZOOM);
	}

	// --------------------------------------------------------------------------------------------
	// Inner classes/interfaces
	// --------------------------------------------------------------------------------------------

	public interface ICandiListener {

		void onSelected(IModel candi);

		void onSingleTap(CandiModel candi);
	}

	public interface IMoveListener {

		void onMoveFinished();
	}

	private class RenderCountDownTimer extends CountDownTimer {

		/*
		 * Only used interally
		 */
		private long	mMillisUntilFinished;

		public RenderCountDownTimer(long millisInFuture, long countDownInterval) {
			super(millisInFuture, countDownInterval);
			mMillisUntilFinished = millisInFuture;
		}

		@Override
		public void onFinish() {
			mRenderingActive = false;
			mMillisUntilFinished = 0;
			Logger.v(this, "Rendering deactivated: thread = " + Thread.currentThread().getName());
		}

		@Override
		public void onTick(long millisUntilFinished) {
			mMillisUntilFinished = millisUntilFinished;
		}

		public long getMillisUntilFinished() {
			return mMillisUntilFinished;
		}
	}

	private class SingleTapGestureDetector implements GestureDetector.OnGestureListener {

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			/*
			 * This gets called because the gesture detector thinks it has a fling gesture
			 */

			/*
			 * Horizontal scrolling
			 */
			if (CandiConstants.RADAR_SCROLL_HORIZONTAL) {

				/* Test for swipe that is too vertical to trigger a fling */
				if (e1 != null && e2 != null) {
					if (Math.abs(e1.getY() - e2.getY()) > CandiConstants.SWIPE_MAX_OFF_PATH) {
						return false;
					}
				}

				/* Check to see if we are at a boundary */
				renderingActivateBump();
				float cameraTargetX = mCameraTargetSprite.getX() + 125;
				if (cameraTargetX <= mBoundsMinX || cameraTargetX >= mBoundsMaxX) {
					return false;
				}

				/* The velocity units are in pixels per second. */
				final float distanceTimeFactor = 0.8f;
				final float totalDx = (distanceTimeFactor * velocityX / 2);

				/* Cap the distance we travel so we don't race past our boundaries */
				float targetX = mCameraTargetSprite.getX() - totalDx;

				if (targetX > mBoundsMaxX - 50) {
					targetX = mBoundsMaxX - 50;
				}
				else if (targetX < mBoundsMinX) {
					targetX = mBoundsMinX - 150;
				}

				mCameraTargetSprite.registerEntityModifier(new MoveModifier(distanceTimeFactor
						, mCameraTargetSprite.getX()
						, targetX
						, mCameraTargetSprite.getY()
						, mCameraTargetSprite.getY()
						, new IEntityModifierListener() {

							@Override
							public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {

								ZoneModel zoneModel = getNearestZone(mCameraTargetSprite.getX(), false);
								if (zoneModel != null) {
									CandiModel candiModel = zoneModel.getCandiesCurrent().get(0);
								}
							}

							@Override
							public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {}

						}
						, CandiConstants.EASE_FLING));
			}
			else {

				/* Test for swipe that is too vertical to trigger a fling */
				if (e1 != null && e2 != null) {
					if (Math.abs(e1.getX() - e2.getX()) > CandiConstants.SWIPE_MAX_OFF_PATH) {
						return false;
					}
				}

				/* Check to see if we are at a boundary */
				renderingActivateBump();
				float cameraTargetY = mCameraTargetSprite.getY();
				if (cameraTargetY <= mBoundsMinY) {
					mCameraTargetSprite.moveToTop(CandiConstants.DURATION_BOUNCEBACK, CandiConstants.EASE_BOUNCE_BACK, null);
					return false;
				}
				else if (cameraTargetY >= mBoundsMaxY) {
					mCameraTargetSprite.moveToBottom(CandiConstants.DURATION_BOUNCEBACK, CandiConstants.EASE_BOUNCE_BACK, null);
					return false;
				}

				/* The velocity units are in pixels per second. */
				final float distanceTimeFactor = 0.8f;
				final float totalDy = (distanceTimeFactor * velocityY / 2);

				/* Cap the distance we travel so we don't race past our boundaries */
				float targetY = mCameraTargetSprite.getY() - totalDy;

				if (targetY > getBoundsMaxY() - 50) {
					targetY = getBoundsMaxY() - 50;
				}
				else if (targetY < getBoundsMinY()) {
					targetY = getBoundsMinY() - 150;
				}

				mCameraTargetSprite.registerEntityModifier(new MoveModifier(distanceTimeFactor
						, mCameraTargetSprite.getX()
						, mCameraTargetSprite.getX()
						, mCameraTargetSprite.getY()
						, targetY
						, new IEntityModifierListener() {

							@Override
							public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
								/* Check to see if we ended up past a boundary */
								renderingActivateBump();
								float cameraTargetY = mCameraTargetSprite.getY();
								if (cameraTargetY <= mBoundsMinY) {
									mCameraTargetSprite.moveToTop(CandiConstants.DURATION_BOUNCEBACK, CandiConstants.EASE_BOUNCE_BACK, null);
								}
								else if (cameraTargetY >= mBoundsMaxY) {
									mCameraTargetSprite.moveToBottom(CandiConstants.DURATION_BOUNCEBACK, CandiConstants.EASE_BOUNCE_BACK, null);
								}
							}

							@Override
							public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {}

						}
						, CandiConstants.EASE_FLING));
			}

			return true;
		}

		@Override
		public boolean onDown(MotionEvent e) {
			return false;
		}

		@Override
		public void onLongPress(MotionEvent e) {}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			return false;
		}

		@Override
		public void onShowPress(MotionEvent e) {}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			return false;
		}
	}

	public static enum TextureReset {
		All, VisibleOnly, NonVisibleOnly
	}

	public static class SortCandiModelsByPriority implements Comparator<CandiModel> {

		@Override
		public int compare(CandiModel object1, CandiModel object2) {
			if (object1.getPriority() < object2.getPriority()) {
				return -1;
			}
			else if (object1.getPriority() == object2.getPriority()) {
				return 0;
			}
			return 1;
		}
	}

}
