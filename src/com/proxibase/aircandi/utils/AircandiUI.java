package com.proxibase.aircandi.utils;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.client.ClientProtocolException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.PorterDuff.Mode;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Toast;

import com.proxibase.aircandi.candi.utils.CandiConstants;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;

public class AircandiUI {
	
	private static Paint mPaint = new Paint();
	private static LinearGradient mShader = new LinearGradient(0, 0, 0, CandiConstants.CANDI_VIEW_REFLECTION_HEIGHT, 0x70ffffff, 0x00ffffff, TileMode.CLAMP);

	public static void showToastNotification(Context context, String message, int duration) {
		CharSequence text = message;
		Toast toast = Toast.makeText(context, text, duration);
		toast.show();
	}

	public static void showToastNotification(Context context, int messageId, int duration) {
		Toast toast = Toast.makeText(context, messageId, duration);
		toast.show();
	}

	public static Bitmap getImage(String url) {
		InputStream inputStream = null;
		try {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inPreferredConfig = CandiConstants.IMAGE_CONFIG_DEFAULT;

			inputStream = ProxibaseService.getInstance().selectAsStream(url, ResponseFormat.Xml);
			
			if (inputStream == null)
				throw new IllegalStateException("Null stream returned when downloading an image");
			
			/*
			 * We convert the stream to a byte array for decoding because of a bug 
			 * in pre 2.3 versions of android.
			 */
			byte[] imageBytes = getBytes(inputStream);
			Bitmap bm = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);
			if (bm == null)
				throw new IllegalStateException("Stream could not be decoded to a bitmap: " + url);
			
			return bm;
		}
		catch (ClientProtocolException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			try {
				inputStream.close();
			}
			catch (IOException exception) {
				exception.printStackTrace();
			}
		}
		return null;
	}

	public static byte[] getImageAsBytes(String url) {
		InputStream stream = null;
		try {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inPreferredConfig = CandiConstants.IMAGE_CONFIG_DEFAULT;

			stream = ProxibaseService.getInstance().selectAsStream(url, ResponseFormat.Xml);
			
			byte[] imageBytes = getBytes(stream);
			return imageBytes;
		}
		catch (ClientProtocolException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			try {
				stream.close();
			}
			catch (IOException exception) {
				exception.printStackTrace();
			}
		}
		return null;
	}

	public static byte[] getBytes(InputStream inputStream) throws IOException {

		int len;
		int size = 1024;
		byte[] buf;

		if (inputStream instanceof ByteArrayInputStream) {
			size = inputStream.available();
			buf = new byte[size];
			len = inputStream.read(buf, 0, size);
		}
		else {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			buf = new byte[size];
			while ((len = inputStream.read(buf, 0, size)) != -1)
				bos.write(buf, 0, len);
			buf = bos.toByteArray();
		}
		return buf;
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

	/**
	 * Loads an {@link Animation} object from a resource
	 * 
	 * @param context Application context used to access resources
	 * @param id The resource id of the animation to load
	 * @return The animation object reference by the specified id
	 * @throws NotFoundException when the animation cannot be loaded
	 */
	public static Animation loadAnimation(Context context, int id) throws NotFoundException {

		XmlResourceParser parser = null;
		try {
			parser = context.getResources().getAnimation(id);
			return createAnimationFromXml(context, parser);
		}
		catch (XmlPullParserException ex) {
			NotFoundException rnf = new NotFoundException("Can't load animation resource ID #0x" + Integer.toHexString(id));
			rnf.initCause(ex);
			throw rnf;
		}
		catch (IOException ex) {
			NotFoundException rnf = new NotFoundException("Can't load animation resource ID #0x" + Integer.toHexString(id));
			rnf.initCause(ex);
			throw rnf;
		}
		finally {
			if (parser != null)
				parser.close();
		}
	}

	private static Animation createAnimationFromXml(Context c, XmlPullParser parser) throws XmlPullParserException, IOException {
		return createAnimationFromXml(c, parser, null, Xml.asAttributeSet(parser));
	}

	private static Animation createAnimationFromXml(Context c, XmlPullParser parser, AnimationSet parent, AttributeSet attrs)
			throws XmlPullParserException, IOException {

		Animation anim = null;

		// Make sure we are on a start tag.
		int type;
		int depth = parser.getDepth();

		while (((type = parser.next()) != XmlPullParser.END_TAG || parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {

			if (type != XmlPullParser.START_TAG) {
				continue;
			}

			String name = parser.getName();

			if (name.equals("set")) {
				anim = new AnimationSet(c, attrs);
				createAnimationFromXml(c, parser, (AnimationSet) anim, attrs);
			}
			else if (name.equals("alpha")) {
				anim = new AlphaAnimation(c, attrs);
			}
			else if (name.equals("scale")) {
				anim = new ScaleAnimation(c, attrs);
			}
			else if (name.equals("rotate")) {
				anim = new RotateAnimation(c, attrs);
			}
			else if (name.equals("translate")) {
				anim = new TranslateAnimation(c, attrs);
			}
			else {
				throw new RuntimeException("Unknown animation name: " + parser.getName());
			}

			if (parent != null) {
				parent.addAnimation(anim);
			}
		}

		return anim;

	}
}