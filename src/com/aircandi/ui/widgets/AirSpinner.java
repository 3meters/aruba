package com.aircandi.ui.widgets;

import com.aircandi.components.FontManager;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Spinner;

public class AirSpinner extends Spinner {

	public AirSpinner(Context context) {
		this(context, null);
	}

	public AirSpinner(Context context, int mode) {
		this(context, null, android.R.attr.spinnerStyle, mode);
	}

	public AirSpinner(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.spinnerStyle);
	}

	public AirSpinner(Context context, AttributeSet attrs, int defStyle) {
		this(context, attrs, defStyle, -1);
	}

	public AirSpinner(Context context, AttributeSet attrs, int defStyle, int mode) {
		super(context, attrs, defStyle, mode);
		if (!isInEditMode()) {
			View view = this.getSelectedView();
			FontManager.getInstance().setTypefaceBoldDefault(null);
		}

	}

}
