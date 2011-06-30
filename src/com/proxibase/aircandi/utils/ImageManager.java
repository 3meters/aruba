package com.proxibase.aircandi.utils;

import java.io.IOException;
import java.io.InputStream;

import org.anddev.andengine.util.Debug;
import org.anddev.andengine.util.StreamUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.Config;
import android.os.AsyncTask;

import com.proxibase.aircandi.controllers.Aircandi;
import com.proxibase.sdk.android.util.UtilitiesUI;

/*
 * TODO: ImageCache uses weak references by default. Should investigate the possible benefits 
 * of switching to using soft references instead.
 */

public class ImageManager {

	private static ImageManager		singletonObject;
	private ImageCache				mImageCache;
	private OnImageReadyListener	mImageReadyListener;
	private static Context			mContext;

	public static synchronized ImageManager getImageManager() {

		if (singletonObject == null) {
			singletonObject = new ImageManager();
		}
		return singletonObject;
	}

	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	/**
	 * Designed as a singleton. The private Constructor prevents any other class from instantiating.
	 */

	private ImageManager() {}

	public ImageCache getImageCache() {

		return mImageCache;
	}

	public void setImageCache(ImageCache mImageCache) {

		this.mImageCache = mImageCache;
	}

	public Bitmap getImage(String key) {
		return mImageCache.get(key);
	}

	public boolean hasImage(String key) {

		return mImageCache.containsKey(key);
	}

	public void setContext(Context context) {
		mContext = context;
	}

	public Context getContext() {
		return mContext;
	}

	public void fetchImageAsynch(String key, String url) {
		ImageRequest imageRequest = new ImageRequest();
		imageRequest.imageId = key;
		imageRequest.imageUrl = url;
		imageRequest.imageShape = "square";
		imageRequest.showReflection = true;
		new GetImageTask().execute(imageRequest);

	}

	public void fetchImageAsynch(ImageRequest imageRequest) {
		new GetImageTask().execute(imageRequest);

	}

	public static Bitmap loadBitmapFromAssets(final String assetPath) {
		InputStream in = null;
		try {
			final BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
			decodeOptions.inPreferredConfig = Config.ARGB_8888;

			in = mContext.getAssets().open(assetPath);
			return BitmapFactory.decodeStream(in, null, decodeOptions);
		}
		catch (final IOException e) {
			Debug.e("Failed loading Bitmap in AssetTextureSource. AssetPath: " + assetPath, e);
			return null;
		}
		finally {
			StreamUtils.close(in);
		}
	}

	public class GetImageTask extends AsyncTask<ImageRequest, Void, Bitmap> {

		ImageRequest	mImageRequest;

		@Override
		protected Bitmap doInBackground(final ImageRequest... params) {

			// We are on the background thread
			mImageRequest = params[0];
			Bitmap bitmap = UtilitiesUI.getImage(mImageRequest.imageUrl);
			if (bitmap != null) {

				if (mImageRequest.widthMinimum > 0 && bitmap.getWidth() < mImageRequest.widthMinimum) {
					float scalingRatio = (float) mImageRequest.widthMinimum / (float) bitmap.getWidth();
					float newHeight = (float) bitmap.getHeight() * scalingRatio;
					bitmap = Bitmap.createScaledBitmap(bitmap, mImageRequest.widthMinimum, (int) (newHeight), true);
				}

				if (mImageRequest.imageShape.equals("square")) {
					bitmap = UtilitiesUI.cropToSquare(bitmap);
				}

				// Create reflection
				if (mImageRequest.showReflection) {
					final Bitmap bitmapReflection = UtilitiesUI.getReflection(bitmap);
					Utilities.Log(Aircandi.APP_NAME, "ImageManager",
							"Image download, cropping and reflection completed for image '" + mImageRequest.imageId + "'");
					mImageCache.put(mImageRequest.imageId, bitmap);
					mImageCache.put(mImageRequest.imageId + ".reflection", bitmapReflection);
					return bitmap;
				}
				else {
					return bitmap;
				}
			}
			else {
				Utilities.Log(Aircandi.APP_NAME, "ImageManager", "Image download failed for image '" + mImageRequest.imageId + "'");
				return null;
			}
		}

		@Override
		protected void onPostExecute(final Bitmap bitmap) {

			// We are on the main thread
			super.onPostExecute(bitmap);

			if (bitmap != null) {
				if (mImageRequest.mImageReadyListener != null)
					mImageRequest.mImageReadyListener.onImageReady(bitmap);
				else if (mImageReadyListener != null) {
					mImageReadyListener.onImageReady(bitmap);
				}
			}
		}
	}

	public void setOnImageReadyListener(OnImageReadyListener listener) {

		mImageReadyListener = listener;
	}

	public final OnImageReadyListener getOnImageReadyListener() {

		return mImageReadyListener;
	}

	public interface OnImageReadyListener {

		void onImageReady(Bitmap bitmap);

	}

	public static class ImageRequest {

		public String				imageUrl;
		public String				imageId;
		public String				imageShape			= "native";
		public int					widthMinimum;
		public boolean				showReflection		= false;
		public OnImageReadyListener	mImageReadyListener	= null;
	}

}