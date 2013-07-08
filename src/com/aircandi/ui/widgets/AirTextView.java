package com.aircandi.ui.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import com.aircandi.components.FontManager;

public class AirTextView extends TextView {

	public AirTextView(Context context) {
		super(context);
		FontManager.getInstance().setTypefaceDefault(this);
	}

	public AirTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		FontManager.getInstance().setTypefaceDefault(this);
	}

	public AirTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		FontManager.getInstance().setTypefaceDefault(this);
	}
}
