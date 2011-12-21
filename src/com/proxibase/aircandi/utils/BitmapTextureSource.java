package com.proxibase.aircandi.utils;

import org.anddev.andengine.opengl.texture.source.ITextureSource;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;

public class BitmapTextureSource implements ITextureSource {

	private int				mHeight;
	private int				mWidth;
	private IBitmapAdapter	mBitmapAdapter;
	private Bitmap			mBitmap;

	public BitmapTextureSource(Bitmap bitmap) {
		this(bitmap, null);
	}

	public BitmapTextureSource(Bitmap bitmap, IBitmapAdapter bitmapAdapter) {

		mBitmap = bitmap;
		mBitmapAdapter = bitmapAdapter;
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

		/* Andengine throws an IllegalArgumentException if we return null. */
		if (mBitmap != null && mBitmap.isRecycled()) {
			if (this.mBitmapAdapter != null) {
				//Log.v("AndEngine", "Reloading recycled texture for Andengine");
				Bitmap bitmap = this.mBitmapAdapter.reloadBitmap();
				if (bitmap != null)
					mBitmap = bitmap;
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
}