package com.aircandi.widgets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class FlowLayout extends ViewGroup
{
	private int[]	rowHeights;
	private int[]	rowWidths;

	public FlowLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int maxInternalWidth = MeasureSpec.getSize(widthMeasureSpec) - getHorizontalPadding();
		int maxInternalHeight = MeasureSpec.getSize(heightMeasureSpec) - getVerticalPadding();

		List<RowMeasurement> rows = new ArrayList<RowMeasurement>();

		RowMeasurement currentRow = null;
		for (View child : getLayoutChildren()) {
			LayoutParams lp = (LayoutParams) child.getLayoutParams();

			int childWidthSpec = createChildMeasureSpec(lp.width, maxInternalWidth, widthMode);
			int childHeightSpec = createChildMeasureSpec(lp.height, maxInternalHeight, heightMode);

			child.measure(childWidthSpec, childHeightSpec);

			int childWidth = child.getMeasuredWidth();
			int childHeight = child.getMeasuredHeight();

			if (currentRow == null
					|| currentRow.isWouldExceedMax(lp.leftMargin + childWidth + lp.rightMargin)) {

				currentRow = new RowMeasurement(maxInternalWidth, widthMode);
				rows.add(currentRow);
			}

			currentRow.addChildDimensions(lp.leftMargin + childWidth + lp.rightMargin
					, lp.topMargin + childHeight + lp.bottomMargin);
		}

		int longestRowWidth = 0;
		int totalRowHeight = 0;
		int rowCount = rows.size();
		this.rowHeights = new int[rowCount];
		this.rowWidths = new int[rowCount];

		for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
			RowMeasurement row = rows.get(rowIndex);
			int rowHeight = row.getHeight();
			int rowWidth = row.getWidth();
			this.rowHeights[rowIndex] = rowHeight;
			this.rowWidths[rowIndex] = rowWidth;
			totalRowHeight = totalRowHeight + rowHeight;
			longestRowWidth = Math.max(longestRowWidth, row.getWidth());
		}

		setMeasuredDimension((widthMode == MeasureSpec.EXACTLY)
				? MeasureSpec.getSize(widthMeasureSpec)
				: (longestRowWidth + getHorizontalPadding())
				, (heightMode == MeasureSpec.EXACTLY)
						? MeasureSpec.getSize(heightMeasureSpec)
						: (totalRowHeight + getVerticalPadding()));
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
		return (layoutParams instanceof LayoutParams);
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

		int widthOffset = getMeasuredWidth() - getPaddingRight();
		int childLeft = getPaddingLeft();
		int childTop = getPaddingTop();

		int rowIndex = 0;
		for (View child : getLayoutChildren()) {

			int childWidth = child.getMeasuredWidth();
			int childHeight = child.getMeasuredHeight();

			LayoutParams lp = (LayoutParams) child.getLayoutParams();
			if ((childLeft + lp.leftMargin + childWidth + lp.rightMargin) > widthOffset) {
				childLeft = getPaddingLeft();
				childTop = childTop + this.rowHeights[rowIndex];
				rowIndex = rowIndex + 1;
			}

			int _y;
			if (lp.centerVertical) {
				_y = childTop + ((this.rowHeights[rowIndex] - childHeight) / 2);
			}
			else {
				_y = childTop;
			}

			int _x;
			if (lp.centerHorizontal) {
				childLeft += lp.leftMargin;
				_x = (((rightPosition - leftPosition) - this.rowWidths[rowIndex]) / 2) + childLeft;
			}
			else {
				_x = childLeft += lp.leftMargin;
			}

			child.layout(_x, _y, _x + childWidth, _y + childHeight);
			childLeft += childWidth + lp.rightMargin;
		}
	}

	private Collection<View> getLayoutChildren() {
		int count = getChildCount();
		Collection<View> children = new ArrayList<View>(count);
		for (int index = 0; index < count; index++) {
			View child = getChildAt(index);
			if (child.getVisibility() != View.GONE)
				children.add(child);
		}
		return children;
	}

	private int getVerticalPadding() {
		return getPaddingTop() + getPaddingBottom();
	}

	private int getHorizontalPadding() {
		return getPaddingLeft() + getPaddingRight();
	}

	public static class LayoutParams extends MarginLayoutParams {
		boolean	centerVertical;
		boolean	centerHorizontal;

		public LayoutParams() {
			this(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		}

		public LayoutParams(int width, int height) {
			super(width, height);
			setCenterVertical(false);
		}

		public LayoutParams(Context context, AttributeSet attrs) {
			super(context, attrs);
			TypedArray attributes = context.obtainStyledAttributes(attrs, new int[] { android.R.attr.layout_centerVertical });
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
		private int	maxWidth;
		private int	widthMode;
		private int	width;
		private int	height;

		RowMeasurement(int maxWidth, int widthMode) {
			this.maxWidth = maxWidth;
			this.widthMode = widthMode;
		}

		int getWidth() {
			return this.width;
		}

		int getHeight() {
			return this.height;
		}

		boolean isWouldExceedMax(int childWidth) {
			return ((this.widthMode != MeasureSpec.UNSPECIFIED) && (getNewWidth(childWidth) > this.maxWidth));
		}

		void addChildDimensions(int childWidth, int childHeight) {
			this.width = getNewWidth(childWidth);
			this.height = Math.max(this.height, childHeight);
		}

		private int getNewWidth(int childWidth) {
			return ((this.width == 0) ? childWidth : (this.width + childWidth));
		}
	}
}