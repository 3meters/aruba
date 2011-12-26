package com.proxibase.aircandi.candi.views;

import java.util.Observable;

import org.anddev.andengine.entity.modifier.DelayModifier;
import org.anddev.andengine.entity.modifier.MoveYModifier;
import org.anddev.andengine.entity.modifier.SequenceEntityModifier;
import org.anddev.andengine.opengl.buffer.BufferObjectManager;
import org.anddev.andengine.opengl.texture.Texture;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.opengl.texture.region.TextureRegionFactory;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.GestureDetector.OnGestureListener;

import com.proxibase.aircandi.candi.models.BaseModel;
import com.proxibase.aircandi.candi.models.CandiModel;
import com.proxibase.aircandi.candi.models.BaseModel.ViewState;
import com.proxibase.aircandi.candi.models.ZoneModel.ZoneAlignment;
import com.proxibase.aircandi.candi.modifiers.CandiAlphaModifier;
import com.proxibase.aircandi.candi.presenters.CandiPatchPresenter;
import com.proxibase.aircandi.candi.sprites.CandiSprite;
import com.proxibase.aircandi.candi.views.ViewAction.ViewActionType;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.utils.BitmapTextureSource;
import com.proxibase.aircandi.utils.ImageManager;
import com.proxibase.aircandi.utils.ImageUtils;
import com.proxibase.aircandi.utils.Logger;
import com.proxibase.aircandi.utils.BitmapTextureSource.IBitmapAdapter;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest.ImageShape;
import com.proxibase.aircandi.utils.NetworkManager.ResponseCode;
import com.proxibase.aircandi.utils.NetworkManager.ServiceResponse;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestListener;

// @SuppressWarnings("unused")
public class CandiView extends BaseView implements OnGestureListener {

	private Texture				mBodyTexture;
	private TextureRegion		mBodyTextureRegion;
	private CandiSprite			mBodySprite;

	private Texture				mReflectionTexture;
	private TextureRegion		mReflectionTextureRegion;
	private CandiSprite			mReflectionSprite;

	@SuppressWarnings("unused")
	private CandiSprite			mBodySpriteActive;
	private CandiSprite			mReflectionSpriteActive;

	private GestureDetector		mGestureDetector;
	private ViewTouchListener	mTouchListener;
	private boolean				mCollapsed			= false;
	private boolean				mReflectionActive	= true;
	private boolean				mHasBitmap			= false;
	private boolean				mActiveImageRequest	= false;

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

		mBodySpriteActive = mPlaceholderSprite;
		mReflectionSpriteActive = mPlaceholderReflectionSprite;
		mVisible = false;
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
		/*
		 * Changes to the properties of this entity will get picked up by
		 * the next update cycle.
		 */
		/*
		 * TODO: This may be getting called more than we want while doing
		 * a partial refresh (new beacons only).
		 */

		final CandiModel candiModel = (CandiModel) this.mModel;
		ViewState viewStateNext = candiModel.getViewStateNext();

		if (!CandiConstants.TRANSITIONS_ACTIVE) {

			/* Positioning and scale */
			setZIndex(viewStateNext.getZIndex());
			setPosition(viewStateNext.getX(), viewStateNext.getY());
			setScale(viewStateNext.getScale());

			/* Configuration */
			if (viewStateNext.reflectionActive() != mReflectionActive) {
				showReflection(viewStateNext.reflectionActive());
			}
			if (viewStateNext.isCollapsed() != this.mCollapsed) {
				configureCollapsed(viewStateNext.isCollapsed()); /* Only position changes */
			}

			/* Visibility */
			if (viewStateNext.isVisible() != this.isVisible()) {
				setVisible(viewStateNext.isVisible());
			}
		}

		/*
		 * Animation and touch support
		 * 
		 * Update thread isn't running unless rendering is active.
		 */
		mCandiPatchPresenter.renderingActivate();
		mCandiPatchPresenter.getEngine().runOnUpdateThread(new Runnable() {

			@Override
			public void run() {
				clearEntityModifiers();
				doViewActions();
				doViewModifiers();
				updateTouchArea(candiModel.isTouchAreaActive());
			}
		});
	}

	@Override
	public void reset() {
		/*
		 * We keep the touch and gesture listeners though they
		 * aren't active becase we remove the touch area.
		 */

		/* Engine resources */
		mCandiPatchPresenter.renderingActivate();
		mCandiPatchPresenter.getEngine().runOnUpdateThread(new Runnable() {

			@Override
			public void run() {
				updateTouchArea(false);
				clearEntityModifiers();
			}
		});

		/* Internal */
		setVisible(false);
		setRotation(0);
		setPosition(0, 1000); /* Offscreen */
		setScale(1);
		configureCollapsed(false);
		mTitleSprite.setVisible(true);
		if (mBodySprite != null) {
			mBodySprite.setVisible(false);
		}
		if (mReflectionSprite != null) {
			mReflectionSprite.setVisible(false);
		}
		mReflectionActive = true;
		mHasBitmap = false;
		mBound = false;
		mModel = null;
		mActiveImageRequest = false;
	}

	// --------------------------------------------------------------------------------------------
	// UI
	// --------------------------------------------------------------------------------------------

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

	private void configureCollapsed(boolean collapsed) {
		/*
		 * We configure both placeholder sprites and regular sprites
		 */
		mCollapsed = collapsed;
		if (collapsed) {
			if (mTitleSprite != null) {
				mTitleSprite.setVisible(false);
			}

			/* Positioning */
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
				mProgressBarSprite.setPosition(0, 0);
				if (mPlaceholderReflectionSprite != null) {
					mPlaceholderReflectionSprite.setPosition(0, CandiConstants.CANDI_VIEW_BODY_HEIGHT);
				}
			}
		}
		else {
			if (mTitleSprite != null) {
				mTitleSprite.setVisible(true);
			}

			/* Positioning */
			if (mBodySprite != null) {
				mBodySprite.setPosition(0, CandiConstants.CANDI_VIEW_TITLE_HEIGHT);
				if (mReflectionSprite != null) {
					mReflectionSprite.setPosition(0, CandiConstants.CANDI_VIEW_BODY_HEIGHT + CandiConstants.CANDI_VIEW_TITLE_HEIGHT
														+ CandiConstants.CANDI_VIEW_REFLECTION_GAP);
				}
			}
			if (mPlaceholderSprite != null) {
				mPlaceholderSprite.setPosition(0, CandiConstants.CANDI_VIEW_TITLE_HEIGHT);
				float progressX = (mPlaceholderSprite.getWidth() - mProgressTextureRegion.getTileWidth()) * 0.5f;
				float progressY = ((mPlaceholderSprite.getHeight() + CandiConstants.CANDI_VIEW_TITLE_HEIGHT) - mProgressTextureRegion.getTileHeight()) * 0.5f;
				mProgressSprite.setPosition(progressX, progressY);
				mProgressBarSprite.setPosition(0, CandiConstants.CANDI_VIEW_TITLE_HEIGHT
						);
				if (mPlaceholderReflectionSprite != null) {
					mPlaceholderReflectionSprite.setPosition(0, CandiConstants.CANDI_VIEW_BODY_HEIGHT + CandiConstants.CANDI_VIEW_TITLE_HEIGHT
																+ CandiConstants.CANDI_VIEW_REFLECTION_GAP);
				}
			}
		}
	}

	private void showReflection(boolean visible) {
		if (mReflectionSpriteActive != null) {
			if (mReflectionSpriteActive != null) {
				mReflectionSpriteActive.setVisible(visible);
				mReflectionActive = visible;
			}
		}
	}

	@SuppressWarnings("unused")
	private void makePlaceholderActive(boolean active) {
		if (active) {
			mBodySprite.setVisible(false);
			mReflectionSprite.setVisible(false);

			mPlaceholderSprite.setVisible(true);
			if (((BaseModel) mModel).getViewStateCurrent().reflectionActive() && mPlaceholderReflectionSprite != null) {
				mPlaceholderReflectionSprite.setVisible(true);
			}

			mBodySpriteActive = mPlaceholderSprite;
			mReflectionSpriteActive = mPlaceholderReflectionSprite;
		}
		else {
			mPlaceholderSprite.setVisible(false);
			mPlaceholderReflectionSprite.setVisible(false);

			mBodySprite.setVisible(true);
			if (((BaseModel) mModel).getViewStateCurrent().reflectionActive() && mReflectionSprite != null) {
				mReflectionSprite.setVisible(true);
			}

			mBodySpriteActive = mBodySprite;
			mReflectionSpriteActive = mReflectionSprite;
		}
	}

	// --------------------------------------------------------------------------------------------
	// Animation
	// --------------------------------------------------------------------------------------------

	private void configureCollapsedAnimated(boolean collapsed, float duration) {
		final CandiModel candiModel = (CandiModel) this.mModel;

		mCollapsed = collapsed;
		if (collapsed) {

			mTitleSprite.registerEntityModifier(new CandiAlphaModifier(mTitleSprite, duration, 1.0f, 0.0f, CandiConstants.EASE_FADE_OUT));

			if (mBodySprite != null) {
				mBodySprite.registerEntityModifier(new MoveYModifier(duration, CandiConstants.CANDI_VIEW_TITLE_HEIGHT, 0));

				if (mReflectionSprite != null) {
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
				if (mProgressBarSprite != null) {
					mProgressBarSprite.registerEntityModifier(new MoveYModifier(duration, CandiConstants.CANDI_VIEW_TITLE_HEIGHT, 0));
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
			mTitleSprite.registerEntityModifier(new CandiAlphaModifier(mTitleSprite, duration, 0.0f, 1.0f, CandiConstants.EASE_FADE_IN));

			if (mBodySprite != null) {
				mBodySprite.registerEntityModifier(new MoveYModifier(duration, 0, CandiConstants.CANDI_VIEW_TITLE_HEIGHT));
				if (mReflectionSprite != null) {
					if (candiModel.getZoneStateNext().getAlignment() == ZoneAlignment.Bottom) {
						mReflectionSprite.registerEntityModifier(new SequenceEntityModifier(
								new MoveYModifier(duration * 0.5f, CandiConstants.CANDI_VIEW_BODY_HEIGHT,
										CandiConstants.CANDI_VIEW_BODY_HEIGHT + CandiConstants.CANDI_VIEW_TITLE_HEIGHT
												+ CandiConstants.CANDI_VIEW_REFLECTION_GAP)));
					}
				}
				if (mProgressBarSprite != null) {
					mProgressBarSprite.registerEntityModifier(new MoveYModifier(duration, 0, CandiConstants.CANDI_VIEW_TITLE_HEIGHT));
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
	}

	private void showReflectionAnimated(boolean visible, float duration) {
		if (mReflectionSprite == null)
			return;

		if (visible) {
			if (mHasBitmap) {
				if (!mReflectionSprite.isVisible()) {
					mReflectionSprite.registerEntityModifier(new SequenceEntityModifier(new DelayModifier(duration * 0.1f), new CandiAlphaModifier(
							mReflectionSprite, duration * 0.5f, 0.0f, 1.0f)));
				}
			}
			else {
				if (!mPlaceholderReflectionSprite.isVisible()) {
					mPlaceholderReflectionSprite.registerEntityModifier(new SequenceEntityModifier(new DelayModifier(duration * 0.1f),
							new CandiAlphaModifier(
									mPlaceholderReflectionSprite, duration * 0.5f, 0.0f, 1.0f)));
				}
			}
		}
		else {
			if (mHasBitmap) {
				mReflectionSprite.registerEntityModifier(new SequenceEntityModifier(new DelayModifier(0.0f), new CandiAlphaModifier(
						mReflectionSprite,
						duration * 0.5f, 1.0f, 0.0f)));
			}
			else {
				mPlaceholderReflectionSprite.registerEntityModifier(new SequenceEntityModifier(new DelayModifier(0.0f), new CandiAlphaModifier(
						mPlaceholderReflectionSprite,
						duration * 0.5f, 1.0f, 0.0f)));
			}
		}
	}

	private void makePlaceholderActiveAnimated(boolean active) {
		if (active) {
			if (mBodySprite.isVisible()) {
				mBodySprite
						.registerEntityModifier(
						new CandiAlphaModifier(mBodySprite, CandiConstants.DURATION_PLACEHOLDER_HIDESHOW, 1.0f, 0.0f,
								CandiConstants.EASE_FADE_OUT));
				if (mReflectionSprite.isVisible()) {
					mReflectionSprite
							.registerEntityModifier(
							new CandiAlphaModifier(mReflectionSprite, CandiConstants.DURATION_PLACEHOLDER_HIDESHOW, 1.0f, 0.0f,
									CandiConstants.EASE_FADE_OUT));
				}
			}

			mPlaceholderSprite
					.registerEntityModifier(
					new CandiAlphaModifier(mPlaceholderSprite, CandiConstants.DURATION_PLACEHOLDER_HIDESHOW, 0.0f, 1.0f, CandiConstants.EASE_FADE_IN));

			if (((BaseModel) mModel).getViewStateCurrent().reflectionActive() && mReflectionSprite != null) {
				mPlaceholderReflectionSprite
						.registerEntityModifier(
						new CandiAlphaModifier(mPlaceholderReflectionSprite, CandiConstants.DURATION_PLACEHOLDER_HIDESHOW, 0.0f, 1.0f,
								CandiConstants.EASE_FADE_IN));
			}
			mBodySpriteActive = mPlaceholderSprite;
			mReflectionSpriteActive = mPlaceholderReflectionSprite;
		}
		else {
			if (mPlaceholderSprite.isVisible()) {
				mPlaceholderSprite
						.registerEntityModifier(
						new CandiAlphaModifier(mPlaceholderSprite, CandiConstants.DURATION_PLACEHOLDER_HIDESHOW, 1.0f, 0.0f,
								CandiConstants.EASE_FADE_OUT));
				if (mPlaceholderReflectionSprite.isVisible()) {
					mPlaceholderReflectionSprite
							.registerEntityModifier(
							new CandiAlphaModifier(mPlaceholderReflectionSprite, CandiConstants.DURATION_PLACEHOLDER_HIDESHOW, 1.0f, 0.0f,
									CandiConstants.EASE_FADE_OUT));
				}
			}

			mBodySprite.registerEntityModifier(
					new CandiAlphaModifier(mBodySprite, CandiConstants.DURATION_PLACEHOLDER_HIDESHOW, 0.0f, 1.0f, CandiConstants.EASE_FADE_IN));

			if (mModel != null && ((BaseModel) mModel).getViewStateCurrent().reflectionActive() && mReflectionSprite != null) {
				mReflectionSprite
						.registerEntityModifier(
						new CandiAlphaModifier(mReflectionSprite, CandiConstants.DURATION_PLACEHOLDER_HIDESHOW, 0.0f, 1.0f,
								CandiConstants.EASE_FADE_IN));
			}
			mBodySpriteActive = mBodySprite;
			mReflectionSpriteActive = mReflectionSprite;
		}
	}

	protected void doViewActions() {
		final BaseModel model = (BaseModel) mModel;
		if (model != null) {

			while (true) {
				ViewAction viewAction = null;
				synchronized (model.getViewActions()) {
					if (!model.getViewActions().isEmpty()) {
						viewAction = model.getViewActions().removeFirst();
					}
				}

				if (viewAction == null) break;

				if (viewAction.getViewActionType() == ViewActionType.UpdateTextures) {
					updateTextureSources(false);
				}
				else if (viewAction.getViewActionType() == ViewActionType.UpdateTexturesForce) {
					mActiveImageRequest = false;
					updateTextureSources(true);
				}
				else if (viewAction.getViewActionType() == ViewActionType.ExpandCollapseAnim) {
					configureCollapsedAnimated(model.getViewStateNext().isCollapsed(), CandiConstants.DURATION_CANDIBODY_COLLAPSE);
				}
				else if (viewAction.getViewActionType() == ViewActionType.ExpandCollapse) {
					configureCollapsed(model.getViewStateNext().isCollapsed());
				}
				else if (viewAction.getViewActionType() == ViewActionType.ReflectionHideShowAnim) {
					showReflectionAnimated(model.getViewStateNext().reflectionActive(), CandiConstants.DURATION_REFLECTION_HIDESHOW);
				}
				else if (viewAction.getViewActionType() == ViewActionType.ReflectionHideShow) {
					showReflection(model.getViewStateNext().reflectionActive());
				}
				else if (viewAction.getViewActionType() == ViewActionType.Position) {
					setPosition(model.getViewStateNext().getX(), model.getViewStateNext().getY());
				}
				else if (viewAction.getViewActionType() == ViewActionType.Scale) {
					setScale(model.getViewStateNext().getScale());
				}
				else if (viewAction.getViewActionType() == ViewActionType.Visibility) {
					setVisible(model.getViewStateNext().isVisible());
				}
				else if (viewAction.getViewActionType() == ViewActionType.ZIndex) {
					setZIndex(model.getViewStateNext().getZIndex());
				}
				if (mCandiPatchPresenter != null) {
					mCandiPatchPresenter.renderingActivate();
				}
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Sprites
	// --------------------------------------------------------------------------------------------

	private void makeReflectionSprite() {
		mReflectionSprite = new CandiSprite(0, CandiConstants.CANDI_VIEW_TITLE_HEIGHT + CandiConstants.CANDI_VIEW_BODY_HEIGHT,
				mReflectionTextureRegion);
		mReflectionSprite.setBlendFunction(CandiConstants.GL_BLEND_FUNCTION_SOURCE, CandiConstants.GL_BLEND_FUNCTION_DESTINATION);
		mReflectionSprite.setVisible(false);
		mReflectionSprite.setZIndex(0);
		attachChild(mReflectionSprite);
	}

	private void makeBodySprite() {
		mBodySprite = new CandiSprite(0, CandiConstants.CANDI_VIEW_TITLE_HEIGHT, mBodyTextureRegion);
		mBodySprite.setGestureDetector(mGestureDetector);
		mBodySprite.setBlendFunction(CandiConstants.GL_BLEND_FUNCTION_SOURCE, CandiConstants.GL_BLEND_FUNCTION_DESTINATION);
		mBodySprite.setVisible(false);
		mBodySprite.setZIndex(0);
		attachChild(mBodySprite);
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
	private void updateTextureSources(final boolean skipCache) {

		final CandiModel candiModel = (CandiModel) this.mModel;
		Logger.v(this, "Updating texture sources: " + ((CandiModel) this.mModel).getTitleText());

		if (!mActiveImageRequest) {
			/*
			 * TODO: We'd like to prioritize highest the candi views that are currently visible
			 * but position hasn't been assigned when this first gets called.
			 */
			//mActiveImageRequest = true;
			ImageRequest imageRequest = new ImageRequest(candiModel.getBodyImageUri(), ImageShape.Square, candiModel.getEntityProxy().imageFormat,
					candiModel.getEntityProxy().javascriptEnabled, CandiConstants.IMAGE_WIDTH_SEARCH_MAX, true, !skipCache, true,
					2, this, new RequestListener() {

						@Override
						public void onComplete(Object response) {
							/*
							 * Executes on the ViewManager thread (which has the lowest possible priority).
							 * First time for a candiview is more expensive because the body and reflection sprites
							 * are created.
							 */
							ServiceResponse serviceResponse = (ServiceResponse) response;

							if (serviceResponse.responseCode != ResponseCode.Success) {
								mActiveImageRequest = false;
								if (!mHasBitmap) {
									Logger.w(this, "Broken image: " + candiModel.getBodyImageUri());
									Bitmap bitmap = ImageManager.getInstance().loadBitmapFromAssets(CandiConstants.IMAGE_BROKEN);
									if (bitmap != null && mModel != null && !mRecycled) {
										mHasBitmap = false;
										updateTextureRegions(bitmap);
										makePlaceholderActiveAnimated(false);
										progressVisible(false);
									}
								}
							}
							else
							{
								Bitmap bodyBitmap = (Bitmap) serviceResponse.data;
								if (bodyBitmap != null) {

									/*
									 * The view could have been recycled while we were busy and won't have a bound
									 * model.
									 */
									if (mModel != null && !mRecycled) {
										mHasBitmap = true;
										updateTextureRegions(bodyBitmap);
										makePlaceholderActiveAnimated(false);
										progressVisible(false);
									}
								}
								mActiveImageRequest = false;
							}

						}

						@Override
						public void onProgressChanged(int progress) {
							mProgressBarSprite.setWidth(progress * ((float) CandiConstants.CANDI_VIEW_WIDTH / 100f));

							/* We tickle the rendering window if it's getting low */
							if (mCandiPatchPresenter.getRenderingTimeLeft() <= 1000) {
								Logger.v(this, "Bumping rendering timer");
								mCandiPatchPresenter.renderingActivate();
							}
						}
					});

			if (mReflectionSprite != null && !mHasBitmap) {
				mReflectionSprite.setVisible(false);
			}

			if (mPlaceholderSprite != null && !ImageManager.isLocalImage(imageRequest.imageUri)) {
				mPlaceholderSprite.setVisible(!ImageManager.getInstance().hasImage(imageRequest.imageUri));
			}

			if (((BaseModel) mModel).getViewStateCurrent().isCollapsed()) {
				if (mTitleSprite != null) {
					mTitleSprite.setVisible(false);
				}
			}

			progressVisible(true);
			Logger.v(this, "Queueing image fetch: " + candiModel.getTitleText());
			ImageManager.getInstance().getImageLoader().fetchImage(imageRequest);
		}
	}

	private void updateTextureRegions(Bitmap bodyBitmap) {

		final CandiModel candiModel = (CandiModel) this.mModel;
		if (candiModel == null) {
			throw new IllegalStateException("Trying to update texture regions with null model");
		}

		/* Process any decorations like text overlays */
		bodyBitmap = decorateTexture(bodyBitmap, false);

		mCandiPatchPresenter.renderingActivate();
		mBodyTexture.clearTextureSources();

		/* Create reflection before creating texture regions because the bitmaps get recycled */
		/* Fetching from the cache is expense because it involves decoding from a file. */
		final String cacheName = ImageManager.getInstance().resolveCacheName(candiModel.getBodyImageUri());
		Bitmap reflectionBitmap = ImageManager.getInstance().getImage(cacheName + ".reflection");
		if (reflectionBitmap == null) {
			if (bodyBitmap != null && !bodyBitmap.isRecycled()) {
				reflectionBitmap = ImageUtils.makeReflection(bodyBitmap, true);
				ImageManager.getInstance().getImageCache().put(cacheName + ".reflection", reflectionBitmap, CompressFormat.PNG);
			}
		}

		mBodyTextureRegion = TextureRegionFactory.createFromSource(mBodyTexture, new BitmapTextureSource(bodyBitmap, new IBitmapAdapter() {

			@Override
			public Bitmap reloadBitmap() {

				/* We could be in recycled state without a bound model. */
				if (mModel != null || !mRecycled) {

					/* TextureSource needs to refresh a recycled bitmap. */
					Bitmap bodyBitmap = ImageManager.getInstance().getImage(cacheName);
					if (bodyBitmap != null) {
						bodyBitmap = decorateTexture(bodyBitmap, false);
						return bodyBitmap;
					}

					/* Cached bitmap is gone so load it again. */
					updateTextureSources(false);
				}

				return null;
			}
		}), 0, 0);

		if (reflectionBitmap != null) {
			reflectionBitmap = decorateTexture(reflectionBitmap, true);
			mReflectionTexture.clearTextureSources();
			mReflectionTextureRegion = TextureRegionFactory.createFromSource(mReflectionTexture, new BitmapTextureSource(reflectionBitmap,
					new IBitmapAdapter() {

						@Override
						public Bitmap reloadBitmap() {
							if (mModel != null && !mRecycled) {
								Bitmap bitmap = ImageManager.getInstance().getImage(cacheName + ".reflection");
								if (bitmap != null) {
									bitmap = decorateTexture(bitmap, true);
									return bitmap;
								}
							}
							return null;
						}
					}), 0, 0);

		}

		/*
		 * This is where the primary sprites get created so we also need to
		 * do some management that couldn't be done until their were created.
		 */
		if (mBodySprite == null) {
			makeBodySprite();
			if (mReflectionSprite == null) {
				makeReflectionSprite();
			}
			configureCollapsed(mCollapsed);
			updateTouchArea(candiModel.isTouchAreaActive());
			sortChildren(); /* zorder sort */
		}
	}

	public Bitmap decorateTexture(Bitmap bitmap, boolean isReflection) {
		final CandiModel candiModel = (CandiModel) this.mModel;
		if (candiModel != null && candiModel.getEntityProxy().entityType.equals(CandiConstants.TYPE_CANDI_POST) &&
				candiModel.getEntityProxy().description != null &&
				candiModel.getEntityProxy().description.length() > 0) {
			if (!isReflection) {
				bitmap = overlayTextOnBitmap(bitmap, 0xffffffff, 0xcc000000, 175, 5, candiModel.getEntityProxy().description, false, false);
			}
			else {
				bitmap = overlayTextOnBitmap(bitmap, 0xffffffff, 0xcc000000, -45, 5, candiModel.getEntityProxy().description, true, true);
			}
		}
		return bitmap;
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
		String associatedModel = mModel != null ? ((CandiModel) mModel).getEntityProxy().label : "Recycled";
		Logger.v(this, "Unloading resources: " + associatedModel);

		if (mProgressSprite != null) {
			mProgressSprite.removeResources();
		}
		if (mProgressBarSprite != null) {
			mProgressBarSprite.removeResources();
		}
		if (mPlaceholderSprite != null) {
			mPlaceholderSprite.removeResources();
		}
		if (mPlaceholderReflectionSprite != null) {
			mPlaceholderReflectionSprite.removeResources();
		}
		if (mReflectionSprite != null) {
			mReflectionSprite.removeResources();
		}
		if (mBodySprite != null) {
			mBodySprite.removeResources();
		}

		if (mBodySprite != null) {
			boolean registeredTouchArea = mCandiPatchPresenter.getEngine().getScene().getTouchAreas().contains(mBodySprite);
			if (registeredTouchArea) {
				mCandiPatchPresenter.getEngine().getScene().unregisterTouchArea(mBodySprite);
			}
		}

		if (mReflectionTextureRegion != null) {
			BufferObjectManager.getActiveInstance().unloadBufferObject(mReflectionTextureRegion.getTextureBuffer());
		}
		if (mProgressTextureRegion != null) {
			BufferObjectManager.getActiveInstance().unloadBufferObject(mProgressTextureRegion.getTextureBuffer());
		}
		if (mBodyTextureRegion != null) {
			BufferObjectManager.getActiveInstance().unloadBufferObject(mBodyTextureRegion.getTextureBuffer());
		}

		if (mReflectionTexture != null) {
			mCandiPatchPresenter.getEngine().getTextureManager().unloadTexture(mReflectionTexture);
		}
		if (mBodyTexture != null) {
			mCandiPatchPresenter.getEngine().getTextureManager().unloadTexture(mBodyTexture);
		}

		/* These are holding onto Contexts */
		mTextView = null;
		mCandiPatchPresenter = null;
	}

	// --------------------------------------------------------------------------------------------
	// Gestures
	// --------------------------------------------------------------------------------------------

	public void setGestureDetector(GestureDetector gestureDetector) {
		this.mGestureDetector = gestureDetector;
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
			Logger.v(this, "SingleTapUp started...");
			mTouchListener.onViewSingleTap(this);
			long estimatedTime = System.nanoTime() - startTime;
			Logger.v(this, "SingleTapUp finished: " + String.valueOf(estimatedTime / 1000000) + "ms");
			return true;
		}
		return false;
	}
}
