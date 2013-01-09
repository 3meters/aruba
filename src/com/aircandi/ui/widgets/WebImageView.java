package com.aircandi.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.aircandi.R;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequest.ImageResponse;
import com.aircandi.service.ProxibaseService.RequestListener;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.ImageUtils;

public class WebImageView extends RelativeLayout {

	private ImageView					mImageMain;
	private ImageView					mImageBadge;
	private ImageView					mImageZoom;
	private ProgressBar					mProgressBar;

	private String						mImageUri;
	private Handler						mThreadHandler			= new Handler();

	private Integer						mSizeHint;

	private boolean						mShowBusy;
	private Integer						mLayoutId;
	private ScaleType					mScaleType				= ScaleType.CENTER_CROP;

	private static final String			androidNamespace		= "http://schemas.android.com/apk/res/android";
	private static final ScaleType[]	sScaleTypeArray			= {
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

		mSizeHint = ta.getDimensionPixelSize(R.styleable.WebImageView_sizeHint, Integer.MAX_VALUE);
		mShowBusy = ta.getBoolean(R.styleable.WebImageView_showBusy, true);
		mLayoutId = ta.getResourceId(R.styleable.WebImageView_layout, R.layout.widget_webimageview);

		ta.recycle();

		if (!isInEditMode()) {
			int scaleTypeValue = attributes.getAttributeIntValue(androidNamespace, "scaleType", 6);
			if (scaleTypeValue >= 0) {
				mScaleType = sScaleTypeArray[scaleTypeValue];
			}
		}

		initialize(context);
	}

	private void initialize(Context context) {
		LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(mLayoutId, this, true);

		mImageMain = (ImageView) view.findViewById(R.id.image_main);

		if (!isInEditMode()) {
			mImageBadge = (ImageView) view.findViewById(R.id.image_badge);
			mImageZoom = (ImageView) view.findViewById(R.id.image_zoom);
			mProgressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
		}

		if (mImageMain != null) {
			mImageMain.setScaleType((ScaleType) mScaleType);
			mImageMain.invalidate();

			if (isInEditMode()) {
				mImageMain.setImageResource(R.drawable.placeholder_logo);
			}
		}

		if (mProgressBar != null) {
			if (!mShowBusy) {
				mProgressBar.setVisibility(View.GONE);
			}
			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(30, 30);
			params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
			mProgressBar.setLayoutParams(params);
		}
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if (mImageMain != null) {
			mImageMain.layout(l, t, r, b);
		}
		super.onLayout(changed, l, t, r, b);
	}

	public void doImageRequest(final BitmapRequest bitmapRequest, final boolean okToRecycle) {
		/*
		 * Handles the image request to set the internal ImageView. Creates a separate
		 * scaled bitmap based on maxWidth and maxHeight or defaults if not set. The original
		 * bitmap supplied to satisfy the image request may be recycled unless !okToRecycle. If an image
		 * is not supplied by the image request, the standard broken image is used.
		 */
		mImageUri = bitmapRequest.getImageUri();

		final RequestListener originalImageReadyListener = bitmapRequest.getRequestListener();

		/* Start the busy indicator */

		if (mShowBusy) {
			showLoading();
		}

		/* Clear the current bitmap */
		mImageMain.setImageBitmap(null);

		bitmapRequest.setImageRequestor(this);
		if (mSizeHint != null) {
			bitmapRequest.setImageSize(mSizeHint);
		}
		bitmapRequest.setRequestListener(new RequestListener() {

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
							/*
							 * Give the original listener a chance to modify the
							 * bitmap before we display it.
							 */
							setImage(imageResponse.bitmap, imageResponse.imageUri);
						}
					}
				}
				else {
					/*
					 * Show broken image
					 */
					mThreadHandler.post(new Runnable() {

						@Override
						public void run() {
							Drawable drawable = WebImageView.this.getContext().getResources().getDrawable(R.drawable.image_broken);
							ImageUtils.showDrawableInImageView(drawable, mImageMain, true, AnimUtils.fadeInMedium());
						}
					});
				}

				mThreadHandler.post(new Runnable() {

					@Override
					public void run() {
						mProgressBar.setVisibility(View.GONE);
					}
				});

				if (originalImageReadyListener != null) {
					originalImageReadyListener.onComplete(serviceResponse);
				}
			}
		});

		BitmapManager.getInstance().fetchBitmap(bitmapRequest);
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	public void clearImage(final boolean animate, final Integer animationId) {
		mThreadHandler.post(new Runnable() {

			@Override
			public void run() {
				ImageUtils.clearImageInImageView(mImageMain, animate, AnimUtils.fadeOutMedium());
			}
		});
	}

	public void showLoading() {
		mProgressBar.setVisibility(View.VISIBLE);
	}

	public void hideLoading() {
		mProgressBar.post(new Runnable() {

			@Override
			public void run() {
				Animation animation = AnimUtils.fadeOutMedium();
				mProgressBar.startAnimation(animation);
			}
		});
	}

	// --------------------------------------------------------------------------------------------
	// Setters/Getters routines
	// --------------------------------------------------------------------------------------------

	public void setImage(final Bitmap bitmap, String imageUri) {

		mImageUri = imageUri;
		mThreadHandler.post(new Runnable() {

			@Override
			public void run() {
				mImageMain.setImageBitmap(null);
				ImageUtils.showImageInImageView(bitmap, mImageMain, true, AnimUtils.fadeInMedium());
			}
		});
	}

	public void setBitmapRequest(final BitmapRequest bitmapRequest) {
		setBitmapRequest(bitmapRequest, true);
	}

	public void setBitmapRequest(final BitmapRequest bitmapRequest, final boolean okToRecycle) {
		doImageRequest(bitmapRequest, okToRecycle);
	}

	public String getImageUri() {
		return mImageUri;
	}

	public void setImageUri(String imageUri) {
		mImageUri = imageUri;
	}

	public void setImageDrawable(Drawable drawable) {
		ImageUtils.showDrawableInImageView(drawable, mImageMain, true, AnimUtils.fadeInMedium());
	}

	public ImageView getImageView() {
		return mImageMain;
	}

	public ImageView getImageBadge() {
		return mImageBadge;
	}

	public ImageView getImageZoom() {
		return mImageZoom;
	}
}
