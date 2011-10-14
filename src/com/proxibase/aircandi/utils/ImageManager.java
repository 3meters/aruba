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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.anddev.andengine.util.StreamUtils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Picture;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.MediaStore.Images;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebView.PictureListener;

import com.proxibase.aircandi.core.AircandiException;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.core.AircandiException.CandiErrorCode;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest.ImageFormat;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest.ImageShape;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException.ProxiErrorCode;

/*
 * TODO: ImageCache uses weak references by default. Should investigate the possible benefits 
 * of switching to using soft references instead.
 */

public class ImageManager {

	private static ImageManager					singletonObject;

	private ImageCache							mImageCache;
	private ImageLoader							mImageLoader;
	private WebView								mWebView;
	private Queue								mWebViewQueue		= new LinkedList<ImageRequest>();
	private boolean								mWebViewProcessing	= false;
	private IImageRequestListener				mImageReadyListener;
	private static Context						mContext;

	@SuppressWarnings("unused")
	private HashMap<String, List<ImageRequest>>	mImageRequests		= new HashMap<String, List<ImageRequest>>();

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
		setImageLoader(new ImageLoader(mContext));
		getImageLoader().setImageCache(mImageCache);
		getImageLoader().setWebView(mWebView);
	}

	public static Bitmap fetchImage(String url) throws ProxibaseException {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = CandiConstants.IMAGE_CONFIG_DEFAULT;
		
		/*
		 * We request a byte array for decoding because of a bug
		 * in pre 2.3 versions of android.
		 */
		byte[] imageBytes = (byte[]) ProxibaseService.getInstance().select(url, ResponseFormat.Bytes);
		Bitmap bm = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);
		if (bm == null)
			throw new IllegalStateException("Stream could not be decoded to a bitmap: " + url);

		return bm;
	}

	public void fetchImageAsynch(ImageRequest imageRequest) throws AircandiException {

		/* Double check that we don't have it in the cache */
		Bitmap bitmap = mImageCache.get(imageRequest.imageUri);
		if (bitmap != null) {
			imageRequest.imageReadyListener.onImageReady(bitmap);
			return;
		}
		
		/* Make sure the cache directory is intact */
		if (!mImageCache.cacheDirectoryExists())
			mImageCache.makeCacheDirectory();

		if (imageRequest.imageFormat == ImageFormat.Html) {
			mWebViewQueue.offer(imageRequest);
			if (!mWebViewProcessing)
				startWebViewProcessing();
		}
		else if (imageRequest.imageFormat == ImageFormat.Binary) {
			try {
				new GetImageBinaryTask().execute(imageRequest);
			}
			catch (RejectedExecutionException exception) {
				throw new AircandiException(exception.getMessage(), CandiErrorCode.RejectedExecutionException, exception);
			}
		}
	}

	public class GetImageBinaryTask extends AsyncTask<ImageRequest, Void, Bitmap> {

		ImageRequest		mImageRequest;
		ProxibaseException	mProxibaseException;

		@Override
		protected Bitmap doInBackground(final ImageRequest... params) {

			/* We are on the background thread */
			mImageRequest = params[0];
			Bitmap bitmap = null;
			try {
				bitmap = fetchImage(mImageRequest.imageUri);
			}
			catch (ProxibaseException exception) {
				mProxibaseException = exception;
				return null;

			}

			if (bitmap != null) {

				Utilities.Log(CandiConstants.APP_NAME, "ImageManager", "Image download completed for image '" + mImageRequest.imageUri + "'");

				/* Crop if requested */
				Bitmap bitmapCropped;
				if (mImageRequest.imageShape == ImageShape.Square) {
					bitmapCropped = ImageUtils.cropToSquare(bitmap);
				}
				else {
					bitmapCropped = bitmap;
				}

				/* Scale if needed */
				Bitmap bitmapCroppedScaled;
				if (mImageRequest.scaleToWidth > 0 && bitmapCropped.getWidth() != mImageRequest.scaleToWidth) {
					float scalingRatio = (float) mImageRequest.scaleToWidth / (float) bitmapCropped.getWidth();
					float newHeight = (float) bitmapCropped.getHeight() * scalingRatio;
					bitmapCroppedScaled = Bitmap.createScaledBitmap(bitmapCropped, mImageRequest.scaleToWidth, (int) (newHeight), true);
				}
				else {
					bitmapCroppedScaled = bitmapCropped;
				}

				/* Make sure the bitmap format is right */
				Bitmap bitmapFinal;
				if (!bitmapCroppedScaled.getConfig().name().equals(CandiConstants.IMAGE_CONFIG_DEFAULT.toString())) {
					bitmapFinal = bitmapCroppedScaled.copy(CandiConstants.IMAGE_CONFIG_DEFAULT, false);
				}
				else {
					bitmapFinal = bitmapCroppedScaled;
				}

				/* Stuff it into the disk and memory caches. Overwrites if it already exists. */
				if (bitmapFinal.isRecycled())
					throw new IllegalArgumentException("bitmapFinal has been recycled");
				mImageCache.put(mImageRequest.imageUri, bitmapFinal);

				/* Create reflection if requested */
				if (mImageRequest.makeReflection) {
					final Bitmap bitmapReflection = ImageUtils.getReflection(bitmapFinal);
					mImageCache.put(mImageRequest.imageUri + ".reflection", bitmapReflection);
					if (mImageCache.isFileCacheOnly())
						bitmapReflection.recycle();
				}

				return bitmapFinal;
			}
			else {
				Utilities.Log(CandiConstants.APP_NAME, "ImageManager", "Image download failed for image '" + mImageRequest.imageUri + "'");
				return null;
			}
		}

		@Override
		protected void onPostExecute(final Bitmap bitmap) {

			/* We are on the main thread */
			super.onPostExecute(bitmap);

			if (mProxibaseException != null) {
				if (mImageRequest.imageReadyListener != null)
					mImageRequest.imageReadyListener.onProxibaseException(mProxibaseException);
				else if (mImageReadyListener != null) {
					mImageReadyListener.onProxibaseException(mProxibaseException);
				}
			}
			else {
				if (bitmap != null) {
					if (mImageRequest.imageReadyListener != null)
						mImageRequest.imageReadyListener.onImageReady(bitmap);
					else if (mImageReadyListener != null) {
						mImageReadyListener.onImageReady(bitmap);
					}
				}
			}
		}
	}

	public class GetImageHtmlTask extends AsyncTask<ImageRequest, Void, Bitmap> {

		ImageRequest		mImageRequest;
		ProxibaseException	mProxibaseException;

		@Override
		protected Bitmap doInBackground(final ImageRequest... params) {

			/* We are on the background thread */
			mImageRequest = params[0];
			String webViewContent = "";
			final AtomicBoolean ready = new AtomicBoolean(false);

			try {
				webViewContent = (String) ProxibaseService.getInstance().select(mImageRequest.imageUri, ResponseFormat.Html);
			}
			catch (ProxibaseException exception) {
				mProxibaseException = exception;
				return null;
			}
			mWebView.setWebViewClient(new WebViewClient() {

				@Override
				public void onPageFinished(WebView view, String url) {
					ready.set(true);
				}

			});
			mWebView.setPictureListener(new PictureListener() {

				@Override
				public void onNewPicture(WebView view, Picture picture) {

					if (ready.get()) {
						Bitmap bitmap = Bitmap.createBitmap(250, 250, CandiConstants.IMAGE_CONFIG_DEFAULT);
						Canvas canvas = new Canvas(bitmap);
						picture.draw(canvas);

						if (bitmap != null) {

							/* Stuff it into the disk and memory caches. Overwrites if it already exists. */
							mImageCache.put(mImageRequest.imageUri, bitmap);

							/* Create reflection if requested */
							if (mImageRequest.makeReflection) {
								Bitmap bitmapReflection = ImageUtils.getReflection(bitmap);
								mImageCache.put(mImageRequest.imageUri + ".reflection", bitmapReflection);
								if (mImageCache.isFileCacheOnly()) {
									bitmapReflection.recycle();
									bitmapReflection = null;
								}
							}

							/* Ready to return to original requestor */
							if (mImageRequest.imageReadyListener != null)
								mImageRequest.imageReadyListener.onImageReady(bitmap);
							else if (mImageReadyListener != null) {
								mImageReadyListener.onImageReady(bitmap);
							}
						}

						/* Release */
						canvas = null;
						mWebViewProcessing = false;
						try {
							startWebViewProcessing();
						}
						catch (AircandiException exception) {
							
							/* TODO: We might have hit the thread limit for AsyncTasks */
							exception.printStackTrace();
						}
					}
				}
			});

			mWebView.loadDataWithBaseURL(mImageRequest.imageUri, webViewContent, "text/html", "utf-8", null);
			return null;
		}
	}

	public Bitmap loadBitmapFromDevice(final Uri imageUri, String imageWidthMax) throws ProxibaseException {

		String[] projection = new String[] { Images.Thumbnails._ID, Images.Thumbnails.DATA, Images.Media.ORIENTATION };
		@SuppressWarnings("unused")
		String imagePath = "";
		File imageFile = null;
		int rotation = 0;

		Cursor cursor = mContext.getContentResolver().query(imageUri, projection, null, null, null);

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
				exception.printStackTrace();
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

	public static Bitmap loadBitmapFromAssets(final String assetPath) {
		InputStream in = null;
		try {
			final BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
			decodeOptions.inPreferredConfig = CandiConstants.IMAGE_CONFIG_DEFAULT;

			in = mContext.getAssets().open(assetPath);
			return BitmapFactory.decodeStream(in, null, decodeOptions);
		}
		catch (final IOException e) {
			e.printStackTrace();
			return null;
		}
		finally {
			StreamUtils.close(in);
		}
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

	public static float rotationForImage(Context context, Uri uri) {
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
			catch (IOException e) {
			}
		}
		return 0f;
	}

	private static float exifOrientationToDegrees(int exifOrientation) {
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
		catch (Exception e1) {
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

	public void startWebViewProcessing() throws AircandiException {
		mWebView.setPictureListener(null);
		ImageRequest imageRequest = (ImageRequest) mWebViewQueue.poll();
		if (imageRequest != null) {
			mWebViewProcessing = true;
			try {
				new GetImageHtmlTask().execute(imageRequest);
			}
			catch (RejectedExecutionException exception) {
				exception.printStackTrace();
				throw new AircandiException(exception.getMessage(), CandiErrorCode.RejectedExecutionException, exception);
			}
		}
	}

	public Bitmap getImage(String key) {
		return mImageCache.get(key);
	}

	public boolean hasImage(String key) {
		return mImageCache.containsKey(key);
	}

	public ImageCache getImageCache() {
		return mImageCache;
	}

	public void setImageCache(ImageCache imageCache) {
		mImageCache = imageCache;
		getImageLoader().setImageCache(imageCache);
	}

	public WebView getWebView() {
		return mWebView;
	}

	public void setWebView(WebView webView) {
		mWebView = webView;
	}

	public Queue getQueue() {
		return mWebViewQueue;
	}

	public void setContext(Context context) {
		mContext = context;
	}

	public void setImageLoader(ImageLoader imageLoader) {
		this.mImageLoader = imageLoader;
	}

	public ImageLoader getImageLoader() {
		return mImageLoader;
	}

	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	public interface IImageRequestListener {

		void onImageReady(Bitmap bitmap);

		void onProxibaseException(ProxibaseException exception);
	}

	public static class ImageRequest {

		public String					imageUri;
		public ImageShape				imageShape			= ImageShape.Native;
		public ImageFormat				imageFormat;
		public Object					imageRequestor;
		public int						priority			= 1;
		public int						scaleToWidth;
		public boolean					makeReflection		= false;
		public IImageRequestListener	imageReadyListener	= null;

		public ImageRequest(String imageUri, ImageShape imageShape, ImageFormat imageFormat, int scaleToWidth, boolean makeReflection, int priority,
				Object imageRequestor, IImageRequestListener imageReadyListener) {
			this.imageUri = imageUri;
			this.imageShape = imageShape;
			this.imageFormat = imageFormat;
			this.imageRequestor = imageRequestor;
			this.scaleToWidth = scaleToWidth;
			this.priority = priority;
			this.makeReflection = makeReflection;
			this.imageReadyListener = imageReadyListener;
		}

		public enum ImageFormat {
			Binary, Html
		}

		public enum ImageShape {
			Native, Square
		}
	}
}
