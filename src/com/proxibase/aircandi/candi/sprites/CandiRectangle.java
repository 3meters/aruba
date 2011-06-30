package com.proxibase.aircandi.candi.sprites;

import javax.microedition.khronos.opengles.GL10;

import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.opengl.buffer.BufferObjectManager;
import org.anddev.andengine.opengl.util.GLHelper;

import com.proxibase.aircandi.controllers.Aircandi;
import com.proxibase.aircandi.utils.Utilities;

public class CandiRectangle extends Rectangle {

	public CandiRectangle(final float x, final float y, final float width, final float height) {
		super(x, y, width, height);
	}

	@Override
	protected void applyRotation(final GL10 pGL) {

		/* Disable culling so we can see the backside of this sprite. */
		GLHelper.disableCulling(pGL);

		final float rotation = this.mRotation;

		if (rotation != 0) {
			Utilities.Log(Aircandi.APP_NAME, "Tricorder", "Sprite: rotation = " + String.valueOf(rotation));

			final float rotationCenterX = this.mRotationCenterX;
			final float rotationCenterY = this.mRotationCenterY;

			pGL.glTranslatef(rotationCenterX, rotationCenterY, 0);
			// Note we are applying rotation around the y-axis and not the z-axis anymore
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

	@Override
	protected void drawVertices(final GL10 pGL, final Camera pCamera) {
		pGL.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
		super.drawVertices(pGL, pCamera);

		// Enable culling as 'normal' entities profit from culling.
		GLHelper.enableCulling(pGL);
	}

	protected void removeResources() {
		BufferObjectManager.getActiveInstance().unloadBufferObject(this.getVertexBuffer());
	}
}