package com.aircandi.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
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
import com.aircandi.components.AnimUtils;
import com.aircandi.components.ImageManager;
import com.aircandi.components.ImageRequest;
import com.aircandi.components.ImageRequest.ImageResponse;
import com.aircandi.components.ImageUtils;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.service.ProxibaseService.RequestListener;

@SuppressWarnings("unused")
public class WebImageView extends RelativeLayout {

	private ImageView					mImageMain;
	private ImageView					mImageBadge;
	private ImageView					mImageZoom;
	private ProgressBar					mProgressBar;

	private String						mImageUri;
	private Handler						mThreadHandler			= new Handler();

	private Integer						mMinWidth;
	private Integer						mMaxWidth;
	private Integer						mMinHeight;
	private Integer						mMaxHeight;

	private String						mLayoutWidth;
	private String						mLayoutHeight;
	private Boolean						mLayoutWidthMatchParent	= false;
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

		mMaxWidth = ta.getDimensionPixelSize(R.styleable.WebImageView_maxWidth, Integer.MAX_VALUE);
		mMaxHeight = ta.getDimensionPixelSize(R.styleable.WebImageView_maxHeight, Integer.MAX_VALUE);

		mShowBusy = ta.getBoolean(R.styleable.WebImageView_showBusy, true);
		mLayoutId = ta.getResourceId(R.styleable.WebImageView_layout, R.layout.widget_webimageview);

//		mLayoutWidth = attributes.getAttributeValue(androidNamespace, "layout_width");
//		mLayoutHeight = attributes.getAttributeValue(androidNamespace, "layout_height");
//		String displayPixels = mLayoutWidth.replaceAll("[^\\d.]", "");
//		if ((int) Float.parseFloat(displayPixels) != LayoutParams.FILL_PARENT) {
//			mLayoutWidthMatchParent = true;
//		}

		if (!isInEditMode()) {
			int scaleTypeValue = attributes.getAttributeIntValue(androidNamespace, "scaleType", 6);
			if (scaleTypeValue >= 0) {
				mScaleType = sScaleTypeArray[scaleTypeValue];
			}
		}

		ta.recycle();

		initialize(context);
	}

	private void initialize(Context context) {
		LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(mLayoutId, this, true);

		mImageBadge = (ImageView) view.findViewById(R.id.image_badge);
		mImageZoom = (ImageView) view.findViewById(R.id.image_zoom);
		mImageMain = (ImageView) view.findViewById(R.id.image_main);
		mProgressBar = (ProgressBar) view.findViewById(R.id.progress_bar);

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
		mImageMain.layout(l, t, r, b);
		super.onLayout(changed, l, t, r, b);
	}

	public void doImageRequest(final ImageRequest imageRequest, final boolean okToRecycle) {
		/*
		 * Handles the image request to set the internal ImageView. Creates a separate
		 * scaled bitmap based on maxWidth and maxHeight or defaults if not set. The original
		 * bitmap supplied to satisfy the image request may be recycled unless !okToRecycle. If an image
		 * is not supplied by the image request, the standard broken image is used.
		 */
		mImageUri = imageRequest.getImageUri();

		final RequestListener originalImageReadyListener = imageRequest.getRequestListener();

		/* Start the busy indicator */

		if (mShowBusy) {
			showLoading();
		}

		mImageMain.setImageBitmap(null);

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

							setImage(imageResponse.bitmap, imageResponse.imageUri);

//							boolean scaleBitmap = (mMaxWidth != Integer.MAX_VALUE && mMaxHeight != Integer.MAX_VALUE);
//							final Bitmap bitmapScaled = scaleBitmap
//									? Bitmap.createScaledBitmap(imageResponse.bitmap, mMaxWidth, mMaxHeight, true)
//									: imageResponse.bitmap;
//
//							setImage(bitmapScaled, imageResponse.imageUri);
						}
					}
				}
				else {
					mThreadHandler.post(new Runnable() {

						@Override
						public void run() {
							Drawable drawable = WebImageView.this.getContext().getResources().getDrawable(R.drawable.image_broken);
							ImageUtils.showDrawableInImageView(drawable, mImageMain, true, AnimUtils.fadeInMedium());
						}
					});
				}
				if (mShowBusy) {
					hideLoading();
				}

				if (originalImageReadyListener != null) {
					originalImageReadyListener.onComplete(serviceResponse);
				}
			}
		});

		ImageManager.getInstance().getImageLoader().fetchImage(imageRequest, true);

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

	public void setImageRequest(final ImageRequest imageRequest) {
		setImageRequest(imageRequest, true);
	}

	public void setImageRequest(final ImageRequest imageRequest, final boolean okToRecycle) {
		doImageRequest(imageRequest, okToRecycle);
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

	public void recycleBitmap() {
		if (mImageMain.getDrawable() != null) {
			BitmapDrawable bitmapDrawable = (BitmapDrawable) mImageMain.getDrawable();
			if (bitmapDrawable != null && bitmapDrawable.getBitmap() != null && !bitmapDrawable.getBitmap().isRecycled()) {
				bitmapDrawable.getBitmap().recycle();
			}
		}
		mImageMain.setImageBitmap(null);
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle routines
	// --------------------------------------------------------------------------------------------

	public void onDestroy() {
		recycleBitmap();
	}
}
