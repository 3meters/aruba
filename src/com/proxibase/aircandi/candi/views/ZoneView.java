package com.proxibase.aircandi.candi.views;

import java.util.Observable;

import javax.microedition.khronos.opengles.GL10;

import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.modifier.IEntityModifier;
import org.anddev.andengine.opengl.buffer.BufferObjectManager;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.util.modifier.IModifier;
import org.anddev.andengine.util.modifier.IModifier.IModifierListener;

import com.proxibase.aircandi.candi.models.BaseModel;
import com.proxibase.aircandi.candi.models.ZoneModel;
import com.proxibase.aircandi.candi.models.CandiModel.Transition;
import com.proxibase.aircandi.candi.presenters.CandiPatchPresenter;
import com.proxibase.aircandi.candi.sprites.CandiSprite;
import com.proxibase.aircandi.candi.utils.CandiConstants;

public class ZoneView extends BaseView {

	private OnViewTexturesLoadedListener	mTexturesLoadedListener;
	private ZoneModel						mZoneModel;

	private TextureRegion					mBodyTextureRegion;
	private CandiSprite						mBodySprite;
	private TextureRegion					mReflectionTextureRegion;
	private CandiSprite						mReflectionSprite;

	public ZoneView(ZoneModel zoneModel, CandiPatchPresenter candiPatchPresenter) {
		super((BaseModel) zoneModel, candiPatchPresenter);
		this.mZoneModel = zoneModel;
	}

	@Override
	public void update(Observable observable, Object data) {
		super.update(observable, data);

		if (mZoneModel.isVisibleNext()) {
			Transition transition = this.mZoneModel.getTransition();
			if (transition == Transition.FadeIn)
				this.setAlpha(0);
			this.setVisible(true);
		}

		doModifiers();
	}

	@Override
	public void initialize() {
		super.initialize();

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
		if (!mZoneModel.getModifiers().isEmpty()) {
			IEntityModifier modifier = mZoneModel.getModifiers().pop();
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

	public void setTexturesLoadedListener(OnViewTexturesLoadedListener texturesLoadedListener) {
		this.mTexturesLoadedListener = texturesLoadedListener;
	}

	@Override
	public void resetTextures() {
		super.resetTextures();

		loadBodyTextureSources();
	}

	@Override
	public void loadTextures() {
		super.loadTextures();

		if (mTexturesLoadedListener != null)
			mTexturesLoadedListener.onTexturesLoaded(this);
	}

	protected void loadBodyTextureSources() {
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
		return this.mZoneModel;
	}

	private void makeBodySprite() {
		mBodySprite = new CandiSprite(0, CandiConstants.CANDI_VIEW_TITLE_HEIGHT, mBodyTextureRegion);
		mBodySprite.setBlendFunction(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);
		mBodySprite.setAlpha(0);
		mBodySprite.setZIndex(0);
		this.attachChild(mBodySprite);
	}

	private void makeReflectionSprite() {
		mReflectionSprite = new CandiSprite(0, CandiConstants.CANDI_VIEW_TITLE_HEIGHT + CandiConstants.CANDI_VIEW_BODY_HEIGHT,
				mReflectionTextureRegion);
		mReflectionSprite.setBlendFunction(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);
		mReflectionSprite.setAlpha(0);
		mReflectionSprite.setZIndex(0);
		this.attachChild(mReflectionSprite);
	}

	@Override
	public void setSingleTapListener(com.proxibase.aircandi.candi.views.CandiView.OnCandiViewTouchListener listener) {}
}
