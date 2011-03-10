package com.georain.ripple.controller;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.client.ClientProtocolException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;
import android.graphics.Shader.TileMode;
import android.widget.Toast;

import com.threemeters.sdk.android.core.RippleService;
import com.threemeters.sdk.android.core.RippleService.QueryFormat;

public class RippleUI
{
	public static void showToastNotification(Context context, String message, int duration)
	{
		CharSequence text = message;
		Toast toast = Toast.makeText(context, text, duration);
		toast.show();
	}

	public static void showToastNotification(Context context, int messageId, int duration)
	{
		Toast toast = Toast.makeText(context, messageId, duration);
		toast.show();
	}

	public static Bitmap getImage(String url)
	{
		RippleService ripple = new RippleService();
		try
		{
			InputStream stream = ripple.getStream(url, QueryFormat.Xml);
			Bitmap bm = BitmapFactory.decodeStream(stream);
			return bm;
		}
		catch (ClientProtocolException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	public static Bitmap cropToSquare(Bitmap bitmap)
	{
		int height = bitmap.getHeight();
		int width = bitmap.getWidth();
		int xStart = 0;
		int yStart = 0;
		int xEnd = width;
		int yEnd = height;
		if (height > width)
		{
			int diff = height - width;
			yStart = yStart + (diff / 2);
			yEnd = width;
		}
		else if (width > height)
		{
			int diff = width - height;
			xStart = xStart + (diff / 2);
			xEnd = height;
		}

		Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, xStart, yStart, xEnd, yEnd);
		return croppedBitmap;
	}

	public static Bitmap createReflectedImages(Bitmap originalImage)
	{
		// The gap we want between the reflection and the original image
		final int reflectionGap = 0;

		int width = originalImage.getWidth();
		int height = originalImage.getHeight();

		// This will not scale but will flip on the Y axis
		Matrix matrix = new Matrix();
		matrix.preScale(1, -1);

		// Create a Bitmap with the flip matrix applied to it.
		// We only want the bottom half of the image
		Bitmap reflectionImage = Bitmap.createBitmap(originalImage, 0, height / 2, width, height / 2, matrix, false);

		// Create a new bitmap with same width but taller to fit reflection
		Bitmap bitmapWithReflection = Bitmap.createBitmap(width, (height + height / 2), Config.ARGB_8888);

		// Create a new Canvas with the bitmap that's big enough for
		// the image plus gap plus reflection
		Canvas canvas = new Canvas(bitmapWithReflection);

		// Draw in the original image
		canvas.drawBitmap(originalImage, 0, 0, null);

		// Draw in the gap
		Paint deafaultPaint = new Paint();
		canvas.drawRect(0, height, width, height + reflectionGap, deafaultPaint);

		// Draw in the reflection
		canvas.drawBitmap(reflectionImage, 0, height + reflectionGap, null);

		// Create a shader that is a linear gradient that covers the reflection
		Paint paint = new Paint();
		LinearGradient shader = new LinearGradient(0, originalImage.getHeight(), 0, bitmapWithReflection.getHeight() + reflectionGap, 0x70ffffff,
				0x00ffffff, TileMode.CLAMP);

		// Set the paint to use this shader (linear gradient)
		paint.setShader(shader);

		// Set the Transfer mode to be porter duff and destination in
		paint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));

		// Draw a rectangle using the paint with our linear gradient
		canvas.drawRect(0, height, width, bitmapWithReflection.getHeight() + reflectionGap, paint);

		// Stash the image with reflection
		return bitmapWithReflection;
	}

	public static int gradient(int startColor, int endColor, float index)
	{
		// index should be a desired percentage of the linear transform from
		// start color to end color

		float indexRedSteps = Math.abs(Color.red(startColor) - Color.red(endColor)) * index;
		float indexGreenSteps = Math.abs(Color.green(startColor) - Color.green(endColor)) * index;
		float indexBlueSteps = Math.abs(Color.blue(startColor) - Color.blue(endColor)) * index;

		float gradientRed = Color.red(startColor) > Color.red(endColor)	? Color.red(startColor) - indexRedSteps
																		: Color.red(startColor) + indexRedSteps;
		float gradientGreen = Color.green(startColor) > Color.green(endColor)	? Color.green(startColor) - indexGreenSteps
																				: Color.green(startColor) + indexGreenSteps;
		float gradientBlue = Color.blue(startColor) > Color.blue(endColor)	? Color.blue(startColor) - indexBlueSteps
																			: Color.blue(startColor) + indexBlueSteps;

		int gradientColor = Color.argb(255, (int) gradientRed, (int) gradientGreen, (int) gradientBlue);

		return gradientColor;
	}

	public static int hexToColor(String hex)
	{
		int color = Color.parseColor(hex);
		return color;
	}

	public static String colorToHex(int color)
	{
		String r = Integer.toHexString(Color.red(color));
		String g = Integer.toHexString(Color.green(color));
		String b = Integer.toHexString(Color.blue(color));

		String hexColor = "#" + r + g + b;
		return hexColor;
	}

}