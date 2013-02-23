package com.aircandi.components.bitmaps;

import com.aircandi.ProxiConstants;
import com.aircandi.service.ProxibaseService.RequestListener;

public class BitmapRequestBuilder {

	private String			mImageUri;
	private final Object			mImageRequestor;
	private RequestListener	mRequestListener;

	public BitmapRequestBuilder(Object imageRequestor) {
		mImageRequestor = imageRequestor;
	}

	public BitmapRequest create() {
		final BitmapRequest bitmapRequest = new BitmapRequest();

		if (mImageUri == null) {
			throw new IllegalStateException("ImageUri must be set on ImageRequest");
		}

		if (mImageRequestor == null) {
			throw new IllegalStateException("ImageRequestor must be set on ImageRequest");
		}

		bitmapRequest.setImageUri(mImageUri);
		bitmapRequest.setImageRequestor(mImageRequestor);

		if (mRequestListener != null) {
			bitmapRequest.setRequestListener(mRequestListener);
		}
		return bitmapRequest;
	}

	public BitmapRequestBuilder setFromUri(String imageUri) {
		if (imageUri != null && !imageUri.equals("")) {
			String imageUriFixed = imageUri;
			if (!imageUri.startsWith("http:") && !imageUri.startsWith("https:") && !imageUri.startsWith("resource:")) {
				imageUriFixed = ProxiConstants.URL_PROXIBASE_MEDIA_IMAGES + imageUri;
			}
			mImageUri = imageUriFixed;
		}
		return this;
	}

	public BitmapRequestBuilder setImageUri(String imageUri) {
		mImageUri = imageUri;
		return this;
	}

	public BitmapRequestBuilder setRequestListener(RequestListener requestListener) {
		mRequestListener = requestListener;
		return this;
	}
}
