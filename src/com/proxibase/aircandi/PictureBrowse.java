package com.proxibase.aircandi;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.proxibase.aircandi.components.DateUtils;
import com.proxibase.aircandi.components.Exceptions;
import com.proxibase.aircandi.components.Logger;
import com.proxibase.aircandi.components.Tracker;
import com.proxibase.aircandi.components.ImageManager.ImageRequest;
import com.proxibase.aircandi.components.ImageManager.ImageRequest.ImageShape;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.widgets.AuthorBlock;
import com.proxibase.aircandi.widgets.WebImageView;
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

		if (mCommon.mEntity.imageUri != null && !mCommon.mEntity.imageUri.equals("")) {

			ImageRequest imageRequest = new ImageRequest(mCommon.mEntity.imageUri, mCommon.mEntity.linkUri, ImageShape.Native, false, false,
						CandiConstants.IMAGE_WIDTH_ORIGINAL, false, false, false, 1, this, new RequestListener() {

							@Override
							public void onComplete(Object response) {

								ServiceResponse serviceResponse = (ServiceResponse) response;
								if (serviceResponse.responseCode != ResponseCode.Success) {
									return;
								}
								else {
									Logger.d(PictureBrowse.this, "Image fetched: " + mCommon.mEntity.imageUri);
									mCommon.mEntity.imageBitmap = (Bitmap) serviceResponse.data;
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

			Logger.d(this, "Fetching Image: " + mCommon.mEntity.imageUri);
			mediaImageView.setImageRequest(imageRequest, null);
		}

		/* Author block */
		if (mCommon.mEntity.author != null) {
			((AuthorBlock) findViewById(R.id.block_author)).bindToAuthor(mCommon.mEntity.author, DateUtils.wcfToDate(mCommon.mEntity.createdDate));
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

		if (mCommon.mEntity.imageBitmap != null) {
			((ImageView) findViewById(R.id.image_media)).setImageBitmap(mCommon.mEntity.imageBitmap);
			((ImageView) findViewById(R.id.image_media)).setVisibility(View.VISIBLE);
		}
		else {
			((ImageView) findViewById(R.id.image_media)).setImageBitmap(null);
			((ImageView) findViewById(R.id.image_media)).setAnimation(null);
			((ImageView) findViewById(R.id.image_media)).setVisibility(View.GONE);
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