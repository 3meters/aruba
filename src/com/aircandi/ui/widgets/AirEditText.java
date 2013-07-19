package com.aircandi.ui.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

import com.aircandi.components.FontManager;

public class AirEditText extends EditText {

	public AirEditText(Context context) {
		super(context);
		if (!isInEditMode()) {
			FontManager.getInstance().setTypefaceDefault(this);
		}
	}

	public AirEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
		if (!isInEditMode()) {
			FontManager.getInstance().setTypefaceDefault(this);
		}
	}

	public AirEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		if (!isInEditMode()) {
			FontManager.getInstance().setTypefaceDefault(this);
		}
	}
}
