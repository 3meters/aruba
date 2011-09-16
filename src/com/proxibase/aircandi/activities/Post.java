package com.proxibase.aircandi.activities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.proxibase.aircandi.core.AircandiException;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.models.PostEntity;
import com.proxibase.aircandi.models.BaseEntity.SubType;
import com.proxibase.aircandi.utils.DateUtils;
import com.proxibase.aircandi.utils.ImageManager;
import com.proxibase.aircandi.utils.ImageUtils;
import com.proxibase.aircandi.utils.S3;
import com.proxibase.aircandi.utils.ImageManager.IImageReadyListener;
import com.proxibase.aircandi.utils.ImageManager.ImageFormat;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest;
import com.proxibase.sdk.android.proxi.consumer.Beacon.BeaconType;
import com.proxibase.sdk.android.proxi.consumer.EntityProxy.Visibility;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.IQueryListener;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException.ProxiErrorCode;
import com.proxibase.sdk.android.util.ProxiConstants;
import com.proxibase.sdk.android.util.Utilities;

public class Post extends AircandiActivity {

	private PostEntity		mPostEntity;
	private boolean			mProcessing		= false;
	private PickerTarget	mPickerTarget	= PickerTarget.None;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		super.onCreate(savedInstanceState);

		bindEntity();
		drawEntity();
	}

	public void bindEntity() {

		if (mVerb == Verb.New) {
			mPostEntity = new PostEntity();
			mPostEntity.beaconId = mBeacon.id;
			mPostEntity.signalFence = mBeacon.levelDb - 10.0f;
			mPostEntity.createdById = String.valueOf(mUser.id);
			mPostEntity.enabled = true;
			mPostEntity.locked = false;
			mPostEntity.visibility = Visibility.Public.ordinal();
			mPostEntity.password = null;
			mPostEntity.entityType = CandiConstants.TYPE_CANDI_POST;
			mPostEntity.imageUri = mUser.imageUri;
			mPostEntity.imageFormat = ImageManager.ImageFormat.Binary.name().toLowerCase();

			if (mPostEntity.imageUri != null && !mPostEntity.imageUri.equals("")) {
				Bitmap bitmap = fetchImage(mPostEntity.imageUri, new IImageReadyListener() {

					@Override
					public void onImageReady(Bitmap bitmap) {
						Utilities.Log(CandiConstants.APP_NAME, "Photo", "Image fetched: " + mPostEntity.imageUri);
						mPostEntity.imageBitmap = bitmap;
						showImageThumbnail(bitmap);
					}

					@Override
					public void onProxibaseException(ProxibaseException exception) {
						if (exception.getErrorCode() == ProxiErrorCode.OperationFailed) {
							Bitmap bitmap = ImageManager.loadBitmapFromAssets("gfx/placeholder3.png");
							mPostEntity.imageBitmap = bitmap;
							showImageThumbnail(bitmap);
						}
					}
				});
				if (bitmap != null) {
					mPostEntity.imageBitmap = bitmap;
					showImageThumbnail(bitmap);
				}
			}

			if (mSubType == SubType.Topic) {
				mPostEntity.parentEntityId = mParentEntityId;
			}
		}
		else if (mVerb == Verb.Edit) {
			String jsonResponse = null;
			try {
				jsonResponse = (String) ProxibaseService.getInstance().select(mEntityProxy.getEntryUri(), ResponseFormat.Json);
			}
			catch (ProxibaseException exception) {
				exception.printStackTrace();
			}

			mPostEntity = (PostEntity) ProxibaseService.convertJsonToObject(jsonResponse, PostEntity.class);

			if (mPostEntity.imageUri != null && !mPostEntity.imageUri.equals("")) {
				Bitmap bitmap = fetchImage(mPostEntity.imageUri, new IImageReadyListener() {

					@Override
					public void onImageReady(Bitmap bitmap) {
						if (mPostEntity != null) {
							Utilities.Log(CandiConstants.APP_NAME, "Photo", "Image fetched: " + mPostEntity.imageUri);
							mPostEntity.imageBitmap = bitmap;
							showImageThumbnail(bitmap);
						}
					}

					@Override
					public void onProxibaseException(ProxibaseException exception) {
						if (exception.getErrorCode() == ProxiErrorCode.OperationFailed) {
							Bitmap bitmap = ImageManager.loadBitmapFromAssets("gfx/placeholder3.png");
							mPostEntity.imageBitmap = bitmap;
							showImageThumbnail(bitmap);
						}
					}
				});
				if (bitmap != null) {
					mPostEntity.imageBitmap = bitmap;
					showImageThumbnail(bitmap);
				}
			}
			if (mPostEntity.mediaUri != null && !mPostEntity.mediaUri.equals("")) {
				((Button) findViewById(R.id.btn_clear_media)).setEnabled(false);
				Bitmap bitmap = fetchImage(mPostEntity.mediaUri, new IImageReadyListener() {

					@Override
					public void onImageReady(Bitmap bitmap) {
						/*
						 * We can get this callback even when activity has finished.
						 * TODO: Cancel all active tasks in onDestroy()
						 */
						if (mPostEntity != null) {
							Utilities.Log(CandiConstants.APP_NAME, "Photo", "Image fetched: " + mPostEntity.mediaUri);
							mPostEntity.mediaBitmap = bitmap;
							showMediaThumbnail(bitmap);
							((Button) findViewById(R.id.btn_clear_media)).setEnabled(true);
						}
					}

					@Override
					public void onProxibaseException(ProxibaseException exception) {
						if (exception.getErrorCode() == ProxiErrorCode.OperationFailed) {
							Bitmap bitmap = ImageManager.loadBitmapFromAssets("gfx/placeholder3.png");
							mPostEntity.mediaBitmap = bitmap;
							showMediaThumbnail(bitmap);
							((Button) findViewById(R.id.btn_clear_media)).setEnabled(true);
						}
					}
				});
				if (bitmap != null) {
					mPostEntity.mediaBitmap = bitmap;
					showMediaThumbnail(bitmap);
					((Button) findViewById(R.id.btn_clear_media)).setEnabled(true);
				}
			}
		}
	}

	private void drawEntity() {

		((EditText) findViewById(R.id.txt_title)).setText(mPostEntity.title);
		((EditText) findViewById(R.id.txt_content)).setText(mPostEntity.description);
		((Spinner) findViewById(R.id.cbo_visibility)).setSelection(mPostEntity.visibility);
		if (mVerb == Verb.New) {
			((Button) findViewById(R.id.btn_delete_post)).setVisibility(View.GONE);
		}
		((EditText) findViewById(R.id.txt_password)).setText(mPostEntity.password);

		if (mSubType == SubType.Topic) {
			((CheckBox) findViewById(R.id.chk_locked)).setVisibility(View.VISIBLE);
			((CheckBox) findViewById(R.id.chk_locked)).setChecked(mPostEntity.locked);
		}
		if (mSubType == SubType.Comment) {
			((CheckBox) findViewById(R.id.chk_locked)).setVisibility(View.GONE);
			((CheckBox) findViewById(R.id.chk_locked)).setChecked(false);
		}

		if (mPostEntity.imageBitmap != null) {
			((ImageView) findViewById(R.id.img_public_image)).setImageBitmap(mPostEntity.imageBitmap);
			((ImageView) findViewById(R.id.img_public_image)).setVisibility(View.VISIBLE);
		}
		else {
			((ImageView) findViewById(R.id.img_public_image)).setImageBitmap(null);
			((ImageView) findViewById(R.id.img_public_image)).setAnimation(null);
			((ImageView) findViewById(R.id.img_public_image)).setVisibility(View.GONE);
		}

		if (mPostEntity.mediaBitmap != null) {
			((ImageView) findViewById(R.id.img_media)).setImageBitmap(mPostEntity.mediaBitmap);
			((ImageView) findViewById(R.id.img_media)).setVisibility(View.VISIBLE);
			((Button) findViewById(R.id.btn_add_media)).setVisibility(View.GONE);
			((Button) findViewById(R.id.btn_clear_media)).setVisibility(View.VISIBLE);
		}
		else {
			((ImageView) findViewById(R.id.img_media)).setImageBitmap(null);
			((ImageView) findViewById(R.id.img_media)).setAnimation(null);
			((ImageView) findViewById(R.id.img_media)).setVisibility(View.GONE);
			((Button) findViewById(R.id.btn_add_media)).setVisibility(View.VISIBLE);
			((Button) findViewById(R.id.btn_clear_media)).setVisibility(View.GONE);
		}
		((ImageView) findViewById(R.id.img_public_image)).requestFocus();
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

	public void onAddMediaButtonClick(View view) {
		if (!mProcessing) {
			mProcessing = true;
			mPickerTarget = PickerTarget.Media;
			showAddMediaDialog();
		}
	}

	public void onClearMediaButtonClick(View view) {
		if (!mProcessing) {
			mProcessing = true;
			clearMedia();
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
			takePictureFromCameraIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File("/sdcard/tmp")));
		}
		else
			takePictureFromCameraIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,
					android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(takePictureFromCameraIntent, CandiConstants.ACTIVITY_PHOTO_MAKE);
	}

	public void useProfilePhoto() {

		if (mPickerTarget == PickerTarget.PublicImage) {
			mPostEntity.imageUri = mUser.imageUri;
			mPostEntity.imageFormat = ImageManager.ImageFormat.Binary.name().toLowerCase();

			if (mPostEntity.imageUri != null && !mPostEntity.imageUri.equals("")) {
				Bitmap bitmap = fetchImage(mPostEntity.imageUri, new IImageReadyListener() {

					@Override
					public void onImageReady(Bitmap bitmap) {
						if (mPostEntity != null) {
							Utilities.Log(CandiConstants.APP_NAME, "Photo", "Image fetched: " + mPostEntity.imageUri);
							mPostEntity.imageBitmap = bitmap;
							showImageThumbnail(bitmap);
						}
					}

					@Override
					public void onProxibaseException(ProxibaseException exception) {
						if (exception.getErrorCode() == ProxiErrorCode.OperationFailed) {
							Bitmap bitmap = ImageManager.loadBitmapFromAssets("gfx/placeholder3.png");
							mPostEntity.imageBitmap = bitmap;
							showImageThumbnail(bitmap);
						}
					}
				});
				if (bitmap != null) {
					mPostEntity.imageBitmap = bitmap;
					showImageThumbnail(bitmap);
				}
			}
			else {
				// User doesn't have a valid profile image
				Bitmap bitmap = ImageManager.loadBitmapFromAssets("gfx/placeholder3.png");
				mPostEntity.imageBitmap = bitmap;
				showImageThumbnail(bitmap);
			}
		}
		else if (mPickerTarget == PickerTarget.Media) {
			mPostEntity.mediaUri = mUser.imageUri;
			mPostEntity.mediaFormat = ImageManager.ImageFormat.Binary.name().toLowerCase();

			if (mPostEntity.mediaUri != null && !mPostEntity.mediaUri.equals("")) {
				Bitmap bitmap = fetchImage(mPostEntity.mediaUri, new IImageReadyListener() {

					@Override
					public void onImageReady(Bitmap bitmap) {
						if (mPostEntity != null) {
							Utilities.Log(CandiConstants.APP_NAME, "Photo", "Image fetched: " + mPostEntity.mediaUri);
							mPostEntity.mediaBitmap = bitmap;
							showMediaThumbnail(bitmap);
						}
					}

					@Override
					public void onProxibaseException(ProxibaseException exception) {
						if (exception.getErrorCode() == ProxiErrorCode.OperationFailed) {
							Bitmap bitmap = ImageManager.loadBitmapFromAssets("gfx/placeholder3.png");
							mPostEntity.mediaBitmap = bitmap;
							showMediaThumbnail(bitmap);
						}
					}
				});
				if (bitmap != null) {
					mPostEntity.mediaBitmap = bitmap;
					showMediaThumbnail(bitmap);
				}
			}
			else {
				// User doesn't have a valid profile image
				Bitmap bitmap = ImageManager.loadBitmapFromAssets("gfx/placeholder3.png");
				mPostEntity.mediaBitmap = bitmap;
				showMediaThumbnail(bitmap);
			}
		}
		mProcessing = false;
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	public void doSave() {

		// Insert beacon if it isn't already registered
		if (mBeacon != null && mBeacon.isUnregistered) {
			mBeacon.registeredById = String.valueOf(mUser.id);
			mBeacon.beaconType = BeaconType.Fixed.name().toLowerCase();
			mBeacon.beaconSetId = ProxiConstants.BEACONSET_WORLD;
			try {
				mBeacon.insert();
			}
			catch (ProxibaseException exception) {
				ImageUtils.showToastNotification(Post.this, getString(R.string.post_update_failed_toast), Toast.LENGTH_SHORT);
				exception.printStackTrace();
			}
		}

		// Delete or upload images to S3 as needed.
		updateImages();

		if (mVerb == Verb.New) {
			insertEntity();
		}
		else if (mVerb == Verb.Edit) {
			updateEntity();
		}
	}

	private void updateImages() {
		/*
		 * If we have imageUri but no imageBitmap then image was cleared and save should null imageUri and delete bitmap
		 * from service.
		 */
		if (mPostEntity.imageUri != null && mPostEntity.imageBitmap == null) {
			try {
				deleteImageFromS3(mPostEntity.imageUri.substring(mPostEntity.imageUri.lastIndexOf("/") + 1));
				ImageManager.getInstance().getImageCache().remove(mPostEntity.imageUri);
				ImageManager.getInstance().getImageCache().remove(mPostEntity.imageUri + ".reflection");
				mPostEntity.imageUri = null;
			}
			catch (ProxibaseException exception) {
				ImageUtils.showToastNotification(Post.this, getString(R.string.post_update_failed_toast), Toast.LENGTH_SHORT);
				exception.printStackTrace();
			}
		}
		/*
		 * If we have no imageUri and do have imageBitmap then new image has been picked and we should save the image
		 * to the service and set imageUri.
		 * TODO: If update then we might have orphaned a photo in S3
		 */
		else if (mPostEntity.imageUri == null && mPostEntity.imageBitmap != null) {
			/*
			 */
			if (mPostEntity.imageBitmap != null) {
				String imageKey = String.valueOf(mUser.id) + "_" + String.valueOf(DateUtils.nowString(DateUtils.DATE_NOW_FORMAT_FILENAME)) + ".jpg";
				try {
					addImageToS3(imageKey, mPostEntity.imageBitmap);
				}
				catch (ProxibaseException exception) {
					ImageUtils.showToastNotification(Post.this, getString(R.string.post_update_failed_toast), Toast.LENGTH_SHORT);
					exception.printStackTrace();
				}
				mPostEntity.imageUri = CandiConstants.URL_AIRCANDI_MEDIA + CandiConstants.S3_BUCKET_IMAGES + "/" + imageKey;
				mPostEntity.imageFormat = ImageFormat.Binary.name().toLowerCase();
			}
		}

		/*
		 * Media
		 */

		if (mPostEntity.mediaUri != null && mPostEntity.mediaBitmap == null) {
			try {
				deleteImageFromS3(mPostEntity.mediaUri.substring(mPostEntity.mediaUri.lastIndexOf("/") + 1));
				ImageManager.getInstance().getImageCache().remove(mPostEntity.mediaUri);
				ImageManager.getInstance().getImageCache().remove(mPostEntity.mediaUri + ".reflection");
				mPostEntity.mediaUri = null;
			}
			catch (ProxibaseException exception) {
				ImageUtils.showToastNotification(Post.this, getString(R.string.post_update_failed_toast), Toast.LENGTH_SHORT);
				exception.printStackTrace();
			}
		}
		else if (mPostEntity.mediaUri == null && mPostEntity.mediaBitmap != null) {
			/*
			 */
			if (mPostEntity.mediaBitmap != null) {
				String imageKey = String.valueOf(mUser.id) + "_" + String.valueOf(DateUtils.nowString(DateUtils.DATE_NOW_FORMAT_FILENAME)) + ".jpg";
				try {
					addImageToS3(imageKey, mPostEntity.mediaBitmap);
				}
				catch (ProxibaseException exception) {
					ImageUtils.showToastNotification(Post.this, getString(R.string.post_update_failed_toast), Toast.LENGTH_SHORT);
					exception.printStackTrace();
				}
				mPostEntity.mediaUri = CandiConstants.URL_AIRCANDI_MEDIA + CandiConstants.S3_BUCKET_IMAGES + "/" + imageKey;
				mPostEntity.mediaFormat = ImageFormat.Binary.name().toLowerCase();
			}
		}
	}

	private void insertEntity() {

		mPostEntity.title = ((EditText) findViewById(R.id.txt_title)).getText().toString().trim();
		mPostEntity.label = ((EditText) findViewById(R.id.txt_title)).getText().toString().trim();
		mPostEntity.description = ((EditText) findViewById(R.id.txt_content)).getText().toString().trim();
		mPostEntity.visibility = ((Spinner) findViewById(R.id.cbo_visibility)).getSelectedItemPosition();
		mPostEntity.password = ((EditText) findViewById(R.id.txt_password)).getText().toString().trim();

		if (mSubType == SubType.Topic) {
			mPostEntity.locked = ((CheckBox) findViewById(R.id.chk_locked)).isChecked();
		}
		if (mParentEntityId != 0) {
			mPostEntity.parentEntityId = mParentEntityId;
		}
		else {
			mPostEntity.parentEntityId = null;
		}
		mPostEntity.enabled = true;
		mPostEntity.createdDate = DateUtils.nowString();

		mPostEntity.insertAsync(new IQueryListener() {

			@Override
			public void onComplete(String jsonResponse) {

				runOnUiThread(new Runnable() {

					public void run() {
						stopTitlebarProgress();
						ImageUtils.showToastNotification(Post.this, getString(R.string.post_insert_success_toast), Toast.LENGTH_SHORT);
						Intent intent = new Intent();

						// We are editing so set the dirty flag
						if (mSubType == SubType.Topic)
							intent.putExtra(getString(R.string.EXTRA_BEACON_DIRTY), mPostEntity.beaconId);
						else if (mSubType == SubType.Comment)
							intent.putExtra(getString(R.string.EXTRA_ENTITY_DIRTY), mPostEntity.parentEntityId);
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
				ImageUtils.showToastNotification(Post.this, getString(R.string.post_insert_failed_toast), Toast.LENGTH_SHORT);
				mProcessing = false;
				exception.printStackTrace();
			}
		});
	}

	private void updateEntity() {

		mPostEntity.title = ((EditText) findViewById(R.id.txt_title)).getText().toString().trim();
		mPostEntity.label = ((EditText) findViewById(R.id.txt_title)).getText().toString().trim();
		mPostEntity.description = ((EditText) findViewById(R.id.txt_content)).getText().toString().trim();
		mPostEntity.visibility = ((Spinner) findViewById(R.id.cbo_visibility)).getSelectedItemPosition();
		mPostEntity.password = ((EditText) findViewById(R.id.txt_password)).getText().toString().trim();
		if (mSubType == SubType.Topic) {
			mPostEntity.locked = ((CheckBox) findViewById(R.id.chk_locked)).isChecked();
		}

		mPostEntity.updateAsync(new IQueryListener() {

			@Override
			public void onComplete(String response) {
				// Post the processed result back to the UI thread
				Post.this.runOnUiThread(new Runnable() {

					public void run() {
						stopTitlebarProgress();
						ImageUtils.showToastNotification(Post.this, getString(R.string.post_update_success_toast), Toast.LENGTH_SHORT);
						Intent intent = new Intent();
						intent.putExtra(getString(R.string.EXTRA_ENTITY_DIRTY), mPostEntity.id);
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
				Post.this.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						ImageUtils.showToastNotification(Post.this, getString(R.string.post_update_failed_toast), Toast.LENGTH_SHORT);
					}
				});

			}
		});
	}

	public void deleteEntity() {
		/*
		 * TODO: We need to update the service so the recursive entity delete also
		 * deletes any associated resources stored with S3. As currently coded, we will
		 * be orphaning any images associated with child entities.
		 */

		// If there is an image stored with S3 then delete it
		if (mPostEntity.imageUri != null && !mPostEntity.imageUri.equals("") && mPostEntity.imageFormat.equals("binary")) {
			String imageKey = mPostEntity.imageUri.substring(mPostEntity.imageUri.lastIndexOf("/") + 1);
			try {
				deleteImageFromS3(imageKey);
				ImageManager.getInstance().getImageCache().remove(mPostEntity.imageUri);
				ImageManager.getInstance().getImageCache().remove(mPostEntity.imageUri + ".reflection");
			}
			catch (ProxibaseException exception) {
				ImageUtils.showToastNotification(Post.this, getString(R.string.post_delete_failed_toast), Toast.LENGTH_SHORT);
				exception.printStackTrace();
			}
		}

		if (mPostEntity.mediaUri != null && !mPostEntity.mediaUri.equals("") && mPostEntity.mediaFormat.equals("binary")) {
			String imageKey = mPostEntity.mediaUri.substring(mPostEntity.mediaUri.lastIndexOf("/") + 1);
			try {
				deleteImageFromS3(imageKey);
				ImageManager.getInstance().getImageCache().remove(mPostEntity.mediaUri);
				ImageManager.getInstance().getImageCache().remove(mPostEntity.mediaUri + ".reflection");
			}
			catch (ProxibaseException exception) {
				ImageUtils.showToastNotification(Post.this, getString(R.string.post_delete_failed_toast), Toast.LENGTH_SHORT);
				exception.printStackTrace();
			}
		}

		// Delete the entity from the service
		Bundle parameters = new Bundle();
		parameters.putInt("entityId", mPostEntity.id);

		try {
			ProxibaseService.getInstance().webMethod("DeleteEntityWithChildren", parameters, ResponseFormat.Json, null);
		}
		catch (ProxibaseException exception) {
			ImageUtils.showToastNotification(Post.this, getString(R.string.post_delete_failed_toast), Toast.LENGTH_SHORT);
			exception.printStackTrace();
		}

		stopTitlebarProgress();
		ImageUtils.showToastNotification(Post.this, getString(R.string.post_delete_success_toast), Toast.LENGTH_SHORT);
		Intent intent = new Intent();
		intent.putExtra(getString(R.string.EXTRA_ENTITY_DIRTY), mPostEntity.id);
		intent.putExtra(getString(R.string.EXTRA_RESULT_VERB), Verb.Delete);
		setResult(Activity.RESULT_FIRST_USER, intent);
		mProcessing = false;
		finish();
		overridePendingTransition(R.anim.hold, R.anim.fade_out_medium);
	}

	private void addImageToS3(String imageKey, Bitmap bitmap) throws ProxibaseException {
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

	private void deleteImageFromS3(String imageKey) throws ProxibaseException {
		// If the image is stored with S3 then it will be deleted
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

	private void clearMedia() {
		/*
		 * The bitmap goes away from view but we still keep the mediaUri so if the user
		 * cancels, the entity will still retain it's previous image. Saving will perform
		 * the real delete of the image and set mediaUri to null.
		 */
		mPostEntity.mediaBitmap.recycle();
		mPostEntity.mediaBitmap = null;
		drawEntity();
		mProcessing = false;
	}

	private Bitmap fetchImage(final String imageUri, IImageReadyListener listener) {

		if (ImageManager.getInstance().hasImage(imageUri)) {
			Bitmap bitmap = ImageManager.getInstance().getImage(imageUri);
			return bitmap;

		}
		else {
			ImageRequest imageRequest = ImageManager.imageRequestFactory(imageUri, ImageFormat.Binary, "square", CandiConstants.IMAGE_WIDTH_MAX,
					false, listener);
			Utilities.Log(CandiConstants.APP_NAME, "Photo", "Fetching Image: " + imageUri);
			try {
				ImageManager.getInstance().fetchImageAsynch(imageRequest);
			}
			catch (AircandiException exception) {
				// TODO: We might have hit the thread limit for asynctasks
				exception.printStackTrace();
			}
			return null;
		}
	}

	private void showImageThumbnail(Bitmap bitmap) {
		Animation animation = AnimationUtils.loadAnimation(Post.this, R.anim.fade_in_medium);
		animation.setFillEnabled(true);
		animation.setFillAfter(true);
		animation.setStartOffset(500);

		((ImageView) findViewById(R.id.img_public_image)).setImageBitmap(bitmap);
		((ImageView) findViewById(R.id.img_public_image)).startAnimation(animation);
		((ImageView) findViewById(R.id.img_public_image)).setVisibility(View.VISIBLE);
	}

	private void showMediaThumbnail(Bitmap bitmap) {
		Animation animation = AnimationUtils.loadAnimation(Post.this, R.anim.fade_in_medium);
		animation.setFillEnabled(true);
		animation.setFillAfter(true);
		animation.setStartOffset(500);

		((ImageView) findViewById(R.id.img_media)).setImageBitmap(bitmap);
		((ImageView) findViewById(R.id.img_media)).startAnimation(animation);
		((ImageView) findViewById(R.id.img_media)).setVisibility(View.VISIBLE);
		((Button) findViewById(R.id.btn_add_media)).setVisibility(View.GONE);
		((Button) findViewById(R.id.btn_clear_media)).setVisibility(View.VISIBLE);
	}

	private void showAddMediaDialog() {

		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				final Dialog dialog = new Dialog(Post.this, R.style.aircandi_theme_dialog);
				final RelativeLayout dialogLayout = (RelativeLayout) getLayoutInflater().inflate(R.layout.temp_dialog_add_photo, null);
				dialog.setContentView(dialogLayout, new FrameLayout.LayoutParams(
						dialog.getWindow().getWindowManager().getDefaultDisplay().getWidth() - 40, LayoutParams.FILL_PARENT, Gravity.CENTER));
				dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
				dialog.getWindow().setBackgroundDrawable(getResources().getDrawable(R.drawable.dialog_bg));

				dialog.setOnDismissListener(new OnDismissListener() {

					@Override
					public void onDismiss(DialogInterface dialog) {
						mProcessing = false;
					}
				});

				((Button) dialogLayout.findViewById(R.id.btn_select_photo)).setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						pickPhoto();
						overridePendingTransition(R.anim.fade_in_medium, R.anim.hold);
						dialog.dismiss();
					}
				});
				((Button) dialogLayout.findViewById(R.id.btn_take_photo)).setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						takePhoto();
						overridePendingTransition(R.anim.fade_in_medium, R.anim.hold);
						dialog.dismiss();
					}
				});
				((Button) dialogLayout.findViewById(R.id.btn_use_profile_photo)).setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						useProfilePhoto();
						overridePendingTransition(R.anim.fade_in_medium, R.anim.hold);
						dialog.dismiss();
					}
				});
				dialog.show();

			}
		});
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------
	protected void onDestroy() {
		super.onDestroy();
		/*
		 * This activity gets destroyed everytime we leave using back or finish().
		 */

		try {
			if (mPostEntity.imageBitmap != null) {
				mPostEntity.imageBitmap.recycle();
			}
			if (mPostEntity.mediaBitmap != null) {
				mPostEntity.mediaBitmap.recycle();
			}
			mPostEntity = null;
		}
		catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	@Override
	protected int getLayoutID() {
		return R.layout.post;
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
				if (mPickerTarget == PickerTarget.PublicImage) {
					mPostEntity.imageUri = null;
					mPostEntity.imageBitmap = bitmap;
					drawEntity();
				}
				else if (mPickerTarget == PickerTarget.Media) {
					mPostEntity.mediaUri = null;
					mPostEntity.mediaBitmap = bitmap;
					drawEntity();
				}
			}
		}
		else if (requestCode == CandiConstants.ACTIVITY_PHOTO_MAKE) {
			if (resultCode == Activity.RESULT_OK) {
				Uri imageUri = null;
				if (ImageManager.getInstance().hasImageCaptureBug()) {
					File imageFile = new File("/sdcard/tmp");
					try {
						imageUri = Uri.parse(android.provider.MediaStore.Images.Media.insertImage(getContentResolver(), imageFile.getAbsolutePath(),
								null, null));
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
				if (mPickerTarget == PickerTarget.PublicImage) {
					mPostEntity.imageUri = null;
					mPostEntity.imageBitmap = bitmap;
					drawEntity();
				}
				else if (mPickerTarget == PickerTarget.Media) {
					mPostEntity.mediaUri = null;
					mPostEntity.mediaBitmap = bitmap;
					drawEntity();
				}
			}
		}
		mProcessing = false;
	}

	public enum PickerTarget {
		None, PublicImage, Media
	}
}