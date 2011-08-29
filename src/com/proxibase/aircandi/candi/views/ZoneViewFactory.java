package com.proxibase.aircandi.candi.views;

import com.proxibase.aircandi.candi.models.ZoneModel;
import com.proxibase.aircandi.candi.presenters.CandiPatchPresenter;
import com.proxibase.aircandi.candi.views.BaseView.OnViewTexturesLoadedListener;



public class ZoneViewFactory {
	/*
	 * The job of the builder is to preconfigure resources and setup external references for
	 * a new instance of a view.
	 */
	public static ZoneView createZoneView(final ZoneModel zoneModel, final CandiPatchPresenter candiPatchPresenter) {

		final ZoneView zoneView = new ZoneView(zoneModel, candiPatchPresenter);

		// Link view to the model
		zoneModel.addObserver(zoneView);

		// Get textures loaded and then initialize
		zoneView.setTexturesLoadedListener(new OnViewTexturesLoadedListener() {
			@Override
			public void onTexturesLoaded(IView zoneView) {
				zoneView.initialize();
			}
		});

		zoneView.loadTextures();

		return zoneView;
	}
}
