package com.aircandi.ui.widgets;

import java.util.Locale;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.Html;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.LocationManager;
import com.aircandi.service.objects.CacheStamp;
import com.aircandi.service.objects.Category;
import com.aircandi.service.objects.Count;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Photo.PhotoSource;
import com.aircandi.service.objects.Place;
import com.aircandi.utilities.UI;

@SuppressWarnings("ucd")
public class CandiView extends RelativeLayout {

	public static final int	HORIZONTAL	= 0;
	public static final int	VERTICAL	= 1;

	private Entity			mEntity;
	private Integer			mLayoutId;
	private ViewGroup		mLayout;

	private AirImageView	mPhotoView;
	private AirImageView	mCategoryPhoto;
	private TextView		mName;
	private TextView		mSubtitle;
	private TextView		mDistance;
	private View			mCandiViewGroup;
	private LinearLayout	mShortcuts;
	private LinearLayout	mInfoHolder;
	private CacheStamp		mCacheStamp;

	public CandiView(Context context) {
		this(context, null);
	}

	public CandiView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CandiView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		if (attrs != null) {
			final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.CandiView, defStyle, 0);
			mLayoutId = ta.getResourceId(R.styleable.CandiView_layout, R.layout.widget_candi_view);
			ta.recycle();
			initialize();
		}
	}

	private void initialize() {

		mLayout = (ViewGroup) LayoutInflater.from(this.getContext()).inflate(mLayoutId, this, true);

		mCandiViewGroup = mLayout.findViewById(R.id.candi_view_group);
		mPhotoView = (AirImageView) mLayout.findViewById(R.id.photo);
		mName = (TextView) mLayout.findViewById(R.id.name);
		mSubtitle = (TextView) mLayout.findViewById(R.id.subtitle);
		mDistance = (TextView) mLayout.findViewById(R.id.distance);
		mCategoryPhoto = (AirImageView) mLayout.findViewById(R.id.subtitle_badge);
		mShortcuts = (LinearLayout) mLayout.findViewById(R.id.shortcuts);
		mInfoHolder = (LinearLayout) mLayout.findViewById(R.id.info_holder);
	}

	public void databind(Entity entity, IndicatorOptions options) {
		synchronized (entity) {
			/*
			 * If it is the same entity and it hasn't changed then nothing to do
			 */
			if (!entity.synthetic) {
				if (mEntity != null && entity.id.equals(mEntity.id) && mCacheStamp.equals(entity.getCacheStamp())) {
					mEntity = entity;
					showDistance(entity);
					if (options.forceUpdate) {
						showIndicators(entity, options);
					}
					return;
				}
			}
			else {
				if (mEntity != null && entity.id != null && mEntity.id != null && entity.id.equals(mEntity.id)) {
					mEntity = entity;
					showDistance(entity);
					if (options.forceUpdate) {
						showIndicators(entity, options);
					}
					return;
				}
			}

			mEntity = entity;
			mCacheStamp = entity.getCacheStamp();

			/* Primary candi image */

			drawPhoto();

			/* Background color */

			if (mCandiViewGroup != null) {
				String colorizeKey = null;
				if (((Place) mEntity).category != null) {
					colorizeKey = ((Place) mEntity).category.name;
				}
				Integer colorResId = Place.getCategoryColorResId(colorizeKey, true, Aircandi.muteColor, false);
				mCandiViewGroup.setBackgroundResource(colorResId);
			}

			/* name */

			setVisibility(mName, View.GONE);
			if (mName != null && entity.name != null && !entity.name.equals("")) {
				mName.setText(Html.fromHtml(entity.name));
				setVisibility(mName, View.VISIBLE);
			}

			/* Subtitle */

			setVisibility(mSubtitle, View.GONE);
			if (mSubtitle != null && entity.subtitle != null && !entity.subtitle.equals("")) {
				mSubtitle.setText(Html.fromHtml(entity.subtitle.toUpperCase(Locale.US)));
				setVisibility(mSubtitle, View.VISIBLE);
			}

			/* Place specific info */

			/* We take over the subtitle field and use it for categories */
			Category category = ((Place) entity).category;
			if (mSubtitle != null) {
				setVisibility(mSubtitle, View.GONE);
				if (category != null && category.name != null && !category.id.equals("generic")) {
					mSubtitle.setText(Html.fromHtml(category.name.toUpperCase(Locale.US)));
					setVisibility(mSubtitle, View.VISIBLE);
				}
			}

			/* Category photo */

			setVisibility(mCategoryPhoto, View.GONE);
			if (mCategoryPhoto != null) {
				if (category != null) {
					Photo photo = category.photo.clone();
					if (!UI.photosEqual(mCategoryPhoto.getPhoto(), photo)) {
						photo.colorize = false;
						mCategoryPhoto.setSizeHint(UI.getRawPixelsForDisplayPixels(this.getContext(), 50));
						UI.drawPhoto(mCategoryPhoto, photo);
					}
					mCategoryPhoto.setVisibility(View.VISIBLE);
				}
			}

			/* Indicators */

			showIndicators(entity, options);

			/* Distance */

			showDistance(entity);
		}
	}

	private void drawPhoto() {

		if (mPhotoView != null) {

			if (UI.photosEqual(mPhotoView.getPhoto(), mEntity.getPhoto())) {
				return;
			}

			/* Don't use gradient if we are not using a photo */
			if (mInfoHolder != null) {
				mInfoHolder.setBackgroundResource((mEntity.photo != null) ? R.drawable.overlay_picture_fadeout : 0);
			}

			Photo photo = mEntity.getPhoto();
			if (mPhotoView.getPhoto() == null || !photo.getUri().equals(mPhotoView.getPhoto().getUri())) {
				UI.drawPhoto(mPhotoView, photo);
			}
		}
	}

	public void showIndicators(Entity entity, IndicatorOptions options) {

		/* Indicators */
		setVisibility(mShortcuts, View.GONE);
		if (mShortcuts != null) {

			mShortcuts.removeAllViews();
			final int sizePixels = UI.getRawPixelsForDisplayPixels(this.getContext(), options.imageSizePixels);

			/* Post indicator always goes first */
			if (options.candigramsEnabled) {
				Count count = entity.getCount(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_CANDIGRAM, false, Direction.in);
				if (options.showIfZero && count == null) count = new Count(Constants.TYPE_LINK_CONTENT, null, 0);
				if (options.showIfZero || (count != null && (count.count.intValue() > 0))) {
					addIndicator(R.id.holder_indicator_candigrams, "resource:ic_candigrams_dark", null, sizePixels);
				}
			}

			if (options.picturesEnabled) {
				Count count = entity.getCount(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_PICTURE, false, Direction.in);
				if (options.showIfZero && count == null) count = new Count(Constants.TYPE_LINK_CONTENT, null, 0);
				if (options.showIfZero || (count != null && (count.count.intValue() > 0))) {
					addIndicator(R.id.holder_indicator_pictures, "resource:ic_pictures_dark", null, sizePixels);
				}
			}

			if (options.commentsEnabled) {
				Count count = entity.getCount(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_COMMENT, false, Direction.in);
				if (options.showIfZero && count == null) count = new Count(Constants.TYPE_LINK_CONTENT, null, 0);
				if (options.showIfZero || (count != null && (count.count.intValue() > 0))) {
					addIndicator(R.id.holder_indicator_comments, "resource:ic_comments_dark", null, sizePixels);
				}
			}

			if (options.likesEnabled) {
				Count count = entity.getCount(Constants.TYPE_LINK_LIKE, null, false, Direction.in);
				if (options.showIfZero && count == null) count = new Count(Constants.TYPE_LINK_LIKE, null, 0);
				if (options.showIfZero || (count != null && (count.count.intValue() > 0))) {
					addIndicator(R.id.holder_indicator_likes, "resource:ic_like_holo_dark", String.valueOf(count.count.intValue()), sizePixels);
				}
			}

			if (options.watchingEnabled) {
				Count count = entity.getCount(Constants.TYPE_LINK_WATCH, null, false, Direction.in);
				if (options.showIfZero && count == null) count = new Count(Constants.TYPE_LINK_WATCH, null, 0);
				if (options.showIfZero || (count != null && (count.count.intValue() > 0))) {
					addIndicator(R.id.holder_indicator_watching, "resource:ic_watched_holo_dark", String.valueOf(count.count.intValue()), sizePixels);
				}
			}
			setVisibility(mShortcuts, View.VISIBLE);
		}
	}

	private void addIndicator(Integer id, String photoUri, String name, Integer sizePixels) {

		View view = LayoutInflater.from(this.getContext()).inflate(R.layout.temp_radar_link_item, null);
		view.setId(id);
		AirImageView photoView = (AirImageView) view.findViewById(R.id.photo);
		TextView label = (TextView) view.findViewById(R.id.name);

		label.setVisibility(View.GONE);
		if (name != null) {
			label.setText(name);
			label.setVisibility(View.VISIBLE);
		}

		photoView.setSizeHint(sizePixels);
		Photo photo = new Photo(photoUri, null, null, null, PhotoSource.resource);
		UI.drawPhoto(photoView, photo);

		mShortcuts.addView(view);
	}

	public void updateIndicator(Integer id, String value) {

		View view = findViewById(id);
		if (view == null) return;

		AirImageView photoView = (AirImageView) view.findViewById(R.id.photo);
		TextView label = (TextView) view.findViewById(R.id.name);

		if (photoView != null && label != null) {
			if (!label.getText().equals(value)) {
				label.setText(value);
				UI.drawPhoto(photoView, photoView.getPhoto());
			}
		}
	}

	public void showDistance(Entity entity) {
		setVisibility(mDistance, View.GONE);
		if (mDistance != null) {

			String info = "here";
			final Float distance = entity.getDistance(true); // In meters
			final String target = entity.hasActiveProximity() ? "B:" : "L:";
			/*
			 * If distance = -1 then we don't have the location info
			 * yet needed to correctly determine distance.
			 */
			if (distance == null) {
				info = "--";
			}
			else if (distance == -1f) { // $codepro.audit.disable floatComparison
				info = "--";
			}
			else {
				final float miles = distance * LocationManager.MetersToMilesConversion;
				final float feet = distance * LocationManager.MetersToFeetConversion;
				final float yards = distance * LocationManager.MetersToYardsConversion;

				if (feet >= 0) {
					if (miles >= 0.1) {
						info = String.format(Locale.US, "%.1f mi", miles);
					}
					else if (feet >= 50) {
						info = String.format(Locale.US, "%.0f yds", yards);
					}
					else {
						info = String.format(Locale.US, "%.0f ft", feet);
					}
				}

				if (Aircandi.getInstance().getCurrentUser() != null
						&& Aircandi.settings.getBoolean(Constants.PREF_ENABLE_DEV, Constants.PREF_ENABLE_DEV_DEFAULT)
						&& Aircandi.getInstance().getCurrentUser().developer != null
						&& Aircandi.getInstance().getCurrentUser().developer) {
					info = target + info;
				}
				else {
					if (feet <= 60) {
						info = "here";
					}
				}
			}

			if (!info.equals("")) {
				mDistance.setText(Html.fromHtml(info));
				setVisibility(mDistance, View.VISIBLE);
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Setters/getters
	// --------------------------------------------------------------------------------------------

	private static void setVisibility(View view, Integer visibility) {
		if (view != null) {
			view.setVisibility(visibility);
		}
	}

	public void setPlace(Place place) {
		mEntity = place;
	}

	public void setLayoutId(Integer layoutId) {
		mLayoutId = layoutId;
	}

	public AirImageView getCandiImage() {
		return mPhotoView;
	}

	public void setCandiImage(AirImageView candiImage) {
		mPhotoView = candiImage;
	}

	public LinearLayout getTextGroup() {
		return mInfoHolder;
	}

	public void setTextGroup(LinearLayout textGroup) {
		mInfoHolder = textGroup;
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------

	public static class IndicatorOptions {
		public int		imageSizePixels		= 20;
		public boolean	showIfZero			= false;
		public boolean	forceUpdate			= false;
		public boolean	picturesEnabled		= true;
		public boolean	candigramsEnabled	= true;
		public boolean	commentsEnabled		= true;
		public boolean	likesEnabled		= true;
		public boolean	watchingEnabled		= true;
	}
}
