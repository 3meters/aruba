package com.aircandi.utilities;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;
import android.widget.Toast;

import com.aircandi.Aircandi;
import com.aircandi.Constants;

public class UI {

	public static void showToastNotification(final String message, final int duration) {
		Aircandi.mainThreadHandler.post(new Runnable() {

			@Override
			public void run() {
				final CharSequence text = message;
				final Toast toast = Toast.makeText(Aircandi.applicationContext, text, duration);
				toast.show();
			}
		});
	}

	public static int getRawPixels(Context context, int displayPixels) {
		final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		final int pixels = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (float) displayPixels, metrics);
		return pixels;
	}

	public static int getImageMemorySize(int height, int width, boolean hasAlpha) {
		return height * width * (hasAlpha ? 4 : 3);
	}

	public static Bitmap ensureBitmapScaleForS3(Bitmap bitmap) {
		Bitmap bitmapScaled = bitmap;
		final Boolean scalingNeeded = (bitmap.getWidth() > Constants.IMAGE_DIMENSION_MAX && bitmap.getHeight() > Constants.IMAGE_DIMENSION_MAX);
		if (scalingNeeded) {

			final Matrix matrix = new Matrix();
			final float scalingRatio = Math.max((float) Constants.IMAGE_DIMENSION_MAX / (float) bitmap.getWidth(), (float) Constants.IMAGE_DIMENSION_MAX
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

	public static void setVisibility(View view, Integer visibility) {
		if (view != null) {
			view.setVisibility(visibility);
		}
	}


}