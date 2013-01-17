package com.aircandi.ui.base;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import com.actionbarsherlock.app.SherlockActivity;
import com.aircandi.Aircandi;
import com.aircandi.CandiConstants;
import com.aircandi.R;
import com.aircandi.components.AircandiCommon;
import com.aircandi.components.AndroidManager;
import com.aircandi.components.CommandType;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.Tracker;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequest.ImageResponse;
import com.aircandi.components.bitmaps.BitmapRequestBuilder;
import com.aircandi.service.ProxibaseService.RequestListener;
import com.aircandi.service.objects.User;
import com.aircandi.ui.Preferences;
import com.aircandi.ui.builders.PicturePicker;
import com.aircandi.ui.widgets.WebImageView;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;

public abstract class FormActivity extends SherlockActivity {

	protected Boolean			mBeaconUnregistered;
	protected String			mImageUriOriginal;
	protected AircandiCommon	mCommon;
	protected RequestListener	mImageRequestListener;
	protected WebImageView		mImageRequestWebImageView;
	protected Uri				mMediaFileUri;
	protected String			mMediaFilePath;
	protected String			mImageName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (!Aircandi.getInstance().wasLaunchedNormally()) {
			/* Try to detect case where this is being created after a crash and bail out. */
			super.onCreate(savedInstanceState);
			setResult(Activity.RESULT_CANCELED);
			finish();
		}
		else {
			/*
			 * Theme has to be set before any UI is constructed. We also have to do it for each activity so they pickup
			 * our custom style attributes.
			 */
			mCommon = new AircandiCommon(this, savedInstanceState);
			mCommon.unpackIntent();
			mCommon.setTheme(null, isDialog());
			super.onCreate(savedInstanceState);
			super.setContentView(this.getLayoutID());
			mCommon.initialize();
		}
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		AnimUtils.doOverridePendingTransition(this, TransitionType.FormToCandiPage);
	}

	public void onCancelButtonClick(View view) {
		finish();
		AnimUtils.doOverridePendingTransition(this, TransitionType.FormToCandiPage);
	}

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		mCommon.doAttachedToWindow();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Logger.d(this, "Configuration changed");
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		mCommon.doSaveInstanceState(savedInstanceState);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		/*
		 * Called before onResume. If we are returning from the market app, we get a zero result code whether the user
		 * decided to start an install or not.
		 */
		if (resultCode == Activity.RESULT_OK) {
			if (requestCode == CandiConstants.ACTIVITY_PICTURE_PICK_DEVICE) {

				Tracker.trackEvent("Entity", "PickPicture", "None", 0);
				Uri imageUri = intent.getData();
				Bitmap bitmap = null;

				/* Bitmap size is trimmed if necessary to fit our max in memory image size. */
				bitmap = BitmapManager.getInstance().loadBitmapFromDeviceSampled(imageUri);
				if (bitmap != null && mImageRequestListener != null) {
					mImageRequestListener.onComplete(new ServiceResponse(), null, bitmap, null, null);
				}
			}
			else if (requestCode == CandiConstants.ACTIVITY_PICTURE_MAKE) {

				Tracker.trackEvent("Entity", "TakePicture", "None", 0);
				Bitmap bitmap = BitmapManager.getInstance().loadBitmapFromDeviceSampled(mMediaFileUri);
				if (mImageRequestListener != null) {
					mImageRequestListener.onComplete(new ServiceResponse(), null, bitmap, null, null);
				}
			}
			else if (requestCode == CandiConstants.ACTIVITY_PICTURE_SEARCH) {

				Tracker.trackEvent("Entity", "PictureSearch", "None", 0);

				if (intent != null && intent.getExtras() != null) {
					Bundle extras = intent.getExtras();
					final String imageUri = extras.getString(CandiConstants.EXTRA_URI);
					final String imageTitle = extras.getString(CandiConstants.EXTRA_URI_TITLE);
					final String imageDescription = extras.getString(CandiConstants.EXTRA_URI_DESCRIPTION);

					BitmapRequestBuilder builder = new BitmapRequestBuilder(mImageRequestWebImageView)
							.setFromUri(imageUri)
							.setRequestListener(new RequestListener() {

								@Override
								public void onComplete(Object response) {

									final ServiceResponse serviceResponse = (ServiceResponse) response;
									if (serviceResponse.responseCode == ResponseCode.Success) {
										runOnUiThread(new Runnable() {

											@Override
											public void run() {
												if (mImageRequestListener != null) {
													ImageResponse imageResponse = (ImageResponse) serviceResponse.data;
													mImageRequestListener.onComplete(serviceResponse
															, imageResponse.imageUri
															, imageResponse.bitmap
															, imageTitle
															, imageDescription);
												}
											}
										});
									}
								}
							});

					BitmapRequest imageRequest = builder.create();
					mImageRequestWebImageView.setBitmapRequest(imageRequest, false);
					
					if (imageTitle != null && !imageTitle.equals("")) {
						EditText title = (EditText) findViewById(R.id.text_title);
						if (title != null && title.getText().toString().equals("")) {
							title.setText(imageTitle);
						}
					}

					if (imageDescription != null && !imageDescription.equals("")) {
						EditText description = (EditText) findViewById(R.id.description);
						if (description != null && description.getText().toString().equals("")) {
							description.setText(imageDescription);
						}
					}
				}
			}
			else if (requestCode == CandiConstants.ACTIVITY_PICTURE_PICK_PLACE) {

				Tracker.trackEvent("Entity", "PicturePlace", "None", 0);

				if (intent != null && intent.getExtras() != null) {
					Bundle extras = intent.getExtras();
					final String imageUri = extras.getString(CandiConstants.EXTRA_URI);

					BitmapRequestBuilder builder = new BitmapRequestBuilder(mImageRequestWebImageView)
							.setFromUri(imageUri)
							.setRequestListener(new RequestListener() {

								@Override
								public void onComplete(Object response) {

									final ServiceResponse serviceResponse = (ServiceResponse) response;
									if (serviceResponse.responseCode == ResponseCode.Success) {
										runOnUiThread(new Runnable() {

											@Override
											public void run() {
												if (mImageRequestListener != null) {
													ImageResponse imageResponse = (ImageResponse) serviceResponse.data;
													mImageRequestListener.onComplete(serviceResponse
															, imageResponse.imageUri
															, imageResponse.bitmap
															, null
															, null);
												}
											}
										});
									}
								}
							});

					BitmapRequest imageRequest = builder.create();
					mImageRequestWebImageView.setBitmapRequest(imageRequest, false);
				}
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Picker routines
	// --------------------------------------------------------------------------------------------

	protected void pictureFromGallery() {
		Intent picturePickerIntent = new Intent(Intent.ACTION_PICK);
		picturePickerIntent.setType("image/*");
		startActivityForResult(picturePickerIntent, CandiConstants.ACTIVITY_PICTURE_PICK_DEVICE);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
	}

	protected void pictureFromCamera() {
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		
		File mediaFile = AndroidManager.getOutputMediaFile(AndroidManager.MEDIA_TYPE_IMAGE);
		mMediaFilePath = mediaFile.getAbsolutePath();
		mMediaFileUri = Uri.fromFile(mediaFile);
		
		intent.putExtra(MediaStore.EXTRA_OUTPUT, mMediaFileUri);		
		startActivityForResult(intent, CandiConstants.ACTIVITY_PICTURE_MAKE);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
	}

	protected void pictureSearch() {
		Intent intent = new Intent(this, PicturePicker.class);
		//intent.putExtra(CandiConstants.EXTRA_SEARCH_PHRASE, defaultSearch);
		startActivityForResult(intent, CandiConstants.ACTIVITY_PICTURE_SEARCH);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
	}

	protected void pictureFromPlace(String entityId) {
		IntentBuilder intentBuilder = new IntentBuilder(this, PicturePicker.class);
		intentBuilder.setCommandType(CommandType.View)
				.setEntityId(entityId);
		Intent intent = intentBuilder.create();
		startActivityForResult(intent, CandiConstants.ACTIVITY_PICTURE_PICK_PLACE);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
	}

	protected void useFacebook() {
		/*
		 * Only used for user pictures
		 */
		final User user = Aircandi.getInstance().getUser();
		user.getPhoto().setImageUri("https://graph.facebook.com/" + user.facebookId + "/picture?type=large");
		user.getPhoto().setSourceName("external");

		BitmapRequestBuilder builder = new BitmapRequestBuilder(mImageRequestWebImageView);
		builder.setFromUri(user.getPhoto().getUri());
		builder.setRequestListener(new RequestListener() {

			@Override
			public void onComplete(Object response) {

				/* Used to pass back the bitmap and imageUri (sometimes) for the entity */
				if (mImageRequestListener != null) {
					mImageRequestListener.onComplete(new ServiceResponse(), user.getUserPhotoUri(), null, null, null);
				}
			}
		});

		BitmapRequest imageRequest = builder.create();
		mImageRequestWebImageView.setBitmapRequest(imageRequest);
	}

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	protected Boolean isDialog() {
		return false;
	}

	protected static void setVisibility(View view, Integer visibility) {
		if (view != null) {
			view.setVisibility(visibility);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onResume() {
		super.onResume();
		mCommon.doResume();
		if (!mCommon.mPrefTheme.equals(Aircandi.settings.getString(Preferences.PREF_THEME, CandiConstants.THEME_DEFAULT))) {
			mCommon.mPrefTheme = Aircandi.settings.getString(Preferences.PREF_THEME, CandiConstants.THEME_DEFAULT);
			mCommon.reload();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		mCommon.doPause();
	}

	protected void onDestroy() {
		/* This activity gets destroyed everytime we leave using back or finish(). */
		Logger.d(this, "onDestroy called");
		try {
			if (mCommon != null) {
				mCommon.doDestroy();
			}
		}
		catch (Exception exception) {
			exception.printStackTrace();
		}
		finally {
			super.onDestroy();
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		mCommon.doStop();
	}

	@Override
	protected void onStart() {
		super.onStart();
		mCommon.doStart();
	}

	protected int getLayoutID() {
		return 0;
	}

	// --------------------------------------------------------------------------------------------
	// Inner classes and enums
	// --------------------------------------------------------------------------------------------

	public class SimpleTextWatcher implements TextWatcher {

		@Override
		public void afterTextChanged(Editable s) {}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {}
	}
}