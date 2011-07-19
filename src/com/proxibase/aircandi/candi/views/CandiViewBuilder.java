package com.proxibase.aircandi.candi.views;

import com.proxibase.aircandi.candi.models.CandiModel;
import com.proxibase.aircandi.candi.presenters.CandiPatchPresenter;
import com.proxibase.aircandi.candi.views.BaseView.OnViewTexturesLoadedListener;
import com.proxibase.aircandi.candi.views.CandiView.OnCandiViewTouchListener;


public class CandiViewBuilder {
	/*
	 * The job of the factory is to preconfigure resources and setup external references for
	 * a new instance of ProxiTile.
	 * 
	 * NOTE: Loading a texture can fail if two requests are hitting the same physical asset.
	 */
	public static CandiView createCandiView(final CandiModel candiModel, final CandiPatchPresenter candiPatchPresenter,
			OnCandiViewTouchListener singleTapListener) {

		final CandiView candiView = new CandiView(candiModel, candiPatchPresenter);

		// We start out hidden
		candiView.setAlpha(0);

		// Single tap listener
		candiView.setSingleTapListener(singleTapListener);

		// Link view to the model
		candiModel.addObserver(candiView);

		// Get textures loaded and then initialize
		candiView.setTexturesLoadedListener(new OnViewTexturesLoadedListener() {
			@Override
			public void onTexturesLoaded(IView candiView) {
				candiView.initialize();
			}
		});

		candiView.loadTextures();

		return candiView;
	}
}
