package com.aircandi.components;

import android.graphics.Bitmap;

import com.aircandi.core.CandiConstants;
import com.aircandi.service.ProxibaseService.RequestListener;
import com.aircandi.service.objects.Entity.ImageFormat;

public class ImageRequest {

	private String			mImageUri				= null;
	private Object			mImageRequestor			= null;
	private RequestListener	mRequestListener		= null;
	private ImageFormat		mImageFormat			= ImageFormat.Binary;
	private ImageShape		mImageShape				= ImageShape.Square;
	private Integer			mPriority				= 1;
	private Integer			mScaleToWidth			= CandiConstants.IMAGE_WIDTH_DEFAULT;
	private Boolean			mLinkZoom				= false;
	private Boolean			mLinkJavascriptEnabled	= false;
	private Boolean			mUpdateCache			= true;
	private Boolean			mSearchCache			= true;

	public ImageRequest() {}

	public ImageRequest(String imageUri, Object imageRequestor, RequestListener requestListener) {
		mImageUri = imageUri;
		mImageRequestor = imageRequestor;
		mRequestListener = requestListener;
	}

	public String getImageUri() {
		return this.mImageUri;
	}

	public ImageRequest setImageUri(String imageUri) {
		this.mImageUri = imageUri;
		return this;
	}

	public ImageFormat getImageFormat() {
		return this.mImageFormat;
	}

	public ImageRequest setImageFormat(ImageFormat imageFormat) {
		this.mImageFormat = imageFormat;
		return this;
	}

	public ImageShape getImageShape() {
		return this.mImageShape;
	}

	public ImageRequest setImageShape(ImageShape imageShape) {
		this.mImageShape = imageShape;
		return this;
	}

	public Object getImageRequestor() {
		return this.mImageRequestor;
	}

	public ImageRequest setImageRequestor(Object imageRequestor) {
		this.mImageRequestor = imageRequestor;
		return this;
	}

	public Integer getPriority() {
		return this.mPriority;
	}

	public ImageRequest setPriority(Integer priority) {
		this.mPriority = priority;
		return this;
	}

	public Integer getScaleToWidth() {
		return this.mScaleToWidth;
	}

	public ImageRequest setScaleToWidth(Integer scaleToWidth) {
		this.mScaleToWidth = scaleToWidth;
		return this;
	}

	public Boolean getLinkZoom() {
		return this.mLinkZoom;
	}

	public ImageRequest setLinkZoom(Boolean linkZoom) {
		this.mLinkZoom = linkZoom;
		return this;
	}

	public Boolean getLinkJavascriptEnabled() {
		return this.mLinkJavascriptEnabled;
	}

	public ImageRequest setLinkJavascriptEnabled(Boolean linkJavascriptEnabled) {
		this.mLinkJavascriptEnabled = linkJavascriptEnabled;
		return this;
	}

	public Boolean doUpdateCache() {
		return this.mUpdateCache;
	}

	public ImageRequest setUpdateCache(Boolean updateCache) {
		this.mUpdateCache = updateCache;
		return this;
	}

	public Boolean doSearchCache() {
		return this.mSearchCache;
	}

	public ImageRequest setSearchCache(Boolean searchCache) {
		this.mSearchCache = searchCache;
		return this;
	}

	public RequestListener getRequestListener() {
		return this.mRequestListener;
	}

	public void setRequestListener(RequestListener requestListener) {
		this.mRequestListener = requestListener;
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
