package com.proxibase.aircandi.utilities;

import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.proxibase.sdk.android.core.ProxibaseService;
import com.proxibase.sdk.android.core.Utilities;
import com.proxibase.sdk.android.widgets.ImageCache;
import com.proxibase.sdk.android.widgets.UtilitiesUI;

public class ImageManager {

	private ImageCache				mImageCache;
	private OnImageReadyListener	mImageReadyListener;


	public ImageManager(ImageCache imageCache) {

		this.mImageCache = imageCache;
	}


	public ImageCache getImageCache() {

		return this.mImageCache;
	}


	public void setImageCache(ImageCache mImageCache) {

		this.mImageCache = mImageCache;
	}
	
	public Bitmap getImage(String key)
	{
		return mImageCache.get(key);
	}


	public boolean hasImage(String key) {

		return mImageCache.containsKey(key);
	}
	
	public void fetchImageAsynch(String key, String url)
	{
		ImageRequest imageRequest = new ImageRequest();
		imageRequest.imageId = key;
		imageRequest.imageUrl = url;
		imageRequest.imageShape = "square";
		imageRequest.showReflection = true;
		new GetImageTask().execute(imageRequest); 
		
	}
	
	public class ImageRequest {

		public String	imageUrl;
		public String	imageId;
		public String	imageShape		= "native";
		public boolean	showReflection	= false;
	}

	public class GetImageTask extends AsyncTask<ImageRequest, Void, Bitmap> {

		ImageRequest	imageRequest;


		@Override
		protected Bitmap doInBackground(final ImageRequest... params) {

			// We are on the background thread
			imageRequest = params[0];
			Bitmap bitmap = UtilitiesUI.getImage(imageRequest.imageUrl);
			if (bitmap != null) {

				if (imageRequest.imageShape.equals("square")) {
					bitmap = UtilitiesUI.cropToSquare(bitmap);
				}

				// Create reflection
				if (imageRequest.showReflection) {
					final Bitmap bitmapWithReflection = UtilitiesUI.createReflectedImages(bitmap);
					Utilities.Log(ProxibaseService.APP_NAME, "RippleView",
							"Image download, cropping and reflection completed for image '" + imageRequest.imageId + "'");
					mImageCache.put(imageRequest.imageId, bitmapWithReflection);
					return bitmapWithReflection;
				}
				else {
					return bitmap;
				}
			}
			else {
				Utilities.Log(ProxibaseService.APP_NAME, "RippleView", "Image download failed for image '" + imageRequest.imageId + "'");
				return null;
			}
		}


		@Override
		protected void onPostExecute(final Bitmap bitmap) {

			// We are on the main thread
			super.onPostExecute(bitmap);

			if (bitmap != null) {
				if (mImageReadyListener != null)
				{
					mImageReadyListener.onImageReady(imageRequest.imageId);
				}
			}
		}
	}
	


	/**
	 * Register a callback to be invoked when an entity in this RippleView has
	 * been selected.
	 * 
	 * @param listener The callback that will run
	 */
	public void setOnImageReadyListener(OnImageReadyListener listener) {

		mImageReadyListener = listener;
	}


	public final OnImageReadyListener getOnImageReadyListener() {

		return mImageReadyListener;
	}


	public interface OnImageReadyListener {

		void onImageReady(String key);

	}

}
