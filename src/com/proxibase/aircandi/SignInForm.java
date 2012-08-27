package com.proxibase.aircandi;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.proxibase.aircandi.components.AircandiCommon;
import com.proxibase.aircandi.components.AnimUtils;
import com.proxibase.aircandi.components.CommandType;
import com.proxibase.aircandi.components.ImageUtils;
import com.proxibase.aircandi.components.IntentBuilder;
import com.proxibase.aircandi.components.Logger;
import com.proxibase.aircandi.components.NetworkManager;
import com.proxibase.aircandi.components.AnimUtils.TransitionType;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.components.ProxiExplorer;
import com.proxibase.aircandi.components.Tracker;
import com.proxibase.aircandi.components.Utilities;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.service.ProxiConstants;
import com.proxibase.service.ProxibaseService;
import com.proxibase.service.ProxibaseService.GsonType;
import com.proxibase.service.ProxibaseService.RequestType;
import com.proxibase.service.ProxibaseService.ResponseFormat;
import com.proxibase.service.ServiceRequest;
import com.proxibase.service.objects.ServiceData;
import com.proxibase.service.objects.User;

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
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------
	public void onSendPasswordButtonClick(View view) {
		AircandiCommon.showAlertDialog(R.drawable.icon_app
				, getResources().getString(R.string.alert_send_password_title)
				, getResources().getString(R.string.alert_send_password_message)
				, SignInForm.this, android.R.string.ok, null, null, null);
		Tracker.trackEvent("DialogSendPassword", "Open", null, 0);
	}

	public void onSignupButtonClick(View view) {
		IntentBuilder intentBuilder = new IntentBuilder(this, SignUpForm.class);
		intentBuilder.setCommandType(CommandType.New);
		Intent intent = intentBuilder.create();
		startActivity(intent);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
	}

	public void onSignInButtonClick(View view) {

		if (validate()) {

			final String email = mTextEmail.getText().toString().toLowerCase();
			final String password = mTextPassword.getText().toString();

			new AsyncTask() {

				@Override
				protected void onPreExecute() {
					mCommon.showProgressDialog(true, getString(R.string.progress_signing_in));
				}

				@Override
				protected Object doInBackground(Object... params) {

					Bundle parameters = new Bundle();
					ServiceRequest serviceRequest = new ServiceRequest();

					parameters.putString("user", "object:{"
							+ "\"email\":\"" + email + "\","
							+ "\"password\":\"" + password + "\""
							+ "}");

					serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_AUTH + "signin")
							.setRequestType(RequestType.Method)
							.setParameters(parameters)
							.setResponseFormat(ResponseFormat.Json);

					ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);

					return serviceResponse;
				}

				@Override
				protected void onPostExecute(Object response) {

					ServiceResponse serviceResponse = (ServiceResponse) response;
					mCommon.showProgressDialog(false, null);
					if (serviceResponse.responseCode == ResponseCode.Success) {

						Tracker.startNewSession();
						Tracker.trackEvent("User", "Signin", null, 0);

						String jsonResponse = (String) serviceResponse.data;
						ServiceData serviceData = ProxibaseService.convertJsonToObject(jsonResponse, ServiceData.class, GsonType.ProxibaseService);
						User user = serviceData.user;
						user.session = serviceData.session;
						Logger.i(this, "User signed in: " + user.name + " (" + user.id + ")");


						Aircandi.getInstance().setUser(user);
						ImageUtils.showToastNotification(getResources().getString(R.string.alert_signed_in)
								+ " " + Aircandi.getInstance().getUser().name, Toast.LENGTH_SHORT);

						String jsonUser = ProxibaseService.convertObjectToJson((Object) user, GsonType.Internal);
						String jsonSession = ProxibaseService.convertObjectToJson((Object) user.session, GsonType.Internal);
						Aircandi.settingsEditor.putString(Preferences.PREF_USER, jsonUser);
						Aircandi.settingsEditor.putString(Preferences.PREF_USER_SESSION, jsonSession);
						Aircandi.settingsEditor.commit();

						/* Different user means different user candi */
						ProxiExplorer.getInstance().getEntityModel().getUserEntities().clear();

						setResult(CandiConstants.RESULT_USER_SIGNED_IN);
						finish();
					}
					else {
						/*
						 * Code for unsuccessful authentication:
						 * 
						 * - 401.1: email or password is wrong
						 */
						String jsonResponse = serviceResponse.exception.getResponseMessage();
						ServiceData serviceData = ProxibaseService.convertJsonToObject(jsonResponse, ServiceData.class, GsonType.Internal);
						if (serviceData.error != null && serviceData.error.code.floatValue() == ProxiConstants.HTTP_STATUS_CODE_UNAUTHORIZED_CREDENTIALS) {
							String message = getString(R.string.alert_signin_invalid_signin);
							AircandiCommon.showAlertDialog(R.drawable.icon_app, null, message,
									SignInForm.this, android.R.string.ok, null, new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int which) {}
									}, null);
							mTextPassword.setText("");
						}
						else {
							mCommon.handleServiceError(serviceResponse);
						}
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
			AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert, null,
					getResources().getString(R.string.alert_invalid_email), this, android.R.string.ok, null, null, null);
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