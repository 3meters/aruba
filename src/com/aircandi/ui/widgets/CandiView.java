package com.aircandi.ui.widgets;

import java.util.Locale;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.text.Html;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.beta.R;
import com.aircandi.components.LocationManager;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.service.objects.Count;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.service.objects.Place;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.UI;

@SuppressWarnings("ucd")
public class CandiView extends RelativeLayout {

	public static final int	HORIZONTAL	= 0;
	public static final int	VERTICAL	= 1;

	private Place			mPlace;
	private Number			mEntityActivityDate;
	private Integer			mLayoutId;
	private ViewGroup		mLayout;

	private WebImageView	mPhoto;
	private ImageView		mCategoryImage;
	private TextView		mName;
	private TextView		mSubtitle;
	private TextView		mDistance;
	private TextView		mPlaceRankScore;
	private View			mCandiViewGroup;
	private LinearLayout	mApplinks;
	private LinearLayout	mInfoHolder;

	private Integer			mColorResId;
	private Boolean			mMuteColor;
	private LayoutInflater	mInflater;

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

		mInflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mLayout = (ViewGroup) mInflater.inflate(mLayoutId, this, true);

		mCandiViewGroup = mLayout.findViewById(R.id.candi_view_group);
		mPhoto = (WebImageView) mLayout.findViewById(R.id.photo);
		mName = (TextView) mLayout.findViewById(R.id.name);
		mSubtitle = (TextView) mLayout.findViewById(R.id.subtitle);
		mDistance = (TextView) mLayout.findViewById(R.id.distance);
		mPlaceRankScore = (TextView) mLayout.findViewById(R.id.place_rank_score);
		mCategoryImage = (ImageView) mLayout.findViewById(R.id.subtitle_badge);
		mApplinks = (LinearLayout) mLayout.findViewById(R.id.shortcuts);
		mInfoHolder = (LinearLayout) mLayout.findViewById(R.id.info_holder);
	}

	public void bindToPlace(Place place) {
		synchronized (place) {
			/*
			 * If it is the same entity and it hasn't changed then nothing to do
			 */
			if (!place.synthetic) {
				if (mPlace != null
						&& place.id.equals(mPlace.id)
						&& place.activityDate.longValue() == mEntityActivityDate.longValue()) {
					mPlace = place;
					showDistance(place);
					return;
				}
			}
			else {
				if (mPlace != null
						&& place.id != null && mPlace.id != null
						&& place.id.equals(mPlace.id)) {
					mPlace = place;
					showDistance(place);
					return;
				}
			}

			/* Clear image as quickly as possible */
			if (mPhoto != null) {
				mPhoto.getImageView().setImageDrawable(null);
			}

			mPlace = place;
			mEntityActivityDate = place.activityDate;
			mMuteColor = android.os.Build.MODEL.toLowerCase(Locale.US).equals("nexus s"); // nexus 4, nexus 7 are others
			mColorResId = Place.getCategoryColorResId((mPlace.category != null) ? mPlace.category.name : null, true, mMuteColor, false);

			/* Primary candi image */

			drawImage();

			if (mCandiViewGroup != null) {
				final Integer padding = UI.getRawPixels(this.getContext(), 3);
				this.setPadding(padding, padding, padding, padding);
				this.setBackgroundResource(R.drawable.selector_image);
				mCandiViewGroup.setBackgroundResource(mColorResId);
			}

			setVisibility(mName, View.GONE);
			if (mName != null && place.name != null && !place.name.equals("")) {
				mName.setText(Html.fromHtml(place.name));
				setVisibility(mName, View.VISIBLE);
			}

			setVisibility(mSubtitle, View.GONE);
			if (mSubtitle != null && place.subtitle != null && !place.subtitle.equals("")) {
				mSubtitle.setText(Html.fromHtml(place.subtitle.toUpperCase(Locale.US)));
				setVisibility(mSubtitle, View.VISIBLE);
			}

			/* Place specific info */

			/* We take over the subtitle field and use it for categories */
			if (mSubtitle != null) {
				setVisibility(mSubtitle, View.GONE);
				if (place.category != null) {
					mSubtitle.setText(Html.fromHtml(place.category.name.toUpperCase(Locale.US)));
					setVisibility(mSubtitle, View.VISIBLE);
				}
			}

			setVisibility(mCategoryImage, View.GONE);
			if (mCategoryImage != null) {
				if (place.category != null) {
					mCategoryImage.setTag(place.category.photo.getUri());
					final BitmapRequest bitmapRequest = new BitmapRequest(place.category.photo.getUri(), mCategoryImage);
					bitmapRequest.setImageRequestor(mCategoryImage);
					bitmapRequest.setImageSize(UI.getRawPixels(this.getContext(), 50));
					BitmapManager.getInstance().masterFetch(bitmapRequest);
					mCategoryImage.setVisibility(View.VISIBLE);
				}
			}

			if (mSubtitle != null && place.subtitle != null && !place.subtitle.equals("")) {
				mSubtitle.setText(Html.fromHtml(place.subtitle.toUpperCase(Locale.US)));
				setVisibility(mSubtitle, View.VISIBLE);
			}

			/* Sources */

			setVisibility(mApplinks, View.GONE);
			if (mApplinks != null && !place.synthetic) {

				mApplinks.removeAllViews();
				final int sizePixels = UI.getRawPixels(this.getContext(), 20);
				final int marginPixels = UI.getRawPixels(this.getContext(), 3);

				/* Post indicator always goes first */
				Count count = place.getCount(Constants.TYPE_LINK_POST, Direction.in);
				if (count != null && place.getCount(Constants.TYPE_LINK_POST, Direction.in).count.intValue() > 0) {
					addApplinkIndicator("resource:ic_candi_dark", sizePixels, marginPixels);
				}

				count = place.getCount(Constants.TYPE_LINK_COMMENT, Direction.in);
				if (count != null && place.getCount(Constants.TYPE_LINK_COMMENT, Direction.in).count.intValue() > 0) {
					addApplinkIndicator("resource:ic_comments_dark", sizePixels, marginPixels);
				}

				count = place.getCount(Constants.TYPE_LINK_LIKE, Direction.in);
				if (count != null && place.getCount(Constants.TYPE_LINK_LIKE, Direction.in).count.intValue() > 0) {
					addApplinkIndicator("resource:ic_like_holo_dark", sizePixels, marginPixels);
				}

				count = place.getCount(Constants.TYPE_LINK_WATCH, Direction.in);
				if (count != null && place.getCount(Constants.TYPE_LINK_WATCH, Direction.in).count.intValue() > 0) {
					addApplinkIndicator("resource:ic_watched_holo_dark", sizePixels, marginPixels);
				}
				setVisibility(mApplinks, View.VISIBLE);
			}

			/* Distance */
			showDistance(place);
		}
	}

	private void addApplinkIndicator(String photoUri, Integer sizePixels, Integer marginPixels) {

		View view = mInflater.inflate(R.layout.temp_radar_candi_item, null);
		WebImageView webImageView = (WebImageView) view.findViewById(R.id.photo);
		webImageView.setSizeHint(sizePixels);

		webImageView.getImageView().setTag(photoUri);

		BitmapRequest bitmapRequest = new BitmapRequest(photoUri, webImageView.getImageView());
		bitmapRequest.setImageSize(mPhoto.getSizeHint());
		bitmapRequest.setImageRequestor(webImageView.getImageView());
		BitmapManager.getInstance().masterFetch(bitmapRequest);

		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sizePixels, sizePixels);
		params.setMargins(marginPixels
				, marginPixels
				, marginPixels
				, marginPixels);
		view.setLayoutParams(params);
		mApplinks.addView(view);

	}

	private void drawImage() {
		if (mPhoto != null) {

			/* Don't use gradient if we are not using a photo */
			if (mInfoHolder != null) {
				mInfoHolder.setBackgroundResource((mPlace.photo != null) ? R.drawable.overlay_picture : 0);
			}

			if (mPlace.photo != null && mPlace.photo.getBitmap() != null) {
				/*
				 * If we are carrying around a bitmap then it should be used
				 */
				UI.showImageInImageView(mPlace.photo.getBitmap(), mPhoto.getImageView(), true, Animate.fadeInMedium());
			}
			else {

				/* Remove colored filters */
				mPhoto.getImageView().clearColorFilter();
				mPhoto.setBackgroundResource(0);
				(mLayout.findViewById(R.id.color_layer)).setVisibility(View.GONE);
				(mLayout.findViewById(R.id.reverse_layer)).setVisibility(View.GONE);

				/* Go get the image for the entity regardless of type */
				final String photoUri = mPlace.getPhotoUri();

				if (photoUri != null) {

					if (photoUri.equals("resource:img_placeholder_logo_bw")) {
						(mLayout.findViewById(R.id.reverse_layer)).setVisibility(View.VISIBLE);
						mPhoto.getImageView().setTag(photoUri);
						mPhoto.getImageView().setImageDrawable(null);
					}
					else {

						/* Tint the image if we are using the default treatment */
						if (mPlace.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
							if (mPlace.photo == null && mPlace.category != null) {

								final int color = Place.getCategoryColor((mPlace.category != null)
										? mPlace.category.name
										: null, true, mMuteColor, false);

								mPhoto.getImageView().setColorFilter(color, PorterDuff.Mode.MULTIPLY);
								mPhoto.setBackgroundResource(mColorResId);
								(mLayout.findViewById(R.id.color_layer)).setVisibility(View.VISIBLE);
								(mLayout.findViewById(R.id.reverse_layer)).setVisibility(View.VISIBLE);
							}
						}

						final BitmapRequest bitmapRequest = new BitmapRequest(photoUri, mPhoto.getImageView());
						bitmapRequest.setImageSize(mPhoto.getSizeHint());
						bitmapRequest.setImageRequestor(mPhoto.getImageView());
						mPhoto.getImageView().setTag(photoUri);
						BitmapManager.getInstance().masterFetch(bitmapRequest);
					}
				}
			}
		}
	}

	public void showDistance(Place place) {
		setVisibility(mDistance, View.GONE);
		if (mDistance != null) {

			String info = "here";
			final float distance = place.getDistance(true); // In meters
			final String target = place.hasActiveProximityLink() ? "B:" : "L:";
			/*
			 * If distance = -1 then we don't have the location info
			 * yet needed to correctly determine distance.
			 */
			if (distance == -1f) { // $codepro.audit.disable floatComparison
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

				if (Aircandi.getInstance().getUser() != null
						&& Aircandi.settings.getBoolean(Constants.PREF_ENABLE_DEV, Constants.PREF_ENABLE_DEV_DEFAULT)
						&& Aircandi.getInstance().getUser().developer != null
						&& Aircandi.getInstance().getUser().developer) {
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
		mPlace = place;
	}

	public void setLayoutId(Integer layoutId) {
		mLayoutId = layoutId;
	}

	public WebImageView getCandiImage() {
		return mPhoto;
	}

	public void setCandiImage(WebImageView candiImage) {
		mPhoto = candiImage;
	}

	public ImageView getCategoryImage() {
		return mCategoryImage;
	}

	public void setCategoryImage(ImageView categoryImage) {
		mCategoryImage = categoryImage;
	}

	public LinearLayout getTextGroup() {
		return mInfoHolder;
	}

	public void setTextGroup(LinearLayout textGroup) {
		mInfoHolder = textGroup;
	}

	public TextView getPlaceRankScore() {
		return mPlaceRankScore;
	}

	public void setPlaceRankScore(TextView placeRankScore) {
		mPlaceRankScore = placeRankScore;
	}
}
