package com.aircandi.ui.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.GridView;

@SuppressWarnings("ucd")
public class FittedGridView extends GridView {

	private boolean	mExpanded	= false;

	public FittedGridView(Context context) {
		super(context);
	}

	public FittedGridView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public FittedGridView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

		// HACK! TAKE THAT ANDROID!
		if (mExpanded) {
			// Calculate entire height by providing a very large height hint.
			// But do not use the highest 2 bits of this integer; those are
			// reserved for the MeasureSpec mode.
			int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
			super.onMeasure(widthMeasureSpec, expandSpec);
			ViewGroup.LayoutParams params = getLayoutParams();
			params.height = getMeasuredHeight();
		}
		else {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		}

		//		int heightSpec;
		//
		//		if (getLayoutParams().height == LayoutParams.WRAP_CONTENT) {
		//			/*
		//			 * The great Android "hackatlon", the love, the magic.
		//			 * The two leftmost bits in the height measure spec have
		//			 * a special meaning, hence we can't use them to describe height.
		//			 */
		//			heightSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
		//		}
		//		else {
		//			/* Any other height should be respected as is. */
		//			heightSpec = heightMeasureSpec;
		//		}
		//
		//		super.onMeasure(widthMeasureSpec, heightSpec);
	}

	public void setExpanded(boolean expanded)
	{
		mExpanded = expanded;
	}

	public boolean isExpanded()
	{
		return mExpanded;
	}
}
