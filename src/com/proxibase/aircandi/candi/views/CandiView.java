package com.proxibase.aircandi.candi.views;

import java.util.Observable;

import javax.microedition.khronos.opengles.GL10;

import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.modifier.AlphaModifier;
import org.anddev.andengine.entity.modifier.DelayModifier;
import org.anddev.andengine.entity.modifier.IEntityModifier;
import org.anddev.andengine.entity.modifier.MoveYModifier;
import org.anddev.andengine.entity.modifier.SequenceEntityModifier;
import org.anddev.andengine.entity.modifier.IEntityModifier.IEntityModifierListener;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.buffer.BufferObjectManager;
import org.anddev.andengine.opengl.texture.Texture;
import org.anddev.andengine.opengl.texture.TextureOptions;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.opengl.texture.region.TextureRegionFactory;
import org.anddev.andengine.opengl.texture.region.TiledTextureRegion;
import org.anddev.andengine.util.modifier.IModifier;
import org.anddev.andengine.util.modifier.IModifier.IModifierListener;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;

import com.proxibase.aircandi.candi.models.BaseModel;
import com.proxibase.aircandi.candi.models.CandiModel;
import com.proxibase.aircandi.candi.models.CandiModel.Transition;
import com.proxibase.aircandi.candi.models.ZoneModel.Position;
import com.proxibase.aircandi.candi.presenters.CandiPatchPresenter;
import com.proxibase.aircandi.candi.sprites.CandiAnimatedSprite;
import com.proxibase.aircandi.candi.sprites.CandiSprite;
import com.proxibase.aircandi.candi.utils.CandiConstants;
import com.proxibase.aircandi.utils.AircandiUI;
import com.proxibase.aircandi.utils.BitmapTextureSource;
import com.proxibase.aircandi.utils.ImageManager;
import com.proxibase.aircandi.utils.Utilities;
import com.proxibase.aircandi.utils.BitmapTextureSource.IBitmapAdapter;
import com.proxibase.aircandi.utils.ImageManager.IImageReadyListener;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest;

@SuppressWarnings("unused")
public class CandiView extends BaseView implements OnGestureListener, OnDoubleTapListener {

	private CandiModel						mCandiModel;

	private Texture							mBodyTexture;
	private TextureRegion					mBodyTextureRegion;
	private CandiSprite						mBodySprite;

	private Texture							mReflectionTexture;
	private TextureRegion					mReflectionTextureRegion;
	private CandiSprite						mReflectionSprite;

	private TiledTextureRegion				mProgressTextureRegion;
	private CandiAnimatedSprite				mProgressSprite;

	private TextureRegion					mPlaceholderTextureRegion;
	private CandiSprite						mPlaceholderSprite;

	private OnViewTexturesLoadedListener	mTexturesLoadedListener;
	private OnCandiViewTouchListener		mTouchListener;
	private boolean							mCollapsed						= false;
	private GestureDetector					mGestureDetector;
	private Boolean							mActiveImageRequest				= false;
	private Boolean							mIsVisibleToCamera				= false;
	private Boolean							mTextureIsLoadedToHardware		= false;
	private Boolean							mTextureUpdateOnHardwareNeeded	= false;

	public CandiView(CandiModel candiModel, CandiPatchPresenter candiPatchPresenter) {
		super((BaseModel) candiModel, candiPatchPresenter);

		mCandiModel = candiModel;
	}

	@Override
	public void update(Observable observable, Object data) {
		super.update(observable, data);

		updateTouchArea(mCandiModel.isTouchAreaActive());

		if (mCandiModel.isBodyOnly() != mCollapsed) {
			if (mCandiModel.isBodyOnly())
				showCollapsedAnimated(CandiConstants.DURATION_CANDIBODY_COLLAPSE);
			else
				showExpandedAnimated(CandiConstants.DURATION_CANDIBODY_EXPAND);
		}

		if (mCandiModel.isVisibleNext()) {
			Transition transition = mCandiModel.getTransition();
			if (transition == Transition.FadeIn)
				setAlpha(0);
			setVisible(true);
		}

		doModifiers();
	}

	@Override
	public void initialize() {
		super.initialize();

		mGestureDetector = new GestureDetector(mCandiPatchPresenter.mCandiActivity, this);
		makeProgressSprites();
		loadBodyTextureSources(true, false);
		construct();
	}

	private void construct() {

		// Body sprite
		if (mBodyTextureRegion != null) {
			makeBodySprite();
			mBodySprite.setVisible(true);
		}

		// Reflection
		if (mReflectionTextureRegion != null) {
			makeReflectionSprite();
			mReflectionSprite.setVisible(true);
		}

		// ZOrder sort
		sortChildren();
	}

	private void updateTouchArea(final boolean isTouchAreaActive) {
		mCandiPatchPresenter.getEngine().runOnUpdateThread(new Runnable() {

			@Override
			public void run() {
				if (mBodySprite != null) {
					boolean registeredTouchArea = mCandiPatchPresenter.getEngine().getScene().getTouchAreas().contains(mBodySprite);
					if (registeredTouchArea && !isTouchAreaActive) {
						mCandiPatchPresenter.getEngine().getScene().unregisterTouchArea(mBodySprite);
					}
					else if (!registeredTouchArea && isTouchAreaActive) {
						mCandiPatchPresenter.getEngine().getScene().registerTouchArea(mBodySprite);
					}
				}
			}
		});
	}

	private void makeProgressSprites() {

		// Placeholder
		mPlaceholderTextureRegion = mCandiPatchPresenter.mPlaceholderTextureRegion.clone();
		mPlaceholderSprite = new CandiSprite(0, CandiConstants.CANDI_VIEW_TITLE_HEIGHT, mPlaceholderTextureRegion);
		mPlaceholderSprite.setBlendFunction(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);
		mPlaceholderSprite.setAlpha(0);
		mPlaceholderSprite.setVisible(false);
		mPlaceholderSprite.setZIndex(-10);
		attachChild(mPlaceholderSprite);

		// Progress
		mProgressTextureRegion = mCandiPatchPresenter.mProgressTextureRegion.clone();
		float progressX = (mPlaceholderSprite.getWidth() - mProgressTextureRegion.getTileWidth()) * 0.5f;
		float progressY = CandiConstants.CANDI_VIEW_TITLE_HEIGHT + (mPlaceholderSprite.getHeight() * 0.5f)
							- (mProgressTextureRegion.getTileHeight() * 0.5f);
		mProgressSprite = new CandiAnimatedSprite(progressX, progressY, mProgressTextureRegion);
		mProgressSprite.setAlpha(0);
		mProgressSprite.animate(150, true);
		mProgressSprite.setVisible(false);
		mProgressSprite.setZIndex(-9);
		attachChild(mProgressSprite);
	}

	private void makeReflectionSprite() {
		mReflectionSprite = new CandiSprite(0, CandiConstants.CANDI_VIEW_TITLE_HEIGHT + CandiConstants.CANDI_VIEW_BODY_HEIGHT,
				mReflectionTextureRegion);
		mReflectionSprite.setBlendFunction(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);
		mReflectionSprite.setAlpha(0);
		mReflectionSprite.setVisible(false);
		mReflectionSprite.setZIndex(0);
		attachChild(mReflectionSprite);
	}

	private void makeBodySprite() {
		mBodySprite = new CandiSprite(0, CandiConstants.CANDI_VIEW_TITLE_HEIGHT, mBodyTextureRegion) {

			@Override
			public boolean onAreaTouched(final TouchEvent sceneTouchEvent, final float touchAreaLocalX, final float touchAreaLocalY) {
				return mGestureDetector.onTouchEvent(sceneTouchEvent.getMotionEvent());
			}

		};
		mBodySprite.setBlendFunction(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);
		mBodySprite.setAlpha(0);
		mBodySprite.setVisible(false);
		mBodySprite.setZIndex(0);
		attachChild(mBodySprite);
		updateTouchArea(mCandiModel.isTouchAreaActive());
	}

	/**
	 * Update texture sources for the body and the body reflection. Create
	 * the body and reflection sprites to show the textures if they don't already
	 * exist.
	 */
	private void updateBodyTextures(Bitmap bodyBitmap) {

		mBodyTexture.clearTextureSources();
		mBodyTextureRegion = TextureRegionFactory.createFromSource(mBodyTexture, new BitmapTextureSource(bodyBitmap, new IBitmapAdapter() {

			@Override
			public Bitmap reloadBitmap() {
				// TextureSource needs to refresh a recycled bitmap.
				Bitmap bitmap = ImageManager.getInstance().getImage(mCandiModel.getBodyImageId());
				if (bitmap != null) {
					return bitmap;
				}
				// Cached bitmap is gone so load it again.
				loadBodyTextureSources(true, false);
				return null;
			}
		}), 0, 0);

		if (mBodySprite == null)
			makeBodySprite();

		Bitmap reflectionBitmap = ImageManager.getInstance().getImage(mCandiModel.getBodyImageId() + ".reflection");
		if (reflectionBitmap == null) {
			if (bodyBitmap != null) {
				reflectionBitmap = AircandiUI.getReflection(bodyBitmap);
				ImageManager.getInstance().getImageCache().put(mCandiModel.getBodyImageId() + ".reflection", reflectionBitmap);
			}
		}

		if (reflectionBitmap != null) {
			mReflectionTexture.clearTextureSources();
			mReflectionTextureRegion = TextureRegionFactory.createFromSource(mReflectionTexture, new BitmapTextureSource(reflectionBitmap,
					new IBitmapAdapter() {

						@Override
						public Bitmap reloadBitmap() {
							Bitmap bitmap = ImageManager.getInstance().getImage(mCandiModel.getBodyImageId() + ".reflection");
							if (bitmap != null) {
								return bitmap;
							}
							return null;
						}
					}), 0, 0);

			if (mReflectionSprite == null)
				makeReflectionSprite();
		}
	}

	@Override
	public boolean isVisibleToCamera(final Camera camera) {

		if (super.isVisibleToCamera(camera))
			return true;
		else {
			if (mBodySprite != null && mBodySprite.isVisibleToCamera(camera))
				return true;
			if (mReflectionSprite != null && mReflectionSprite.isVisibleToCamera(camera))
				return true;
			if (mPlaceholderSprite != null && mPlaceholderSprite.isVisibleToCamera(camera))
				return true;
		}
		return false;
	}

	private void doModifiers() {
		if (!mCandiModel.getModifiers().isEmpty()) {
			IEntityModifier modifier = mCandiModel.getModifiers().removeFirst();
			modifier.addModifierListener(new IModifierListener<IEntity>() {

				@Override
				public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
					doModifiers();
				}

				@Override
				public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {
				// TODO Auto-generated method stub

				}
			});
			registerEntityModifier(modifier);
		}
	}

	private void showProgress() {
		mProgressSprite.setVisible(true);
		mProgressSprite.animate(150, true);
	}

	private void hideProgress() {
		mProgressSprite.setVisible(false);
		mProgressSprite.stopAnimation();
	}

	private void showPlaceholder() {
		if (mReflectionSprite != null)
			mReflectionSprite.setVisible(false);
		if (mBodySprite != null)
			mBodySprite.setVisible(false);
		mPlaceholderSprite.setVisible(true);
		showProgress();
	}

	public void showCollapsed() {

		mTitleSprite.setAlpha(0);
		mTitleSprite.setVisible(false);
		if (mBodySprite != null) {
			mBodySprite.setPosition(0, 0);
			if (mReflectionSprite != null) {
				mReflectionSprite.setPosition(0, CandiConstants.CANDI_VIEW_BODY_HEIGHT);
				reflectionVisible(mCandiModel.getPositionNext().rowLast, false, 0);
			}
		}
		if (mPlaceholderSprite != null) {
			mPlaceholderSprite.setPosition(0, 0);
			float progressX = (mPlaceholderSprite.getWidth() - mProgressTextureRegion.getTileWidth()) * 0.5f;
			float progressY = (mPlaceholderSprite.getHeight() - mProgressTextureRegion.getTileHeight()) * 0.5f;
			mProgressSprite.setPosition(progressX, progressY);
		}
		mCollapsed = true;
	}

	public void showExpanded() {

		mTitleSprite.setAlpha(1);
		mTitleSprite.setVisible(true);
		if (mBodySprite != null) {
			mBodySprite.setPosition(0, CandiConstants.CANDI_VIEW_TITLE_HEIGHT);
			if (mReflectionSprite != null) {
				mReflectionSprite.setPosition(0, CandiConstants.CANDI_VIEW_BODY_HEIGHT + CandiConstants.CANDI_VIEW_TITLE_HEIGHT);
				reflectionVisible(mCandiModel.getPositionNext().rowLast, false, 0);
			}
		}
		if (mPlaceholderSprite != null) {
			mPlaceholderSprite.setPosition(0, CandiConstants.CANDI_VIEW_TITLE_HEIGHT);
			float progressX = (mPlaceholderSprite.getWidth() - mProgressTextureRegion.getTileWidth()) * 0.5f;
			float progressY = ((mPlaceholderSprite.getHeight() + CandiConstants.CANDI_VIEW_TITLE_HEIGHT) - mProgressTextureRegion.getTileHeight()) * 0.5f;
			mProgressSprite.setPosition(progressX, progressY);
		}
		mCollapsed = false;
	}

	public void showCollapsedAnimated(float duration) {
		if (!mCandiModel.isVisibleNext())
			showCollapsed();

		mTitleSprite.registerEntityModifier(new AlphaModifier(duration, 1.0f, 0.0f, new IEntityModifierListener() {

			@Override
			public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
				mTitleSprite.setVisible(false);
			}

			@Override
			public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {}
		}));

		if (mBodySprite != null) {
			mBodySprite.registerEntityModifier(new MoveYModifier(duration, CandiConstants.CANDI_VIEW_TITLE_HEIGHT, 0));
			if (mReflectionSprite != null) {
				reflectionVisible(mCandiModel.getPositionNext().rowLast, true, 1);
				if (!mCandiModel.getPositionNext().rowLast) {
					mReflectionSprite.registerEntityModifier(new SequenceEntityModifier(new DelayModifier(duration * 0.5f), new MoveYModifier(
							duration * 0.5f, CandiConstants.CANDI_VIEW_BODY_HEIGHT + CandiConstants.CANDI_VIEW_TITLE_HEIGHT,
							CandiConstants.CANDI_VIEW_BODY_HEIGHT)));
				}
				else {
					mReflectionSprite.registerEntityModifier(new SequenceEntityModifier(new DelayModifier(duration * 0.5f), new MoveYModifier(
							duration * 0.5f, CandiConstants.CANDI_VIEW_BODY_HEIGHT + CandiConstants.CANDI_VIEW_TITLE_HEIGHT,
							CandiConstants.CANDI_VIEW_BODY_HEIGHT)));
				}
			}
		}
		if (mPlaceholderSprite != null) {
			mPlaceholderSprite.registerEntityModifier(new MoveYModifier(duration, CandiConstants.CANDI_VIEW_TITLE_HEIGHT, 0));
			float progressToY = (mPlaceholderSprite.getHeight() - mProgressTextureRegion.getTileHeight()) * 0.5f;
			float progressFromY = ((mPlaceholderSprite.getHeight() + CandiConstants.CANDI_VIEW_TITLE_HEIGHT) - mProgressTextureRegion.getTileHeight()) * 0.5f;
			mProgressSprite.registerEntityModifier(new MoveYModifier(duration, progressFromY, progressToY));
		}
		mCollapsed = true;
	}

	public void showExpandedAnimated(float duration) {
		if (!mCandiModel.isVisibleNext())
			showExpanded();

		mTitleSprite.setVisible(true);
		mTitleSprite.registerEntityModifier(new AlphaModifier(duration, 0.0f, 1.0f));

		if (mBodySprite != null) {
			mBodySprite.registerEntityModifier(new MoveYModifier(duration, 0, CandiConstants.CANDI_VIEW_TITLE_HEIGHT));
			if (mReflectionSprite != null) {
				reflectionVisible(mCandiModel.getPositionNext().rowLast, true, 1);
				if (mCandiModel.getPositionNext().rowLast) {
					mReflectionSprite.registerEntityModifier(new SequenceEntityModifier(new MoveYModifier(duration * 0.5f,
							CandiConstants.CANDI_VIEW_BODY_HEIGHT, CandiConstants.CANDI_VIEW_BODY_HEIGHT + CandiConstants.CANDI_VIEW_TITLE_HEIGHT)));
				}
			}
		}
		if (mPlaceholderSprite != null) {
			mPlaceholderSprite.registerEntityModifier(new MoveYModifier(duration, 0, CandiConstants.CANDI_VIEW_TITLE_HEIGHT));
			float progressFromY = (mPlaceholderSprite.getHeight() - mProgressTextureRegion.getTileHeight()) * 0.5f;
			float progressToY = ((mPlaceholderSprite.getHeight() + CandiConstants.CANDI_VIEW_TITLE_HEIGHT) - mProgressTextureRegion.getTileHeight()) * 0.5f;
			mProgressSprite.registerEntityModifier(new MoveYModifier(duration, progressFromY, progressToY));
		}
		mCollapsed = false;
	}

	public void reflectionVisible(boolean visible, boolean animate, float duration) {
		if (mReflectionSprite == null)
			return;

		if (!animate) {
			mReflectionSprite.setAlpha(visible ? 1 : 0);
			mReflectionSprite.setVisible(visible);
		}
		else {
			if (visible) {
				if (!mReflectionSprite.isVisible()) {
					mReflectionSprite.setAlpha(0);
					mReflectionSprite.setVisible(true);
					mReflectionSprite.registerEntityModifier(new SequenceEntityModifier(new DelayModifier(duration * 0.5f), new AlphaModifier(
							duration * 0.5f, 0.0f, 1.0f)));
				}
			}
			else {
				mReflectionSprite.registerEntityModifier(new SequenceEntityModifier(new DelayModifier(0.0f), new AlphaModifier(duration * 0.5f, 1.0f,
						0.0f, new IEntityModifierListener() {

							@Override
							public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
								mReflectionSprite.setVisible(false);
							}

							@Override
							public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {}
						})));
			}
		}
	}

	private void swapImageForPlaceholder() {
		hideProgress();
		if (mCandiModel.isVisibleCurrent()) {
			mPlaceholderSprite.registerEntityModifier(new AlphaModifier(CandiConstants.DURATION_PLACEHOLDER_HIDESHOW, 1.0f, 0.0f));
			mBodySprite.registerEntityModifier(new AlphaModifier(CandiConstants.DURATION_PLACEHOLDER_HIDESHOW, 0.0f, 1.0f));
			mReflectionSprite.registerEntityModifier(new AlphaModifier(CandiConstants.DURATION_PLACEHOLDER_HIDESHOW, 0.0f, 1.0f));
		}
		else {
			mPlaceholderSprite.setAlpha(0);
		}
	}

	@Override
	public void resetTextures() {
		super.resetTextures();

		mBodyTexture.clearTextureSources();
		mReflectionTexture.clearTextureSources();
		mPlaceholderTextureRegion = mCandiPatchPresenter.mPlaceholderTextureRegion.clone();
		mProgressTextureRegion = mCandiPatchPresenter.mProgressTextureRegion.clone();
		loadBodyTextureSources(true, false);
	}

	@Override
	public void loadTextures() {
		super.loadTextures();

		mReflectionTexture = new Texture(256, 128, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		mCandiPatchPresenter.getEngine().getTextureManager().loadTexture(mReflectionTexture);
		mBodyTexture = new Texture(256, 256, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		mCandiPatchPresenter.getEngine().getTextureManager().loadTexture(mBodyTexture);

		if (mTexturesLoadedListener != null)
			mTexturesLoadedListener.onTexturesLoaded(this);
	}

	public void loadBodyTextureSources(final boolean showPlaceholder, final boolean forceRefetch) {

		if (forceRefetch || !ImageManager.getInstance().hasImage(mCandiModel.getBodyImageId())) {
			if (!mActiveImageRequest) {
				mActiveImageRequest = true;
				ImageRequest imageRequest = ImageManager.createImageRequest(mCandiModel.getBodyImageUri(), mCandiModel.getBodyImageFormat(),
						"square", 250, true, new IImageReadyListener() {

							@Override
							public void onImageReady(Bitmap bodyBitmap) {
								Utilities.Log(CandiConstants.APP_NAME, "CandiView", "Image fetched: " + mCandiModel.getBodyImageId());
								updateBodyTextures(bodyBitmap);

								if (showPlaceholder) {
									if (mCollapsed) {
										showCollapsed();
										mBodySprite.setVisible(true);
									}
									else {
										showExpanded();
										mBodySprite.setVisible(true);
									}
									swapImageForPlaceholder();
								}
								mActiveImageRequest = false;
							}
						});

				if (showPlaceholder) {
					showPlaceholder();
				}
				ImageManager.getInstance().fetchImageAsynch(imageRequest);
			}
		}
		else {
			Utilities.Log(CandiConstants.APP_NAME, "CandiView", mCandiModel.getEntityProxy().label + ": " + "cache hit");
			Bitmap bodyBitmap = ImageManager.getInstance().getImage(mCandiModel.getBodyImageId());
			if (bodyBitmap != null) {
				updateBodyTextures(bodyBitmap);
			}
			mActiveImageRequest = false;
		}
	}

	@Override
	public void unloadResources() {
		super.unloadResources();
		/*
		 * Completely remove all resources associated with this sprite.
		 * This should only be called from the engine update thread.
		 */
		Utilities.Log(CandiConstants.APP_NAME, "ProxiTile", "Unloading resources: " + mCandiModel.getEntityProxy().label);

		if (mProgressSprite != null)
			mProgressSprite.removeResources();
		if (mPlaceholderSprite != null)
			mPlaceholderSprite.removeResources();
		if (mReflectionSprite != null)
			mReflectionSprite.removeResources();
		if (mBodySprite != null)
			mBodySprite.removeResources();

		if (mBodySprite != null) {
			boolean registeredTouchArea = mCandiPatchPresenter.getEngine().getScene().getTouchAreas().contains(mBodySprite);
			if (registeredTouchArea)
				mCandiPatchPresenter.getEngine().getScene().unregisterTouchArea(mBodySprite);
		}

		if (mReflectionTextureRegion != null)
			BufferObjectManager.getActiveInstance().unloadBufferObject(mReflectionTextureRegion.getTextureBuffer());
		if (mProgressTextureRegion != null)
			BufferObjectManager.getActiveInstance().unloadBufferObject(mProgressTextureRegion.getTextureBuffer());
		if (mBodyTextureRegion != null)
			BufferObjectManager.getActiveInstance().unloadBufferObject(mBodyTextureRegion.getTextureBuffer());

		if (mReflectionTexture != null)
			mCandiPatchPresenter.getEngine().getTextureManager().unloadTexture(mReflectionTexture);
		if (mBodyTexture != null)
			mCandiPatchPresenter.getEngine().getTextureManager().unloadTexture(mBodyTexture);
	}

	public void setTexturesLoadedListener(OnViewTexturesLoadedListener texturesLoadedListener) {
		mTexturesLoadedListener = texturesLoadedListener;
	}

	public void setSingleTapListener(OnCandiViewTouchListener listener) {
		mTouchListener = listener;
	}

	@Override
	public CandiModel getModel() {
		return mCandiModel;
	}

	public interface OnCandiViewTouchListener {

		void onCandiViewSingleTap(IView candiView);

		void onCandiViewDoubleTap(IView candiView);

		void onCandiViewLongPress(IView candiView);
	}

	public interface OnFetchTexturesListener {

		void onFetchEnd();
	}

	@Override
	public boolean onDoubleTap(MotionEvent e) {
		if (mTouchListener != null) {
			mTouchListener.onCandiViewDoubleTap(this);
			return true;
		}
		return false;
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
		if (mTouchListener != null) {
			mTouchListener.onCandiViewSingleTap(this);
			return true;
		}
		return false;
	}

	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		if (mTouchListener != null) {
			mTouchListener.onCandiViewLongPress(this);
		}
	}

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
