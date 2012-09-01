package com.aircandi.candi.views;

import java.util.Observable;

import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.opengl.buffer.BufferObjectManager;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import com.aircandi.candi.models.BaseModel;
import com.aircandi.candi.models.ZoneModel;
import com.aircandi.candi.models.BaseModel.ViewState;
import com.aircandi.candi.presenters.CandiPatchPresenter;
import com.aircandi.candi.sprites.CandiSprite;
import com.aircandi.candi.views.ViewAction.ViewActionType;
import com.aircandi.core.CandiConstants;

public class ZoneView extends BaseView {

	private ViewTexturesLoadedListener	mTexturesLoadedListener;

	private TextureRegion				mBodyTextureRegion;
	public CandiSprite					mBodySprite;
	private TextureRegion				mReflectionTextureRegion;
	public CandiSprite					mReflectionSprite;

	// --------------------------------------------------------------------------------------------
	// Initialization
	// --------------------------------------------------------------------------------------------

	public ZoneView() {
		this(null, null);
	}

	public ZoneView(CandiPatchPresenter candiPatchPresenter) {
		this(null, candiPatchPresenter);
	}

	public ZoneView(Object model, CandiPatchPresenter candiPatchPresenter) {
		super(model, candiPatchPresenter);
		setVisible(false);
	}

	@Override
	public void initialize() {
		super.initialize();

		updateTextureRegions(null);

		if (mBodyTextureRegion != null) {
			makeBodySprite();
		}
		if (mReflectionTextureRegion != null) {
			makeReflectionSprite();
		}
		sortChildren();
	}

	@Override
	public void initializeModel() {
		super.initializeModel();

		updateTextureRegions(null);
	}

	// --------------------------------------------------------------------------------------------
	// Primary
	// --------------------------------------------------------------------------------------------

	@Override
	public void update(Observable observable, Object data) {
		super.update(observable, data);

		final ZoneModel zoneModel = (ZoneModel) mModel;
		ViewState viewStateNext = zoneModel.getViewStateNext();

		if (!CandiConstants.TRANSITIONS_ACTIVE) {
			if (viewStateNext.isVisible() != this.isVisible()) {
				setVisible(viewStateNext.isVisible());
			}
		}

		mCandiPatchPresenter.renderingActivateBump();
		mCandiPatchPresenter.getEngine().runOnUpdateThread(new Runnable() {

			@Override
			public void run() {
				clearEntityModifiers();
				clearSpriteModifiers();
				doViewActions();
				doViewModifiers();
			}
		});
	}

	// --------------------------------------------------------------------------------------------
	// UI
	// --------------------------------------------------------------------------------------------

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

				if (viewAction.getViewActionType() == ViewActionType.Position) {
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
			}
		}
	}

	@Override
	public boolean isVisibleToCamera(final Camera camera) {

		if (super.isVisibleToCamera(camera)) {
			return true;
		}
		else {
			if (mBodySprite != null && mBodySprite.isVisibleToCamera(camera)) {
				return true;
			}
			if (mReflectionSprite != null && mReflectionSprite.isVisibleToCamera(camera)) {
				return true;
			}
		}
		return false;
	}

	// --------------------------------------------------------------------------------------------
	// Sprites
	// --------------------------------------------------------------------------------------------

	private void makeBodySprite() {
		mBodySprite = new CandiSprite(0, CandiConstants.CANDI_VIEW_TITLE_HEIGHT, mBodyTextureRegion);
		mBodySprite.setBlendFunction(CandiConstants.GL_BLEND_FUNCTION_SOURCE, CandiConstants.GL_BLEND_FUNCTION_DESTINATION);
		mBodySprite.setZIndex(0);
		mBodySprite.setVisible(true);
		attachChild(mBodySprite);
	}

	private void makeReflectionSprite() {
		mReflectionSprite = new CandiSprite(0, CandiConstants.CANDI_VIEW_TITLE_HEIGHT
				+ CandiConstants.CANDI_VIEW_BODY_HEIGHT
				+ CandiConstants.CANDI_VIEW_REFLECTION_GAP, mReflectionTextureRegion);
		mReflectionSprite.setBlendFunction(CandiConstants.GL_BLEND_FUNCTION_SOURCE, CandiConstants.GL_BLEND_FUNCTION_DESTINATION);
		mReflectionSprite.setZIndex(0);
		mReflectionSprite.setVisible(true);
		attachChild(mReflectionSprite);
	}

	// --------------------------------------------------------------------------------------------
	// Textures
	// --------------------------------------------------------------------------------------------

	public void setTexturesLoadedListener(ViewTexturesLoadedListener texturesLoadedListener) {
		mTexturesLoadedListener = texturesLoadedListener;
	}

	@Override
	public void loadHardwareTextures() {
		super.loadHardwareTextures();

		if (mTexturesLoadedListener != null) {
			mTexturesLoadedListener.onTexturesLoaded(this);
		}
	}

	@Override
	protected void updateTextureRegions(String namePrefix) {
		super.updateTextureRegions("Zone title: ");

		mBodyTextureRegion = mCandiPatchPresenter.mZoneBodyTextureRegion.clone();
		mReflectionTextureRegion = mCandiPatchPresenter.mZoneReflectionTextureRegion.clone();
	}

	@Override
	public void unloadResources() {
		super.unloadResources();

		/* Completely remove all resources associated with this sprite. */
		if (mReflectionSprite != null) {
			mReflectionSprite.removeResources();
		}
		if (mBodySprite != null) {
			mBodySprite.removeResources();
		}

		if (mReflectionTextureRegion != null) {
			BufferObjectManager.getActiveInstance().unloadBufferObject(mReflectionTextureRegion.getTextureBuffer());
		}
		if (mBodyTextureRegion != null) {
			BufferObjectManager.getActiveInstance().unloadBufferObject(mBodyTextureRegion.getTextureBuffer());
		}
	}

	// --------------------------------------------------------------------------------------------
	// Setters/Getters
	// --------------------------------------------------------------------------------------------

	@Override
	public ZoneModel getModel() {
		return (ZoneModel) mModel;
	}

	// --------------------------------------------------------------------------------------------
	// Gestures
	// --------------------------------------------------------------------------------------------

	public void setViewTouchListener(ViewTouchListener listener) {}
}
