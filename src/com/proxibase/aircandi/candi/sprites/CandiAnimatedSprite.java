package com.proxibase.aircandi.candi.sprites;

import javax.microedition.khronos.opengles.GL10;

import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.entity.sprite.AnimatedSprite;
import org.anddev.andengine.opengl.buffer.BufferObjectManager;
import org.anddev.andengine.opengl.texture.region.TiledTextureRegion;
import org.anddev.andengine.opengl.util.GLHelper;

import com.proxibase.aircandi.candi.utils.CandiConstants;

public class CandiAnimatedSprite extends AnimatedSprite {

	public CandiAnimatedSprite(final float x, final float y, final TiledTextureRegion textureRegion) {
		super(x, y, textureRegion);
	}

	@Override
	protected void applyRotation(final GL10 pGL) {

		/* Disable culling so we can see the backside of this sprite. */
		GLHelper.disableCulling(pGL);

		final float rotation = this.mRotation;

		if (rotation != 0) {
			final float rotationCenterX = this.mRotationCenterX;
			final float rotationCenterY = this.mRotationCenterY;

			pGL.glTranslatef(rotationCenterX, rotationCenterY, 0);
			// Note we are applying rotation around the y-axis and not the z-axis anymore!
			pGL.glRotatef(rotation, 0, 1, 0);
			pGL.glTranslatef(-rotationCenterX, -rotationCenterY, 0);
		}
	}

	@Override
	public void setAlpha(float alpha) {
		super.setAlpha(alpha);

		for (int i = 0; i < getChildCount(); i++) {
			getChild(i).setAlpha(alpha);
		}
	}

	public boolean isVisibleToCamera(Camera camera) {
		return !isCulled(camera);
	}
	
	@Override
	public boolean isCulled(Camera pCamera) {
		final float[] coordinates = this.convertLocalToSceneCoordinates(this.mX, this.mY);
		final float x = coordinates[CandiConstants.VERTEX_INDEX_X];
		final float y = coordinates[CandiConstants.VERTEX_INDEX_Y];
		return x > pCamera.getMaxX() || 
			y > pCamera.getMaxY() || 
			x + this.getWidth() < pCamera.getMinX() || 
			y + this.getHeight() < pCamera.getMinY();
	}
	

	@Override
	protected void drawVertices(final GL10 pGL, final Camera pCamera) {

		pGL.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
		super.drawVertices(pGL, pCamera);

		// Enable culling as 'normal' entities profit from culling.
		GLHelper.enableCulling(pGL);
	}

	public void removeResources() {
		BufferObjectManager.getActiveInstance().unloadBufferObject(this.getVertexBuffer());
	}
}
