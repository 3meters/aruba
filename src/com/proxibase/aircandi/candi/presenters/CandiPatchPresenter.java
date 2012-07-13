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
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import com.proxibase.aircandi.Aircandi;
import com.proxibase.aircandi.CandiRadar;
import com.proxibase.aircandi.Preferences;
import com.proxibase.aircandi.R;
import com.proxibase.aircandi.candi.camera.ChaseCamera;
import com.proxibase.aircandi.candi.models.BaseModel.ViewState;
import com.proxibase.aircandi.candi.models.CandiModel;
import com.proxibase.aircandi.candi.models.CandiModel.ReasonInactive;
import com.proxibase.aircandi.candi.models.CandiModel.Transition;
import com.proxibase.aircandi.candi.models.CandiModelFactory;
import com.proxibase.aircandi.candi.models.CandiPatchModel;
import com.proxibase.aircandi.candi.models.CandiPatchModel.Navigation;
import com.proxibase.aircandi.candi.models.IModel;
import com.proxibase.aircandi.candi.models.ZoneModel;
import com.proxibase.aircandi.candi.models.ZoneModel.ZoneStatus;
import com.proxibase.aircandi.candi.modifiers.CandiAlphaModifier;
import com.proxibase.aircandi.candi.sprites.CameraTargetSprite;
import com.proxibase.aircandi.candi.sprites.CameraTargetSprite.MoveListener;
import com.proxibase.aircandi.candi.sprites.CandiAnimatedSprite;
import com.proxibase.aircandi.candi.sprites.CandiScene;
import com.proxibase.aircandi.candi.views.CandiView;
import com.proxibase.aircandi.candi.views.IView;
import com.proxibase.aircandi.candi.views.IView.ViewTouchListener;
import com.proxibase.aircandi.candi.views.ViewAction;
import com.proxibase.aircandi.candi.views.ViewAction.ViewActionType;
import com.proxibase.aircandi.candi.views.ZoneView;
import com.proxibase.aircandi.components.BitmapTextureSource;
import com.proxibase.aircandi.components.BitmapTextureSource.IBitmapAdapter;
import com.proxibase.aircandi.components.CandiList;
import com.proxibase.aircandi.components.CommandType;
import com.proxibase.aircandi.components.CountDownTimer;
import com.proxibase.aircandi.components.DateUtils;
import com.proxibase.aircandi.components.ImageManager;
import com.proxibase.aircandi.components.ImageRequestBuilder;
import com.proxibase.aircandi.components.ImageUtils;
import com.proxibase.aircandi.components.Logger;
import com.proxibase.aircandi.components.ProxiExplorer.EntityModel;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.service.objects.Entity;

public class CandiPatchPresenter implements Observer {

	public static float				SCALE_NORMAL			= 1;

	public CandiPatchModel			mCandiPatchModel;
	private HashMap					mCandiViewsActiveHash	= new HashMap();
	public List<ZoneView>			mZoneViews				= new ArrayList<ZoneView>();
	private CandiViewPool			mCandiViewPool;
	private EntityModel				mEntityModelSnapshot;

	private GestureDetector			mGestureDetector;
	public boolean					mIgnoreInput			= false;
	private Context					mContext;
	private Engine					mEngine;
	public CandiAnimatedSprite		mProgressSprite;
	public Rectangle				mHighlight;
	private float					mLastMotionX;
	private CameraTargetSprite		mCameraTargetSprite;
	private float					mBoundsMinX;
	private float					mBoundsMaxX;
	private ChaseCamera				mCamera;
	private Scene					mScene;

	public Texture					mTexture;

	public TextureRegion			mPlaceholderTextureRegion;
	public TextureRegion			mZoneBodyTextureRegion;
	public TextureRegion			mZoneReflectionTextureRegion;

	public CandiRadar				mCandiRadarActivity;
	public RenderSurfaceView		mRenderSurfaceView;
	private ManageViewsThread		mManageViewsThread;
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
		mCandiRadarActivity = (CandiRadar) activity;
		mGestureDetector = new GestureDetector(mContext, new SingleTapGestureDetector());

		/* Rendering timer */
		mRenderingTimer = new RenderCountDownTimer(5000, 500);

		/* Textures */
		loadHardwareTextures();

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

		/* Origin */
		mCandiPatchModel.setOriginX(0);
		mCandiPatchModel.setOriginY(0);

		/* Resource references per theme */
		loadStyles();

		loadTextureSources();
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

		{
			/* Highlight */
			mHighlight = new Rectangle(0, 0, 258, 258);
			mHighlight.setColor(1.0f, 0.7f, 0f, 1.0f);
			mHighlight.setVisible(false);
			scene.getChild(CandiConstants.LAYER_GENERAL).attachChild(mHighlight);

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
					 * TouchEvent is world coordinates MotionEvent is screen coordinates
					 */
					renderingActivate();
					if (mFullUpdateInProgress || mIgnoreInput)
						return true;

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

					if (pSceneTouchEvent.isActionUp()) {
						ZoneModel nearestZone = getNearestZone(mCameraTargetSprite.getX(), false);
						if (nearestZone != null) {
							mCandiPatchModel.setCandiModelFocused(nearestZone.getCandiesCurrent().get(0));
							mCameraTargetSprite.moveToZone(getNearestZone(mCameraTargetSprite.getX(), false), CandiConstants.DURATION_SLOTTING_MINOR,
									CandiConstants.EASE_SLOTTING_MINOR, new MoveListener() {

										@Override
										public void onMoveFinished() {
											manageViewsAsync();
										}

										@Override
										public void onMoveStarted() {}
									});

						}
						return true;
					}

					if (pSceneTouchEvent.isActionMove()) {
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

	public void updateCandiData(EntityModel entityModel, boolean fullBuild, boolean chunking, boolean delayObserverUpdate) {
		/*
		 * Push the new and updated entities into the system. Updates all the models and views.
		 * We create a snapshot to protect from any asynch changes to the entity model while we are
		 * updating the dependent candi models and views.
		 * 
		 * TODO: Clone is not creating copies of the child entities.
		 */
		mEntityModelSnapshot = entityModel.copy();

		if (chunking) {
			/*
			 * None should be considered rookies and we need to synchronize the discoveryTime.
			 */
			for (Entity entity : mEntityModelSnapshot.getEntities()) {
				if (entity.type != CandiConstants.TYPE_CANDI_COMMAND) {
					if (!entity.hidden) {
						entity.rookie = false;
						entity.discoveryTime = entity.beacon.discoveryTime;
					}
				}
				for (Entity childEntity : entity.children) {
					if (childEntity.type != CandiConstants.TYPE_CANDI_COMMAND) {
						if (!childEntity.hidden) {
							childEntity.rookie = false;
							childEntity.discoveryTime = childEntity.beacon.discoveryTime;
						}
					}
				}
			}
		}
		else {
			/* Check to see if there is a brand new entity in the collection */
			mEntityModelSnapshot.setRookieHit(rookieHit());
		}

		renderingActivate();
		if (fullBuild) {
			/*
			 * Clears all game engine sprites. Clears all touch areas. Clears zone and candi model collections. Reloads
			 * shared textures. Creates new root candi model
			 */
			Logger.d(null, "Starting full build.");
			mFullUpdateInProgress = true;
			mCandiViewsActiveHash.clear();
			mZoneViews.clear();
			clearCandiLayer();
			clearZoneLayer();
			mEngine.getScene().clearTouchAreas();
			mCandiPatchModel.reset(); /* Clears zone and candi model collections */
			initialize(); /* Reloads shared textures */
		}

		else {
			Logger.d(null, "Starting standard update.");

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
				boolean orphaned = true;
				for (Entity entity : mEntityModelSnapshot.getEntities()) {
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
		CandiModel candiRootNext = new CandiModel("0", mCandiPatchModel);
		candiRootNext.setSuperRoot(true);

		/* Strip linkages */
		for (CandiModel candiModel : mCandiPatchModel.getCandiModels()) {
			candiModel.setParent(null);
			candiModel.getChildren().clear();
		}

		/* Make sure each entity has a candi model */
		for (Entity entity : mEntityModelSnapshot.getEntities()) {

			CandiModel candiModel = null;
			if (mCandiPatchModel.hasCandiModelForEntity(entity.id)) {
				candiModel = mCandiPatchModel.updateCandiModel(entity, mCandiRadarActivity.mPrefDisplayExtras);
				candiModel.setParent(candiRootNext);
				candiRootNext.getChildren().add(candiModel);
			}
			else {
				/*
				 * We keep bumping the date up until the entity is finally visible.
				 */
				if (entity.rookie) {
					entity.discoveryTime = DateUtils.nowDate();
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

			for (Entity childEntity : entity.children) {

				CandiModel childCandiModel = null;
				if (mCandiPatchModel.hasCandiModelForEntity(childEntity.id)) {
					childCandiModel = mCandiPatchModel.updateCandiModel(childEntity, mCandiRadarActivity.mPrefDisplayExtras);
					candiModel.getChildren().add(childCandiModel);
					childCandiModel.setParent(candiModel);
				}
				else {
					if (childEntity.rookie) {
						childEntity.discoveryTime = DateUtils.nowDate();
						if (!childEntity.hidden) {
							childEntity.rookie = false;
						}
					}
					childCandiModel = CandiModelFactory.newCandiModel(childEntity.id, childEntity, mCandiPatchModel);
					childCandiModel.setDisplayExtra(mCandiRadarActivity.mPrefDisplayExtras);
					mCandiPatchModel.addCandiModel(childCandiModel);
					candiModel.getChildren().add(childCandiModel);
					childCandiModel.setParent(candiModel);
				}
			}
		}

		/* Paging indication for top candi */
		CandiModel commandCandiModel = null;
		if (mEntityModelSnapshot.getEntities().isMore()) {
			/*
			 * This indicator entity will be removed when the main entity list
			 * is rebuilt for any reason because they are not associated with
			 * any beacons. It will get added back if needed again by this code.
			 */
			Entity commandEntity = Entity.getCommandEntity(CommandType.ChunkEntities);
			commandCandiModel = CandiModelFactory.newCandiModel(commandEntity.id, commandEntity, mCandiPatchModel);
			commandCandiModel.setDisplayExtra(mCandiRadarActivity.mPrefDisplayExtras);
			mCandiPatchModel.addCandiModel(commandCandiModel);
			commandCandiModel.setParent(candiRootNext);
			candiRootNext.getChildren().add(commandCandiModel);
		}

		/* Paging indication for child candi */
		for (Entity entity : mEntityModelSnapshot.getEntities()) {
			if (entity.children != null && entity.children.isMore()) {
				Entity commandEntityChild = Entity.getCommandEntity(CommandType.ChunkChildEntities);

				CandiModel commandCandiModelChild = CandiModelFactory.newCandiModel(commandEntityChild.id, commandEntityChild, mCandiPatchModel);
				CandiModel candiModelParent = mCandiPatchModel.getCandiModelForEntity(entity.id);
				
				commandCandiModelChild.setDisplayExtra(mCandiRadarActivity.mPrefDisplayExtras);
				mCandiPatchModel.addCandiModel(commandCandiModelChild);
				candiModelParent.getChildren().add(commandCandiModelChild);
				commandCandiModelChild.setParent(candiModelParent);
				/* So chunking logic can figure which entity to chunk children for */
				commandCandiModelChild.getEntity().parentId = candiModelParent.getEntity().id;
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
		navigateModel(mCandiPatchModel.getCandiRootNext(), delayObserverUpdate, fullBuild, Navigation.None, chunking);

		if (fullBuild) {
			manageViewsAsync();
		}
		renderingActivate(5000);
		Logger.d(this, "Model updated with entities");
	}

	public void navigateModel(IModel candiRootNext, boolean delayObserverUpdate, boolean fullUpdate, Navigation navigation, boolean chunking) {
		/*
		 * navigation = None always comes from updateCandiData.
		 * navigation = Up comes from candi radar navigateUp.
		 * navigation = Down comes from doCandiViewSingleTap.
		 * 
		 * updateCandiData gets called from radar chunking and beacon scans.
		 */
		Logger.d(null, "Starting model navigation.");

		mCandiPatchModel.setCandiRootNext(candiRootNext);

		/* Need to sort the candi before assigning to zones */
		/* Sort the candi by discovery time then modified time */
		mCandiPatchModel.sortCandiModels(mCandiPatchModel.getCandiRootNext().getChildren());
		for (IModel candiModel : mCandiPatchModel.getCandiRootNext().getChildren()) {
			mCandiPatchModel.sortCandiModels(candiModel.getChildren());
		}

		/*
		 * Set candi visible state, move candi to inactive zone if appropriate. Set candiModelFocused to null if candi
		 * with current focus will not be visible in next update.
		 */
		mCandiPatchModel.updateVisibilityNext(chunking);
		/*
		 * Clear zones and re-assign candi to zones. Swap zones if needed to keep candi with focus in the zone the user
		 * is slotted on. Set visible for zones based on candi count.
		 */
		mCandiPatchModel.updateZonesNext(navigation, chunking);

		/* Set x, y, scale and alignment for all candi */
		mCandiPatchModel.updatePositionsNext();

		/* Zone titles, candi reflections and collapsed state */
		mCandiPatchModel.updateMiscNext();

		/* Make sure zones have a view */
		for (ZoneModel zoneModel : mCandiPatchModel.getZones()) {
			ensureZoneView(zoneModel);
		}

		/*
		 * Reset focus to primary if we are navigating up to root and currently focused on a child
		 */
		if (navigation == Navigation.Up && mCandiPatchModel.getCandiModelFocused() != null) {
			if (!mCandiPatchModel.getCandiModelFocused().getParent().isSuperRoot()) {
				mCandiPatchModel.setCandiModelFocused((CandiModel) mCandiPatchModel.getCandiModelFocused().getParent());
			}
		}

		/*
		 * OK to stop blocking async processes that are on hold because the data model is being rebuilt.
		 */
		Aircandi.getInstance().setRebuildingDataModel(false);

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
				for (IModel childModel : candiModel.getChildren()) {
					CandiModel childCandiModel = (CandiModel) childModel;
					if (mCandiViewsActiveHash.get(childCandiModel.getModelIdAsString()) == null) {
						synchronized (childCandiModel.getViewModifiers()) {
							childCandiModel.getViewModifiers().clear();
						}
						synchronized (childCandiModel.getViewActions()) {
							childCandiModel.getViewActions().clear();
						}
					}
				}
			}

			/* Wait for modifiers of active candi views to finish */
			boolean modifiersFinished = false;
			int attempts = 0;
			while (!modifiersFinished) {
				attempts++;

				if (attempts >= 3) {
					Logger.d(null, "Cleared all modifiers/actions after two attempts to let them finish.");
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
						for (IModel childModel : candiModel.getChildren()) {
							CandiModel childCandiModel = (CandiModel) childModel;
							synchronized (childCandiModel.getViewModifiers()) {
								childCandiModel.getViewModifiers().clear();
							}
							synchronized (childCandiModel.getViewActions()) {
								childCandiModel.getViewActions().clear();
							}
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
					for (IModel childModel : candiModel.getChildren()) {
						CandiModel childCandiModel = (CandiModel) childModel;
						synchronized (childCandiModel.getViewModifiers()) {
							if (childCandiModel.getViewModifiers().size() > 0) {
								modifierHit = true;
								break;
							}
						}
					}
					if (modifierHit) {
						break;
					}
				}

				if (!modifierHit) {
					modifiersFinished = true;
				}
				else {
					Logger.v(this, "Still active modifiers: sleeping");
					try {
						Thread.sleep(500);
					}
					catch (InterruptedException exception) {
						return;
					}
				}
			}

			manageViews(false, true);
			doZoneAnimations(navigation);
			doTransitionAnimations(navigation);
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

		/*
		 * Now that all the view entities have updated we can do global operations like z sorting.
		 */
		mEngine.getScene().getChild(CandiConstants.LAYER_CANDI).sortChildren();

		/* Without animations, we can lazy create views. */
		if (!CandiConstants.TRANSITIONS_ACTIVE) {
			manageViewsAsync();
		}

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
					candiView.setGestureDetector(new GestureDetector(mCandiRadarActivity, candiView));
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
			Logger.v(null, "CandiView pulled from pool: " + (candiModel.getTitleText() != null ? candiModel.getTitleText() : "[Untitled]"));
		}
		else {
			Logger.v(null, "CandiView created: " + (candiModel.getTitleText() != null ? candiModel.getTitleText() : "[Untitled]"));
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
			Logger.v(null, "CandiView recycled to the pool: " + (candiModel.getTitleText() != null ? candiModel.getTitleText() : "[Untitled]"));
			mCandiViewPool.recyclePoolItem(candiView);
		}
	}

	private class ManageViewsThread extends Thread {

		@Override
		public void run() {
			manageViews(true, false);
		}
	};

	@SuppressWarnings("unused")
	private void manageViews(boolean localUpdate, boolean useNext) {
		/*
		 * - Called synchronously from navigate if doing animations else async. - Called async when finished with scroll
		 * or fling.
		 */
		// CandiList candiModels = mCandiPatchModel.getCandiModels();
		ArrayList<CandiModel> candiModels = new ArrayList<CandiModel>(mCandiPatchModel.getCandiModels());
		int countCandiModels = candiModels.size();

		if (localUpdate) {
			Logger.v(this, "Starting view management pass: async");
		}
		else {
			Logger.v(this, "Starting view management pass");
		}

		/* Recycle views first */
		int recycleCount = 0;

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
				recycleCount++;
			}
		}
		// Logger.d(this, "Recycled views: " + String.valueOf(recycleCount));

		/* First, allocate any views needed for the candi with the current focus */
		CandiModel candiModelFocused = mCandiPatchModel.getCandiModelFocused();
		int loanedCount = 0;
		if (localUpdate) {
			if (candiModelFocused.getViewStateCurrent().isVisible() && !mCandiViewsActiveHash.containsKey(candiModelFocused.getModelIdAsString())) {
				getCandiViewFromPool(candiModelFocused, localUpdate, useNext);
				synchronized (candiModelFocused.getChildren()) {
					for (IModel candiModelChildFocused : candiModelFocused.getChildren()) {
						if (candiModelChildFocused.getViewStateCurrent().isVisible() && !mCandiViewsActiveHash
								.containsKey(((CandiModel) candiModelChildFocused)
										.getModelIdAsString())) {
							getCandiViewFromPool((CandiModel) candiModelChildFocused, localUpdate, useNext);
							loanedCount++;
						}
					}
				}
			}
		}

		/*
		 * Now do the rest. Any we already did above won't be done again because they are now in the view tracking hash
		 * map.
		 */
		for (int i = 0; i < countCandiModels; i++) {
			final CandiModel candiModel = (CandiModel) candiModels.get(i);
			ViewState viewState = useNext ? candiModel.getViewStateNext() : candiModel.getViewStateCurrent();

			if (viewState.isVisible()) {
				boolean isWithinHalo = viewState.isWithinHalo(mCamera);
				if (isWithinHalo && !mCandiViewsActiveHash.containsKey(candiModel.getModelIdAsString())) {
					getCandiViewFromPool(candiModel, localUpdate, useNext);
					loanedCount++;
				}
			}
		}
		// Logger.d(this, "Loaned views: " + String.valueOf(loanedCount));

		/* update debug info */
		mCandiRadarActivity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (Aircandi.settings.getBoolean(Preferences.PREF_SHOW_DEBUG, false)) {
					mCandiRadarActivity.updateDebugInfo();
				}
			}
		});
	}

	public float getLastVisibleCandiModelX(boolean useNext) {
		CandiList candiModels = mCandiPatchModel.getCandiModels();
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
								mCandiRadarActivity.runOnUiThread(new Runnable() {

									@Override
									public void run() {
										CandiModel candiParent = (CandiModel) candiModel.getParent();
										Entity entityParent = candiParent.getEntity();
										navigateModel(candiModel.getParent(), false, false, Navigation.Down, false);
										mCandiRadarActivity.getCommon().setActionBarTitleAndIcon(entityParent, true);
										mIgnoreInput = false;
									}
								});
							}
							else {
								if (mCandiListener != null) {
									mCandiListener.onSingleTap(candiModel);
								}
							}
						}

						@Override
						public void onMoveStarted() {}
					});
		}
		else {
			/*
			 * Fan out child candi in Radar
			 */
			if (candiModel.getZoneStateCurrent().getStatus() == ZoneStatus.Secondary) {
				CandiModel candiParent = (CandiModel) candiModel.getParent();
				Entity entityParent = candiParent.getEntity();
				navigateModel(candiModel.getParent(), false, false, Navigation.Down, false);
				mCandiRadarActivity.getCommon().setActionBarTitleAndIcon(entityParent, true);
				mIgnoreInput = false;
			}
			else {
				if (mCandiListener != null) {
					mCandiListener.onSingleTap(candiModel);
				}
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
	// Misc
	// --------------------------------------------------------------------------------------------

	public boolean isVisibleEntity() {
		if (mEntityModelSnapshot == null || mEntityModelSnapshot.getEntities().size() == 0) {
			return false;
		}
		for (Entity entity : mEntityModelSnapshot.getEntities()) {
			if (!entity.hidden) {
				return true;
			}
		}
		return false;
	}

	public boolean rookieHit() {
		boolean rookieHit = false;
		for (Entity entity : mEntityModelSnapshot.getEntities()) {
			if (entity.type != CandiConstants.TYPE_CANDI_COMMAND) {
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
					for (Entity childEntity : entity.children) {
						if (!childEntity.hidden) {
							rookieHit = true;
							break;
						}
					}
					if (rookieHit) {
						break;
					}
				}
			}
		}
		return rookieHit;
	}

	// --------------------------------------------------------------------------------------------
	// Animation
	// --------------------------------------------------------------------------------------------

	private void doZoneAnimations(Navigation navigation) {
		/*
		 * This always gets called as part of a navigation operation either drilling in or out.
		 */
		for (ZoneModel zoneModel : mCandiPatchModel.getZones()) {
			for (ZoneView zoneView : mZoneViews) {
				if (zoneView.getModel() == zoneModel) {
					if (navigation != Navigation.None) {
						if (zoneModel.getCandiesNext().size() == 1 && zoneModel.getCandiesCurrent().size() > 1) {
							synchronized (zoneModel.getViewModifiers()) {
								zoneModel.getViewModifiers().addLast(
										new CandiAlphaModifier(null, CandiConstants.DURATION_TRANSITIONS_FADE, 1.0f, 0.0f,
												CandiConstants.EASE_FADE_IN));
							}
						}
						else if (zoneModel.getCandiesNext().size() > 1 && zoneModel.getCandiesCurrent().size() == 1) {
							synchronized (zoneModel.getViewModifiers()) {
								zoneModel.getViewModifiers().addLast(
										new CandiAlphaModifier(null, CandiConstants.DURATION_TRANSITIONS_FADE, 0.0f, 1.0f,
												CandiConstants.EASE_FADE_IN));
							}
						}
					}
					else {
						if (zoneModel.getViewStateNext().isVisible()) {
							/*
							 * If not camera visible, go ahead and make it visible. We don't show it because it causes a
							 * flash when it pops in and is then replace by a cand fade in.
							 */
							if (!zoneModel.getViewStateCurrent().isVisible()) {
								if (zoneModel.getCandiesNext().size() > 1 || !zoneView.isVisibleToCamera(mCamera)) {
									synchronized (zoneModel.getViewModifiers()) {
										zoneModel.getViewModifiers().addLast(
												new CandiAlphaModifier(null, CandiConstants.DURATION_TRANSITIONS_FADE, 0.0f, 1.0f,
														CandiConstants.EASE_FADE_IN));
									}
								}
							}
						}
					}
					break;
				}
			}
		}
	}

	private void doTransitionAnimations(Navigation navigation) {
		/*
		 * Zone transitions The zone might already have a fade out modifier because manageViews() populated it with a
		 * full size candi view.
		 */
		for (ZoneModel zoneModel : mCandiPatchModel.getZones()) {
			synchronized (zoneModel.getViewActions()) {
				zoneModel.getViewActions().addLast(new ViewAction(ViewActionType.Visibility));
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
						if (candiModel.getViewModifiers().isEmpty()) {

							Transition transition = candiModel.getTransition();
							ViewState viewStateCurrent = candiModel.getViewStateCurrent();
							ViewState viewStateNext = candiModel.getViewStateNext();

							if (transition != Transition.None) {
								Logger.v(this, "Transition To: " + transition.toString()
										+ ": " + (candiModel.getTitleText() != null ? candiModel.getTitleText() : "[Untitled]"));
							}

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

	private void ensureZoneFocus() {
		/*
		 * If candi with current focus is going away, move camera target to another entity before other moves/shows
		 * happen.
		 */
		if (mCandiPatchModel.getCandiModelFocused() != null
				&& mCandiPatchModel.getCandiModelFocused().getZoneStateCurrent().getZone().getZoneIndex() + 1 > mCandiPatchModel
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
			mBoundsMaxX = (mBoundsMinX + (mCandiPatchModel.getZonesOccupiedCurrentCount() - 1)
					* (CandiConstants.CANDI_VIEW_WIDTH + CandiConstants.CANDI_VIEW_SPACING));
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
			renderingActivate();

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

		/* Repeat for all children */
		for (IModel childModel : candiModel.getChildren()) {
			CandiModel childCandiModel = (CandiModel) childModel;
			final CandiView childCandiView = (CandiView) mCandiViewsActiveHash.get(childCandiModel.getModelIdAsString());
			mCandiViewsActiveHash.remove(childCandiModel.getModelIdAsString());
			if (childCandiView != null) {
				renderingActivate();

				/*
				 * Remove associated images from image cache if the parent was deleted for good
				 */
				if (candiModel.isDeleted()) {
					String imageUri = ImageRequestBuilder.getImageUriFromEntity(childCandiModel.getEntity());
					final String cacheName = ImageManager.getInstance().resolveCacheName(imageUri);
					ImageManager.getInstance().deleteImage(cacheName);
					ImageManager.getInstance().deleteImage(cacheName + ".reflection");
				}

				/* Recycle the candi view */
				if (mCandiViewsActiveHash.containsKey(childCandiModel.getModelIdAsString())) {
					sendCandiViewToPool(childCandiModel, false);
				}
			}
		}

		/* Remove child models */
		for (IModel childCandiModel : candiModel.getChildren()) {
			/*
			 * This is ok because we are not removing from the same list we are iterating.
			 */
			mCandiPatchModel.getCandiModels().remove(childCandiModel); // Search is done using model id

			if (mCandiPatchModel.getCandiModelFocused() == childCandiModel) {
				mCandiPatchModel.setCandiModelFocused(null);
			}
			if (mCandiPatchModel.getCandiModelSelected() == childCandiModel) {
				mCandiPatchModel.setCandiModelSelected(null);
			}
			if (mCandiPatchModel.getCandiRootCurrent() == childCandiModel) {
				mCandiPatchModel.setCandiRootCurrent(null);
			}
		}

		/* Remove parent model */
		mCandiPatchModel.getCandiModels().remove(candiModel); // Search is done using model id
		if (mCandiPatchModel.getCandiModelFocused() == candiModel) {
			mCandiPatchModel.setCandiModelFocused(null);
		}
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

	public void loadHardwareTextures() {
		mTexture = new Texture(512, 512, CandiConstants.GL_TEXTURE_OPTION);
		mTexture.setName("Global placeholder");
		mEngine.getTextureManager().loadTexture(mTexture);
	}

	public void loadTextureSources() {

		/* Textures that are shared by zone views */
		Bitmap zoneBodyBitmap = null;
		zoneBodyBitmap = ImageManager.getInstance().loadBitmapFromAssets(mStyleTextureBodyZone);
		if (zoneBodyBitmap != null) {
			Bitmap zoneReflectionBitmap = ImageUtils.makeReflection(zoneBodyBitmap, true);

			mZoneBodyTextureRegion = TextureRegionFactory.createFromSource(mTexture, new BitmapTextureSource(zoneBodyBitmap, mStyleTextureBodyZone,
					new IBitmapAdapter() {

						@Override
						public Bitmap reloadBitmap() {
							Bitmap zoneBodyBitmap = null;
							zoneBodyBitmap = ImageManager.getInstance().loadBitmapFromAssets(mStyleTextureBodyZone);
							return zoneBodyBitmap;
						}
					}), 0, 0);

			mZoneReflectionTextureRegion = TextureRegionFactory.createFromSource(mTexture, new BitmapTextureSource(zoneReflectionBitmap, mStyleTextureBodyZone,
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
	}

	public void resetTextures(TextureReset textureReset) {

		/* Candi views */
		Iterator it = mCandiViewsActiveHash.entrySet().iterator();
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

	public EntityModel getEntityModelSnapshot() {
		return mEntityModelSnapshot;
	}

	public void setEntityModelSnapshot(EntityModel entityModelSnapshot) {
		mEntityModelSnapshot = entityModelSnapshot;
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

			// boolean smallFling = (Math.abs(totalDx) <=
			// CandiConstants.SWIPE_SMALL_FLING);
			// if (smallFling) {
			// ZoneModel targetZoneModel =
			// mCandiPatchModel.getCandiModelFocused().getZoneStateCurrent().getZone();
			// ZoneModel nextZoneModel =
			// mCandiPatchModel.getZoneNeighbor(targetZoneModel, totalDx < 0 ?
			// true : false);
			// if (nextZoneModel != null) {
			// targetX = mCameraTargetSprite.getX() -
			// nextZoneModel.getViewStateCurrent().getX();
			// }
			// }

			if (targetX > mBoundsMaxX - 50) {
				targetX = mBoundsMaxX - 50;
			}
			else if (targetX < mBoundsMinX) {
				targetX = mBoundsMinX - 150;
			}

			// final String info = "targetX = " + String.valueOf(targetX) +
			// " totalDx = " + String.valueOf(totalDx);

			mCameraTargetSprite.registerEntityModifier(new MoveModifier(distanceTimeFactor, mCameraTargetSprite.getX(), targetX, mCameraTargetSprite
					.getY(), mCameraTargetSprite.getY(), new IEntityModifierListener() {

				@Override
				public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {

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
										public void onMoveStarted() {}
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

	@SuppressWarnings("unused")
	private class DoubleTapGestureDetector extends SimpleOnGestureListener {

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			return false;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

			/*
			 * This gets called because the gesture detector thinks its has a fling gesture
			 */

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
