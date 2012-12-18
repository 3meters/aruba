package com.aircandi;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Window;
import com.aircandi.components.DrawableManager.ViewHolder;
import com.aircandi.components.Exceptions;
import com.aircandi.components.FontManager;
import com.aircandi.components.ImageManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.PhotoPagerAdapter;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.service.ProxiConstants;
import com.aircandi.service.ProxibaseService.RequestListener;
import com.aircandi.service.objects.Photo;
import com.aircandi.widgets.UserView;

public class PictureDetail extends FormActivity {

	protected List<Photo>	mPhotosForPaging	= new ArrayList<Photo>();
	private String			mImageUri;
	protected ViewPager		mViewPager;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		/* This has to be called before setContentView */
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);

		initialize();
		bind(true);
	}

	private void initialize() {
		Bundle extras = this.getIntent().getExtras();
		if (extras != null) {
			mImageUri = extras.getString(CandiConstants.EXTRA_URI);
		}

		setSupportProgressBarIndeterminateVisibility(true);
		mCommon.mActionBar.setSubtitle("double-tap to zoom");

	}

	private void bind(Boolean pagingEnabled) {
		List<Photo> photos = ProxiExplorer.getInstance().getEntityModel().getPhotos();
		Photo photo = ProxiExplorer.getInstance().getEntityModel().getPhoto(mImageUri);
		if (!pagingEnabled) {
			photos = new ArrayList<Photo>();
			photos.add(photo);
		}
		updateViewPager(photos);
	}

	public static ViewGroup buildPictureDetail(final Context context, Photo photo, ViewGroup layout) {

		final SherlockActivity activity = (SherlockActivity) context;
		final TextView title = (TextView) layout.findViewById(R.id.text_title);
		final UserView user = (UserView) layout.findViewById(R.id.author);
		final ProgressBar progress = (ProgressBar) layout.findViewById(R.id.progressBar);
		final ViewGroup progressGroup = (ViewGroup) layout.findViewById(R.id.progress_group);
		final ImageView image = (ImageView) layout.findViewById(R.id.image);
		((ImageViewTouch) image).setFitToScreen(true);
		((ImageViewTouch) image).setScrollEnabled(false);
		
		FontManager.getInstance().setTypefaceDefault(title);

		/* Title */
		setVisibility(title, View.GONE);
		if (photo.getTitle() != null && !photo.getTitle().equals("")) {
			title.setText(photo.getTitle());
			setVisibility(title, View.VISIBLE);
		}

		/* Author block */
		setVisibility(user, View.GONE);
		if (photo.getUser() != null) {
			user.bindToAuthor(photo.getUser(), photo.getCreatedAt().longValue(), false);
			setVisibility(user, View.VISIBLE);
		}

		/* Image */
		String imageUri = photo.getImageUri();
		if (!imageUri.startsWith("http:") && !imageUri.startsWith("https:") && !imageUri.startsWith("resource:")) {
			imageUri = ProxiConstants.URL_PROXIBASE_MEDIA_IMAGES + imageUri;
		}
		ViewHolder holder = new ViewHolder();
		holder.itemImage = image;
		holder.itemImage.setTag(imageUri);
		holder.itemImage.setImageBitmap(null);
		ImageManager.getInstance().getDrawableManager().fetchDrawableOnThread(imageUri, holder, new RequestListener() {

			@Override
			public void onComplete(Object response) {
				final ServiceResponse serviceResponse = (ServiceResponse) response;
				activity.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						if (serviceResponse.responseCode != ResponseCode.Success) {
							image.setScaleType(ScaleType.CENTER);
							image.setImageResource(R.drawable.image_broken);
						}
						progressGroup.setVisibility(View.GONE);
						activity.setSupportProgressBarIndeterminateVisibility(false);
					}
				});
			}

			@Override
			public void onProgressChanged(final int progressPcnt) {
				activity.runOnUiThread(new Runnable() {

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

		return layout;
	}

	protected void updateViewPager(List<Photo> photos)
	{
		if (mViewPager == null) {

			mViewPager = (ViewPager) findViewById(R.id.view_pager);
			mPhotosForPaging = photos;

			mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {

				@Override
				public void onPageSelected(int position) {}
			});

			mViewPager.setAdapter(new PhotoPagerAdapter(this, mViewPager, mPhotosForPaging));

			synchronized (mPhotosForPaging) {
				for (int i = 0; i < mPhotosForPaging.size(); i++) {
					if (mPhotosForPaging.get(i).getImageUri() != null) {
						if (mPhotosForPaging.get(i).getImageUri().equals(mImageUri)) {
							mViewPager.setCurrentItem(i, false);
							break;
						}
					}
					else {
						if (mPhotosForPaging.get(i).getImageUri().equals(mImageUri)) {
							mViewPager.setCurrentItem(i, false);
							break;
						}
					}
				}
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		final ImageView image = (ImageView) findViewById(R.id.image);
		((ImageViewTouch) image).setFitToScreen(true);
		super.onConfigurationChanged(newConfig);
	}

	protected void onDestroy() {
		/*
		 * This activity gets destroyed everytime we leave using back or
		 * finish().
		 */
		try {
			mCommon.doDestroy();
		}
		catch (Exception exception) {
			Exceptions.Handle(exception);
		}
		finally {
			super.onDestroy();
		}
	}

	@Override
	protected int getLayoutID() {
		return R.layout.picture_detail;
	}
}