package com.proxibase.aircandi.proxi;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.engine.options.EngineOptions.ScreenOrientation;
import org.anddev.andengine.engine.options.resolutionpolicy.FillResolutionPolicy;
import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.modifier.AlphaModifier;
import org.anddev.andengine.entity.modifier.FadeInModifier;
import org.anddev.andengine.entity.modifier.MoveModifier;
import org.anddev.andengine.entity.modifier.MoveXModifier;
import org.anddev.andengine.entity.modifier.ParallelEntityModifier;
import org.anddev.andengine.entity.modifier.RotationAtModifier;
import org.anddev.andengine.entity.modifier.ScaleAtModifier;
import org.anddev.andengine.entity.modifier.IEntityModifier.IEntityModifierListener;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.Scene.IOnSceneTouchListener;
import org.anddev.andengine.entity.scene.background.ColorBackground;
import org.anddev.andengine.entity.sprite.AnimatedSprite;
import org.anddev.andengine.entity.util.FPSLogger;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.font.Font;
import org.anddev.andengine.opengl.texture.Texture;
import org.anddev.andengine.opengl.texture.TextureOptions;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.opengl.texture.region.TextureRegionFactory;
import org.anddev.andengine.opengl.texture.region.TiledTextureRegion;
import org.anddev.andengine.opengl.util.GLHelper;
import org.anddev.andengine.util.modifier.IModifier;
import org.anddev.andengine.util.modifier.ease.EaseBackInOut;
import org.anddev.andengine.util.modifier.ease.EaseCubicOut;
import org.anddev.andengine.util.modifier.ease.EaseExponentialOut;
import org.anddev.andengine.util.modifier.ease.EaseQuartOut;
import org.anddev.andengine.util.modifier.ease.EaseStrongIn;
import org.anddev.andengine.util.modifier.ease.IEaseFunction;

import com.proxibase.aircandi.proxi.ProxiTile.OnProxiTileSingleTapListener;
import com.proxibase.aircandi.utilities.BitmapTextureSource;
import com.proxibase.aircandi.utilities.ImageManager;
import com.proxibase.aircandi.utilities.ImageManager.OnImageReadyListener;
import com.proxibase.sdk.android.core.ProxibaseService;
import com.proxibase.sdk.android.core.Utilities;
import com.proxibase.sdk.android.core.proxi.ProxiEntity;
import com.proxibase.sdk.android.core.proxi.ProxiBeacon.BeaconState;
import com.proxibase.sdk.android.widgets.ImageCache;
import com.proxibase.sdk.android.widgets.UtilitiesUI;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.opengl.GLU;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.GestureDetector.SimpleOnGestureListener;

@SuppressWarnings("unused")
public class ProxiUiHandler {

	private static final int		SWIPE_MAX_OFF_PATH	= 600;
	public static final int			LAYER_GENERAL		= 0;
	public static final int			LAYER_PROXI			= 1;

	private List<ProxiEntity>		mProxiEntities;

	public int						mTouchSlopSquare;
	public int						mDoubleTapSlopSquare;

	private GestureDetector			mGestureDetector;
	private Context					mContext;
	private Engine					mEngine;
	public ProxiAnimatedSprite		mBusySprite;
	private float					mLastMotionX;
	private float					mLastMotionY;
	private Rectangle				mCameraTarget;
	private int						mCameraWidth;
	private int						mCameraHeight;
	private float					mBoundsMinX;
	private float					mBoundsMaxX;
	private float					mBoundsMinY;
	private float					mBoundsMaxY;
	private Camera					mCamera;

	public Texture					mTexture;
	public TiledTextureRegion		mTextureRegionBusy;
	public TextureRegion			mTextureRegionGenericTile;
	public Texture					mFontTexture;
	public Font						mFont;

	private Activity				mActivity;
	private ImageManager			mImageManager;
	private OnProxiEntityListener	mProxiEntityListener;


	public ProxiUiHandler(Context context, Activity activity, Engine engine, Camera camera, List<ProxiEntity> proxiEntities, ImageCache imageCache) {

		this.mImageManager = new ImageManager(imageCache);
		this.mContext = context;
		this.mEngine = engine;
		this.mCamera = camera;
		this.mActivity = activity;
		this.mProxiEntities = proxiEntities;
		initialize();

	}


	private void initialize() {

		// Gestures
		mGestureDetector = new GestureDetector(mContext, new MySurfaceGestureDetector());
		final ViewConfiguration configuration = ViewConfiguration.get(mContext);

		int touchSlop = configuration.getScaledTouchSlop();
		int doubleTapSlop = configuration.getScaledDoubleTapSlop();

		mTouchSlopSquare = (touchSlop * touchSlop) / 4;
		mDoubleTapSlopSquare = doubleTapSlop * doubleTapSlop;

		// Busy sprite
		mTexture = new Texture(512, 512, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		if (!mImageManager.hasImage("tile_untagged.png"))
			mTextureRegionGenericTile = TextureRegionFactory.createFromAsset(mTexture, mContext, "gfx/generic10.png", 0, 0);
		else {
			Bitmap bitmap = mImageManager.getImage("tile_untagged.png");
			mTextureRegionGenericTile = TextureRegionFactory.createFromSource(mTexture, new BitmapTextureSource(bitmap), 0, 0);
		}

		mTextureRegionBusy = TextureRegionFactory.createTiledFromAsset(mTexture, mContext, "gfx/busyspritesIV.png", 256, 0, 4, 2);
		mEngine.getTextureManager().loadTexture(mTexture);
		mBusySprite = new ProxiAnimatedSprite(0, 0, mTextureRegionBusy);

		// Shared font
		mFontTexture = new Texture(256, 256, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		mFont = new Font(mFontTexture, Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL), ProxiConstants.PROXI_FONT_SIZE, true, Color.WHITE);
		mEngine.getTextureManager().loadTexture(mFontTexture);
		mEngine.getFontManager().loadFont(mFont);

		// Images
		mImageManager.setOnImageReadyListener(new OnImageReadyListener() {

			@Override
			public void onImageReady(String key) {

				// TODO: Check to see if one our proxi tiles needs the image.
				Utilities.Log(ProxibaseService.APP_NAME, "ProxiController", "ImageReady: " + key);
				int entityCount = mProxiEntities.size();
				for (int i = 0; i < entityCount; i++)
					if (mProxiEntities.get(i).entity.pointResourceId.equals(key)) {
						if (mProxiEntities.get(i).sprite != null) {
							((ProxiTile) mProxiEntities.get(i).sprite).setBodyBitmap(key);
							((ProxiTile) mProxiEntities.get(i).sprite).hideBusy();
							((ProxiTile) mProxiEntities.get(i).sprite).showBody();
						}
						break;
					}

			}
		});

	}


	public void initScene() {

		Scene scene = mEngine.getScene();

		if (scene != null) {

			// Create the loading sprite and add it to the scene

			final int centerX = (int) ((this.mCamera.getWidth() - mBusySprite.getWidth()) / 2);
			final int centerY = (int) ((this.mCamera.getHeight() - mBusySprite.getHeight()) / 2);

			mBusySprite.setPosition(centerX, centerY);
			mBusySprite.animate(150, true);
			scene.getChild(ProxiUiHandler.LAYER_GENERAL).attachChild(mBusySprite);

			scene.setOnSceneTouchListener(new IOnSceneTouchListener() {

				@Override
				public boolean onSceneTouchEvent(final Scene pScene, final TouchEvent pSceneTouchEvent) {

					// TouchEvent is world coordinates
					// MotionEvent is screen coordinates

					final float screenY = pSceneTouchEvent.getMotionEvent().getY();
					final float screenX = pSceneTouchEvent.getMotionEvent().getX();

					// We are only detecting flings right now
					if (mGestureDetector.onTouchEvent(pSceneTouchEvent.getMotionEvent()))
						return true;

					if (pSceneTouchEvent.isActionDown()) {
						mLastMotionX = screenX;
						mLastMotionY = screenY;
						mCameraTarget.clearEntityModifiers();
						return true;
					}

					if (pSceneTouchEvent.isActionUp()) {
						moveToEntityNearest(mCameraTarget.getX(), EaseQuartOut.getInstance());
						return true;
					}

					if (pSceneTouchEvent.isActionMove()) {

						mEngine.runOnUpdateThread(new Runnable() {

							@Override
							public void run() {

								float scrollX = mLastMotionX - screenX;
								float scrollY = mLastMotionY - screenY;

								float cameraTargetX = mCameraTarget.getX();

								if (Math.abs(scrollX) >= 1) {
									Utilities.Log(ProxibaseService.APP_NAME, "Tricorder", "Set target position x = " + String
																									.valueOf(cameraTargetX + scrollX));
									mCameraTarget.setPosition(cameraTargetX + scrollX, mCameraTarget.getY());
								}

								mLastMotionX = screenX;
								mLastMotionY = screenY;
							}
						});
						return true;
					}
					return false;
				}

			});
		}
	}


	public void clearProxiLayer() {

		mEngine.runOnUpdateThread(new Runnable() {

			@Override
			public void run() {

				IEntity layer = mEngine.getScene().getChild(LAYER_PROXI);
				int childCount = layer.getChildCount();
				for (int i = childCount - 1; i >= 0; i--) {
					IEntity child = layer.getChild(i);
					if (child instanceof ProxiTile) {

						// TODO: Should we null this so the GC can collect them.
						ProxiTile proxiTile = (ProxiTile) child;
						proxiTile.unloadResources();
						proxiTile.detachSelf();
					}
					else {
						child.detachSelf(); // Just in case
					}
				}
			}
		});
	}


	/**
	 * Updates the ProxiView UI based on the current data source. Throws an exception if the
	 * data source is null or the wrong type.
	 */
	public void refreshProxiView(boolean fullUpdate) {

		if (fullUpdate) {
			clearProxiLayer();
		}

		Scene scene = this.mEngine.getScene();

		// Make the first move: hide ones we already have a sprite for
		for (int i = 0; i < mProxiEntities.size(); i++) {
			ProxiEntity proxiEntity = mProxiEntities.get(i);
			if (proxiEntity.isHidden && proxiEntity.sprite != null) {
				ProxiTile proxiTile = (ProxiTile) proxiEntity.sprite;
				scene.unregisterTouchArea(proxiTile.mBodySprite);
				proxiTile.hideAll();
			}
		}
		// Update the boundaries because
		// setCameraBoundaries();

		moveToEntityNearest(mCameraTarget.getX(), EaseQuartOut.getInstance());

		int centerX = (int) ((this.mCamera.getWidth() - ProxiConstants.PROXI_TILE_WIDTH) * 0.5f);
		int centerY = (int) ((this.mCamera.getHeight() - ProxiConstants.PROXI_TILE_HEIGHT) * 0.5f);

		List<ProxiTile> proxiTiles = new ArrayList<ProxiTile>();

		int slotVisible = 1;
		Utilities.Log(ProxibaseService.APP_NAME, "ProxiHandler", "Entity count: " + String.valueOf(mProxiEntities.size()));
		for (int i = 0; i < mProxiEntities.size(); i++) {

			ProxiEntity proxiEntity = mProxiEntities.get(i);
			if (!proxiEntity.isHidden) {
				ProxiTile proxiTile = (ProxiTile) mProxiEntities.get(i).sprite;
				if (proxiTile == null) {
					proxiTile = ProxiTileBuilder.createProxiTile(mContext, centerX + ((slotVisible - 1) * 270), centerY, proxiEntity, i,
							ProxiUiHandler.this, new ProxiTile.OnProxiTileSingleTapListener() {

								@Override
								public void onProxiTileSingleTap(ProxiTile proxiTile) {

									moveToEntity(proxiTile.mIndex, 1, EaseQuartOut.getInstance());
									if (mProxiEntityListener != null)
										mProxiEntityListener.onSingleTap(proxiTile.getProxiEntity());
								}

							});

					proxiTile.mSlotVisible = slotVisible;
					proxiTiles.add(proxiTile);
					Utilities.Log(ProxibaseService.APP_NAME, "ProxiHandler", "Adding new tile: " + proxiTile.getProxiEntity().entity.label);
				}
				else {
					// Might be transitioning from hidden to not hidden
					// Will get set as New
					proxiTile.mOldX = proxiTile.getX();
					proxiTile.setPosition(centerX + ((slotVisible - 1) * 270), centerY);
					proxiTile.mSlotVisible = slotVisible;
				}
				slotVisible++;
			}
		}

		// Now move existing ones
		for (int i = 0; i < mProxiEntities.size(); i++) {
			ProxiEntity proxiEntity = mProxiEntities.get(i);
			if (!proxiEntity.isHidden && proxiEntity.beacon.state != BeaconState.New) {
				ProxiTile proxiTile = (ProxiTile) proxiEntity.sprite;
				proxiTile.registerEntityModifier(new MoveXModifier(2f, proxiTile.mOldX, centerX + ((proxiTile.mSlotVisible - 1) * 270), EaseQuartOut
						.getInstance()));
			}
		}

		// Show the new old ones
		for (int i = 0; i < mProxiEntities.size(); i++) {
			ProxiEntity proxiEntity = mProxiEntities.get(i);
			if (proxiEntity.beacon.state == BeaconState.New && !proxiEntity.isHidden)
				if (((ProxiTile) proxiEntity.sprite).mOldX != 0) {
					ProxiTile proxiTile = (ProxiTile) proxiEntity.sprite;
					proxiTile.firstDraw();
					if (proxiTile.mBodySprite != null) {
						scene.registerTouchArea(proxiTile.mBodySprite);
						proxiTile.showBody();
					}
					else {
						proxiTile.showBusy();
					}
				}
		}

		// Show the new new ones
		for (ProxiTile proxiTile : proxiTiles) {
			proxiTile.firstDraw();
			if (proxiTile.mBodySprite != null) {
				scene.registerTouchArea(proxiTile.mBodySprite);
				proxiTile.showBody();
			}
			else {
				proxiTile.showBusy();
			}
			scene.getChild(ProxiUiHandler.LAYER_PROXI).attachChild(proxiTile);
		}

		// Update the boundaries
		setCameraBoundaries();

		if (mBusySprite != null && mBusySprite.isVisible())
			mBusySprite.setVisible(false);

		// moveToEntity(mProxiEntities.size() - 1, 2, EaseBackInOut.getInstance());
	}


	private void moveToEntity(int index, float duration, IEaseFunction easeFunction) {

		ProxiEntity targetEntity = mProxiEntities.get(index);
		ProxiTile proxiTile = (ProxiTile) targetEntity.sprite;
		Utilities.Log(ProxibaseService.APP_NAME, "Tricorder", "Move to nearest x = " + String.valueOf(proxiTile.getX()));
		mCameraTarget.registerEntityModifier(new MoveModifier(duration, mCameraTarget.getX(), proxiTile.getX(), mCameraTarget.getY(), mCameraTarget
				.getY(), easeFunction));
	}


	private void moveToEntityNearest(float nearestToX, IEaseFunction easeFunction) {

		if (mProxiEntities.size() == 0)
			return;

		int nearestIndex = 0;
		float smallestDistance = 999999;
		for (int i = 0; i < mProxiEntities.size(); i++) {
			ProxiEntity targetEntity = mProxiEntities.get(i);
			if (!targetEntity.isHidden && targetEntity.sprite != null) {
				ProxiTile proxiTile = (ProxiTile) targetEntity.sprite;
				float distance = Math.abs(proxiTile.getX() - nearestToX);
				if (distance < smallestDistance) {
					nearestIndex = i;
					smallestDistance = distance;
				}
			}
		}

		ProxiEntity targetEntity = mProxiEntities.get(nearestIndex);
		if (!targetEntity.isHidden) {
			ProxiTile proxiTile = (ProxiTile) targetEntity.sprite;
			Utilities.Log(ProxibaseService.APP_NAME, "Tricorder", "Move to nearest x = " + String.valueOf(proxiTile.getX()));
			mCameraTarget.registerEntityModifier(new MoveModifier(.6f, mCameraTarget.getX(), proxiTile.getX(), mCameraTarget.getY(), mCameraTarget
					.getY(), easeFunction));
		}
	}


	private void setCameraBoundaries() {

		mBoundsMinX = 0;
		mBoundsMaxX = this.mCameraWidth;
		mBoundsMinY = 0;
		mBoundsMaxY = this.mCameraHeight;

		if (mProxiEntities.size() > 0) {
			for (int i = 0; i < mProxiEntities.size(); i++)
				if (!mProxiEntities.get(i).isHidden && mProxiEntities.get(i).sprite != null) {
					mBoundsMinX = ((ProxiTile) mProxiEntities.get(i).sprite).getX();
					break;
				}

			for (int i = mProxiEntities.size() - 1; i >= 0; i--)
				if (!mProxiEntities.get(i).isHidden && mProxiEntities.get(i).sprite != null) {
					mBoundsMaxX = ((ProxiTile) mProxiEntities.get(i).sprite).getX();
					break;
				}
			mBoundsMinY = 0;
			mBoundsMaxY = this.mCameraHeight;
		}

	}


	public List<ProxiEntity> getProxiEntities() {

		return this.mProxiEntities;
	}


	public void setProxiEntities(List<ProxiEntity> mProxiEntities) {

		this.mProxiEntities = mProxiEntities;
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
		float targetX = mCameraTarget.getX() - (mCameraTarget.getWidth() * 0.5f);
		GLU.gluLookAt(pGL, targetX + 10, 0, camZ, targetX + 10, 0, 0, 0, 1, 0); // move camera back
		pGL.glScalef(1, -1, 1); // reverse y-axis
		pGL.glTranslatef(-mCamera.getWidth() / 2, -mCamera.getHeight() / 2, 0); // origin at top left
	}


	/**
	 * @return the mCamera
	 */
	public Camera getCamera() {

		return this.mCamera;
	}


	/**
	 * @param mCamera the mCamera to set
	 */
	public void setCamera(Camera mCamera) {

		this.mCamera = mCamera;
	}


	/**
	 * @return the mCameraTarget
	 */
	public Rectangle getCameraTarget() {

		return this.mCameraTarget;
	}


	/**
	 * @param mCameraTarget the mCameraTarget to set
	 */
	public void setCameraTarget(Rectangle mCameraTarget) {

		this.mCameraTarget = mCameraTarget;
	}


	public ImageManager getImageManager() {

		return this.mImageManager;
	}


	public void setImageManager(ImageManager mImageManager) {

		this.mImageManager = mImageManager;
	}


	public Engine getEngine() {

		return this.mEngine;
	}


	public void setEngine(Engine mEngine) {

		this.mEngine = mEngine;
	}


	class MySurfaceGestureDetector extends SimpleOnGestureListener {

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

			// Test for swipe that is too vertical to trigger a fling
			if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH) {
				moveToEntityNearest(mCameraTarget.getX(), EaseQuartOut.getInstance());
				return false;
			}

			// Check to see if we are at a boundary
			float cameraTargetX = mCameraTarget.getX();
			if (cameraTargetX <= mBoundsMinX || cameraTargetX >= mBoundsMaxX) {
				moveToEntityNearest(mCameraTarget.getX(), EaseQuartOut.getInstance());
				return false;
			}

			// The velocity units are in pixels per second.
			final float distanceTimeFactor = 0.8f;
			final float totalDx = (distanceTimeFactor * velocityX / 2);
			final float totalDy = (distanceTimeFactor * velocityY / 2);

			// Cap the distance we travel so we don't race past our boundaries
			float targetX = mCameraTarget.getX() - totalDx;
			if (targetX > mBoundsMaxX)
				targetX = mBoundsMaxX + 50;
			else if (targetX < mBoundsMinX)
				targetX = mBoundsMinX - 50;

			mCameraTarget.registerEntityModifier(new MoveModifier(distanceTimeFactor, mCameraTarget.getX(), targetX, mCameraTarget.getY(),
					mCameraTarget.getY(), new IEntityModifierListener() {

						@Override
						public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {

							moveToEntityNearest(mCameraTarget.getX(), EaseQuartOut.getInstance());
						}
					}, EaseExponentialOut.getInstance()));

			return true;
		}
	}


	public void setOnProxiEntityListener(OnProxiEntityListener listener) {

		mProxiEntityListener = listener;
	}


	public final OnProxiEntityListener getOnProxiEntityListener() {

		return mProxiEntityListener;
	}


	public interface OnProxiEntityListener {

		void onSelected(ProxiEntity proxiEntity);


		void onSingleTap(ProxiEntity proxiEntity);
	}

}
