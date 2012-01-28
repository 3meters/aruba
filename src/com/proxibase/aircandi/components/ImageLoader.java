package com.proxibase.aircandi.components;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Picture;
import android.graphics.Bitmap.CompressFormat;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebView.PictureListener;

import com.proxibase.aircandi.components.ImageRequest.ImageFormat;
import com.proxibase.aircandi.components.ImageRequest.ImageResponse;
import com.proxibase.aircandi.components.ImageRequest.ImageShape;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ResultCodeDetail;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.sdk.android.proxi.service.ServiceRequest;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestListener;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;

public class ImageLoader {

	private ImageCache		mImageCache;
	private ImagesQueue		mImagesQueue		= new ImagesQueue();
	private ImagesLoader	mImageLoaderThread	= new ImagesLoader();
	private WebView			mWebView;

	public ImageLoader() {

		/* Make the background thead low priority so it doesn't effect the UI performance. */
		mImageLoaderThread.setPriority(Thread.MIN_PRIORITY);
		mImageLoaderThread.setName("ImageLoader");
	}

	// --------------------------------------------------------------------------------------------
	// Primary entry routines
	// --------------------------------------------------------------------------------------------

	public void fetchImage(ImageRequest imageRequest) {

		ServiceResponse serviceResponse = new ServiceResponse();
		if (imageRequest.getRequestListener() == null) {
			throw new IllegalArgumentException("imageRequest.imageReadyListener is required");
		}

		if (imageRequest.getImageUri().toLowerCase().startsWith("resource:")) {

			String rawResourceName = imageRequest.getImageUri().substring(imageRequest.getImageUri().indexOf("resource:") + 9);
			String resolvedResourceName = ImageManager.getInstance().resolveResourceName(rawResourceName);

			if (imageRequest.doSearchCache()) {
				Bitmap bitmap = mImageCache.get(resolvedResourceName);
				if (bitmap != null) {
					if (imageRequest.getScaleToWidth() != CandiConstants.IMAGE_WIDTH_ORIGINAL && imageRequest.getScaleToWidth() != bitmap.getWidth()) {
						/*
						 * We might have cached a large version of an image so we
						 * need to make sure we honor the image request specifications.
						 */
						bitmap = scaleAndCropBitmap(bitmap, imageRequest);
					}
					serviceResponse.data = new ImageResponse(bitmap, imageRequest.getImageUri());
					imageRequest.getRequestListener().onComplete(serviceResponse);
					return;
				}
			}

			int resourceId = ImageManager.getInstance().getActivity().getResources().getIdentifier(resolvedResourceName, "drawable",
					"com.proxibase.aircandi");
			Bitmap bitmap = ImageManager.getInstance().loadBitmapFromResources(resourceId);

			if (bitmap != null) {
				if (imageRequest.getScaleToWidth() != CandiConstants.IMAGE_WIDTH_ORIGINAL && imageRequest.getScaleToWidth() != bitmap.getWidth()) {
					/*
					 * We might have cached a large version of an image so we
					 * need to make sure we honor the image request specifications.
					 */
					bitmap = scaleAndCropBitmap(bitmap, imageRequest);
				}

				/* We put resource images into the cache so they are consistent */
				if (imageRequest.doUpdateCache()) {
					Logger.v(this, resolvedResourceName + ": Pushing into cache...");
					mImageCache.put(resolvedResourceName, bitmap);
				}

				serviceResponse.data = new ImageResponse(bitmap, imageRequest.getImageUri());
				imageRequest.getRequestListener().onComplete(serviceResponse);
				return;
			}
			else {
				throw new IllegalStateException("Bitmap resource is null: " + resolvedResourceName);
			}
		}
		else if (imageRequest.getImageUri().toLowerCase().startsWith("asset:")) {

			String assetName = imageRequest.getImageUri().substring(imageRequest.getImageUri().indexOf("asset:") + 6);
			Bitmap bitmap = ImageManager.getInstance().loadBitmapFromAssets(assetName);
			if (bitmap != null) {
				if (imageRequest.getScaleToWidth() != CandiConstants.IMAGE_WIDTH_ORIGINAL && imageRequest.getScaleToWidth() != bitmap.getWidth()) {
					bitmap = scaleAndCropBitmap(bitmap, imageRequest);
				}

				/* We put resource images into the cache so they are consistent */
				if (imageRequest.doUpdateCache()) {
					Logger.v(this, imageRequest.getImageUri() + ": Pushing into cache...");
					mImageCache.put(imageRequest.getImageUri(), bitmap);
				}

				serviceResponse.data = new ImageResponse(bitmap, imageRequest.getImageUri());
				imageRequest.getRequestListener().onComplete(serviceResponse);
				return;
			}
		}
		else {

			if (imageRequest.doSearchCache()) {
				Bitmap bitmap = mImageCache.get(imageRequest.getImageUri());
				if (bitmap != null) {
					Logger.v(this, "Image request satisfied from cache: " + imageRequest.getImageUri());
					if (imageRequest.getScaleToWidth() != CandiConstants.IMAGE_WIDTH_ORIGINAL && imageRequest.getScaleToWidth() != bitmap.getWidth()) {
						/*
						 * We might have cached a large version of an image so we
						 * need to make sure we honor the image request specifications.
						 */
						bitmap = scaleAndCropBitmap(bitmap, imageRequest);

						/* Create reflection if requested and needed */

						Bitmap reflectionBitmap = ImageManager.getInstance().getImage(imageRequest.getImageUri() + ".reflection");
						if (reflectionBitmap == null) {
							if (imageRequest.getMakeReflection()) {
								final Bitmap bitmapReflection = ImageUtils.makeReflection(bitmap, true);
								mImageCache.put(imageRequest.getImageUri() + ".reflection", bitmapReflection, CompressFormat.PNG);
								if (mImageCache.isFileCacheOnly()) {
									bitmapReflection.recycle();
								}
							}
						}
					}
					serviceResponse.data = new ImageResponse(bitmap, imageRequest.getImageUri());
					imageRequest.getRequestListener().onComplete(serviceResponse);
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
		mImagesQueue.Clean(imageRequest.getImageRequestor());
		synchronized (mImagesQueue.mImagesToLoad) {
			Logger.v(this, imageRequest.getImageUri() + ": Queued for download");
			mImagesQueue.mImagesToLoad.add(imageRequest);
			mImagesQueue.mImagesToLoad.notifyAll();
		}

		/* Start thread if it's not started yet */
		if (mImageLoaderThread.getState() == Thread.State.NEW) {
			mImageLoaderThread.start();
		}
	}

	private static ServiceResponse getBitmap(String url, ImageRequest imageRequest, RequestListener listener) {
		/*
		 * We request a byte array for decoding because of a bug
		 * in pre 2.3 versions of android.
		 */
		ServiceRequest serviceRequest = new ServiceRequest();
		serviceRequest.setUri(url);
		serviceRequest.setRequestType(RequestType.Get);
		serviceRequest.setResponseFormat(ResponseFormat.Bytes);
		serviceRequest.setRequestListener(listener);

		ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (serviceResponse.responseCode == ResponseCode.Success) {

			byte[] imageBytes = (byte[]) serviceResponse.data;

			/* Turn byte array into bitmap that fits in our desired max size */
			Logger.v(null, url + ": " + String.valueOf(imageBytes.length) + " bytes received");
			Bitmap bitmap = ImageManager.getInstance().bitmapForByteArraySampled(imageBytes, imageRequest, CandiConstants.IMAGE_BYTES_MAX);

			if (bitmap == null) {
				throw new IllegalStateException("Stream could not be decoded to a bitmap: " + url);
			}
			else {
				serviceResponse.data = bitmap;
			}
		}
		return serviceResponse;
	}

	private void getWebPageAsBitmap(String uri, final ImageRequest imageRequest, final RequestListener listener) {

		//String webViewContent = "";
		final ServiceResponse serviceResponse = new ServiceResponse(ResponseCode.Success, ResultCodeDetail.Success, null, null);
		final AtomicBoolean ready = new AtomicBoolean(false);
		final AtomicInteger pictureCount = new AtomicInteger(0);

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
		if (imageRequest.getLinkZoom()) {
			mWebView.getSettings().setUseWideViewPort(false);
		}
		mWebView.getSettings().setUserAgentString(CandiConstants.USER_AGENT);
		mWebView.getSettings().setJavaScriptEnabled(imageRequest.getLinkJavascriptEnabled());
		mWebView.getSettings().setLoadWithOverviewMode(true);
		mWebView.refreshDrawableState();

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
					serviceResponse.data = bitmap;
					listener.onComplete(serviceResponse);

					/* We only allow a maximum of two picture calls */
					if (pictureCount.get() >= 2) {
						mWebView.setPictureListener(null);
					}
				}
			}
		});

		mWebView.loadUrl(uri);
	}

	private Bitmap scaleAndCropBitmap(Bitmap bitmap, ImageRequest imageRequest) {

		/* Crop if requested */
		Bitmap bitmapCropped;
		if (imageRequest.getImageShape() == ImageShape.Square) {
			bitmapCropped = ImageUtils.cropToSquare(bitmap);
		}
		else {
			bitmapCropped = bitmap;
		}

		/* Scale if needed */
		Bitmap bitmapCroppedScaled;
		if (imageRequest.getScaleToWidth() > 0 && bitmapCropped.getWidth() != imageRequest.getScaleToWidth()) {
			float scalingRatio = (float) imageRequest.getScaleToWidth() / (float) bitmapCropped.getWidth();
			float newHeight = (float) bitmapCropped.getHeight() * scalingRatio;
			bitmapCroppedScaled = Bitmap.createScaledBitmap(bitmapCropped, imageRequest.getScaleToWidth(), (int) (newHeight), true);
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

	private class ImagesQueue {

		private LinkedList<ImageRequest>	mImagesToLoad	= new LinkedList<ImageRequest>();

		/* Removes all instances of imageRequest associated with the imageRequestor */
		public void Clean(Object imageRequestor) {
			for (int j = 0; j < mImagesToLoad.size();) {
				if (mImagesToLoad.get(j).getImageRequestor() == imageRequestor) {
					mImagesToLoad.remove(j);
				}
				else {
					++j;
				}
			}
		}
	}

	private class ImagesLoader extends Thread {

		private boolean	processingWebPage	= false;

		public void run() {

			try {

				while (true) {

					/* Thread waits until there are any images to load in the queue */
					if (mImagesQueue.mImagesToLoad.size() == 0) {
						synchronized (mImagesQueue.mImagesToLoad) {
							mImagesQueue.mImagesToLoad.wait();
							Logger.v(this, "ImageQueue thread is waiting");
						}
					}

					if (mImagesQueue.mImagesToLoad.size() != 0) {
						Logger.v(this, "ImageQueue has " + String.valueOf(mImagesQueue.mImagesToLoad.size()) + " images requests pending");
						final ImageRequest imageRequest;
						synchronized (mImagesQueue.mImagesToLoad) {
							ImageRequest imageRequestCheck = mImagesQueue.mImagesToLoad.peek();
							if (imageRequestCheck.getImageFormat() == ImageFormat.Html) {
								if (processingWebPage) {
									Logger.v(this, "ImageQueue thread is waiting");
									mImagesQueue.mImagesToLoad.wait();
								}
							}
							imageRequest = mImagesQueue.mImagesToLoad.poll();
						}

						Bitmap bitmap = null;
						ServiceResponse serviceResponse = new ServiceResponse();
						/*
						 * Html image
						 */
						if (imageRequest.getImageFormat() == ImageFormat.Html) {

							processingWebPage = true;
							Logger.v(this, "Starting html image processing: " + imageRequest.getImageUri());
							getWebPageAsBitmap(imageRequest.getImageUri(), imageRequest, new RequestListener() {

								@Override
								public void onComplete(Object response) {

									ServiceResponse serviceResponse = (ServiceResponse) response;
									if (serviceResponse.responseCode == ResponseCode.Success) {
										processingWebPage = false;

										/* It safe to start processing another web page image if we have one */
										synchronized (mImagesQueue.mImagesToLoad) {
											mImagesQueue.mImagesToLoad.notifyAll();
										}

										/* Perform requested post processing */
										Bitmap bitmap = (Bitmap) serviceResponse.data;
										bitmap = scaleAndCropBitmap(bitmap, imageRequest);

										/* Stuff it into the cache. Overwrites if it already exists. */
										mImageCache.put(imageRequest.getImageUri(), bitmap, CompressFormat.JPEG);

										/* Create reflection if requested */
										if (imageRequest.getMakeReflection()) {
											final Bitmap bitmapReflection = ImageUtils.makeReflection(bitmap, true);
											mImageCache.put(imageRequest.getImageUri() + ".reflection", bitmapReflection, CompressFormat.PNG);
											if (mImageCache.isFileCacheOnly()) {
												bitmapReflection.recycle();
											}
										}

										Logger.v(this, "Html image processed: " + imageRequest.getImageUri());
										serviceResponse.data = new ImageResponse(bitmap, imageRequest.getImageUri());
									}
									imageRequest.getRequestListener().onComplete(serviceResponse);
								}

								@Override
								public void onProgressChanged(int progress) {
									imageRequest.getRequestListener().onProgressChanged(progress);
								}
							});
						}
						/*
						 * Binary image
						 */
						else if (imageRequest.getImageFormat() == ImageFormat.Binary) {

							long startTime = System.nanoTime();
							float estimatedTime = System.nanoTime();
							Logger.v(this, imageRequest.getImageUri() + ": Download started...");

							serviceResponse = getBitmap(imageRequest.getImageUri(), imageRequest, new RequestListener() {

								@Override
								public void onProgressChanged(int progress) {
									if (imageRequest.getRequestListener() != null) {
										if (progress > 0) {
											imageRequest.getRequestListener().onProgressChanged((int) (70 * ((float) progress / 100f)));
										}
									}
								}
							});

							if (serviceResponse.responseCode == ResponseCode.Success) {

								bitmap = (Bitmap) serviceResponse.data;
								Logger.v(this, imageRequest.getImageUri() + ": Download finished: " + String.valueOf(estimatedTime / 1000000) + "ms");

								//imageRequest.imageReadyListener.onProgressChanged(70);
								estimatedTime = System.nanoTime() - startTime;
								startTime = System.nanoTime();

								/* Perform requested post processing */
								if (imageRequest.getScaleToWidth() != CandiConstants.IMAGE_WIDTH_ORIGINAL) {
									Logger.v(this, imageRequest.getImageUri() + ": Scale and crop...");
									bitmap = scaleAndCropBitmap(bitmap, imageRequest);
								}

								estimatedTime = System.nanoTime() - startTime;
								startTime = System.nanoTime();
								Logger.v(this, imageRequest.getImageUri() + ": Post processing: " + String.valueOf(estimatedTime / 1000000) + "ms");

								/*
								 * Stuff it into the cache. Overwrites if it already exists.
								 * This is a perf hit in the process cause writing files is slow.
								 */
								if (imageRequest.doUpdateCache()) {
									Logger.v(this, imageRequest.getImageUri() + ": Pushing into cache...");
									mImageCache.put(imageRequest.getImageUri(), bitmap);
								}
								imageRequest.getRequestListener().onProgressChanged(80);

								Logger.v(this, imageRequest.getImageUri() + ": Progress complete");
								serviceResponse.data = new ImageResponse(bitmap, imageRequest.getImageUri());
								imageRequest.getRequestListener().onProgressChanged(100);
							}
							imageRequest.getRequestListener().onComplete(serviceResponse);
						}
					}
					if (Thread.interrupted()) {
						Logger.d(this, "ImageQueue thread interrupted");
						break;
					}
				}
			}
			catch (InterruptedException e) {
				/* Allow thread to exit */
			}
		}
	}

	public void stopLoaderThread() {
		/*
		 * We call this when the search activity is being destroyed.
		 */
		mImageLoaderThread.interrupt();
		mImagesQueue.mImagesToLoad.clear();
		mImageLoaderThread = new ImagesLoader();
	}
}
