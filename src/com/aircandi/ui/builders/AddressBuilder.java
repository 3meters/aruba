package com.aircandi.ui.builders;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.actionbarsherlock.view.MenuItem;
import com.aircandi.CandiConstants;
import com.aircandi.beta.R;
import com.aircandi.components.FontManager;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.ServiceDataType;
import com.aircandi.service.objects.Location;
import com.aircandi.ui.base.FormActivity;
import com.aircandi.utilities.MiscUtils;

public class AddressBuilder extends FormActivity {

	private Location	mLocation;
	private String		mPhone;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!isFinishing()) {
			initialize();
			draw();
		}
	}

	private void initialize() {
		final Bundle extras = this.getIntent().getExtras();
		if (extras != null) {
			mPhone = extras.getString(CandiConstants.EXTRA_PHONE);
			final String jsonAddress = extras.getString(CandiConstants.EXTRA_ADDRESS);
			if (jsonAddress != null) {
				mLocation = (Location) HttpService.convertJsonToObjectInternalSmart(jsonAddress, ServiceDataType.Location);
			}
		}

		if (mLocation == null) {
			mLocation = new Location();
		}

		mCommon.mActionBar.setDisplayHomeAsUpEnabled(true);
		mCommon.mActionBar.setTitle(R.string.dialog_address_builder_title);

		((EditText) findViewById(R.id.phone)).setImeOptions(EditorInfo.IME_ACTION_DONE);
		((EditText) findViewById(R.id.phone)).setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					gather();
					doSave();
					return true;
				}
				return false;
			}
		});

		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.title));
		FontManager.getInstance().setTypefaceDefault((EditText) findViewById(R.id.phone));
		FontManager.getInstance().setTypefaceDefault((EditText) findViewById(R.id.address));
		FontManager.getInstance().setTypefaceDefault((EditText) findViewById(R.id.cross_street));
		FontManager.getInstance().setTypefaceDefault((EditText) findViewById(R.id.city));
		FontManager.getInstance().setTypefaceDefault((EditText) findViewById(R.id.state));
		FontManager.getInstance().setTypefaceDefault((EditText) findViewById(R.id.zip_code));
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

	private void gather() {
		mPhone = MiscUtils.emptyAsNull(((EditText) findViewById(R.id.phone)).getEditableText().toString());
		mLocation.address = MiscUtils.emptyAsNull(((EditText) findViewById(R.id.address)).getEditableText().toString());
		mLocation.crossStreet = MiscUtils.emptyAsNull(((EditText) findViewById(R.id.cross_street)).getEditableText().toString());
		mLocation.city = MiscUtils.emptyAsNull(((EditText) findViewById(R.id.city)).getEditableText().toString());
		mLocation.state = MiscUtils.emptyAsNull(((EditText) findViewById(R.id.state)).getEditableText().toString());
		mLocation.postalCode = MiscUtils.emptyAsNull(((EditText) findViewById(R.id.zip_code)).getEditableText().toString());
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
		final Intent intent = new Intent();
		intent.putExtra(CandiConstants.EXTRA_PHONE, mPhone);
		if (mLocation != null) {
			final String jsonAddress = HttpService.convertObjectToJsonSmart(mLocation, false, true);
			intent.putExtra(CandiConstants.EXTRA_ADDRESS, jsonAddress);
		}
		setResult(Activity.RESULT_OK, intent);
		finish();
	}

	// --------------------------------------------------------------------------------------------
	// Application menu routines (settings)
	// --------------------------------------------------------------------------------------------

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.accept) {
			gather();
			doSave();
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
	protected Boolean isDialog() {
		return false;
	}

	@Override
	protected int getLayoutID() {
		return R.layout.builder_address;
	}
}