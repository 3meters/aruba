package com.aircandi;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.aircandi.components.AircandiCommon;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.Events;
import com.aircandi.components.ImageUtils;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.ProxiExplorer.ModelResult;
import com.aircandi.components.Tracker;
import com.aircandi.components.Utilities;
import com.aircandi.core.CandiConstants;
import com.aircandi.service.ProxibaseService;
import com.aircandi.service.ProxibaseService.ServiceDataType;
import com.aircandi.service.objects.ServiceData;
import com.aircandi.service.objects.User;

public class SignInForm extends FormActivity {

	private EditText	mTextEmail;
	private EditText	mTextPassword;
	private TextView	mTextMessage;
	private Button		mButtonSignIn;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initialize();
		draw();
	}

	private void initialize() {
		mTextEmail = (EditText) findViewById(R.id.text_email);
		mTextPassword = (EditText) findViewById(R.id.text_password);
		mTextMessage = (TextView) findViewById(R.id.form_message);
		mButtonSignIn = (Button) findViewById(R.id.btn_signin);
		mButtonSignIn.setEnabled(false);
		mTextEmail.addTextChangedListener(new SimpleTextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				mButtonSignIn.setEnabled(isValid());
			}
		});
		mTextPassword.addTextChangedListener(new SimpleTextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				mButtonSignIn.setEnabled(isValid());
			}
		});
	}

	public void draw() {
		if (mCommon.mMessage != null) {
			mTextMessage.setText(mCommon.mMessage);
		}
		String email = Aircandi.settings.getString(Preferences.SETTING_LAST_EMAIL, null);
		if (email != null) {
			mTextEmail.setText(email);
			mTextPassword.requestFocus();
		}
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------
	public void onSendPasswordButtonClick(View view) {
		AircandiCommon.showAlertDialog(R.drawable.icon_app
				, getResources().getString(R.string.alert_send_password_title)
				, getResources().getString(R.string.alert_send_password_message)
				, null
				, SignInForm.this, android.R.string.ok, null, null, null);
		Tracker.trackEvent("DialogSendPassword", "Open", null, 0);
	}

	public void onSignupButtonClick(View view) {
		mCommon.signup();
	}

	public void onSignInButtonClick(View view) {

		if (validate()) {

			final String email = mTextEmail.getText().toString().toLowerCase();
			final String password = mTextPassword.getText().toString();

			new AsyncTask() {

				@Override
				protected void onPreExecute() {
					mCommon.showProgressDialog(getString(R.string.progress_signing_in), false);
				}

				@Override
				protected Object doInBackground(Object... params) {
					
					ModelResult result = ProxiExplorer.getInstance().getEntityModel().signin(email, password);
					return result;
				}

				@Override
				protected void onPostExecute(Object response) {
					
					ModelResult result = (ModelResult) response;
					mCommon.hideProgressDialog();
					if (result.serviceResponse.responseCode == ResponseCode.Success) {

						Tracker.startNewSession();
						Tracker.trackEvent("User", "Signin", null, 0);

						String jsonResponse = (String) result.serviceResponse.data;
						ServiceData serviceData = ProxibaseService.convertJsonToObjectSmart(jsonResponse, ServiceDataType.None);
						User user = serviceData.user;
						user.session = serviceData.session;
						Logger.i(this, "User signed in: " + user.name + " (" + user.id + ")");

						Aircandi.getInstance().setUser(user);
						Events.EventBus.onUserChanged(user);						
						ImageUtils.showToastNotification(getResources().getString(R.string.alert_signed_in)
								+ " " + Aircandi.getInstance().getUser().name, Toast.LENGTH_SHORT);

						String jsonUser = ProxibaseService.convertObjectToJsonSmart(user, true);
						String jsonSession = ProxibaseService.convertObjectToJsonSmart(user.session, true);

						Aircandi.settingsEditor.putString(Preferences.PREF_USER, jsonUser);
						Aircandi.settingsEditor.putString(Preferences.PREF_USER_SESSION, jsonSession);
						Aircandi.settingsEditor.putString(Preferences.SETTING_LAST_EMAIL, user.email);
						Aircandi.settingsEditor.commit();

						setResult(CandiConstants.RESULT_USER_SIGNED_IN);
						finish();
					}
					else {
						mTextPassword.setText("");
						mCommon.handleServiceError(result.serviceResponse, ServiceOperation.Signin);
					}
				}
			}.execute();
		}
	}

	private boolean isValid() {
		/*
		 * Client validation logic is handle here. The service may still reject based on
		 * additional validation rules.
		 */
		if (mTextEmail.getText().length() == 0) {
			return false;
		}
		if (mTextPassword.getText().length() < 6) {
			return false;
		}

		return true;
	}

	private boolean validate() {
		if (!Utilities.validEmail(mTextEmail.getText().toString())) {
			mCommon.showAlertDialogSimple(null, getString(R.string.error_invalid_email));
			return false;
		}
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Application menu routines (settings)
	// --------------------------------------------------------------------------------------------

	public boolean onCreateOptionsMenu(Menu menu) {
		mCommon.doCreateOptionsMenu(menu);
		return true;
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		mCommon.doPrepareOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		mCommon.doOptionsItemSelected(item);
		return true;
	}

	//--------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutID() {
		return R.layout.signin_form;
	}
}