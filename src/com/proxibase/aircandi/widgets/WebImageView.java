package com.proxibase.aircandi.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ImageView.ScaleType;

import com.proxibase.aircandi.R;
import com.proxibase.aircandi.components.ImageManager;
import com.proxibase.aircandi.components.ImageRequest;
import com.proxibase.aircandi.components.ImageUtils;
import com.proxibase.aircandi.components.Logger;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestListener;

public class WebImageView extends RelativeLayout {

	private String		mImageUri;
	private Handler		mThreadHandler	= new Handler();
	private ImageView	mImageView;
	private ImageView	mImageViewLoading;
	private Integer		mBusyWidth;
	private Integer		mMinWidth;
	private Integer		mMaxWidth;
	private boolean		mShowBusy;
	private Integer		mLayoutId;
	private ScaleType	mScaleType;
	private String		mThemeTone;

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
		mShowBusy = ta.getBoolean(R.styleable.WebImageView_showBusy, true);
		mLayoutId = ta.getResourceId(R.styleable.WebImageView_layout, R.layout.temp_webimageview);

		TypedValue resourceName = new TypedValue();
		if (context.getTheme().resolveAttribute(R.attr.themeTone, resourceName, true)) {
			mThemeTone = (String) resourceName.coerceToString();
		}

		int scaleTypeValue = attributes.getAttributeIntValue("http://schemas.android.com/apk/res/android", "scaleType", 0);
		mScaleType = ScaleType.values()[scaleTypeValue];

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
		}
		mImageViewLoading = (ImageView) findViewById(R.id.image_loading);

		if (mImageViewLoading != null) {
			if (!mShowBusy) {
				mImageViewLoading.setVisibility(View.GONE);
			}
			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mBusyWidth, mBusyWidth);
			params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
			mImageViewLoading.setLayoutParams(params);
		}
	}

	public void setImageRequest(final ImageRequest imageRequest, final ImageView imageReflection) {

		final RequestListener originalImageReadyListener = imageRequest.getRequestListener();

		/* Start the busy indicator */

		showLoading(true);
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

				/* Who's looking for imageUri? */

				ServiceResponse serviceResponse = (ServiceResponse) response;

				if (serviceResponse.responseCode == ResponseCode.Success) {

					final Bitmap bitmap = (Bitmap) serviceResponse.data;
					@SuppressWarnings("unused")
					String imageFileUri = ImageManager.getInstance().getImageCache().getImageFileUri(imageRequest.getImageUri());
					mImageView.setTag(imageRequest.getImageUri());

					if (bitmap != null) {
						mThreadHandler.post(new Runnable() {

							@Override
							public void run() {
								if (imageRequest.getMakeReflection() && imageReflection != null) {
									String cacheName = ImageManager.getInstance().resolveCacheName(imageRequest.getImageUri());
									Bitmap bitmapReflection = ImageManager.getInstance().getImage(cacheName + ".reflection");
									ImageUtils.showImageInImageView(bitmap, bitmapReflection, mImageView, imageReflection);
								}
								else {
									ImageUtils.showImageInImageView(bitmap, mImageView);
								}
							}
						});
					}
				}
				else {
					final Bitmap bitmap = ImageManager.getInstance().loadBitmapFromAssets(CandiConstants.IMAGE_BROKEN);
					if (bitmap != null) {
						mThreadHandler.post(new Runnable() {

							@Override
							public void run() {
								if (imageRequest.getMakeReflection() && imageReflection != null) {
									String cacheName = ImageManager.getInstance().resolveCacheName(imageRequest.getImageUri());
									Bitmap bitmapReflection = ImageManager.getInstance().getImage(cacheName + ".reflection");
									ImageUtils.showImageInImageView(bitmap, bitmapReflection, mImageView, imageReflection);
								}
								else {
									ImageUtils.showImageInImageView(bitmap, mImageView);
								}
							}
						});
					}
				}
				showLoading(false);

				if (originalImageReadyListener != null) {
					originalImageReadyListener.onComplete(serviceResponse);
				}
			}
		});

		Logger.v(this, "Fetching Image: " + imageRequest.getImageUri());
		ImageManager.getInstance().getImageLoader().fetchImage(imageRequest);
	}

	public void showLoading(boolean loading) {
		if (loading) {
			mImageViewLoading.post(new Runnable() {

				@Override
				public void run() {
					if (mThemeTone.equals("dark")) {
						mImageViewLoading.setBackgroundResource(R.drawable.busy_anim_dark);
					}
					else if (mThemeTone.equals("light")) {
						mImageViewLoading.setBackgroundResource(R.drawable.busy_anim_light);
					}

					final AnimationDrawable animation = (AnimationDrawable) mImageViewLoading.getBackground();
					animation.start();
					mImageViewLoading.setVisibility(View.VISIBLE);
				}
			});
		}
		else {
			mImageViewLoading.post(new Runnable() {

				@Override
				public void run() {
					mImageViewLoading.setBackgroundDrawable(null);
					mImageViewLoading.setVisibility(View.GONE);
				}
			});
		}
	}

	public String getImageUri() {
		return mImageUri;
	}

	public void setImageDrawable(Drawable drawable) {
		ImageUtils.showDrawableInImageView(drawable, mImageView, true);
	}

	public ImageView getImageView() {
		return mImageView;
	}

	public void onDestroy() {
		if (mImageView.getDrawable() != null) {
			BitmapDrawable bitmapDrawable = (BitmapDrawable) mImageView.getDrawable();
			if (bitmapDrawable != null && bitmapDrawable.getBitmap() != null) {
				bitmapDrawable.getBitmap().recycle();
			}
		}
	}
}
