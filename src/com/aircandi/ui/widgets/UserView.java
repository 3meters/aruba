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

import com.aircandi.beta.R;
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
	private ImageView		mImageWatched;
	private ImageView		mImageLiked;
	private TextView		mTextName;
	private TextView		mTextLocation;
	private TextView		mTextLabel;
	private TextView		mTextLikeCount;
	private TextView		mTextWatchCount;
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

		final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.AuthorLayout, defStyle, 0);
		final int layoutId = ta.getResourceId(R.styleable.AuthorLayout_layout, R.layout.widget_user_view);
		final LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
			mImageWatched = (ImageView) mBoundView.findViewById(R.id.image_watched);
			mImageLiked = (ImageView) mBoundView.findViewById(R.id.image_liked);
			mTextLikeCount = (TextView) mBoundView.findViewById(R.id.like_count);
			mTextWatchCount = (TextView) mBoundView.findViewById(R.id.watch_count);
			FontManager.getInstance().setTypefaceDefault(mTextName);
			FontManager.getInstance().setTypefaceDefault(mTextLocation);
			FontManager.getInstance().setTypefaceDefault(mTextTimeSince);
			FontManager.getInstance().setTypefaceDefault(mTextLabel);
			FontManager.getInstance().setTypefaceDefault(mTextLikeCount);
			FontManager.getInstance().setTypefaceDefault(mTextWatchCount);
		}

		this.removeAllViews();
		this.addView(mBoundView);
	}

	public void bindToUser(User user, Long date, Boolean locked) {
		mAuthor = user;
		this.setTag(user);
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
					authorName = (mAuthor.firstName != null) ? mAuthor.firstName : "";
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
					final BitmapRequestBuilder builder = new BitmapRequestBuilder(mImageUser);
					builder.setFromUri(mAuthor.getUserPhotoUri());
					final BitmapRequest imageRequest = builder.create();
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

			if (mBoundView.findViewById(R.id.stats_group) != null) {
				if (mImageWatched != null) {
					mTextWatchCount.setText(String.valueOf(user.watchCount));
				}
				if (mImageLiked != null) {
					mTextLikeCount.setText(String.valueOf(user.likeCount));
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
					final BitmapRequestBuilder builder = new BitmapRequestBuilder(mImageUser);
					builder.setFromUri(mUser.getUserPhotoUri());
					final BitmapRequest imageRequest = builder.create();
					mImageUser.setBitmapRequest(imageRequest);
				}
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Pager methods and callbacks
	// --------------------------------------------------------------------------------------------

	public void setAuthor(User author) {
		mAuthor = author;
	}

	public User getAuthor() {
		return mAuthor;
	}

	public void setActivity(Activity activity) {
		mActivity = activity;
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
