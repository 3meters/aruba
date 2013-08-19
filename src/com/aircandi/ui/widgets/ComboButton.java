package com.aircandi.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.aircandi.R;

@SuppressWarnings("ucd")
public class ComboButton extends RelativeLayout {

	private Integer			mLayoutId;
	private Integer			mDrawableId;
	private String			mLabel;
	private ViewGroup		mLayout;
	private ImageView		mImageIcon;
	private TextView		mTextLabel;
	private ViewAnimator	mViewAnimator;

	public ComboButton(Context context) {
		this(context, null);
	}

	public ComboButton(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ComboButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		if (attrs != null) {

			final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ComboButton, defStyle, 0);
			mLayoutId = ta.getResourceId(R.styleable.ComboButton_layout, R.layout.widget_combo_button_no_text);
			mDrawableId = ta.getResourceId(R.styleable.ComboButton_drawable, R.drawable.ic_launcher);
			mLabel = ta.getString(R.styleable.ComboButton_label);

			ta.recycle();
			initialize();
			if (!this.isInEditMode()) {
				draw();
			}
		}
	}

	private void initialize() {

		mLayout = (ViewGroup) LayoutInflater.from(this.getContext()).inflate(mLayoutId, this, true);
		if (!this.isInEditMode()) {
			mTextLabel = (TextView) mLayout.findViewById(R.id.button_label);
			mImageIcon = (ImageView) mLayout.findViewById(R.id.button_image);
			mViewAnimator = (ViewAnimator) mLayout.findViewById(R.id.button_animator);
		}
	}

	private void draw() {
		if (mDrawableId != 0) {
			mImageIcon.setImageDrawable(getResources().getDrawable(mDrawableId));
		}
		else {
			mImageIcon.setVisibility(View.GONE);
		}
		if (mTextLabel != null) {
			if (mLabel != null) {
				mTextLabel.setText(mLabel);
			}
			else {
				mTextLabel.setVisibility(View.GONE);
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Setters/getters
	// --------------------------------------------------------------------------------------------

	public void setLayoutId(Integer layoutId) {
		mLayoutId = layoutId;
	}

	public Integer getDrawableId() {
		return mDrawableId;
	}

	public void setDrawableId(Integer drawableId) {
		mDrawableId = drawableId;
		draw();
	}

	public String getLabel() {
		return mLabel;
	}

	public void setLabel(String label) {
		mLabel = label;
		draw();
	}

	public ImageView getImageIcon() {
		return mImageIcon;
	}

	public void setImageIcon(ImageView imageIcon) {
		mImageIcon = imageIcon;
	}

	public ViewAnimator getViewAnimator() {
		return mViewAnimator;
	}
}
