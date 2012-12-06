package com.aircandi.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aircandi.R;
import com.aircandi.components.FontManager;

public class SectionLayout extends LinearLayout {

	private Integer	mLayoutHeaderId;
	private Integer	mLayoutFooterId;
	private String	mHeaderTitle;
	private TextView mTextViewHeader;
	private TextView mButtonMore;

	public SectionLayout(Context context) {
		this(context, null);
	}

	public SectionLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public SectionLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SectionLayout, defStyle, 0);

		mLayoutHeaderId = ta.getResourceId(R.styleable.SectionLayout_layoutHeader, R.layout.temp_section_header);
		mLayoutFooterId = ta.getResourceId(R.styleable.SectionLayout_layoutFooter, 0);
		mHeaderTitle = ta.getString(R.styleable.SectionLayout_headerTitle);

		ta.recycle();

		initialize();
	}

	private void initialize() {
		LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (mLayoutHeaderId != 0) {
			View view = inflater.inflate(mLayoutHeaderId, null);
			
			mTextViewHeader = (TextView) view.findViewById(R.id.title);
			mButtonMore = (TextView) view.findViewById(R.id.button_more);
			FontManager.getInstance().setTypefaceLight(mTextViewHeader);
			FontManager.getInstance().setTypefaceLight(mButtonMore);
			
			if (mTextViewHeader != null) {
				if (mHeaderTitle == null) {
					mTextViewHeader.setVisibility(View.GONE);
				}
				else {
					mTextViewHeader.setText(mHeaderTitle);
					mTextViewHeader.setVisibility(View.VISIBLE);
				}
			}
			this.addView(view, 0);
		}

		if (mLayoutFooterId != 0) {
			View view = inflater.inflate(mLayoutFooterId, null);
			this.addView(view, this.getChildCount() + 1);
		}
	}

	public TextView getTextViewHeader() {
		return mTextViewHeader;
	}

	public void setTextViewHeader(TextView textViewHeader) {
		mTextViewHeader = textViewHeader;
	}

	public TextView getButtonMore() {
		return mButtonMore;
	}

	public void setButtonMore(TextView buttonMore) {
		mButtonMore = buttonMore;
	}
}
