package com.aircandi.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi;
import com.aircandi.CandiConstants;
import com.aircandi.beta.R;
import com.aircandi.components.AircandiCommon;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.CommandType;
import com.aircandi.components.FontManager;
import com.aircandi.components.LocationManager;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProxiManager;
import com.aircandi.components.ProxiManager.ModelResult;
import com.aircandi.components.Tracker;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequestBuilder;
import com.aircandi.service.ProxibaseService;
import com.aircandi.service.ProxibaseService.RequestListener;
import com.aircandi.service.ProxibaseService.ServiceDataType;
import com.aircandi.service.objects.Beacon;
import com.aircandi.service.objects.Category;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Entity.Visibility;
import com.aircandi.service.objects.Location;
import com.aircandi.service.objects.Observation;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Photo.PhotoSource;
import com.aircandi.service.objects.Place;
import com.aircandi.service.objects.Source;
import com.aircandi.ui.base.FormActivity;
import com.aircandi.ui.builders.AddressBuilder;
import com.aircandi.ui.builders.CategoryBuilder;
import com.aircandi.ui.builders.SourcesBuilder;
import com.aircandi.ui.widgets.BuilderButton;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.ui.widgets.WebImageView;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;
import com.aircandi.utilities.ImageUtils;

public class EntityForm extends FormActivity {

	private ViewFlipper		mViewFlipper;
	private WebImageView	mImageViewPicture;
	private Bitmap			mEntityBitmap;
	private Boolean			mEntityBitmapLocalOnly	= false;
	private Entity			mEntityForForm;
	private Boolean			mDirty					= false;
	private TextView		mTitle;
	private TextView		mDescription;
	private CheckBox		mLocked;
	private Boolean			mMuteColor;
	private Integer			mColorResId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		if (!isFinishing()) {
			initialize();
			bind();
			draw();
		}
	}

	private void initialize() {
		/*
		 * Starting determining the users location if we are creating new candi. We are pulling
		 * a single shot coarse location which is usually based on network location method.
		 */
		if (mCommon.mEntityType.equals(CandiConstants.TYPE_CANDI_PLACE)) {
			mCommon.mActionBar.setTitle(R.string.form_title_place);
		}
		else if (mCommon.mEntityType.equals(CandiConstants.TYPE_CANDI_PICTURE)) {
			mCommon.mActionBar.setTitle(R.string.form_title_picture);
		}
		else if (mCommon.mEntityType.equals(CandiConstants.TYPE_CANDI_POST)) {
			mCommon.mActionBar.setTitle(R.string.form_title_post);
		}

		mTitle = (TextView) findViewById(R.id.text_title);
		mDescription = (TextView) findViewById(R.id.description);
		mLocked = (CheckBox) findViewById(R.id.chk_locked);
		mImageViewPicture = (WebImageView) findViewById(R.id.image_picture);
		mViewFlipper = (ViewFlipper) findViewById(R.id.flipper_form);
		mCommon.setViewFlipper(mViewFlipper);

		if (mTitle != null) {
			mTitle.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					if (!s.toString().equals(mEntityForForm.name)) {
						mDirty = true;
					}
				}
			});
		}
		if (mDescription != null) {
			mDescription.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					if (!s.toString().equals(mEntityForForm.description)) {
						mDirty = true;
					}
				}
			});
		}
		if (mLocked != null) {
			mLocked.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (mEntityForForm.locked != isChecked) {
						mDirty = true;
					}
				}
			});
		}
	}

	private void bind() {
		/*
		 * Fill in the system and default properties for the base entity properties. The activities that subclass this
		 * will set any additional properties beyond the base ones.
		 */
		mCommon.mActionBar.setDisplayHomeAsUpEnabled(true);
		if (mCommon.mEntityType.equals(CandiConstants.TYPE_CANDI_PLACE)) {}

		if (mCommon.mCommandType == CommandType.New) {
			mEntityForForm = makeEntity(mCommon.mEntityType);
		}
		else {
			if (mEntityForForm == null && mCommon.mEntityId != null) {
				/*
				 * Entity is coming from entity model. We want to create a clone so
				 * that any changes only show up in the entity model if the changes make it
				 * to the service.
				 */
				final Entity entityForModel = ProxiManager.getInstance().getEntityModel().getCacheEntity(mCommon.mEntityId);
				if (entityForModel != null) {
					mEntityForForm = entityForModel.clone();
				}
			}
		}
		@SuppressWarnings("unused")
		List<Entity> entities = mEntityForForm.getChildren();		

	}

	private Entity makeEntity(String type) {
		final Entity entity = new Entity();
		entity.signalFence = -100.0f;
		entity.enabled = true;
		entity.locked = false;
		entity.isCollection = (type.equals(CandiConstants.TYPE_CANDI_PLACE));
		entity.visibility = Visibility.Public.toString().toLowerCase(Locale.US);
		entity.type = type;
		if (type.equals(CandiConstants.TYPE_CANDI_PLACE)) {
			entity.getPlace().provider = "user";
			entity.getPlace().id = Aircandi.getInstance().getUser().id;
			entity.locked = false;
		}
		return entity;
	}

	private void draw() {

		if (mEntityForForm != null) {

			final Entity entity = mEntityForForm;

			/* Color */

			mMuteColor = android.os.Build.MODEL.toLowerCase(Locale.US).equals("nexus s"); // nexus 4, nexus 7 are others		

			/* Fonts */

			FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_change_image));
			FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.uri));
			FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.text_title));
			FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.description));
			FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.chk_locked));

			/* Content */

			drawImage(entity);

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

				drawSources(entity);

				if (entity.place.location != null) {
					if (findViewById(R.id.address) != null) {
						final String addressBlock = entity.place.location.getAddressBlock();
						if (!addressBlock.equals("")) {
							((BuilderButton) findViewById(R.id.address)).setText(entity.place.location.address);
						}
					}
				}
				if (entity.place.category != null) {
					if (findViewById(R.id.category) != null) {
						((BuilderButton) findViewById(R.id.category)).setText(entity.place.category.name);
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
			setVisibility(findViewById(R.id.button_delete), View.GONE);
			if (entity.ownerId != null
					&& (entity.ownerId.equals(Aircandi.getInstance().getUser().id)
					|| (Aircandi.settings.getBoolean(CandiConstants.PREF_ENABLE_DEV, CandiConstants.PREF_ENABLE_DEV_DEFAULT)
							&& Aircandi.getInstance().getUser().isDeveloper != null
							&& Aircandi.getInstance().getUser().isDeveloper))) {
				setVisibility(findViewById(R.id.button_delete), View.VISIBLE);
			}
		}
	}

	private void drawImage(Entity entity) {
		if (mImageViewPicture != null) {

			mImageViewPicture.getImageView().clearColorFilter();
			mImageViewPicture.getImageView().setBackgroundResource(0);
			if (findViewById(R.id.color_layer) != null) {
				(findViewById(R.id.color_layer)).setBackgroundResource(0);
				(findViewById(R.id.color_layer)).setVisibility(View.GONE);
				(findViewById(R.id.reverse_layer)).setVisibility(View.GONE);
			}

			if (mEntityBitmap != null) {
				mImageViewPicture.hideLoading();
				ImageUtils.showImageInImageView(mEntityBitmap, mImageViewPicture.getImageView(), true, AnimUtils.fadeInMedium());
				mImageViewPicture.setVisibility(View.VISIBLE);
			}
			else {
				if (entity.type.equals(CandiConstants.TYPE_CANDI_PLACE)) {
					if (entity.photo == null && entity.place != null && entity.place.category != null) {

						final int color = Place.getCategoryColor((entity.place.category != null)
								? entity.place.category.name
								: null, true, mMuteColor, false);

						mImageViewPicture.getImageView().setColorFilter(color, PorterDuff.Mode.MULTIPLY);
						mColorResId = Place.getCategoryColorResId((entity.place != null && entity.place.category != null) ? entity.place.category.name : null,
								true, mMuteColor, false);

						if (findViewById(R.id.color_layer) != null) {
							(findViewById(R.id.color_layer)).setBackgroundResource(mColorResId);
							(findViewById(R.id.color_layer)).setVisibility(View.VISIBLE);
							(findViewById(R.id.reverse_layer)).setVisibility(View.VISIBLE);
						}
						else {
							mImageViewPicture.getImageView().setBackgroundResource(mColorResId);
						}
					}
				}

				final String imageUri = entity.getEntityPhotoUri();
				final BitmapRequest bitmapRequest = new BitmapRequest(imageUri, mImageViewPicture.getImageView());
				bitmapRequest.setImageSize(mImageViewPicture.getSizeHint());
				bitmapRequest.setImageRequestor(mImageViewPicture.getImageView());
				mImageViewPicture.getImageView().setTag(imageUri);
				BitmapManager.getInstance().masterFetch(bitmapRequest);
			}
		}
	}

	private void drawSources(Entity entity) {
		if (findViewById(R.id.sources) != null) {
			/*
			 * We are expecting a builder button with a viewgroup to
			 * hold a set of images.
			 */
			final BuilderButton button = (BuilderButton) findViewById(R.id.sources);
			if (entity.sources == null || entity.sources.size() == 0) {
				button.getTextView().setVisibility(View.VISIBLE);
				button.getViewGroup().setVisibility(View.GONE);
			}
			else {
				button.getTextView().setVisibility(View.GONE);
				button.getViewGroup().setVisibility(View.VISIBLE);
				button.getViewGroup().removeAllViews();
				final LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				final int sizePixels = ImageUtils.getRawPixels(this, 30);
				final int marginPixels = ImageUtils.getRawPixels(this, 5);

				/* We only show the first five */
				int sourceCount = 0;
				for (Source source : entity.sources) {
					if (sourceCount >= 5) {
						break;
					}
					if (source.system != null && source.system) {
						continue;
					}
					if (source.hidden != null && source.hidden) {
						continue;
					}
					View view = inflater.inflate(R.layout.temp_radar_candi_item, null);
					WebImageView webImageView = (WebImageView) view.findViewById(R.id.image);
					webImageView.setSizeHint(sizePixels);

					String imageUri = source.getImageUri();
					BitmapRequestBuilder builder = new BitmapRequestBuilder(webImageView).setImageUri(imageUri);
					BitmapRequest imageRequest = builder.create();
					webImageView.setBitmapRequest(imageRequest);

					LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sizePixels, sizePixels);
					params.setMargins(marginPixels
							, marginPixels
							, marginPixels
							, marginPixels);
					view.setLayoutParams(params);
					button.getViewGroup().addView(view);
					sourceCount++;
				}
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	@Override
	public void onBackPressed() {
		if (isDirty()) {
			confirmDirtyExit();
		}
		else {
			setResult(Activity.RESULT_CANCELED);
			finish();
			AnimUtils.doOverridePendingTransition(EntityForm.this, TransitionType.FormToPage);
		}
	}

	@SuppressWarnings("ucd")
	public void onChangePictureButtonClick(View view) {
		mCommon.showPictureSourcePicker(mEntityForForm.id);
		mImageRequestWebImageView = mImageViewPicture;
		mImageRequestListener = new RequestListener() {

			@Override
			public void onComplete(Object response, Photo photo, String imageUri, Bitmap imageBitmap, String title, String description, Boolean bitmapLocalOnly) {

				final ServiceResponse serviceResponse = (ServiceResponse) response;
				if (serviceResponse.responseCode == ResponseCode.Success) {

					mDirty = true;
					mEntityBitmapLocalOnly = bitmapLocalOnly;
					/* Could get set to null if we are using the default */
					mEntityBitmap = imageBitmap;
					if (photo != null) {
						mEntityForForm.photo = photo;
					}
					else if (imageUri != null) {
						mEntityForForm.photo = new Photo(imageUri, null, null, null, PhotoSource.aircandi);
					}
					drawImage(mEntityForForm);
				}
			}
		};
	}

	@SuppressWarnings("ucd")
	public void onAddressBuilderClick(View view) {
		final Intent intent = new Intent(this, AddressBuilder.class);
		if (mEntityForForm.getPlace().location != null) {
			final String jsonAddress = ProxibaseService.convertObjectToJsonSmart(mEntityForForm.place.location, false, true);
			intent.putExtra(CandiConstants.EXTRA_ADDRESS, jsonAddress);
		}
		if (mEntityForForm.getPlace().contact != null && mEntityForForm.getPlace().contact.phone != null) {
			intent.putExtra(CandiConstants.EXTRA_PHONE, mEntityForForm.getPlace().contact.phone);
		}
		startActivityForResult(intent, CandiConstants.ACTIVITY_ADDRESS_EDIT);
		AnimUtils.doOverridePendingTransition(this, TransitionType.PageToForm);
	}

	@SuppressWarnings("ucd")
	public void onCategoryBuilderClick(View view) {
		final Intent intent = new Intent(this, CategoryBuilder.class);
		if (mEntityForForm.getPlace().category != null) {
			final String jsonCategory = ProxibaseService.convertObjectToJsonSmart(mEntityForForm.getPlace().category, false, true);
			intent.putExtra(CandiConstants.EXTRA_CATEGORY, jsonCategory);
		}
		startActivityForResult(intent, CandiConstants.ACTIVITY_CATEGORY_EDIT);
		AnimUtils.doOverridePendingTransition(this, TransitionType.PageToForm);
	}

	@SuppressWarnings("ucd")
	public void onSourcesBuilderClick(View view) {
		final Intent intent = new Intent(this, SourcesBuilder.class);
		intent.putExtra(CandiConstants.EXTRA_ENTITY_ID, mEntityForForm.id);

		/* Serialize the sources for the current entity */
		if (mEntityForForm.sources != null && mEntityForForm.sources.size() > 0) {
			final List<String> sourceStrings = new ArrayList<String>();
			for (Source source : mEntityForForm.sources) {
				sourceStrings.add(ProxibaseService.convertObjectToJsonSmart(source, true, true));
			}
			intent.putStringArrayListExtra(CandiConstants.EXTRA_SOURCES, (ArrayList<String>) sourceStrings);
		}
		startActivityForResult(intent, CandiConstants.ACTIVITY_SOURCES_EDIT);
		AnimUtils.doOverridePendingTransition(this, TransitionType.PageToForm);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {

		if (resultCode == Activity.RESULT_OK) {
			if (requestCode == CandiConstants.ACTIVITY_ADDRESS_EDIT) {
				if (intent != null && intent.getExtras() != null) {
					mDirty = true;
					final Bundle extras = intent.getExtras();

					String phone = extras.getString(CandiConstants.EXTRA_PHONE);
					phone = phone.replaceAll("[^\\d.]", "");
					mEntityForForm.getPlace().getContact().phone = phone;
					mEntityForForm.getPlace().getContact().formattedPhone = PhoneNumberUtils.formatNumber(phone);

					final String jsonAddress = extras.getString(CandiConstants.EXTRA_ADDRESS);
					if (jsonAddress != null) {
						final Location locationUpdated = (Location) ProxibaseService.convertJsonToObjectInternalSmart(jsonAddress, ServiceDataType.Location);
						mEntityForForm.getPlace().location = locationUpdated;
						((BuilderButton) findViewById(R.id.address)).setText(mEntityForForm.place.location.address);
					}
				}
			}
			else if (requestCode == CandiConstants.ACTIVITY_CATEGORY_EDIT) {
				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final String jsonCategory = extras.getString(CandiConstants.EXTRA_CATEGORY);
					if (jsonCategory != null) {
						final Category categoryUpdated = (Category) ProxibaseService.convertJsonToObjectInternalSmart(jsonCategory, ServiceDataType.Category);
						if (categoryUpdated != null) {
							mDirty = true;
							mEntityForForm.getPlace().category = categoryUpdated;
							((BuilderButton) findViewById(R.id.category)).setText(categoryUpdated.name);
							drawImage(mEntityForForm);
						}
					}
				}
			}
			else if (requestCode == CandiConstants.ACTIVITY_SOURCES_EDIT) {
				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final List<String> jsonSources = extras.getStringArrayList(CandiConstants.EXTRA_SOURCES);
					final List<Source> sources = new ArrayList<Source>();
					for (String jsonSource : jsonSources) {
						Source source = (Source) ProxibaseService.convertJsonToObjectInternalSmart(jsonSource, ServiceDataType.Source);
						sources.add(source);
					}
					mDirty = true;
					mEntityForForm.sources = sources;
					drawSources(mEntityForForm);
				}
			}
			else if (requestCode == CandiConstants.ACTIVITY_PICTURE_SOURCE_PICK) {
				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final String pictureSource = extras.getString(CandiConstants.EXTRA_PICTURE_SOURCE);
					if (pictureSource != null && !pictureSource.equals("")) {
						if (pictureSource.equals("search")) {
							pictureSearch();
						}
						else if (pictureSource.equals("gallery")) {
							pictureFromGallery();
						}
						else if (pictureSource.equals("camera")) {
							pictureFromCamera();
						}
						else if (pictureSource.equals("place")) {
							pictureFromPlace(mEntityForForm.id);
						}
						else if (pictureSource.equals("default")) {
							usePictureDefault(mEntityForForm);
						}
					}
				}
			}
			else {
				super.onActivityResult(requestCode, resultCode, intent);
			}
		}
	}

	private void usePictureDefault(Entity entity) {
		/*
		 * Setting the photo to null will trigger correct default handling.
		 */
		mDirty = true;
		if (entity.photo != null) {
			entity.photo.setBitmap(null);
			entity.photo = null;
		}
		mEntityBitmap = null;
		mEntityBitmapLocalOnly = false;
		drawImage(entity);
		Tracker.sendEvent("ui_action", "set_entity_picture_to_default", null, 0);
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
				Thread.currentThread().setName("InsertUpdateEntity");
				ModelResult result = new ModelResult();

				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						mCommon.showBusy(R.string.progress_saving, true);
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
				return result.serviceResponse;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ServiceResponse serviceResponse = (ServiceResponse) response;
				mCommon.hideBusy(true);
				if (serviceResponse.responseCode == ResponseCode.Success) {
					finish();
					AnimUtils.doOverridePendingTransition(EntityForm.this, TransitionType.FormToPage);
				}
				else {
					mCommon.handleServiceError(serviceResponse, ServiceOperation.CandiSave, EntityForm.this);
				}
			}

		}.execute();
	}

	private Boolean isDirty() {
		return mDirty;
	}

	private void confirmDirtyExit() {
		final AlertDialog dialog = AircandiCommon.showAlertDialog(null
				, getResources().getString(R.string.alert_entity_dirty_exit_title)
				, getResources().getString(R.string.alert_entity_dirty_exit_message)
				, null
				, this
				, android.R.string.ok
				, android.R.string.cancel
				, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == Dialog.BUTTON_POSITIVE) {
							setResult(Activity.RESULT_CANCELED);
							finish();
							AnimUtils.doOverridePendingTransition(EntityForm.this, TransitionType.FormToPage);
						}
					}
				}
				, null);
		dialog.setCanceledOnTouchOutside(false);
	}

	private void confirmDelete() {
		final AlertDialog dialog = AircandiCommon.showAlertDialog(null
				, getResources().getString(R.string.alert_entity_delete_title)
				, mEntityForForm.type.equals(CandiConstants.TYPE_CANDI_PLACE)
						? getResources().getString(R.string.alert_place_delete_message_single)
						: getResources().getString(R.string.alert_candi_delete_message_single)
				, null
				, this
				, android.R.string.ok
				, android.R.string.cancel
				, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == Dialog.BUTTON_POSITIVE) {
							deleteEntityAtService();
						}
					}
				}
				, null);
		dialog.setCanceledOnTouchOutside(false);
	}

	private ModelResult insertEntityAtService() {
		ModelResult result = new ModelResult();
		List<Beacon> beacons = null;
		Beacon primaryBeacon = null;

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
			beacons = ProxiManager.getInstance().getStrongestBeacons(5);
			primaryBeacon = (beacons.size() > 0) ? beacons.get(0) : null;

			/*
			 * Set location info if this is a place entity
			 */
			if (mEntityForForm.type.equals(CandiConstants.TYPE_CANDI_PLACE)) {
				mEntityForForm.getPlace().provider = "user";
				mEntityForForm.getPlace().id = Aircandi.getInstance().getUser().id;
				/*
				 * We add location info as a consistent feature
				 */
				final Observation observation = LocationManager.getInstance().getObservationLocked();
				if (observation != null) {
					mEntityForForm.place.location = new com.aircandi.service.objects.Location();
					mEntityForForm.place.location.lat = observation.latitude;
					mEntityForForm.place.location.lng = observation.longitude;
				}
			}
		}

		Tracker.sendEvent("ui_action", "entity_insert", mEntityForForm.type, 0);
		Bitmap bitmap = mEntityBitmap;
		if (mEntityBitmapLocalOnly) {
			bitmap = null;
		}
		result = ProxiManager.getInstance().getEntityModel().insertEntity(mEntityForForm, beacons, primaryBeacon, bitmap, false);

		/* Add picture entity if a new picture has been set for a place */
		final Entity entity = (Entity) result.data;
		if (mEntityForForm.type.equals(CandiConstants.TYPE_CANDI_PLACE) && mEntityForForm.photo != null) {
			Entity pictureEntity = makeEntity(CandiConstants.TYPE_CANDI_PICTURE);
			pictureEntity.photo = entity.photo.clone();
			pictureEntity.parentId = entity.id;
			result = ProxiManager.getInstance().getEntityModel().insertEntity(pictureEntity, null, null, null, false);
		}
		return result;
	}

	private ModelResult updateEntityAtService() {
		Tracker.sendEvent("ui_action", "entity_update", mEntityForForm.type, 0);
		Bitmap bitmap = mEntityBitmap;
		if (mEntityBitmapLocalOnly) {
			bitmap = null;
		}

		List<Entity> entities = mEntityForForm.getChildren();

		/* Something in the call caused us to lose the most recent picture. */
		ModelResult result = ProxiManager.getInstance().getEntityModel().updateEntity(mEntityForForm, bitmap);

		if (mEntityForForm.type.equals(CandiConstants.TYPE_CANDI_PLACE) && mEntityForForm.photo != null) {

			entities = mEntityForForm.getChildren();
			Boolean candiMatch = false;
			for (Entity entity : entities) {
				if (entity.type.equals(CandiConstants.TYPE_CANDI_PICTURE)) {
					if (entity.getPhoto().getUri().equals(mEntityForForm.getPhoto().getUri())) {
						candiMatch = true;
						break;
					}
				}
			}
			
			if (!candiMatch) {
				Entity pictureEntity = makeEntity(CandiConstants.TYPE_CANDI_PICTURE);
				pictureEntity.photo = mEntityForForm.photo.clone();
				pictureEntity.parentId = mEntityForForm.id;
				result = ProxiManager.getInstance().getEntityModel().insertEntity(pictureEntity, null, null, null, false);
			}
		}

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
				mCommon.showBusy(R.string.progress_deleting, true);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("DeleteEntity");
				Tracker.sendEvent("ui_action", "entity_delete", mEntityForForm.type, 0);
				final ModelResult result = ProxiManager.getInstance().getEntityModel().deleteEntity(mEntityForForm.id, false);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					Logger.i(this, "Deleted entity: " + mEntityForForm.name);

					mCommon.hideBusy(true);
					ImageUtils.showToastNotification(getString(R.string.alert_deleted), Toast.LENGTH_SHORT);
					setResult(CandiConstants.RESULT_ENTITY_DELETED);
					/*
					 * We either go back to a list or to radar.
					 */
					finish();
					AnimUtils.doOverridePendingTransition(EntityForm.this, TransitionType.FormToPageAfterDelete);
				}
				else {
					mCommon.handleServiceError(result.serviceResponse, ServiceOperation.CandiDelete, EntityForm.this);
				}
			}

		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// Application menu routines (settings)
	// --------------------------------------------------------------------------------------------

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		mCommon.doCreateOptionsMenu(menu);
		if (mEntityForForm != null) {

			if (mEntityForForm.ownerId != null
					&& (mEntityForForm.ownerId.equals(Aircandi.getInstance().getUser().id)
					|| (Aircandi.settings.getBoolean(CandiConstants.PREF_ENABLE_DEV, CandiConstants.PREF_ENABLE_DEV_DEFAULT)
							&& Aircandi.getInstance().getUser().isDeveloper != null
							&& Aircandi.getInstance().getUser().isDeveloper))) {
				MenuItem menuItem = menu.findItem(R.id.delete);
				menuItem.setVisible(true);
			}
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.save) {
			if (isDirty()) {
				doSave();
			}
			else {
				finish();
				AnimUtils.doOverridePendingTransition(EntityForm.this, TransitionType.FormToPage);
			}
			return true;
		}
		else if (item.getItemId() == R.id.delete) {
			confirmDelete();
			return true;
		}

		/* In case we add general menu items later */
		mCommon.doOptionsItemSelected(item);
		return true;
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
		else if (mCommon.mEntityType.equals(CandiConstants.TYPE_CANDI_PLACE)) {
			return R.layout.place_form;
		}
		else {
			return 0;
		}
	}
}