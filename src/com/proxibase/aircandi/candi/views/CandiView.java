package com.proxibase.aircandi.candi.views;

import java.util.ArrayList;
import java.util.Observable;

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
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.opengl.texture.region.TextureRegionFactory;
import org.anddev.andengine.util.modifier.IModifier;
import org.anddev.andengine.util.modifier.IModifier.IModifierListener;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.GestureDetector.OnGestureListener;

import com.proxibase.aircandi.candi.models.BaseModel;
import com.proxibase.aircandi.candi.models.CandiModel;
import com.proxibase.aircandi.candi.models.ZoneModel;
import com.proxibase.aircandi.candi.models.BaseModel.UpdateType;
import com.proxibase.aircandi.candi.models.BaseModel.ViewState;
import com.proxibase.aircandi.candi.models.CandiModel.Transition;
import com.proxibase.aircandi.candi.models.ZoneModel.ZoneAlignment;
import com.proxibase.aircandi.candi.models.ZoneModel.ZoneStatus;
import com.proxibase.aircandi.candi.modifiers.CandiAlphaModifier;
import com.proxibase.aircandi.candi.presenters.CandiPatchPresenter;
import com.proxibase.aircandi.candi.sprites.CandiSprite;
import com.proxibase.aircandi.core.AircandiException;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.utils.BitmapTextureSource;
import com.proxibase.aircandi.utils.ImageManager;
import com.proxibase.aircandi.utils.ImageUtils;
import com.proxibase.aircandi.utils.Utilities;
import com.proxibase.aircandi.utils.BitmapTextureSource.IBitmapAdapter;
import com.proxibase.aircandi.utils.ImageManager.IImageRequestListener;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest.ImageShape;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException.ProxiErrorCode;

@SuppressWarnings("unused")
public class CandiView extends BaseView implements OnGestureListener {

	private Texture				mBodyTexture;
	private TextureRegion		mBodyTextureRegion;
	private CandiSprite			mBodySprite;

	private Texture				mReflectionTexture;
	private TextureRegion		mReflectionTextureRegion;
	private CandiSprite			mReflectionSprite;

	private GestureDetector		mGestureDetector;
	private ViewTouchListener	mTouchListener;
	private boolean				mCollapsed						= false;
	private Boolean				mTextureIsLoadedToHardware		= false;
	private Boolean				mTextureUpdateOnHardwareNeeded	= false;
	private boolean				mReflectionActive				= true;
	private boolean				mActiveImageRequest				= false;

	// --------------------------------------------------------------------------------------------
	// Initialization
	// --------------------------------------------------------------------------------------------

	public CandiView() {
		this(null, null);
	}

	public CandiView(CandiPatchPresenter candiPatchPresenter) {
		this(null, candiPatchPresenter);
	}

	public CandiView(Object model, CandiPatchPresenter candiPatchPresenter) {
		super(model, candiPatchPresenter);
		this.setVisible(false);
		this.setAlpha(0);
	}

	@Override
	public void initialize() {
		if (mModel == null) {
			throw new IllegalStateException("Must set the model before initializing");
		}
		super.initialize();

		updateTextureSources(false);
	}

	@Override
	public void initializeModel() {
		if (mModel == null) {
			throw new IllegalStateException("Must set the model before initializing");
		}
		super.initializeModel();

		updateTextureSources(false);
	}

	// --------------------------------------------------------------------------------------------
	// Primary
	// --------------------------------------------------------------------------------------------

	@Override
	public void update(Observable observable, Object data) {
		super.update(observable, data);
		/**
		 * Pass EntityType.Local when view needs updating independent of
		 * the model it is observing. An example is image processing.
		 */
		/*
		 * TODO: This may be getting called more than we want while doing
		 * a partial refresh (new beacons only).
		 */

		UpdateType updateType = (UpdateType) data;
		CandiModel candiModel = (CandiModel) this.mModel;

		if (candiModel.isBodyTextureSourcesDirty()) {
			updateTextureSources(true);
			candiModel.setBodyTextureSourcesDirty(false);
		}

		if (updateType == UpdateType.Local) {
			return;
		}

		if (!CandiConstants.TRANSITIONS_ACTIVE) {
			ViewState viewStateNext = candiModel.getViewStateNext();
			/*
			 * Positioning and scale
			 */
			setZIndex(viewStateNext.getZIndex());
			setPosition(viewStateNext.getX(), viewStateNext.getY());
			setScale(viewStateNext.getScale());
			/*
			 * Configuration
			 */
			configureCollapsed(viewStateNext.isCollapsed());
			mTitleSprite.setVisible(!viewStateNext.isCollapsed());
			mTitleSprite.setAlpha(viewStateNext.isCollapsed() ? 0 : 1);
			/*
			 * Visibility
			 */
			if (this.mReflectionActive != viewStateNext.hasReflection()) {
				showReflection(viewStateNext.hasReflection());
			}
			updateTouchArea(candiModel.isTouchAreaActive());
			setVisible(viewStateNext.isVisible());
			if (updateType == UpdateType.Reuse) {
				if (mBodySprite != null) {
					mBodySprite.setAlpha(0);
					if (mReflectionSprite != null) {
						mReflectionSprite.setAlpha(0);
					}
				}
			}
			else {
				setAlpha(viewStateNext.isVisible() ? 1 : 0);
			}
		}
		else {

			ViewState viewStateNext = candiModel.getViewStateNext();

			updateTouchArea(candiModel.isTouchAreaActive());
			clearEntityModifiers();

			if (this.mZIndex != candiModel.getViewStateNext().getZIndex()) {
				this.mZIndex = (int) candiModel.getViewStateNext().getZIndex();
			}

			if (this.mReflectionActive != viewStateNext.hasReflection()) {
				this.showReflectionAnimated(viewStateNext.hasReflection(), CandiConstants.DURATION_TRANSITIONS_FADE);
			}

			if (viewStateNext.isCollapsed() != mCollapsed) {
				configureCollapsedAnimated(viewStateNext.isCollapsed(), CandiConstants.DURATION_CANDIBODY_COLLAPSE);
			}

			if (this.mScaleX != candiModel.getViewStateNext().getScale())
				this.setScale(candiModel.getViewStateNext().getScale());

			if (this.mX != candiModel.getViewStateNext().getX() || this.mY != candiModel.getViewStateNext().getY())
				this.setPosition(candiModel.getViewStateNext().getX(), candiModel.getViewStateNext().getY());

			if (candiModel.getViewStateNext().isVisible()) {
				Transition transition = candiModel.getTransition();
				if (transition == Transition.FadeIn)
					setAlpha(0);
				setVisible(true);
			}

			doEntityModifiers();
		}
	}

	@Override
	public void reset() {
		setVisible(false);
		setAlpha(0);
		setPosition(0, 1000); // Offscreen
		setScale(1);

		updateTouchArea(false);
		clearEntityModifiers();
		configureCollapsed(false);
		mTitleSprite.setVisible(true);

		mBound = false;
		mModel = null;
		mActiveImageRequest = false;
	}

	// --------------------------------------------------------------------------------------------
	// UI
	// --------------------------------------------------------------------------------------------

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);

		if (!visible) {
			for (int i = 0; i < getChildCount(); i++) {
				getChild(i).setVisible(false);
			}
		}
		else {
			if (!mCollapsed) {
				if (mTitleSprite != null) {
					mTitleSprite.setVisible(true);
				}
			}
			if (!mBound) {
				mPlaceholderSprite.setVisible(true);
			}
			else if (mActiveImageRequest) {
				mPlaceholderSprite.setVisible(true);
				mProgressSprite.setVisible(true);
			}
			else {
				if (mBodySprite != null) {
					mBodySprite.setVisible(true);
					if (mReflectionActive) {
						if (mReflectionSprite != null) {
							mReflectionSprite.setVisible(true);
						}
					}
				}
			}
		}
	}

	@Override
	public void setPosition(float pX, float pY) {
		super.setPosition(pX, pY);
	}

	@Override
	public void setZIndex(final int pZIndex) {
		super.setZIndex(pZIndex);
		if (this.mBodySprite != null)
			this.mBodySprite.setZIndex(pZIndex);
		if (this.mReflectionSprite != null)
			this.mReflectionSprite.setZIndex(pZIndex);
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

	public void configureCollapsed(boolean collapsed) {

		if (collapsed) {
			// // Visibility
			// if (this.isVisible()) {
			// if (mTitleSprite != null) {
			// mTitleSprite.setVisible(false);
			// mTitleSprite.setAlpha(0);
			// }
			// }

			// Positioning
			if (mBodySprite != null) {
				mBodySprite.setPosition(0, 0);
				if (mReflectionSprite != null) {
					mReflectionSprite.setPosition(0, CandiConstants.CANDI_VIEW_BODY_HEIGHT);
				}
			}
			if (mPlaceholderSprite != null) {
				mPlaceholderSprite.setPosition(0, 0);
				float progressX = (mPlaceholderSprite.getWidth() - mProgressTextureRegion.getTileWidth()) * 0.5f;
				float progressY = (mPlaceholderSprite.getHeight() - mProgressTextureRegion.getTileHeight()) * 0.5f;
				mProgressSprite.setPosition(progressX, progressY);
			}
		}
		else {
			// // Visibility
			// if (this.isVisible()) {
			// if (mTitleSprite != null) {
			// mTitleSprite.setVisible(true);
			// mTitleSprite.setAlpha(1);
			// }
			// }

			// Positioning
			if (mBodySprite != null) {
				mBodySprite.setPosition(0, CandiConstants.CANDI_VIEW_TITLE_HEIGHT);
				if (mReflectionSprite != null) {
					mReflectionSprite.setPosition(0, CandiConstants.CANDI_VIEW_BODY_HEIGHT + CandiConstants.CANDI_VIEW_TITLE_HEIGHT);
				}
			}
			if (mPlaceholderSprite != null) {
				mPlaceholderSprite.setPosition(0, CandiConstants.CANDI_VIEW_TITLE_HEIGHT);
				float progressX = (mPlaceholderSprite.getWidth() - mProgressTextureRegion.getTileWidth()) * 0.5f;
				float progressY = ((mPlaceholderSprite.getHeight() + CandiConstants.CANDI_VIEW_TITLE_HEIGHT) - mProgressTextureRegion.getTileHeight()) * 0.5f;
				mProgressSprite.setPosition(progressX, progressY);
			}
		}

		mCollapsed = collapsed;
	}

	public void showReflection(boolean visible) {
		if (mReflectionSprite == null)
			return;

		mReflectionSprite.setVisible(visible);
		mReflectionSprite.setAlpha(visible ? 1 : 0);
		mReflectionActive = visible;
	}

	private void showPlaceholder() {
		if (mReflectionSprite != null)
			mReflectionSprite.setVisible(false);
		if (mBodySprite != null)
			mBodySprite.setVisible(false);
		mPlaceholderSprite.setVisible(true);
		progressVisible(true);
	}

	// --------------------------------------------------------------------------------------------
	// Animation
	// --------------------------------------------------------------------------------------------

	private void doEntityModifiers() {
		final CandiModel candiModel = (CandiModel) this.mModel;
		if (!candiModel.getModifiers().isEmpty()) {
			final IEntityModifier modifier = candiModel.getModifiers().removeFirst();
			modifier.addModifierListener(new IModifierListener<IEntity>() {

				@Override
				public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
					if (modifier instanceof CandiAlphaModifier) {
						if (((CandiAlphaModifier) modifier).getToAlpha() == 0)
							setVisible(false);
					}
					doEntityModifiers();
				}

				@Override
				public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {}
			});
			registerEntityModifier(modifier);
		}
	}

	@Override
	public void clearEntityModifiers() {
		super.clearEntityModifiers();
		if (mTitleSprite != null)
			mTitleSprite.clearEntityModifiers();
		if (mBodySprite != null)
			mBodySprite.clearEntityModifiers();
		if (mReflectionSprite != null)
			mReflectionSprite.clearEntityModifiers();
		if (mPlaceholderSprite != null)
			mPlaceholderSprite.clearEntityModifiers();
	}

	public void configureCollapsedAnimated(boolean collapsed, float duration) {
		final CandiModel candiModel = (CandiModel) this.mModel;

		if (collapsed) {

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
					showReflectionAnimated(mReflectionActive, 1);
					if (candiModel.getZoneStateNext().getAlignment() != ZoneAlignment.Bottom) {
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
				float progressFromY = ((mPlaceholderSprite.getHeight() + CandiConstants.CANDI_VIEW_TITLE_HEIGHT) - mProgressTextureRegion
						.getTileHeight()) * 0.5f;
				mProgressSprite.registerEntityModifier(new MoveYModifier(duration, progressFromY, progressToY));
			}
		}
		else {
			mTitleSprite.setVisible(true);
			mTitleSprite.registerEntityModifier(new AlphaModifier(duration, 0.0f, 1.0f));

			if (mBodySprite != null) {
				mBodySprite.registerEntityModifier(new MoveYModifier(duration, 0, CandiConstants.CANDI_VIEW_TITLE_HEIGHT));
				if (mReflectionSprite != null) {
					showReflectionAnimated(mReflectionActive, 1);
					if (candiModel.getZoneStateNext().getAlignment() != ZoneAlignment.Bottom) {
						mReflectionSprite.registerEntityModifier(new SequenceEntityModifier(
								new MoveYModifier(duration * 0.5f, CandiConstants.CANDI_VIEW_BODY_HEIGHT,
										CandiConstants.CANDI_VIEW_BODY_HEIGHT + CandiConstants.CANDI_VIEW_TITLE_HEIGHT)));
					}
				}
			}
			if (mPlaceholderSprite != null) {
				mPlaceholderSprite.registerEntityModifier(new MoveYModifier(duration, 0, CandiConstants.CANDI_VIEW_TITLE_HEIGHT));
				float progressFromY = (mPlaceholderSprite.getHeight() - mProgressTextureRegion.getTileHeight()) * 0.5f;
				float progressToY = ((mPlaceholderSprite.getHeight() + CandiConstants.CANDI_VIEW_TITLE_HEIGHT) - mProgressTextureRegion
						.getTileHeight()) * 0.5f;
				mProgressSprite.registerEntityModifier(new MoveYModifier(duration, progressFromY, progressToY));
			}
		}
		mCollapsed = collapsed;
	}

	public void showReflectionAnimated(boolean visible, float duration) {
		if (mReflectionSprite == null)
			return;

		mReflectionActive = visible;
		if (visible) {
			if (!mReflectionSprite.isVisible()) {
				mReflectionSprite.setVisible(true);
				mReflectionSprite.registerEntityModifier(new SequenceEntityModifier(new DelayModifier(duration * 0.1f), new AlphaModifier(
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

	private void swapImageForPlaceholderAnimated() {
		progressVisible(false);
		if (((BaseModel) mModel).getViewStateCurrent().isVisible()) {
			if (mPlaceholderSprite.isVisible()) {
				mPlaceholderSprite.registerEntityModifier(new AlphaModifier(CandiConstants.DURATION_PLACEHOLDER_HIDESHOW, 1.0f, 0.0f,
						new IEntityModifierListener() {

							@Override
							public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
								mPlaceholderSprite.setVisible(false);
							}

							@Override
							public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {}
						}));
			}

			mBodySprite.registerEntityModifier(new AlphaModifier(CandiConstants.DURATION_PLACEHOLDER_HIDESHOW, 0.0f, 1.0f));
			mBodySprite.setVisible(true);
			if (((BaseModel) mModel).getViewStateCurrent().hasReflection() && mReflectionSprite != null) {
				mReflectionSprite.registerEntityModifier(new AlphaModifier(CandiConstants.DURATION_PLACEHOLDER_HIDESHOW, 0.0f, 1.0f));
				mReflectionSprite.setVisible(true);
			}
		}
		else {
			mPlaceholderSprite.setVisible(false);
			mPlaceholderSprite.setAlpha(0);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Sprites
	// --------------------------------------------------------------------------------------------

	private void makeReflectionSprite() {
		CandiModel candiModel = (CandiModel) mModel;
		mReflectionSprite = new CandiSprite(0, CandiConstants.CANDI_VIEW_TITLE_HEIGHT + CandiConstants.CANDI_VIEW_BODY_HEIGHT,
				mReflectionTextureRegion);
		mReflectionSprite.setBlendFunction(CandiConstants.GL_BLEND_FUNCTION_SOURCE, CandiConstants.GL_BLEND_FUNCTION_DESTINATION);
		mReflectionSprite.setAlpha(0);
		mReflectionSprite.setVisible(false);
		mReflectionSprite.setZIndex(0);
		attachChild(mReflectionSprite);
		configureCollapsed(candiModel.getViewStateCurrent().isCollapsed());
	}

	private void makeBodySprite() {
		CandiModel candiModel = (CandiModel) mModel;
		mBodySprite = new CandiSprite(0, CandiConstants.CANDI_VIEW_TITLE_HEIGHT, mBodyTextureRegion);
		mBodySprite.setGestureDetector(mGestureDetector);
		mBodySprite.setBlendFunction(CandiConstants.GL_BLEND_FUNCTION_SOURCE, CandiConstants.GL_BLEND_FUNCTION_DESTINATION);
		mBodySprite.setAlpha(0);
		mBodySprite.setVisible(false);
		mBodySprite.setZIndex(0);
		attachChild(mBodySprite);
		configureCollapsed(candiModel.getViewStateCurrent().isCollapsed());
	}

	// --------------------------------------------------------------------------------------------
	// Setters/Getters
	// --------------------------------------------------------------------------------------------

	@Override
	public CandiModel getModel() {
		return (CandiModel) mModel;
	}

	// --------------------------------------------------------------------------------------------
	// Textures
	// --------------------------------------------------------------------------------------------
	public void updateTextureSources(final boolean skipCache) {

		final CandiModel candiModel = (CandiModel) this.mModel;

		progressVisible(true);
		if (!mActiveImageRequest) {
			/*
			 * TODO: We'd like to prioritize highest the candi views that are currently visible
			 * but position hasn't been assigned when this first gets called.
			 */
			mActiveImageRequest = true;
			ImageRequest imageRequest = new ImageRequest(candiModel.getBodyImageUri(), ImageShape.Square, candiModel.getBodyImageFormat(), 250, true,
					2, this, new IImageRequestListener() {

						@Override
						public void onImageReady(final Bitmap bodyBitmap) {
							/*
							 * Executes on the ViewManager thread (which has the lowest possible priority).
							 * First time for a candiview is more expensive because the body and reflection sprites
							 * are created.
							 */
							Utilities.Log(CandiConstants.APP_NAME, "CandiView", "Image fetched: " + candiModel.getBodyImageUri());
							if (bodyBitmap != null) {
								updateTextureRegions(bodyBitmap);
								swapImageForPlaceholderAnimated();
							}
							mActiveImageRequest = false;
						}

						@Override
						public void onProxibaseException(ProxibaseException exception) {
							mActiveImageRequest = false;
						}
					});

			ImageManager.getInstance().getImageLoader().fetchImage(imageRequest, skipCache);
		}
	}

	private void updateTextureRegions(Bitmap bodyBitmap) {

		final CandiModel candiModel = (CandiModel) this.mModel;

		mBodyTexture.clearTextureSources();
		mBodyTextureRegion = TextureRegionFactory.createFromSource(mBodyTexture, new BitmapTextureSource(bodyBitmap, new IBitmapAdapter() {

			@Override
			public Bitmap reloadBitmap() {
				// TextureSource needs to refresh a recycled bitmap.
				Bitmap bitmap = ImageManager.getInstance().getImage(candiModel.getBodyImageUri());
				if (bitmap != null) {
					return bitmap;
				}
				// Cached bitmap is gone so load it again.
				updateTextureSources(false);
				return null;
			}
		}), 0, 0);

		if (mBodySprite == null)
			makeBodySprite();

		/*
		 * Fetching from the cache is expense because it involves decoding from a file.
		 */
		Bitmap reflectionBitmap = ImageManager.getInstance().getImage(candiModel.getBodyImageUri() + ".reflection");
		if (reflectionBitmap == null) {
			if (bodyBitmap != null) {
				reflectionBitmap = ImageUtils.getReflection(bodyBitmap);
				ImageManager.getInstance().getImageCache().put(candiModel.getBodyImageUri() + ".reflection", reflectionBitmap, CompressFormat.PNG);
			}
		}

		if (reflectionBitmap != null) {
			mReflectionTexture.clearTextureSources();
			mReflectionTextureRegion = TextureRegionFactory.createFromSource(mReflectionTexture, new BitmapTextureSource(reflectionBitmap,
					new IBitmapAdapter() {

						@Override
						public Bitmap reloadBitmap() {
							Bitmap bitmap = ImageManager.getInstance().getImage(candiModel.getBodyImageUri() + ".reflection");
							if (bitmap != null) {
								return bitmap;
							}
							return null;
						}
					}), 0, 0);

			if (mReflectionSprite == null)
				makeReflectionSprite();
		}
		// ZOrder sort
		sortChildren();
	}

	@Override
	public void resetTextureSources() {
		super.resetTextureSources();

		mBodyTexture.clearTextureSources();
		mReflectionTexture.clearTextureSources();
		mPlaceholderTextureRegion = mCandiPatchPresenter.mPlaceholderTextureRegion.clone();
		mProgressTextureRegion = mCandiPatchPresenter.mProgressTextureRegion.clone();
		updateTextureSources(false);
	}

	@Override
	public void loadHardwareTextures() {
		super.loadHardwareTextures();

		mReflectionTexture = new Texture(256, 128, CandiConstants.GL_TEXTURE_OPTION);
		mCandiPatchPresenter.getEngine().getTextureManager().loadTexture(mReflectionTexture);
		mBodyTexture = new Texture(256, 256, CandiConstants.GL_TEXTURE_OPTION);
		mCandiPatchPresenter.getEngine().getTextureManager().loadTexture(mBodyTexture);
		mHardwareTexturesInitialized = true;
	}

	@Override
	public void unloadResources() {
		super.unloadResources();
		/*
		 * Completely remove all resources associated with this sprite.
		 * This should only be called from the engine update thread.
		 */
		Utilities.Log(CandiConstants.APP_NAME, "ProxiTile", "Unloading resources: " + ((CandiModel) mModel).getEntityProxy().label);

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

		mTextureIsLoadedToHardware = false;
	}

	public void unloadHardwareTextures() {}

	// --------------------------------------------------------------------------------------------
	// Gestures
	// --------------------------------------------------------------------------------------------

	public void setGestureDetector(GestureDetector gestureDetector) {
		this.mGestureDetector = gestureDetector;
	}

	public GestureDetector getGestureDetector() {
		return mGestureDetector;
	}

	public void setViewTouchListener(ViewTouchListener listener) {
		mTouchListener = listener;
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
			mTouchListener.onViewLongPress(this);
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

		if (mTouchListener != null) {
			long startTime = System.nanoTime();
			Utilities.Log(CandiConstants.APP_NAME, "CandiView", "SingleTapUp started...");
			mTouchListener.onViewSingleTap(this);
			long estimatedTime = System.nanoTime() - startTime;
			Utilities.Log(CandiConstants.APP_NAME, "CandiView", "SingleTapUp finished: " + String.valueOf(estimatedTime / 1000000) + "ms");
			return true;
		}
		return false;
	}

}
