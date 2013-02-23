package com.aircandi.ui.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.widget.ScrollView;

import com.aircandi.CandiConstants;

public class BounceScrollView extends ScrollView {


	private final Context	mContext;
	private int		mMaxYOverscrollDistance;

	public BounceScrollView(Context context) {
		super(context);
		mContext = context;
		initBounceScrollView();
	}

	public BounceScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		initBounceScrollView();
	}

	public BounceScrollView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		initBounceScrollView();
	}

	private void initBounceScrollView() {
		/*
		 * Get the density of the screen and do some maths with it on the max overscroll distance
		 * variable so that you get similar behaviors no matter what the screen size
		 */
		final DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
		final float density = metrics.density;
		mMaxYOverscrollDistance = (int) (density * CandiConstants.MAX_Y_OVERSCROLL_DISTANCE);
	}

	@Override
	protected boolean overScrollBy(int deltaX
			, int deltaY
			, int scrollX
			, int scrollY
			, int scrollRangeX
			, int scrollRangeY
			, int maxOverScrollX
			, int maxOverScrollY
			, boolean isTouchEvent) {
		/*
		 * This is where the magic happens, we have replaced the incoming maxOverScrollY 
		 * with our own custom variable mMaxYOverscrollDistance;
		 */
		return super.overScrollBy(deltaX, deltaY, scrollX, scrollY, scrollRangeX, scrollRangeY, maxOverScrollX, mMaxYOverscrollDistance, isTouchEvent);
	}

}
