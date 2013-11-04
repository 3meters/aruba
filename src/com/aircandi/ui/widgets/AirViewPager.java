package com.aircandi.ui.widgets;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class AirViewPager extends ViewPager {
	
	private boolean mSwipeable = true;

    public AirViewPager(Context context) {
        super(context);
    }

    public AirViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setSwipeable(boolean swipeable) {
        this.mSwipeable = swipeable;
    }
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent arg0) {
        return (this.mSwipeable) ? super.onInterceptTouchEvent(arg0) : false; 
    }    

    @Override
    public boolean onTouchEvent(MotionEvent arg0) {
        return (this.mSwipeable) ? super.onTouchEvent(arg0) : false; 
    }	
}
