package com.proxibase.aircandi.candi.models;

import java.util.LinkedList;

import org.anddev.andengine.entity.modifier.IEntityModifier;

import com.proxibase.aircandi.candi.models.ZoneModel.Position;
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
		None, FadeIn, FadeOut, OverflowIn, OverflowOut, Move, Shift
	}

	public static enum DisplayExtra {
		None, Level, Tag, Time
	}

	public static enum ReasonInactive {
		New, Navigation, Hidden, None
	}

	private EntityProxy		mEntityProxy		= null;
	private String			mModelId;
	private ZoneModel		mZoneNext			= null;
	private ZoneModel		mZoneCurrent		= null;
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

	public CandiModel(ModelType modelType, String modelId) {
		super(modelType);
		this.setModelId(modelId);
	}

	public Transition getTransition() {

		Transition transition = Transition.None;
		if (this.isVisibleCurrent() && this.isOverflowNext()) {
			transition = Transition.FadeOut;
		}
		else if (!this.isVisibleCurrent() && !this.isOverflowNext()) {
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
				Position positionCurrent = getZoneCurrent().getChildPositionCurrent(this);
				Position positionNext = getZoneNext().getChildPositionNext(this);
				if (!positionNext.equals(positionCurrent))
					transition = Transition.Shift;
			}
		}
		return transition;
	}

	@Override
	public void shiftToNext() {
		super.shiftToNext();

		mZoneCurrent = mZoneNext;
		mOverflowCurrent = mOverflowNext;
	}

	public EntityProxy getEntityProxy() {
		return this.mEntityProxy;
	}

	public DisplayExtra getDisplayExtra() {
		return mDisplayExtra;
	}

	public String getModelId() {
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

	public void setModelId(String modelId) {
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

		return this.mModelId.equals(that.mModelId);
	}

	@Override
	public int hashCode() {
		int result = 100;
		result = 31 * result + this.mModelId.hashCode();
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
}