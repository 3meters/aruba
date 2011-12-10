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

import com.proxibase.aircandi.models.PictureEntity;
import com.proxibase.aircandi.utils.Exceptions;
import com.proxibase.aircandi.utils.ImageManager;
import com.proxibase.aircandi.utils.Logger;
import com.proxibase.aircandi.utils.ImageLoader.ImageProfile;
import com.proxibase.aircandi.utils.ImageManager.ImageRequestListener;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;

public class PictureBrowse extends AircandiActivity {

	private ImageView	mPicturePlaceholderProgress;
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
			mEntity = (PictureEntity) ProxibaseService.convertJsonToObject(jsonResponse, PictureEntity.class, GsonType.ProxibaseService);
		}
		catch (ProxibaseException exception) {
			Exceptions.Handle(exception);
		}

		final PictureEntity entity = (PictureEntity) mEntity;

		if (entity.mediaUri != null && !entity.mediaUri.equals("")) {

			ImageManager.getInstance().getImageLoader().fetchImageByProfile(ImageProfile.Original, entity.mediaUri, new ImageRequestListener() {

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
										Logger.d(PictureBrowse.this, "Image fetched: " + entity.mediaUri);
										entity.mediaBitmap = bitmap;
										showPicture(bitmap);
									}
								}
							});
						}

						@Override
						public void onProgressChanged(int progress) {
							mProgressBar.setProgress(progress);
						}
					});

			Logger.d(this, "Fetching Image: " + entity.mediaUri);
		}
	}

	protected void startProgress() {
		mPicturePlaceholderProgress.bringToFront();
		mPicturePlaceholderProgress.post(new Runnable() {

			@Override
			public void run() {
				AnimationDrawable animation = (AnimationDrawable) mPicturePlaceholderProgress.getBackground();
				animation.start();

			}
		});
		//mPhotoPlaceholderProgress.invalidate();
	}

	protected void stopProgress() {
		//mPhotoPlaceholderProgress.setAnimation(null);
	}

	protected void drawEntity() {

		final PictureEntity entity = (PictureEntity) mEntity;

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

	private void showPicture(Bitmap bitmap) {
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
			final PictureEntity entity = (PictureEntity) mEntity;
			if (entity.mediaBitmap != null) {
				entity.mediaBitmap.recycle();
			}
		}
		catch (Exception exception) {
			Exceptions.Handle(exception);
		}
		finally {
			super.onDestroy();
		}
	}

	@Override
	protected int getLayoutID() {
		return R.layout.picture_browse;
	}
}