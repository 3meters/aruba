package com.proxibase.aircandi.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;
import android.graphics.Shader.TileMode;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.Toast;

import com.proxibase.aircandi.R;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.widgets.WebImageView;

public class ImageUtils {

	private static Paint			mPaint	= new Paint();
	//private static Canvas			mCanvas	= new Canvas();
	private static LinearGradient	mShader	= new LinearGradient(0, 0, 0, CandiConstants.CANDI_VIEW_REFLECTION_HEIGHT, 0x88ffffff, 0x00ffffff,
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

	public static int getRawPixelsForDisplayPixels(int displayPixels) {
		final float scale = ImageManager.getInstance().getDisplayMetrics().density;
		return (int) (displayPixels * scale + 0.5f);
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

		Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, xStart, yStart, xEnd, yEnd);
		return croppedBitmap;
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
		//LinearGradient shader = new LinearGradient(0, 0, 0, CandiConstants.CANDI_VIEW_REFLECTION_HEIGHT, 0x88ffffff, 0x00ffffff, TileMode.CLAMP);
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
		String r = Integer.toHexString(Color.red(color));
		String g = Integer.toHexString(Color.green(color));
		String b = Integer.toHexString(Color.blue(color));

		String hexColor = "#" + r + g + b;
		return hexColor;
	}

	public static void showImageInImageView(Bitmap bitmap, WebImageView imageView) {
		showImageInImageView(bitmap, null, imageView, null);
	}

	public static void showImageInImageView(Bitmap bitmap, Bitmap bitmapReflection, WebImageView imageView, ImageView imageViewReflection) {
		imageView.setImageBitmap(bitmap);
		Animation animation = AnimUtils.loadAnimation(R.anim.fade_in_medium);
		animation.setFillEnabled(true);
		animation.setFillAfter(true);
		imageView.startAnimation(animation);

		if (bitmapReflection != null && imageViewReflection != null) {
			imageViewReflection.setImageBitmap(bitmapReflection);
			imageViewReflection.startAnimation(animation);
		}
	}
}