package com.aircandi.components.bitmaps;

import com.aircandi.ProxiConstants;
import com.aircandi.service.HttpService.RequestListener;

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

	public BitmapRequestBuilder setFromUri(String photoUri) {
		if (photoUri != null && !photoUri.equals("")) {
			String photoUriFixed = photoUri;
			if (!photoUri.startsWith("http:") && !photoUri.startsWith("https:") && !photoUri.startsWith("resource:")) {
				photoUriFixed = ProxiConstants.URL_PROXIBASE_MEDIA_IMAGES + photoUri;
			}
			mImageUri = photoUriFixed;
		}
		return this;
	}

	public BitmapRequestBuilder setImageUri(String photoUri) {
		mImageUri = photoUri;
		return this;
	}

	public BitmapRequestBuilder setRequestListener(RequestListener requestListener) {
		mRequestListener = requestListener;
		return this;
	}
}
