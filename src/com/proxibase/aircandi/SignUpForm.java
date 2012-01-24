package com.proxibase.aircandi;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.proxibase.aircandi.components.AircandiCommon;
import com.proxibase.aircandi.components.DateUtils;
import com.proxibase.aircandi.components.Exceptions;
import com.proxibase.aircandi.components.ImageRequest;
import com.proxibase.aircandi.components.ImageRequestBuilder;
import com.proxibase.aircandi.components.ImageUtils;
import com.proxibase.aircandi.components.Logger;
import com.proxibase.aircandi.components.NetworkManager;
import com.proxibase.aircandi.components.S3;
import com.proxibase.aircandi.components.Tracker;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.widgets.WebImageView;
import com.proxibase.sdk.android.proxi.consumer.User;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.Query;
import com.proxibase.sdk.android.proxi.service.ServiceRequest;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestListener;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;
import com.proxibase.sdk.android.util.ProxiConstants;

public class SignUpForm extends FormActivity {

	private EditText		mTextFullname;
	private EditText		mTextEmail;
	private EditText		mTextPassword;
	private EditText		mTextPasswordConfirm;
	private WebImageView	mImageUser;
	private Button			mButtonSignUp;
	private User			mUser;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);

		initialize();
		bind();
		draw();
		Tracker.trackPageView("/SignUpForm");

		if (savedInstanceState != null) {
			doRestoreInstanceState(savedInstanceState);
		}
	}

	protected void initialize() {
		mImageUser = (WebImageView) findViewById(R.id.image_picture);
		mTextFullname = (EditText) findViewById(R.id.text_fullname);
		mTextEmail = (EditText) findViewById(R.id.text_email);
		mTextPassword = (EditText) findViewById(R.id.text_password);
		mTextPasswordConfirm = (EditText) findViewById(R.id.text_password_confirm);
		mButtonSignUp = (Button) findViewById(R.id.btn_signup);
		mButtonSignUp.setEnabled(false);

		mTextFullname.addTextChangedListener(new SimpleTextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				mButtonSignUp.setEnabled(isValid());
			}
		});

		mTextEmail.addTextChangedListener(new SimpleTextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				mButtonSignUp.setEnabled(isValid());
			}

		});

		mTextPassword.addTextChangedListener(new SimpleTextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				mButtonSignUp.setEnabled(isValid());
			}

		});

		mTextPasswordConfirm.addTextChangedListener(new SimpleTextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				mButtonSignUp.setEnabled(isValid());
			}

		});
	}

	protected void bind() {
		mUser = new User();
		mUser.imageUri = (String) mImageUser.getTag();
		Tracker.dispatch();
	}

	protected void draw() {
		/*
		 * We only want to enable the save button when there is something in all
		 * the required fields: fullname, email, password
		 */
		if (mUser.imageUri != null && !mUser.imageUri.equals("")) {
			if (mUser.imageBitmap != null) {
				ImageUtils.showImageInImageView(mUser.imageBitmap, mImageUser.getImageView());
			}
			else {

				ImageRequestBuilder builder = new ImageRequestBuilder(mImageUser);
				builder.setFromUris(mUser.imageUri, mUser.linkUri);
				builder.setRequestListener(new RequestListener() {

					@Override
					public void onComplete(Object response) {
						ServiceResponse serviceResponse = (ServiceResponse) response;
						if (serviceResponse.responseCode == ResponseCode.Success) {
							mUser.imageBitmap = (Bitmap) serviceResponse.data;
						}
					}
				});

				ImageRequest imageRequest = builder.create();
				mImageUser.setImageRequest(imageRequest, null);
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onSignUpButtonClick(View view) {
		mCommon.startTitlebarProgress();
		doSave();
	}

	public void onChangePictureButtonClick(View view) {

		showChangePictureDialog(true, mImageUser, new RequestListener() {

			@Override
			public void onComplete(Object response, Object extra) {

				ServiceResponse serviceResponse = (ServiceResponse) response;
				if (serviceResponse.responseCode == ResponseCode.Success) {
					Bitmap bitmap = (Bitmap) serviceResponse.data;
					String imageUri = (String) extra;
					mUser.imageUri = imageUri;
					mUser.imageBitmap = bitmap;
				}
			}
		});
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	protected void doSave() {
		if (!validate()) {
			return;
		}
		insert();
	}

	private boolean validate() {
		if (!mTextPassword.getText().toString().equals(mTextPasswordConfirm.getText().toString())) {
			AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert, getResources().getString(
					R.string.signup_alert_missmatched_passwords_title),
					getResources().getString(R.string.signup_alert_missmatched_passwords_message), this, null);
			return false;
		}
		return true;
	}

	protected void insert() {

		mUser.email = mTextEmail.getText().toString().trim();
		mUser.fullname = mTextFullname.getText().toString().trim();
		mUser.password = mTextPassword.getText().toString().trim();
		mUser.createdDate = (int) (DateUtils.nowDate().getTime() / 1000L);

		Logger.i(this, "Insert user: " + mUser.fullname);

		ServiceRequest serviceRequest = new ServiceRequest();
		serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_ODATA + mUser.getCollection());
		serviceRequest.setRequestType(RequestType.Insert);
		serviceRequest.setRequestBody(ProxibaseService.convertObjectToJson((Object) mUser, GsonType.ProxibaseService));
		serviceRequest.setResponseFormat(ResponseFormat.Json);
		serviceRequest.setRequestListener(new RequestListener() {

			@Override
			public void onComplete(Object response) {

				ServiceResponse serviceResponse = (ServiceResponse) response;
				if (serviceResponse.responseCode == ResponseCode.Success) {

					/* Load the just insert user to get the user id */
					Query query = new Query("Users").filter("Email eq '" + mUser.email + "'");
					serviceResponse = NetworkManager.getInstance().request(
							new ServiceRequest(ProxiConstants.URL_AIRCANDI_SERVICE_ODATA, query, RequestType.Get, ResponseFormat.Json));

					if (serviceResponse.responseCode == ResponseCode.Success) {

						/* Jayma: We might have succeeded inserting user but not the user picture */
						String jsonResponse = (String) serviceResponse.data;
						mUser = (User) ProxibaseService.convertJsonToObject(jsonResponse, User.class, GsonType.ProxibaseService);

						/* Upload images to S3 as needed. */
						if (mUser.imageUri != null && !mUser.imageUri.contains("resource:") && mUser.imageBitmap != null) {
							String imageKey = String.valueOf(mUser.id) + "_"
														+ String.valueOf(DateUtils.nowString(DateUtils.DATE_NOW_FORMAT_FILENAME))
														+ ".jpg";
							try {
								S3.putImage(imageKey, mUser.imageBitmap);
							}
							catch (ProxibaseException exception) {
								if (!Exceptions.Handle(exception)) {
									ImageUtils.showToastNotification(getString(R.string.alert_update_failed), Toast.LENGTH_SHORT);
								}
							}
							mUser.imageUri = CandiConstants.URL_AIRCANDI_MEDIA + CandiConstants.S3_BUCKET_IMAGES + "/" + imageKey;

							/* Need to update the user to capture the uri for the image we saved */
							ServiceRequest serviceRequest = new ServiceRequest();
							serviceRequest.setUri(mUser.getEntryUri());
							serviceRequest.setRequestType(RequestType.Update);
							serviceRequest.setRequestBody(ProxibaseService.convertObjectToJson((Object) mUser, GsonType.ProxibaseService));
							serviceRequest.setResponseFormat(ResponseFormat.Json);

							/* Doing an insert so we don't need anything back */
							NetworkManager.getInstance().request(serviceRequest);
						}

						Logger.i(SignUpForm.this, "Inserted new user: " + mUser.fullname + " (" + mUser.id + ")");
						mCommon.stopTitlebarProgress();
						AircandiCommon.showAlertDialog(R.drawable.icon_app, getResources().getString(R.string.signup_alert_new_user_title),
										getResources().getString(R.string.signup_alert_new_user_message),
										SignUpForm.this, new OnClickListener() {

											public void onClick(DialogInterface dialog, int which) {
												setResult(CandiConstants.RESULT_PROFILE_INSERTED);
												finish();
											}
										});
					}
				}

			}
		});

		NetworkManager.getInstance().requestAsync(serviceRequest);
	}

	private boolean isValid() {
		/*
		 * Could be either a check for a new user or an update to an existing user
		 */
		if (mTextFullname.getText().length() == 0) {
			return false;
		}
		if (mTextEmail.getText().length() == 0) {
			return false;
		}
		if (mTextPassword.getText().length() == 0) {
			return false;
		}
		if (mTextPasswordConfirm.getText().length() == 0) {
			return false;
		}
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Persistence routines
	// --------------------------------------------------------------------------------------------

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		Logger.d(this, "onSaveInstanceState called");

		if (mTextFullname != null) {
			savedInstanceState.putString("fullname", mTextFullname.getText().toString());
		}
		if (mTextEmail != null) {
			savedInstanceState.putString("email", mTextEmail.getText().toString());
		}
	}

	private void doRestoreInstanceState(Bundle savedInstanceState) {
		Logger.d(this, "Restoring previous state");

		if (mTextFullname != null) {
			mTextFullname.setText(savedInstanceState.getString("fullname"));
		}
		if (mTextEmail != null) {
			mTextEmail.setText(savedInstanceState.getString("email"));
		}
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutID() {
		return R.layout.signup_form;
	}
}