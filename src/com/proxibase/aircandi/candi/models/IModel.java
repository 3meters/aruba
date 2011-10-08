package com.proxibase.aircandi.candi.models;

import java.util.LinkedList;

import org.anddev.andengine.entity.modifier.IEntityModifier;

import com.proxibase.aircandi.candi.models.BaseModel.ViewState;
import com.proxibase.aircandi.utils.CandiList;

public interface IModel {

	public void update();

	public void shiftToNext();

	public LinkedList<IEntityModifier> getModifiers();

	public CandiList<IModel> getChildren();

	public String getTitleText();

	public IModel getParent();

	public ViewState getViewStateCurrent();

	public ViewState getViewStateNext();

	public void setParent(IModel parent);

	public boolean isSuperRoot();

	public void setSuperRoot(boolean root);

}