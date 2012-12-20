package com.aircandi.ui.widgets;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aircandi.components.FontManager;
import com.aircandi.components.ImageRequest;
import com.aircandi.components.ImageRequestBuilder;
import com.aircandi.service.objects.User;
import com.aircandi.utilities.DateUtils;
import com.aircandi.R;

public class UserView extends RelativeLayout {

	public static final int	HORIZONTAL	= 0;
	public static final int	VERTICAL	= 1;
	private ViewGroup		mBoundView;
	private WebImageView	mImageUser;
	private ImageView		mImageLocked;
	private TextView		mTextName;
	private TextView		mTextTimeSince;
	private User			mAuthor;
	private User			mUser;
	private Activity		mActivity;

	public UserView(Context context) {
		this(context, null);
	}

	public UserView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public UserView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.AuthorLayout, defStyle, 0);
		int layoutId = ta.getResourceId(R.styleable.AuthorLayout_layout, R.layout.widget_user_view);
		LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mBoundView = (ViewGroup) inflater.inflate(layoutId, null);

		ta.recycle();
		bindToView();
	}

	private void bindToView() {
		if (!isInEditMode()) {
			mImageUser = (WebImageView) mBoundView.findViewById(R.id.image_user_picture);
			mTextName = (TextView) mBoundView.findViewById(R.id.candi_user_fullname);
			mTextTimeSince = (TextView) mBoundView.findViewById(R.id.candi_user_timesince);
			mImageLocked = (ImageView) mBoundView.findViewById(R.id.image_locked);
			FontManager.getInstance().setTypefaceDefault(mTextName);
			FontManager.getInstance().setTypefaceDefault(mTextTimeSince);
		}

		this.removeAllViews();
		this.addView(mBoundView);
	}

	public void bindToAuthor(User author, Long date, boolean locked) {
		mAuthor = author;
		if (mAuthor != null) {
			if (mTextName != null) {
				String authorName = mAuthor.name;
				if (authorName == null) {
					authorName = mAuthor.firstName != null ? mAuthor.firstName : "";
					if (mAuthor.lastName != null) {
						authorName += " " + mAuthor.lastName;
					}
				}
				mTextName.setText(authorName);
			}
			if (mTextTimeSince != null) {
				if (date != null) {
					mTextTimeSince.setText(DateUtils.timeSince(date, DateUtils.nowDate().getTime()));
				}
				else {
					mTextTimeSince.setVisibility(View.GONE);
				}
			}
			if (mImageUser != null) {
				if (mAuthor.getImageUri() != null && mAuthor.getImageUri().length() != 0) {
					ImageRequestBuilder builder = new ImageRequestBuilder(mImageUser);
					builder.setFromUris(mAuthor.getImageUri(), null);
					ImageRequest imageRequest = builder.create();
					mImageUser.setImageRequest(imageRequest);
				}
			}
			if (mImageLocked != null) {
				if (locked) {
					mImageLocked.setVisibility(View.VISIBLE);
				}
				else {
					mImageLocked.setVisibility(View.GONE);
				}
			}
		}
	}

	public void bindToUser(User user, Long date) {
		mUser = user;
		if (mUser != null) {
			if (mTextName != null) {
				mTextName.setText(mUser.name);
			}
			if (mTextTimeSince != null) {
				if (date != null) {
					mTextTimeSince.setText(DateUtils.timeSince(date, DateUtils.nowDate().getTime()));
				}
				else {
					mTextTimeSince.setVisibility(View.GONE);
				}
			}
			if (mImageUser != null) {
				if (mUser.getImageUri() != null && mUser.getImageUri().length() != 0) {
					ImageRequestBuilder builder = new ImageRequestBuilder(mImageUser);
					builder.setFromUris(mUser.getImageUri(), null);
					ImageRequest imageRequest = builder.create();
					mImageUser.setImageRequest(imageRequest);
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

	public void setAuthor(User author) {
		this.mAuthor = author;
	}

	public User getAuthor() {
		return mAuthor;
	}

	public void setActivity(Activity activity) {
		this.mActivity = activity;
	}

	public Activity getActivity() {
		return mActivity;
	}
}
