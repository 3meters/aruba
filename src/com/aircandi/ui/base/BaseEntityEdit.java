package com.aircandi.ui.base;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.ServiceConstants;
import com.aircandi.components.AndroidManager;
import com.aircandi.components.EntityManager;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.Logger;
import com.aircandi.components.Maps;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.Tracker;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.service.RequestListener;
import com.aircandi.service.ServiceResponse;
import com.aircandi.service.objects.Applink;
import com.aircandi.service.objects.Beacon;
import com.aircandi.service.objects.Cursor;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Photo.PhotoSource;
import com.aircandi.service.objects.Shortcut;
import com.aircandi.service.objects.ShortcutSettings;
import com.aircandi.service.objects.User;
import com.aircandi.ui.edit.ApplinkEdit;
import com.aircandi.ui.edit.CandigramEdit;
import com.aircandi.ui.edit.CandigramWizard;
import com.aircandi.ui.edit.CommentEdit;
import com.aircandi.ui.edit.PictureEdit;
import com.aircandi.ui.edit.PlaceEdit;
import com.aircandi.ui.user.UserEdit;
import com.aircandi.ui.widgets.AirEditText;
import com.aircandi.ui.widgets.AirImageView;
import com.aircandi.ui.widgets.BuilderButton;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.Animate.TransitionType;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.Routing.Route;
import com.aircandi.utilities.Type;
import com.aircandi.utilities.UI;

public abstract class BaseEntityEdit extends BaseEdit {

	protected AirImageView		mPhotoView;
	protected List<Entity>		mApplinks;

	protected AirEditText		mName;
	protected AirEditText		mDescription;
	protected ViewGroup			mPhotoHolder;
	protected CheckBox			mLocked;
	private TextView			mHintLocked;

	protected RequestListener	mImageRequestListener;
	protected AirImageView		mImageRequestWebImageView;
	protected Uri				mMediaFileUri;
	protected File				mMediaFile;
	protected String			mPhotoSource;

	protected Integer			mInsertProgressResId	= R.string.progress_saving;
	protected Integer			mUpdateProgressResId	= R.string.progress_updating;
	protected Integer			mDeleteProgressResId	= R.string.progress_deleting;
	protected Integer			mInsertedResId			= R.string.alert_inserted;
	protected Integer			mUpdatedResId			= R.string.alert_updated;
	protected Integer			mDeletedResId			= R.string.alert_deleted;

	/* Inputs */
	protected Entity			mEntity;
	public String				mParentId;
	public String				mEntitySchema;

	@Override
	public void unpackIntent() {
		super.unpackIntent();
		/*
		 * Intent inputs:
		 * - Both: Edit_Only
		 * - New: Schema (required), Parent_Entity_Id
		 * - Edit: Entity (required)
		 */
		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			final String jsonEntity = extras.getString(Constants.EXTRA_ENTITY);
			if (jsonEntity != null) {
				mEntity = (Entity) Json.jsonToObject(jsonEntity, Json.ObjectType.ENTITY);
			}

			mParentId = extras.getString(Constants.EXTRA_ENTITY_PARENT_ID);
			mEntitySchema = extras.getString(Constants.EXTRA_ENTITY_SCHEMA);
		}
		mEditing = (mEntity == null ? false : true);
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mName = (AirEditText) findViewById(R.id.name);
		mDescription = (AirEditText) findViewById(R.id.description);
		mPhotoView = (AirImageView) findViewById(R.id.photo);
		mPhotoHolder = (ViewGroup) findViewById(R.id.photo_holder);
		mLocked = (CheckBox) findViewById(R.id.chk_locked);
		mHintLocked = (TextView) findViewById(R.id.hint_locked);

		if (mName != null) {
			mName.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					if (mEntity.name == null || !s.toString().equals(mEntity.name)) {
						if (!mFirstDraw) {
							mDirty = true;
						}
					}
				}
			});
		}
		if (mDescription != null) {
			mDescription.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					if (mEntity.description == null || !s.toString().equals(mEntity.description)) {
						if (!mFirstDraw) {
							mDirty = true;
						}
					}
				}
			});
		}
		if (mLocked != null) {
			mLocked.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					mHintLocked.setText(getString(isChecked ? R.string.form_locked_true_help : R.string.form_locked_false_help));
					if (mEntity.locked != isChecked) {
						if (!mFirstDraw) {
							mDirty = true;
						}
					}
				}
			});
		}
	}

	@Override
	public void bind(BindingMode mode) {
		if (!mEditing && mEntity == null && mEntitySchema != null) {
			mEntity = Entity.makeEntity(mEntitySchema);
			setActivityTitle("new " + mEntity.getSchemaMapped());
			mEntity.creator = Aircandi.getInstance().getCurrentUser();
			mEntity.creatorId = Aircandi.getInstance().getCurrentUser().id;
		}
	}

	@Override
	public void draw() {

		if (mEntity != null) {

			final Entity entity = mEntity;
			if (mEditing) {
				String title = !TextUtils.isEmpty(mEntity.name) ? mEntity.name : mEntity.getSchemaMapped();
				setActivityTitle(title);
			}

			/* Content */

			drawPhoto();

			if (entity.name != null && !entity.name.equals("")) {
				if (findViewById(R.id.name) != null) {
					((TextView) findViewById(R.id.name)).setText(entity.name);
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
				mHintLocked.setText(getString(entity.locked ? R.string.form_locked_true_help : R.string.form_locked_false_help));
			}

			/* Shortcuts */

			if (findViewById(R.id.applinks) != null) {
				drawShortcuts(entity);
			}

			/* Creator block */
			final UserView creator = (UserView) findViewById(R.id.created_by);
			final UserView editor = (UserView) findViewById(R.id.edited_by);

			UI.setVisibility(creator, View.GONE);
			UI.setVisibility(editor, View.GONE);

			if (mEditing) {
				if (creator != null
						&& entity.creator != null
						&& !entity.creator.id.equals(ServiceConstants.ADMIN_USER_ID)
						&& !mEntity.creator.id.equals(ServiceConstants.ANONYMOUS_USER_ID)) {

					creator.setLabel(getString(R.string.candi_label_user_added_by));
					creator.databind(entity.creator, entity.createdDate != null ? entity.createdDate.longValue() : null, entity.locked);
					UI.setVisibility(creator, View.VISIBLE);
				}

				/* Editor block */

				if (editor != null && entity.modifier != null
						&& !entity.modifier.id.equals(ServiceConstants.ADMIN_USER_ID)
						&& !entity.modifier.id.equals(ServiceConstants.ANONYMOUS_USER_ID)) {
					if (entity.createdDate.longValue() != entity.modifiedDate.longValue()) {
						editor.setLabel(getString(R.string.candi_label_user_edited_by));
						editor.databind(entity.modifier, entity.modifiedDate.longValue(), null);
						UI.setVisibility(editor, View.VISIBLE);
					}
				}
			}

			/* Configure UI */
			UI.setVisibility(findViewById(R.id.button_delete), View.GONE);
			if (entity.ownerId != null
					&& (entity.ownerId.equals(Aircandi.getInstance().getCurrentUser().id)
					|| (Aircandi.settings.getBoolean(Constants.PREF_ENABLE_DEV, Constants.PREF_ENABLE_DEV_DEFAULT)
							&& Aircandi.getInstance().getCurrentUser().developer != null
							&& Aircandi.getInstance().getCurrentUser().developer))) {
				UI.setVisibility(findViewById(R.id.button_delete), View.VISIBLE);
			}
			mFirstDraw = false;
		}
	}

	protected void drawPhoto() {
		if (mPhotoView != null) {
			if (mPhotoView.getPhoto() == null
					|| mEntity.photo == null
					|| !mPhotoView.getPhoto().getUri().equals(mEntity.getPhoto().getUri())) {
				UI.drawPhoto(mPhotoView, mEntity.getPhoto());
			}
		}
	}

	protected void drawShortcuts(Entity entity) {

		/*
		 * We are expecting a builder button with a viewgroup to
		 * hold a set of images.
		 */
		final BuilderButton button = (BuilderButton) findViewById(R.id.applinks);

		List<Shortcut> shortcuts = null;

		if (mApplinks != null) {
			shortcuts = new ArrayList<Shortcut>();
			for (Entity applink : mApplinks) {
				Shortcut shortcut = ((Applink) applink).getShortcut();
				shortcuts.add(shortcut);
			}
		}
		else {
			ShortcutSettings settings = new ShortcutSettings(Constants.TYPE_LINK_CONTENT, Constants.SCHEMA_ENTITY_APPLINK, Direction.in, null, false, false);
			shortcuts = (List<Shortcut>) entity.getShortcuts(settings, null, new Shortcut.SortByPositionSortDate());
		}

		Collections.sort(shortcuts, new Shortcut.SortByPositionSortDate());

		if (shortcuts.size() == 0) {
			button.getTextView().setVisibility(View.VISIBLE);
			button.getViewGroup().setVisibility(View.GONE);
		}
		else {
			button.getTextView().setVisibility(View.GONE);
			button.getViewGroup().setVisibility(View.VISIBLE);
			button.getViewGroup().removeAllViews();
			final LayoutInflater inflater = LayoutInflater.from(this);
			final int sizePixels = UI.getRawPixelsForDisplayPixels(this, 30);
			final int marginPixels = UI.getRawPixelsForDisplayPixels(this, 5);

			/* We only show the first five */
			int shortcutCount = 0;
			for (Shortcut shortcut : shortcuts) {
				if (shortcutCount >= 5) {
					break;
				}
				View view = inflater.inflate(R.layout.temp_entity_edit_link_item, null);
				AirImageView photoView = (AirImageView) view.findViewById(R.id.photo);
				photoView.setSizeHint(sizePixels);

				UI.drawPhoto(photoView, shortcut.getPhoto());

				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sizePixels, sizePixels);
				params.setMargins(marginPixels
						, marginPixels
						, marginPixels
						, marginPixels);
				view.setLayoutParams(params);
				button.getViewGroup().addView(view);
				shortcutCount++;
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	public void onAccept() {
		if (isDirty()) {
			if (validate()) {

				/* 
				 * Pull all the control values back into the entity object. Validate
				 * does that too but we don't know if validate is always being performed. 
				 */
				gather();

				if (mSkipSave) {
					final IntentBuilder intentBuilder = new IntentBuilder().setEntity(mEntity);
					setResult(Constants.RESULT_ENTITY_EDITED, intentBuilder.create());
					finish();
					Animate.doOverridePendingTransition(this, TransitionType.FORM_TO_PAGE);
				}
				else {
					if (mEditing) {
						update();
					}
					else {
						insert();
					}
				}
			}
		}
		else {
			onCancel(false);
		}
	}

	@SuppressWarnings("ucd")
	public void onChangePhotoButtonClick(View view) {

		gather(); // So picture logic has the latest property values
		Routing.route(this, Route.PHOTO_SOURCE, mEntity);

		mImageRequestWebImageView = mPhotoView;
		mImageRequestListener = new RequestListener() {

			@Override
			public void onComplete(Object response, Photo photo, Bitmap bitmap, Boolean bitmapLocalOnly) {

				final ServiceResponse serviceResponse = (ServiceResponse) response;
				if (serviceResponse.responseCode == ResponseCode.SUCCESS) {

					mDirty = true;
					if (photo != null) {
						mEntity.photo = photo;
					}

					if (bitmap != null) {
						final String imageKey = mEntity.schema + "_" + mEntity.id + ".jpg";
						mEntity.photo = new Photo(null, null, null, null, PhotoSource.cache);
						mEntity.photo.setBitmap(imageKey, bitmap); // Could get set to null if we are using the default 
						mEntity.photo.setBitmapLocalOnly(bitmapLocalOnly);
						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								UI.drawPhoto(mPhotoView, mEntity.photo, null);
							}
						});
					}
				}
			}
		};
	}

	@SuppressWarnings("ucd")
	public void onUserClick(View view) {
		Entity entity = (Entity) view.getTag();
		Routing.route(this, Route.PROFILE, entity);
	}

	@SuppressWarnings("ucd")
	public void onApplinksBuilderClick(View view) {
		loadApplinks();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		/*
		 * Called before onResume. If we are returning from the market app, we get a zero result code whether the user
		 * decided to start an install or not.
		 */
		if (resultCode != Activity.RESULT_CANCELED) {
			if (requestCode == Constants.ACTIVITY_PICTURE_SOURCE_PICK) {

				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final String photoSource = extras.getString(Constants.EXTRA_PHOTO_SOURCE);

					if (!TextUtils.isEmpty(photoSource)) {
						mPhotoSource = photoSource;
						if (photoSource.equals(Constants.PHOTO_SOURCE_SEARCH)) {
							String defaultSearch = null;
							if (mEntity.name != null) {
								defaultSearch = mEntity.name.trim();
							}
							photoSearch(defaultSearch);
						}
						else if (photoSource.equals(Constants.PHOTO_SOURCE_GALLERY)) {
							photoFromGallery();
						}
						else if (photoSource.equals(Constants.PHOTO_SOURCE_CAMERA)) {
							photoFromCamera();
						}
						else if (photoSource.equals(Constants.PHOTO_SOURCE_PLACE)) {
							photoFromPlace(mEntity);
						}
						else if (photoSource.equals(Constants.PHOTO_SOURCE_DEFAULT)) {
							usePhotoDefault();
						}
						else if (photoSource.equals(Constants.PHOTO_SOURCE_FACEBOOK)) {
							mEntity.photo = new Photo("https://graph.facebook.com/" + ((Applink) mEntity).appId + "/picture?type=large", null, null, null,
									PhotoSource.facebook);
							drawPhoto();
						}
						else if (photoSource.equals(Constants.PHOTO_SOURCE_TWITTER)) {
							mEntity.photo = new Photo(
									"https://api.twitter.com/1/users/profile_image?screen_name=" + ((Applink) mEntity).appId + "&size=bigger",
									null,
									null, null, PhotoSource.twitter);
							drawPhoto();
						}
						else if (photoSource.equals(Constants.PHOTO_SOURCE_WEBSITE_THUMBNAIL)) {
							usePhotoDefault();
						}
					}
				}
			}
			else if (requestCode == Constants.ACTIVITY_PICTURE_PICK_DEVICE) {

				Tracker.sendEvent("ui_action", "select_picture_device", null, 0, Aircandi.getInstance().getCurrentUser());
				final Uri photoUri = intent.getData();

				/* Bitmap size is trimmed if necessary to fit our max in memory image size. */
				Bitmap bitmap = BitmapManager.getInstance().loadBitmapFromDeviceSampled(photoUri);
				if (bitmap != null && mImageRequestListener != null) {
					mImageRequestListener.onComplete(new ServiceResponse(), null, bitmap, false);
				}
			}
			else if (requestCode == Constants.ACTIVITY_PICTURE_MAKE) {

				Tracker.sendEvent("ui_action", "create_picture_camera", null, 0, Aircandi.getInstance().getCurrentUser());
				sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, mMediaFileUri));

				/* Bitmap size is trimmed if necessary to fit our max in memory image size. */
				final Bitmap bitmap = BitmapManager.getInstance().loadBitmapFromDeviceSampled(mMediaFileUri);
				if (bitmap != null && mImageRequestListener != null) {
					mImageRequestListener.onComplete(new ServiceResponse(), null, bitmap, false);
				}
			}
			else if (requestCode == Constants.ACTIVITY_PICTURE_SEARCH) {

				Tracker.sendEvent("ui_action", "select_picture_search", null, 0, Aircandi.getInstance().getCurrentUser());
				if (intent != null && intent.getExtras() != null) {
					
					final Bundle extras = intent.getExtras();
					final String jsonPhoto = extras.getString(Constants.EXTRA_PHOTO);
					final Photo photo = (Photo) Json.jsonToObject(jsonPhoto, Json.ObjectType.PHOTO);
					photo.setBitmapLocalOnly(true);
					photo.photoBroken = Entity.getBrokenPhoto(); 

					if (mImageRequestWebImageView.getPhoto() == null || !mImageRequestWebImageView.getPhoto().getUri().equals(photo.getUri())) {
						UI.drawPhoto(mImageRequestWebImageView, photo, mImageRequestListener);
					}
				}
			}
			else if (requestCode == Constants.ACTIVITY_PICTURE_PICK_PLACE) {

				Tracker.sendEvent("ui_action", "select_picture_place", null, 0, Aircandi.getInstance().getCurrentUser());
				if (intent != null && intent.getExtras() != null) {

					final Bundle extras = intent.getExtras();
					final String jsonPhoto = extras.getString(Constants.EXTRA_PHOTO);
					final Photo photo = (Photo) Json.jsonToObject(jsonPhoto, Json.ObjectType.PHOTO);
					photo.setBitmapLocalOnly(true);
					photo.photoBroken = Entity.getBrokenPhoto(); 

					if (mImageRequestWebImageView.getPhoto() == null || !mImageRequestWebImageView.getPhoto().getUri().equals(photo.getUri())) {
						UI.drawPhoto(mImageRequestWebImageView, photo, mImageRequestListener);
					}
				}
			}
			else {
				super.onActivityResult(requestCode, resultCode, intent);
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	protected void buildPhoto() {
		if (mPhotoSource != null && mEntity.schema.equals(Constants.SCHEMA_ENTITY_APPLINK)) {
			if (mPhotoSource.equals(Constants.PHOTO_SOURCE_FACEBOOK)) {
				mEntity.photo = new Photo("https://graph.facebook.com/" + ((Applink) mEntity).appId + "/picture?type=large", null, null, null,
						PhotoSource.facebook);
			}
			else if (mPhotoSource.equals(Constants.PHOTO_SOURCE_TWITTER)) {
				mEntity.photo = new Photo(
						"https://api.twitter.com/1/users/profile_image?screen_name=" + ((Applink) mEntity).appId + "&size=bigger",
						null,
						null, null, PhotoSource.twitter);
			}
		}
	}

	protected String getLinkType() {
		return null;
	};

	protected void gather() {
		if (findViewById(R.id.name) != null) {
			mEntity.name = Type.emptyAsNull(((TextView) findViewById(R.id.name)).getText().toString().trim());
		}
		if (findViewById(R.id.description) != null) {
			mEntity.description = Type.emptyAsNull(((TextView) findViewById(R.id.description)).getText().toString().trim());
		}
		if (findViewById(R.id.chk_locked) != null) {
			mEntity.locked = ((CheckBox) findViewById(R.id.chk_locked)).isChecked();
		}

		/* Might need to rebuild photo because it requires current property values */
		buildPhoto();
	}

	protected void setEntityType(String type) {
		mEntity.type = type;
	}

	public static Class<?> editFormBySchema(String schema) {
		if (schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
			return PlaceEdit.class;
		}
		else if (schema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {
			return PictureEdit.class;
		}
		else if (schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)) {
			return CandigramEdit.class;
		}
		else if (schema.equals(Constants.SCHEMA_ENTITY_APPLINK)) {
			return ApplinkEdit.class;
		}
		else if (schema.equals(Constants.SCHEMA_ENTITY_USER)) {
			return UserEdit.class;
		}
		else if (schema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
			return CommentEdit.class;
		}
		return null;
	}

	public static Class<?> insertFormBySchema(String schema) {
		if (schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
			return PlaceEdit.class;
		}
		else if (schema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {
			return PictureEdit.class;
		}
		else if (schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)) {
			return CandigramWizard.class;
		}
		else if (schema.equals(Constants.SCHEMA_ENTITY_APPLINK)) {
			return ApplinkEdit.class;
		}
		else if (schema.equals(Constants.SCHEMA_ENTITY_USER)) {
			return UserEdit.class;
		}
		else if (schema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
			return CommentEdit.class;
		}
		return null;
	}

	private void loadApplinks() {
		/*
		 * First, we need the real applinks to send them to the applinks editor
		 */
		if (mApplinks != null) {
			doApplinksBuilder();
		}
		else {
			new AsyncTask() {

				@Override
				protected void onPreExecute() {
					showBusy(R.string.progress_loading_applinks, false);
				}

				@Override
				protected Object doInBackground(Object... params) {
					Thread.currentThread().setName("BindApplinks");

					List<String> linkTypes = new ArrayList<String>();
					List<String> schemas = new ArrayList<String>();
					linkTypes.add(Constants.TYPE_LINK_CONTENT);
					schemas.add(Constants.SCHEMA_ENTITY_APPLINK);

					Cursor cursor = new Cursor()
							.setLimit(ServiceConstants.PAGE_SIZE_APPLINKS)
							.setSort(Maps.asMap("modifiedDate", -1))
							.setSkip(0)
							.setSchemas(schemas)
							.setLinkTypes(linkTypes);

					ModelResult result = EntityManager.getInstance().loadEntitiesForEntity(mEntity.id, null, cursor, null);

					return result;
				}

				@Override
				protected void onPostExecute(Object modelResult) {
					final ModelResult result = (ModelResult) modelResult;
					hideBusy();
					if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
						mApplinks = (List<Entity>) result.data;
						doApplinksBuilder();
					}
					else {
						Errors.handleError(BaseEntityEdit.this, result.serviceResponse);
					}
				}

			}.execute();
		}
	}

	public void doApplinksBuilder() {

		/* Serialize the applinks */
		Bundle extras = new Bundle();
		if (mApplinks.size() > 0) {
			final List<String> applinkStrings = new ArrayList<String>();
			for (Entity applink : mApplinks) {
				applinkStrings.add(Json.objectToJson(applink, Json.UseAnnotations.FALSE, Json.ExcludeNulls.TRUE));
			}
			extras.putStringArrayList(Constants.EXTRA_ENTITIES, (ArrayList<String>) applinkStrings);
		}

		extras.putString(Constants.EXTRA_LIST_LINK_SCHEMA, Constants.SCHEMA_ENTITY_APPLINK);
		Routing.route(this, Route.APPLINKS_EDIT, mEntity, null, null, extras);
	}

	protected void usePhotoDefault() {
		/*
		 * Setting the photo to null will trigger correct default handling.
		 */
		mDirty = true;
		if (mEntity.photo != null) {
			mEntity.photo.removeBitmap();
			mEntity.photo = null;
		}
		drawPhoto();
		Tracker.sendEvent("ui_action", "set_" + mEntitySchema + "_photo_to_default", null, 0, Aircandi.getInstance().getCurrentUser());
	}

	// --------------------------------------------------------------------------------------------
	// Pickers
	// --------------------------------------------------------------------------------------------

	@SuppressLint("InlinedApi")
	protected void photoFromGallery() {
		Routing.route(this, Route.PHOTO_FROM_GALLERY);
	}

	protected void photoFromCamera() {
		Bundle extras = new Bundle();
		mMediaFile = AndroidManager.getOutputMediaFile(AndroidManager.MEDIA_TYPE_IMAGE);
		if (mMediaFile != null) {
			mMediaFileUri = Uri.fromFile(mMediaFile);
			extras.putParcelable(MediaStore.EXTRA_OUTPUT, mMediaFileUri);
			Routing.route(this, Route.PHOTO_FROM_CAMERA, mEntity, null, null, extras);
		}
	}

	protected void photoSearch(String defaultSearch) {
		Bundle extras = new Bundle();
		extras.putString(Constants.EXTRA_SEARCH_PHRASE, defaultSearch);
		Routing.route(this, Route.PHOTO_SEARCH, null, null, null, extras);
	}

	protected void photoFromPlace(Entity entity) {
		Routing.route(this, Route.PHOTO_PLACE_SEARCH, entity);
	}

	// --------------------------------------------------------------------------------------------
	// Services
	// --------------------------------------------------------------------------------------------

	@Override
	protected void insert() {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusyManager.showBusy(mInsertProgressResId);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("InsertUpdateEntity");

				ModelResult result = new ModelResult();
				List<Beacon> beacons = null;
				Beacon primaryBeacon = null;
				Link link = null;

				/* If parent id then this is a child */
				if (mEntity.linksIn != null) {
					mEntity.linksIn.clear();
				}
				if (mEntity.linksOut != null) {
					mEntity.linksOut.clear();
				}

				if (mParentId != null) {
					mEntity.toId = mParentId;
					link = new Link(mParentId, getLinkType(), mEntity.schema, true);
					if (mEntity.schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)) {
						link.strong = false;
					}
				}
				else {
					mEntity.toId = null;
				}

				/* We only send beacons if a place is being inserted */
				if (mEntity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
					beacons = ProximityManager.getInstance().getStrongestBeacons(ServiceConstants.PROXIMITY_BEACON_COVERAGE);
					primaryBeacon = (beacons.size() > 0) ? beacons.get(0) : null;
				}

				Bitmap bitmap = null;
				if (mEntity.photo != null && mEntity.photo.hasBitmap() && !mEntity.photo.isBitmapLocalOnly()) {
					bitmap = mEntity.photo.getBitmap();
				}

				/* In case a derived class needs to augment the entity before insert */
				beforeInsert(mEntity);

				result = EntityManager.getInstance().insertEntity(mEntity, link, beacons, primaryBeacon, bitmap);

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					Entity insertedEntity = (Entity) result.data;

					if (mApplinks != null) {
						result = EntityManager.getInstance().replaceEntitiesForEntity(insertedEntity.id, mApplinks, Constants.SCHEMA_ENTITY_APPLINK);
						/*
						 * Need to update the linkIn for the entity or these won't show
						 * without a service refresh.
						 */
					}

					UI.showToastNotification(getString(mInsertedResId), Toast.LENGTH_SHORT);
					final IntentBuilder intentBuilder = new IntentBuilder().setEntityId(insertedEntity.id);
					setResult(Constants.RESULT_ENTITY_INSERTED, intentBuilder.create());
				}
				return result.serviceResponse;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ServiceResponse serviceResponse = (ServiceResponse) response;
				hideBusy();
				if (serviceResponse.responseCode == ResponseCode.SUCCESS) {
					setResult(Constants.RESULT_ENTITY_INSERTED);
					finish();
					Animate.doOverridePendingTransition(BaseEntityEdit.this, TransitionType.CANDIGRAM_OUT);
				}
				else {
					Errors.handleError(BaseEntityEdit.this, serviceResponse);
				}
			}

		}.execute();
	}

	@Override
	protected void update() {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusyManager.showBusy(mUpdateProgressResId);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("InsertUpdateEntity");
				ModelResult result = new ModelResult();

				/* Save applinks first */
				if (mApplinks != null) {
					result = EntityManager.getInstance().replaceEntitiesForEntity(mEntity.id, mApplinks, Constants.SCHEMA_ENTITY_APPLINK);
				}

				/* Update entity */
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

					Bitmap bitmap = null;
					if (mEntity.photo != null && mEntity.photo.hasBitmap() && !mEntity.photo.isBitmapLocalOnly()) {
						bitmap = mEntity.photo.getBitmap();
					}

					result = EntityManager.getInstance().updateEntity(mEntity, bitmap);

					if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
						if (mEntity.schema.equals(Constants.SCHEMA_ENTITY_USER)) {
							if (Aircandi.getInstance().getCurrentUser().id.equals(mEntity.id)) {

								/* We also need to update the user that has been persisted for AUTO sign in. */
								final String jsonUser = Json.objectToJson(mEntity);
								Aircandi.settingsEditor.putString(Constants.SETTING_USER, jsonUser);
								Aircandi.settingsEditor.commit();

								/* Update the global user but retain the session info */
								((User) mEntity).session = Aircandi.getInstance().getCurrentUser().session;
								Aircandi.getInstance().setCurrentUser((User) mEntity);
							}
						}
					}
				}
				return result.serviceResponse;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ServiceResponse serviceResponse = (ServiceResponse) response;
				hideBusy();
				if (serviceResponse.responseCode == ResponseCode.SUCCESS) {
					UI.showToastNotification(getString(mUpdatedResId), Toast.LENGTH_SHORT);
					setResult(Constants.RESULT_ENTITY_UPDATED);
					finish();
					Animate.doOverridePendingTransition(BaseEntityEdit.this, TransitionType.FORM_TO_PAGE);
				}
				else {
					Errors.handleError(BaseEntityEdit.this, serviceResponse);
				}
			}

		}.execute();
	}

	@Override
	protected void delete() {
		/*
		 * TODO: We need to update the service so the recursive entity delete also deletes any associated resources
		 * stored with S3. As currently coded, we will be orphaning any images associated with child entities.
		 */

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusyManager.showBusy(mDeleteProgressResId);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("DeleteEntity");
				final ModelResult result = EntityManager.getInstance().deleteEntity(mEntity.id, false);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					Logger.i(this, "Deleted entity: " + mEntity.name);

					/*
					 * We either go back to a list or to radar.
					 */
					hideBusy();
					UI.showToastNotification(getString(mDeletedResId), Toast.LENGTH_SHORT);
					setResult(Constants.RESULT_ENTITY_DELETED);
					finish();
					Animate.doOverridePendingTransition(BaseEntityEdit.this, TransitionType.FORM_TO_PAGE_AFTER_DELETE);
				}
				else {
					Errors.handleError(BaseEntityEdit.this, result.serviceResponse);
				}
			}

		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		if (mEntity != null) {

			MenuItem menuItem = menu.findItem(R.id.delete);
			if (menuItem != null) {
				menuItem.setVisible(false);
				if (mEntity.ownerId != null && Aircandi.getInstance().getCurrentUser() != null
						&& (mEntity.ownerId.equals(Aircandi.getInstance().getCurrentUser().id)
						|| (Aircandi.settings.getBoolean(Constants.PREF_ENABLE_DEV, Constants.PREF_ENABLE_DEV_DEFAULT)
								&& Aircandi.getInstance().getCurrentUser().developer != null
								&& Aircandi.getInstance().getCurrentUser().developer))) {
					menuItem.setVisible(true);
				}
			}
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return Routing.route(this, Routing.routeForMenuId(item.getItemId()), mEntity);
	}

}