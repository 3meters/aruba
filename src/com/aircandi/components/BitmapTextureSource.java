package com.aircandi.components;

import org.anddev.andengine.opengl.texture.source.ITextureSource;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;

public class BitmapTextureSource implements ITextureSource {

	private int				mHeight;
	private int				mWidth;
	private IBitmapAdapter	mBitmapAdapter;
	private Bitmap			mBitmap;
	private String			mName;

	public BitmapTextureSource(Bitmap bitmap, String name) {
		this(bitmap, name, null);
	}

	public BitmapTextureSource(Bitmap bitmap, String name, IBitmapAdapter bitmapAdapter) {

		mBitmap = bitmap;
		mBitmapAdapter = bitmapAdapter;
		mName = name;
		if (mBitmap != null) {
			mHeight = mBitmap.getHeight();
			mWidth = mBitmap.getWidth();
		}
		else {
			mHeight = 0;
			mWidth = 0;
		}
	}

	@Override
	public BitmapTextureSource clone() {
		return null;
	}

	@Override
	public int getHeight() {
		return mHeight;
	}

	@Override
	public int getWidth() {
		return mWidth;
	}

	public Bitmap getBitmap() {
		return this.mBitmap;
	}

	public void setBitmap(Bitmap bitmap) {
		this.mBitmap = bitmap;
	}

	@Override
	public Bitmap onLoadBitmap(Config pBitmapConfig) {
		/*
		 * Andengine throws an IllegalArgumentException if we return null.
		 */
		if (mBitmap != null && mBitmap.isRecycled()) {
			Logger.v(this, "AndEngine requesting bitmap: source bitmap has been recycled...reloading: " + this.mName);
			if (this.mBitmapAdapter != null) {
				mBitmap = this.mBitmapAdapter.reloadBitmap();
			}
		}
		else {
			if (mBitmap == null) {
				Logger.v(this, "AndEngine requesting bitmap: source bitmap is null: " + this.mName);
			}
			else {
				Logger.v(this, "AndEngine requesting bitmap: source bitmap is good: " + this.mName);
			}
		}
		return mBitmap;
	}

	/**
	 * Callback interface for Aircandi async requests.
	 */
	public static interface IBitmapAdapter {

		public Bitmap reloadBitmap();
	}

	@Override
	public String getName() {
		return mName;
	}

	public void setName(String name) {
		mName = name;
	}
}