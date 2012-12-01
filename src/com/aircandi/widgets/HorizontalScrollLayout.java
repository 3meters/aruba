package com.aircandi.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import com.aircandi.R;

public class HorizontalScrollLayout extends HorizontalScrollView {

	private LayoutInflater	mInflater;
	private Integer			mLayoutItemId;
	private Context			mContext;
	private LinearLayout	mLayout;

	public HorizontalScrollLayout(Context context) {
		this(context, null);
	}

	public HorizontalScrollLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public HorizontalScrollLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.HorizontalScrollLayout, defStyle, 0);
		mLayoutItemId = ta.getResourceId(R.styleable.HorizontalScrollLayout_layout, R.layout.temp_place_photo_item);
		mInflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		initialize();
	}

	private synchronized void initialize() {

		mLayout = new LinearLayout(mContext);
		mLayout.setOrientation(LinearLayout.HORIZONTAL);
		FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		this.addView(mLayout, layoutParams);

		/* Show something in design mode */
		if (isInEditMode()) {
			int itemCount = 0;
			while (itemCount < 3) {
				mInflater.inflate(mLayoutItemId, mLayout, true);
				this.invalidate();
				mLayout.invalidate();
				itemCount++;
			}
		}
	}

	@Override
	public void addView(View child) {
		mLayout.addView(child);
		mLayout.invalidate();
	}
}
