package com.aircandi.components.images;

import com.aircandi.components.images.ImageRequest.ImageShape;
import com.aircandi.service.ProxiConstants;
import com.aircandi.service.ProxibaseService.RequestListener;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Entity.ImageFormat;

public class ImageRequestBuilder {

	public String			mImageUri;
	public Object			mImageRequestor;
	public ImageFormat		mImageFormat;
	public ImageShape		mImageShape;
	public Integer			mPriority;
	public Integer			mScaleToWidth;
	public Boolean			mLinkZoom;
	public Boolean			mLinkJavascriptEnabled;
	public Boolean			mUpdateCache;
	public Boolean			mSearchCache;
	public RequestListener	mRequestListener;

	public ImageRequestBuilder(Object imageRequestor) {
		mImageRequestor = imageRequestor;
	}

	public ImageRequest create() {
		ImageRequest imageRequest = new ImageRequest();

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
		if (mUpdateCache != null) {
			imageRequest.setUpdateCache(mUpdateCache);
		}
		if (mSearchCache != null) {
			imageRequest.setSearchCache(mSearchCache);
		}
		if (mRequestListener != null) {
			imageRequest.setRequestListener(mRequestListener);
		}
		return imageRequest;
	}

	public static String getImageUriFromEntity(Entity entity) {
		return entity.getImageUri();
	}

	public ImageRequestBuilder setFromUris(String imageUri, String linkUri) {
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

	public ImageRequestBuilder setImageUri(String imageUri) {
		this.mImageUri = imageUri;
		return this;
	}

	public ImageRequestBuilder setImageFormat(ImageFormat imageFormat) {
		this.mImageFormat = imageFormat;
		return this;
	}

	public ImageRequestBuilder setImageShape(ImageShape imageShape) {
		this.mImageShape = imageShape;
		return this;
	}

	public ImageRequestBuilder setImageRequestor(Object imageRequestor) {
		this.mImageRequestor = imageRequestor;
		return this;
	}

	public ImageRequestBuilder setPriority(Integer priority) {
		this.mPriority = priority;
		return this;
	}

	public ImageRequestBuilder setScaleToWidth(Integer scaleToWidth) {
		this.mScaleToWidth = scaleToWidth;
		return this;
	}

	public ImageRequestBuilder setLinkZoom(Boolean linkZoom) {
		this.mLinkZoom = linkZoom;
		return this;
	}

	public ImageRequestBuilder setLinkJavascriptEnabled(Boolean linkJavascriptEnabled) {
		this.mLinkJavascriptEnabled = linkJavascriptEnabled;
		return this;
	}

	public ImageRequestBuilder setUpdateCache(Boolean updateCache) {
		this.mUpdateCache = updateCache;
		return this;
	}

	public ImageRequestBuilder setSearchCache(Boolean searchCache) {
		this.mSearchCache = searchCache;
		return this;
	}

	public ImageRequestBuilder setRequestListener(RequestListener requestListener) {
		this.mRequestListener = requestListener;
		return this;
	}
}
