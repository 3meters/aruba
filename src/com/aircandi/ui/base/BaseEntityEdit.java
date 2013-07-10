package com.aircandi.ui.base;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.ProxiConstants;
import com.aircandi.beta.R;
import com.aircandi.components.AndroidManager;
import com.aircandi.components.EntityManager;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.Logger;
import com.aircandi.components.Maps;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProximityManager;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.Tracker;
import com.aircandi.components.bitmaps.BitmapManager;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequest.ImageResponse;
import com.aircandi.components.bitmaps.BitmapRequestBuilder;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.ExcludeNulls;
import com.aircandi.service.HttpService.ObjectType;
import com.aircandi.service.HttpService.RequestListener;
import com.aircandi.service.HttpService.UseAnnotations;
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
import com.aircandi.ui.edit.ApplinkEdit;
import com.aircandi.ui.edit.ApplinkListEdit;
import com.aircandi.ui.edit.PlaceEdit;
import com.aircandi.ui.edit.PostEdit;
import com.aircandi.ui.helpers.PicturePicker;
import com.aircandi.ui.helpers.PictureSourcePicker;
import com.aircandi.ui.widgets.BuilderButton;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.ui.widgets.WebImageView;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.Animate.TransitionType;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.UI;
import com.aircandi.utilities.Utilities;

public abstract class BaseEntityEdit extends BaseEdit {

	protected WebImageView		mPhoto;
	protected List<Entity>		mApplinks;

	protected TextView			mName;
	protected TextView			mDescription;
	protected ViewGroup			mPhotoHolder;
	protected CheckBox			mLocked;

	protected String			mImageUriOriginal;
	protected RequestListener	mImageRequestListener;
	protected WebImageView		mImageRequestWebImageView;
	protected Uri				mMediaFileUri;
	protected String			mMediaFilePath;
	protected File				mMediaFile;

	/* Inputs */
	protected Entity			mEntity;
	public String				mParentId;
	public String				mEntityId;
	public String				mEntitySchema;
	public String				mMessage;

	@Override
	protected void unpackIntent() {
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
				mEntity = (Entity) HttpService.jsonToObject(jsonEntity, ObjectType.Entity);
			}

			mParentId = extras.getString(Constants.EXTRA_ENTITY_PARENT_ID);
			mEntitySchema = extras.getString(Constants.EXTRA_ENTITY_SCHEMA);
			mEntityId = extras.getString(Constants.EXTRA_ENTITY_ID);
			mMessage = extras.getString(Constants.EXTRA_MESSAGE);
		}
	}

	@Override
	protected void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mName = (EditText) findViewById(R.id.name);
		mDescription = (TextView) findViewById(R.id.description);
		mPhoto = (WebImageView) findViewById(R.id.photo);
		mPhotoHolder = (ViewGroup) findViewById(R.id.photo_holder);
		mLocked = (CheckBox) findViewById(R.id.chk_locked);

		if (mName != null) {
			mName.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					if (mEntity.name == null || !s.toString().equals(mEntity.name)) {
						mDirty = true;
					}
				}
			});
		}
		if (mDescription != null) {
			mDescription.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					if (mEntity.description == null || !s.toString().equals(mEntity.description)) {
						mDirty = true;
					}
				}
			});
		}
		if (mLocked != null) {
			mLocked.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (mEntity.locked != isChecked) {
						mDirty = true;
					}
				}
			});
		}
	}

	@Override
	protected void bind() {
		if (mEntity == null) {
			mEntity = Entity.makeEntity(mEntitySchema);
			mEditing = false;
			mActionBar.setTitle(mEntity.schema);
		}
		else {
			mEditing = true;
			mActionBar.setTitle(mEntity.name);
		}
	}

	@Override
	protected void draw() {

		if (mEntity != null) {

			final Entity entity = mEntity;

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
			}

			/* Shortcuts */

			drawShortcuts(entity);

			/* Creator block */
			final UserView creator = (UserView) findViewById(R.id.created_by);
			final UserView editor = (UserView) findViewById(R.id.edited_by);

			UI.setVisibility(creator, View.GONE);
			if (creator != null
					&& entity.creator != null
					&& !entity.creator.id.equals(ProxiConstants.ADMIN_USER_ID)) {

				creator.setLabel(getString(R.string.candi_label_user_added_by));
				creator.bindToUser(entity.creator, entity.createdDate != null ? entity.createdDate.longValue() : null, entity.locked);
				UI.setVisibility(creator, View.VISIBLE);
			}

			/* Editor block */

			UI.setVisibility(editor, View.GONE);
			if (editor != null && entity.modifier != null && !entity.modifier.id.equals(ProxiConstants.ADMIN_USER_ID)) {
				if (entity.createdDate.longValue() != entity.modifiedDate.longValue()) {
					editor.setLabel(getString(R.string.candi_label_user_edited_by));
					editor.bindToUser(entity.modifier, entity.modifiedDate.longValue(), null);
					UI.setVisibility(editor, View.VISIBLE);
				}
			}

			/* Configure UI */
			UI.setVisibility(findViewById(R.id.button_delete), View.GONE);
			if (entity.ownerId != null
					&& (entity.ownerId.equals(Aircandi.getInstance().getUser().id)
					|| (Aircandi.settings.getBoolean(Constants.PREF_ENABLE_DEV, Constants.PREF_ENABLE_DEV_DEFAULT)
							&& Aircandi.getInstance().getUser().developer != null
							&& Aircandi.getInstance().getUser().developer))) {
				UI.setVisibility(findViewById(R.id.button_delete), View.VISIBLE);
			}
		}
	}

	protected void drawPhoto() {
		if (mPhoto != null) {

			mPhoto.getImageView().clearColorFilter();
			mPhoto.getImageView().setBackgroundResource(0);
			if (findViewById(R.id.color_layer) != null) {
				(findViewById(R.id.color_layer)).setBackgroundResource(0);
				(findViewById(R.id.color_layer)).setVisibility(View.GONE);
				(findViewById(R.id.reverse_layer)).setVisibility(View.GONE);
			}

			if (mEntity.photo != null && mEntity.photo.hasBitmap()) {
				mPhoto.hideLoading();
				UI.showImageInImageView(mEntity.photo.getBitmap(), mPhoto.getImageView(), true, Animate.fadeInMedium());
				mPhoto.setVisibility(View.VISIBLE);
			}
			else {
				final String photoUri = mEntity.getPhotoUri();
				final BitmapRequest bitmapRequest = new BitmapRequest(photoUri, mPhoto.getImageView());
				bitmapRequest.setImageSize(mPhoto.getSizeHint());
				bitmapRequest.setImageRequestor(mPhoto.getImageView());
				mPhoto.getImageView().setTag(photoUri);
				BitmapManager.getInstance().masterFetch(bitmapRequest);
			}
		}
	}

	protected void drawShortcuts(Entity entity) {
		if (findViewById(R.id.applinks) != null) {
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
				shortcuts = (List<Shortcut>) entity.getShortcuts(new ShortcutSettings(Constants.TYPE_LINK_APPLINK, null, Direction.in, false), true);
			}

			Collections.sort(shortcuts, new Shortcut.SortByPosition());

			if (shortcuts.size() == 0) {
				button.getTextView().setVisibility(View.VISIBLE);
				button.getViewGroup().setVisibility(View.GONE);
			}
			else {
				button.getTextView().setVisibility(View.GONE);
				button.getViewGroup().setVisibility(View.VISIBLE);
				button.getViewGroup().removeAllViews();
				final LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				final int sizePixels = UI.getRawPixels(this, 30);
				final int marginPixels = UI.getRawPixels(this, 5);

				/* We only show the first five */
				int shortcutCount = 0;
				for (Shortcut shortcut : shortcuts) {
					if (shortcutCount >= 5) {
						break;
					}
					View view = inflater.inflate(R.layout.temp_radar_candi_item, null);
					WebImageView webImageView = (WebImageView) view.findViewById(R.id.photo);
					webImageView.setSizeHint(sizePixels);

					if (shortcut.photo != null && shortcut.photo.hasBitmap()) {
						UI.showImageInImageView(shortcut.photo.getBitmap(), webImageView.getImageView(), true, Animate.fadeInMedium());
					}
					else {
						String photoUri = shortcut.photo.getUri();
						BitmapRequestBuilder builder = new BitmapRequestBuilder(webImageView).setImageUri(photoUri);
						BitmapRequest imageRequest = builder.create();
						webImageView.setBitmapRequest(imageRequest);
					}

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
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	public void onAccept() {
		if (isDirty()) {
			if (validate()) {

				/* Pull all the control values back into the entity object */
				gather();

				if (mSkipSave) {
					final IntentBuilder intentBuilder = new IntentBuilder().setEntity(mEntity);
					setResult(Constants.RESULT_ENTITY_EDITED, intentBuilder.create());
					finish();
					Animate.doOverridePendingTransition(BaseEntityEdit.this, TransitionType.FormToPage);
				}
				else {
					if (mEditing) {
						update();
					}
					else {
						update();
					}
				}
			}
		}
		else {
			finish();
			Animate.doOverridePendingTransition(BaseEntityEdit.this, TransitionType.FormToPage);
		}
	}

	@SuppressWarnings("ucd")
	public void onChangePhotoButtonClick(View view) {
		gather(); // So picture logic has the latest property values
		IntentBuilder intentBuilder = new IntentBuilder(this, PictureSourcePicker.class).setEntity(mEntity);
		startActivityForResult(intentBuilder.create(), Constants.ACTIVITY_PICTURE_SOURCE_PICK);
		Animate.doOverridePendingTransition(this, TransitionType.PageToForm);

		mImageRequestWebImageView = mPhoto;
		mImageRequestListener = new RequestListener() {

			@Override
			public void onComplete(Object response, Photo photo, String photoUri, Bitmap imageBitmap, String title, String description, Boolean bitmapLocalOnly) {

				final ServiceResponse serviceResponse = (ServiceResponse) response;
				if (serviceResponse.responseCode == ResponseCode.Success) {

					mDirty = true;
					if (photo != null) {
						mEntity.photo = photo;
					}
					else if (photoUri != null) {
						mEntity.photo = new Photo(photoUri, null, null, null, PhotoSource.aircandi);
					}

					if (imageBitmap != null) {
						final String imageKey = mEntity.schema + "_" + mEntity.id + ".jpg";
						mEntity.photo = new Photo(photoUri, null, null, null, PhotoSource.cache);
						mEntity.photo.setBitmap(imageKey, imageBitmap); // Could get set to null if we are using the default 
						mEntity.photo.setBitmapLocalOnly(bitmapLocalOnly);
					}

					drawPhoto();
				}
			}
		};
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
					final String pictureSource = extras.getString(Constants.EXTRA_PICTURE_SOURCE);
					if (pictureSource != null && !pictureSource.equals("")) {

						if (pictureSource.equals(Constants.PHOTO_SOURCE_SEARCH)) {
							String defaultSearch = null;
							if (findViewById(R.id.name) != null) {
								defaultSearch = Utilities.emptyAsNull(((TextView) findViewById(R.id.name)).getText().toString().trim());
							}
							pictureSearch(defaultSearch);
						}
						else if (pictureSource.equals(Constants.PHOTO_SOURCE_GALLERY)) {
							pictureFromGallery();
						}
						else if (pictureSource.equals(Constants.PHOTO_SOURCE_CAMERA)) {
							pictureFromCamera();
						}
						else if (pictureSource.equals(Constants.PHOTO_SOURCE_PLACE)) {
							pictureFromPlace(mEntity.id);
						}
						else if (pictureSource.equals(Constants.PHOTO_SOURCE_DEFAULT)) {
							usePictureDefault();
						}
						else if (pictureSource.equals(Constants.PHOTO_SOURCE_FACEBOOK)) {
							mEntity.photo = new Photo("https://graph.facebook.com/" + ((Applink) mEntity).appId + "/picture?type=large", null, null, null,
									PhotoSource.facebook);
							drawPhoto();
						}
						else if (pictureSource.equals(Constants.PHOTO_SOURCE_TWITTER)) {
							mEntity.photo = new Photo(
									"https://api.twitter.com/1/users/profile_image?screen_name=" + ((Applink) mEntity).appId + "&size=bigger",
									null,
									null, null, PhotoSource.twitter);
							drawPhoto();
						}
					}
				}
			}
			else if (requestCode == Constants.ACTIVITY_PICTURE_PICK_DEVICE) {

				Tracker.sendEvent("ui_action", "select_picture_device", null, 0, Aircandi.getInstance().getUser());
				final Uri photoUri = intent.getData();
				Bitmap bitmap = null;

				/* Bitmap size is trimmed if necessary to fit our max in memory image size. */
				bitmap = BitmapManager.getInstance().loadBitmapFromDeviceSampled(photoUri);
				if (bitmap != null && mImageRequestListener != null) {
					mImageRequestListener.onComplete(new ServiceResponse()
							, null
							, null
							, bitmap
							, null
							, null
							, false);
				}
			}
			else if (requestCode == Constants.ACTIVITY_PICTURE_MAKE) {

				Tracker.sendEvent("ui_action", "create_picture_camera", null, 0, Aircandi.getInstance().getUser());
				final Bitmap bitmap = BitmapManager.getInstance().loadBitmapFromDeviceSampled(mMediaFileUri);
				sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, mMediaFileUri));
				if (mImageRequestListener != null) {
					mImageRequestListener.onComplete(new ServiceResponse()
							, null
							, null
							, bitmap
							, null
							, null
							, false);
				}
			}
			else if (requestCode == Constants.ACTIVITY_PICTURE_SEARCH) {

				Tracker.sendEvent("ui_action", "select_picture_search", null, 0, Aircandi.getInstance().getUser());
				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final String imageTitle = extras.getString(Constants.EXTRA_URI_TITLE);
					final String imageDescription = extras.getString(Constants.EXTRA_URI_DESCRIPTION);
					final String jsonPhoto = extras.getString(Constants.EXTRA_PHOTO);
					final Photo photo = (Photo) HttpService.jsonToObject(jsonPhoto, ObjectType.Photo);

					final BitmapRequestBuilder builder = new BitmapRequestBuilder(mImageRequestWebImageView)
							.setFromUri(photo.getUri())
							.setRequestListener(new RequestListener() {

								@Override
								public void onComplete(Object response) {

									final ServiceResponse serviceResponse = (ServiceResponse) response;
									if (serviceResponse.responseCode == ResponseCode.Success) {
										runOnUiThread(new Runnable() {

											@Override
											public void run() {
												if (mImageRequestListener != null) {
													final ImageResponse imageResponse = (ImageResponse) serviceResponse.data;
													/*
													 * We cache search pictures to aircandi storage because it give us
													 * a chance to pre-process them to be a more standardized blob size
													 * and dimensions. We also can serve them up with more predictable
													 * performance.
													 */
													mImageRequestListener.onComplete(serviceResponse
															, photo
															, null
															, imageResponse.bitmap
															, imageTitle
															, imageDescription
															, false);
												}
											}
										});
									}
								}
							});

					final BitmapRequest imageRequest = builder.create();
					mImageRequestWebImageView.setBitmapRequest(imageRequest, false);

					if (imageTitle != null && !imageTitle.equals("")) {
						final EditText title = (EditText) findViewById(R.id.name);
						if (title != null && title.getText().toString().equals("")) {
							title.setText(imageTitle);
						}
					}

					if (imageDescription != null && !imageDescription.equals("")) {
						final EditText description = (EditText) findViewById(R.id.description);
						if (description != null && description.getText().toString().equals("")) {
							description.setText(imageDescription);
						}
					}
				}
			}
			else if (requestCode == Constants.ACTIVITY_PICTURE_PICK_PLACE) {

				Tracker.sendEvent("ui_action", "select_picture_place", null, 0, Aircandi.getInstance().getUser());
				if (intent != null && intent.getExtras() != null) {

					final Bundle extras = intent.getExtras();
					final String jsonPhoto = extras.getString(Constants.EXTRA_PHOTO);
					final Photo photo = (Photo) HttpService.jsonToObject(jsonPhoto, ObjectType.Photo);

					final BitmapRequestBuilder builder = new BitmapRequestBuilder(mImageRequestWebImageView)
							.setFromUri(photo.getUri())
							.setRequestListener(new RequestListener() {

								@Override
								public void onComplete(Object response) {

									final ServiceResponse serviceResponse = (ServiceResponse) response;
									if (serviceResponse.responseCode == ResponseCode.Success) {
										runOnUiThread(new Runnable() {

											@Override
											public void run() {
												if (mImageRequestListener != null) {
													final ImageResponse imageResponse = (ImageResponse) serviceResponse.data;
													/*
													 * We don't cache place pictures from foursquare because they
													 * wouldn't like that. Plus they serve them up in a more consistent
													 * way than
													 * something like search pictures (which we do cache).
													 */
													mImageRequestListener.onComplete(serviceResponse
															, photo
															, imageResponse.photoUri
															, imageResponse.bitmap
															, null
															, null
															, true);
												}
											}
										});
									}
								}
							});

					final BitmapRequest imageRequest = builder.create();
					mImageRequestWebImageView.setBitmapRequest(imageRequest, false);
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

	protected void gather() {
		if (findViewById(R.id.name) != null) {
			mEntity.name = Utilities.emptyAsNull(((TextView) findViewById(R.id.name)).getText().toString().trim());
		}
		if (findViewById(R.id.description) != null) {
			mEntity.description = Utilities.emptyAsNull(((TextView) findViewById(R.id.description)).getText().toString().trim());
		}
		if (findViewById(R.id.chk_locked) != null) {
			mEntity.locked = ((CheckBox) findViewById(R.id.chk_locked)).isChecked();
		}
	}

	protected void setEntityType(String type) {
		mEntity.type = type;
	}

	public static Class<?> editFormBySchema(String schema) {
		if (schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
			return PlaceEdit.class;
		}
		else if (schema.equals(Constants.SCHEMA_ENTITY_POST)) {
			return PostEdit.class;
		}
		else if (schema.equals(Constants.SCHEMA_ENTITY_APPLINK)) {
			return ApplinkEdit.class;
		}
		return null;
	}

	private void loadApplinks() {
		if (mApplinks != null) {
			doApplinksBuilder();
		}
		else {
			new AsyncTask() {

				@Override
				protected void onPreExecute() {
					mBusyManager.showBusy();
				}

				@Override
				protected Object doInBackground(Object... params) {
					Thread.currentThread().setName("BindApplinks");

					Map map = new HashMap<String, Object>();
					map.put("modifiedDate", -1);
					Cursor cursor = new Cursor()
							.setLimit(ProxiConstants.LIMIT_CHILD_ENTITIES)
							.setSort(Maps.asMap("modifiedDate", -1))
							.setSkip(0);

					ModelResult result = EntityManager.getInstance().loadEntitiesForEntity(mEntity.id
							, Constants.TYPE_LINK_APPLINK
							, null
							, cursor);

					return result;
				}

				@Override
				protected void onPostExecute(Object modelResult) {
					final ModelResult result = (ModelResult) modelResult;
					if (result.serviceResponse.responseCode == ResponseCode.Success) {
						mApplinks = (List<Entity>) result.data;
						doApplinksBuilder();
					}
					else {
						Routing.serviceError(BaseEntityEdit.this, result.serviceResponse);
					}
					mBusyManager.hideBusy();
				}

			}.execute();
		}
	}

	public void doApplinksBuilder() {
		final Intent intent = new Intent(this, ApplinkListEdit.class);

		/* Serialize the applinks */
		if (mApplinks.size() > 0) {
			final List<String> applinkStrings = new ArrayList<String>();
			for (Entity applink : mApplinks) {
				applinkStrings.add(HttpService.objectToJson(applink, UseAnnotations.False, ExcludeNulls.True));
			}
			intent.putStringArrayListExtra(Constants.EXTRA_ENTITIES, (ArrayList<String>) applinkStrings);
		}

		intent.putExtra(Constants.EXTRA_ENTITY_SCHEMA, Constants.SCHEMA_ENTITY_APPLINK);

		if (mEntity.id != null) {
			intent.putExtra(Constants.EXTRA_ENTITY_ID, mEntity.id);
		}

		startActivityForResult(intent, Constants.ACTIVITY_APPLINKS_EDIT);
		Animate.doOverridePendingTransition(this, TransitionType.PageToForm);
	}

	protected void usePictureDefault() {
		/*
		 * Setting the photo to null will trigger correct default handling.
		 */
		mDirty = true;
		if (mEntity.photo != null) {
			mEntity.photo.removeBitmap();
			mEntity.photo = null;
		}
		mEntity.photo = mEntity.getDefaultPhoto();
		drawPhoto();
		Tracker.sendEvent("ui_action", "set_" + mEntitySchema + "_photo_to_default", null, 0, Aircandi.getInstance().getUser());
	}

	// --------------------------------------------------------------------------------------------
	// Pickers
	// --------------------------------------------------------------------------------------------

	@SuppressLint("InlinedApi")
	protected void pictureFromGallery() {
		//		Intent picturePickerIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
		//		picturePickerIntent.setType("image/*");
		//		startActivityForResult(picturePickerIntent, CandiConstants.ACTIVITY_PICTURE_PICK_DEVICE);

		final Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		/*
		 * We want to filter out remove images like the linked in from picasa.
		 */
		if (Constants.SUPPORTS_HONEYCOMB) {
			intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
		}
		startActivityForResult(Intent.createChooser(intent, getString(R.string.chooser_gallery_title))
				, Constants.ACTIVITY_PICTURE_PICK_DEVICE);
		Animate.doOverridePendingTransition(this, TransitionType.PageToSource);
	}

	protected void pictureFromCamera() {
		final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		mMediaFile = AndroidManager.getOutputMediaFile(AndroidManager.MEDIA_TYPE_IMAGE);
		if (mMediaFile != null) {
			mMediaFilePath = mMediaFile.getAbsolutePath();
			mMediaFileUri = Uri.fromFile(mMediaFile);
			intent.putExtra(MediaStore.EXTRA_OUTPUT, mMediaFileUri);
			startActivityForResult(intent, Constants.ACTIVITY_PICTURE_MAKE);
			Animate.doOverridePendingTransition(this, TransitionType.PageToSource);
		}
	}

	protected void pictureSearch(String defaultSearch) {
		final Intent intent = new Intent(this, PicturePicker.class);
		intent.putExtra(Constants.EXTRA_SEARCH_PHRASE, defaultSearch);
		startActivityForResult(intent, Constants.ACTIVITY_PICTURE_SEARCH);
		Animate.doOverridePendingTransition(this, TransitionType.PageToForm);
	}

	protected void pictureFromPlace(String entityId) {
		final IntentBuilder intentBuilder = new IntentBuilder(this, PicturePicker.class)
				.setEntityId(entityId);
		final Intent intent = intentBuilder.create();
		startActivityForResult(intent, Constants.ACTIVITY_PICTURE_PICK_PLACE);
		Animate.doOverridePendingTransition(this, TransitionType.PageToForm);
	}

	// --------------------------------------------------------------------------------------------
	// Services
	// --------------------------------------------------------------------------------------------

	@Override
	protected void insert() {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusyManager.showBusy(R.string.progress_saving);
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
					link = new Link(mParentId, mEntity.schema, true);
				}
				else {
					mEntity.toId = null;
				}

				/* We always send beacons to support nearby notifications */
				beacons = ProximityManager.getInstance().getStrongestBeacons(ProxiConstants.PROXIMITY_BEACON_COVERAGE);
				primaryBeacon = (beacons.size() > 0) ? beacons.get(0) : null;

				Tracker.sendEvent("ui_action", "entity_insert", mEntity.type, 0, Aircandi.getInstance().getUser());

				Bitmap bitmap = null;
				if (mEntity.photo != null && mEntity.photo.hasBitmap() && !mEntity.photo.isBitmapLocalOnly()) {
					bitmap = mEntity.photo.getBitmap();
				}

				/* In case a derived class needs to augment the entity before insert */
				beforeInsert(mEntity);

				result = EntityManager.getInstance().insertEntity(mEntity, link, beacons, primaryBeacon, bitmap);

				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					Entity insertedEntity = (Entity) result.data;

					if (mApplinks != null) {
						result = EntityManager.getInstance().replaceEntitiesForEntity(insertedEntity.id, mApplinks, Constants.TYPE_LINK_APPLINK);
					}

					UI.showToastNotification(getString(R.string.alert_inserted), Toast.LENGTH_SHORT);
					final IntentBuilder intentBuilder = new IntentBuilder().setEntityId(insertedEntity.id);
					setResult(Constants.RESULT_ENTITY_INSERTED, intentBuilder.create());
				}
				return result.serviceResponse;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ServiceResponse serviceResponse = (ServiceResponse) response;
				mBusyManager.hideBusy();
				if (serviceResponse.responseCode == ResponseCode.Success) {
					finish();
					Animate.doOverridePendingTransition(BaseEntityEdit.this, TransitionType.FormToPage);
				}
				else {
					Routing.serviceError(BaseEntityEdit.this, serviceResponse);
				}
			}

		}.execute();
	}

	@Override
	protected void update() {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusyManager.showBusy(R.string.progress_saving);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("InsertUpdateEntity");
				ModelResult result = new ModelResult();

				/* Save applinks first */
				if (mApplinks != null) {
					result = EntityManager.getInstance().replaceEntitiesForEntity(mEntity.id, mApplinks, Constants.TYPE_LINK_APPLINK);
				}

				/* Update entity */
				if (result.serviceResponse.responseCode == ResponseCode.Success) {

					Tracker.sendEvent("ui_action", "entity_update", mEntity.type, 0, Aircandi.getInstance().getUser());
					Bitmap bitmap = null;
					if (mEntity.photo != null && mEntity.photo.hasBitmap() && !mEntity.photo.isBitmapLocalOnly()) {
						bitmap = mEntity.photo.getBitmap();
					}

					/* Something in the call caused us to lose the most recent picture. */
					result = EntityManager.getInstance().updateEntity(mEntity, bitmap);

					if (result.serviceResponse.responseCode == ResponseCode.Success) {
						UI.showToastNotification(getString(R.string.alert_updated), Toast.LENGTH_SHORT);
						setResult(Constants.RESULT_ENTITY_UPDATED);
					}
				}
				return result.serviceResponse;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ServiceResponse serviceResponse = (ServiceResponse) response;
				mBusyManager.hideBusy();
				if (serviceResponse.responseCode == ResponseCode.Success) {
					finish();
					Animate.doOverridePendingTransition(BaseEntityEdit.this, TransitionType.FormToPage);
				}
				else {
					Routing.serviceError(BaseEntityEdit.this, serviceResponse);
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
				mBusyManager.showBusy(R.string.progress_deleting);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("DeleteEntity");
				Tracker.sendEvent("ui_action", "entity_delete", mEntity.type, 0, Aircandi.getInstance().getUser());
				final ModelResult result = EntityManager.getInstance().deleteEntity(mEntity.id, false);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					Logger.i(this, "Deleted entity: " + mEntity.name);

					mBusyManager.hideBusy();
					UI.showToastNotification(getString(R.string.alert_deleted), Toast.LENGTH_SHORT);
					setResult(Constants.RESULT_ENTITY_DELETED);
					/*
					 * We either go back to a list or to radar.
					 */
					finish();
					Animate.doOverridePendingTransition(BaseEntityEdit.this, TransitionType.FormToPageAfterDelete);
				}
				else {
					Routing.serviceError(BaseEntityEdit.this, result.serviceResponse);
				}
			}

		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		/* Cache edit and delete menus because we need to toggle it later */
		mMenuItemDelete = menu.findItem(R.id.delete);

		if (mEntity != null) {

			MenuItem menuItem = menu.findItem(R.id.delete);
			if (menuItem != null) {
				menuItem.setVisible(false);
				if (mEntity.ownerId != null && Aircandi.getInstance().getUser() != null
						&& (mEntity.ownerId.equals(Aircandi.getInstance().getUser().id)
						|| (Aircandi.settings.getBoolean(Constants.PREF_ENABLE_DEV, Constants.PREF_ENABLE_DEV_DEFAULT)
								&& Aircandi.getInstance().getUser().developer != null
								&& Aircandi.getInstance().getUser().developer))) {
					menuItem.setVisible(true);
				}
			}
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		return Routing.route(this, Routing.routeForMenu(menuItem), mEntity);
	}

}