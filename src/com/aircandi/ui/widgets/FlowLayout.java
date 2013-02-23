package com.aircandi.ui.widgets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

@SuppressWarnings("ucd")
public class FlowLayout extends ViewGroup
{
	private int[]					mRowHeights;
	private int[]					mRowWidths;
	private int						mSpacingVertical	= 0;
	private int						mSpacingHorizontal	= 0;
	private final List<RowMeasurement>	mRows				= new ArrayList<RowMeasurement>();

	public FlowLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		final int maxInternalWidth = MeasureSpec.getSize(widthMeasureSpec) - getHorizontalPadding();
		final int maxInternalHeight = MeasureSpec.getSize(heightMeasureSpec) - getVerticalPadding();

		mRows.clear();

		RowMeasurement currentRow = null;

		for (View child : getLayoutChildren()) {

			LayoutParams lp = (LayoutParams) child.getLayoutParams();

			int childWidthSpec = createChildMeasureSpec(lp.width, maxInternalWidth, widthMode);
			int childHeightSpec = createChildMeasureSpec(lp.height, maxInternalHeight, heightMode);
			child.measure(childWidthSpec, childHeightSpec);

			int childWidth = child.getMeasuredWidth();
			int childHeight = child.getMeasuredHeight();

			/* If child won't fit then create new row */
			if (currentRow == null || currentRow.isWouldExceedMax(lp.leftMargin + childWidth + lp.rightMargin)) {
				currentRow = new RowMeasurement(maxInternalWidth, widthMode);
				mRows.add(currentRow);
			}

			currentRow.addChildDimensions(lp.leftMargin + childWidth + lp.rightMargin + mSpacingHorizontal
					, lp.topMargin + childHeight + lp.bottomMargin);
		}

		int longestRowWidth = 0;
		int totalRowHeight = 0;
		final int rowCount = mRows.size();
		mRowHeights = new int[rowCount];
		mRowWidths = new int[rowCount];

		RowMeasurement row;
		int rowHeight;
		int rowWidth;
		for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
			row = mRows.get(rowIndex);
			rowHeight = row.getHeight();
			rowWidth = row.getWidth();
			mRowHeights[rowIndex] = rowHeight;
			mRowWidths[rowIndex] = rowWidth;
			totalRowHeight = totalRowHeight + rowHeight + mSpacingVertical;
			longestRowWidth = Math.max(longestRowWidth, row.getWidth());
		}

		setMeasuredDimension((widthMode == MeasureSpec.EXACTLY) ? MeasureSpec.getSize(widthMeasureSpec) : (longestRowWidth + getHorizontalPadding())
				, (heightMode == MeasureSpec.EXACTLY) ? MeasureSpec.getSize(heightMeasureSpec) : (totalRowHeight + getVerticalPadding()));
	}

	private int createChildMeasureSpec(int childLayoutParam, int max, int parentMode) {
		int spec;
		if (childLayoutParam == ViewGroup.LayoutParams.MATCH_PARENT) {
			spec = MeasureSpec.makeMeasureSpec(max, MeasureSpec.EXACTLY);
		}
		else if (childLayoutParam == ViewGroup.LayoutParams.WRAP_CONTENT) {
			spec = MeasureSpec.makeMeasureSpec(max, ((parentMode == MeasureSpec.UNSPECIFIED) ? MeasureSpec.UNSPECIFIED : MeasureSpec.AT_MOST));
		}
		else {
			spec = MeasureSpec.makeMeasureSpec(childLayoutParam, MeasureSpec.EXACTLY);
		}
		return spec;
	}

	@Override
	protected LayoutParams generateDefaultLayoutParams() {
		return new LayoutParams();
	}

	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams layoutParams) {
		return layoutParams instanceof LayoutParams;
	}

	@Override
	public LayoutParams generateLayoutParams(AttributeSet attrs) {
		return new LayoutParams(getContext(), attrs);
	}

	@Override
	protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
		return new LayoutParams(p);
	}

	@Override
	protected void onLayout(boolean changed, int leftPosition, int topPosition, int rightPosition, int bottomPosition) {

		final int widthOffset = getMeasuredWidth() - getPaddingRight();
		int childLeft = getPaddingLeft();
		int childTop = getPaddingTop();

		int rowIndex = 0;
		for (View child : getLayoutChildren()) {

			int childWidth = child.getMeasuredWidth();
			int childHeight = child.getMeasuredHeight();

			LayoutParams lp = (LayoutParams) child.getLayoutParams();
			if ((childLeft + lp.leftMargin + childWidth + lp.rightMargin) > widthOffset) {
				childLeft = getPaddingLeft();
				childTop = childTop + mRowHeights[rowIndex] + mSpacingVertical;
				rowIndex = rowIndex + 1;
			}

			int _y; // $codepro.audit.disable localVariableNamingConvention
			if (lp.centerVertical) {
				_y = childTop + ((mRowHeights[rowIndex] - childHeight) >> 1);
			}
			else {
				_y = childTop;
			}

			int _x; // $codepro.audit.disable localVariableNamingConvention
			if (lp.centerHorizontal) {
				childLeft += lp.leftMargin;
				_x = (((rightPosition - leftPosition) - mRowWidths[rowIndex]) >> 1) + childLeft;
			}
			else {
				_x = childLeft += lp.leftMargin;
			}

			child.layout(_x, _y, _x + childWidth, _y + childHeight);
			childLeft += childWidth + lp.rightMargin + mSpacingHorizontal;
		}
	}

	private Collection<View> getLayoutChildren() {
		final int count = getChildCount();
		final Collection<View> children = new ArrayList<View>(count);
		View child;
		for (int index = 0; index < count; index++) {
			child = getChildAt(index);
			if (child.getVisibility() != View.GONE){
				children.add(child);
			}
		}
		return children;
	}

	private int getVerticalPadding() {
		return getPaddingTop() + getPaddingBottom();
	}

	private int getHorizontalPadding() {
		return getPaddingLeft() + getPaddingRight();
	}

	public void setSpacingVertical(int spacingVertical) {
		mSpacingVertical = spacingVertical;
	}

	public void setSpacingHorizontal(int spacingHorizontal) {
		mSpacingHorizontal = spacingHorizontal;
	}

	@SuppressWarnings("ucd")
	public static class LayoutParams extends MarginLayoutParams {
		private boolean	centerVertical;
		private boolean	centerHorizontal;

		public LayoutParams() {
			this(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		}

		public LayoutParams(int width, int height) {
			super(width, height);
			setCenterVertical(false);
		}

		public LayoutParams(Context context, AttributeSet attrs) {
			super(context, attrs);
			final TypedArray attributes = context.obtainStyledAttributes(attrs, new int[] { android.R.attr.layout_centerVertical });
			setCenterVertical(attributes.getBoolean(0, false));
			attributes.recycle();
		}

		public LayoutParams(ViewGroup.LayoutParams source) {
			super(source);
		}

		public LayoutParams(MarginLayoutParams source) {
			super(source);
		}

		public LayoutParams(LayoutParams source) {
			this(source.width, source.height);
		}

		public void setCenterVertical(boolean centerVertical) {
			this.centerVertical = centerVertical;
		}

		public void setCenterHorizontal(boolean centerHorizontal) {
			this.centerHorizontal = centerHorizontal;
		}
	}

	private static class RowMeasurement
	{
		private final int	maxWidth;
		private final int	widthMode;
		private int	width;
		private int	height;

		RowMeasurement(int maxWidth, int widthMode) {
			this.maxWidth = maxWidth;
			this.widthMode = widthMode;
		}

		int getWidth() {
			return width;
		}

		int getHeight() {
			return height;
		}

		boolean isWouldExceedMax(int childWidth) {
			return (widthMode != MeasureSpec.UNSPECIFIED) && (getNewWidth(childWidth) > maxWidth);
		}

		void addChildDimensions(int childWidth, int childHeight) {
			width = getNewWidth(childWidth);
			height = Math.max(height, childHeight);
		}

		private int getNewWidth(int childWidth) {
			return (width == 0) ? childWidth : (width + childWidth);
		}
	}
}