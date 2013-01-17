package com.aircandi.utilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;
import android.widget.Toast;

import com.aircandi.Aircandi;
import com.aircandi.CandiConstants;
import com.aircandi.components.bitmaps.BitmapRequest;

public class ImageUtils {

	private static Paint			mPaint	= new Paint();
	private static LinearGradient	mShader	= new LinearGradient(0, 0, 0, CandiConstants.CANDI_VIEW_REFLECTION_HEIGHT, 0x80ffffff, 0x00ffffff,
													TileMode.CLAMP);

	public static Bitmap bitmapFromView(View view, int width, int height) {
		Bitmap bitmap = Bitmap.createBitmap(width, height, CandiConstants.IMAGE_CONFIG_DEFAULT);
		Canvas canvas = new Canvas(bitmap);
		view.draw(canvas);
		canvas = null;
		return bitmap;
	}

	public static void showToastNotification(final String message, final int duration) {
		Aircandi.mainThreadHandler.post(new Runnable() {

			@Override
			public void run() {
				CharSequence text = message;
				Toast toast = Toast.makeText(Aircandi.applicationContext, text, duration);
				toast.show();
			}
		});
	}

	public static void showToastNotification(final int messageId, final int duration) {
		Aircandi.mainThreadHandler.post(new Runnable() {

			@Override
			public void run() {
				Toast toast = Toast.makeText(Aircandi.applicationContext, messageId, duration);
				toast.show();
			}
		});
	}

	public static int getRawPixelsForDisplayPixels(int displayPixels) {
		if (Aircandi.displayMetrics != null) {
			final float scale = Aircandi.displayMetrics.density;
			return (int) (displayPixels * scale + 0.5f);
		}
		else {
			return (int) (displayPixels * 1.5f + 0.5f);
		}
	}

	/**
	 * Converts a immutable bitmap to a mutable bitmap. This operation doesn't allocates
	 * more memory that there is already allocated.
	 * 
	 * @param bitmap
	 *            - Source image. It will be released, and should not be used more
	 * @return a copy of imgIn, but muttable.
	 */
	public static Bitmap convertToMutable(Bitmap bitmap) {
		try {
			//this is the file going to use temporally to save the bytes. 
			// This file will not be a image, it will store the raw image data.
			File file = new File(Environment.getExternalStorageDirectory() + File.separator + "temp.tmp");

			//Open an RandomAccessFile
			//Make sure you have added uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
			//into AndroidManifest.xml file
			RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");

			// get the width and height of the source bitmap.
			int width = bitmap.getWidth();
			int height = bitmap.getHeight();
			Config type = bitmap.getConfig();

			//Copy the byte to the file
			//Assume source bitmap loaded using options.inPreferredConfig = Config.ARGB_8888;
			FileChannel channel = randomAccessFile.getChannel();
			MappedByteBuffer map = channel.map(MapMode.READ_WRITE, 0, bitmap.getRowBytes() * height);
			bitmap.copyPixelsToBuffer(map);
			//recycle the source bitmap, this will be no longer used.
			bitmap.recycle();
			//System.gc();// try to force the bytes from the imgIn to be released

			//Create a new bitmap to load the bitmap again. Probably the memory will be available. 
			bitmap = Bitmap.createBitmap(width, height, type);
			map.position(0);
			//load it back from temporary 
			bitmap.copyPixelsFromBuffer(map);
			//close the temporary file and channel , then delete that also
			channel.close();
			randomAccessFile.close();

			// delete the temp file
			file.delete();

		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		return bitmap;
	}

	@SuppressWarnings("unused")
	public static File saveBitmapToFile(Bitmap bitmap) {
		try {
			/* File to hold byte array */
			File file = new File(Environment.getExternalStorageDirectory() + File.separator + "temp.tmp");

			/* Open an RandomAccessFile */
			RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");

			int width = bitmap.getWidth();
			int height = bitmap.getHeight();
			Config type = bitmap.getConfig();

			/*
			 * Copy the byte to the file
			 * Assume source bitmap loaded using options.inPreferredConfig = Config.ARGB_8888;
			 */
			FileChannel channel = randomAccessFile.getChannel();
			MappedByteBuffer map = channel.map(MapMode.READ_WRITE, 0, bitmap.getRowBytes() * height);
			bitmap.copyPixelsToBuffer(map);
			bitmap.recycle();

			/* close the temporary file and channel */
			channel.close();
			randomAccessFile.close();

			return file;
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	public static int getRawPixels(Context context, String displayPixels) {
		displayPixels = displayPixels.replaceAll("[^\\d.]", "");
		float pixels = getRawPixels(context, (int) Float.parseFloat(displayPixels));
		return (int) pixels;
	}

	public static int getRawPixels(Context context, int displayPixels) {
		DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		int pixels = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (float) displayPixels, metrics);
		return pixels;
	}

	public static Bitmap replacePixels(Bitmap bitmap, String fromColorHex, String toColorHex) {
		int fromColor = hexToColor(fromColorHex);
		int toColor = hexToColor(toColorHex);
		return replacePixels(bitmap, fromColor, toColor);
	}

	public static Bitmap replacePixels(Bitmap bitmap, int fromColor, int toColor) {
		int fromRed = Color.red(fromColor);
		int fromGreen = Color.green(fromColor);
		int fromBlue = Color.blue(fromColor);

		int[] pixels = new int[bitmap.getHeight() * bitmap.getWidth()];
		bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

		for (int i = 0; i < bitmap.getHeight() * bitmap.getWidth(); i++) {
			int toRed = Color.red(pixels[i]);
			int toGreen = Color.green(pixels[i]);
			int toBlue = Color.blue(pixels[i]);

			if (fromRed == toRed && fromGreen == toGreen && fromBlue == toBlue) {
				pixels[i] = toColor;
			}
		}

		bitmap.setPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
		return bitmap;
	}

	public static Bitmap keepPixels(Bitmap bitmap, String keepColorHex) {
		int keepColor = hexToColor(keepColorHex);
		return keepPixels(bitmap, keepColor);
	}

	public static Bitmap keepPixels(Bitmap bitmap, int keepColor) {
		int keepRed = Color.red(keepColor);
		int keepGreen = Color.green(keepColor);
		int keepBlue = Color.blue(keepColor);

		int[] pixels = new int[bitmap.getHeight() * bitmap.getWidth()];
		bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

		//		for( int i = 0; i < pixels.length; i++ ) {
		//		    // Invert the red channel as an alpha bitmask for the desired color.
		//		    pixels[i] = ~( pixels[i] << 8 & 0xFF000000 ) & Color.BLACK;
		//		}		

		for (int i = 0; i < bitmap.getHeight() * bitmap.getWidth(); i++) {
			int toRed = Color.red(pixels[i]);
			int toGreen = Color.green(pixels[i]);
			int toBlue = Color.blue(pixels[i]);

			if (toRed != keepRed || toGreen != keepGreen || toBlue != keepBlue) {
				pixels[i] = 0;
			}
		}

		bitmap.setPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
		return bitmap;
	}

	public Bitmap toGrayscale(Bitmap bitmapOriginal)
	{
		int height = bitmapOriginal.getHeight();
		int width = bitmapOriginal.getWidth();

		Bitmap bitmapGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmapGrayscale);
		Paint paint = new Paint();
		ColorMatrix colorMatrix = new ColorMatrix();

		colorMatrix.setSaturation(0);
		ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
		paint.setColorFilter(filter);

		canvas.drawBitmap(bitmapOriginal, 0, 0, paint);
		return bitmapGrayscale;
	}

	public static int getColorBySignalLevel(int signalLevel, int alpha) {
		/*
		 * hue: 0 = red, 120 = green
		 */
		float levelMax = 100;
		float levelMin = 50;
		float signalLevelAbs = (float) Math.abs(signalLevel);
		if (signalLevelAbs > levelMax) {
			signalLevelAbs = levelMax;
		}
		else if (signalLevelAbs < levelMin) {
			signalLevelAbs = levelMin;
		}

		float signalLevelPcnt = (signalLevelAbs - levelMin) / (levelMax - levelMin);

		float[] hsv = new float[] { (120 - (120 * signalLevelPcnt)), 1.0f, 0.85f };
		return Color.HSVToColor(hsv);
	}

	public static int getImageMemorySize(int height, int width, boolean hasAlpha) {
		return height * width * (hasAlpha ? 4 : 3);
	}

	public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, int pixels) {
		Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap
				.getHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(output);

		final int color = 0xff424242;
		final Paint paint = new Paint();
		final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
		final RectF rectF = new RectF(rect);
		final float roundPx = pixels;

		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(bitmap, rect, rect, paint);

		return output;
	}

	public static Bitmap cropToSquare(Bitmap bitmap) {
		int height = bitmap.getHeight();
		int width = bitmap.getWidth();
		int xStart = 0;
		int yStart = 0;
		int xEnd = width;
		int yEnd = height;

		if (height > width) {
			int diff = height - width;
			yStart = yStart + (diff / 2);
			yEnd = width;
		}
		else if (width > height) {
			int diff = width - height;
			xStart = xStart + (diff / 2);
			xEnd = height;
		}

		Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, xStart, yStart, xEnd, yEnd, null, true);
		return croppedBitmap;
	}

	public static Bitmap scaleAndCropBitmap(Bitmap bitmap, BitmapRequest bitmapRequest) {
		/*
		 * Only being used when sending an image to S3
		 */
		return scaleAndCropBitmap(bitmap, 500);
	}

	public static Bitmap scaleAndCropBitmap(Bitmap bitmap, int scaleToWidth) {

		/* Scale if needed */
		Bitmap bitmapScaled = bitmap;
		if (scaleToWidth > 0) {
			boolean portrait = bitmap.getHeight() > bitmap.getWidth();
			if (portrait) {
				if (bitmap.getWidth() != scaleToWidth) {
					float scalingRatio = (float) scaleToWidth / (float) bitmap.getWidth();
					float newHeight = (float) bitmap.getHeight() * scalingRatio;
					bitmapScaled = Bitmap.createScaledBitmap(bitmap, scaleToWidth, (int) (newHeight), true);
					if (!bitmapScaled.equals(bitmap)) {
						bitmap.recycle();
					}
				}
			}
			else {
				if (bitmap.getHeight() != scaleToWidth) {
					float scalingRatio = (float) scaleToWidth / (float) bitmap.getHeight();
					float newWidth = (float) bitmap.getWidth() * scalingRatio;
					bitmapScaled = Bitmap.createScaledBitmap(bitmap, (int) (newWidth), scaleToWidth, true);
					if (!bitmapScaled.equals(bitmap)) {
						bitmap.recycle();
					}
				}
			}
		}

		/* Make sure the bitmap format is right */
		Bitmap bitmapFinal = configBitmap(bitmapScaled);

		if (bitmapFinal.isRecycled()) {
			throw new IllegalArgumentException("bitmapFinal has been recycled");
		}

		return bitmapFinal;
	}

	public static Bitmap scaleBitmap(Bitmap bitmap, int scaleToWidth) {

		/* Scale if needed */
		Bitmap bitmapScaled = bitmap;
		if (scaleToWidth > 0) {
			boolean portrait = bitmap.getHeight() > bitmap.getWidth();
			if (portrait) {
				if (bitmap.getWidth() != scaleToWidth) {
					float scalingRatio = (float) scaleToWidth / (float) bitmap.getWidth();
					float newHeight = (float) bitmap.getHeight() * scalingRatio;
					bitmapScaled = Bitmap.createScaledBitmap(bitmap, scaleToWidth, (int) (newHeight), true);
					if (!bitmapScaled.equals(bitmap)) {
						bitmap.recycle();
					}
				}
			}
			else {
				if (bitmap.getHeight() != scaleToWidth) {
					float scalingRatio = (float) scaleToWidth / (float) bitmap.getHeight();
					float newWidth = (float) bitmap.getWidth() * scalingRatio;
					bitmapScaled = Bitmap.createScaledBitmap(bitmap, (int) (newWidth), scaleToWidth, true);
					if (!bitmapScaled.equals(bitmap)) {
						bitmap.recycle();
					}
				}
			}
		}

		return bitmapScaled;
	}

	public static Bitmap ensureBitmapScaleForS3(Bitmap bitmap) {
		Bitmap bitmapScaled = bitmap;
		Boolean scalingNeeded = (bitmap.getWidth() > CandiConstants.IMAGE_DIMENSION_MAX && bitmap.getHeight() > CandiConstants.IMAGE_DIMENSION_MAX);
		if (scalingNeeded) {

			Matrix matrix = new Matrix();
			float scalingRatio = Math.max((float) CandiConstants.IMAGE_DIMENSION_MAX / (float) bitmap.getWidth(), (float) CandiConstants.IMAGE_DIMENSION_MAX
					/ (float) bitmap.getHeight());
			matrix.postScale(scalingRatio, scalingRatio);
			/*
			 * Create a new bitmap from the original using the matrix to transform the result.
			 * Potential for OM condition because if the garbage collector is behind, we could
			 * have several large bitmaps in memory at the same time.
			 */
			bitmapScaled = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
		}
		return bitmapScaled;
	}

	public static Bitmap configBitmap(Bitmap bitmap) {
		/* Make sure the bitmap format is right */
		Bitmap bitmapModified = bitmap;
		if (!bitmap.getConfig().name().equals(CandiConstants.IMAGE_CONFIG_DEFAULT.toString())) {
			bitmapModified = bitmap.copy(CandiConstants.IMAGE_CONFIG_DEFAULT, false);
			if (!bitmapModified.equals(bitmap)) {
				bitmap.recycle();
			}
		}
		return bitmapModified;
	}

	public static Bitmap makeReflection(Bitmap originalBitmap, boolean applyReflectionGradient) {

		int width = originalBitmap.getWidth();
		int height = originalBitmap.getHeight();

		/* This will not scale but will flip on the Y axis */
		Matrix matrix = new Matrix();
		matrix.preScale(1, -1);

		/*
		 * Create a Bitmap with the flip matrix applied to it.
		 * We only want the bottom half of the image
		 */
		Bitmap reflectionImage = Bitmap.createBitmap(originalBitmap, 0, height / 2, width, height / 2, matrix, false);

		/* Create a new bitmap with same width but taller to fit reflection */
		Bitmap reflectionBitmap = Bitmap.createBitmap(width, (height / 2), CandiConstants.IMAGE_CONFIG_DEFAULT);

		Canvas canvas = new Canvas();

		canvas.setBitmap(reflectionBitmap);

		/* Draw in the reflection */
		canvas.drawBitmap(reflectionImage, 0, 0, null);

		/* Apply reflection gradient */
		if (applyReflectionGradient) {
			applyReflectionGradient(reflectionBitmap, Mode.DST_IN);
		}

		/* Stash the image with reflection */
		return reflectionBitmap;
	}

	public static void applyReflectionGradient(Bitmap reflectionBitmap, Mode mode) {

		Canvas canvas = new Canvas();

		canvas.setBitmap(reflectionBitmap);

		/* Set the paint to use this shader (linear gradient) */
		mPaint.setShader(mShader);

		/* Set the Transfer mode to be porter duff and destination in */
		mPaint.setXfermode(new PorterDuffXfermode(mode));

		/* Draw a rectangle using the paint with our linear gradient */
		canvas.drawRect(0, 0, reflectionBitmap.getWidth(), reflectionBitmap.getHeight(), mPaint);
	}

	public static int hexToColor(String hex) {
		int color = Color.parseColor(hex);
		return color;
	}

	public static String colorToHex(int color) {
		String hexColor = String.format("#%06X", (0xFFFFFF & color));
		//		String r = Integer.toHexString(Color.red(color));
		//		String g = Integer.toHexString(Color.green(color));
		//		String b = Integer.toHexString(Color.blue(color));
		//
		//		String hexColor = "#" + r + g + b;
		return hexColor;
	}

	public static void showImageInImageView(Bitmap bitmap, final ImageView imageView, boolean animate, Animation animation) {
		imageView.setImageBitmap(bitmap);
		if (animate) {
			animation.setFillEnabled(true);
			animation.setFillAfter(true);
			animation.setAnimationListener(new AnimationListener() {

				@Override
				public void onAnimationStart(Animation animation) {}

				@Override
				public void onAnimationEnd(Animation animation) {
					imageView.clearAnimation();
				}

				@Override
				public void onAnimationRepeat(Animation animation) {}
			});
			imageView.startAnimation(animation);
		}
	}

	public static void clearImageInImageView(ImageView imageView, boolean animate, Animation animation) {
		if (animate) {
			animation.setFillEnabled(true);
			animation.setFillAfter(true);
			imageView.startAnimation(animation);
		}
		else {
			imageView.setAnimation(null);
			imageView.setImageBitmap(null);
		}
	}

	public static void showDrawableInImageView(Drawable drawable, ImageView imageView, boolean animate, Animation animation) {
		if (imageView != null) {
			imageView.setImageDrawable(drawable);
			if (animate) {
				animation.setFillEnabled(true);
				animation.setFillAfter(true);
				imageView.startAnimation(animation);
			}
			imageView.postInvalidate();
		}
	}
}