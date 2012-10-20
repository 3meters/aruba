package com.aircandi.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.aircandi.R;
import com.aircandi.components.AnimUtils;
import com.aircandi.components.ImageManager;
import com.aircandi.components.ImageRequest;
import com.aircandi.components.ImageRequest.ImageResponse;
import com.aircandi.components.ImageUtils;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.service.ProxibaseService.RequestListener;

public class WebImageView extends RelativeLayout {

	private String						mImageUri;
	private Handler						mThreadHandler	= new Handler();
	private ImageView					mImageView;
	private ProgressBar					mProgressBar;
	private Integer						mBusyWidth;
	private Integer						mMinWidth;
	private Integer						mMaxWidth;
	private Integer						mMinHeight;
	private Integer						mMaxHeight;
	private boolean						mShowBusy;
	private Integer						mLayoutId;
	private ScaleType					mScaleType		= ScaleType.CENTER_CROP;
	@SuppressWarnings("unused")
	private String						mThemeTone;

	private static final ScaleType[]	sScaleTypeArray	= {
														ScaleType.MATRIX,
														ScaleType.FIT_XY,
														ScaleType.FIT_START,
														ScaleType.FIT_CENTER,
														ScaleType.FIT_END,
														ScaleType.CENTER,
														ScaleType.CENTER_CROP,
														ScaleType.CENTER_INSIDE
														};

	public WebImageView(Context context) {
		this(context, null);
	}

	public WebImageView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public WebImageView(Context context, AttributeSet attributes, int defStyle) {
		super(context, attributes, defStyle);

		TypedArray ta = context.obtainStyledAttributes(attributes, R.styleable.WebImageView, defStyle, 0);

		mBusyWidth = ta.getDimensionPixelSize(R.styleable.WebImageView_busyWidth, 30);
		mMinWidth = ta.getDimensionPixelSize(R.styleable.WebImageView_minWidth, 0);
		mMaxWidth = ta.getDimensionPixelSize(R.styleable.WebImageView_maxWidth, Integer.MAX_VALUE);
		mMinHeight = ta.getDimensionPixelSize(R.styleable.WebImageView_minHeight, 0);
		mMaxHeight = ta.getDimensionPixelSize(R.styleable.WebImageView_maxHeight, Integer.MAX_VALUE);
		mShowBusy = ta.getBoolean(R.styleable.WebImageView_showBusy, true);
		mLayoutId = ta.getResourceId(R.styleable.WebImageView_layout, R.layout.temp_webimageview);

		TypedValue resourceName = new TypedValue();
		if (context.getTheme().resolveAttribute(R.attr.themeTone, resourceName, true)) {
			mThemeTone = (String) resourceName.coerceToString();
		}

		if (!isInEditMode()) {
			int scaleTypeValue = attributes.getAttributeIntValue("http://schemas.android.com/apk/res/android", "scaleType", 6);
			if (scaleTypeValue >= 0) {
				mScaleType = sScaleTypeArray[scaleTypeValue];
			}
		}

		ta.recycle();

		initialize();
	}

	private void initialize() {
		LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(mLayoutId, this, true);
		mImageView = (ImageView) findViewById(R.id.image);
		if (mImageView != null) {
			mImageView.setScaleType((ScaleType) mScaleType);
			mImageView.setMinimumWidth(mMinWidth);
			mImageView.setMaxWidth(mMaxWidth);
			mImageView.setMinimumHeight(mMinHeight);
			mImageView.setMaxHeight(mMaxHeight);
			if (isInEditMode()) {
				mImageView.setImageResource(R.drawable.jaymassena);
			}
		}
		mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);

		if (mProgressBar != null) {
			if (!mShowBusy) {
				mProgressBar.setVisibility(View.GONE);
			}
			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mBusyWidth, mBusyWidth);
			params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
			mProgressBar.setLayoutParams(params);
		}
	}

	public void setImage(final Bitmap bitmap, String imageUri) {

		mImageUri = imageUri;
		mThreadHandler.post(new Runnable() {

			@Override
			public void run() {
				mImageView.setImageBitmap(null);
				ImageUtils.showImageInImageView(bitmap, mImageView, true, AnimUtils.fadeInMedium());
			}
		});
	}

	/**
	 * Handles the image request to set the internal ImageView. Creates a separate
	 * scaled bitmap based on maxWidth and maxHeight or defaults if not set. The original
	 * bitmap supplied to satisfy the image request may be recycled. If an image
	 * is not supplied by the image request, the standard broken image is used.
	 * 
	 * @param imageRequest
	 * @param imageReflection
	 */
	public void setImageRequest(final ImageRequest imageRequest) {
		setImageRequest(imageRequest, true);
	}

	/**
	 * Handles the image request to set the internal ImageView. Creates a separate
	 * scaled bitmap based on maxWidth and maxHeight or defaults if not set. The original
	 * bitmap supplied to satisfy the image request may be recycled unless !okToRecycle. If an image
	 * is not supplied by the image request, the standard broken image is used.
	 * 
	 * @param imageRequest
	 * @param imageReflection
	 * @param okToRecycle
	 */
	public void setImageRequest(final ImageRequest imageRequest, final boolean okToRecycle) {

		mImageUri = imageRequest.getImageUri();

		final RequestListener originalImageReadyListener = imageRequest.getRequestListener();

		/* Start the busy indicator */

		if (mShowBusy) {
			showLoading(true);
		}

		mImageView.setImageBitmap(null);

		imageRequest.setRequestListener(new RequestListener() {

			@Override
			public void onProgressChanged(int progress) {
				if (originalImageReadyListener != null) {
					originalImageReadyListener.onProgressChanged(progress);
				}
			}

			@Override
			public void onComplete(Object response) {
				ServiceResponse serviceResponse = (ServiceResponse) response;

				if (serviceResponse.responseCode == ResponseCode.Success) {

					final ImageResponse imageResponse = (ImageResponse) serviceResponse.data;

					/* Make sure this is the right target for the image */
					if (imageResponse.imageUri.equals(mImageUri)) {

						if (imageResponse.bitmap != null) {

							boolean scaleBitmap = (mMaxWidth != Integer.MAX_VALUE && mMaxHeight != Integer.MAX_VALUE);
							final Bitmap bitmapScaled = scaleBitmap
									? Bitmap.createScaledBitmap(imageResponse.bitmap, mMaxWidth, mMaxHeight, true)
									: imageResponse.bitmap;

							setImage(bitmapScaled, imageResponse.imageUri);
						}
					}
				}
				else {
					mThreadHandler.post(new Runnable() {

						@Override
						public void run() {
							Drawable drawable = WebImageView.this.getContext().getResources().getDrawable(R.drawable.image_broken);
							ImageUtils.showDrawableInImageView(drawable, mImageView, true, AnimUtils.fadeInMedium());
						}
					});
				}
				if (mShowBusy) {
					showLoading(false);
				}

				if (originalImageReadyListener != null) {
					originalImageReadyListener.onComplete(serviceResponse);
				}
			}
		});

		ImageManager.getInstance().getImageLoader().fetchImage(imageRequest, true);
	}

	public void clearImage(final boolean animate, final Integer animationId) {
		mThreadHandler.post(new Runnable() {

			@Override
			public void run() {
				ImageUtils.clearImageInImageView(mImageView, animate, AnimUtils.fadeOutMedium());
			}
		});
	}

	public void showLoading(boolean loading) {
		if (loading) {
			mProgressBar.setVisibility(View.VISIBLE);
			
//			mProgressBar.post(new Runnable() {
//
//				@Override
//				public void run() {
//					mProgressBar.setBackgroundResource(mThemeBusyIndicatorResId);
//					final AnimationDrawable animation = (AnimationDrawable) mProgressBar.getBackground();
//					animation.start();
//					mProgressBar.setVisibility(View.VISIBLE);
//				}
//			});
		}
		else {
			mProgressBar.post(new Runnable() {

				@Override
				public void run() {
					Animation animation = AnimUtils.fadeOutMedium();
//					animation.setAnimationListener(new AnimationListener() {
//
//						@Override
//						public void onAnimationStart(Animation animation) {}
//
//						@SuppressWarnings("deprecation")
//						@Override
//						public void onAnimationEnd(Animation animation) {
//							mProgressBar.setBackgroundDrawable(null);
//						}
//
//						@Override
//						public void onAnimationRepeat(Animation animation) {}
//					});
					mProgressBar.startAnimation(animation);
				}
			});
		}
	}

	public String getImageUri() {
		return mImageUri;
	}

	public void setImageUri(String imageUri) {
		mImageUri = imageUri;
	}

	public void setImageDrawable(Drawable drawable) {
		ImageUtils.showDrawableInImageView(drawable, mImageView, true, AnimUtils.fadeInMedium());
	}

	public ImageView getImageView() {
		return mImageView;
	}

	public void recycleBitmap() {
		if (mImageView.getDrawable() != null) {
			BitmapDrawable bitmapDrawable = (BitmapDrawable) mImageView.getDrawable();
			if (bitmapDrawable != null && bitmapDrawable.getBitmap() != null && !bitmapDrawable.getBitmap().isRecycled()) {
				bitmapDrawable.getBitmap().recycle();
			}
		}
		mImageView.setImageBitmap(null);
	}

	public void onDestroy() {
		recycleBitmap();
	}
}
