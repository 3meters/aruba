package com.aircandi.ui.builders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
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
import com.aircandi.CandiConstants;
import com.aircandi.beta.R;
import com.aircandi.components.AircandiCommon;
import com.aircandi.components.AndroidManager;
import com.aircandi.components.FontManager;
import com.aircandi.components.Tracker;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequestBuilder;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.ServiceDataType;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Photo.PhotoSource;
import com.aircandi.service.objects.Source;
import com.aircandi.ui.base.FormActivity;
import com.aircandi.ui.widgets.WebImageView;
import com.aircandi.utilities.MiscUtils;

@SuppressWarnings("ucd")
public class SourceBuilder extends FormActivity {

	private Source			mSource;
	private Boolean			mEditing	= false;

	private WebImageView	mSourceIcon;
	private Spinner			mSourceTypePicker;
	private EditText		mSourceLabel;
	private EditText		mSourceId;
	private EditText		mSourceUrl;
	private Integer			mSpinnerItem;

	private List<String>	mSourceSuggestionStrings;

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
			final String jsonSource = extras.getString(CandiConstants.EXTRA_SOURCE);
			if (jsonSource != null) {
				mSource = (Source) HttpService.convertJsonToObjectInternalSmart(jsonSource, ServiceDataType.Source);
				mEditing = true;
				mCommon.mActionBar.setTitle(mSource.name);
			}
			else {
				mEditing = false;
				mCommon.mActionBar.setTitle(R.string.dialog_source_builder_title_new);
			}
		}
		mSourceIcon = (WebImageView) findViewById(R.id.image);
		mSourceTypePicker = (Spinner) findViewById(R.id.source_type_picker);
		mSourceLabel = (EditText) findViewById(R.id.source_label);
		mSourceId = (EditText) findViewById(R.id.source_id);
		mSourceUrl = (EditText) findViewById(R.id.source_url);

		mSpinnerItem = mCommon.mThemeTone.equals("dark") ? R.layout.spinner_item_dark : R.layout.spinner_item_light;

		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.title));
		FontManager.getInstance().setTypefaceDefault((EditText) findViewById(R.id.source_label));
		FontManager.getInstance().setTypefaceDefault((EditText) findViewById(R.id.source_id));
		FontManager.getInstance().setTypefaceDefault((EditText) findViewById(R.id.source_url));
	}

	private void bind() {
		if (mEditing) {
			mSourceLabel.setText(mSource.label);
			mSourceId.setText(mSource.id);
			mSourceUrl.setText(mSource.url);
			drawSourceIcon();
		}
		else {
			mSourceTypePicker.setVisibility(View.VISIBLE);
			mSourceSuggestionStrings = new ArrayList<String>();
			mSourceSuggestionStrings.add("website");
			mSourceSuggestionStrings.add("facebook");
			mSourceSuggestionStrings.add("twitter");
			mSourceSuggestionStrings.add("email");
			mSourceSuggestionStrings.add(getString(R.string.form_source_type_hint));
			initializeSpinner(mSourceSuggestionStrings);
		}
	}

	private Source buildCustomSource(String sourceType) {
		final Source source = new Source();

		source.type = sourceType;
		if (source.data == null) {
			source.data = new HashMap<String, Object>();
		}

		source.data.put("origin", "user");
		source.photo = new Photo(Source.getDefaultIcon(sourceType), null, null, null, PhotoSource.assets);
		source.packageName = Source.getPackageName(sourceType);

		return source;
	}

	private void drawSourceIcon() {
		final String imageUri = mSource.getImageUri();
		final BitmapRequestBuilder builder = new BitmapRequestBuilder(mSourceIcon).setImageUri(imageUri);
		final BitmapRequest bitmapRequest = builder.create();
		mSourceIcon.setBitmapRequest(bitmapRequest);
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("ucd")
	public void onChangePictureButtonClick(View view) {
		mCommon.showPictureSourcePicker(null, mSource.type);
	}

	@SuppressWarnings("ucd")
	public void onTestButtonClick(View view) {
		doSourceTest();
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
			if (requestCode == CandiConstants.ACTIVITY_PICTURE_SOURCE_PICK) {
				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final String pictureSource = extras.getString(CandiConstants.EXTRA_PICTURE_SOURCE);
					if (pictureSource != null && !pictureSource.equals("")) {
						if (pictureSource.equals("default")) {
							usePictureDefault();
						}
						else {
							gather();
							if (mSource.id == null || mSource.id.equals("")) {
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
								if (pictureSource.equals("facebook")) {
									mSource.photo = new Photo("https://graph.facebook.com/" + mSource.id + "/picture?type=large", null, null, null,
											PhotoSource.facebook);
									drawSourceIcon();
								}
								else if (pictureSource.equals("twitter")) {
									mSource.photo = new Photo("https://api.twitter.com/1/users/profile_image?screen_name=" + mSource.id + "&size=bigger", null,
											null, null, PhotoSource.twitter);
									drawSourceIcon();
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
		mSource.photo = new Photo(Source.getDefaultIcon(mSource.type), null, null, null, PhotoSource.assets);
		drawSourceIcon();
		Tracker.sendEvent("ui_action", "set_source_picture_to_default", null, 0, Aircandi.getInstance().getUser());
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	private void doSave() {
		final Intent intent = new Intent();
		if (mSource != null) {
			final String jsonSource = HttpService.convertObjectToJsonSmart(mSource, false, true);
			intent.putExtra(CandiConstants.EXTRA_SOURCE, jsonSource);
		}
		setResult(Activity.RESULT_OK, intent);
		finish();
	}

	private boolean validate() {
		if (mSourceLabel.getText().length() == 0) {
			AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_missing_source_label)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}

		if (mSourceId.getText().length() == 0 && mSourceUrl.getText().length() == 0) {
			AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_missing_source_id_and_url)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}

		final String sourceUrl = mSourceUrl.getEditableText().toString();
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
			mSource.label = mSourceLabel.getEditableText().toString();
			mSource.id = mSourceId.getEditableText().toString();
			mSource.url = mSourceUrl.getEditableText().toString();
		}
		else {
			mSource.label = mSourceLabel.getEditableText().toString();
			mSource.id = mSourceId.getEditableText().toString();
			mSource.url = mSourceUrl.getEditableText().toString();
		}
		if (mSource.type.equals("website")) {
			if (!mSource.url.startsWith("http://") && !mSource.url.startsWith("https://")) {
				mSource.url = "http://" + mSource.url;
			}
		}
	}

	@Override
	protected Boolean isDialog() {
		return false;
	}

	private void doSourceTest() {
		gather();
		if (mSource.type.equals("twitter")) {
			AndroidManager.getInstance().callTwitterActivity(this, (mSource.id != null) ? mSource.id : mSource.url);
		}
		else if (mSource.type.equals("foursquare")) {
			AndroidManager.getInstance().callFoursquareActivity(this, (mSource.id != null) ? mSource.id : mSource.url);
		}
		else if (mSource.type.equals("facebook")) {
			AndroidManager.getInstance().callFacebookActivity(this, (mSource.id != null) ? mSource.id : mSource.url);
		}
		else if (mSource.type.equals("yelp")) {
			AndroidManager.getInstance().callYelpActivity(this, mSource.id, mSource.url);
		}
		else if (mSource.type.equals("opentable")) {
			AndroidManager.getInstance().callOpentableActivity(this, mSource.id, mSource.url);
		}
		else if (mSource.type.equals("website")) {
			AndroidManager.getInstance().callBrowserActivity(this, (mSource.url != null) ? mSource.url : mSource.id);
		}
		else if (mSource.type.equals("email")) {
			AndroidManager.getInstance().callSendToActivity(this, mSource.label, mSource.id, null, null);
		}
		else {
			AndroidManager.getInstance().callGenericActivity(this, (mSource.url != null) ? mSource.url : mSource.id);
		}
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

		mSourceTypePicker.setAdapter(adapter);

		if (!mEditing) {
			mSourceTypePicker.setSelection(adapter.getCount());
		}

		mSourceTypePicker.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

				if (mCommon.mThemeTone.equals("dark")) {
					if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
						((TextView) parent.getChildAt(0)).setTextColor(getResources().getColor(R.color.text_light));
					}
				}

				/* Do nothing when the hint item is selected */
				if (position != parent.getCount()) {
					if (position < mSourceSuggestionStrings.size()) {
						final String sourceType = mSourceSuggestionStrings.get(position);
						mSource = buildCustomSource(sourceType);
						mSourceLabel.setText(mSource.label);
						mSourceId.setText(mSource.id);
						if (mSource.type.equals("website")) {
							mSourceId.setVisibility(View.GONE);
						}
						else {
							mSourceId.setVisibility(View.VISIBLE);
						}
						mSource.custom = true;
						drawSourceIcon();
					}
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {}

		});
	}

	public void updateCustomImage(String uri) {
		final BitmapRequestBuilder builder = new BitmapRequestBuilder(mSourceIcon).setImageUri(uri);
		final BitmapRequest imageRequest = builder.create();
		mSourceIcon.setBitmapRequest(imageRequest);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.builder_source;
	}
}