package com.proxibase.aircandi.candi.views;

import java.util.Observable;
import java.util.Observer;

import javax.microedition.khronos.opengles.GL10;

import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.modifier.IEntityModifier;
import org.anddev.andengine.entity.modifier.ParallelEntityModifier;
import org.anddev.andengine.entity.modifier.SequenceEntityModifier;
import org.anddev.andengine.opengl.buffer.BufferObjectManager;
import org.anddev.andengine.opengl.texture.Texture;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.opengl.texture.region.TextureRegionFactory;
import org.anddev.andengine.opengl.texture.region.TiledTextureRegion;
import org.anddev.andengine.opengl.util.GLHelper;
import org.anddev.andengine.util.modifier.IModifier;
import org.anddev.andengine.util.modifier.IModifier.IModifierListener;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.text.TextUtils.TruncateAt;
import android.view.Gravity;
import android.view.View.MeasureSpec;
import android.widget.LinearLayout;

import com.proxibase.aircandi.candi.models.BaseModel;
import com.proxibase.aircandi.candi.modifiers.CandiAlphaModifier;
import com.proxibase.aircandi.candi.presenters.CandiPatchPresenter;
import com.proxibase.aircandi.candi.sprites.CandiAnimatedSprite;
import com.proxibase.aircandi.candi.sprites.CandiRectangle;
import com.proxibase.aircandi.candi.sprites.CandiSprite;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.utils.BitmapTextureSource;
import com.proxibase.aircandi.utils.BitmapTextureSource.IBitmapAdapter;
import com.proxibase.aircandi.widgets.TextViewEllipsizing;

public abstract class BaseView extends Entity implements Observer, IView {

	protected CandiPatchPresenter	mCandiPatchPresenter;
	protected Object				mModel;

	protected TiledTextureRegion	mProgressTextureRegion;
	protected CandiAnimatedSprite	mProgressSprite;
	protected CandiRectangle		mProgressBarSprite;

	protected TextureRegion			mPlaceholderTextureRegion;
	protected CandiSprite			mPlaceholderSprite;
	private TextureRegion			mPlaceholderReflectionTextureRegion;
	protected CandiSprite			mPlaceholderReflectionSprite;

	protected boolean				mBound							= false;
	protected boolean				mRecycled						= false;

	protected Texture				mTitleTexture;
	private TextureRegion			mTitleTextureRegion;
	protected CandiSprite			mTitleSprite;

	protected String				mTitleText;
	private int						mTitleTextColor;
	private int						mTitleTextFillColor				= Color.TRANSPARENT;
	private TextViewEllipsizing		mTitleTextView;
	protected boolean				mHardwareTexturesInitialized	= false;

	public BaseView() {
		this(null, null);
	}

	public BaseView(CandiPatchPresenter candiPatchPresenter) {
		this(null, candiPatchPresenter);
	}

	public BaseView(Object model, CandiPatchPresenter candiPatchPresenter) {
		super();

		mModel = model;
		setCandiPatchPresenter(candiPatchPresenter);
		makeSprites();
	}

	public void initialize() {
		updateTextureSources();
		mBound = true;
	}

	public void initializeModel() {
		updateTextureSources();
		mBound = true;
	}

	@Override
	public void update(Observable observable, Object data) {
		/*
		 * Any requested display extras are added to the title text by
		 * the getTitleText() method.
		 */
		String titleTextModel = ((BaseModel) mModel).getTitleText();
		if (mTitleSprite != null && titleTextModel != null && !titleTextModel.equals(mTitleText)) {
			mTitleText = titleTextModel;
			updateTextureSources();
		}
	}

	private void makeTitleSprite() {
		//		mTitleSprite = new CandiSprite(0,
		//				CandiConstants.CANDI_VIEW_TITLE_HEIGHT - (mTitleTextureRegion.getHeight() + CandiConstants.CANDI_VIEW_TITLE_SPACER_HEIGHT),
		//				mTitleTextureRegion);
		mTitleSprite = new CandiSprite(0, 0, mTitleTextureRegion);
		mTitleSprite.setBlendFunction(CandiConstants.GL_BLEND_FUNCTION_SOURCE, CandiConstants.GL_BLEND_FUNCTION_DESTINATION);
		mTitleSprite.setVisible(true);
		mTitleSprite.setZIndex(0);
		attachChild(mTitleSprite);
	}

	private void makeSprites() {

		/* Placeholder and reflection */
		mPlaceholderTextureRegion = mCandiPatchPresenter.mPlaceholderTextureRegion.clone();
		mPlaceholderSprite = new CandiSprite(0, CandiConstants.CANDI_VIEW_TITLE_HEIGHT, mPlaceholderTextureRegion);
		mPlaceholderSprite.setBlendFunction(CandiConstants.GL_BLEND_FUNCTION_SOURCE, CandiConstants.GL_BLEND_FUNCTION_DESTINATION);
		mPlaceholderSprite.setVisible(false);
		mPlaceholderSprite.setZIndex(10);
		attachChild(mPlaceholderSprite);

		mPlaceholderReflectionTextureRegion = mCandiPatchPresenter.mZoneReflectionTextureRegion.clone();
		mPlaceholderReflectionSprite = new CandiSprite(0, CandiConstants.CANDI_VIEW_TITLE_HEIGHT + CandiConstants.CANDI_VIEW_BODY_HEIGHT,
				mPlaceholderReflectionTextureRegion);
		mPlaceholderReflectionSprite.setBlendFunction(CandiConstants.GL_BLEND_FUNCTION_SOURCE, CandiConstants.GL_BLEND_FUNCTION_DESTINATION);
		mPlaceholderReflectionSprite.setVisible(false);
		mPlaceholderReflectionSprite.setZIndex(10);
		attachChild(mPlaceholderReflectionSprite);

		/* Progress */
		mProgressTextureRegion = mCandiPatchPresenter.mProgressTextureRegion.clone();
		float progressX = (mPlaceholderSprite.getWidth() - mProgressTextureRegion.getTileWidth()) * 0.5f;
		float progressY = CandiConstants.CANDI_VIEW_TITLE_HEIGHT + (mPlaceholderSprite.getHeight() * 0.5f)
							- (mProgressTextureRegion.getTileHeight() * 0.5f);
		mProgressSprite = new CandiAnimatedSprite(progressX, progressY, mProgressTextureRegion);
		mProgressSprite.setBlendFunction(CandiConstants.GL_BLEND_FUNCTION_SOURCE, CandiConstants.GL_BLEND_FUNCTION_DESTINATION);
		mProgressSprite.setVisible(false);
		mProgressSprite.setZIndex(20);
		attachChild(mProgressSprite);

		/* Progress bar */
		mProgressBarSprite = new CandiRectangle(0, CandiConstants.CANDI_VIEW_TITLE_HEIGHT, 0, 10);
		mProgressBarSprite.setBlendFunction(CandiConstants.GL_BLEND_FUNCTION_SOURCE, CandiConstants.GL_BLEND_FUNCTION_DESTINATION);
		mProgressBarSprite.setVisible(false);
		mProgressBarSprite.setColor(1, 0.827f, 0);
		mProgressBarSprite.setZIndex(20);

		attachChild(mProgressBarSprite);

		this.sortChildren();
	}

	protected void updateTextureSources() {
		if (((BaseModel) mModel).getTitleText() != null) {

			mTitleTexture.clearTextureSources();
			Bitmap titleBitmap = makeTextBitmap(CandiConstants.CANDI_VIEW_WIDTH, CandiConstants.CANDI_VIEW_TITLE_HEIGHT, ((BaseModel) mModel)
					.getTitleText());
			mTitleTextureRegion = TextureRegionFactory.createFromSource(mTitleTexture, new BitmapTextureSource(titleBitmap, new IBitmapAdapter() {

				@Override
				public Bitmap reloadBitmap() {
					/*
					 * This gets called if the bitmap being used as the texture source has been recycled
					 */
					Bitmap titleBitmap = null;
					if (mModel != null) {
						titleBitmap = makeTextBitmap(CandiConstants.CANDI_VIEW_WIDTH, CandiConstants.CANDI_VIEW_TITLE_HEIGHT, ((BaseModel) mModel)
								.getTitleText());
					}
					return titleBitmap;
				}
			}), 0, 0);

			if (mTitleSprite == null) {
				makeTitleSprite();
			}
		}
	}

	protected void doViewModifiers() {
		final BaseModel model = (BaseModel) mModel;
		if (model != null) {
			/*
			 * If this view get recyled, the modifier collection could be cleared while we
			 * are trying to grab an item from it.
			 */
			IEntityModifier modifier = null;
			synchronized (model.getViewModifiers()) {
				if (!model.getViewModifiers().isEmpty()) {
					modifier = model.getViewModifiers().removeFirst();
				}
			}

			if (modifier != null) {
				if (modifier instanceof CandiAlphaModifier) {
					((CandiAlphaModifier) modifier).setEntity(this);
				}
				else if (modifier instanceof ParallelEntityModifier) {
					final IModifier[] modifiers = ((ParallelEntityModifier) modifier).getModifiers();
					for (int i = modifiers.length - 1; i >= 0; i--) {
						if (modifiers[i] instanceof CandiAlphaModifier) {
							((CandiAlphaModifier) modifiers[i]).setEntity(this);
						}
					}
				}
				else if (modifier instanceof SequenceEntityModifier) {
					final IModifier[] modifiers = ((SequenceEntityModifier) modifier).getSubSequenceModifiers();
					for (int i = modifiers.length - 1; i >= 0; i--) {
						if (modifiers[i] instanceof CandiAlphaModifier) {
							((CandiAlphaModifier) modifiers[i]).setEntity(this);
						}
					}
				}
				modifier.addModifierListener(new IModifierListener<IEntity>() {

					@Override
					public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
						doViewModifiers();
					}

					@Override
					public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {}
				});
				registerEntityModifier(modifier);
			}
		}
	}

	public void progressVisible(boolean visible) {
		//mProgressSprite.setVisible(visible);
		mProgressBarSprite.setVisible(visible);
		if (visible) {
			//mProgressSprite.animate(150, true);
		}
		else {
			//mProgressSprite.stopAnimation();
			mProgressBarSprite.setWidth(0);
		}
	}

	private Bitmap makeTextBitmap(int width, int height, CharSequence text) {

		if (mTitleTextView == null) {
			mTitleTextView = new TextViewEllipsizing(mCandiPatchPresenter.mCandiActivity);
		}
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(width, height);
		mTitleTextView.setLayoutParams(layoutParams);
		mTitleTextView.setText(text);
		mTitleTextView.setTextColor(mTitleTextColor);
		mTitleTextView.setBackgroundColor(mTitleTextFillColor);
		mTitleTextView.setEllipsize(TruncateAt.END);
		mTitleTextView.setMaxLines(4);
		mTitleTextView.setSingleLine(false);

		Bitmap bitmap = Bitmap.createBitmap(width, height, CandiConstants.IMAGE_CONFIG_DEFAULT);
		Canvas canvas = new Canvas(bitmap);
		mTitleTextView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
		mTitleTextView.layout(0, 0, width, height);
		mTitleTextView.setGravity(Gravity.BOTTOM);
		mTitleTextView.draw(canvas);
		canvas = null;

		return bitmap;
	}

	public void unloadResources() {

		/* Completely remove all resources associated with this sprite. */
		if (mTitleSprite != null)
			mTitleSprite.removeResources();

		if (mTitleTextureRegion != null)
			BufferObjectManager.getActiveInstance().unloadBufferObject(mTitleTextureRegion.getTextureBuffer());

		if (mTitleTexture != null)
			mCandiPatchPresenter.getEngine().getTextureManager().unloadTexture(mTitleTexture);
	}

	public void resetTextureSources() {
		updateTextureSources();
	}

	public void loadHardwareTextures() {
		mTitleTexture = new Texture(256, 128, CandiConstants.GL_TEXTURE_OPTION);
		mCandiPatchPresenter.getEngine().getTextureManager().loadTextures(mTitleTexture);
	}

	@Override
	protected void applyRotation(final GL10 pGL) {

		/* Disable culling so we can see the backside of this sprite. */
		GLHelper.disableCulling(pGL);

		final float rotation = mRotation;

		if (rotation != 0) {
			final float rotationCenterX = mRotationCenterX;
			final float rotationCenterY = mRotationCenterY;

			pGL.glTranslatef(rotationCenterX, rotationCenterY, 0);

			/* Note we are applying rotation around the y-axis and not the z-axis anymore! */
			pGL.glRotatef(rotation, 0, 1, 0);
			pGL.glTranslatef(-rotationCenterX, -rotationCenterY, 0);
		}
	}

	public BaseView setTitleTextColor(int titleTextColor) {
		this.mTitleTextColor = titleTextColor;
		return this;
	}

	public BaseView setTitleTextFillColor(int titleTextFillColor) {
		this.mTitleTextFillColor = titleTextFillColor;
		return this;
	}

	public BaseView setCandiPatchPresenter(CandiPatchPresenter candiPatchPresenter) {
		this.mCandiPatchPresenter = candiPatchPresenter;
		return this;
	}

	protected boolean isVisibleToCamera(final Camera camera) {

		if (mPlaceholderSprite != null && mPlaceholderSprite.isVisibleToCamera(camera)) {
			return true;
		}
		else if (mTitleSprite != null && mTitleSprite.isVisibleToCamera(camera)) {
			return true;
		}
		return false;
	}

	@Override
	public void setAlpha(float alpha) {
		super.setAlpha(alpha);

		for (int i = 0; i < getChildCount(); i++) {
			getChild(i).setAlpha(alpha);
		}
	}

	@Override
	public void setZIndex(final int zIndex) {
		super.setZIndex(zIndex);

		for (int i = 0; i < getChildCount(); i++) {
			getChild(i).setZIndex(zIndex);
		}
	}

	public BaseModel getModel() {
		return (BaseModel) mModel;
	}

	public BaseView setModel(Object model) {
		mModel = model;
		return this;
	}

	public void setHardwareTexturesInitialized(boolean hardwareTexturesInitialized) {
		this.mHardwareTexturesInitialized = hardwareTexturesInitialized;
	}

	public boolean isHardwareTexturesInitialized() {
		return mHardwareTexturesInitialized;
	}

	public void setUnbound(boolean unbound) {
		this.mBound = unbound;
	}

	public boolean isUnbound() {
		return mBound;
	}

	public void setRecycled(boolean recycled) {
		this.mRecycled = recycled;
	}

	public boolean isRecycled() {
		return mRecycled;
	}
}