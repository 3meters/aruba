package com.proxibase.aircandi;

import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.models.PhotoEntity;
import com.proxibase.aircandi.utils.ImageManager;
import com.proxibase.aircandi.utils.Logger;
import com.proxibase.aircandi.utils.ImageManager.IImageRequestListener;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest.ImageFormat;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest.ImageShape;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException.ProxiErrorCode;

public class PhotoBrowse extends AircandiActivity {

	private ImageView	mPhotoPlaceholderProgress;
	private ViewFlipper	mViewFlipper;
	private ProgressBar	mProgressBar;
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		configure();
		bindEntity();
		drawEntity();
	}

	protected void bindEntity() {

		String jsonResponse = null;
		try {
			jsonResponse = (String) ProxibaseService.getInstance().select(mEntityProxy.getEntryUri(), ResponseFormat.Json, null);
			mEntity = (PhotoEntity) ProxibaseService.convertJsonToObject(jsonResponse, PhotoEntity.class, GsonType.ProxibaseService);
		}
		catch (ProxibaseException exception) {
			exception.printStackTrace();
		}

		final PhotoEntity entity = (PhotoEntity) mEntity;

		if (entity.mediaUri != null && !entity.mediaUri.equals("")) {

			ImageRequest imageRequest = new ImageRequest(entity.mediaUri, ImageShape.Native, ImageFormat.Binary, false,
					CandiConstants.IMAGE_WIDTH_ORIGINAL, false, false, 1,
					this, new IImageRequestListener() {

						@Override
						public void onImageReady(final Bitmap bitmap) {
							/*
							 * We can get this callback even when activity has finished.
							 * TODO: Cancel all active tasks in onDestroy()
							 */
							runOnUiThread(new Runnable() {

								@Override
								public void run() {
									if (mEntity != null) {
										Logger.d(CandiConstants.APP_NAME, "Photo", "Image fetched: " + entity.mediaUri);
										entity.mediaBitmap = bitmap;
										showPhoto(bitmap);
									}
								}
							});
						}

						@Override
						public void onProxibaseException(ProxibaseException exception) {
							if (exception.getErrorCode() == ProxiErrorCode.OperationFailed) {
								Bitmap bitmap = ImageManager.getInstance().loadBitmapFromAssets("gfx/placeholder3.png");
								entity.mediaBitmap = bitmap;
								showPhoto(bitmap);
							}
						}

						@Override
						public boolean onProgressChanged(int progress) {
							Logger.v(CandiConstants.APP_NAME, "Photo", "Image fetch: " + entity.mediaUri + " progress: " + String.valueOf(progress));
							mProgressBar.setProgress(progress);
							return false;
						}
					});

			Logger.d(CandiConstants.APP_NAME, "Photo", "Fetching Image: " + entity.mediaUri);
			ImageManager.getInstance().getImageLoader().fetchImage(imageRequest, true);
		}
	}

	protected void startProgress() {
		mPhotoPlaceholderProgress.bringToFront();
		mPhotoPlaceholderProgress.post(new Runnable() {

			@Override
			public void run() {
				AnimationDrawable animation = (AnimationDrawable) mPhotoPlaceholderProgress.getBackground();
				animation.start();

			}
		});
		//mPhotoPlaceholderProgress.invalidate();
	}

	protected void stopProgress() {
		//mPhotoPlaceholderProgress.setAnimation(null);
	}

	protected void drawEntity() {

		final PhotoEntity entity = (PhotoEntity) mEntity;

		if (findViewById(R.id.txt_title) != null)
			((TextView) findViewById(R.id.txt_title)).setText(entity.title);

		if (findViewById(R.id.txt_content) != null)
			((TextView) findViewById(R.id.txt_content)).setText(entity.description);

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

	private void configure() {
		//mPhotoPlaceholderProgress = (ImageView) findViewById(R.id.img_photo_progress_indicator);
		mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
		
		mViewFlipper = (ViewFlipper) findViewById(R.id.flipper_form);
		mViewFlipper.setDisplayedChild(1);
		mViewFlipper.setInAnimation(this, R.anim.fade_in_medium);
		mViewFlipper.setOutAnimation(this, R.anim.fade_out_medium);

		mContextButton = (Button) findViewById(R.id.btn_context);
		if (mContextButton != null) {
			mContextButton.setVisibility(View.INVISIBLE);
			showBackButton(true, getString(R.string.post_back_button));
		}
	}

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	private void showPhoto(Bitmap bitmap) {
		((ImageView) findViewById(R.id.img_media)).setImageBitmap(bitmap);
		stopProgress();
		mViewFlipper.setDisplayedChild(0);
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