package com.proxibase.aircandi.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.ImageView;

import com.proxibase.aircandi.R;
import com.proxibase.aircandi.utils.Exceptions;
import com.proxibase.aircandi.utils.ImageManager;
import com.proxibase.aircandi.utils.ImageUtils;
import com.proxibase.aircandi.utils.Logger;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest;
import com.proxibase.aircandi.utils.ImageManager.ImageRequestListener;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;

public class WebImageView extends ImageView {

	private Drawable	mPlaceholder	= null;
	private String		mImageUri;
	private int			mProgressColor;
	private Handler		mThreadHandler	= new Handler();

	public WebImageView(Context context) {
		super(context);
	}

	public WebImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public WebImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		//		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SuperImageView, defStyle, 0);
		//		Drawable d = a.getDrawable(R.styleable.SuperImageView_placeholder);
		//		if (d != null) {
		//			setDrawablePlaceholder(d);
		//		}
		//		setProgressColor(a.getColor(R.styleable.SuperImageView_progressColor, Color.parseColor("#ffffff00")));

		TypedValue resourceName = new TypedValue();
		if (context.getTheme().resolveAttribute(R.attr.textureBodyZone, resourceName, true)) {
			String placeholderAsset = (String) resourceName.coerceToString();
			Bitmap placeholderBitmap = null;
			try {
				placeholderBitmap = ImageManager.getInstance().loadBitmapFromAssets(placeholderAsset);
				mPlaceholder = new BitmapDrawable(placeholderBitmap);
			}
			catch (ProxibaseException exception) {
				Exceptions.Handle(exception);
			}
			setImageBitmap(placeholderBitmap);
		}
	}

	public void setDrawablePlaceholder(Drawable drawablePlaceholder) {
		this.mPlaceholder = drawablePlaceholder;
	}

	public Drawable getDrawablePlaceholder() {
		return mPlaceholder;
	}

	public void setProgressColor(int progressColor) {
		this.mProgressColor = progressColor;
	}

	public int getProgressColor() {
		return mProgressColor;
	}

	public void setImageRequest(final ImageRequest imageRequest, final ImageView imageReflection) {
		
		final ImageRequestListener originalImageReadyListener = imageRequest.imageReadyListener;

		imageRequest.imageReadyListener = new ImageRequestListener() {

			@Override
			public void onImageReady(final Bitmap bitmap) {
				if (bitmap != null) {
					if (originalImageReadyListener != null) {
						originalImageReadyListener.onImageReady(bitmap);
					}
					mThreadHandler.post(new Runnable() {

						@Override
						public void run() {
							if (imageRequest.makeReflection && imageReflection != null)
							{
								String cacheName = ImageManager.getInstance().resolveCacheName(imageRequest.imageUri);
								Bitmap bitmapReflection = ImageManager.getInstance().getImage(cacheName + ".reflection");
								ImageUtils.showImageInImageView(bitmap, bitmapReflection, WebImageView.this, imageReflection);
							}
							else
							{
								ImageUtils.showImageInImageView(bitmap, WebImageView.this);
							}
						}
					});
				}
			}
		};

		Logger.v(this, "Fetching Image: " + imageRequest.imageUri);
		ImageManager.getInstance().getImageLoader().fetchImage(imageRequest);
	}

	public String getImageUri() {
		return mImageUri;
	}
}
