package com.aircandi.ui.widgets;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class AirViewPager extends ViewPager {
	public AirViewPager(Context context) {
		super(context);
	}

	public AirViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent arg0) {
		/* Never allow swiping to switch between pages */
		return false;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		/* Never allow swiping to switch between pages */
		return false;
	}
}
