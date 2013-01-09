package com.aircandi.components.bitmaps;

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
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
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

public class BitmapManager {

	private static BitmapManager		singletonObject;
	private DiskLruImageCache			mDiskLruCache;
	private final Object				mDiskCacheLock		= new Object();
	private boolean						mDiskCacheStarting	= true;
	private static final int			DISK_CACHE_SIZE		= 1024 * 1024 * 10; // 10MB
	private static final String			DISK_CACHE_SUBDIR	= "bitmaps";
	private BitmapLoader				mBitmapLoader;

	private LruCache<String, Bitmap>	mMemoryCache;
	private LruCache<String, Bitmap>	mThumbnailCache;

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
		final int cacheSize = 1024 * 1024 * memClass / 2;

		mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
			@Override
			protected int sizeOf(String key, Bitmap bitmap) {
				/* The cache size will be measured in bytes rather than number of items. */
				return bitmap.getByteCount();
			}
		};

		mThumbnailCache = new LruCache<String, Bitmap>(cacheSize / 2) {
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

	public void fetchBitmap(final BitmapRequest imageRequest) {
		/*
		 * We keep drawables on the main thread.
		 */
		if (imageRequest.getImageUri().toLowerCase().startsWith("resource:")) {

			ServiceResponse serviceResponse = new ServiceResponse();
			String rawResourceName = imageRequest.getImageUri().substring(imageRequest.getImageUri().indexOf("resource:") + 9);
			String resolvedResourceName = resolveResourceName(rawResourceName);

			int resourceId = Aircandi.applicationContext.getResources().getIdentifier(resolvedResourceName, "drawable", "com.aircandi");
			Bitmap bitmap = loadBitmapFromResources(resourceId);

			if (bitmap != null) {
				if (imageRequest.getRequestListener() != null) {
					serviceResponse.data = new ImageResponse(bitmap, imageRequest.getImageUri());
					imageRequest.getRequestListener().onComplete(serviceResponse);
				}

				if (imageRequest.getImageView() != null) {
					BitmapDrawable bitmapDrawable = new BitmapDrawable(Aircandi.applicationContext.getResources(), bitmap);
					ImageUtils.showDrawableInImageView(bitmapDrawable, imageRequest.getImageView(), true, AnimUtils.fadeInMedium());
				}
			}
			else {
				throw new IllegalStateException("Bitmap resource is null: " + resolvedResourceName);
			}
		}
		else {
			/*
			 * Fetching from cache often involves file io so we take this off the main (ui) thread.
			 */
			AsyncTask task = new AsyncTask() {

				@Override
				protected Object doInBackground(Object... params) {
					Thread.currentThread().setName("FetchImage");
					ServiceResponse serviceResponse = new ServiceResponse();
					Bitmap bitmap = null;
					if (imageRequest.getUseThumbnailCache()) {
						bitmap = getThumbnail(imageRequest.getImageUri(), imageRequest.getImageSize());
					}
					else {
						bitmap = getBitmap(imageRequest.getImageUri(), imageRequest.getImageSize());
					}

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
									ImageUtils.showDrawableInImageView(bitmapDrawable, imageRequest.getImageView(), true, AnimUtils.fadeInMedium());
								}
							});
						}
					}
					else {
						serviceResponse.responseCode = ResponseCode.Failed;
					}

					if (serviceResponse.responseCode != ResponseCode.Success) {
						mBitmapLoader.queueBitmapRequest(imageRequest);
					}
					return null;
				}
			};

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
			else {
				task.execute();
			}
		}
	}

	public void putBitmap(String key, Bitmap bitmap, Integer size) {
		/*
		 * Our strategy is to push the raw bitmap into the file cache
		 * and a scaled bitmap into the memory cache.
		 */
		String diskCacheKey = key;
		String diskKeyHashed = MiscUtils.md5(diskCacheKey);

		/* First add unscaled version to disk cache */
		synchronized (mDiskCacheLock) {
			if (mDiskLruCache != null) {
				if (!mDiskLruCache.containsKey(diskKeyHashed)) {
					mDiskLruCache.put(diskKeyHashed, bitmap);
				}
			}
		}

		/*
		 * Next push scaled version to mem cache. The call will create
		 * the sized memory cache entry from the disk cache data we
		 * just pushed
		 */
		getBitmap(key, size);
	}

	public Bitmap putImageBytes(String key, byte[] imageBytes, Integer size) {
		/*
		 * Our strategy is to push the raw bitmap into the file cache
		 * and a scaled bitmap into the memory cache.
		 */
		String diskCacheKey = key;
		String diskKeyHashed = MiscUtils.md5(diskCacheKey);

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
		Bitmap bitmap = getBitmap(key, size);
		return bitmap;
	}

	public Bitmap getBitmap(String key, Integer size) {
		String memCacheKey = key;
		String diskCacheKey = key;
		if (size != null) {
			memCacheKey += "." + String.valueOf(size);
		}
		String memKeyHashed = MiscUtils.md5(memCacheKey);
		String diskKeyHashed = MiscUtils.md5(diskCacheKey);

		if (mMemoryCache.get(memKeyHashed) != null) {
			Logger.v(this, "Image request satisfied from MEMORY cache: " + memCacheKey);
			return mMemoryCache.get(memKeyHashed);
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
					if (mDiskLruCache.containsKey(diskKeyHashed)) {
						Logger.v(this, "Image request satisfied from FILE cache: " + key);
						Bitmap bitmap = null;

						/* Push to the mem cache */
						byte[] imageBytes = mDiskLruCache.getImageBytes(diskKeyHashed);

						/* Scale if needed */
						bitmap = bitmapForByteArraySampled(imageBytes, size, null);

						synchronized (mMemoryCache) {
							mMemoryCache.put(memKeyHashed, bitmap);
						}

						/* Deliver to caller */
						return bitmap;
					}
				}
			}
			return null;
		}
	}

	public Bitmap putThumbnailBytes(String key, byte[] imageBytes, Integer size) {
		String memCacheKey = key;
		if (size != null) {
			memCacheKey += "." + String.valueOf(size);
		}
		String memKeyHashed = MiscUtils.md5(memCacheKey);

		/* Scale if needed and push */
		Bitmap bitmap = bitmapForByteArraySampled(imageBytes, size, null);
		mThumbnailCache.put(memKeyHashed, bitmap);
		return bitmap;
	}

	public Bitmap getThumbnail(String key, Integer size) {
		String memCacheKey = key;
		if (size != null) {
			memCacheKey += "." + String.valueOf(size);
		}
		String memKeyHashed = MiscUtils.md5(memCacheKey);

		if (mThumbnailCache.get(memKeyHashed) != null) {
			Logger.v(this, "Image request satisfied from THUMBNAIL cache: " + memCacheKey);
			return mThumbnailCache.get(memKeyHashed);
		}
		return null;
	}

	// --------------------------------------------------------------------------------------------
	// Load routines
	// --------------------------------------------------------------------------------------------

	public Bitmap loadBitmapFromDeviceSampled(final Uri imageUri) {

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

		Bitmap bitmap = bitmapForByteArraySampled(imageBytes, null, rotation);
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
		Bitmap bitmap = BitmapFactory.decodeResource(Aircandi.applicationContext.getResources(), resourceId);
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
	public Bitmap bitmapForByteArraySampled(byte[] imageBytes, Integer size, Integer rotation) {

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		options.inPurgeable = true;
		options.inPreferredConfig = CandiConstants.IMAGE_CONFIG_DEFAULT;

		/* Initial decode is just to get the bitmap dimensions */
		BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);

		int width = options.outWidth;
		int height = options.outHeight;

		int scale = 1;
		if (size != null) {
			if (width > size && height > size) {
				scale = Math.min(width / size, height / size);
			}
		}
		else {
			int imageMemorySize = ImageUtils.getImageMemorySize(height, width, true);
			if (imageMemorySize > CandiConstants.IMAGE_MEMORY_BYTES_MAX) {
				scale = Math.round(((float) imageMemorySize / (float) CandiConstants.IMAGE_MEMORY_BYTES_MAX) / 2f);
			}
		}

		options.inSampleSize = scale;
		options.inJustDecodeBounds = false;

		Bitmap bitmapSampled = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);

		/* Rotate the image if needed */
		if (rotation != null && rotation != 0) {
			Matrix matrix = new Matrix();
			matrix.postRotate(rotation);
			bitmapSampled = Bitmap.createBitmap(bitmapSampled, 0, 0, bitmapSampled.getWidth(), bitmapSampled.getHeight(), matrix, true);
		}

		return bitmapSampled;
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
