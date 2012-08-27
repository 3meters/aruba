package com.proxibase.aircandi;

import org.apache.http.HttpStatus;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.proxibase.aircandi.components.AircandiCommon;
import com.proxibase.aircandi.components.CommandType;
import com.proxibase.aircandi.components.DateUtils;
import com.proxibase.aircandi.components.Exceptions;
import com.proxibase.aircandi.components.ImageRequest;
import com.proxibase.aircandi.components.ProxiExplorer;
import com.proxibase.aircandi.components.ImageRequest.ImageResponse;
import com.proxibase.aircandi.components.ImageRequestBuilder;
import com.proxibase.aircandi.components.ImageUtils;
import com.proxibase.aircandi.components.Logger;
import com.proxibase.aircandi.components.NetworkManager;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ResponseCodeDetail;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.components.S3;
import com.proxibase.aircandi.components.Tracker;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.widgets.WebImageView;
import com.proxibase.service.ProxiConstants;
import com.proxibase.service.ProxibaseService;
import com.proxibase.service.ProxibaseService.GsonType;
import com.proxibase.service.ProxibaseService.RequestListener;
import com.proxibase.service.ProxibaseService.RequestType;
import com.proxibase.service.ProxibaseService.ResponseFormat;
import com.proxibase.service.ProxibaseServiceException;
import com.proxibase.service.Query;
import com.proxibase.service.ServiceRequest;
import com.proxibase.service.objects.ServiceData;
import com.proxibase.service.objects.User;

@SuppressWarnings("unused")
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
			AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert, getResources().getString(
					R.string.alert_signup_missmatched_passwords_title),
					getResources().getString(R.string.alert_signup_missmatched_passwords_message), this, android.R.string.ok, null, null, null);
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

				Bundle parameters = new Bundle();
				ServiceRequest serviceRequest = new ServiceRequest();

				parameters.putString("user", "object:{"
						+ "\"_id\":\"" + mUser.id + "\","
						+ "\"oldPassword\":\"" + mTextPasswordOld.getText().toString() + "\","
						+ "\"newPassword\":\"" + mTextPassword.getText().toString() + "\""
						+ "}");

				serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_USER + "changepw")
						.setRequestType(RequestType.Method)
						.setParameters(parameters)
						.setSession(Aircandi.getInstance().getUser().session)
						.setResponseFormat(ResponseFormat.Json);

				ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);

				return serviceResponse;
			}

			@Override
			protected void onPostExecute(Object response) {

				ServiceResponse serviceResponse = (ServiceResponse) response;
				mCommon.showProgressDialog(false, null);
				if (serviceResponse.responseCode == ResponseCode.Success) {
					
					Logger.i(this, "User changed password: " + Aircandi.getInstance().getUser().name + " (" +  Aircandi.getInstance().getUser().id + ")");
					Tracker.trackEvent("User", "PasswordChange", null, 0);
					ImageUtils.showToastNotification(getResources().getString(R.string.alert_password_changed)
							+ " " + Aircandi.getInstance().getUser().name, Toast.LENGTH_SHORT);
					finish();
				}
				else {

					/*
					 * This could have been caused any problem while inserting/updating the user and the user image.
					 * We look first for ones that are known responses from the service.
					 * 
					 * - 403.x: password not strong enough
					 * - 403.x: email not unique
					 * - 401.2: expired session
					 */
					String jsonResponse = serviceResponse.exception.getResponseMessage();
					ServiceData serviceData = ProxibaseService.convertJsonToObject(jsonResponse, ServiceData.class, GsonType.ProxibaseService);
					if (serviceData.error != null) {
						String message = null;
						float errorCode = serviceData.error.code.floatValue();
						if (errorCode == ProxiConstants.HTTP_STATUS_CODE_UNAUTHORIZED_CREDENTIALS) {
							message = getString(R.string.alert_change_password_unauthorized);
						}
						else if (errorCode == ProxiConstants.HTTP_STATUS_CODE_FORBIDDEN_USER_PASSWORD_WEAK) {
							message = getString(R.string.alert_signup_password_weak);
						}
						AircandiCommon.showAlertDialog(R.drawable.icon_app, null, message,
								PasswordForm.this, android.R.string.ok, null, new DialogInterface.OnClickListener() {
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