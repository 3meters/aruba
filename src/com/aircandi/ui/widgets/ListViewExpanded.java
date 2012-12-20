package com.aircandi.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListAdapter;

import com.aircandi.R;

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
		this(context, attrs, 0);
	}

	public ListViewExpanded(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ListViewExpanded, defStyle, 0);

		mLayoutItemId = ta.getResourceId(R.styleable.ListViewExpanded_layoutItems, R.layout.temp_listitem_tip);
		mItemMaxCount = ta.getInteger(R.styleable.ListViewExpanded_itemMaxCount, 2);
		mShowDivider = ta.getBoolean(R.styleable.ListViewExpanded_showDivider, true);

		ta.recycle();

		if (this.isInEditMode()) {
			initialize();
		}
	}

	private void initialize() {
		LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
		LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		int itemCount = mAdapter.getCount();
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