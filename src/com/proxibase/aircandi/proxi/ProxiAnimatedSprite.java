package com.proxibase.aircandi.proxi;

import javax.microedition.khronos.opengles.GL10;

import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.entity.sprite.AnimatedSprite;
import org.anddev.andengine.opengl.buffer.BufferObjectManager;
import org.anddev.andengine.opengl.texture.region.TiledTextureRegion;
import org.anddev.andengine.opengl.util.GLHelper;

import com.proxibase.sdk.android.core.ProxibaseService;
import com.proxibase.sdk.android.core.Utilities;


public class ProxiAnimatedSprite extends AnimatedSprite {
	
	public ProxiAnimatedSprite(final float x, final float y, final TiledTextureRegion textureRegion)
	{
		super(x, y, textureRegion);
	}
	
	@Override
	protected void applyRotation(final GL10 pGL) {

		/* Disable culling so we can see the backside of this sprite. */
		GLHelper.disableCulling(pGL);

		final float rotation = this.mRotation;

		if (rotation != 0) {
			Utilities.Log(ProxibaseService.APP_NAME, "Tricorder", "Sprite: rotation = " + String.valueOf(rotation));

			final float rotationCenterX = this.mRotationCenterX;
			final float rotationCenterY = this.mRotationCenterY;

			pGL.glTranslatef(rotationCenterX, rotationCenterY, 0);
			// Note we are applying rotation around the y-axis and not the z-axis anymore!
			pGL.glRotatef(rotation, 0, 1, 0);
			pGL.glTranslatef(-rotationCenterX, -rotationCenterY, 0);
		}
	}
	
	@Override
	public void setAlpha(float alpha)
	{
		super.setAlpha(alpha);
		super.setColor(alpha, alpha, alpha);
	}


	@Override
	protected void drawVertices(final GL10 pGL, final Camera pCamera) {

		pGL.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
		super.drawVertices(pGL, pCamera);

		// Enable culling as 'normal' entities profit from culling.
		GLHelper.enableCulling(pGL);
	}
	protected void removeResources()
	{
		BufferObjectManager.getActiveInstance().unloadBufferObject(this.getVertexBuffer());
	}
	

}
