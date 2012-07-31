package com.proxibase.aircandi;

import java.io.FileNotFoundException;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.proxibase.aircandi.components.AircandiCommon;
import com.proxibase.aircandi.components.AnimUtils;
import com.proxibase.aircandi.components.ImageManager;
import com.proxibase.aircandi.components.ImageRequest;
import com.proxibase.aircandi.components.AnimUtils.TransitionType;
import com.proxibase.aircandi.components.ImageRequest.ImageResponse;
import com.proxibase.aircandi.components.ImageRequest.ImageShape;
import com.proxibase.aircandi.components.ImageRequestBuilder;
import com.proxibase.aircandi.components.ImageUtils;
import com.proxibase.aircandi.components.Logger;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.components.Tracker;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.widgets.WebImageView;
import com.proxibase.service.ProxibaseService.RequestListener;
import com.proxibase.service.objects.User;

public abstract class FormActivity extends SherlockActivity {

	protected Boolean			mBeaconUnregistered;
	protected String			mImageUriOriginal;
	protected AircandiCommon	mCommon;
	protected RequestListener	mImageRequestListener;
	protected WebImageView		mImageRequestWebImageView;
	protected String			mImagePath;
	protected String			mImageName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (!Aircandi.getInstance().getLaunchedFromRadar()) {
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
			mCommon.setTheme(isDialog());
			super.onCreate(savedInstanceState);
			super.setContentView(this.getLayoutID());
			mCommon.initialize();
			mImagePath = Environment.getExternalStorageDirectory() + CandiConstants.IMAGE_CAPTURE_PATH;
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
			if (requestCode == CandiConstants.ACTIVITY_PICTURE_SEARCH) {

				Tracker.trackEvent("Entity", "PictureSearch", "None", 0);

				if (intent != null && intent.getExtras() != null) {
					Bundle extras = intent.getExtras();
					final String imageUri = extras.getString(getString(R.string.EXTRA_URI));
					final String imageTitle = extras.getString(getString(R.string.EXTRA_URI_TITLE));
					final String imageDescription = extras.getString(getString(R.string.EXTRA_URI_DESCRIPTION));

					ImageRequestBuilder builder = new ImageRequestBuilder(mImageRequestWebImageView);
					builder.setFromUris(imageUri, null);
					builder.setRequestListener(new RequestListener() {

						@Override
						public void onComplete(Object response) {

							final ServiceResponse serviceResponse = (ServiceResponse) response;
							if (serviceResponse.responseCode == ResponseCode.Success) {
								runOnUiThread(new Runnable() {

									@Override
									public void run() {
										if (mImageRequestListener != null) {
											ImageResponse imageResponse = (ImageResponse) serviceResponse.data;
											mImageRequestListener.onComplete(serviceResponse, imageResponse.imageUri, null, imageResponse.bitmap, imageTitle,
													imageDescription);
										}
									}
								});
							}
						}
					});

					if (imageTitle != null && !imageTitle.equals("")) {
						EditText title = (EditText) findViewById(R.id.text_title);
						if (title != null && title.getText().toString().equals("")) {
							title.setText(imageTitle);
						}
					}

					if (imageDescription != null && !imageDescription.equals("")) {
						EditText description = (EditText) findViewById(R.id.text_content);
						if (description != null && description.getText().toString().equals("")) {
							description.setText(imageDescription);
						}
					}

					ImageRequest imageRequest = builder.create();
					mImageRequestWebImageView.setImageRequest(imageRequest, false);
				}
			}
			else if (requestCode == CandiConstants.ACTIVITY_PICTURE_PICK_DEVICE) {

				Tracker.trackEvent("Entity", "PickPicture", "None", 0);
				Uri imageUri = intent.getData();
				Bitmap bitmap = null;

				bitmap = ImageManager.getInstance().loadBitmapFromDevice(imageUri, String.valueOf(CandiConstants.IMAGE_WIDTH_DEFAULT));
				if (bitmap != null && mImageRequestListener != null) {
					mImageRequestWebImageView.getImageView().setImageBitmap(null);
					ImageUtils.showImageInImageView(bitmap, mImageRequestWebImageView.getImageView(), true, AnimUtils.fadeInMedium());
					mImageRequestListener.onComplete(new ServiceResponse(), null, null, bitmap, null, null);
				}
			}
			else if (requestCode == CandiConstants.ACTIVITY_PICTURE_MAKE) {

				Tracker.trackEvent("Entity", "TakePicture", "None", 0);
				try {
					/* Get bitmap */
					Bitmap bitmap = Media.getBitmap(getContentResolver(), Uri.fromFile(mCommon.getTempFile(this, "image_capture.tmp")));

					/* Scale and crop */
					ImageRequest imageRequest = new ImageRequest();
					imageRequest.setImageShape(ImageShape.Square);
					imageRequest.setScaleToWidth(CandiConstants.IMAGE_WIDTH_DEFAULT);
					bitmap = ImageUtils.scaleAndCropBitmap(bitmap, imageRequest);

					/* Adjust rotation using file Exif information */
					ExifInterface exif = new ExifInterface(mCommon.getTempFile(this, "image_capture.tmp").getAbsolutePath());
					int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
					int rotate = 0;
					switch (orientation) {
						case ExifInterface.ORIENTATION_ROTATE_270:
							rotate = 270;
							break;
						case ExifInterface.ORIENTATION_ROTATE_180:
							rotate = 180;
							break;
						case ExifInterface.ORIENTATION_ROTATE_90:
							rotate = 90;
							break;
					}

					// create a matrix object
					Matrix matrix = new Matrix();
					matrix.postRotate(rotate); // anti-clockwise by 90 degrees

					// create a new bitmap from the original using the matrix to transform the result
					bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

					mImageRequestWebImageView.getImageView().setImageBitmap(null);
					if (mImageRequestListener != null) {
						ImageUtils.showImageInImageView(bitmap, mImageRequestWebImageView.getImageView(), true, AnimUtils.fadeInMedium());
						mImageRequestListener.onComplete(new ServiceResponse(), null, null, bitmap, null, null);
					}
				}
				catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
			else if (requestCode == CandiConstants.ACTIVITY_LINK_PICK) {

				Tracker.trackEvent("Entity", "PickBookmark", "None", 0);
				if (intent != null && intent.getExtras() != null) {
					Bundle extras = intent.getExtras();
					final String linkUri = extras.getString(getString(R.string.EXTRA_URI));
					final String linkTitle = extras.getString(getString(R.string.EXTRA_URI_TITLE));
					final String linkDescription = extras.getString(getString(R.string.EXTRA_URI_DESCRIPTION));

					if (mImageRequestWebImageView != null) {
						if (linkUri != null && !linkUri.equals("")) {

							ImageRequestBuilder builder = new ImageRequestBuilder(mImageRequestWebImageView);
							builder.setFromUris(null, linkUri);
							builder.setRequestListener(new RequestListener() {

								@Override
								public void onComplete(Object response) {

									final ServiceResponse serviceResponse = (ServiceResponse) response;
									if (serviceResponse.responseCode == ResponseCode.Success) {
										runOnUiThread(new Runnable() {

											@Override
											public void run() {
												if (mImageRequestListener != null) {
													ImageResponse imageResponse = (ImageResponse) serviceResponse.data;
													mImageRequestListener.onComplete(serviceResponse, null, linkUri, imageResponse.bitmap, linkTitle,
															linkDescription);
												}
											}
										});
									}
								}
							});

							ImageRequest imageRequest = builder.create();
							mImageRequestWebImageView.setImageRequest(imageRequest, false);
						}
					}
					else {
						if (mImageRequestListener != null) {
							mImageRequestListener.onComplete(new ServiceResponse(), null, linkUri, null, linkTitle, linkDescription);
						}
					}
				}
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Picker routines
	// --------------------------------------------------------------------------------------------

	protected void pickPicture() {
		Intent picturePickerIntent = new Intent(Intent.ACTION_PICK);
		picturePickerIntent.setType("image/*");
		startActivityForResult(picturePickerIntent, CandiConstants.ACTIVITY_PICTURE_PICK_DEVICE);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
	}

	protected void pickVideo() {
		Intent videoPickerIntent = new Intent(Intent.ACTION_PICK);
		videoPickerIntent.setType("video/*");
		startActivityForResult(videoPickerIntent, CandiConstants.ACTIVITY_VIDEO_PICK);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
	}

	protected void takeVideo() {
		Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		startActivityForResult(takeVideoIntent, CandiConstants.ACTIVITY_VIDEO_MAKE);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
	}

	protected void takePicture() {
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mCommon.getTempFile(this, "image_capture.tmp")));
		startActivityForResult(intent, CandiConstants.ACTIVITY_PICTURE_MAKE);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
	}

	private void pickWebPage() {
		Intent intent = new Intent(this, BookmarkPicker.class);
		startActivityForResult(intent, CandiConstants.ACTIVITY_LINK_PICK);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
	}

	protected void pickAircandiPicture() {
		Intent candigramPickerIntent = new Intent(this, PictureSearch.class);
		startActivityForResult(candigramPickerIntent, CandiConstants.ACTIVITY_PICTURE_SEARCH);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
	}

	protected void useFacebook() {
		/*
		 * Only used for user pictures
		 */
		final User user = Aircandi.getInstance().getUser();
		user.imageUri = "https://graph.facebook.com/" + user.facebookId + "/picture?type=large";

		ImageRequestBuilder builder = new ImageRequestBuilder(mImageRequestWebImageView);
		builder.setFromUris(user.imageUri, user.linkUri);
		builder.setRequestListener(new RequestListener() {

			@Override
			public void onComplete(Object response) {

				/* Used to pass back the bitmap and imageUri (sometimes) for the entity */
				if (mImageRequestListener != null) {
					mImageRequestListener.onComplete(response, user.imageUri);
				}
			}
		});

		ImageRequest imageRequest = builder.create();
		mImageRequestWebImageView.setImageRequest(imageRequest);
	}

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	protected void showChangePictureDialog(final boolean showFacebookOption, WebImageView webImageView, final RequestListener listener) {

		mImageRequestListener = listener;
		mImageRequestWebImageView = webImageView;

		int listId = R.array.dialog_list_picture_sources;
		if (showFacebookOption) {
			listId = R.array.dialog_list_picture_sources_facebook;
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(FormActivity.this);

		View titleView = getLayoutInflater().inflate(R.layout.temp_dialog_title, null);
		((TextView) titleView.findViewById(R.id.dialog_title_text)).setText(getResources().getString(R.string.dialog_change_picture_title));
		//builder.setCustomTitle(titleView);
		builder.setTitle(R.string.dialog_change_picture_title);
		builder.setIcon(R.drawable.icon_app);

		builder.setCancelable(true);
		builder.setNegativeButton(getResources().getString(R.string.dialog_button_cancel), null);
		builder.setIcon(R.drawable.icon_app);

		builder.setItems(listId, new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int item) {

				/* Aircandi picture */
				if (item == 0) {
					pickAircandiPicture();
				}

				/* Gallery picture */
				else if (item == 1) {
					pickPicture();
				}

				/* Take picture */
				else if (item == 2) {
					takePicture();
				}

				/* Use a web page */
				else if (item == 3) {
					pickWebPage();
				}

				/* Facebook */
				else if (item == 4 && showFacebookOption) {
					useFacebook();
				}

				/* None */
				else if ((item == 4 && !showFacebookOption) || (item == 5 && showFacebookOption)) {

					/* Tag has the uri to use for the placeholder */
					String imageUri = "resource:placeholder_picture";
					if (mCommon.mEntityType.equals(CandiConstants.TYPE_CANDI_COLLECTION)) {
						imageUri = "resource:ic_collection_250";
					}
					ImageRequestBuilder builder = new ImageRequestBuilder(mImageRequestWebImageView);
					builder.setFromUris(imageUri, null);

					ImageRequest imageRequest = builder.create();

					mImageRequestWebImageView.setImageRequest(imageRequest);

					if (mImageRequestListener != null) {
						mImageRequestListener.onComplete(new ServiceResponse(), imageUri, null, null, null, null);
					}

					Tracker.trackEvent("Entity", "DefaultPicture", "None", 0);
				}
				else {
					ImageUtils.showToastNotification("Not implemented yet.", Toast.LENGTH_SHORT);
				}
				dialog.dismiss();
			}
		});

		AlertDialog alert = builder.create();
		alert.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		alert.show();
	}

	protected Boolean isDialog() {
		return false;
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onResume() {
		super.onResume();
		mCommon.doResume();
		if (!mCommon.mPrefTheme.equals(Aircandi.settings.getString(Preferences.PREF_THEME, "aircandi_theme_midnight"))) {
			mCommon.mPrefTheme = Aircandi.settings.getString(Preferences.PREF_THEME, "aircandi_theme_midnight");
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
				mCommon.recycleImageViewDrawable(R.id.image_picture);
				mCommon.recycleImageViewDrawable(R.id.image_user_picture);
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

	protected class SimpleTextWatcher implements TextWatcher {

		@Override
		public void afterTextChanged(Editable s) {}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {}
	}
}