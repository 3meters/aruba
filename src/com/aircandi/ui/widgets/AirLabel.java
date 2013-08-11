package com.aircandi.ui.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import com.aircandi.components.FontManager;

public class AirLabel extends TextView {

	@SuppressWarnings("ucd")
	public AirLabel(Context context) {
		super(context);
		if (!isInEditMode()) {
			FontManager.getInstance().setTypefaceBoldDefault(this);
		}
	}

	@SuppressWarnings("ucd")
	public AirLabel(Context context, AttributeSet attrs) {
		super(context, attrs);
		if (!isInEditMode()) {
			FontManager.getInstance().setTypefaceBoldDefault(this);
		}
	}

	@SuppressWarnings("ucd")
	public AirLabel(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		if (!isInEditMode()) {
			FontManager.getInstance().setTypefaceBoldDefault(this);
		}
	}
}
