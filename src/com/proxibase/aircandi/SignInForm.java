package com.proxibase.aircandi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;

public class SignInForm extends EntityBaseForm {

	private EditText	mUsername;
	private EditText	mPassword;
	private Button		mSigninButton;
	private TextView	mSigninError;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		bindEntity();
		drawEntity();
	}

	@Override
	public void drawEntity() {
		super.drawEntity();
		if (findViewById(R.id.txt_message) != null) {
			if (mMessage != null) {
				((TextView) findViewById(R.id.txt_message)).setText(mMessage);
			}
			else {
				((TextView) findViewById(R.id.txt_message)).setVisibility(View.GONE);
			}
		}
		if (findViewById(R.id.btn_signin) != null) {
			mSigninButton = (Button) findViewById(R.id.btn_signin);
		}
		if (findViewById(R.id.txt_username) != null) {
			mUsername = (EditText) findViewById(R.id.txt_username);
			mUsername.addTextChangedListener(new TextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					mSigninButton.setEnabled(s.length() > 0 && mPassword.getText().length() > 0);
				}

				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				}

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
				}
			});
		}
		if (findViewById(R.id.txt_password) != null) {
			mPassword = (EditText) findViewById(R.id.txt_password);
			mPassword.addTextChangedListener(new TextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					mSigninButton.setEnabled(s.length() > 0 && mUsername.getText().length() > 0);
				}

				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				}

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
				}
			});
		}
		if (findViewById(R.id.txt_signin_error) != null) {
			mSigninError = (TextView) findViewById(R.id.txt_signin_error);
		}
	}

	public void onSignUpButtonClick(View view) {
		Toast.makeText(this, "Not implemented yet.", Toast.LENGTH_SHORT).show();
	}

	public void onSignInButtonClick(View view) {
		try {
			mSigninError.setVisibility(View.GONE);
			String username = (String) ((EditText) findViewById(R.id.txt_username)).getText().toString();
			showProgressDialog(true, "Signing in...");
			mUser = ProxibaseService.getInstance().loadUser(username);
		}
		catch (ProxibaseException exception) {
			exception.printStackTrace();
		}
		finally {
			showProgressDialog(false, null);
		}

		if (mUser == null) {
			mSigninError.setVisibility(View.VISIBLE);
			mPassword.setText("");
		}
		else {
			Toast.makeText(this, "Signed in as " + mUser.fullname, Toast.LENGTH_SHORT).show();
			Intent intent = new Intent();
			String jsonUser = ProxibaseService.getGson(GsonType.Internal).toJson(mUser);
			if (!jsonUser.equals("")) {
				intent.putExtra(getString(R.string.EXTRA_USER), jsonUser);
			}

			setResult(Activity.RESULT_FIRST_USER, intent);
			finish();
		}
	}

	@Override
	public void onCancelButtonClick(View view) {
		setResult(Activity.RESULT_CANCELED);
		finish();
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutID() {
		return R.layout.signin_form;
	}
}