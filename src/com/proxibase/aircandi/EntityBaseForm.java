package com.proxibase.aircandi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.proxibase.aircandi.WebForm.FormTab;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.models.BaseEntity;
import com.proxibase.aircandi.utils.DateUtils;
import com.proxibase.aircandi.utils.ImageManager;
import com.proxibase.aircandi.utils.ImageUtils;
import com.proxibase.aircandi.utils.Logger;
import com.proxibase.aircandi.utils.S3;
import com.proxibase.aircandi.utils.ImageManager.IImageRequestListener;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest.ImageFormat;
import com.proxibase.sdk.android.proxi.consumer.Beacon.BeaconType;
import com.proxibase.sdk.android.proxi.consumer.EntityProxy.Visibility;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.IQueryListener;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException.ProxiErrorCode;
import com.proxibase.sdk.android.util.ProxiConstants;

public abstract class EntityBaseForm extends AircandiActivity {

	protected boolean		mProcessing		= false;
	protected PickerTarget	mPickerTarget	= PickerTarget.None;
	private FormTab			mActiveTab		= FormTab.Content;
	private ViewFlipper		mViewFlipper;
	private int				mTextColorFocused;
	private int				mTextColorUnfocused;
	private int				mHeightActive;
	private int				mHeightInactive;
	private ImageView		mImageViewContent;
	private ImageView		mImageViewSettings;
	private TextView		mTextViewContent;
	private TextView		mTextViewSettings;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		configure();
	}

	private void configure() {
		mViewFlipper = (ViewFlipper) findViewById(R.id.flipper_form);
		mImageViewContent = (ImageView) findViewById(R.id.img_tab_content);
		mImageViewSettings = (ImageView) findViewById(R.id.img_tab_settings);
		mTextViewContent = (TextView) findViewById(R.id.txt_tab_content);
		mTextViewSettings = (TextView) findViewById(R.id.txt_tab_settings);

		TypedValue resourceName = new TypedValue();
		if (this.getTheme().resolveAttribute(R.attr.textColorFocused, resourceName, true)) {
			mTextColorFocused = Color.parseColor((String) resourceName.coerceToString());
		}

		if (this.getTheme().resolveAttribute(R.attr.textColorUnfocused, resourceName, true)) {
			mTextColorUnfocused = Color.parseColor((String) resourceName.coerceToString());
		}

		mHeightActive = ImageUtils.getRawPixelsForDisplayPixels(mDisplayMetrics, 6);
		mHeightInactive = ImageUtils.getRawPixelsForDisplayPixels(mDisplayMetrics, 2);
		if (mViewFlipper != null) {
			setActiveTab(FormTab.Content);
		}
	}

	protected void bindEntity() {

		if (mCommand != null) {

			if (mCommand.verb.equals("new")) {
				/*
				 * Fill in the system and default properties for the new entity
				 */
				final BaseEntity entity = (BaseEntity) mEntity;
				entity.beaconId = mBeacon.id;
				entity.signalFence = mBeacon.levelDb - 10.0f;
				entity.createdById = String.valueOf(mUser.id);
				entity.enabled = true;
				entity.visibility = Visibility.Public.ordinal();
				entity.password = null;
				entity.imageUri = mUser.imageUri;
				entity.imageFormat = ImageFormat.Binary.name().toLowerCase();

				if (entity.imageUri != null && !entity.imageUri.equals("")) {

					fetchImage(entity.imageUri, new IImageRequestListener() {

						@Override
						public void onImageReady(Bitmap bitmap) {
							Logger.d(CandiConstants.APP_NAME, "Photo", "Image fetched: " + entity.imageUri);
							entity.imageBitmap = bitmap;
							showImageThumbnail(bitmap);
						}

						@Override
						public void onProxibaseException(ProxibaseException exception) {
							if (exception.getErrorCode() == ProxiErrorCode.OperationFailed) {
								Bitmap bitmap = ImageManager.getInstance().loadBitmapFromAssets("gfx/placeholder3.png");
								entity.imageBitmap = bitmap;
								showImageThumbnail(bitmap);
							}
						}

						@Override
						public boolean onProgressChanged(int progress) {
							// TODO Auto-generated method stub
							return false;
						}
					});
				}
				entity.parentEntityId = mParentEntityId;
			}
			else if (mCommand.verb.equals("edit")) {
				/*
				 * Do any fetching needed to support editing the entity.
				 */
				final BaseEntity entity = (BaseEntity) mEntity;
				if (entity.imageUri != null && !entity.imageUri.equals("")) {
					fetchImage(entity.imageUri, new IImageRequestListener() {

						@Override
						public void onImageReady(Bitmap bitmap) {
							if (mEntity != null) {
								Logger.d(CandiConstants.APP_NAME, "Photo", "Image fetched: " + entity.imageUri);
								entity.imageBitmap = bitmap;
								showImageThumbnail(bitmap);
							}
						}

						@Override
						public void onProxibaseException(ProxibaseException exception) {
							if (exception.getErrorCode() == ProxiErrorCode.OperationFailed) {
								Bitmap bitmap = ImageManager.getInstance().loadBitmapFromAssets("gfx/placeholder3.png");
								entity.imageBitmap = bitmap;
								showImageThumbnail(bitmap);
							}
						}

						@Override
						public boolean onProgressChanged(int progress) {
							return false;
						}
					});
				}
			}
			else {
			}
		}
	}

	protected void drawEntity() {

		if (mEntity != null) {

			final BaseEntity entity = (BaseEntity) mEntity;

			/* Content */

			if (findViewById(R.id.txt_title) != null) {
				((TextView) findViewById(R.id.txt_title)).setText(entity.title);
			}

			if (findViewById(R.id.txt_content) != null) {
				((TextView) findViewById(R.id.txt_content)).setText(entity.description);
			}

			if (mCommand.verb.equals("new")) {
				if (findViewById(R.id.btn_delete_post) != null) {
					((Button) findViewById(R.id.btn_delete_post)).setVisibility(View.GONE);
				}
			}

			/* Settings */

			if (findViewById(R.id.cbo_visibility) != null) {
				((Spinner) findViewById(R.id.cbo_visibility)).setSelection(entity.visibility);
			}

			if (findViewById(R.id.txt_password) != null) {
				((TextView) findViewById(R.id.txt_password)).setText(entity.password);
			}

			updateImage(entity);

			if (findViewById(R.id.img_public_image) != null) {
				((ImageView) findViewById(R.id.img_public_image)).requestFocus();
			}
		}
	}

	private void updateImage(BaseEntity entity) {
		if (findViewById(R.id.img_public_image) != null) {
			if (entity.imageBitmap != null) {
				((ImageView) findViewById(R.id.img_public_image)).setImageBitmap(entity.imageBitmap);
				((ImageView) findViewById(R.id.img_public_image)).setVisibility(View.VISIBLE);
			}
			else {
				((ImageView) findViewById(R.id.img_public_image)).setImageBitmap(null);
				((ImageView) findViewById(R.id.img_public_image)).setAnimation(null);
				((ImageView) findViewById(R.id.img_public_image)).setVisibility(View.GONE);
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onContentTabClick(View view) {
		if (mActiveTab != FormTab.Content) {
			setActiveTab(FormTab.Content);
		}
	}

	public void onSettingsTabClick(View view) {
		if (mActiveTab != FormTab.Settings) {
			setActiveTab(FormTab.Settings);
		}
	}

	public void onDeleteButtonClick(View view) {
		if (!mProcessing) {
			mProcessing = true;
			startTitlebarProgress();
			deleteEntity();
		}
	}

	public void onCancelButtonClick(View view) {
		mProcessing = false;
		setResult(Activity.RESULT_CANCELED);
		finish();
		//overridePendingTransition(R.anim.hold, R.anim.fade_out_medium);
	}

	public void onChangePublicImageButtonClick(View view) {
		if (!mProcessing) {
			mProcessing = true;
			mPickerTarget = PickerTarget.PublicImage;
			showAddMediaDialog();
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

	protected void useProfilePhoto() {

		if (mPickerTarget == PickerTarget.PublicImage) {

			final BaseEntity entity = (BaseEntity) mEntity;
			entity.imageUri = mUser.imageUri;
			entity.imageFormat = ImageFormat.Binary.name().toLowerCase();

			if (entity.imageUri != null && !entity.imageUri.equals("")) {
				fetchImage(entity.imageUri, new IImageRequestListener() {

					@Override
					public void onImageReady(final Bitmap bitmap) {
						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								if (mEntity != null) {
									Logger.d(CandiConstants.APP_NAME, "Photo", "Image fetched: " + entity.imageUri);
									entity.imageBitmap = bitmap;
									showImageThumbnail(bitmap);
								}
							}
						});
					}

					@Override
					public void onProxibaseException(final ProxibaseException exception) {
						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								if (exception.getErrorCode() == ProxiErrorCode.OperationFailed) {
									Bitmap bitmap = ImageManager.getInstance().loadBitmapFromAssets("gfx/placeholder3.png");
									entity.imageBitmap = bitmap;
									showImageThumbnail(bitmap);
								}
							}
						});
					}

					@Override
					public boolean onProgressChanged(int progress) {
						// TODO Auto-generated method stub
						return false;
					}
				});
			}
			else {

				/* User doesn't have a valid profile image */
				Bitmap bitmap = ImageManager.getInstance().loadBitmapFromAssets("gfx/placeholder3.png");
				entity.imageBitmap = bitmap;
				showImageThumbnail(bitmap);
			}
			mProcessing = false;
		}
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	protected void doSave() {

		/* Insert beacon if it isn't already registered */
		if (mBeacon != null && mBeacon.isUnregistered) {
			mBeacon.registeredById = String.valueOf(mUser.id);
			mBeacon.beaconType = BeaconType.Fixed.name().toLowerCase();
			mBeacon.beaconSetId = ProxiConstants.BEACONSET_WORLD;
			try {
				Logger.i(CandiConstants.APP_NAME, getClass().getSimpleName(), "Inserting beacon: " + mBeacon.id);
				mBeacon.insert();
			}
			catch (ProxibaseException exception) {
				ImageUtils.showToastNotification(EntityBaseForm.this, getString(R.string.post_update_failed_toast), Toast.LENGTH_SHORT);
				exception.printStackTrace();
			}
		}
	}

	protected void updateImages() {
		/*
		 * If we have imageUri but no imageBitmap then image was cleared and save should null imageUri and delete bitmap
		 * from service.
		 */
		BaseEntity entity = (BaseEntity) mEntity;

		if (entity.imageUri != null && entity.imageBitmap == null) {
			try {
				deleteImageFromS3(entity.imageUri.substring(entity.imageUri.lastIndexOf("/") + 1));
				ImageManager.getInstance().getImageCache().remove(entity.imageUri);
				ImageManager.getInstance().getImageCache().remove(entity.imageUri + ".reflection");
				entity.imageUri = null;
			}
			catch (ProxibaseException exception) {
				ImageUtils.showToastNotification(EntityBaseForm.this, getString(R.string.post_update_failed_toast), Toast.LENGTH_SHORT);
				exception.printStackTrace();
			}
		}
		/*
		 * If we have no imageUri and do have imageBitmap then new image has been picked and we should save the image
		 * to the service and set imageUri.
		 * TODO: If update then we might have orphaned a photo in S3
		 */
		else if (entity.imageUri == null && entity.imageBitmap != null) {
			if (entity.imageBitmap != null) {
				String imageKey = String.valueOf(mUser.id) + "_" + String.valueOf(DateUtils.nowString(DateUtils.DATE_NOW_FORMAT_FILENAME)) + ".jpg";
				try {
					addImageToS3(imageKey, entity.imageBitmap);
				}
				catch (ProxibaseException exception) {
					ImageUtils.showToastNotification(EntityBaseForm.this, getString(R.string.post_update_failed_toast), Toast.LENGTH_SHORT);
					exception.printStackTrace();
				}
				entity.imageUri = CandiConstants.URL_AIRCANDI_MEDIA + CandiConstants.S3_BUCKET_IMAGES + "/" + imageKey;
				entity.imageFormat = ImageFormat.Binary.name().toLowerCase();
			}
		}
	}

	protected void insertEntity() {

		final BaseEntity entity = (BaseEntity) mEntity;
		if (findViewById(R.id.txt_title) != null)
			entity.title = ((TextView) findViewById(R.id.txt_title)).getText().toString().trim();
		if (findViewById(R.id.txt_title) != null)
			entity.label = ((TextView) findViewById(R.id.txt_title)).getText().toString().trim();
		if (findViewById(R.id.txt_content) != null)
			entity.description = ((TextView) findViewById(R.id.txt_content)).getText().toString().trim();
		if (findViewById(R.id.cbo_visibility) != null)
			entity.visibility = ((Spinner) findViewById(R.id.cbo_visibility)).getSelectedItemPosition();
		if (findViewById(R.id.txt_password) != null)
			entity.password = ((TextView) findViewById(R.id.txt_password)).getText().toString().trim();

		if (mParentEntityId != 0) {
			entity.parentEntityId = mParentEntityId;
		}
		else {
			entity.parentEntityId = null;
		}
		entity.enabled = true;
		entity.createdDate = DateUtils.nowString();
		Logger.i(CandiConstants.APP_NAME, getClass().getSimpleName(), "Inserting entity: " + entity.title);

		entity.insertAsync(new IQueryListener() {

			@Override
			public void onComplete(String jsonResponse) {

				runOnUiThread(new Runnable() {

					public void run() {
						stopTitlebarProgress();
						ImageUtils.showToastNotification(getApplicationContext(), getString(R.string.post_insert_success_toast), Toast.LENGTH_SHORT);
						Intent intent = new Intent();

						/* We are editing so set the dirty flag */
						intent.putExtra(getString(R.string.EXTRA_BEACON_DIRTY), entity.beaconId);
						intent.putExtra(getString(R.string.EXTRA_RESULT_VERB), Verb.New);

						setResult(Activity.RESULT_FIRST_USER, intent);
						mProcessing = false;
						finish();
						overridePendingTransition(R.anim.hold, R.anim.fade_out_medium);
					}
				});
			}

			@Override
			public void onProxibaseException(ProxibaseException exception) {
				ImageUtils.showToastNotification(getApplicationContext(), getString(R.string.post_insert_failed_toast), Toast.LENGTH_SHORT);
				mProcessing = false;
				exception.printStackTrace();
			}
		});
	}

	protected void updateEntity() {

		final BaseEntity entity = (BaseEntity) mEntity;

		if (findViewById(R.id.txt_title) != null)
			entity.title = ((TextView) findViewById(R.id.txt_title)).getText().toString().trim();
		if (findViewById(R.id.txt_title) != null)
			entity.label = ((TextView) findViewById(R.id.txt_title)).getText().toString().trim();
		if (findViewById(R.id.txt_content) != null)
			entity.description = ((TextView) findViewById(R.id.txt_content)).getText().toString().trim();
		if (findViewById(R.id.cbo_visibility) != null)
			entity.visibility = ((Spinner) findViewById(R.id.cbo_visibility)).getSelectedItemPosition();
		if (findViewById(R.id.txt_password) != null)
			entity.password = ((TextView) findViewById(R.id.txt_password)).getText().toString().trim();

		Logger.i(CandiConstants.APP_NAME, getClass().getSimpleName(), "Updating entity: " + entity.title);
		entity.updateAsync(new IQueryListener() {

			@Override
			public void onComplete(String response) {

				/* Post the processed result back to the UI thread */
				runOnUiThread(new Runnable() {

					public void run() {
						stopTitlebarProgress();
						ImageUtils.showToastNotification(getApplicationContext(), getString(R.string.post_update_success_toast), Toast.LENGTH_SHORT);
						Intent intent = new Intent();
						intent.putExtra(getString(R.string.EXTRA_ENTITY_DIRTY), entity.id);
						intent.putExtra(getString(R.string.EXTRA_RESULT_VERB), Verb.Edit);

						setResult(Activity.RESULT_FIRST_USER, intent);
						mProcessing = false;
						finish();
						overridePendingTransition(R.anim.hold, R.anim.fade_out_medium);
					}
				});
			}

			@Override
			public void onProxibaseException(ProxibaseException exception) {
				exception.printStackTrace();
				mProcessing = false;
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						ImageUtils.showToastNotification(getApplicationContext(), getString(R.string.post_update_failed_toast), Toast.LENGTH_SHORT);
					}
				});
			}
		});

	}

	protected void deleteEntity() {
		/*
		 * TODO: We need to update the service so the recursive entity delete also
		 * deletes any associated resources stored with S3. As currently coded, we will
		 * be orphaning any images associated with child entities.
		 */

		/* If there is an image stored with S3 then delete it */
		BaseEntity entity = (BaseEntity) mEntity;
		if (entity.imageUri != null && !entity.imageUri.equals("") && entity.imageFormat.equals("binary")) {
			String imageKey = entity.imageUri.substring(entity.imageUri.lastIndexOf("/") + 1);
			try {
				deleteImageFromS3(imageKey);
				ImageManager.getInstance().getImageCache().remove(entity.imageUri);
				ImageManager.getInstance().getImageCache().remove(entity.imageUri + ".reflection");
			}
			catch (ProxibaseException exception) {
				ImageUtils.showToastNotification(EntityBaseForm.this, getString(R.string.post_delete_failed_toast), Toast.LENGTH_SHORT);
				exception.printStackTrace();
			}
		}

		/* Delete the entity from the service */
		Bundle parameters = new Bundle();
		parameters.putInt("entityId", entity.id);
		Logger.i(CandiConstants.APP_NAME, getClass().getSimpleName(), "Deleting entity: " + entity.title);

		try {
			ProxibaseService.getInstance().webMethod("DeleteEntityWithChildren", parameters, ResponseFormat.Json, null);
		}
		catch (ProxibaseException exception) {
			ImageUtils.showToastNotification(EntityBaseForm.this, getString(R.string.post_delete_failed_toast), Toast.LENGTH_SHORT);
			exception.printStackTrace();
		}

		stopTitlebarProgress();
		ImageUtils.showToastNotification(EntityBaseForm.this, getString(R.string.post_delete_success_toast), Toast.LENGTH_SHORT);
		Intent intent = new Intent();
		intent.putExtra(getString(R.string.EXTRA_ENTITY_DIRTY), entity.id);
		intent.putExtra(getString(R.string.EXTRA_RESULT_VERB), Verb.Delete);
		setResult(Activity.RESULT_FIRST_USER, intent);
		mProcessing = false;
		finish();
		overridePendingTransition(R.anim.hold, R.anim.fade_out_medium);
	}

	protected void addImageToS3(String imageKey, Bitmap bitmap) throws ProxibaseException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
		byte[] bitmapBytes = outputStream.toByteArray();
		ByteArrayInputStream inputStream = new ByteArrayInputStream(bitmapBytes);
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(bitmapBytes.length);
		metadata.setContentType("image/jpeg");

		try {
			S3.getInstance().putObject(CandiConstants.S3_BUCKET_IMAGES, imageKey, inputStream, metadata);
			S3.getInstance().setObjectAcl(CandiConstants.S3_BUCKET_IMAGES, imageKey, CannedAccessControlList.PublicRead);
		}
		catch (final AmazonServiceException exception) {
			throw new ProxibaseException(exception.getMessage(), ProxiErrorCode.AmazonServiceException, exception);
		}
		catch (final AmazonClientException exception) {
			throw new ProxibaseException(exception.getMessage(), ProxiErrorCode.AmazonClientException, exception);
		}
		finally {
			try {
				outputStream.close();
				inputStream.close();
			}
			catch (IOException exception) {
				throw new ProxibaseException(exception.getMessage(), ProxiErrorCode.IOException, exception);
			}
		}
	}

	protected void deleteImageFromS3(String imageKey) throws ProxibaseException {

		/* If the image is stored with S3 then it will be deleted */
		try {
			S3.getInstance().deleteObject(CandiConstants.S3_BUCKET_IMAGES, imageKey);
		}
		catch (final AmazonServiceException exception) {
			throw new ProxibaseException(exception.getMessage(), ProxiErrorCode.AmazonServiceException, exception);
		}
		catch (final AmazonClientException exception) {
			throw new ProxibaseException(exception.getMessage(), ProxiErrorCode.AmazonClientException, exception);
		}
	}

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	private void setActiveTab(FormTab formTab) {
		if (formTab == FormTab.Content) {
			mTextViewContent.setTextColor(mTextColorFocused);
			mTextViewSettings.setTextColor(mTextColorUnfocused);

			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, mHeightActive);
			params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
			mImageViewContent.setLayoutParams(params);

			params = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, mHeightInactive);
			params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
			mImageViewSettings.setLayoutParams(params);

		}
		else if (formTab == FormTab.Settings) {
			mTextViewContent.setTextColor(mTextColorUnfocused);
			mTextViewSettings.setTextColor(mTextColorFocused);

			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, mHeightActive);
			params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
			mImageViewSettings.setLayoutParams(params);

			params = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, mHeightInactive);
			params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
			mImageViewContent.setLayoutParams(params);

		}
		mViewFlipper.setDisplayedChild(formTab.ordinal());
		mActiveTab = formTab;
	}

	public void onSaveButtonClick(View view) {
		if (!mProcessing) {
			mProcessing = true;
			startTitlebarProgress();
			doSave();
		}
	}

	private void showImageThumbnail(Bitmap bitmap) {
		if (findViewById(R.id.img_public_image) != null) {
			Animation animation = AnimationUtils.loadAnimation(EntityBaseForm.this, R.anim.fade_in_medium);
			animation.setFillEnabled(true);
			animation.setFillAfter(true);
			animation.setStartOffset(500);

			((ImageView) findViewById(R.id.img_public_image)).setImageBitmap(bitmap);
			((ImageView) findViewById(R.id.img_public_image)).startAnimation(animation);
			((ImageView) findViewById(R.id.img_public_image)).setVisibility(View.VISIBLE);
		}
	}

	protected void showAddMediaDialog() {

		runOnUiThread(new Runnable() {

			@Override
			public void run() {

				final CharSequence[] items = { "Select a gallery photo", "Take a new photo", "Use your profile photo" };
				AlertDialog.Builder builder = new AlertDialog.Builder(EntityBaseForm.this);
				builder.setTitle("Select photo...");
				builder.setCancelable(true);
				builder.setOnCancelListener(new OnCancelListener() {

					@Override
					public void onCancel(DialogInterface dialog) {
						mProcessing = false;
					}
				});
				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						mProcessing = false;
					}
				});
				builder.setItems(items, new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int item) {
						if (item == 0) {
							pickPhoto();
							overridePendingTransition(R.anim.fade_in_medium, R.anim.hold);
							mProcessing = false;
							dialog.dismiss();
						}
						else if (item == 1) {
							takePhoto();
							overridePendingTransition(R.anim.fade_in_medium, R.anim.hold);
							mProcessing = false;
							dialog.dismiss();
						}
						else if (item == 2) {
							useProfilePhoto();
							overridePendingTransition(R.anim.fade_in_medium, R.anim.hold);
							mProcessing = false;
							dialog.dismiss();
						}
						else {
							mProcessing = false;
							Toast.makeText(getApplicationContext(), "Not implemented yet.", Toast.LENGTH_SHORT).show();
						}
					}
				});
				AlertDialog alert = builder.create();
				alert.show();
			}
		});
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------
	protected void onDestroy() {

		/* This activity gets destroyed everytime we leave using back or finish(). */
		try {
			BaseEntity entity = (BaseEntity) mEntity;
			if (entity.imageBitmap != null) {
				entity.imageBitmap.recycle();
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
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		/*
		 * Called before onResume. If we are returning from the market app, we
		 * get a zero result code whether the user decided to start an install
		 * or not.
		 */
		if (requestCode == CandiConstants.ACTIVITY_PHOTO_PICK) {
			if (resultCode == Activity.RESULT_OK) {
				if (mPickerTarget == PickerTarget.PublicImage) {
					BaseEntity entity = (BaseEntity) mEntity;

					Uri imageUri = data.getData();
					Bitmap bitmap = null;
					try {
						bitmap = ImageManager.getInstance().loadBitmapFromDevice(imageUri, String.valueOf(CandiConstants.IMAGE_WIDTH_MAX));
					}
					catch (ProxibaseException exception) {
						exception.printStackTrace();
					}
					if (bitmap == null) {
						throw new IllegalStateException("bitmap picked from gallery is null");
					}

					entity.imageUri = null;
					entity.imageBitmap = bitmap;
					updateImage(entity);
				}
			}
		}
		else if (requestCode == CandiConstants.ACTIVITY_PHOTO_MAKE) {
			if (resultCode == Activity.RESULT_OK) {
				if (mPickerTarget == PickerTarget.PublicImage) {
					BaseEntity entity = (BaseEntity) mEntity;

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
							exception.printStackTrace();
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
						exception.printStackTrace();
					}
					if (bitmap == null)
						throw new IllegalStateException("bitmap taken with camera is null");

					entity.imageUri = null;
					entity.imageBitmap = bitmap;
					updateImage(entity);
				}
			}
		}
		mProcessing = false;
	}

	public enum PickerTarget {
		None, PublicImage, Media
	}
}