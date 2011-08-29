package com.proxibase.aircandi.candi.models;

import java.util.LinkedList;
import java.util.Observable;

import org.anddev.andengine.entity.modifier.IEntityModifier;

import com.proxibase.aircandi.utils.CandiList;

public abstract class BaseModel extends Observable implements IModel {

	protected CandiList<IModel>				mChildren		= new CandiList<IModel>();
	protected IModel						mParent			= null;
	protected boolean						mRoot			= false;
	protected LinkedList<IEntityModifier>	mModifiers		= new LinkedList<IEntityModifier>();
	protected boolean						mVisibleCurrent	= false;
	protected boolean						mVisibleNext	= false;
	private boolean							mZoomed			= false;
	private String							mTitleText		= "Title";
	private ModelType						mModelType		= ModelType.Entity;

	public BaseModel() {
		this(ModelType.Entity);
	}

	public BaseModel(ModelType modelType) {
		mModelType = modelType;
	}

	public void update() {
		/*
		 * The super class only updates observers if hasChanged == true.
		 * Super class also handles clearChanged.
		 */
		notifyObservers();
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
		return mModifiers;
	}

	public CandiList<IModel> getChildren() {
		return mChildren;
	}

	public boolean isVisibleCurrent() {
		return mVisibleCurrent;
	}

	public boolean isVisibleNext() {
		return mVisibleNext;
	}

	public void setVisibleCurrent(boolean visibleCurrent) {
		mVisibleCurrent = visibleCurrent;
		setChanged();
	}

	public void setVisibleNext(boolean visibleNext) {
		mVisibleNext = visibleNext;
		setChanged();
	}

	public String getTitleText() {

		String title = mTitleText;
		return title;
	}

	public void setTitleText(String titleText) {

		mTitleText = titleText;
		setChanged();
	}

	public boolean hasVisibleChildrenNext() {
		for (IModel model : mChildren)
			if (model.isVisibleNext())
				return true;

		return false;
	}

	public boolean hasVisibleChildrenCurrent() {
		for (IModel model : mChildren)
			if (model.isVisibleCurrent())
				return true;

		return false;
	}

	public boolean hasSibling() {
		if (mParent == null)
			return false;

		for (IModel model : mParent.getChildren())
			if (model.isVisible() && !model.equals(this))
				return true;

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

	public void setZoomed(boolean zoomed) {
		mZoomed = zoomed;
		setChanged();
	}

	public boolean isZoomed() {
		return mZoomed;
	}

	public void setModelType(ModelType modelType) {
		mModelType = modelType;
	}

	public ModelType getModelType() {
		return mModelType;
	}

	public enum ModelType {
		Root, Entity
	}
}