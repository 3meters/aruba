package com.aircandi.ui.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

import com.aircandi.components.FontManager;

public class AirButton extends Button {

	public AirButton(Context context) {
		super(context);
		FontManager.getInstance().setTypefaceRegular(this);
	}

	public AirButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		FontManager.getInstance().setTypefaceRegular(this);
	}

	public AirButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		FontManager.getInstance().setTypefaceRegular(this);
	}
}
