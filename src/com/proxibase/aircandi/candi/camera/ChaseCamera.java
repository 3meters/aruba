package com.proxibase.aircandi.candi.camera;

import static org.anddev.andengine.util.constants.Constants.VERTEX_INDEX_X;
import static org.anddev.andengine.util.constants.Constants.VERTEX_INDEX_Y;

import org.anddev.andengine.engine.camera.ZoomCamera;
import org.anddev.andengine.entity.IEntity;

public class ChaseCamera extends ZoomCamera {

	private IEntity	mChaseEntity;

	public ChaseCamera(float pX, float pY, float pWidth, float pHeight) {
		super(pX, pY, pWidth, pHeight);
	}

	@Override
	public void updateChaseEntity() {
		if (mChaseEntity != null) {
			this.setZoomFactor(mChaseEntity.getScaleX());
			final float[] centerCoordinates = this.mChaseEntity.getSceneCenterCoordinates();
			this.setCenter(centerCoordinates[VERTEX_INDEX_X], centerCoordinates[VERTEX_INDEX_Y]);
		}
	}

	@Override
	public void onUpdate(final float pSecondsElapsed) {
		this.updateChaseEntity();
		// super.onUpdate(pSecondsElapsed);
	}

	@Override
	public void setChaseEntity(final IEntity pChaseEntity) {
		this.mChaseEntity = pChaseEntity;
	}
}
