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

	public void setImageUri(String imageUri) {
		this.mImageUri = imageUri;
	}

	public ImageFormat getImageFormat() {
		return this.mImageFormat;
	}

	public void setImageFormat(ImageFormat imageFormat) {
		this.mImageFormat = imageFormat;
	}

	public ImageShape getImageShape() {
		return this.mImageShape;
	}

	public void setImageShape(ImageShape imageShape) {
		this.mImageShape = imageShape;
	}

	public Object getImageRequestor() {
		return this.mImageRequestor;
	}

	public void setImageRequestor(Object imageRequestor) {
		this.mImageRequestor = imageRequestor;
	}

	public Integer getPriority() {
		return this.mPriority;
	}

	public void setPriority(Integer priority) {
		this.mPriority = priority;
	}

	public Integer getScaleToWidth() {
		return this.mScaleToWidth;
	}

	public void setScaleToWidth(Integer scaleToWidth) {
		this.mScaleToWidth = scaleToWidth;
	}

	public Boolean getLinkZoom() {
		return this.mLinkZoom;
	}

	public void setLinkZoom(Boolean linkZoom) {
		this.mLinkZoom = linkZoom;
	}

	public Boolean getLinkJavascriptEnabled() {
		return this.mLinkJavascriptEnabled;
	}

	public void setLinkJavascriptEnabled(Boolean linkJavascriptEnabled) {
		this.mLinkJavascriptEnabled = linkJavascriptEnabled;
	}

	public Boolean doUpdateCache() {
		return this.mUpdateCache;
	}

	public void setUpdateCache(Boolean updateCache) {
		this.mUpdateCache = updateCache;
	}

	public Boolean doSearchCache() {
		return this.mSearchCache;
	}

	public void setSearchCache(Boolean searchCache) {
		this.mSearchCache = searchCache;
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
