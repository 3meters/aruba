package com.proxibase.aircandi.utils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Picture;
import android.graphics.Bitmap.CompressFormat;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebView.PictureListener;

import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest;
import com.proxibase.aircandi.utils.ImageManager.ImageRequestListener;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest.ImageFormat;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest.ImageShape;
import com.proxibase.aircandi.utils.Utilities.SimpleCountDownTimer;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.IProgressListener;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;

public class ImageLoader {

	private ImageCache				mImageCache;
	private Map<Object, String>		mImageRequestors	= Collections.synchronizedMap(new WeakHashMap<Object, String>());
	private ImagesQueue				mImagesQueue		= new ImagesQueue();
	private ImagesLoader			mImageLoaderThread	= new ImagesLoader();
	@SuppressWarnings("unused")
	private SimpleCountDownTimer	mCountDownTimer;

	private WebView					mWebView;
	private ImageManager			mImageManager;

	public ImageLoader() {

		/* Make the background thead low priority so it doesn't effect the UI performance. */
		mImageLoaderThread.setPriority(Thread.MIN_PRIORITY);
		mImageLoaderThread.setName("ImageLoader");
	}

	// --------------------------------------------------------------------------------------------
	// Primary entry routines
	// --------------------------------------------------------------------------------------------

	public void fetchImageByProfile(ImageProfile imageProfile, String imageUri, ImageRequestListener listener) {
		ImageRequest imageRequest = getImageRequestByProfile(imageProfile, imageUri, listener);
		if (imageRequest != null) {
			Logger.d(this, "Fetching Image: " + imageUri);
			fetchImage(imageRequest);
		}
	}

	public ImageRequest getImageRequestByProfile(ImageProfile imageProfile, String imageUri, ImageRequestListener listener) {
		ImageRequest imageRequest = null;

		if (imageProfile == ImageProfile.SquareTile) {
			imageRequest = new ImageRequest(imageUri,
					ImageShape.Square,
					ImageFormat.Binary,
					false,
					CandiConstants.IMAGE_WIDTH_MAX,
					false,
					true,
					true,
					1,
					this,
					listener);
		}
		else if (imageProfile == ImageProfile.SquareUser) {
			imageRequest = new ImageRequest(imageUri,
					ImageShape.Native,
					ImageFormat.Binary,
					false,
					CandiConstants.IMAGE_WIDTH_USER_SMALL,
					false,
					true,
					true,
					1,
					this,
					listener);
		}
		else if (imageProfile == ImageProfile.Original) {
			imageRequest = new ImageRequest(imageUri,
					ImageShape.Native,
					ImageFormat.Binary,
					false,
					CandiConstants.IMAGE_WIDTH_ORIGINAL,
					false,
					false,
					false,
					1,
					this,
					listener);
		}
		return imageRequest;
	}

	public void fetchImage(ImageRequest imageRequest) {
		if (imageRequest.imageReadyListener == null) {
			throw new IllegalArgumentException("imageRequest.imageReadyListener is required");
		}

		mImageRequestors.put(imageRequest.imageRequestor, imageRequest.imageUri);

		if (imageRequest.imageUri.toLowerCase().contains("resource:")) {

			String rawResourceName = imageRequest.imageUri.substring(imageRequest.imageUri.indexOf("resource:") + 9);
			String resolvedResourceName = ImageManager.getInstance().resolveResourceName(rawResourceName);

			if (imageRequest.searchCache) {
				Bitmap bitmap = mImageCache.get(resolvedResourceName);
				if (bitmap != null) {
					if (imageRequest.scaleToWidth != CandiConstants.IMAGE_WIDTH_ORIGINAL && imageRequest.scaleToWidth != bitmap.getWidth()) {
						/*
						 * We might have cached a large version of an image so we
						 * need to make sure we honor the image request specifications.
						 */
						bitmap = scaleAndCropBitmap(bitmap, imageRequest);
					}
					imageRequest.imageReadyListener.onImageReady(bitmap);
					return;
				}
			}

			int resourceId = ImageManager.getInstance().getActivity().getResources().getIdentifier(resolvedResourceName, "drawable",
					"com.proxibase.aircandi");
			Bitmap bitmap = ImageManager.getInstance().loadBitmapFromResources(resourceId);

			if (bitmap != null) {
				if (imageRequest.scaleToWidth != CandiConstants.IMAGE_WIDTH_ORIGINAL && imageRequest.scaleToWidth != bitmap.getWidth()) {
					/*
					 * We might have cached a large version of an image so we
					 * need to make sure we honor the image request specifications.
					 */
					bitmap = scaleAndCropBitmap(bitmap, imageRequest);
				}

				/* We put resource images into the cache so they are consistent */
				if (imageRequest.updateCache) {
					Logger.v(this, resolvedResourceName + ": Pushing into cache...");
					mImageCache.put(resolvedResourceName, bitmap);
				}

				imageRequest.imageReadyListener.onImageReady(bitmap);
				return;
			}
			else {
				throw new IllegalStateException("Bitmap resource is null: " + resolvedResourceName);
			}
		}
		else if (imageRequest.imageUri.toLowerCase().contains("asset:")) {
			String assetName = imageRequest.imageUri.substring(imageRequest.imageUri.indexOf("asset:") + 6);
			Bitmap bitmap = null;
			try {
				bitmap = ImageManager.getInstance().loadBitmapFromAssets(assetName);
			}
			catch (ProxibaseException exception) {
				imageRequest.imageReadyListener.onProxibaseException(exception);
			}
			if (bitmap != null) {
				if (imageRequest.scaleToWidth != CandiConstants.IMAGE_WIDTH_ORIGINAL && imageRequest.scaleToWidth != bitmap.getWidth()) {
					bitmap = scaleAndCropBitmap(bitmap, imageRequest);
				}

				/* We put resource images into the cache so they are consistent */
				if (imageRequest.updateCache) {
					Logger.v(this, imageRequest.imageUri + ": Pushing into cache...");
					mImageCache.put(imageRequest.imageUri, bitmap);
				}

				imageRequest.imageReadyListener.onImageReady(bitmap);
				return;
			}
		}
		else {
			if (imageRequest.searchCache) {
				Bitmap bitmap = mImageCache.get(imageRequest.imageUri);
				if (bitmap != null) {
					if (imageRequest.scaleToWidth != CandiConstants.IMAGE_WIDTH_ORIGINAL && imageRequest.scaleToWidth != bitmap.getWidth()) {
						/*
						 * We might have cached a large version of an image so we
						 * need to make sure we honor the image request specifications.
						 */
						bitmap = scaleAndCropBitmap(bitmap, imageRequest);
					}
					imageRequest.imageReadyListener.onImageReady(bitmap);
					return;
				}
			}
		}

		queueImage(imageRequest);
	}

	// --------------------------------------------------------------------------------------------
	// Processing routines
	// --------------------------------------------------------------------------------------------

	private void queueImage(ImageRequest imageRequest) {

		/*
		 * The image requestor may have called for other images before. So there may be some old tasks
		 * in the queue. We need to discard them.
		 */
		mImagesQueue.Clean(imageRequest.imageRequestor);
		synchronized (mImagesQueue.mImagesToLoad) {
			if (imageRequest.priority == 1) {
				mImagesQueue.mImagesToLoad.addFirst(imageRequest);
			}
			else {
				mImagesQueue.mImagesToLoad.add(imageRequest);
			}
			mImagesQueue.mImagesToLoad.notifyAll();
		}

		/* Start thread if it's not started yet */
		if (mImageLoaderThread.getState() == Thread.State.NEW) {
			mImageLoaderThread.start();
		}
	}

	public static Bitmap getBitmap(String url, IProgressListener listener) throws ProxibaseException {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = CandiConstants.IMAGE_CONFIG_DEFAULT;

		/*
		 * We request a byte array for decoding because of a bug
		 * in pre 2.3 versions of android.
		 */
		byte[] imageBytes = (byte[]) ProxibaseService.getInstance().select(url, ResponseFormat.Bytes, listener);
		if (imageBytes == null || imageBytes.length == 0) {
			throw new IllegalStateException("Image byte array is null or empty: " + url);
		}
		Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);
		if (bitmap == null) {
			throw new IllegalStateException("Stream could not be decoded to a bitmap: " + url);
		}

		return bitmap;
	}

	public void getWebPageAsBitmap(String uri, final ImageRequest imageRequest, final ImageRequestListener listener) {

		//String webViewContent = "";
		final AtomicBoolean ready = new AtomicBoolean(false);
		final AtomicInteger pictureCount = new AtomicInteger(0);

		//		try {
		//			webViewContent = (String) ProxibaseService.getInstance().select(uri, ResponseFormat.Html, null);
		//		}
		//		catch (ProxibaseException exception) {
		//			listener.onProxibaseException(exception);
		//		}

		/*
		 * Setting WideViewPort to false will cause html text to layout to try and fit the sizing of the
		 * webview though our screen capture will still be cover the full page width. Ju
		 * 
		 * Setting to true will handle text nicely but will show the full width
		 * of the webview even if the page content only fills a portion of it.
		 * 
		 * We might have to have a property to control the desired result when
		 * using html for the tile display image.
		 * 
		 * Makes the Webview have a normal viewport (such as a normal desktop browser), while when false the webview
		 * will have a viewport constrained to it's own dimensions (so if the webview is 50px*50px the viewport will be
		 * the same size)
		 */

		mWebView.getSettings().setUseWideViewPort(true);
		if (imageRequest.imageFormat == ImageFormat.HtmlZoom) {
			mWebView.getSettings().setUseWideViewPort(false);
		}

		/* Clear */
		//mWebView.loadUrl("about:blank");
		//mWebView.loadData("<html><head></head><body></body></html>", "text/html", "UTF-8");

		/*
		 * We turn off javascript for performance reasons. Some pages will render differently
		 * because javascript is used to control UI elements.
		 */
		//mWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
		//mWebView.getSettings().setAppCacheEnabled(false);
		mWebView.getSettings().setUserAgentString(CandiConstants.USER_AGENT);
		//mWebView.setDrawingCacheEnabled(false);
		//mWebView.clearView();
		mWebView.getSettings().setJavaScriptEnabled(imageRequest.javascriptEnabled);
		mWebView.getSettings().setLoadWithOverviewMode(true);
		mWebView.refreshDrawableState();

		//mWebView.requestLayout();
		//		mWebView.invalidate();
		//		mWebView.postInvalidate();

		mWebView.setWebViewClient(new WebViewClient() {

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				super.onPageStarted(view, url, favicon);
				Logger.v(this, "Page started: " + url);
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
				Logger.v(this, "Page finished: " + url);
				ready.set(true);
			}
		});

		mWebView.setWebChromeClient(new WebChromeClient() {

			@Override
			public void onProgressChanged(WebView view, int progress) {
				listener.onProgressChanged(progress);
				/*
				 * Somehow I am getting called here even though the web view is still
				 * showing content from previous site. Is draw pulling from drawing cache?
				 */
				//				if (!imageSent.get() && ready.get() && progress == 100)
				//				{
				//					//Picture picture = mWebView.capturePicture();
				//					final Bitmap bitmap = Bitmap.createBitmap(CandiConstants.CANDI_VIEW_WIDTH, CandiConstants.CANDI_VIEW_WIDTH,
				//							CandiConstants.IMAGE_CONFIG_DEFAULT);
				//					Canvas canvas = new Canvas(bitmap);
				//
				//					Matrix matrix = new Matrix();
				//					float scale = (float) CandiConstants.CANDI_VIEW_WIDTH / (float) view.getWidth();
				//					matrix.postScale(scale, scale);
				//
				//					canvas.setMatrix(matrix);
				//					//view.destroyDrawingCache();
				//					view.draw(canvas);
				//					//canvas.drawPicture(picture);
				//
				//					/* Release */
				//					canvas = null;
				//					listener.onImageReady(bitmap);
				//					imageSent.set(true);
				//				}
			}
		});

		mWebView.setPictureListener(new PictureListener() {

			@Override
			public void onNewPicture(WebView view, Picture picture) {
				/*
				 * Sometimes the first call isn't finished with layout but the
				 * second one is correct. How can we tell the difference?
				 */

				if (ready.get()) {
					pictureCount.getAndIncrement();

					final Bitmap bitmap = Bitmap.createBitmap(CandiConstants.CANDI_VIEW_WIDTH, CandiConstants.CANDI_VIEW_WIDTH,
													CandiConstants.IMAGE_CONFIG_DEFAULT);
					Canvas canvas = new Canvas(bitmap);

					Matrix matrix = new Matrix();
					float scale = (float) CandiConstants.CANDI_VIEW_WIDTH / (float) picture.getWidth();
					matrix.postScale(scale, scale);

					canvas.setMatrix(matrix);
					canvas.drawPicture(picture);

					/* Release */
					canvas = null;
					listener.onImageReady(bitmap);

					/* We only allow a maximum of two picture calls */
					if (pictureCount.get() >= 2) {
						mWebView.setPictureListener(null);
					}
				}

			}
		});

		//mWebView.loadDataWithBaseURL(uri, webViewContent, "text/html", "utf-8", null);
		mWebView.loadUrl(uri);

	}

	public Bitmap scaleAndCropBitmap(Bitmap bitmap, ImageRequest imageRequest) {

		/* Crop if requested */
		Bitmap bitmapCropped;
		if (imageRequest.imageShape == ImageShape.Square) {
			bitmapCropped = ImageUtils.cropToSquare(bitmap);
		}
		else {
			bitmapCropped = bitmap;
		}

		/* Scale if needed */
		Bitmap bitmapCroppedScaled;
		if (imageRequest.scaleToWidth > 0 && bitmapCropped.getWidth() != imageRequest.scaleToWidth) {
			float scalingRatio = (float) imageRequest.scaleToWidth / (float) bitmapCropped.getWidth();
			float newHeight = (float) bitmapCropped.getHeight() * scalingRatio;
			bitmapCroppedScaled = Bitmap.createScaledBitmap(bitmapCropped, imageRequest.scaleToWidth, (int) (newHeight), true);
		}
		else {
			bitmapCroppedScaled = bitmapCropped;
		}

		/* Make sure the bitmap format is right */
		Bitmap bitmapFinal;
		if (!bitmapCroppedScaled.getConfig().name().equals(CandiConstants.IMAGE_CONFIG_DEFAULT.toString())) {
			bitmapFinal = bitmapCroppedScaled.copy(CandiConstants.IMAGE_CONFIG_DEFAULT, false);
		}
		else {
			bitmapFinal = bitmapCroppedScaled;
		}

		if (bitmapFinal.isRecycled()) {
			throw new IllegalArgumentException("bitmapFinal has been recycled");
		}

		return bitmapFinal;
	}

	// --------------------------------------------------------------------------------------------
	// Setter/Getter routines
	// --------------------------------------------------------------------------------------------

	public void setImageCache(ImageCache imageCache) {
		this.mImageCache = imageCache;
	}

	public ImageCache getImageCache() {
		return mImageCache;
	}

	public void setWebView(WebView webView) {
		mWebView = webView;
	}

	// --------------------------------------------------------------------------------------------
	// Loader routines
	// --------------------------------------------------------------------------------------------

	public class ImagesQueue {

		private LinkedList<ImageRequest>	mImagesToLoad	= new LinkedList<ImageRequest>();

		/* Removes all instances of imageRequest associated with the imageRequestor */
		public void Clean(Object imageRequestor) {
			for (int j = 0; j < mImagesToLoad.size();) {
				if (mImagesToLoad.get(j).imageRequestor == imageRequestor) {
					mImagesToLoad.remove(j);
				}
				else {
					++j;
				}
			}
		}
	}

	public class ImagesLoader extends Thread {

		private boolean	processingWebPage	= false;

		public void run() {
			try {

				while (true) {

					/* Thread waits until there are any images to load in the queue */
					if (mImagesQueue.mImagesToLoad.size() == 0) {
						synchronized (mImagesQueue.mImagesToLoad) {
							mImagesQueue.mImagesToLoad.wait();
						}
					}

					if (mImagesQueue.mImagesToLoad.size() != 0) {
						final ImageRequest imageRequest;
						synchronized (mImagesQueue.mImagesToLoad) {
							ImageRequest imageRequestCheck = mImagesQueue.mImagesToLoad.peek();
							if (imageRequestCheck.imageFormat == ImageFormat.Html || imageRequestCheck.imageFormat == ImageFormat.HtmlZoom) {
								if (processingWebPage) {
									mImagesQueue.mImagesToLoad.wait();
								}
							}
							imageRequest = mImagesQueue.mImagesToLoad.poll();
						}

						Bitmap bitmap = null;
						if (imageRequest.imageFormat == ImageFormat.Html || imageRequest.imageFormat == ImageFormat.HtmlZoom) {
							processingWebPage = true;
							//							mWebView.stopLoading();
							//							mWebView.setPictureListener(null);
							//							mWebView.setWebChromeClient(null);
							//							mWebView.setWebViewClient(null);
							Logger.v(this, "Starting html image processing: " + imageRequest.imageUri);
							getWebPageAsBitmap(imageRequest.imageUri, imageRequest, new ImageRequestListener() {

								@Override
								public void onImageReady(Bitmap bitmap) {
									processingWebPage = false;

									/* It safe to start processing another web page image if we have one */
									synchronized (mImagesQueue.mImagesToLoad) {
										mImagesQueue.mImagesToLoad.notifyAll();
									}

									/* Perform requested post processing */
									bitmap = scaleAndCropBitmap(bitmap, imageRequest);

									/* Stuff it into the cache. Overwrites if it already exists. */
									mImageCache.put(imageRequest.imageUri, bitmap, CompressFormat.JPEG);

									/* Create reflection if requested */
									if (imageRequest.makeReflection) {
										final Bitmap bitmapReflection = ImageUtils.makeReflection(bitmap, true);
										mImageCache.put(imageRequest.imageUri + ".reflection", bitmapReflection, CompressFormat.PNG);
										if (mImageCache.isFileCacheOnly()) {
											bitmapReflection.recycle();
										}
									}

									Logger.v(this, "Html image processed: " + imageRequest.imageUri);
									Logger.v(this, "Html image hashcode: " + Utilities.md5HashForBitmap(bitmap));
									String uri = mImageRequestors.get(imageRequest.imageRequestor);
									if (uri != null && uri.equals(imageRequest.imageUri)) {
										imageRequest.imageReadyListener.onImageReady(bitmap);
									}
								}

								@Override
								public void onProgressChanged(int progress) {
									imageRequest.imageReadyListener.onProgressChanged(progress);
								}

								@Override
								public void onProxibaseException(ProxibaseException exception) {
									imageRequest.imageReadyListener.onProxibaseException(exception);
								}
							});
						}
						else if (imageRequest.imageFormat == ImageFormat.Binary) {
							long startTime = System.nanoTime();
							float estimatedTime = System.nanoTime();
							Logger.v(this, imageRequest.imageUri + ": Download started...");

							try {
								bitmap = getBitmap(imageRequest.imageUri, new IProgressListener() {

									@Override
									public boolean onProgressChanged(int progress) {
										if (imageRequest.imageReadyListener != null) {
											if (progress > 0) {
												imageRequest.imageReadyListener.onProgressChanged((int) (70 * ((float) progress / 100f)));
											}
										}
										return false;
									}
								});

								Logger.v(this, imageRequest.imageUri + ": Download finished: " + String.valueOf(estimatedTime / 1000000) + "ms");
							}
							catch (ProxibaseException exception) {
								imageRequest.imageReadyListener.onProxibaseException(exception);
							}

							//imageRequest.imageReadyListener.onProgressChanged(70);
							estimatedTime = System.nanoTime() - startTime;
							startTime = System.nanoTime();

							/* Perform requested post processing */
							if (imageRequest.scaleToWidth != CandiConstants.IMAGE_WIDTH_ORIGINAL) {
								Logger.v(this, imageRequest.imageUri + ": Scale and crop...");
								bitmap = scaleAndCropBitmap(bitmap, imageRequest);
							}

							estimatedTime = System.nanoTime() - startTime;
							startTime = System.nanoTime();
							Logger.v(this, imageRequest.imageUri + ": Post processing: " + String.valueOf(estimatedTime / 1000000) + "ms");

							/*
							 * Stuff it into the cache. Overwrites if it already exists.
							 * This is a perf hit in the process cause writing files is slow.
							 */
							if (imageRequest.updateCache) {
								Logger.v(this, imageRequest.imageUri + ": Pushing into cache...");
								mImageCache.put(imageRequest.imageUri, bitmap);
							}

							/* Create reflection if requested */
							imageRequest.imageReadyListener.onProgressChanged(80);
							if (imageRequest.makeReflection) {
								final Bitmap bitmapReflection = ImageUtils.makeReflection(bitmap, true);
								estimatedTime = System.nanoTime() - startTime;
								startTime = System.nanoTime();
								Logger.v(this, imageRequest.imageUri + ": Reflection created: " + String.valueOf(estimatedTime / 1000000) + "ms");

								mImageCache.put(imageRequest.imageUri + ".reflection", bitmapReflection);
								if (mImageCache.isFileCacheOnly()) {
									bitmapReflection.recycle();
								}
							}

							String uri = mImageRequestors.get(imageRequest.imageRequestor);
							if (uri != null && uri.equals(imageRequest.imageUri)) {
								imageRequest.imageReadyListener.onProgressChanged(100);
								imageRequest.imageReadyListener.onImageReady(bitmap);
							}
						}
					}
					if (Thread.interrupted()) {
						break;
					}
				}
			}
			catch (InterruptedException e) {
				/* Allow thread to exit */
			}
		}
	}

	public void stopThread() {
		/*
		 * We call this when the search activity is being destroyed. S
		 */
		mImageLoaderThread.interrupt();
		mImagesQueue.mImagesToLoad.clear();
		mImageLoaderThread = new ImagesLoader();
	}

	public void setImageManager(ImageManager imageManager) {
		this.mImageManager = imageManager;
	}

	public ImageManager getImageManager() {
		return mImageManager;
	}

	public enum ImageProfile {
		SquareTile,
		Original,
		SquareUser
	}
}
