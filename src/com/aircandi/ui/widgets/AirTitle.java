package com.aircandi.ui.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import com.aircandi.components.FontManager;

public class AirTitle extends TextView {

	@SuppressWarnings("ucd")
	public AirTitle(Context context) {
		this(context, null);
	}

	@SuppressWarnings("ucd")
	public AirTitle(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.textViewStyle);
	}

	@SuppressWarnings("ucd")
	public AirTitle(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		if (!isInEditMode()) {
			FontManager.getInstance().setTypefaceLight(this);
		}
	}
}
