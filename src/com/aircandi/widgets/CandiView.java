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

import com.aircandi.R;
import com.aircandi.components.DrawableManager.ViewHolder;
import com.aircandi.components.ImageManager;
import com.aircandi.components.ImageRequest;
import com.aircandi.components.ImageRequestBuilder;
import com.aircandi.components.ImageUtils;
import com.aircandi.core.CandiConstants;
import com.aircandi.service.ProxibaseService.RequestListener;
import com.aircandi.service.objects.Category;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Entity.ImageFormat;
import com.aircandi.service.objects.Place;

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
	private String			mFromColor;
	private String			mToColor;

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

		mCandiImage = (WebImageView) mLayout.findViewById(R.id.image);
		mTitle = (TextView) mLayout.findViewById(R.id.title);
		mSubtitle = (TextView) mLayout.findViewById(R.id.subtitle);
		mCategoryImage = (ImageView) mLayout.findViewById(R.id.subtitle_badge);
	}

	public void bindToEntity(Entity entity) {
		mEntity = entity;
		if (mEntity != null) {

			/* Primary candi image */

			if (mCandiImage != null) {
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
								public Bitmap onFilter(Bitmap bitmap) {
									/*
									 * Turn gray pixels to transparent. Making the bitmap mutable will
									 * put pressure on memory so this should only be done when working with
									 * small images. There is an ImageUtils routine that will make make a
									 * bitmap mutable without using extra memory.
									 */
									if (mFromColor != null && mToColor != null) {
										if (!bitmap.isMutable()) {
											Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
											bitmap.recycle();
											mutableBitmap = ImageUtils.replacePixels(mutableBitmap, mFromColor, mToColor);
											return mutableBitmap;
										}
										else {
											bitmap = ImageUtils.replacePixels(bitmap, mFromColor, mToColor);
											return bitmap;
										}
									}
									return bitmap;
								}
							});

					ImageRequest imageRequest = builder.create();
					mCandiImage.setImageRequest(imageRequest);

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
				if (mCategoryImage != null && entity.place.categories != null && entity.place.categories.size() > 0) {
					ViewHolder holder = new ViewHolder();
					holder.itemImage = mCategoryImage;
					holder.itemImage.setTag(entity.place.categories.get(0).iconUri());
					ImageManager.getInstance().getDrawableManager()
							.fetchDrawableOnThread(entity.place.categories.get(0).iconUri(), holder, new RequestListener() {

								@Override
								public Bitmap onFilter(Bitmap bitmap) {
									/*
									 * Turn gray pixels to transparent. Making the bitmap mutable will
									 * put pressure on memory so this should only be done when working with
									 * small images. There is an ImageUtils routine that will make make a
									 * bitmap mutable without using extra memory.
									 */
									if (!bitmap.isMutable()) {
										Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
										bitmap.recycle();
										mutableBitmap = ImageUtils.replacePixels(mutableBitmap, "#ffc4c3bc", "#00000000");
										return mutableBitmap;
									}
									else {
										bitmap = ImageUtils.replacePixels(bitmap, "#ffc4c3bc", "#00000000");
										return bitmap;
									}
								}

							});
					mCategoryImage.setVisibility(View.VISIBLE);
				}

				if (mSubtitle != null && entity.subtitle != null && !entity.subtitle.equals("")) {
					mSubtitle.setText(Html.fromHtml(entity.subtitle));
					setVisibility(mSubtitle, View.VISIBLE);
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

	public void setColorFilter(String fromColor, String toColor) {
		mFromColor = fromColor;
		mToColor = toColor;
	}

	public WebImageView getCandiImage() {
		return mCandiImage;
	}

	public void setCandiImage(WebImageView candiImage) {
		mCandiImage = candiImage;
	}
}
