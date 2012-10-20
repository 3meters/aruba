package com.aircandi.candi.views;

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
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;

import com.aircandi.candi.models.BaseModel;
import com.aircandi.candi.models.CandiModel;
import com.aircandi.candi.models.BaseModel.ViewState;
import com.aircandi.candi.models.CandiModel.ReasonInactive;
import com.aircandi.candi.models.ZoneModel.ZoneAlignment;
import com.aircandi.candi.modifiers.CandiAlphaModifier;
import com.aircandi.candi.presenters.CandiPatchPresenter;
import com.aircandi.candi.sprites.CandiSprite;
import com.aircandi.candi.views.ViewAction.ViewActionType;
import com.aircandi.components.BitmapTextureSource;
import com.aircandi.components.ImageManager;
import com.aircandi.components.ImageRequest;
import com.aircandi.components.ImageRequestBuilder;
import com.aircandi.components.ImageUtils;
import com.aircandi.components.Logger;
import com.aircandi.components.BitmapTextureSource.IBitmapAdapter;
import com.aircandi.components.ImageRequest.ImageResponse;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.core.CandiConstants;
import com.aircandi.service.ProxibaseService.RequestListener;
import com.aircandi.service.objects.Entity;
import com.aircandi.R;

public class CandiView extends BaseView implements OnGestureListener {

	private Texture				mBodyTexture;
	private TextureRegion		mBodyTextureRegion;
	public CandiSprite			mBodySprite;

	private Texture				mReflectionTexture;
	private TextureRegion		mReflectionTextureRegion;
	public CandiSprite			mReflectionSprite;

	private GestureDetector		mGestureDetector;
	private ViewTouchListener	mTouchListener;
	private boolean				mCollapsed			= false;
	public boolean				mReflectionActive	= true;
	public boolean				mHasBitmap			= false;
	private boolean				mActiveImageRequest	= false;
	private float				mTouchStartY;

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

		setVisible(false);
		setScale(CandiConstants.CANDI_VIEW_SCALE);

	}

	@Override
	public void initialize() {
		if (mModel == null) {
			throw new IllegalStateException("Must set the model before initializing");
		}
		super.initialize();
		final CandiModel candiModel = (CandiModel) this.mModel;
		if (!candiModel.getEntity().global && !candiModel.getEntity().synthetic) {
			mProximitySprite.setVisible(true);
		}

		requestTextureSources(false, true);
	}

	@Override
	public void initializeModel() {
		if (mModel == null) {
			throw new IllegalStateException("Must set the model before initializing");
		}
		super.initializeModel();
		final CandiModel candiModel = (CandiModel) this.mModel;
		if (!candiModel.getEntity().global) {
			mProximitySprite.setVisible(true);
		}

		requestTextureSources(false, true);
	}

	// --------------------------------------------------------------------------------------------
	// Primary
	// --------------------------------------------------------------------------------------------

	@Override
	public void update(Observable observable, Object data) {
		super.update(observable, data);
		/*
		 * Changes to the properties of this entity will get picked up by the next update cycle.
		 */
		/*
		 * TODO: This may be getting called more than we want while doing a partial refresh (new beacons only).
		 */

		final CandiModel candiModel = (CandiModel) this.mModel;
		ViewState viewStateNext = candiModel.getViewStateNext();

		if (!CandiConstants.TRANSITIONS_ACTIVE) {

			/* Positioning and scale */
			setZIndex(viewStateNext.getZIndex());
			setPosition(viewStateNext.getX(), viewStateNext.getY());
			setScale(viewStateNext.getScale());

			/* Configuration */
			if (viewStateNext.reflectionActive() != mReflectionActive && CandiConstants.CANDI_VIEW_REFLECTION_SHOW) {
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
		 * Animation and touch support Update thread isn't running unless rendering is active.
		 */
		mCandiPatchPresenter.renderingActivateBump();
		mCandiPatchPresenter.getEngine().runOnUpdateThread(new Runnable() {

			@Override
			public void run() {
				clearEntityModifiers();
				doViewActions();
				doViewModifiers();
				updateTouchArea(candiModel.isTouchAreaActive());
				if (candiModel.getEntity() != null && candiModel.getEntity().getBeacon() != null) {
					int avgBeaconLevel = candiModel.getEntity().getBeacon().getAvgBeaconLevel();
					int color = ImageUtils.getColorBySignalLevel(avgBeaconLevel, 255);
					setProximityColor(color);
				}
			}
		});
	}

	@Override
	public void reset() {
		/*
		 * We keep the touch and gesture listeners though they aren't active becase we remove the touch area.
		 */

		/* Engine resources */
		mCandiPatchPresenter.renderingActivateBump();
		mCandiPatchPresenter.getEngine().runOnUpdateThread(new Runnable() {

			@Override
			public void run() {
				updateTouchArea(false);
				clearEntityModifiers();
				clearSpriteModifiers();
			}
		});

		/* Internal */
		setVisible(false);
		setRotation(0);
		setPosition(0, 1000); /* Offscreen */
		setScale(CandiConstants.CANDI_VIEW_SCALE);
		configureCollapsed(false);
		if (mTitleSprite != null) {
			mTitleSprite.setLocked(false);
			mTitleSprite.setVisible(true);
		}
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
		final CandiModel candiModel = (CandiModel) this.mModel;
		if (collapsed) {
			if (mTitleSprite != null) {
				mTitleSprite.setLocked(false);
				mTitleSprite.setVisible(false);
				mTitleSprite.setLocked(true);
			}

			/* Positioning */
			if (mBodySprite != null) {
				mBodySprite.setPosition(0, 0);
				if (mReflectionSprite != null) {
					if (candiModel.getViewStateNext().getScale() == 1) {
						mReflectionSprite.setPosition(0, CandiConstants.CANDI_VIEW_BODY_HEIGHT + CandiConstants.CANDI_VIEW_REFLECTION_GAP);
					}
					else {
						mReflectionSprite.setPosition(0, CandiConstants.CANDI_VIEW_BODY_HEIGHT + CandiConstants.CANDI_VIEW_REFLECTION_GAP + 2);
					}
				}
			}
			if (mPlaceholderSprite != null) {
				mPlaceholderSprite.setPosition(0, 0);
				mProgressBarSprite.setPosition(0, 0);
				if (mPlaceholderReflectionSprite != null) {
					mPlaceholderReflectionSprite.setPosition(0, CandiConstants.CANDI_VIEW_BODY_HEIGHT + CandiConstants.CANDI_VIEW_REFLECTION_GAP);
				}
			}
		}
		else {
			if (mTitleSprite != null) {
				mTitleSprite.setLocked(false);
				mTitleSprite.setVisible(true);
				mTitleSprite.setLocked(true);
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
		if (mReflectionSprite != null) {
			mReflectionSprite.setVisible(visible);
			mReflectionActive = visible;
		}
	}

	// --------------------------------------------------------------------------------------------
	// Animation
	// --------------------------------------------------------------------------------------------

	private void configureCollapsedAnimated(boolean collapsed, float duration) {
		final CandiModel candiModel = (CandiModel) this.mModel;

		mCollapsed = collapsed;
		if (collapsed) {

			if (mTitleSprite != null) {
				mTitleSprite.setLocked(false);
				mTitleSprite.registerEntityModifier(new CandiAlphaModifier(mTitleSprite, duration, 1.0f, 0.0f, CandiConstants.EASE_FADE_OUT));
			}

			if (mBodySprite != null) {
				mBodySprite.registerEntityModifier(new MoveYModifier(duration, CandiConstants.CANDI_VIEW_TITLE_HEIGHT, 0));

				if (mReflectionSprite != null) {
					if (candiModel.getZoneStateNext().getAlignment() != ZoneAlignment.Bottom) {

						mReflectionSprite.registerEntityModifier(new SequenceEntityModifier(new DelayModifier(duration * 0.5f), new MoveYModifier(
								duration * 0.5f, CandiConstants.CANDI_VIEW_BODY_HEIGHT + CandiConstants.CANDI_VIEW_TITLE_HEIGHT
										+ CandiConstants.CANDI_VIEW_REFLECTION_GAP,
								CandiConstants.CANDI_VIEW_BODY_HEIGHT + CandiConstants.CANDI_VIEW_REFLECTION_GAP)));
					}
					else {
						if (candiModel.getViewStateNext().getScale() == 1) {
							mReflectionSprite.registerEntityModifier(new SequenceEntityModifier(new DelayModifier(duration * 0.5f),
									new MoveYModifier(
											duration * 0.5f, CandiConstants.CANDI_VIEW_BODY_HEIGHT + CandiConstants.CANDI_VIEW_TITLE_HEIGHT
													+ CandiConstants.CANDI_VIEW_REFLECTION_GAP,
											CandiConstants.CANDI_VIEW_BODY_HEIGHT + CandiConstants.CANDI_VIEW_REFLECTION_GAP)));
						}
						else {
							mReflectionSprite.registerEntityModifier(new SequenceEntityModifier(new DelayModifier(duration * 0.5f),
									new MoveYModifier(
											duration * 0.5f, CandiConstants.CANDI_VIEW_BODY_HEIGHT + CandiConstants.CANDI_VIEW_TITLE_HEIGHT
													+ CandiConstants.CANDI_VIEW_REFLECTION_GAP,
											CandiConstants.CANDI_VIEW_BODY_HEIGHT + CandiConstants.CANDI_VIEW_REFLECTION_GAP + 2)));
						}
					}
				}
				if (mProgressBarSprite != null) {
					mProgressBarSprite.registerEntityModifier(new MoveYModifier(duration, CandiConstants.CANDI_VIEW_TITLE_HEIGHT, 0));
				}
			}
			if (mPlaceholderSprite != null) {
				mPlaceholderSprite.registerEntityModifier(new MoveYModifier(duration, CandiConstants.CANDI_VIEW_TITLE_HEIGHT, 0));
			}
		}
		else {
			if (mTitleSprite != null) {
				mTitleSprite.setLocked(false);
				mTitleSprite.registerEntityModifier(new CandiAlphaModifier(mTitleSprite, duration, 0.0f, 1.0f, CandiConstants.EASE_FADE_IN));
			}

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

	private void showBodyAndReflectionAnimated() {
		if (mCandiPatchPresenter != null) {
			mCandiPatchPresenter.renderingActivateBump();
		}

		if (mPlaceholderSprite.isVisible()) {
			mPlaceholderSprite.registerEntityModifier(
					new CandiAlphaModifier(mPlaceholderSprite,
							CandiConstants.DURATION_PLACEHOLDER_HIDESHOW, 1.0f, 0.0f,
							CandiConstants.EASE_FADE_OUT));
			if (mPlaceholderReflectionSprite.isVisible()) {
				mPlaceholderReflectionSprite.registerEntityModifier(
						new CandiAlphaModifier(mPlaceholderReflectionSprite,
								CandiConstants.DURATION_PLACEHOLDER_HIDESHOW, 1.0f, 0.0f,
								CandiConstants.EASE_FADE_OUT));
			}
		}

		mBodySprite.registerEntityModifier(
				new CandiAlphaModifier(mBodySprite,
						CandiConstants.DURATION_PLACEHOLDER_HIDESHOW, 0.0f, 1.0f,
						CandiConstants.EASE_FADE_IN));

		if (mModel != null && ((BaseModel) mModel).getViewStateCurrent().reflectionActive() && mReflectionSprite != null) {
			mReflectionSprite.registerEntityModifier(
					new CandiAlphaModifier(mReflectionSprite,
							CandiConstants.DURATION_PLACEHOLDER_HIDESHOW, 0.0f, 1.0f,
							CandiConstants.EASE_FADE_IN));
		}
	}

	@Override
	public void clearSpriteModifiers() {
		// super.clearSpriteModifiers();
		// if (mBodySprite != null) {
		// mBodySprite.clearEntityModifiers();
		// mReflectionSprite.clearEntityModifiers();
		// }
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
					requestTextureSources(false, true);
				}
				else if (viewAction.getViewActionType() == ViewActionType.UpdateTexturesForce) {
					mActiveImageRequest = false;
					requestTextureSources(true, true);
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
					if (CandiConstants.CANDI_VIEW_REFLECTION_SHOW) {
						showReflection(model.getViewStateNext().reflectionActive());
					}
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
					mCandiPatchPresenter.renderingActivateBump();
				}
			}
		}
	}

	@Override
	public void setAlpha(float alpha) {
		for (int i = 0; i < getChildCount(); i++) {
			getChild(i).setAlpha(alpha);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Sprites
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("unused")
	private void makeReflectionSprite() {
		mReflectionSprite = new CandiSprite(0, CandiConstants.CANDI_VIEW_TITLE_HEIGHT + CandiConstants.CANDI_VIEW_BODY_HEIGHT
				+ CandiConstants.CANDI_VIEW_REFLECTION_GAP,
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

	private void requestTextureSources(final boolean skipCache, final boolean showBody) {

		if (this.mModel == null) return;
		final CandiModel candiModel = (CandiModel) this.mModel;
		final String titleText = (candiModel.getTitleText() != null ? candiModel.getTitleText() : "[Untitled]");
		final long startTime = System.nanoTime();

		if (!mActiveImageRequest) {
			/*
			 * TODO: We'd like to prioritize highest the candi views that are currently visible but position hasn't been
			 * assigned when this first gets called.
			 */
			mActiveImageRequest = true;
			final Entity entity = candiModel.getEntity();

			final ImageRequestBuilder builder = new ImageRequestBuilder(this);
			builder.setImageUri(entity.getMasterImageUri());
			builder.setImageFormat(entity.getMasterImageFormat());
			builder.setLinkZoom(entity.linkZoom);
			builder.setLinkJavascriptEnabled(entity.linkJavascriptEnabled);
			builder.setSearchCache(!skipCache);
			builder.setRequestListener(new RequestListener() {

				@Override
				public void onComplete(Object response) {
					/*
					 * Executes on the ViewManager thread (which has the lowest possible priority). First time for a
					 * candiview is more expensive because the body and reflection sprites are created.
					 */
					ServiceResponse serviceResponse = (ServiceResponse) response;

					/*
					 * We could be coming back while the data model is getting rebuilt which makes the current work
					 * expendable.
					 */
					if (serviceResponse.responseCode == ResponseCode.Success) {
						/*
						 * The view could have been recycled while we were busy and won't have a bound model.
						 */
						if (mModel != null && !mRecycled) {
							/*
							 * The view could have been recycled and put back into service targeting a different
							 * bitmap.
							 */
							final CandiModel candiModel = (CandiModel) mModel;
							ImageResponse imageResponse = (ImageResponse) serviceResponse.data;
							String imageUri = ImageRequestBuilder.getImageUriFromEntity(candiModel.getEntity());

							if (imageResponse.imageUri.equals(imageUri) && imageResponse.bitmap != null) {
								Logger.v(CandiView.this, "Texture request complete: " + titleText);
								mHasBitmap = true;
								updateTextureRegions(imageResponse.bitmap, skipCache);
								if (candiModel.getViewStateCurrent().isVisible()) {
									showBodyAndReflectionAnimated();
								}
							}
							Logger.v(CandiView.this, "Texture region update complete: " + titleText + ": "
									+ String.valueOf((System.nanoTime() - startTime) / 1000000) + "ms");
							progressVisible(false);
						}
						else {
							if (mModel != null) {
								String modelTitle = ((CandiModel) mModel).getTitleText();
								Logger.v(CandiView.this, "Texture request complete but requestor recycled : " + modelTitle);
							}
						}
					}
					else {
						if (!mHasBitmap && mModel != null && !mRecycled) {
							Logger.w(CandiView.this, "Broken image: " + entity.imagePreviewUri);
							Bitmap bitmap = ImageManager.getInstance().loadBitmapFromResources(R.drawable.image_broken);
							if (bitmap != null) {
								mHasBitmap = false;
								updateTextureRegions(bitmap, skipCache);
								if (candiModel.getViewStateCurrent().isVisible()) {
									showBodyAndReflectionAnimated();
								}
								progressVisible(false);
							}
						}
					}
					mActiveImageRequest = false;
				}

				@Override
				public void onProgressChanged(int progress) {
					mProgressBarSprite.setWidth(progress * ((float) CandiConstants.CANDI_VIEW_WIDTH / 100f));

					/* We tickle the rendering window if it's getting low */
					if (mCandiPatchPresenter != null && mCandiPatchPresenter.getRenderingTimeLeft() <= 1000) {
						Logger.v(this, "Bumping rendering timer");
						mCandiPatchPresenter.renderingActivateBump();
					}
				}
			});

			ImageRequest imageRequest = builder.create();

			if (mReflectionSprite != null && !mHasBitmap) {
				mReflectionSprite.setVisible(false);
			}

			if (mPlaceholderSprite != null) {
				if (imageRequest.getImageUri() != null && !ImageManager.isLocalImage(imageRequest.getImageUri())) {
					mPlaceholderSprite.setVisible(!ImageManager.getInstance().hasImage(imageRequest.getImageUri()));
					mPlaceholderReflectionSprite.setVisible(mPlaceholderSprite.isVisible());
				}
			}

			if (((BaseModel) mModel).getViewStateCurrent().isCollapsed()) {
				if (mTitleSprite != null) {
					mTitleSprite.setVisible(false);
				}
			}

			progressVisible(true);
			ImageManager.getInstance().getImageLoader().fetchImage(imageRequest, false);
		}
	}

	private void updateTextureRegions(Bitmap bodyBitmap, Boolean skipCache) {

		final CandiModel candiModel = (CandiModel) this.mModel;
		if (candiModel == null) {
			Logger.d(this, "Trying to update texture regions for candi view that has no model");
			return;
		}
		final String titleText = (candiModel.getTitleText() != null ? candiModel.getTitleText() : "[Untitled]");
		super.updateTextureRegions("Candi title: ");

		Entity entity = candiModel.getEntity();

		mCandiPatchPresenter.renderingActivateBump();
		mBodyTexture.clearTextureSources();

		/* Create reflection before creating texture regions because the bitmaps get recycled */
		/* Fetching from the cache is expense because it involves decoding from a file. */
		String imageUri = ImageRequestBuilder.getImageUriFromEntity(entity);
		final String cacheName = ImageManager.getInstance().resolveCacheName(imageUri);
		Bitmap reflectionBitmap = ImageManager.getInstance().getImage(cacheName + ".reflection");
		if (reflectionBitmap == null || skipCache) {
			if (bodyBitmap != null && !bodyBitmap.isRecycled()) {
				reflectionBitmap = ImageUtils.makeReflection(bodyBitmap, true);
				ImageManager.getInstance().putImage(cacheName + ".reflection", reflectionBitmap, CompressFormat.PNG);
				Logger.v(CandiView.this, "Texture reflection created: " + titleText);
			}
		}

		/* Process any decorations like text overlays */
		bodyBitmap = decorateTexture(bodyBitmap, false, true);

		/*
		 * TODO: Getting crash: textureSource must not exceed bounds of texture
		 */
		mBodyTextureRegion = TextureRegionFactory.createFromSource(mBodyTexture,
				new BitmapTextureSource(bodyBitmap, "Candi body source: " + candiModel.getTitleText(), new IBitmapAdapter() {

					@Override
					public Bitmap reloadBitmap() {

						/*
						 * We could be in recycled state without a bound model. The engine could also be requesting a
						 * bitmap for a candi that has been deleted (including the bitmap in S3).
						 */
						if (mModel != null || !mRecycled) {

							/* TextureSource needs to refresh a recycled bitmap. */
							Bitmap bodyBitmap = ImageManager.getInstance().getImage(cacheName);

							if (bodyBitmap != null) {
								Logger.v(this, "Engine request: texture refreshed from cache: " + titleText);
								bodyBitmap = decorateTexture(bodyBitmap, false, true);
								return bodyBitmap;
							}

							if (((CandiModel) mModel).getReasonInactive() != ReasonInactive.Deleting) {
								/* Cached bitmap is gone so load it again. */
								Logger.v(this, "Engine request: texture not in cache: request it: " + titleText);
								requestTextureSources(false, true);
							}
						}
						return null;
					}
				}), 0, 0);

		if (reflectionBitmap != null) {
			reflectionBitmap = decorateTexture(reflectionBitmap, true, true);
			mReflectionTexture.clearTextureSources();
			mReflectionTextureRegion = TextureRegionFactory.createFromSource(mReflectionTexture, new BitmapTextureSource(reflectionBitmap,
					"Candi reflection source: " + titleText,
					new IBitmapAdapter() {

						@Override
						public Bitmap reloadBitmap() {
							if (mModel != null && !mRecycled) {
								Bitmap bitmap = ImageManager.getInstance().getImage(cacheName + ".reflection");
								if (bitmap != null) {
									bitmap = decorateTexture(bitmap, true, true);
									return bitmap;
								}
								else {
									/* TODO: We should rebuild the reflection? */
								}
							}
							return null;
						}
					}), 0, 0);

		}

		/*
		 * This is where the primary sprites get created so we also need to do some management that couldn't be done
		 * until their were created.
		 */
		if (mBodySprite == null) {
			makeBodySprite();
			if (mReflectionSprite == null) {
				//makeReflectionSprite();
			}
			configureCollapsed(mCollapsed);
			updateTouchArea(candiModel.isTouchAreaActive());
			sortChildren(); /* zorder sort */
		}
	}

	public Bitmap decorateTexture(Bitmap bitmap, boolean isReflection, boolean insetCollectionImage) {
		final CandiModel candiModel = (CandiModel) this.mModel;

		if (candiModel != null) {

			/* Handle text overlay for posts */
			if (candiModel.getEntity().type.equals(CandiConstants.TYPE_CANDI_POST) && mCandiPatchPresenter != null) {
				bitmap = overlayBitmapOnBitmap(bitmap
						, mCandiPatchPresenter.mBitmapBadgePosts
						, null
						, CandiConstants.CANDI_VIEW_WIDTH - (CandiConstants.CANDI_VIEW_BADGE_WIDTH + 7)
						, CandiConstants.CANDI_VIEW_WIDTH - (CandiConstants.CANDI_VIEW_BADGE_WIDTH + 7)
						, false
						, false);
			}

			else if (candiModel.getEntity().type.equals(CandiConstants.TYPE_CANDI_COLLECTION)) {

				/* Handle bitmap overlay for collections that have a badge set */
				if (candiModel.getEntity().getMasterImageUri() == null
						|| !candiModel.getEntity().getMasterImageUri().toLowerCase().startsWith("resource:")) {

					if (!isReflection && mCandiPatchPresenter != null) {
						bitmap = overlayBitmapOnBitmap(bitmap
								, mCandiPatchPresenter.mBitmapBadgeCollections
								, null
								, CandiConstants.CANDI_VIEW_WIDTH - (CandiConstants.CANDI_VIEW_BADGE_WIDTH + 7)
								, CandiConstants.CANDI_VIEW_WIDTH - (CandiConstants.CANDI_VIEW_BADGE_WIDTH + 7)
								, false
								, false);
					}
				}
			}
		}
		return bitmap;
	}

	@Override
	public void loadHardwareTextures() {
		super.loadHardwareTextures();

		mReflectionTexture = new Texture(256, 128, CandiConstants.GL_TEXTURE_OPTION);
		mReflectionTexture.setName("Reflection: " + ((BaseModel) this.mModel).getTitleText());
		mCandiPatchPresenter.getEngine().getTextureManager().loadTexture(mReflectionTexture);

		mBodyTexture = new Texture(256, 256, CandiConstants.GL_TEXTURE_OPTION);
		mReflectionTexture.setName("Body: " + ((BaseModel) this.mModel).getTitleText());
		mCandiPatchPresenter.getEngine().getTextureManager().loadTexture(mBodyTexture);

		mHardwareTexturesInitialized = true;
	}

	@Override
	public void unloadResources() {
		super.unloadResources();

		/*
		 * Completely remove all resources associated with this sprite. This should only be called from the engine
		 * update thread.
		 */
		String associatedModel = mModel != null ? ((CandiModel) mModel).getEntity().label : "Recycled";
		Logger.v(this, "Unloading resources: " + associatedModel);

		if (mProgressBarSprite != null) {
			mProgressBarSprite.removeResources();
		}
		if (mProximitySprite != null) {
			mProximitySprite.removeResources();
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

	// --------------------------------------------------------------------------------------------
	// Gesture listener implementation
	// --------------------------------------------------------------------------------------------

	@Override
	public boolean onDown(MotionEvent e) {
		if (mCandiPatchPresenter.isIgnoreInput()) {
			return true;
		}
		/*
		 * Set the position of the highlight sprite
		 */
		float top = this.getY();
		if (!getModel().getViewStateCurrent().isCollapsed()) {
			top = top + CandiConstants.CANDI_VIEW_TITLE_HEIGHT;
		}

		mCandiPatchPresenter.mHighlight.setWidth((CandiConstants.CANDI_VIEW_WIDTH * this.getScaleX()) + (CandiConstants.CANDI_VIEW_HIGHLIGHT_THICKNESS * 2));
		mCandiPatchPresenter.mHighlight.setHeight((CandiConstants.CANDI_VIEW_WIDTH * this.getScaleY()) + (CandiConstants.CANDI_VIEW_HIGHLIGHT_THICKNESS * 2));

		mCandiPatchPresenter.mHighlight
				.setPosition(getX() - CandiConstants.CANDI_VIEW_HIGHLIGHT_THICKNESS, top - CandiConstants.CANDI_VIEW_HIGHLIGHT_THICKNESS);
		mCandiPatchPresenter.mHighlight.setVisible(true);
		mTouchStartY = e.getY();
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		if (mCandiPatchPresenter.isIgnoreInput()) {
			return true;
		}
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		/*
		 * This happens while the finger is still down.
		 */
		if (mCandiPatchPresenter.isIgnoreInput()) {
			return;
		}
		if (mTouchListener != null) {
			mTouchListener.onViewLongPress(this);
		}
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		/*
		 * This gets called continuously as a candiview is dragged. Also bubbles up
		 * to the scene touch event.
		 */
		if (mCandiPatchPresenter.isIgnoreInput()) {
			return true;
		}
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		if (mCandiPatchPresenter.isIgnoreInput()) {
			return true;
		}
		mCandiPatchPresenter.mHighlight.setVisible(false);
		/*
		 * We have our own test for whether the touch event
		 * was enough of a move to not quality as a single tap.
		 */
		if (mTouchListener != null && Math.abs(mTouchStartY - e.getY()) <= mCandiPatchPresenter.mTouchSlopSquare) {
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
