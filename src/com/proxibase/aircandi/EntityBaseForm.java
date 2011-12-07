package com.proxibase.aircandi;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.models.BaseEntity;
import com.proxibase.aircandi.utils.DateUtils;
import com.proxibase.aircandi.utils.ImageManager;
import com.proxibase.aircandi.utils.ImageUtils;
import com.proxibase.aircandi.utils.Logger;
import com.proxibase.aircandi.utils.S3;
import com.proxibase.aircandi.utils.ImageLoader.ImageProfile;
import com.proxibase.aircandi.utils.ImageManager.ImageRequestListener;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest.ImageFormat;
import com.proxibase.sdk.android.proxi.consumer.User;
import com.proxibase.sdk.android.proxi.consumer.Beacon.BeaconType;
import com.proxibase.sdk.android.proxi.consumer.EntityProxy.Visibility;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.IQueryListener;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;
import com.proxibase.sdk.android.util.ProxiConstants;

public abstract class EntityBaseForm extends AircandiActivity {

	private FormTab		mActiveTab	= FormTab.Content;
	private ViewFlipper	mViewFlipper;
	private int			mTextColorFocused;
	private int			mTextColorUnfocused;
	private int			mHeightActive;
	private int			mHeightInactive;
	private ImageView	mImageViewContent;
	private ImageView	mImageViewSettings;
	private TextView	mTextViewContent;
	private TextView	mTextViewSettings;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);

		configure();
		bindEntity();
		drawEntity();
	}

	protected void configure() {
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
		/*
		 * Fill in the system and default properties for the base entity properties. The activities
		 * that subclass this will set any additional properties beyond the base ones.
		 */
		final BaseEntity entity = (BaseEntity) mEntity;
		if (mCommand.verb.equals("new")) {
			entity.beaconId = mBeacon.id;
			entity.signalFence = -100.0f;
			entity.createdById = String.valueOf(((User) mUser).id);
			entity.enabled = true;
			entity.visibility = Visibility.Public.ordinal();
			entity.password = null;
		}
		else if (mCommand.verb.equals("edit")) {
			mImageUriOriginal = entity.imageUri;
		}

	}

	protected void drawEntity() {

		if (mEntity != null) {
			final BaseEntity entity = (BaseEntity) mEntity;

			/* Content */

			if (findViewById(R.id.img_public_image) != null) {
				if (entity.imageUri != null && entity.imageUri.length() > 0) {
					if (entity.imageBitmap != null) {
						((ImageView) findViewById(R.id.img_public_image)).setImageBitmap(entity.imageBitmap);
						((ImageView) findViewById(R.id.img_public_image)).setVisibility(View.VISIBLE);
					}
					else {
						ImageManager.getInstance().getImageLoader().fetchImageByProfile(ImageProfile.SquareTile, entity.imageUri,
								new ImageRequestListener() {

									@Override
									public void onImageReady(Bitmap bitmap) {
										Logger.d(EntityBaseForm.this, "Image fetched: " + entity.imageUri);
										entity.imageBitmap = bitmap;
										showPicture(bitmap, R.id.img_public_image);
									}
								});
					}
				}
			}

			if (findViewById(R.id.txt_title) != null) {
				((TextView) findViewById(R.id.txt_title)).setText(entity.title);
			}

			if (findViewById(R.id.txt_content) != null) {
				((TextView) findViewById(R.id.txt_content)).setText(entity.description);
			}

			/* Settings */

			if (findViewById(R.id.cbo_visibility) != null) {
				((Spinner) findViewById(R.id.cbo_visibility)).setSelection(entity.visibility);
			}

			if (findViewById(R.id.txt_password) != null) {
				((TextView) findViewById(R.id.txt_password)).setText(entity.password);
			}

			/* Configure UI */

			if (mCommand.verb.equals("new")) {
				if (findViewById(R.id.btn_delete_post) != null) {
					((Button) findViewById(R.id.btn_delete_post)).setVisibility(View.GONE);
				}
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

	public void onSaveButtonClick(View view) {
		startTitlebarProgress();
		doSave(true);
	}

	public void onDeleteButtonClick(View view) {
		startTitlebarProgress();
		deleteEntity();
	}

	public void onCancelButtonClick(View view) {
		setResult(Activity.RESULT_CANCELED);
		finish();
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	protected void doSave(boolean updateImages) {

		/* Insert beacon if it isn't already registered */
		if (mBeacon != null && mBeacon.isUnregistered) {
			mBeacon.registeredById = String.valueOf(((User) mUser).id);
			mBeacon.beaconType = BeaconType.Fixed.name().toLowerCase();
			mBeacon.beaconSetId = ProxiConstants.BEACONSET_WORLD;
			try {
				Logger.i(this, "Inserting beacon: " + mBeacon.id);
				mBeacon.insert();
			}
			catch (ProxibaseException exception) {
				ImageUtils.showToastNotification(EntityBaseForm.this, getString(R.string.post_update_failed_toast), Toast.LENGTH_SHORT);
				exception.printStackTrace();
			}
		}

		/* Pull all the control values back into the entity object */
		gather();

		/* Delete or upload images to S3 as needed. */
		if (updateImages) {
			updateImages();
		}

		if (mCommand.verb.equals("new")) {
			insertEntity();
		}
		else if (mCommand.verb.equals("edit")) {
			updateEntity();
		}
	}

	protected void gather() {
		final BaseEntity entity = (BaseEntity) mEntity;
		if (findViewById(R.id.txt_title) != null) {
			entity.title = ((TextView) findViewById(R.id.txt_title)).getText().toString().trim();
		}
		if (findViewById(R.id.txt_title) != null) {
			entity.label = ((TextView) findViewById(R.id.txt_title)).getText().toString().trim();
		}
		if (findViewById(R.id.txt_content) != null) {
			entity.description = ((TextView) findViewById(R.id.txt_content)).getText().toString().trim();
		}
		if (findViewById(R.id.cbo_visibility) != null) {
			entity.visibility = ((Spinner) findViewById(R.id.cbo_visibility)).getSelectedItemPosition();
		}
		if (findViewById(R.id.txt_password) != null) {
			entity.password = ((TextView) findViewById(R.id.txt_password)).getText().toString().trim();
		}
	}

	protected void updateImages() {
		BaseEntity entity = (BaseEntity) mEntity;

		/* Delete image from S3 if it has been orphaned */
		if (mImageUriOriginal != null && !ImageManager.isLocalImage(mImageUriOriginal)) {
			if (!entity.imageUri.equals(mImageUriOriginal)) {
				try {
					S3.deleteImage(mImageUriOriginal.substring(mImageUriOriginal.lastIndexOf("/") + 1));
					ImageManager.getInstance().deleteImage(mImageUriOriginal);
					ImageManager.getInstance().deleteImage(mImageUriOriginal + ".reflection");
				}
				catch (ProxibaseException exception) {
					ImageUtils.showToastNotification(EntityBaseForm.this, getString(R.string.post_update_failed_toast), Toast.LENGTH_SHORT);
					exception.printStackTrace();
				}
			}
		}

		/* Put image to S3 if we have a new one. */
		if (entity.imageUri != null && !ImageManager.isLocalImage(entity.imageUri)) {
			if (!entity.imageUri.equals(mImageUriOriginal) && entity.imageBitmap != null) {
				String imageKey = String.valueOf(((User) mUser).id) + "_"
									+ String.valueOf(DateUtils.nowString(DateUtils.DATE_NOW_FORMAT_FILENAME))
									+ ".jpg";
				try {
					S3.putImage(imageKey, entity.imageBitmap);
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
		entity.createdDate = DateUtils.nowString();
		Logger.i(this, "Inserting entity: " + entity.title);

		entity.insertAsync(new IQueryListener() {

			@Override
			public void onComplete(String jsonResponse) {

				runOnUiThread(new Runnable() {

					public void run() {
						stopTitlebarProgress();
						ImageUtils.showToastNotification(getApplicationContext(), getString(R.string.post_insert_success_toast), Toast.LENGTH_SHORT);
						Intent intent = new Intent();

						/* We are editing so set the dirty flag */
						if (entity.parentEntityId == null) {
							intent.putExtra(getString(R.string.EXTRA_BEACON_DIRTY), entity.beaconId);
						}
						else {
							intent.putExtra(getString(R.string.EXTRA_ENTITY_DIRTY), entity.parentEntityId);
						}
						intent.putExtra(getString(R.string.EXTRA_RESULT_VERB), Verb.New);

						setResult(Activity.RESULT_FIRST_USER, intent);
						finish();
					}
				});
			}

			@Override
			public void onProxibaseException(ProxibaseException exception) {
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						ImageUtils.showToastNotification(getApplicationContext(), getString(R.string.post_insert_failed_toast), Toast.LENGTH_SHORT);
					}
				});
				exception.printStackTrace();
			}
		});
	}

	protected void updateEntity() {

		final BaseEntity entity = (BaseEntity) mEntity;
		Logger.i(this, "Updating entity: " + entity.title);
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
						finish();
					}
				});
			}

			@Override
			public void onProxibaseException(ProxibaseException exception) {
				exception.printStackTrace();
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
		if (entity.imageUri != null && entity.imageUri.length() != 0 &&
				!ImageManager.isLocalImage(entity.imageUri) && entity.imageFormat.equals("binary")) {
			String imageKey = entity.imageUri.substring(entity.imageUri.lastIndexOf("/") + 1);
			try {
				S3.deleteImage(imageKey);
				ImageManager.getInstance().deleteImage(entity.imageUri);
				ImageManager.getInstance().deleteImage(entity.imageUri + ".reflection");
			}
			catch (ProxibaseException exception) {
				ImageUtils.showToastNotification(EntityBaseForm.this, getString(R.string.post_delete_failed_toast), Toast.LENGTH_SHORT);
				exception.printStackTrace();
			}
		}

		/* Delete the entity from the service */
		Bundle parameters = new Bundle();
		parameters.putInt("entityId", entity.id);
		Logger.i(this, "Deleting entity: " + entity.title);

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
		finish();
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

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------
	protected void onDestroy() {

		/* This activity gets destroyed everytime we leave using back or finish(). */
		try {
			BaseEntity entity = (BaseEntity) mEntity;
			if (entity != null && entity.imageBitmap != null) {
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

	protected enum FormTab {
		Content,
		Settings
	}
}