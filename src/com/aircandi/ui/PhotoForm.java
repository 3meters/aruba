package com.aircandi.ui;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase.DisplayType;
import android.content.res.Configuration;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Window;
import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequest.BitmapResponse;
import com.aircandi.components.bitmaps.ImageResult;
import com.aircandi.service.RequestListener;
import com.aircandi.service.ServiceResponse;
import com.aircandi.service.objects.Photo;
import com.aircandi.ui.base.BaseBrowse;
import com.aircandi.ui.base.IForm;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.UI;

public class PhotoForm extends BaseBrowse implements IForm {

	private Photo			mPhoto;
	private AirImageView	mPhotoView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		/* This has to be called before setContentView */
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);
	}

	@Override
	public void unpackIntent() {
		final Bundle extras = this.getIntent().getExtras();
		if (extras != null) {
			final String jsonPhoto = extras.getString(Constants.EXTRA_PHOTO);
			mPhoto = (Photo) Json.jsonToObject(jsonPhoto, Json.ObjectType.PHOTO);
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		setSupportProgressBarIndeterminateVisibility(true);
	}

	@Override
	protected void configureActionBar() {
		super.configureActionBar();

		if (mActionBar != null) {
			mActionBar.setSubtitle("double-tap to zoom");
		}
	}

	@Override
	public void databind(BindingMode mode) {}

	@Override
	public void draw() {

		mPhotoView = (AirImageView) findViewById(R.id.photo);
		ViewGroup form = (ViewGroup) findViewById(R.id.form_holder);
		form.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				UI.showToastNotification("touched", Toast.LENGTH_SHORT);
				return false;
			}
		});

		final TextView name = (TextView) findViewById(R.id.name);
		final UserView user = (UserView) findViewById(R.id.author);
		final ProgressBar progress = (ProgressBar) findViewById(R.id.progressBar);
		final ViewGroup progressGroup = (ViewGroup) findViewById(R.id.progress_group);
		final ImageViewTouch imageView = (ImageViewTouch) mPhotoView.getImageView();
		imageView.setDisplayType(DisplayType.FIT_TO_SCREEN);
		imageView.setScrollEnabled(true);

		/* Title */
		UI.setVisibility(name, View.GONE);
		if (mPhoto.getName() != null && !mPhoto.getName().equals("")) {
			name.setText(mPhoto.getName());
			UI.setVisibility(name, View.VISIBLE);
		}

		/* Author block */
		UI.setVisibility(user, View.GONE);
		if (mPhoto.getUser() != null) {
			user.databind(mPhoto.getUser(), mPhoto.getCreatedAt().longValue(), false);
			UI.setVisibility(user, View.VISIBLE);
		}

		/* Image */
		final String photoUri = mPhoto.getUri();
		final ViewHolder holder = new ViewHolder();
		holder.photoView = mPhotoView;
		holder.photoView.setTag(photoUri);
		holder.photoView.getImageView().setImageBitmap(null);

		final BitmapRequest bitmapRequest = new BitmapRequest()
				.setBitmapUri(photoUri)
				.setBitmapRequestor(mPhotoView)
				.setRequestListener(new RequestListener() {

					@Override
					public void onComplete(Object response) {
						final ServiceResponse serviceResponse = (ServiceResponse) response;

						if (serviceResponse.responseCode == ResponseCode.SUCCESS) {
							final BitmapResponse bitmapResponse = (BitmapResponse) serviceResponse.data;
							if (bitmapResponse.bitmap != null && bitmapResponse.photoUri.equals(photoUri)) {

								final BitmapDrawable bitmapDrawable = new BitmapDrawable(Aircandi.applicationContext.getResources(), bitmapResponse.bitmap);
								UI.showDrawableInImageView(bitmapDrawable, (ImageViewTouch) mPhotoView.getImageView(), 1.0f, 4.0f, true, Animate.fadeInMedium());
							}
						}
						else {
							mPhotoView.getImageView().setScaleType(ScaleType.CENTER);
							mPhotoView.getImageView().setImageResource(R.drawable.img_broken);
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
	}

	@Override
	protected void onResume() {
		super.onResume();
		draw();
	}

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		final ImageView image = (ImageView) findViewById(R.id.photo);
		((ImageViewTouch) image).setDisplayType(DisplayType.FIT_TO_SCREEN);
		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.picture_detail;
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------

	public static class ViewHolder {

		public AirImageView	photoView;
		public ImageResult	data;
	}
}