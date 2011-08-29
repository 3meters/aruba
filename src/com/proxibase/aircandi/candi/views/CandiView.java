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

		this.mCandiModel = candiModel;
	}

	@Override
	public void update(Observable observable, Object data) {
		super.update(observable, data);

		updateTouchArea(mCandiModel.isTouchAreaActive());

		if (mCandiModel.isBodyOnly() != this.mCollapsed) {
			if (mCandiModel.isBodyOnly())
				this.showCollapsedAnimated(CandiConstants.DURATION_CANDIBODY_COLLAPSE);
			else
				this.showExpandedAnimated(CandiConstants.DURATION_CANDIBODY_EXPAND);
		}

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
		mPlaceholderSprite = new CandiSprite(0, CandiConstants.CANDI_VIEW_TITLE_HEIGHT, mPlaceholderTextureRegion);
		mPlaceholderSprite.setBlendFunction(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);
		mPlaceholderSprite.setAlpha(0);
		mPlaceholderSprite.setVisible(false);
		mPlaceholderSprite.setZIndex(-10);
		this.attachChild(mPlaceholderSprite);

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
		this.attachChild(mProgressSprite);
	}

	private void makeReflectionSprite() {
		mReflectionSprite = new CandiSprite(0, CandiConstants.CANDI_VIEW_TITLE_HEIGHT + CandiConstants.CANDI_VIEW_BODY_HEIGHT,
				mReflectionTextureRegion);
		mReflectionSprite.setBlendFunction(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);
		mReflectionSprite.setAlpha(0);
		mReflectionSprite.setVisible(false);
		mReflectionSprite.setZIndex(0);
		this.attachChild(mReflectionSprite);
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
		this.attachChild(mBodySprite);
		updateTouchArea(true);
	}

	private void updateBodySprite(Bitmap bodyBitmap) {

		Bitmap reflectionBitmap = ImageManager.getInstance().getImage(mCandiModel.getBodyImageId() + ".reflection");
		if (reflectionBitmap == null) {
			if (bodyBitmap != null) {
				reflectionBitmap = AircandiUI.getReflection(bodyBitmap);
				ImageManager.getInstance().getImageCache().put(mCandiModel.getBodyImageId() + ".reflection", reflectionBitmap);
			}
		}

		mBodyTexture.clearTextureSources();
		mBodyTextureRegion = TextureRegionFactory.createFromSource(mBodyTexture, new BitmapTextureSource(bodyBitmap, new IBitmapAdapter() {

			@Override
			public Bitmap reloadBitmap() {
				/*
				 * TextureSource needs to refresh a recycled bitmap
				 */
				Utilities.Log(CandiConstants.APP_NAME, "CandiView",
						"Andengine reloading body texture to hardware: " + mCandiModel.getEntityProxy().title);
				Bitmap bitmap = ImageManager.getInstance().getImage(mCandiModel.getBodyImageId());
				if (bitmap != null) {
					return bitmap;
				}
				else {
					loadBodyTextureSources();
					// This is where we should go restore the image to the cache by downloading it
					return null;
				}
			}
		}), 0, 0);

		if (mBodySprite == null)
			makeBodySprite();

		if (reflectionBitmap != null) {
			mReflectionTexture.clearTextureSources();
			mReflectionTextureRegion = TextureRegionFactory.createFromSource(mReflectionTexture, new BitmapTextureSource(reflectionBitmap,
					new IBitmapAdapter() {

						@Override
						public Bitmap reloadBitmap() {
							/*
							 * TextureSource needs to refresh a recycled bitmap
							 */
							Utilities.Log(CandiConstants.APP_NAME, "CandiView", "Andengine reloading reflection texture to hardware: " + mCandiModel
																						.getEntityProxy().title);
							Bitmap bitmap = ImageManager.getInstance().getImage(mCandiModel.getBodyImageId() + ".reflection");
							if (bitmap != null) {
								return bitmap;
							}
							else {
								return null;
							}
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
		showProgress();
	}

	public void showCollapsed() {

		mTitleSprite.setAlpha(0);
		mTitleSprite.setVisible(false);
		if (mBodySprite != null) {
			mBodySprite.setPosition(0, 0);
			if (mReflectionSprite != null) {
				mReflectionSprite.setPosition(0, CandiConstants.CANDI_VIEW_BODY_HEIGHT);
				this.reflectionVisible(this.mCandiModel.getPositionNext().rowLast, false, 0);
			}
		}
		if (mPlaceholderSprite != null) {
			mPlaceholderSprite.setPosition(0, 0);
			float progressX = (mPlaceholderSprite.getWidth() - mProgressTextureRegion.getTileWidth()) * 0.5f;
			float progressY = (mPlaceholderSprite.getHeight() - mProgressTextureRegion.getTileHeight()) * 0.5f;
			mProgressSprite.setPosition(progressX, progressY);
		}
		this.mCollapsed = true;
	}

	public void showExpanded() {

		mTitleSprite.setAlpha(1);
		mTitleSprite.setVisible(true);
		if (mBodySprite != null) {
			mBodySprite.setPosition(0, CandiConstants.CANDI_VIEW_TITLE_HEIGHT);
			if (mReflectionSprite != null) {
				mReflectionSprite.setPosition(0, CandiConstants.CANDI_VIEW_BODY_HEIGHT + CandiConstants.CANDI_VIEW_TITLE_HEIGHT);
				this.reflectionVisible(this.mCandiModel.getPositionNext().rowLast, false, 0);
			}
		}
		if (mPlaceholderSprite != null) {
			mPlaceholderSprite.setPosition(0, CandiConstants.CANDI_VIEW_TITLE_HEIGHT);
			float progressX = (mPlaceholderSprite.getWidth() - mProgressTextureRegion.getTileWidth()) * 0.5f;
			float progressY = ((mPlaceholderSprite.getHeight() + CandiConstants.CANDI_VIEW_TITLE_HEIGHT) - mProgressTextureRegion.getTileHeight()) * 0.5f;
			mProgressSprite.setPosition(progressX, progressY);
		}
		this.mCollapsed = false;
	}

	public void showCollapsedAnimated(float duration) {
		if (!this.mCandiModel.isVisibleNext())
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
				this.reflectionVisible(this.mCandiModel.getPositionNext().rowLast, true, 1);
				if (!this.mCandiModel.getPositionNext().rowLast) {
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
		this.mCollapsed = true;
	}

	public void showExpandedAnimated(float duration) {
		if (!this.mCandiModel.isVisibleNext())
			showExpanded();

		mTitleSprite.setVisible(true);
		mTitleSprite.registerEntityModifier(new AlphaModifier(duration, 0.0f, 1.0f));

		if (mBodySprite != null) {
			mBodySprite.registerEntityModifier(new MoveYModifier(duration, 0, CandiConstants.CANDI_VIEW_TITLE_HEIGHT));
			if (mReflectionSprite != null) {
				this.reflectionVisible(this.mCandiModel.getPositionNext().rowLast, true, 1);
				if (this.mCandiModel.getPositionNext().rowLast) {
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
		this.mCollapsed = false;
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

		if (!ImageManager.getInstance().hasImage(mCandiModel.getBodyImageId())) {
			Utilities.Log(CandiConstants.APP_NAME, "CandiView", mCandiModel.getEntityProxy().label + ": "
																+ "mActiveImageRequest"
																+ String.valueOf(mActiveImageRequest));
			if (!mActiveImageRequest) {
				mActiveImageRequest = true;
				ImageRequest imageRequest = ImageManager.createImageRequest(mCandiModel.getBodyImageUri(), mCandiModel.getBodyImageFormat(),
						"square", 250, true, new IImageReadyListener() {

							@Override
							public void onImageReady(Bitmap bitmap) {
								Utilities.Log(CandiConstants.APP_NAME, "CandiView", "Image fetched: " + mCandiModel.getBodyImageId());
								updateBodySprite(bitmap);
								if (mCollapsed) {
									showCollapsed();
									mBodySprite.setVisible(true);
								}
								else {
									showExpanded();
									mBodySprite.setVisible(true);
								}
								swapImageForPlaceholder();
								mActiveImageRequest = false;
							}
						});

				if (mReflectionSprite != null)
					mReflectionSprite.setVisible(false);
				if (mBodySprite != null)
					mBodySprite.setVisible(false);
				showPlaceholder();
				Utilities.Log(CandiConstants.APP_NAME, "CandiView", "Image fetch started: " + mCandiModel.getBodyImageId());
				ImageManager.getInstance().fetchImageAsynch(imageRequest);
			}
		}
		else {
			/*
			 * Getting an image from the cache will pull it into the L1 memory cache even if it was initially found
			 * in the L2 file cache. If it gets recycled
			 */
			Utilities.Log(CandiConstants.APP_NAME, "CandiView", mCandiModel.getEntityProxy().label + ": " + "cache hit");
			Bitmap bodyBitmap = ImageManager.getInstance().getImage(mCandiModel.getBodyImageId());
			if (bodyBitmap != null) {

				Bitmap reflectionBitmap = ImageManager.getInstance().getImage(mCandiModel.getBodyImageId() + ".reflection");
				if (reflectionBitmap == null) {
					if (bodyBitmap != null) {
						reflectionBitmap = AircandiUI.getReflection(bodyBitmap);
						ImageManager.getInstance().getImageCache().put(mCandiModel.getBodyImageId() + ".reflection", reflectionBitmap);
					}
				}

				mBodyTextureRegion = TextureRegionFactory.createFromSource(mBodyTexture, new BitmapTextureSource(bodyBitmap, new IBitmapAdapter() {

					@Override
					public Bitmap reloadBitmap() {
						/*
						 * TextureSource needs to refresh a recycled bitmap
						 */
						Bitmap bitmap = ImageManager.getInstance().getImage(mCandiModel.getBodyImageId());
						if (bitmap != null) {
							Utilities.Log(CandiConstants.APP_NAME, "CandiView",
									"Andengine reload: bitmap found: " + mCandiModel.getEntityProxy().title);
							return bitmap;
						}
						else {
							/*
							 * Our cache has been cleared either by the system, by the app, or by the user so we need
							 * to refetch the image from the service. This call will take care of the reflection too
							 * as a byproduct.
							 */
							Utilities.Log(CandiConstants.APP_NAME, "CandiView", "Andengine reload: bitmap gone so refetch: " + mCandiModel
																						.getEntityProxy().title);
							loadBodyTextureSources();
							return null;
						}
					}
				}), 0, 0);

				if (reflectionBitmap != null) {
					mReflectionTextureRegion = TextureRegionFactory.createFromSource(mReflectionTexture, new BitmapTextureSource(reflectionBitmap,
							new IBitmapAdapter() {

								@Override
								public Bitmap reloadBitmap() {
									/*
									 * TextureSource needs to refresh a recycled bitmap
									 */
									Bitmap bitmap = ImageManager.getInstance().getImage(mCandiModel.getBodyImageId() + ".reflection");
									if (bitmap != null) {
										return bitmap;
									}
									return null;
								}
							}), 0, 0);

				}
			}
		}
	}

	public void refreshBodyTextureSources() {
		/*
		 * Call this when you want to replace the image we have cached
		 * with a fresh version.
		 */
		if (!mActiveImageRequest) {
			mActiveImageRequest = true;
			ImageRequest imageRequest = new ImageManager.ImageRequest();
			imageRequest.imageId = mCandiModel.getBodyImageId();
			imageRequest.imageUri = mCandiModel.getBodyImageUri();
			imageRequest.imageFormat = mCandiModel.getBodyImageFormat();
			imageRequest.imageShape = "square";
			imageRequest.widthMinimum = 250;
			imageRequest.showReflection = true;
			imageRequest.imageReadyListener = new IImageReadyListener() {

				@Override
				public void onImageReady(final Bitmap bitmap) {
					Utilities.Log(CandiConstants.APP_NAME, "CandiView", "Image refreshed: " + mCandiModel.getBodyImageId());
					updateBodySprite(bitmap);
					mActiveImageRequest = false;
				}
			};
			ImageManager.getInstance().fetchImageAsynch(imageRequest);
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
