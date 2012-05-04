package com.proxibase.aircandi;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Criteria;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
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

import com.proxibase.aircandi.components.AircandiCommon;
import com.proxibase.aircandi.components.AircandiCommon.ServiceOperation;
import com.proxibase.aircandi.components.Command;
import com.proxibase.aircandi.components.Command.CommandType;
import com.proxibase.aircandi.components.DateUtils;
import com.proxibase.aircandi.components.GeoLocationManager;
import com.proxibase.aircandi.components.ImageManager;
import com.proxibase.aircandi.components.ImageRequest;
import com.proxibase.aircandi.components.ImageRequest.ImageResponse;
import com.proxibase.aircandi.components.ImageRequestBuilder;
import com.proxibase.aircandi.components.ImageUtils;
import com.proxibase.aircandi.components.IntentBuilder;
import com.proxibase.aircandi.components.Logger;
import com.proxibase.aircandi.components.NetworkManager;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ResponseCodeDetail;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.components.ProxiExplorer;
import com.proxibase.aircandi.components.S3;
import com.proxibase.aircandi.components.Tracker;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.widgets.AuthorBlock;
import com.proxibase.aircandi.widgets.WebImageView;
import com.proxibase.service.ProxiConstants;
import com.proxibase.service.ProxibaseService;
import com.proxibase.service.ProxibaseService.GsonType;
import com.proxibase.service.ProxibaseService.RequestListener;
import com.proxibase.service.ProxibaseService.RequestType;
import com.proxibase.service.ProxibaseService.ResponseFormat;
import com.proxibase.service.ProxibaseServiceException;
import com.proxibase.service.ServiceRequest;
import com.proxibase.service.objects.Beacon;
import com.proxibase.service.objects.Beacon.BeaconType;
import com.proxibase.service.objects.Entity;
import com.proxibase.service.objects.Entity.Visibility;
import com.proxibase.service.objects.Link;
import com.proxibase.service.objects.Observation;
import com.proxibase.service.objects.ServiceData;
import com.proxibase.service.objects.User;

public class EntityForm extends FormActivity {

	private ViewFlipper		mViewFlipper;
	protected WebImageView	mImagePicture;
	private EditText		mTextUri;
	private boolean			mUriValidated	= false;
	private AsyncTask		mAsyncTask		= null;
	private Observation		mObservation;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		Logger.d(this, "onCreate called");

		User user = Aircandi.getInstance().getUser();
		if (user != null && user.anonymous) {
			IntentBuilder intentBuilder = new IntentBuilder(this, SignInForm.class);
			intentBuilder.setCommand(new Command(CommandType.Edit));
			intentBuilder.setMessage(getString(R.string.signin_message_new_candi));
			Intent intent = intentBuilder.create();

			startActivityForResult(intent, CandiConstants.ACTIVITY_SIGNIN);
			overridePendingTransition(R.anim.form_in, R.anim.browse_out);
		}

		initialize();

		/* Restore current tab */
		if (savedInstanceState != null) {
			if (findViewById(R.id.image_tab_host) != null) {
				mCommon.setActiveTab(((ViewGroup) findViewById(R.id.image_tab_host)).getChildAt(savedInstanceState
						.getInt("tab_index")));
				mViewFlipper.setDisplayedChild(mCommon.mTabIndex);
			}
		}

		if (bind(savedInstanceState)) {
			draw();

			/* Can overwrite any values from the original intent */
			if (savedInstanceState != null) {
				doRestoreInstanceState(savedInstanceState);
			}
		}
	}

	protected void initialize() {

		/* Starting determining the users location if we are creating new candi. */
		if (mCommon.mCommand.type == CommandType.New) {
			GeoLocationManager.getInstance().setCurrentLocation(null);
			Criteria criteria = new Criteria();
			criteria.setAccuracy(Criteria.ACCURACY_COARSE);
			GeoLocationManager.getInstance().getSingleLocationUpdate(null, criteria);
			// GeoLocationManager.getInstance().getLastBestLocation(CandiConstants.LOCATION_MAX_DISTANCE,
			// System.currentTimeMillis() - CandiConstants.LOCATION_MAX_TIME,
			// criteria, null);
		}

		/* Tracking */
		mCommon.track();

		mImagePicture = (WebImageView) findViewById(R.id.image_picture);
		mTextUri = (EditText) findViewById(R.id.text_uri);

		if (mTextUri != null) {
			mTextUri.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					mUriValidated = false;
				}
			});
		}

		mViewFlipper = (ViewFlipper) findViewById(R.id.flipper_form);
		if (mViewFlipper != null) {
			mCommon.setActiveTab(((ViewGroup) findViewById(R.id.image_tab_host)).getChildAt(0));
		}
	}

	protected boolean bind(final Bundle savedInstanceState) {
		/*
		 * Fill in the system and default properties for the base entity properties. The activities that subclass this
		 * will set any additional properties beyond the base ones.
		 */
		if (mCommon.mCommand.type == CommandType.New) {

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
				entity.imagePreviewUri = "resource:placeholder_picture";
				entity.imageUri = entity.imagePreviewUri;
			}
			mCommon.mEntity = entity;
			return true;
		}
		else {
			if (mCommon.mEntity == null && mCommon.mEntityId != null) {

				new AsyncTask() {

					@Override
					protected void onPreExecute() {
						mCommon.showProgressDialog(true, "Loading...");
					}

					@Override
					protected Object doInBackground(Object... params) {
						String jsonEagerLoad = "{\"children\":false,\"comments\":false}";
						ServiceResponse serviceResponse = ProxiExplorer.getInstance().getEntity(mCommon.mEntityId, jsonEagerLoad, null, null);
						return serviceResponse;
					}

					@Override
					protected void onPostExecute(Object response) {
						ServiceResponse serviceResponse = (ServiceResponse) response;
						if (serviceResponse.responseCode == ResponseCode.Success) {

							mCommon.showProgressDialog(false, null);
							mCommon.mEntity = (Entity) ((ServiceData) serviceResponse.data).data;
							mImageUriOriginal = mCommon.mEntity.imageUri;

							draw();

							/* Can overwrite any values from the original intent */
							if (savedInstanceState != null) {
								doRestoreInstanceState(savedInstanceState);
							}
						}
						else {
							mCommon.handleServiceError(serviceResponse);
						}
					}
				}.execute();
				return false;
			}
			else {
				if (mCommon.mEntity != null) {
					mImageUriOriginal = mCommon.mEntity.imageUri;
				}
				return true;
			}
		}
	}

	protected void draw() {

		if (mCommon.mEntity != null) {

			final Entity entity = mCommon.mEntity;

			/* Content */

			if (mImagePicture != null) {
				if (entity.imageUri != null && !entity.imageUri.equals("")) {
					if (entity.imageBitmap != null) {
						ImageUtils.showImageInImageView(entity.imageBitmap, mImagePicture.getImageView(), true, R.anim.fade_in_medium);
						mImagePicture.setVisibility(View.VISIBLE);
					}
					else {

						ImageRequestBuilder builder = new ImageRequestBuilder(mImagePicture);
						builder.setImageUri(entity.getMasterImageUri());
						builder.setImageFormat(entity.getMasterImageFormat());
						builder.setLinkZoom(entity.linkZoom);
						builder.setLinkJavascriptEnabled(entity.linkJavascriptEnabled);
						builder.setRequestListener(new RequestListener() {

							@Override
							public void onComplete(Object response) {
								ServiceResponse serviceResponse = (ServiceResponse) response;
								if (serviceResponse.responseCode == ResponseCode.Success) {
									ImageResponse imageResponse = (ImageResponse) serviceResponse.data;
									entity.imageBitmap = imageResponse.bitmap;
								}
							}
						});

						ImageRequest imageRequest = builder.create();
						mImagePicture.setImageRequest(imageRequest);
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
				if (entity.visibility.toLowerCase().equals(Visibility.Private.toString().toLowerCase())) {
					((Spinner) findViewById(R.id.cbo_visibility)).setSelection(Visibility.Private.ordinal());
				}
				else if (entity.visibility.toLowerCase().equals(Visibility.Public.toString().toLowerCase())) {
					((Spinner) findViewById(R.id.cbo_visibility)).setSelection(Visibility.Public.ordinal());
				}
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

			if (mCommon.mCommand.type == CommandType.New) {
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
		doSave();
	}

	public void onDeleteButtonClick(View view) {
		mCommon.startTitlebarProgress();
		delete();
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
				bind(null);
				draw();
			}
		}
		super.onActivityResult(requestCode, resultCode, intent);
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	protected void doSave() {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				if (mTextUri != null && !mUriValidated) {
					mCommon.showProgressDialog(true, "Validating...");
				}
			}

			@Override
			protected Object doInBackground(Object... params) {
				ServiceResponse serviceResponse = new ServiceResponse();

				/* If using uri then validate */
				if (mTextUri != null && !mUriValidated) {

					String linkUri = mTextUri.getText().toString();
					if (!linkUri.startsWith("http://") && !linkUri.startsWith("https://")) {
						linkUri = "http://" + linkUri;
					}

					if (!validateUriSyntax(linkUri)) {
						ImageUtils.showToastNotification("Invalid link specified", Toast.LENGTH_SHORT);
						serviceResponse = new ServiceResponse(ResponseCode.Failed,
								ResponseCodeDetail.ProtocolException, null, null);
					}

					if (serviceResponse.responseCode == ResponseCode.Success) {

						ServiceRequest serviceRequest = new ServiceRequest();
						serviceRequest.setUri(linkUri);
						serviceRequest.setRequestType(RequestType.Get);
						serviceRequest.setResponseFormat(ResponseFormat.Html);
						serviceRequest.setSuppressUI(true);

						serviceResponse = NetworkManager.getInstance().request(serviceRequest);
						if (serviceResponse.responseCode == ResponseCode.Success) {

							mCommon.mEntity.linkUri = linkUri;
							mCommon.mEntity.imageUri = null;
							mCommon.mEntity.imageBitmap = null;
							mUriValidated = true;
						}
					}
				}

				/* Insert beacon if it isn't already registered */
				if (serviceResponse.responseCode == ResponseCode.Success) {

					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							mCommon.showProgressDialog(true, "Saving...");
						}
					});

					/* Pull all the control values back into the entity object */
					gather();

					if (serviceResponse.responseCode == ResponseCode.Success) {
						if (mCommon.mCommand.type == CommandType.New) {
							serviceResponse = insert();
							if (serviceResponse.responseCode == ResponseCode.Success) {
								ImageUtils.showToastNotification(getString(R.string.alert_inserted), Toast.LENGTH_SHORT);
								setResult(CandiConstants.RESULT_ENTITY_INSERTED);
							}
						}
						else if (mCommon.mCommand.type == CommandType.Edit) {
							serviceResponse = update();
							if (serviceResponse.responseCode == ResponseCode.Success) {
								ImageUtils.showToastNotification(getString(R.string.alert_updated), Toast.LENGTH_SHORT);
								setResult(CandiConstants.RESULT_ENTITY_UPDATED);
							}
						}
					}
				}
				return serviceResponse;
			}

			@Override
			protected void onPostExecute(Object response) {
				ServiceResponse serviceResponse = (ServiceResponse) response;
				if (serviceResponse.responseCode == ResponseCode.Success) {
					mCommon.showProgressDialog(false, null);
					finish();
				}
				else {
					mCommon.handleServiceError(serviceResponse, ServiceOperation.CandiSave, EntityForm.this);
				}
			}
		}.execute();
	}

	private boolean validateUriSyntax(String uri) {

		// Validate URL
		if (!URLUtil.isValidUrl(uri)) {
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
			Visibility visibility = Visibility.values()[((Spinner) findViewById(R.id.cbo_visibility))
					.getSelectedItemPosition()];
			mCommon.mEntity.visibility = visibility.toString().toLowerCase();
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

	protected ServiceResponse insert() {

		Entity entity = mCommon.mEntity;

		Logger.i(this, "Inserting entity: " + entity.title);
		Tracker.trackEvent("Entity", "Insert", entity.type, 0);
		ServiceResponse serviceResponse = new ServiceResponse();

		/* Get strongest nearby beacon and alert if none */
		Beacon beacon = ProxiExplorer.getInstance().getStrongestWifiAsBeacon();
		if (beacon == null && mCommon.mParentId == null) {
			AircandiCommon.showAlertDialog(R.drawable.icon_app, "Aircandi beacons",
					getString(R.string.alert_beacons_zero),
					EntityForm.this, android.R.string.ok, null, new
					DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {}
					});
			return new ServiceResponse(ResponseCode.Failed, ResponseCodeDetail.UnknownException, null, null);
		}

		/* If parent id then this is a child */
		if (mCommon.mParentId != null) {
			entity.root = false;
			entity.parentId = mCommon.mParentId;
		}
		else {
			entity.root = true;
			entity.parentId = null;
			entity.beaconId = beacon.id;
		}

		if (serviceResponse.responseCode == ResponseCode.Success) {
			serviceResponse = updateImages(); /* Upload images to S3 as needed. */
		}

		if (serviceResponse.responseCode == ResponseCode.Success) {

			// Construct entity, link, and observation
			Bundle parameters = new Bundle();

			/* User */
			parameters.putString("userId", Aircandi.getInstance().getUser().id);

			/*
			 * We record an observation if we found a nearby beacon. Beacon might not be registered with proxibase but
			 * will be after the call.
			 */
			if (beacon != null) {
				mObservation = GeoLocationManager.getInstance().getObservation();
				if (mObservation != null) {
					mObservation.beaconId = beacon.id;
					parameters.putString("observation",
							"object:" + ProxibaseService.convertObjectToJson(mObservation, GsonType.ProxibaseService));
				}
			}

			/* Beacon */
			if (beacon != null) {
				if (GeoLocationManager.getInstance().getCurrentLocation() != null) {
					Location currentLocation = GeoLocationManager.getInstance().getCurrentLocation();

					beacon.latitude = currentLocation.getLatitude();
					beacon.longitude = currentLocation.getLongitude();
					if (currentLocation.hasAltitude()) {
						beacon.altitude = currentLocation.getAltitude();
					}
					if (currentLocation.hasAccuracy()) {
						beacon.accuracy = currentLocation.getAccuracy();
					}
					if (currentLocation.hasBearing()) {
						beacon.bearing = currentLocation.getBearing();
					}
					if (currentLocation.hasSpeed()) {
						beacon.speed = currentLocation.getSpeed();
					}
				}

				beacon.beaconType = BeaconType.Fixed.name().toLowerCase();
				beacon.ownerId = Aircandi.getInstance().getUser().id;
				beacon.creatorId = Aircandi.getInstance().getUser().id;
				beacon.modifierId = Aircandi.getInstance().getUser().id;
				beacon.locked = false;

				parameters.putString("beacon",
						"object:" + ProxibaseService.convertObjectToJson(beacon, GsonType.ProxibaseService));
			}

			/* Link */
			Link link = new Link();
			link.toId = mCommon.mParentId == null ? beacon.id : entity.parentId;
			parameters.putString("link",
					"object:" + ProxibaseService.convertObjectToJson(link, GsonType.ProxibaseService));

			/* Entity */
			parameters.putString("entity",
					"object:" + ProxibaseService.convertObjectToJson(entity, GsonType.ProxibaseService));

			ServiceRequest serviceRequest = new ServiceRequest();
			serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "insertEntity");
			serviceRequest.setRequestType(RequestType.Method);
			serviceRequest.setParameters(parameters);
			serviceRequest.setResponseFormat(ResponseFormat.Json);

			serviceResponse = NetworkManager.getInstance().request(serviceRequest);
		}

		if (serviceResponse.responseCode == ResponseCode.Success) {
			Tracker.trackEvent("Entity", "New", mCommon.mEntityType, 0);
		}

		return serviceResponse;
	}

	protected ServiceResponse update() {

		ServiceResponse serviceResponse = new ServiceResponse();

		/* Upload new images to S3 as needed. */
		if (serviceResponse.responseCode == ResponseCode.Success) {
			serviceResponse = updateImages();
		}

		if (serviceResponse.responseCode == ResponseCode.Success) {
			mCommon.mEntity.modifierId = Aircandi.getInstance().getUser().id;
			Logger.i(this, "Updating entity: " + mCommon.mEntity.title);
			Tracker.trackEvent("Entity", "Update", mCommon.mEntity.type, 0);

			// Construct entity, link, and observation
			Bundle parameters = new Bundle();
			parameters.putBoolean("skipActivityDate", false);
			parameters.putString("entity",
					"object:" + ProxibaseService.convertObjectToJson(mCommon.mEntity, GsonType.ProxibaseService));

			ServiceRequest serviceRequest = new ServiceRequest();
			serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "updateEntity");
			serviceRequest.setRequestType(RequestType.Method);
			serviceRequest.setParameters(parameters);
			serviceRequest.setResponseFormat(ResponseFormat.Json);
			serviceResponse = NetworkManager.getInstance().request(serviceRequest);
		}

		if (serviceResponse.responseCode == ResponseCode.Success) {
			Tracker.trackEvent("Entity", "Update", mCommon.mEntityType, 0);
		}

		return serviceResponse;
	}

	protected void delete() {
		/*
		 * TODO: We need to update the service so the recursive entity delete also deletes any associated resources
		 * stored with S3. As currently coded, we will be orphaning any images associated with child entities.
		 */

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showProgressDialog(true, "Deleting...");
			}

			@Override
			protected Object doInBackground(Object... params) {
				ServiceResponse serviceResponse = new ServiceResponse();

				/*
				 * If there is an image stored with S3 then delete it.
				 * TODO: Flag image for garbage collection but don't
				 * delete it because because it might be needed while aircandi users have current sessions.
				 */
				if (mCommon.mEntity.imagePreviewUri != null
						&& !mCommon.mEntity.imagePreviewUri.equals("")
						&& !ImageManager.isLocalImage(mCommon.mEntity.imagePreviewUri)) {
					try {
						// String imageKey =
						// mCommon.mEntity.imagePreviewUri.substring(mCommon.mEntity.imagePreviewUri.lastIndexOf("/") +
						// 1);
						// S3.deleteImage(imageKey);
						/*
						 * Associated images are removed from the local image cache when the candi model is finally
						 * removed and the cand view is killed or recycled
						 */
					}
					catch (ProxibaseServiceException exception) {
						serviceResponse = new ServiceResponse(ResponseCode.Failed, ResponseCodeDetail.ServiceException, null, exception);
					}
				}

				/*
				 * Delete the entity and all links and observations it is associated with. We attempt to continue even
				 * if the call to delete the image failed.
				 */
				Logger.i(this, "Deleting entity: " + mCommon.mEntity.title);

				Bundle parameters = new Bundle();
				parameters.putString("entityId", mCommon.mEntity.id);
				parameters.putBoolean("deleteChildren", true);

				ServiceRequest serviceRequest = new ServiceRequest();
				serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "deleteEntity");
				serviceRequest.setRequestType(RequestType.Method);
				serviceRequest.setParameters(parameters);
				serviceRequest.setResponseFormat(ResponseFormat.Json);
				serviceResponse = NetworkManager.getInstance().request(serviceRequest);
				return serviceResponse;
			}

			@Override
			protected void onPostExecute(Object response) {
				ServiceResponse serviceResponse = (ServiceResponse) response;

				if (serviceResponse.responseCode == ResponseCode.Success) {
					Tracker.trackEvent("Entity", "Delete", mCommon.mEntityType, 0);
					Logger.i(this, "Deleted entity: " + mCommon.mEntity.title);

					mCommon.showProgressDialog(false, null);
					ImageUtils.showToastNotification(getString(R.string.alert_deleted), Toast.LENGTH_SHORT);
					setResult(CandiConstants.RESULT_ENTITY_DELETED);
					finish();
				}
				else {
					mCommon.handleServiceError(serviceResponse, ServiceOperation.CandiDelete, EntityForm.this);
				}
			}
		}.execute();
	}

	protected ServiceResponse updateImages() {

		/*
		 * Delete image from S3 if it has been orphaned TODO: We are going with a garbage collection scheme for orphaned
		 * images. We need to use an extended property on S3 items that is set to a date when collection is ok. This
		 * allows downloaded entities to keep working even if an image for entity has changed.
		 */
		ServiceResponse serviceResponse = new ServiceResponse();

		/* Upload image to S3 if we have a new one. */
		if (mCommon.mEntity.imageBitmap != null) {
			try {
				String imageKey = String.valueOf(Aircandi.getInstance().getUser().id) + "_"
						+ String.valueOf(DateUtils.nowString(DateUtils.DATE_NOW_FORMAT_FILENAME))
						+ ".jpg";
				S3.putImage(imageKey, mCommon.mEntity.imageBitmap);
				mCommon.mEntity.imagePreviewUri = CandiConstants.URL_AIRCANDI_MEDIA + CandiConstants.S3_BUCKET_IMAGES
						+ "/" + imageKey;
				if (mCommon.mEntity.imageUri == null || mCommon.mEntity.imageUri.equals("")) {
					mCommon.mEntity.imageUri = mCommon.mEntity.imagePreviewUri;
				}
			}
			catch (ProxibaseServiceException exception) {
				return new ServiceResponse(ResponseCode.Failed, ResponseCodeDetail.ServiceException, null, exception);
			}
		}
		return serviceResponse;
	}

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	private void showBookmarkActivity() {

		Intent intent = new Intent(this, BookmarkList.class);
		startActivityForResult(intent, CandiConstants.ACTIVITY_LINK_PICK);
		overridePendingTransition(R.anim.form_in, R.anim.browse_out);

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
		// final Intent intent = new Intent(Intent.ACTION_MAIN, null);
		// intent.addCategory(Intent.CATEGORY_LAUNCHER);
		// ComponentName cm = new ComponentName("com.android.browser",
		// "com.android.browser.CombinedBookmarkHistoryActivity");
		// intent.setComponent(cm);
		// try {
		// startActivityForResult(intent, CandiConstants.ACTIVITY_LINK_PICK);
		// overridePendingTransition(R.anim.form_in, R.anim.browse_out);
		// }
		// catch (Exception exception) {
		// /* We fallback to try a different way to construct the component */
		// Logger.d(this, "Exception launching bookmark activity");
		// cm = new ComponentName("com.android.browser", "CombinedBookmarkHistoryActivity");
		// }
	}

	public void updateLinkFields(final String linkUri) {

		final EditText textUri = (EditText) findViewById(R.id.text_uri);
		@SuppressWarnings("unused")
		final EditText textTitle = (EditText) findViewById(R.id.text_title);
		@SuppressWarnings("unused")
		final EditText textDescription = (EditText) findViewById(R.id.text_content);

		textUri.setText(linkUri);

		mAsyncTask = new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

					public void onCancel(DialogInterface dialog) {
						mAsyncTask.cancel(true);
						ImageUtils.showToastNotification("Validation canceled", Toast.LENGTH_SHORT);
					}
				});
				mCommon.showProgressDialog(true, "Validating...");
			}

			@Override
			protected Object doInBackground(Object... params) {

				if (!validateUriSyntax(linkUri)) {
					ImageUtils.showToastNotification("Invalid link specified", Toast.LENGTH_SHORT);
					return new ServiceResponse(ResponseCode.Failed, ResponseCodeDetail.ProtocolException, null, null);
				}

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
					mUriValidated = true;

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

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		Logger.d(this, "onSaveInstanceState called");

		if (findViewById(R.id.text_title) != null) {
			savedInstanceState.putString("title", ((TextView) findViewById(R.id.text_title)).getText().toString());
		}
		if (findViewById(R.id.text_content) != null) {
			savedInstanceState.putString("content", ((TextView) findViewById(R.id.text_content)).getText().toString());
		}
		if (findViewById(R.id.text_uri) != null) {
			savedInstanceState.putString("uri", ((TextView) findViewById(R.id.text_uri)).getText().toString());
		}
		if (findViewById(R.id.cbo_visibility) != null) {
			savedInstanceState.putInt("visibility",
					((Spinner) findViewById(R.id.cbo_visibility)).getSelectedItemPosition());
		}
		if (findViewById(R.id.chk_html_javascript) != null) {
			savedInstanceState.putBoolean("html_javascript",
					((CheckBox) findViewById(R.id.chk_html_javascript)).isChecked());
		}
		if (findViewById(R.id.chk_html_zoom) != null) {
			savedInstanceState.putBoolean("html_zoom", ((CheckBox) findViewById(R.id.chk_html_zoom)).isChecked());
		}
		if (findViewById(R.id.chk_locked) != null) {
			savedInstanceState.putBoolean("locked", ((CheckBox) findViewById(R.id.chk_locked)).isChecked());
		}
		if (findViewById(R.id.image_tab_host) != null) {
			savedInstanceState.putInt("tab_index", mCommon.mTabIndex);
		}
	}

	private void doRestoreInstanceState(Bundle savedInstanceState) {
		Logger.d(this, "Restoring previous state");

		if (findViewById(R.id.text_title) != null) {
			((TextView) findViewById(R.id.text_title)).setText(savedInstanceState.getString("title"));
		}
		if (findViewById(R.id.text_content) != null) {
			((TextView) findViewById(R.id.text_content)).setText(savedInstanceState.getString("content"));
		}
		if (findViewById(R.id.text_uri) != null) {
			((TextView) findViewById(R.id.text_uri)).setText(savedInstanceState.getString("uri"));
		}
		if (findViewById(R.id.cbo_visibility) != null) {
			((Spinner) findViewById(R.id.cbo_visibility)).setSelection(savedInstanceState.getInt("visibility"));
		}
		if (findViewById(R.id.chk_html_zoom) != null) {
			((CheckBox) findViewById(R.id.chk_html_zoom)).setChecked(savedInstanceState.getBoolean("html_zoom"));
		}

		if (findViewById(R.id.chk_html_javascript) != null) {
			((CheckBox) findViewById(R.id.chk_html_javascript)).setChecked(savedInstanceState
					.getBoolean("html_javascript"));
		}
		if (findViewById(R.id.chk_locked) != null) {
			((CheckBox) findViewById(R.id.chk_locked)).setChecked(savedInstanceState.getBoolean("locked"));
		}
	}

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
		super.onPause();
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