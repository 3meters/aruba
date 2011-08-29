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

import com.proxibase.aircandi.candi.utils.CandiConstants;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;

/*
 * TODO: ImageCache uses weak references by default. Should investigate the possible benefits 
 * of switching to using soft references instead.
 */

public class ImageManager {

	private static ImageManager					singletonObject;
	@SuppressWarnings("unused")
	private static String						USER_AGENT			= "Mozilla/5.0 (Linux; U; Android 2.2.1; fr-ch; A43 Build/FROYO) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1";
	private ImageCache							mImageCache;
	private WebView								mWebView;
	private Queue								mWebViewQueue		= new LinkedList<ImageRequest>();
	private boolean								mWebViewProcessing	= false;
	private IImageReadyListener					mImageReadyListener;
	private static Context						mContext;
	@SuppressWarnings("unused")
	private HashMap<String, List<ImageRequest>>	mImageRequests		= new HashMap<String, List<ImageRequest>>();

	public static synchronized ImageManager getInstance() {

		if (singletonObject == null) {
			singletonObject = new ImageManager();
		}
		return singletonObject;
	}

	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	/**
	 * Designed as a singleton. The private Constructor prevents any other class from instantiating.
	 */
	private ImageManager() {}

	public ImageCache getImageCache() {
		return mImageCache;
	}

	public void setImageCache(ImageCache mImageCache) {
		this.mImageCache = mImageCache;
	}

	public Bitmap getImage(String key) {
		return mImageCache.get(key);
	}

	public boolean hasImage(String key) {
		return mImageCache.containsKey(key);
	}

	public void setContext(Context context) {
		mContext = context;
	}

	public Context getContext() {
		return mContext;
	}

	public void fetchImageAsynch(ImageRequest imageRequest) {
		
		// Make sure the cache directory is intact
		if (!mImageCache.cacheDirectoryExists())
			mImageCache.makeCacheDirectory();
		
		if (imageRequest.imageFormat == ImageFormat.Html) {
			mWebViewQueue.offer(imageRequest);
			if (!mWebViewProcessing)
				startWebViewProcessing();
		}
		else if (imageRequest.imageFormat == ImageFormat.Binary) {
			new GetImageBinaryTask().execute(imageRequest);
		}
	}

	public void startWebViewProcessing() {
		mWebView.setPictureListener(null);
		ImageRequest imageRequest = (ImageRequest) mWebViewQueue.poll();
		if (imageRequest != null) {
			mWebViewProcessing = true;
			new GetImageHtmlTask().execute(imageRequest);
		}
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

	public Bitmap bitmapForByteArray(byte[] imageBytes, String imageWidthMax, String imageOrientation) {

		BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
		bitmapOptions.inJustDecodeBounds = true;
		bitmapOptions.inPreferredConfig = CandiConstants.IMAGE_CONFIG_DEFAULT;

		// Initial decode is just to get the bitmap dimensions
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

				// Release
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

				// Create a matrix for the manipulation
				Matrix matrix = new Matrix();

				// Resize the bitmap
				matrix.postScale(scaleBy, scaleBy);
				if ((imageOrientation != null) && (imageOrientation.equals("90") || imageOrientation.equals("180") || imageOrientation.equals("270"))) {
					matrix.postRotate(Integer.valueOf(imageOrientation));
				}

				Bitmap bitmapSampledAndScaled = Bitmap.createBitmap(bitmapSampled, 0, 0, bitmapSampled.getWidth(), bitmapSampled.getHeight(), matrix,
						true);

				ByteArrayOutputStream bitmapByteArrayOutputStream = new ByteArrayOutputStream();
				bitmapSampledAndScaled.compress(Bitmap.CompressFormat.JPEG, 85, bitmapByteArrayOutputStream);

				Bitmap bitmapSampledScaledCompressed = BitmapFactory.decodeByteArray(bitmapByteArrayOutputStream.toByteArray(), 0,
						bitmapByteArrayOutputStream.toByteArray().length);

				// Release
				bitmapSampled.recycle();
				bitmapSampledAndScaled.recycle();

				return bitmapSampledScaledCompressed;
			}
		}
	}

	public Bitmap loadBitmapFromDevice(final Uri imageUri, String imageWidthMax) {

		String[] projection = new String[] { Images.Thumbnails._ID, Images.Thumbnails.DATA, Images.Media.ORIENTATION };
		String imageOrientation = "";
		@SuppressWarnings("unused")
		String imagePath = "";
		File imageFile = null;

		Cursor cursor = mContext.getContentResolver().query(imageUri, projection, null, null, null);

		if (cursor != null) {
			/*
			 * Means the image is in the media store
			 */
			String imageData = "";
			if (cursor.moveToFirst()) {
				int dataColumn = cursor.getColumnIndex(Images.Media.DATA);
				int orientationColumn = cursor.getColumnIndex(Images.Media.ORIENTATION);
				imageData = cursor.getString(dataColumn);
				imageOrientation = cursor.getString(orientationColumn);
			}

			imageFile = new File(imageData);
			imagePath = imageData;
		}
		else {
			/*
			 * The image is in the local file system
			 */
			imagePath = imageUri.toString().replace("file://", "");
			imageFile = new File(imageUri.toString().replace("file://", ""));
		}

		byte[] imageBytes = new byte[(int) imageFile.length()];

		DataInputStream in = null;

		try {
			in = new DataInputStream(new FileInputStream(imageFile));
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		try {
			in.readFully(imageBytes);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		try {
			in.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		imageOrientation = getExifOrientation(imageUri.getPath(), imageOrientation);
		Bitmap bitmap = this.bitmapForByteArray(imageBytes, imageWidthMax, imageOrientation);
		return bitmap;
	}

	public class GetImageHtmlTask extends AsyncTask<ImageRequest, Void, Bitmap> {

		ImageRequest	mImageRequest;

		@Override
		protected Bitmap doInBackground(final ImageRequest... params) {

			// We are on the background thread
			mImageRequest = params[0];
			String webViewContent = "";
			final AtomicBoolean ready = new AtomicBoolean(false);

			webViewContent = ProxibaseService.getInstance().selectAsString(mImageRequest.imageUri, ResponseFormat.Html);
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

							// Stuff it into the disk and memory caches. Overwrites if it already exists.
							mImageCache.put(mImageRequest.imageId, bitmap);

							// Create reflection if requested
							if (mImageRequest.showReflection) {
								Bitmap bitmapReflection = AircandiUI.getReflection(bitmap);
								mImageCache.put(mImageRequest.imageId + ".reflection", bitmapReflection);
								if (mImageCache.isFileCacheOnly()) {
									bitmapReflection.recycle();
									bitmapReflection = null;
								}
							}

							// Ready to return to original requestor
							if (mImageRequest.imageReadyListener != null)
								mImageRequest.imageReadyListener.onImageReady(bitmap);
							else if (mImageReadyListener != null) {
								mImageReadyListener.onImageReady(bitmap);
							}
						}

						// Release
						canvas = null;
						mWebViewProcessing = false;
						startWebViewProcessing();
					}
				}
			});

			mWebView.loadDataWithBaseURL(mImageRequest.imageUri, webViewContent, "text/html", "utf-8", null);
			return null;
		}
	}

	public String getExifOrientation(String imagePath, String imageOrientation) {
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
					// Unpack original exception when possible
					imageOrientation = "0";
				}
				catch (IllegalAccessException exception) {
					System.err.println("unexpected " + exception);
					imageOrientation = "0";
				}
				// Success, this is a newer device
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
		// list of known devices that have the bug
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

	public class GetImageBinaryTask extends AsyncTask<ImageRequest, Void, Bitmap> {

		ImageRequest	mImageRequest;

		@Override
		protected Bitmap doInBackground(final ImageRequest... params) {

			// We are on the background thread
			mImageRequest = params[0];
			Bitmap bitmap = AircandiUI.getImage(mImageRequest.imageUri);

			if (bitmap != null) {

				Utilities.Log(CandiConstants.APP_NAME, "ImageManager", "Image download completed for image '" + mImageRequest.imageId + "'");

				// Crop if requested
				Bitmap bitmapCropped;
				if (mImageRequest.imageShape.equals("square")) {
					bitmapCropped = AircandiUI.cropToSquare(bitmap);
				}
				else {
					bitmapCropped = bitmap;
				}

				// Scale if needed
				Bitmap bitmapCroppedScaled;
				if (mImageRequest.widthMinimum > 0 && bitmapCropped.getWidth() != mImageRequest.widthMinimum) {
					float scalingRatio = (float) mImageRequest.widthMinimum / (float) bitmapCropped.getWidth();
					float newHeight = (float) bitmapCropped.getHeight() * scalingRatio;
					bitmapCroppedScaled = Bitmap.createScaledBitmap(bitmapCropped, mImageRequest.widthMinimum, (int) (newHeight), true);
				}
				else {
					bitmapCroppedScaled = bitmapCropped;
				}

				// Make sure the bitmap format is right
				Bitmap bitmapFinal;
				if (!bitmapCroppedScaled.getConfig().name().equals(CandiConstants.IMAGE_CONFIG_DEFAULT.toString())) {
					bitmapFinal = bitmapCroppedScaled.copy(CandiConstants.IMAGE_CONFIG_DEFAULT, false);
				}
				else {
					bitmapFinal = bitmapCroppedScaled;
				}

				// Stuff it into the disk and memory caches. Overwrites if it already exists.
				if (bitmapFinal.isRecycled())
					throw new IllegalArgumentException("bitmapFinal has been recycled");
				mImageCache.put(mImageRequest.imageId, bitmapFinal);

				// Create reflection if requested
				if (mImageRequest.showReflection) {
					final Bitmap bitmapReflection = AircandiUI.getReflection(bitmapFinal);
					mImageCache.put(mImageRequest.imageId + ".reflection", bitmapReflection);
					if (mImageCache.isFileCacheOnly())
						bitmapReflection.recycle();
				}

				return bitmapFinal;
			}
			else {
				Utilities.Log(CandiConstants.APP_NAME, "ImageManager", "Image download failed for image '" + mImageRequest.imageId + "'");
				return null;
			}
		}

		@Override
		protected void onPostExecute(final Bitmap bitmap) {

			// We are on the main thread
			super.onPostExecute(bitmap);

			if (bitmap != null) {
				if (mImageRequest.imageReadyListener != null)
					mImageRequest.imageReadyListener.onImageReady(bitmap);
				else if (mImageReadyListener != null) {
					mImageReadyListener.onImageReady(bitmap);
				}
			}
		}
	}

	public void setOnImageReadyListener(IImageReadyListener listener) {
		mImageReadyListener = listener;
	}

	public final IImageReadyListener getOnImageReadyListener() {
		return mImageReadyListener;
	}

	public void setWebView(WebView webView) {
		this.mWebView = webView;
	}

	public WebView getWebView() {
		return mWebView;
	}

	public Queue getQueue() {
		return mWebViewQueue;
	}

	public interface IImageReadyListener {

		void onImageReady(Bitmap bitmap);
	}

	public static class ImageRequest {

		public String				imageUri;
		public String				imageId;
		public String				imageShape			= "native";
		public ImageFormat			imageFormat;
		public int					widthMinimum;
		public boolean				showReflection		= false;
		public IImageReadyListener	imageReadyListener	= null;
	}

	public static ImageRequest createImageRequest(String imageUri, ImageFormat imageFormat, String imageShape, int minWidth, boolean makeReflection, IImageReadyListener imageReadyListener) {
		ImageRequest imageRequest = new ImageRequest();
		imageRequest.imageId = imageUri;
		imageRequest.imageUri = imageUri;
		imageRequest.imageFormat = imageFormat;
		imageRequest.imageShape = imageShape;
		imageRequest.widthMinimum = minWidth;
		imageRequest.showReflection = makeReflection;
		imageRequest.imageReadyListener = imageReadyListener;
		return imageRequest;
	}

	public enum ImageFormat {
		Binary, Html
	}
}
