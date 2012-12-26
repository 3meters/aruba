package com.aircandi.components.bitmaps;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.app.ActivityManager;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.MediaStore.Images;
import android.support.v4.util.LruCache;
import android.util.FloatMath;
import android.util.TypedValue;
import android.widget.ImageView;

import com.aircandi.Aircandi;
import com.aircandi.CandiConstants;
import com.aircandi.components.Exceptions;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.bitmaps.BitmapRequest.ImageResponse;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.ImageUtils;
import com.aircandi.utilities.MiscUtils;

public class BitmapManager {

	private static BitmapManager		singletonObject;
	private DiskLruImageCache			mDiskLruCache;
	private final Object				mDiskCacheLock		= new Object();
	private boolean						mDiskCacheStarting	= true;
	private static final int			DISK_CACHE_SIZE		= 1024 * 1024 * 10; // 10MB
	private static final String			DISK_CACHE_SUBDIR	= "bitmaps";
	private BitmapLoader				mBitmapLoader;

	private LruCache<String, Bitmap>	mMemoryCache;

	public static synchronized BitmapManager getInstance() {
		if (singletonObject == null) {
			singletonObject = new BitmapManager();
		}
		return singletonObject;
	}

	private BitmapManager() {
		mBitmapLoader = new BitmapLoader();
		/*
		 * Get memory class of this device, exceeding this amount will throw an
		 * OutOfMemory exception.
		 */
		final int memClass = ((ActivityManager) Aircandi.applicationContext.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();

		/* Use 1/4th of the available memory for this memory cache. */
		final int cacheSize = 1024 * 1024 * memClass / 4;

		mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
			@Override
			protected int sizeOf(String key, Bitmap bitmap) {
				/* The cache size will be measured in bytes rather than number of items. */
				return bitmap.getByteCount();
			}
		};

		/* Initialize disk cache on background thread */
		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				synchronized (mDiskCacheLock) {
					mDiskLruCache = new DiskLruImageCache(Aircandi.applicationContext, DISK_CACHE_SUBDIR, DISK_CACHE_SIZE, CompressFormat.JPEG, 70);
					mDiskCacheStarting = false; // Finished initialization
					mDiskCacheLock.notifyAll(); // Wake any waiting threads
				}
				return null;
			}
		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// Public cache routines
	// --------------------------------------------------------------------------------------------

	public void fetchBitmap(final BitmapRequest imageRequest) {

		if (imageRequest.getImageUri().toLowerCase().startsWith("resource:")) {

			ServiceResponse serviceResponse = new ServiceResponse();
			String rawResourceName = imageRequest.getImageUri().substring(imageRequest.getImageUri().indexOf("resource:") + 9);
			String resolvedResourceName = resolveResourceName(rawResourceName);

			/* First check to see it has already been pulled into the cache */
			Bitmap bitmap = getBitmap(resolvedResourceName);
			if (bitmap != null) {

				if (imageRequest.getRequestListener() != null) {
					serviceResponse.data = new ImageResponse(bitmap, imageRequest.getImageUri());
					imageRequest.getRequestListener().onComplete(serviceResponse);
				}

				if (imageRequest.getImageView() != null) {
					/* Is auto scaled based on device screen density */
					BitmapDrawable bitmapDrawable = new BitmapDrawable(Aircandi.applicationContext.getResources(), bitmap);
					ImageUtils.showDrawableInImageView(bitmapDrawable, imageRequest.getImageView(), false, AnimUtils.fadeInMedium());
				}
				return;
			}

			/* Not in the cache so pull it directly from resources */
			int resourceId = Aircandi.applicationContext.getResources().getIdentifier(resolvedResourceName, "drawable", "com.aircandi");
			bitmap = loadBitmapFromResources(resourceId);
			if (bitmap != null) {

				/* We put resource images into the cache so they are consistent */
				Logger.v(this, resolvedResourceName + ": Pushing into cache...");
				BitmapManager.getInstance().putBitmap(resolvedResourceName, bitmap);

				if (imageRequest.getRequestListener() != null) {
					serviceResponse.data = new ImageResponse(bitmap, imageRequest.getImageUri());
					imageRequest.getRequestListener().onComplete(serviceResponse);
				}

				if (imageRequest.getImageView() != null) {
					BitmapDrawable bitmapDrawable = new BitmapDrawable(Aircandi.applicationContext.getResources(), bitmap);
					ImageUtils.showDrawableInImageView(bitmapDrawable, imageRequest.getImageView(), false, AnimUtils.fadeInMedium());
				}
				return;
			}
			else {
				throw new IllegalStateException("Bitmap resource is null: " + resolvedResourceName);
			}
		}
		else {
			/*
			 * We use async for WebImageView for faster performance in lists. We don't use
			 * it for CandiViews because of state conflicts and dependencies plus the rendering
			 * process is already async.
			 */
			AsyncTask task = new AsyncTask() {

				@Override
				protected Object doInBackground(Object... params) {

					ServiceResponse serviceResponse = new ServiceResponse();
					Bitmap bitmap = getBitmap(imageRequest.getImageUri());
					if (bitmap != null) {
						Logger.v(this, "Image request satisfied from cache: " + imageRequest.getImageUri());
						serviceResponse.data = new ImageResponse(bitmap, imageRequest.getImageUri());

						if (imageRequest.getRequestListener() != null) {
							serviceResponse.data = new ImageResponse(bitmap, imageRequest.getImageUri());
							imageRequest.getRequestListener().onComplete(serviceResponse);
						}

						if (imageRequest.getImageView() != null) {
							final BitmapDrawable bitmapDrawable = new BitmapDrawable(Aircandi.applicationContext.getResources(), bitmap);
							/* Put this on the main thread */
							Aircandi.applicationHandler.post(new Runnable() {

								@Override
								public void run() {
									ImageUtils.showDrawableInImageView(bitmapDrawable, imageRequest.getImageView(), false, AnimUtils.fadeInMedium());
								}
							});
						}
					}
					else {
						serviceResponse.responseCode = ResponseCode.Failed;
					}
					return serviceResponse;
				}

				@Override
				protected void onPostExecute(Object result) {
					ServiceResponse serviceResponse = (ServiceResponse) result;
					if (serviceResponse.responseCode != ResponseCode.Success) {
						mBitmapLoader.queueBitmapRequest(imageRequest);
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
	}

	public void putBitmap(String key, Bitmap bitmap) {
		String keyHashed = MiscUtils.md5(key);
		if (getBitmap(keyHashed) == null) {
			mMemoryCache.put(keyHashed, bitmap);
		}

		/* Also add to disk cache */
		synchronized (mDiskCacheLock) {
			if (mDiskLruCache != null) {
				mDiskLruCache.put(keyHashed, bitmap);
			}
		}
	}

	public Bitmap getBitmap(String key) {
		String keyHashed = MiscUtils.md5(key);
		if (mMemoryCache.get(keyHashed) != null) {
			return mMemoryCache.get(keyHashed);
		}
		else {
			synchronized (mDiskCacheLock) {
				/* Wait while disk cache is started from background thread */
				while (mDiskCacheStarting) {
					try {
						mDiskCacheLock.wait();
					}
					catch (InterruptedException e) {}
				}
				if (mDiskLruCache != null) {
					if (mDiskLruCache.containsKey(keyHashed)) {
						/* Push to the mem cache */
						putBitmap(keyHashed, mDiskLruCache.getBitmap(keyHashed));
						return mDiskLruCache.getBitmap(keyHashed);
					}
				}
			}
			return null;
		}
	}

	// --------------------------------------------------------------------------------------------
	// Load routines
	// --------------------------------------------------------------------------------------------

	public Bitmap loadBitmapFromDevice(final Uri imageUri, String imageWidthMax) {

		String[] projection = new String[] { Images.Thumbnails._ID, Images.Thumbnails.DATA, Images.Media.ORIENTATION };
		@SuppressWarnings("unused")
		String imagePath = "";
		File imageFile = null;
		int rotation = 0;

		Cursor cursor = Aircandi.applicationContext.getContentResolver().query(imageUri, projection, null, null, null);

		if (cursor != null) {

			/* Means the image is in the media store */
			String imageData = "";
			if (cursor.moveToFirst()) {
				int dataColumn = cursor.getColumnIndex(Images.Media.DATA);
				int orientationColumn = cursor.getColumnIndex(Images.Media.ORIENTATION);
				imageData = cursor.getString(dataColumn);
				rotation = cursor.getInt(orientationColumn);
			}

			imageFile = new File(imageData);
			imagePath = imageData;
		}
		else {

			/* The image is in the local file system */
			imagePath = imageUri.toString().replace("file://", "");
			imageFile = new File(imageUri.toString().replace("file://", ""));
			ExifInterface exif;
			try {
				exif = new ExifInterface(imageUri.getPath());
				rotation = (int) exifOrientationToDegrees(exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL));
			}
			catch (IOException exception) {
				Exceptions.Handle(exception);
				return null;
			}
		}

		byte[] imageBytes = new byte[(int) imageFile.length()];

		DataInputStream in = null;

		try {
			in = new DataInputStream(new FileInputStream(imageFile));
		}
		catch (FileNotFoundException exception) {
			Exceptions.Handle(exception);
			return null;
		}
		try {
			in.readFully(imageBytes);
		}
		catch (IOException exception) {
			Exceptions.Handle(exception);
			return null;
		}
		try {
			in.close();
		}
		catch (IOException exception) {
			Exceptions.Handle(exception);
			return null;
		}

		// imageOrientation = getExifOrientation(imageUri.getPath(),
		// imageOrientation);
		// imageOrientation = getExifOrientation(imageFile.getAbsolutePath(),
		// imageOrientation);
		Bitmap bitmap = bitmapForByteArray(imageBytes, imageWidthMax, rotation);
		return bitmap;
	}

	public Bitmap loadBitmapFromAssets(final String assetPath) {
		InputStream in = null;
		try {
			final BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
			decodeOptions.inPreferredConfig = CandiConstants.IMAGE_CONFIG_DEFAULT;

			in = Aircandi.applicationContext.getAssets().open(assetPath);
			in.close();
			return BitmapFactory.decodeStream(in, null, decodeOptions);
		}
		catch (final IOException exception) {
			Exceptions.Handle(exception);
			return null;
		}
	}

	public Bitmap loadBitmapFromResources(final int resourceId) {
		BitmapFactory.Options options = new Options();
		options.inScaled = false;
		Bitmap bitmap = BitmapFactory.decodeResource(Aircandi.applicationContext.getResources(), resourceId, options);
		return bitmap;
	}

	// --------------------------------------------------------------------------------------------
	// Processing routines
	// --------------------------------------------------------------------------------------------
	/**
	 * Decodes a byte array into a bitmap at a size that is equal to or
	 * less than imageMemoryBytesMax.
	 * 
	 * @param imageBytes
	 * @param imageRequest
	 * @param imageMemoryBytesMax
	 * @return
	 */
	public Bitmap bitmapForByteArraySampled(byte[] imageBytes, int imageMemoryBytesMax) {

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		options.inPreferredConfig = CandiConstants.IMAGE_CONFIG_DEFAULT;

		/* Initial decode is just to get the bitmap dimensions */
		BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);

		int targetWidth = options.outWidth;
		int targetHeight = options.outHeight;
		int scale = 1;
		while (true) {
			if ((targetWidth * targetHeight) * 4 < imageMemoryBytesMax) {
				break;
			}
			targetWidth /= 2;
			targetHeight /= 2;
			scale *= 2;
		}

		options.inSampleSize = scale;
		options.inJustDecodeBounds = false;

		Bitmap bitmapSampled = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);
		
		return bitmapSampled;
	}

	public Bitmap bitmapForByteArray(byte[] imageBytes, String imageWidthMax, int rotation) {

		BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
		bitmapOptions.inJustDecodeBounds = true;
		bitmapOptions.inPreferredConfig = CandiConstants.IMAGE_CONFIG_DEFAULT;

		/* Initial decode is just to get the bitmap dimensions */
		BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, bitmapOptions);

		int widthRaw = bitmapOptions.outWidth;
		int widthFinal = 500; /* default to this if there's a problem */
		int heightRaw = bitmapOptions.outHeight;
		int heightFinal = 0;

		if (imageWidthMax.toLowerCase().equals("original")) {
			if (ImageUtils.getImageMemorySize(heightRaw, widthRaw, true) > CandiConstants.IMAGE_MEMORY_BYTES_MAX) {
				float finWidth = 1000;
				int sample = 0;

				float fWidth = widthRaw;
				sample = (int) FloatMath.ceil(fWidth / finWidth);

				if (sample == 3) {
					sample = 4;
				}
				else if (sample > 4 && sample < 8) {
					sample = 8;
				}
				else if (sample > 8 && sample < 16) {
					sample = 16;
				}

				bitmapOptions.inSampleSize = sample;
				bitmapOptions.inJustDecodeBounds = false;

				float percentage = (float) widthFinal / widthRaw;
				float proportionateHeight = heightRaw * percentage;
				heightFinal = (int) Math.rint(proportionateHeight);

				Bitmap bitmapSampled = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, bitmapOptions);

				/* Rotate the image if needed */
				if (rotation != 0) {
					Matrix matrix = new Matrix();
					matrix.postRotate(rotation);
					bitmapSampled = Bitmap.createBitmap(bitmapSampled, 0, 0, bitmapSampled.getWidth(), bitmapSampled.getHeight(), matrix, true);
				}

				/* Compress the image */
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				bitmapSampled.compress(Bitmap.CompressFormat.JPEG, 90, baos);
				Bitmap bitmapSampledAndCompressed = BitmapFactory.decodeByteArray(baos.toByteArray(), 0, baos.toByteArray().length);

				/* Release */
				bitmapSampled.recycle();

				return bitmapSampledAndCompressed;
			}
			else {
				bitmapOptions.inJustDecodeBounds = false;
				Bitmap bitmapOriginalSize = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, bitmapOptions);

				/* Rotate the image if needed */
				if (rotation != 0) {
					Matrix matrix = new Matrix();
					matrix.postRotate(rotation);
					bitmapOriginalSize = Bitmap.createBitmap(bitmapOriginalSize, 0, 0, bitmapOriginalSize.getWidth(), bitmapOriginalSize.getHeight(), matrix,
							true);
				}

				return bitmapOriginalSize;
			}
		}
		else {
			widthFinal = Integer.parseInt(imageWidthMax);
			if (widthFinal > widthRaw) {
				bitmapOptions.inJustDecodeBounds = false;
				Bitmap bitmapOriginalSize = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, bitmapOptions);

				/* Rotate the image if needed */
				if (rotation != 0) {
					Matrix matrix = new Matrix();
					matrix.postRotate(rotation);
					bitmapOriginalSize = Bitmap.createBitmap(bitmapOriginalSize, 0, 0, bitmapOriginalSize.getWidth(), bitmapOriginalSize.getHeight(), matrix,
							true);
				}

				return bitmapOriginalSize;
			}
			else {
				int sample = 0;

				float fWidth = widthRaw;
				sample = (int) FloatMath.ceil(fWidth / 1200f);

				if (sample == 3) {
					sample = 4;
				}
				else if (sample > 4 && sample < 8) {
					sample = 8;
				}

				bitmapOptions.inSampleSize = sample;
				bitmapOptions.inJustDecodeBounds = false;

				Bitmap bitmapSampled = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, bitmapOptions);

				float percentage = (float) widthFinal / bitmapSampled.getWidth();
				float proportionateHeight = bitmapSampled.getHeight() * percentage;
				heightFinal = (int) Math.rint(proportionateHeight);

				float scaleWidth = ((float) widthFinal) / bitmapSampled.getWidth();
				float scaleHeight = ((float) heightFinal) / bitmapSampled.getHeight();
				float scaleBy = Math.min(scaleWidth, scaleHeight);

				/* Create a matrix for the manipulation */
				Matrix matrix = new Matrix();

				/* Resize the bitmap */
				matrix.postScale(scaleBy, scaleBy);
				if (rotation != 0) {
					matrix.postRotate(rotation);
				}

				Bitmap bitmapSampledAndScaled = Bitmap.createBitmap(bitmapSampled, 0, 0, bitmapSampled.getWidth(), bitmapSampled.getHeight(), matrix,
						true);

				ByteArrayOutputStream bitmapByteArrayOutputStream = new ByteArrayOutputStream();
				bitmapSampledAndScaled.compress(Bitmap.CompressFormat.JPEG, 85, bitmapByteArrayOutputStream);

				Bitmap bitmapSampledScaledCompressed = BitmapFactory.decodeByteArray(bitmapByteArrayOutputStream.toByteArray(), 0,
						bitmapByteArrayOutputStream.toByteArray().length);

				/* Release */
				bitmapSampled.recycle();
				bitmapSampledAndScaled.recycle();

				return bitmapSampledScaledCompressed;
			}
		}
	}

	public String resolveResourceName(String rawResourceName) {
		int resourceId = Aircandi.applicationContext.getResources().getIdentifier(rawResourceName, "drawable", "com.aircandi");
		if (resourceId == 0) {
			resourceId = Aircandi.applicationContext.getResources().getIdentifier(rawResourceName, "attr", "com.aircandi");
			TypedValue value = new TypedValue();
			if (Aircandi.applicationContext.getTheme().resolveAttribute(resourceId, value, true)) {
				String redirectedResourceName = (String) value.coerceToString();
				return redirectedResourceName;
			}
			else {
				/* We failed to resolve the resource name */
				throw new IllegalStateException("Resource not resolved: " + rawResourceName);
			}
		}
		else {
			return rawResourceName;
		}
	}

	private float exifOrientationToDegrees(int exifOrientation) {
		if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
			return 90;
		}
		else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
			return 180;
		}
		else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
			return 270;
		}
		return 0;
	}

	public static boolean isLocalImage(String imageUri) {
		if (imageUri == null) {
			return false;
		}
		if (imageUri.toLowerCase().startsWith("resource:")) {
			return true;
		}
		if (imageUri.toLowerCase().startsWith("asset:")) {
			return true;
		}
		return false;
	}

	public void stopBitmapLoaderThread() {
		mBitmapLoader.stopBitmapLoaderThread();
	}

	// --------------------------------------------------------------------------------------------
	// Setters/Getters routines
	// --------------------------------------------------------------------------------------------

	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	public static class ViewHolder {

		public ImageView	itemImage;
		public Object		data;
	}
}
