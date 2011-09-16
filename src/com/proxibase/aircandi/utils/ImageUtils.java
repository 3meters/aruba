package com.proxibase.aircandi.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.PorterDuff.Mode;
import android.graphics.Shader.TileMode;
import android.util.DisplayMetrics;
import android.widget.Toast;

import com.proxibase.aircandi.core.CandiConstants;

public class ImageUtils {

	private static Paint			mPaint	= new Paint();
	private static LinearGradient	mShader	= new LinearGradient(0, 0, 0, CandiConstants.CANDI_VIEW_REFLECTION_HEIGHT, 0x70ffffff, 0x00ffffff,
													TileMode.CLAMP);

	public static void showToastNotification(Context context, String message, int duration) {
		CharSequence text = message;
		Toast toast = Toast.makeText(context, text, duration);
		toast.show();
	}

	public static void showToastNotification(Context context, int messageId, int duration) {
		Toast toast = Toast.makeText(context, messageId, duration);
		toast.show();
	}
	
	public static int getRawPixelsForDisplayPixels(DisplayMetrics displayMetrics, int displayPixels)
	{
		final float scale = displayMetrics.density;
		return (int) (displayPixels * scale + 0.5f);
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

		Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, xStart, yStart, xEnd, yEnd);
		return croppedBitmap;
	}

	public static Bitmap getReflection(Bitmap originalBitmap) {

		int width = originalBitmap.getWidth();
		int height = originalBitmap.getHeight();

		// This will not scale but will flip on the Y axis
		Matrix matrix = new Matrix();
		matrix.preScale(1, -1);

		// Create a Bitmap with the flip matrix applied to it.
		// We only want the bottom half of the image
		Bitmap reflectionImage = Bitmap.createBitmap(originalBitmap, 0, height / 2, width, height / 2, matrix, false);

		// Create a new bitmap with same width but taller to fit reflection
		Bitmap reflectionBitmap = Bitmap.createBitmap(width, (height / 2), CandiConstants.IMAGE_CONFIG_DEFAULT);

		// Create a new Canvas with the bitmap that's big enough for
		// the image plus gap plus reflection
		Canvas canvas = new Canvas(reflectionBitmap);

		// Draw in the reflection
		canvas.drawBitmap(reflectionImage, 0, 0, null);

		// Set the paint to use this shader (linear gradient)
		mPaint.setShader(mShader);

		// Set the Transfer mode to be porter duff and destination in
		mPaint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));

		// Draw a rectangle using the paint with our linear gradient
		canvas.drawRect(0, 0, width, reflectionBitmap.getHeight(), mPaint);

		// Release
		reflectionImage.recycle();
		reflectionImage = null;
		canvas = null;

		// Stash the image with reflection
		return reflectionBitmap;
	}

	public static int hexToColor(String hex) {
		int color = Color.parseColor(hex);
		return color;
	}

	public static String colorToHex(int color) {
		String r = Integer.toHexString(Color.red(color));
		String g = Integer.toHexString(Color.green(color));
		String b = Integer.toHexString(Color.blue(color));

		String hexColor = "#" + r + g + b;
		return hexColor;
	}

}