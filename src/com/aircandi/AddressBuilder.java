package com.aircandi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.aircandi.core.CandiConstants;
import com.aircandi.service.ProxibaseService;
import com.aircandi.service.ProxibaseService.ServiceDataType;
import com.aircandi.service.objects.Location;

public class AddressBuilder extends FormActivity {

	private Button		mButtonSave;
	private Location	mLocation;
	private String		mPhone;
	private EditText	mAddress;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initialize();
		draw();
	}

	private void initialize() {
		Bundle extras = this.getIntent().getExtras();
		if (extras != null) {
			mPhone = extras.getString(getString(R.string.EXTRA_PHONE));
			String jsonAddress = extras.getString(getString(R.string.EXTRA_ADDRESS));
			if (jsonAddress != null) {
				mLocation = (Location) ProxibaseService.convertJsonToObjectInternalSmart(jsonAddress, ServiceDataType.Location);
			}
		}

		if (mLocation == null) {
			mLocation = new Location();
		}

		mButtonSave = (Button) findViewById(R.id.btn_save);
		mButtonSave.setEnabled(false);

		mAddress = (EditText) findViewById(R.id.address);
		mAddress.addTextChangedListener(new SimpleTextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				mButtonSave.setEnabled(enableSave());
			}
		});
	}

	private void draw() {
		/* Author */
		if (mLocation.address != null) {
			((EditText) findViewById(R.id.address)).setText(mLocation.address);
		}
		if (mLocation.crossStreet != null) {
			((EditText) findViewById(R.id.cross_street)).setText(mLocation.crossStreet);
		}
		if (mLocation.city != null) {
			((EditText) findViewById(R.id.city)).setText(mLocation.city);
		}
		if (mLocation.state != null) {
			((EditText) findViewById(R.id.state)).setText(mLocation.state);
		}
		if (mLocation.postalCode != null) {
			((EditText) findViewById(R.id.zip_code)).setText(mLocation.postalCode);
		}
		if (mPhone != null) {
			((EditText) findViewById(R.id.phone)).setText(mPhone);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onSaveButtonClick(View view) {
		gather();
		doSave();
	}

	private void gather() {
		mPhone = ((EditText) findViewById(R.id.phone)).getEditableText().toString();
		mLocation.address = ((EditText) findViewById(R.id.address)).getEditableText().toString();
		mLocation.crossStreet = ((EditText) findViewById(R.id.cross_street)).getEditableText().toString();
		mLocation.city = ((EditText) findViewById(R.id.city)).getEditableText().toString();
		mLocation.state = ((EditText) findViewById(R.id.state)).getEditableText().toString();
		mLocation.postalCode = ((EditText) findViewById(R.id.zip_code)).getEditableText().toString();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {

		if (requestCode == CandiConstants.ACTIVITY_SIGNIN) {
			if (resultCode == Activity.RESULT_CANCELED) {
				setResult(resultCode);
				finish();
			}
			else {
				initialize();
				draw();
			}
		}
		else {
			super.onActivityResult(requestCode, resultCode, intent);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	private void doSave() {
		Intent intent = new Intent();
		intent.putExtra(getString(R.string.EXTRA_PHONE), mPhone);
		if (mLocation != null) {
			String jsonAddress = ProxibaseService.convertObjectToJsonSmart(mLocation, false, true);
			intent.putExtra(getString(R.string.EXTRA_ADDRESS), jsonAddress);
		}
		setResult(Activity.RESULT_OK, intent);
		finish();
	}

	private boolean enableSave() {
		if (mAddress.getText().toString().length() > 0) {
			return true;
		}
		return false;
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------
	@Override
	protected Integer getThemeResId() {
		return R.style.aircandi_theme_dialog_light;
	}

	@Override
	protected int getLayoutID() {
		return R.layout.address_builder;
	}
}