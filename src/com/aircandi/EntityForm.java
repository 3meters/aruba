package com.aircandi;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Criteria;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.aircandi.components.AircandiCommon;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.AnimUtils;
import com.aircandi.components.AnimUtils.TransitionType;
import com.aircandi.components.CommandType;
import com.aircandi.components.GeoLocationManager;
import com.aircandi.components.ImageRequest;
import com.aircandi.components.ImageRequestBuilder;
import com.aircandi.components.ImageUtils;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.ProxiExplorer.ModelResult;
import com.aircandi.components.Tracker;
import com.aircandi.components.Utilities;
import com.aircandi.core.CandiConstants;
import com.aircandi.service.ProxibaseService.RequestListener;
import com.aircandi.service.ProxibaseService.RequestType;
import com.aircandi.service.ProxibaseService.ResponseFormat;
import com.aircandi.service.ServiceRequest;
import com.aircandi.service.objects.Beacon;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Entity.Visibility;
import com.aircandi.service.objects.User;
import com.aircandi.widgets.AuthorBlock;
import com.aircandi.widgets.WebImageView;

public class EntityForm extends FormActivity {

	private ViewFlipper		mViewFlipper;
	protected WebImageView	mImageViewPicture;
	private EditText		mTextUri;
	private boolean			mUriVerified	= false;
	private AsyncTask		mAsyncTask		= null;
	private Bitmap			mEntityBitmap;
	private Entity			mEntityForForm;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/*
		 * Signin required if user is anonymous
		 */
		User user = Aircandi.getInstance().getUser();
		Integer messageResId = (mCommon.mCommandType == CommandType.New ? R.string.signin_message_candi_new : R.string.signin_message_candi_edit);
		if (user != null) {
			Boolean userAnonymous = user.isAnonymous();
			if (userAnonymous) {
				IntentBuilder intentBuilder = new IntentBuilder(this, SignInForm.class);
				intentBuilder.setCommandType(CommandType.Edit);
				intentBuilder.setMessage(getString(messageResId));
				Intent intent = intentBuilder.create();
				startActivityForResult(intent, CandiConstants.ACTIVITY_SIGNIN);
				AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
				return;
			}
		}
		initialize();
		bind();
		draw();
	}

	private void initialize() {
		/*
		 * Starting determining the users location if we are creating new candi. We are pulling
		 * a single shot coarse location which is usually based on network location method.
		 */
		if (mCommon.mCommandType == CommandType.New) {
			GeoLocationManager.getInstance().setCurrentLocation(null);
			Criteria criteria = new Criteria();
			criteria.setAccuracy(Criteria.ACCURACY_COARSE);
			GeoLocationManager.getInstance().getSingleLocationUpdate(null, criteria);
		}

		if (mCommon.mEntityType.equals(CandiConstants.TYPE_CANDI_LINK)) {
			mCommon.mActionBar.setTitle(R.string.form_title_link);
		}
		else if (mCommon.mEntityType.equals(CandiConstants.TYPE_CANDI_PICTURE)) {
			mCommon.mActionBar.setTitle(R.string.form_title_picture);
		}
		else if (mCommon.mEntityType.equals(CandiConstants.TYPE_CANDI_POST)) {
			mCommon.mActionBar.setTitle(R.string.form_title_post);
		}
		else if (mCommon.mEntityType.equals(CandiConstants.TYPE_CANDI_COLLECTION)) {
			mCommon.mActionBar.setTitle(R.string.form_title_collection);
		}

		mImageViewPicture = (WebImageView) findViewById(R.id.image_picture);
		mTextUri = (EditText) findViewById(R.id.text_uri);

		if (mTextUri != null) {
			mTextUri.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					mUriVerified = false;
				}
			});
		}

		mViewFlipper = (ViewFlipper) findViewById(R.id.flipper_form);
		mCommon.setViewFlipper(mViewFlipper);
	}

	private void bind() {
		/*
		 * Fill in the system and default properties for the base entity properties. The activities that subclass this
		 * will set any additional properties beyond the base ones.
		 */
		if (mCommon.mCommandType == CommandType.New) {

			Entity entity = new Entity();
			entity.signalFence = -100.0f;
			entity.creatorId = Aircandi.getInstance().getUser().id;
			entity.modifierId = Aircandi.getInstance().getUser().id;
			entity.enabled = true;
			entity.locked = false;
			entity.linkJavascriptEnabled = false;
			entity.linkZoom = false;
			entity.visibility = Visibility.Public.toString().toLowerCase();
			entity.type = mCommon.mEntityType;

			if (mCommon.mEntityType.equals(CandiConstants.TYPE_CANDI_PICTURE)) {
				entity.imagePreviewUri = "resource:placeholder_logo";
				entity.imageUri = entity.imagePreviewUri;
			}
			else if (mCommon.mEntityType.equals(CandiConstants.TYPE_CANDI_COLLECTION)) {
				entity.title = getString(R.string.entity_collection_title);
				entity.label = entity.title;
				entity.imagePreviewUri = "resource:ic_collection_250";
				entity.imageUri = entity.imagePreviewUri;
			}
			mEntityForForm = entity;
		}
		else {

			if (mEntityForForm == null && mCommon.mEntityId != null) {
				/*
				 * Entity is coming from entity model. We want to create a clone so
				 * that any changes only show up in the entity model if the changes make it
				 * to the service.
				 */
				Entity entityForModel = ProxiExplorer.getInstance().getEntityModel().getEntity(mCommon.mEntityId);
				if (entityForModel != null) {
					mEntityForForm = entityForModel.clone();
					mImageUriOriginal = mEntityForForm.imageUri;
				}
			}
			else {
				if (mEntityForForm != null) {
					mImageUriOriginal = mEntityForForm.imageUri;
				}
			}
		}
	}

	private void draw() {

		if (mEntityForForm != null) {

			final Entity entity = mEntityForForm;

			/* Content */

			if (mImageViewPicture != null) {
				if (entity.imageUri != null && !entity.imageUri.equals("")) {
					if (mEntityBitmap != null) {
						mImageViewPicture.showLoading(false);
						ImageUtils.showImageInImageView(mEntityBitmap, mImageViewPicture.getImageView(), true, AnimUtils.fadeInMedium());
						mImageViewPicture.setVisibility(View.VISIBLE);
					}
					else {
						ImageRequestBuilder builder = new ImageRequestBuilder(mImageViewPicture);
						builder.setImageUri(entity.getMasterImageUri());
						builder.setImageFormat(entity.getMasterImageFormat());
						builder.setLinkZoom(entity.linkZoom);
						builder.setLinkJavascriptEnabled(entity.linkJavascriptEnabled);

						ImageRequest imageRequest = builder.create();
						mImageViewPicture.setImageRequest(imageRequest);
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

			if (entity != null && entity.creator != null) {
				((AuthorBlock) findViewById(R.id.block_author)).bindToAuthor(entity.creator,
						entity.modifiedDate.longValue(), entity.locked);
			}
			else {
				((AuthorBlock) findViewById(R.id.block_author)).setVisibility(View.GONE);
			}

			/* Configure UI */

			if (mCommon.mCommandType == CommandType.New) {
				if (findViewById(R.id.btn_delete_post) != null) {
					((Button) findViewById(R.id.btn_delete_post)).setVisibility(View.GONE);
				}
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onChangePictureButtonClick(View view) {
		showChangePictureDialog(false, mImageViewPicture, new RequestListener() {

			@Override
			public void onComplete(Object response, String imageUri, String linkUri, Bitmap imageBitmap, String title, String description) {

				ServiceResponse serviceResponse = (ServiceResponse) response;
				if (serviceResponse.responseCode == ResponseCode.Success) {
					mEntityBitmap = imageBitmap;
					mEntityForForm.imageUri = imageUri;
					mEntityForForm.imagePreviewUri = imageUri;
					mEntityForForm.linkUri = linkUri;
				}
			}
		});
	}

	public void onSaveButtonClick(View view) {
		doSave();
	}

	public void onDeleteButtonClick(View view) {
		deleteEntityAtService();
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
			}
		}
		else {
			super.onActivityResult(requestCode, resultCode, intent);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	private boolean validate() {
		/*
		 * We only validate the web address if the form had an input for it and
		 * the user set it to something.
		 */
		if (mTextUri != null) {
			String linkUri = mTextUri.getText().toString();
			if (linkUri != null && !linkUri.equals("")) {

				if (!linkUri.startsWith("http://") && !linkUri.startsWith("https://")) {
					linkUri = "http://" + linkUri;
				}

				if (!Utilities.validWebUri(linkUri)) {
					mCommon.showAlertDialogSimple(null, getString(R.string.error_weburi_invalid));
					return false;
				}
			}
		}
		return true;
	}

	private void gather(Entity entity) {
		if (findViewById(R.id.text_title) != null) {
			entity.title = ((TextView) findViewById(R.id.text_title)).getText().toString().trim();
			entity.label = ((TextView) findViewById(R.id.text_title)).getText().toString().trim();
		}
		if (findViewById(R.id.text_content) != null) {
			entity.description = ((TextView) findViewById(R.id.text_content)).getText().toString().trim();
		}
		if (findViewById(R.id.chk_html_javascript) != null) {
			entity.linkJavascriptEnabled = ((CheckBox) findViewById(R.id.chk_html_javascript)).isChecked();
		}
		if (findViewById(R.id.chk_html_zoom) != null) {
			entity.linkZoom = ((CheckBox) findViewById(R.id.chk_html_zoom)).isChecked();
		}
		if (findViewById(R.id.chk_locked) != null) {
			entity.locked = ((CheckBox) findViewById(R.id.chk_locked)).isChecked();
		}
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	private void doSave() {

		if (validate()) {

			new AsyncTask() {

				@Override
				protected void onPreExecute() {
					if (mTextUri != null && !mUriVerified) {
						mCommon.showProgressDialog(true, getString(R.string.progress_verifying));
					}
				}

				@Override
				protected Object doInBackground(Object... params) {
					ModelResult result = new ModelResult();
					/*
					 * If using uri then we have already checked to see if it is a well formed
					 * web address by now
					 */
					if (mTextUri != null) {

						String linkUri = mTextUri.getText().toString();
						if (linkUri != null && !linkUri.equals("")) {
							if (!linkUri.startsWith("http://") && !linkUri.startsWith("https://")) {
								linkUri = "http://" + linkUri;
							}

							ServiceRequest serviceRequest = new ServiceRequest()
									.setUri(linkUri)
									.setRequestType(RequestType.Get)
									.setResponseFormat(ResponseFormat.Html)
									.setSuppressUI(true);

							result.serviceResponse = NetworkManager.getInstance().request(serviceRequest);

							/*
							 * Success means the uri was verified.
							 */
							if (result.serviceResponse.responseCode == ResponseCode.Success) {
								mEntityForForm.linkUri = linkUri;
								mEntityForForm.imageUri = null;
								mEntityBitmap = null;
								mUriVerified = true;
							}
						}
					}

					if (result.serviceResponse.responseCode == ResponseCode.Success) {

						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								mCommon.showProgressDialog(true, getString(R.string.progress_saving));
							}
						});

						if (mCommon.mCommandType == CommandType.New) {
							/*
							 * Pull all the control values back into the entity object
							 */
							gather(mEntityForForm);
							result = insertEntityAtService();

							if (result.serviceResponse.responseCode == ResponseCode.Success) {
								ImageUtils.showToastNotification(getString(R.string.alert_inserted), Toast.LENGTH_SHORT);
								setResult(CandiConstants.RESULT_ENTITY_INSERTED);
							}
						}
						else if (mCommon.mCommandType == CommandType.Edit) {
							/*
							 * Pull all the control values back into the entity object being used to
							 * update the service. Because the entity reference comes from an entity model
							 * collection, that entity gets updated.
							 */
							gather(mEntityForForm);
							result = updateEntityAtService();

							if (result.serviceResponse.responseCode == ResponseCode.Success) {
								ImageUtils.showToastNotification(getString(R.string.alert_updated), Toast.LENGTH_SHORT);
								setResult(CandiConstants.RESULT_ENTITY_UPDATED);
							}
						}
					}
					return result.serviceResponse;
				}

				@Override
				protected void onPostExecute(Object response) {
					ServiceResponse serviceResponse = (ServiceResponse) response;
					mCommon.showProgressDialog(false, null);
					if (serviceResponse.responseCode == ResponseCode.Success) {
						finish();
					}
					else {
						mCommon.handleServiceError(serviceResponse, ServiceOperation.CandiSave, EntityForm.this);
					}
				}

			}.execute();
		}
	}

	private ModelResult insertEntityAtService() {
		ModelResult result = new ModelResult();

		/* Get strongest nearby beacon and alert if none */
		Beacon beacon = ProxiExplorer.getInstance().getStrongestWifiAsBeacon();
		if (beacon == null && mCommon.mParentId == null) {
			AircandiCommon.showAlertDialog(R.drawable.icon_app
					, "Aircandi beacons"
					, getString(R.string.alert_beacons_zero)
					, null
					, EntityForm.this, android.R.string.ok, null, new
					DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {}
					}, null);
			result.serviceResponse = new ServiceResponse(ResponseCode.Failed, null, null);
			return result;
		}

		/* If parent id then this is a child */
		if (mCommon.mParentId != null) {
			mEntityForForm.root = false;
			mEntityForForm.parentId = mCommon.mParentId;
			mEntityForForm.beaconId = null;
		}
		else {
			mEntityForForm.root = true;
			mEntityForForm.parentId = null;
			mEntityForForm.beaconId = beacon.id;
		}

		result = ProxiExplorer.getInstance().getEntityModel().insertEntity(mEntityForForm, beacon, mEntityBitmap, false);
		return result;
	}

	private ModelResult updateEntityAtService() {
		ModelResult result = ProxiExplorer.getInstance().getEntityModel().updateEntity(mEntityForForm, mEntityBitmap, false);
		return result;
	}

	private void deleteEntityAtService() {
		/*
		 * TODO: We need to update the service so the recursive entity delete also deletes any associated resources
		 * stored with S3. As currently coded, we will be orphaning any images associated with child entities.
		 */

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showProgressDialog(true, getString(R.string.progress_deleting));
			}

			@Override
			protected Object doInBackground(Object... params) {

				ModelResult result = ProxiExplorer.getInstance().getEntityModel().deleteEntity(mEntityForForm.id, false);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				ModelResult result = (ModelResult) response;

				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					Tracker.trackEvent("Entity", "Delete", mEntityForForm.type, 0);
					Logger.i(this, "Deleted entity: " + mEntityForForm.title);

					mCommon.showProgressDialog(false, null);
					ImageUtils.showToastNotification(getString(R.string.alert_deleted), Toast.LENGTH_SHORT);
					setResult(CandiConstants.RESULT_ENTITY_DELETED);
					finish();
				}
				else {
					mCommon.handleServiceError(result.serviceResponse, ServiceOperation.CandiDelete, EntityForm.this);
				}
			}

		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	private void showBookmarkActivity() {

		Intent intent = new Intent(this, BookmarkPicker.class);
		intent.putExtra(getString(R.string.EXTRA_VERIFY_URI), false);

		startActivityForResult(intent, CandiConstants.ACTIVITY_LINK_PICK);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);

		super.mImageRequestListener = new RequestListener() {

			@Override
			public void onComplete(Object response, String imageUri, String linkUri, Bitmap imageBitmap, String title, String description) {

				ServiceResponse serviceResponse = (ServiceResponse) response;
				if (serviceResponse.responseCode == ResponseCode.Success) {
					/*
					 * Validate before we move on
					 */
					if (!linkUri.startsWith("http://") && !linkUri.startsWith("https://")) {
						linkUri = "http://" + linkUri;
					}

					if (!Utilities.validWebUri(linkUri)) {
						mCommon.showAlertDialogSimple(null, getString(R.string.error_weburi_invalid));
						return;
					}
					else {
						mEntityForForm.linkUri = linkUri;
						mEntityForForm.imageUri = null;
						mEntityBitmap = null;
						updateLinkFields(linkUri);
					}
				}
			}
		};
	}

	private void updateLinkFields(final String linkUri) {
		/*
		 * Link has been validated before we get here.
		 */
		final EditText textUri = (EditText) findViewById(R.id.text_uri);
		@SuppressWarnings("unused")
		final EditText textTitle = (EditText) findViewById(R.id.text_title);
		@SuppressWarnings("unused")
		final EditText textDescription = (EditText) findViewById(R.id.text_content);

		textUri.setText(linkUri);
		mUriVerified = false;

		mAsyncTask = new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.getProgressDialog().setOnCancelListener(new DialogInterface.OnCancelListener() {

					public void onCancel(DialogInterface dialog) {
						mAsyncTask.cancel(true);
						ImageUtils.showToastNotification("Validation canceled", Toast.LENGTH_SHORT);
					}
				});
				mCommon.showProgressDialog(true, getString(R.string.progress_verifying));
			}

			@Override
			protected Object doInBackground(Object... params) {

				ServiceRequest serviceRequest = new ServiceRequest()
						.setUri(linkUri)
						.setRequestType(RequestType.Get)
						.setResponseFormat(ResponseFormat.Html);

				ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);
				return serviceResponse;
			}

			@Override
			protected void onPostExecute(Object result) {
				ServiceResponse serviceResponse = (ServiceResponse) result;
				if (serviceResponse.responseCode == ResponseCode.Success) {
					mUriVerified = true;
					/*
					 * We only push values if the user hasn't already supplied some
					 */
					Document document = Jsoup.parse((String) serviceResponse.data);

					((EditText) findViewById(R.id.text_title)).setText(document.title());

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
					mCommon.showProgressDialog(false, null);
				}
				else {
					mCommon.handleServiceError(serviceResponse, ServiceOperation.LinkLookup, EntityForm.this);
				}
			}
		}.execute();

	}

	// --------------------------------------------------------------------------------------------
	// Persistence routines
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Lifecycle routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onResume() {
		super.onResume();
		mCommon.startScanService();
	}

	@Override
	protected void onPause() {
		mCommon.stopScanService();
		super.onPause();
	}

	protected void onDestroy() {
		super.onDestroy();
		if (mEntityBitmap != null && !mEntityBitmap.isRecycled()) {
			mEntityBitmap.recycle();
			mEntityBitmap = null;
		}
		System.gc();
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
		else if (mCommon.mEntityType.equals(CandiConstants.TYPE_CANDI_COLLECTION)) {
			return R.layout.collection_form;
		}
		else {
			return 0;
		}
	}
}