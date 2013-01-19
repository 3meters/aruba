package com.aircandi.components.bitmaps;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.LinkedList;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import com.aircandi.Aircandi;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.bitmaps.BitmapRequest.ImageResponse;
import com.aircandi.service.ProxibaseService;
import com.aircandi.service.ProxibaseService.RequestListener;
import com.aircandi.service.ProxibaseService.RequestType;
import com.aircandi.service.ProxibaseService.ResponseFormat;
import com.aircandi.service.ServiceRequest;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.ImageUtils;

public class BitmapLoader {

	private BitmapQueue			mBitmapQueue		= new BitmapQueue();
	private BitmapLoaderThread	mBitmapLoaderThread	= new BitmapLoaderThread();

	public BitmapLoader() {

		/* Make the background thead low priority so it doesn't effect the UI performance. */
		mBitmapLoaderThread.setPriority(Thread.MIN_PRIORITY);
	}

	// --------------------------------------------------------------------------------------------
	// Primary entry routines
	// --------------------------------------------------------------------------------------------

	public void queueBitmapRequest(BitmapRequest bitmapRequest) {
		/*
		 * The image requestor may have called for other images before. So there may be some old tasks in the queue. We
		 * need to discard them.
		 */
		mBitmapQueue.clean(bitmapRequest.getImageRequestor());
		synchronized (mBitmapQueue.mBitmapsToLoad) {
			Logger.v(this, bitmapRequest.getImageUri() + ": Queued for download");
			mBitmapQueue.mBitmapsToLoad.add(bitmapRequest);
			mBitmapQueue.mBitmapsToLoad.notifyAll();
		}

		/* Start thread if it's not started yet */
		if (mBitmapLoaderThread.getState() == Thread.State.NEW && !mBitmapLoaderThread.isAlive()) {
			mBitmapLoaderThread.start();
		}
	}

	public void stopBitmapLoaderThread() {
		/*
		 * We call this when the search activity is being destroyed.
		 */
		mBitmapLoaderThread.interrupt();
		mBitmapQueue.mBitmapsToLoad.clear();
		mBitmapLoaderThread = new BitmapLoaderThread();
	}

	// --------------------------------------------------------------------------------------------
	// Processing routines
	// --------------------------------------------------------------------------------------------

	public static ServiceResponse downloadAsBitmapSampled(String url, RequestListener listener) {
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
				bitmap = BitmapManager.getInstance().bitmapForByteArraySampled(imageBytes, null, null);
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

	public static ServiceResponse downloadAsByteArray(String url, RequestListener listener) {
		/*
		 * We request a byte array for decoding because of a bug in pre 2.3 versions of android.
		 */
		ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(url)
				.setRequestType(RequestType.Get)
				.setResponseFormat(ResponseFormat.Bytes)
				.setRequestListener(listener);

		ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);
		return serviceResponse;
	}

	// --------------------------------------------------------------------------------------------
	// Inner classes
	// --------------------------------------------------------------------------------------------

	private class BitmapLoaderThread extends Thread {

		public void run() {

			Thread.currentThread().setName("BitmapLoader");

			try {

				while (true) {

					/* Thread waits until there are any images to load in the queue */
					if (mBitmapQueue.mBitmapsToLoad.size() == 0) {
						synchronized (mBitmapQueue.mBitmapsToLoad) {
							mBitmapQueue.mBitmapsToLoad.wait();
							Logger.v(this, "ImageQueue thread is waiting");
						}
					}

					if (mBitmapQueue.mBitmapsToLoad.size() != 0) {

						Logger.v(this, "ImageQueue has " + String.valueOf(mBitmapQueue.mBitmapsToLoad.size()) + " images requests pending");
						final BitmapRequest imageRequest;
						synchronized (mBitmapQueue.mBitmapsToLoad) {
							imageRequest = mBitmapQueue.mBitmapsToLoad.poll();
						}

						/* Make sure this is still a valid request */
						if (imageRequest.getImageView() == null || imageRequest.getImageView().getTag().equals(imageRequest.getImageUri())) {
							/*
							 * Start a new thread so we can do the actual image fetching in parallel
							 * like DrawableManager does.
							 */
							Logger.v(BitmapLoader.this, imageRequest.getImageUri() + ": Download started...");
							Bitmap bitmap = null;
							ServiceResponse serviceResponse = new ServiceResponse();

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
									 * Stuff it into the cache. Overwrites if it already exists. This is a perf
									 * hit in
									 * the
									 * process because writing files is slow.
									 * 
									 * We aren't doing anything to shrink the raw size of the image before
									 * storing it to
									 * disk. We also aren't
									 * handling the case where the image format is gif.
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
							else {
								if (imageRequest.getRequestListener() != null) {
									imageRequest.getRequestListener().onComplete(serviceResponse);
								}
								else {
//									imageRequest.setImageUri("resource:image_broken");
//									BitmapManager.getInstance().fetchBitmap(imageRequest);
								}
							}
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

	private class BitmapQueue {

		private LinkedList<BitmapRequest>	mBitmapsToLoad	= new LinkedList<BitmapRequest>();

		/* Removes all instances of imageRequest associated with the imageRequestor */
		public void clean(Object bitmapRequestor) {
			for (int j = 0; j < mBitmapsToLoad.size();) {
				if (mBitmapsToLoad.get(j).getImageRequestor() == bitmapRequestor) {
					mBitmapsToLoad.remove(j);
				}
				else {
					++j;
				}
			}
		}
	}

}
