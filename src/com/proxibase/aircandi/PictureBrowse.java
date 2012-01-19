package com.proxibase.aircandi;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.proxibase.aircandi.components.Exceptions;
import com.proxibase.aircandi.components.ImageRequest;
import com.proxibase.aircandi.components.ImageRequestBuilder;
import com.proxibase.aircandi.components.ImageUtils;
import com.proxibase.aircandi.components.Logger;
import com.proxibase.aircandi.components.Tracker;
import com.proxibase.aircandi.components.ImageRequest.ImageShape;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.widgets.AuthorBlock;
import com.proxibase.aircandi.widgets.WebImageView;
import com.proxibase.sdk.android.proxi.consumer.Entity;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestListener;

public class PictureBrowse extends FormActivity {

	private ViewFlipper	mViewFlipper;
	private ProgressBar	mProgressBar;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initialize();
		bind();
		draw();
		Tracker.trackPageView("/PictureBrowse");

	}

	protected void bind() {

		WebImageView mediaImageView = (WebImageView) findViewById(R.id.image_media);
		final Entity entity = mCommon.mEntity;

		if (entity.imageUri != null && !entity.imageUri.equals("")) {
			ImageRequestBuilder builder = new ImageRequestBuilder(mediaImageView);
			builder.setFromUris(entity.imageUri, entity.linkUri);
			builder.setImageShape(ImageShape.Native);
			builder.setScaleToWidth(CandiConstants.IMAGE_WIDTH_ORIGINAL);
			builder.setSearchCache(false);
			builder.setUpdateCache(false);
			builder.setRequestListener(new RequestListener() {

				@Override
				public void onComplete(Object response) {

					ServiceResponse serviceResponse = (ServiceResponse) response;
					if (serviceResponse.responseCode == ResponseCode.Success) {
						Logger.d(PictureBrowse.this, "Image fetched: " + entity.imageUri);
						entity.imageBitmap = (Bitmap) serviceResponse.data;
						runOnUiThread(new Runnable() {

							@Override
							public void run() {
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

			ImageRequest imageRequest = builder.create();
			Logger.d(this, "Requesting Image: " + entity.imageUri);
			mediaImageView.setImageRequest(imageRequest, null);
		}

		/* Author block */
		if (entity.author != null) {
			Integer dateToUse = entity.updatedDate != null ? entity.updatedDate : entity.createdDate;
			((AuthorBlock) findViewById(R.id.block_author)).bindToAuthor(entity.author, dateToUse, entity.locked);
		}
		else {
			((View) findViewById(R.id.block_author)).setVisibility(View.GONE);
		}

		Tracker.dispatch();

	}

	protected void draw() {

		if (findViewById(R.id.text_title) != null) {
			((TextView) findViewById(R.id.text_title)).setText(mCommon.mEntity.title);
		}

		WebImageView webImageView = (WebImageView) findViewById(R.id.image_media);
		if (mCommon.mEntity.imageBitmap != null) {
			ImageUtils.showImageInImageView(mCommon.mEntity.imageBitmap, webImageView.getImageView());
			webImageView.setVisibility(View.VISIBLE);
		}
		else {
			webImageView.getImageView().setImageBitmap(null);
			webImageView.getImageView().setAnimation(null);
			webImageView.setVisibility(View.GONE);
		}
	}

	private void initialize() {
		//mPhotoPlaceholderProgress = (ImageView) findViewById(R.id.image_photo_progress_indicator);
		mProgressBar = (ProgressBar) findViewById(R.id.progressBar);

		mViewFlipper = (ViewFlipper) findViewById(R.id.flipper_form);
		mViewFlipper.setDisplayedChild(1);
		mViewFlipper.setInAnimation(this, R.anim.fade_in_medium);
		mViewFlipper.setOutAnimation(this, R.anim.fade_out_medium);
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------
	protected void onDestroy() {

		/* This activity gets destroyed everytime we leave using back or finish(). */
		try {
			if (mCommon.mEntity.imageBitmap != null) {
				mCommon.mEntity.imageBitmap.recycle();
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