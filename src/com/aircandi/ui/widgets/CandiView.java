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
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aircandi.Aircandi;
import com.aircandi.CandiConstants;
import com.aircandi.R;
import com.aircandi.components.FontManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequestBuilder;
import com.aircandi.components.bitmaps.BitmapRequest.ImageResponse;
import com.aircandi.service.ProxibaseService.RequestListener;
import com.aircandi.service.objects.Category;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Entity.ImageFormat;
import com.aircandi.service.objects.Link;
import com.aircandi.service.objects.Place;
import com.aircandi.ui.Preferences;
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

	private String			mKeepColor;
	private String			mFilterColor;

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
						BitmapRequestBuilder builder = new BitmapRequestBuilder(mCandiImage)
								.setImageUri(imageUri)
								.setImageFormat(imageFormat)
								.setLinkZoom(CandiConstants.LINK_ZOOM)
								.setLinkJavascriptEnabled(CandiConstants.LINK_JAVASCRIPT_ENABLED)
								.setRequestListener(new RequestListener() {

									@Override
									public void onComplete(Object response) {

										if (mFilterColor != null) {
											Aircandi.applicationHandler.post(new Runnable() {

												@Override
												public void run() {
													mCandiImage.getImageView().setColorFilter(ImageUtils.hexToColor(mFilterColor), PorterDuff.Mode.MULTIPLY);
												}
											});
										}
									}

								});

						BitmapRequest imageRequest = builder.create();
						if (mEntity.synthetic) {
							int color = mEntity.place.getCategoryColor();
							mCandiImage.setColorFilter(color);
						}
						mCandiImage.setBitmapRequest(imageRequest);

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

				setVisibility(mCategoryImage, View.GONE);
				if (mCategoryImage != null) {
					if (entity.place.categories != null && entity.place.categories.size() > 0) {

						BitmapRequest request = new BitmapRequest();
						request.setImageUri(entity.place.categories.get(0).iconUri(false))
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
													if (mCategoryImage != null && mBadgeFilterColor != null) {
														mCategoryImage.setColorFilter(ImageUtils.hexToColor(mBadgeFilterColor), PorterDuff.Mode.MULTIPLY);
													}
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
			}
		}
	}

	public void showStats(Entity entity) {
		setVisibility(mDistance, View.GONE);
		if (Aircandi.settings.getBoolean(Preferences.PREF_SHOW_DISTANCE, false)) {
			if (mDistance != null) {
				String info = "";
				if (!entity.synthetic) {
					if (entity.links == null || entity.links.size() == 0) {
						info = String.format("M:%.0f", entity.getDistance());
					}
					else {
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
								, entity.getDistance());
					}
				}
				else {
					info = String.format("M:%.0f", entity.getDistance());
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

	public void setBadgeColorFilter(String filterColor) {
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
