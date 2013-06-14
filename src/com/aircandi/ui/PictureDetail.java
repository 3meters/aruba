package com.aircandi.ui;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
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
import com.aircandi.components.Exceptions;
import com.aircandi.components.FontManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.PhotoPagerAdapter;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.components.bitmaps.BitmapManager.ViewHolder;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.service.HttpService.RequestListener;
import com.aircandi.service.objects.Photo;
import com.aircandi.ui.base.FormActivity;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;

public class PictureDetail extends FormActivity {

	private List<Photo>	mPhotosForPaging	= new ArrayList<Photo>();
	private String		mImageUri;
	private ViewPager	mViewPager;
	private Boolean		mPagingEnabled		= false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		/* This has to be called before setContentView */
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);

		if (!isFinishing()) {
			initialize();
			bind();
		}
	}

	private void initialize() {
		final Bundle extras = this.getIntent().getExtras();
		if (extras != null) {
			mImageUri = extras.getString(Constants.EXTRA_URI);
			mPagingEnabled = extras.getBoolean(Constants.EXTRA_PAGING_ENABLED, false);
		}

		setSupportProgressBarIndeterminateVisibility(true);
		mCommon.mActionBar.setSubtitle("double-tap to zoom");
		mCommon.mActionBar.setDisplayHomeAsUpEnabled(true);
	}

	private void bind() {
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

	@Override
	public void onBackPressed() {
		setResult(Activity.RESULT_CANCELED);
		super.onBackPressed();
		AnimUtils.doOverridePendingTransition(this, TransitionType.PageBack);
	}

	@Override
	public void onCancelButtonClick(View view) {
		setResult(Activity.RESULT_CANCELED);
		finish();
		AnimUtils.doOverridePendingTransition(this, TransitionType.PageBack);
	}

	public static View buildPictureDetail(final Context context, Photo photo, View layout) {

		final SherlockActivity activity = (SherlockActivity) context;
		final TextView title = (TextView) layout.findViewById(R.id.text_title);
		final UserView user = (UserView) layout.findViewById(R.id.author);
		final ProgressBar progress = (ProgressBar) layout.findViewById(R.id.progressBar);
		final ViewGroup progressGroup = (ViewGroup) layout.findViewById(R.id.progress_group);
		final ImageView image = (ImageView) layout.findViewById(R.id.image);
		((ImageViewTouch) image).setFitToScreen(true);
		((ImageViewTouch) image).setScrollEnabled(true);

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
			user.bindToUser(photo.getUser(), photo.getCreatedAt().longValue(), false);
			setVisibility(user, View.VISIBLE);
		}

		/* Image */
		String imageUri = photo.getUri();
		if (!imageUri.startsWith("http:") && !imageUri.startsWith("https:") && !imageUri.startsWith("resource:")) {
			imageUri = ProxiConstants.URL_PROXIBASE_MEDIA_IMAGES + imageUri;
		}
		final ViewHolder holder = new ViewHolder();
		holder.itemImage = image;
		holder.itemImage.setTag(imageUri);
		holder.itemImage.setImageBitmap(null);

		final BitmapRequest bitmapRequest = new BitmapRequest(imageUri, image);
		bitmapRequest.setImageRequestor(image);
		bitmapRequest.setImageUri(imageUri);
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
	// Misc routines
	// --------------------------------------------------------------------------------------------
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		final ImageView image = (ImageView) findViewById(R.id.image);
		((ImageViewTouch) image).setFitToScreen(true);
		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onDestroy() {
		/*
		 * This activity gets destroyed everytime we leave using back or
		 * finish().
		 */
		try {
			mCommon.doDestroy();
		}
		catch (Exception exception) {
			Exceptions.handle(exception);
		}
		finally {
			super.onDestroy();
		}
	}

	@Override
	protected int getLayoutId() {
		return R.layout.picture_detail;
	}
}