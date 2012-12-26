package com.aircandi.components.bitmaps;

import java.lang.ref.WeakReference;

import android.graphics.Bitmap;
import android.widget.ImageView;

import com.aircandi.CandiConstants;
import com.aircandi.service.ProxibaseService.RequestListener;
import com.aircandi.service.objects.Entity.ImageFormat;

public class BitmapRequest {

	private String						mImageUri				= null;
	private WeakReference<Object>		mImageRequestor			= null;
	private RequestListener				mRequestListener		= null;
	private WeakReference<ImageView>	mImageView				= null;
	private ImageFormat					mImageFormat			= ImageFormat.Binary;
	private ImageShape					mImageShape				= ImageShape.Native;
	private Integer						mPriority				= 1;
	private Integer						mScaleToWidth			= CandiConstants.IMAGE_WIDTH_DEFAULT;
	private Boolean						mLinkZoom				= false;
	private Boolean						mLinkJavascriptEnabled	= false;

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

	public ImageFormat getImageFormat() {
		return this.mImageFormat;
	}

	public BitmapRequest setImageFormat(ImageFormat imageFormat) {
		this.mImageFormat = imageFormat;
		return this;
	}

	public ImageShape getImageShape() {
		return this.mImageShape;
	}

	public BitmapRequest setImageShape(ImageShape imageShape) {
		this.mImageShape = imageShape;
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

	public Integer getPriority() {
		return this.mPriority;
	}

	public BitmapRequest setPriority(Integer priority) {
		this.mPriority = priority;
		return this;
	}

	public Integer getScaleToWidth() {
		return this.mScaleToWidth;
	}

	public BitmapRequest setScaleToWidth(Integer scaleToWidth) {
		this.mScaleToWidth = scaleToWidth;
		return this;
	}

	public Boolean getLinkZoom() {
		return this.mLinkZoom;
	}

	public BitmapRequest setLinkZoom(Boolean linkZoom) {
		this.mLinkZoom = linkZoom;
		return this;
	}

	public Boolean getLinkJavascriptEnabled() {
		return this.mLinkJavascriptEnabled;
	}

	public BitmapRequest setLinkJavascriptEnabled(Boolean linkJavascriptEnabled) {
		this.mLinkJavascriptEnabled = linkJavascriptEnabled;
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

	public static class ImageResponse {

		public Bitmap	bitmap;
		public String	imageUri;

		public ImageResponse(Bitmap bitmap, String imageUri) {
			this.bitmap = bitmap;
			this.imageUri = imageUri;
		}
	}

	public static enum ImageShape {
		Native, Square
	}
}
