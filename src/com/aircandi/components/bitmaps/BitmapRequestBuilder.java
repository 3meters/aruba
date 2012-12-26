package com.aircandi.components.bitmaps;

import com.aircandi.components.bitmaps.BitmapRequest.ImageShape;
import com.aircandi.service.ProxiConstants;
import com.aircandi.service.ProxibaseService.RequestListener;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Entity.ImageFormat;

public class BitmapRequestBuilder {

	public String			mImageUri;
	public Object			mImageRequestor;
	public ImageFormat		mImageFormat;
	public ImageShape		mImageShape;
	public Integer			mPriority;
	public Integer			mScaleToWidth;
	public Boolean			mLinkZoom;
	public Boolean			mLinkJavascriptEnabled;
	public RequestListener	mRequestListener;

	public BitmapRequestBuilder(Object imageRequestor) {
		mImageRequestor = imageRequestor;
	}

	public BitmapRequest create() {
		BitmapRequest imageRequest = new BitmapRequest();

		if (mImageUri == null) {
			throw new IllegalStateException("ImageUri must be set on ImageRequest");
		}

		if (mImageRequestor == null) {
			throw new IllegalStateException("ImageRequestor must be set on ImageRequest");
		}

		imageRequest.setImageUri(mImageUri);
		imageRequest.setImageRequestor(mImageRequestor);

		if (mImageFormat != null) {
			imageRequest.setImageFormat(mImageFormat);
		}
		if (mImageShape != null) {
			imageRequest.setImageShape(mImageShape);
		}
		if (mPriority != null) {
			imageRequest.setPriority(mPriority);
		}
		if (mScaleToWidth != null) {
			imageRequest.setScaleToWidth(mScaleToWidth);
		}
		if (mLinkZoom != null) {
			imageRequest.setLinkZoom(mLinkZoom);
		}
		if (mLinkJavascriptEnabled != null) {
			imageRequest.setLinkJavascriptEnabled(mLinkJavascriptEnabled);
		}
		if (mRequestListener != null) {
			imageRequest.setRequestListener(mRequestListener);
		}
		return imageRequest;
	}

	public static String getImageUriFromEntity(Entity entity) {
		return entity.getImageUri();
	}

	public BitmapRequestBuilder setFromUris(String imageUri, String linkUri) {
		if (imageUri != null && !imageUri.equals("")) {
			String imageUriFixed = imageUri;
			if (!imageUri.startsWith("http:") && !imageUri.startsWith("https:") && !imageUri.startsWith("resource:")) {
				imageUriFixed = ProxiConstants.URL_PROXIBASE_MEDIA_IMAGES + imageUri;
			}

			this.mImageUri = imageUriFixed;
			this.mImageFormat = ImageFormat.Binary;
		}
		else if (linkUri != null && !linkUri.equals("")) {
			this.mImageUri = linkUri;
			this.mImageFormat = ImageFormat.Html;
		}
		return this;
	}

	public BitmapRequestBuilder setImageUri(String imageUri) {
		this.mImageUri = imageUri;
		return this;
	}

	public BitmapRequestBuilder setImageFormat(ImageFormat imageFormat) {
		this.mImageFormat = imageFormat;
		return this;
	}

	public BitmapRequestBuilder setImageShape(ImageShape imageShape) {
		this.mImageShape = imageShape;
		return this;
	}

	public BitmapRequestBuilder setImageRequestor(Object imageRequestor) {
		this.mImageRequestor = imageRequestor;
		return this;
	}

	public BitmapRequestBuilder setPriority(Integer priority) {
		this.mPriority = priority;
		return this;
	}

	public BitmapRequestBuilder setScaleToWidth(Integer scaleToWidth) {
		this.mScaleToWidth = scaleToWidth;
		return this;
	}

	public BitmapRequestBuilder setLinkZoom(Boolean linkZoom) {
		this.mLinkZoom = linkZoom;
		return this;
	}

	public BitmapRequestBuilder setLinkJavascriptEnabled(Boolean linkJavascriptEnabled) {
		this.mLinkJavascriptEnabled = linkJavascriptEnabled;
		return this;
	}

	public BitmapRequestBuilder setRequestListener(RequestListener requestListener) {
		this.mRequestListener = requestListener;
		return this;
	}
}
