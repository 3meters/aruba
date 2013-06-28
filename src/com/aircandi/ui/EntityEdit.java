package com.aircandi.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
import com.aircandi.components.EntityManager;
import com.aircandi.components.FontManager;
import com.aircandi.components.LocationManager;
import com.aircandi.components.Logger;
import com.aircandi.components.Maps;
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
import com.aircandi.service.objects.Cursor;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Link;
import com.aircandi.service.objects.Link.Direction;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Photo.PhotoSource;
import com.aircandi.service.objects.Place;
import com.aircandi.service.objects.Shortcut;
import com.aircandi.service.objects.User;
import com.aircandi.ui.base.FormActivity;
import com.aircandi.ui.builders.AddressBuilder;
import com.aircandi.ui.builders.ApplinksBuilder;
import com.aircandi.ui.builders.CategoryBuilder;
import com.aircandi.ui.widgets.BuilderButton;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.ui.widgets.WebImageView;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;
import com.aircandi.utilities.ImageUtils;
import com.aircandi.utilities.MiscUtils;

public class EntityEdit extends FormActivity {

	private Entity			mEntity;
	private List<Entity>	mApplinks;
	private Bitmap			mEntityBitmap;
	private Boolean			mEntityBitmapLocalOnly	= false;

	private Boolean			mEditing				= false;
	private Boolean			mDirty					= false;

	private WebImageView	mPhoto;
	private TextView		mTitle;
	private TextView		mDescription;
	private CheckBox		mLocked;
	private Spinner			mTypePicker;
	private EditText		mName;
	private EditText		mAppId;
	private EditText		mAppUrl;
	private Integer			mSpinnerItem;
	private ViewFlipper		mViewFlipper;

	private List<String>	mApplinkSuggestionStrings;

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

		mCommon.mActionBar.setDisplayHomeAsUpEnabled(true);
		if (mCommon.mEntitySchema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
			mCommon.mActionBar.setTitle(R.string.form_title_place);
		}
		else if (mCommon.mEntitySchema.equals(Constants.SCHEMA_ENTITY_POST)) {
			mCommon.mActionBar.setTitle(R.string.form_title_post);
		}
		else if (mCommon.mEntitySchema.equals(Constants.SCHEMA_ENTITY_APPLINK)) {
			mCommon.mActionBar.setTitle(R.string.form_title_applink);
		}

		mTitle = (TextView) findViewById(R.id.text_title);
		mDescription = (TextView) findViewById(R.id.description);
		mPhoto = (WebImageView) findViewById(R.id.photo);
		mLocked = (CheckBox) findViewById(R.id.chk_locked);

		mTypePicker = (Spinner) findViewById(R.id.type_picker);
		mName = (EditText) findViewById(R.id.name);
		mAppId = (EditText) findViewById(R.id.app_id);
		mAppUrl = (EditText) findViewById(R.id.app_url);
		mSpinnerItem = mCommon.mThemeTone.equals("dark") ? R.layout.spinner_item_dark : R.layout.spinner_item_light;

		mViewFlipper = (ViewFlipper) findViewById(R.id.flipper_form);
		mCommon.setViewFlipper(mViewFlipper);

		if (mTitle != null) {
			mTitle.addTextChangedListener(new SimpleTextWatcher() {

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

		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_change_image));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.uri));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.text_title));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.description));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.chk_locked));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.title));
		FontManager.getInstance().setTypefaceDefault((EditText) findViewById(R.id.name));
		FontManager.getInstance().setTypefaceDefault((EditText) findViewById(R.id.app_id));
		FontManager.getInstance().setTypefaceDefault((EditText) findViewById(R.id.app_url));

	}

	private void bind() {
		/*
		 * Fill in the system and default properties for the base entity properties. The activities that subclass this
		 * will set any additional properties beyond the base ones.
		 */
		final Bundle extras = this.getIntent().getExtras();
		if (extras != null) {
			final String jsonEntity = extras.getString(Constants.EXTRA_ENTITY);
			if (jsonEntity != null) {
				mEntity = (Applink) HttpService.convertJsonToObjectInternalSmart(jsonEntity, ServiceDataType.Entity);
				mEditing = true;
				mCommon.mActionBar.setTitle(mEntity.name);
			}
			else {
				mEntity = Entity.makeEntity(mCommon.mEntitySchema);
				mEditing = false;
				mCommon.mActionBar.setTitle(R.string.dialog_source_builder_title_new);
			}
		}
	}

	private void draw() {

		if (mEntity != null) {

			final Entity entity = mEntity;

			/* Spinners */

			if (!mEditing && mEntity.schema.equals(Constants.SCHEMA_ENTITY_APPLINK)) {
				mTypePicker.setVisibility(View.VISIBLE);
				mApplinkSuggestionStrings = new ArrayList<String>();
				mApplinkSuggestionStrings.add("website");
				mApplinkSuggestionStrings.add("facebook");
				mApplinkSuggestionStrings.add("twitter");
				mApplinkSuggestionStrings.add("email");
				mApplinkSuggestionStrings.add(getString(R.string.form_source_type_hint));
				initializeSpinner(mApplinkSuggestionStrings);
			}

			/* Color */

			mMuteColor = android.os.Build.MODEL.toLowerCase(Locale.US).equals("nexus s"); // nexus 4, nexus 7 are others		

			/* Content */

			drawPhoto();

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

				drawShortcuts(place);

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

	private void drawPhoto() {
		if (mPhoto != null) {

			mPhoto.getImageView().clearColorFilter();
			mPhoto.getImageView().setBackgroundResource(0);
			if (findViewById(R.id.color_layer) != null) {
				(findViewById(R.id.color_layer)).setBackgroundResource(0);
				(findViewById(R.id.color_layer)).setVisibility(View.GONE);
				(findViewById(R.id.reverse_layer)).setVisibility(View.GONE);
			}

			if (mEntityBitmap != null) {
				mPhoto.hideLoading();
				ImageUtils.showImageInImageView(mEntityBitmap, mPhoto.getImageView(), true, AnimUtils.fadeInMedium());
				mPhoto.setVisibility(View.VISIBLE);
			}
			else {
				if (mEntity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
					Place place = (Place) mEntity;
					if (place.photo == null && place.category != null) {

						final int color = Place.getCategoryColor((place.category != null)
								? place.category.name
								: null, true, mMuteColor, false);

						mPhoto.getImageView().setColorFilter(color, PorterDuff.Mode.MULTIPLY);
						mColorResId = Place.getCategoryColorResId(place.category != null ? place.category.name : null,
								true, mMuteColor, false);

						if (findViewById(R.id.color_layer) != null) {
							(findViewById(R.id.color_layer)).setBackgroundResource(mColorResId);
							(findViewById(R.id.color_layer)).setVisibility(View.VISIBLE);
							(findViewById(R.id.reverse_layer)).setVisibility(View.VISIBLE);
						}
						else {
							mPhoto.getImageView().setBackgroundResource(mColorResId);
						}
					}
				}

				final String imageUri = mEntity.getPhotoUri();
				final BitmapRequest bitmapRequest = new BitmapRequest(imageUri, mPhoto.getImageView());
				bitmapRequest.setImageSize(mPhoto.getSizeHint());
				bitmapRequest.setImageRequestor(mPhoto.getImageView());
				mPhoto.getImageView().setTag(imageUri);
				BitmapManager.getInstance().masterFetch(bitmapRequest);
			}
		}
	}

	private void drawShortcuts(Entity entity) {
		if (findViewById(R.id.sources) != null) {
			/*
			 * We are expecting a builder button with a viewgroup to
			 * hold a set of images.
			 */
			final BuilderButton button = (BuilderButton) findViewById(R.id.sources);

			List<Shortcut> shortcuts = null;

			if (mApplinks != null) {
				shortcuts = new ArrayList<Shortcut>();
				for (Entity applink : mApplinks) {
					Shortcut shortcut = ((Applink) applink).getShortcut();
					shortcuts.add(shortcut);
				}
			}
			else {
				shortcuts = (List<Shortcut>) entity.getShortcuts(Constants.TYPE_LINK_APPLINK, Direction.in, false, true);
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
				final int sizePixels = ImageUtils.getRawPixels(this, 30);
				final int marginPixels = ImageUtils.getRawPixels(this, 5);

				/* We only show the first five */
				int shortcutCount = 0;
				for (Shortcut shortcut : shortcuts) {
					if (shortcutCount >= 5) {
						break;
					}
					View view = inflater.inflate(R.layout.temp_radar_candi_item, null);
					WebImageView webImageView = (WebImageView) view.findViewById(R.id.photo);
					webImageView.setSizeHint(sizePixels);

					String imageUri = shortcut.photo.getUri();
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
					shortcutCount++;
				}
			}
		}
	}

	private void initializeSpinner(final List<String> items) {

		final ArrayAdapter adapter = new ArrayAdapter(this, mSpinnerItem, items) {

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {

				final View view = super.getView(position, convertView, parent);

				final TextView text = (TextView) view.findViewById(R.id.spinner_name);
				if (mCommon.mThemeTone.equals("dark")) {
					if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
						text.setTextColor(Aircandi.getInstance().getResources().getColor(R.color.text_dark));
					}
				}

				FontManager.getInstance().setTypefaceDefault((TextView) view.findViewById(R.id.spinner_name));

				if (position == getCount()) {
					((TextView) view.findViewById(R.id.spinner_name)).setText("");
					((TextView) view.findViewById(R.id.spinner_name)).setHint(items.get(getCount())); //"Hint to be displayed"
				}

				return view;
			}

			@Override
			public int getCount() {
				return super.getCount() - 1; // dont display last item. It is used as hint.
			}
		};

		if (mCommon.mThemeTone.equals("dark")) {
			if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
				adapter.setDropDownViewResource(R.layout.spinner_item_light);
			}
		}

		mTypePicker.setAdapter(adapter);

		if (!mEditing) {
			mTypePicker.setSelection(adapter.getCount());
		}

		mTypePicker.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

				if (mCommon.mThemeTone.equals("dark")) {
					if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
						((TextView) parent.getChildAt(0)).setTextColor(getResources().getColor(R.color.text_light));
					}
				}

				/* Do nothing when the hint item is selected */
				if (position != parent.getCount()) {
					if (position < mApplinkSuggestionStrings.size()) {
						final String sourceType = mApplinkSuggestionStrings.get(position);
						setEntityType(sourceType);
						mName.setText(mEntity.name);
						mAppId.setText(mEntity.id);
						if (mEntity.type.equals("website")) {
							mAppId.setVisibility(View.GONE);
						}
						else {
							mAppId.setVisibility(View.VISIBLE);
						}
						drawPhoto();
					}
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {}

		});
	}

	private void setEntityType(String type) {
		mEntity.type = type;
		if (mEntity.schema.equals(Constants.SCHEMA_ENTITY_APPLINK)) {
			if (mEntity.data == null) {
				mEntity.data = new HashMap<String, Object>();
			}

			mEntity.data.put("origin", "user");
			mEntity.photo = new Photo(Applink.getDefaultPhoto(type), null, null, null, PhotoSource.assets);
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
			AnimUtils.doOverridePendingTransition(EntityEdit.this, TransitionType.FormToPage);
		}
	}

	@SuppressWarnings("ucd")
	public void onChangePictureButtonClick(View view) {
		mCommon.showPictureSourcePicker(mEntity.id, mEntity.schema, mEntity.type);
		mImageRequestWebImageView = mPhoto;
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
						mEntity.photo = photo;
					}
					else if (imageUri != null) {
						mEntity.photo = new Photo(imageUri, null, null, null, PhotoSource.aircandi);
					}
					drawPhoto();
				}
			}
		};
	}

	@SuppressWarnings("ucd")
	public void onTestButtonClick(View view) {
		doApplinkTest();
	}

	@SuppressWarnings("ucd")
	public void onAddressBuilderClick(View view) {
		final Intent intent = new Intent(this, AddressBuilder.class);
		final String jsonPlace = HttpService.convertObjectToJsonSmart(mEntity, false, true);
		intent.putExtra(Constants.EXTRA_PLACE, jsonPlace);
		startActivityForResult(intent, Constants.ACTIVITY_ADDRESS_EDIT);
		AnimUtils.doOverridePendingTransition(this, TransitionType.PageToForm);
	}

	@SuppressWarnings("ucd")
	public void onCategoryBuilderClick(View view) {
		final Intent intent = new Intent(this, CategoryBuilder.class);
		if (((Place) mEntity).category != null) {
			final String jsonCategory = HttpService.convertObjectToJsonSmart(((Place) mEntity).category, false, true);
			intent.putExtra(Constants.EXTRA_CATEGORY, jsonCategory);
		}
		startActivityForResult(intent, Constants.ACTIVITY_CATEGORY_EDIT);
		AnimUtils.doOverridePendingTransition(this, TransitionType.PageToForm);
	}

	@SuppressWarnings("ucd")
	public void onApplinksBuilderClick(View view) {
		loadApplinks();
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
						mEntity = placeUpdated;
						((BuilderButton) findViewById(R.id.address)).setText(((Place) mEntity).address);
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
							((Place) mEntity).category = categoryUpdated;
							((BuilderButton) findViewById(R.id.category)).setText(categoryUpdated.name);
							drawPhoto();
						}
					}
				}
			}
			else if (requestCode == Constants.ACTIVITY_APPLINKS_EDIT) {

				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final List<String> jsonApplinks = extras.getStringArrayList(Constants.EXTRA_APPLINKS);
					mApplinks.clear();
					for (String jsonApplink : jsonApplinks) {
						Applink applink = (Applink) HttpService.convertJsonToObjectInternalSmart(jsonApplink, ServiceDataType.Applink);
						mApplinks.add(applink);
					}
					mDirty = true;
					drawShortcuts(mEntity);
				}
			}
			else if (requestCode == Constants.ACTIVITY_PICTURE_SOURCE_PICK) {
				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final String pictureSource = extras.getString(Constants.EXTRA_PICTURE_SOURCE);
					if (pictureSource != null && !pictureSource.equals("")) {

						if (pictureSource.equals(Constants.PHOTO_SOURCE_SEARCH)) {
							String defaultSearch = null;
							if (findViewById(R.id.text_title) != null) {
								defaultSearch = MiscUtils.emptyAsNull(((TextView) findViewById(R.id.text_title)).getText().toString().trim());
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
						else {
							if (mEntity.schema.equals(Constants.SCHEMA_ENTITY_APPLINK)) {
								Applink applink = (Applink) mEntity;
								gather();
								if (applink.appId == null || applink.appId.equals("")) {
									AircandiCommon.showAlertDialog(
											android.R.drawable.ic_dialog_alert
											,
											null
											,
											getResources().getString(
													pictureSource.equals("facebook") ? R.string.error_missing_source_id_facebook
															: R.string.error_missing_source_id_twitter)
											, null
											, this
											, android.R.string.ok
											, null, null, null, null);
								}
								else {
									if (pictureSource.equals(Constants.PHOTO_SOURCE_FACEBOOK)) {
										mEntity.photo = new Photo("https://graph.facebook.com/" + applink.appId + "/picture?type=large", null, null, null,
												PhotoSource.facebook);
										drawPhoto();
									}
									else if (pictureSource.equals(Constants.PHOTO_SOURCE_TWITTER)) {
										mEntity.photo = new Photo(
												"https://api.twitter.com/1/users/profile_image?screen_name=" + applink.appId + "&size=bigger",
												null,
												null, null, PhotoSource.twitter);
										drawPhoto();
									}
								}
							}
						}
					}
				}
			}
			else {
				super.onActivityResult(requestCode, resultCode, intent);
			}
		}
	}

	private void usePictureDefault() {
		/*
		 * Setting the photo to null will trigger correct default handling.
		 */
		mDirty = true;
		if (mEntity.photo != null) {
			mEntity.photo.setBitmap(null);
			mEntity.photo = null;
		}
		mEntityBitmap = null;
		mEntityBitmapLocalOnly = false;
		mEntity.photo = mEntity.getDefaultPhoto();
		drawPhoto();
		Tracker.sendEvent("ui_action", "set_entity_picture_to_default", null, 0, Aircandi.getInstance().getUser());
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	private void gather() {
		if (findViewById(R.id.text_title) != null) {
			mEntity.name = MiscUtils.emptyAsNull(((TextView) findViewById(R.id.text_title)).getText().toString().trim());
		}
		if (findViewById(R.id.description) != null) {
			mEntity.description = MiscUtils.emptyAsNull(((TextView) findViewById(R.id.description)).getText().toString().trim());
		}
		if (findViewById(R.id.chk_locked) != null) {
			mEntity.locked = ((CheckBox) findViewById(R.id.chk_locked)).isChecked();
		}
		if (mAppId != null) {
			((Applink) mEntity).appId = mAppId.getEditableText().toString();
		}
		if (mAppUrl != null) {
			((Applink) mEntity).appUrl = mAppUrl.getEditableText().toString();
			if (mEntity.type.equals(Constants.TYPE_APPLINK_WEBSITE)) {
				String appUrl = ((Applink) mEntity).appUrl;
				if (!appUrl.startsWith("http://") && !appUrl.startsWith("https://")) {
					((Applink) mEntity).appUrl = "http://" + appUrl;
				}
			}
		}
	}

	private void loadApplinks() {
		if (mApplinks != null) {
			doApplinksBuilder();
		}
		else {
			new AsyncTask() {

				@Override
				protected void onPreExecute() {
					mCommon.showBusy(true);
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

					ModelResult result = EntityManager.getInstance().loadEntitiesForEntity(mCommon.mEntityId
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
						mCommon.handleServiceError(result.serviceResponse, ServiceOperation.CommentBrowse);
					}
					mCommon.hideBusy(true);
				}

			}.execute();
		}
	}

	public void doApplinksBuilder() {
		final Intent intent = new Intent(this, ApplinksBuilder.class);

		/* Serialize the applinks */
		if (mApplinks.size() > 0) {
			final List<String> applinkStrings = new ArrayList<String>();
			for (Entity applink : mApplinks) {
				applinkStrings.add(HttpService.convertObjectToJsonSmart(applink, false, true));
			}
			intent.putStringArrayListExtra(Constants.EXTRA_APPLINKS, (ArrayList<String>) applinkStrings);
		}

		if (mEntity.id != null) {
			intent.putExtra(Constants.EXTRA_ENTITY_ID, mEntity.id);
		}

		startActivityForResult(intent, Constants.ACTIVITY_APPLINKS_EDIT);
		AnimUtils.doOverridePendingTransition(this, TransitionType.PageToForm);
	}

	private void doApplinkTest() {
		gather();
		mCommon.routeShortcut(mEntity.getShortcut(), null);
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

				if (mCommon.mEntityId == null) {
					/*
					 * Pull all the control values back into the entity object
					 */
					gather();
					result = insertEntityAtService();

					if (result.serviceResponse.responseCode == ResponseCode.Success) {

						String entityId = ((Entity) result.data).id;
						if (mApplinks != null) {
							result = EntityManager.getInstance().replaceEntitiesForEntity(entityId, mApplinks, Constants.TYPE_LINK_APPLINK);
						}

						/* Return the id of the inserted entity in case the caller can use it */
						final Intent intent = new Intent();
						intent.putExtra(Constants.EXTRA_ENTITY_ID, entityId);
						ImageUtils.showToastNotification(getString(R.string.alert_inserted), Toast.LENGTH_SHORT);
						setResult(Constants.RESULT_ENTITY_INSERTED, intent);
					}
				}
				else {
					/*
					 * Pull all the control values back into the entity object being used to
					 * update the service. Because the entity reference comes from an entity model
					 * collection, that entity gets updated.
					 */
					gather();
					if (mApplinks != null) {
						result = EntityManager.getInstance().replaceEntitiesForEntity(mCommon.mEntityId, mApplinks, Constants.TYPE_LINK_APPLINK);
					}
					if (result.serviceResponse.responseCode == ResponseCode.Success) {
						result = updateEntityAtService();

						if (result.serviceResponse.responseCode == ResponseCode.Success) {
							ImageUtils.showToastNotification(getString(R.string.alert_updated), Toast.LENGTH_SHORT);
							setResult(Constants.RESULT_ENTITY_UPDATED);
						}
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
					AnimUtils.doOverridePendingTransition(EntityEdit.this, TransitionType.FormToPage);
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
				, EntityEdit.this
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
							AnimUtils.doOverridePendingTransition(EntityEdit.this, TransitionType.FormToPage);
						}
					}
				}
				, null);
		dialog.setCanceledOnTouchOutside(false);
	}

	private void confirmDelete() {
		final AlertDialog dialog = AircandiCommon.showAlertDialog(null
				, getResources().getString(R.string.alert_entity_delete_title)
				, mEntity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)
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

	private boolean validate() {
		if (mName != null && mName.getText().length() == 0) {
			AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_missing_source_label)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}

		if (mAppId != null && mAppId.getText().length() == 0 && mAppUrl.getText().length() == 0) {
			AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_missing_source_id_and_url)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}

		if (mAppUrl != null) {
			final String sourceUrl = mAppUrl.getEditableText().toString();
			if (sourceUrl != null && sourceUrl.length() > 0 && !MiscUtils.validWebUri(sourceUrl)) {
				AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert
						, null
						, getResources().getString(R.string.error_weburi_invalid)
						, null
						, this
						, android.R.string.ok
						, null, null, null, null);
				return false;
			}
		}
		return true;
	}

	private ModelResult insertEntityAtService() {
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

		if (mCommon.mParentId != null) {
			mEntity.toId = mCommon.mParentId;
			link = new Link(mCommon.mParentId, mCommon.mEntitySchema, true);
		}
		else {
			mEntity.toId = null;
		}

		/* We always send beacons to support nearby notifications */
		beacons = ProximityManager.getInstance().getStrongestBeacons(ProxiConstants.PROXIMITY_BEACON_COVERAGE);
		primaryBeacon = (beacons.size() > 0) ? beacons.get(0) : null;

		/*
		 * Set location info if this is a place entity
		 */
		if (mEntity.schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
			((Place) mEntity).provider.aircandi = Aircandi.getInstance().getUser().id;
			/*
			 * We add location info as a consistent feature
			 */
			final AirLocation location = LocationManager.getInstance().getAirLocationLocked();
			if (location != null) {
				((Place) mEntity).location.lat = location.lat;
				((Place) mEntity).location.lng = location.lng;
			}
		}

		Tracker.sendEvent("ui_action", "entity_insert", mEntity.type, 0, Aircandi.getInstance().getUser());
		Bitmap bitmap = mEntityBitmap;
		if (mEntityBitmapLocalOnly) bitmap = null;

		result = EntityManager.getInstance().insertEntity(mEntity, link, beacons, primaryBeacon, bitmap);
		return result;
	}

	private ModelResult updateEntityAtService() {

		Tracker.sendEvent("ui_action", "entity_update", mEntity.type, 0, Aircandi.getInstance().getUser());
		Bitmap bitmap = mEntityBitmap;
		if (mEntityBitmapLocalOnly) {
			bitmap = null;
		}

		/* Something in the call caused us to lose the most recent picture. */
		ModelResult result = EntityManager.getInstance().updateEntity(mEntity, bitmap);
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
				Tracker.sendEvent("ui_action", "entity_delete", mEntity.type, 0, Aircandi.getInstance().getUser());
				final ModelResult result = EntityManager.getInstance().deleteEntity(mEntity.id, false);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					Logger.i(this, "Deleted entity: " + mEntity.name);

					mCommon.hideBusy(true);
					ImageUtils.showToastNotification(getString(R.string.alert_deleted), Toast.LENGTH_SHORT);
					setResult(Constants.RESULT_ENTITY_DELETED);
					/*
					 * We either go back to a list or to radar.
					 */
					finish();
					AnimUtils.doOverridePendingTransition(EntityEdit.this, TransitionType.FormToPageAfterDelete);
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
		if (mEntity != null) {

			MenuItem menuItem = menu.findItem(R.id.delete);
			menuItem.setVisible(false);
			if (mEntity.ownerId != null && Aircandi.getInstance().getUser() != null
					&& (mEntity.ownerId.equals(Aircandi.getInstance().getUser().id)
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
				if (validate()) {
					doSave();
				}
			}
			else {
				finish();
				AnimUtils.doOverridePendingTransition(EntityEdit.this, TransitionType.FormToPage);
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
				AnimUtils.doOverridePendingTransition(EntityEdit.this, TransitionType.FormToPage);
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
		else if (mCommon.mEntitySchema.equals(Constants.SCHEMA_ENTITY_APPLINK)) {
			return R.layout.applink_form;
		}
		else {
			return 0;
		}
	}
}