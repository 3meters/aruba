package com.proxibase.aircandi.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.anddev.andengine.util.Debug;
import org.anddev.andengine.util.StreamUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.graphics.Bitmap.Config;
import android.os.AsyncTask;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebView.PictureListener;

import com.proxibase.aircandi.candi.utils.CandiConstants;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;

/*
 * TODO: ImageCache uses weak references by default. Should investigate the possible benefits 
 * of switching to using soft references instead.
 */

public class ImageManager {

	private static ImageManager	singletonObject;
	@SuppressWarnings("unused")
	private static String		USER_AGENT			= "Mozilla/5.0 (Linux; U; Android 2.2.1; fr-ch; A43 Build/FROYO) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1";
	private ImageCache			mImageCache;
	private WebView				mWebView;
	private Queue				mWebViewQueue		= new LinkedList<ImageRequest>();
	private boolean				mWebViewProcessing	= false;
	private IImageReadyListener	mImageReadyListener;
	private static Context		mContext;

	public static synchronized ImageManager getInstance() {

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

	public void fetchImageAsynch(ImageRequest imageRequest) {
		if (imageRequest.imageFormat == ImageFormat.Html) {
			mWebViewQueue.offer(imageRequest);
			if (!mWebViewProcessing)
				startWebViewProcessing();
		}
		else if (imageRequest.imageFormat == ImageFormat.Binary) {
			new GetImageBinaryTask().execute(imageRequest);
		}
	}

	public void startWebViewProcessing() {
		mWebView.setPictureListener(null);
		ImageRequest imageRequest = (ImageRequest) mWebViewQueue.poll();
		if (imageRequest != null) {
			mWebViewProcessing = true;
			new GetImageHtmlTask().execute(imageRequest);
		}
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

	public class GetImageHtmlTask extends AsyncTask<ImageRequest, Void, Bitmap> {

		ImageRequest	mImageRequest;

		@Override
		protected Bitmap doInBackground(final ImageRequest... params) {

			// We are on the background thread
			mImageRequest = params[0];
			String webViewContent = "";
			final AtomicBoolean ready = new AtomicBoolean(false);

			webViewContent = ProxibaseService.getInstance().selectAsString(mImageRequest.imageUri, ResponseFormat.Html);
			mWebView.setWebViewClient(new WebViewClient() {

				@Override
				public void onPageFinished(WebView view, String url) {
					ready.set(true);
				}

			});
			mWebView.setPictureListener(new PictureListener() {

				@Override
				public void onNewPicture(WebView view, Picture picture) {

					if (ready.get()) {
						Bitmap bitmap = Bitmap.createBitmap(250, 250, Bitmap.Config.ARGB_8888);
						Canvas canvas = new Canvas(bitmap);
						picture.draw(canvas);

						if (bitmap != null) {
							// Stuff it into the disk and memory caches. Overwrites if it already exists.
							mImageCache.put(mImageRequest.imageId, bitmap);

							// Create reflection if requested
							if (mImageRequest.showReflection) {
								final Bitmap bitmapReflection = AircandiUI.getReflection(bitmap);
								mImageCache.put(mImageRequest.imageId + ".reflection", bitmapReflection);
							}

							// Ready to return to original requestor
							if (mImageRequest.imageReadyListener != null)
								mImageRequest.imageReadyListener.onImageReady(bitmap);
							else if (mImageReadyListener != null) {
								mImageReadyListener.onImageReady(bitmap);
							}
						}
						mWebViewProcessing = false;
						startWebViewProcessing();
					}
				}
			});

			mWebView.loadDataWithBaseURL(mImageRequest.imageUri, webViewContent, "text/html", "utf-8", null);
			return null;
		}
	}

	public class GetImageBinaryTask extends AsyncTask<ImageRequest, Void, Bitmap> {

		ImageRequest	mImageRequest;

		@Override
		protected Bitmap doInBackground(final ImageRequest... params) {

			// We are on the background thread
			mImageRequest = params[0];
			Bitmap bitmap = AircandiUI.getImage(mImageRequest.imageUri);
			if (bitmap != null) {

				Utilities.Log(CandiConstants.APP_NAME, "ImageManager", "Image download completed for image '" + mImageRequest.imageId + "'");

				// Crop if requested
				if (mImageRequest.imageShape.equals("square")) {
					bitmap = AircandiUI.cropToSquare(bitmap);
				}

				// Scale if needed
				if (mImageRequest.widthMinimum > 0 && bitmap.getWidth() != mImageRequest.widthMinimum) {
					float scalingRatio = (float) mImageRequest.widthMinimum / (float) bitmap.getWidth();
					float newHeight = (float) bitmap.getHeight() * scalingRatio;
					bitmap = Bitmap.createScaledBitmap(bitmap, mImageRequest.widthMinimum, (int) (newHeight), true);
				}

				// Stuff it into the disk and memory caches. Overwrites if it already exists.
				mImageCache.put(mImageRequest.imageId, bitmap);

				// Create reflection if requested
				if (mImageRequest.showReflection) {
					final Bitmap bitmapReflection = AircandiUI.getReflection(bitmap);
					mImageCache.put(mImageRequest.imageId + ".reflection", bitmapReflection);
				}

				return bitmap;
			}
			else {
				Utilities.Log(CandiConstants.APP_NAME, "ImageManager", "Image download failed for image '" + mImageRequest.imageId + "'");
				return null;
			}
		}

		@Override
		protected void onPostExecute(final Bitmap bitmap) {

			// We are on the main thread
			super.onPostExecute(bitmap);

			if (bitmap != null) {
				if (mImageRequest.imageReadyListener != null)
					mImageRequest.imageReadyListener.onImageReady(bitmap);
				else if (mImageReadyListener != null) {
					mImageReadyListener.onImageReady(bitmap);
				}
			}
		}
	}

	public void setOnImageReadyListener(IImageReadyListener listener) {
		mImageReadyListener = listener;
	}

	public final IImageReadyListener getOnImageReadyListener() {
		return mImageReadyListener;
	}

	public void setWebView(WebView webView) {
		this.mWebView = webView;
	}

	public WebView getWebView() {
		return mWebView;
	}

	public Queue getQueue() {
		return mWebViewQueue;
	}

	public interface IImageReadyListener {

		void onImageReady(Bitmap bitmap);
	}

	public static class ImageRequest {

		public String				imageUri;
		public String				imageId;
		public String				imageShape			= "native";
		public ImageFormat			imageFormat;
		public int					widthMinimum;
		public boolean				showReflection		= false;
		public IImageReadyListener	imageReadyListener	= null;
	}

	public enum ImageFormat {
		Binary, Html
	}

}
