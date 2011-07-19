package com.proxibase.aircandi.candi.views;

import java.util.Observable;

import org.anddev.andengine.entity.IEntity;

import com.proxibase.aircandi.candi.models.IModel;
import com.proxibase.aircandi.candi.views.CandiView.OnCandiViewTouchListener;

public interface IView extends IEntity {

	public void update(Observable observable, Object data);

	public void initialize();

	public void setSingleTapListener(OnCandiViewTouchListener listener);

	public IModel getModel();

	public void unloadResources();

	public void loadTextures();

}