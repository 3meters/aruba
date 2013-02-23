package com.aircandi.components.bitmaps;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Environment;

import com.aircandi.BuildConfig;
import com.aircandi.components.Logger;

@SuppressWarnings("ucd")
public class DiskLruImageCache {

	private DiskLruCache		mDiskCache; // $codepro.audit.disable variableShouldBeFinal
	private CompressFormat		mCompressFormat		= CompressFormat.JPEG;
	private int					mCompressQuality	= 70;
	private static final int	APP_VERSION			= 1;
	private static final int	VALUE_COUNT			= 1;
	@SuppressWarnings("unused")
	private static final String	TAG					= "DiskLruImageCache";

	public DiskLruImageCache(Context context
			, String uniqueName
			, int diskCacheSize
			, CompressFormat compressFormat
			, int quality) {

		try {
			final File diskCacheDir = getDiskCacheDir(context, uniqueName);
			mDiskCache = DiskLruCache.open(diskCacheDir, APP_VERSION, VALUE_COUNT, diskCacheSize);
			mCompressFormat = compressFormat;
			mCompressQuality = quality;
		}
		catch (IOException e) {
			if (BuildConfig.DEBUG) {
				e.printStackTrace();
			}
		}
	}

	private boolean writeBitmapToFile(Bitmap bitmap, DiskLruCache.Editor editor) throws IOException, FileNotFoundException {
		OutputStream out = null;
		CompressFormat compressFormat = mCompressFormat;
		try {
			if (bitmap.hasAlpha()) {
				compressFormat = CompressFormat.PNG;
			}
			out = new BufferedOutputStream(editor.newOutputStream(0), Utils.IO_BUFFER_SIZE);
			return bitmap.compress(compressFormat, mCompressQuality, out);
		}
		finally {
			if (out != null) {
				out.close();
			}
		}
	}

	private boolean writeImageBytesToFile(byte[] imageBytes, DiskLruCache.Editor editor) throws IOException, FileNotFoundException {
		OutputStream out = null;
		try {
			out = new BufferedOutputStream(editor.newOutputStream(0), Utils.IO_BUFFER_SIZE);
			out.write(imageBytes);
			return true;
		}
		finally {
			if (out != null) {
				out.flush();
				out.close();
			}
		}
	}

	private File getDiskCacheDir(Context context, String uniqueName) {
		/*
		 * Check if media is mounted or storage is built-in, if so, try and use external cache dir
		 * otherwise use internal cache dir
		 */
		final String cachePath = (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)
				|| !Utils.isExternalStorageRemovable()) ? Utils.getExternalCacheDir(context).getPath() : context.getCacheDir().getPath();

		return new File(cachePath + File.separator + uniqueName);
	}

	public void putBitmap(String key, Bitmap data) {

		DiskLruCache.Editor editor = null;
		try {
			editor = mDiskCache.edit(key);
			if (editor == null) {
				return;
			}

			if (writeBitmapToFile(data, editor)) {
				editor.commit();
				mDiskCache.flush();
				if (BuildConfig.DEBUG) {
					Logger.d(this, "Image put on disk cache: " + String.valueOf(data.getWidth()) + "x" + String.valueOf(data.getHeight()) + " " + key);
				}
			}
			else {
				editor.abort();
				if (BuildConfig.DEBUG) {
					Logger.d(this, "ERROR on: image put on disk cache " + key);
				}
			}
		}
		catch (IOException e) {
			if (BuildConfig.DEBUG) {
				Logger.d(this, "ERROR on: image put on disk cache " + key);
			}
			try {
				if (editor != null) {
					editor.abort();
				}
			}
			catch (IOException ignored) {} // $codepro.audit.disable emptyCatchClause
		}
	}

	public void putBytes(String key, byte[] imageBytes) {

		DiskLruCache.Editor editor = null;
		try {
			editor = mDiskCache.edit(key);
			if (editor == null) {
				return;
			}

			if (writeImageBytesToFile(imageBytes, editor)) {
				editor.commit();
				mDiskCache.flush();
				if (BuildConfig.DEBUG) {
					Logger.d(this, "Image put on disk cache: " + String.valueOf(imageBytes.length) + " " + key);
				}
			}
			else {
				editor.abort();
				if (BuildConfig.DEBUG) {
					Logger.d(this, "ERROR on: image put on disk cache " + key);
				}
			}
		}
		catch (IOException e) {
			if (BuildConfig.DEBUG) {
				Logger.d(this, "ERROR on: image put on disk cache " + key);
			}
			try {
				if (editor != null) {
					editor.abort();
				}
			}
			catch (IOException ignored) {} // $codepro.audit.disable emptyCatchClause
		}
	}

	public Bitmap getBitmap(String key) {

		Bitmap bitmap = null;
		DiskLruCache.Snapshot snapshot = null;
		try {

			snapshot = mDiskCache.get(key);
			if (snapshot == null) {
				return null;
			}
			final InputStream in = snapshot.getInputStream(0);
			if (in != null) {
				final BufferedInputStream buffIn = new BufferedInputStream(in, Utils.IO_BUFFER_SIZE);
				bitmap = BitmapFactory.decodeStream(buffIn);
			}
		}
		catch (IOException e) {
			if (BuildConfig.DEBUG) {
				e.printStackTrace();
			}
		}
		finally {
			if (snapshot != null) {
				snapshot.close();
			}
		}

		if (BuildConfig.DEBUG) {
			Logger.d(this, (bitmap == null) ? "" : "image read from disk " + key);
		}
		return bitmap;
	}

	public byte[] getImageBytes(String key) {

		DiskLruCache.Snapshot snapshot = null;
		byte[] imageBytes = null;
		try {
			snapshot = mDiskCache.get(key);
			if (snapshot == null) {
				return null;
			}
			final InputStream in = snapshot.getInputStream(0);
			if (in != null) {
				final BufferedInputStream buffIn = new BufferedInputStream(in, Utils.IO_BUFFER_SIZE);
				final int size = buffIn.available();
				imageBytes = new byte[size];
				buffIn.read(imageBytes, 0, size);
			}
		}
		catch (IOException e) {
			if (BuildConfig.DEBUG) {
				e.printStackTrace();
			}
		}
		finally {
			if (snapshot != null) {
				snapshot.close();
			}
		}

		if (BuildConfig.DEBUG) {
			Logger.d(this, (imageBytes.length == 0) ? "" : "image bytes read from disk " + key);
		}
		return imageBytes;
	}

	public boolean containsKey(String key) {

		boolean contained = false;
		DiskLruCache.Snapshot snapshot = null;
		try {
			snapshot = mDiskCache.get(key);
			contained = snapshot != null;
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (snapshot != null) {
				snapshot.close();
			}
		}
		return contained;
	}

	public void clearCache() {
		if (BuildConfig.DEBUG) {
			Logger.d(this, "disk cache CLEARED");
		}
		try {
			mDiskCache.delete();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public File getCacheFolder() {
		return mDiskCache.getDirectory();
	}
}