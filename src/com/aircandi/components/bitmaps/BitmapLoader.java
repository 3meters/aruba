package com.aircandi.components.bitmaps;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import com.aircandi.Aircandi;
import com.aircandi.beta.BuildConfig;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.bitmaps.BitmapRequest.ImageResponse;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.RequestListener;
import com.aircandi.service.HttpService.RequestType;
import com.aircandi.service.HttpService.ResponseFormat;
import com.aircandi.service.ServiceRequest;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.ImageUtils;

@SuppressWarnings("ucd")
public class BitmapLoader {

	private final BitmapQueue	mBitmapQueue	= new BitmapQueue();
	private BitmapLoaderThread	mBitmapLoaderThread;

	public BitmapLoader() {

		/* Make the background thead low priority so it doesn't effect the UI performance. */
		mBitmapLoaderThread = new BitmapLoaderThread(mBitmapQueue);
		mBitmapLoaderThread.setPriority(Thread.MIN_PRIORITY);
		mBitmapLoaderThread.start();
	}

	// --------------------------------------------------------------------------------------------
	// Primary entry routines
	// --------------------------------------------------------------------------------------------

	public void queueBitmapRequest(BitmapRequest bitmapRequest) {

		synchronized (mBitmapQueue.mQueue) {
			/*
			 * The image requestor may have called for other images before. So there may be some
			 * old tasks in the queue. We need to discard them.
			 */
			mBitmapQueue.clean(bitmapRequest.getImageRequestor());
			mBitmapQueue.mQueue.offer(bitmapRequest);
			synchronized (mBitmapQueue.mQueue) {
				mBitmapQueue.mQueue.notifyAll();
			}
			Logger.v(this, bitmapRequest.getImageUri() + ": Queued for download");
		}
	}

	public void stopBitmapLoaderThread() {
		/*
		 * We call this when the search activity is being destroyed.
		 */
		mBitmapLoaderThread.interrupt();
		mBitmapQueue.mQueue.clear();
		mBitmapLoaderThread = new BitmapLoaderThread(mBitmapQueue);
	}

	// --------------------------------------------------------------------------------------------
	// Processing routines
	// --------------------------------------------------------------------------------------------

	public static ServiceResponse downloadAsBitmapSampled(String url, RequestListener listener) {
		/*
		 * We request a byte array for decoding because of a bug in pre 2.3 versions of android.
		 */
		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(url)
				.setRequestType(RequestType.Get)
				.setResponseFormat(ResponseFormat.Bytes)
				.setRequestListener(listener);

		final ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest, null);

		if (serviceResponse.responseCode == ResponseCode.Success) {

			final byte[] imageBytes = (byte[]) serviceResponse.data;

			String extension = "";
			final int i = url.lastIndexOf('.');
			if (i > 0) {
				extension = url.substring(i + 1);
			}

			Bitmap bitmap = null;
			if (extension.equals("gif")) {
				/*
				 * Potential memory issue: We don't have the same sampling protection as
				 * we get when decoding a jpeg or png.
				 */
				final InputStream inputStream = new ByteArrayInputStream(imageBytes);
				final GifDecoder decoder = new GifDecoder();
				decoder.read(inputStream);
				bitmap = decoder.getBitmap();
				try {
					inputStream.close();
				}
				catch (IOException e) {
					if (BuildConfig.DEBUG) {
						e.printStackTrace();
					}
				}
			}
			else {
				/* Turn byte array into bitmap that fits in our desired max size */
				Logger.v(null, url + ": " + String.valueOf(imageBytes.length) + " bytes received");
				bitmap = BitmapManager.getInstance().bitmapForByteArraySampled(imageBytes, null, null);
			}

			if (bitmap == null) {
				Logger.w(null, url + ": stream could not be decoded to a bitmap");
				serviceResponse.responseCode = ResponseCode.Failed;
				serviceResponse.exception = HttpService.makeHttpServiceException(null, null, new IllegalStateException(
						"Stream could not be decoded to a bitmap: " + url));
			}
			else {
				serviceResponse.data = bitmap;
			}
		}
		return serviceResponse;
	}

	private static ServiceResponse downloadAsByteArray(String url, RequestListener listener) {
		/*
		 * We request a byte array for decoding because of a bug in pre 2.3 versions of android.
		 */
		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(url)
				.setRequestType(RequestType.Get)
				.setResponseFormat(ResponseFormat.Bytes)
				.setRequestListener(listener);

		final ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest, null);
		return serviceResponse;
	}

	// --------------------------------------------------------------------------------------------
	// Inner classes
	// --------------------------------------------------------------------------------------------

	private class BitmapLoaderThread extends Thread {

		private final BitmapQueue	mBitmapQueue;

		public BitmapLoaderThread(BitmapQueue bitmapQueue) {
			super();
			mBitmapQueue = bitmapQueue;
		}

		@Override
		public void run() {

			Thread.currentThread().setName("BitmapLoader");

			while (true) {
				consume();
				try {
					synchronized (mBitmapQueue.mQueue) {
						/* Thread waits until there are any images to load in the queue */
						mBitmapQueue.mQueue.wait();
					}
				}
				catch (InterruptedException e) {
					Logger.v(this, "Loader thread interrupted");
					/* Allow thread to exit */
				}
			}
		}

		private void consume() {

			while (!mBitmapQueue.mQueue.isEmpty()) {

				final BitmapRequest imageRequest = mBitmapQueue.mQueue.poll();

				if (imageRequest != null) {

					/* Make sure this is still a valid request */
					ServiceResponse serviceResponse = new ServiceResponse();
					if (imageRequest.getImageView() == null || imageRequest.getImageView().getTag().equals(imageRequest.getImageUri())) {

						Logger.v(BitmapLoader.this, imageRequest.getImageUri() + ": Download started...");
						Bitmap bitmap = null;

						long startTime = System.nanoTime();
						float estimatedTime = System.nanoTime();
						/*
						 * Gets bitmap at native size and downsamples if necessary to stay within the max
						 * size in
						 * memory.
						 */
						serviceResponse = downloadAsByteArray(imageRequest.getImageUri(), new RequestListener() {

							@Override
							public void onProgressChanged(int progress) {
								if (imageRequest.getRequestListener() != null) {
									if (progress > 0) {
										if (imageRequest.getRequestListener() != null) {
											imageRequest.getRequestListener().onProgressChanged((int) (70 * ((float) progress / 100f)));
										}
									}
								}
							}
						});

						if (serviceResponse.responseCode == ResponseCode.Success) {

							if (imageRequest.getImageView() == null || imageRequest.getImageView().getTag().equals(imageRequest.getImageUri())) {

								Logger.v(BitmapLoader.this,
										imageRequest.getImageUri() + ": Download finished: " + String.valueOf(estimatedTime / 1000000)
												+ "ms");

								estimatedTime = System.nanoTime() - startTime;
								startTime = System.nanoTime();

								estimatedTime = System.nanoTime() - startTime;
								startTime = System.nanoTime();
								Logger.v(BitmapLoader.this,
										imageRequest.getImageUri() + ": Post processing: " + String.valueOf(estimatedTime / 1000000)
												+ "ms");
								/*
								 * Stuff it into the cache. Overwrites if it already exists. This is a perf hit in
								 * the process because writing files is slow.
								 * 
								 * We aren't doing anything to shrink the raw size of the image before storing it to
								 * disk. We also aren't handling the case where the image format is gif.
								 */
								Logger.v(BitmapLoader.this, imageRequest.getImageUri() + ": Pushing into cache...");
								bitmap = BitmapManager.getInstance().putImageBytes(imageRequest.getImageUri(), (byte[]) serviceResponse.data,
										imageRequest.getImageSize());

								/* Update progress */
								if (imageRequest.getRequestListener() != null) {
									imageRequest.getRequestListener().onProgressChanged(80);
								}

								Logger.v(BitmapLoader.this, imageRequest.getImageUri() + ": Progress complete");
								serviceResponse.data = new ImageResponse(bitmap, imageRequest.getImageUri());

								if (imageRequest.getRequestListener() != null) {
									imageRequest.getRequestListener().onProgressChanged(100);
								}

								if (imageRequest.getRequestListener() != null) {
									imageRequest.getRequestListener().onComplete(serviceResponse);
								}

								if (imageRequest.getImageView() != null) {
									if (imageRequest.getImageView().getTag().equals(imageRequest.getImageUri())) {

										final BitmapDrawable bitmapDrawable = new BitmapDrawable(Aircandi.applicationContext.getResources(), bitmap);
										/* Put this on the main thread */
										Aircandi.mainThreadHandler.post(new Runnable() {

											@Override
											public void run() {
												ImageUtils.showDrawableInImageView(bitmapDrawable, imageRequest.getImageView(), true,
														AnimUtils.fadeInMedium());
											}
										});
									}
								}
							}
						}

					}
					else {
						if (imageRequest.getRequestListener() != null) {
							imageRequest.getRequestListener().onComplete(serviceResponse);
						}
					}
				}
				if (Thread.interrupted()) {
					Logger.d(this, "ImageQueue thread interrupted");
					break;
				}

			}
		}
	}

	private class BitmapQueue {

		private final Queue<BitmapRequest>	mQueue	= new ConcurrentLinkedQueue<BitmapRequest>();

		/* Removes all instances of imageRequest associated with the imageRequestor */
		public void clean(Object bitmapRequestor) {

			synchronized (mQueue) {
				if (bitmapRequestor != null && !mQueue.isEmpty()) {
					Iterator<BitmapRequest> iter = mQueue.iterator();
					while (iter.hasNext()) {
						BitmapRequest request = iter.next();
						Object imageRequestor = request.getImageRequestor();
						if (imageRequestor != null && imageRequestor.equals(bitmapRequestor)) {
							mQueue.remove(request);
						}
					}
				}
			}
		}
	}
}
