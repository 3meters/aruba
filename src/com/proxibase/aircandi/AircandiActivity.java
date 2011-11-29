package com.proxibase.aircandi;

import java.io.File;
import java.io.FileNotFoundException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.utils.Exceptions;
import com.proxibase.aircandi.utils.ImageManager;
import com.proxibase.aircandi.utils.ImageLoader.ImageProfile;
import com.proxibase.aircandi.utils.ImageManager.ImageRequestListener;
import com.proxibase.sdk.android.proxi.consumer.Beacon;
import com.proxibase.sdk.android.proxi.consumer.Command;
import com.proxibase.sdk.android.proxi.consumer.EntityProxy;
import com.proxibase.sdk.android.proxi.consumer.User;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;

public abstract class AircandiActivity extends Activity {

	protected ImageView				mProgressIndicator;
	protected ImageView				mButtonRefresh;
	protected Button				mContextButton;

	protected Integer				mParentEntityId;
	protected Beacon				mBeacon;
	protected Boolean				mBeaconUnregistered;
	protected EntityProxy			mEntityProxy;
	protected Object				mEntity;
	protected Command				mCommand;
	protected User					mUser;
	protected String				mPrefTheme;
	protected DisplayMetrics		mDisplayMetrics;
	protected ProgressDialog		mProgressDialog;

	protected ImageRequestListener	mImageRequestListener;

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
			if (json != null && !json.equals("")) {
				mEntityProxy = ProxibaseService.getGson(GsonType.Internal).fromJson(json, EntityProxy.class);
			}

			json = extras.getString(getString(R.string.EXTRA_COMMAND));
			if (json != null && !json.equals("")) {
				mCommand = ProxibaseService.getGson(GsonType.Internal).fromJson(json, Command.class);
				if (mCommand.verb == null || mCommand.verb.length() == 0) {
					throw new IllegalStateException("A command passed to an activity must include a verb");
				}
			}
			else {
				throw new IllegalStateException("A command must be passed when calling an Aircandi activity");
			}

			json = extras.getString(getString(R.string.EXTRA_BEACON));
			if (json != null && !json.equals("")) {
				mBeacon = ProxibaseService.getGson(GsonType.Internal).fromJson(json, Beacon.class);
			}

			json = extras.getString(getString(R.string.EXTRA_USER));
			if (json != null && !json.equals("")) {
				mUser = ProxibaseService.getGson(GsonType.Internal).fromJson(json, User.class);
			}
		}
	}

	private void setTheme() {
		mPrefTheme = Aircandi.settings.getString(Preferences.PREF_THEME, "aircandi_theme.blueray");
		int themeResourceId = getApplicationContext().getResources().getIdentifier(mPrefTheme, "style", getPackageName());
		this.setTheme(themeResourceId);
	}

	private void configure() {

		mDisplayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);

		mProgressIndicator = (ImageView) findViewById(R.id.img_progress_indicator);
		if (mProgressIndicator != null) {
			mProgressIndicator.setVisibility(View.INVISIBLE);
		}

		mButtonRefresh = (ImageView) findViewById(R.id.img_refresh_button);
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

	public void pickPhoto() {
		Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
		photoPickerIntent.setType("image/*");
		startActivityForResult(photoPickerIntent, CandiConstants.ACTIVITY_PHOTO_PICK);
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

	public void takePhoto() {
		Intent takePictureFromCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		if (ImageManager.getInstance().hasImageCaptureBug()) {
			takePictureFromCameraIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File("/sdcard/tmp/foo.jpeg")));
		}
		else
			takePictureFromCameraIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,
					android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(takePictureFromCameraIntent, CandiConstants.ACTIVITY_PHOTO_MAKE);
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

	protected void showAlertDialog(int iconResource, String title, String message, OnClickListener listener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(title);
		builder.setMessage(message);
		builder.setIcon(iconResource);
		if (listener != null) {
			builder.setPositiveButton(android.R.string.ok, listener);
		}
		builder.show();
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

	protected void showAddPictureDialog(final boolean showFacebookOption, final ImageRequestListener listener) {

		mImageRequestListener = listener;

		runOnUiThread(new Runnable() {

			@Override
			public void run() {

				int listId = R.array.dialog_list_picture_sources;

				if (showFacebookOption) {
					listId = R.array.dialog_list_picture_sources_facebook;
				}
				AlertDialog.Builder builder = new AlertDialog.Builder(AircandiActivity.this);
				builder.setTitle("Select picture...");
				builder.setCancelable(true);
				builder.setOnCancelListener(new OnCancelListener() {

					@Override
					public void onCancel(DialogInterface dialog) {
					}
				});
				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				});

				builder.setItems(listId, new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int item) {
						if (item == 0) { /* Gallery picture */
							pickPhoto();
							overridePendingTransition(R.anim.fade_in_medium, R.anim.hold);
						}
						else if (item == 1) { /* Take picture */
							takePhoto();
							overridePendingTransition(R.anim.fade_in_medium, R.anim.hold);
						}
						else if (item == 2) { /* Facebook picture */
							if (showFacebookOption) {
								mUser.imageUri = "https://graph.facebook.com/" + mUser.facebookId + "/picture?type=large";
								ImageManager.getInstance().getImageLoader().fetchImageByProfile(ImageProfile.SquareTile, mUser.imageUri,
										new ImageRequestListener() {

											@Override
											public void onImageReady(final Bitmap bitmap) {
												listener.onImageReady(bitmap);
											}

											@Override
											public void onProxibaseException(final ProxibaseException exception) {
												listener.onProxibaseException(exception);
											}
										});
							}
							else {
								listener.onImageReady(null);
							}
						}
						else if (item == 3) { /* None */
							if (showFacebookOption) {
								listener.onImageReady(null);
							}
						}
						else {
							Toast.makeText(getApplicationContext(), "Not implemented yet.", Toast.LENGTH_SHORT).show();
						}
						dialog.dismiss();
					}
				});
				AlertDialog alert = builder.create();
				alert.show();
			}
		});
	}

	protected void showPicture(final Bitmap bitmap, final int targetImageViewId) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				ImageView targetImageView = (ImageView) findViewById(targetImageViewId);
				if (targetImageView != null) {
					Animation animation = AnimationUtils.loadAnimation(AircandiActivity.this, R.anim.fade_in_medium);
					animation.setFillEnabled(true);
					animation.setFillAfter(true);
					animation.setStartOffset(500);

					targetImageView.setImageBitmap(bitmap);
					targetImageView.startAnimation(animation);
					targetImageView.setVisibility(View.VISIBLE);
				}
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
		if (requestCode == CandiConstants.ACTIVITY_PHOTO_PICK) {
			if (resultCode == Activity.RESULT_OK) {

				Uri imageUri = data.getData();
				Bitmap bitmap = null;
				try {
					bitmap = ImageManager.getInstance().loadBitmapFromDevice(imageUri, String.valueOf(CandiConstants.IMAGE_WIDTH_MAX));
				}
				catch (ProxibaseException exception) {
					if (mImageRequestListener != null) {
						mImageRequestListener.onProxibaseException(exception);
					}
					else {
						Exceptions.Handle(exception);
					}
				}
				if (bitmap == null) {
					throw new IllegalStateException("bitmap picked from gallery is null");
				}
				else {
					if (mImageRequestListener != null) {
						mImageRequestListener.onImageReady(bitmap);
					}
				}

				//				BaseEntity entity = (BaseEntity) mEntity;
				//
				//				Uri imageUri = data.getData();
				//				Bitmap bitmap = null;
				//				try {
				//					bitmap = ImageManager.getInstance().loadBitmapFromDevice(imageUri, String.valueOf(CandiConstants.IMAGE_WIDTH_MAX));
				//				}
				//				catch (ProxibaseException exception) {
				//					exception.printStackTrace();
				//				}
				//				if (bitmap == null) {
				//					throw new IllegalStateException("bitmap picked from gallery is null");
				//				}
				//
				//				entity.imageUri = null;
				//				entity.imageBitmap = bitmap;
				//				((ImageView) findViewById(mPickerTarget)).setImageBitmap(entity.imageBitmap);
				//				((ImageView) findViewById(mPickerTarget)).setVisibility(View.VISIBLE);
			}
		}
		else if (requestCode == CandiConstants.ACTIVITY_PHOTO_MAKE) {
			if (resultCode == Activity.RESULT_OK) {

				Uri imageUri = null;
				if (ImageManager.getInstance().hasImageCaptureBug()) {
					File imageFile = new File("/sdcard/tmp/foo.jpeg");
					try {
						imageUri = Uri.parse(android.provider.MediaStore.Images.Media.insertImage(getContentResolver(), imageFile
									.getAbsolutePath(), null, null));
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
				Bitmap bitmap = null;
				try {
					bitmap = ImageManager.getInstance().loadBitmapFromDevice(imageUri, String.valueOf(CandiConstants.IMAGE_WIDTH_MAX));
				}
				catch (ProxibaseException exception) {
					if (mImageRequestListener != null) {
						mImageRequestListener.onProxibaseException(exception);
					}
					else {
						Exceptions.Handle(exception);
					}
				}
				if (bitmap == null) {
					throw new IllegalStateException("bitmap taken with camera is null");
				}
				else {
					if (mImageRequestListener != null) {
						mImageRequestListener.onImageReady(bitmap);
					}
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