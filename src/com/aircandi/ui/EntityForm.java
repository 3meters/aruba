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
import com.aircandi.Constants;
import com.aircandi.ProxiConstants;
import com.aircandi.beta.R;
import com.aircandi.components.AircandiCommon;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.CommandType;
import com.aircandi.components.EntityManager;
import com.aircandi.components.FontManager;
import com.aircandi.components.LocationManager;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProximityManager;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.Tracker;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequestBuilder;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.RequestListener;
import com.aircandi.service.HttpService.ServiceDataType;
import com.aircandi.service.objects.AirLocation;
import com.aircandi.service.objects.Applink;
import com.aircandi.service.objects.Beacon;
import com.aircandi.service.objects.Category;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Photo.PhotoSource;
import com.aircandi.service.objects.Place;
import com.aircandi.service.objects.Post;
import com.aircandi.service.objects.ProviderMap;
import com.aircandi.service.objects.User;
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
import com.aircandi.utilities.MiscUtils;

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
		if (mCommon.mEntitySchema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
			mCommon.mActionBar.setTitle(R.string.form_title_place);
		}
		else if (mCommon.mEntitySchema.equals(Constants.SCHEMA_ENTITY_POST)) {
			mCommon.mActionBar.setTitle(R.string.form_title_candigram);
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
					if (mEntityForForm.name == null || !s.toString().equals(mEntityForForm.name)) {
						mDirty = true;
					}
				}
			});
		}
		if (mDescription != null) {
			mDescription.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					if (mEntityForForm.description == null || !s.toString().equals(mEntityForForm.description)) {
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
		if (mCommon.mEntitySchema.equals(Constants.SCHEMA_ENTITY_PLACE)) {}

		if (mCommon.mCommandType == CommandType.New) {
			mEntityForForm = makeEntity(mCommon.mEntitySchema);
		}
		else {
			if (mEntityForForm == null && mCommon.mEntityId != null) {
				/*
				 * Entity is coming from entity model. We want to create a clone so
				 * that any changes only show up in the entity model if the changes make it
				 * to the service.
				 */
				final Entity entityForModel = EntityManager.getInstance().getEntity(mCommon.mEntityId);
				if (entityForModel != null) {
					mEntityForForm = entityForModel.clone();
				}
			}
		}
	}

	private Entity makeEntity(String schema) {
		if (schema == null) {
			throw new IllegalArgumentException("EntityForm.makeEntity(): schema parameter is null");
		}
		Entity entity = null;

		if (schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
			entity = new Place();
		}
		else if (schema.equals(Constants.SCHEMA_ENTITY_POST)) {
			entity = new Post();
		}
		else if (schema.equals(Constants.SCHEMA_ENTITY_APPLINK)) {
			entity = new Applink();
		}

		entity.schema = schema;
		entity.signalFence = -100.0f;

		if (entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
			((Place) entity).provider = new ProviderMap();
			((Place) entity).provider.aircandi = Aircandi.getInstance().getUser().id;
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
			if (entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
				Place place = (Place) entity;

				drawApplinks(place);

				if (findViewById(R.id.address) != null) {
					final String addressBlock = place.getAddressBlock();
					if (!addressBlock.equals("")) {
						((BuilderButton) findViewById(R.id.address)).setText(place.address);
					}
				}
				if (place.category != null) {
					if (findViewById(R.id.category) != null) {
						((BuilderButton) findViewById(R.id.category)).setText(place.category.name);
					}
				}
			}

			/* Creator block */
			final UserView creator = (UserView) findViewById(R.id.created_by);
			final UserView editor = (UserView) findViewById(R.id.edited_by);

			setVisibility(creator, View.GONE);
			if (creator != null
					&& entity.creator != null
					&& !entity.creator.id.equals(ProxiConstants.ADMIN_USER_ID)) {

				if (entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
					if (((Place) entity).getProvider().type.equals("aircandi")) {
						creator.setLabel(getString(R.string.candi_label_user_created_by));
						creator.bindToUser(entity.creator, entity.createdDate.longValue(), entity.locked);
						setVisibility(creator, View.VISIBLE);
					}
				}
				else {
					creator.setLabel(getString(R.string.candi_label_user_added_by));
					creator.bindToUser(entity.creator, entity.createdDate.longValue(), entity.locked);
					setVisibility(creator, View.VISIBLE);
				}
			}

			/* Editor block */

			setVisibility(editor, View.GONE);
			if (editor != null && entity.modifier != null && !entity.modifier.id.equals(ProxiConstants.ADMIN_USER_ID)) {
				if (entity.createdDate.longValue() != entity.modifiedDate.longValue()) {
					editor.setLabel(getString(R.string.candi_label_user_edited_by));
					editor.bindToUser(entity.modifier, entity.modifiedDate.longValue(), null);
					setVisibility(editor, View.VISIBLE);
				}
			}

			/* Configure UI */
			setVisibility(findViewById(R.id.button_delete), View.GONE);
			if (entity.ownerId != null
					&& (entity.ownerId.equals(Aircandi.getInstance().getUser().id)
					|| (Aircandi.settings.getBoolean(Constants.PREF_ENABLE_DEV, Constants.PREF_ENABLE_DEV_DEFAULT)
							&& Aircandi.getInstance().getUser().developer != null
							&& Aircandi.getInstance().getUser().developer))) {
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
				if (entity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
					Place place = (Place) entity;
					if (place.photo == null && place.category != null) {

						final int color = Place.getCategoryColor((place.category != null)
								? place.category.name
								: null, true, mMuteColor, false);

						mImageViewPicture.getImageView().setColorFilter(color, PorterDuff.Mode.MULTIPLY);
						mColorResId = Place.getCategoryColorResId(place.category != null ? place.category.name : null,
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

				final String imageUri = entity.getPhotoUri();
				final BitmapRequest bitmapRequest = new BitmapRequest(imageUri, mImageViewPicture.getImageView());
				bitmapRequest.setImageSize(mImageViewPicture.getSizeHint());
				bitmapRequest.setImageRequestor(mImageViewPicture.getImageView());
				mImageViewPicture.getImageView().setTag(imageUri);
				BitmapManager.getInstance().masterFetch(bitmapRequest);
			}
		}
	}

	private void drawApplinks(Entity entity) {
		if (findViewById(R.id.sources) != null) {
			/*
			 * We are expecting a builder button with a viewgroup to
			 * hold a set of images.
			 */
			final BuilderButton button = (BuilderButton) findViewById(R.id.sources);
			List<Applink> applinks = (List<Applink>) entity.getLinkedEntitiesByLinkType(Constants.TYPE_LINK_APPLINK, null, Direction.in, false);
			if (applinks.size() == 0) {
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
				int applinkCount = 0;
				for (Applink applink : applinks) {
					if (applinkCount >= 5) {
						break;
					}
					if (applink.system != null && applink.system) {
						continue;
					}
					View view = inflater.inflate(R.layout.temp_radar_candi_item, null);
					WebImageView webImageView = (WebImageView) view.findViewById(R.id.image);
					webImageView.setSizeHint(sizePixels);

					String imageUri = applink.getPhotoUri();
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
					applinkCount++;
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
		mCommon.showPictureSourcePicker(mEntityForForm.id, null);
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
		final String jsonPlace = HttpService.convertObjectToJsonSmart(mEntityForForm, false, true);
		intent.putExtra(Constants.EXTRA_PLACE, jsonPlace);
		startActivityForResult(intent, Constants.ACTIVITY_ADDRESS_EDIT);
		AnimUtils.doOverridePendingTransition(this, TransitionType.PageToForm);
	}

	@SuppressWarnings("ucd")
	public void onCategoryBuilderClick(View view) {
		final Intent intent = new Intent(this, CategoryBuilder.class);
		if (((Place) mEntityForForm).category != null) {
			final String jsonCategory = HttpService.convertObjectToJsonSmart(((Place) mEntityForForm).category, false, true);
			intent.putExtra(Constants.EXTRA_CATEGORY, jsonCategory);
		}
		startActivityForResult(intent, Constants.ACTIVITY_CATEGORY_EDIT);
		AnimUtils.doOverridePendingTransition(this, TransitionType.PageToForm);
	}

	@SuppressWarnings("ucd")
	public void onApplinksBuilderClick(View view) {
		final Intent intent = new Intent(this, SourcesBuilder.class);

		/* Serialize the sources for the current entity */
		List<Applink> applinks = (List<Applink>) mEntityForForm.getLinkedEntitiesByLinkType(Constants.TYPE_LINK_APPLINK, null, Direction.in, false);

		if (applinks.size() > 0) {
			final List<String> applinkStrings = new ArrayList<String>();
			for (Applink applink : applinks) {
				applinkStrings.add(HttpService.convertObjectToJsonSmart(applink, true, true));
			}
			intent.putStringArrayListExtra(Constants.EXTRA_APPLINKS, (ArrayList<String>) applinkStrings);
		}

		if (mEntityForForm.id != null) {
			intent.putExtra(Constants.EXTRA_ENTITY_ID, mEntityForForm.id);
		}

		startActivityForResult(intent, Constants.ACTIVITY_SOURCES_EDIT);
		AnimUtils.doOverridePendingTransition(this, TransitionType.PageToForm);
	}

	public void onUserClick(View view) {
		User user = (User) view.getTag();
		mCommon.doUserClick(user);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {

		if (resultCode == Activity.RESULT_OK) {
			if (requestCode == Constants.ACTIVITY_ADDRESS_EDIT) {
				if (intent != null && intent.getExtras() != null) {
					mDirty = true;
					final Bundle extras = intent.getExtras();

					final String jsonPlace = extras.getString(Constants.EXTRA_PLACE);
					if (jsonPlace != null) {
						final Place placeUpdated = (Place) HttpService.convertJsonToObjectInternalSmart(jsonPlace, ServiceDataType.Place);
						if (placeUpdated.phone != null) {
							placeUpdated.phone = placeUpdated.phone.replaceAll("[^\\d.]", "");
						}
						mEntityForForm = placeUpdated;
						((BuilderButton) findViewById(R.id.address)).setText(((Place) mEntityForForm).address);
					}
				}
			}
			else if (requestCode == Constants.ACTIVITY_CATEGORY_EDIT) {
				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final String jsonCategory = extras.getString(Constants.EXTRA_CATEGORY);
					if (jsonCategory != null) {
						final Category categoryUpdated = (Category) HttpService.convertJsonToObjectInternalSmart(jsonCategory, ServiceDataType.Category);
						if (categoryUpdated != null) {
							mDirty = true;
							((Place) mEntityForForm).category = categoryUpdated;
							((BuilderButton) findViewById(R.id.category)).setText(categoryUpdated.name);
							drawImage(mEntityForForm);
						}
					}
				}
			}
			else if (requestCode == Constants.ACTIVITY_SOURCES_EDIT) {
				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final List<String> jsonApplinks = extras.getStringArrayList(Constants.EXTRA_APPLINKS);
					final List<Applink> applinks = new ArrayList<Applink>();
					for (String jsonApplink : jsonApplinks) {
						Applink applink = (Applink) HttpService.convertJsonToObjectInternalSmart(jsonApplink, ServiceDataType.Applink);
						applinks.add(applink);
					}
					mDirty = true;
					/*
					 * FIXME: update applinks in cache
					 */
					drawApplinks(mEntityForForm);
				}
			}
			else if (requestCode == Constants.ACTIVITY_PICTURE_SOURCE_PICK) {
				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final String pictureSource = extras.getString(Constants.EXTRA_PICTURE_SOURCE);
					if (pictureSource != null && !pictureSource.equals("")) {
						if (pictureSource.equals("search")) {
							String defaultSearch = null;
							if (findViewById(R.id.text_title) != null) {
								defaultSearch = MiscUtils.emptyAsNull(((TextView) findViewById(R.id.text_title)).getText().toString().trim());
							}
							pictureSearch(defaultSearch);
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
		Tracker.sendEvent("ui_action", "set_entity_picture_to_default", null, 0, Aircandi.getInstance().getUser());
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	private void gather(Entity entity) {
		if (findViewById(R.id.text_title) != null) {
			entity.name = MiscUtils.emptyAsNull(((TextView) findViewById(R.id.text_title)).getText().toString().trim());
		}
		if (findViewById(R.id.description) != null) {
			entity.description = MiscUtils.emptyAsNull(((TextView) findViewById(R.id.description)).getText().toString().trim());
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
			protected void onPreExecute() {
				mCommon.showBusy(R.string.progress_saving, true);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("InsertUpdateEntity");
				ModelResult result = new ModelResult();

				if (mCommon.mCommandType == CommandType.New) {
					/*
					 * Pull all the control values back into the entity object
					 */
					gather(mEntityForForm);
					result = insertEntityAtService();

					if (result.serviceResponse.responseCode == ResponseCode.Success) {
						ImageUtils.showToastNotification(getString(R.string.alert_inserted), Toast.LENGTH_SHORT);

						/* Return the id of the inserted entity in case the caller can use it */
						final Intent intent = new Intent();
						intent.putExtra(Constants.EXTRA_ENTITY_ID, ((Entity) result.data).id);
						setResult(Constants.RESULT_ENTITY_INSERTED, intent);
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
						setResult(Constants.RESULT_ENTITY_UPDATED);
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
					mCommon.handleServiceError(serviceResponse, ServiceOperation.CandiSave);
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
				, EntityForm.this
				, R.string.alert_dirty_save
				, android.R.string.cancel
				, R.string.alert_dirty_discard
				, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == Dialog.BUTTON_POSITIVE) {
							doSave();
						}
						else if (which == Dialog.BUTTON_NEUTRAL) {
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
				, mEntityForForm.type.equals(Constants.SCHEMA_ENTITY_PLACE)
						? getResources().getString(R.string.alert_place_delete_message_single)
						: getResources().getString(R.string.alert_candi_delete_message_single)
				, null
				, this
				, android.R.string.ok
				, android.R.string.cancel
				, null
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
		if (mEntityForForm.linksIn != null) {
			mEntityForForm.linksIn.clear();
		}
		if (mEntityForForm.linksOut != null) {
			mEntityForForm.linksOut.clear();
		}

		if (mCommon.mParentId != null) {
			mEntityForForm.toId = mCommon.mParentId;
		}
		else {
			mEntityForForm.toId = null;
		}

		/* We always send beacons to support nearby notifications */
		beacons = ProximityManager.getInstance().getStrongestBeacons(5);
		primaryBeacon = (beacons.size() > 0) ? beacons.get(0) : null;

		/*
		 * Set location info if this is a place entity
		 */
		if (mEntityForForm.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
			((Place) mEntityForForm).provider.aircandi = Aircandi.getInstance().getUser().id;
			/*
			 * We add location info as a consistent feature
			 */
			final AirLocation location = LocationManager.getInstance().getAirLocationLocked();
			if (location != null) {
				((Place) mEntityForForm).location.lat = location.lat;
				((Place) mEntityForForm).location.lng = location.lng;
			}
		}

		Tracker.sendEvent("ui_action", "entity_insert", mEntityForForm.type, 0, Aircandi.getInstance().getUser());
		Bitmap bitmap = mEntityBitmap;
		if (mEntityBitmapLocalOnly) bitmap = null;

		result = EntityManager.getInstance().insertEntity(mEntityForForm, beacons, primaryBeacon, bitmap, false, null);

		/* Add picture entity if a new picture has been set for a place */
		if (result.serviceResponse.responseCode == ResponseCode.Success) {
			final Entity entity = (Entity) result.data;
			if (mEntityForForm.type.equals(Constants.SCHEMA_ENTITY_PLACE) && mEntityForForm.photo != null) {
				Entity pictureEntity = makeEntity(Constants.SCHEMA_ENTITY_POST);
				pictureEntity.photo = entity.photo.clone();
				pictureEntity.toId = entity.id;
				result = EntityManager.getInstance().insertEntity(pictureEntity, null, null, null, false, true);
			}
		}
		return result;
	}

	private ModelResult updateEntityAtService() {

		Tracker.sendEvent("ui_action", "entity_update", mEntityForForm.type, 0, Aircandi.getInstance().getUser());
		Bitmap bitmap = mEntityBitmap;
		if (mEntityBitmapLocalOnly) {
			bitmap = null;
		}

		/* Something in the call caused us to lose the most recent picture. */
		ModelResult result = EntityManager.getInstance().updateEntity(mEntityForForm, bitmap);

		if (result.serviceResponse.responseCode == ResponseCode.Success) {
			/*
			 * If photo changed, add a child picture entity if we don't already have it.
			 */
			if (mEntityForForm.type.equals(Constants.SCHEMA_ENTITY_PLACE) && mEntityForForm.photo != null) {

				List<Entity> entities = (List<Entity>) mEntityForForm.getLinkedEntitiesByLinkType(Constants.TYPE_LINK_POST, null, Direction.in, false);
				Boolean candiMatch = false;
				for (Entity entity : entities) {
					if (entity.getPhotoUri() != null && entity.getPhotoUri().equals(mEntityForForm.getPhotoUri())) {
						candiMatch = true;
						break;
					}
				}

				if (!candiMatch) {
					Entity pictureEntity = makeEntity(Constants.SCHEMA_ENTITY_POST);
					pictureEntity.photo = mEntityForForm.photo.clone();
					pictureEntity.toId = mEntityForForm.id;
					result = EntityManager.getInstance().insertEntity(pictureEntity, null, null, null, false, true);
				}
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
				Tracker.sendEvent("ui_action", "entity_delete", mEntityForForm.type, 0, Aircandi.getInstance().getUser());
				final ModelResult result = EntityManager.getInstance().deleteEntity(mEntityForForm.id, false);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					Logger.i(this, "Deleted entity: " + mEntityForForm.name);

					mCommon.hideBusy(true);
					ImageUtils.showToastNotification(getString(R.string.alert_deleted), Toast.LENGTH_SHORT);
					setResult(Constants.RESULT_ENTITY_DELETED);
					/*
					 * We either go back to a list or to radar.
					 */
					finish();
					AnimUtils.doOverridePendingTransition(EntityForm.this, TransitionType.FormToPageAfterDelete);
				}
				else {
					mCommon.handleServiceError(result.serviceResponse, ServiceOperation.CandiDelete);
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

			MenuItem menuItem = menu.findItem(R.id.delete);
			menuItem.setVisible(false);
			if (mEntityForForm.ownerId != null && Aircandi.getInstance().getUser() != null
					&& (mEntityForForm.ownerId.equals(Aircandi.getInstance().getUser().id)
					|| (Aircandi.settings.getBoolean(Constants.PREF_ENABLE_DEV, Constants.PREF_ENABLE_DEV_DEFAULT)
							&& Aircandi.getInstance().getUser().developer != null
							&& Aircandi.getInstance().getUser().developer))) {
				menuItem.setVisible(true);
			}
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.accept) {
			if (isDirty()) {
				doSave();
			}
			else {
				finish();
				AnimUtils.doOverridePendingTransition(EntityForm.this, TransitionType.FormToPage);
			}
			return true;
		}
		else if (item.getItemId() == R.id.cancel) {
			if (isDirty()) {
				confirmDirtyExit();
			}
			else {
				setResult(Activity.RESULT_CANCELED);
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
	protected int getLayoutId() {
		if (mCommon.mEntitySchema.equals(Constants.SCHEMA_ENTITY_POST)) {
			return R.layout.picture_form;
		}
		else if (mCommon.mEntitySchema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
			return R.layout.place_form;
		}
		else {
			return 0;
		}
	}
}