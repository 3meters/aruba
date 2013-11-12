package com.aircandi.ui;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase.DisplayType;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase.OnScaleChangeListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Window;
import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.applications.Places;
import com.aircandi.components.EntityManager;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequest.BitmapResponse;
import com.aircandi.components.bitmaps.ImageResult;
import com.aircandi.service.RequestListener;
import com.aircandi.service.ServiceResponse;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.ServiceBase;
import com.aircandi.service.objects.Shortcut;
import com.aircandi.service.objects.ShortcutSettings;
import com.aircandi.ui.base.BaseBrowse;
import com.aircandi.ui.base.FormDelegate;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.AirViewPager;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.UI;

public class PhotoForm extends BaseBrowse implements FormDelegate {

	final protected int		DEFAULT_ANIMATION_DURATION	= 200;

	private Photo			mPhoto;
	private List<Photo>		mPhotosForPaging			= new ArrayList<Photo>();
	private AirViewPager	mViewPager;
	private Boolean			mPagingEnabled				= true;
	private String			mForEntityId;
	private Entity			mForEntity;
	private String			mListLinkType;
	private String			mListLinkSchema;
	private ImageViewTouch	mImageViewTouch;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		/* This has to be called before setContentView */
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
	}

	@Override
	public void unpackIntent() {
		final Bundle extras = this.getIntent().getExtras();
		if (extras != null) {
			final String jsonPhoto = extras.getString(Constants.EXTRA_PHOTO);
			mPhoto = (Photo) Json.jsonToObject(jsonPhoto, Json.ObjectType.PHOTO);
			mForEntityId = extras.getString(Constants.EXTRA_ENTITY_PARENT_ID);
			mListLinkSchema = extras.getString(Constants.EXTRA_LIST_LINK_SCHEMA);
			mListLinkType = extras.getString(Constants.EXTRA_LIST_LINK_TYPE);
			mPagingEnabled = extras.getBoolean(Constants.EXTRA_PAGING_ENABLED, true);
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		setSupportProgressBarIndeterminateVisibility(true);
		if (mForEntityId == null) {
			mPagingEnabled = false;
		}
	}

	@Override
	protected void configureActionBar() {
		super.configureActionBar();

		if (mActionBar != null) {
			mActionBar.setSubtitle("double-tap to zoom");
		}
	}

	@Override
	public void databind(BindingMode mode) {
		if (!mPagingEnabled) {
			final ViewGroup layout = (ViewGroup) ((ViewStub) findViewById(R.id.stub_picture_detail)).inflate();
			buildPictureDetail(mPhoto, layout);
		}
		else {
			if (mForEntityId != null) {
				if (mListLinkType.equals(Constants.TYPE_LINK_WATCH)
						|| mListLinkType.equals(Constants.TYPE_LINK_CREATE)) {
					mForEntity = EntityManager.getEntity(mForEntityId);
					ShortcutSettings settings = new ShortcutSettings(mListLinkType, mListLinkSchema, Direction.out, null, false, false);
					settings.appClass = Places.class;
					List<Shortcut> shortcuts = (List<Shortcut>) mForEntity.getShortcuts(settings, new ServiceBase.SortByPositionSortDate(), null);
					for (Shortcut shortcut : shortcuts) {
						Photo photo = shortcut.getPhoto();
						photo.setCreatedAt(shortcut.sortDate.longValue());
						photo.setName(shortcut.name);
						//photo.setUser(shortcut.creator);
						mPhotosForPaging.add(photo);
					}
				}
				else {
					List<Entity> entities = (List<Entity>) EntityManager.getEntityCache().getEntitiesForEntity(mForEntityId, mListLinkSchema, null,
							null, null, null);
					/*
					 * We might get here just using a link without a downloaded entity. Treat it like paging
					 * is not enabled.
					 */
					if (entities == null || entities.size() == 0) {
						mPagingEnabled = false;
						databind(mode);
					}
					else {
						Collections.sort(entities, new Entity.SortByPositionSortDate());
						for (Entity entity : entities) {
							Photo photo = entity.getPhoto();
							photo.setCreatedAt(entity.modifiedDate.longValue());
							photo.setName(entity.name);
							photo.setUser(entity.creator);
							mPhotosForPaging.add(photo);
						}
					}
				}
				updateViewPager();
			}
		}
	}

	@Override
	public void draw() {}

	public ViewGroup buildPictureDetail(Photo photo, ViewGroup layout) {

		final AirImageView photoView = (AirImageView) layout.findViewById(R.id.photo);
		layout.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				UI.showToastNotification("touched", Toast.LENGTH_SHORT);
				return false;
			}
		});

		final TextView name = (TextView) layout.findViewById(R.id.name);
		final UserView user = (UserView) layout.findViewById(R.id.author);
		final ProgressBar progress = (ProgressBar) layout.findViewById(R.id.progressBar);
		final ViewGroup progressGroup = (ViewGroup) layout.findViewById(R.id.progress_group);
		final ImageViewTouch imageView = (ImageViewTouch) photoView.getImageView();
		imageView.setDisplayType(DisplayType.FIT_TO_SCREEN);
		imageView.setScrollEnabled(true);

		/* Title */
		UI.setVisibility(name, View.GONE);
		if (photo.getName() != null && !photo.getName().equals("")) {
			name.setText(photo.getName());
			UI.setVisibility(name, View.VISIBLE);
		}

		/* Author block */
		UI.setVisibility(user, View.GONE);
		if (photo.getUser() != null) {
			user.databind(photo.getUser(), photo.getCreatedAt().longValue(), false);
			UI.setVisibility(user, View.VISIBLE);
		}

		/* Image */
		final String photoUri = photo.getUri();
		final ViewHolder holder = new ViewHolder();
		holder.photoView = photoView;
		holder.photoView.setTag(photoUri);
		holder.photoView.getImageView().setImageBitmap(null);

		final BitmapRequest bitmapRequest = new BitmapRequest()
				.setBitmapUri(photoUri)
				.setBitmapRequestor(photoView)
				.setRequestListener(new RequestListener() {

					@Override
					public void onComplete(Object response) {
						final ServiceResponse serviceResponse = (ServiceResponse) response;

						if (serviceResponse.responseCode == ResponseCode.SUCCESS) {
							final BitmapResponse bitmapResponse = (BitmapResponse) serviceResponse.data;
							if (bitmapResponse.bitmap != null && bitmapResponse.photoUri.equals(photoUri)) {

								final BitmapDrawable bitmapDrawable = new BitmapDrawable(Aircandi.applicationContext.getResources(), bitmapResponse.bitmap);
								UI.showDrawableInImageView(bitmapDrawable, (ImageViewTouch) photoView.getImageView(), 1.0f, 4.0f, true, Animate.fadeInMedium());
							}
						}
						else {
							photoView.getImageView().setScaleType(ScaleType.CENTER);
							photoView.getImageView().setImageResource(R.drawable.img_broken);
						}
						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								progressGroup.setVisibility(View.GONE);
								setSupportProgressBarIndeterminateVisibility(false);
							}
						});
					}

					@Override
					public void onError(Object response) {
						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								progressGroup.setVisibility(View.GONE);
								setSupportProgressBarIndeterminateVisibility(false);
							}
						});
					}

					@Override
					public void onProgressChanged(final int progressPcnt) {
						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								progress.setProgress(progressPcnt);
								if (progressPcnt == 100) {
									progressGroup.setVisibility(View.GONE);
								}
							}
						});
					}
				});
		BitmapManager.getInstance().masterFetch(bitmapRequest);
		return layout;
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	public void onZoomIn() {
		mImageViewTouch.setDoubleTapDirection(1);
		float scale = mImageViewTouch.getScale();
		float targetScale = scale;
		targetScale = mImageViewTouch.onDoubleTapPost(scale, mImageViewTouch.getMaxScale());
		targetScale = Math.min(mImageViewTouch.getMaxScale(), Math.max(targetScale, mImageViewTouch.getMinScale()));
		mImageViewTouch.zoomTo(targetScale, DEFAULT_ANIMATION_DURATION);
	}

	public void onZoomOut() {
		mImageViewTouch.setDoubleTapDirection(-1);
		float scale = mImageViewTouch.getScale();
		float targetScale = scale;
		targetScale = mImageViewTouch.onDoubleTapPost(scale, mImageViewTouch.getMaxScale());
		targetScale = Math.min(mImageViewTouch.getMaxScale(), Math.max(targetScale, mImageViewTouch.getMinScale()));
		mImageViewTouch.zoomTo(targetScale, DEFAULT_ANIMATION_DURATION);

	}

	private void updateViewPager()
	{
		if (mViewPager == null) {

			mViewPager = (AirViewPager) findViewById(R.id.view_pager);
			mViewPager.setVisibility(View.VISIBLE);
			mViewPager.setOnTouchListener(new OnTouchListener() {

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					/*
					 * ViewPager ignores any gesture it doesn't see as a horizontal move or fling.
					 */
					int action = event.getAction();
					switch (action & MotionEvent.ACTION_MASK) {
						case MotionEvent.ACTION_UP:
							return false;
					}
					return false;
				}
			});

			mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {

				@Override
				public void onPageScrollStateChanged(int state) {
					super.onPageScrollStateChanged(state);
					if (state == ViewPager.SCROLL_STATE_IDLE) {
						Logger.v(this, "Page idle");
						bindImageViewTouch();
					}
				}

				@Override
				public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
					super.onPageScrolled(position, positionOffset, positionOffsetPixels);
				}

				@Override
				public void onPageSelected(int position) {
					Logger.v(this, "Page selected");
				}
			});

			mViewPager.setAdapter(new PhotoPagerAdapter());

			synchronized (mPhotosForPaging) {
				for (int i = 0; i < mPhotosForPaging.size(); i++) {
					if (mPhotosForPaging.get(i).getUri() != null) {
						if (mPhotosForPaging.get(i).getUri().equals(mPhoto.getUri())) {
							mViewPager.setCurrentItem(i, false);
							break;
						}
					}
				}
			}
		}
	}

	protected void bindImageViewTouch() {
		ViewGroup view = (ViewGroup) mViewPager.findViewWithTag("page" + mViewPager.getCurrentItem());
		if (view != null) {
			AirImageView image = (AirImageView) view.findViewById(R.id.photo);
			mImageViewTouch = (ImageViewTouch) image.getImageView();
			mImageViewTouch.setOnScaleChangeListener(new OnScaleChangeListener() {

				@Override
				public void onScaleChanged(float scale) {
					mViewPager.setSwipeable(scale <= 1.01f);
				}
			});
		}
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onResume() {
		super.onResume();
		if (!isFinishing()) {
			databind(BindingMode.AUTO);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		final AirImageView photoView = (AirImageView) findViewById(R.id.photo);
		final ImageViewTouch imageView = (ImageViewTouch) photoView.getImageView();
		imageView.setDisplayType(DisplayType.FIT_TO_SCREEN);
		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.picture_detail;
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------

	public class PhotoPagerAdapter extends PagerAdapter {

		@Override
		public int getCount() {
			return mPhotosForPaging.size();
		}

		@Override
		public Object instantiateItem(ViewGroup collection, int position) {
			final Photo photo = mPhotosForPaging.get(position);
			ViewGroup layout = (ViewGroup) LayoutInflater.from(PhotoForm.this).inflate(R.layout.temp_photo_detail, null);
			layout.setTag("page" + String.valueOf(position));
			buildPictureDetail(photo, layout);
			collection.addView(layout, 0);
			if (position == mViewPager.getCurrentItem()) {
				bindImageViewTouch();
			}

			return layout;
		}

		@Override
		public int getItemPosition(Object object) {
			/*
			 * Causes the view pager to recreate all the pages
			 * when notifyDataSetChanged is call on the adapter.
			 */
			return POSITION_NONE;
		}

		@Override
		public void destroyItem(ViewGroup collection, int position, Object view) {
			collection.removeView((ViewGroup) view);
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return (view == object);
		}

		@Override
		public void finishUpdate(View arg0) {}

		@Override
		public Parcelable saveState() {
			return null;
		}

		@Override
		public void restoreState(Parcelable state, ClassLoader loader) {}

	}

	public static class ViewHolder {

		public AirImageView	photoView;
		public ImageResult	data;
	}
}