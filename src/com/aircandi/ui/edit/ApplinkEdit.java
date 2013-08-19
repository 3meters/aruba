package com.aircandi.ui.edit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.service.objects.Applink;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Photo.PhotoSource;
import com.aircandi.ui.base.BaseEntityEdit;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.UI;
import com.aircandi.utilities.Utilities;

public class ApplinkEdit extends BaseEntityEdit {

	private Spinner			mTypePicker;
	private TextView		mAppId;
	private TextView		mAppUrl;
	private Button			mButtonTest;

	private Integer			mSpinnerItemResId;
	private Integer			mMissingResId;

	private List<String>	mApplinkSuggestionStrings;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mTypePicker = (Spinner) findViewById(R.id.type_picker);
		mAppId = (EditText) findViewById(R.id.app_id);
		mAppUrl = (EditText) findViewById(R.id.app_url);
		mButtonTest = (Button) findViewById(R.id.button_test);

		mSpinnerItemResId = getThemeTone().equals("dark") ? R.layout.spinner_item_dark : R.layout.spinner_item_light;

		/* Turn on test button if user has something to test */

		if (mAppId != null) {
			mAppId.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					if (((Applink) mEntity).appId == null || !s.toString().equals(((Applink) mEntity).appId)) {
						mDirty = true;
					}
					if (s.toString() != "") {
						mButtonTest.setVisibility(View.VISIBLE);
					}
					else {
						mButtonTest.setVisibility(View.INVISIBLE);
					}
				}
			});
		}

		if (mAppUrl != null) {
			mAppUrl.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					if (((Applink) mEntity).appUrl == null || !s.toString().equals(((Applink) mEntity).appUrl)) {
						mDirty = true;
					}
					if (s.toString() != "") {
						mButtonTest.setVisibility(View.VISIBLE);
					}
					else {
						mButtonTest.setVisibility(View.INVISIBLE);
					}
				}
			});
		}
	}

	@Override
	protected void draw() {
		super.draw();

		final Applink applink = (Applink) mEntity;

		/* Spinners */

		if (mEditing) {
			((TextView) findViewById(R.id.type)).setText("Link type: " + applink.type);
			UI.setVisibility(findViewById(R.id.type), View.VISIBLE);
			UI.setVisibility(findViewById(R.id.type_picker), View.GONE);
			UI.setVisibility(mPhotoHolder, View.VISIBLE);
			UI.setVisibility(findViewById(R.id.name_holder), View.VISIBLE);
			UI.setVisibility(findViewById(R.id.app_id_holder), View.VISIBLE);
			UI.setVisibility(findViewById(R.id.app_url_holder), View.VISIBLE);

			mName.setHint(R.string.form_applink_name_hint);
			mAppId.setText(applink.appId);
			mAppUrl.setText(applink.appUrl);
			drawPhoto();
		}
		else {
			UI.setVisibility(findViewById(R.id.type), View.GONE);
			UI.setVisibility(findViewById(R.id.type_picker), View.VISIBLE);
			if (mTypePicker.getAdapter() == null) {
				mApplinkSuggestionStrings = new ArrayList<String>();
				mApplinkSuggestionStrings.add("website");
				mApplinkSuggestionStrings.add("facebook");
				mApplinkSuggestionStrings.add("twitter");
				mApplinkSuggestionStrings.add("email");
				mApplinkSuggestionStrings.add(getString(R.string.form_applink_type_hint));
				initializeSpinner(mApplinkSuggestionStrings);
			}

			if (mEntity.type != null) {
				mPhotoHolder.setVisibility(View.VISIBLE);
				mName.setHint(R.string.form_applink_name_hint);
				UI.setVisibility(findViewById(R.id.app_id_holder), View.GONE);
				UI.setVisibility(findViewById(R.id.app_url_holder), View.GONE);

				if (mEntity.type.equals(Constants.TYPE_APP_WEBSITE)) {
					UI.setVisibility(findViewById(R.id.app_url_holder), View.VISIBLE);
					mAppUrl.setHint(R.string.form_applink_url_website_hint);
					mMissingResId = R.string.error_missing_applink_url_website;
				}
				else if (mEntity.type.equals(Constants.TYPE_APP_EMAIL)) {
					UI.setVisibility(findViewById(R.id.app_id_holder), View.VISIBLE);
					mAppId.setHint(R.string.form_applink_id_email_hint);
					mMissingResId = R.string.error_missing_applink_id_email;
				}
				else if (mEntity.type.equals(Constants.TYPE_APP_FACEBOOK)) {
					UI.setVisibility(findViewById(R.id.app_id_holder), View.VISIBLE);
					mAppId.setHint(R.string.form_applink_id_facebook_hint);
					mMissingResId = R.string.error_missing_applink_id_facebook;
				}
				else if (mEntity.type.equals(Constants.TYPE_APP_TWITTER)) {
					UI.setVisibility(findViewById(R.id.app_id_holder), View.VISIBLE);
					mAppId.setHint(R.string.form_applink_id_twitter_hint);
					mMissingResId = R.string.error_missing_applink_id_twitter;
				}
				if (applink.appId != null && !applink.appId.equals("")) {
					if (mAppId != null) {
						mAppId.setText(applink.appId);
					}
				}
				if (applink.appUrl != null && !applink.appUrl.equals("")) {
					if (mAppUrl != null) {
						mAppUrl.setText(applink.appUrl);
					}
				}

				drawPhoto();
			}
		}
	}

	private void initializeSpinner(final List<String> items) {

		final ArrayAdapter adapter = new ArrayAdapter(this, mSpinnerItemResId, items) {

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {

				final View view = super.getView(position, convertView, parent);

				final TextView text = (TextView) view.findViewById(R.id.spinner_name);
				if (getThemeTone().equals("dark")) {
					if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
						text.setTextColor(Aircandi.getInstance().getResources().getColor(R.color.text_dark));
					}
				}

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

		if (getThemeTone().equals("dark")) {
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

				if (getThemeTone().equals("dark")) {
					if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
						((TextView) parent.getChildAt(0)).setTextColor(getResources().getColor(R.color.text_light));
					}
				}

				/* Do nothing when the hint item is selected */
				if (position != parent.getCount()) {
					if (position < mApplinkSuggestionStrings.size()) {
						final String sourceType = mApplinkSuggestionStrings.get(position);
						setEntityType(sourceType);
						draw();
					}
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {}

		});
	}

	@Override
	protected void setEntityType(String type) {
		super.setEntityType(type);
		if (mEntity.data == null) {
			mEntity.data = new HashMap<String, Object>();
		}
		mEntity.data.put("origin", "user");
		mEntity.photo = new Photo(Applink.getDefaultPhotoUri(type), null, null, null, PhotoSource.assets);
		((Applink) mEntity).appId = null;
		((Applink) mEntity).appUrl = null;
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("ucd")
	public void onTestButtonClick(View view) {
		if (validate()) {
			gather();
			Routing.shortcut(this, mEntity.getShortcut(), null, null);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	protected void gather() {
		super.gather();

		if (mAppId != null) {
			((Applink) mEntity).appId = Utilities.emptyAsNull(mAppId.getText().toString().trim());
		}

		if (mAppUrl != null) {
			((Applink) mEntity).appUrl = Utilities.emptyAsNull(mAppUrl.getText().toString().trim());
			if (mEntity.type.equals(Constants.TYPE_APP_WEBSITE)) {
				String appUrl = ((Applink) mEntity).appUrl;
				if (!appUrl.startsWith("http://") && !appUrl.startsWith("https://")) {
					((Applink) mEntity).appUrl = "http://" + appUrl;
				}
			}
		}
	}

	@Override
	protected String getLinkType() {
		return Constants.TYPE_LINK_APPLINK;
	};

	// --------------------------------------------------------------------------------------------
	// Services
	// --------------------------------------------------------------------------------------------

	@Override
	protected boolean validate() {
		if (!super.validate()) {
			return false;
		}
		if (mEntity.type == null) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_missing_applink_type)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}

		if (mName != null && mName.getText().length() == 0) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_missing_entity_label)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}

		if (!mEditing && mAppId != null && mAppId.getText().length() == 0 && findViewById(R.id.app_id_holder).getVisibility() == View.VISIBLE) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(mMissingResId)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}

		if (!mEditing && mAppUrl != null && findViewById(R.id.app_url_holder).getVisibility() == View.VISIBLE) {
			final String url = mAppUrl.getEditableText().toString();
			if (url == null || url.length() == 0) {
				Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
						, null
						, getResources().getString(mMissingResId)
						, null
						, this
						, android.R.string.ok
						, null, null, null, null);
				return false;
			}
			else if (url != null && url.length() > 0 && !Utilities.validWebUri(url)) {
				Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
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

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.applink_edit;
	}
}