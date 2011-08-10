package com.proxibase.aircandi.utils;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.client.ClientProtocolException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;

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
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;
import android.graphics.Shader.TileMode;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.CycleInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Toast;
import android.widget.ViewAnimator;

public class AircandiUI {

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
		try {
			InputStream stream = ProxibaseService.getInstance().selectAsStream(url, ResponseFormat.Xml);
			Bitmap bm = BitmapFactory.decodeStream(stream);
			return bm;
		}
		catch (ClientProtocolException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return null;
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

	public static Bitmap getReflection(Bitmap originalImage) {

		int width = originalImage.getWidth();
		int height = originalImage.getHeight();

		// This will not scale but will flip on the Y axis
		Matrix matrix = new Matrix();
		matrix.preScale(1, -1);

		// Create a Bitmap with the flip matrix applied to it.
		// We only want the bottom half of the image
		Bitmap reflectionImage = Bitmap.createBitmap(originalImage, 0, height / 2, width, height / 2, matrix, false);

		// Create a new bitmap with same width but taller to fit reflection
		Bitmap reflectionBitmap = Bitmap.createBitmap(width, (height / 2), Config.ARGB_8888);

		// Create a new Canvas with the bitmap that's big enough for
		// the image plus gap plus reflection
		Canvas canvas = new Canvas(reflectionBitmap);

		// Draw in the reflection
		canvas.drawBitmap(reflectionImage, 0, 0, null);

		// Create a shader that is a linear gradient that covers the reflection
		Paint paint = new Paint();
		LinearGradient shader = new LinearGradient(0, 0, 0, reflectionBitmap.getHeight(), 0x70ffffff, 0x00ffffff, TileMode.CLAMP);

		// Set the paint to use this shader (linear gradient)
		paint.setShader(shader);

		// Set the Transfer mode to be porter duff and destination in
		paint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));

		// Draw a rectangle using the paint with our linear gradient
		canvas.drawRect(0, 0, width, reflectionBitmap.getHeight(), paint);

		// Stash the image with reflection
		return reflectionBitmap;
	}

	public static int gradient(int startColor, int endColor, float index) {
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

	/**
	 * Returns the current animation time in milliseconds. This time should be used when invoking
	 * {@link Animation#setStartTime(long)}. Refer to {@link android.os.SystemClock} for more
	 * information about the different available clocks. The clock used by this method is <em>not</em> the "wall" clock
	 * (it is not {@link System#currentTimeMillis}).
	 * 
	 * @return the current animation time in milliseconds
	 * @see android.os.SystemClock
	 */
	public static long currentAnimationTimeMillis() {
		return SystemClock.uptimeMillis();
	}

	/**
	 * Loads an {@link Interpolator} object from a resource
	 * 
	 * @param context Application context used to access resources
	 * @param id The resource id of the animation to load
	 * @return The animation object reference by the specified id
	 * @throws NotFoundException
	 */
	public static Interpolator loadInterpolator(Context context, int id) throws NotFoundException {
		XmlResourceParser parser = null;
		try {
			parser = context.getResources().getAnimation(id);
			return createInterpolatorFromXml(context, parser);
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

	private static Interpolator createInterpolatorFromXml(Context c, XmlPullParser parser) throws XmlPullParserException, IOException {

		Interpolator interpolator = null;

		// Make sure we are on a start tag.
		int type;
		int depth = parser.getDepth();

		while (((type = parser.next()) != XmlPullParser.END_TAG || parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {

			if (type != XmlPullParser.START_TAG) {
				continue;
			}

			AttributeSet attrs = Xml.asAttributeSet(parser);

			String name = parser.getName();

			if (name.equals("linearInterpolator")) {
				interpolator = new LinearInterpolator(c, attrs);
			}
			else if (name.equals("accelerateInterpolator")) {
				interpolator = new AccelerateInterpolator(c, attrs);
			}
			else if (name.equals("decelerateInterpolator")) {
				interpolator = new DecelerateInterpolator(c, attrs);
			}
			else if (name.equals("accelerateDecelerateInterpolator")) {
				interpolator = new AccelerateDecelerateInterpolator(c, attrs);
			}
			else if (name.equals("cycleInterpolator")) {
				interpolator = new CycleInterpolator(c, attrs);
			}
			else if (name.equals("anticipateInterpolator")) {
				interpolator = new AnticipateInterpolator(c, attrs);
			}
			else if (name.equals("overshootInterpolator")) {
				interpolator = new OvershootInterpolator(c, attrs);
			}
			else if (name.equals("anticipateOvershootInterpolator")) {
				interpolator = new AnticipateOvershootInterpolator(c, attrs);
			}
			else if (name.equals("bounceInterpolator")) {
				interpolator = new BounceInterpolator(c, attrs);
			}
			else {
				throw new RuntimeException("Unknown interpolator name: " + parser.getName());
			}

		}

		return interpolator;

	}

	public static Animation getAnimationFade(long duration, float fromAlpha, float toAlpha) {

		// Alpha animation
		Animation animation = new AlphaAnimation(fromAlpha, toAlpha);
		animation.setDuration(duration);
		animation.setInterpolator(new AccelerateInterpolator());
		animation.setFillEnabled(true);
		animation.setFillAfter(true);
		return animation;
	}

	public static Animation getAnimationMove(long duration, float fromXDelta, float toXDelta, float fromYDelta, float toYDelta) {

		// Alpha animation
		Animation animation = new TranslateAnimation(fromXDelta, toXDelta, fromYDelta, toYDelta);
		animation.setDuration(duration);
		animation.setInterpolator(new AccelerateInterpolator());
		animation.setFillEnabled(true);
		animation.setFillAfter(true);
		return animation;
	}

	public static Animation getAnimationZoom(long duration, float fromScale, float toScale) {

		// Scaling animation
		Animation animation = new ScaleAnimation(fromScale, toScale, fromScale, toScale, Animation.RELATIVE_TO_SELF, 0.5f,
				Animation.RELATIVE_TO_SELF, 0.5f) {};
		animation.setInterpolator(new DecelerateInterpolator());
		animation.setDuration(duration);
		animation.setFillEnabled(true);
		animation.setFillAfter(true);
		return animation;
	}

	public static AnimationSet getAnimIn() {
		final AnimationSet setIn = new AnimationSet(true);
		setIn.addAnimation(AircandiUI.getAnimationFade(1000, 0.0f, 1.0f));
		setIn.addAnimation(AircandiUI.getAnimationZoom(700, 0.8f, 1.0f));
		setIn.setFillEnabled(true);
		setIn.setFillAfter(true);
		return setIn;
	}

	public static AnimationSet getAnimOut() {
		final AnimationSet setOut = new AnimationSet(true);
		setOut.addAnimation(AircandiUI.getAnimationFade(1000, 1.0f, 0.0f));
		setOut.addAnimation(AircandiUI.getAnimationZoom(700, 1.0f, 0.8f));
		setOut.setFillEnabled(true);
		setOut.setFillAfter(true);
		return setOut;
	}

	/**
	 * This class listens for the end of the first half of the animation. It then posts a new action that effectively
	 * swaps the views when the container is rotated 90 degrees and thus invisible.
	 */
	@SuppressWarnings("unused")
	private final class TileFlip implements Animation.AnimationListener {

		private final ViewAnimator	viewFlipper_;

		private TileFlip(ViewAnimator viewFlipper) {

			viewFlipper_ = viewFlipper;
		}

		public void onAnimationStart(Animation animation) {

		}

		public void onAnimationEnd(Animation animation) {

		// invalidate();

		// ImageView imageView = (ImageView) viewFlipper_.getCurrentView();
		// invalidateDrawable(imageView.getDrawable());
		}

		public void onAnimationRepeat(Animation animation) {

		}
	}
}