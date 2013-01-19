package com.aircandi.ui.builders;

import java.util.ArrayList;
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

import com.actionbarsherlock.view.Window;
import com.aircandi.CandiConstants;
import com.aircandi.R;
import com.aircandi.components.AircandiCommon;
import com.aircandi.components.FontManager;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequestBuilder;
import com.aircandi.service.ProxibaseService;
import com.aircandi.service.ProxibaseService.ServiceDataType;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Source;
import com.aircandi.ui.base.FormActivity;
import com.aircandi.ui.widgets.WebImageView;
import com.aircandi.utilities.MiscUtils;

public class SourceBuilder extends FormActivity {

	private Source			mSource;
	private Boolean			mEditing	= false;

	private TextView		mTitle;
	private WebImageView	mSourceIcon;
	private Spinner			mSourceTypePicker;
	private EditText		mSourceName;
	private EditText		mSourceId;

	private List<String>	mSourceSuggestionStrings;
	private Entity			mEntity;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		super.onCreate(savedInstanceState);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
		initialize();
		bind();
	}

	private void initialize() {
		mTitle = (TextView) findViewById(R.id.custom_title);
		Bundle extras = this.getIntent().getExtras();
		if (extras != null) {
			String jsonSource = extras.getString(CandiConstants.EXTRA_SOURCE);
			if (jsonSource != null) {
				mSource = (Source) ProxibaseService.convertJsonToObjectInternalSmart(jsonSource, ServiceDataType.Source);
				mEditing = true;
				mTitle.setText(R.string.dialog_source_builder_title_editing);
			}
			String entityId = extras.getString(CandiConstants.EXTRA_ENTITY_ID);
			if (entityId != null) {
				mEntity = ProxiExplorer.getInstance().getEntityModel().getCacheEntity(entityId);
				mEditing = false;
				mTitle.setText(R.string.dialog_source_builder_title_new);
			}
		}
		mSourceIcon = (WebImageView) findViewById(R.id.image);
		mSourceTypePicker = (Spinner) findViewById(R.id.source_type_picker);
		mSourceName = (EditText) findViewById(R.id.name);
		mSourceId = (EditText) findViewById(R.id.id);

		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.custom_title));
		FontManager.getInstance().setTypefaceDefault((EditText) findViewById(R.id.name));
		FontManager.getInstance().setTypefaceDefault((EditText) findViewById(R.id.id));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_save));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_cancel));
	}

	public void bind() {
		if (mEditing) {
			mSourceName.setText(mSource.name);
			mSourceId.setText(mSource.id);
			drawSourceIcon();
		}
		else {
			mSourceTypePicker.setVisibility(View.VISIBLE);
			mSourceSuggestionStrings = new ArrayList<String>();
			if (mEntity != null && mEntity.sourceSuggestions != null) {
				for (Source source : mEntity.sourceSuggestions) {
					mSourceSuggestionStrings.add(source.source);
				}
			}
			mSourceSuggestionStrings.add("website");
			mSourceSuggestionStrings.add(getString(R.string.form_source_type_hint));
			initializeSpinner(mSourceSuggestionStrings);
		}
	}

	public Source buildCustomSource(String sourceType) {
		Source source = new Source();
		source.source = sourceType;
		source.origin = "user";
		if (sourceType.equals("website")) {
			source.icon = "source_website.png";
		}
		return source;
	}

	public void drawSourceIcon() {
		String imageUri = mSource.getImageUri();
		BitmapRequestBuilder builder = new BitmapRequestBuilder(mSourceIcon).setImageUri(imageUri);
		BitmapRequest bitmapRequest = builder.create();
		mSourceIcon.setBitmapRequest(bitmapRequest);
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onSaveButtonClick(View view) {
		if (validate()) {
			gather();
			doSave();
		}
	}

	private void gather() {
		if (mEditing) {
			mSource.name = mSourceName.getEditableText().toString();
			mSource.id = mSourceId.getEditableText().toString();
		}
		else {
			mSource.name = mSourceName.getEditableText().toString();
			mSource.id = mSourceId.getEditableText().toString();
		}
		if (mSource.source.equals("website")) {
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
		Intent intent = new Intent();
		if (mSource != null) {
			String jsonSource = ProxibaseService.convertObjectToJsonSmart(mSource, false, true);
			intent.putExtra(CandiConstants.EXTRA_SOURCE, jsonSource);
		}
		setResult(Activity.RESULT_OK, intent);
		finish();
	}

	private boolean validate() {
		if (mSourceName.getText().length() == 0) {
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

		String sourceId = mSourceId.getEditableText().toString();
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

		ArrayAdapter adapter = new ArrayAdapter(this, R.layout.spinner_item, items) {

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {

				View view = super.getView(position, convertView, parent);
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

		mSourceTypePicker.setAdapter(adapter);

		if (!mEditing) {
			mSourceTypePicker.setSelection(adapter.getCount());
		}

		mSourceTypePicker.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

				/* Do nothing when the hint item is selected */
				if (mEntity == null) {
					if (position == 0) {
						String sourceType = mSourceSuggestionStrings.get(position);
						mSource = buildCustomSource(sourceType);
						mSourceName.setText(mSource.name);
						mSourceId.setText(mSource.id);
						mSource.custom = true;
						drawSourceIcon();
					}
				}
				else {
					if (position != parent.getCount()) {
						if (mEntity.sourceSuggestions != null && position < mEntity.sourceSuggestions.size()) {
							mSource = (Source) mEntity.sourceSuggestions.get(position);
							mSourceName.setText(mSource.name);
							mSourceId.setText(mSource.id);
							drawSourceIcon();
						}
						else if (position < mSourceSuggestionStrings.size()) {
							String sourceType = mSourceSuggestionStrings.get(position);
							mSource = buildCustomSource(sourceType);
							mSourceName.setText(mSource.name);
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
		BitmapRequestBuilder builder = new BitmapRequestBuilder(mSourceIcon).setImageUri(uri);
		final BitmapRequest imageRequest = builder.create();
		mSourceIcon.setBitmapRequest(imageRequest);
	}

	@Override
	protected int getLayoutID() {
		return R.layout.builder_source;
	}
}