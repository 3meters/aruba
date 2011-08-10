package com.proxibase.aircandi.candi.presenters;

import static org.anddev.andengine.util.constants.Constants.VERTEX_INDEX_X;
import static org.anddev.andengine.util.constants.Constants.VERTEX_INDEX_Y;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.microedition.khronos.opengles.GL10;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.modifier.AlphaModifier;
import org.anddev.andengine.entity.modifier.DelayModifier;
import org.anddev.andengine.entity.modifier.MoveModifier;
import org.anddev.andengine.entity.modifier.ParallelEntityModifier;
import org.anddev.andengine.entity.modifier.ScaleModifier;
import org.anddev.andengine.entity.modifier.IEntityModifier.IEntityModifierListener;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.Scene.IOnSceneTouchListener;
import org.anddev.andengine.entity.scene.background.ColorBackground;
import org.anddev.andengine.entity.util.FPSLogger;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.texture.Texture;
import org.anddev.andengine.opengl.texture.TextureOptions;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.opengl.texture.region.TextureRegionFactory;
import org.anddev.andengine.opengl.texture.region.TiledTextureRegion;
import org.anddev.andengine.opengl.util.GLHelper;
import org.anddev.andengine.opengl.view.RenderSurfaceView;
import org.anddev.andengine.util.modifier.IModifier;
import org.anddev.andengine.util.modifier.ease.EaseExponentialOut;
import org.anddev.andengine.util.modifier.ease.EaseQuartInOut;
import org.anddev.andengine.util.modifier.ease.EaseQuartOut;
import org.anddev.andengine.util.modifier.ease.IEaseFunction;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLU;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.GestureDetector.SimpleOnGestureListener;

import com.proxibase.aircandi.activities.CandiSearchActivity;
import com.proxibase.aircandi.candi.camera.ChaseCamera;
import com.proxibase.aircandi.candi.models.CandiModel;
import com.proxibase.aircandi.candi.models.CandiModelBuilder;
import com.proxibase.aircandi.candi.models.CandiPatchModel;
import com.proxibase.aircandi.candi.models.IModel;
import com.proxibase.aircandi.candi.models.ZoneModel;
import com.proxibase.aircandi.candi.models.BaseModel.ModelType;
import com.proxibase.aircandi.candi.models.CandiModel.DisplayExtra;
import com.proxibase.aircandi.candi.models.CandiModel.Transition;
import com.proxibase.aircandi.candi.models.ZoneModel.Position;
import com.proxibase.aircandi.candi.sprites.CameraTargetSprite;
import com.proxibase.aircandi.candi.sprites.CandiAnimatedSprite;
import com.proxibase.aircandi.candi.sprites.CandiScene;
import com.proxibase.aircandi.candi.sprites.CameraTargetSprite.MoveListener;
import com.proxibase.aircandi.candi.utils.CandiConstants;
import com.proxibase.aircandi.candi.views.BaseView;
import com.proxibase.aircandi.candi.views.CandiView;
import com.proxibase.aircandi.candi.views.CandiViewBuilder;
import com.proxibase.aircandi.candi.views.IView;
import com.proxibase.aircandi.candi.views.ZoneView;
import com.proxibase.aircandi.candi.views.ZoneViewBuilder;
import com.proxibase.aircandi.utils.AircandiUI;
import com.proxibase.aircandi.utils.BitmapTextureSource;
import com.proxibase.aircandi.utils.ImageManager;
import com.proxibase.aircandi.utils.Utilities;
import com.proxibase.sdk.android.proxi.consumer.EntityProxy;

public class CandiPatchPresenter implements Observer {

	public static enum TextureReset {
		All, VisibleOnly, NonVisibleOnly
	}

	private static String		COMPONENT_NAME			= "CandiPatchPresenter";
	private static float		SCALE_NORMAL			= 1;

	public CandiPatchModel		mCandiPatchModel;
	private List<CandiView>		mCandiViews				= new ArrayList<CandiView>();
	private List<ZoneView>		mZoneViews				= new ArrayList<ZoneView>();

	private GestureDetector		mGestureDetector;
	private boolean				mIgnoreInput			= false;
	private Context				mContext;
	private Engine				mEngine;
	public CandiAnimatedSprite	mProgressSprite;
	private float				mLastMotionX;
	private CameraTargetSprite	mCameraTargetSprite;
	private float				mBoundsMinX;
	private float				mBoundsMaxX;
	private ChaseCamera			mCamera;
	private Scene				mScene;

	public Texture				mTexture;

	public TiledTextureRegion	mProgressTextureRegion;
	public TextureRegion		mPlaceholderTextureRegion;
	public TextureRegion		mZoneBodyTextureRegion;
	public TextureRegion		mZoneReflectionTextureRegion;

	public CandiSearchActivity	mCandiActivity;
	private RenderSurfaceView	mRenderSurfaceView;
	public DisplayExtra			mDisplayExtras;
	public boolean				mFullUpdateInProgress	= true;

	private ICandiListener		mCandiListener;
	public int					mTouchSlopSquare;
	public int					mDoubleTapSlopSquare;
	protected float				mTouchStartX;
	protected float				mTouchStartY;
	protected boolean			mTouchGrabbed;

	private IEaseFunction		mEaseSlottingMinor		= EaseQuartOut.getInstance();
	private IEaseFunction		mEaseSlottingMajor		= EaseQuartOut.getInstance();
	private IEaseFunction		mEaseBounceBack			= EaseQuartOut.getInstance();
	private IEaseFunction		mEaseFling				= EaseExponentialOut.getInstance();

	private float				mDurationSlottingMinor	= 0.5f;
	private float				mDurationSlottingMajor	= 1.0f;
	private float				mDurationBounceBack		= 1.0f;
	private float				mDurationZoom			= 1.0f;

	public CandiPatchPresenter(Context context, CandiSearchActivity activity, Engine engine, RenderSurfaceView renderSurfaceView,
			CandiPatchModel candiPatchModel) {

		this.mCandiPatchModel = candiPatchModel;
		this.mContext = context;
		this.mEngine = engine;
		this.mCamera = (ChaseCamera) engine.getCamera();
		this.mRenderSurfaceView = renderSurfaceView;
		this.mCandiActivity = activity;

		initialize();
	}

	private void initialize() {

		// Gestures
		mGestureDetector = new GestureDetector(mContext, new CandiPatchSurfaceGestureDetector());
		final ViewConfiguration configuration = ViewConfiguration.get(mContext);

		int touchSlop = configuration.getScaledTouchSlop();
		int doubleTapSlop = configuration.getScaledDoubleTapSlop();

		mTouchSlopSquare = (touchSlop * touchSlop) / 4;
		mDoubleTapSlopSquare = doubleTapSlop * doubleTapSlop;

		// Origin
		mCandiPatchModel.setOriginX(0);
		mCandiPatchModel.setOriginY(0);

		// Textures
		loadTextures();
		loadTextureSources();
	}

	public Scene initializeScene() {
		Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, "initScene called");

		final CandiScene scene = new CandiScene(3) {

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

		scene.setBackground(new ColorBackground(0, 0, 0, 0)); // Transparent
		scene.setTouchAreaBindingEnabled(true);

		{
			// Camera target
			final int centerX = (int) ((CandiConstants.CANDI_VIEW_WIDTH - mProgressTextureRegion.getTileWidth()) / 2);
			final int centerY = (int) (CandiConstants.CANDI_VIEW_TITLE_HEIGHT + (CandiConstants.CANDI_VIEW_BODY_HEIGHT - mProgressTextureRegion
					.getTileHeight()) / 2);

			// Progress sprite
			mProgressSprite = new CandiAnimatedSprite(0, 0, mProgressTextureRegion);
			mProgressSprite.setPosition(centerX, centerY);
			mProgressSprite.animate(150, true);
			mProgressSprite.setVisible(true);
			scene.getChild(CandiConstants.LAYER_GENERAL).attachChild(mProgressSprite);

			// Invisible entity used to scroll
			mCameraTargetSprite = new CameraTargetSprite(0, CandiConstants.CANDI_VIEW_TITLE_HEIGHT, CandiConstants.CANDI_VIEW_WIDTH,
					CandiConstants.CANDI_VIEW_BODY_HEIGHT);
			mCameraTargetSprite.setColor(1, 0, 0, 0.2f);
			mCameraTargetSprite.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
			mCameraTargetSprite.setVisible(false);
			scene.getChild(CandiConstants.LAYER_GENERAL).attachChild(mCameraTargetSprite);

			// Tie camera position to target position.
			mCamera.setChaseEntity(mCameraTargetSprite);

			// Render if dirty
			scene.registerUpdateHandler(new IUpdateHandler() {

				@Override
				public void onUpdate(float pSecondsElapsed) {

					// TODO: Implement a way to only render when needed for animations, movement, etc.
					mRenderSurfaceView.requestRender();
				}

				@Override
				public void reset() {

				}
			});

			// Scene touch handling
			scene.setOnSceneTouchListener(new IOnSceneTouchListener() {

				@Override
				public boolean onSceneTouchEvent(final Scene pScene, final TouchEvent pSceneTouchEvent) {

					// TouchEvent is world coordinates
					// MotionEvent is screen coordinates
					if (mFullUpdateInProgress || mIgnoreInput)
						return true;

					final float screenX = pSceneTouchEvent.getMotionEvent().getX();

					// Check for a fling or double tap using the gesture detector
					if (mGestureDetector.onTouchEvent(pSceneTouchEvent.getMotionEvent()))
						return true;

					if (pSceneTouchEvent.isActionDown()) {
						mLastMotionX = screenX;
						mCameraTargetSprite.clearEntityModifiers();
						return true;
					}

					if (pSceneTouchEvent.isActionUp()) {
						Utilities.Log(CandiConstants.APP_NAME, "Tricorder", "MoveEntityNearest: From Scene Touch");
						ZoneModel nearestZone = getNearestZone(mCameraTargetSprite.getX(), false);
						if (nearestZone != null) {
							mCandiPatchModel.setCandiModelFocused(nearestZone.getCandiesCurrent().get(0));
							mCameraTargetSprite.moveToZone(getNearestZone(mCameraTargetSprite.getX(), false), mDurationSlottingMinor,
									mEaseSlottingMinor);
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

		this.mEngine.registerUpdateHandler(new FPSLogger());
		this.mScene = scene;
		return scene;
	}

	@Override
	public void update(Observable observable, Object data) {
		Utilities.Log(CandiConstants.APP_NAME, "CandiPatchPresenter", "Update call from observable: " + observable.toString());
	}

	public void updateCandiData(List<EntityProxy> proxiEntities, boolean fullUpdate) {
		/*
		 * Primary entry point from the host activity. This is a primary trigger
		 * that should update the model and ripple to the views.
		 */
		IModel candiRootNext = mCandiPatchModel.getCandiRootCurrent();

		if (fullUpdate) {
			Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, "Performing full update...");
			mFullUpdateInProgress = true;
			mCandiViews.clear();
			mZoneViews.clear();
			clearCandiLayer();
			clearZoneLayer();
			mEngine.getScene().clearTouchAreas();
			mCandiPatchModel.reset();
			this.initialize();
			candiRootNext = new CandiModel(ModelType.Root, "root");
			candiRootNext.setRoot(true);
		}

		// Make sure each entity has a candi model
		for (EntityProxy entity : proxiEntities) {

			CandiModel candiModel = CandiModelBuilder.newCandiModel(ModelType.Entity, entity.id, entity);
			candiModel.setDisplayExtra(mDisplayExtras);

			if (mCandiPatchModel.containsCandiModel(candiModel)) {
				// Transfer data changes to the model we already have
				candiModel = mCandiPatchModel.updateCandiModel(candiModel);
				ensureCandiView(candiModel);
			}
			else {
				// Add model to parent (default zone is assigned)
				mCandiPatchModel.addCandiModel(candiModel);
				ensureCandiView(candiModel);
			}

			if (fullUpdate) {
				candiModel.setParent(candiRootNext);
				candiRootNext.getChildren().add(candiModel);
			}

			for (EntityProxy childEntity : entity.children) {
				CandiModel childCandiModel = CandiModelBuilder.newCandiModel(ModelType.Entity, childEntity.id, childEntity);
				childCandiModel.setDisplayExtra(mDisplayExtras);

				if (mCandiPatchModel.containsCandiModel(childCandiModel)) {
					childCandiModel = mCandiPatchModel.updateCandiModel(childCandiModel);
					ensureCandiView(childCandiModel);
				}
				else {
					// Nest the models
					mCandiPatchModel.addCandiModel(childCandiModel);
					ensureCandiView(childCandiModel);
				}
				if (fullUpdate) {
					candiModel.getChildren().add(childCandiModel);
					childCandiModel.setParent(candiModel);
				}
			}
		}

		// Stage the updated model tree
		mCandiPatchModel.setCandiRootNext(candiRootNext);

		// Navigate to make sure we are completely configured
		navigateModel(mCandiPatchModel.getCandiRootNext(), true);

		// Make sure we have a current candi and UI is centered on it
		if (fullUpdate) {
			mFullUpdateInProgress = false;
			if (mCandiPatchModel.getZonesOccupiedNextCount() > 0) {
				mCandiPatchModel.setCandiModelFocused(mCandiPatchModel.getZones().get(0).getCandiesNext().get(0));
				mCameraTargetSprite.moveToZone(mCandiPatchModel.getZones().get(0), 0.5f);
			}
		}
	}

	public void navigateModel(IModel candiRoot, boolean updateRootPrevious) {

		if (updateRootPrevious) {
			mCandiPatchModel.setCandiRootPrevious(mCandiPatchModel.getCandiRootCurrent());
		}
		mCandiPatchModel.setCandiRootNext(candiRoot);

		// Rebuild zone assignments
		mCandiPatchModel.updateZones();

		// Make sure zones have a view
		for (ZoneModel zone : mCandiPatchModel.getZones()) {
			ensureZoneView(zone);
		}

		// Reset camera boundaries
		setCameraBoundaries(mScene);

		// Handle transitions and animations
		doTransitions();

		// Trigger observer updates
		mCandiPatchModel.update();

		// Copy next to current across the model and child models
		mCandiPatchModel.shiftToNext();

		// Back button needs updating
		mCandiActivity.updateCandiBackButton();

	}

	private IView ensureCandiView(CandiModel candiModel) {

		if (!candiModel.getEntityProxy().isHidden && candiModel.countObservers() == 0) {

			Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, "Making CandiView: " + candiModel.getEntityProxy().label);

			// Create candi view for model
			CandiView candiView = CandiViewBuilder.createCandiView(candiModel, CandiPatchPresenter.this, new CandiView.OnCandiViewTouchListener() {

				@Override
				public void onCandiViewSingleTap(final IView candiView) {
					if (!mIgnoreInput) {
						Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, "CandiView single tap acted on");
						mIgnoreInput = true;
						doCandiViewSingleTap(candiView);
					}
				}

				@Override
				public void onCandiViewDoubleTap(IView candiView) {
					if (!mIgnoreInput) {
						mIgnoreInput = true;
						doCandiViewDoubleTap(candiView);
					}
				}

				@Override
				public void onCandiViewLongPress(IView candiView) {}
			});

			// Track in our collection
			mCandiViews.add(candiView);

			// Bind candi view to model as observer
			candiModel.addObserver(candiView);

			// Set initial view position
			candiView.setPosition(candiModel.getZoneCurrent().getX(), candiModel.getZoneCurrent().getY());

			// Add candi view to scene (starts out hidden using transparency).
			mEngine.getScene().getChild(CandiConstants.LAYER_CANDI).attachChild(candiView);

			return candiView;
		}

		return null;
	}

	private void doCandiViewSingleTap(IView candiView) {
		final CandiModel candiModel = (CandiModel) candiView.getModel();
		Utilities.Log(CandiConstants.APP_NAME, "CandiPatchPresenter", "SingleTap triggered: " + candiModel.getEntityProxy().label);

		mCandiPatchModel.setCandiModelFocused(candiModel);
		float distanceToMove = Math.abs(mCameraTargetSprite.getX() - candiModel.getZoneCurrent().getX());

		if (distanceToMove != 0) {

			mCameraTargetSprite.clearEntityModifiers();
			mCameraTargetSprite.moveToZone(candiModel.getZoneCurrent(), mDurationSlottingMajor, mEaseSlottingMajor, new MoveListener() {

				@Override
				public void onMoveFinished() {
					if (candiModel.hasSibling() && mCandiPatchModel.getCandiRootCurrent() != candiModel.getParent()) {
						mCandiActivity.runOnUiThread(new Runnable() {

							@Override
							public void run() {
								navigateModel(candiModel.getParent(), true);
								mIgnoreInput = false;
							}
						});
					}
					else {
						if (mCandiListener != null)
							mCandiListener.onSingleTap(candiModel);
					}
				}
			});
		}
		else {
			if (candiModel.hasSibling() && mCandiPatchModel.getCandiRootCurrent() != candiModel.getParent()) {
				navigateModel(candiModel.getParent(), true);
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
		Utilities.Log(CandiConstants.APP_NAME, "CandiPatchPresenter", "DoubleTap triggered: " + candiModel.getEntityProxy().label);

		float fromScale = mCameraTargetSprite.getScaleX();
		float toScale = 1;

		if (mCameraTargetSprite.getScaleX() == 1) {
			float fullWidth = mCamera.getSurfaceWidth() - (CandiConstants.CANDI_VIEW_ZOOMED_PADDING * 2);
			float fullHeight = mCamera.getSurfaceHeight() - (CandiConstants.CANDI_VIEW_ZOOMED_PADDING * 2);
			float toScaleWidth = fullWidth / CandiConstants.CANDI_VIEW_WIDTH;
			float toScaleHeight = fullHeight / CandiConstants.CANDI_VIEW_BODY_HEIGHT;
			toScale = toScaleWidth < toScaleHeight ? toScaleWidth : toScaleHeight;
		}

		mCameraTargetSprite.registerEntityModifier(new ParallelEntityModifier(new ScaleModifier(mDurationZoom, fromScale, toScale, EaseQuartInOut
				.getInstance())));

		mIgnoreInput = false;
	}

	private IView ensureZoneView(ZoneModel zoneModel) {

		if (zoneModel.isVisibleNext() && zoneModel.countObservers() == 0) {
			Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, "Making ZoneView: " + zoneModel.getTitleText());

			// Create view for model
			ZoneView zoneView = ZoneViewBuilder.createZoneView(zoneModel, CandiPatchPresenter.this);

			// Track in our collection
			mZoneViews.add(zoneView);

			// Bind view to model as observer
			zoneModel.addObserver(zoneView);

			// Set initial view position
			zoneView.setPosition(zoneModel.getX(), zoneModel.getY());

			// Add view to scene (starts out hidden using transparency).
			mEngine.getScene().getChild(CandiConstants.LAYER_ZONES).attachChild(zoneView);

			return zoneView;
		}

		return null;
	}

	private void doTransitions() {

		// Configure zones

		for (ZoneModel zoneModel : mCandiPatchModel.getZones()) {
			if (zoneModel.getCandiesNext().size() > 1 && zoneModel.getCandiModelPrimaryNext() != null)
				zoneModel.setTitleText(zoneModel.getCandiModelPrimaryNext().getTitleText());

			Transition transition = zoneModel.getTransition();
			if (transition == Transition.FadeIn)
				zoneModel.getModifiers().addLast(new AlphaModifier(1.0f, 0.0f, 1.0f));
			else if (transition == Transition.FadeOut)
				zoneModel.getModifiers().addLast(new AlphaModifier(1.0f, 1.0f, 0.0f));
		}

		// Configure candi

		for (ZoneModel zone : mCandiPatchModel.getZones()) {

			boolean needDelay = false;

			if (zone.isOccupiedCurrent()) {

				for (CandiModel candiModelCurrent : zone.getCandiesCurrent()) {
					if (candiModelCurrent.getModifiers().isEmpty()) {

						Transition transition = candiModelCurrent.getTransition();
						Position positionCurrent = candiModelCurrent.getZoneCurrent().getChildPositionCurrent(candiModelCurrent);
						Position positionNext = candiModelCurrent.getZoneNext().getChildPositionNext(candiModelCurrent);

						if (transition == Transition.FadeOut) {
							candiModelCurrent.getModifiers().addLast(new AlphaModifier(1.0f, 1.0f, 0.0f));
							needDelay = true;
						}
						else if (transition == Transition.Move) {

							candiModelCurrent.setBodyOnly(positionNext.scale != SCALE_NORMAL);
							if (positionCurrent.scale == positionNext.scale) {
								candiModelCurrent.getModifiers().addLast(
										new MoveModifier(1.0f, positionCurrent.x, positionNext.x, positionCurrent.y, positionNext.y, EaseQuartInOut
												.getInstance()));
							}
							else {
								candiModelCurrent.getModifiers().addLast(
										new ParallelEntityModifier(new ScaleModifier(1.0f, positionCurrent.scale, positionNext.scale, EaseQuartInOut
												.getInstance()), new MoveModifier(1.0f, positionCurrent.x, positionNext.x, positionCurrent.y,
												positionNext.y, EaseQuartInOut.getInstance())));
							}
							needDelay = true;
						}
						else if (transition == Transition.Shift) {
							/*
							 * A shift can mean a move within a zone and can mean expand/collapse
							 */
							if (positionCurrent.scale == positionNext.scale) {

								CandiView candiView = (CandiView) getViewForModel(candiModelCurrent);
								candiView.showReflection(positionNext.rowLast, true, 1);
								candiModelCurrent.getModifiers().addLast(
										new MoveModifier(1.0f, positionCurrent.x, positionNext.x, positionCurrent.y, positionNext.y, EaseQuartInOut
												.getInstance()));
							}
							else {
								candiModelCurrent.setBodyOnly(positionNext.scale != SCALE_NORMAL);
								candiModelCurrent.getModifiers().addLast(
										new ParallelEntityModifier(new ScaleModifier(1.0f, positionCurrent.scale, positionNext.scale, EaseQuartInOut
												.getInstance()), new MoveModifier(1.0f, positionCurrent.x, positionNext.x, positionCurrent.y,
												positionNext.y, EaseQuartInOut.getInstance())));
							}
						}
					}
				}
			}

			if (zone.isOccupiedNext()) {

				for (CandiModel candiModelNext : zone.getCandiesNext()) {
					if (candiModelNext.getModifiers().isEmpty()) {

						Transition transition = candiModelNext.getTransition();
						Position positionCurrent = candiModelNext.getZoneCurrent().getChildPositionCurrent(candiModelNext);
						Position positionNext = candiModelNext.getZoneNext().getChildPositionNext(candiModelNext);

						if (transition == Transition.FadeIn) {

							CandiView candiView = (CandiView) getViewForModel(candiModelNext);
							// candiModelNext.setBodyOnly(positionNext.scale != SCALE_NORMAL);
							if (positionNext.scale != SCALE_NORMAL) {
								// candiView.showCollapsed();
								candiModelNext.setBodyOnly(true);
							}
							else {
								// candiView.showExpanded();
								candiModelNext.setBodyOnly(false);
							}
							candiView.setScale(positionNext.scale);
							candiView.setPosition(positionNext.x, positionNext.y);

							if (needDelay)
								candiModelNext.getModifiers().addLast(new DelayModifier(0.5f));

							candiModelNext.getModifiers().addLast(new AlphaModifier(0.8f, 0.0f, 1.0f));
						}
						else if (transition == Transition.Move) {

							candiModelNext.setBodyOnly(positionNext.scale != SCALE_NORMAL);

							if (needDelay)
								candiModelNext.getModifiers().addLast(new DelayModifier(0.5f));

							if (positionCurrent.scale == positionNext.scale) {
								candiModelNext.getModifiers().addLast(
										new MoveModifier(1.0f, positionCurrent.x, positionNext.x, positionCurrent.y, positionNext.y, EaseQuartInOut
												.getInstance()));
							}
							else {
								candiModelNext.getModifiers().addLast(
										new ParallelEntityModifier(new ScaleModifier(1.0f, positionCurrent.scale, positionNext.scale, EaseQuartInOut
												.getInstance()), new MoveModifier(1.0f, positionCurrent.x, positionNext.x, positionCurrent.y,
												positionNext.y, EaseQuartInOut.getInstance())));
							}
						}
					}
				}
			}
		}
		// If current focus is going away, move camera target to another entity before other moves/shows happen.
		if (mCandiPatchModel.getCandiModelFocused() != null && mCandiPatchModel.getCandiModelFocused().getZoneCurrent().getZoneIndex() + 1 > mCandiPatchModel
					.getZonesOccupiedNextCount()) {
			ZoneModel zoneModel = getZoneContainsCandiNext(mCandiPatchModel.getCandiModelFocused());
			if (zoneModel != null)
				mCameraTargetSprite.moveToZone(zoneModel, mDurationSlottingMajor, mEaseSlottingMajor);
			else
				mCameraTargetSprite.moveToZone(getNearestZone(mCameraTargetSprite.getX(), true), mDurationSlottingMajor, mEaseSlottingMajor);
		}
	}

	private void clearCandiLayer() {
		IEntity layer = mEngine.getScene().getChild(CandiConstants.LAYER_CANDI);
		int childCount = layer.getChildCount();
		for (int i = childCount - 1; i >= 0; i--) {
			IEntity child = layer.getChild(i);
			if (child instanceof CandiView) {
				// TODO: Should we null this so the GC can collect them.
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
				// TODO: Should we null this so the GC can collect them.
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

	public void resetTextures(TextureReset textureReset) {
		// Candi views
		for (final CandiView candiView : mCandiViews) {
			if (textureReset == TextureReset.VisibleOnly && !candiView.isVisibleToCamera(this.mCamera))
				continue;
			else if (textureReset == TextureReset.NonVisibleOnly && candiView.isVisibleToCamera(this.mCamera))
				continue;
			candiView.resetTextures();
		}

		// Zone views
		for (final ZoneView zoneView : mZoneViews) {
			if (textureReset == TextureReset.VisibleOnly && !zoneView.isVisibleToCamera(this.mCamera))
				continue;
			else if (textureReset == TextureReset.NonVisibleOnly && zoneView.isVisibleToCamera(this.mCamera))
				continue;
			zoneView.resetTextures();
		}
	}

	public void resetSharedTextures() {
		mTexture.clearTextureSources();
		loadTextureSources();
	}

	public void loadTextures() {
		mTexture = new Texture(512, 512, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		mEngine.getTextureManager().loadTexture(mTexture);
	}

	public void loadTextureSources() {
		// Push generic candi view body texture into the image cache
		Bitmap bitmap = ImageManager.loadBitmapFromAssets("gfx/generic10.png");
		ImageManager.getInstance().getImageCache().put("tile_untagged.png", bitmap);

		// Textures that are shared by zone views
		Bitmap bitmapZoneBody = ImageManager.loadBitmapFromAssets("gfx/trans_30.png");
		if (bitmapZoneBody != null) {
			mZoneBodyTextureRegion = TextureRegionFactory.createFromSource(mTexture, new BitmapTextureSource(bitmapZoneBody), 0, 0);
			Bitmap reflectionBitmap = AircandiUI.getReflection(bitmapZoneBody);
			mZoneReflectionTextureRegion = TextureRegionFactory.createFromSource(mTexture, new BitmapTextureSource(reflectionBitmap), 0, 256);
		}

		// Scene progress sprite
		mPlaceholderTextureRegion = TextureRegionFactory.createFromAsset(mTexture, mContext, "gfx/trans_30.png", 256, 0);
		mProgressTextureRegion = TextureRegionFactory.createTiledFromAsset(mTexture, mContext, "gfx/busyspritesIV.png", 256, 256, 4, 2);
	}

	private ZoneModel getNearestZone(float nearestToX, boolean requireOccupiedNext) {
		if (mCandiPatchModel.getZonesOccupiedNextCount() == 0)
			return null;

		int nearestIndex = 0;
		float smallestDistance = 999999;

		for (ZoneModel zone : mCandiPatchModel.getZones()) {
			if (requireOccupiedNext && !zone.isOccupiedNext())
				continue;
			if (zone.getCandiesCurrent().size() > 0) {
				float distance = Math.abs(zone.getX() - nearestToX);
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

	private void setCameraBoundaries(Scene scene) {
		// float[] coordinates = scene.convertLocalToSceneCoordinates(mCandiPatchModel.getOriginX(),
		// mCandiPatchModel.getOriginY());
		// mBoundsMinX = coordinates[CandiConstants.VERTEX_INDEX_X];
		mBoundsMinX = 125;
		mBoundsMaxX = (mBoundsMinX + (mCandiPatchModel.getZonesOccupiedNextCount() - 1) * (CandiConstants.CANDI_VIEW_WIDTH + CandiConstants.CANDI_VIEW_SPACING));
	}

	public void setFrustum(GL10 pGL) {

		// Set field of view to 60 degrees
		float fov_degrees = 60;
		float fov_radians = fov_degrees / 180 * (float) Math.PI;

		// Set aspect ratio and distance of the screen
		float aspect = mCamera.getWidth() / mCamera.getHeight();
		float camZ = mCamera.getHeight() / 2 / (float) Math.tan(fov_radians / 2);

		// Set projection
		GLHelper.setProjectionIdentityMatrix(pGL);
		GLU.gluPerspective(pGL, fov_degrees, aspect, camZ / 10, camZ * 10);

		/*
		 * The logic here positions the viewer but does not position the camera.
		 * That means we could be looking at slot #5 but the camera is still sitting
		 * on slot #2. Touch areas track with the camera so they will be messed up. So
		 * we still set the camera target sprite as the chase entity for the
		 * camera.
		 * :
		 * Because the y axis is reversed, we need to subtract to move down.
		 */

		final float[] centerCoordinates = mCameraTargetSprite.getSceneCenterCoordinates();
		float targetX = centerCoordinates[VERTEX_INDEX_X];
		float targetY = -centerCoordinates[VERTEX_INDEX_Y];

		GLU.gluLookAt(pGL, targetX, targetY, camZ, targetX, targetY, 0, 0, 1, 0); // move camera back
		pGL.glScalef(1, -1, 1); // reverse y-axis
		pGL.glTranslatef(0f, 0, 0); // origin at top left
	}

	public void setCameraTarget(CameraTargetSprite mCameraTarget) {
		this.mCameraTargetSprite = mCameraTarget;
	}

	public void setCandiListener(ICandiListener listener) {
		mCandiListener = listener;
	}

	public Engine getEngine() {
		return this.mEngine;
	}

	public BaseView getViewForModel(IModel candiModel) {
		for (CandiView candiView : mCandiViews)
			if (candiView.getModel() == candiModel)
				return candiView;
		return null;
	}

	public boolean isFullUpdateInProgress() {
		return this.mFullUpdateInProgress;
	}

	public void setFullUpdateInProgress(boolean fullUpdateInProgress) {
		this.mFullUpdateInProgress = fullUpdateInProgress;
	}

	public void setScene(Scene scene) {
		this.mScene = scene;
	}

	public Scene getScene() {
		return mScene;
	}

	
	public boolean isIgnoreInput() {
		return this.mIgnoreInput;
	}

	
	public void setIgnoreInput(boolean ignoreInput) {
		this.mIgnoreInput = ignoreInput;
	}

	public interface ICandiListener {

		void onSelected(IModel candi);

		void onSingleTap(CandiModel candi);
	}

	public interface IMoveListener {

		void onMoveFinished();
	}

	class CandiPatchSurfaceGestureDetector extends SimpleOnGestureListener {

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			Utilities.Log(CandiConstants.APP_NAME, "CandiPatchPresenter", "Detected double tap");
			return false;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			/*
			 * This gets called because the gesture detector thinks its has a fling gesture
			 */

			// Test for swipe that is too vertical to trigger a fling
			if (Math.abs(e1.getY() - e2.getY()) > CandiConstants.SWIPE_MAX_OFF_PATH) {
				return false;
			}

			// Check to see if we are at a boundary
			float cameraTargetX = mCameraTargetSprite.getX() + 125;
			if (cameraTargetX <= mBoundsMinX || cameraTargetX >= mBoundsMaxX) {
				mCameraTargetSprite.moveToZone(getNearestZone(mCameraTargetSprite.getX(), false), mDurationBounceBack, mEaseBounceBack);
				return false;
			}

			// The velocity units are in pixels per second.
			final float distanceTimeFactor = 0.8f;
			final float totalDx = (distanceTimeFactor * velocityX / 2);
			boolean smallFling = (Math.abs(totalDx) <= CandiConstants.SWIPE_SMALL_FLING);

			Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, "Fling distance: " + String.valueOf(Math.abs(totalDx)));
			Utilities.Log(CandiConstants.APP_NAME, COMPONENT_NAME, "Fling velocity: " + String.valueOf(velocityX));

			// Cap the distance we travel so we don't race past our boundaries
			float targetX = mCameraTargetSprite.getX() - totalDx;

			if (smallFling) {
				ZoneModel targetZoneModel = mCandiPatchModel.getCandiModelFocused().getZoneCurrent();
				ZoneModel nextZoneModel = mCandiPatchModel.getZoneNeighbor(targetZoneModel, totalDx < 0 ? true : false);
				if (nextZoneModel != null)
					targetX = mCameraTargetSprite.getX() - nextZoneModel.getX();
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
					mCameraTargetSprite.moveToZone(getNearestZone(mCameraTargetSprite.getX(), false), mDurationSlottingMinor, mEaseSlottingMinor);
				}

				@Override
				public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {}

			}, mEaseFling));

			return true;
		}
	}

}
