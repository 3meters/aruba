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

import com.aircandi.beta.R;
import com.aircandi.components.FontManager;

@SuppressWarnings("ucd")
public class ComboButton extends RelativeLayout {

	private Integer			mLayoutId;
	private Integer			mDrawableId;
	private String			mLabel;
	private ViewGroup		mLayout;
	private ImageView		mImageIcon;
	private TextView		mTextLabel;
	private LayoutInflater	mInflater;

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
			mLayoutId = ta.getResourceId(R.styleable.ComboButton_layout, R.layout.widget_combo_button);
			mDrawableId = ta.getResourceId(R.styleable.ComboButton_drawable, 0);
			mLabel = ta.getString(R.styleable.ComboButton_label);

			ta.recycle();
			initialize();
			draw();
		}
	}

	private void initialize() {

		mInflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mLayout = (ViewGroup) mInflater.inflate(mLayoutId, this, true);
		mTextLabel = (TextView) mLayout.findViewById(R.id.label);
		mImageIcon = (ImageView) mLayout.findViewById(R.id.icon);
		FontManager.getInstance().setTypefaceRegular(mTextLabel);
	}

	private void draw() {
		if (mDrawableId != 0) {
			mImageIcon.setImageResource(mDrawableId);
		}
		else {
			mImageIcon.setVisibility(View.GONE);
		}
		if (mLabel != null) {
			mTextLabel.setText(mLabel);
		}
		else {
			mTextLabel.setVisibility(View.GONE);
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

}
