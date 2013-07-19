package com.aircandi.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aircandi.beta.R;

@SuppressWarnings("ucd")
public class SectionLayout extends LinearLayout {

	private Integer		mLayoutHeaderId;
	private Integer		mLayoutFooterId;
	private String		mHeaderTitle;
	private TextView	mTextViewHeader;
	private View		mHeaderRule;
	private ViewGroup	mHeader;

	public SectionLayout(Context context) {
		this(context, null);
	}

	public SectionLayout(Context context, AttributeSet attrs) {
		super(context, attrs);

		final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SectionLayout);

		mLayoutHeaderId = ta.getResourceId(R.styleable.SectionLayout_layoutHeader, 0);
		mLayoutFooterId = ta.getResourceId(R.styleable.SectionLayout_layoutFooter, 0);
		mHeaderTitle = ta.getString(R.styleable.SectionLayout_headerTitle);

		ta.recycle();

		initialize();
	}

	private void initialize() {
		final LayoutInflater inflater = LayoutInflater.from(this.getContext());
		if (mLayoutHeaderId != 0) {
			final View view = inflater.inflate(mLayoutHeaderId, null);

			mTextViewHeader = (TextView) view.findViewById(R.id.name);
			mHeaderRule = view.findViewById(R.id.rule);
			mHeader = (ViewGroup) view.findViewById(R.id.header);

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

	public void setFooter(View view) {
		int childCount = this.getChildCount();
		for (int i = 0; i < childCount; i++) {
			if (this.getChildAt(i).getTag() != null && this.getChildAt(i).getTag().equals("footer")) {
				this.removeViewAt(i);
				break;
			}
		}
		view.setTag("footer");
		this.addView(view);
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

	public View getHeaderRule() {
		return mHeaderRule;
	}

	public void setHeaderRule(View headerRule) {
		mHeaderRule = headerRule;
	}

	public ViewGroup getHeader() {
		return mHeader;
	}

	public void setHeader(ViewGroup header) {
		mHeader = header;
	}
}
