package com.proxibase.aircandi.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.proxibase.aircandi.components.ImageManager;
import com.proxibase.aircandi.components.ImageUtils;
import com.proxibase.aircandi.components.Logger;
import com.proxibase.aircandi.components.ImageManager.ImageRequest;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestListener;

public class WebImageView extends ImageView {

	private String		mImageUri;
	private Handler		mThreadHandler	= new Handler();

	public WebImageView(Context context) {
		super(context);
	}

	public WebImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public WebImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void setImageRequest(final ImageRequest imageRequest, final ImageView imageReflection) {

		final RequestListener originalImageReadyListener = imageRequest.requestListener;

		imageRequest.requestListener = new RequestListener() {

			@Override
			public void onProgressChanged(int progress) {
				if (originalImageReadyListener != null) {
					originalImageReadyListener.onProgressChanged(progress);
				}
			}

			@Override
			public void onComplete(Object response) {

				/* Who's looking for imageUri? */

				ServiceResponse serviceResponse = (ServiceResponse) response;

				if (serviceResponse.responseCode == ResponseCode.Success) {

					final Bitmap bitmap = (Bitmap) serviceResponse.data;
					@SuppressWarnings("unused")
					String imageFileUri = ImageManager.getInstance().getImageCache().getImageFileUri(imageRequest.imageUri);
					WebImageView.this.setTag(imageRequest.imageUri);

					if (bitmap != null) {
						mThreadHandler.post(new Runnable() {

							@Override
							public void run() {
								if (imageRequest.makeReflection && imageReflection != null) {
									String cacheName = ImageManager.getInstance().resolveCacheName(imageRequest.imageUri);
									Bitmap bitmapReflection = ImageManager.getInstance().getImage(cacheName + ".reflection");
									ImageUtils.showImageInImageView(bitmap, bitmapReflection, WebImageView.this, imageReflection);
								}
								else {
									ImageUtils.showImageInImageView(bitmap, WebImageView.this);
								}
							}
						});
					}
				}
				else {
					final Bitmap bitmap = ImageManager.getInstance().loadBitmapFromAssets(CandiConstants.IMAGE_BROKEN);
					if (bitmap != null) {
						mThreadHandler.post(new Runnable() {

							@Override
							public void run() {
								if (imageRequest.makeReflection && imageReflection != null) {
									String cacheName = ImageManager.getInstance().resolveCacheName(imageRequest.imageUri);
									Bitmap bitmapReflection = ImageManager.getInstance().getImage(cacheName + ".reflection");
									ImageUtils.showImageInImageView(bitmap, bitmapReflection, WebImageView.this, imageReflection);
								}
								else {
									ImageUtils.showImageInImageView(bitmap, WebImageView.this);
								}
							}
						});
					}
				}
				if (originalImageReadyListener != null) {
					originalImageReadyListener.onComplete(serviceResponse);
				}
			}
		};

		Logger.v(this, "Fetching Image: " + imageRequest.imageUri);
		ImageManager.getInstance().getImageLoader().fetchImage(imageRequest);
	}

	public String getImageUri() {
		return mImageUri;
	}
}
