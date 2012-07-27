package com.proxibase.aircandi;

import java.util.Collections;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Criteria;
import android.location.Location;
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

import com.proxibase.aircandi.components.AircandiCommon;
import com.proxibase.aircandi.components.AircandiCommon.ServiceOperation;
import com.proxibase.aircandi.components.AnimUtils.TransitionType;
import com.proxibase.aircandi.components.AnimUtils;
import com.proxibase.aircandi.components.CommandType;
import com.proxibase.aircandi.components.DateUtils;
import com.proxibase.aircandi.components.EntityList;
import com.proxibase.aircandi.components.GeoLocationManager;
import com.proxibase.aircandi.components.ImageManager;
import com.proxibase.aircandi.components.ImageRequest;
import com.proxibase.aircandi.components.ImageRequestBuilder;
import com.proxibase.aircandi.components.ImageUtils;
import com.proxibase.aircandi.components.IntentBuilder;
import com.proxibase.aircandi.components.Logger;
import com.proxibase.aircandi.components.NetworkManager;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ResponseCodeDetail;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.components.ProxiExplorer;
import com.proxibase.aircandi.components.ProxiExplorer.EntityTree;
import com.proxibase.aircandi.components.S3;
import com.proxibase.aircandi.components.Tracker;
import com.proxibase.aircandi.components.Utilities;
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
import com.proxibase.service.ProxibaseServiceException.ErrorCode;
import com.proxibase.service.ServiceRequest;
import com.proxibase.service.objects.Beacon;
import com.proxibase.service.objects.Beacon.BeaconType;
import com.proxibase.service.objects.Entity;
import com.proxibase.service.objects.Entity.Visibility;
import com.proxibase.service.objects.Link;
import com.proxibase.service.objects.Observation;
import com.proxibase.service.objects.Result;
import com.proxibase.service.objects.ServiceData;
import com.proxibase.service.objects.User;

public class EntityForm extends FormActivity {

	private ViewFlipper		mViewFlipper;
	protected WebImageView	mImagePicture;
	private EditText		mTextUri;
	private boolean			mUriVerified	= false;
	private AsyncTask		mAsyncTask		= null;
	private Observation		mObservation;
	private Bitmap			mEntityBitmap;
	private Entity			mEntityForForm;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/*
		 * Two sign in cases:
		 * 
		 * - Currently anonymous.
		 * - Session expired.
		 */
		User user = Aircandi.getInstance().getUser();
		Boolean expired = false;
		Integer messageResId = (mCommon.mCommandType == CommandType.New ? R.string.signin_message_candi_new : R.string.signin_message_candi_edit);
		if (user != null) {
			Boolean userAnonymous = user.anonymous;
			if (user.session != null) {
				expired = user.session.renewSession(DateUtils.nowDate().getTime());
			}
			if (userAnonymous || expired) {
				if (expired) {
					messageResId = R.string.signin_message_session_expired;
				}
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

		mImagePicture = (WebImageView) findViewById(R.id.image_picture);
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
				entity.imagePreviewUri = "resource:placeholder_picture";
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
				Entity entityForModel = ProxiExplorer.getInstance().getEntityModel().getEntityById(mCommon.mEntityId, mCommon.mParentId, mCommon.mEntityTree);
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

			if (mImagePicture != null) {
				if (entity.imageUri != null && !entity.imageUri.equals("")) {
					if (mEntityBitmap != null) {
						ImageUtils.showImageInImageView(mEntityBitmap, mImagePicture.getImageView(), true, AnimUtils.fadeInMedium());
						mImagePicture.setVisibility(View.VISIBLE);
					}
					else {
						ImageRequestBuilder builder = new ImageRequestBuilder(mImagePicture);
						builder.setImageUri(entity.getMasterImageUri());
						builder.setImageFormat(entity.getMasterImageFormat());
						builder.setLinkZoom(entity.linkZoom);
						builder.setLinkJavascriptEnabled(entity.linkJavascriptEnabled);

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
		showChangePictureDialog(false, mImagePicture, new RequestListener() {

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
		mCommon.startTitlebarProgress();
		doSave();
	}

	public void onDeleteButtonClick(View view) {
		mCommon.startTitlebarProgress();
		deleteEntity();
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
					AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert, null,
							getResources().getString(R.string.alert_weburi_invalid), this, android.R.string.ok, null, null);
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
					ServiceResponse serviceResponse = new ServiceResponse();
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

							ServiceRequest serviceRequest = new ServiceRequest();
							serviceRequest.setUri(linkUri)
									.setRequestType(RequestType.Get)
									.setResponseFormat(ResponseFormat.Html)
									.setSuppressUI(true);

							serviceResponse = NetworkManager.getInstance().request(serviceRequest);

							/*
							 * Success means the uri was verified.
							 */
							if (serviceResponse.responseCode == ResponseCode.Success) {
								mEntityForForm.linkUri = linkUri;
								mEntityForForm.imageUri = null;
								mEntityBitmap = null;
								mUriVerified = true;
							}
						}
					}

					/* Insert beacon if it isn't already registered */
					if (serviceResponse.responseCode == ResponseCode.Success) {

						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								mCommon.showProgressDialog(true, getString(R.string.progress_saving));
							}
						});

						if (serviceResponse.responseCode == ResponseCode.Success) {
							if (mCommon.mCommandType == CommandType.New) {
								/*
								 * Pull all the control values back into the entity object
								 */
								gather(mEntityForForm);

								serviceResponse = insertEntity();
								if (serviceResponse.responseCode == ResponseCode.Success) {
									/*
									 * Insert new entity into the entity model.
									 * 
									 * We need to do a reasonable job of filling out the fields that
									 * normally get set by the service.
									 */
									Entity entity = mEntityForForm;
									Beacon beacon = ProxiExplorer.getInstance().getEntityModel().getBeaconById(entity.beaconId);
									String jsonResponse = (String) serviceResponse.data;
									ServiceData serviceData = ProxibaseService.convertJsonToObject(jsonResponse, Result.class, GsonType.ProxibaseService);
									Result result = (Result) serviceData.data;

									entity.id = result._id;
									entity.createdDate = DateUtils.nowDate().getTime();
									entity.modifiedDate = entity.createdDate;
									entity.ownerId = Aircandi.getInstance().getUser().id;
									entity.owner = Aircandi.getInstance().getUser();
									entity.creator = Aircandi.getInstance().getUser();
									entity.modifier = Aircandi.getInstance().getUser();
									entity.creatorId = entity.ownerId;
									entity.modifierId = entity.ownerId;

									/*
									 * Push to radar entities.
									 */
									Entity parentEntity = null;
									if (entity.parentId != null) {
										parentEntity = ProxiExplorer.getInstance().getEntityModel().getEntityById(entity.parentId, null, EntityTree.Radar);
									}
									ProxiExplorer.getInstance().getEntityModel().insertEntity(entity, beacon, parentEntity, EntityTree.Radar);
									if (parentEntity != null) {
										/* Sort child into the right spot */
										if (parentEntity.children.size() > 1) {
											Collections.sort(parentEntity.children, new EntityList.SortEntitiesByModifiedDate());
										}
									}

									/*
									 * Push to user entities if we have already loaded them. The entity should be
									 * inserted at the top level and also added as a child if it has a parent. Beacon
									 * parameter will be ignored.
									 * 
									 * We assume that if the user tree is empty then it will get loaded later and
									 * all the correct data will come from the service.
									 */
									if (!ProxiExplorer.getInstance().getEntityModel().getUserEntities().isEmpty()) {

										/*
										 * Insert at top level. The routine pushes the new entity in at the top of the
										 * list
										 * so we shouldn't have to sort since it will produce the same result.
										 */
										ProxiExplorer.getInstance().getEntityModel().insertEntity(entity, null, null, EntityTree.User);

										/* Insert at child level */
										if (entity.parentId != null) {
											parentEntity = ProxiExplorer.getInstance().getEntityModel()
													.getEntityById(entity.parentId, null, EntityTree.User);
											if (parentEntity != null) {
												ProxiExplorer.getInstance().getEntityModel()
														.insertEntity(entity, beacon, parentEntity, EntityTree.User);
												/* Sort child into the right spot */
												if (parentEntity.children.size() > 1) {
													Collections.sort(parentEntity.children, new EntityList.SortEntitiesByModifiedDate());
												}
											}
										}
									}

									ProxiExplorer.getInstance().getEntityModel().setLastActivityDate(DateUtils.nowDate().getTime());

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
								mEntityForForm.modifierId = Aircandi.getInstance().getUser().id;
								mEntityForForm.modifiedDate = DateUtils.nowDate().getTime();

								serviceResponse = updateEntity(mEntityForForm);

								if (serviceResponse.responseCode == ResponseCode.Success) {
									/*
									 * Now that we know the entity has been updated with the service, go ahead
									 * and update the appropriate entities in the entity model to match.
									 */
									Entity entityByRadar = ProxiExplorer.getInstance().getEntityModel()
											.getEntityById(mCommon.mEntityId, mCommon.mParentId, EntityTree.Radar);

									if (entityByRadar != null) {
										gather(entityByRadar);
										entityByRadar.modifierId = mEntityForForm.modifierId;
										entityByRadar.modifiedDate = mEntityForForm.modifiedDate;
										entityByRadar.imagePreviewUri = mEntityForForm.imagePreviewUri;
										entityByRadar.imageUri = mEntityForForm.imageUri;
										entityByRadar.linkUri = mEntityForForm.linkUri;
									}
									/*
									 * The entity could also be in the mycandi collection.
									 */
									Entity entityByUser = ProxiExplorer.getInstance().getEntityModel()
											.getEntityById(mCommon.mEntityId, mCommon.mParentId, EntityTree.User);

									if (entityByUser != null) {
										gather(entityByUser);
										entityByUser.modifierId = mEntityForForm.modifierId;
										entityByUser.modifiedDate = mEntityForForm.modifiedDate;
										entityByUser.imagePreviewUri = mEntityForForm.imagePreviewUri;
										entityByUser.imageUri = mEntityForForm.imageUri;
										entityByUser.linkUri = mEntityForForm.linkUri;
									}

									ProxiExplorer.getInstance().getEntityModel().setLastActivityDate(DateUtils.nowDate().getTime());
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
					mCommon.showProgressDialog(false, null);
					if (serviceResponse.responseCode == ResponseCode.Success) {
						finish();
					}
					else {
						if (serviceResponse.exception.getErrorCode() == ErrorCode.SessionException) {
							AircandiCommon.showAlertDialog(R.drawable.icon_app
									, getResources().getString(R.string.alert_session_expired_title)
									, getResources().getString(R.string.alert_session_expired_message)
									, EntityForm.this, android.R.string.ok, null, new OnClickListener() {

										public void onClick(DialogInterface dialog, int which) {
											setResult(CandiConstants.RESULT_PROFILE_INSERTED);
											finish();
										}
									});
						}
						else {
							mCommon.handleServiceError(serviceResponse, ServiceOperation.CandiSave, EntityForm.this);
						}
					}
				}

			}.execute();
		}
	}

	private ServiceResponse insertEntity() {

		Logger.i(this, "Inserting entity: " + mEntityForForm.title);
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
			mEntityForForm.root = false;
			mEntityForForm.parentId = mCommon.mParentId;
		}
		else {
			mEntityForForm.root = true;
			mEntityForForm.parentId = null;
			mEntityForForm.beaconId = beacon.id;
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
			link.toId = mCommon.mParentId == null ? beacon.id : mEntityForForm.parentId;
			parameters.putString("link",
					"object:" + ProxibaseService.convertObjectToJson(link, GsonType.ProxibaseService));

			/* Entity */
			parameters.putString("entity",
					"object:" + ProxibaseService.convertObjectToJson(mEntityForForm, GsonType.ProxibaseService));

			ServiceRequest serviceRequest = new ServiceRequest();
			serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "insertEntity")
					.setRequestType(RequestType.Method)
					.setParameters(parameters)
					.setSession(Aircandi.getInstance().getUser().session)
					.setResponseFormat(ResponseFormat.Json);

			serviceResponse = NetworkManager.getInstance().request(serviceRequest);
		}

		if (serviceResponse.responseCode == ResponseCode.Success) {
			Tracker.trackEvent("Entity", "New", mEntityForForm.type, 0);
		}

		return serviceResponse;
	}

	private ServiceResponse updateEntity(Entity entity) {

		ServiceResponse serviceResponse = new ServiceResponse();

		/* Upload new images to S3 as needed. */
		if (serviceResponse.responseCode == ResponseCode.Success) {
			serviceResponse = updateImages();
		}

		if (serviceResponse.responseCode == ResponseCode.Success) {
			Logger.i(this, "Updating entity: " + entity.title);

			// Construct entity, link, and observation
			Bundle parameters = new Bundle();
			parameters.putBoolean("skipActivityDate", false);
			parameters.putString("entity", "object:" + ProxibaseService.convertObjectToJson(entity, GsonType.ProxibaseService));

			ServiceRequest serviceRequest = new ServiceRequest();
			serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "updateEntity")
					.setRequestType(RequestType.Method)
					.setParameters(parameters)
					.setSession(Aircandi.getInstance().getUser().session)
					.setResponseFormat(ResponseFormat.Json);

			serviceResponse = NetworkManager.getInstance().request(serviceRequest);
		}

		if (serviceResponse.responseCode == ResponseCode.Success) {
			Tracker.trackEvent("Entity", "Update", entity.type, 0);
		}

		return serviceResponse;
	}

	private void deleteEntity() {
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
				ServiceResponse serviceResponse = new ServiceResponse();

				/*
				 * If there is an image stored with S3 then delete it.
				 * TODO: Flag image for garbage collection but don't
				 * delete it because because it might be needed while aircandi users have current sessions.
				 */
				if (mEntityForForm.imagePreviewUri != null
						&& !mEntityForForm.imagePreviewUri.equals("")
						&& !ImageManager.isLocalImage(mEntityForForm.imagePreviewUri)) {
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
				Logger.i(this, "Deleting entity: " + mEntityForForm.title);

				Bundle parameters = new Bundle();
				parameters.putString("entityId", mEntityForForm.id);
				parameters.putBoolean("deleteChildren", true);

				ServiceRequest serviceRequest = new ServiceRequest();
				serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "deleteEntity")
						.setRequestType(RequestType.Method)
						.setParameters(parameters)
						.setSession(Aircandi.getInstance().getUser().session)
						.setResponseFormat(ResponseFormat.Json);

				serviceResponse = NetworkManager.getInstance().request(serviceRequest);
				return serviceResponse;
			}

			@Override
			protected void onPostExecute(Object response) {
				ServiceResponse serviceResponse = (ServiceResponse) response;

				if (serviceResponse.responseCode == ResponseCode.Success) {
					Tracker.trackEvent("Entity", "Delete", mEntityForForm.type, 0);
					Logger.i(this, "Deleted entity: " + mEntityForForm.title);

					ProxiExplorer.getInstance().getEntityModel().deleteEntity(mEntityForForm, EntityTree.Radar);
					ProxiExplorer.getInstance().getEntityModel().deleteEntity(mEntityForForm, EntityTree.User);
					ProxiExplorer.getInstance().getEntityModel().setLastActivityDate(DateUtils.nowDate().getTime());

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

	private ServiceResponse updateImages() {

		/*
		 * Delete image from S3 if it has been orphaned TODO: We are going with a garbage collection scheme for orphaned
		 * images. We need to use an extended property on S3 items that is set to a date when collection is ok. This
		 * allows downloaded entities to keep working even if an image for entity has changed.
		 */
		ServiceResponse serviceResponse = new ServiceResponse();

		/* Upload image to S3 if we have a new one. */
		if (mEntityBitmap != null) {
			try {
				String imageKey = String.valueOf(Aircandi.getInstance().getUser().id) + "_"
						+ String.valueOf(DateUtils.nowString(DateUtils.DATE_NOW_FORMAT_FILENAME))
						+ ".jpg";
				S3.putImage(imageKey, mEntityBitmap);
				mEntityForForm.imagePreviewUri = CandiConstants.URL_AIRCANDI_MEDIA + CandiConstants.S3_BUCKET_IMAGES
						+ "/" + imageKey;
				if (mEntityForForm.imageUri == null || mEntityForForm.imageUri.equals("")) {
					mEntityForForm.imageUri = mEntityForForm.imagePreviewUri;
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

		Intent intent = new Intent(this, BookmarkPicker.class);
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
						AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert, null,
								getResources().getString(R.string.alert_weburi_invalid), EntityForm.this, android.R.string.ok, null, null);
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
				mCommon.mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

					public void onCancel(DialogInterface dialog) {
						mAsyncTask.cancel(true);
						ImageUtils.showToastNotification("Validation canceled", Toast.LENGTH_SHORT);
					}
				});
				mCommon.showProgressDialog(true, getString(R.string.progress_verifying));
			}

			@Override
			protected Object doInBackground(Object... params) {

				ServiceRequest serviceRequest = new ServiceRequest();
				serviceRequest.setUri(linkUri)
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