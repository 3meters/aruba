package com.proxibase.aircandi;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.proxibase.aircandi.components.Exceptions;
import com.proxibase.aircandi.components.Logger;
import com.proxibase.aircandi.components.NetworkManager;
import com.proxibase.aircandi.components.ImageManager.ImageRequest;
import com.proxibase.aircandi.components.ImageManager.ImageRequest.ImageShape;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.models.PictureEntity;
import com.proxibase.aircandi.widgets.WebImageView;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ServiceRequest;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestListener;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;

public class PictureBrowse extends CandiActivity {

	private ImageView	mPicturePlaceholderProgress;
	private ViewFlipper	mViewFlipper;
	private ProgressBar	mProgressBar;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		configure();
		bindEntity();
		drawEntity();
		GoogleAnalyticsTracker.getInstance().trackPageView("/PictureBrowse");

	}

	protected void bindEntity() {

		ServiceResponse serviceResponse = NetworkManager.getInstance().request(
				new ServiceRequest(mEntityProxy.getEntryUri(), RequestType.Get, ResponseFormat.Json));

		if (serviceResponse.responseCode != ResponseCode.Success) {
			setResult(Activity.RESULT_CANCELED);
			finish();
			overridePendingTransition(R.anim.hold, R.anim.fade_out_medium);
		}
		else {
			String jsonResponse = (String) serviceResponse.data;
			mEntity = (PictureEntity) ProxibaseService.convertJsonToObject(jsonResponse, PictureEntity.class, GsonType.ProxibaseService);

			final PictureEntity entity = (PictureEntity) mEntity;
			WebImageView mediaImageView = (WebImageView) findViewById(R.id.image_media);

			if (entity.mediaUri != null && entity.mediaUri.length() > 0) {

				ImageRequest imageRequest = new ImageRequest(entity.mediaUri, ImageShape.Native, "binary", false,
						CandiConstants.IMAGE_WIDTH_ORIGINAL, false, false, false, 1, this, new RequestListener() {

							@Override
							public void onComplete(Object response) {

								ServiceResponse serviceResponse = (ServiceResponse) response;
								if (serviceResponse.responseCode != ResponseCode.Success) {
									return;
								}
								else {
									Logger.d(PictureBrowse.this, "Image fetched: " + entity.mediaUri);
									entity.mediaBitmap = (Bitmap) serviceResponse.data;
									runOnUiThread(new Runnable() {

										@Override
										public void run() {
											stopProgress();
											mViewFlipper.setDisplayedChild(0);
										}
									});
								}
							}

							@Override
							public void onProgressChanged(int progress) {
								mProgressBar.setProgress(progress);
							}
						});

				Logger.d(this, "Fetching Image: " + entity.mediaUri);
				mediaImageView.setImageRequest(imageRequest, null);
			}
			GoogleAnalyticsTracker.getInstance().dispatch();
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

		if (findViewById(R.id.text_title) != null) {
			((TextView) findViewById(R.id.text_title)).setText(entity.title);
		}

		if (findViewById(R.id.text_content) != null) {
			((TextView) findViewById(R.id.text_content)).setText(entity.description);
		}

		if (entity.mediaBitmap != null) {
			((ImageView) findViewById(R.id.image_media)).setImageBitmap(entity.mediaBitmap);
			((ImageView) findViewById(R.id.image_media)).setVisibility(View.VISIBLE);
		}
		else {
			((ImageView) findViewById(R.id.image_media)).setImageBitmap(null);
			((ImageView) findViewById(R.id.image_media)).setAnimation(null);
			((ImageView) findViewById(R.id.image_media)).setVisibility(View.GONE);
		}
	}

	private void configure() {
		//mPhotoPlaceholderProgress = (ImageView) findViewById(R.id.image_photo_progress_indicator);
		mProgressBar = (ProgressBar) findViewById(R.id.progressBar);

		mViewFlipper = (ViewFlipper) findViewById(R.id.flipper_form);
		mViewFlipper.setDisplayedChild(1);
		mViewFlipper.setInAnimation(this, R.anim.fade_in_medium);
		mViewFlipper.setOutAnimation(this, R.anim.fade_out_medium);

		mContextButton = (Button) findViewById(R.id.btn_context);
		if (mContextButton != null) {
			mContextButton.setVisibility(View.INVISIBLE);
			showBackButton(true, getString(R.string.form_button_back));
		}
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