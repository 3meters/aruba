package com.aircandi.utilities;

import org.apache.http.HttpStatus;

import android.app.Service;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
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
import com.aircandi.R;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequest.ImageResponse;
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

	public static void drawPhoto(final AirImageView photoView, final Photo photo) {
		drawPhoto(photoView, photo, null);
	}

	public static void drawPhoto(final AirImageView photoView, final Photo photo, final RequestListener listener) {

		if (photo != null && photo.hasBitmap()) {
			photoView.showLoading(false);
			UI.showImageInImageView(photo.getBitmap(), photoView.getImageView(), true, Animate.fadeInMedium());
			photoView.setVisibility(View.VISIBLE);
		}
		else {
			photoView.getImageView().setImageDrawable(null);
			photoView.getImageView().setTag(photo.getUri());
			photoView.setPhoto(photo);
			aircandi(photoView, photo, listener);
		}
		/*
		 * Special color treatment if enabled.
		 */
		if (photo.colorize != null && photo.colorize) {
			if (photo.color != null) {
				photoView.getImageView().setColorFilter(photo.color, PorterDuff.Mode.SRC_ATOP);
				photoView.getImageView().setBackgroundResource(0);
				if (photoView.findViewById(R.id.color_layer) != null) {
					(photoView.findViewById(R.id.color_layer)).setBackgroundResource(0);
					(photoView.findViewById(R.id.color_layer)).setVisibility(View.GONE);
					(photoView.findViewById(R.id.reverse_layer)).setVisibility(View.GONE);
				}
			}
			else {
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

	public static void aircandi(final AirImageView photoView, final Photo photo, final RequestListener listener) {
		/*
		 * We don't pass photoView so we handle getting the bitmap displayed.
		 */
		final BitmapRequest bitmapRequest = new BitmapRequest()
				.setImageUri(photo.getUri())
				.setImageSize(photoView.getSizeHint())
				.setImageRequestor(photoView)
				.setRequestListener(new RequestListener() {

					@Override
					public void onStart() {
						photoView.showLoading(true);
					}

					@Override
					public void onComplete(Object response) {
						final ServiceResponse serviceResponse = (ServiceResponse) response;
						if (serviceResponse.responseCode == ResponseCode.Success) {

							final ImageResponse imageResponse = (ImageResponse) serviceResponse.data;
							/*
							 * Make sure we still need the bitmap we got
							 */
							if (imageResponse.bitmap != null && imageResponse.photoUri.equals(photo.getUri())) {
								final BitmapDrawable bitmapDrawable = new BitmapDrawable(Aircandi.applicationContext.getResources(), imageResponse.bitmap);
								UI.showDrawableInImageView(bitmapDrawable, photoView.getImageView(), true, Animate.fadeInMedium());
								if (listener != null) {
									listener.onComplete(response, photo, imageResponse.bitmap, false);
								}
							}
						}
						photoView.showLoading(false);
					}

					@Override
					public void onError(Object response) {
						final ServiceResponse serviceResponse = (ServiceResponse) response;

						if (serviceResponse.exception != null && serviceResponse.exception.getStatusCode() != null) {
							Float statusCode = serviceResponse.exception.getStatusCode();
							String exception = serviceResponse.exception.getInnerException().getClass().getSimpleName();
							if (statusCode == HttpStatus.SC_NOT_FOUND) {
								UI.showToastNotification("Photo not found", Toast.LENGTH_SHORT);
							}
							else if (statusCode == HttpStatus.SC_NOT_ACCEPTABLE) {
								UI.showToastNotification("Unknown photo format", Toast.LENGTH_SHORT);
							}
							else {
								UI.showToastNotification("Unhandled status code: " + String.valueOf(statusCode), Toast.LENGTH_LONG);
								UI.showToastNotification("Unhandled exception: " + exception, Toast.LENGTH_LONG);
							}
						}
						if (photoView.getBrokenPhoto() != null) {
							photoView.getImageView().setTag(photo.getUri());
							photoView.setPhoto(photo);
							aircandi(photoView, photoView.getBrokenPhoto(), listener);
						}
						else {
							photoView.showLoading(false);
							photoView.showBroken(true);
						}
					}
				});

		BitmapManager.getInstance().masterFetch(bitmapRequest);
	}

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

	public static int getRawPixelsForDisplayPixels(Context context, int displayPixels) {
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

	public static void showImageInImageView(final Bitmap bitmap, final ImageView imageView, final boolean animate, final Animation animation) {
		/*
		 * Make sure this on the main thread
		 */
		Aircandi.mainThreadHandler.post(new Runnable() {

			@Override
			public void run() {
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
		});

	}

	public static void clearImageInImageView(final ImageView imageView, final boolean animate, final Animation animation) {
		Aircandi.mainThreadHandler.post(new Runnable() {

			@Override
			public void run() {
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
		});
	}

	public static void showDrawableInImageView(final Drawable drawable, final ImageView imageView, final boolean animate, final Animation animation) {
		/*
		 * Make sure this on the main thread
		 */
		Aircandi.mainThreadHandler.post(new Runnable() {

			@Override
			public void run() {
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
		});
	}

	public static void setImageBitmapWithFade(final ImageView imageView, final Bitmap bitmap) {
		Resources resources = imageView.getResources();
		BitmapDrawable bitmapDrawable = new BitmapDrawable(resources, bitmap);
		setImageDrawableWithFade(imageView, bitmapDrawable);
	}

	public static void setImageDrawableWithFade(final ImageView imageView, final Drawable drawable) {
		/*
		 * Make sure this on the main thread
		 */
		Aircandi.mainThreadHandler.post(new Runnable() {

			@Override
			public void run() {
				Drawable currentDrawable = imageView.getDrawable();
				if (currentDrawable != null) {
					Drawable[] arrayDrawable = new Drawable[2];
					arrayDrawable[0] = currentDrawable;
					arrayDrawable[1] = drawable;
					TransitionDrawable transitionDrawable = new TransitionDrawable(arrayDrawable);
					transitionDrawable.setCrossFadeEnabled(true);
					imageView.setImageDrawable(transitionDrawable);
					transitionDrawable.startTransition(2000);
				}
				else {
					imageView.setImageDrawable(drawable);
				}
			}
		});
	}

	public static void setVisibility(View view, Integer visibility) {
		if (view != null) {
			view.setVisibility(visibility);
		}
	}

	@SuppressWarnings("ucd")
	public static void hideSoftInput(Context context) {
		InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Service.INPUT_METHOD_SERVICE);
		inputManager.hideSoftInputFromWindow(new View(context).getWindowToken(), 0);
	}
	
	@SuppressWarnings("ucd")
	public static void showSoftInput(Context context) {
		InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Service.INPUT_METHOD_SERVICE);
		inputManager.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
	}

	@SuppressWarnings("ucd")
	public static int showScreenSize() {
		int screenSize = Aircandi.applicationContext.getResources().getConfiguration().screenLayout &
				Configuration.SCREENLAYOUT_SIZE_MASK;

		switch (screenSize) {
			case Configuration.SCREENLAYOUT_SIZE_XLARGE:
				showToastNotification("Extra large screen", Toast.LENGTH_LONG);
				break;
			case Configuration.SCREENLAYOUT_SIZE_LARGE:
				showToastNotification("Large screen", Toast.LENGTH_LONG);
				break;
			case Configuration.SCREENLAYOUT_SIZE_NORMAL:
				showToastNotification("Normal screen", Toast.LENGTH_LONG);
				break;
			case Configuration.SCREENLAYOUT_SIZE_SMALL:
				showToastNotification("Small screen", Toast.LENGTH_LONG);
				break;
			default:
				showToastNotification("Screen size is neither xlarge, large, normal or small", Toast.LENGTH_LONG);
		}
		return screenSize;
	}
}