package com.proxibase.aircandi.widgets;

import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.proxibase.aircandi.R;
import com.proxibase.aircandi.components.DateUtils;
import com.proxibase.aircandi.components.ImageManager.ImageRequest;
import com.proxibase.aircandi.components.ImageManager.ImageRequest.ImageShape;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.sdk.android.proxi.consumer.Author;
import com.proxibase.sdk.android.proxi.consumer.User;

public class AuthorBlock extends RelativeLayout {

	public static final int	HORIZONTAL	= 0;
	public static final int	VERTICAL	= 1;
	private ViewGroup			mBoundView;
	private WebImageView	mImageUser;
	private TextView		mTextFullname;
	private TextView		mTextTimeSince;
	private Author			mAuthor;
	private User			mUser;
	private Activity		mActivity;

	public AuthorBlock(Context context) {
		this(context, null);
	}

	public AuthorBlock(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public AuthorBlock(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.AuthorLayout, defStyle, 0);

		int layoutId = ta.getResourceId(R.styleable.AuthorLayout_layout, R.layout.temp_user_info);
		LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mBoundView = (ViewGroup) inflater.inflate(layoutId, null);

		ta.recycle();
		bindToView();
	}

	private void bindToView() {

		mImageUser = (WebImageView) mBoundView.findViewById(R.id.image_user_picture);
		mTextFullname = (TextView) mBoundView.findViewById(R.id.text_user_fullname);
		mTextTimeSince = (TextView) mBoundView.findViewById(R.id.text_user_timesince);

		this.removeAllViews();
		this.addView(mBoundView);
	}

	public void bindToAuthor(Author author, Date date) {
		mAuthor = author;
		if (mAuthor != null) {
			if (mTextFullname != null) {
				mTextFullname.setText(mAuthor.fullname);
			}
			if (mTextTimeSince != null) {
				if (date != null) {
					mTextTimeSince.setText(DateUtils.timeSince(date, DateUtils.nowDate()));
				}
				else {
					mTextTimeSince.setVisibility(View.GONE);
				}
			}
			if (mImageUser != null) {
				if (mAuthor.imageUri != null && mAuthor.imageUri.length() != 0) {
					ImageRequest imageRequest = new ImageRequest(mAuthor.imageUri, mAuthor.linkUri, ImageShape.Square, false, false,
							CandiConstants.IMAGE_WIDTH_USER_SMALL, false, true, true, 1, this, null);
					mImageUser.setImageRequest(imageRequest, null);
				}
			}
		}
	}

	public void bindToUser(User user, Date date) {
		mUser = user;
		if (mUser != null) {
			if (mTextFullname != null) {
				mTextFullname.setText(mUser.fullname);
			}
			if (mTextTimeSince != null) {
				if (date != null) {
					mTextTimeSince.setText(DateUtils.timeSince(date, DateUtils.nowDate()));
				}
				else {
					mTextTimeSince.setVisibility(View.GONE);
				}
			}
			if (mImageUser != null) {
				if (mUser.imageUri != null && mUser.imageUri.length() != 0) {
					ImageRequest imageRequest = new ImageRequest(mUser.imageUri, mUser.linkUri, ImageShape.Square, false, false,
							CandiConstants.IMAGE_WIDTH_USER_SMALL, false, true, true, 1, this, null);
					mImageUser.setImageRequest(imageRequest, null);
				}
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
