package com.proxibase.aircandi;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.proxibase.aircandi.R;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.models.PhotoEntity;
import com.proxibase.aircandi.utils.ImageManager;
import com.proxibase.aircandi.utils.Logger;
import com.proxibase.aircandi.utils.ImageManager.IImageRequestListener;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException.ProxiErrorCode;

public class PhotoBrowse extends EntityBase {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		bindEntity();
		drawEntity();
	}

	@Override
	protected void bindEntity() {

		String jsonResponse = null;
		try {
			jsonResponse = (String) ProxibaseService.getInstance().select(mEntityProxy.getEntryUri(), ResponseFormat.Json);
		}
		catch (ProxibaseException exception) {
			exception.printStackTrace();
		}

		mEntity = (PhotoEntity) ProxibaseService.convertJsonToObject(jsonResponse, PhotoEntity.class, GsonType.ProxibaseService);
		super.bindEntity();

		final PhotoEntity entity = (PhotoEntity) mEntity;

		if (entity.mediaUri != null && !entity.mediaUri.equals("")) {
			Bitmap bitmap = fetchImage(entity.mediaUri, new IImageRequestListener() {

				@Override
				public void onImageReady(Bitmap bitmap) {
					/*
					 * We can get this callback even when activity has finished.
					 * TODO: Cancel all active tasks in onDestroy()
					 */
					if (mEntity != null) {
						Logger.d(CandiConstants.APP_NAME, "Photo", "Image fetched: " + entity.mediaUri);
						entity.mediaBitmap = bitmap;
						showMediaThumbnail(bitmap);
					}
				}

				@Override
				public void onProxibaseException(ProxibaseException exception) {
					if (exception.getErrorCode() == ProxiErrorCode.OperationFailed) {
						Bitmap bitmap = ImageManager.loadBitmapFromAssets("gfx/placeholder3.png");
						entity.mediaBitmap = bitmap;
						showMediaThumbnail(bitmap);
					}
				}

				@Override
				public boolean onProgressChanged(int progress) {
					return false;
				}
			});
			if (bitmap != null) {
				entity.mediaBitmap = bitmap;
				showMediaThumbnail(bitmap);
			}
		}
	}

	@Override
	protected void drawEntity() {
		super.drawEntity();

		final PhotoEntity entity = (PhotoEntity) mEntity;

		if (entity.mediaBitmap != null) {
			((ImageView) findViewById(R.id.img_media)).setImageBitmap(entity.mediaBitmap);
			((ImageView) findViewById(R.id.img_media)).setVisibility(View.VISIBLE);
		}
		else {
			((ImageView) findViewById(R.id.img_media)).setImageBitmap(null);
			((ImageView) findViewById(R.id.img_media)).setAnimation(null);
			((ImageView) findViewById(R.id.img_media)).setVisibility(View.GONE);
		}
	}

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	private void showMediaThumbnail(Bitmap bitmap) {
		Animation animation = AnimationUtils.loadAnimation(PhotoBrowse.this, R.anim.fade_in_medium);
		animation.setFillEnabled(true);
		animation.setFillAfter(true);
		animation.setStartOffset(500);

		((ImageView) findViewById(R.id.img_media)).setImageBitmap(bitmap);
		((ImageView) findViewById(R.id.img_media)).startAnimation(animation);
		((ImageView) findViewById(R.id.img_media)).setVisibility(View.VISIBLE);
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------
	protected void onDestroy() {

		/* This activity gets destroyed everytime we leave using back or finish(). */
		try {
			final PhotoEntity entity = (PhotoEntity) mEntity;
			if (entity.mediaBitmap != null) {
				entity.mediaBitmap.recycle();
			}
		}
		catch (Exception exception) {
			exception.printStackTrace();
		}
		finally {
			super.onDestroy();
		}
	}

	@Override
	protected int getLayoutID() {
		return R.layout.photo_browse;
	}
}