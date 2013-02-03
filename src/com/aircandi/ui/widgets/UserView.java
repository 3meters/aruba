package com.aircandi.ui.widgets;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.text.Html;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aircandi.R;
import com.aircandi.components.FontManager;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequestBuilder;
import com.aircandi.service.objects.User;
import com.aircandi.utilities.DateUtils;

@SuppressWarnings("ucd")
public class UserView extends RelativeLayout {

	private ViewGroup		mBoundView;
	private WebImageView	mImageUser;
	private ImageView		mImageLocked;
	private TextView		mTextName;
	private TextView		mTextLocation;
	private TextView		mTextLabel;
	private String			mLabel;
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
			mImageUser = (WebImageView) mBoundView.findViewById(R.id.candi_user_picture);
			mTextName = (TextView) mBoundView.findViewById(R.id.candi_user_fullname);
			mTextLocation = (TextView) mBoundView.findViewById(R.id.candi_user_location);
			mTextLabel = (TextView) mBoundView.findViewById(R.id.candi_user_label);
			mTextTimeSince = (TextView) mBoundView.findViewById(R.id.candi_user_timesince);
			mImageLocked = (ImageView) mBoundView.findViewById(R.id.image_locked);
			FontManager.getInstance().setTypefaceDefault(mTextName);
			FontManager.getInstance().setTypefaceDefault(mTextLocation);
			FontManager.getInstance().setTypefaceDefault(mTextTimeSince);
			FontManager.getInstance().setTypefaceDefault(mTextLabel);
		}

		this.removeAllViews();
		this.addView(mBoundView);
	}

	public void bindToAuthor(User author, Long date, Boolean locked) {
		mAuthor = author;
		this.setTag(author);
		if (mAuthor != null) {
			if (mTextLabel != null) {
				if (mLabel != null) {
					mTextLabel.setText(mLabel);
				}
				else {
					mTextLabel.setVisibility(View.GONE);
				}
			}

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

			if (mTextLocation != null && mAuthor.location != null && !mAuthor.location.equals("")) {
				mTextLocation.setText(Html.fromHtml(mAuthor.location));
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
				if (mAuthor.getUserPhotoUri() != null && mAuthor.getUserPhotoUri().length() != 0) {
					BitmapRequestBuilder builder = new BitmapRequestBuilder(mImageUser);
					builder.setFromUri(mAuthor.getUserPhotoUri());
					BitmapRequest imageRequest = builder.create();
					mImageUser.setBitmapRequest(imageRequest);
				}
			}

			if (mImageLocked != null) {
				if (locked != null && locked) {
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
			if (mTextLabel != null) {
				if (mLabel != null) {
					mTextLabel.setText(mLabel);
				}
				else {
					mTextLabel.setVisibility(View.GONE);
				}
			}
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
				if (mUser.getUserPhotoUri() != null && mUser.getUserPhotoUri().length() != 0) {
					BitmapRequestBuilder builder = new BitmapRequestBuilder(mImageUser);
					builder.setFromUri(mUser.getUserPhotoUri());
					BitmapRequest imageRequest = builder.create();
					mImageUser.setBitmapRequest(imageRequest);
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

	public TextView getTextLabel() {
		return mTextLabel;
	}

	public void setTextLabel(TextView textLabel) {
		mTextLabel = textLabel;
	}

	public String getLabel() {
		return mLabel;
	}

	public void setLabel(String label) {
		mLabel = label;
	}
}
