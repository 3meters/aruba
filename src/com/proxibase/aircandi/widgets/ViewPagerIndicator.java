package com.proxibase.aircandi.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import com.proxibase.aircandi.activities.R;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.utils.Log;

/**
 * An small bar indicating the title of the previous, current and next page to be shown in a ViewPager.
 * Made to resemble the indicator in the Google+ application in function.
 */
public class ViewPagerIndicator extends RelativeLayout implements OnPageChangeListener {

	PageInfoProvider	mPageInfoProvider;
	View				mBoundView;
	int					mPageCount;
	boolean				mFirstLayout		= true;
	int					mPositionCurrent;

	PagerTextView		mTextViewPrevious;
	PagerTextView		mTextViewCurrent;
	PagerTextView		mTextViewNext;

	int[]				mTextColorFocused	= new int[] { 0, 0, 0 };
	int[]				mTextColorUnfocused	= new int[] { 0, 0, 0 };

	public ViewPagerIndicator(Context context) {
		this(context, null);
	}

	public ViewPagerIndicator(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ViewPagerIndicator(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ViewPagerIndicator, defStyle, 0);
		String colorString = ta.getString(R.styleable.ViewPagerIndicator_textColorFocused);
		if (colorString != null) {

			setTextColorFocused(Color.parseColor(colorString));
		}
		colorString = ta.getString(R.styleable.ViewPagerIndicator_textColorUnfocused);
		if (colorString != null) {
			setTextColorUnfocused(Color.parseColor(colorString));
		}
		ta.recycle();
	}

	/**
	 * Sets the initial state and page info provider. Call this when
	 * titles wil be available from the page info provider.
	 * 
	 * @param startPos The initially selected element in the ViewPager
	 * @param pageCount Total amount of elements in the ViewPager
	 * @param pageInfoProvider Interface that returns page titles
	 */
	public void initialize(int startPageNumber, int pageCount, PageInfoProvider pageInfoProvider) {
		if (mBoundView == null)
			throw new IllegalStateException("bindToView must be called before initialization.");

		if (startPageNumber < 1)
			throw new IllegalArgumentException("startPageNumber must be 1 or more.");

		this.mPageCount = pageCount;
		mPageInfoProvider = pageInfoProvider;
		setPageTitles(startPageNumber);
		mPositionCurrent = startPageNumber - 1;
		requestLayout();
	}

	/**
	 * Set layout view to bind to.
	 */
	public void bindToView(View view) {
		mBoundView = view;
		mTextViewCurrent = (PagerTextView) view.findViewById(R.id.txt_pager_indicator_current);
		mTextViewPrevious = (PagerTextView) view.findViewById(R.id.txt_pager_indicator_previous);
		mTextViewNext = (PagerTextView) view.findViewById(R.id.txt_pager_indicator_next);
		if (mTextViewCurrent != null)
			mTextViewCurrent.setTextColor(Color.argb(255, mTextColorFocused[0], mTextColorFocused[1], mTextColorFocused[2]));
		if (mTextViewPrevious != null)
			mTextViewPrevious.setTextColor(Color.argb(255, mTextColorUnfocused[0], mTextColorUnfocused[1], mTextColorUnfocused[2]));
		if (mTextViewNext != null)
			mTextViewNext.setTextColor(Color.argb(255, mTextColorUnfocused[0], mTextColorUnfocused[1], mTextColorUnfocused[2]));

		this.removeAllViews();
		this.addView(mBoundView);
	}

	// --------------------------------------------------------------------------------------------
	// Pager methods and callbacks
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		if (mFirstLayout) {
			super.onLayout(changed, left, top, right, bottom);
			mFirstLayout = false;
		}
	}

	@Override
	public void onPageSelected(int position) {
		Log.d(CandiConstants.APP_NAME, "ViewPagerIndicator", "PageSelected Position: " + String.valueOf(position));
	}

	@Override
	public void onPageScrollStateChanged(int state) {
		Log.d(CandiConstants.APP_NAME, "ViewPagerIndicator", "PageScrollStateChanged State: " + String.valueOf(state));
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, final int positionOffsetPixels) {
		Log.d(CandiConstants.APP_NAME, "ViewPagerIndicator", "Position: " + String.valueOf(position)
																		+ " PositionOffset: "
																		+ String.valueOf(positionOffset)
																		+ " PositionOffsetPixels: "
																		+ String.valueOf(positionOffsetPixels));

		int currentCenterSpan = getSpanBetweenCenters(mTextViewCurrent);
		int nextCenterSpan = getSpanBetweenCenters(mTextViewNext);

		if (position == mPositionCurrent) {
			mTextViewCurrent.setCenterX(mTextViewCurrent.getInitialCenterX() - (int) ((float) currentCenterSpan * positionOffset));
			mTextViewNext.setCenterX(mTextViewNext.getInitialCenterX() - (int) ((float) nextCenterSpan * positionOffset));
			updateColor(positionOffsetPixels);
		}
		else {

			/* Configure the back button */
		}
		invalidate();
	}

	/**
	 * Update text depending on it's position
	 * 
	 * @param currentPosition
	 */
	void setPageTitles(int currentPosition) {

		/* Text for previous */
		if (currentPosition == 1) {
			mTextViewPrevious.setText("");
		}
		else {
			mTextViewPrevious.setText(mPageInfoProvider.getTitle(currentPosition - 2));
		}

		/* Text for current */
		mTextViewCurrent.setText(mPageInfoProvider.getTitle(currentPosition - 1));

		/* Text for next */
		if (currentPosition + 1 <= this.mPageCount) {
			mTextViewNext.setText(mPageInfoProvider.getTitle(currentPosition));
		}
		else {
			mTextViewNext.setText("");
		}
	}

	private int getSpanBetweenCenters(PagerTextView pagerTextView) {
		int span = (((View) pagerTextView.getParent()).getWidth() / 2) - (pagerTextView.getInitialCenterX() - pagerTextView.getInitialLeft());
		return span;
	}

	public void setTextColorFocused(int color) {
		mTextColorFocused[0] = Color.red(color);
		mTextColorFocused[1] = Color.green(color);
		mTextColorFocused[2] = Color.blue(color);
		if (mTextViewCurrent != null)
			mTextViewCurrent.setTextColor(color);
	}

	public void setTextColorUnfocused(int color) {
		mTextColorUnfocused[0] = Color.red(color);
		mTextColorUnfocused[1] = Color.green(color);
		mTextColorUnfocused[2] = Color.blue(color);
		if (mTextViewNext != null)
			mTextViewNext.setTextColor(color);
		if (mTextViewPrevious != null)
			mTextViewPrevious.setTextColor(color);
	}

	/**
	 * Fade "currently showing" color depending on it's position
	 * 
	 * @param offset
	 */
	void updateColor(int offset) {
		offset = Math.abs(offset);
		float fraction = offset / ((float) this.getWidth() / 4.0f);
		fraction = Math.min(1, fraction);
		int r = (int) (mTextColorUnfocused[0] * fraction + mTextColorFocused[0] * (1 - fraction));
		int g = (int) (mTextColorUnfocused[1] * fraction + mTextColorFocused[1] * (1 - fraction));
		int b = (int) (mTextColorUnfocused[2] * fraction + mTextColorFocused[2] * (1 - fraction));
		if (mTextViewCurrent != null)
			mTextViewCurrent.setTextColor(Color.argb(255, r, g, b));

		r = (int) (mTextColorFocused[0] * fraction + mTextColorUnfocused[0] * (1 - fraction));
		g = (int) (mTextColorFocused[1] * fraction + mTextColorUnfocused[1] * (1 - fraction));
		b = (int) (mTextColorFocused[2] * fraction + mTextColorUnfocused[2] * (1 - fraction));

		if (mTextViewNext != null)
			mTextViewNext.setTextColor(Color.argb(255, r, g, b));
	}

	public interface PageInfoProvider {

		String getTitle(int pos);
	}
}
