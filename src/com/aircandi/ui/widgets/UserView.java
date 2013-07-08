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

import com.aircandi.Constants;
import com.aircandi.beta.R;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequestBuilder;
import com.aircandi.service.objects.Count;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link.Direction;
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
	private Entity			mUser;
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
			mImageUser = (WebImageView) mBoundView.findViewById(R.id.photo);
			mTextName = (TextView) mBoundView.findViewById(R.id.fullname);
			mTextLocation = (TextView) mBoundView.findViewById(R.id.area);
			mTextLabel = (TextView) mBoundView.findViewById(R.id.label);
			mTextTimeSince = (TextView) mBoundView.findViewById(R.id.timesince);
			mImageLocked = (ImageView) mBoundView.findViewById(R.id.image_locked);
			mImageWatched = (ImageView) mBoundView.findViewById(R.id.image_watched);
			mImageLiked = (ImageView) mBoundView.findViewById(R.id.image_liked);
			mTextLikeCount = (TextView) mBoundView.findViewById(R.id.like_count);
			mTextWatchCount = (TextView) mBoundView.findViewById(R.id.watch_count);
		}

		this.removeAllViews();
		this.addView(mBoundView);
	}

	public void bindToUser(Entity user, Long date) {
		bindToUser(user, date, null);
		mUser = user;
	}

	public void bindToUser(Entity userEntity, Long date, Boolean locked) {
		mUser = userEntity;
		this.setTag(userEntity);
		User user = (User) mUser;
		
		if (user != null) {
			if (mTextLabel != null) {
				if (mLabel != null) {
					mTextLabel.setText(mLabel);
				}
				else {
					mTextLabel.setVisibility(View.GONE);
				}
			}

			if (mTextName != null) {
				if (user.name != null) {
					mTextName.setText(user.name);
				}
			}

			if (mTextLocation != null && user.location != null && !user.location.equals("")) {
				mTextLocation.setText(Html.fromHtml(user.area));
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
				if (user.getPhotoUri() != null && user.getPhotoUri().length() != 0) {
					final BitmapRequestBuilder builder = new BitmapRequestBuilder(mImageUser);
					builder.setFromUri(user.getPhotoUri());
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
					Count count = user.getCount(Constants.TYPE_LINK_WATCH, Direction.in);
					mTextWatchCount.setText(String.valueOf(count != null ? count.count.intValue() : 0));
				}
				if (mImageLiked != null) {
					Count count = user.getCount(Constants.TYPE_LINK_LIKE, Direction.in);
					mTextLikeCount.setText(String.valueOf(count != null ? count.count.intValue() : 0));
				}
			}

		}
	}

	// --------------------------------------------------------------------------------------------
	// Pager methods and callbacks
	// --------------------------------------------------------------------------------------------

	public void setUser(Entity user) {
		mUser = user;
	}

	public Entity getUser() {
		return mUser;
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
