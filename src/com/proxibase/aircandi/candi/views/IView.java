package com.proxibase.aircandi.candi.views;

import java.util.Observable;

import org.anddev.andengine.entity.IEntity;

import com.proxibase.aircandi.candi.models.IModel;
import com.proxibase.aircandi.candi.views.CandiView.OnCandiViewSingleTapListener;

public interface IView extends IEntity {

	public void update(Observable observable, Object data);

	public void initialize();

	public void setSingleTapListener(OnCandiViewSingleTapListener listener);

	public IModel getModel();

	public void unloadResources();

	public void loadTextures();

}