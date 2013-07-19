package com.aircandi.ui;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Window;
import com.aircandi.Constants;
import com.aircandi.ProxiConstants;
import com.aircandi.beta.R;
import com.aircandi.components.EntityManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.PhotoPagerAdapter;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.components.bitmaps.BitmapManager.ViewHolder;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.service.HttpService.RequestListener;
import com.aircandi.service.objects.Photo;
import com.aircandi.ui.base.BaseBrowse;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.utilities.UI;

public class PhotoForm extends BaseBrowse {

	private List<Photo>	mPhotosForPaging	= new ArrayList<Photo>();
	private String		mImageUri;
	private ViewPager	mViewPager;
	private Boolean		mPagingEnabled		= false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		/* This has to be called before setContentView */
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		final Bundle extras = this.getIntent().getExtras();
		if (extras != null) {
			mImageUri = extras.getString(Constants.EXTRA_URI);
			mPagingEnabled = extras.getBoolean(Constants.EXTRA_PAGING_ENABLED, false);
		}

		setSupportProgressBarIndeterminateVisibility(true);
		mActionBar.setSubtitle("double-tap to zoom");
		mActionBar.setDisplayHomeAsUpEnabled(true);
	}

	@Override
	protected void databind(Boolean refresh) {
		List<Photo> photos = EntityManager.getInstance().getPhotos();
		final Photo photo = EntityManager.getInstance().getPhoto(mImageUri);
		if (!mPagingEnabled) {
			photos = new ArrayList<Photo>();
			photos.add(photo);
			final View layout = ((ViewStub) findViewById(R.id.stub_picture_detail)).inflate();
			buildPictureDetail(this, photo, layout);
		}
		else {
			updateViewPager(photos);
		}
	}

	public static View buildPictureDetail(final Context context, Photo photo, View layout) {

		final SherlockActivity activity = (SherlockActivity) context;
		final TextView name = (TextView) layout.findViewById(R.id.name);
		final UserView user = (UserView) layout.findViewById(R.id.author);
		final ProgressBar progress = (ProgressBar) layout.findViewById(R.id.progressBar);
		final ViewGroup progressGroup = (ViewGroup) layout.findViewById(R.id.progress_group);
		final ImageView image = (ImageView) layout.findViewById(R.id.photo);
		((ImageViewTouch) image).setFitToScreen(true);
		((ImageViewTouch) image).setScrollEnabled(true);

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
		String photoUri = photo.getUri();
		if (!photoUri.startsWith("http:") && !photoUri.startsWith("https:") && !photoUri.startsWith("resource:")) {
			photoUri = ProxiConstants.URL_PROXIBASE_MEDIA_IMAGES + photoUri;
		}
		final ViewHolder holder = new ViewHolder();
		holder.itemImage = image;
		holder.itemImage.setTag(photoUri);
		holder.itemImage.setImageBitmap(null);

		final BitmapRequest bitmapRequest = new BitmapRequest(photoUri, image);
		bitmapRequest.setImageRequestor(image);
		bitmapRequest.setImageUri(photoUri);
		bitmapRequest.setImageView(image);
		bitmapRequest.setRequestListener(new RequestListener() {

			@Override
			public void onComplete(Object response) {
				final ServiceResponse serviceResponse = (ServiceResponse) response;
				activity.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						if (serviceResponse.responseCode != ResponseCode.Success) {
							image.setScaleType(ScaleType.CENTER);
							image.setImageResource(R.drawable.img_broken);
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
		BitmapManager.getInstance().masterFetch(bitmapRequest);
		return layout;
	}

	private void updateViewPager(List<Photo> photos)
	{
		if (mViewPager == null) {

			mViewPager = (ViewPager) findViewById(R.id.view_pager);
			mViewPager.setVisibility(View.VISIBLE);
			mPhotosForPaging = photos;

			mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {

				@Override
				public void onPageSelected(int position) {}
			});

			mViewPager.setAdapter(new PhotoPagerAdapter(this, mViewPager, mPhotosForPaging));

			synchronized (mPhotosForPaging) {
				for (int i = 0; i < mPhotosForPaging.size(); i++) {
					if (mPhotosForPaging.get(i).getUri() != null) {
						if (mPhotosForPaging.get(i).getUri().equals(mImageUri)) {
							mViewPager.setCurrentItem(i, false);
							break;
						}
					}
					else {
						if (mPhotosForPaging.get(i).getUri().equals(mImageUri)) {
							mViewPager.setCurrentItem(i, false);
							break;
						}
					}
				}
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		final ImageView image = (ImageView) findViewById(R.id.photo);
		((ImageViewTouch) image).setFitToScreen(true);
		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.picture_detail;
	}

}