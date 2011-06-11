package com.proxibase.aircandi.utilities;

import org.anddev.andengine.opengl.texture.source.ITextureSource;

import android.graphics.Bitmap;

public class BitmapTextureSource implements ITextureSource {

	private int		mHeight;
	private int		mWidth;

	private Bitmap	mBitmap;


	public BitmapTextureSource(Bitmap bitmap) {

		mBitmap = bitmap;
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

		// TODO Auto-generated method stub
		return mHeight;
	}


	@Override
	public int getWidth() {

		// TODO Auto-generated method stub
		return mWidth;
	}


	@Override
	public Bitmap onLoadBitmap() {

		// TODO Auto-generated method stub
		return mBitmap;
	}

}