package com.aircandi.components.bitmaps;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import com.aircandi.CandiConstants;
import com.aircandi.components.Exceptions;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.bitmaps.BitmapRequest.ImageResponse;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.ImageUtils;
import com.aircandi.utilities.MiscUtils;

@SuppressWarnings("ucd")
public class BitmapManager {

	private static BitmapManager		singletonObject;
	private DiskLruImageCache			mDiskLruCache;
	private final Object				mDiskCacheLock		= new Object();
	private boolean						mDiskCacheStarting	= true;
	private static final int			DISK_CACHE_SIZE		= 1024 * 1024 * 10; // 10MB
	private static final String			DISK_CACHE_SUBDIR	= "bitmaps";
	private final BitmapLoader				mBitmapLoader;

	private final LruCache<String, Bitmap>	mMemoryCache;

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
		Logger.d(this, "Device memory class: " + String.valueOf(memClass));

		/* Use 1/4th of the available memory for this memory cache. */
		final int cacheSize = (1024 * 1024 * memClass) / 4;
		Logger.d(this, "Memory cache size: " + String.valueOf(cacheSize));

		mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
			@Override
			protected int sizeOf(String key, Bitmap bitmap) {
				/* The cache size will be measured in bytes rather than number of items. */
				return ImageUtils.getImageMemorySize(bitmap.getHeight(), bitmap.getWidth(), bitmap.hasAlpha());
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

	public static Boolean isDrawable(BitmapRequest bitmapRequest) {
		if (bitmapRequest.getImageUri().toLowerCase(Locale.US).startsWith("resource:")) {
			return true;
		}
		return false;
	}

	public void masterFetch(final BitmapRequest bitmapRequest) {
		if (BitmapManager.isDrawable(bitmapRequest)) {
			BitmapManager.getInstance().fetchDrawable(bitmapRequest);
		}
		else {
			final ServiceResponse serviceResponse = BitmapManager.getInstance().fetchBitmap(bitmapRequest);
			if (serviceResponse.responseCode != ResponseCode.Success) {
				BitmapManager.getInstance().downloadBitmap(bitmapRequest);
			}
		}
	}

	public synchronized ServiceResponse fetchDrawable(final BitmapRequest bitmapRequest) {

		final ServiceResponse serviceResponse = new ServiceResponse();
		final String rawResourceName = bitmapRequest.getImageUri().substring(bitmapRequest.getImageUri().indexOf("resource:") + 9);
		final String resolvedResourceName = resolveResourceName(rawResourceName);
		if (resolvedResourceName == null) {
			serviceResponse.responseCode = ResponseCode.Failed;
			if (bitmapRequest.getRequestListener() != null) {
				bitmapRequest.getRequestListener().onComplete(serviceResponse);
			}
			return serviceResponse;
		}

		final int resourceId = Aircandi.applicationContext.getResources().getIdentifier(resolvedResourceName, "drawable", "com.aircandi");
		String memCacheKey = String.valueOf(resourceId);
		if (bitmapRequest.getImageSize() != null) {
			memCacheKey += "." + String.valueOf(bitmapRequest.getImageSize());
		}
		final String memKeyHashed = MiscUtils.md5(memCacheKey);
		Bitmap bitmap = mMemoryCache.get(memKeyHashed);

		if (bitmap == null) {
			bitmap = loadBitmapFromResourcesSampled(resourceId, bitmapRequest.getImageSize());
			synchronized (mMemoryCache) {
				mMemoryCache.put(memKeyHashed, bitmap);
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
					ImageUtils.showDrawableInImageView(bitmapDrawable, bitmapRequest.getImageView(), true, AnimUtils.fadeInMedium());
				}
			});
		}

		return serviceResponse;
	}

	public synchronized ServiceResponse fetchBitmap(final BitmapRequest bitmapRequest) {
		
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
						ImageUtils.showDrawableInImageView(bitmapDrawable, bitmapRequest.getImageView(), true, AnimUtils.fadeInMedium());
					}
				});
			}
		}
		else {
			serviceResponse.responseCode = ResponseCode.Failed;
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
		final String diskKeyHashed = MiscUtils.md5(diskCacheKey);

		/* First add unscaled version to disk cache */
		synchronized (mDiskCacheLock) {
			if (mDiskLruCache != null) {
				if (!mDiskLruCache.containsKey(diskKeyHashed)) {
					mDiskLruCache.put(diskKeyHashed, imageBytes);
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
		final String memKeyHashed = MiscUtils.md5(memCacheKey);
		final String diskKeyHashed = MiscUtils.md5(diskCacheKey);

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
							synchronized (mMemoryCache) {
								mMemoryCache.put(memKeyHashed, bitmap);
							}
							Logger.v(this, "Image request satisfied from FILE cache: " + key);
						}
						return bitmap;
					}
				}
			}
			return null;
		}
	}

	// --------------------------------------------------------------------------------------------
	// Load routines
	// --------------------------------------------------------------------------------------------

	public Bitmap loadBitmapFromDeviceSampled(final Uri imageUri) {

		final String[] projection = new String[] { Images.Thumbnails._ID, Images.Thumbnails.DATA, Images.Media.ORIENTATION };
		File imageFile = null;
		int rotation = 0;

		final Cursor cursor = Aircandi.applicationContext.getContentResolver().query(imageUri, projection, null, null, null);

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
		}
		else {

			/* The image is in the local file system */
			imageFile = new File(imageUri.toString().replace("file://", ""));

			final ExifInterface exif;
			try {
				exif = new ExifInterface(imageUri.getPath());
				rotation = (int) exifOrientationToDegrees(exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL));
			}
			catch (IOException exception) {
				Exceptions.Handle(exception);
				return null;
			}
		}

		final byte[] imageBytes = new byte[(int) imageFile.length()];

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

		final Bitmap bitmap = bitmapForByteArraySampled(imageBytes, null, rotation);
		return bitmap;
	}

	private Bitmap loadBitmapFromResourcesSampled(final Integer resourceId, Integer size) {
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		options.inPurgeable = true;
		options.inPreferredConfig = CandiConstants.IMAGE_CONFIG_DEFAULT;

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
		options.inPreferredConfig = CandiConstants.IMAGE_CONFIG_DEFAULT;

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
			final int imageMemorySize = ImageUtils.getImageMemorySize(height, width, true);
			if (imageMemorySize > CandiConstants.IMAGE_MEMORY_BYTES_MAX) {
				scale = Math.round(((float) imageMemorySize / (float) CandiConstants.IMAGE_MEMORY_BYTES_MAX) / 2f);
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

	private String resolveResourceName(String rawResourceName) {
		int resourceId = Aircandi.applicationContext.getResources().getIdentifier(rawResourceName, "drawable", "com.aircandi");
		if (resourceId == 0) {
			resourceId = Aircandi.applicationContext.getResources().getIdentifier(rawResourceName, "attr", "com.aircandi");
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

	public static boolean isLocalImage(String imageUri) {
		if (imageUri == null) {
			return false;
		}
		if (imageUri.toLowerCase(Locale.US).startsWith("resource:")) {
			return true;
		}
		if (imageUri.toLowerCase(Locale.US).startsWith("asset:")) {
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

	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	@SuppressWarnings("ucd")
	public static class ViewHolder {

		public ImageView	itemImage;
		public ImageResult	data;

	}
}
