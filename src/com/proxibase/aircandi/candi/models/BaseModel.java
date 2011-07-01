package com.proxibase.aircandi.candi.models;

import java.util.LinkedList;
import java.util.Observable;

import org.anddev.andengine.entity.modifier.IEntityModifier;

import com.proxibase.aircandi.utils.CandiList;

public abstract class BaseModel extends Observable implements IModel {

	protected CandiList<CandiModel>			mChildren		= new CandiList<CandiModel>();
	protected IModel						mParent			= null;
	protected boolean						mRoot			= false;
	protected LinkedList<IEntityModifier>	mModifiers		= new LinkedList<IEntityModifier>();
	private boolean							mVisibleCurrent	= false;
	private boolean							mVisibleNext	= false;
	private boolean							mGrouped		= false;
	private String							mTitleText		= "Title";

	public BaseModel() {
		super();
	}

	public void update() {
		/*
		 * The super class only updates observers if hasChanged == true.
		 * Super class also handles clearChanged.
		 */
		this.notifyObservers();
	}

	public boolean isVisible() {
		return mVisibleCurrent;
	}

	public void shiftToNext() {
		mVisibleCurrent = mVisibleNext;
	}

	@Override
	public void setChanged() {
		super.setChanged();
	}

	@Override
	public void clearChanged() {
		super.clearChanged();
	}

	public LinkedList<IEntityModifier> getModifiers() {
		return this.mModifiers;
	}

	public CandiList<CandiModel> getChildren() {
		return this.mChildren;
	}

	public boolean isVisibleCurrent() {
		if (this.mChildren.size() == 0)
			return this.mVisibleCurrent;
		else
			for (CandiModel candiChild : this.mChildren)
				if (candiChild.isVisibleCurrent())
					return true;
		return false;
	}

	public boolean isVisibleNext() {
		if (this.mChildren.size() == 0)
			return this.mVisibleNext;
		else
			for (CandiModel candiChild : this.mChildren)
				if (candiChild.isVisibleNext())
					return true;
		return false;
	}

	public void setVisibleCurrent(boolean visibleCurrent) {
		this.mVisibleCurrent = visibleCurrent;
		this.setChanged();
	}

	public void setVisibleNext(boolean visibleNext) {
		this.mVisibleNext = visibleNext;
		this.setChanged();
	}

	public String getTitleText() {

		String title = mTitleText;
		return title;
	}

	public void setTitleText(String titleText) {

		this.mTitleText = titleText;
		this.setChanged();
	}

	public void setGrouped(boolean grouped) {
		this.mGrouped = grouped;
	}

	public boolean isGrouped() {
		return mGrouped;
	}

	public IModel getParent() {
		return this.mParent;
	}

	public void setParent(IModel parent) {
		this.mParent = parent;
	}

	public boolean isSuperRoot() {
		return this.mRoot;
	}

	public void setRoot(boolean root) {
		this.mRoot = root;
	}

}