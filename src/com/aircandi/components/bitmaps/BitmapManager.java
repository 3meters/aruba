package com.aircandi.components.bitmaps;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import android.app.ActivityManager;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore.Images;
import android.support.v4.util.LruCache;
import android.util.TypedValue;
import android.widget.ImageView;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.components.Exceptions;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.bitmaps.BitmapRequest.ImageResponse;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.UI;
import com.aircandi.utilities.Utilities;

@SuppressWarnings("ucd")
public class BitmapManager {

	private DiskLruImageCache				mDiskLruCache;
	private final Object					mDiskCacheLock		= new Object();
	private boolean							mDiskCacheStarting	= true;
	private static final int				DISK_CACHE_SIZE		= 10 << 10 << 10;	// 10MB
	private static final String				DISK_CACHE_SUBDIR	= "bitmaps";
	private final BitmapLoader				mBitmapLoader;

	private final LruCache<String, Bitmap>	mMemoryCache;
	private final Object					mMemoryCacheLock	= new Object();

	private static class BitmapManagerHolder {
		public static final BitmapManager	instance	= new BitmapManager();
	}

	public static BitmapManager getInstance() {
		return BitmapManagerHolder.instance;
	}

	private BitmapManager() {
		mBitmapLoader = new BitmapLoader();
		/*
		 * get memory class of this device, exceeding this amount will throw an
		 * OutOfMemory exception.
		 */
		final int memClass = ((ActivityManager) Aircandi.applicationContext.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
		Logger.i(this, "DEVICE memory class: " + String.valueOf(memClass));

		/* Use 1/4th of the available memory for this memory cache. */
		final int cacheSize = (memClass << 10 << 10) >> 2;
		Logger.i(this, "Memory cache size: " + String.valueOf(cacheSize));

		mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
			@Override
			protected int sizeOf(String key, Bitmap bitmap) {
				/* The cache size will be measured in bytes rather than number of items. */
				return UI.getImageMemorySize(bitmap.getHeight(), bitmap.getWidth(), bitmap.hasAlpha());
			}
		};

		/* Initialize disk cache on background thread */
		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("InitDiskCache");
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

	public void masterFetch(final BitmapRequest bitmapRequest) {
		if (BitmapManager.isDrawable(bitmapRequest)) {
			BitmapManager.getInstance().fetchDrawable(bitmapRequest);
		}
		else {
			final ServiceResponse serviceResponse = BitmapManager.getInstance().fetchBitmap(bitmapRequest);
			if (serviceResponse.responseCode != ResponseCode.SUCCESS) {
				if (bitmapRequest.getRequestListener() != null) {
					bitmapRequest.getRequestListener().onStart();
				}
								
				BitmapManager.getInstance().downloadBitmap(bitmapRequest);
			}
		}
	}

	public static Boolean isDrawable(BitmapRequest bitmapRequest) {
		if (bitmapRequest.getImageUri().toLowerCase(Locale.US).startsWith("resource:")) {
			return true;
		}
		return false;
	}

	public ServiceResponse fetchDrawable(final BitmapRequest bitmapRequest) {

		final ServiceResponse serviceResponse = new ServiceResponse();
		final String rawResourceName = bitmapRequest.getImageUri().substring(bitmapRequest.getImageUri().indexOf("resource:") + 9);
		final String resolvedResourceName = resolveResourceName(rawResourceName);
		if (resolvedResourceName == null) {
			serviceResponse.responseCode = ResponseCode.FAILED;
			if (bitmapRequest.getRequestListener() != null) {
				bitmapRequest.getRequestListener().onComplete(serviceResponse);
			}
			return serviceResponse;
		}

		final int resourceId = Aircandi.applicationContext.getResources().getIdentifier(resolvedResourceName, "drawable",
				Aircandi.getInstance().getPackageName());

		String memCacheKey = String.valueOf(resourceId);
		if (bitmapRequest.getImageSize() != null) {
			memCacheKey += "." + String.valueOf(bitmapRequest.getImageSize());
		}

		Bitmap bitmap = null;
		synchronized (mMemoryCacheLock) {
			final String memKeyHashed = Utilities.md5(memCacheKey);
			bitmap = mMemoryCache.get(memKeyHashed);

			if (bitmap == null) {
				bitmap = loadBitmapFromResourcesSampled(resourceId, bitmapRequest.getImageSize());
				synchronized (mMemoryCache) {
					mMemoryCache.put(memKeyHashed, bitmap);
				}
			}
		}

		if (bitmapRequest.getRequestListener() != null) {
			serviceResponse.data = new ImageResponse(bitmap, bitmapRequest.getImageUri());
			bitmapRequest.getRequestListener().onComplete(serviceResponse);
		}

		if (bitmapRequest.getImageView() != null) {
			final BitmapDrawable bitmapDrawable = new BitmapDrawable(Aircandi.applicationContext.getResources(), bitmap);

			/* Put this on the main thread */
			Aircandi.mainThreadHandler.post(new Runnable() {
				@Override
				public void run() {
					UI.showDrawableInImageView(bitmapDrawable, bitmapRequest.getImageView(), true, Animate.fadeInMedium());
				}
			});
		}

		return serviceResponse;
	}

	public ServiceResponse fetchBitmap(final BitmapRequest bitmapRequest) {

		final ServiceResponse serviceResponse = new ServiceResponse();
		final Bitmap bitmap = getBitmap(bitmapRequest.getImageUri(), bitmapRequest.getImageSize());

		if (bitmap != null) {
			serviceResponse.data = new ImageResponse(bitmap, bitmapRequest.getImageUri());

			if (bitmapRequest.getRequestListener() != null) {
				serviceResponse.data = new ImageResponse(bitmap, bitmapRequest.getImageUri());
				bitmapRequest.getRequestListener().onComplete(serviceResponse);
			}

			if (bitmapRequest.getImageView() != null) {
				final BitmapDrawable bitmapDrawable = new BitmapDrawable(Aircandi.applicationContext.getResources(), bitmap);

				/* Put this on the main thread */
				Aircandi.mainThreadHandler.post(new Runnable() {
					@Override
					public void run() {
						UI.showDrawableInImageView(bitmapDrawable, bitmapRequest.getImageView(), true, Animate.fadeInMedium());
					}
				});
			}
		}
		else {
			serviceResponse.responseCode = ResponseCode.FAILED;
		}

		return serviceResponse;
	}

	public void downloadBitmap(final BitmapRequest bitmapRequest) {
		mBitmapLoader.queueBitmapRequest(bitmapRequest);
	}

	public Bitmap putImageBytes(String key, byte[] imageBytes, Integer size) {
		/*
		 * Our strategy is to push the raw bitmap into the file cache
		 * and a scaled bitmap into the memory cache.
		 */
		final String diskCacheKey = key;
		final String diskKeyHashed = Utilities.md5(diskCacheKey);

		/* First add unscaled version to disk cache */
		synchronized (mDiskCacheLock) {
			if (mDiskLruCache != null) {
				if (!mDiskLruCache.containsKey(diskKeyHashed)) {
					mDiskLruCache.putBytes(diskKeyHashed, imageBytes);
				}
			}
		}

		/*
		 * Next push scaled version to mem cache. The call will create
		 * the sized memory cache entry from the disk cache data we
		 * just pushed
		 */
		final Bitmap bitmap = getBitmap(key, size);
		return bitmap;
	}

	private Bitmap getBitmap(String key, Integer size) {
		String memCacheKey = key;
		final String diskCacheKey = key;
		if (size != null) {
			memCacheKey += "." + String.valueOf(size);
		}
		final String memKeyHashed = Utilities.md5(memCacheKey);
		final String diskKeyHashed = Utilities.md5(diskCacheKey);

		synchronized (mMemoryCacheLock) {
			Bitmap bitmap = mMemoryCache.get(memKeyHashed);
			if (bitmap != null) {
				Logger.v(this, "Image request satisfied from MEMORY cache: " + key);
				return bitmap;
			}
			else {
				synchronized (mDiskCacheLock) {
					/* Wait while disk cache is started from background thread */
					while (mDiskCacheStarting) {
						try {
							mDiskCacheLock.wait();
						}
						catch (InterruptedException e) {} // $codepro.audit.disable emptyCatchClause
					}
					if (mDiskLruCache != null) {
						if (mDiskLruCache.containsKey(diskKeyHashed)) {

							/* Push to the mem cache */
							final byte[] imageBytes = mDiskLruCache.getImageBytes(diskKeyHashed);

							/* Scale if needed */
							bitmap = bitmapForByteArraySampled(imageBytes, size, null);

							if (bitmap != null) {
								mMemoryCache.put(memKeyHashed, bitmap);
								Logger.v(this, "Image request satisfied from FILE cache: " + key);
							}
							return bitmap;
						}
					}
				}
				return null;
			}
		}
	}

	public void putBitmapInMemoryCache(String key, Bitmap bitmap) {
		synchronized (mMemoryCacheLock) {
			mMemoryCache.put(key, bitmap);
		}
	}

	public Bitmap getBitmapFromMemoryCache(String key) {
		synchronized (mMemoryCacheLock) {
			return mMemoryCache.get(key);
		}
	}

	public Bitmap removeBitmapFromMemoryCache(String key) {
		synchronized (mMemoryCacheLock) {
			return mMemoryCache.remove(key);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Load routines
	// --------------------------------------------------------------------------------------------

	public Bitmap loadBitmapFromDeviceSampled(final Uri photoUri) {

		final String[] projection = new String[] { Images.Thumbnails._ID, Images.Thumbnails.DATA, Images.Media.ORIENTATION };
		File imageFile = null;
		int rotation = 0;

		final Cursor cursor = Aircandi.applicationContext.getContentResolver().query(photoUri, projection, null, null, null);

		if (cursor != null) {

			/* Means the image is in the media store */
			String imageData = "";
			if (cursor.moveToFirst()) {
				final int dataColumn = cursor.getColumnIndex(Images.Media.DATA);
				final int orientationColumn = cursor.getColumnIndex(Images.Media.ORIENTATION);
				imageData = cursor.getString(dataColumn);
				rotation = cursor.getInt(orientationColumn);
			}

			imageFile = new File(imageData);
			cursor.close();
		}
		else {

			/* The image is in the local file system */
			imageFile = new File(photoUri.toString().replace("file://", ""));

			final ExifInterface exif;
			try {
				exif = new ExifInterface(photoUri.getPath());
				rotation = (int) exifOrientationToDegrees(exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL));
			}
			catch (IOException exception) {
				Exceptions.handle(exception);
				return null;
			}
		}

		final Bitmap bitmap = bitmapForImageFileSampled(imageFile, null, rotation);
		return bitmap;
	}

	private Bitmap loadBitmapFromResourcesSampled(final Integer resourceId, Integer size) {
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		options.inPurgeable = true;
		options.inPreferredConfig = Constants.IMAGE_CONFIG_DEFAULT;

		BitmapFactory.decodeResource(Aircandi.applicationContext.getResources(), resourceId, options);

		final int width = options.outWidth;
		final int height = options.outHeight;

		int scale = 1;
		if (size != null) {
			if (width > size && height > size) {
				scale = Math.min(width / size, height / size);
			}
		}

		options.inSampleSize = scale;
		options.inJustDecodeBounds = false;

		final Bitmap bitmap = BitmapFactory.decodeResource(Aircandi.applicationContext.getResources(), resourceId, options);
		return bitmap;
	}

	// --------------------------------------------------------------------------------------------
	// Processing routines
	// --------------------------------------------------------------------------------------------

	/**
	 * Decode byte array into a bitmap. The byte array is sampled if needed to keep the memory size of the bitmap
	 * approximately less than or equal to IMAGE_MEMORY_BYTES_MAX. If rotation != 0 then the image is rotated after it
	 * is decoded.
	 * 
	 * @param imageBytes
	 * @param rotation
	 * @return
	 */
	@SuppressWarnings("ucd")
	public Bitmap bitmapForByteArraySampled(byte[] imageBytes, Integer size, Integer rotation) {

		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		options.inPurgeable = true;
		options.inPreferredConfig = Constants.IMAGE_CONFIG_DEFAULT;

		/* Initial decode is just to get the bitmap dimensions */
		BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);

		final int width = options.outWidth;
		final int height = options.outHeight;

		int scale = 1;
		if (size != null) {
			if (width > size && height > size) {
				scale = Math.min(width / size, height / size);
			}
		}
		else {
			final int imageMemorySize = UI.getImageMemorySize(height, width, true);
			if (imageMemorySize > Constants.IMAGE_MEMORY_BYTES_MAX) {
				scale = Math.round(((float) imageMemorySize / (float) Constants.IMAGE_MEMORY_BYTES_MAX) / 2f);
			}
		}

		options.inSampleSize = scale;
		options.inJustDecodeBounds = false;

		Bitmap bitmapSampled = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);

		/* Rotate the image if needed */
		if (rotation != null && rotation != 0) {
			final Matrix matrix = new Matrix();
			matrix.postRotate(rotation);
			bitmapSampled = Bitmap.createBitmap(bitmapSampled, 0, 0, bitmapSampled.getWidth(), bitmapSampled.getHeight(), matrix, true);
		}

		return bitmapSampled;
	}

	/**
	 * Decode an image file into a bitmap. The image file is sampled if needed to keep the memory size of the bitmap
	 * approximately less than or equal to IMAGE_MEMORY_BYTES_MAX. If rotation != 0 then the image is rotated after it
	 * is decoded.
	 * 
	 * @param imageFile
	 * @param rotation
	 * @return
	 */
	@SuppressWarnings("ucd")
	public Bitmap bitmapForImageFileSampled(File imageFile, Integer size, Integer rotation) {

		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		options.inPurgeable = true;
		options.inPreferredConfig = Constants.IMAGE_CONFIG_DEFAULT;

		/* Initial decode is just to get the bitmap dimensions */
		BitmapFactory.decodeFile(imageFile.getPath(), options);

		final int width = options.outWidth;
		final int height = options.outHeight;

		int scale = 1;
		if (size != null) {
			if (width > size && height > size) {
				scale = Math.min(width / size, height / size);
			}
		}
		else {
			final int imageMemorySize = UI.getImageMemorySize(height, width, true);
			if (imageMemorySize > Constants.IMAGE_MEMORY_BYTES_MAX) {
				scale = Math.round(((float) imageMemorySize / (float) Constants.IMAGE_MEMORY_BYTES_MAX) / 2f);
			}
		}

		options.inSampleSize = scale;
		options.inJustDecodeBounds = false;

		Bitmap bitmapSampled = BitmapFactory.decodeFile(imageFile.getPath(), options);

		/* Rotate the image if needed */
		if (rotation != null && rotation != 0) {
			final Matrix matrix = new Matrix();
			matrix.postRotate(rotation);
			bitmapSampled = Bitmap.createBitmap(bitmapSampled, 0, 0, bitmapSampled.getWidth(), bitmapSampled.getHeight(), matrix, true);
		}

		return bitmapSampled;
	}

	public String resolveResourceName(String rawResourceName) {
		int resourceId = Aircandi.applicationContext.getResources().getIdentifier(rawResourceName, "drawable", Aircandi.getInstance().getPackageName());
		if (resourceId == 0) {
			resourceId = Aircandi.applicationContext.getResources().getIdentifier(rawResourceName, "attr", Aircandi.getInstance().getPackageName());
			final TypedValue value = new TypedValue();
			if (Aircandi.applicationContext.getTheme().resolveAttribute(resourceId, value, true)) {
				final String redirectedResourceName = (String) value.coerceToString();
				return redirectedResourceName;
			}
			else {
				return null;
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

	public static boolean isLocalImage(String photoUri) {
		if (photoUri == null) {
			return false;
		}
		if (photoUri.toLowerCase(Locale.US).startsWith("resource:")) {
			return true;
		}
		if (photoUri.toLowerCase(Locale.US).startsWith("asset:")) {
			return true;
		}
		return false;
	}

	public void stopBitmapLoaderThread() {
		/*
		 * Called when AircandiForm is being destroyed.
		 */
		mBitmapLoader.stopBitmapLoaderThread();
	}

	// --------------------------------------------------------------------------------------------
	// Setters/Getters routines
	// --------------------------------------------------------------------------------------------

	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	@SuppressWarnings("ucd")
	public static class ViewHolder {

		public ImageView	photoView;
		public ImageResult	data;

	}
}
