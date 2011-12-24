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

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.models.BaseEntity;
import com.proxibase.aircandi.utils.DateUtils;
import com.proxibase.aircandi.utils.ImageManager;
import com.proxibase.aircandi.utils.ImageUtils;
import com.proxibase.aircandi.utils.Logger;
import com.proxibase.aircandi.utils.NetworkManager;
import com.proxibase.aircandi.utils.S3;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest.ImageFormat;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest.ImageShape;
import com.proxibase.aircandi.utils.NetworkManager.ResultCode;
import com.proxibase.aircandi.utils.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.widgets.AuthorBlock;
import com.proxibase.aircandi.widgets.WebImageView;
import com.proxibase.sdk.android.proxi.consumer.User;
import com.proxibase.sdk.android.proxi.consumer.Beacon.BeaconType;
import com.proxibase.sdk.android.proxi.consumer.EntityProxy.Visibility;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ServiceRequest;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestListener;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;
import com.proxibase.sdk.android.util.ProxiConstants;

public abstract class EntityBaseForm extends AircandiActivity {

	private FormTab			mActiveTab	= FormTab.Content;
	private ViewFlipper		mViewFlipper;
	private int				mTextColorFocused;
	private int				mTextColorUnfocused;
	private int				mHeightActive;
	private int				mHeightInactive;
	private ImageView		mImageViewContent;
	private ImageView		mImageViewSettings;
	private TextView		mTextViewContent;
	private TextView		mTextViewSettings;
	protected WebImageView	mImagePicture;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);

		configure();
		bindEntity();
		drawEntity();

		/* Tracking */
		GoogleAnalyticsTracker.getInstance().trackPageView("/" + mCommand.handler);
		GoogleAnalyticsTracker.getInstance().trackEvent("Entity", mCommand.verb, ((BaseEntity) mEntity).entityType, 0);
	}

	protected void configure() {
		mViewFlipper = (ViewFlipper) findViewById(R.id.flipper_form);
		mImageViewContent = (ImageView) findViewById(R.id.image_tab_content);
		mImageViewSettings = (ImageView) findViewById(R.id.image_tab_settings);
		mTextViewContent = (TextView) findViewById(R.id.text_tab_content);
		mTextViewSettings = (TextView) findViewById(R.id.text_tab_settings);
		mImagePicture = (WebImageView) findViewById(R.id.image_picture);

		TypedValue resourceName = new TypedValue();
		if (this.getTheme().resolveAttribute(R.attr.textColorFocused, resourceName, true)) {
			mTextColorFocused = Color.parseColor((String) resourceName.coerceToString());
		}

		if (this.getTheme().resolveAttribute(R.attr.textColorUnfocused, resourceName, true)) {
			mTextColorUnfocused = Color.parseColor((String) resourceName.coerceToString());
		}

		mHeightActive = ImageUtils.getRawPixelsForDisplayPixels(6);
		mHeightInactive = ImageUtils.getRawPixelsForDisplayPixels(2);
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
		if (entity != null) {
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
		//GoogleAnalyticsTracker.getInstance().dispatch();
	}

	protected void drawEntity() {

		if (mEntity != null) {
			final BaseEntity entity = (BaseEntity) mEntity;

			/* Content */

			if (findViewById(R.id.image_picture) != null) {
				mImagePicture = (WebImageView) findViewById(R.id.image_picture);
				if (entity.imageUri != null && entity.imageUri.length() > 0) {
					if (entity.imageBitmap != null) {
						mImagePicture.setImageBitmap(entity.imageBitmap);
						mImagePicture.setVisibility(View.VISIBLE);
					}
					else {

						ImageRequest imageRequest = new ImageRequest(entity.imageUri, ImageShape.Square, entity.imageFormat,
								entity.javascriptEnabled,
								CandiConstants.IMAGE_WIDTH_SEARCH_MAX, false, true, true, 1, this, new RequestListener() {

									@Override
									public void onComplete(Object response) {
										ServiceResponse serviceResponse = (ServiceResponse) response;
										if (serviceResponse.resultCode == ResultCode.Success) {
											Bitmap bitmap = (Bitmap) serviceResponse.data;
											entity.imageBitmap = bitmap;
										}
									}
								});

						mImagePicture.setImageRequest(imageRequest, null);
					}
				}
			}

			if (findViewById(R.id.text_title) != null) {
				((TextView) findViewById(R.id.text_title)).setText(entity.title);
			}

			if (findViewById(R.id.text_content) != null) {
				((TextView) findViewById(R.id.text_content)).setText(entity.description);
			}

			/* Settings */

			if (findViewById(R.id.cbo_visibility) != null) {
				((Spinner) findViewById(R.id.cbo_visibility)).setSelection(entity.visibility);
			}

			if (findViewById(R.id.text_password) != null) {
				((TextView) findViewById(R.id.text_password)).setText(entity.password);
			}

			/* Author */

			if (mEntityProxy != null && mEntityProxy.author != null) {
				((AuthorBlock) findViewById(R.id.block_author)).bindToAuthor(mEntityProxy.author, DateUtils.wcfToDate(mEntityProxy.createdDate));
			}
			else {
				((AuthorBlock) findViewById(R.id.block_author)).setVisibility(View.GONE);
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

	public void onChangePictureButtonClick(View view) {
		showChangePictureDialog(false, mImagePicture, new RequestListener() {

			@Override
			public void onComplete(Object response, Object extra) {

				ServiceResponse serviceResponse = (ServiceResponse) response;
				if (serviceResponse.resultCode == ResultCode.Success) {
					Bitmap bitmap = (Bitmap) serviceResponse.data;
					String imageUri = (String) extra;
					BaseEntity entity = (BaseEntity) mEntity;
					entity.imageUri = imageUri;
					entity.imageBitmap = bitmap;
				}
			}
		});
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

			Logger.i(this, "Inserting beacon: " + mBeacon.id);
			ServiceRequest serviceRequest = new ServiceRequest();
			serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_ODATA + mBeacon.getCollection());
			serviceRequest.setRequestType(RequestType.Insert);
			serviceRequest.setRequestBody(ProxibaseService.convertObjectToJson((Object) this, GsonType.ProxibaseService));
			serviceRequest.setResponseFormat(ResponseFormat.Json);

			ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);
			if (serviceResponse.resultCode != ResultCode.Success) {
				return;
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
		GoogleAnalyticsTracker.getInstance().dispatch();
	}

	protected void gather() {
		final BaseEntity entity = (BaseEntity) mEntity;
		if (findViewById(R.id.text_title) != null) {
			entity.title = ((TextView) findViewById(R.id.text_title)).getText().toString().trim();
			entity.label = ((TextView) findViewById(R.id.text_title)).getText().toString().trim();
		}
		if (findViewById(R.id.text_content) != null) {
			entity.description = ((TextView) findViewById(R.id.text_content)).getText().toString().trim();
		}
		if (findViewById(R.id.cbo_visibility) != null) {
			entity.visibility = ((Spinner) findViewById(R.id.cbo_visibility)).getSelectedItemPosition();
		}
		if (findViewById(R.id.text_password) != null) {
			entity.password = ((TextView) findViewById(R.id.text_password)).getText().toString().trim();
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
					ImageUtils.showToastNotification(getString(R.string.alert_update_failed), Toast.LENGTH_SHORT);
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
					ImageUtils.showToastNotification(getString(R.string.alert_update_failed), Toast.LENGTH_SHORT);
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
		GoogleAnalyticsTracker.getInstance().trackEvent("Entity", "Insert", entity.entityType, 0);

		ServiceRequest serviceRequest = new ServiceRequest();
		serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_ODATA + entity.getCollection());
		serviceRequest.setRequestType(RequestType.Insert);
		serviceRequest.setRequestBody(ProxibaseService.convertObjectToJson(entity, GsonType.ProxibaseService));
		serviceRequest.setResponseFormat(ResponseFormat.Json);
		serviceRequest.setRequestListener(new RequestListener() {

			@Override
			public void onComplete(Object response) {
				ServiceResponse serviceResponse = (ServiceResponse) response;
				stopTitlebarProgress();
				if (serviceResponse.resultCode != ResultCode.Success) {
					ImageUtils.showToastNotification(getString(R.string.alert_insert_failed), Toast.LENGTH_SHORT);
					return;
				}

				ImageUtils.showToastNotification(getString(R.string.alert_inserted), Toast.LENGTH_SHORT);
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

		NetworkManager.getInstance().requestAsync(serviceRequest);

	}

	protected void updateEntity() {

		final BaseEntity entity = (BaseEntity) mEntity;
		Logger.i(this, "Updating entity: " + entity.title);
		GoogleAnalyticsTracker.getInstance().trackEvent("Entity", "Update", entity.entityType, 0);

		ServiceRequest serviceRequest = new ServiceRequest();
		serviceRequest.setUri(entity.getEntryUri());
		serviceRequest.setRequestType(RequestType.Update);
		serviceRequest.setRequestBody(ProxibaseService.convertObjectToJson(entity, GsonType.ProxibaseService));
		serviceRequest.setResponseFormat(ResponseFormat.Json);
		serviceRequest.setRequestListener(new RequestListener() {

			@Override
			public void onComplete(Object response) {

				ServiceResponse serviceResponse = (ServiceResponse) response;
				stopTitlebarProgress();
				if (serviceResponse.resultCode != ResultCode.Success) {
					ImageUtils.showToastNotification(getString(R.string.alert_update_failed), Toast.LENGTH_SHORT);
					return;
				}

				ImageUtils.showToastNotification(getString(R.string.alert_updated), Toast.LENGTH_SHORT);
				Intent intent = new Intent();
				intent.putExtra(getString(R.string.EXTRA_ENTITY_DIRTY), entity.id);
				intent.putExtra(getString(R.string.EXTRA_RESULT_VERB), Verb.Edit);

				setResult(Activity.RESULT_FIRST_USER, intent);
				finish();
			}
		});

		NetworkManager.getInstance().requestAsync(serviceRequest);
	}

	protected void deleteEntity() {
		/*
		 * TODO: We need to update the service so the recursive entity delete also
		 * deletes any associated resources stored with S3. As currently coded, we will
		 * be orphaning any images associated with child entities.
		 */

		/* If there is an image stored with S3 then delete it */
		BaseEntity entity = (BaseEntity) mEntity;
		GoogleAnalyticsTracker.getInstance().trackEvent("Entity", "Delete", entity.entityType, 0);
		if (entity.imageUri != null && entity.imageUri.length() != 0 &&
				!ImageManager.isLocalImage(entity.imageUri) && entity.imageFormat.equals("binary")) {
			String imageKey = entity.imageUri.substring(entity.imageUri.lastIndexOf("/") + 1);
			try {
				S3.deleteImage(imageKey);
				ImageManager.getInstance().deleteImage(entity.imageUri);
				ImageManager.getInstance().deleteImage(entity.imageUri + ".reflection");
			}
			catch (ProxibaseException exception) {
				ImageUtils.showToastNotification(getString(R.string.alert_delete_failed), Toast.LENGTH_SHORT);
				exception.printStackTrace();
				stopTitlebarProgress();
				return;
			}
		}

		/* Delete the entity from the service */
		Bundle parameters = new Bundle();
		parameters.putInt("entityId", entity.id);
		Logger.i(this, "Deleting entity: " + entity.title);

		ServiceRequest serviceRequest = new ServiceRequest();
		serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE + "DeleteEntityWithChildren");
		serviceRequest.setParameters(parameters);
		serviceRequest.setRequestType(RequestType.Method);
		serviceRequest.setResponseFormat(ResponseFormat.Json);

		ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);
		if (serviceResponse.resultCode != ResultCode.Success) {
			ImageUtils.showToastNotification(getString(R.string.alert_delete_failed), Toast.LENGTH_SHORT);
			serviceResponse.exception.printStackTrace();
			stopTitlebarProgress();
			return;
		}

		stopTitlebarProgress();
		ImageUtils.showToastNotification(getString(R.string.alert_deleted), Toast.LENGTH_SHORT);
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