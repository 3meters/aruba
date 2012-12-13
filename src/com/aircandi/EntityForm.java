package com.aircandi;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.CommandType;
import com.aircandi.components.FontManager;
import com.aircandi.components.ImageRequest;
import com.aircandi.components.ImageRequestBuilder;
import com.aircandi.components.LocationManager;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.ProxiExplorer.ModelResult;
import com.aircandi.components.Tracker;
import com.aircandi.core.CandiConstants;
import com.aircandi.service.ProxibaseService;
import com.aircandi.service.ProxibaseService.RequestListener;
import com.aircandi.service.ProxibaseService.ServiceDataType;
import com.aircandi.service.objects.Beacon;
import com.aircandi.service.objects.Category;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Entity.ImageFormat;
import com.aircandi.service.objects.Entity.Visibility;
import com.aircandi.service.objects.Location;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;
import com.aircandi.utilities.ImageUtils;
import com.aircandi.widgets.BuilderButton;
import com.aircandi.widgets.UserView;
import com.aircandi.widgets.WebImageView;

public class EntityForm extends FormActivity {

	private ViewFlipper		mViewFlipper;
	protected WebImageView	mImageViewPicture;
	private Bitmap			mEntityBitmap;
	private Entity			mEntityForForm;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
			LocationManager.getInstance().getLastLocation();
		}

		if (mCommon.mEntityType.equals(CandiConstants.TYPE_CANDI_PLACE)) {
			mCommon.mActionBar.setTitle(R.string.form_title_place);
		}
		else if (mCommon.mEntityType.equals(CandiConstants.TYPE_CANDI_PICTURE)) {
			mCommon.mActionBar.setTitle(R.string.form_title_picture);
		}
		else if (mCommon.mEntityType.equals(CandiConstants.TYPE_CANDI_POST)) {
			mCommon.mActionBar.setTitle(R.string.form_title_post);
		}
		else if (mCommon.mEntityType.equals(CandiConstants.TYPE_CANDI_FOLDER)) {
			mCommon.mActionBar.setTitle(R.string.form_title_folder);
		}

		mImageViewPicture = (WebImageView) findViewById(R.id.image_picture);

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
			entity.enabled = true;
			entity.locked = false;
			entity.isCollection = false;
			entity.visibility = Visibility.Public.toString().toLowerCase();
			entity.type = mCommon.mEntityType;

			if (entity.type.equals(CandiConstants.TYPE_CANDI_PICTURE)
					|| mCommon.mEntityType.equals(CandiConstants.TYPE_CANDI_PLACE)) {
				entity.getPhoto().setImageUri("resource:placeholder_logo_bw");
			}
			else if (entity.type.equals(CandiConstants.TYPE_CANDI_FOLDER)) {
				entity.isCollection = true;
				entity.name = getString(R.string.entity_folder_title);
				entity.getPhoto().setImageUri("resource:ic_collection_250");
			}
			else if (entity.type.equals(CandiConstants.TYPE_CANDI_PLACE)) {
				entity.isCollection = true;
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
				Entity entityForModel = ProxiExplorer.getInstance().getEntityModel().getCacheEntity(mCommon.mEntityId);
				if (entityForModel != null) {
					mEntityForForm = entityForModel.clone();
					mImageUriOriginal = mEntityForForm.getPhoto().getImageUri();
				}
			}
			else {
				if (mEntityForForm != null) {
					mImageUriOriginal = mEntityForForm.getPhoto().getImageUri();
				}
			}
		}
	}

	private void draw() {

		if (mEntityForForm != null) {

			final Entity entity = mEntityForForm;

			/* Fonts */

			FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_cancel));
			FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_delete));
			FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_save));
			FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_change_image));
			FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.twitter));
			FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.facebook));
			FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.text_uri));
			FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.text_title));
			FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.description));

			/* Content */

			if (mImageViewPicture != null) {
				if (entity.getPhoto().getImageUri() != null) {
					if (mEntityBitmap != null) {
						mImageViewPicture.hideLoading();
						ImageUtils.showImageInImageView(mEntityBitmap, mImageViewPicture.getImageView(), true, AnimUtils.fadeInMedium());
						mImageViewPicture.setVisibility(View.VISIBLE);
					}
					else {
						ImageRequestBuilder builder = new ImageRequestBuilder(mImageViewPicture);
						builder.setImageUri(entity.getImageUri());
						builder.setImageFormat(entity.getImageFormat());
						builder.setLinkZoom(CandiConstants.LINK_ZOOM);
						builder.setLinkJavascriptEnabled(CandiConstants.LINK_JAVASCRIPT_ENABLED);

						ImageRequest imageRequest = builder.create();
						mImageViewPicture.setImageRequest(imageRequest);
					}
				}
			}

			if (entity.name != null && !entity.name.equals("")) {
				if (findViewById(R.id.text_title) != null) {
					((TextView) findViewById(R.id.text_title)).setText(entity.name);
				}
			}

			if (entity.description != null && !entity.description.equals("")) {
				if (findViewById(R.id.description) != null) {
					((TextView) findViewById(R.id.description)).setText(entity.description);
				}
			}

			if (findViewById(R.id.chk_locked) != null) {
				((CheckBox) findViewById(R.id.chk_locked)).setVisibility(View.VISIBLE);
				((CheckBox) findViewById(R.id.chk_locked)).setChecked(entity.locked);
			}

			/* Place content */
			if (entity.place != null) {
				if (entity.place.location != null) {
					if (findViewById(R.id.address) != null) {
						String addressBlock = entity.place.location.getAddressBlock();
						if (!addressBlock.equals("")) {
							((BuilderButton) findViewById(R.id.address)).setText(entity.place.location.address);
						}
					}
				}
				if (entity.place.website != null && !entity.place.website.equals("")) {
					if (findViewById(R.id.website) != null) {
						((BuilderButton) findViewById(R.id.website)).setText(entity.place.website);
					}
				}
				if (entity.place.categories != null && entity.place.categories.size() > 0) {
					Category category = entity.place.getCategoryPrimary();
					if (findViewById(R.id.category) != null) {
						((BuilderButton) findViewById(R.id.category)).setText(category.name);
					}
				}
				if (entity.place.contact != null) {
					if (entity.place.contact.twitter != null && !entity.place.contact.twitter.equals("")) {
						if (findViewById(R.id.twitter) != null) {
							((TextView) findViewById(R.id.twitter)).setText(entity.place.facebook);
						}
					}
				}
				if (entity.place.facebook != null && !entity.place.facebook.equals("")) {
					if (findViewById(R.id.facebook) != null) {
						((TextView) findViewById(R.id.facebook)).setText("@" + entity.place.contact.twitter);
					}
				}
			}
			else {
				if (findViewById(R.id.text_uri) != null) {
					if (!entity.getPhoto().getDetail().isEmpty() && entity.getPhoto().getDetail().getImageFormat() == ImageFormat.Html) {
						((TextView) findViewById(R.id.text_uri)).setText(entity.getPhoto().getImageUri());
					}
				}
			}

			/* Author */

			if (entity != null && entity.creator != null) {
				((UserView) findViewById(R.id.author)).bindToAuthor(entity.creator,
						entity.modifiedDate.longValue(), entity.locked);
			}
			else {
				((UserView) findViewById(R.id.author)).setVisibility(View.GONE);
			}

			/* Configure UI */

			if (mCommon.mCommandType == CommandType.New) {
				if (findViewById(R.id.button_delete) != null) {
					((Button) findViewById(R.id.button_delete)).setVisibility(View.GONE);
				}
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onChangePictureButtonClick(View view) {
		showChangePictureDialog(false, false, null, null, null, mImageViewPicture, new RequestListener() {

			@Override
			public void onComplete(Object response, String imageUri, String linkUri, Bitmap imageBitmap, String title, String description) {

				ServiceResponse serviceResponse = (ServiceResponse) response;
				if (serviceResponse.responseCode == ResponseCode.Success) {
					/*
					 * If we get back a bitmap and an imageUri we want to just store a preview.
					 * otherwise if the bitmap is wider that our default, we want to store
					 * both preview and native versions.
					 */
					mEntityBitmap = imageBitmap;
					if (linkUri != null) {
						mEntityForForm.getPhoto().setImageUri(linkUri);
						mEntityForForm.getPhoto().setImageFormat(ImageFormat.Html);
					}
					else {
						if (imageUri != null) {
							mEntityForForm.getPhoto().setImageUri(imageUri);
						}
					}
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

	public void onWebsiteBuilderClick(View view) {
		Intent intent = new Intent(this, LinkPicker.class);
		intent.putExtra(CandiConstants.EXTRA_VERIFY_URI, false);
		String website = ((EditText) findViewById(R.id.website)).getText().toString();
		if (website != null && !website.equals("")) {
			intent.putExtra(CandiConstants.EXTRA_URI, website);
		}

		startActivityForResult(intent, CandiConstants.ACTIVITY_WEBSITE_EDIT);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
	}

	public void onAddressBuilderClick(View view) {
		Intent intent = new Intent(this, AddressBuilder.class);
		if (mEntityForForm.getPlace().location != null) {
			String jsonAddress = ProxibaseService.convertObjectToJsonSmart(mEntityForForm.place.location, false, true);
			intent.putExtra(CandiConstants.EXTRA_ADDRESS, jsonAddress);
		}
		if (mEntityForForm.getPlace().contact != null && mEntityForForm.getPlace().contact.phone != null) {
			intent.putExtra(CandiConstants.EXTRA_PHONE, mEntityForForm.getPlace().contact.phone);
		}
		startActivityForResult(intent, CandiConstants.ACTIVITY_ADDRESS_EDIT);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
	}

	public void onCategoryBuilderClick(View view) {
		Intent intent = new Intent(this, CategoryBuilder.class);
		if (mEntityForForm.getPlace().categories != null && mEntityForForm.getPlace().categories.size() > 0) {
			String jsonCategory = ProxibaseService.convertObjectToJsonSmart(mEntityForForm.getPlace().getCategoryPrimary(), false, true);
			intent.putExtra(CandiConstants.EXTRA_CATEGORY, jsonCategory);
		}
		startActivityForResult(intent, CandiConstants.ACTIVITY_CATEGORY_EDIT);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
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
			if (resultCode == Activity.RESULT_OK && requestCode == CandiConstants.ACTIVITY_ADDRESS_EDIT) {
				if (intent != null && intent.getExtras() != null) {
					Bundle extras = intent.getExtras();

					String phone = extras.getString(CandiConstants.EXTRA_PHONE);
					phone = phone.replaceAll("[^\\d.]", "");
					mEntityForForm.getPlace().getContact().phone = phone;
					mEntityForForm.getPlace().getContact().formattedPhone = PhoneNumberUtils.formatNumber(phone);

					String jsonAddress = extras.getString(CandiConstants.EXTRA_ADDRESS);
					if (jsonAddress != null) {
						Location locationUpdated = (Location) ProxibaseService.convertJsonToObjectInternalSmart(jsonAddress, ServiceDataType.Location);
						mEntityForForm.getPlace().location = locationUpdated;
						((BuilderButton) findViewById(R.id.address)).setText(mEntityForForm.place.location.address);
					}
				}
			}
			else if (resultCode == Activity.RESULT_OK && requestCode == CandiConstants.ACTIVITY_CATEGORY_EDIT) {
				if (intent != null && intent.getExtras() != null) {
					Bundle extras = intent.getExtras();
					String jsonCategory = extras.getString(CandiConstants.EXTRA_CATEGORY);
					if (jsonCategory != null) {
						Category categoryUpdated = (Category) ProxibaseService.convertJsonToObjectInternalSmart(jsonCategory, ServiceDataType.Category);
						if (categoryUpdated != null) {
							if (mEntityForForm.getPlace().categories == null) {
								mEntityForForm.getPlace().categories = new ArrayList<Category>();
							}
							mEntityForForm.getPlace().categories.clear();
							mEntityForForm.getPlace().categories.add(categoryUpdated);
							((BuilderButton) findViewById(R.id.category)).setText(categoryUpdated.name);
						}
					}
				}
			}
			else if (resultCode == Activity.RESULT_OK && requestCode == CandiConstants.ACTIVITY_WEBSITE_EDIT) {
				if (intent != null && intent.getExtras() != null) {
					Bundle extras = intent.getExtras();
					String linkUri = extras.getString(CandiConstants.EXTRA_URI);
					if (!linkUri.startsWith("http://") && !linkUri.startsWith("https://")) {
						linkUri = "http://" + linkUri;
					}
					mEntityForForm.getPlace().website = linkUri;
					((BuilderButton) findViewById(R.id.website)).setText(linkUri);
				}
			}
			else {
				super.onActivityResult(requestCode, resultCode, intent);
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	private void gather(Entity entity) {
		if (findViewById(R.id.text_title) != null) {
			entity.name = ((TextView) findViewById(R.id.text_title)).getText().toString().trim();
		}
		if (findViewById(R.id.description) != null) {
			entity.description = ((TextView) findViewById(R.id.description)).getText().toString().trim();
		}
		if (findViewById(R.id.chk_locked) != null) {
			entity.locked = ((CheckBox) findViewById(R.id.chk_locked)).isChecked();
		}
		if (findViewById(R.id.website) != null) {
			entity.getPlace().website = ((BuilderButton) findViewById(R.id.website)).getText();
		}
		if (findViewById(R.id.twitter) != null) {
			String twitter = ((TextView) findViewById(R.id.twitter)).getText().toString();
			/* We don't store the at sign */
			twitter = twitter.replace("@", "");
			entity.getPlace().getContact().twitter = twitter;
		}
		if (findViewById(R.id.facebook) != null) {
			String facebook = ((TextView) findViewById(R.id.facebook)).getText().toString();
			/* We don't store the at sign */
			entity.getPlace().facebook = facebook;
		}
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	private void doSave() {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {}

			@Override
			protected Object doInBackground(Object... params) {
				ModelResult result = new ModelResult();

				if (result.serviceResponse.responseCode == ResponseCode.Success) {

					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							mCommon.showBusy(R.string.progress_saving);
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
				mCommon.hideBusy();
				if (serviceResponse.responseCode == ResponseCode.Success) {
					finish();
				}
				else {
					mCommon.handleServiceError(serviceResponse, ServiceOperation.CandiSave, EntityForm.this);
				}
			}

		}.execute();
	}

	private ModelResult insertEntityAtService() {
		ModelResult result = new ModelResult();
		List<Beacon> beacons = null;
		Beacon primaryBeacon = null;

		/* First we want to make sure we have the freshest set of beacons */
		if (NetworkManager.getInstance().isWifiEnabled()) {
			ProxiExplorer.getInstance().scanForWifi(new RequestListener() {});
		}

		/* If parent id then this is a child */
		if (mEntityForForm.links != null) {
			mEntityForForm.links.clear();
		}

		if (mCommon.mParentId != null) {
			mEntityForForm.parentId = mCommon.mParentId;
		}
		else {
			mEntityForForm.parentId = null;
		}

		if (mEntityForForm.parentId == null) {
			/*
			 * We are linking to a beacon so get the best and alert if none
			 */
			beacons = ProxiExplorer.getInstance().getStrongestWifiAsBeacons(5);
			primaryBeacon = beacons.size() > 0 ? beacons.get(0) : null;

			/*
			 * Set location info if this is a place entity
			 */
			if (mEntityForForm.type.equals(CandiConstants.TYPE_CANDI_PLACE)) {
				mEntityForForm.getPlace().source = "user";
				mEntityForForm.getPlace().sourceId = Aircandi.getInstance().getUser().id;
				/*
				 * We add location info as a consistent feature
				 */
				android.location.Location currentLocation = LocationManager.getInstance().getLocation();
				if (currentLocation != null) {
					mEntityForForm.place.location = new com.aircandi.service.objects.Location();
					mEntityForForm.place.location.lat = currentLocation.getLatitude();
					mEntityForForm.place.location.lng = currentLocation.getLongitude();
				}
			}
		}

		result = ProxiExplorer.getInstance().getEntityModel().insertEntity(mEntityForForm, beacons, primaryBeacon, mEntityBitmap, false);
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
				mCommon.showBusy(R.string.progress_deleting);
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
					Logger.i(this, "Deleted entity: " + mEntityForForm.name);

					mCommon.hideBusy();
					ImageUtils.showToastNotification(getString(R.string.alert_deleted), Toast.LENGTH_SHORT);
					setResult(CandiConstants.RESULT_ENTITY_DELETED);
					/*
					 * We either go back to a list or to radar.
					 */
					finish();
					AnimUtils.doOverridePendingTransition(EntityForm.this, TransitionType.FormToCandiPageAfterDelete);
				}
				else {
					mCommon.handleServiceError(result.serviceResponse, ServiceOperation.CandiDelete, EntityForm.this);
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
		mCommon.startScanService(CandiConstants.INTERVAL_SCAN_RADAR);
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
		else if (mCommon.mEntityType.equals(CandiConstants.TYPE_CANDI_FOLDER)) {
			return R.layout.folder_form;
		}
		else if (mCommon.mEntityType.equals(CandiConstants.TYPE_CANDI_PLACE)) {
			return R.layout.place_form;
		}
		else {
			return 0;
		}
	}
}