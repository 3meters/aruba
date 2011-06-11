package com.proxibase.aircandi.proxi;

import org.anddev.andengine.opengl.texture.region.TextureRegionFactory;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;

import com.proxibase.aircandi.proxi.ProxiTile.OnProxiTileSingleTapListener;
import com.proxibase.aircandi.utilities.BitmapTextureSource;
import com.proxibase.sdk.android.core.ProxibaseService;
import com.proxibase.sdk.android.core.Utilities;
import com.proxibase.sdk.android.core.proxi.ProxiEntity;

/*
 * The job of the factory is to preconfigure resources and setup external references for
 * a new instance of ProxiTile.
 * 
 * NOTE: Loading a texture can fail if two requests are hitting the same physical asset.
 */

public class ProxiTileBuilder {

	public static ProxiTile createProxiTile(Context context, float x, float y, ProxiEntity proxiEntity, int index, ProxiUiHandler proxiController,
			OnProxiTileSingleTapListener singleTapListener) {

		ProxiTile proxiTile = new ProxiTile(context, x, y, proxiEntity, index, proxiController);

		// Single tap listener
		proxiTile.mSingleTapListener = singleTapListener;

		// Link proxi entity to tile
		proxiEntity.sprite = proxiTile;

		// Textures
		if (proxiEntity.beacon.isLocalOnly) {
			proxiTile.mBodyTextureRegion = proxiController.mTextureRegionGenericTile.clone();
		}
		else {
			boolean imageCached = proxiController.getImageManager().hasImage(proxiEntity.entity.pointResourceId);
			if (!imageCached) {
				proxiController.getImageManager().fetchImageAsynch(proxiEntity.entity.pointResourceId, proxiEntity.imageUrl);
			}
			else {
				Utilities.Log(ProxibaseService.APP_NAME, "Tricorder", "Cache hit for " + proxiEntity.entity.pointResourceId);
				Bitmap bitmap = proxiController.getImageManager().getImage(proxiEntity.entity.pointResourceId);
				if (bitmap != null) {
					Bitmap bitmapCopy = bitmap.copy(Config.ARGB_8888, false);
					proxiTile.mBodyTextureRegion = TextureRegionFactory.createFromSource(proxiTile.getBodyTexture(), new BitmapTextureSource(bitmapCopy), 0, 0);
				}
			}
		}

		proxiTile.mBusyTextureRegion = proxiController.mTextureRegionBusy.clone();

		// Fonts
		proxiTile.mFont = proxiController.mFont;

		// Ready to initialize
		proxiTile.initialize();

		return proxiTile;
	}
}
