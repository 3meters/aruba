package com.aircandi.utilities;

import android.app.Service;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.beta.R;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.service.HttpService.RequestListener;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Place;
import com.aircandi.ui.widgets.AirImageView;

public class UI {

	public static Boolean photosEqual(Photo photoCurrent, Photo photoNew) {
		if (photoCurrent != null && photoNew != null && photoNew.getUri().equals(photoCurrent.getUri())) {
			return true;
		}
		return false;
	}

	public static void drawPhoto(final AirImageView photoView, Photo photo) {

		if (photo != null && photo.hasBitmap()) {
			photoView.hideLoading();
			UI.showImageInImageView(photo.getBitmap(), photoView.getImageView(), true, Animate.fadeInMedium());
			photoView.setVisibility(View.VISIBLE);
		}
		else {
			photoView.getImageView().setTag(photo.getUri());
			photoView.setPhoto(photo);
			aircandi(photoView, photo);
		}
		/*
		 * Special color treatment if enabled.
		 */
		if (photo.colorize != null && photo.colorize) {

			final int color = Place.getCategoryColor(photo.colorizeKey, true, Aircandi.muteColor, false);
			photoView.getImageView().setColorFilter(color, PorterDuff.Mode.MULTIPLY);

			Integer colorResId = Place.getCategoryColorResId(photo.colorizeKey, true, Aircandi.muteColor, false);
			if (photoView.findViewById(R.id.color_layer) != null) {
				(photoView.findViewById(R.id.color_layer)).setBackgroundResource(colorResId);
				(photoView.findViewById(R.id.color_layer)).setVisibility(View.VISIBLE);
				(photoView.findViewById(R.id.reverse_layer)).setVisibility(View.VISIBLE);
			}
			else {
				photoView.getImageView().setBackgroundResource(colorResId);
			}
		}
		else {
			photoView.getImageView().clearColorFilter();
			photoView.getImageView().setBackgroundResource(0);
			if (photoView.findViewById(R.id.color_layer) != null) {
				(photoView.findViewById(R.id.color_layer)).setBackgroundResource(0);
				(photoView.findViewById(R.id.color_layer)).setVisibility(View.GONE);
				(photoView.findViewById(R.id.reverse_layer)).setVisibility(View.GONE);
			}
		}
	}

	public static void aircandi(final AirImageView photoView, Photo photo) {
		final BitmapRequest bitmapRequest = new BitmapRequest(photo.getUri(), photoView.getImageView())
				.setImageSize(photoView.getSizeHint())
				.setImageRequestor(photoView)
				.setRequestListener(new RequestListener() {

					@Override
					public void onStart() {
						photoView.showLoading();
					}

					@Override
					public void onComplete(Object response) {

						final ServiceResponse serviceResponse = (ServiceResponse) response;
						photoView.hideLoading();
						if (serviceResponse.responseCode != ResponseCode.Success) {
							photoView.showBroken();
						}
					}

				});

		BitmapManager.getInstance().masterFetch(bitmapRequest);
	}

//	public static void picasso(final AirImageView photoView, Photo photo) {
//
//		String photoUri = photo.getUri();
//
//		if (photoUri.toLowerCase(Locale.US).startsWith("resource:")) {
//			final String rawResourceName = photoUri.substring(photoUri.indexOf("resource:") + 9);
//			final String resolvedResourceName = BitmapManager.getInstance().resolveResourceName(rawResourceName);
//			final int resourceId = Aircandi.applicationContext.getResources().getIdentifier(resolvedResourceName, "drawable",
//					Aircandi.getInstance().getPackageName());
//
//			Picasso.with(Aircandi.applicationContext)
//					.load(resourceId)
//					.resize(photoView.getSizeHint(), photoView.getSizeHint())
//					.centerCrop()
//					.into(photoView.getImageView());
//		}
//		else {
//			Picasso.with(Aircandi.applicationContext)
//					.load(photoUri)
//					.resize(photoView.getSizeHint(), photoView.getSizeHint())
//					.centerCrop()
//					.into(photoView.getImageView());
//		}
//	}
//
//	public static void picassoFancy(final AirImageView photoView, Photo photo) {
//
//		String photoUri = photo.getUri();
//
//		if (photoUri.toLowerCase(Locale.US).startsWith("resource:")) {
//			final String rawResourceName = photoUri.substring(photoUri.indexOf("resource:") + 9);
//			final String resolvedResourceName = BitmapManager.getInstance().resolveResourceName(rawResourceName);
//			final int resourceId = Aircandi.applicationContext.getResources().getIdentifier(resolvedResourceName, "drawable",
//					Aircandi.getInstance().getPackageName());
//
//			Picasso.with(Aircandi.applicationContext)
//					.load(resourceId)
//					.resize(photoView.getSizeHint(), photoView.getSizeHint())
//					.centerCrop()
//					.into(new Target() {
//
//						@Override
//						public void onError() {}
//
//						@Override
//						public void onSuccess(Bitmap bitmap) {
//							final BitmapDrawable bitmapDrawable = new BitmapDrawable(Aircandi.applicationContext.getResources(), bitmap);
//							UI.showDrawableInImageView(bitmapDrawable, photoView.getImageView(), true, Animate.fadeInMedium());
//						}
//					});
//		}
//		else {
//			Picasso.with(Aircandi.applicationContext)
//					.load(photoUri)
//					.resize(photoView.getSizeHint(), photoView.getSizeHint())
//					.centerCrop()
//					.into(new Target() {
//
//						@Override
//						public void onError() {}
//
//						@Override
//						public void onSuccess(Bitmap bitmap) {
//							final BitmapDrawable bitmapDrawable = new BitmapDrawable(Aircandi.applicationContext.getResources(), bitmap);
//							UI.showDrawableInImageView(bitmapDrawable, photoView.getImageView(), true, Animate.fadeInMedium());
//						}
//					});
//		}
//	}

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
		imageView.postInvalidate();
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

	public static void hideSoftInput(Context context) {
		InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Service.INPUT_METHOD_SERVICE);
		inputManager.hideSoftInputFromWindow(new View(context).getWindowToken(), 0);
	}
}