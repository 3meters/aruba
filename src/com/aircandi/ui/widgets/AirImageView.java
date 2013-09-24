package com.aircandi.ui.widgets;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.aircandi.R;
import com.aircandi.service.objects.Photo;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.UI;

@SuppressWarnings("ucd")
public class AirImageView extends RelativeLayout {

	private ImageView					mImageMain;
	private ImageView					mImageZoom;
	private ProgressBar					mProgressBar;

	private Photo						mPhoto;
	private final Handler				mThreadHandler		= new Handler();

	private Integer						mSizeHint;

	private boolean						mShowBusy;
	private Integer						mLayoutId;
	private Integer						mBrokenDrawable;
	private Photo						mBrokenPhoto;
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

	public AirImageView(Context context) {
		this(context, null);
	}

	public AirImageView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public AirImageView(Context context, AttributeSet attributes, int defStyle) {
		super(context, attributes, defStyle);

		final TypedArray ta = context.obtainStyledAttributes(attributes, R.styleable.AirImageView, defStyle, 0);

		mSizeHint = ta.getDimensionPixelSize(R.styleable.AirImageView_sizeHint, Integer.MAX_VALUE);
		mShowBusy = ta.getBoolean(R.styleable.AirImageView_showBusy, true);
		mLayoutId = ta.getResourceId(R.styleable.AirImageView_layout, R.layout.widget_webimageview);
		mBrokenDrawable = ta.getResourceId(R.styleable.AirImageView_brokenDrawable, R.drawable.img_broken);

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
		final View view = LayoutInflater.from(context).inflate(mLayoutId, this, true);

		mImageMain = (ImageView) view.findViewById(R.id.image_main);

		if (!isInEditMode()) {
			mImageZoom = (ImageView) view.findViewById(R.id.image_zoom);
			mProgressBar = (ProgressBar) view.findViewById(R.id.image_progress);
		}

		if (mImageMain != null) {
			if (!(mImageMain instanceof ImageViewTouch)) {
				mImageMain.setScaleType(mScaleType);
			}
			if (isInEditMode()) {
				mImageMain.setImageResource(R.drawable.img_placeholder_logo);
			}
			mImageMain.invalidate();
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

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if (mImageMain != null) {
			mImageMain.layout(l, t, r, b);
		}
		super.onLayout(changed, l, t, r, b);
	}

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mImageMain instanceof ImageViewTouch) {
			return mImageMain.onTouchEvent(event);
		}
		else {
			return super.onTouchEvent(event);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	public void showBroken(final Boolean visible) {
		mThreadHandler.post(new Runnable() {

			@Override
			public void run() {
				if (visible) {
					final Drawable drawable = AirImageView.this.getContext().getResources().getDrawable(mBrokenDrawable);
					UI.showDrawableInImageView(drawable, mImageMain, true, Animate.fadeInMedium());
				}
			}
		});
	}

	public void showLoading(final Boolean visible) {
		mThreadHandler.post(new Runnable() {

			@Override
			public void run() {
				mProgressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
			}
		});
	}

	// --------------------------------------------------------------------------------------------
	// Properties
	// --------------------------------------------------------------------------------------------

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

	public Photo getPhoto() {
		return mPhoto;
	}

	public void setPhoto(Photo photo) {
		mPhoto = photo;
	}

	public void setBrokenDrawable(Integer brokenDrawable) {
		mBrokenDrawable = brokenDrawable;
	}

	public Photo getBrokenPhoto() {
		return mBrokenPhoto;
	}

	public void setBrokenPhoto(Photo brokenPhoto) {
		mBrokenPhoto = brokenPhoto;
	}
}
