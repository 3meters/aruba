package com.proxibase.aircandi;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.proxibase.aircandi.components.Command;
import com.proxibase.aircandi.components.ImageUtils;
import com.proxibase.aircandi.components.NetworkManager;
import com.proxibase.aircandi.components.Tracker;
import com.proxibase.aircandi.components.Command.CommandVerb;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.sdk.android.proxi.consumer.User;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.Query;
import com.proxibase.sdk.android.proxi.service.ServiceRequest;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;
import com.proxibase.sdk.android.util.ProxiConstants;

public class SignInForm extends FormActivity {

	protected String	mMessage;

	private EditText	mTextEmail;
	private EditText	mTextPassword;
	private TextView	mTextMessage;
	private TextView	mTextError;
	private Button		mButtonSignIn;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initialize();
		draw();
		Tracker.trackPageView("/SignInForm");
		Tracker.dispatch();
	}

	private void initialize() {

		mTextEmail = (EditText) findViewById(R.id.text_email);
		mTextPassword = (EditText) findViewById(R.id.text_password);
		mTextMessage = (TextView) findViewById(R.id.form_message);
		mTextError = (TextView) findViewById(R.id.text_signin_error);
		mButtonSignIn = (Button) findViewById(R.id.btn_signin);
		mButtonSignIn.setEnabled(false);
		mTextEmail.addTextChangedListener(new SimpleTextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				mButtonSignIn.setEnabled(s.length() > 0 && mTextPassword.getText().length() > 0);
			}
		});
		mTextPassword.addTextChangedListener(new SimpleTextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				mButtonSignIn.setEnabled(s.length() > 0 && mTextEmail.getText().length() > 0);
			}
		});
	}

	public void draw() {
		if (mMessage != null) {
			mTextMessage.setText(mMessage);
		}
		else {
			mTextMessage.setVisibility(View.GONE);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onSignUpButtonClick(View view) {
		String json = ProxibaseService.getGson(GsonType.Internal).toJson(new Command(CommandVerb.New));
		Intent intent = new Intent(this, SignUpForm.class);
		intent.putExtra(getString(R.string.EXTRA_COMMAND), json);
		startActivity(intent);
		overridePendingTransition(R.anim.form_in, R.anim.browse_out);

	}

	public void onSignInButtonClick(View view) {
		mTextError.setVisibility(View.GONE);
		final String email = mTextEmail.getText().toString();
		
		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showProgressDialog(true, "Signing in...");
			}

			@Override
			protected Object doInBackground(Object... params) {
				
				Query query = new Query("users").filter("{\"email\":\"" + email + "\"}");
				ServiceResponse serviceResponse = NetworkManager.getInstance().request(
						new ServiceRequest(ProxiConstants.URL_PROXIBASE_SERVICE, query, RequestType.Get, ResponseFormat.Json));
				return serviceResponse;
			}

			@Override
			protected void onPostExecute(Object response) {

				ServiceResponse serviceResponse = (ServiceResponse) response;
				mCommon.showProgressDialog(false, null);
				if (serviceResponse.responseCode == ResponseCode.Success) {
					String jsonResponse = (String) serviceResponse.data;
					User user = (User) ProxibaseService.convertJsonToObject(jsonResponse, User.class, GsonType.ProxibaseService);

					if (user == null) {
						mTextError.setVisibility(View.VISIBLE);
						mTextPassword.setText("");
					}
					else {
						Aircandi.getInstance().setUser(user);
						ImageUtils.showToastNotification(
								getResources().getString(R.string.alert_signed_in) + " " + Aircandi.getInstance().getUser().name, Toast.LENGTH_SHORT);

						Aircandi.settingsEditor.putString(Preferences.PREF_USER, jsonResponse);
						Aircandi.settingsEditor.commit();

						setResult(CandiConstants.RESULT_USER_SIGNED_IN);
						finish();
					}
				}
				else {
					mCommon.handleServiceError(serviceResponse);
				}
			}
		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutID() {
		return R.layout.signin_form;
	}
}