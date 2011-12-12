package com.proxibase.aircandi.utils;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.anddev.andengine.util.StreamUtils;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore.Images;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException.ProxiErrorCode;

/*
 * TODO: ImageCache uses weak references by default. Should investigate the possible benefits
 * of switching to using soft references instead.
 */

public class ImageManager {

	private static ImageManager	singletonObject;

	private ImageCache			mImageCache;
	private ImageLoader			mImageLoader;
	private DisplayMetrics		mDisplayMetrics;
	private Activity			mActivity;

	public static synchronized ImageManager getInstance() {
		if (singletonObject == null) {
			singletonObject = new ImageManager();
		}
		return singletonObject;
	}

	/**
	 * Designed as a singleton. The private Constructor prevents any other class from instantiating.
	 */
	private ImageManager() {
		setImageLoader(new ImageLoader());
		getImageLoader().setImageCache(mImageCache);
		getImageLoader().setImageManager(this);
	}

	// --------------------------------------------------------------------------------------------
	// Load routines
	// --------------------------------------------------------------------------------------------

	public Bitmap loadBitmapFromDevice(final Uri imageUri, String imageWidthMax) throws ProxibaseException {

		String[] projection = new String[] { Images.Thumbnails._ID, Images.Thumbnails.DATA, Images.Media.ORIENTATION };
		@SuppressWarnings("unused")
		String imagePath = "";
		File imageFile = null;
		int rotation = 0;

		Cursor cursor = mActivity.getContentResolver().query(imageUri, projection, null, null, null);

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
				throw new ProxibaseException(exception.getMessage(), ProxiErrorCode.IOException, exception);
			}
		}

		byte[] imageBytes = new byte[(int) imageFile.length()];

		DataInputStream in = null;

		try {
			in = new DataInputStream(new FileInputStream(imageFile));
		}
		catch (FileNotFoundException exception) {
			throw new ProxibaseException(exception.getMessage(), ProxiErrorCode.FileNotFoundException, exception);
		}
		try {
			in.readFully(imageBytes);
		}
		catch (IOException exception) {
			throw new ProxibaseException(exception.getMessage(), ProxiErrorCode.IOException, exception);
		}
		try {
			in.close();
		}
		catch (IOException exception) {
			throw new ProxibaseException(exception.getMessage(), ProxiErrorCode.IOException, exception);
		}

		// imageOrientation = getExifOrientation(imageUri.getPath(), imageOrientation);
		// imageOrientation = getExifOrientation(imageFile.getAbsolutePath(), imageOrientation);
		Bitmap bitmap = bitmapForByteArray(imageBytes, imageWidthMax, rotation);
		return bitmap;
	}

	public Bitmap loadBitmapFromAssets(final String assetPath) throws ProxibaseException {
		InputStream in = null;
		try {
			final BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
			decodeOptions.inPreferredConfig = CandiConstants.IMAGE_CONFIG_DEFAULT;

			in = mActivity.getAssets().open(assetPath);
			return BitmapFactory.decodeStream(in, null, decodeOptions);
		}
		catch (final IOException exception) {
			throw new ProxibaseException(exception.getMessage(), ProxiErrorCode.IOException, exception);
		}
		finally {
			StreamUtils.close(in);
		}
	}

	public Bitmap loadBitmapFromResources(final int resourceId) {
		Bitmap bitmap = BitmapFactory.decodeResource(mActivity.getResources(), resourceId);
		//Drawable drawable = mContext.getResources().getDrawable(resourceId);
		//BitmapDrawable bitmapDrawable = (BitmapDrawable) mContext.getResources().getDrawable(resourceId); 
		//BitmapDrawable bitmapDrawable = new BitmapDrawable(BitmapFactory.decodeResource(mContext.getResources(), resourceId));		
		return bitmap;
	}

	// --------------------------------------------------------------------------------------------
	// Processing routines
	// --------------------------------------------------------------------------------------------

	public static byte[] byteArrayForBitmap(Bitmap bitmap) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
		byte[] bitmapBytes = outputStream.toByteArray();
		return bitmapBytes;
	}

	private Bitmap bitmapForByteArray(byte[] imageBytes, String imageWidthMax, int rotation) {

		BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
		bitmapOptions.inJustDecodeBounds = true;
		bitmapOptions.inPreferredConfig = CandiConstants.IMAGE_CONFIG_DEFAULT;

		/* Initial decode is just to get the bitmap dimensions */
		BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, bitmapOptions);

		int widthRaw = bitmapOptions.outWidth;
		int widthFinal = 500; // default to this if there's a problem
		int heightRaw = bitmapOptions.outHeight;
		int heightFinal = 0;

		if (imageWidthMax.toLowerCase().equals("original")) {
			if (imageBytes.length > CandiConstants.IMAGE_BYTES_MAX) {
				float finWidth = 1000;
				int sample = 0;

				float fWidth = widthRaw;
				sample = new Double(Math.ceil(fWidth / finWidth)).intValue();

				if (sample == 3) {
					sample = 4;
				}
				else if (sample > 4 && sample < 8) {
					sample = 8;
				}

				bitmapOptions.inSampleSize = sample;
				bitmapOptions.inJustDecodeBounds = false;

				float percentage = (float) widthFinal / widthRaw;
				float proportionateHeight = heightRaw * percentage;
				heightFinal = (int) Math.rint(proportionateHeight);

				Bitmap bitmapSampled = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, bitmapOptions);

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
				return bitmapOriginalSize;
			}
		}
		else {
			widthFinal = Integer.parseInt(imageWidthMax);
			if (widthFinal > widthRaw) {
				bitmapOptions.inJustDecodeBounds = false;
				Bitmap bitmapOriginalSize = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, bitmapOptions);
				return bitmapOriginalSize;
			}
			else {
				int sample = 0;

				float fWidth = widthRaw;
				sample = new Double(Math.ceil(fWidth / 1200)).intValue();

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
		int resourceId = mActivity.getResources().getIdentifier(rawResourceName, "drawable", "com.proxibase.aircandi");
		if (resourceId == 0) {
			resourceId = mActivity.getResources().getIdentifier(rawResourceName, "attr", "com.proxibase.aircandi");
			TypedValue value = new TypedValue();
			if (mActivity.getTheme().resolveAttribute(resourceId, value, true)) {
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

	public String resolveCacheName(String imageUri) {
		if (imageUri.toLowerCase().startsWith("resource:", 0)) {
			String rawCacheName = imageUri.substring(imageUri.indexOf("resource:") + 9);

			int resourceId = mActivity.getResources().getIdentifier(rawCacheName, "drawable", "com.proxibase.aircandi");
			if (resourceId == 0) {
				resourceId = mActivity.getResources().getIdentifier(rawCacheName, "attr", "com.proxibase.aircandi");
				TypedValue value = new TypedValue();
				if (mActivity.getTheme().resolveAttribute(resourceId, value, true)) {
					String redirectedResourceName = (String) value.coerceToString();
					return redirectedResourceName;
				}
				else {
					/* We failed to resolve the resource name */
					throw new IllegalStateException("Resource not resolved: " + rawCacheName);
				}
			}
			else {
				return rawCacheName;
			}
		}
		else if (imageUri.toLowerCase().startsWith("asset:", 0)) {
			String rawCacheName = imageUri.substring(imageUri.indexOf("asset:") + 6);
			return rawCacheName;
		}
		else {
			return imageUri;
		}
	}

	public float rotationForImage(Context context, Uri uri) {
		if (uri.getScheme().equals("content")) {
			String[] projection = { Images.ImageColumns.ORIENTATION };
			Cursor c = context.getContentResolver().query(uri, projection, null, null, null);
			if (c.moveToFirst()) {
				return c.getInt(0);
			}
		}
		else if (uri.getScheme().equals("file")) {
			try {
				ExifInterface exif = new ExifInterface(uri.getPath());
				int rotation = (int) exifOrientationToDegrees(exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL));
				return rotation;
			}
			catch (IOException exception) {
				Exceptions.Handle(exception);
			}
		}
		return 0f;
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

	@SuppressWarnings("unused")
	private String getExifOrientation(String imagePath, String imageOrientation) {

		/*
		 * Return image EXIF orientation using reflection (if Android 2.0 or higher)
		 * http://developer.android.com/resources/articles/backward-compatibility.html
		 */
		Method exifGetAttribute;
		Constructor<ExifInterface> exifConstructor;
		String exifOrientation = "";

		int sdkInt = 0;
		try {
			sdkInt = Integer.valueOf(Build.VERSION.SDK);
		}
		catch (Exception exception) {
			sdkInt = 3; // assume they are on cupcake
		}
		if (sdkInt >= 5) {
			try {
				exifConstructor = ExifInterface.class.getConstructor(new Class[] { String.class });
				Object exif = exifConstructor.newInstance(imagePath);
				exifGetAttribute = ExifInterface.class.getMethod("getAttribute", new Class[] { String.class });

				try {
					exifOrientation = (String) exifGetAttribute.invoke(exif, ExifInterface.TAG_ORIENTATION);
					if (exifOrientation != null) {
						if (exifOrientation.equals("1")) {
							imageOrientation = "0";
						}
						else if (exifOrientation.equals("3")) {
							imageOrientation = "180";
						}
						else if (exifOrientation.equals("6")) {
							imageOrientation = "90";
						}
						else if (exifOrientation.equals("8")) {
							imageOrientation = "270";
						}
					}
					else {
						imageOrientation = "0";
					}
				}
				catch (InvocationTargetException exception) {

					/* Unpack original exception when possible */
					imageOrientation = "0";
				}
				catch (IllegalAccessException exception) {
					System.err.println("unexpected " + exception);
					imageOrientation = "0";
				}
				/* Success, this is a newer device */
			}
			catch (NoSuchMethodException exception) {
				imageOrientation = "0";
			}
			catch (IllegalArgumentException exception) {
				imageOrientation = "0";
			}
			catch (InstantiationException exception) {
				imageOrientation = "0";
			}
			catch (IllegalAccessException exception) {
				imageOrientation = "0";
			}
			catch (InvocationTargetException exception) {
				imageOrientation = "0";
			}
		}
		return imageOrientation;
	}

	public boolean hasImageCaptureBug() {
		/*
		 * List of known devices that have the bug where the
		 * OK button does nothing when using MediaStore.ACTION_IMAGE_CAPTURE intent.
		 * http://code.google.com/p/android/issues/detail?id=1480
		 * Nexus S fingerprint: google/soju/crespo
		 */
		ArrayList<String> devices = new ArrayList<String>();
		devices.add("android-devphone1/dream_devphone/dream");
		devices.add("google/soju/crespo");
		devices.add("generic/sdk/generic");
		devices.add("vodafone/vfpioneer/sapphire");
		devices.add("tmobile/kila/dream");
		devices.add("verizon/voles/sholes");
		devices.add("google_ion/google_ion/sapphire");
		return devices.contains(android.os.Build.BRAND + "/" + android.os.Build.PRODUCT + "/" + android.os.Build.DEVICE);
	}

	public static boolean isLocalImage(String imageUri) {
		if (imageUri.toLowerCase().contains("resource:"))
			return true;
		if (imageUri.toLowerCase().contains("asset:"))
			return true;
		return false;
	}

	// --------------------------------------------------------------------------------------------
	// Cache routines
	// --------------------------------------------------------------------------------------------

	public Bitmap getImage(String key) {
		return mImageCache.get(key);
	}

	public boolean hasImage(String key) {
		return mImageCache.containsKey(key);
	}

	public void deleteImage(String key) {
		mImageCache.remove(key);
	}

	public ImageCache getImageCache() {
		return mImageCache;
	}

	public void setImageCache(ImageCache imageCache) {
		mImageCache = imageCache;
		getImageLoader().setImageCache(imageCache);
	}

	// --------------------------------------------------------------------------------------------
	// Setters/Getters routines
	// --------------------------------------------------------------------------------------------

	public void setImageLoader(ImageLoader imageLoader) {
		this.mImageLoader = imageLoader;
	}

	public ImageLoader getImageLoader() {
		return mImageLoader;
	}

	public void setActivity(Activity activity) {
		mActivity = activity;
	}

	public Activity getActivity() {
		return mActivity;
	}

	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	// --------------------------------------------------------------------------------------------
	// Inner classes and enums
	// --------------------------------------------------------------------------------------------

	public void setDisplayMetrics(DisplayMetrics displayMetrics) {
		this.mDisplayMetrics = displayMetrics;
	}

	public DisplayMetrics getDisplayMetrics() {
		return mDisplayMetrics;
	}

	private interface IImageRequestListener {

		void onImageReady(Bitmap bitmap);

		void onProgressChanged(int progress);

		void onProxibaseException(ProxibaseException exception);
	}

	public static class ImageRequestListener implements IImageRequestListener {

		public void onImageReady(Bitmap bitmap) {}

		public void onProgressChanged(int progress) {}

		public void onProxibaseException(ProxibaseException exception) {
			Exceptions.Handle(exception);
		}
	}

	public static class ImageRequest {

		public String				imageUri;
		public ImageShape			imageShape			= ImageShape.Native;
		public ImageFormat			imageFormat;
		public Object				imageRequestor;
		public int					priority			= 1;
		public int					scaleToWidth;
		public boolean				makeReflection		= false;
		public boolean				javascriptEnabled	= false;
		public boolean				updateCache			= true;
		public boolean				searchCache			= true;
		public ImageRequestListener	imageReadyListener	= null;

		public ImageRequest(String imageUri, ImageShape imageShape, ImageFormat imageFormat, boolean javascriptEnabled, int scaleToWidth,
				boolean makeReflection, boolean searchCache, boolean updateCache, int priority,
				Object imageRequestor, ImageRequestListener imageReadyListener) {
			this.imageUri = imageUri;
			this.imageShape = imageShape;
			this.imageFormat = imageFormat;
			this.imageRequestor = imageRequestor;
			this.scaleToWidth = scaleToWidth;
			this.priority = priority;
			this.makeReflection = makeReflection;
			this.javascriptEnabled = javascriptEnabled;
			this.searchCache = searchCache;
			this.updateCache = updateCache;
			this.imageReadyListener = imageReadyListener;
		}

		public enum ImageFormat {
			Binary, Html, HtmlZoom
		}

		public enum ImageShape {
			Native, Square
		}
	}
}
