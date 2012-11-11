package com.aircandi.widgets;

import com.aircandi.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ScaleDrawable;
import android.util.AttributeSet;
import android.widget.Button;

public class ComboButton extends Button {

	protected float	mDrawableScale;

	public ComboButton(Context context) {
		super(context);
		setDrawableSize(context, null, 0);
	}

	public ComboButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		setDrawableSize(context, attrs, 0);
	}

	public ComboButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		setDrawableSize(context, attrs, defStyle);
	}

	private void setDrawableSize(Context context, AttributeSet attrs, int defStyle) {
		TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ComboButton, defStyle, 0);
		mDrawableScale = ta.getFloat(R.styleable.ComboButton_drawableScale, 0.5f);

		Drawable[] drawables = getCompoundDrawables();
		for (int i = 0; i < drawables.length; i++) {
			if (drawables[i] != null) {
				Drawable drawable = drawables[i];
				drawable.setBounds(0, 0, (int) (drawable.getIntrinsicWidth() * mDrawableScale), (int) (drawable.getIntrinsicHeight() * mDrawableScale));
				ScaleDrawable sd = new ScaleDrawable(drawable, 0, 20, 20);
				setCompoundDrawables(sd.getDrawable(), null, null, null); //set drawableLeft for example
				break;
			}
		}
	}
}
