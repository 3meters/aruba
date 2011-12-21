package com.proxibase.aircandi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.proxibase.aircandi.utils.NetworkManager;
import com.proxibase.aircandi.utils.NetworkManager.ResultCode;
import com.proxibase.aircandi.utils.NetworkManager.ServiceResponse;
import com.proxibase.sdk.android.proxi.consumer.Command;
import com.proxibase.sdk.android.proxi.consumer.User;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.Query;
import com.proxibase.sdk.android.proxi.service.ServiceRequest;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;
import com.proxibase.sdk.android.util.ProxiConstants;

public class SignInForm extends AircandiActivity {

	protected String	mMessage;

	private EditText	mTextEmail;
	private EditText	mTextPassword;
	private TextView	mTextMessage;
	private TextView	mTextError;
	private Button		mButtonSignIn;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		configure();
		draw();
		GoogleAnalyticsTracker.getInstance().trackPageView("/SignInForm");
		GoogleAnalyticsTracker.getInstance().dispatch();
	}

	@Override
	protected void unpackIntent(Intent intent) {
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mMessage = extras.getString(getString(R.string.EXTRA_MESSAGE));
		}
		super.unpackIntent(intent);
	}

	private void configure() {

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
		String json = ProxibaseService.getGson(GsonType.Internal).toJson(new Command("new"));
		Intent intent = new Intent(this, SignUpForm.class);
		intent.putExtra(getString(R.string.EXTRA_COMMAND), json);
		startActivity(intent);
	}

	public void onSignInButtonClick(View view) {
		mTextError.setVisibility(View.GONE);
		String email = mTextEmail.getText().toString();
		showProgressDialog(true, getResources().getString(R.string.alert_signing_in));
		Query query = new Query("Users").filter("Email eq '" + email + "'");

		ServiceResponse serviceResponse = NetworkManager.getInstance().request(
				new ServiceRequest(ProxiConstants.URL_AIRCANDI_SERVICE_ODATA, query, RequestType.Get, ResponseFormat.Json));

		if (serviceResponse.resultCode != ResultCode.Success) {
			setResult(Activity.RESULT_CANCELED);
			finish();
			overridePendingTransition(R.anim.hold, R.anim.fade_out_medium);
		}
		else {
			String jsonResponse = (String) serviceResponse.data;
			mUser = (User) ProxibaseService.convertJsonToObject(jsonResponse, User.class, GsonType.ProxibaseService);
			showProgressDialog(false, null);

			if (mUser == null) {
				mTextError.setVisibility(View.VISIBLE);
				mTextPassword.setText("");
			}
			else {
				Toast.makeText(this, getResources().getString(R.string.alert_signed_in) + " " + ((User) mUser).fullname, Toast.LENGTH_SHORT).show();
				Intent intent = new Intent();
				String jsonUser = ProxibaseService.getGson(GsonType.Internal).toJson(mUser);
				if (jsonUser.length() > 0) {
					intent.putExtra(getString(R.string.EXTRA_USER), jsonUser);
				}

				setResult(Activity.RESULT_FIRST_USER, intent);
				finish();
			}
		}
	}

	public void onCancelButtonClick(View view) {
		setResult(Activity.RESULT_CANCELED);
		finish();
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutID() {
		return R.layout.signin_form;
	}
}