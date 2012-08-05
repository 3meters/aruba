package com.proxibase.aircandi.candi.models;

import com.proxibase.aircandi.candi.models.ZoneModel.ZoneAlignment;
import com.proxibase.aircandi.candi.models.ZoneModel.ZoneStatus;
import com.proxibase.service.objects.Entity;

/**
 * @author Jayma
 *         Inputs: Zone, Visibility, Animations
 *         Observers: CandiPatchPresenter, CandiView
 *         Observes: Nothing
 */

public class CandiModel extends BaseModel {

	private Entity			mEntity				= null;
	private String			mModelId;
	private ZoneState		mZoneStateCurrent	= new ZoneState();
	private ZoneState		mZoneStateNext		= new ZoneState();
	private DisplayExtra	mDisplayExtra		= DisplayExtra.None;
	private boolean			mTouchAreaActive	= false;
	private boolean			mRookie				= true;
	private boolean			mDeleted			= false;
	private int				mPriority			= 0;
	private boolean			mMasterImageUpdated	= false;
	private ReasonInactive	mReasonInactive		= ReasonInactive.None;

	public CandiModel(String modelId, CandiPatchModel candiPatchModel) {
		super();
		setModelId(modelId);
		mCandiPatchModel = candiPatchModel;
	}

	public Transition getTransition() {
		/*
		 * If OverflowNext is true then VisibleNext was set to false
		 * But if OverflowNext = false then VisibleNext can be true or false
		 */
		Transition transition = Transition.None;

		if (mReasonInactive == ReasonInactive.Deleting) {
			transition = Transition.Out;
		}
		else if (mViewStateCurrent.isVisible() && mZoneStateNext.isOverflow()) {
			transition = Transition.OverflowOut;
		}
		else if (!mViewStateCurrent.isVisible() && mViewStateNext.isVisible() && mZoneStateCurrent.isOverflow()) {
			transition = Transition.OverflowIn;
		}
		else if (!mViewStateCurrent.isVisible() && mViewStateNext.isVisible() && !mZoneStateNext.isOverflow()) {
			transition = Transition.FadeIn;
		}
		else if (!mViewStateCurrent.isVisible() && mViewStateNext.isVisible()) {
			transition = Transition.FadeIn;
		}
		else if (mViewStateCurrent.isVisible() && !mViewStateNext.isVisible()) {
			transition = Transition.FadeOut;
		}
		else if (mViewStateNext.isVisible()) {
			if (mZoneStateCurrent.getZone().getZoneIndex() != mZoneStateNext.getZone().getZoneIndex()) {
				transition = Transition.Move;
			}
			else {
				if (!mViewStateNext.equals(mViewStateCurrent)) {
					transition = Transition.Shift;
				}
			}
		}
		return transition;
	}

	public void shiftToNext() {
		mViewStateCurrent.setX(mViewStateNext.getX());
		mViewStateCurrent.setY(mViewStateNext.getY());
		mViewStateCurrent.setHeight(mViewStateNext.getHeight());
		mViewStateCurrent.setWidth(mViewStateNext.getWidth());
		mViewStateCurrent.setScale(mViewStateNext.getScale());
		mViewStateCurrent.setVisible(mViewStateNext.isVisible());
		mViewStateCurrent.setZIndex(mViewStateNext.getZIndex());
		mViewStateCurrent.setZoomed(mViewStateNext.isZoomed());
		mViewStateCurrent.setCollapsed(mViewStateNext.isCollapsed());
		mViewStateCurrent.setHasReflection(mViewStateNext.reflectionActive());
		mViewStateCurrent.setOkToAnimate(mViewStateNext.isOkToAnimate());
		mViewStateCurrent.setAlpha(mViewStateNext.getAlpha());
		mViewStateCurrent.setLastWithinHalo(mViewStateNext.isLastWithinHalo());

		mZoneStateCurrent.setAlignment(mZoneStateNext.getAlignment());
		mZoneStateCurrent.setOverflow(mZoneStateNext.isOverflow());
		mZoneStateCurrent.setStatus(mZoneStateNext.getStatus());
		mZoneStateCurrent.setZone(mZoneStateNext.getZone());
	}

	public Entity getEntity() {
		return mEntity;
	}

	public DisplayExtra getDisplayExtra() {
		return mDisplayExtra;
	}

	public String getModelId() {
		return mModelId;
	}

	public String getModelIdAsString() {
		return String.valueOf(mModelId);
	}

	@Override
	public String getTitleText() {
		String title = super.getTitleText();
		String displayExtra = null;

		if (mEntity.beacon != null) {
			if (mDisplayExtra == DisplayExtra.Level) {
				displayExtra = String.valueOf(mEntity.beacon.getAvgBeaconLevel());
			}
			else if (mDisplayExtra == DisplayExtra.Tag) {
				displayExtra = String.valueOf(mEntity.beacon.id);
			}
			else if (mDisplayExtra == DisplayExtra.Time) {
				displayExtra = String.valueOf(mEntity.beacon.discoveryTime.getTime() / 100);
			}
		}

		if (displayExtra != null) {
			title += " " + displayExtra;
		}

		return title;
	}

	public boolean isTouchAreaActive() {
		return mTouchAreaActive;
	}

	public void setDisplayExtra(DisplayExtra displayExtra) {
		mDisplayExtra = displayExtra;
	}

	public void setEntity(Entity entity) {
		mEntity = entity;
	}

	public void setTouchAreaActive(boolean touchAreaActive) {
		mTouchAreaActive = touchAreaActive;
	}

	public void setModelId(String modelId) {
		mModelId = modelId;
	}

	public void setRookie(boolean rookie) {
		mRookie = rookie;
	}

	@Override
	public boolean equals(Object other) {

		if (this == other) {
			return true;
		}
		if (!(other instanceof CandiModel)) {
			return false;
		}
		CandiModel that = (CandiModel) other;

		return mModelId == that.mModelId;
	}

	@Override
	public int hashCode() {
		int result = 100;
		result = 31 * result + String.valueOf(mModelId).hashCode();
		return result;
	}

	public boolean isRookie() {
		return mRookie;
	}

	public void setReasonInactive(ReasonInactive reasonInactive) {
		mReasonInactive = reasonInactive;
	}

	public ReasonInactive getReasonInactive() {
		return mReasonInactive;
	}

	public ZoneState getZoneStateCurrent() {
		return this.mZoneStateCurrent;
	}

	public ZoneState getZoneStateNext() {
		return this.mZoneStateNext;
	}

	public void setZoneStateCurrent(ZoneState zoneStateCurrent) {
		this.mZoneStateCurrent = zoneStateCurrent;
	}

	public void setZoneStateNext(ZoneState zoneStateNext) {
		this.mZoneStateNext = zoneStateNext;
	}

	public void setDeleted(boolean deleted) {
		this.mDeleted = deleted;
	}

	public boolean isDeleted() {
		return mDeleted;
	}

	public void setMasterImageUpdated(boolean masterImageUpdated) {
		this.mMasterImageUpdated = masterImageUpdated;
	}

	public boolean isMasterImageUpdated() {
		return mMasterImageUpdated;
	}

	public int getPriority() {
		return mPriority;
	}

	public void setPriority(int priority) {
		mPriority = priority;
	}

	public static class ZoneState {

		private ZoneModel		mZone		= null;
		private ZoneStatus		mStatus		= ZoneStatus.Normal;
		private ZoneAlignment	mAlignment	= ZoneAlignment.None;
		private boolean			mOverflow	= false;

		public ZoneState() {}

		public void setZone(ZoneModel zone) {
			this.mZone = zone;
		}

		public ZoneModel getZone() {
			return mZone;
		}

		public void setStatus(ZoneStatus status) {
			this.mStatus = status;
		}

		public ZoneStatus getStatus() {
			return mStatus;
		}

		public void setAlignment(ZoneAlignment alignment) {
			this.mAlignment = alignment;
		}

		public ZoneAlignment getAlignment() {
			return mAlignment;
		}

		public void setOverflow(boolean overflow) {
			this.mOverflow = overflow;
		}

		public boolean isOverflow() {
			return mOverflow;
		}
	}

	public static enum Transition {
		None, FadeIn, FadeOut, OverflowIn, OverflowOut, Move, Shift, In, Out
	}

	public static enum DisplayExtra {
		None, Level, Tag, Time
	}

	public static enum ReasonInactive {
		New, Navigation, Hidden, None, Deleting
	}

}