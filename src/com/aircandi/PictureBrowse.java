package com.aircandi;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.view.Window;
import com.aircandi.components.Exceptions;
import com.aircandi.components.ImageManager;
import com.aircandi.components.ImageRequest;
import com.aircandi.components.ImageRequestBuilder;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.ImageRequest.ImageResponse;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.core.CandiConstants;
import com.aircandi.service.ProxiConstants;
import com.aircandi.service.ProxibaseService.RequestListener;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Entity.ImageFormat;
import com.aircandi.widgets.AuthorBlock;
import com.aircandi.R;

public class PictureBrowse extends FormActivity {

	private ImageViewTouch	mImageViewTouch;
	private ProgressBar		mProgress;
	private ViewGroup		mProgressGroup;
	private Bitmap			mBitmap;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		/* This has to be called before setContentView */
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);

		initialize();
		draw();
	}

	private void initialize() {
		mProgress = (ProgressBar) findViewById(R.id.progressBar);
		mProgressGroup = (ViewGroup) findViewById(R.id.progress_group);
		mImageViewTouch = (ImageViewTouch) findViewById(R.id.image);
		mImageViewTouch.setFitToScreen(true);
		setSupportProgressBarIndeterminateVisibility(true);
		mCommon.mActionBar.setSubtitle("double-tap to zoom");
	}

	protected void draw() {

		final Entity entity = ProxiExplorer.getInstance().getEntityModel().getEntityById(mCommon.mEntityId, mCommon.mParentId, mCommon.mEntityTree);

		/* Title */
		if (findViewById(R.id.text_title) != null) {
			((TextView) findViewById(R.id.text_title)).setText(entity.title);
		}

		/* Author block */
		if (entity.creator != null) {
			((AuthorBlock) findViewById(R.id.block_author)).bindToAuthor(entity.creator,
					entity.modifiedDate.longValue(), entity.locked);
		}
		else {
			((View) findViewById(R.id.block_author)).setVisibility(View.GONE);
		}

		if (mBitmap != null) {
			mImageViewTouch.setImageBitmap(mBitmap);			
		}
		else {

			final ImageRequestBuilder builder = new ImageRequestBuilder(this);
			String imageUri = entity.imageUri;
			if (!imageUri.startsWith("http:") && !imageUri.startsWith("https:") && !imageUri.startsWith("resource:")) {
				imageUri = ProxiConstants.URL_PROXIBASE_MEDIA_IMAGES + imageUri;
			}
			builder.setImageUri(imageUri)
					.setImageFormat(ImageFormat.Binary)
					.setSearchCache(false)
					.setUpdateCache(false)
					.setScaleToWidth(CandiConstants.IMAGE_WIDTH_ORIGINAL)
					.setRequestListener(new RequestListener() {

						@Override
						public void onComplete(Object response) {
							final ServiceResponse serviceResponse = (ServiceResponse) response;
							runOnUiThread(new Runnable() {

								@Override
								public void run() {
									if (serviceResponse.responseCode == ResponseCode.Success) {
										ImageResponse imageResponse = (ImageResponse) serviceResponse.data;
										mBitmap = imageResponse.bitmap;
										mImageViewTouch.setImageBitmap(imageResponse.bitmap);
									}
									else {
										//mImageViewTouch.setFitToScreen(false);
										mImageViewTouch.setScaleType(ScaleType.CENTER);
										mImageViewTouch.setImageResource(R.drawable.image_broken);
										mCommon.handleServiceError(serviceResponse, ServiceOperation.PictureBrowse);
									}
									setSupportProgressBarIndeterminateVisibility(false);

								}
							});
						}

						@Override
						public void onProgressChanged(final int progress) {
							runOnUiThread(new Runnable() {

								@Override
								public void run() {
									mProgress.setProgress(progress);
									if (progress == 100) {
										mProgressGroup.setVisibility(View.GONE);
									}
								}
							});
						}
					});

			ImageRequest imageRequest = builder.create();
			ImageManager.getInstance().getImageLoader().fetchImage(imageRequest, false);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		mImageViewTouch.setFitToScreen(true);
		super.onConfigurationChanged(newConfig);
	}


	protected void onDestroy() {
		/*
		 * This activity gets destroyed everytime we leave using back or
		 * finish().
		 */
		try {
			mCommon.doDestroy();
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