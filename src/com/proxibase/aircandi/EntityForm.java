package com.proxibase.aircandi;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.proxibase.aircandi.components.Command;
import com.proxibase.aircandi.components.DateUtils;
import com.proxibase.aircandi.components.ImageManager;
import com.proxibase.aircandi.components.ImageUtils;
import com.proxibase.aircandi.components.Logger;
import com.proxibase.aircandi.components.NetworkManager;
import com.proxibase.aircandi.components.ProxiExplorer;
import com.proxibase.aircandi.components.S3;
import com.proxibase.aircandi.components.Tracker;
import com.proxibase.aircandi.components.AircandiCommon.IntentBuilder;
import com.proxibase.aircandi.components.Command.CommandVerb;
import com.proxibase.aircandi.components.ImageManager.ImageRequest;
import com.proxibase.aircandi.components.ImageManager.ImageRequest.ImageShape;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.widgets.AuthorBlock;
import com.proxibase.aircandi.widgets.WebImageView;
import com.proxibase.sdk.android.proxi.consumer.Beacon;
import com.proxibase.sdk.android.proxi.consumer.Entity;
import com.proxibase.sdk.android.proxi.consumer.User;
import com.proxibase.sdk.android.proxi.consumer.Beacon.BeaconType;
import com.proxibase.sdk.android.proxi.consumer.Entity.Visibility;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ServiceRequest;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestListener;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;
import com.proxibase.sdk.android.util.ProxiConstants;

public class EntityForm extends FormActivity {

	private ViewFlipper		mViewFlipper;
	protected WebImageView	mImagePicture;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);

		User user = Aircandi.getInstance().getUser();
		if (user != null && user.anonymous) {

			IntentBuilder intentBuilder = new IntentBuilder(this, SignInForm.class);
			intentBuilder.setCommand(new Command(CommandVerb.Edit));
			intentBuilder.setMessage(getString(R.string.signin_message_new_candi));
			Intent intent = intentBuilder.create();

			startActivityForResult(intent, CandiConstants.ACTIVITY_SIGNIN);
		}

		initialize();
		bind();
		draw();

		/* Tracking */
		Tracker.trackPageView("/" + mCommon.mCommand.activityName);
		Tracker.trackEvent("Entity", mCommon.mCommand.verb.toString(), mCommon.mEntityType, 0);
	}

	protected void initialize() {
		mViewFlipper = (ViewFlipper) findViewById(R.id.flipper_form);
		mImagePicture = (WebImageView) findViewById(R.id.image_picture);

		if (mViewFlipper != null) {
			mCommon.setActiveTab(((ViewGroup) findViewById(R.id.image_tab_host)).getChildAt(0));
		}
	}

	protected void bind() {
		/*
		 * Fill in the system and default properties for the base entity properties. The activities
		 * that subclass this will set any additional properties beyond the base ones.
		 */
		if (mCommon.mCommand.verb == CommandVerb.New) {
			Entity entity = new Entity();
			entity = new Entity();
			entity.beaconId = mCommon.mBeaconId;
			entity.signalFence = -100.0f;
			entity.createdById = Integer.parseInt(Aircandi.getInstance().getUser().id);
			entity.enabled = true;
			entity.locked = false;
			entity.linkJavascriptEnabled = false;
			entity.linkZoom = false;
			entity.visibility = Visibility.Public.ordinal();
			entity.entityType = mCommon.mEntityType;
			entity.parentEntityId = null;

			if (mCommon.mParentEntityId != 0) {
				entity.parentEntityId = mCommon.mParentEntityId;
			}

			if (mCommon.mEntityType.equals(CandiConstants.TYPE_CANDI_PICTURE)) {
				entity.imagePreviewUri = "resource:placeholder_picture";
				entity.imageUri = entity.imagePreviewUri;
			}
			mCommon.mEntity = entity;
		}
		else if (mCommon.mCommand.verb == CommandVerb.Edit) {
			if (mCommon.mEntity == null && mCommon.mEntityId != null) {
				ServiceResponse serviceResponse = ProxiExplorer.getInstance().getEntityFromService(mCommon.mEntityId, false);
				if (serviceResponse.responseCode == ResponseCode.Success) {
					mCommon.mEntity = (Entity) serviceResponse.data;
				}
			}
			mImageUriOriginal = mCommon.mEntity.imageUri;
		}
	}

	protected void draw() {

		if (mCommon.mEntity != null) {

			final Entity entity = mCommon.mEntity;

			/* Content */

			if (findViewById(R.id.image_picture) != null) {
				mImagePicture = (WebImageView) findViewById(R.id.image_picture);
				if (entity.imageUri != null && entity.imageUri.length() > 0) {
					if (entity.imageBitmap != null) {
						mImagePicture.setImageBitmap(entity.imageBitmap);
						mImagePicture.setVisibility(View.VISIBLE);
					}
					else {

						ImageRequest imageRequest = new ImageRequest(entity, ImageShape.Square,
								CandiConstants.IMAGE_WIDTH_SEARCH_MAX, false, true, true, 1, this, new RequestListener() {

									@Override
									public void onComplete(Object response) {
										ServiceResponse serviceResponse = (ServiceResponse) response;
										if (serviceResponse.responseCode == ResponseCode.Success) {
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

			if (findViewById(R.id.text_uri) != null) {
				((TextView) findViewById(R.id.text_uri)).setText(entity.linkUri);
			}

			/* Settings */

			if (findViewById(R.id.cbo_visibility) != null) {
				((Spinner) findViewById(R.id.cbo_visibility)).setSelection(entity.visibility);
			}

			if (findViewById(R.id.chk_html_zoom) != null) {
				if (entity.linkUri != null) {
					((CheckBox) findViewById(R.id.chk_html_zoom)).setVisibility(View.VISIBLE);
					((CheckBox) findViewById(R.id.chk_html_zoom)).setChecked(entity.linkZoom);
				}
			}

			if (findViewById(R.id.chk_html_javascript) != null) {
				if (entity.linkUri != null) {
					((CheckBox) findViewById(R.id.chk_html_javascript)).setVisibility(View.VISIBLE);
					((CheckBox) findViewById(R.id.chk_html_javascript)).setChecked(entity.linkJavascriptEnabled);
				}
			}

			if (findViewById(R.id.chk_locked) != null) {
				((CheckBox) findViewById(R.id.chk_locked)).setVisibility(View.VISIBLE);
				((CheckBox) findViewById(R.id.chk_locked)).setChecked(entity.locked);
			}

			/* Author */

			if (entity != null && entity.author != null) {
				((AuthorBlock) findViewById(R.id.block_author)).bindToAuthor(entity.author, DateUtils.wcfToDate(entity.createdDate));
			}
			else {
				((AuthorBlock) findViewById(R.id.block_author)).setVisibility(View.GONE);
			}

			/* Configure UI */

			if (mCommon.mCommand.verb == CommandVerb.New) {
				if (findViewById(R.id.btn_delete_post) != null) {
					((Button) findViewById(R.id.btn_delete_post)).setVisibility(View.GONE);
				}
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onTabClick(View view) {
		mCommon.setActiveTab(view);
		if (view.getTag().equals("content")) {
			mViewFlipper.setDisplayedChild(0);
		}
		else if (view.getTag().equals("settings")) {
			mViewFlipper.setDisplayedChild(1);
		}
	}

	public void onChangePictureButtonClick(View view) {
		showChangePictureDialog(false, mImagePicture, new RequestListener() {

			@Override
			public void onComplete(Object response, String imageUri, String linkUri, Bitmap imageBitmap) {

				ServiceResponse serviceResponse = (ServiceResponse) response;
				if (serviceResponse.responseCode == ResponseCode.Success) {
					mCommon.mEntity.imageBitmap = imageBitmap;
					mCommon.mEntity.imageUri = imageUri;
					mCommon.mEntity.linkUri = linkUri;
				}
			}
		});
	}

	public void onSaveButtonClick(View view) {
		mCommon.startTitlebarProgress();
		doSave(true);
	}

	public void onDeleteButtonClick(View view) {
		mCommon.startTitlebarProgress();
		delete();
	}

	public void onCancelButtonClick(View view) {
		setResult(Activity.RESULT_CANCELED);
		finish();
	}

	public void onLinkBuilderClick(View view) {
		showBookmarkActivity();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {

		if (requestCode == CandiConstants.ACTIVITY_SIGNIN) {
			if (resultCode == Activity.RESULT_CANCELED) {
				setResult(resultCode);
				finish();
			}
			else {
				initialize();
				bind();
				draw();

				/* Tracking */
				Tracker.trackPageView("/" + mCommon.mCommand.activityName);
				Tracker.trackEvent("Entity", mCommon.mCommand.verb.toString(), mCommon.mEntityType, 0);
			}
		}
		super.onActivityResult(requestCode, resultCode, intent);
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	protected void doSave(boolean updateImages) {

		/* Insert beacon if it isn't already registered */
		Beacon beacon = ProxiExplorer.getInstance().getBeaconById(mCommon.mBeaconId);
		if (beacon != null && beacon.isUnregistered) {
			beacon.registeredById = String.valueOf(Aircandi.getInstance().getUser().id);
			beacon.beaconType = BeaconType.Fixed.name().toLowerCase();
			beacon.beaconSetId = ProxiConstants.BEACONSET_WORLD;

			Logger.i(this, "Inserting beacon: " + mCommon.mBeaconId);
			ServiceRequest serviceRequest = new ServiceRequest();
			serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_ODATA + beacon.getCollection());
			serviceRequest.setRequestType(RequestType.Insert);
			serviceRequest.setRequestBody(ProxibaseService.convertObjectToJson((Object) this, GsonType.ProxibaseService));
			serviceRequest.setResponseFormat(ResponseFormat.Json);

			ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);
			if (serviceResponse.responseCode == ResponseCode.Success) {
				beacon.isUnregistered = false;
			}
		}

		/* Pull all the control values back into the entity object */
		gather();

		/* Delete or upload images to S3 as needed. */
		if (updateImages) {
			updateImages();
		}

		if (mCommon.mCommand.verb == CommandVerb.New) {
			insert();
		}
		else if (mCommon.mCommand.verb == CommandVerb.Edit) {
			update();
		}
		Tracker.dispatch();
	}

	@SuppressWarnings("unused")
	private boolean validate() {

		// Validate URL
		String uri = ((EditText) findViewById(R.id.text_uri)).getText().toString().trim();
		if (!URLUtil.isValidUrl(uri)) {
			Toast.makeText(this, "Invalid URL specified", Toast.LENGTH_SHORT).show();
			return false;
		}
		return true;
	}

	protected void gather() {
		if (findViewById(R.id.text_title) != null) {
			mCommon.mEntity.title = ((TextView) findViewById(R.id.text_title)).getText().toString().trim();
			mCommon.mEntity.label = ((TextView) findViewById(R.id.text_title)).getText().toString().trim();
		}
		if (findViewById(R.id.text_content) != null) {
			mCommon.mEntity.description = ((TextView) findViewById(R.id.text_content)).getText().toString().trim();
		}
		if (findViewById(R.id.cbo_visibility) != null) {
			mCommon.mEntity.visibility = ((Spinner) findViewById(R.id.cbo_visibility)).getSelectedItemPosition();
		}
		if (findViewById(R.id.chk_html_javascript) != null) {
			mCommon.mEntity.linkJavascriptEnabled = ((CheckBox) findViewById(R.id.chk_html_javascript)).isChecked();
		}
		if (findViewById(R.id.chk_html_zoom) != null) {
			mCommon.mEntity.linkZoom = ((CheckBox) findViewById(R.id.chk_html_zoom)).isChecked();
		}
		if (findViewById(R.id.chk_locked) != null) {
			mCommon.mEntity.locked = ((CheckBox) findViewById(R.id.chk_locked)).isChecked();
		}
	}

	protected void updateImages() {

		/* Delete image from S3 if it has been orphaned */
		if (mImageUriOriginal != null && !ImageManager.isLocalImage(mImageUriOriginal)) {
			if (!mCommon.mEntity.imageUri.equals(mImageUriOriginal) && mCommon.mEntity.imagePreviewUri != null
				&& !mCommon.mEntity.imagePreviewUri.equals("")) {
				try {
					S3.deleteImage(mCommon.mEntity.imagePreviewUri.substring(mCommon.mEntity.imagePreviewUri.lastIndexOf("/") + 1));
					ImageManager.getInstance().deleteImage(mCommon.mEntity.imagePreviewUri);
					ImageManager.getInstance().deleteImage(mCommon.mEntity.imagePreviewUri + ".reflection");
				}
				catch (ProxibaseException exception) {
					ImageUtils.showToastNotification(getString(R.string.alert_update_failed), Toast.LENGTH_SHORT);
					exception.printStackTrace();
				}
			}
		}

		/* Put image to S3 if we have a new one. */
		if (mCommon.mEntity.imageBitmap != null) {
			String imageKey = String.valueOf(Aircandi.getInstance().getUser().id) + "_"
								+ String.valueOf(DateUtils.nowString(DateUtils.DATE_NOW_FORMAT_FILENAME))
								+ ".jpg";
			try {
				S3.putImage(imageKey, mCommon.mEntity.imageBitmap);
			}
			catch (ProxibaseException exception) {
				ImageUtils.showToastNotification(getString(R.string.alert_update_failed), Toast.LENGTH_SHORT);
				exception.printStackTrace();
			}
			mCommon.mEntity.imagePreviewUri = CandiConstants.URL_AIRCANDI_MEDIA + CandiConstants.S3_BUCKET_IMAGES + "/" + imageKey;
			if (mCommon.mEntity.imageUri == null || mCommon.mEntity.imageUri.equals("")) {
				mCommon.mEntity.imageUri = mCommon.mEntity.imagePreviewUri;
			}
		}
	}

	protected void insert() {

		mCommon.mEntity.createdDate = DateUtils.nowString();
		Logger.i(this, "Inserting entity: " + mCommon.mEntity.title);
		Tracker.trackEvent("Entity", "Insert", mCommon.mEntity.entityType, 0);

		ServiceRequest serviceRequest = new ServiceRequest();
		serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_ODATA + mCommon.mEntity.getCollection());
		serviceRequest.setRequestType(RequestType.Insert);
		serviceRequest.setRequestBody(ProxibaseService.convertObjectToJson(mCommon.mEntity, GsonType.ProxibaseService));
		serviceRequest.setResponseFormat(ResponseFormat.Json);
		serviceRequest.setRequestListener(new RequestListener() {

			@Override
			public void onComplete(Object response) {
				ServiceResponse serviceResponse = (ServiceResponse) response;
				if (serviceResponse.responseCode != ResponseCode.Success) {
					ImageUtils.showToastNotification(getString(R.string.alert_insert_failed), Toast.LENGTH_SHORT);
					return;
				}

				ImageUtils.showToastNotification(getString(R.string.alert_inserted), Toast.LENGTH_SHORT);

				ProxiExplorer.getInstance().mEntitiesInserted.put(mCommon.mEntity.id, mCommon.mEntity);
				setResult(CandiConstants.RESULT_ENTITY_INSERTED);
				finish();
			}
		});

		NetworkManager.getInstance().requestAsync(serviceRequest);

	}

	protected void update() {

		Logger.i(this, "Updating entity: " + mCommon.mEntity.title);
		Tracker.trackEvent("Entity", "Update", mCommon.mEntity.entityType, 0);

		ServiceRequest serviceRequest = new ServiceRequest();
		serviceRequest.setUri(mCommon.mEntity.getEntryUri());
		serviceRequest.setRequestType(RequestType.Update);
		serviceRequest.setRequestBody(ProxibaseService.convertObjectToJson(mCommon.mEntity, GsonType.ProxibaseService));
		serviceRequest.setResponseFormat(ResponseFormat.Json);
		serviceRequest.setRequestListener(new RequestListener() {

			@Override
			public void onComplete(Object response) {

				ServiceResponse serviceResponse = (ServiceResponse) response;
				if (serviceResponse.responseCode != ResponseCode.Success) {
					ImageUtils.showToastNotification(getString(R.string.alert_update_failed), Toast.LENGTH_SHORT);
					return;
				}

				ImageUtils.showToastNotification(getString(R.string.alert_updated), Toast.LENGTH_SHORT);

				/* Replaces if it is already in the collection */
				ProxiExplorer.getInstance().mEntitiesUpdated.put(mCommon.mEntity.id, mCommon.mEntity);
				setResult(CandiConstants.RESULT_ENTITY_UPDATED);
				finish();
			}
		});

		NetworkManager.getInstance().requestAsync(serviceRequest);
	}

	protected void delete() {
		/*
		 * TODO: We need to update the service so the recursive entity delete also
		 * deletes any associated resources stored with S3. As currently coded, we will
		 * be orphaning any images associated with child entities.
		 */

		/* If there is an image stored with S3 then delete it */
		Tracker.trackEvent("Entity", "Delete", mCommon.mEntity.entityType, 0);

		if (mCommon.mEntity.imagePreviewUri != null && !mCommon.mEntity.imagePreviewUri.equals("")
			&& !ImageManager.isLocalImage(mCommon.mEntity.imagePreviewUri)) {
			String imageKey = mCommon.mEntity.imagePreviewUri.substring(mCommon.mEntity.imagePreviewUri.lastIndexOf("/") + 1);
			try {
				S3.deleteImage(imageKey);
				ImageManager.getInstance().deleteImage(mCommon.mEntity.imagePreviewUri);
				ImageManager.getInstance().deleteImage(mCommon.mEntity.imagePreviewUri + ".reflection");
			}
			catch (ProxibaseException exception) {
				ImageUtils.showToastNotification(getString(R.string.alert_delete_failed), Toast.LENGTH_SHORT);
				exception.printStackTrace();
				return;
			}
		}

		/* Delete the entity from the service */
		Bundle parameters = new Bundle();
		parameters.putInt("entityId", mCommon.mEntity.id);
		Logger.i(this, "Deleting entity: " + mCommon.mEntity.title);

		ServiceRequest serviceRequest = new ServiceRequest();
		serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE + "DeleteEntityWithChildren");
		serviceRequest.setParameters(parameters);
		serviceRequest.setRequestType(RequestType.Method);
		serviceRequest.setResponseFormat(ResponseFormat.Json);

		ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);
		if (serviceResponse.responseCode != ResponseCode.Success) {
			ImageUtils.showToastNotification(getString(R.string.alert_delete_failed), Toast.LENGTH_SHORT);
			serviceResponse.exception.printStackTrace();
			return;
		}

		ImageUtils.showToastNotification(getString(R.string.alert_deleted), Toast.LENGTH_SHORT);

		ProxiExplorer.getInstance().mEntitiesDeleted.put(mCommon.mEntity.id, mCommon.mEntity);
		setResult(CandiConstants.RESULT_ENTITY_DELETED);
		finish();
	}

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	private void showBookmarkActivity() {

		mImageRequestListener = new RequestListener() {

			@Override
			public void onComplete(Object response, String imageUri, String linkUri, Bitmap imageBitmap) {

				ServiceResponse serviceResponse = (ServiceResponse) response;
				if (serviceResponse.responseCode == ResponseCode.Success) {
					mCommon.mEntity.linkUri = linkUri;
					mCommon.mEntity.imageUri = null;
					mCommon.mEntity.imageBitmap = null;
					updateLinkFields(linkUri);
				}
			}
		};
		final Intent intent = new Intent(Intent.ACTION_MAIN, null);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		ComponentName cm = new ComponentName("com.android.browser", "com.android.browser.CombinedBookmarkHistoryActivity");
		intent.setComponent(cm);
		try {
			startActivityForResult(intent, CandiConstants.ACTIVITY_LINK_PICK);
		}
		catch (Exception exception) {
			/* We fallback to try a different way to construct the component */
			cm = new ComponentName("com.android.browser", "CombinedBookmarkHistoryActivity");
		}
	}

	public void updateLinkFields(final String linkUri) {

		final EditText textUri = (EditText) findViewById(R.id.text_uri);
		@SuppressWarnings("unused")
		final EditText textTitle = (EditText) findViewById(R.id.text_title);
		@SuppressWarnings("unused")
		final EditText textDescription = (EditText) findViewById(R.id.text_content);

		textUri.setText(linkUri);

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showProgressDialog(true, "Validating...");
			}

			@Override
			protected Object doInBackground(Object... params) {

				ServiceRequest serviceRequest = new ServiceRequest();
				serviceRequest.setUri(linkUri);
				serviceRequest.setRequestType(RequestType.Get);
				serviceRequest.setResponseFormat(ResponseFormat.Html);

				ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);
				return serviceResponse;
			}

			@Override
			protected void onPostExecute(Object result) {
				ServiceResponse serviceResponse = (ServiceResponse) result;
				if (serviceResponse.responseCode == ResponseCode.Success) {

					/* We only push values if the user hasn't already supplied some */

					Document document = Jsoup.parse((String) serviceResponse.data);
					
					//if (textTitle.getText().toString().equals("")) {
						((EditText) findViewById(R.id.text_title)).setText(document.title());
					//}

					//if (textDescription.getText().toString().equals("")) {
						String description = null;
						Element element = document.select("meta[name=description]").first();
						if (element != null) {
							description = element.attr("content");
						}

						if (description == null) {
							element = document.select("p[class=description]").first();
							if (element != null) {
								description = element.text();
							}
						}
						if (description == null) {
							element = document.select("p").first();
							if (element != null) {
								description = element.text();
							}
						}

						if (description != null) {
							((EditText) findViewById(R.id.text_content)).setText(description);
						}
						else {
							((EditText) findViewById(R.id.text_content)).setText("");
						}
					//}
				}
				else {
					ImageUtils.showToastNotification(getResources().getString(R.string.web_alert_website_unavailable),
							Toast.LENGTH_SHORT);
				}
				mCommon.showProgressDialog(false, null);
			}
		}.execute();

	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------
	@Override
	protected int getLayoutID() {
		if (mCommon.mEntityType.equals(CandiConstants.TYPE_CANDI_POST)) {
			return R.layout.post_form;
		}
		else if (mCommon.mEntityType.equals(CandiConstants.TYPE_CANDI_PICTURE)) {
			return R.layout.picture_form;
		}
		else if (mCommon.mEntityType.equals(CandiConstants.TYPE_CANDI_LINK)) {
			return R.layout.link_form;
		}
		else {
			return 0;
		}
	}
}