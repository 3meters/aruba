package com.aircandi.ui.base;

import java.io.File;

import android.annotation.SuppressLint;
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
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi;
import com.aircandi.CandiConstants;
import com.aircandi.beta.BuildConfig;
import com.aircandi.beta.R;
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
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.RequestListener;
import com.aircandi.service.HttpService.ServiceDataType;
import com.aircandi.service.objects.Photo;
import com.aircandi.ui.SplashForm;
import com.aircandi.ui.builders.PicturePicker;
import com.aircandi.ui.widgets.WebImageView;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;

@SuppressWarnings("ucd")
public abstract class FormActivity extends SherlockActivity {

	protected String			mImageUriOriginal;
	protected AircandiCommon	mCommon;
	protected RequestListener	mImageRequestListener;
	protected WebImageView		mImageRequestWebImageView;
	protected Uri				mMediaFileUri;
	protected String			mMediaFilePath;
	protected File				mMediaFile;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (Aircandi.LAUNCHED_NORMALLY == null) {
			/*
			 * We restarting after a crash or after being killed by Android
			 */
			super.onCreate(savedInstanceState);
			Logger.d(this, "Aircandi not launched normally, routing to splash activity");
			Intent intent = new Intent(this, SplashForm.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			setResult(Activity.RESULT_CANCELED);
			startActivity(intent);
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
		setResult(Activity.RESULT_CANCELED);
		super.onBackPressed();
		AnimUtils.doOverridePendingTransition(this, TransitionType.FormToPage);
	}

	public void onCancelButtonClick(View view) {
		setResult(Activity.RESULT_CANCELED);
		finish();
		AnimUtils.doOverridePendingTransition(this, TransitionType.FormToPage);
	}

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		mCommon.doAttachedToWindow();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		mCommon.doConfigurationChanged(newConfig);
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

				Tracker.sendEvent("ui_action", "select_picture_device", null, 0, Aircandi.getInstance().getUser());
				final Uri imageUri = intent.getData();
				Bitmap bitmap = null;

				/* Bitmap size is trimmed if necessary to fit our max in memory image size. */
				bitmap = BitmapManager.getInstance().loadBitmapFromDeviceSampled(imageUri);
				if (bitmap != null && mImageRequestListener != null) {
					mImageRequestListener.onComplete(new ServiceResponse()
							, null
							, null
							, bitmap
							, null
							, null
							, false);
				}
			}
			else if (requestCode == CandiConstants.ACTIVITY_PICTURE_MAKE) {

				Tracker.sendEvent("ui_action", "create_picture_camera", null, 0, Aircandi.getInstance().getUser());
				final Bitmap bitmap = BitmapManager.getInstance().loadBitmapFromDeviceSampled(mMediaFileUri);
				sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, mMediaFileUri));
				if (mImageRequestListener != null) {
					mImageRequestListener.onComplete(new ServiceResponse()
							, null
							, null
							, bitmap
							, null
							, null
							, false);
				}
			}
			else if (requestCode == CandiConstants.ACTIVITY_PICTURE_SEARCH) {

				Tracker.sendEvent("ui_action", "select_picture_search", null, 0, Aircandi.getInstance().getUser());
				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final String imageTitle = extras.getString(CandiConstants.EXTRA_URI_TITLE);
					final String imageDescription = extras.getString(CandiConstants.EXTRA_URI_DESCRIPTION);
					final String jsonPhoto = extras.getString(CandiConstants.EXTRA_PHOTO);
					final Photo photo = (Photo) HttpService.convertJsonToObjectInternalSmart(jsonPhoto, ServiceDataType.Photo);

					final BitmapRequestBuilder builder = new BitmapRequestBuilder(mImageRequestWebImageView)
							.setFromUri(photo.getUri())
							.setRequestListener(new RequestListener() {

								@Override
								public void onComplete(Object response) {

									final ServiceResponse serviceResponse = (ServiceResponse) response;
									if (serviceResponse.responseCode == ResponseCode.Success) {
										runOnUiThread(new Runnable() {

											@Override
											public void run() {
												if (mImageRequestListener != null) {
													final ImageResponse imageResponse = (ImageResponse) serviceResponse.data;
													/*
													 * We cache search pictures to aircandi storage because it give us
													 * a chance to pre-process them to be a more standardized blob size
													 * and dimensions. We also can serve them up with more predictable
													 * performance.
													 */
													mImageRequestListener.onComplete(serviceResponse
															, photo
															, null
															, imageResponse.bitmap
															, imageTitle
															, imageDescription
															, false);
												}
											}
										});
									}
								}
							});

					final BitmapRequest imageRequest = builder.create();
					mImageRequestWebImageView.setBitmapRequest(imageRequest, false);

					if (imageTitle != null && !imageTitle.equals("")) {
						final EditText title = (EditText) findViewById(R.id.text_title);
						if (title != null && title.getText().toString().equals("")) {
							title.setText(imageTitle);
						}
					}

					if (imageDescription != null && !imageDescription.equals("")) {
						final EditText description = (EditText) findViewById(R.id.description);
						if (description != null && description.getText().toString().equals("")) {
							description.setText(imageDescription);
						}
					}
				}
			}
			else if (requestCode == CandiConstants.ACTIVITY_PICTURE_PICK_PLACE) {

				Tracker.sendEvent("ui_action", "select_picture_place", null, 0, Aircandi.getInstance().getUser());
				if (intent != null && intent.getExtras() != null) {

					final Bundle extras = intent.getExtras();
					final String jsonPhoto = extras.getString(CandiConstants.EXTRA_PHOTO);
					final Photo photo = (Photo) HttpService.convertJsonToObjectInternalSmart(jsonPhoto, ServiceDataType.Photo);

					final BitmapRequestBuilder builder = new BitmapRequestBuilder(mImageRequestWebImageView)
							.setFromUri(photo.getUri())
							.setRequestListener(new RequestListener() {

								@Override
								public void onComplete(Object response) {

									final ServiceResponse serviceResponse = (ServiceResponse) response;
									if (serviceResponse.responseCode == ResponseCode.Success) {
										runOnUiThread(new Runnable() {

											@Override
											public void run() {
												if (mImageRequestListener != null) {
													final ImageResponse imageResponse = (ImageResponse) serviceResponse.data;
													/*
													 * We don't cache place pictures from foursquare because they
													 * wouldn't like that. Plus they serve them up in a more consistent
													 * way than
													 * something like search pictures (which we do cache).
													 */
													mImageRequestListener.onComplete(serviceResponse
															, photo
															, imageResponse.imageUri
															, imageResponse.bitmap
															, null
															, null
															, true);
												}
											}
										});
									}
								}
							});

					final BitmapRequest imageRequest = builder.create();
					mImageRequestWebImageView.setBitmapRequest(imageRequest, false);
				}
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Picker routines
	// --------------------------------------------------------------------------------------------

	@SuppressLint("InlinedApi")
	protected void pictureFromGallery() {
		//		Intent picturePickerIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
		//		picturePickerIntent.setType("image/*");
		//		startActivityForResult(picturePickerIntent, CandiConstants.ACTIVITY_PICTURE_PICK_DEVICE);

		final Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		/*
		 * We want to filter out remove images like the linked in from picasa.
		 */
		if (CandiConstants.SUPPORTS_HONEYCOMB) {
			intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
		}
		startActivityForResult(Intent.createChooser(intent, getString(R.string.chooser_gallery_title))
				, CandiConstants.ACTIVITY_PICTURE_PICK_DEVICE);
		AnimUtils.doOverridePendingTransition(this, TransitionType.PageToSource);
	}

	protected void pictureFromCamera() {
		final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		mMediaFile = AndroidManager.getOutputMediaFile(AndroidManager.MEDIA_TYPE_IMAGE);
		if (mMediaFile != null) {
			mMediaFilePath = mMediaFile.getAbsolutePath();
			mMediaFileUri = Uri.fromFile(mMediaFile);
			intent.putExtra(MediaStore.EXTRA_OUTPUT, mMediaFileUri);
			startActivityForResult(intent, CandiConstants.ACTIVITY_PICTURE_MAKE);
			AnimUtils.doOverridePendingTransition(this, TransitionType.PageToSource);
		}
	}

	protected void pictureSearch(String defaultSearch) {
		final Intent intent = new Intent(this, PicturePicker.class);
		intent.putExtra(CandiConstants.EXTRA_SEARCH_PHRASE, defaultSearch);
		startActivityForResult(intent, CandiConstants.ACTIVITY_PICTURE_SEARCH);
		AnimUtils.doOverridePendingTransition(this, TransitionType.PageToForm);
	}

	protected void pictureFromPlace(String entityId) {
		final IntentBuilder intentBuilder = new IntentBuilder(this, PicturePicker.class)
				.setCommandType(CommandType.View)
				.setEntityId(entityId);
		final Intent intent = intentBuilder.create();
		startActivityForResult(intent, CandiConstants.ACTIVITY_PICTURE_PICK_PLACE);
		AnimUtils.doOverridePendingTransition(this, TransitionType.PageToForm);
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
	// Application menu routines (settings)
	// --------------------------------------------------------------------------------------------

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		mCommon.doCreateOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		mCommon.doPrepareOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		mCommon.doOptionsItemSelected(item);
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onResume() {
		super.onResume();
		mCommon.doResume();

		if (!Aircandi.getInstance().getPrefTheme().equals(Aircandi.settings.getString(CandiConstants.PREF_THEME, CandiConstants.PREF_THEME_DEFAULT))) {
			Logger.d(this, "Pref change: theme, restarting current activity");
			Aircandi.getInstance().snapshotPreferences();
			mCommon.reload();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mCommon != null) {
			mCommon.doPause();
		}
	}

	@Override
	protected void onDestroy() {
		/* This activity gets destroyed everytime we leave using back or finish(). */
		Logger.d(this, "onDestroy called");
		try {
			if (mCommon != null) {
				mCommon.doDestroy();
			}
		}
		catch (Exception e) {
			if (BuildConfig.DEBUG) {
				e.printStackTrace();
			}
		}
		finally {
			super.onDestroy();
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (mCommon != null) {
			mCommon.doStop();
		}
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

	public static class SimpleTextWatcher implements TextWatcher {

		@Override
		public void afterTextChanged(Editable s) {}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {}
	}
}