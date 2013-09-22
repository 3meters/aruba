package com.aircandi.ui.widgets;

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
import com.aircandi.R;
import com.aircandi.service.objects.Count;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.service.objects.User;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.DateTime.IntervalContext;
import com.aircandi.utilities.UI;

@SuppressWarnings("ucd")
public class UserView extends RelativeLayout {

	private ViewGroup		mBoundView;
	private AirImageView	mPhotoView;
	private ImageView		mImageLocked;
	private ImageView		mImageWatched;
	private ImageView		mImageLiked;
	private TextView		mName;
	private TextView		mArea;
	private TextView		mLabel;
	private TextView		mLikeCount;
	private TextView		mWatchCount;
	private TextView		mTimeSince;

	private Entity			mUser;
	private String			mLabelString;
	private Long			mDate;
	private Boolean			mLocked	= false;

	public UserView(Context context) {
		this(context, null);
	}

	public UserView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public UserView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.UserLayout, defStyle, 0);
		final int layoutId = ta.getResourceId(R.styleable.UserLayout_layout, R.layout.widget_user_view_detailed);
		mBoundView = (ViewGroup) LayoutInflater.from(this.getContext()).inflate(layoutId, null);

		ta.recycle();
		initialize();
	}

	private void initialize() {
		if (!isInEditMode()) {
			mPhotoView = (AirImageView) mBoundView.findViewById(R.id.widget_photo);
			mName = (TextView) mBoundView.findViewById(R.id.widget_name);
			mArea = (TextView) mBoundView.findViewById(R.id.area);
			mLabel = (TextView) mBoundView.findViewById(R.id.label);
			mTimeSince = (TextView) mBoundView.findViewById(R.id.timesince);
			mImageLocked = (ImageView) mBoundView.findViewById(R.id.image_locked);
			mImageWatched = (ImageView) mBoundView.findViewById(R.id.image_watched);
			mImageLiked = (ImageView) mBoundView.findViewById(R.id.image_liked);
			mLikeCount = (TextView) mBoundView.findViewById(R.id.like_count);
			mWatchCount = (TextView) mBoundView.findViewById(R.id.watch_count);
		}

		this.removeAllViews();
		this.addView(mBoundView);
	}

	public void databind(Entity user, Long date) {
		databind(user, date, null);
	}

	public void databind(Entity entity, Long date, Boolean locked) {
		mUser = entity;
		mDate = date;
		mLocked = locked;
		this.setTag(entity);
		draw();
	}

	private void draw() {
		User user = (User) mUser;

		if (user != null) {
			if (mLabel != null) {
				if (mLabelString != null) {
					mLabel.setText(mLabelString);
				}
				else {
					mLabel.setVisibility(View.GONE);
				}
			}

			if (mName != null) {
				if (user.name != null) {
					mName.setText(user.name);
				}
			}

			if (mArea != null && user.area != null && !user.area.equals("")) {
				mArea.setText(Html.fromHtml(user.area));
			}

			if (mTimeSince != null) {
				if (mDate != null) {
					mTimeSince.setText(DateTime.interval(mDate, DateTime.nowDate().getTime(), IntervalContext.PAST));
				}
				else {
					mTimeSince.setVisibility(View.GONE);
				}
			}

			if (mPhotoView != null) {
				if (mPhotoView.getPhoto() == null || !mPhotoView.getPhoto().getUri().equals(user.getPhoto().getUri())) {
					UI.drawPhoto(mPhotoView, user.getPhoto());
				}
			}

			if (mImageLocked != null) {
				if (mLocked != null && mLocked) {
					mImageLocked.setVisibility(View.VISIBLE);
				}
				else {
					mImageLocked.setVisibility(View.INVISIBLE);
				}
			}

			if (mBoundView.findViewById(R.id.stats_group) != null) {
				if (mImageWatched != null) {
					Count count = user.getCount(Constants.TYPE_LINK_WATCH, Direction.in);
					mWatchCount.setText(String.valueOf(count != null ? count.count.intValue() : 0));
				}
				if (mImageLiked != null) {
					Count count = user.getCount(Constants.TYPE_LINK_LIKE, Direction.in);
					mLikeCount.setText(String.valueOf(count != null ? count.count.intValue() : 0));
				}
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Set/GET
	// --------------------------------------------------------------------------------------------

	public void setLabel(String label) {
		mLabelString = label;
	}
}
