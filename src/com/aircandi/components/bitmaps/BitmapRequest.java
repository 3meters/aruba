package com.aircandi.components.bitmaps;

import java.lang.ref.WeakReference;

import android.graphics.Bitmap;
import android.widget.ImageView;

import com.aircandi.service.HttpService.RequestListener;

public class BitmapRequest {

	private String						mImageUri				= null;
	private WeakReference<Object>		mImageRequestor			= null;
	private RequestListener				mRequestListener		= null;
	private WeakReference<ImageView>	mImageView				= null;
	private Integer						mImageSize				= null;
	private Integer						mBrokenDrawableResId	= null;

	public BitmapRequest() {}

	public BitmapRequest(String photoUri, ImageView imageView) {
		mImageUri = photoUri;
		mImageView = new WeakReference<ImageView>(imageView);
	}

	public String getImageUri() {
		return mImageUri;
	}

	public BitmapRequest setImageUri(String photoUri) {
		mImageUri = photoUri;
		return this;
	}

	public Object getImageRequestor() {
		if (mImageRequestor != null) {
			return mImageRequestor.get();
		}
		return null;
	}

	public BitmapRequest setImageRequestor(Object imageRequestor) {
		mImageRequestor = new WeakReference<Object>(imageRequestor);
		return this;
	}

	public RequestListener getRequestListener() {
		return mRequestListener;
	}

	public BitmapRequest setRequestListener(RequestListener requestListener) {
		mRequestListener = requestListener;
		return this;
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

	public Integer getBrokenDrawableResId() {
		return mBrokenDrawableResId;
	}

	public void setBrokenDrawableResId(Integer brokenDrawableResId) {
		mBrokenDrawableResId = brokenDrawableResId;
	}

	public static class ImageResponse {

		public Bitmap	bitmap;
		public String	photoUri;

		ImageResponse(Bitmap bitmap, String photoUri) {
			this.bitmap = bitmap;
			this.photoUri = photoUri;
		}
	}
}
