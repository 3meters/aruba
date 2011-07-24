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
import com.proxibase.aircandi.utils.BitmapTextureSource;
import com.proxibase.aircandi.utils.ImageManager;
import com.proxibase.aircandi.utils.Utilities;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest;
import com.proxibase.aircandi.utils.ImageManager.OnImageReadyListener;
import com.proxibase.sdk.android.util.UtilitiesUI;

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

	private boolean							mBodyOnly	= false;
	private float							mBodyTop	= CandiConstants.CANDI_VIEW_TITLE_HEIGHT;

	private GestureDetector					mGestureDetector;

	public CandiView(CandiModel candiModel, CandiPatchPresenter candiPatchPresenter) {
		super((BaseModel) candiModel, candiPatchPresenter);

		this.mCandiModel = candiModel;
	}

	@Override
	public void update(Observable observable, Object data) {
		super.update(observable, data);

		updateTouchArea(mCandiModel.isTouchAreaActive());

		if (mCandiModel.isBodyOnly() != this.mBodyOnly)
			showBodyOnly(mCandiModel.isBodyOnly(), true, 1);

		if (mCandiModel.isVisibleNext()) {
			Transition transition = this.mCandiModel.getTransition();
			if (transition == Transition.FadeIn)
				this.setAlpha(0);
			this.setVisible(true);
		}

		doModifiers();
	}

	@Override
	public void initialize() {
		super.initialize();

		mGestureDetector = new GestureDetector(mCandiPatchPresenter.mCandiActivity, this);
		makeProgressSprites();
		loadBodyTextureSources();
		construct();
	}

	private void construct() {

		// Body sprite
		if (mBodyTextureRegion != null)
			makeBodySprite();

		// Reflection
		if (mReflectionTextureRegion != null)
			makeReflectionSprite();

		// ZOrder sort
		this.sortChildren();
	}

	private void updateTouchArea(final boolean isTouchAreaActive) {
		mCandiPatchPresenter.getEngine().runOnUpdateThread(new Runnable() {

			@Override
			public void run() {
				if (mBodySprite == null)
					return;
				boolean registeredTouchArea = mCandiPatchPresenter.getEngine().getScene().getTouchAreas().contains(mBodySprite);
				if (registeredTouchArea && !isTouchAreaActive) {
					mCandiPatchPresenter.getEngine().getScene().unregisterTouchArea(mBodySprite);
				}
				else if (!registeredTouchArea && isTouchAreaActive) {
					mCandiPatchPresenter.getEngine().getScene().registerTouchArea(mBodySprite);
				}
			}
		});
	}

	private void makeProgressSprites() {

		// Placeholder
		mPlaceholderTextureRegion = mCandiPatchPresenter.mPlaceholderTextureRegion.clone();
		mPlaceholderSprite = new CandiSprite(0, mBodyTop, mPlaceholderTextureRegion);
		mPlaceholderSprite.setColor(.9f, .7f, .2f);
		mPlaceholderSprite.setBlendFunction(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);
		mPlaceholderSprite.setAlpha(0);
		mPlaceholderSprite.setVisible(false);
		mPlaceholderSprite.setZIndex(-10);
		this.attachChild(mPlaceholderSprite);

		// Progress
		mProgressTextureRegion = mCandiPatchPresenter.mProgressTextureRegion.clone();
		mProgressSprite = new CandiAnimatedSprite((mPlaceholderSprite.getWidth() - mProgressTextureRegion.getTileWidth()) * 0.5f,
				((mPlaceholderSprite.getHeight() + mBodyTop) - mProgressTextureRegion.getTileHeight()) * 0.5f, mProgressTextureRegion);
		mProgressSprite.setAlpha(0);
		mProgressSprite.animate(150, true);
		mProgressSprite.setVisible(false);
		mProgressSprite.setZIndex(-9);
		this.attachChild(mProgressSprite);
	}

	private void makeReflectionSprite() {
		mReflectionSprite = new CandiSprite(0, mBodyTop + CandiConstants.CANDI_VIEW_BODY_HEIGHT, mReflectionTextureRegion);
		mReflectionSprite.setBlendFunction(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);
		mReflectionSprite.setAlpha(0);
		mReflectionSprite.setZIndex(0);
		this.attachChild(mReflectionSprite);
	}

	private void makeBodySprite() {
		mBodySprite = new CandiSprite(0, mBodyTop, mBodyTextureRegion) {

			@Override
			public boolean onAreaTouched(final TouchEvent sceneTouchEvent, final float touchAreaLocalX, final float touchAreaLocalY) {
				return mGestureDetector.onTouchEvent(sceneTouchEvent.getMotionEvent());
			}

		};
		mBodySprite.setBlendFunction(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);
		mBodySprite.setAlpha(0);
		mBodySprite.setZIndex(0);
		this.attachChild(mBodySprite);
		updateTouchArea(true);
	}

	private void updateBodySprite(Bitmap bitmap) {

		if (bitmap == null)
			throw new IllegalArgumentException();

		Bitmap bitmapCopy = bitmap.copy(Config.ARGB_8888, false); // Bitmap gets recycled once loaded to texture
		if (bitmapCopy != null) {
			mBodyTexture.clearTextureSources();
			mBodyTextureRegion = TextureRegionFactory.createFromSource(mBodyTexture, new BitmapTextureSource(bitmapCopy), 0, 0);
			if (mBodySprite == null)
				makeBodySprite();
		}

		Bitmap reflectionBitmap = ImageManager.getImageManager().getImage(mCandiModel.getBodyImageId() + ".reflection");

		if (reflectionBitmap == null) {
			Bitmap sourceBitmap = ImageManager.getImageManager().getImage(mCandiModel.getBodyImageId());
			if (sourceBitmap != null) {
				reflectionBitmap = UtilitiesUI.getReflection(sourceBitmap);
				ImageManager.getImageManager().getImageCache().put(mCandiModel.getBodyImageId() + ".reflection", reflectionBitmap);
			}
		}
		if (reflectionBitmap != null) {
			Bitmap reflectionBitmapCopy = reflectionBitmap.copy(Config.ARGB_8888, false); // Prevent recycle
			mReflectionTextureRegion = TextureRegionFactory.createFromSource(mReflectionTexture, new BitmapTextureSource(reflectionBitmapCopy), 0, 0);
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
			IEntityModifier modifier = mCandiModel.getModifiers().pop();
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
			this.registerEntityModifier(modifier);
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
		mPlaceholderSprite.setVisible(true);
		mPlaceholderSprite.registerEntityModifier(new AlphaModifier(0.5f, 0.0f, 1.0f));
		showProgress();
	}

	public void showBodyOnly(final boolean showBodyOnly, boolean animate, float duration) {

		mBodyOnly = showBodyOnly;
		boolean okToAnimate = (animate && this.mCandiModel.isVisibleNext());

		if (showBodyOnly) {
			/*
			 * Show body only
			 */
			mBodyTop = 0;
			if (okToAnimate) {
				mTitleSprite.registerEntityModifier(new AlphaModifier(duration, 1.0f, 0.0f));
			}
			else {
				mTitleSprite.setAlpha(0);
			}

			if (mBodySprite != null) {
				if (okToAnimate) {
					mBodySprite.registerEntityModifier(new MoveYModifier(duration, CandiConstants.CANDI_VIEW_TITLE_HEIGHT, mBodyTop));
				}
				else {
					mBodySprite.setPosition(0, mBodyTop);
				}

				if (mReflectionSprite != null) {
					Position position = this.mCandiModel.getZoneNext().getChildPositionNext(this.mCandiModel);
					if (okToAnimate) {
						if (!position.rowLast) {
							mReflectionSprite.registerEntityModifier(new SequenceEntityModifier(new AlphaModifier(0.5f, 1.0f, 0f), new MoveYModifier(
									0.1f, CandiConstants.CANDI_VIEW_BODY_HEIGHT + CandiConstants.CANDI_VIEW_TITLE_HEIGHT,
									CandiConstants.CANDI_VIEW_BODY_HEIGHT)));
						}
						else {
							mReflectionSprite.registerEntityModifier(new SequenceEntityModifier(new DelayModifier(0.5f), new MoveYModifier(0.1f,
									CandiConstants.CANDI_VIEW_BODY_HEIGHT + CandiConstants.CANDI_VIEW_TITLE_HEIGHT,
									CandiConstants.CANDI_VIEW_BODY_HEIGHT)));
						}
					}
					else {
						mReflectionSprite.setPosition(0, CandiConstants.CANDI_VIEW_BODY_HEIGHT);
						if (!position.rowLast)
							mReflectionSprite.setVisible(false);
					}
				}
			}
			if (mPlaceholderSprite != null) {
				if (okToAnimate) {
					mPlaceholderSprite.registerEntityModifier(new MoveYModifier(duration, CandiConstants.CANDI_VIEW_TITLE_HEIGHT, mBodyTop));
				}
				else {
					mPlaceholderSprite.setPosition(0, mBodyTop);
					mProgressSprite.setPosition((mPlaceholderSprite.getWidth() - mProgressTextureRegion.getTileWidth()) * 0.5f, (mPlaceholderSprite
							.getHeight() - mProgressTextureRegion.getTileHeight()) * 0.5f);
				}
			}
		}
		else {
			/*
			 * Show body, title and reflection
			 */
			mBodyTop = CandiConstants.CANDI_VIEW_TITLE_HEIGHT;

			if (okToAnimate) {
				mTitleSprite.registerEntityModifier(new AlphaModifier(duration, 0.0f, 1.0f));
			}
			else {
				// mTitleSprite.setAlpha(1);
			}

			if (mBodySprite != null) {

				if (okToAnimate) {
					mBodySprite.registerEntityModifier(new MoveYModifier(duration, 0, mBodyTop));
				}
				else {
					mBodySprite.setPosition(0, mBodyTop);
				}

				if (mReflectionSprite != null) {
					Position position = this.mCandiModel.getZoneCurrent().getChildPositionCurrent(this.mCandiModel);
					if (okToAnimate) {
						if (!position.rowLast) {
							mReflectionSprite.registerEntityModifier(new AlphaModifier(duration, 0.0f, 1.0f));
							mReflectionSprite.setPosition(0, mBodyTop + CandiConstants.CANDI_VIEW_BODY_HEIGHT);
						}
						else {
							mReflectionSprite.registerEntityModifier(new SequenceEntityModifier(new MoveYModifier(duration * 0.5f,
									CandiConstants.CANDI_VIEW_BODY_HEIGHT,
									CandiConstants.CANDI_VIEW_BODY_HEIGHT + CandiConstants.CANDI_VIEW_TITLE_HEIGHT)));
						}
					}
					else
						mReflectionSprite.setPosition(0, mBodyTop + CandiConstants.CANDI_VIEW_BODY_HEIGHT);
					mReflectionSprite.setVisible(true);
				}
			}
			if (mPlaceholderSprite != null) {

				if (okToAnimate) {
					mPlaceholderSprite.registerEntityModifier(new MoveYModifier(duration, 0, mBodyTop));
				}
				else {
					mPlaceholderSprite.setPosition(0, mBodyTop);
					mProgressSprite.setPosition((mPlaceholderSprite.getWidth() - mProgressTextureRegion.getTileWidth()) * 0.5f, ((mPlaceholderSprite
							.getHeight() + mBodyTop) - mProgressTextureRegion.getTileHeight()) * 0.5f);
				}
			}
		}
	}

	private void swapImageForPlaceholder() {
		hideProgress();
		mPlaceholderSprite.registerEntityModifier(new AlphaModifier(0.5f, 1.0f, 0.0f));
		mBodySprite.registerEntityModifier(new AlphaModifier(0.5f, 0.0f, 1.0f));
		mReflectionSprite.registerEntityModifier(new AlphaModifier(0.5f, 0.0f, 1.0f));
	}

	@Override
	public void resetTextures() {
		super.resetTextures();

		mBodyTexture.clearTextureSources();
		mReflectionTexture.clearTextureSources();

		Position position = this.mCandiModel.getZoneCurrent().getChildPositionCurrent(this.mCandiModel);
		if (!position.rowLast) {
			mReflectionSprite.setAlpha(0);
		}

		mPlaceholderTextureRegion = mCandiPatchPresenter.mPlaceholderTextureRegion.clone();
		mProgressTextureRegion = mCandiPatchPresenter.mProgressTextureRegion.clone();
		loadBodyTextureSources();
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

	protected void loadBodyTextureSources() {
		if (!ImageManager.getImageManager().hasImage(mCandiModel.getBodyImageId())) {

			Utilities.Log(CandiConstants.APP_NAME, "CandiView", "Image cache miss - start network fetch: " + mCandiModel.getBodyImageId());
			ImageRequest imageRequest = new ImageManager.ImageRequest();
			imageRequest.imageId = mCandiModel.getBodyImageId();
			imageRequest.imageUrl = mCandiModel.getBodyImageUrl();
			imageRequest.imageShape = "square";
			imageRequest.widthMinimum = 250;
			imageRequest.showReflection = true;
			imageRequest.imageReadyListener = new OnImageReadyListener() {

				@Override
				public void onImageReady(Bitmap bitmap) {

					Utilities.Log(CandiConstants.APP_NAME, "CandiView", "Image fetched: " + mCandiModel.getBodyImageId());
					updateBodySprite(bitmap);
					swapImageForPlaceholder();
				}
			};

			showPlaceholder();
			ImageManager.getImageManager().fetchImageAsynch(imageRequest);
		}
		else {

			Utilities.Log(CandiConstants.APP_NAME, "CandiView", "Image cache hit: " + mCandiModel.getBodyImageId());
			Bitmap bitmap = ImageManager.getImageManager().getImage(mCandiModel.getBodyImageId());

			if (bitmap != null) {
				Bitmap bitmapCopy = bitmap.copy(Config.ARGB_8888, false); // Prevent recycle of cached bitmap
				mBodyTextureRegion = TextureRegionFactory.createFromSource(mBodyTexture, new BitmapTextureSource(bitmapCopy), 0, 0);
			}

			// Reflection
			Bitmap reflectionBitmap = ImageManager.getImageManager().getImage(mCandiModel.getBodyImageId() + ".reflection");
			if (reflectionBitmap == null) {
				Bitmap sourceBitmap = ImageManager.getImageManager().getImage(mCandiModel.getBodyImageId());
				if (sourceBitmap != null) {
					reflectionBitmap = UtilitiesUI.getReflection(sourceBitmap);
					ImageManager.getImageManager().getImageCache().put(mCandiModel.getBodyImageId() + ".reflection", reflectionBitmap);
				}
			}

			if (reflectionBitmap != null) {
				reflectionBitmap = reflectionBitmap.copy(Config.ARGB_8888, false); // Prevent recycle
				mReflectionTextureRegion = TextureRegionFactory.createFromSource(mReflectionTexture, new BitmapTextureSource(reflectionBitmap), 0, 0);
			}
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
		this.mTexturesLoadedListener = texturesLoadedListener;
	}

	public void setSingleTapListener(OnCandiViewTouchListener listener) {
		mTouchListener = listener;
	}

	@Override
	public CandiModel getModel() {
		return this.mCandiModel;
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
