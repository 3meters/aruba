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
			setZoomFactor(mChaseEntity.getScaleX());
			final float[] centerCoordinates = mChaseEntity.getSceneCenterCoordinates();
			setCenter(centerCoordinates[VERTEX_INDEX_X], centerCoordinates[VERTEX_INDEX_Y]);
		}
	}

	@Override
	public void onUpdate(final float pSecondsElapsed) {
		this.updateChaseEntity();
	}

	@Override
	public void setChaseEntity(final IEntity pChaseEntity) {
		this.mChaseEntity = pChaseEntity;
	}
}
