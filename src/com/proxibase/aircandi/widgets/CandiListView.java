package com.proxibase.aircandi.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.ListView;

import com.proxibase.aircandi.R;

@SuppressWarnings("unused")
public class CandiListView extends ListView {

	private Handler	mThreadHandler	= new Handler();
	private Integer	mItemLayoutId;
	private String	mThemeTone;

	public CandiListView(Context context) {
		this(context, null);
	}

	public CandiListView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CandiListView(Context context, AttributeSet attributes, int defStyle) {
		super(context, attributes, defStyle);

		TypedArray ta = context.obtainStyledAttributes(attributes, R.styleable.CandiListView, defStyle, 0);

		mItemLayoutId = ta.getResourceId(R.styleable.WebImageView_layout, R.layout.temp_webimageview);

		TypedValue resourceName = new TypedValue();
		if (context.getTheme().resolveAttribute(R.attr.themeTone, resourceName, true)) {
			mThemeTone = (String) resourceName.coerceToString();
		}

		ta.recycle();
		initialize();
	}

	private void initialize() {}
}
