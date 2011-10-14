package com.proxibase.aircandi.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class PagerTextView extends TextView {

	private int		mInitialCenterX;
	private int		mInitialLeft;
	private int		mInitialRight;
	private boolean	mFirstLayout	= true;

	public PagerTextView(Context context) {
		super(context);
	}

	public PagerTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public PagerTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		if (mFirstLayout) {
			super.onLayout(changed, left, top, right, bottom);
			mInitialCenterX = this.getCenterX();
			mInitialLeft = this.getLeft();
			mInitialRight = this.getRight();
			mFirstLayout = false;
		}
	}

	/**
	 * Center x co-ordinate relative to parent.
	 */
	public int getCenterX() {
		return this.getLeft() + (this.getWidth() / 2);
	}

	/**
	 * Set center x position relative to parent. Will reset
	 * back to layout settings after next layout pass.
	 */
	public void setCenterX(int centerX) {

		/* Triggers call to onLayout() */
		this.layout(mInitialLeft + (centerX - mInitialCenterX), this.getTop(), mInitialRight + (centerX - mInitialCenterX), this.getBottom());
	}

	/**
	 * Center x position relative to parent immediately
	 * after performing the last layout.
	 */
	public int getInitialCenterX() {
		return mInitialCenterX;
	}

	public int getInitialLeft() {
		return mInitialLeft;
	}

	public int getInitialRight() {
		return mInitialRight;
	}
}
