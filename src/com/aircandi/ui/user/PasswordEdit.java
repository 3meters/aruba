package com.aircandi.ui.user;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Toast;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.EntityManager;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.ui.base.BaseEdit;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.UI;

@SuppressWarnings("ucd")
public class PasswordEdit extends BaseEdit {

	private EditText	mPasswordOld;
	private EditText	mPassword;
	private CheckBox	mPasswordUnmask;
	private CheckBox	mPasswordUnmaskOld;

	/* Inputs */
	protected String	mEntityId;

	@Override
	public void unpackIntent() {
		super.unpackIntent();
		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mEntityId = extras.getString(Constants.EXTRA_ENTITY_ID);
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mPasswordOld = (EditText) findViewById(R.id.password_old);
		mPassword = (EditText) findViewById(R.id.password);
		mPasswordUnmask = (CheckBox) findViewById(R.id.chk_unmask);
		mPasswordUnmaskOld = (CheckBox) findViewById(R.id.chk_unmask_old);
		
		mPasswordUnmask.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					mPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
							| InputType.TYPE_CLASS_TEXT
							| InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
				}
				else {
					mPassword.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD
							| InputType.TYPE_CLASS_TEXT
							| InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
				}
			}});
		
		mPasswordUnmaskOld.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					mPasswordOld.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
							| InputType.TYPE_CLASS_TEXT
							| InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
				}
				else {
					mPasswordOld.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD
							| InputType.TYPE_CLASS_TEXT
							| InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
				}
			}});
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	public void onAccept() {
		if (validate()) {
			update();
		}
	}

	// --------------------------------------------------------------------------------------------
	// Services
	// --------------------------------------------------------------------------------------------

	@Override
	protected void update() {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusyManager.showBusy(R.string.progress_changing_password);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("UpdatePassword");
				final ModelResult result = EntityManager.getInstance().updatePassword(
						mEntityId,
						mPasswordOld.getText().toString(),
						mPassword.getText().toString(),
						PasswordEdit.class.getSimpleName());
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;
				hideBusy();
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

					Logger.i(this, "User changed password: " + Aircandi.getInstance().getCurrentUser().name + " (" + Aircandi.getInstance().getCurrentUser().id + ")");
					UI.showToastNotification(getResources().getString(R.string.alert_password_changed)
							+ " " + Aircandi.getInstance().getCurrentUser().name, Toast.LENGTH_SHORT);
					finish();
				}
				else {
					mPassword.setText("");
					Errors.handleError(PasswordEdit.this, result.serviceResponse);
				}
			}
		}.execute();
	}

	@Override
	protected boolean validate() {
		if (!super.validate()) {
			return false;
		}
		if (mPasswordOld.getText().length() == 0) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_missing_password_new)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}
		if (mPassword.getText().length() == 0) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_missing_password_new)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}
		if (mPassword.getText().length() < 6) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_missing_password_weak)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.password_edit;
	}

}