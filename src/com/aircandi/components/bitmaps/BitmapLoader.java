package com.aircandi.components.bitmaps;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import com.aircandi.Aircandi;
import com.aircandi.BuildConfig;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.bitmaps.BitmapRequest.BitmapResponse;
import com.aircandi.service.RequestListener;
import com.aircandi.service.RequestType;
import com.aircandi.service.ResponseFormat;
import com.aircandi.service.ServiceRequest;
import com.aircandi.service.ServiceResponse;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.UI;

@SuppressWarnings("ucd")
public class BitmapLoader {

	private final BitmapQueue			mBitmapQueue	= new BitmapQueue();
	@SuppressWarnings("unused")
	private BitmapLoaderThread			mBitmapLoaderThread;
	private List<BitmapLoaderThread>	mThreads		= Collections.synchronizedList(new ArrayList<BitmapLoaderThread>());

	public BitmapLoader() {

		/*
		 * This is a very basic thread pool implementation. We are pre-starting three worker threads
		 * to handle image downloads and processing. This could be much better using dynamic
		 * thread allocation with ThreadPoolExecutor.
		 * 
		 * see-> http://http://developer.android.com/training/multiple-threads/index.html
		 */

		mThreads.add(new BitmapLoaderThread(mBitmapQueue, Thread.MIN_PRIORITY));
		mThreads.add(new BitmapLoaderThread(mBitmapQueue, Thread.MIN_PRIORITY));
		mThreads.add(new BitmapLoaderThread(mBitmapQueue, Thread.MIN_PRIORITY));

		for (Thread thread : mThreads) {
			thread.start();
		}
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
			mBitmapQueue.clean(bitmapRequest.getBitmapRequestor());
			mBitmapQueue.mQueue.offer(bitmapRequest);
			synchronized (mBitmapQueue.mQueue) {
				mBitmapQueue.mQueue.notifyAll();
			}
			Logger.v(this, bitmapRequest.getBitmapUri() + ": Queued for download");
		}
	}

	public void stopBitmapLoaderThread() {
		for (Thread thread : mThreads) {
			thread.interrupt();
		}
		mBitmapQueue.mQueue.clear();
	}

	// --------------------------------------------------------------------------------------------
	// Processing routines
	// --------------------------------------------------------------------------------------------

	public static ServiceResponse downloadAsByteArraySampled(String url, RequestListener listener) {
		/*
		 * We request a byte array for decoding because of a bug in pre 2.3 versions of android.
		 */
		final ServiceResponse serviceResponse = downloadAsByteArray(url, listener);

		if (serviceResponse.responseCode == ResponseCode.SUCCESS) {

			if (serviceResponse.contentType != null && serviceResponse.contentType.equals("image/gif")) {
				/*
				 * Potential memory issue: We don't have the same sampling protection as
				 * we get when decoding a jpeg or png.
				 */
				final InputStream inputStream = new ByteArrayInputStream((byte[]) serviceResponse.data);
				final GifDecoder decoder = new GifDecoder();
				decoder.read(inputStream);
				Bitmap bitmap = decoder.getBitmap();
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream); // should be lossless.
				serviceResponse.data = outputStream.toByteArray();

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
				serviceResponse.data = BitmapManager.getInstance().byteArraySampled((byte[]) serviceResponse.data, null, null, serviceResponse);
			}

			if (serviceResponse.data == null) {
				return new ServiceResponse(ResponseCode.FAILED, null, new IllegalStateException("Bytes could not be decoded to a bitmap: " + url));
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
				.setRequestType(RequestType.GET)
				.setResponseFormat(ResponseFormat.BYTES)
				.setRequestListener(listener);

		final ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest, null);
		return serviceResponse;
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------

	private class BitmapLoaderThread extends Thread {

		private final BitmapQueue	mBitmapQueue;

		public BitmapLoaderThread(BitmapQueue bitmapQueue, int priority) {
			super();
			mBitmapQueue = bitmapQueue;
			setPriority(priority);
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

				final BitmapRequest bitmapRequest = mBitmapQueue.mQueue.poll();

				if (bitmapRequest != null) {

					/* Make sure this is still a valid request */
					ServiceResponse serviceResponse = new ServiceResponse();

					if (bitmapRequest.getImageView() == null || bitmapRequest.getImageView().getTag() == null
							|| bitmapRequest.getImageView().getTag().equals(bitmapRequest.getBitmapUri())) {

						Logger.v(BitmapLoader.this, bitmapRequest.getBitmapUri() + ": Download started...");
						Bitmap bitmap = null;

						long startTime = System.nanoTime();
						/*
						 * Gets bitmap at native size and downsamples if necessary to stay within the max size in
						 * memory.
						 */
						serviceResponse = downloadAsByteArraySampled(bitmapRequest.getBitmapUri(), new RequestListener() {

							@Override
							public void onProgressChanged(int progress) {
								if (bitmapRequest.getRequestListener() != null) {
									if (progress > 0) {
										if (bitmapRequest.getRequestListener() != null) {
											bitmapRequest.getRequestListener().onProgressChanged((int) (70 * ((float) progress / 100f)));
										}
									}
								}
							}
						});

						float finishTime = System.nanoTime();
						if (serviceResponse.responseCode == ResponseCode.SUCCESS) {

							if (bitmapRequest.getImageView() == null || bitmapRequest.getImageView().getTag() == null
									|| bitmapRequest.getImageView().getTag().equals(bitmapRequest.getBitmapUri())) {

								String imageUri = bitmapRequest.getBitmapUri();

								Logger.v(BitmapLoader.this, imageUri + ": Download finished: " + String.valueOf((int) ((finishTime - startTime) / 1000000))
										+ "ms");
								Logger.v(BitmapLoader.this, "Download: encoding = " + serviceResponse.contentEncoding
										+ ", type = " + serviceResponse.contentType
										+ ", length = " + serviceResponse.contentLength
										+ ", length decoded = " + serviceResponse.contentLength
										);

								/*
								 * Stuff it into the cache. Overwrites if it already exists. This is a perf hit in
								 * the process because writing files is slow.
								 * 
								 * We aren't doing anything to shrink the raw size of the image before storing it to
								 * disk. We also aren't handling the case where the image format is gif.
								 */
								Logger.v(BitmapLoader.this, imageUri + ": Pushing into cache...");
								bitmap = BitmapManager.getInstance().putImageBytes(bitmapRequest.getBitmapUri(), (byte[]) serviceResponse.data,
										bitmapRequest.getBitmapSize());

								/* Update progress */
								if (bitmapRequest.getRequestListener() != null) {
									bitmapRequest.getRequestListener().onProgressChanged(80);
								}

								Logger.v(BitmapLoader.this, bitmapRequest.getBitmapUri() + ": Progress complete");
								serviceResponse.data = new BitmapResponse(bitmap, bitmapRequest.getBitmapUri());

								if (bitmapRequest.getRequestListener() != null) {
									bitmapRequest.getRequestListener().onProgressChanged(100);
								}

								if (bitmapRequest.getRequestListener() != null) {
									bitmapRequest.getRequestListener().onComplete(serviceResponse);
								}

								if (bitmapRequest.getImageView() != null) {
									if (bitmapRequest.getImageView().getTag() == null
											|| bitmapRequest.getImageView().getTag().equals(bitmapRequest.getBitmapUri())) {
										final BitmapDrawable bitmapDrawable = new BitmapDrawable(Aircandi.applicationContext.getResources(), bitmap);
										UI.showDrawableInImageView(bitmapDrawable, bitmapRequest.getImageView(), true, Animate.fadeInMedium());
									}
								}
							}
						}
						else {
							if (bitmapRequest.getRequestListener() != null) {
								bitmapRequest.getRequestListener().onError(serviceResponse);
							}
						}
					}
					else {
						if (bitmapRequest.getRequestListener() != null) {
							bitmapRequest.getRequestListener().onComplete(serviceResponse);
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

		/* Removes all instances of bitmapRequest associated with the bitmapRequestor */
		public void clean(Object bitmapRequestor) {

			synchronized (mQueue) {
				if (bitmapRequestor != null && !mQueue.isEmpty()) {
					Iterator<BitmapRequest> iter = mQueue.iterator();
					while (iter.hasNext()) {
						BitmapRequest request = iter.next();
						Object queuedBitmapRequestor = request.getBitmapRequestor();
						if (queuedBitmapRequestor != null && queuedBitmapRequestor.equals(bitmapRequestor)) {
							mQueue.remove(request);
						}
					}
				}
			}
		}
	}
}
