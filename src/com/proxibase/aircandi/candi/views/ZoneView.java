package com.proxibase.aircandi.candi.views;

import java.util.Observable;

import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.modifier.IEntityModifier;
import org.anddev.andengine.opengl.buffer.BufferObjectManager;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.util.modifier.IModifier;
import org.anddev.andengine.util.modifier.IModifier.IModifierListener;

import com.proxibase.aircandi.candi.models.ZoneModel;
import com.proxibase.aircandi.candi.models.BaseModel.ViewState;
import com.proxibase.aircandi.candi.models.CandiModel.Transition;
import com.proxibase.aircandi.candi.modifiers.CandiAlphaModifier;
import com.proxibase.aircandi.candi.presenters.CandiPatchPresenter;
import com.proxibase.aircandi.candi.sprites.CandiSprite;
import com.proxibase.aircandi.core.CandiConstants;

public class ZoneView extends BaseView {

	private ViewTexturesLoadedListener	mTexturesLoadedListener;

	private TextureRegion				mBodyTextureRegion;
	private CandiSprite					mBodySprite;
	private TextureRegion				mReflectionTextureRegion;
	private CandiSprite					mReflectionSprite;

	public ZoneView() {
		this(null, null);
	}

	public ZoneView(CandiPatchPresenter candiPatchPresenter) {
		this(null, candiPatchPresenter);
	}

	public ZoneView(Object model, CandiPatchPresenter candiPatchPresenter) {
		super(model, candiPatchPresenter);
		this.setVisible(false);
		this.setAlpha(0);
	}

	@Override
	public void update(Observable observable, Object data) {
		super.update(observable, data);

		final ZoneModel zoneModel = (ZoneModel) mModel;

		if (!CandiConstants.TRANSITIONS_ACTIVE) {
			ViewState viewStateNext = zoneModel.getViewStateNext();
			/*
			 * Visibility
			 */
			setAlpha(viewStateNext.getAlpha());
			setVisible(zoneModel.getViewStateNext().isVisible());
			/*
			 * Modifiers
			 */
			doModifiers();
		}
		else {
			if (zoneModel.getViewStateNext().isVisible()) {
				Transition transition = zoneModel.getTransition();
				if (transition == Transition.FadeIn)
					setAlpha(0);
				setVisible(true);
			}
			doModifiers();
		}
	}

	@Override
	public void initialize() {
		super.initialize();

		updateTextureSources();
		constructSprites();
	}

	@Override
	public void initializeModel() {
		super.initializeModel();

		updateTextureSources();
	}

	private void constructSprites() {
		// Body sprite
		if (mBodyTextureRegion != null)
			makeBodySprite();

		// Reflection
		if (mReflectionTextureRegion != null)
			makeReflectionSprite();

		// ZOrder sort
		sortChildren();
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);

		if (!visible) {
			for (int i = 0; i < getChildCount(); i++) {
				getChild(i).setVisible(false);
			}
		}
		else {
			if (mTitleSprite != null) {
				mTitleSprite.setVisible(true);
			}
			if (mBodySprite != null) {
				mBodySprite.setVisible(true);
				if (mReflectionSprite != null) {
					mReflectionSprite.setVisible(true);
				}
			}
		}
	}

	public void setVisibleAnimated(boolean visible) {
		super.setVisible(visible);

		if (!visible) {
			for (int i = 0; i < getChildCount(); i++) {
				getChild(i).setVisible(false);
			}
		}
		else {
			if (mTitleSprite != null) {
				mTitleSprite.setVisible(true);
			}
			if (mBodySprite != null) {
				mBodySprite.setVisible(true);
				if (mReflectionSprite != null) {
					mReflectionSprite.setVisible(true);
				}
			}
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
		}
		return false;
	}

	protected void doModifiers() {
		final ZoneModel zoneModel = (ZoneModel) mModel;
		if (!zoneModel.getModifiers().isEmpty()) {
			final IEntityModifier modifier = zoneModel.getModifiers().removeFirst();
			modifier.addModifierListener(new IModifierListener<IEntity>() {

				@Override
				public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
					if (modifier instanceof CandiAlphaModifier) {
						if (((CandiAlphaModifier) modifier).getToAlpha() == 0)
							setVisible(false);
					}
					doModifiers();
				}

				@Override
				public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {}
			});
			registerEntityModifier(modifier);
		}
	}

	public void setTexturesLoadedListener(ViewTexturesLoadedListener texturesLoadedListener) {
		mTexturesLoadedListener = texturesLoadedListener;
	}

	@Override
	public void resetTextureSources() {
		super.resetTextureSources();

		updateTextureSources();
	}

	@Override
	public void loadHardwareTextures() {
		super.loadHardwareTextures();

		if (mTexturesLoadedListener != null)
			mTexturesLoadedListener.onTexturesLoaded(this);
	}

	@Override
	protected void updateTextureSources() {
		super.updateTextureSources();

		mBodyTextureRegion = mCandiPatchPresenter.mZoneBodyTextureRegion.clone();
		mReflectionTextureRegion = mCandiPatchPresenter.mZoneReflectionTextureRegion.clone();
	}

	@Override
	public void unloadResources() {
		super.unloadResources();
		/*
		 * Completely remove all resources associated with this sprite.
		 */
		if (mReflectionSprite != null)
			mReflectionSprite.removeResources();
		if (mBodySprite != null) {
			mBodySprite.removeResources();
		}

		if (mReflectionTextureRegion != null)
			BufferObjectManager.getActiveInstance().unloadBufferObject(mReflectionTextureRegion.getTextureBuffer());
		if (mBodyTextureRegion != null)
			BufferObjectManager.getActiveInstance().unloadBufferObject(mBodyTextureRegion.getTextureBuffer());
	}

	@Override
	public ZoneModel getModel() {
		return (ZoneModel) mModel;
	}

	private void makeBodySprite() {
		mBodySprite = new CandiSprite(0, CandiConstants.CANDI_VIEW_TITLE_HEIGHT, mBodyTextureRegion);
		mBodySprite.setBlendFunction(CandiConstants.GL_BLEND_FUNCTION_SOURCE, CandiConstants.GL_BLEND_FUNCTION_DESTINATION);
		mBodySprite.setAlpha(0);
		mBodySprite.setZIndex(0);
		attachChild(mBodySprite);
	}

	private void makeReflectionSprite() {
		mReflectionSprite = new CandiSprite(0, CandiConstants.CANDI_VIEW_TITLE_HEIGHT + CandiConstants.CANDI_VIEW_BODY_HEIGHT,
				mReflectionTextureRegion);
		mReflectionSprite.setBlendFunction(CandiConstants.GL_BLEND_FUNCTION_SOURCE, CandiConstants.GL_BLEND_FUNCTION_DESTINATION);
		mReflectionSprite.setAlpha(0);
		mReflectionSprite.setZIndex(0);
		attachChild(mReflectionSprite);
	}

	public void setViewTouchListener(ViewTouchListener listener) {}

}
