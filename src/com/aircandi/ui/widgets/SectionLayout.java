package com.aircandi.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aircandi.R;
import com.aircandi.components.FontManager;

@SuppressWarnings("ucd")
public class SectionLayout extends LinearLayout {

	private Integer		mLayoutHeaderId;
	private Integer		mLayoutFooterId;
	private String		mHeaderTitle;
	private TextView	mTextViewHeader;

	public SectionLayout(Context context) {
		this(context, null);
	}

	public SectionLayout(Context context, AttributeSet attrs) {
		super(context, attrs);

		final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SectionLayout);

		mLayoutHeaderId = ta.getResourceId(R.styleable.SectionLayout_layoutHeader, R.layout.temp_section_header);
		mLayoutFooterId = ta.getResourceId(R.styleable.SectionLayout_layoutFooter, 0);
		mHeaderTitle = ta.getString(R.styleable.SectionLayout_headerTitle);

		ta.recycle();

		initialize();
	}

	private void initialize() {
		final LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (mLayoutHeaderId != 0) {
			final View view = inflater.inflate(mLayoutHeaderId, null);

			mTextViewHeader = (TextView) view.findViewById(R.id.title);
			FontManager.getInstance().setTypefaceDefault(mTextViewHeader);

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
			final View view = inflater.inflate(mLayoutFooterId, null);
			this.addView(view, this.getChildCount() + 1);
		}
	}

	public TextView getTextViewHeader() {
		return mTextViewHeader;
	}

	public void setTextViewHeader(TextView textViewHeader) {
		mTextViewHeader = textViewHeader;
	}

	public String getHeaderTitle() {
		return mHeaderTitle;
	}

	public void setHeaderTitle(String headerTitle) {
		mHeaderTitle = headerTitle;
		if (mTextViewHeader != null) {
			if (mHeaderTitle == null) {
				mTextViewHeader.setVisibility(View.GONE);
			}
			else {
				mTextViewHeader.setText(mHeaderTitle);
				mTextViewHeader.setVisibility(View.VISIBLE);
			}
		}
	}
}
