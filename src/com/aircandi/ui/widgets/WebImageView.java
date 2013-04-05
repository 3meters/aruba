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

import com.aircandi.beta.R;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequest.ImageResponse;
import com.aircandi.service.HttpService.RequestListener;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.ImageUtils;

@SuppressWarnings("ucd")
public class WebImageView extends RelativeLayout {

	private ImageView					mImageMain;
	private ImageView					mImageZoom;
	private ProgressBar					mProgressBar;

	private String						mImageUri;
	private final Handler						mThreadHandler		= new Handler();

	private Integer						mSizeHint;

	private boolean						mShowBusy;
	private Integer						mLayoutId;
	private Integer						mBrokenDrawable;
	private ScaleType					mScaleType			= ScaleType.CENTER_CROP;

	private static final String			androidNamespace	= "http://schemas.android.com/apk/res/android";
	private static final ScaleType[]	sScaleTypeArray		= {
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

		final TypedArray ta = context.obtainStyledAttributes(attributes, R.styleable.WebImageView, defStyle, 0);

		mSizeHint = ta.getDimensionPixelSize(R.styleable.WebImageView_sizeHint, Integer.MAX_VALUE);
		mShowBusy = ta.getBoolean(R.styleable.WebImageView_showBusy, true);
		mLayoutId = ta.getResourceId(R.styleable.WebImageView_layout, R.layout.widget_webimageview);
		mBrokenDrawable = ta.getResourceId(R.styleable.WebImageView_brokenDrawable, R.drawable.img_broken);

		ta.recycle();

		if (!isInEditMode()) {
			final int scaleTypeValue = attributes.getAttributeIntValue(androidNamespace, "scaleType", 6);
			if (scaleTypeValue >= 0) {
				mScaleType = sScaleTypeArray[scaleTypeValue];
			}
		}

		initialize(context);
	}

	private void initialize(Context context) {
		final LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final View view = inflater.inflate(mLayoutId, this, true);

		mImageMain = (ImageView) view.findViewById(R.id.image_main);

		if (!isInEditMode()) {
			mImageZoom = (ImageView) view.findViewById(R.id.image_zoom);
			mProgressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
		}

		if (mImageMain != null) {
			mImageMain.setScaleType(mScaleType);
			mImageMain.invalidate();

			if (isInEditMode()) {
				mImageMain.setImageResource(R.drawable.img_placeholder_logo);
			}
		}

		if (mProgressBar != null) {
			if (!mShowBusy) {
				mProgressBar.setVisibility(View.GONE);
			}
			final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(30, 30);
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

	private void doImageRequest(final BitmapRequest bitmapRequest, final boolean okToRecycle) {
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

		bitmapRequest.setImageRequestor(mImageMain);
		bitmapRequest.setImageSize(mSizeHint);

		bitmapRequest.setRequestListener(new RequestListener() {

			@Override
			public void onProgressChanged(int progress) {
				if (originalImageReadyListener != null) {
					originalImageReadyListener.onProgressChanged(progress);
				}
			}

			@Override
			public void onComplete(Object response) {
				final ServiceResponse serviceResponse = (ServiceResponse) response;

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
							final Drawable drawable = WebImageView.this.getContext().getResources().getDrawable(mBrokenDrawable);
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

		BitmapManager.getInstance().masterFetch(bitmapRequest);

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

	private void showLoading() {
		mProgressBar.setVisibility(View.VISIBLE);
	}

	public void hideLoading() {
		mProgressBar.post(new Runnable() {

			@Override
			public void run() {
				final Animation animation = AnimUtils.fadeOutMedium();
				mProgressBar.startAnimation(animation);
			}
		});
	}

	// --------------------------------------------------------------------------------------------
	// Setters/Getters routines
	// --------------------------------------------------------------------------------------------

	private void setImage(final Bitmap bitmap, String imageUri) {

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

	public ImageView getImageZoom() {
		return mImageZoom;
	}

	public Integer getSizeHint() {
		return mSizeHint;
	}

	public void setSizeHint(Integer sizeHint) {
		mSizeHint = sizeHint;
	}
}
