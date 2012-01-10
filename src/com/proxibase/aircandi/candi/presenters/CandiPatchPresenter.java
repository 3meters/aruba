package com.proxibase.aircandi.candi.presenters;

import static org.anddev.andengine.util.constants.Constants.VERTEX_INDEX_X;
import static org.anddev.andengine.util.constants.Constants.VERTEX_INDEX_Y;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.microedition.khronos.opengles.GL10;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.modifier.DelayModifier;
import org.anddev.andengine.entity.modifier.MoveModifier;
import org.anddev.andengine.entity.modifier.ParallelEntityModifier;
import org.anddev.andengine.entity.modifier.ScaleModifier;
import org.anddev.andengine.entity.modifier.IEntityModifier.IEntityModifierListener;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.Scene.IOnSceneTouchListener;
import org.anddev.andengine.entity.scene.background.ColorBackground;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.texture.Texture;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.opengl.texture.region.TextureRegionFactory;
import org.anddev.andengine.opengl.texture.region.TiledTextureRegion;
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
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.GestureDetector.SimpleOnGestureListener;

import com.proxibase.aircandi.CandiSearchActivity;
import com.proxibase.aircandi.R;
import com.proxibase.aircandi.candi.camera.ChaseCamera;
import com.proxibase.aircandi.candi.models.CandiModel;
import com.proxibase.aircandi.candi.models.CandiModelFactory;
import com.proxibase.aircandi.candi.models.CandiPatchModel;
import com.proxibase.aircandi.candi.models.IModel;
import com.proxibase.aircandi.candi.models.ZoneModel;
import com.proxibase.aircandi.candi.models.BaseModel.ViewState;
import com.proxibase.aircandi.candi.models.CandiModel.DisplayExtra;
import com.proxibase.aircandi.candi.models.CandiModel.ReasonInactive;
import com.proxibase.aircandi.candi.models.CandiModel.Transition;
import com.proxibase.aircandi.candi.models.ZoneModel.ZoneStatus;
import com.proxibase.aircandi.candi.modifiers.CandiAlphaModifier;
import com.proxibase.aircandi.candi.sprites.CameraTargetSprite;
import com.proxibase.aircandi.candi.sprites.CandiAnimatedSprite;
import com.proxibase.aircandi.candi.sprites.CandiScene;
import com.proxibase.aircandi.candi.sprites.CameraTargetSprite.MoveListener;
import com.proxibase.aircandi.candi.views.CandiView;
import com.proxibase.aircandi.candi.views.IView;
import com.proxibase.aircandi.candi.views.ViewAction;
import com.proxibase.aircandi.candi.views.ZoneView;
import com.proxibase.aircandi.candi.views.IView.ViewTouchListener;
import com.proxibase.aircandi.candi.views.ViewAction.ViewActionType;
import com.proxibase.aircandi.components.BitmapTextureSource;
import com.proxibase.aircandi.components.CandiList;
import com.proxibase.aircandi.components.CountDownTimer;
import com.proxibase.aircandi.components.ImageManager;
import com.proxibase.aircandi.components.ImageUtils;
import com.proxibase.aircandi.components.Logger;
import com.proxibase.aircandi.components.BitmapTextureSource.IBitmapAdapter;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.sdk.android.proxi.consumer.Entity;

public class CandiPatchPresenter implements Observer {

	public static float				SCALE_NORMAL			= 1;

	public CandiPatchModel			mCandiPatchModel;
	private HashMap					mCandiViewsHash			= new HashMap();
	private List<ZoneView>			mZoneViews				= new ArrayList<ZoneView>();
	private CandiViewPool			mCandiViewPool;

	private GestureDetector			mGestureDetector;
	public boolean					mIgnoreInput			= false;
	private Context					mContext;
	private Engine					mEngine;
	public CandiAnimatedSprite		mProgressSprite;
	private float					mLastMotionX;
	private CameraTargetSprite		mCameraTargetSprite;
	private float					mBoundsMinX;
	private float					mBoundsMaxX;
	private ChaseCamera				mCamera;
	private Scene					mScene;

	public Texture					mTexture;

	public TiledTextureRegion		mProgressTextureRegion;
	public TextureRegion			mPlaceholderTextureRegion;
	public TextureRegion			mZoneBodyTextureRegion;
	public TextureRegion			mZoneReflectionTextureRegion;

	public CandiSearchActivity		mCandiActivity;
	public RenderSurfaceView		mRenderSurfaceView;
	private ManageViewsThread		mManageViewsThread;
	public DisplayExtra				mDisplayExtras;
	public boolean					mFullUpdateInProgress	= true;

	private ICandiListener			mCandiListener;
	public int						mTouchSlopSquare;
	public int						mDoubleTapSlopSquare;
	protected float					mTouchStartX;
	protected float					mTouchStartY;
	protected boolean				mTouchGrabbed;
	private boolean					mRenderingActive		= true;
	private RenderCountDownTimer	mRenderingTimer;

	private String					mStyleTextureBodyZone;
	private String					mStyleTextureBusyIndicator;
	private String					mStyleTextColorTitle;

	// --------------------------------------------------------------------------------------------
	// Initialization
	// --------------------------------------------------------------------------------------------

	public CandiPatchPresenter(Context context, Activity activity, Engine engine, RenderSurfaceView renderSurfaceView, CandiPatchModel candiPatchModel) {

		mCandiPatchModel = candiPatchModel;
		mContext = context;
		mEngine = engine;
		mCamera = (ChaseCamera) engine.getCamera();
		mRenderSurfaceView = renderSurfaceView;
		mCandiActivity = (CandiSearchActivity) activity;
		mGestureDetector = new GestureDetector(mContext, new SingleTapGestureDetector());
		
		/* Rendering timer */
		mRenderingTimer = new RenderCountDownTimer(5000, 500);

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
		mCandiViewPool = new CandiViewPool();


		/* Origin */
		mCandiPatchModel.setOriginX(0);
		mCandiPatchModel.setOriginY(0);

		/* Resource references per theme */
		loadStyles();

		/* Textures */
		loadTextures();
		loadTextureSources();
	}

	public Scene initializeScene() {
		Logger.d(this, "initScene called");

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

					/* Note we are applying rotation around the y-axis and not the z-axis anymore! */
					pGL.glRotatef(rotation, 0, 1, 0);
					pGL.glTranslatef(-rotationCenterX, -rotationCenterY, 0);
				}
			}
		};

		scene.setBackground(new ColorBackground(0, 0, 0, 0)); /* Transparent */
		scene.setTouchAreaBindingEnabled(true);

		{
			/* Camera target */
			final int centerX = (int) ((CandiConstants.CANDI_VIEW_WIDTH - mProgressTextureRegion.getTileWidth()) / 2);
			final int centerY = (int) (CandiConstants.CANDI_VIEW_TITLE_HEIGHT + (CandiConstants.CANDI_VIEW_BODY_HEIGHT - mProgressTextureRegion
					.getTileHeight()) / 2);

			/* Progress sprite */
			mProgressSprite = new CandiAnimatedSprite(0, 0, mProgressTextureRegion);
			mProgressSprite.setPosition(centerX, centerY);
			mProgressSprite.setBlendFunction(CandiConstants.GL_BLEND_FUNCTION_SOURCE, CandiConstants.GL_BLEND_FUNCTION_DESTINATION);
			mProgressSprite.animate(150, true);
			mProgressSprite.setVisible(false);
			//scene.getChild(CandiConstants.LAYER_GENERAL).attachChild(mProgressSprite);

			/* Invisible entity used to scroll */
			mCameraTargetSprite = new CameraTargetSprite(0, CandiConstants.CANDI_VIEW_TITLE_HEIGHT, CandiConstants.CANDI_VIEW_WIDTH,
					CandiConstants.CANDI_VIEW_BODY_HEIGHT, this);
			mCameraTargetSprite.setColor(1, 0, 0, 0.2f);
			mCameraTargetSprite.setBlendFunction(CandiConstants.GL_BLEND_FUNCTION_SOURCE, CandiConstants.GL_BLEND_FUNCTION_DESTINATION);
			mCameraTargetSprite.setVisible(false);
			scene.getChild(CandiConstants.LAYER_GENERAL).attachChild(mCameraTargetSprite);

			/* Tie camera position to target position. */
			mCamera.setChaseEntity(mCameraTargetSprite);

			/* Scene touch handling */
			scene.setOnSceneTouchListener(new IOnSceneTouchListener() {

				@Override
				public boolean onSceneTouchEvent(final Scene pScene, final TouchEvent pSceneTouchEvent) {

					/*
					 * TouchEvent is world coordinates
					 * MotionEvent is screen coordinates
					 */
					renderingActivate();
					if (mFullUpdateInProgress || mIgnoreInput)
						return true;

					final float screenX = pSceneTouchEvent.getMotionEvent().getX();

					/* Check for a fling or double tap using the gesture detector */
					if (mGestureDetector.onTouchEvent(pSceneTouchEvent.getMotionEvent())) {
						return true;
					}

					if (pSceneTouchEvent.isActionDown()) {
						mLastMotionX = screenX;
						mCameraTargetSprite.clearEntityModifiers();
						return true;
					}

					if (pSceneTouchEvent.isActionUp()) {
						ZoneModel nearestZone = getNearestZone(mCameraTargetSprite.getX(), false);
						if (nearestZone != null) {
							Logger.v(null, "MoveNearestZone: From Scene Touch");
							mCandiPatchModel.setCandiModelFocused(nearestZone.getCandiesCurrent().get(0));
							mCameraTargetSprite.moveToZone(getNearestZone(mCameraTargetSprite.getX(), false), CandiConstants.DURATION_SLOTTING_MINOR,
									CandiConstants.EASE_SLOTTING_MINOR, new MoveListener() {

										@Override
										public void onMoveFinished() {
											manageViewsAsync();
										}

										@Override
										public void onMoveStarted() {
										}
									});

						}
						return true;
					}

					if (pSceneTouchEvent.isActionMove()) {

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
			mStyleTextureBodyZone = (String) resourceName.coerceToString();
		}
		else {
			throw new IllegalStateException("Placeholder texture was not found in the current theme");
		}

		if (mContext.getTheme().resolveAttribute(R.attr.textureBusyIndicator, resourceName, true)) {
			mStyleTextureBusyIndicator = (String) resourceName.coerceToString();
		}
		else {
			throw new IllegalStateException("Busy indicator texture was not found in the current theme");
		}

		if (mContext.getTheme().resolveAttribute(R.attr.textColorTitle, resourceName, true)) {
			mStyleTextColorTitle = (String) resourceName.coerceToString();
		}
		else {
			throw new IllegalStateException("Text color title was not found in the current theme");
		}
	}

	// --------------------------------------------------------------------------------------------
	// Primary
	// --------------------------------------------------------------------------------------------

	public void updateCandiModelFromEntity(Entity entity) {

		CandiModel candiModel = null;
		if (mCandiPatchModel.hasCandiModelForEntity(entity.id)) {
			candiModel = mCandiPatchModel.updateCandiModel(entity, mDisplayExtras);
			candiModel.getChildren().clear();
			candiModel.setChanged();

			for (Entity childEntity : entity.children) {

				CandiModel childCandiModel = null;
				if (mCandiPatchModel.hasCandiModelForEntity(childEntity.id)) {
					childCandiModel = mCandiPatchModel.updateCandiModel(childEntity, mDisplayExtras);
					childCandiModel.getViewStateCurrent().setVisible(!childCandiModel.getEntity().isHidden);
					candiModel.getChildren().add(childCandiModel);
					childCandiModel.setParent(candiModel);
					childCandiModel.setChanged();
				}
				else {
					childCandiModel = CandiModelFactory.newCandiModel(childEntity.id, childEntity, mCandiPatchModel);
					childCandiModel.setDisplayExtra(mDisplayExtras);
					mCandiPatchModel.addCandiModel(childCandiModel);
					childCandiModel.getViewStateCurrent().setVisible(!childCandiModel.getEntity().isHidden);
					candiModel.getChildren().add(childCandiModel);
					childCandiModel.setParent(candiModel);
					childCandiModel.setChanged();
				}
			}
		}
	}

	public void deleteCandiModelByEntity(Entity deletedEntity) {
		/*
		 * Used to synchronize candi models with entities
		 */
		for (CandiModel candiModel : mCandiPatchModel.getCandiModels()) {
			if (candiModel.getEntity().id.equals(deletedEntity.id)) {
				if (candiModel.getParent() != null) {
					CandiModel parentCandiModel = (CandiModel) candiModel.getParent();
					parentCandiModel.getChildren().remove(candiModel);
				}
				removeCandiModel(candiModel);

				/* Make sure we have a new focus if needed */
				if (mCandiPatchModel.getCandiModelFocused() == null) {
					ZoneModel zoneModel = getNearestZone(mCameraTargetSprite.getX(), false);
					if (zoneModel != null && zoneModel.getCandiesCurrent().size() > 0) {
						mCandiPatchModel.setCandiModelFocused(getNearestZone(mCameraTargetSprite.getX(), false).getCandiesCurrent().get(0));
						//						mCameraTargetSprite.moveToZone(getNearestZone(mCameraTargetSprite.getX(), false), CandiConstants.DURATION_SLOTTING_MINOR,
						//								CandiConstants.EASE_SLOTTING_MINOR);
					}
				}

				break;
			}
		}
	}

	public void updateCandiData(List<Entity> proxiEntities, boolean fullUpdate, boolean delayObserverUpdate) {
		/*
		 * Primary entry point from the host activity. This is a primary trigger
		 * that should update the model and ripple to the views.
		 */
		if (fullUpdate) {
			Logger.d(null, "Starting full update.");

			/*
			 * Clears all game engine sprites.
			 * Clears all touch areas.
			 * Clears zone and candi model collections.
			 * Reloads shared textures.
			 * Creates new root candi model
			 */
			mFullUpdateInProgress = true;
			mCandiViewsHash.clear();
			mZoneViews.clear();
			clearCandiLayer();
			clearZoneLayer();
			mEngine.getScene().clearTouchAreas();
			mCandiPatchModel.reset(); /* Clears zone and candi model collections */
			initialize(); /* Reloads shared textures */
		}

		else {
			Logger.d(null, "Starting partial update.");

			/* Clear out models that have previously marked for deletion */
			for (int i = mCandiPatchModel.getCandiModels().size() - 1; i >= 0; i--) {
				if (mCandiPatchModel.getCandiModels().get(i).getReasonInactive() == ReasonInactive.Deleting)
					removeCandiModel(mCandiPatchModel.getCandiModels().get(i));
			}

			/* Remove orphaned models */
			for (int i = mCandiPatchModel.getCandiModels().size() - 1; i >= 0; i--) {
				CandiModel candiModel = mCandiPatchModel.getCandiModels().get(i);
				boolean orphaned = true;
				for (Entity entity : proxiEntities) {
					if (entity.id.equals(candiModel.getEntity().id)) {
						orphaned = false;
						break;
					}
					else {
						for (Entity childEntity : entity.children) {
							if (childEntity.id.equals(candiModel.getEntity().id)) {
								orphaned = false;
								break;
							}
						}
					}
				}
				if (orphaned) {
					candiModel.setReasonInactive(ReasonInactive.Deleting);
				}
			}
		}

		CandiModel candiRootPrev = (CandiModel) mCandiPatchModel.getCandiRootCurrent();
		CandiModel candiRootNext = new CandiModel(-1, mCandiPatchModel);
		candiRootNext.setSuperRoot(true);

		/* Strip linkages */
		for (CandiModel candiModel : mCandiPatchModel.getCandiModels()) {
			candiModel.setParent(null);
			candiModel.getChildren().clear();
		}

		/* Make sure each entity has a candi model */
		for (Entity entity : proxiEntities) {

			CandiModel candiModel = null;
			if (mCandiPatchModel.hasCandiModelForEntity(entity.id)) {
				candiModel = mCandiPatchModel.updateCandiModel(entity, mDisplayExtras);
				candiModel.setParent(candiRootNext);
				candiRootNext.getChildren().add(candiModel);
			}
			else {
				candiModel = CandiModelFactory.newCandiModel(entity.id, entity, mCandiPatchModel);
				candiModel.setDisplayExtra(mDisplayExtras);
				mCandiPatchModel.addCandiModel(candiModel);
				candiModel.setParent(candiRootNext);
				candiRootNext.getChildren().add(candiModel);
			}

			for (Entity childEntity : entity.children) {

				CandiModel childCandiModel = null;
				if (mCandiPatchModel.hasCandiModelForEntity(childEntity.id)) {
					childCandiModel = mCandiPatchModel.updateCandiModel(childEntity, mDisplayExtras);
					candiModel.getChildren().add(childCandiModel);
					childCandiModel.setParent(candiModel);
				}
				else {
					childCandiModel = CandiModelFactory.newCandiModel(childEntity.id, childEntity, mCandiPatchModel);
					childCandiModel.setDisplayExtra(mDisplayExtras);
					mCandiPatchModel.addCandiModel(childCandiModel);
					candiModel.getChildren().add(childCandiModel);
					childCandiModel.setParent(candiModel);
				}
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

		/* Navigate to make sure we are completely configured */
		navigateModel(mCandiPatchModel.getCandiRootNext(), delayObserverUpdate);

		/* Make sure we have a current candi and UI is centered on it */
		if (fullUpdate) {
			mFullUpdateInProgress = false;
			if (mCandiPatchModel.getZonesOccupiedNextCount() > 0) {
				mCandiPatchModel.setCandiModelFocused(mCandiPatchModel.getZones().get(0).getCandiesNext().get(0));
				mCameraTargetSprite.moveToZone(mCandiPatchModel.getZones().get(0), 0.5f);
			}
		}
		else {
			if (mCandiPatchModel.getCandiModelFocused() == null) {
				ZoneModel zoneModel = getNearestZone(mCameraTargetSprite.getX(), false);
				if (zoneModel != null && zoneModel.getCandiesCurrent().size() > 0) {
					mCandiPatchModel.setCandiModelFocused(getNearestZone(mCameraTargetSprite.getX(), false).getCandiesCurrent().get(0));
					mCameraTargetSprite.moveToZone(getNearestZone(mCameraTargetSprite.getX(), false), CandiConstants.DURATION_SLOTTING_MINOR,
							CandiConstants.EASE_SLOTTING_MINOR);
				}
			}
		}
	}

	public void navigateModel(IModel candiRootNext, boolean delayObserverUpdate) {

		long startTime = System.nanoTime();
		Logger.d(null, "Starting model navigation.");

		mCandiPatchModel.setCandiRootNext(candiRootNext);

		/* Updates */
		mCandiPatchModel.updateVisibilityNext();
		mCandiPatchModel.updateZonesNext();
		mCandiPatchModel.updatePositionsNext();
		mCandiPatchModel.updateMiscNext();

		/* Make sure zones have a view */
		for (ZoneModel zoneModel : mCandiPatchModel.getZones()) {
			ensureZoneView(zoneModel);
		}

		/*
		 * Reset focus to primary if we are navigating up to root and currently
		 * focused on a child
		 */
		if (candiRootNext.isSuperRoot() && mCandiPatchModel.getCandiModelFocused() != null) {
			//Logger.d(null, "Starting model navigation.");
			if (!mCandiPatchModel.getCandiModelFocused().getParent().isSuperRoot()) {
				mCandiPatchModel.setCandiModelFocused((CandiModel) mCandiPatchModel.getCandiModelFocused().getParent());
			}
		}

		/* For animations, we need to create views in advance. */
		if (CandiConstants.TRANSITIONS_ACTIVE) {

			/* Clear out any left over actions and modifiers */
			for (CandiModel candiModel : mCandiPatchModel.getCandiModels()) {
				synchronized (candiModel.getViewModifiers()) {
					candiModel.getViewModifiers().clear();
				}
				for (IModel childModel : candiModel.getChildren()) {
					CandiModel childCandiModel = (CandiModel) childModel;
					synchronized (childCandiModel.getViewModifiers()) {
						childCandiModel.getViewModifiers().clear();
					}
				}
			}

			manageViews(false, true);
			doTransitionAnimations();
		}

		/* Candies come and go so make sure our zone positioning is correct */
		ensureZoneFocus();

		/* Trigger epoch observer updates */
		if (!delayObserverUpdate) {
			mCandiPatchModel.update();
		}

		/* Copy next to current across the model and child models */
		mCandiPatchModel.shiftToNext();

		/* Reset camera boundaries */
		setCameraBoundaries(mScene);

		/* Now that all the view entities have updated we can do global operations like z sorting. */
		IEntity layer = mEngine.getScene().getChild(CandiConstants.LAYER_CANDI);
		layer.sortChildren();

		/* Without animations, we can lazy create views. */
		if (!CandiConstants.TRANSITIONS_ACTIVE) {
			manageViewsAsync();
		}

		long estimatedTime = System.nanoTime() - startTime;
		Logger.v(null, "Navigate finished: " + String.valueOf(estimatedTime / 1000000) + "ms");
	}

	@SuppressWarnings("unused")
	private IView ensureCandiView(final CandiModel candiModel) {

		/* We use the observer count as an indication of whether this candi model already has a candi view. */
		if (!mCandiViewsHash.containsKey(candiModel.getModelIdAsString()) && !candiModel.getEntity().isHidden) {

			if (candiModel.countObservers() > 0) {
				throw new IllegalStateException("CandiModel has an observer but CandiViewHash is empty");
			}

			final CandiView candiView = new CandiView(CandiPatchPresenter.this);
			candiView.setTitleTextColor(Color.parseColor(getStyleTextColorTitle()));
			mEngine.getScene().getChild(CandiConstants.LAYER_CANDI).attachChild(candiView);

			candiView.setViewTouchListener(new ViewTouchListener() {

				@Override
				public void onViewDoubleTap(IView view) {
					if (!mIgnoreInput) {
						mIgnoreInput = true;
						doCandiViewDoubleTap(candiView);
					}
				}

				@Override
				public void onViewLongPress(IView view) {}

				@Override
				public void onViewSingleTap(IView view) {
					if (!mIgnoreInput) {
						mIgnoreInput = true;
						doCandiViewSingleTap(candiView);
					}
				}
			});

			candiView.setModel(candiModel);
			candiModel.addObserver(candiView);
			mCandiViewsHash.put(String.valueOf(candiModel.getModelId()), candiView);

			candiView.progressVisible(true);
			candiView.loadHardwareTextures();
			candiView.initialize();

			return candiView;
		}

		return null;
	}

	private IView ensureZoneView(ZoneModel zoneModel) {

		if (zoneModel.getViewStateNext().isVisible() && zoneModel.countObservers() == 0) {

			final ZoneView zoneView = new ZoneView(zoneModel, CandiPatchPresenter.this);
			zoneView.setTitleTextColor(Color.parseColor(getStyleTextColorTitle()));
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

			/* Create a candi view and do all the configuration that isn't model specific. */
			final CandiView candiView = new CandiView(CandiPatchPresenter.this);

			candiView.setTitleTextColor(Color.parseColor(getStyleTextColorTitle()));
			candiView.setTitleTextFillColor(Color.TRANSPARENT);
			mEngine.getScene().getChild(CandiConstants.LAYER_CANDI).attachChild(candiView);

			mCandiActivity.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					candiView.setGestureDetector(new GestureDetector(mCandiActivity, candiView));

				}
			});

			candiView.setViewTouchListener(new ViewTouchListener() {

				@Override
				public void onViewDoubleTap(IView view) {
					if (!mIgnoreInput) {
						mIgnoreInput = true;
						doCandiViewDoubleTap(candiView);
					}
				}

				@Override
				public void onViewLongPress(IView view) {}

				@Override
				public void onViewSingleTap(IView view) {
					if (!mIgnoreInput) {
						mIgnoreInput = true;
						doCandiViewSingleTap(candiView);
					}
				}
			});

			return candiView;
		}
	}

	public void getCandiViewFromPool(final CandiModel candiModel, boolean localUpdate, boolean useNext) {

		/* CandiView has been pre-configured except for anything that is model specific. */
		final CandiView candiView = mCandiViewPool.obtainPoolItem();

		if (candiView.isRecycled()) {
			Logger.v(null, "CandiView pulled from pool: " + candiModel.getTitleText());
		}
		else {
			Logger.v(null, "CandiView created: " + candiModel.getTitleText());
		}

		candiView.setRecycled(false);
		candiView.setModel(candiModel);
		candiView.setCandiPatchPresenter(CandiPatchPresenter.this);
		candiModel.addObserver(candiView);
		getCandiViewsHash().put(String.valueOf(candiModel.getModelId()), candiView);

		if (candiView.isHardwareTexturesInitialized()) {
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
			synchronized (zoneModel.getViewModifiers()) {
				zoneModel.getViewModifiers().clear();
				zoneModel.getViewModifiers().addLast(
						new CandiAlphaModifier(null, CandiConstants.DURATION_TRANSITIONS_FADE, 1.0f, 0.0f, CandiConstants.EASE_FADE_OUT));
			}
			zoneModel.getViewStateCurrent().setVisible(false);
			zoneModel.setChanged();
			zoneModel.update();
		}

		/*
		 * Using localUpdate means this candiview is not part of a double buffer update so
		 * we take complete control of how the candiview is snapped to the model. If the model
		 * has any pre-loaded actions and/or modifiers, we strip them.
		 * 
		 * We monitor current visible state because transition logic won't fade in if it
		 * thinks it is already supposed to be visible.
		 */
		if (localUpdate || candiModel.getViewStateCurrent().isVisible()) {
			if (CandiConstants.TRANSITIONS_ACTIVE) {
				synchronized (candiModel.getViewModifiers()) {
					candiModel.getViewModifiers().clear();
					candiModel.getViewModifiers().addLast(

					new CandiAlphaModifier(null, CandiConstants.DURATION_INSTANT, 0.0f, 1.0f, CandiConstants.EASE_FADE_IN));
				}
				synchronized (candiModel.getViewActions()) {
					candiModel.getViewActions().clear();
					candiModel.getViewActions().addLast(new ViewAction(ViewActionType.Position));
					candiModel.getViewActions().addLast(new ViewAction(ViewActionType.Scale));
					candiModel.getViewActions().addLast(new ViewAction(ViewActionType.ExpandCollapse));
					//candiModel.getViewActions().addLast(new ViewAction(ViewActionType.ReflectionHideShow));
				}
			}
			candiModel.setChanged();
			candiModel.update();
		}
	}

	public void sendCandiViewToPool(CandiModel candiModel, boolean useNext) {
		Logger.v(null, "CandiView recycled to the pool: " + candiModel.getTitleText());

		String modelId = String.valueOf(candiModel.getModelId());
		CandiView candiView = (CandiView) getCandiViewsHash().get(modelId);

		/* Show zone ui */
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
		getCandiViewsHash().remove(modelId);
		if (candiView != null) {
			mCandiViewPool.recyclePoolItem(candiView);
		}
	}

	public class ManageViewsThread extends Thread {

		@Override
		public void run() {
			manageViews(true, false);
		}
	};

	private void manageViews(boolean localUpdate, boolean useNext) {
		CandiList candiModels = mCandiPatchModel.getCandiModels();
		int countCandiModels = candiModels.size();

		if (localUpdate) {
			Logger.v(this, "Starting view management pass: async");
		}
		else {
			Logger.v(this, "Starting view management pass");
		}

		/* Find the last visible candi so we can use it to determine the halo */
		//float candiModelLastX = getLastVisibleCandiModelX(useNext);
		float candiModelLastX = 0;

		/* Recycle views first */
		for (int i = 0; i < countCandiModels; i++) {
			final CandiModel candiModel = (CandiModel) candiModels.get(i);
			ViewState viewStateCurrent = candiModel.getViewStateCurrent();
			ViewState viewStateNext = candiModel.getViewStateNext();

			/* Keep views that are within the halo current or next to optimize transitions */
			boolean isWithinHalo = viewStateCurrent.isWithinHalo(mCamera, candiModelLastX);
			if (!isWithinHalo) {
				isWithinHalo = viewStateNext.isWithinHalo(mCamera, candiModelLastX);
			}

			if (!isWithinHalo && mCandiViewsHash.containsKey(candiModel.getModelIdAsString())) {
				sendCandiViewToPool(candiModel, useNext);
			}
		}

		/* First, allocate any views needed for the candi with the current focus */
		CandiModel candiModelFocused = mCandiPatchModel.getCandiModelFocused();
		if (localUpdate) {
			if (candiModelFocused.getViewStateCurrent().isVisible() && !mCandiViewsHash.containsKey(candiModelFocused.getModelIdAsString())) {
				getCandiViewFromPool(candiModelFocused, localUpdate, useNext);
				for (IModel candiModelChildFocused : candiModelFocused.getChildren()) {
					if (candiModelChildFocused.getViewStateCurrent().isVisible() && !mCandiViewsHash
								.containsKey(((CandiModel) candiModelChildFocused)
										.getModelIdAsString())) {
						getCandiViewFromPool((CandiModel) candiModelChildFocused, localUpdate, useNext);
					}
				}
			}
		}

		/*
		 * Now do the rest. Any we already did above won't be done again because they
		 * are now in the view tracking hash map.
		 */
		for (int i = 0; i < countCandiModels; i++) {
			final CandiModel candiModel = (CandiModel) candiModels.get(i);
			ViewState viewState = useNext ? candiModel.getViewStateNext() : candiModel.getViewStateCurrent();

			if (viewState.isVisible()) {
				boolean isWithinHalo = viewState.isWithinHalo(mCamera, candiModelLastX);
				if (isWithinHalo && !mCandiViewsHash.containsKey(candiModel.getModelIdAsString())) {
					getCandiViewFromPool(candiModel, localUpdate, useNext);
				}
			}
		}
	}

	public float getLastVisibleCandiModelX(boolean useNext) {
		CandiList candiModels = mCandiPatchModel.getCandiModels();
		int countCandiModels = candiModels.size();

		float lastX = 0;
		for (int i = 0; i < countCandiModels; i++) {
			final CandiModel candiModel = (CandiModel) candiModels.get(i);
			ViewState viewState = useNext ? candiModel.getViewStateNext() : candiModel.getViewStateCurrent();

			//			if (viewState.isVisible()) {
			//				Logger.v(CandiConstants.APP_NAME, this.getClass().getSimpleName(), "Treating as active: " + candiModel.getTitleText());
			//			}

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
	// Event handlers
	// --------------------------------------------------------------------------------------------

	private void doCandiViewSingleTap(IView candiView) {
		renderingActivate();

		final CandiModel candiModel = (CandiModel) candiView.getModel();
		Logger.d(this, "SingleTap triggered: " + candiModel.getEntity().label);

		mCandiPatchModel.setCandiModelFocused(candiModel);
		float distanceToMove = Math.abs(mCameraTargetSprite.getX() - candiModel.getZoneStateCurrent().getZone().getViewStateCurrent().getX());

		if (distanceToMove != 0) {

			mCameraTargetSprite.clearEntityModifiers();
			mCameraTargetSprite.moveToZone(candiModel.getZoneStateCurrent().getZone(), CandiConstants.DURATION_SLOTTING_MAJOR,
					CandiConstants.EASE_SLOTTING_MAJOR,
					new MoveListener() {

						@Override
						public void onMoveFinished() {
							if (candiModel.getZoneStateCurrent().getStatus() == ZoneStatus.Secondary) {
								mCandiActivity.runOnUiThread(new Runnable() {

									@Override
									public void run() {
										navigateModel(candiModel.getParent(), false);
										mIgnoreInput = false;
									}
								});
							}
							else {
								if (mCandiListener != null)
									mCandiListener.onSingleTap(candiModel);
							}
						}

						@Override
						public void onMoveStarted() {
						}
					});
		}
		else {
			if (candiModel.getZoneStateCurrent().getStatus() == ZoneStatus.Secondary) {
				navigateModel(candiModel.getParent(), false);
				mIgnoreInput = false;
			}
			else {
				if (mCandiListener != null)
					mCandiListener.onSingleTap(candiModel);
			}
		}
	}

	private void doCandiViewDoubleTap(IView candiView) {
		final CandiModel candiModel = (CandiModel) candiView.getModel();
		Logger.d(this, "DoubleTap triggered: " + candiModel.getEntity().label);

		float fromScale = mCameraTargetSprite.getScaleX();
		float toScale = 1;

		if (mCameraTargetSprite.getScaleX() == 1) {
			float fullWidth = mCamera.getSurfaceWidth() - (CandiConstants.CANDI_VIEW_ZOOMED_PADDING * 2);
			float fullHeight = mCamera.getSurfaceHeight() - (CandiConstants.CANDI_VIEW_ZOOMED_PADDING * 2);
			float toScaleWidth = fullWidth / CandiConstants.CANDI_VIEW_WIDTH;
			float toScaleHeight = fullHeight / CandiConstants.CANDI_VIEW_BODY_HEIGHT;
			toScale = toScaleWidth < toScaleHeight ? toScaleWidth : toScaleHeight;
		}

		mCameraTargetSprite.registerEntityModifier(new ParallelEntityModifier(new ScaleModifier(CandiConstants.DURATION_ZOOM, fromScale, toScale,
				EaseQuartInOut.getInstance())));

		mIgnoreInput = false;
	}

	// --------------------------------------------------------------------------------------------
	// Animation
	// --------------------------------------------------------------------------------------------

	private void doTransitionAnimations() {

		/*
		 * Zone transitions
		 * 
		 * The zone might already have a fade out modifier because manageViews() populated
		 * it with a full size candi view.
		 */

		for (ZoneModel zoneModel : mCandiPatchModel.getZones()) {
			synchronized (zoneModel.getViewActions()) {
				zoneModel.getViewActions().addLast(new ViewAction(ViewActionType.Visibility));
			}
		}

		/* Clear out any left over actions and modifiers */
		for (CandiModel candiModel : mCandiPatchModel.getCandiModels()) {
			synchronized (candiModel.getViewModifiers()) {
				//candiModel.getViewModifiers().clear();
			}
			for (IModel childModel : candiModel.getChildren()) {
				CandiModel childCandiModel = (CandiModel) childModel;
				synchronized (childCandiModel.getViewModifiers()) {
					//childCandiModel.getViewModifiers().clear();
				}
			}
		}

		/* Ensure that the candi with focus always draws front-most during animations. */
		if (mCandiPatchModel.getCandiModelFocused() != null) {
			CandiModel candiModel = mCandiPatchModel.getCandiModelFocused();
			int childCount = mCandiPatchModel.getCandiModels().size();
			for (int i = 0; i < childCount; i++) {
				CandiModel candiModelTemp = mCandiPatchModel.getCandiModels().get(i);
				if (candiModelTemp.equals(candiModel)) {
					candiModelTemp.getViewStateNext().setZIndex(childCount);
				}
				else {
					candiModelTemp.getViewStateNext().setZIndex(i);
				}
				synchronized (candiModelTemp.getViewActions()) {
					candiModelTemp.getViewActions().addLast(new ViewAction(ViewActionType.ZIndex));
				}
			}
		}

		/* Candi transitions */
		for (ZoneModel zone : mCandiPatchModel.getZones()) {
			boolean needDelay = false;
			if (zone.isOccupiedCurrent()) {

				for (CandiModel candiModel : zone.getCandiesCurrent()) {
					synchronized (candiModel.getViewModifiers()) {
						if (!candiModel.getViewModifiers().isEmpty()) {
							Logger.v(this, "Transition skipped because modifiers not empty: " + candiModel.getTitleText());
						}

						if (candiModel.getViewModifiers().isEmpty()) {

							Transition transition = candiModel.getTransition();
							ViewState viewStateCurrent = candiModel.getViewStateCurrent();
							ViewState viewStateNext = candiModel.getViewStateNext();

							if (transition != Transition.None) {
								Logger.v(this, "Transition From: " + transition.toString()
																					+ ": "
																					+ candiModel.getTitleText());
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
							else if (transition == Transition.OverflowOut) {
								ViewState viewStateNextFocus = mCandiPatchModel.getCandiModelFocused().getViewStateNext();
								candiModel.getViewModifiers().addLast(
										new ParallelEntityModifier(
												new CandiAlphaModifier(null, CandiConstants.DURATION_TRANSITIONS_OVERFLOW_FADE, 1.0f, 0.0f,
														CandiConstants.EASE_FADE_OUT_WEAK),
												new ScaleModifier(CandiConstants.DURATION_TRANSITIONS_MOVE, viewStateCurrent.getScale(),
														viewStateNextFocus.getScale(), EaseQuartInOut.getInstance()),
												new MoveModifier(CandiConstants.DURATION_TRANSITIONS_MOVE, viewStateCurrent.getX(),
														viewStateNextFocus.getX(), viewStateCurrent.getY(), viewStateNextFocus.getY(), EaseQuartInOut
																.getInstance())));

								synchronized (candiModel.getViewActions()) {
									candiModel.getViewActions().addLast(new ViewAction(ViewActionType.ReflectionHideShowAnim));
								}
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
						if (!candiModel.getViewModifiers().isEmpty()) {
							Logger.v(this, "Transition skipped because modifiers not empty: " + candiModel.getTitleText());
						}
						if (candiModel.getViewModifiers().isEmpty()) {

							Transition transition = candiModel.getTransition();
							ViewState viewStateCurrent = candiModel.getViewStateCurrent();
							ViewState viewStateNext = candiModel.getViewStateNext();

							if (transition != Transition.None) {
								Logger.v(this, "Transition To: " + transition.toString()
																					+ ": "
																					+ candiModel.getTitleText());
							}

							//							if (transition != Transition.None && candiModel.getViewStateNext().isLastWithinHalo()) {
							//								Logger.v(this, "Transition To: " + transition.toString()
							//																					+ ": "
							//																					+ candiModel.getTitleText());
							//							}

							if (transition == Transition.FadeOut) {
								// viewStateNext.setVisible(false);
							}
							else if (transition == Transition.OverflowIn) {
								ViewState viewStateCurrentFocus = mCandiPatchModel.getCandiModelFocused().getViewStateCurrent();

								candiModel.getViewModifiers().addLast(
										new ParallelEntityModifier(
												new CandiAlphaModifier(null, CandiConstants.DURATION_TRANSITIONS_FADE, 0.0f, 1.0f,
														CandiConstants.EASE_FADE_IN),
												new ScaleModifier(CandiConstants.DURATION_TRANSITIONS_MOVE, viewStateCurrentFocus.getScale(),
														viewStateNext.getScale(), EaseQuartInOut.getInstance()),
												new MoveModifier(CandiConstants.DURATION_TRANSITIONS_MOVE, viewStateCurrentFocus.getX(),
														viewStateNext.getX(), viewStateCurrentFocus.getY(), viewStateNext.getY(), EaseQuartInOut
																.getInstance())));
								synchronized (candiModel.getViewActions()) {
									candiModel.getViewActions().addLast(new ViewAction(ViewActionType.ExpandCollapseAnim));
									candiModel.getViewActions().addLast(new ViewAction(ViewActionType.ReflectionHideShowAnim));
								}
							}
							else if (transition == Transition.FadeIn) {

								//								if (candiModel.getViewStateNext().isLastWithinHalo()) {
								if (needDelay) {
									candiModel.getViewModifiers().addLast(new DelayModifier(CandiConstants.DURATION_TRANSITIONS_DELAY));
								}

								candiModel.getViewModifiers().addLast(
											new CandiAlphaModifier(null, CandiConstants.DURATION_TRANSITIONS_FADE, 0.0f, 1.0f,
													CandiConstants.EASE_FADE_IN));
								synchronized (candiModel.getViewActions()) {
									candiModel.getViewActions().addLast(new ViewAction(ViewActionType.Position));
									candiModel.getViewActions().addLast(new ViewAction(ViewActionType.Scale));
									candiModel.getViewActions().addLast(new ViewAction(ViewActionType.ExpandCollapse));
									candiModel.getViewActions().addLast(new ViewAction(ViewActionType.ReflectionHideShow));
								}
								//							}
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
				}
			}
		}
	}

	private void ensureZoneFocus() {
		/*
		 * If candi with current focus is going away, move camera target
		 * to another entity before other moves/shows happen.
		 */
		if (mCandiPatchModel.getCandiModelFocused() != null && mCandiPatchModel.getCandiModelFocused().getZoneStateCurrent().getZone().getZoneIndex() + 1 > mCandiPatchModel
					.getZonesOccupiedNextCount()) {
			ZoneModel zoneModel = getZoneContainsCandiNext(mCandiPatchModel.getCandiModelFocused());
			if (zoneModel != null) {
				mCameraTargetSprite.moveToZone(zoneModel, CandiConstants.DURATION_SLOTTING_MAJOR, CandiConstants.EASE_SLOTTING_MAJOR);
			}
			else {
				mCameraTargetSprite.moveToZone(getNearestZone(mCameraTargetSprite.getX(), true), CandiConstants.DURATION_SLOTTING_MAJOR,
						CandiConstants.EASE_SLOTTING_MAJOR);
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
		if (firstOccupiedZone != null) {
			mBoundsMinX = firstOccupiedZone.getViewStateCurrent().getX();
			mBoundsMaxX = (mBoundsMinX + (mCandiPatchModel.getZonesOccupiedCurrentCount() - 1) * (CandiConstants.CANDI_VIEW_WIDTH + CandiConstants.CANDI_VIEW_SPACING));
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
		 * The logic here positions the viewer but does not position the camera.
		 * That means we could be looking at slot #5 but the camera is still sitting
		 * on slot #2. Touch areas track with the camera so they will be messed up. So
		 * we still set the camera target sprite as the chase entity for the
		 * camera.
		 * 
		 * Because the y axis is reversed, we need to subtract to move down.
		 */
		final float[] centerCoordinates = mCameraTargetSprite.getSceneCenterCoordinates();
		float targetX = centerCoordinates[VERTEX_INDEX_X];
		float targetY = -centerCoordinates[VERTEX_INDEX_Y];

		GLU.gluLookAt(pGL, targetX, targetY, camZ, targetX, targetY, 0, 0, 1, 0); /* move camera back */
		pGL.glScalef(1, -1, 1); /* reverse y-axis */
		pGL.glTranslatef(0f, 0, 0); /* origin at top left */
	}

	// --------------------------------------------------------------------------------------------
	// Textures
	// --------------------------------------------------------------------------------------------

	public void removeCandiModel(CandiModel candiModel) {

		/* Remove associated candi view */
		final CandiView candiView = (CandiView) getCandiViewsHash().get(String.valueOf(candiModel.getModelId()));
		getCandiViewsHash().remove(String.valueOf(candiModel.getModelId()));
		if (candiView != null) {
			renderingActivate();
			mEngine.runOnUpdateThread(new Runnable() {

				@Override
				public void run() {
					candiView.unloadResources(); /* Also removes any active touch areas for it */
					candiView.detachSelf();
				}
			});
		}

		/* Repeat for all children */
		for (IModel childModel : candiModel.getChildren()) {
			CandiModel childCandiModel = (CandiModel) childModel;
			final CandiView childCandiView = (CandiView) getCandiViewsHash().get(String.valueOf(childCandiModel.getModelId()));
			getCandiViewsHash().remove(String.valueOf(childCandiModel.getModelId()));
			renderingActivate();
			mEngine.runOnUpdateThread(new Runnable() {

				@Override
				public void run() {
					childCandiView.unloadResources(); /* Also removes any active touch areas for it */
					childCandiView.detachSelf();
				}
			});
		}

		/* Remove child models */
		for (IModel childCandiModel : candiModel.getChildren()) {
			mCandiPatchModel.getCandiModels().remove(childCandiModel); /* Search is done using model id */
			if (mCandiPatchModel.getCandiModelFocused() == childCandiModel)
				mCandiPatchModel.setCandiModelFocused(null);
			if (mCandiPatchModel.getCandiModelSelected() == childCandiModel)
				mCandiPatchModel.setCandiModelSelected(null);
			if (mCandiPatchModel.getCandiRootCurrent() == childCandiModel)
				mCandiPatchModel.setCandiRootCurrent(null);
		}

		/* Remove parent model */
		mCandiPatchModel.getCandiModels().remove(candiModel); /* Search is done using model id */
		if (mCandiPatchModel.getCandiModelFocused() == candiModel)
			mCandiPatchModel.setCandiModelFocused(null);
		if (mCandiPatchModel.getCandiModelSelected() == candiModel)
			mCandiPatchModel.setCandiModelSelected(null);
		if (mCandiPatchModel.getCandiRootCurrent() == candiModel)
			mCandiPatchModel.setCandiRootCurrent(null);
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

	public void loadTextures() {
		mTexture = new Texture(512, 512, CandiConstants.GL_TEXTURE_OPTION);
		mEngine.getTextureManager().loadTexture(mTexture);
	}

	public void loadTextureSources() {

		/* Textures that are shared by zone views */
		Bitmap zoneBodyBitmap = null;
		zoneBodyBitmap = ImageManager.getInstance().loadBitmapFromAssets(mStyleTextureBodyZone);
		if (zoneBodyBitmap != null) {
			Bitmap zoneReflectionBitmap = ImageUtils.makeReflection(zoneBodyBitmap, true);

			mZoneBodyTextureRegion = TextureRegionFactory.createFromSource(mTexture, new BitmapTextureSource(zoneBodyBitmap, new IBitmapAdapter() {

				@Override
				public Bitmap reloadBitmap() {
					Bitmap zoneBodyBitmap = null;
					zoneBodyBitmap = ImageManager.getInstance().loadBitmapFromAssets(mStyleTextureBodyZone);
					return zoneBodyBitmap;
				}
			}), 0, 0);

			mZoneReflectionTextureRegion = TextureRegionFactory.createFromSource(mTexture, new BitmapTextureSource(zoneReflectionBitmap,
					new IBitmapAdapter() {

						@Override
						public Bitmap reloadBitmap() {
							Bitmap zoneBodyBitmap = null;
							zoneBodyBitmap = ImageManager.getInstance().loadBitmapFromAssets(mStyleTextureBodyZone);
							if (zoneBodyBitmap != null) {
								Bitmap zoneReflectionBitmap = ImageUtils.makeReflection(zoneBodyBitmap, true);
								return zoneReflectionBitmap;
							}
							return null;
						}
					}), 0, 256);
		}

		/* Scene progress sprite */
		mPlaceholderTextureRegion = TextureRegionFactory.createFromAsset(mTexture, mContext, mStyleTextureBodyZone, 256, 0);
		mProgressTextureRegion = TextureRegionFactory.createTiledFromAsset(mTexture, mContext, mStyleTextureBusyIndicator, 256, 256, 4, 2);
	}

	public void resetTextures(TextureReset textureReset) {

		/* Candi views */
		Iterator it = getCandiViewsHash().entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			CandiView candiView = (CandiView) entry.getValue();
			if (textureReset == TextureReset.VisibleOnly && !candiView.getModel().getViewStateCurrent().isVisibleToCamera(mCamera)) {
				continue;
			}
			else if (textureReset == TextureReset.NonVisibleOnly && candiView.getModel().getViewStateCurrent().isVisibleToCamera(mCamera)) {
				continue;
			}
			candiView.resetTextureSources();
		}

		/* Zone views */
		for (final ZoneView zoneView : mZoneViews) {
			if (textureReset == TextureReset.VisibleOnly && !zoneView.isVisibleToCamera(mCamera))
				continue;
			else if (textureReset == TextureReset.NonVisibleOnly && zoneView.isVisibleToCamera(mCamera))
				continue;
			zoneView.resetTextureSources();
		}
	}

	public void resetSharedTextures() {
		mTexture.clearTextureSources();
		loadTextureSources();
	}

	// --------------------------------------------------------------------------------------------
	// Utility routines
	// --------------------------------------------------------------------------------------------

	private ZoneModel getNearestZone(float nearestToX, boolean requireOccupiedNext) {
		if (mCandiPatchModel.getZonesOccupiedNextCount() == 0)
			return null;

		int nearestIndex = 0;
		float smallestDistance = 999999;

		for (ZoneModel zone : mCandiPatchModel.getZones()) {
			if (requireOccupiedNext && !zone.isOccupiedNext())
				continue;
			if (zone.getCandiesCurrent().size() > 0) {
				float distance = Math.abs(zone.getViewStateCurrent().getX() - nearestToX);
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

	public void setCameraTarget(CameraTargetSprite mCameraTarget) {
		mCameraTargetSprite = mCameraTarget;
	}

	public void setCandiListener(ICandiListener listener) {
		mCandiListener = listener;
	}

	public Engine getEngine() {
		return mEngine;
	}

	public void setFullUpdateInProgress(boolean fullUpdateInProgress) {
		mFullUpdateInProgress = fullUpdateInProgress;
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

	public String getStyleTextureBodyZone() {
		return this.mStyleTextureBodyZone;
	}

	public String getStyleTextureBusyIndicator() {
		return this.mStyleTextureBusyIndicator;
	}

	public String getStyleTextColorTitle() {
		return mStyleTextColorTitle;
	}

	// --------------------------------------------------------------------------------------------
	// Inner classes/interfaces
	// --------------------------------------------------------------------------------------------

	public void setCandiViewsHash(HashMap candiViewsHash) {
		this.mCandiViewsHash = candiViewsHash;
	}

	public HashMap getCandiViewsHash() {
		return mCandiViewsHash;
	}

	public void setContext(Context context) {
		this.mContext = context;
	}

	public Context getContext() {
		return mContext;
	}

	public boolean isRenderingActive() {
		return mRenderingActive;
	}

	public interface ICandiListener {

		void onSelected(IModel candi);

		void onSingleTap(CandiModel candi);
	}

	public interface IMoveListener {

		void onMoveFinished();
	}

	public void renderingActivate() {
		mRenderingActive = true;
		mRenderSurfaceView.requestRender();
		synchronized (mRenderingTimer) {
			if (mRenderingTimer.getMillisUntilFinished() == 0) {
				Logger.v(this, "Rendering activated");
			}
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

	public class RenderCountDownTimer extends CountDownTimer {

		private long	mMillisUntilFinished;

		public RenderCountDownTimer(long millisInFuture) {
			this(millisInFuture, millisInFuture);
		}

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

	class SingleTapGestureDetector implements GestureDetector.OnGestureListener {

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

			/* This gets called because the gesture detector thinks it has a fling gesture */

			/* Test for swipe that is too vertical to trigger a fling */
			if (e1 != null && e2 != null) {
				if (Math.abs(e1.getY() - e2.getY()) > CandiConstants.SWIPE_MAX_OFF_PATH) {
					return false;
				}
			}

			/* Check to see if we are at a boundary */
			renderingActivate();
			float cameraTargetX = mCameraTargetSprite.getX() + 125;
			if (cameraTargetX <= mBoundsMinX || cameraTargetX >= mBoundsMaxX) {
				Logger.v(this, "MoveNearestZone: At boundary");

				mCameraTargetSprite
						.moveToZone(getNearestZone(mCameraTargetSprite.getX(), false), CandiConstants.DURATION_BOUNCEBACK,
								CandiConstants.EASE_BOUNCE_BACK);
				return false;
			}

			/* The velocity units are in pixels per second. */
			final float distanceTimeFactor = 0.8f;
			final float totalDx = (distanceTimeFactor * velocityX / 2);

			/* Cap the distance we travel so we don't race past our boundaries */
			float targetX = mCameraTargetSprite.getX() - totalDx;

			//			boolean smallFling = (Math.abs(totalDx) <= CandiConstants.SWIPE_SMALL_FLING);
			//			if (smallFling) {
			//				ZoneModel targetZoneModel = mCandiPatchModel.getCandiModelFocused().getZoneStateCurrent().getZone();
			//				ZoneModel nextZoneModel = mCandiPatchModel.getZoneNeighbor(targetZoneModel, totalDx < 0 ? true : false);
			//				if (nextZoneModel != null) {
			//					targetX = mCameraTargetSprite.getX() - nextZoneModel.getViewStateCurrent().getX();
			//				}
			//			}

			if (targetX > mBoundsMaxX - 50) {
				targetX = mBoundsMaxX - 50;
			}
			else if (targetX < mBoundsMinX) {
				targetX = mBoundsMinX - 150;
			}

			//			final String info =  "targetX = " + String.valueOf(targetX) + " totalDx = " + String.valueOf(totalDx);

			mCameraTargetSprite.registerEntityModifier(new MoveModifier(distanceTimeFactor, mCameraTargetSprite.getX(), targetX, mCameraTargetSprite
					.getY(), mCameraTargetSprite.getY(), new IEntityModifierListener() {

				@Override
				public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
					//Logger.v(this, "MoveNearestZone: Settling after fling: " + info);

					ZoneModel zoneModel = getNearestZone(mCameraTargetSprite.getX(), false);
					if (zoneModel != null) {
						CandiModel candiModel = zoneModel.getCandiesCurrent().get(0);
						if (candiModel != null) {
							mCandiPatchModel.setCandiModelFocused(zoneModel.getCandiesCurrent().get(0));

							mCameraTargetSprite.moveToZone(zoneModel, CandiConstants.DURATION_SLOTTING_MINOR,
									CandiConstants.EASE_SLOTTING_MINOR, new MoveListener() {

										@Override
										public void onMoveFinished() {
											manageViewsAsync();
										}

										@Override
										public void onMoveStarted() {
										}
									});
						}
					}

				}

				@Override
				public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {}

			}, CandiConstants.EASE_FLING));

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

	class DoubleTapGestureDetector extends SimpleOnGestureListener {

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			return false;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

			/* This gets called because the gesture detector thinks its has a fling gesture */

			/* Test for swipe that is too vertical to trigger a fling */
			if (Math.abs(e1.getY() - e2.getY()) > CandiConstants.SWIPE_MAX_OFF_PATH) {
				return false;
			}

			/* Check to see if we are at a boundary */
			float cameraTargetX = mCameraTargetSprite.getX() + 125;
			if (cameraTargetX <= mBoundsMinX || cameraTargetX >= mBoundsMaxX) {
				mCameraTargetSprite
						.moveToZone(getNearestZone(mCameraTargetSprite.getX(), false), CandiConstants.DURATION_BOUNCEBACK,
								CandiConstants.EASE_BOUNCE_BACK);
				return false;
			}

			/* The velocity units are in pixels per second. */
			final float distanceTimeFactor = 0.8f;
			final float totalDx = (distanceTimeFactor * velocityX / 2);
			boolean smallFling = (Math.abs(totalDx) <= CandiConstants.SWIPE_SMALL_FLING);

			/* Cap the distance we travel so we don't race past our boundaries */
			float targetX = mCameraTargetSprite.getX() - totalDx;

			if (smallFling) {
				ZoneModel targetZoneModel = mCandiPatchModel.getCandiModelFocused().getZoneStateCurrent().getZone();
				ZoneModel nextZoneModel = mCandiPatchModel.getZoneNeighbor(targetZoneModel, totalDx < 0 ? true : false);
				if (nextZoneModel != null)
					targetX = mCameraTargetSprite.getX() - nextZoneModel.getViewStateCurrent().getX();
			}

			if (targetX > mBoundsMaxX - 50) {
				targetX = mBoundsMaxX - 50;
			}
			else if (targetX < mBoundsMinX) {
				targetX = mBoundsMinX - 150;
			}

			mCameraTargetSprite.registerEntityModifier(new MoveModifier(distanceTimeFactor, mCameraTargetSprite.getX(), targetX, mCameraTargetSprite
					.getY(), mCameraTargetSprite.getY(), new IEntityModifierListener() {

				@Override
				public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
					mCandiPatchModel.setCandiModelFocused(getNearestZone(mCameraTargetSprite.getX(), false).getCandiesCurrent().get(0));
					mCameraTargetSprite.moveToZone(getNearestZone(mCameraTargetSprite.getX(), false), CandiConstants.DURATION_SLOTTING_MINOR,
							CandiConstants.EASE_SLOTTING_MINOR);
				}

				@Override
				public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {}

			}, CandiConstants.EASE_FLING));

			return true;
		}
	}

	public static enum TextureReset {
		All, VisibleOnly, NonVisibleOnly
	}
}
