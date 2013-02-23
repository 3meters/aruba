package com.aircandi.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aircandi.R;
import com.aircandi.components.FontManager;

@SuppressWarnings("ucd")
public class BuilderButton extends RelativeLayout {

	private TextView		mTextView;
	private LinearLayout	mViewGroup;
	private String			mHint;
	private String			mThemeTone;
	private Integer			mLayoutId;

	public BuilderButton(Context context) {
		this(context, null);
	}

	public BuilderButton(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public BuilderButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.BuilderButton, defStyle, 0);
		mHint = ta.getString(R.styleable.BuilderButton_hint);
		mLayoutId = ta.getResourceId(R.styleable.BuilderButton_layout, R.layout.widget_builder_button);
		ta.recycle();

		final TypedValue resourceName = new TypedValue();
		if (context.getTheme().resolveAttribute(R.attr.themeTone, resourceName, true)) {
			mThemeTone = (String) resourceName.coerceToString();
		}

		initialize();
	}

	private void initialize() {
		final LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final View view = inflater.inflate(mLayoutId, this);

		mTextView = (TextView) view.findViewById(R.id.builder_text);
		mViewGroup = (LinearLayout) view.findViewById(R.id.builder_images);
		FontManager.getInstance().setTypefaceDefault(mTextView);

		if (mTextView != null && mHint != null) {
			mTextView.setTextColor(getResources().getColor(R.color.edit_hint));
			mTextView.setText(mHint);
		}
	}

	public void setText(String text) {
		if (mTextView != null) {
			if (text != null && !text.equals("")) {
				if (mThemeTone.equals("dark")) {
					mTextView.setTextColor(getResources().getColor(R.color.text_dark));
				}
				else {
					mTextView.setTextColor(getResources().getColor(R.color.text_light));
				}
				mTextView.setText(text);
			}
			else {
				mTextView.setTextColor(getResources().getColor(R.color.edit_hint));
				mTextView.setText(mHint);
			}
		}
	}

	public String getText() {
		if (mTextView != null) {
			final String text = mTextView.getText().toString();
			if (!text.equals(mHint)) {
				return text;
			}
		}
		return null;
	}

	public LinearLayout getViewGroup() {
		return mViewGroup;
	}

	public void setViewGroup(LinearLayout viewGroup) {
		mViewGroup = viewGroup;
	}

	public TextView getTextView() {
		return mTextView;
	}

	public void setTextView(TextView textView) {
		mTextView = textView;
	}
}
