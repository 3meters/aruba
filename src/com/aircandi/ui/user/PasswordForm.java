package com.aircandi.ui.user;

import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi;
import com.aircandi.beta.R;
import com.aircandi.components.AircandiCommon;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.EntityManager;
import com.aircandi.components.FontManager;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.Tracker;
import com.aircandi.service.objects.User;
import com.aircandi.ui.base.FormActivity;
import com.aircandi.utilities.ImageUtils;

@SuppressWarnings("ucd")
public class PasswordForm extends FormActivity {

	private EditText	mTextPasswordOld;
	private EditText	mTextPassword;
	private EditText	mTextPasswordConfirm;
	private User		mUser;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!isFinishing()) {
			initialize();
		}
	}

	private void initialize() {
		
		mCommon.mActionBar.setDisplayHomeAsUpEnabled(true);

		mUser = Aircandi.getInstance().getUser();
		mTextPasswordOld = (EditText) findViewById(R.id.text_password_old);
		mTextPassword = (EditText) findViewById(R.id.text_password);
		mTextPasswordConfirm = (EditText) findViewById(R.id.text_password_confirm);

		FontManager.getInstance().setTypefaceDefault(mTextPasswordOld);
		FontManager.getInstance().setTypefaceDefault(mTextPassword);
		FontManager.getInstance().setTypefaceDefault(mTextPasswordConfirm);
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.text_label_password_old));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.text_label_password));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.text_label_password_confirm));
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	protected void doSave() {

		if (validate()) {

			new AsyncTask() {

				@Override
				protected void onPreExecute() {
					mCommon.showBusy(R.string.progress_changing_password, true);
				}

				@Override
				protected Object doInBackground(Object... params) {
					Thread.currentThread().setName("UpdatePassword");
					final ModelResult result = EntityManager.getInstance().updatePassword(
							mUser.id, 
							mTextPasswordOld.getText().toString(), 
							mTextPassword.getText().toString());
					return result;
				}

				@Override
				protected void onPostExecute(Object response) {
					final ModelResult result = (ModelResult) response;
					mCommon.hideBusy(true);
					if (result.serviceResponse.responseCode == ResponseCode.Success) {

						Logger.i(this, "User changed password: " + Aircandi.getInstance().getUser().name + " (" + Aircandi.getInstance().getUser().id + ")");
						Tracker.sendEvent("ui_action", "change_password", null, 0, Aircandi.getInstance().getUser());
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
	}

	private boolean validate() {
		if (mTextPasswordOld.getText().length() == 0) {
			AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_missing_password_new)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}
		if (mTextPassword.getText().length() == 0) {
			AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_missing_password_new)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}
		if (mTextPasswordConfirm.getText().length() == 0) {
			AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_missing_password_confirmation)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}
		if (mTextPassword.getText().length() < 6 || mTextPasswordConfirm.getText().length() < 6) {
			AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_missing_password_weak)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}
		if (!mTextPassword.getText().toString().equals(mTextPasswordConfirm.getText().toString())) {

			AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert
					, getResources().getString(R.string.error_signup_missmatched_passwords_title)
					, getResources().getString(R.string.error_signup_missmatched_passwords_message)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			mTextPasswordConfirm.setText("");
			return false;
		}
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Application menu routines (settings)
	// --------------------------------------------------------------------------------------------

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.accept) {
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
	protected int getLayoutId() {
		return R.layout.password_form;
	}
}