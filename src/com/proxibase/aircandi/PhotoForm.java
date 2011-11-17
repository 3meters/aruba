package com.proxibase.aircandi;

import java.io.File;
import java.io.FileNotFoundException;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.models.PhotoEntity;
import com.proxibase.aircandi.utils.ImageManager;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;

public class PhotoForm extends EntityBaseForm {

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
			mEntity = new PhotoEntity();
			((PhotoEntity) mEntity).entityType = CandiConstants.TYPE_CANDI_PHOTO;

		}
		else if (mCommand.verb.equals("edit")) {
			String jsonResponse = null;
			try {
				jsonResponse = (String) ProxibaseService.getInstance().select(mEntityProxy.getEntryUri(), ResponseFormat.Json);
				mEntity = (PhotoEntity) ProxibaseService.convertJsonToObject(jsonResponse, PhotoEntity.class, GsonType.ProxibaseService);
			}
			catch (ProxibaseException exception) {
				exception.printStackTrace();
			}
		}
		super.bindEntity();
	}

	@Override
	protected void drawEntity() {
		super.drawEntity();

		((TextView) findViewById(R.id.txt_header_title)).setText(getResources().getString(R.string.form_title_photo));
	}

	// --------------------------------------------------------------------------------------------
	// Picker routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected void useProfilePhoto() {
		super.useProfilePhoto();
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
		final PhotoEntity entity = (PhotoEntity) mEntity;
		if (entity.imageUri != null) {
			entity.mediaUri = entity.imageUri;
			entity.mediaFormat = entity.mediaFormat;
		}
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
		final PhotoEntity entity = (PhotoEntity) mEntity;
		entity.mediaBitmap.recycle();
		entity.mediaBitmap = null;
		drawEntity();
		mProcessing = false;
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutID() {
		return R.layout.photo_form;
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
					final PhotoEntity entity = (PhotoEntity) mEntity;

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
					final PhotoEntity entity = (PhotoEntity) mEntity;

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