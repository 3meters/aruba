package com.proxibase.aircandi.candi.models;

import java.util.LinkedList;

import org.anddev.andengine.entity.modifier.IEntityModifier;

import com.proxibase.aircandi.utils.CandiList;

public interface IModel {

	public void update();

	public boolean isVisible();

	public void shiftToNext();

	public LinkedList<IEntityModifier> getModifiers();

	public CandiList<IModel> getChildren();

	public void setVisibleCurrent(boolean visibleCurrent);

	public void setVisibleNext(boolean visibleNext);

	public boolean isVisibleCurrent();

	public boolean isVisibleNext();

	public String getTitleText();

	public IModel getParent();

	public void setParent(IModel parent);

	public boolean isSuperRoot();

	public void setRoot(boolean root);

}