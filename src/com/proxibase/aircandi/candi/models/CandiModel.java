package com.proxibase.aircandi.candi.models;

import java.util.LinkedList;

import org.anddev.andengine.entity.modifier.IEntityModifier;

import com.proxibase.aircandi.candi.models.ZoneModel.Position;
import com.proxibase.aircandi.utils.CandiList;
import com.proxibase.sdk.android.proxi.consumer.EntityProxy;

/**
 * @author Jayma
 *         Inputs: Zone, Visibility, Animations
 *         Observers: CandiPatchPresenter, CandiView
 *         Observes: Nothing
 */

public class CandiModel extends BaseModel {

	public static enum Transition {
		None, FadeIn, FadeOut, Move, Shift
	}

	public static enum DisplayExtra {
		None, Level, Tag, Time
	}

	private EntityProxy		mEntityProxy		= null;
	private ZoneModel		mZoneNext			= null;
	private ZoneModel		mZoneCurrent		= null;
	private DisplayExtra	mDisplayExtra		= DisplayExtra.None;
	private boolean			mTitleOnly			= false;
	private boolean			mBodyOnly			= false;
	private boolean			mTouchAreaActive	= true;
	private boolean			mRookie				= true;
	private String			mBodyImageId		= "";
	private String			mBodyImageUrl		= "";

	public CandiModel() {
		this(new EntityProxy());
	}

	public CandiModel(EntityProxy entityProxy) {
		super();
		this.setEntityProxy(entityProxy);
		this.setTitleText(entityProxy.label);
		this.setBodyImageId(entityProxy.imageUri);
		this.setBodyImageUrl(entityProxy.imageUri);
	}

	public Transition getTransition() {

		Transition transition = Transition.None;
		if (!this.isVisibleCurrent() && this.isVisibleNext())
			transition = Transition.FadeIn;
		else if (this.isVisibleCurrent() && !this.isVisibleNext())
			transition = Transition.FadeOut;
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
	}

	public EntityProxy getEntityProxy() {
		return this.mEntityProxy;
	}

	public DisplayExtra getDisplayExtra() {
		return mDisplayExtra;
	}

	@Override
	public String getTitleText() {
		String title = super.getTitleText();

		if (mDisplayExtra == DisplayExtra.Level) {
			title += " " + String.valueOf(getEntityProxy().beacon.getAvgBeaconLevel());
		}
		else if (mDisplayExtra == DisplayExtra.Tag) {
			title += " " + String.valueOf(getEntityProxy().beacon.beaconId);
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

	public String getBodyImageUrl() {
		return this.mBodyImageUrl;
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

	public void setChildren(CandiList<CandiModel> children) {
		this.mChildren = children;
	}

	public void setZoneNext(ZoneModel zoneNext) {
		this.mZoneNext = zoneNext;
		super.setChanged();
	}

	public void setZoneCurrent(ZoneModel zoneCurrent) {
		this.mZoneCurrent = zoneCurrent;
		super.setChanged();
	}

	public void setBodyImageId(String bodyImageId) {
		this.mBodyImageId = bodyImageId;
	}

	public void setBodyImageUrl(String bodyImageUrl) {
		this.mBodyImageUrl = bodyImageUrl;
	}

	public void setTitleOnly(boolean titleOnly) {
		this.mTitleOnly = titleOnly;
	}

	public void setBodyOnly(boolean bodyOnly) {
		this.mBodyOnly = bodyOnly;
		super.setChanged();
	}

	public void setTouchAreaActive(boolean touchAreaActive) {
		this.mTouchAreaActive = touchAreaActive;
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

		return this.mEntityProxy.entityProxyId.equals(that.mEntityProxy.entityProxyId);

	}

	@Override
	public int hashCode() {

		int result = 100;
		result = 31 * result + this.mEntityProxy.entityProxyId.hashCode();
		return result;
	}

	public void setRookie(boolean rookie) {
		this.mRookie = rookie;
	}

	public boolean isRookie() {
		return mRookie;
	}
}