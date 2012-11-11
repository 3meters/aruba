package com.aircandi.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.aircandi.R;

public class CollectionButton extends FrameLayout {

	private WebImageView	mWebImageView;
	private TextView		mTextViewCount;

	public CollectionButton(Context context) {
		this(context, null);
	}

	public CollectionButton(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CollectionButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initialize();
	}

	private void initialize() {
		LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.widget_collection_button, this);

		if (!isInEditMode()) {
			mWebImageView = (WebImageView) view.findViewById(R.id.image_collection);
			mTextViewCount = (TextView) view.findViewById(R.id.text_children_count);
		}
	}

	public WebImageView getWebImageView() {
		return mWebImageView;
	}

	public TextView getTextViewCount() {
		return mTextViewCount;
	}

}
