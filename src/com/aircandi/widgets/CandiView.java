package com.aircandi.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.text.Html;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.graphics.PorterDuff;

import com.aircandi.Aircandi;
import com.aircandi.Preferences;
import com.aircandi.R;
import com.aircandi.components.DrawableManager.ViewHolder;
import com.aircandi.components.GeoLocationManager.MeasurementSystem;
import com.aircandi.components.FontManager;
import com.aircandi.components.ImageManager;
import com.aircandi.components.ImageRequest;
import com.aircandi.components.ImageRequestBuilder;
import com.aircandi.core.CandiConstants;
import com.aircandi.service.ProxibaseService.RequestListener;
import com.aircandi.service.objects.Category;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Entity.ImageFormat;
import com.aircandi.service.objects.Link;
import com.aircandi.service.objects.Place;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.ImageUtils;

public class CandiView extends RelativeLayout {

	public static final int	HORIZONTAL	= 0;
	public static final int	VERTICAL	= 1;

	private Entity			mEntity;
	private Integer			mLayoutId;
	private ViewGroup		mLayout;

	private WebImageView	mCandiImage;
	private ImageView		mCategoryImage;
	private TextView		mTitle;
	private TextView		mSubtitle;
	private TextView		mDistance;
	private View			mCandiViewGroup;

	private String			mFromColor;
	private String			mToColor;
	private String			mKeepColor;
	private String			mFilterColor;

	private String			mBadgeFromColor;
	private String			mBadgeToColor;
	private String			mBadgeKeepColor;
	private String			mBadgeFilterColor;

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
		mCategoryImage = (ImageView) mLayout.findViewById(R.id.candi_view_subtitle_badge);
		
		FontManager.getInstance().setTypefaceRegular(mTitle);
		FontManager.getInstance().setTypefaceDefault(mSubtitle);

	}

	public void bindToEntity(Entity entity) {
		/*
		 * If it is the same entity and it hasn't changed then nothing to do
		 */
		if (!entity.synthetic) {
			if (mEntity != null && entity.id.equals(mEntity.id) && entity.activityDate.longValue() == mEntity.activityDate.longValue()) {
				mEntity = entity;
				/* We still should update stats */
				if (entity.place != null) {
					showStats(entity);
				}
				return;
			}
		}
		else {
			if (mEntity != null && entity.id != null && mEntity.id != null && entity.id.equals(mEntity.id)) {
				mEntity = entity;
				/* We still should update stats */
				if (entity.place != null) {
					showStats(entity);
				}
				return;
			}
		}

		mEntity = entity;
		if (mEntity != null) {

			/* Primary candi image */

			if (mCandiImage != null) {

				/* If we are carrying around a bitmap then it should be used */
				if (mEntity.photo != null && mEntity.photo.getBitmap() != null) {
					ImageUtils.showImageInImageView(entity.photo.getBitmap(), mCandiImage.getImageView(), true, AnimUtils.fadeInMedium());
				}
				else {

					String imageUri = entity.getImageUri();
					if (imageUri != null) {

						ImageFormat imageFormat = entity.getImageFormat();
						ImageRequestBuilder builder = new ImageRequestBuilder(mCandiImage)
								.setImageUri(imageUri)
								.setImageFormat(imageFormat)
								.setLinkZoom(CandiConstants.LINK_ZOOM)
								.setLinkJavascriptEnabled(CandiConstants.LINK_JAVASCRIPT_ENABLED)
								.setRequestListener(new RequestListener() {

									@Override
									public void onComplete(Object response) {
										if (mFilterColor != null) {
											mCandiImage.getImageView().setColorFilter(ImageUtils.hexToColor(mFilterColor), PorterDuff.Mode.MULTIPLY);
										}
									}

									@Override
									public Bitmap onFilter(Bitmap bitmap) {
										/*
										 * Turn gray pixels to transparent. Making the bitmap mutable will
										 * put pressure on memory so this should only be done when working with
										 * small images. There is an ImageUtils routine that will make make a
										 * bitmap mutable without using extra memory.
										 */
										Bitmap transformedBitmap = null;
										if (mKeepColor == null && mFromColor == null && mToColor == null) {
											return bitmap;
										}
										else {
											transformedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
											bitmap.recycle();

											if (mKeepColor != null) {
												transformedBitmap = ImageUtils.keepPixels(transformedBitmap, mKeepColor);
											}

											if (mFromColor != null && mToColor != null) {
												transformedBitmap = ImageUtils.replacePixels(transformedBitmap, mFromColor, mToColor);
											}
										}

										return transformedBitmap;
									}
								});

						ImageRequest imageRequest = builder.create();
						if (mEntity.synthetic) {
							int color = mEntity.place.getCategoryColor();
							mCandiImage.setColorFilter(color);
						}
						mCandiImage.setImageRequest(imageRequest);

					}
				}
				if (entity.type.equals(CandiConstants.TYPE_CANDI_FOLDER)) {
					if (entity.getImageUri() == null
							|| !entity.getImageUri().toLowerCase().startsWith("resource:")) {
						mCandiImage.getImageBadge().setImageResource(R.drawable.ic_collection_250);
						mCandiImage.getImageBadge().setVisibility(View.VISIBLE);
						mCandiImage.getImageZoom().setVisibility(View.VISIBLE);
						mCandiImage.setClickable(true);
					}
					else {
						mCandiImage.getImageBadge().setVisibility(View.GONE);
						mCandiImage.getImageZoom().setVisibility(View.GONE);
						mCandiImage.setClickable(false);
					}
				}
				else if (entity.type.equals(CandiConstants.TYPE_CANDI_PLACE)) {
					mCandiImage.getImageBadge().setVisibility(View.GONE);
					mCandiImage.getImageZoom().setVisibility(View.GONE);
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
				mCandiViewGroup.setTag(entity);
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
				
				/* Developer only stats */
				showStats(entity);

				setVisibility(mDistance, View.GONE);
				if (Aircandi.settings.getBoolean(Preferences.PREF_SHOW_DISTANCE, false)) {
					if (mDistance != null) {
						String info = "";
						if (!entity.synthetic) {
							int primaryCount = 0;
							for (Link link : entity.links) {
								if (link.primary) {
									primaryCount++;
								}
							}
							info = String.format("T:%d L:%d P:%d M:%.0f"
									, entity.getTuningScore()
									, entity.links.size()
									, primaryCount
									, entity.getDistance(MeasurementSystem.Metric));
						}
						else {
							info = String.format("M:%.0f", entity.getDistance(MeasurementSystem.Metric));
						}

						if (!info.equals("")) {
							mDistance.setText(Html.fromHtml(info));
							setVisibility(mDistance, View.VISIBLE);
						}
					}
				}

				setVisibility(mCategoryImage, View.GONE);
				if (mCategoryImage != null) {
					//mCategoryImage.setImageResource(R.drawable.ic_action_location_dark);
					if (entity.place.categories != null && entity.place.categories.size() > 0) {
						ViewHolder holder = new ViewHolder();
						holder.itemImage = mCategoryImage;
						holder.itemImage.setTag(entity.place.categories.get(0).iconUri());
						ImageManager.getInstance().getDrawableManager()
								.fetchDrawableOnThread(entity.place.categories.get(0).iconUri(), holder, new RequestListener() {

									@Override
									public void onComplete(Object response) {
										if (mCategoryImage != null && mBadgeFilterColor != null) {
											mCategoryImage.setColorFilter(ImageUtils.hexToColor(mBadgeFilterColor), PorterDuff.Mode.MULTIPLY);
										}
									}

									@Override
									public Bitmap onFilter(Bitmap bitmap) {
										/*
										 * Turn gray pixels to transparent. Making the bitmap mutable will
										 * put pressure on memory so this should only be done when working with
										 * small images. There is an ImageUtils routine that will make make a
										 * bitmap mutable without using extra memory.
										 */
										Bitmap transformedBitmap = null;
										if (mBadgeKeepColor == null && mBadgeFromColor == null && mBadgeToColor == null) {
											return bitmap;
										}
										else {
											transformedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
											//bitmap.recycle();

											if (mBadgeKeepColor != null) {
												transformedBitmap = ImageUtils.keepPixels(transformedBitmap, mBadgeKeepColor);
											}

											if (mBadgeFromColor != null && mBadgeToColor != null) {
												transformedBitmap = ImageUtils.replacePixels(transformedBitmap, mBadgeFromColor, mBadgeToColor);
											}
										}
										return transformedBitmap;
									}

								});
						mCategoryImage.setVisibility(View.VISIBLE);
					}
				}

				if (mSubtitle != null && entity.subtitle != null && !entity.subtitle.equals("")) {
					mSubtitle.setText(Html.fromHtml(entity.subtitle));
					setVisibility(mSubtitle, View.VISIBLE);
				}
			}
		}
	}

	public void showStats(Entity entity) {
		setVisibility(mDistance, View.GONE);
		if (Aircandi.settings.getBoolean(Preferences.PREF_SHOW_DISTANCE, false)) {
			if (mDistance != null) {
				String info = "";
				if (!entity.synthetic) {
					int primaryCount = 0;
					for (Link link : entity.links) {
						if (link.primary) {
							primaryCount++;
						}
					}
					info = String.format("T:%d L:%d P:%d M:%.0f"
							, entity.getTuningScore()
							, entity.links.size()
							, primaryCount
							, entity.getDistance(MeasurementSystem.Metric));
				}
				else {
					info = String.format("M:%.0f", entity.getDistance(MeasurementSystem.Metric));
				}

				if (!info.equals("")) {
					mDistance.setText(Html.fromHtml(info));
					setVisibility(mDistance, View.VISIBLE);
				}
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

	public void setColorFilter(String filterColor, String fromColor, String toColor, String keepColor) {
		mFromColor = fromColor;
		mToColor = toColor;
		mKeepColor = keepColor;
		mFilterColor = filterColor;
	}

	public void setBadgeColorFilter(String filterColor, String fromColor, String toColor, String keepColor) {
		mBadgeFromColor = fromColor;
		mBadgeToColor = toColor;
		mBadgeKeepColor = keepColor;
		mBadgeFilterColor = filterColor;
	}

	public WebImageView getCandiImage() {
		return mCandiImage;
	}

	public void setCandiImage(WebImageView candiImage) {
		mCandiImage = candiImage;
	}

	public String getKeepColor() {
		return mKeepColor;
	}

	public void setKeepColor(String keepColor) {
		mKeepColor = keepColor;
	}

	public ImageView getCategoryImage() {
		return mCategoryImage;
	}

	public void setCategoryImage(ImageView categoryImage) {
		mCategoryImage = categoryImage;
	}
}
