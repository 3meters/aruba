package com.aircandi.ui.builders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.beta.R;
import com.aircandi.components.AircandiCommon;
import com.aircandi.components.FontManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.Tracker;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequestBuilder;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.RequestListener;
import com.aircandi.service.HttpService.ServiceDataType;
import com.aircandi.service.objects.Applink;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Photo.PhotoSource;
import com.aircandi.ui.base.FormActivity;
import com.aircandi.ui.widgets.WebImageView;
import com.aircandi.utilities.MiscUtils;

@SuppressWarnings("ucd")
public class ApplinkBuilder extends FormActivity {

	private Applink			mEntity;
	private Boolean			mEditing				= false;
	private WebImageView	mPhoto;
	private Spinner			mTypePicker;
	private EditText		mName;
	private EditText		mAppId;
	private EditText		mAppUrl;
	private Integer			mSpinnerItem;

	private List<String>	mApplinkSuggestionStrings;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!isFinishing()) {
			initialize();
			bind();
		}
	}

	private void initialize() {
		mCommon.mActionBar.setDisplayHomeAsUpEnabled(true);
		final Bundle extras = this.getIntent().getExtras();
		if (extras != null) {
			final String jsonApplink = extras.getString(Constants.EXTRA_APPLINK);
			if (jsonApplink != null) {
				mEntity = (Applink) HttpService.convertJsonToObjectInternalSmart(jsonApplink, ServiceDataType.Applink);
				mEditing = true;
				mCommon.mActionBar.setTitle(mEntity.name);
			}
			else {
				mEditing = false;
				mCommon.mActionBar.setTitle(R.string.dialog_source_builder_title_new);
			}
		}
		mPhoto = (WebImageView) findViewById(R.id.photo);
		mTypePicker = (Spinner) findViewById(R.id.type_picker);
		mName = (EditText) findViewById(R.id.name);
		mAppId = (EditText) findViewById(R.id.app_id);
		mAppUrl = (EditText) findViewById(R.id.app_url);

		mSpinnerItem = mCommon.mThemeTone.equals("dark") ? R.layout.spinner_item_dark : R.layout.spinner_item_light;

		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.title));
		FontManager.getInstance().setTypefaceDefault((EditText) findViewById(R.id.name));
		FontManager.getInstance().setTypefaceDefault((EditText) findViewById(R.id.app_id));
		FontManager.getInstance().setTypefaceDefault((EditText) findViewById(R.id.app_url));
	}

	private void bind() {
		if (mEditing) {
			mName.setText(mEntity.name);
			mAppId.setText(mEntity.appId);
			mAppUrl.setText(mEntity.appUrl);
			drawPhoto();
		}
		else {
			mTypePicker.setVisibility(View.VISIBLE);
			mApplinkSuggestionStrings = new ArrayList<String>();
			mApplinkSuggestionStrings.add("website");
			mApplinkSuggestionStrings.add("facebook");
			mApplinkSuggestionStrings.add("twitter");
			mApplinkSuggestionStrings.add("email");
			mApplinkSuggestionStrings.add(getString(R.string.form_source_type_hint));
			initializeSpinner(mApplinkSuggestionStrings);
		}
	}

	private Applink buildCustomApplink(String type) {
		final Applink applink = new Applink();

		applink.type = type;
		if (applink.data == null) {
			applink.data = new HashMap<String, Object>();
		}

		applink.data.put("origin", "user");
		applink.photo = new Photo(Applink.getDefaultPhoto(type), null, null, null, PhotoSource.assets);

		return applink;
	}

	private void drawPhoto() {
		final String imageUri = mEntity.getPhotoUri();
		final BitmapRequestBuilder builder = new BitmapRequestBuilder(mPhoto).setImageUri(imageUri);
		final BitmapRequest bitmapRequest = builder.create();
		mPhoto.setBitmapRequest(bitmapRequest);
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("ucd")
	public void onChangePhotoButtonClick(View view) {
		mCommon.showPictureSourcePicker(mEntity.id, mEntity.schema, mEntity.type);
		mImageRequestWebImageView = mPhoto;
		mImageRequestListener = new RequestListener() {

			@Override
			public void onComplete(Object response, Photo photo, String imageUri, Bitmap imageBitmap, String title, String description, Boolean bitmapLocalOnly) {

				final ServiceResponse serviceResponse = (ServiceResponse) response;
				if (serviceResponse.responseCode == ResponseCode.Success) {

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
	public void onSaveButtonClick(View view) {
		if (validate()) {
			gather();
			doSave();
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {

		if (resultCode == Activity.RESULT_OK) {
			if (requestCode == Constants.ACTIVITY_PICTURE_SOURCE_PICK) {
				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final String pictureSource = extras.getString(Constants.EXTRA_PICTURE_SOURCE);
					if (pictureSource != null && !pictureSource.equals("")) {
						
						if (pictureSource.equals(Constants.PHOTO_SOURCE_SEARCH)) {
							String defaultSearch = null;
							if (MiscUtils.emptyAsNull(mName.getText().toString()) != null) {
								defaultSearch = mName.getText().toString().trim();
							}
							pictureSearch(defaultSearch);
						}
						else if (pictureSource.equals(Constants.PHOTO_SOURCE_GALLERY)) {
							pictureFromGallery();
						}
						else if (pictureSource.equals(Constants.PHOTO_SOURCE_CAMERA)) {
							pictureFromCamera();
						}
						else if (pictureSource.equals(Constants.PHOTO_SOURCE_DEFAULT)) {
							usePictureDefault();
						}
						else {
							gather();
							if (mEntity.appId == null || mEntity.appId.equals("")) {
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
									mEntity.photo = new Photo("https://graph.facebook.com/" + mEntity.appId + "/picture?type=large", null, null, null,
											PhotoSource.facebook);
									drawPhoto();
								}
								else if (pictureSource.equals(Constants.PHOTO_SOURCE_TWITTER)) {
									mEntity.photo = new Photo("https://api.twitter.com/1/users/profile_image?screen_name=" + mEntity.appId + "&size=bigger", null,
											null, null, PhotoSource.twitter);
									drawPhoto();
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
		if (mEntity.photo != null) {
			mEntity.photo.setBitmap(null);
			mEntity.photo = null;
		}
		mEntity.photo = mEntity.getDefaultPhoto();
		drawPhoto();
		Tracker.sendEvent("ui_action", "set_source_picture_to_default", null, 0, Aircandi.getInstance().getUser());
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	private void doSave() {
		final Intent intent = new Intent();
		if (mEntity != null) {
			final String jsonSource = HttpService.convertObjectToJsonSmart(mEntity, false, true);
			intent.putExtra(Constants.EXTRA_APPLINK, jsonSource);
		}
		setResult(Activity.RESULT_OK, intent);
		finish();
	}

	private boolean validate() {
		if (mName.getText().length() == 0) {
			AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_missing_source_label)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}

		if (mAppId.getText().length() == 0 && mAppUrl.getText().length() == 0) {
			AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_missing_source_id_and_url)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}

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
		return true;
	}

	private void gather() {
		if (mEditing) {
			mEntity.name = mName.getEditableText().toString();
			mEntity.appId = mAppId.getEditableText().toString();
			mEntity.appUrl = mAppUrl.getEditableText().toString();
		}
		else {
			mEntity.name = mName.getEditableText().toString();
			mEntity.appId = mAppId.getEditableText().toString();
			mEntity.appUrl = mAppUrl.getEditableText().toString();
		}
		if (mEntity.type.equals(Constants.TYPE_APPLINK_WEBSITE)) {
			if (!mEntity.appUrl.startsWith("http://") && !mEntity.appUrl.startsWith("https://")) {
				mEntity.appUrl = "http://" + mEntity.appUrl;
			}
		}
	}

	@Override
	protected Boolean isDialog() {
		return false;
	}

	private void doApplinkTest() {
		gather();
		mCommon.routeShortcut(mEntity.getShortcut(), null);
	}

	// --------------------------------------------------------------------------------------------
	// Application menu routines (settings)
	// --------------------------------------------------------------------------------------------

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.accept) {
			if (validate()) {
				gather();
				doSave();
			}
			return true;
		}

		/* In case we add general menu items later */
		mCommon.doOptionsItemSelected(item);
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

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
						mEntity = buildCustomApplink(sourceType);
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

	public void updateCustomImage(String uri) {
		final BitmapRequestBuilder builder = new BitmapRequestBuilder(mPhoto).setImageUri(uri);
		final BitmapRequest imageRequest = builder.create();
		mPhoto.setBitmapRequest(imageRequest);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.builder_applink;
	}
}