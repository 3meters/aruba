package com.proxibase.aircandi.components;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Picture;
import android.os.AsyncTask;
import android.os.Build;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebView.PictureListener;
import android.webkit.WebViewClient;

import com.aircandi.service.ProxibaseServiceException;
import com.aircandi.service.ServiceRequest;
import com.aircandi.service.ProxibaseService.RequestListener;
import com.aircandi.service.ProxibaseService.RequestType;
import com.aircandi.service.ProxibaseService.ResponseFormat;
import com.aircandi.service.ProxibaseServiceException.ErrorCode;
import com.aircandi.service.ProxibaseServiceException.ErrorType;
import com.aircandi.service.objects.Entity.ImageFormat;
import com.proxibase.aircandi.Aircandi;
import com.proxibase.aircandi.components.ImageRequest.ImageResponse;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ResponseCodeDetail;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.core.CandiConstants;

@SuppressWarnings("deprecation")
public class ImageLoader {

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

	@SuppressLint("NewApi")
	public void fetchImage(final ImageRequest imageRequest, boolean asyncOk) {

		if (imageRequest.getRequestListener() == null) {
			throw new IllegalArgumentException("imageRequest.imageReadyListener is required");
		}

		if (imageRequest.getImageUri().toLowerCase().startsWith("resource:")) {

			ServiceResponse serviceResponse = new ServiceResponse();
			
			String rawResourceName = imageRequest.getImageUri().substring(imageRequest.getImageUri().indexOf("resource:") + 9);
			String resolvedResourceName = ImageManager.getInstance().resolveResourceName(rawResourceName);

			if (imageRequest.doSearchCache()) {
				Bitmap bitmap = ImageManager.getInstance().getImage(resolvedResourceName);
				if (bitmap != null) {
					if (imageRequest.getScaleToWidth() != CandiConstants.IMAGE_WIDTH_ORIGINAL && imageRequest.getScaleToWidth() != bitmap.getWidth()) {
						/*
						 * We might have cached a large version of an image so we need to make sure we honor the image
						 * request specifications.
						 */
						bitmap = ImageUtils.scaleAndCropBitmap(bitmap, imageRequest);
					}
					serviceResponse.data = new ImageResponse(bitmap, imageRequest.getImageUri());
					imageRequest.getRequestListener().onComplete(serviceResponse);
					return;
				}
			}

			int resourceId = ImageManager.getInstance().getActivity().getResources().getIdentifier(resolvedResourceName, "drawable", "com.proxibase.aircandi");
			Bitmap bitmap = ImageManager.getInstance().loadBitmapFromResources(resourceId);

			if (bitmap != null) {
				if (imageRequest.getScaleToWidth() != CandiConstants.IMAGE_WIDTH_ORIGINAL && imageRequest.getScaleToWidth() != bitmap.getWidth()) {
					/*
					 * We might have cached a large version of an image so we need to make sure we honor the image
					 * request specifications.
					 */
					bitmap = ImageUtils.scaleAndCropBitmap(bitmap, imageRequest);
				}

				/* We put resource images into the cache so they are consistent */
				if (imageRequest.doUpdateCache()) {
					Logger.v(this, resolvedResourceName + ": Pushing into cache...");
					ImageManager.getInstance().putImage(resolvedResourceName, bitmap);
				}

				serviceResponse.data = new ImageResponse(bitmap, imageRequest.getImageUri());
				imageRequest.getRequestListener().onComplete(serviceResponse);
				return;
			}
			else {
				throw new IllegalStateException("Bitmap resource is null: " + resolvedResourceName);
			}
		}
		else {

			if (imageRequest.doSearchCache()) {
				/*
				 * We use async for WebImageView for faster performance in lists. We don't use
				 * it for CandiViews because of state conflicts and dependencies plus the rendering
				 * process is already async.
				 */
				if (asyncOk) {

					AsyncTask task = new AsyncTask() {

						@Override
						protected Object doInBackground(Object... params) {
							ServiceResponse serviceResponse = getCachedImage(imageRequest);
							return serviceResponse;
						}

						@Override
						protected void onPostExecute(Object result) {
							ServiceResponse serviceResponse = (ServiceResponse) result;
							if (serviceResponse.responseCode == ResponseCode.Success) {
								imageRequest.getRequestListener().onComplete(serviceResponse);
							}
							else {
								queueImage(imageRequest);
							}
						}

					};

					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
						task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
					}
					else {
						task.execute();
					}
				}
				else {
					ServiceResponse serviceResponse = getCachedImage(imageRequest);

					if (serviceResponse.responseCode == ResponseCode.Success) {
						imageRequest.getRequestListener().onComplete(serviceResponse);
					}
					else {
						queueImage(imageRequest);
					}
				}
			}
			else {
				queueImage(imageRequest);
			}
		}
	}
	
	private ServiceResponse getCachedImage(ImageRequest imageRequest) {
		
		ServiceResponse serviceResponse = new ServiceResponse();
		Bitmap bitmap = ImageManager.getInstance().getImage(imageRequest.getImageUri());
		if (bitmap != null) {
			Logger.v(this, "Image request satisfied from cache: " + imageRequest.getImageUri());
			if (imageRequest.getScaleToWidth() != CandiConstants.IMAGE_WIDTH_ORIGINAL
					&& imageRequest.getScaleToWidth() != bitmap.getWidth()) {
				/*
				 * We might have cached a large version of an image so we need to make sure we honor
				 * the image request specifications.
				 */
				Logger.v(this, "Image from cache needs to be scaled: " + imageRequest.getImageUri());
				bitmap = ImageUtils.scaleAndCropBitmap(bitmap, imageRequest);
			}
			serviceResponse.data = new ImageResponse(bitmap, imageRequest.getImageUri());
		}
		else {
			serviceResponse.responseCode = ResponseCode.Failed;
		}
		return serviceResponse;
	}

	// --------------------------------------------------------------------------------------------
	// Processing routines
	// --------------------------------------------------------------------------------------------

	private void queueImage(ImageRequest imageRequest) {
		/*
		 * The image requestor may have called for other images before. So there may be some old tasks in the queue. We
		 * need to discard them.
		 */
		mImagesQueue.Clean(imageRequest.getImageRequestor());
		synchronized (mImagesQueue.mImagesToLoad) {
			Logger.v(this, imageRequest.getImageUri() + ": Queued for download");
			mImagesQueue.mImagesToLoad.add(imageRequest);
			mImagesQueue.mImagesToLoad.notifyAll();
		}

		/* Start thread if it's not started yet */
		if (mImageLoaderThread.getState() == Thread.State.NEW && !mImageLoaderThread.isAlive()) {
			mImageLoaderThread.start();
		}
	}

	private static ServiceResponse getBitmap(String url, ImageRequest imageRequest, RequestListener listener) {
		/*
		 * We request a byte array for decoding because of a bug in pre 2.3 versions of android.
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
			Bitmap bitmap = ImageManager.getInstance().bitmapForByteArraySampled(imageBytes, imageRequest, CandiConstants.IMAGE_MEMORY_BYTES_MAX);

			if (bitmap == null) {
				Logger.w(null, url + ": stream could not be decoded to a bitmap");
				serviceResponse.responseCode = ResponseCode.Failed;
				serviceResponse.responseCodeDetail = ResponseCodeDetail.IllegalStateException;
				String message = "Stream could not be decoded to a bitmap: " + url;
				serviceResponse.exception = new ProxibaseServiceException(message, ErrorType.Client, ErrorCode.IllegalStateException,
						new IllegalStateException("Stream could not be decoded to a bitmap: " + url));
			}
			else {
				serviceResponse.data = bitmap;
			}
		}
		return serviceResponse;
	}

	private void getWebPageAsBitmap(final String originalUri, final ImageRequest imageRequest, final RequestListener listener) {

		final ServiceResponse serviceResponse = new ServiceResponse();
		final AtomicInteger pictureCount = new AtomicInteger(0);
		final AtomicBoolean ready = new AtomicBoolean(false);

		/*
		 * Setting WideViewPort to false will cause html text to layout to try and fit the sizing of the webview though
		 * our screen capture will still be cover the full page width. Ju Setting to true will handle text nicely but
		 * will show the full width of the webview even if the page content only fills a portion of it. We might have to
		 * have a property to control the desired result when using html for the tile display image. Makes the Webview
		 * have a normal viewport (such as a normal desktop browser), while when false the webview will have a viewport
		 * constrained to it's own dimensions (so if the webview is 50px*50px the viewport will be the same size)
		 */

		Aircandi.applicationHandler.post(new Runnable() {

			@SuppressLint("SetJavaScriptEnabled")
			@Override
			public void run() {

				mWebView.getSettings().setUseWideViewPort(true);
				if (imageRequest.getLinkZoom()) {
					mWebView.getSettings().setUseWideViewPort(false);
				}

				mWebView.getSettings().setUserAgentString(CandiConstants.USER_AGENT_MOBILE);
				/*
				 * Using setJavaScriptEnabled can introduce XSS vulnerabilities.
				 */
				mWebView.getSettings().setJavaScriptEnabled(imageRequest.getLinkJavascriptEnabled());
				mWebView.getSettings().setLoadWithOverviewMode(true);
				mWebView.getSettings().setDomStorageEnabled(true);
				mWebView.refreshDrawableState();

				mWebView.setWebViewClient(new WebViewClient() {

					@Override
					public void onLoadResource(WebView view, String url) {
						super.onLoadResource(view, url);
					}

					@Override
					public void onPageStarted(WebView view, String url, Bitmap favicon) {
						super.onPageStarted(view, url, favicon);
						Logger.v(ImageLoader.this, "Page started: " + url);
					}

					@Override
					public void onPageFinished(WebView view, String url) {
						super.onPageFinished(view, url);
						Logger.v(ImageLoader.this, "Page finished: " + url);
						ready.set(true);
					}

					public boolean shouldOverrideUrlLoading(WebView view, String url) {
						view.loadUrl(url);
						Logger.v(ImageLoader.this, "Url intercepted and loaded: " + url);
						return false;
					}

				});

				mWebView.setWebChromeClient(new WebChromeClient() {

					@Override
					public void onProgressChanged(WebView view, int progress) {
						listener.onProgressChanged(progress);

						Logger.v(ImageLoader.this, "Progress: " + String.valueOf(progress) + " :" + view.getUrl());
						if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
							if (view.getUrl() != null && progress >= 100) {
								Logger.v(ImageLoader.this, "Capturing screenshot: " + view.getUrl());

								Aircandi.applicationHandler.postDelayed(new Runnable() {

									@Override
									public void run() {
										Bitmap bitmap = captureWebView(mWebView.capturePicture());
										serviceResponse.data = bitmap;
										listener.onComplete(serviceResponse);
									}

								}, 1000);

							}
						}
					}
				});

				/*
				 * Using picture listener works best on older versions.
				 */
				if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
					mWebView.setPictureListener(new PictureListener() {

						@Override
						public void onNewPicture(WebView view, Picture picture) {
							Logger.v(this, "WebView onNewPicture called");
							/*
							 * Sometimes the first call isn't finished with layout but the second one is correct.
							 * How can we tell the difference?
							 */
							if (ready.get()) {
								pictureCount.getAndIncrement();
								Bitmap bitmap = captureWebView(picture);
								serviceResponse.data = bitmap;
								listener.onComplete(serviceResponse);

								/* We only allow a maximum of two picture calls */
								if (pictureCount.get() >= 2) {
									mWebView.setPictureListener(null);
								}
							}
						}
					});
				}
				mWebView.loadUrl(originalUri);
			}
		});
	}

	private Bitmap captureWebView(Picture picture) {

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
		return bitmap;
	}

	// --------------------------------------------------------------------------------------------
	// Setter/Getter routines
	// --------------------------------------------------------------------------------------------

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
							Logger.v(ImageLoader.this, "Starting html image processing: " + imageRequest.getImageUri());
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
										bitmap = ImageUtils.scaleAndCropBitmap(bitmap, imageRequest);

										/* Stuff it into the cache. Overwrites if it already exists. */
										ImageManager.getInstance().putImage(imageRequest.getImageUri(), bitmap, CompressFormat.JPEG);

										Logger.v(ImageLoader.this, "Html image processed: " + imageRequest.getImageUri());
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
							Logger.v(ImageLoader.this, imageRequest.getImageUri() + ": Download started...");

							/*
							 * Gets bitmap at native size and downsamples if necessary to stay within the max size in
							 * memory.
							 */
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
								Logger.v(ImageLoader.this, imageRequest.getImageUri() + ": Download finished: " + String.valueOf(estimatedTime / 1000000)
										+ "ms");

								estimatedTime = System.nanoTime() - startTime;
								startTime = System.nanoTime();

								/* Perform requested post processing */
								if (imageRequest.getScaleToWidth() != CandiConstants.IMAGE_WIDTH_ORIGINAL) {
									bitmap = ImageUtils.scaleAndCropBitmap(bitmap, imageRequest);
								}

								estimatedTime = System.nanoTime() - startTime;
								startTime = System.nanoTime();
								Logger.v(ImageLoader.this, imageRequest.getImageUri() + ": Post processing: " + String.valueOf(estimatedTime / 1000000) + "ms");

								/*
								 * Stuff it into the cache. Overwrites if it already exists. This is a perf hit in the
								 * process because writing files is slow.
								 */
								if (imageRequest.doUpdateCache()) {
									Logger.v(ImageLoader.this, imageRequest.getImageUri() + ": Pushing into cache...");
									ImageManager.getInstance().putImage(imageRequest.getImageUri(), bitmap);
								}
								imageRequest.getRequestListener().onProgressChanged(80);

								Logger.v(ImageLoader.this, imageRequest.getImageUri() + ": Progress complete");
								serviceResponse.data = new ImageResponse(bitmap, imageRequest.getImageUri());
								imageRequest.getRequestListener().onProgressChanged(100);
							}
							else if (serviceResponse.responseCodeDetail == ResponseCodeDetail.IllegalStateException) {
								/*
								 * Data couldn't be successfully decoded into a bitmap so substitute
								 * the broken image placeholder
								 */
								imageRequest.setImageUri("resource:placeholder_logo");
								fetchImage(imageRequest, false);
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
