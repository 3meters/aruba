package com.proxibase.aircandi.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;

import com.google.common.collect.MapMaker;

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

	private int					mCachedImageQuality		= 75;
	private String				mSecondLevelCacheDir;
	private Map<String, Bitmap>	mCache;
	private CompressFormat		mCompressedImageFormat	= CompressFormat.PNG;

	public ImageCache(Context context, String cacheSubDirectory, int initialCapacity, int concurrencyLevel) {
		this.mCache = new MapMaker().initialCapacity(initialCapacity).concurrencyLevel(concurrencyLevel).weakValues().makeMap();
		this.mSecondLevelCacheDir = context.getApplicationContext().getCacheDir() + cacheSubDirectory;
		makeCacheDirectory();
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

	public synchronized Bitmap get(Object key) {

		String imageUrl = (String) key;
		Bitmap bitmap = mCache.get(imageUrl);

		if (bitmap != null) {
			// 1st level cache hit (memory)
			return bitmap;
		}

		File imageFile = getImageFile(imageUrl);
		if (imageFile.exists()) {
			// 2nd level cache hit (disk)
			bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
			if (bitmap == null) {
				// treat decoding errors as a cache miss
				return null;
			}
			mCache.put(imageUrl, bitmap);
			return bitmap;
		}

		// cache miss
		return null;
	}

	public Bitmap put(String imageUrl, Bitmap image) {
		/*
		 * Write bitmap to disk cache and then memory cache
		 */
		File imageFile = getImageFile(imageUrl);
		try {
			imageFile.createNewFile();
			FileOutputStream ostream = new FileOutputStream(imageFile);
			image.compress(mCompressedImageFormat, mCachedImageQuality, ostream);
			ostream.close();
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			// If the cache has been cleared then our directory structure is gone too.
			if (!cacheDirectoryExists())
				makeCacheDirectory();
			e.printStackTrace();
		}

		// Write file to memory cache as well
		return mCache.put(imageUrl, image);
	}

	public void putAll(Map<? extends String, ? extends Bitmap> t) {
		throw new UnsupportedOperationException();
	}

	public boolean containsKey(Object key) {

		if (mCache.containsKey(key))
			return true;
		else {
			File imageFile = getImageFile((String) key);
			if (imageFile.exists())
				return true;
		}
		return false;
	}

	public boolean containsValue(Object value) {
		return mCache.containsValue(value);
	}

	public Bitmap remove(Object key) {
		return mCache.remove(key);
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

	private File getImageFile(String imageUrl) {
		String fileName = Integer.toHexString(imageUrl.hashCode()) + "." + mCompressedImageFormat.name();
		return new File(mSecondLevelCacheDir + "/" + fileName);
	}
}
