package com.proxibase.aircandi.candi.presenters;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.microedition.khronos.opengles.GL10;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.engine.options.EngineOptions.ScreenOrientation;
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

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLU;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.GestureDetector.SimpleOnGestureListener;

import com.proxibase.aircandi.candi.models.CandiModel;
import com.proxibase.aircandi.candi.models.CandiModelBuilder;
import com.proxibase.aircandi.candi.models.CandiPatchModel;
import com.proxibase.aircandi.candi.models.IModel;
import com.proxibase.aircandi.candi.models.ZoneModel;
import com.proxibase.aircandi.candi.models.CandiModel.DisplayExtra;
import com.proxibase.aircandi.candi.models.CandiModel.Transition;
import com.proxibase.aircandi.candi.models.ZoneModel.Position;
import com.proxibase.aircandi.candi.sprites.CameraTargetSprite;
import com.proxibase.aircandi.candi.sprites.CandiAnimatedSprite;
import com.proxibase.aircandi.candi.sprites.CandiScene;
import com.proxibase.aircandi.candi.utils.CandiConstants;
import com.proxibase.aircandi.candi.views.BaseView;
import com.proxibase.aircandi.candi.views.CandiView;
import com.proxibase.aircandi.candi.views.CandiViewBuilder;
import com.proxibase.aircandi.candi.views.IView;
import com.proxibase.aircandi.candi.views.ZoneView;
import com.proxibase.aircandi.candi.views.ZoneViewBuilder;
import com.proxibase.aircandi.controllers.Aircandi;
import com.proxibase.aircandi.controllers.CandiSearchActivity;
import com.proxibase.aircandi.utils.BitmapTextureSource;
import com.proxibase.aircandi.utils.ImageManager;
import com.proxibase.aircandi.utils.Utilities;
import com.proxibase.sdk.android.proxi.consumer.ProxiEntity;
import com.proxibase.sdk.android.util.UtilitiesUI;

public class CandiPatchPresenter implements Observer {

	public static enum TextureReset {
		All, VisibleOnly, NonVisibleOnly
	}

	private static String		COMPONENT_NAME			= "CandiPatchPresenter";

	public CandiPatchModel		mCandiPatchModel;
	private List<CandiView>		mCandiViews				= new ArrayList<CandiView>();
	private List<ZoneView>		mZoneViews				= new ArrayList<ZoneView>();

	public int					mTouchSlopSquare;
	public int					mDoubleTapSlopSquare;

	private GestureDetector		mGestureDetector;
	public boolean				mIgnoreInput			= false;
	private Context				mContext;
	private Engine				mEngine;
	public CandiAnimatedSprite	mProgressSprite;
	private float				mLastMotionX;
	private CameraTargetSprite	mCameraTargetSprite;
	private float				mBoundsMinX;
	private float				mBoundsMaxX;
	private Camera				mCamera;

	public Texture				mTexture;

	public TiledTextureRegion	mProgressTextureRegion;
	public TextureRegion		mPlaceholderTextureRegion;
	public TextureRegion		mZoneBodyTextureRegion;
	public TextureRegion		mZoneReflectionTextureRegion;

	public CandiSearchActivity	mCandiActivity;
	private RenderSurfaceView	mRenderSurfaceView;
	public DisplayExtra			mDisplayExtras;
	public boolean				mFullUpdateInProgress	= false;

	private ICandiListener		mCandiListener;
	protected float				mTouchStartX;
	protected float				mTouchStartY;
	protected boolean			mTouchGrabbed;

	public CandiPatchPresenter(Context context, CandiSearchActivity activity, Engine engine, RenderSurfaceView renderSurfaceView,
			CandiPatchModel candiPatchModel) {

		this.mCandiPatchModel = candiPatchModel;
		this.mContext = context;
		this.mEngine = engine;
		this.mCamera = engine.getCamera();
		this.mRenderSurfaceView = renderSurfaceView;
		this.mCandiActivity = activity;

		init();
	}

	private void init() {

		// Gestures
		mGestureDetector = new GestureDetector(mContext, new CandiPatchSurfaceGestureDetector());
		final ViewConfiguration configuration = ViewConfiguration.get(mContext);

		int touchSlop = configuration.getScaledTouchSlop();
		int doubleTapSlop = configuration.getScaledDoubleTapSlop();

		mTouchSlopSquare = (touchSlop * touchSlop) / 4;
		mDoubleTapSlopSquare = doubleTapSlop * doubleTapSlop;

		// Origin
		mCandiPatchModel.setOriginX((int) ((this.mCamera.getWidth() - CandiConstants.CANDI_VIEW_WIDTH) * 0.5f));
		mCandiPatchModel.setOriginY((int) ((this.mCamera.getHeight() - CandiConstants.CANDI_VIEW_HEIGHT) * 0.5f) - 25);

		// Zone for inactive candi
		mCandiPatchModel.setZoneInactive(new ZoneModel(0, mCandiPatchModel));
		mCandiPatchModel.getZoneInactive().setInactive(true);

		// Textures
		loadTextures();
		loadTextureSources();
	}

	public Scene initScene() {
		Utilities.Log(Aircandi.APP_NAME, COMPONENT_NAME, "initScene called");

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
			final int centerX = (int) ((this.mCamera.getWidth() - mProgressTextureRegion.getTileWidth()) / 2);
			final int centerY = (int) ((this.mCamera.getHeight() - mProgressTextureRegion.getTileHeight()) / 2);

			// Progress sprite
			mProgressSprite = new CandiAnimatedSprite(0, 0, mProgressTextureRegion);
			mProgressSprite.setPosition(centerX, centerY);
			mProgressSprite.animate(150, true);
			mProgressSprite.setVisible(false);
			scene.getChild(CandiConstants.LAYER_GENERAL).attachChild(mProgressSprite);

			// Invisible entity used to scroll
			final int cameraTargetCenterX = (int) ((this.mCamera.getWidth() - CandiConstants.CANDI_VIEW_WIDTH) / 2);
			final int cameraTargetCenterY = (int) ((this.mCamera.getHeight() - CandiConstants.CANDI_VIEW_HEIGHT) / 2);
			mCameraTargetSprite = new CameraTargetSprite(cameraTargetCenterX, cameraTargetCenterY, CandiConstants.CANDI_VIEW_WIDTH,
					CandiConstants.CANDI_VIEW_HEIGHT);
			mCameraTargetSprite.setScaleCenter(cameraTargetCenterX, cameraTargetCenterY);
			mCameraTargetSprite.setColor(0, 0, 0, 0f);
			mCameraTargetSprite.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
			mCameraTargetSprite.setVisible(false);
			scene.getChild(CandiConstants.LAYER_GENERAL).attachChild(mCameraTargetSprite);

			// Jayma: hack until I can figure out a better way to do this. This will only work
			// for a device that has an 800x480 screen.
			if (mCandiActivity.getScreenOrientation() == ScreenOrientation.LANDSCAPE)
				scene.setPosition(160, 0);

			// Tie camera position to target position
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
					if (mFullUpdateInProgress)
						return true;

					final float screenX = pSceneTouchEvent.getMotionEvent().getX();

					// We are only detecting flings right now
					if (mGestureDetector.onTouchEvent(pSceneTouchEvent.getMotionEvent()))
						return true;

					if (pSceneTouchEvent.isActionDown()) {
						mLastMotionX = screenX;
						mCameraTargetSprite.clearEntityModifiers();
						return true;
					}

					if (pSceneTouchEvent.isActionUp()) {
						Utilities.Log(Aircandi.APP_NAME, "Tricorder", "MoveEntityNearest: From Scene Touch");
						ZoneModel nearestZone = getNearestZone(mCameraTargetSprite.getX());
						if (nearestZone != null) {
							mCandiPatchModel.setCandiModelFocused(nearestZone.getCandiesCurrent().get(0));
							mCameraTargetSprite.moveToZone(getNearestZone(mCameraTargetSprite.getX()), 0.5f);
						}
						return true;
					}

					if (pSceneTouchEvent.isActionMove()) {

						float scrollX = mLastMotionX - screenX;
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
		return scene;
	}

	@Override
	public void update(Observable observable, Object data) {
		Utilities.Log(Aircandi.APP_NAME, "CandiPatchPresenter", "Update call from observable: " + observable.toString());
	}

	public void updateCandiData(List<ProxiEntity> proxiEntities, boolean fullUpdate) {
		/*
		 * Primary entry point from the host activity. This is a primary trigger
		 * that should update the model and ripple to the views.
		 */
		if (fullUpdate) {
			Utilities.Log(Aircandi.APP_NAME, COMPONENT_NAME, "Performing full update...");
			mFullUpdateInProgress = true;
			mCandiViews.clear();
			mZoneViews.clear();
			clearCandiLayer();
			clearZoneLayer();
			mEngine.getScene().clearTouchAreas();
			mCandiPatchModel.reset();
			this.init();
		}

		for (ProxiEntity proxiEntity : proxiEntities) {

			CandiModel candiModel = CandiModelBuilder.createCandiModel(proxiEntity);
			candiModel.setDisplayExtra(mDisplayExtras);

			if (mCandiPatchModel.containsCandiModel(candiModel)) {
				mCandiPatchModel.updateCandiModel(candiModel);
				candiModel = mCandiPatchModel.getCandiModels().getByKey(candiModel.getProxiEntity().entityId);
				ensureCandiView(candiModel);
			}
			else {
				// Add model to parent (default zone is assigned)
				mCandiPatchModel.addCandiModel(candiModel);
				ensureCandiView(candiModel);
			}
		}

		// Rebuild tree
		CandiModel rootPrevious = (CandiModel) mCandiPatchModel.getCandiRootCurrent();
		mCandiPatchModel.buildCandiTree();
		if (!fullUpdate && rootPrevious != null && !rootPrevious.isSuperRoot()) {
			for (CandiModel candiModel : mCandiPatchModel.getCandiRootNext().getChildren())
				if (candiModel.getProxiEntity().entityType.equals(rootPrevious.getProxiEntity().entityType)) {
					navigateModel(candiModel, false);
				}
		}
		else {
			navigateModel(mCandiPatchModel.getCandiRootNext(), true);
			if (fullUpdate) {
				mFullUpdateInProgress = false;
				if (mCandiPatchModel.getZonesOccupiedNextCount() > 0) {
					mCandiPatchModel.setCandiModelFocused(mCandiPatchModel.getZones().get(0).getCandiesNext().get(0));
					mCameraTargetSprite.moveToZone(mCandiPatchModel.getZones().get(0), 0.5f);
				}
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
		setCameraBoundaries();

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

		if (!candiModel.getProxiEntity().isHidden && candiModel.countObservers() == 0) {

			Utilities.Log(Aircandi.APP_NAME, COMPONENT_NAME, "Making CandiView: " + candiModel.getProxiEntity().label);
			// Create view for model
			CandiView candiView = CandiViewBuilder.createCandiView(candiModel, CandiPatchPresenter.this,
					new CandiView.OnCandiViewSingleTapListener() {

						@Override
						public void onCandiViewSingleTap(IView candiView) {

							if (!mIgnoreInput) {
								if (((CandiModel) candiView.getModel()).isGrouped() && mCandiPatchModel.getCandiRootCurrent() != candiView.getModel()
											.getParent()) {
									mCandiPatchModel.setCandiModelFocused((CandiModel) candiView.getModel());
									float distanceMoved = mCameraTargetSprite.moveToZone(((CandiModel) candiView.getModel()).getZoneCurrent(), 1.0f);
									if (distanceMoved != 0) {
										try {
											Thread.sleep(500);
										}
										catch (InterruptedException exception) {
											// TODO Auto-generated catch block
											exception.printStackTrace();
										}
									}
									navigateModel(candiView.getModel().getParent(), true);
								}
								else {
									mCandiPatchModel.setCandiModelFocused((CandiModel) candiView.getModel());
									mCameraTargetSprite.moveToZone(((CandiModel) candiView.getModel()).getZoneCurrent(), 0.5f);
									if (mCandiListener != null)
										mCandiListener.onSingleTap((CandiModel) candiView.getModel());
								}
							}
						}

					});

			// Track in our collection
			mCandiViews.add(candiView);

			// Bind view to model as observer
			candiModel.addObserver(candiView);

			// Set initial view position
			candiView.setPosition(candiModel.getZoneCurrent().getX(), candiModel.getZoneCurrent().getY());

			// Add view to scene (starts out hidden using transparency).
			mEngine.getScene().getChild(CandiConstants.LAYER_CANDI).attachChild(candiView);

			return candiView;
		}

		return null;
	}

	private IView ensureZoneView(ZoneModel zoneModel) {

		if (zoneModel.isVisibleNext() && zoneModel.countObservers() == 0) {
			Utilities.Log(Aircandi.APP_NAME, COMPONENT_NAME, "Making ZoneView: " + zoneModel.getTitleText());

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
			if (zoneModel.getCandiesNext().size() > 1)
				zoneModel.setTitleText(zoneModel.getCandiesNext().get(0).getProxiEntity().entityTypeLabel);

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

						candiModelCurrent.setBodyOnly(positionNext.scale != 1);

						if (transition == Transition.FadeOut) {
							Utilities.Log(Aircandi.APP_NAME, "CandiPatchPresenter", "FadeOut " + candiModelCurrent.getTitleText());
							candiModelCurrent.getModifiers().addLast(new AlphaModifier(1.0f, 1.0f, 0.0f));
							needDelay = true;
						}
						else if (transition == Transition.Move) {

							Utilities.Log(Aircandi.APP_NAME, "CandiPatchPresenter", "Move " + candiModelCurrent.getTitleText()
																					+ " from: "
																					+ String.valueOf(positionCurrent.x)
																					+ ","
																					+ String.valueOf(positionCurrent.y)
																					+ " to "
																					+ String.valueOf(positionNext.x)
																					+ ","
																					+ String.valueOf(positionNext.y));

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

							Utilities.Log(Aircandi.APP_NAME, "CandiPatchPresenter", "Shift " + candiModelCurrent.getTitleText()
																					+ " from: "
																					+ String.valueOf(positionCurrent.x)
																					+ ","
																					+ String.valueOf(positionCurrent.y)
																					+ " to "
																					+ String.valueOf(positionNext.x)
																					+ ","
																					+ String.valueOf(positionNext.y));

							// Not moving but might need to rearrange within the zone
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
							Utilities.Log(Aircandi.APP_NAME, "CandiPatchPresenter", "FadeIn " + candiModelNext.getTitleText());

							CandiView candiView = (CandiView) getViewForModel(candiModelNext);
							candiModelNext.setBodyOnly(positionNext.scale != 1);
							candiView.showBodyOnly(positionNext.scale != 1, false, 0);
							candiView.setScale(positionNext.scale);
							candiView.setPosition(positionNext.x, positionNext.y);

							if (needDelay)
								candiModelNext.getModifiers().addLast(new DelayModifier(0.5f));

							candiModelNext.getModifiers().addLast(new AlphaModifier(0.8f, 0.0f, 1.0f));
						}
						else if (transition == Transition.Move) {

							candiModelNext.setBodyOnly(positionNext.scale != 1);

							Utilities.Log(Aircandi.APP_NAME, "CandiPatchPresenter", "Move " + candiModelNext.getTitleText()
																					+ " to: "
																					+ String.valueOf(positionNext.x)
																					+ ","
																					+ String.valueOf(positionNext.y)
																					+ "from "
																					+ String.valueOf(positionCurrent.x)
																					+ ","
																					+ String.valueOf(positionCurrent.y));

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
		// If current focus is going away, move camera target to
		// another entity before other moves/shows happen.
		// if (mCameraTarget.getTargetCandiView() != null && !mCameraTarget.getTargetEntity().isVisibleNext()) {
		// mCameraTarget.moveToZone(getNearestZone(mCameraTarget.getTargetCandiView().getX()), null);
		// }

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
		ImageManager.getImageManager().getImageCache().put("tile_untagged.png", bitmap);

		// Textures that are shared by zone views
		Bitmap bitmapZoneBody = ImageManager.loadBitmapFromAssets("gfx/zone_orange_30.png");
		if (bitmapZoneBody != null) {
			mZoneBodyTextureRegion = TextureRegionFactory.createFromSource(mTexture, new BitmapTextureSource(bitmapZoneBody), 0, 0);
			Bitmap reflectionBitmap = UtilitiesUI.getReflection(bitmapZoneBody);
			mZoneReflectionTextureRegion = TextureRegionFactory.createFromSource(mTexture, new BitmapTextureSource(reflectionBitmap), 0, 256);
		}

		// Scene progress sprite
		mPlaceholderTextureRegion = TextureRegionFactory.createFromAsset(mTexture, mContext, "gfx/trans_30.png", 256, 0);
		mProgressTextureRegion = TextureRegionFactory.createTiledFromAsset(mTexture, mContext, "gfx/busyspritesIV.png", 256, 256, 4, 2);
	}

	private ZoneModel getNearestZone(float nearestToX) {
		if (mCandiPatchModel.getZonesOccupiedNextCount() == 0)
			return null;

		int nearestIndex = 0;
		float smallestDistance = 999999;

		for (ZoneModel zone : mCandiPatchModel.getZones()) {
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

	private void setCameraBoundaries() {
		float sceneScale = mEngine.getScene().getChild(CandiConstants.LAYER_CANDI).getScaleX();
		mBoundsMinX = mCandiPatchModel.getOriginX() * sceneScale;
		mBoundsMaxX = (mCandiPatchModel.getOriginX() + ((mCandiPatchModel.getZonesOccupiedNextCount() - 1) * (CandiConstants.CANDI_VIEW_WIDTH + CandiConstants.CANDI_VIEW_SPACING))) * sceneScale;
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

		// Set view
		// TODO: Hack: Adding 10 to correctly center in the view but doubt
		// this will hold up with other devices/resolutions/sizing.
		float targetX = mCameraTargetSprite.getX() - (mCameraTargetSprite.getWidth() * 0.5f);
		GLU.gluLookAt(pGL, targetX + 10, 0, camZ, targetX + 10, 0, 0, 0, 1, 0); // move camera back
		pGL.glScalef(1, -1, 1); // reverse y-axis
		pGL.glTranslatef(-mCamera.getWidth() / 2, -mCamera.getHeight() / 2, 0); // origin at top left
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

	private BaseView getViewForModel(IModel candiModel) {
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

	public interface ICandiListener {

		void onSelected(IModel candi);

		void onSingleTap(CandiModel candi);
	}

	public interface IMoveListener {

		void onMoveFinished();
	}

	class CandiPatchSurfaceGestureDetector extends SimpleOnGestureListener {

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

			// Test for swipe that is too vertical to trigger a fling
			if (Math.abs(e1.getY() - e2.getY()) > CandiConstants.SWIPE_MAX_OFF_PATH) {
				return false;
			}

			// Check to see if we are at a boundary
			float cameraTargetX = mCameraTargetSprite.getX();
			if (cameraTargetX <= mBoundsMinX || cameraTargetX >= mBoundsMaxX) {
				mCameraTargetSprite.moveToZone(getNearestZone(mCameraTargetSprite.getX()), 0.5f);
				return false;
			}

			// The velocity units are in pixels per second.
			final float distanceTimeFactor = 0.8f;
			final float totalDx = (distanceTimeFactor * velocityX / 2);

			// Cap the distance we travel so we don't race past our boundaries
			float targetX = mCameraTargetSprite.getX() - totalDx;
			if (targetX > mBoundsMaxX) {
				targetX = mBoundsMaxX + 150;
			}
			else if (targetX < mBoundsMinX) {
				targetX = mBoundsMinX - 150;
			}
			mCameraTargetSprite.registerEntityModifier(new MoveModifier(distanceTimeFactor, mCameraTargetSprite.getX(), targetX, mCameraTargetSprite
					.getY(), mCameraTargetSprite.getY(), new IEntityModifierListener() {

				@Override
				public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
					mCandiPatchModel.setCandiModelFocused(getNearestZone(mCameraTargetSprite.getX()).getCandiesCurrent().get(0));
					mCameraTargetSprite.moveToZone(getNearestZone(mCameraTargetSprite.getX()), 0.5f);
				}

				@Override
				public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {
				// TODO Auto-generated method stub

				}

			}, EaseExponentialOut.getInstance()));

			return true;
		}
	}

}
