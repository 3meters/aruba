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

import com.aircandi.Aircandi;
import com.aircandi.CandiConstants;
import com.aircandi.ProxiConstants;
import com.aircandi.beta.R;
import com.aircandi.components.AircandiCommon;
import com.aircandi.components.FontManager;
import com.aircandi.components.ProxiManager;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequestBuilder;
import com.aircandi.service.ProxibaseService;
import com.aircandi.service.ProxibaseService.ServiceDataType;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Source;
import com.aircandi.ui.base.FormActivity;
import com.aircandi.ui.widgets.WebImageView;
import com.aircandi.utilities.MiscUtils;

@SuppressWarnings("ucd")
public class SourceBuilder extends FormActivity {

	private Source			mSource;
	private Boolean			mEditing	= false;

	private TextView		mTitle;
	private WebImageView	mSourceIcon;
	private Spinner		mSourceTypePicker;
	private EditText		mSourceCaption;
	private EditText		mSourceId;
	private Integer			mSpinnerItem;

	private List<String>	mSourceSuggestionStrings;
	private Entity			mEntity;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!isFinishing()) {
			initialize();
			bind();
		}
	}

	private void initialize() {
		mTitle = (TextView) findViewById(R.id.title);
		final Bundle extras = this.getIntent().getExtras();
		if (extras != null) {
			final String jsonSource = extras.getString(CandiConstants.EXTRA_SOURCE);
			if (jsonSource != null) {
				mSource = (Source) ProxibaseService.convertJsonToObjectInternalSmart(jsonSource, ServiceDataType.Source);
				mEditing = true;
				mTitle.setText(R.string.dialog_source_builder_title_editing);
			}
			final String entityId = extras.getString(CandiConstants.EXTRA_ENTITY_ID);
			if (entityId != null) {
				mEntity = ProxiManager.getInstance().getEntityModel().getCacheEntity(entityId);
				mEditing = false;
				mTitle.setText(R.string.dialog_source_builder_title_new);
			}
		}
		mSourceIcon = (WebImageView) findViewById(R.id.image);
		mSourceTypePicker = (Spinner) findViewById(R.id.source_type_picker);
		mSourceCaption = (EditText) findViewById(R.id.caption);
		mSourceId = (EditText) findViewById(R.id.id);

		mSpinnerItem = mCommon.mThemeTone.equals("dark") ? R.layout.spinner_item_dark : R.layout.spinner_item_light;

		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.title));
		FontManager.getInstance().setTypefaceDefault((EditText) findViewById(R.id.caption));
		FontManager.getInstance().setTypefaceDefault((EditText) findViewById(R.id.id));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_save));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_cancel));
	}

	private void bind() {
		if (mEditing) {
			mSourceCaption.setText(mSource.caption);
			mSourceId.setText(mSource.id);
			drawSourceIcon();
		}
		else {
			mSourceTypePicker.setVisibility(View.VISIBLE);
			mSourceSuggestionStrings = new ArrayList<String>();
			//			if (mEntity != null && mEntity.sourceSuggestions != null) {
			//				for (Source source : mEntity.sourceSuggestions) {
			//					mSourceSuggestionStrings.add(source.type);
			//				}
			//			}
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

		if (sourceType.equals("website")) {
			source.icon = ProxiConstants.PATH_PROXIBASE_SERVICE_ASSETS_SOURCE_ICONS + "website.png";
		}
		else if (sourceType.equals("facebook")) {
			source.icon = ProxiConstants.PATH_PROXIBASE_SERVICE_ASSETS_SOURCE_ICONS + "facebook.png";
			source.packageName = "com.facebook.katana";
		}
		else if (sourceType.equals("twitter")) {
			source.icon = ProxiConstants.PATH_PROXIBASE_SERVICE_ASSETS_SOURCE_ICONS + "twitter.png";
			source.packageName = "com.twitter.android";
		}
		else if (sourceType.equals("email")) {
			source.icon = ProxiConstants.PATH_PROXIBASE_SERVICE_ASSETS_SOURCE_ICONS + "email.png";
		}
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
	public void onSaveButtonClick(View view) {
		if (validate()) {
			gather();
			doSave();
		}
	}

	private void gather() {
		if (mEditing) {
			mSource.caption = mSourceCaption.getEditableText().toString();
			mSource.id = mSourceId.getEditableText().toString();
		}
		else {
			mSource.caption = mSourceCaption.getEditableText().toString();
			mSource.id = mSourceId.getEditableText().toString();
		}
		if (mSource.type.equals("website")) {
			if (!mSource.id.startsWith("http://") && !mSource.id.startsWith("https://")) {
				mSource.id = "http://" + mSource.id;
			}
		}
	}

	@Override
	protected Boolean isDialog() {
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	private void doSave() {
		final Intent intent = new Intent();
		if (mSource != null) {
			final String jsonSource = ProxibaseService.convertObjectToJsonSmart(mSource, false, true);
			intent.putExtra(CandiConstants.EXTRA_SOURCE, jsonSource);
		}
		setResult(Activity.RESULT_OK, intent);
		finish();
	}

	private boolean validate() {
		if (mSourceCaption.getText().length() == 0) {
			AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_missing_source_name)
					, null
					, this
					, android.R.string.ok
					, null, null, null);
			return false;
		}
		if (mSourceId.getText().length() == 0) {
			AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_missing_source_id)
					, null
					, this
					, android.R.string.ok
					, null, null, null);
			return false;
		}

		final String sourceId = mSourceId.getEditableText().toString();
		if (sourceId.startsWith("http") || sourceId.startsWith("https")) {
			if (!MiscUtils.validWebUri(sourceId)) {
				AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert
						, null
						, getResources().getString(R.string.error_weburi_invalid)
						, null
						, this
						, android.R.string.ok
						, null, null, null);
				return false;
			}
		}
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
						((TextView)parent.getChildAt(0)).setTextColor(getResources().getColor(R.color.text_light));			
					}
				}
				
				/* Do nothing when the hint item is selected */
				if (mEntity == null) {
					if (position == 0) {
						final String sourceType = mSourceSuggestionStrings.get(position);
						mSource = buildCustomSource(sourceType);
						mSourceCaption.setText(mSource.caption);
						mSourceId.setText(mSource.id);
						mSource.custom = true;
						drawSourceIcon();
					}
				}
				else {
					if (position != parent.getCount()) {
						if (mEntity.sourceSuggestions != null && position < mEntity.sourceSuggestions.size()) {
							mSource = mEntity.sourceSuggestions.get(position);
							mSourceCaption.setText(mSource.caption);
							mSourceId.setText(mSource.id);
							drawSourceIcon();
						}
						else if (position < mSourceSuggestionStrings.size()) {
							final String sourceType = mSourceSuggestionStrings.get(position);
							mSource = buildCustomSource(sourceType);
							mSourceCaption.setText(mSource.caption);
							mSourceId.setText(mSource.id);
							mSource.custom = true;
							drawSourceIcon();
						}
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
	protected int getLayoutID() {
		return R.layout.builder_source;
	}
}