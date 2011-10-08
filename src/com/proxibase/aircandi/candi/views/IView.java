package com.proxibase.aircandi.candi.views;

import java.util.Observable;

import org.anddev.andengine.entity.IEntity;

import com.proxibase.aircandi.candi.models.IModel;

public interface IView extends IEntity {

	public void update(Observable observable, Object data);

	public void initializeModel();
	
	public void initialize();

	public void setViewTouchListener(ViewTouchListener listener);

	public IModel getModel();

	public void unloadResources();

	public void loadHardwareTextures();

	public interface ViewTouchListener {

		void onViewSingleTap(IView view);

		void onViewDoubleTap(IView view);

		void onViewLongPress(IView view);
	}
	
	public interface ViewTexturesLoadedListener {

		void onTexturesLoaded(IView candiView);
	}
}