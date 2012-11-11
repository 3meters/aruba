/*
 * HorizontalListView.java v1.5
 * 
 * 
 * The MIT License
 * Copyright (c) 2011 Paul Soucy (paul@dev-smart.com)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
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

package com.aircandi.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import com.aircandi.R;

public class HorizontalScrollLayout extends HorizontalScrollView {

	private LayoutInflater	mInflater;
	private Integer			mLayoutItemId;
	private Context			mContext;
	private LinearLayout	mLayout;

	public HorizontalScrollLayout(Context context) {
		this(context, null);
	}

	public HorizontalScrollLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public HorizontalScrollLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.HorizontalScrollLayout, defStyle, 0);
		mLayoutItemId = ta.getResourceId(R.styleable.HorizontalScrollLayout_layout, R.layout.temp_place_photo_item);
		mInflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		initialize();
	}

	private synchronized void initialize() {

		mLayout = new LinearLayout(mContext);
		mLayout.setOrientation(LinearLayout.HORIZONTAL);
		@SuppressWarnings("deprecation")
		FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		this.addView(mLayout, layoutParams);

		/* Show something in design mode */
		if (isInEditMode()) {
			int itemCount = 0;
			while (itemCount < 3) {
				mInflater.inflate(mLayoutItemId, mLayout, true);
				this.invalidate();
				mLayout.invalidate();
				itemCount++;
			}
		}
	}

	@Override
	public void addView(View child) {
		mLayout.addView(child);
		mLayout.invalidate();
	}
}
