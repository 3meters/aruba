package com.aircandi.ui.helpers;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Constants;
import com.aircandi.beta.R;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.ObjectType;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Place;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.utilities.Utilities;

public class AddressBuilder extends BaseActivity {

	private Entity	mEntity;

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
			final String jsonAddress = extras.getString(Constants.EXTRA_PLACE);
			if (jsonAddress != null) {
				mEntity = (Place) HttpService.jsonToObject(jsonAddress, ObjectType.Place);
			}
		}

		if (mEntity == null) {
			mEntity = new Place();
		}

		mActionBar.setDisplayHomeAsUpEnabled(true);
		mActionBar.setTitle(R.string.dialog_address_builder_title);

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
	}

	private void draw() {
		
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
	// Event routines
	// --------------------------------------------------------------------------------------------

	private void gather() {
		Place place = (Place) mEntity;
		place.phone = Utilities.emptyAsNull(((EditText) findViewById(R.id.phone)).getEditableText().toString());
		place.address = Utilities.emptyAsNull(((EditText) findViewById(R.id.address)).getEditableText().toString());
		place.city = Utilities.emptyAsNull(((EditText) findViewById(R.id.city)).getEditableText().toString());
		place.region = Utilities.emptyAsNull(((EditText) findViewById(R.id.state)).getEditableText().toString());
		place.postalCode = Utilities.emptyAsNull(((EditText) findViewById(R.id.zip_code)).getEditableText().toString());
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	private void doSave() {
		final Intent intent = new Intent();
		if (mEntity != null) {
			final String jsonAddress = HttpService.objectToJson(mEntity);
			intent.putExtra(Constants.EXTRA_PLACE, jsonAddress);
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
		super.onOptionsItemSelected(item);
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.builder_address;
	}
}