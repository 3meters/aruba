package com.proxibase.aircandi.activities;

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
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.proxibase.aircandi.core.AircandiException;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.models.BaseEntity;
import com.proxibase.aircandi.utils.DateUtils;
import com.proxibase.aircandi.utils.ImageManager;
import com.proxibase.aircandi.utils.ImageUtils;
import com.proxibase.aircandi.utils.Log;
import com.proxibase.aircandi.utils.S3;
import com.proxibase.aircandi.utils.ImageManager.IImageRequestListener;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest.ImageFormat;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest.ImageShape;
import com.proxibase.sdk.android.proxi.consumer.Beacon.BeaconType;
import com.proxibase.sdk.android.proxi.consumer.EntityProxy.Visibility;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.IQueryListener;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException.ProxiErrorCode;
import com.proxibase.sdk.android.util.ProxiConstants;

public abstract class EntityBase extends AircandiActivity {

	protected Object		mEntity;
	protected boolean		mProcessing		= false;
	protected PickerTarget	mPickerTarget	= PickerTarget.None;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		super.onCreate(savedInstanceState);
	}

	protected void bindEntity() {

		if (mVerb == Verb.New) {
			final BaseEntity entity = (BaseEntity) mEntity;
			entity.beaconId = mBeacon.id;
			entity.signalFence = mBeacon.levelDb - 10.0f;
			entity.createdById = String.valueOf(mUser.id);
			entity.enabled = true;
			entity.visibility = Visibility.Public.ordinal();
			entity.password = null;
			entity.entityType = CandiConstants.TYPE_CANDI_POST;
			entity.imageUri = mUser.imageUri;
			entity.imageFormat = ImageFormat.Binary.name().toLowerCase();

			if (entity.imageUri != null && !entity.imageUri.equals("")) {
				Bitmap bitmap = fetchImage(entity.imageUri, new IImageRequestListener() {

					@Override
					public void onImageReady(Bitmap bitmap) {
						Log.d(CandiConstants.APP_NAME, "Photo", "Image fetched: " + entity.imageUri);
						entity.imageBitmap = bitmap;
						showImageThumbnail(bitmap);
					}

					@Override
					public void onProxibaseException(ProxibaseException exception) {
						if (exception.getErrorCode() == ProxiErrorCode.OperationFailed) {
							Bitmap bitmap = ImageManager.loadBitmapFromAssets("gfx/placeholder3.png");
							entity.imageBitmap = bitmap;
							showImageThumbnail(bitmap);
						}
					}
				});
				if (bitmap != null) {
					entity.imageBitmap = bitmap;
					showImageThumbnail(bitmap);
				}
			}
			entity.parentEntityId = mParentEntityId;
		}
		else if (mVerb == Verb.Edit) {

			final BaseEntity entity = (BaseEntity) mEntity;
			if (entity.imageUri != null && !entity.imageUri.equals("")) {
				Bitmap bitmap = fetchImage(entity.imageUri, new IImageRequestListener() {

					@Override
					public void onImageReady(Bitmap bitmap) {
						if (mEntity != null) {
							Log.d(CandiConstants.APP_NAME, "Photo", "Image fetched: " + entity.imageUri);
							entity.imageBitmap = bitmap;
							showImageThumbnail(bitmap);
						}
					}

					@Override
					public void onProxibaseException(ProxibaseException exception) {
						if (exception.getErrorCode() == ProxiErrorCode.OperationFailed) {
							Bitmap bitmap = ImageManager.loadBitmapFromAssets("gfx/placeholder3.png");
							entity.imageBitmap = bitmap;
							showImageThumbnail(bitmap);
						}
					}
				});
				if (bitmap != null) {
					entity.imageBitmap = bitmap;
					showImageThumbnail(bitmap);
				}
			}
		}
	}

	protected void drawEntity() {

		final BaseEntity entity = (BaseEntity) mEntity;

		if (findViewById(R.id.txt_title) != null)
			((EditText) findViewById(R.id.txt_title)).setText(entity.title);

		if (findViewById(R.id.txt_content) != null)
			((EditText) findViewById(R.id.txt_content)).setText(entity.description);

		if (findViewById(R.id.cbo_visibility) != null)
			((Spinner) findViewById(R.id.cbo_visibility)).setSelection(entity.visibility);

		if (mVerb == Verb.New) {
			if (findViewById(R.id.btn_delete_post) != null)
				((Button) findViewById(R.id.btn_delete_post)).setVisibility(View.GONE);
		}
		if (findViewById(R.id.txt_password) != null)
			((EditText) findViewById(R.id.txt_password)).setText(entity.password);

		updateImage(entity);

		if (findViewById(R.id.img_public_image) != null)
			((ImageView) findViewById(R.id.img_public_image)).requestFocus();
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

	public void onSaveButtonClick(View view) {
		if (!mProcessing) {
			mProcessing = true;
			startTitlebarProgress();
			doSave();
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
		startTitlebarProgress();
		setResult(Activity.RESULT_CANCELED);
		finish();
		overridePendingTransition(R.anim.hold, R.anim.fade_out_medium);
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
				Bitmap bitmap = fetchImage(entity.imageUri, new IImageRequestListener() {

					@Override
					public void onImageReady(Bitmap bitmap) {
						if (mEntity != null) {
							Log.d(CandiConstants.APP_NAME, "Photo", "Image fetched: " + entity.imageUri);
							entity.imageBitmap = bitmap;
							showImageThumbnail(bitmap);
						}
					}

					@Override
					public void onProxibaseException(ProxibaseException exception) {
						if (exception.getErrorCode() == ProxiErrorCode.OperationFailed) {
							Bitmap bitmap = ImageManager.loadBitmapFromAssets("gfx/placeholder3.png");
							entity.imageBitmap = bitmap;
							showImageThumbnail(bitmap);
						}
					}
				});
				if (bitmap != null) {
					entity.imageBitmap = bitmap;
					showImageThumbnail(bitmap);
				}
			}
			else {
				
				/* User doesn't have a valid profile image */
				Bitmap bitmap = ImageManager.loadBitmapFromAssets("gfx/placeholder3.png");
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
				mBeacon.insert();
			}
			catch (ProxibaseException exception) {
				ImageUtils.showToastNotification(EntityBase.this, getString(R.string.post_update_failed_toast), Toast.LENGTH_SHORT);
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
				ImageUtils.showToastNotification(EntityBase.this, getString(R.string.post_update_failed_toast), Toast.LENGTH_SHORT);
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
					ImageUtils.showToastNotification(EntityBase.this, getString(R.string.post_update_failed_toast), Toast.LENGTH_SHORT);
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
			entity.title = ((EditText) findViewById(R.id.txt_title)).getText().toString().trim();
		if (findViewById(R.id.txt_title) != null)
			entity.label = ((EditText) findViewById(R.id.txt_title)).getText().toString().trim();
		if (findViewById(R.id.txt_content) != null)
			entity.description = ((EditText) findViewById(R.id.txt_content)).getText().toString().trim();
		if (findViewById(R.id.cbo_visibility) != null)
			entity.visibility = ((Spinner) findViewById(R.id.cbo_visibility)).getSelectedItemPosition();
		if (findViewById(R.id.txt_password) != null)
			entity.password = ((EditText) findViewById(R.id.txt_password)).getText().toString().trim();

		if (mParentEntityId != 0) {
			entity.parentEntityId = mParentEntityId;
		}
		else {
			entity.parentEntityId = null;
		}
		entity.enabled = true;
		entity.createdDate = DateUtils.nowString();

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
			entity.title = ((EditText) findViewById(R.id.txt_title)).getText().toString().trim();
		if (findViewById(R.id.txt_title) != null)
			entity.label = ((EditText) findViewById(R.id.txt_title)).getText().toString().trim();
		if (findViewById(R.id.txt_content) != null)
			entity.description = ((EditText) findViewById(R.id.txt_content)).getText().toString().trim();
		if (findViewById(R.id.cbo_visibility) != null)
			entity.visibility = ((Spinner) findViewById(R.id.cbo_visibility)).getSelectedItemPosition();
		if (findViewById(R.id.txt_password) != null)
			entity.password = ((EditText) findViewById(R.id.txt_password)).getText().toString().trim();

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
				ImageUtils.showToastNotification(EntityBase.this, getString(R.string.post_delete_failed_toast), Toast.LENGTH_SHORT);
				exception.printStackTrace();
			}
		}

		/* Delete the entity from the service */
		Bundle parameters = new Bundle();
		parameters.putInt("entityId", entity.id);

		try {
			ProxibaseService.getInstance().webMethod("DeleteEntityWithChildren", parameters, ResponseFormat.Json, null);
		}
		catch (ProxibaseException exception) {
			ImageUtils.showToastNotification(EntityBase.this, getString(R.string.post_delete_failed_toast), Toast.LENGTH_SHORT);
			exception.printStackTrace();
		}

		stopTitlebarProgress();
		ImageUtils.showToastNotification(EntityBase.this, getString(R.string.post_delete_success_toast), Toast.LENGTH_SHORT);
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

	protected Bitmap fetchImage(final String imageUri, IImageRequestListener listener) {

		if (ImageManager.getInstance().hasImage(imageUri)) {
			Bitmap bitmap = ImageManager.getInstance().getImage(imageUri);
			return bitmap;

		}
		else {
			ImageRequest imageRequest = new ImageRequest(imageUri, ImageShape.Square, ImageFormat.Binary, CandiConstants.IMAGE_WIDTH_MAX, false, 1,
					this, listener);
			Log.d(CandiConstants.APP_NAME, "Photo", "Fetching Image: " + imageUri);
			try {
				ImageManager.getInstance().fetchImageAsynch(imageRequest);
			}
			catch (AircandiException exception) {
				
				/* TODO: We might have hit the thread limit for asynctasks */
				exception.printStackTrace();
			}
			return null;
		}
	}

	private void showImageThumbnail(Bitmap bitmap) {
		if (findViewById(R.id.img_public_image) != null) {
			Animation animation = AnimationUtils.loadAnimation(EntityBase.this, R.anim.fade_in_medium);
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
				AlertDialog.Builder builder = new AlertDialog.Builder(EntityBase.this);
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
		super.onDestroy();
		
		/* This activity gets destroyed everytime we leave using back or finish(). */

		try {
			BaseEntity entity = (BaseEntity) mEntity;
			if (entity.imageBitmap != null) {
				entity.imageBitmap.recycle();
			}
			mEntity = null;
		}
		catch (Exception exception) {
			exception.printStackTrace();
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
					if (bitmap == null)
						throw new IllegalStateException("bitmap picked from gallery is null");

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