package com.aircandi.components.bitmaps;

import java.lang.ref.WeakReference;

import android.graphics.Bitmap;
import android.widget.ImageView;

import com.aircandi.service.ProxibaseService.RequestListener;

public class BitmapRequest {

	private String						mImageUri			= null;
	private WeakReference<Object>		mImageRequestor		= null;
	private RequestListener				mRequestListener	= null;
	private WeakReference<ImageView>	mImageView			= null;
	private Integer						mImageSize			= null;
	private Boolean						mUseThumbnailCache = false;

	public BitmapRequest() {}

	public BitmapRequest(String imageUri, Object imageRequestor, RequestListener requestListener) {
		mImageUri = imageUri;
		mImageRequestor = new WeakReference<Object>(imageRequestor);
		mRequestListener = requestListener;
	}

	public BitmapRequest(String imageUri, ImageView imageView) {
		mImageUri = imageUri;
		mImageView = new WeakReference<ImageView>(imageView);
	}

	public String getImageUri() {
		return this.mImageUri;
	}

	public BitmapRequest setImageUri(String imageUri) {
		this.mImageUri = imageUri;
		return this;
	}

	public Object getImageRequestor() {
		if (mImageRequestor != null) {
			return this.mImageRequestor.get();
		}
		return null;
	}

	public BitmapRequest setImageRequestor(Object imageRequestor) {
		mImageRequestor = new WeakReference<Object>(imageRequestor);
		return this;
	}

	public RequestListener getRequestListener() {
		return this.mRequestListener;
	}

	public void setRequestListener(RequestListener requestListener) {
		this.mRequestListener = requestListener;
	}

	public ImageView getImageView() {
		if (mImageView != null) {
			return mImageView.get();
		}
		return null;
	}

	public BitmapRequest setImageView(ImageView imageView) {
		mImageView = new WeakReference<ImageView>(imageView);
		return this;
	}

	public Integer getImageSize() {
		return mImageSize;
	}

	public BitmapRequest setImageSize(Integer imageSize) {
		mImageSize = imageSize;
		return this;
	}

	public Boolean getUseThumbnailCache() {
		return mUseThumbnailCache;
	}

	public BitmapRequest setUseThumbnailCache(Boolean useThumbnailCache) {
		mUseThumbnailCache = useThumbnailCache;
		return this;
	}

	public static class ImageResponse {

		public Bitmap	bitmap;
		public String	imageUri;

		public ImageResponse(Bitmap bitmap, String imageUri) {
			this.bitmap = bitmap;
			this.imageUri = imageUri;
		}
	}
}
