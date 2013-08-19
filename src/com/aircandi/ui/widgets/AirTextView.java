package com.aircandi.ui.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import com.aircandi.components.FontManager;

public class AirTextView extends TextView {

	@SuppressWarnings("ucd")
	public AirTextView(Context context) {
		this(context, null);
	}

	@SuppressWarnings("ucd")
	public AirTextView(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.textViewStyle);
	}

	@SuppressWarnings("ucd")
	public AirTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		if (!isInEditMode()) {
			FontManager.getInstance().setTypefaceDefault(this);
		}
	}
}
