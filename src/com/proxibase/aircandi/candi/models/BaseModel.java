package com.proxibase.aircandi.candi.models;

import java.util.LinkedList;
import java.util.Observable;

import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.entity.modifier.IEntityModifier;

import com.proxibase.aircandi.candi.views.ViewAction;
import com.proxibase.aircandi.components.CandiList;
import com.proxibase.aircandi.components.Exceptions;
import com.proxibase.aircandi.core.CandiConstants;

public abstract class BaseModel extends Observable implements IModel {

	protected CandiPatchModel				mCandiPatchModel	= null;
	protected CandiList<IModel>				mChildren			= new CandiList<IModel>();
	protected IModel						mParent				= null;
	protected boolean						mRoot				= false;
	protected LinkedList<IEntityModifier>	mViewModifiers		= new LinkedList<IEntityModifier>();
	private LinkedList<ViewAction>			mViewActions		= new LinkedList<ViewAction>();
	protected ViewState						mViewStateCurrent	= new ViewState();
	protected ViewState						mViewStateNext		= new ViewState();
	private String							mTitleText			= "";

	public BaseModel() {}

	public void update() {
		/*
		 * The super class only updates observers if hasChanged == true.
		 * Super class also handles clearChanged.
		 */
		notifyObservers();
	}

	@Override
	public void setChanged() {
		super.setChanged();
	}

	@Override
	public void clearChanged() {
		super.clearChanged();
	}

	public LinkedList<IEntityModifier> getViewModifiers() {
		return mViewModifiers;
	}

	public CandiList<IModel> getChildren() {
		return mChildren;
	}

	public String getTitleText() {
		return mTitleText;
	}

	public void setTitleText(String titleText) {
		mTitleText = titleText;
	}

	public boolean hasVisibleChildrenNext() {
		for (IModel model : mChildren) {
			if (model.getViewStateNext().isVisible()) {
				return true;
			}
		}
		return false;
	}

	public boolean hasVisibleChildrenCurrent() {
		for (IModel model : mChildren) {
			if (model.getViewStateCurrent().isVisible()) {
				return true;
			}
		}
		return false;
	}

	public int visibleChildrenNextCount() {
		int visibleCount = 0;
		for (IModel model : mChildren) {
			if (model.getViewStateNext().isVisible()) {
				visibleCount++;
			}
		}
		return visibleCount;
	}

	public int visibleChildrenCurrent() {
		int visibleCount = 0;
		for (IModel model : mChildren) {
			if (model.getViewStateCurrent().isVisible()) {
				visibleCount++;
			}
		}
		return visibleCount;
	}

	public boolean hasSibling() {
		if (mParent == null) {
			return false;
		}
		for (IModel model : mParent.getChildren()) {
			if (model.getViewStateCurrent().isVisible() && !model.equals(this)) {
				return true;
			}
		}
		return false;
	}

	public IModel getParent() {
		return mParent;
	}

	public void setParent(IModel parent) {
		mParent = parent;
	}

	public boolean isSuperRoot() {
		return mRoot;
	}

	public void setSuperRoot(boolean root) {
		mRoot = root;
	}

	public ViewState getViewStateCurrent() {
		return mViewStateCurrent;
	}

	public ViewState getViewStateNext() {
		return mViewStateNext;
	}

	public void setViewActions(LinkedList<ViewAction> viewActions) {
		this.mViewActions = viewActions;
	}

	public LinkedList<ViewAction> getViewActions() {
		return mViewActions;
	}

	public static class ViewState {

		private float	mX;
		private float	mY;
		private float	mHeight			= CandiConstants.CANDI_VIEW_BODY_HEIGHT;
		private float	mWidth			= CandiConstants.CANDI_VIEW_WIDTH;
		private int		mZIndex;
		private float	mScale			= CandiConstants.CANDI_VIEW_SCALE;
		private boolean	mZoomed			= false;
		private boolean	mVisible		= false;
		private boolean	mLastWithinHalo	= false;
		private boolean	mHasReflection	= true;
		private boolean	mCollapsed		= false;
		private boolean	mOkToAnimate	= true;
		private float	mAlpha			= 1;

		public ViewState() {}

		public boolean isVisibleToCamera(Camera camera) {
			return (!isCulled(camera));
		}

		public boolean isWithinHalo(Camera camera) {
			mLastWithinHalo = !isOutsideHalo(camera);
			return (mLastWithinHalo);
		}

		private boolean isCulled(Camera camera) {
			return mX > camera.getMaxX() 
					|| mY > camera.getMaxY()
					|| mX + (mWidth * mScale) < camera.getMinX()
					|| mY + (mHeight * mScale) < camera.getMinY();
		}

		private boolean isOutsideHalo(Camera camera) {
			//			float haloMinX = camera.getMinX() - (CandiConstants.CANDI_VIEW_HALO * mCandiPatchModel.getScreenWidth());
			//			float haloMaxX = camera.getMaxX() + (CandiConstants.CANDI_VIEW_HALO * mCandiPatchModel.getScreenWidth());

			float haloMinX = camera.getMinX() - (CandiConstants.CANDI_VIEW_HALO * camera.getWidth());
			float haloMaxX = camera.getMaxX() + (CandiConstants.CANDI_VIEW_HALO * camera.getWidth());

			//			if (haloMinX < 0) {
			//				haloMaxX += Math.abs(haloMinX);
			//				haloMinX = 0;
			//			}
			//			else if (haloMaxX > lastX) {
			//				haloMinX -= Math.abs(lastX - haloMaxX);
			//				haloMaxX = lastX;
			//			}

			boolean outsideHalo = mX + (mWidth * mScale) < haloMinX || mX > haloMaxX;
			
			return outsideHalo;
		}

		public float getX() {
			return this.mX;
		}

		public float getY() {
			return this.mY;
		}

		public float getCenterX() {
			return this.mX + (this.mWidth * 0.5f);
		}

		public float getCenterY() {
			return this.mY + (this.mHeight * 0.5f);
		}

		public float getHeight() {
			return this.mHeight;
		}

		public float getWidth() {
			return this.mWidth;
		}

		public int getZIndex() {
			return this.mZIndex;
		}

		public float getScale() {
			return this.mScale;
		}

		public boolean isZoomed() {
			return this.mZoomed;
		}

		public boolean isVisible() {
			return this.mVisible;
		}

		public void setX(float x) {
			this.mX = x;
		}

		public void setY(float y) {
			this.mY = y;
		}

		public void setHeight(float height) {
			this.mHeight = height;
		}

		public void setWidth(float width) {
			this.mWidth = width;
		}

		public void setZIndex(int zIndex) {
			this.mZIndex = zIndex;
		}

		public void setScale(float scale) {
			this.mScale = scale;
		}

		public void setZoomed(boolean zoomed) {
			this.mZoomed = zoomed;
		}

		public ViewState setVisible(boolean visible) {
			this.mVisible = visible;
			return this;
		}

		@Override
		public boolean equals(Object other) {

			if (this == other) {
				return true;
			}
			if (!(other instanceof ViewState)) {
				return false;
			}
			ViewState that = (ViewState) other;
			return (this.mX == that.mX && this.mY == that.mY && this.mScale == that.mScale);
		}

		@Override
		protected Object clone() {
			try {
				return super.clone();
			}
			catch (CloneNotSupportedException exception) {
				Exceptions.Handle(exception);
				return null;
			}
		}

		@Override
		public int hashCode() {

			int result = 100;
			result = 31 * result + (int) this.mX;
			result = 31 * result + (int) this.mY;
			result = 31 * result + (int) this.mScale;
			return result;
		}

		public void setHasReflection(boolean hasReflection) {
			this.mHasReflection = hasReflection;
		}

		public boolean reflectionActive() {
			return mHasReflection;
		}

		public void setCollapsed(boolean collapsed) {
			this.mCollapsed = collapsed;
		}

		public boolean isCollapsed() {
			return mCollapsed;
		}

		public void setOkToAnimate(boolean okToAnimate) {
			this.mOkToAnimate = okToAnimate;
		}

		public boolean isOkToAnimate() {
			return mOkToAnimate;
		}

		public void setAlpha(float alpha) {
			this.mAlpha = alpha;
		}

		public float getAlpha() {
			return mAlpha;
		}

		public void setLastWithinHalo(boolean lastWithinHalo) {
			this.mLastWithinHalo = lastWithinHalo;
		}

		public boolean isLastWithinHalo() {
			return mLastWithinHalo;
		}

	}

	public static enum ModelType {
		Root, Entity
	}
}