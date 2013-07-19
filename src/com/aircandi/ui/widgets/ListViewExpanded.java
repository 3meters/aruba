package com.aircandi.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListAdapter;

import com.aircandi.beta.R;

@SuppressWarnings("ucd")
public class ListViewExpanded extends LinearLayout {

	/*
	 * Layout attribute is used for design purposes but at runtime, the code
	 * setting the adapter and providing views chooses the item layout.
	 */

	private Integer		mLayoutItemId;
	private Integer		mItemMaxCount;
	private Boolean		mShowDivider;
	private ListAdapter	mAdapter;

	public ListViewExpanded(Context context) {
		this(context, null);
	}

	public ListViewExpanded(Context context, AttributeSet attrs) {
		super(context, attrs);

		final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ListViewExpanded);

		mLayoutItemId = ta.getResourceId(R.styleable.ListViewExpanded_layoutItems, 0);
		mItemMaxCount = ta.getInteger(R.styleable.ListViewExpanded_itemMaxCount, 2);
		mShowDivider = ta.getBoolean(R.styleable.ListViewExpanded_showDivider, true);

		ta.recycle();

		if (this.isInEditMode()) {
			initialize();
		}
	}

	private void initialize() {
		final LayoutInflater inflater = LayoutInflater.from(this.getContext());
		if (mLayoutItemId != 0) {
			int itemCount = 0;
			while (itemCount < mItemMaxCount) {

				if (mShowDivider && itemCount > 0) {
					inflater.inflate(R.layout.temp_rule, this);
				}

				View view = inflater.inflate(mLayoutItemId, null);
				this.addView(view);
				itemCount++;
			}
		}
	}

	public void setAdapter(ListAdapter adapter) {
		mAdapter = adapter;
		final LayoutInflater inflater = LayoutInflater.from(this.getContext());

		final int itemCount = mAdapter.getCount();
		for (int i = 0; i < itemCount; i++) {
			if (i >= mItemMaxCount) {
				break;
			}
			if (mShowDivider && i > 0) {
				inflater.inflate(R.layout.temp_rule, this);
			}
			View view = mAdapter.getView(i, null, this);
			this.addView(view);
		}
	}

	public Integer getItemMaxCount() {
		return mItemMaxCount;
	}

	public void setItemMaxCount(Integer itemMaxCount) {
		mItemMaxCount = itemMaxCount;
	}
}
