package com.proxibase.aircandi.utils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
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
import com.proxibase.aircandi.utils.ImageManager.IImageRequestListener;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest.ImageFormat;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest.ImageShape;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.IProgressListener;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException.ProxiErrorCode;

public class ImageLoader {

	private ImageCache			mImageCache;
	private Map<Object, String>	mImageRequestors	= Collections.synchronizedMap(new WeakHashMap<Object, String>());
	private ImagesQueue			mImagesQueue		= new ImagesQueue();
	private ImagesLoader		mImageLoaderThread	= new ImagesLoader();
	private WebView				mWebView;

	public ImageLoader(Context context) {

		/* Make the background thead low priority so it doesn't effect the UI performance. */
		mImageLoaderThread.setPriority(Thread.MIN_PRIORITY);
		mImageLoaderThread.setName("ImageLoader");
	}

	public void fetchImage(ImageRequest imageRequest, boolean skipCache) {
		if (imageRequest.imageReadyListener == null) {
			throw new IllegalArgumentException("imageRequest.imageReadyListener is required");
		}
		mImageRequestors.put(imageRequest.imageRequestor, imageRequest.imageUri);
		if (!skipCache) {
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
		queueImage(imageRequest);
	}

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
		if (mImageLoaderThread.getState() == Thread.State.NEW)
			mImageLoaderThread.start();
	}

	public static Bitmap getBitmap(String url, IProgressListener listener) throws ProxibaseException {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = CandiConstants.IMAGE_CONFIG_DEFAULT;

		/*
		 * We request a byte array for decoding because of a bug
		 * in pre 2.3 versions of android.
		 */
		byte[] imageBytes = (byte[]) ProxibaseService.getInstance().select(url, ResponseFormat.Bytes, listener);
		Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);
		if (bitmap == null) {
			throw new IllegalStateException("Stream could not be decoded to a bitmap: " + url);
		}

		return bitmap;
	}

	public void getWebPageAsBitmap(String uri, ImageRequest imageRequest, final IImageRequestListener listener) {
		String webViewContent = "";
		final AtomicBoolean ready = new AtomicBoolean(false);

		try {
			webViewContent = (String) ProxibaseService.getInstance().select(uri, ResponseFormat.Html, null);
		}
		catch (ProxibaseException exception) {
			listener.onProxibaseException(exception);
		}

		/*
		 * We turn off javascript for performance reasons. Some pages will render differently
		 * because javascript is used to control UI elements.
		 */
		mWebView.getSettings().setUserAgentString(CandiConstants.USER_AGENT);
		mWebView.getSettings().setJavaScriptEnabled(imageRequest.javascriptEnabled);
		mWebView.getSettings().setLoadWithOverviewMode(true);

		/*
		 * Setting WideViewPort to false will cause html text to layout to try and fit the sizing of the
		 * webview though our screen capture will still be cover the full page width. Ju
		 * 
		 * Setting to true will handle text nicely but will show the full width
		 * of the webview even if the page content only fills a portion of it.
		 * 
		 * We might have to have a property to control the desired result when
		 * using html for the tile display image.
		 */

		if (imageRequest.imageFormat == ImageFormat.HtmlZoom) {
			mWebView.getSettings().setUseWideViewPort(false);
		}
		else if (imageRequest.imageFormat == ImageFormat.Html) {
			mWebView.getSettings().setUseWideViewPort(true);
		}

		mWebView.setWebViewClient(new WebViewClient() {

			@Override
			public void onPageFinished(WebView view, String url) {
				ready.set(true);
			}
		});

		mWebView.setWebChromeClient(new WebChromeClient() {

			@Override
			public void onProgressChanged(WebView view, int progress) {
				listener.onProgressChanged(progress);
			}
		});

		mWebView.setPictureListener(new PictureListener() {

			@Override
			public void onNewPicture(WebView view, Picture picture) {

				if (ready.get()) {
					Bitmap bitmap = Bitmap.createBitmap(CandiConstants.CANDI_VIEW_WIDTH, CandiConstants.CANDI_VIEW_WIDTH,
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
					mWebView.setPictureListener(null);
				}
			}
		});

		mWebView.loadDataWithBaseURL(uri, webViewContent, "text/html", "utf-8", null);
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

	public void stopThread() {
		mImageLoaderThread.interrupt();
	}

	public void setImageCache(ImageCache imageCache) {
		this.mImageCache = imageCache;
	}

	public ImageCache getImageCache() {
		return mImageCache;
	}

	public void setWebView(WebView webView) {
		mWebView = webView;
	}

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
							imageRequest = mImagesQueue.mImagesToLoad.poll();
						}

						Bitmap bitmap = null;
						if (imageRequest.imageFormat == ImageFormat.Html || imageRequest.imageFormat == ImageFormat.HtmlZoom) {
							getWebPageAsBitmap(imageRequest.imageUri, imageRequest, new IImageRequestListener() {

								@Override
								public void onImageReady(Bitmap bitmap) {

									/* Perform requested post processing */
									bitmap = scaleAndCropBitmap(bitmap, imageRequest);

									/* Stuff it into the cache. Overwrites if it already exists. */
									mImageCache.put(imageRequest.imageUri, bitmap, CompressFormat.JPEG);

									/* Create reflection if requested */
									if (imageRequest.makeReflection) {
										final Bitmap bitmapReflection = ImageUtils.getReflection(bitmap);
										mImageCache.put(imageRequest.imageUri + ".reflection", bitmapReflection, CompressFormat.PNG);
										if (mImageCache.isFileCacheOnly()) {
											bitmapReflection.recycle();
										}
									}

									Logger.v(CandiConstants.APP_NAME, this.getClass().getSimpleName(),
											"Html image processed: " + imageRequest.imageUri);
									String uri = mImageRequestors.get(imageRequest.imageRequestor);
									if (uri != null && uri.equals(imageRequest.imageUri)) {
										imageRequest.imageReadyListener.onImageReady(bitmap);
									}
								}

								@Override
								public boolean onProgressChanged(int progress)
								{
									imageRequest.imageReadyListener.onProgressChanged(progress);
									return true;
								}

								@Override
								public void onProxibaseException(ProxibaseException exception) {
									if (exception.getErrorCode() == ProxiErrorCode.OperationFailed) {
										Bitmap bitmap = ImageManager.getInstance().loadBitmapFromAssets("gfx/placeholder3.png");
										imageRequest.imageReadyListener.onImageReady(bitmap);
									}
								}
							});
						}
						else if (imageRequest.imageFormat == ImageFormat.Binary) {
							long startTime = System.nanoTime();
							float estimatedTime = System.nanoTime();
							Logger.v(CandiConstants.APP_NAME, this.getClass().getSimpleName(), imageRequest.imageUri + ": Download started...");

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

								Logger.v(CandiConstants.APP_NAME, this.getClass().getSimpleName(), imageRequest.imageUri + ": Download finished: "
																									+ String.valueOf(estimatedTime / 1000000)
																									+ "ms");
							}
							catch (ProxibaseException exception) {
								bitmap = ImageManager.getInstance().loadBitmapFromAssets("gfx/placeholder3.png");
								Logger.v(CandiConstants.APP_NAME, this.getClass().getSimpleName(),
										imageRequest.imageUri + ": Download failed, using placeholder: "
												+ String.valueOf(estimatedTime / 1000000)
												+ "ms");
							}

							//imageRequest.imageReadyListener.onProgressChanged(70);
							estimatedTime = System.nanoTime() - startTime;
							startTime = System.nanoTime();

							/* Perform requested post processing */
							if (imageRequest.scaleToWidth != CandiConstants.IMAGE_WIDTH_ORIGINAL) {
								Logger.v(CandiConstants.APP_NAME, this.getClass().getSimpleName(), imageRequest.imageUri + ": Scale and crop...");
								bitmap = scaleAndCropBitmap(bitmap, imageRequest);
							}

							estimatedTime = System.nanoTime() - startTime;
							startTime = System.nanoTime();
							Logger.v(CandiConstants.APP_NAME, this.getClass().getSimpleName(), imageRequest.imageUri + ": Post processing: "
																									+ String.valueOf(estimatedTime / 1000000)
																									+ "ms");

							/*
							 * Stuff it into the cache. Overwrites if it already exists.
							 * This is a perf hit in the process cause writing files is slow.
							 */
							if (imageRequest.cachingAllowed) {
								Logger.v(CandiConstants.APP_NAME, this.getClass().getSimpleName(), imageRequest.imageUri + ": Pushing into cache...");
								mImageCache.put(imageRequest.imageUri, bitmap);
							}

							/* Create reflection if requested */
							imageRequest.imageReadyListener.onProgressChanged(80);
							if (imageRequest.makeReflection) {
								final Bitmap bitmapReflection = ImageUtils.getReflection(bitmap);
								estimatedTime = System.nanoTime() - startTime;
								startTime = System.nanoTime();
								Logger.v(CandiConstants.APP_NAME, this.getClass().getSimpleName(),
										imageRequest.imageUri + ": Reflection created: " + String.valueOf(estimatedTime / 1000000) + "ms");

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
}
