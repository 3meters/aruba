package com.proxibase.aircandi;

import java.io.File;
import java.io.FileNotFoundException;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Toast;

import com.proxibase.aircandi.R;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.models.PostEntity;
import com.proxibase.aircandi.utils.DateUtils;
import com.proxibase.aircandi.utils.ImageManager;
import com.proxibase.aircandi.utils.ImageUtils;
import com.proxibase.aircandi.utils.Logger;
import com.proxibase.aircandi.utils.ImageManager.IImageRequestListener;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest.ImageFormat;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException.ProxiErrorCode;

public class PostForm extends EntityBase {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		bindEntity();
		drawEntity();
	}

	@Override
	protected void bindEntity() {

		/* We handle all the elements that are different than the base entity. */
		if (mCommand.verb.equals("new")) {
			mEntity = new PostEntity();
		}
		else if (mCommand.verb.equals("edit")) {
			String jsonResponse = null;
			try {
				jsonResponse = (String) ProxibaseService.getInstance().select(mEntityProxy.getEntryUri(), ResponseFormat.Json);
			}
			catch (ProxibaseException exception) {
				exception.printStackTrace();
			}

			mEntity = (PostEntity) ProxibaseService.convertJsonToObject(jsonResponse, PostEntity.class, GsonType.ProxibaseService);
		}

		super.bindEntity();

		final PostEntity entity = (PostEntity) mEntity;

		if (mCommand.verb.equals("new")) {
			entity.entityType = CandiConstants.TYPE_CANDI_POST;
		}
		else if (mCommand.verb.equals("edit")) {
			if (entity.mediaUri != null && !entity.mediaUri.equals("")) {
				((Button) findViewById(R.id.btn_clear_media)).setEnabled(false);
				Bitmap bitmap = fetchImage(entity.mediaUri, new IImageRequestListener() {

					@Override
					public void onImageReady(Bitmap bitmap) {
						/*
						 * We can get this callback even when activity has finished.
						 * TODO: Cancel all active tasks in onDestroy()
						 */
						if (mEntity != null) {
							Logger.d(CandiConstants.APP_NAME, "Photo", "Image fetched: " + entity.mediaUri);
							entity.mediaBitmap = bitmap;
							showMediaThumbnail(bitmap);
							((Button) findViewById(R.id.btn_clear_media)).setEnabled(true);
						}
					}

					@Override
					public void onProxibaseException(ProxibaseException exception) {
						if (exception.getErrorCode() == ProxiErrorCode.OperationFailed) {
							Bitmap bitmap = ImageManager.loadBitmapFromAssets("gfx/placeholder3.png");
							entity.mediaBitmap = bitmap;
							showMediaThumbnail(bitmap);
							((Button) findViewById(R.id.btn_clear_media)).setEnabled(true);
						}
					}

					@Override
					public boolean onProgressChanged(int progress) {
						// TODO Auto-generated method stub
						return false;
					}
				});
				if (bitmap != null) {
					entity.mediaBitmap = bitmap;
					showMediaThumbnail(bitmap);
					((Button) findViewById(R.id.btn_clear_media)).setEnabled(true);
				}
			}
		}
	}

	@Override
	protected void drawEntity() {
		super.drawEntity();

		final PostEntity entity = (PostEntity) mEntity;

		if (findViewById(R.id.chk_locked) != null) {
			((CheckBox) findViewById(R.id.chk_locked)).setVisibility(View.VISIBLE);
			((CheckBox) findViewById(R.id.chk_locked)).setChecked(entity.locked);
		}
		
		if (entity.mediaBitmap != null) {
			((ImageView) findViewById(R.id.img_media)).setImageBitmap(entity.mediaBitmap);
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
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

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

	@Override
	protected void useProfilePhoto() {
		super.useProfilePhoto();

		if (mPickerTarget == PickerTarget.Media) {

			final PostEntity entity = (PostEntity) mEntity;
			entity.mediaUri = mUser.imageUri;
			entity.mediaFormat = ImageFormat.Binary.name().toLowerCase();

			if (entity.mediaUri != null && !entity.mediaUri.equals("")) {
				Bitmap bitmap = fetchImage(entity.mediaUri, new IImageRequestListener() {

					@Override
					public void onImageReady(Bitmap bitmap) {
						if (mEntity != null) {
							Logger.d(CandiConstants.APP_NAME, "Photo", "Image fetched: " + entity.mediaUri);
							entity.mediaBitmap = bitmap;
							showMediaThumbnail(bitmap);
						}
					}

					@Override
					public void onProxibaseException(ProxibaseException exception) {
						if (exception.getErrorCode() == ProxiErrorCode.OperationFailed) {
							Bitmap bitmap = ImageManager.loadBitmapFromAssets("gfx/placeholder3.png");
							entity.mediaBitmap = bitmap;
							showMediaThumbnail(bitmap);
						}
					}

					@Override
					public boolean onProgressChanged(int progress) {
						// TODO Auto-generated method stub
						return false;
					}
				});
				if (bitmap != null) {
					entity.mediaBitmap = bitmap;
					showMediaThumbnail(bitmap);
				}
			}
			else {

				/* User doesn't have a valid profile image */
				Bitmap bitmap = ImageManager.loadBitmapFromAssets("gfx/placeholder3.png");
				entity.mediaBitmap = bitmap;
				showMediaThumbnail(bitmap);
			}
			mProcessing = false;
		}
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected void doSave() {
		super.doSave();

		/* Delete or upload images to S3 as needed. */
		updateImages();

		if (mCommand.verb.equals("new")) {
			insertEntity();
		}
		else if (mCommand.verb.equals("edit")) {
			updateEntity();
		}
	}

	@Override
	protected void updateImages() {
		super.updateImages();

		PostEntity entity = (PostEntity) mEntity;

		if (entity.mediaUri != null && entity.mediaBitmap == null) {
			try {
				deleteImageFromS3(entity.mediaUri.substring(entity.mediaUri.lastIndexOf("/") + 1));
				ImageManager.getInstance().getImageCache().remove(entity.mediaUri);
				ImageManager.getInstance().getImageCache().remove(entity.mediaUri + ".reflection");
				entity.mediaUri = null;
			}
			catch (ProxibaseException exception) {
				ImageUtils.showToastNotification(PostForm.this, getString(R.string.post_update_failed_toast), Toast.LENGTH_SHORT);
				exception.printStackTrace();
			}
		}
		else if (entity.mediaUri == null && entity.mediaBitmap != null) {
			/*
			 */
			if (entity.mediaBitmap != null) {
				String imageKey = String.valueOf(mUser.id) + "_" + String.valueOf(DateUtils.nowString(DateUtils.DATE_NOW_FORMAT_FILENAME)) + ".jpg";
				try {
					addImageToS3(imageKey, entity.mediaBitmap);
				}
				catch (ProxibaseException exception) {
					ImageUtils.showToastNotification(PostForm.this, getString(R.string.post_update_failed_toast), Toast.LENGTH_SHORT);
					exception.printStackTrace();
				}
				entity.mediaUri = CandiConstants.URL_AIRCANDI_MEDIA + CandiConstants.S3_BUCKET_IMAGES + "/" + imageKey;
				entity.mediaFormat = ImageFormat.Binary.name().toLowerCase();
			}
		}
	}

	@Override
	protected void insertEntity() {
		final PostEntity entity = (PostEntity) mEntity;
		entity.locked = ((CheckBox) findViewById(R.id.chk_locked)).isChecked();
		super.insertEntity();

	}

	@Override
	protected void updateEntity() {
		final PostEntity entity = (PostEntity) mEntity;
		entity.locked = ((CheckBox) findViewById(R.id.chk_locked)).isChecked();
		super.updateEntity();
	}

	@Override
	protected void deleteEntity() {
		/*
		 * TODO: We need to update the service so the recursive entity delete also
		 * deletes any associated resources stored with S3. As currently coded, we will
		 * be orphaning any images associated with child entities.
		 */

		/* If there is an image stored with S3 then delete it */
		final PostEntity entity = (PostEntity) mEntity;
		if (entity.mediaUri != null && !entity.mediaUri.equals("") && entity.mediaFormat.equals("binary")) {
			String imageKey = entity.mediaUri.substring(entity.mediaUri.lastIndexOf("/") + 1);
			try {
				deleteImageFromS3(imageKey);
				ImageManager.getInstance().getImageCache().remove(entity.mediaUri);
				ImageManager.getInstance().getImageCache().remove(entity.mediaUri + ".reflection");
			}
			catch (ProxibaseException exception) {
				ImageUtils.showToastNotification(PostForm.this, getString(R.string.post_delete_failed_toast), Toast.LENGTH_SHORT);
				exception.printStackTrace();
			}
		}

		/* Delete the entity from the service */
		super.deleteEntity();
	}

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	protected void clearMedia() {
		/*
		 * The bitmap goes away from view but we still keep the mediaUri so if the user
		 * cancels, the entity will still retain it's previous image. Saving will perform
		 * the real delete of the image and set mediaUri to null.
		 */
		final PostEntity entity = (PostEntity) mEntity;
		entity.mediaBitmap.recycle();
		entity.mediaBitmap = null;
		drawEntity();
		mProcessing = false;
	}

	private void showMediaThumbnail(Bitmap bitmap) {
		Animation animation = AnimationUtils.loadAnimation(PostForm.this, R.anim.fade_in_medium);
		animation.setFillEnabled(true);
		animation.setFillAfter(true);
		animation.setStartOffset(500);

		((ImageView) findViewById(R.id.img_media)).setImageBitmap(bitmap);
		((ImageView) findViewById(R.id.img_media)).startAnimation(animation);
		((ImageView) findViewById(R.id.img_media)).setVisibility(View.VISIBLE);
		((Button) findViewById(R.id.btn_add_media)).setVisibility(View.GONE);
		((Button) findViewById(R.id.btn_clear_media)).setVisibility(View.VISIBLE);
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------
	protected void onDestroy() {

		/* This activity gets destroyed everytime we leave using back or finish(). */
		try {
			final PostEntity entity = (PostEntity) mEntity;
			if (entity.mediaBitmap != null) {
				entity.mediaBitmap.recycle();
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
	protected int getLayoutID() {
		return R.layout.post_form;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		/*
		 * Called before onResume. If we are returning from the market app, we
		 * get a zero result code whether the user decided to start an install
		 * or not.
		 */
		if (requestCode == CandiConstants.ACTIVITY_PHOTO_PICK) {
			if (resultCode == Activity.RESULT_OK) {
				if (mPickerTarget == PickerTarget.Media) {
					final PostEntity entity = (PostEntity) mEntity;

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

					entity.mediaUri = null;
					entity.mediaBitmap = bitmap;
					drawEntity();
				}
			}
		}
		else if (requestCode == CandiConstants.ACTIVITY_PHOTO_MAKE) {
			if (resultCode == Activity.RESULT_OK) {
				if (mPickerTarget == PickerTarget.Media) {
					final PostEntity entity = (PostEntity) mEntity;

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

					entity.mediaUri = null;
					entity.mediaBitmap = bitmap;
					drawEntity();
				}
			}
		}
		mProcessing = false;
	}
}