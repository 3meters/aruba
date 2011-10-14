package com.proxibase.aircandi.candi.views;

public class ViewAction {

	private ViewActionType	mViewActionType;

	public ViewAction(ViewActionType viewActionType) {
		mViewActionType = viewActionType;
	}

	public void setViewActionType(ViewActionType viewActionType) {
		this.mViewActionType = viewActionType;
	}

	public ViewActionType getViewActionType() {
		return mViewActionType;
	}

	public enum ViewActionType {
		ExpandCollapse, ExpandCollapseAnim, ReflectionHideShow, ReflectionHideShowAnim, UpdateTextures, 
		UpdateTexturesForce, Position, Visibility, Scale, ZIndex
	}
}
