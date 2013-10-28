package com.aircandi.ui.user;

import java.util.Locale;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.EntityManager;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NotificationManager;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.service.objects.LinkOptions;
import com.aircandi.service.objects.LinkOptions.LinkProfile;
import com.aircandi.service.objects.ServiceData;
import com.aircandi.service.objects.User;
import com.aircandi.ui.base.BaseEdit;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.Animate.TransitionType;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Json;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.Routing.Route;
import com.aircandi.utilities.UI;
import com.aircandi.utilities.Utilities;

public class SignInEdit extends BaseEdit {

	private EditText		mEmail;
	private EditText		mPassword;
	private TextView		mMessage;
	private CheckBox		mPasswordUnmask;

	private String			mFormMessage;
	private PendingIntent	mPendingIntent;

	@Override
	public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mFormMessage = extras.getString(Constants.EXTRA_MESSAGE);
			mPendingIntent = (PendingIntent) extras.getParcelable("pendingIntent");
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mEmail = (EditText) findViewById(R.id.email);
		mPassword = (EditText) findViewById(R.id.password);
		mMessage = (TextView) findViewById(R.id.message);
		mPasswordUnmask = (CheckBox) findViewById(R.id.chk_unmask);

		mPasswordUnmask.setOnCheckedChangeListener(new OnCheckedChangeListener() {

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
			}
		});

		mPassword.setImeOptions(EditorInfo.IME_ACTION_GO);
		mPassword.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_GO) {
					signin();
					return true;
				}
				return false;
			}
		});
	}

	@Override
	public void draw() {
		if (mMessage != null) {
			mMessage.setText(mFormMessage);
		}
		final String email = Aircandi.settings.getString(Constants.SETTING_LAST_EMAIL, null);
		if (email != null) {
			mEmail.setText(email);
			mPassword.requestFocus();
		}
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("ucd")
	public void onSendPasswordButtonClick(View view) {
		Dialogs.sendPassword(this);
	}

	@SuppressWarnings("ucd")
	public void onSignInButtonClick(View view) {
		signin();
	}

	@SuppressWarnings("ucd")
	public void onSignupButtonClick(View view) {
		Routing.route(this, Route.REGISTER);
	}

	@Override
	public void onAccept() {
		if (validate()) {
			signin();
		}
	}

	private void signin() {

		final String email = mEmail.getText().toString().toLowerCase(Locale.US);
		final String password = mPassword.getText().toString();

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusyManager.showBusy(R.string.progress_signing_in);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("SignIn");
				
				ModelResult result = EntityManager.getInstance().signin(email, password, SignInEdit.class.getSimpleName());
				
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {
					
					final String jsonResponse = (String) result.serviceResponse.data;
					final ServiceData serviceData = (ServiceData) Json.jsonToObject(jsonResponse, Json.ObjectType.NONE, Json.ServiceDataWrapper.TRUE);
					Aircandi.getInstance().setCurrentUser(serviceData.user);
					Aircandi.getInstance().getCurrentUser().session = serviceData.session;
					Logger.i(this, "User signed in: " + Aircandi.getInstance().getCurrentUser().name);
					
					/* Load user data */
					LinkOptions options = LinkOptions.getDefault(LinkProfile.LINKS_FOR_USER_CURRENT);
					result = EntityManager.getInstance().getEntity(Aircandi.getInstance().getCurrentUser().id, true, options);
					
					/* Turn notifications on */
					NotificationManager.getInstance().registerDeviceWithAircandi(); // might fail but we eat it
				}
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {

				final ModelResult result = (ModelResult) response;
				hideBusy();
				if (result.serviceResponse.responseCode == ResponseCode.SUCCESS) {

					UI.showToastNotification(getResources().getString(R.string.alert_signed_in)
							+ " " + Aircandi.getInstance().getCurrentUser().name, Toast.LENGTH_SHORT);
					
					User user = Aircandi.getInstance().getCurrentUser();

					final String jsonUser = Json.objectToJson(user);
					final String jsonSession = Json.objectToJson(user.session);

					Aircandi.settingsEditor.putString(Constants.SETTING_USER, jsonUser);
					Aircandi.settingsEditor.putString(Constants.SETTING_USER_SESSION, jsonSession);
					Aircandi.settingsEditor.putString(Constants.SETTING_LAST_EMAIL, user.email);
					Aircandi.settingsEditor.commit();

					if (mPendingIntent != null) {
						try {
							mPendingIntent.send();
							finish();
							Animate.doOverridePendingTransition(SignInEdit.this, TransitionType.FORM_TO_PAGE);
						}
						catch (CanceledException exception) {
							exception.printStackTrace();
						}
					}

					setResultCode(Constants.RESULT_USER_SIGNED_IN);
					finish();
					Animate.doOverridePendingTransition(SignInEdit.this, TransitionType.FORM_TO_PAGE);
				}
				else {
					Errors.handleError(SignInEdit.this, result.serviceResponse);
				}
			}
		}.execute();
	}

	@Override
	protected boolean validate() {
		if (mPassword.getText().length() < 6) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_missing_password)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}
		if (!Utilities.validEmail(mEmail.getText().toString())) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_invalid_email)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}
		return true;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == Constants.ACTIVITY_SIGNIN) {
			if (resultCode == Constants.RESULT_USER_SIGNED_IN) {
				setResultCode(Constants.RESULT_USER_SIGNED_IN);
				finish();
				Animate.doOverridePendingTransition(SignInEdit.this, TransitionType.FORM_TO_PAGE);
			}
		}
		super.onActivityResult(requestCode, resultCode, intent);
	}

	//--------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.signin_edit;
	}

}