package com.aircandi;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.aircandi.components.AircandiCommon;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.ImageUtils;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.ProxiExplorer.ModelResult;
import com.aircandi.components.Tracker;
import com.aircandi.service.objects.User;

public class PasswordForm extends FormActivity {

	private EditText	mTextPasswordOld;
	private EditText	mTextPassword;
	private EditText	mTextPasswordConfirm;
	private Button		mButtonSave;
	private User		mUser;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initialize();

	}

	protected void initialize() {
		mUser = Aircandi.getInstance().getUser();
		mTextPasswordOld = (EditText) findViewById(R.id.text_password_old);
		mTextPassword = (EditText) findViewById(R.id.text_password);
		mTextPasswordConfirm = (EditText) findViewById(R.id.text_password_confirm);
		mButtonSave = (Button) findViewById(R.id.btn_save);

		mTextPasswordOld.addTextChangedListener(new SimpleTextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				mButtonSave.setEnabled(isValid());
			}
		});

		mTextPassword.addTextChangedListener(new SimpleTextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				mButtonSave.setEnabled(isValid());
			}
		});

		mTextPasswordConfirm.addTextChangedListener(new SimpleTextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				mButtonSave.setEnabled(isValid());
			}
		});
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onSaveButtonClick(View view) {
		if (validate()) {
			doSave();
		}
	}

	private boolean validate() {
		if (!mTextPassword.getText().toString().equals(mTextPasswordConfirm.getText().toString())) {

			AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert
					, getResources().getString(R.string.error_signup_missmatched_passwords_title)
					, getResources().getString(R.string.error_signup_missmatched_passwords_message)
					, null
					, this
					, android.R.string.ok
					, null, null, null);
			mTextPasswordConfirm.setText("");
			return false;
		}
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	protected void doSave() {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showProgressDialog(true, getString(R.string.progress_changing_password));
			}

			@Override
			protected Object doInBackground(Object... params) {
				ModelResult result = ProxiExplorer.getInstance().getEntityModel().updatePassword(mUser.id,  mTextPasswordOld.getText().toString(), mTextPassword.getText().toString());
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				ModelResult result = (ModelResult) response;
				mCommon.showProgressDialog(false, null);
				if (result.serviceResponse.responseCode == ResponseCode.Success) {

					Logger.i(this, "User changed password: " + Aircandi.getInstance().getUser().name + " (" + Aircandi.getInstance().getUser().id + ")");
					Tracker.trackEvent("User", "PasswordChange", null, 0);
					ImageUtils.showToastNotification(getResources().getString(R.string.alert_password_changed)
							+ " " + Aircandi.getInstance().getUser().name, Toast.LENGTH_SHORT);
					finish();
				}
				else {
					mTextPassword.setText("");
					mCommon.handleServiceError(result.serviceResponse, ServiceOperation.PasswordChange);
				}
			}
		}.execute();
	}

	private boolean isValid() {
		if (mTextPasswordOld.getText().length() < 6) {
			return false;
		}
		if (mTextPassword.getText().length() < 6) {
			return false;
		}
		if (mTextPasswordConfirm.getText().length() < 6) {
			return false;
		}
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutID() {
		return R.layout.password_form;
	}
}