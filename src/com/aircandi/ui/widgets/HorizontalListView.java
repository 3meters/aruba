/*
 * HorizontalListView.java v1.5
 * 
 * 
 * The MIT License
 * Copyright (c) 2011 Paul Soucy (paul@dev-smart.com)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * IN the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included IN
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.aircandi.ui.widgets;

import java.util.LinkedList;
import java.util.Queue;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.Scroller;

import com.aircandi.R;

@SuppressWarnings("ucd")
public class HorizontalListView extends AdapterView<ListAdapter> {

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		return true;
		//		// TODO Auto-generated method stub
		//		return super.onInterceptTouchEvent(ev);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		if (event.getAction() == MotionEvent.ACTION_MOVE && getParent() != null) {
			getParent().requestDisallowInterceptTouchEvent(true);
		}

		return super.onTouchEvent(event);
	}

	private ListAdapter				mAdapter;
	private int						mLeftViewIndex		= -1;
	private int						mRightViewIndex		= 0;
	private int						mCurrentX;
	private int						mNextX;
	private int						mMaxX				= Integer.MAX_VALUE;
	private int						mDisplayOffset		= 0;
	private Scroller				mScroller;
	private GestureDetector			mGesture;
	private final Queue<View>				mRemovedViewQueue	= new LinkedList<View>();
	private OnItemSelectedListener	mOnItemSelected;
	private OnItemClickListener		mOnItemClicked;
	private boolean					mDataChanged		= false;
	private Runnable				mRequestLayoutRunnable;

	public HorizontalListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize();
	}

	private synchronized void initialize() {
		mLeftViewIndex = -1;
		mRightViewIndex = 0;
		mDisplayOffset = 0;
		mCurrentX = 0;
		mNextX = 0;
		mMaxX = Integer.MAX_VALUE;

		mRequestLayoutRunnable = new Runnable() {

			@Override
			public void run() {
				requestLayout();
			}
		};

		mScroller = new Scroller(getContext(), new DecelerateInterpolator());
		if (!this.isInEditMode()) {
			mGesture = new GestureDetector(getContext(), mOnGesture);
		}
		else {
			if (mAdapter == null) {
				setAdapter(new BaseAdapter() {

					@Override
					public int getCount() {
						return 4;
					}

					@Override
					public Object getItem(int position) {
						return null;
					}

					@Override
					public long getItemId(int position) {
						return 0;
					}

					@Override
					public View getView(int position, View convertView, ViewGroup parent) {
						View view = convertView;
						if (view == null) {
							view = LayoutInflater.from(parent.getContext()).inflate(R.layout.temp_place_photo_item, null);
						}
						return view;
					}
				});
			}
		}
	}

	@Override
	public void setOnItemSelectedListener(AdapterView.OnItemSelectedListener listener) {
		mOnItemSelected = listener;
	}

	@Override
	public void setOnItemClickListener(AdapterView.OnItemClickListener listener) {
		mOnItemClicked = listener;
	}

	private final DataSetObserver	mDataObserver	= new DataSetObserver() {

												@Override
												public void onChanged() {
													synchronized (HorizontalListView.this) {
														mDataChanged = true;
													}
													invalidate();
													requestLayout();
												}

												@Override
												public void onInvalidated() {
													reset();
													invalidate();
													requestLayout();
												}

											};

	@Override
	public View getSelectedView() {
		// TODO: implement
		return null;
	}

	@Override
	public void setAdapter(ListAdapter adapter) {
		if (mAdapter != null) {
			mAdapter.unregisterDataSetObserver(mDataObserver);
		}
		mAdapter = adapter;
		mAdapter.registerDataSetObserver(mDataObserver);
		reset();
	}

	private synchronized void reset() {
		initialize();
		removeAllViewsInLayout();
		requestLayout();
	}

	@Override
	public void setSelection(int position) {
		// TODO: implement
	}

	private void addAndMeasureChild(final View child, int viewPos) {
		LayoutParams params = child.getLayoutParams();
		if (params == null) {
			params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		}

		addViewInLayout(child, viewPos, params, true);
		child.measure(MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.AT_MOST));
	}

	@Override
	protected synchronized void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);

		if (mAdapter == null) {
			return;
		}

		if (mDataChanged) {
			final int oldCurrentX = mCurrentX;
			initialize();
			removeAllViewsInLayout();
			mNextX = oldCurrentX;
			mDataChanged = false;
		}

		if (mScroller.computeScrollOffset()) {
			final int scrollx = mScroller.getCurrX();
			mNextX = scrollx;
		}

		if (mNextX < 0) {
			mNextX = 0;
			mScroller.forceFinished(true);
		}
		if (mNextX > mMaxX) {
			mNextX = mMaxX;
			mScroller.forceFinished(true);
		}

		final int dx = mCurrentX - mNextX;

		removeNonVisibleItems(dx);
		fillList(dx);
		positionItems(dx);

		mCurrentX = mNextX;

		if (!mScroller.isFinished()) {
			mRequestLayoutRunnable.run();
		}
	}

	private void fillList(final int dx) {
		int edge = 0;
		View child = getChildAt(getChildCount() - 1);
		if (child != null) {
			edge = child.getRight();
		}
		fillListRight(edge, dx);

		edge = 0;
		child = getChildAt(0);
		if (child != null) {
			edge = child.getLeft();
		}
		fillListLeft(edge, dx);

	}

	private void fillListRight(int rightEdge, final int dx) {
		while (rightEdge + dx < getWidth() && mRightViewIndex < mAdapter.getCount()) {

			View child = mAdapter.getView(mRightViewIndex, mRemovedViewQueue.poll(), this);
			addAndMeasureChild(child, -1);
			rightEdge += child.getMeasuredWidth();

			if (mRightViewIndex == mAdapter.getCount() - 1) {
				mMaxX = mCurrentX + rightEdge - getWidth();
				if (mMaxX < 0){
					mMaxX = 0;
				}
			}
			mRightViewIndex++;
		}

	}

	private void fillListLeft(int leftEdge, final int dx) {
		while (leftEdge + dx > 0 && mLeftViewIndex >= 0) {
			View child = mAdapter.getView(mLeftViewIndex, mRemovedViewQueue.poll(), this);
			addAndMeasureChild(child, 0);
			leftEdge -= child.getMeasuredWidth();
			mLeftViewIndex--;
			mDisplayOffset -= child.getMeasuredWidth();
		}
	}

	private void removeNonVisibleItems(final int dx) {
		View child = getChildAt(0);
		while (child != null && child.getRight() + dx <= 0) {
			mDisplayOffset += child.getMeasuredWidth();
			mRemovedViewQueue.offer(child);
			removeViewInLayout(child);
			mLeftViewIndex++;
			child = getChildAt(0);

		}

		child = getChildAt(getChildCount() - 1);
		while (child != null && child.getLeft() + dx >= getWidth()) {
			mRemovedViewQueue.offer(child);
			removeViewInLayout(child);
			mRightViewIndex--;
			child = getChildAt(getChildCount() - 1);
		}
	}

	private void positionItems(final int dx) {
		if (getChildCount() > 0) {
			mDisplayOffset += dx;
			int left = mDisplayOffset;
			for (int i = 0; i < getChildCount(); i++) {
				View child = getChildAt(i);
				int childWidth = child.getMeasuredWidth();
				child.layout(left, 0, left + childWidth, child.getMeasuredHeight());
				left += childWidth;
			}
		}
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		final boolean handled = mGesture.onTouchEvent(ev);
		return handled;
	}

	private View getGestureTarget(MotionEvent e) {
		final Rect viewRect = new Rect();
		for (int i = 0; i < getChildCount(); i++) {
			View child = getChildAt(i);
			int left = child.getLeft();
			int right = child.getRight();
			int top = child.getTop();
			int bottom = child.getBottom();
			viewRect.set(left, top, right, bottom);
			if (viewRect.contains((int) e.getX(), (int) e.getY())) {
				return child;
			}
		}
		return null;
	}

	private Integer getViewPosition(View view) {
		for (int i = 0; i < getChildCount(); i++) {
			if (view.equals(getChildAt(i))) {
				return mLeftViewIndex + 1 + i;
			}
		}
		return null;
	}

	private final OnGestureListener	mOnGesture	= new GestureDetector.SimpleOnGestureListener() {

												@Override
												public void onShowPress(MotionEvent e) {
													final View view = getGestureTarget(e);
													if (view != null) {
														view.setBackgroundResource(R.drawable.bg_button_pressed);
													}
												}

												@Override
												public boolean onSingleTapUp(MotionEvent e) {
													final View view = getGestureTarget(e);
													if (view != null) {
														view.setBackgroundResource(android.R.color.transparent);
													}
													return true;
												}

												@Override
												public boolean onDown(MotionEvent e) {
													mScroller.forceFinished(true);
													return true;
												}

												@Override
												public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
													synchronized (HorizontalListView.this) {
														mScroller.fling(mNextX, 0, (int) -velocityX, 0, 0, mMaxX, 0, 0);
													}
													requestLayout();
													return true;
												}

												@Override
												public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
													synchronized (HorizontalListView.this) {
														mNextX += (int) distanceX;
													}
													requestLayout();
													return true;
												}

												@Override
												public boolean onSingleTapConfirmed(MotionEvent e) {
													final View view = getGestureTarget(e);
													if (view != null) {
														final Integer position = getViewPosition(view);
														if (position != null) {
															if (mOnItemClicked != null) {
																mOnItemClicked.onItemClick(HorizontalListView.this, view, position, mAdapter
																		.getItemId(position));
															}
															if (mOnItemSelected != null) {
																mOnItemSelected.onItemSelected(HorizontalListView.this, view, position, mAdapter
																		.getItemId(position));
															}
														}
													}
													return true;
												}
											};

	@Override
	public ListAdapter getAdapter() {
		return null;
	}
}
