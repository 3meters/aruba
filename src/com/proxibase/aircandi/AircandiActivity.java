package com.proxibase.aircandi;

import java.io.File;
import java.io.FileNotFoundException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.proxibase.aircandi.components.Exceptions;
import com.proxibase.aircandi.components.ImageManager;
import com.proxibase.aircandi.components.ImageUtils;
import com.proxibase.aircandi.components.ImageManager.ImageRequest;
import com.proxibase.aircandi.components.ImageManager.ImageRequest.ImageShape;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ResultCodeDetail;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.widgets.WebImageView;
import com.proxibase.sdk.android.proxi.consumer.Beacon;
import com.proxibase.sdk.android.proxi.consumer.Command;
import com.proxibase.sdk.android.proxi.consumer.Comment;
import com.proxibase.sdk.android.proxi.consumer.EntityProxy;
import com.proxibase.sdk.android.proxi.consumer.User;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestListener;

public abstract class AircandiActivity extends Activity {

	protected ImageView			mProgressIndicator;
	protected ImageView			mButtonRefresh;
	protected Button			mContextButton;

	protected Integer			mParentEntityId;
	protected Beacon			mBeacon;
	protected Boolean			mBeaconUnregistered;
	protected EntityProxy		mEntityProxy;
	protected Object			mEntity;
	protected String			mImageUriOriginal;
	protected Command			mCommand;
	protected User				mUser;
	protected Comment			mComment;
	protected String			mPrefTheme;
	protected ProgressDialog	mProgressDialog;

	protected RequestListener	mImageRequestListener;
	protected WebImageView		mImageRequestWebImageView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		/*
		 * Theme has to be set before any UI is constructed. We also
		 * have to do it for each activity so they pickup our custom
		 * style attributes.
		 */
		setTheme();
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		super.setContentView(this.getLayoutID());

		unpackIntent(getIntent());
		configure();
	}

	protected void unpackIntent(Intent intent) {

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mParentEntityId = extras.getInt(getString(R.string.EXTRA_PARENT_ENTITY_ID));

			String json = extras.getString(getString(R.string.EXTRA_ENTITY));
			if (json != null && json.length() > 0) {
				mEntityProxy = ProxibaseService.getGson(GsonType.Internal).fromJson(json, EntityProxy.class);
			}

			json = extras.getString(getString(R.string.EXTRA_COMMAND));
			if (json != null && json.length() > 0) {
				mCommand = ProxibaseService.getGson(GsonType.Internal).fromJson(json, Command.class);
				if (mCommand.verb == null || mCommand.verb.length() == 0) {
					throw new IllegalStateException("A command passed to an activity must include a verb");
				}
			}
			else {
				throw new IllegalStateException("A command must be passed when calling an Aircandi activity");
			}

			json = extras.getString(getString(R.string.EXTRA_BEACON));
			if (json != null && json.length() > 0) {
				mBeacon = ProxibaseService.getGson(GsonType.Internal).fromJson(json, Beacon.class);
			}

			json = extras.getString(getString(R.string.EXTRA_USER));
			if (json != null && json.length() > 0) {
				mUser = ProxibaseService.getGson(GsonType.Internal).fromJson(json, User.class);
			}
			
			json = extras.getString(getString(R.string.EXTRA_COMMENT));
			if (json != null && json.length() > 0) {
				mComment = ProxibaseService.getGson(GsonType.Internal).fromJson(json, Comment.class);
			}
		}
	}

	private void setTheme() {
		mPrefTheme = Aircandi.settings.getString(Preferences.PREF_THEME, "aircandi_theme_blueray");
		int themeResourceId = getApplicationContext().getResources().getIdentifier(mPrefTheme, "style", getPackageName());
		this.setTheme(themeResourceId);
	}
	
	@SuppressWarnings("unused")
	private void startAircandi()
	{
		Intent intent = new Intent(this, CandiSearchActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	private void configure() {

		mProgressIndicator = (ImageView) findViewById(R.id.image_progress_indicator);
		if (mProgressIndicator != null) {
			mProgressIndicator.setVisibility(View.INVISIBLE);
		}

		mButtonRefresh = (ImageView) findViewById(R.id.image_refresh_button);
		if (mButtonRefresh != null) {
			mButtonRefresh.setVisibility(View.VISIBLE);
		}

		mProgressDialog = new ProgressDialog(this);
	}

	// --------------------------------------------------------------------------------------------
	// Events routines
	// --------------------------------------------------------------------------------------------

	@Override
	public void onBackPressed() {
		startTitlebarProgress();
		setResult(Activity.RESULT_OK);
		super.onBackPressed();
	}

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();

		Window window = getWindow();
		window.setFormat(PixelFormat.RGBA_8888);
	}

	public void onHomeClick(View view) {
		Intent intent = new Intent(this, CandiSearchActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);

		/* Hide the sign out option if we don't have a current session */
		MenuItem item = menu.findItem(R.id.signinout);
		item.setVisible(false);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		/* Hide the sign out option if we don't have a current session */
		MenuItem item = menu.findItem(R.id.signinout);
		item.setVisible(false);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
			case R.id.settings :
				startActivity(new Intent(this, Preferences.class));
				return (true);
			default :
				return (super.onOptionsItemSelected(item));
		}
	}

	// --------------------------------------------------------------------------------------------
	// Picker routines
	// --------------------------------------------------------------------------------------------

	public void pickPicture() {
		Intent picturePickerIntent = new Intent(Intent.ACTION_PICK);
		picturePickerIntent.setType("image/*");
		startActivityForResult(picturePickerIntent, CandiConstants.ACTIVITY_PICTURE_PICK_DEVICE);
	}

	public void pickVideo() {
		Intent videoPickerIntent = new Intent(Intent.ACTION_PICK);
		videoPickerIntent.setType("video/*");
		startActivityForResult(videoPickerIntent, CandiConstants.ACTIVITY_VIDEO_PICK);
	}

	public void takeVideo() {
		Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		startActivityForResult(takeVideoIntent, CandiConstants.ACTIVITY_VIDEO_MAKE);
	}

	public void takePicture() {
		Intent takePictureFromCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		if (ImageManager.getInstance().hasImageCaptureBug()) {
			takePictureFromCameraIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File("/sdcard/tmp/foo.jpeg")));
		}
		else
			takePictureFromCameraIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,
					android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(takePictureFromCameraIntent, CandiConstants.ACTIVITY_PICTURE_MAKE);
	}

	public void pickAircandiPicture() {
		Intent candigramPickerIntent = new Intent(this, PictureSearch.class);
		startActivityForResult(candigramPickerIntent, CandiConstants.ACTIVITY_PICTURE_PICK_AIRCANDI);
	}

	public void useFacebook() {

		mUser.imageUri = "https://graph.facebook.com/" + mUser.facebookId + "/picture?type=large";

		ImageRequest imageRequest = new ImageRequest(mUser.imageUri, ImageShape.Square, "binary", false,
				CandiConstants.IMAGE_WIDTH_SEARCH_MAX, false, true, true, 1, this, new RequestListener() {

					@Override
					public void onComplete(Object response) {

						/* Used to pass back the bitmap and imageUri (sometimes) for the entity */
						if (mImageRequestListener != null) {
							mImageRequestListener.onComplete(response, mUser.imageUri);
						}

						final ServiceResponse serviceResponse = (ServiceResponse) response;
						if (serviceResponse.responseCode != ResponseCode.Success) {
							return;
						}
						else {
							runOnUiThread(new Runnable() {

								@Override
								public void run() {
									GoogleAnalyticsTracker.getInstance().trackEvent("Entity", "FacebookPicture", "None", 0);
									Bitmap bitmap = (Bitmap) serviceResponse.data;
									ImageUtils.showImageInImageView(bitmap, mImageRequestWebImageView);
								}
							});
						}
					}
				});

		mImageRequestWebImageView.setImageBitmap(null);
		ImageManager.getInstance().getImageLoader().fetchImage(imageRequest);
	}

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	protected void showProgressDialog(boolean visible, String message) {
		if (visible) {
			mProgressDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
			mProgressDialog.setMessage(message);
			mProgressDialog.setIndeterminate(true);
			mProgressDialog.show();
		}
		else {
			mProgressDialog.dismiss();
		}
	}

	public void showBackButton(boolean show, String backButtonText) {
		if (show) {
			mContextButton.setVisibility(View.VISIBLE);
			mContextButton.setText(backButtonText);
			mContextButton.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					onBackPressed();
				}
			});
		}
	}

	protected void showChangePictureDialog(final boolean showFacebookOption, WebImageView webImageView, final RequestListener listener) {

		mImageRequestListener = listener;
		mImageRequestWebImageView = webImageView;

		runOnUiThread(new Runnable() {

			@Override
			public void run() {

				int listId = R.array.dialog_list_picture_sources;
				if (showFacebookOption) {
					listId = R.array.dialog_list_picture_sources_facebook;
				}

				AlertDialog.Builder builder = new AlertDialog.Builder(AircandiActivity.this);

				builder.setTitle(getResources().getString(R.string.dialog_change_picture_title));

				builder.setCancelable(true);

				builder.setOnCancelListener(new OnCancelListener() {

					@Override
					public void onCancel(DialogInterface dialog) {}
				});

				builder.setNegativeButton(getResources().getString(R.string.dialog_change_picture_button_negative),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {}
						});

				builder.setItems(listId, new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int item) {
						if (item == 0) { /* Aircandi picture */
							pickAircandiPicture();
							overridePendingTransition(R.anim.fade_in_medium, R.anim.hold);
						}
						else if (item == 1) { /* Gallery picture */
							pickPicture();
							overridePendingTransition(R.anim.fade_in_medium, R.anim.hold);
						}
						else if (item == 2) { /* Take picture */
							takePicture();
							overridePendingTransition(R.anim.fade_in_medium, R.anim.hold);
						}
						else if (item == 3 && showFacebookOption) { /* Facebook */
							useFacebook();
						}
						else if ((item == 3 && !showFacebookOption) || (item == 4 && showFacebookOption)) { /* None */

							String imageUri = (String) mImageRequestWebImageView.getTag();

							ImageRequest imageRequest = new ImageRequest(imageUri, ImageShape.Square, "binary", false,
										CandiConstants.IMAGE_WIDTH_SEARCH_MAX, false, true, true, 1, this, null);

							mImageRequestWebImageView.setImageRequest(imageRequest, null);

							if (mImageRequestListener != null) {
								mImageRequestListener.onComplete(new ServiceResponse(ResponseCode.Success, ResultCodeDetail.Success, null, null),
										imageUri);
							}
							GoogleAnalyticsTracker.getInstance().trackEvent("Entity", "DefaultPicture", "None", 0);
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
		});
	}

	protected void startTitlebarProgress() {
		if (mProgressIndicator != null) {
			mProgressIndicator.setVisibility(View.VISIBLE);
			mButtonRefresh.setVisibility(View.INVISIBLE);
			mProgressIndicator.bringToFront();
			AnimationDrawable animation = (AnimationDrawable) mProgressIndicator.getBackground();
			animation.start();
			mProgressIndicator.invalidate();
		}
	}

	protected void stopTitlebarProgress() {
		if (mProgressIndicator != null) {
			mProgressIndicator.setAnimation(null);
			mButtonRefresh.setVisibility(View.VISIBLE);
			mButtonRefresh.bringToFront();
			mProgressIndicator.setVisibility(View.INVISIBLE);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle routines
	// --------------------------------------------------------------------------------------------

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		/*
		 * Called before onResume. If we are returning from the market app, we
		 * get a zero result code whether the user decided to start an install
		 * or not.
		 */
		if (requestCode == CandiConstants.ACTIVITY_PICTURE_PICK_AIRCANDI) {
			if (resultCode == Activity.RESULT_FIRST_USER) {
				if (data != null) {
					Bundle extras = data.getExtras();
					if (extras != null) {
						final String aircandiImageUri = extras.getString(getString(R.string.EXTRA_URI));

						ImageRequest imageRequest = new ImageRequest(aircandiImageUri, ImageShape.Square, "binary", false,
								CandiConstants.IMAGE_WIDTH_SEARCH_MAX, false, true, true, 1, this, new RequestListener() {

									@Override
									public void onComplete(Object response) {

										final ServiceResponse serviceResponse = (ServiceResponse) response;
										if (serviceResponse.responseCode != ResponseCode.Success) {
											return;
										}
										else {
											final Bitmap bitmap = (Bitmap) serviceResponse.data;
											runOnUiThread(new Runnable() {

												@Override
												public void run() {
													ImageUtils.showImageInImageView(bitmap, mImageRequestWebImageView);
													if (mImageRequestListener != null) {
														mImageRequestListener.onComplete(serviceResponse, aircandiImageUri);
													}
												}
											});
										}
									}
								});

						mImageRequestWebImageView.setImageBitmap(null);
						ImageManager.getInstance().getImageLoader().fetchImage(imageRequest);
					}
				}
			}
		}
		else if (requestCode == CandiConstants.ACTIVITY_PICTURE_PICK_DEVICE) {
			if (resultCode == Activity.RESULT_OK) {

				Uri imageUri = data.getData();
				Bitmap bitmap = null;
				mImageRequestWebImageView.setImageBitmap(null);

				bitmap = ImageManager.getInstance().loadBitmapFromDevice(imageUri, String.valueOf(CandiConstants.IMAGE_WIDTH_SEARCH_MAX));
				if (mImageRequestListener != null) {
					ImageUtils.showImageInImageView(bitmap, mImageRequestWebImageView);
					mImageRequestListener.onComplete(new ServiceResponse(ResponseCode.Success, ResultCodeDetail.Success, bitmap, null), "updated");
					GoogleAnalyticsTracker.getInstance().trackEvent("Entity", "PickPicture", "None", 0);
				}
			}
		}
		else if (requestCode == CandiConstants.ACTIVITY_PICTURE_MAKE) {
			if (resultCode == Activity.RESULT_OK) {

				mImageRequestWebImageView.setImageBitmap(null);
				Uri imageUri = null;
				if (ImageManager.getInstance().hasImageCaptureBug()) {
					File imageFile = new File("/sdcard/tmp/foo.jpeg");
					try {
						imageUri = Uri.parse(android.provider.MediaStore.Images.Media.insertImage(getContentResolver(), imageFile.getAbsolutePath(),
								null, null));
						if (!imageFile.delete()) {
						}
					}
					catch (FileNotFoundException exception) {
						Exceptions.Handle(exception);
					}
				}
				else {
					imageUri = data.getData();
				}

				Bitmap bitmap = ImageManager.getInstance().loadBitmapFromDevice(imageUri, String.valueOf(CandiConstants.IMAGE_WIDTH_SEARCH_MAX));
				if (mImageRequestListener != null) {
					ImageUtils.showImageInImageView(bitmap, mImageRequestWebImageView);
					mImageRequestListener.onComplete(new ServiceResponse(ResponseCode.Success, ResultCodeDetail.Success, bitmap, null), "updated");
					GoogleAnalyticsTracker.getInstance().trackEvent("Entity", "TakePicture", "None", 0);
				}
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	protected void onDestroy() {
		super.onDestroy();

		/* This activity gets destroyed everytime we leave using back or finish(). */
		mEntity = null;
		mUser = null;
		mBeacon = null;
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

	public static enum Verb {
		New, Edit, Delete, View
	}
}