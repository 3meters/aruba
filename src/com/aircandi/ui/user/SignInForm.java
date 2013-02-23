package com.aircandi.ui.user;

import java.util.Locale;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.aircandi.Aircandi;
import com.aircandi.CandiConstants;
import com.aircandi.R;
import com.aircandi.components.AircandiCommon;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.FontManager;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProxiManager;
import com.aircandi.components.ProxiManager.ModelResult;
import com.aircandi.components.Tracker;
import com.aircandi.service.ProxibaseService;
import com.aircandi.service.ProxibaseService.ServiceDataType;
import com.aircandi.service.objects.ServiceData;
import com.aircandi.service.objects.User;
import com.aircandi.ui.Preferences;
import com.aircandi.ui.base.FormActivity;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;
import com.aircandi.utilities.ImageUtils;
import com.aircandi.utilities.MiscUtils;

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
		
		FontManager.getInstance().setTypefaceDefault(mTextEmail);
		FontManager.getInstance().setTypefaceDefault(mTextPassword);
		FontManager.getInstance().setTypefaceDefault(mTextMessage);
		FontManager.getInstance().setTypefaceDefault(mButtonSignIn);
		FontManager.getInstance().setTypefaceDefault((Button) findViewById(R.id.button_send_password));
		FontManager.getInstance().setTypefaceDefault((Button) findViewById(R.id.button_cancel));
	}

	private void draw() {
		if (mCommon.mMessage != null) {
			mTextMessage.setText(mCommon.mMessage);
		}
		final String email = Aircandi.settings.getString(Preferences.SETTING_LAST_EMAIL, null);
		if (email != null) {
			mTextEmail.setText(email);
			mTextPassword.requestFocus();
		}
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------
	@SuppressWarnings("ucd")
	public void onSendPasswordButtonClick(View view) {
		AircandiCommon.showAlertDialog(R.drawable.ic_app
				, getResources().getString(R.string.alert_send_password_title)
				, getResources().getString(R.string.alert_send_password_message)
				, null
				, SignInForm.this, android.R.string.ok, null, null, null);
		Tracker.sendEvent("ui_action", "recover_password", null, 0);
	}

	@SuppressWarnings("ucd")
	public void onSignInButtonClick(View view) {

		if (validate()) {

			final String email = mTextEmail.getText().toString().toLowerCase(Locale.US);
			final String password = mTextPassword.getText().toString();

			new AsyncTask() {

				@Override
				protected void onPreExecute() {
					mCommon.showBusy(R.string.progress_signing_in, true);
				}

				@Override
				protected Object doInBackground(Object... params) {
					Thread.currentThread().setName("SignIn");				
					final ModelResult result = ProxiManager.getInstance().getEntityModel().signin(email, password);
					return result;
				}

				@Override
				protected void onPostExecute(Object response) {
					
					final ModelResult result = (ModelResult) response;
					mCommon.hideBusy(true);
					if (result.serviceResponse.responseCode == ResponseCode.Success) {

						Tracker.startNewSession();
						Tracker.sendEvent("ui_action", "signin_user", null, 0);

						final String jsonResponse = (String) result.serviceResponse.data;
						final ServiceData serviceData = ProxibaseService.convertJsonToObjectSmart(jsonResponse, ServiceDataType.None);
						final User user = serviceData.user;
						user.session = serviceData.session;
						Logger.i(this, "User signed in: " + user.name + " (" + user.id + ")");

						Aircandi.getInstance().setUser(user);
						ImageUtils.showToastNotification(getResources().getString(R.string.alert_signed_in)
								+ " " + Aircandi.getInstance().getUser().name, Toast.LENGTH_SHORT);

						final String jsonUser = ProxibaseService.convertObjectToJsonSmart(user, false, true);
						final String jsonSession = ProxibaseService.convertObjectToJsonSmart(user.session, false, true);

						Aircandi.settingsEditor.putString(Preferences.SETTING_USER, jsonUser);
						Aircandi.settingsEditor.putString(Preferences.SETTING_USER_SESSION, jsonSession);
						Aircandi.settingsEditor.putString(Preferences.SETTING_LAST_EMAIL, user.email);
						Aircandi.settingsEditor.commit();

						setResult(CandiConstants.RESULT_USER_SIGNED_IN);
						finish();
						AnimUtils.doOverridePendingTransition(SignInForm.this, TransitionType.FormToPage);
					}
					else {
						mTextPassword.setText("");
						mCommon.handleServiceError(result.serviceResponse, ServiceOperation.Signin);
					}
				}
			}.execute();
		}
	}

	private boolean validate() {
		if (mTextPassword.getText().length() < 6) {
			AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_missing_password)
					, null
					, this
					, android.R.string.ok
					, null, null, null);
			return false;
		}
		if (!MiscUtils.validEmail(mTextEmail.getText().toString())) {
			AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_invalid_email)
					, null
					, this
					, android.R.string.ok
					, null, null, null);
			return false;
		}
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