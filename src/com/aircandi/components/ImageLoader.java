package com.aircandi.components;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.LinkedList;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;

import com.aircandi.components.ImageRequest.ImageResponse;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.core.CandiConstants;
import com.aircandi.service.ProxibaseService;
import com.aircandi.service.ProxibaseService.RequestListener;
import com.aircandi.service.ProxibaseService.RequestType;
import com.aircandi.service.ProxibaseService.ResponseFormat;
import com.aircandi.service.ProxibaseServiceException.ErrorCode;
import com.aircandi.service.ServiceRequest;
import com.aircandi.service.objects.Entity.ImageFormat;
import com.aircandi.utilities.ImageUtils;

public class ImageLoader {

	private ImagesQueue		mImagesQueue		= new ImagesQueue();
	private ImagesLoader	mImageLoaderThread	= new ImagesLoader();

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

			int resourceId = ImageManager.getInstance().getActivity().getResources().getIdentifier(resolvedResourceName, "drawable", "com.aircandi");
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
						task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
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

	public ServiceResponse getCachedImage(ImageRequest imageRequest) {

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
		ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(url)
				.setRequestType(RequestType.Get)
				.setResponseFormat(ResponseFormat.Bytes)
				.setRequestListener(listener);

		ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);

		if (serviceResponse.responseCode == ResponseCode.Success) {

			byte[] imageBytes = (byte[]) serviceResponse.data;

			String extension = "";
			int i = url.lastIndexOf('.');
			if (i > 0) {
				extension = url.substring(i + 1);
			}

			Bitmap bitmap = null;
			if (extension.equals("gif")) {
				/*
				 * Potential memory issue: We don't have the same sampling protection as
				 * we get when decoding a jpeg or png.
				 */
				InputStream inputStream = new ByteArrayInputStream(imageBytes);
				GifDecoder decoder = new GifDecoder();
				decoder.read(inputStream);
				bitmap = decoder.getBitmap();
			}
			else {
				/* Turn byte array into bitmap that fits in our desired max size */
				Logger.v(null, url + ": " + String.valueOf(imageBytes.length) + " bytes received");
				bitmap = ImageManager.getInstance().bitmapForByteArraySampled(imageBytes, imageRequest, CandiConstants.IMAGE_MEMORY_BYTES_MAX);
			}

			if (bitmap == null) {
				Logger.w(null, url + ": stream could not be decoded to a bitmap");
				serviceResponse.responseCode = ResponseCode.Failed;
				serviceResponse.exception = ProxibaseService.makeProxibaseServiceException(null, new IllegalStateException(
						"Stream could not be decoded to a bitmap: " + url));
			}
			else {
				serviceResponse.data = bitmap;
			}
		}
		return serviceResponse;
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
							imageRequest = mImagesQueue.mImagesToLoad.poll();
						}

						Bitmap bitmap = null;
						ServiceResponse serviceResponse = new ServiceResponse();
						if (imageRequest.getImageFormat() == ImageFormat.Binary) {

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
							else if (serviceResponse.exception.getErrorCode() == ErrorCode.IllegalStateException) {
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
