package com.proxibase.aircandi.candi.models;

import java.util.LinkedList;

import org.anddev.andengine.entity.modifier.IEntityModifier;

import com.proxibase.aircandi.candi.models.ZoneModel.Position;
import com.proxibase.aircandi.candi.models.ZoneModel.ZoneStatus;
import com.proxibase.aircandi.utils.ImageManager.ImageFormat;
import com.proxibase.sdk.android.proxi.consumer.EntityProxy;

/**
 * @author Jayma
 *         Inputs: Zone, Visibility, Animations
 *         Observers: CandiPatchPresenter, CandiView
 *         Observes: Nothing
 */

public class CandiModel extends BaseModel {

	public static enum Transition {
		None, FadeIn, FadeOut, OverflowIn, OverflowOut, Move, Shift, In, Out
	}

	public static enum DisplayExtra {
		None, Level, Tag, Time
	}

	public static enum ReasonInactive {
		New, Navigation, Hidden, None, Deleting
	}

	private EntityProxy		mEntityProxy		= null;
	private int				mModelId;
	private ZoneModel		mZoneNext			= null;
	private ZoneModel		mZoneCurrent		= null;
	private ZoneStatus		mZoneStatusCurrent	= ZoneStatus.Normal;
	private ZoneStatus		mZoneStatusNext		= ZoneStatus.Normal;
	private Position		mPositionCurrent	= null;
	private Position		mPositionNext		= null;
	private DisplayExtra	mDisplayExtra		= DisplayExtra.None;
	private boolean			mTitleOnly			= false;
	private boolean			mBodyOnly			= false;
	private boolean			mTouchAreaActive	= true;
	private boolean			mRookie				= true;
	private String			mBodyImageId		= "";
	private String			mBodyImageUri		= "";
	private ImageFormat		mBodyImageFormat;
	private boolean			mOverflowCurrent	= false;
	private boolean			mOverflowNext		= false;
	private ReasonInactive	mReasonInactive		= ReasonInactive.None;

	public CandiModel(ModelType modelType, int modelId) {
		super(modelType);
		this.setModelId(modelId);
	}

	public Transition getTransition() {
		/*
		 * If OverflowNext is true then VisibleNext was set to false
		 * But if OverflowNext = false then VisibleNext can be true or false
		 */

		Transition transition = Transition.None;
		if (this.mReasonInactive == ReasonInactive.Deleting) {
			transition = Transition.Out;
		}
		else if (this.isVisibleCurrent() && this.isOverflowNext()) {
			transition = Transition.FadeOut;
		}
		else if (!this.isVisibleCurrent() && this.isVisibleNext() && !this.isOverflowNext()) {
			transition = Transition.FadeIn;
		}
		else if (!this.isVisibleCurrent() && this.isVisibleNext()) {
			transition = Transition.FadeIn;
		}
		else if (this.isVisibleCurrent() && !this.isVisibleNext()) {
			transition = Transition.FadeOut;
		}
		else if (this.isVisibleNext()) {
			if (this.mZoneCurrent.getZoneIndex() != this.mZoneNext.getZoneIndex())
				transition = Transition.Move;
			else {
				if (!mPositionNext.equals(mPositionCurrent))
					transition = Transition.Shift;
			}
		}
		else if (mOverflowNext && !mVisibleCurrent && !mVisibleNext) {
			transition = Transition.FadeOut;
		}
		return transition;
	}

	@Override
	public void shiftToNext() {
		super.shiftToNext();

		mZoneCurrent = mZoneNext;

		Position positionCurrent = this.getPositionCurrent();
		positionCurrent.x = mPositionNext.x;
		positionCurrent.y = mPositionNext.y;
		positionCurrent.scale = mPositionNext.scale;
		positionCurrent.col = mPositionNext.col;
		positionCurrent.row = mPositionNext.row;
		positionCurrent.colFirst = mPositionNext.colFirst;
		positionCurrent.colLast = mPositionNext.colLast;
		positionCurrent.rowFirst = mPositionNext.rowFirst;
		positionCurrent.rowLast = mPositionNext.rowLast;

		mOverflowCurrent = mOverflowNext;
		mZoneStatusCurrent = mZoneStatusNext;
	}

	public EntityProxy getEntityProxy() {
		return this.mEntityProxy;
	}

	public DisplayExtra getDisplayExtra() {
		return mDisplayExtra;
	}

	public int getModelId() {
		return mModelId;
	}

	@Override
	public String getTitleText() {
		String title = super.getTitleText();

		if (mDisplayExtra == DisplayExtra.Level) {
			title += " " + String.valueOf(getEntityProxy().beacon.getAvgBeaconLevel());
		}
		else if (mDisplayExtra == DisplayExtra.Tag) {
			title += " " + String.valueOf(getEntityProxy().beacon.id);
		}
		else if (mDisplayExtra == DisplayExtra.Time) {
			title += " " + String.valueOf(getEntityProxy().beacon.discoveryTime.getTime() / 100);
		}
		return title;
	}

	public ZoneModel getZoneNext() {
		return this.mZoneNext;
	}

	public ZoneModel getZoneCurrent() {
		return this.mZoneCurrent;
	}

	public String getBodyImageId() {
		return this.mBodyImageId;
	}

	public String getBodyImageUri() {
		return this.mBodyImageUri;
	}

	public ImageFormat getBodyImageFormat() {
		return mBodyImageFormat;
	}

	public boolean isTitleOnly() {
		return mTitleOnly;
	}

	public boolean isBodyOnly() {
		return mBodyOnly;
	}

	public boolean isTouchAreaActive() {
		return mTouchAreaActive;
	}

	public void setDisplayExtra(DisplayExtra displayExtra) {
		this.mDisplayExtra = displayExtra;
		super.setChanged();
	}

	public void setModifiers(LinkedList<IEntityModifier> modifiers) {
		this.mModifiers = modifiers;
	}

	public void setEntityProxy(EntityProxy entityProxy) {
		this.mEntityProxy = entityProxy;
		super.setChanged();
	}

	public void setZoneNext(ZoneModel zoneNext) {
		this.mZoneNext = zoneNext;
		super.setChanged();
	}

	public void setZoneCurrent(ZoneModel zoneCurrent) {
		this.mZoneCurrent = zoneCurrent;
		super.setChanged();
	}

	public void setBodyOnly(boolean bodyOnly) {
		this.mBodyOnly = bodyOnly;
		super.setChanged();
	}

	public void setBodyImageId(String bodyImageId) {
		this.mBodyImageId = bodyImageId;
	}

	public void setBodyImageUri(String bodyImageUri) {
		this.mBodyImageUri = bodyImageUri;
	}

	public void setBodyImageFormat(ImageFormat bodyImageFormat) {
		this.mBodyImageFormat = bodyImageFormat;
	}

	public void setTitleOnly(boolean titleOnly) {
		this.mTitleOnly = titleOnly;
	}

	public void setTouchAreaActive(boolean touchAreaActive) {
		this.mTouchAreaActive = touchAreaActive;
	}

	public void setModelId(int modelId) {
		this.mModelId = modelId;
	}

	public void setOverflowNext(boolean overflowNext) {
		this.mOverflowNext = overflowNext;
	}

	public void setRookie(boolean rookie) {
		this.mRookie = rookie;
	}

	public void setOverflowCurrent(boolean overflowCurrent) {
		this.mOverflowCurrent = overflowCurrent;
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

		return this.mModelId == that.mModelId;
	}

	@Override
	public int hashCode() {
		int result = 100;
		result = 31 * result + String.valueOf(this.mModelId).hashCode();
		return result;
	}

	public boolean isRookie() {
		return mRookie;
	}

	public boolean isOverflowCurrent() {
		return mOverflowCurrent;
	}

	public boolean isOverflowNext() {
		return mOverflowNext;
	}

	public void setReasonInactive(ReasonInactive reasonInactive) {
		this.mReasonInactive = reasonInactive;
	}

	public ReasonInactive getReasonInactive() {
		return mReasonInactive;
	}

	public void setZoneStatusCurrent(ZoneStatus zoneStatusCurrent) {
		this.mZoneStatusCurrent = zoneStatusCurrent;
	}

	public ZoneStatus getZoneStatusCurrent() {
		return mZoneStatusCurrent;
	}

	public void setZoneStatusNext(ZoneStatus zoneStatusNext) {
		this.mZoneStatusNext = zoneStatusNext;
	}

	public ZoneStatus getZoneStatusNext() {
		return mZoneStatusNext;
	}

	public void setPositionCurrent(Position positionCurrent) {
		this.mPositionCurrent = positionCurrent;
	}

	public Position getPositionCurrent() {
		if (mPositionCurrent == null)
			mPositionCurrent = new Position();
		return mPositionCurrent;
	}

	public void setPositionNext(Position positionNext) {
		this.mPositionNext = positionNext;
	}

	public Position getPositionNext() {
		if (mPositionNext == null)
			mPositionNext = new Position();
		return mPositionNext;
	}
}