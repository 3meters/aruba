package com.proxibase.aircandi.widgets;

import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.proxibase.aircandi.R;
import com.proxibase.aircandi.utils.DateUtils;
import com.proxibase.aircandi.utils.ImageManager;
import com.proxibase.aircandi.utils.ImageLoader.ImageProfile;
import com.proxibase.sdk.android.proxi.consumer.Author;

public class AuthorBlock extends RelativeLayout {

	public static final int	HORIZONTAL	= 0;
	public static final int	VERTICAL	= 1;
	private int				mOrientation;
	private View			mBoundView;
	private WebImageView	mImageUser;
	private TextView		mTextFullname;
	private TextView		mTextTimeSince;
	private Author			mAuthor;
	private Activity		mActivity;

	public AuthorBlock(Context context) {
		this(context, null);
	}

	public AuthorBlock(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public AuthorBlock(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.UserLayout, defStyle, 0);

		int index = ta.getInt(R.styleable.UserLayout_orientation, -1);
		if (index >= 0) {
			setOrientation(index);
		}
		ta.recycle();
		bindToView();
	}

	private void bindToView() {
		LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (mOrientation == HORIZONTAL) {
			mBoundView = (RelativeLayout) inflater.inflate(R.layout.temp_user_info, null);
		}
		else if (mOrientation == VERTICAL) {
			mBoundView = (RelativeLayout) inflater.inflate(R.layout.temp_user_info_stacked, null);
		}

		mImageUser = (WebImageView) mBoundView.findViewById(R.id.image_user_picture);
		mTextFullname = (TextView) mBoundView.findViewById(R.id.text_user_fullname);
		mTextTimeSince = (TextView) mBoundView.findViewById(R.id.text_user_timesince);

		this.removeAllViews();
		this.addView(mBoundView);
	}

	public void bindToAuthor(Author author, Date date) {
		mAuthor = author;
		if (mAuthor != null) {
			mTextFullname.setText(mAuthor.fullname);
			if (date != null) {
				mTextTimeSince.setText(DateUtils.intervalSince(date, DateUtils.nowDate()));
			}
			if (mAuthor.imageUri != null && mAuthor.imageUri.length() != 0) {
				mImageUser.setImageRequest(ImageManager.getInstance().getImageLoader().getImageRequestByProfile(ImageProfile.SquareUser,
						mAuthor.imageUri, null), null);
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Pager methods and callbacks
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
	}

	public void setOrientation(int orientation) {
		this.mOrientation = orientation;
	}

	public int getOrientation() {
		return mOrientation;
	}

	public void setAuthor(Author author) {
		this.mAuthor = author;
	}

	public Author getAuthor() {
		return mAuthor;
	}

	public void setActivity(Activity activity) {
		this.mActivity = activity;
	}

	public Activity getActivity() {
		return mActivity;
	}
}
