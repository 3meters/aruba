package com.aircandi.components.bitmaps;

import java.lang.ref.WeakReference;

import android.graphics.Bitmap;
import android.widget.ImageView;

import com.aircandi.service.RequestListener;

public class BitmapRequest {

	private String						mBitmapUri				= null;
	private WeakReference<Object>		mBitmapRequestor			= null;
	private RequestListener				mRequestListener		= null;
	private WeakReference<ImageView>	mImageView				= null;
	private Integer						mBitmapSize				= null;

	public BitmapRequest() {}

	public BitmapRequest(String photoUri) {
		mBitmapUri = photoUri;
	}

	public String getBitmapUri() {
		return mBitmapUri;
	}

	public BitmapRequest setBitmapUri(String photoUri) {
		mBitmapUri = photoUri;
		return this;
	}

	public Object getBitmapRequestor() {
		if (mBitmapRequestor != null) {
			return mBitmapRequestor.get();
		}
		return null;
	}

	public BitmapRequest setBitmapRequestor(Object imageRequestor) {
		mBitmapRequestor = new WeakReference<Object>(imageRequestor);
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

	public Integer getBitmapSize() {
		return mBitmapSize;
	}

	public BitmapRequest setBitmapSize(Integer imageSize) {
		mBitmapSize = imageSize;
		return this;
	}

	public static class BitmapResponse {

		public Bitmap	bitmap;
		public String	photoUri;

		BitmapResponse(Bitmap bitmap, String photoUri) {
			this.bitmap = bitmap;
			this.photoUri = photoUri;
		}
	}
}
