package com.aircandi.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
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
import com.aircandi.CandiConstants;
import com.aircandi.R;
import com.aircandi.components.EntityList;
import com.aircandi.components.FontManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequest.ImageResponse;
import com.aircandi.components.bitmaps.BitmapRequestBuilder;
import com.aircandi.service.ProxibaseService.RequestListener;
import com.aircandi.service.objects.Category;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Place;
import com.aircandi.ui.Preferences;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.ImageUtils;

public class CandiView extends RelativeLayout {

	public static final int		HORIZONTAL				= 0;
	public static final int		VERTICAL				= 1;
	public static final float	MetersToMilesConversion	= 0.000621371192237334f;
	public static final float	MetersToFeetConversion	= 3.28084f;
	public static final float	MetersToYardsConversion	= 1.09361f;

	private Entity				mEntity;
	private Number				mEntityActivityDate;
	private Integer				mLayoutId;
	private ViewGroup			mLayout;

	private WebImageView		mCandiImage;
	private ImageView			mCategoryImage;
	private TextView			mTitle;
	private TextView			mSubtitle;
	private TextView			mDistance;
	private TextView			mPlaceRankScore;
	private View				mCandiViewGroup;
	private LinearLayout		mCandiSources;
	private LinearLayout		mTextGroup;

	public CandiView(Context context) {
		this(context, null);
	}

	public CandiView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CandiView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		if (attrs != null) {
			TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.CandiView, defStyle, 0);
			mLayoutId = ta.getResourceId(R.styleable.CandiView_layout, R.layout.widget_candi_view);
			ta.recycle();
			initialize();
		}
	}

	public void initialize() {
		LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.removeAllViews();

		mLayout = (ViewGroup) inflater.inflate(mLayoutId, this, true);

		mCandiViewGroup = (View) mLayout.findViewById(R.id.candi_view_group);
		mCandiImage = (WebImageView) mLayout.findViewById(R.id.candi_view_image);
		mTitle = (TextView) mLayout.findViewById(R.id.candi_view_title);
		mSubtitle = (TextView) mLayout.findViewById(R.id.candi_view_subtitle);
		mDistance = (TextView) mLayout.findViewById(R.id.candi_view_distance);
		mPlaceRankScore = (TextView) mLayout.findViewById(R.id.candi_view_place_rank_score);
		mCategoryImage = (ImageView) mLayout.findViewById(R.id.candi_view_subtitle_badge);
		mCandiSources = (LinearLayout) mLayout.findViewById(R.id.candi_view_sources);
		mTextGroup = (LinearLayout) mLayout.findViewById(R.id.text_group);

		FontManager.getInstance().setTypefaceRegular(mTitle);
		FontManager.getInstance().setTypefaceDefault(mSubtitle);
		FontManager.getInstance().setTypefaceDefault(mDistance);
	}

	public void bindToEntity(Entity entity) {
		/*
		 * If it is the same entity and it hasn't changed then nothing to do
		 */
		if (!entity.synthetic) {
			if (mEntity != null && entity.id.equals(mEntity.id) && entity.activityDate.longValue() == mEntityActivityDate.longValue()) {
				mEntity = entity;
				showDistance(entity);
				return;
			}
		}
		else {
			if (mEntity != null && entity.id != null && mEntity.id != null && entity.id.equals(mEntity.id)) {
				mEntity = entity;
				showDistance(entity);
				return;
			}
		}

		mEntity = entity;
		mEntityActivityDate = entity.activityDate;

		/* Primary candi image */

		Boolean boostColor = !android.os.Build.MODEL.toLowerCase().equals("nexus 4");
		int colorResId = mEntity.place.getCategoryColorResId(true, boostColor, false);
		if (mCandiImage != null) {

			/* We always color the primary background */
			//setBackgroundResource(colorResId);

			/* Handle image background coloring if we are using default treatment */
			if (mEntity.photo == null) {
				mCandiImage.setBackgroundResource(colorResId);
			}
			else {
				mCandiImage.setBackground(null);
			}

			/* Don't use gradient if we are not using a photo */
			if (mTextGroup != null) {
				if (mEntity.photo == null) {
					mTextGroup.setBackground(null);
				}
				else {
					mTextGroup.setBackgroundResource(R.drawable.picture_overlay_dark);
				}
			}

			if (mEntity.getPhoto().getBitmap() != null) {
				/*
				 * If we are carrying around a bitmap then it should be used
				 */
				ImageUtils.showImageInImageView(entity.photo.getBitmap(), mCandiImage.getImageView(), true, AnimUtils.fadeInMedium());
			}
			else {
				/*
				 * Go get the image for the entity regardless of type
				 */
				String imageUri = entity.getEntityPhotoUri();
				if (imageUri != null) {

					BitmapRequestBuilder builder = new BitmapRequestBuilder(mCandiImage).setImageUri(imageUri);
					BitmapRequest imageRequest = builder.create();

					/* Tint the image if we are using the default treatment */
					if (mEntity.type.equals(CandiConstants.TYPE_CANDI_PLACE) && mEntity.photo == null) {
						int color = mEntity.place.getCategoryColor(true, boostColor, false);
						mCandiImage.getImageView().setColorFilter(color, PorterDuff.Mode.MULTIPLY);
					}
					else {
						mCandiImage.getImageView().clearColorFilter();
					}

					mCandiImage.setBitmapRequest(imageRequest);
				}
			}
			if (entity.type.equals(CandiConstants.TYPE_CANDI_PLACE)) {
				if (mCandiImage.getImageBadge() != null) {
					mCandiImage.getImageBadge().setVisibility(View.GONE);
				}
				if (mCandiImage.getImageZoom() != null) {
					mCandiImage.getImageZoom().setVisibility(View.GONE);
				}
				mCandiImage.setClickable(false);
			}
			else if (entity.type.equals(CandiConstants.TYPE_CANDI_POST)) {
				mCandiImage.getImageBadge().setVisibility(View.GONE);
				mCandiImage.getImageZoom().setVisibility(View.GONE);
				mCandiImage.setClickable(false);
			}
			else {
				mCandiImage.getImageBadge().setVisibility(View.GONE);
				mCandiImage.getImageZoom().setVisibility(View.VISIBLE);
				mCandiImage.setClickable(true);
			}
		}

		if (mCandiViewGroup != null) {
			Integer padding = ImageUtils.getRawPixels(this.getContext(), 3);
			this.setPadding(padding, padding, padding, padding);
			this.setBackgroundResource(R.drawable.app_image_selector);
			mCandiViewGroup.setTag(entity);
			mCandiViewGroup.setBackgroundResource(colorResId);
		}

		setVisibility(mTitle, View.GONE);
		if (mTitle != null && entity.name != null && !entity.name.equals("")) {
			mTitle.setText(Html.fromHtml(entity.name));
			setVisibility(mTitle, View.VISIBLE);
		}

		setVisibility(mSubtitle, View.GONE);
		if (mSubtitle != null && entity.subtitle != null && !entity.subtitle.equals("")) {
			mSubtitle.setText(Html.fromHtml(entity.subtitle));
			setVisibility(mSubtitle, View.VISIBLE);
		}

		/* Place specific info */
		if (entity.place != null) {
			final Place place = entity.place;

			/* We take over the subtitle field and use it for categories */
			if (mSubtitle != null) {
				setVisibility(mSubtitle, View.GONE);
				if (place.categories != null && place.categories.size() > 0) {
					String categories = "";
					for (Category category : place.categories) {
						if (category.primary != null && category.primary) {
							categories += "<b>" + category.name + "</b>, ";
						}
						else {
							categories += category.name + ", ";
						}
					}
					categories = categories.substring(0, categories.length() - 2);
					mSubtitle.setText(Html.fromHtml(categories));
					setVisibility(mSubtitle, View.VISIBLE);
				}
			}

			setVisibility(mCategoryImage, View.GONE);
			if (mCategoryImage != null) {
				if (entity.place.categories != null && entity.place.categories.size() > 0) {

					BitmapRequest request = new BitmapRequest();
					request.setImageUri(entity.place.categories.get(0).iconUri())
							.setImageRequestor(this)
							.setRequestListener(new RequestListener() {

								@Override
								public void onComplete(Object response) {

									ServiceResponse serviceResponse = (ServiceResponse) response;
									if (serviceResponse.responseCode == ResponseCode.Success) {

										ImageResponse imageResponse = (ImageResponse) serviceResponse.data;
										Bitmap bitmap = imageResponse.bitmap;
										final BitmapDrawable bitmapDrawable = new BitmapDrawable(Aircandi.applicationContext.getResources(), bitmap);

										Aircandi.applicationHandler.post(new Runnable() {
											@Override
											public void run() {
												ImageUtils.showDrawableInImageView(bitmapDrawable, mCategoryImage, false, AnimUtils.fadeInMedium());
											}
										});
									}
								}
							});
					BitmapManager.getInstance().fetchBitmap(request);
					mCategoryImage.setVisibility(View.VISIBLE);
				}
			}

			if (mSubtitle != null && entity.subtitle != null && !entity.subtitle.equals("")) {
				mSubtitle.setText(Html.fromHtml(entity.subtitle));
				setVisibility(mSubtitle, View.VISIBLE);
			}

			/* Sources */

			setVisibility(mCandiSources, View.GONE);
			if (mCandiSources != null && entity.sources != null && entity.sources.size() > 0) {
				mCandiSources.removeAllViews();
				EntityList<Entity> entities = entity.getChildren();
				final LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				int sizePixels = ImageUtils.getRawPixels(this.getContext(), 20);
				int marginPixels = ImageUtils.getRawPixels(this.getContext(), 3);

				/* We only show the first five */
				int sourceCount = 0;
				for (Entity sourceEntity : entities) {
					if (sourceEntity.source.equals("comments") && entity.commentCount == 0) {
						continue;
					}
					if (sourceCount >= 5) {
						break;
					}
					View view = inflater.inflate(R.layout.temp_radar_candi_item, null);
					WebImageView webImageView = (WebImageView) view.findViewById(R.id.image);

					String imageUri = sourceEntity.getEntityPhotoUri();
					/* TODO: temp fixup until I figure out what to do with icons that look bad against color backgrounds */
					if (sourceEntity.source.equals("yelp")) {
						imageUri = "resource:source_yelp_white";
					}
					if (sourceEntity.source.equals("twitter")) {
						imageUri = "resource:source_twitter_ii";
					}
					BitmapRequestBuilder builder = new BitmapRequestBuilder(webImageView).setImageUri(imageUri);
					BitmapRequest imageRequest = builder.create();
					webImageView.setBitmapRequest(imageRequest);

					LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sizePixels, sizePixels);
					params.setMargins(marginPixels
							, marginPixels
							, marginPixels
							, marginPixels);
					view.setLayoutParams(params);
					mCandiSources.addView(view);
					sourceCount++;
				}
				setVisibility(mCandiSources, View.VISIBLE);
			}

			/* Distance */
			showDistance(entity);

			/* Place rank score - dev only */
			setVisibility(mPlaceRankScore, View.GONE);
			if (mPlaceRankScore != null && Aircandi.settings.getBoolean(Preferences.PREF_SHOW_PLACE_RANK_SCORE, false)) {
				mPlaceRankScore.setText(String.valueOf(entity.getPlaceRankScore()));
				setVisibility(mPlaceRankScore, View.VISIBLE);
			}
		}
	}

	public void showDistance(Entity entity) {
		setVisibility(mDistance, View.GONE);
		if (mDistance != null) {
			String info = "here";
			float distance = entity.getDistance();
			/*
			 * If distance = -1 then we don't have the location info
			 * yet needed to correctly determine distance.
			 */
			if (distance == -1f) {
				info = "--";
			}
			else {
				float miles = distance * MetersToMilesConversion;
				float feet = distance * MetersToFeetConversion;
				float yards = distance * MetersToYardsConversion;

				if (feet > 0) {
					if (miles >= 0.1) {
						info = String.format("%.1f mi", miles);
					}
					else if (feet >= 50) {
						info = String.format("%.0f yds", yards);
					}
					else  {
						info = String.format("%.0f ft", feet);
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

	protected static void setVisibility(View view, Integer visibility) {
		if (view != null) {
			view.setVisibility(visibility);
		}
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
	}

	public void setEntity(Entity entity) {
		this.mEntity = entity;
	}

	public void setLayoutId(Integer layoutId) {
		mLayoutId = layoutId;
	}

	public WebImageView getCandiImage() {
		return mCandiImage;
	}

	public void setCandiImage(WebImageView candiImage) {
		mCandiImage = candiImage;
	}

	public ImageView getCategoryImage() {
		return mCategoryImage;
	}

	public void setCategoryImage(ImageView categoryImage) {
		mCategoryImage = categoryImage;
	}

	public LinearLayout getTextGroup() {
		return mTextGroup;
	}

	public void setTextGroup(LinearLayout textGroup) {
		mTextGroup = textGroup;
	}
}
