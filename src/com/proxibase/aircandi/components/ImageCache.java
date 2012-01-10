package com.proxibase.aircandi.components;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;

import com.google.common.collect.MapMaker;
import com.proxibase.aircandi.core.CandiConstants;

/**
 * <p>
 * A simple 2-level cache for bitmap images consisting of a small and fast in-memory cache (1st level cache) and a
 * slower but bigger disk cache (2nd level cache). For second level caching, the application's cache directory will be
 * used. Please note that Android may at any point decide to wipe that directory.
 * </p>
 * <p>
 * When pulling from the cache, it will first attempt to load the image from memory. If that fails, it will try to load
 * it from disk. If that succeeds, the image will be put in the 1st level cache and returned. Otherwise it's a cache
 * miss, and the caller is responsible for loading the image from elsewhere (probably the Internet).
 * </p>
 * <p>
 * Pushes to the cache are always write-through (i.e., the image will be stored both on disk and in memory).
 * </p>
 */

public class ImageCache implements Map<String, Bitmap> {

	private int						mCachedImageQuality		= 100;
	private String					mSecondLevelCacheDir;
	private Map<String, Bitmap>		mCache;
	private CompressFormat			mCompressedImageFormat	= CompressFormat.PNG;
	private BitmapFactory.Options	mOptions;
	private boolean					mFileCacheOnly			= false;

	public ImageCache(Context context, String cacheSubDirectory, int initialCapacity, int concurrencyLevel) {
		this.mCache = new MapMaker().initialCapacity(initialCapacity).concurrencyLevel(concurrencyLevel).weakValues().makeMap();
		this.mSecondLevelCacheDir = context.getApplicationContext().getCacheDir() + cacheSubDirectory;
		makeCacheDirectory();
		mOptions = new BitmapFactory.Options();
		mOptions.inPreferredConfig = CandiConstants.IMAGE_CONFIG_DEFAULT;
	}

	public boolean cacheDirectoryExists() {
		boolean exists = (new File(mSecondLevelCacheDir)).exists();
		return exists;
	}

	public void makeCacheDirectory() {
		new File(mSecondLevelCacheDir).mkdirs();
	}

	/**
	 * The image format that should be used when caching images on disk. The default value is {@link CompressFormat#PNG}
	 * . Note that when switching to a format like JPEG, you will lose any transparency that was part of the image.
	 * 
	 * @param compressedImageFormat the {@link CompressFormat}
	 */
	public void setCompressedImageFormat(CompressFormat compressedImageFormat) {
		this.mCompressedImageFormat = compressedImageFormat;
	}

	public CompressFormat getCompressedImageFormat() {
		return mCompressedImageFormat;
	}

	/**
	 * @param cachedImageQuality the quality of images being compressed and written to disk (2nd level cache) as a
	 *            number in [0..100]
	 */
	public void setCachedImageQuality(int cachedImageQuality) {
		this.mCachedImageQuality = cachedImageQuality;
	}

	public int getCachedImageQuality() {
		return mCachedImageQuality;
	}

	public void recycleBitmaps() {
		final Enumeration<String> strEnum = Collections.enumeration(mCache.keySet());
		while (strEnum.hasMoreElements()) {
			mCache.get(strEnum.nextElement()).recycle();
		}
	}

	public synchronized Bitmap get(Object key) {

		String imageUrl = (String) key;

		if (!mFileCacheOnly) {
			Bitmap bitmap = mCache.get(imageUrl);

			/* 1st level cache hit (memory) */
			if (bitmap != null) {
				return mCache.get(imageUrl);
			}
		}

		File imageFile = getImageFile(imageUrl);
		if (imageFile.exists()) {

			/* 2nd level cache hit (disk) */
			Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), mOptions);

			/* treat decoding errors as a cache miss */
			if (bitmap == null) {
				return null;
			}
			if (!mFileCacheOnly) {
				mCache.put(imageUrl, bitmap);
			}
			return bitmap;
		}

		/* cache miss */
		return null;
	}

	public Bitmap put(String imageUrl, Bitmap image) {
		return put(imageUrl, image, mCompressedImageFormat);
	}

	public Bitmap put(String imageUrl, Bitmap image, CompressFormat compressFormat) {
		/*
		 * Write bitmap to disk cache and then memory cache
		 */

		/* Cache could have been nuked externally so make sure it's there. */
		if (!cacheDirectoryExists()) {
			makeCacheDirectory();
		}

		File imageFile = getImageFile(imageUrl);
		FileOutputStream ostream = null;
		try {
			imageFile.createNewFile();
			ostream = new FileOutputStream(imageFile);
			image.compress(compressFormat, mCachedImageQuality, ostream);
		}
		catch (IOException exception) {
			Exceptions.Handle(exception);
		}
		finally {
			try {
				if (ostream != null) {
					ostream.close();
				}
			}
			catch (IOException exception) {
				Exceptions.Handle(exception);
			}
		}

		/* Write file to memory cache as well if allowed */
		if (!mFileCacheOnly) {
			mCache.put(imageUrl, image);
		}

		return image;
	}

	public void putAll(Map<? extends String, ? extends Bitmap> t) {
		throw new UnsupportedOperationException();
	}

	public void cleanCacheAsync(Context context) {
		Thread t = new Thread() {

			@Override
			public void run() {
				try {
					File cacheDir = new File(mSecondLevelCacheDir);
					cleanCache(cacheDir, CandiConstants.CACHE_TRIGGER_SIZE, CandiConstants.CACHE_TARGET_SIZE);
					Logger.i(this, "Checked size of file cache");
				}
				catch (Exception exception) {
					Exceptions.Handle(exception);
				}
			}
		};

		t.setName("CacheCleanThread");
		t.start();
	}

	private void cleanCache(File cacheDir, long triggerSize, long targetSize) {
		try {

			File[] files = cacheDir.listFiles();
			if (files == null) {
				return;
			}

			Arrays.sort(files, new SortFilesByModified());
			long cacheSize = cacheSize(files);
			Logger.d(this, "Cache size: " + String.valueOf(cacheSize)
							+ " trigger size: "
							+ String.valueOf(triggerSize)
							+ " target size: "
							+ String.valueOf(targetSize));
			if (cacheSize > triggerSize) {
				Logger.d(this, "Trimming file cache");
				cleanCache(files, targetSize);
			}
		}
		catch (Exception exception) {
			Exceptions.Handle(exception);
		}
	}

	class SortFilesByModified implements Comparator<File> {

		@Override
		public int compare(File f1, File f2) {
			long m1 = f1.lastModified();
			long m2 = f2.lastModified();

			if (m2 > m1) {
				return 1;
			}
			else if (m2 == m1) {
				return 0;
			}
			else {
				return -1;
			}
		}
	}

	private static long cacheSize(File[] files) {

		long total = 0;

		for (File f : files) {
			total += f.length();
		}

		return total;
	}

	private void cleanCache(File[] files, long maxSize) {

		long total = 0;
		int deletes = 0;

		for (int i = 0; i < files.length; i++) {
			File f = files[i];
			total += f.length();
			if (total < maxSize) {
				/* ok */
			}
			else {
				f.delete();
				deletes++;
				Logger.v(this, "Deleting from file cache: " + f.getAbsolutePath());
			}
		}
	}

	public boolean containsKey(Object key) {

		if (mFileCacheOnly) {
			File imageFile = getImageFile((String) key);
			if (imageFile.exists()) {
				return true;
			}
			return false;
		}
		else {
			if (mCache.containsKey(key))
				return true;
			else {
				File imageFile = getImageFile((String) key);
				if (imageFile.exists()) {
					return true;
				}
				return false;
			}
		}
	}

	public boolean containsValue(Object value) {
		return mCache.containsValue(value);
	}

	public Bitmap remove(Object key) {

		/* Memory cache */
		if (mCache.containsKey(key)) {
			mCache.remove(key);
		}

		/* File cache */
		File imageFile = getImageFile((String) key);
		if (imageFile.exists()) {
			imageFile.delete();
		}

		return null;
	}

	public Set<String> keySet() {
		return mCache.keySet();
	}

	public Set<java.util.Map.Entry<String, Bitmap>> entrySet() {
		return mCache.entrySet();
	}

	public int size() {
		return mCache.size();
	}

	public boolean isEmpty() {
		return mCache.isEmpty();
	}

	public void clear() {
		mCache.clear();
	}

	public Collection<Bitmap> values() {
		return mCache.values();
	}

	private File getImageFile(String imageUri) {
		String fileName = Integer.toHexString(imageUri.hashCode()) + "." + mCompressedImageFormat.name();
		return new File(mSecondLevelCacheDir + "/" + fileName);
	}

	public String getImageFileUri(String imageUri) {
		String fileName = Integer.toHexString(imageUri.hashCode()) + "." + mCompressedImageFormat.name();
		return mSecondLevelCacheDir + "/" + fileName;
	}

	public void setFileCacheOnly(boolean fileCacheOnly) {
		this.mFileCacheOnly = fileCacheOnly;
	}

	public boolean isFileCacheOnly() {
		return mFileCacheOnly;
	}
}
