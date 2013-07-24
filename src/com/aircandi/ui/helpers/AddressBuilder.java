package com.aircandi.ui.helpers;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.aircandi.Constants;
import com.aircandi.beta.R;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.ObjectType;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Place;
import com.aircandi.ui.base.BaseEdit;
import com.aircandi.utilities.Utilities;

public class AddressBuilder extends BaseEdit {

	private Entity	mEntity;

	@Override
	protected void unpackIntent() {
		super.unpackIntent();
		
		final Bundle extras = this.getIntent().getExtras();
		if (extras != null) {
			final String jsonAddress = extras.getString(Constants.EXTRA_ENTITY);
			if (jsonAddress != null) {
				mEntity = (Place) HttpService.jsonToObject(jsonAddress, ObjectType.Place);
			}
		}
	}
	
	@Override
	protected void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		setActivityTitle(getString(R.string.dialog_address_builder_title));

		((EditText) findViewById(R.id.phone)).setImeOptions(EditorInfo.IME_ACTION_DONE);
		((EditText) findViewById(R.id.phone)).setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					onAccept();
					return true;
				}
				return false;
			}
		});
	}

	@Override
	protected void databind() {
		if (mEntity == null) {
			mEntity = new Place();
		}
	}

	@Override
	protected void draw() {

		Place place = (Place) mEntity;
		if (place.address != null) {
			((EditText) findViewById(R.id.address)).setText(place.address);
		}
		if (place.city != null) {
			((EditText) findViewById(R.id.city)).setText(place.city);
		}
		if (place.region != null) {
			((EditText) findViewById(R.id.state)).setText(place.region);
		}
		if (place.postalCode != null) {
			((EditText) findViewById(R.id.zip_code)).setText(place.postalCode);
		}
		if (place.phone != null) {
			((EditText) findViewById(R.id.phone)).setText(place.phone);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	private void gather() {
		Place place = (Place) mEntity;
		place.phone = Utilities.emptyAsNull(((EditText) findViewById(R.id.phone)).getEditableText().toString());
		place.address = Utilities.emptyAsNull(((EditText) findViewById(R.id.address)).getEditableText().toString());
		place.city = Utilities.emptyAsNull(((EditText) findViewById(R.id.city)).getEditableText().toString());
		place.region = Utilities.emptyAsNull(((EditText) findViewById(R.id.state)).getEditableText().toString());
		place.postalCode = Utilities.emptyAsNull(((EditText) findViewById(R.id.zip_code)).getEditableText().toString());
	}

	@Override
	public void onAccept() {
		gather();
		save();
	}

	// --------------------------------------------------------------------------------------------
	// Services
	// --------------------------------------------------------------------------------------------

	private void save() {
		final Intent intent = new Intent();
		if (mEntity != null) {
			final String jsonAddress = HttpService.objectToJson(mEntity);
			intent.putExtra(Constants.EXTRA_PLACE, jsonAddress);
		}
		setResult(Activity.RESULT_OK, intent);
		finish();
	}

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.builder_address;
	}
}