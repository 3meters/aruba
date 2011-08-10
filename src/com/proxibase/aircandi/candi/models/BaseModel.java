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
	private boolean							mVisibleCurrent	= false;
	private boolean							mVisibleNext	= false;
	private boolean							mZoomed			= false;
	private String							mTitleText		= "Title";
	private ModelType						mModelType		= ModelType.Entity;

	public BaseModel() {
		this(ModelType.Entity);
	}

	public BaseModel(ModelType modelType) {
		this.mModelType = modelType;
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

	public CandiList<IModel> getChildren() {
		return this.mChildren;
	}

	public boolean isVisibleCurrent() {
		return this.mVisibleCurrent;
	}

	public boolean isVisibleNext() {
		return this.mVisibleNext;
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

	public boolean hasChildren() {
		for (IModel model : this.mChildren)
			if (model.isVisible())
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

	public void setZoomed(boolean zoomed) {
		this.mZoomed = zoomed;
		this.setChanged();
	}

	public boolean isZoomed() {
		return mZoomed;
	}

	public void setModelType(ModelType modelType) {
		this.mModelType = modelType;
	}

	public ModelType getModelType() {
		return mModelType;
	}

	public enum ModelType {
		Root, Entity
	}
}