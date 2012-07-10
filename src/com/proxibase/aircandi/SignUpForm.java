package com.proxibase.aircandi;

import org.apache.http.HttpStatus;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.proxibase.aircandi.components.AircandiCommon;
import com.proxibase.aircandi.components.DateUtils;
import com.proxibase.aircandi.components.ImageRequest;
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
import com.proxibase.service.ServiceRequest;
import com.proxibase.service.objects.User;

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
		super.onCreate(savedInstanceState);

		initialize();
		bind();
		draw();

		if (savedInstanceState != null) {
			doRestoreInstanceState(savedInstanceState);
		}
	}

	protected void initialize() {
		mCommon.track();
		mCommon.mActionBar.setTitle(R.string.form_title_signup);
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
	}

	protected void draw() {
		/*
		 * We only want to enable the save button when there is something in all
		 * the required fields: fullname, email, password
		 */
		if (mUser.imageUri != null && !mUser.imageUri.equals("")) {
			if (mUser.imageBitmap != null) {
				ImageUtils.showImageInImageView(mUser.imageBitmap, mImageUser.getImageView(), true, R.anim.fade_in_medium);
			}
			else {

				ImageRequestBuilder builder = new ImageRequestBuilder(mImageUser);
				builder.setFromUris(mUser.imageUri, mUser.linkUri);
				builder.setRequestListener(new RequestListener() {

					@Override
					public void onComplete(Object response) {
						ServiceResponse serviceResponse = (ServiceResponse) response;
						if (serviceResponse.responseCode == ResponseCode.Success) {
							ImageResponse imageResponse = (ImageResponse) serviceResponse.data;
							mUser.imageBitmap = imageResponse.bitmap;
						}
					}
				});

				ImageRequest imageRequest = builder.create();
				mImageUser.setImageRequest(imageRequest);
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

	public void onViewTermsButtonClick(View view) {

		AircandiCommon.showAlertDialog(R.drawable.icon_app
				, getResources().getString(R.string.alert_terms_title)
				, getResources().getString(R.string.alert_terms_message)
				, SignUpForm.this, android.R.string.ok, null, null);
	}
	
	public void onChangePictureButtonClick(View view) {

		showChangePictureDialog(true, mImageUser, new RequestListener() {

			@Override
			public void onComplete(Object response) {

				ServiceResponse serviceResponse = (ServiceResponse) response;
				if (serviceResponse.responseCode == ResponseCode.Success) {
					ImageResponse imageResponse = (ImageResponse) serviceResponse.data;
					String imageUri = (String) imageResponse.imageUri;
					mUser.imageUri = imageUri;
					mUser.imageBitmap = imageResponse.bitmap;
				}
			}
		});
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	protected void doSave() {
		if (validate()) {
			insert();
		}
	}

	private boolean validate() {
		if (!mTextPassword.getText().toString().equals(mTextPasswordConfirm.getText().toString())) {
			AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert, getResources().getString(
					R.string.signup_alert_missmatched_passwords_title),
					getResources().getString(R.string.signup_alert_missmatched_passwords_message), this, android.R.string.ok, null, null);
			mTextPasswordConfirm.setText("");
			return false;
		}
		return true;
	}

	protected void insert() {

		mUser.email = mTextEmail.getText().toString().trim().toLowerCase();
		mUser.name = mTextFullname.getText().toString().trim();
		mUser.password = mTextPassword.getText().toString().trim();
		mUser.createdDate = DateUtils.nowDate().getTime();
		mUser.modifiedDate = mUser.createdDate;

		Logger.i(this, "Insert user: " + mUser.name);

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showProgressDialog(true, getString(R.string.progress_signing_up));
			}

			@Override
			protected Object doInBackground(Object... params) {

				ServiceRequest serviceRequest = new ServiceRequest();
				serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE + mUser.getCollection())
						.setRequestType(RequestType.Insert)
						.setRequestBody(ProxibaseService.convertObjectToJson((Object) mUser, GsonType.ProxibaseService))
						.setResponseFormat(ResponseFormat.Json);

				/* 
				 * Insert user.
				 */
				ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);

				if (serviceResponse.responseCode == ResponseCode.Success) {

					String jsonResponse = (String) serviceResponse.data;
					User userStub = (User) ProxibaseService.convertJsonToObject(jsonResponse, User.class, GsonType.ProxibaseService).data;
					mUser.id = userStub.id;
					/* 
					 * Upload images to S3 as needed. 
					 */
					if (mUser.imageUri != null && !mUser.imageUri.contains("resource:") && mUser.imageBitmap != null) {
						String imageKey = String.valueOf(mUser.id) + "_"
								+ String.valueOf(DateUtils.nowString(DateUtils.DATE_NOW_FORMAT_FILENAME))
								+ ".jpg";
						try {
							S3.putImage(imageKey, mUser.imageBitmap);
						}
						catch (ProxibaseServiceException exception) {
							serviceResponse = new ServiceResponse(ResponseCode.Failed, ResponseCodeDetail.UnknownException, null, exception);
						}

						if (serviceResponse.responseCode == ResponseCode.Success) {
							/*
							 * Update user.
							 *  
							 * Need to update the user to capture the uri for the image we saved. Must strip out the password
							 * because service only allows password updates through a different service call. 
							 */
							mUser.imageUri = CandiConstants.URL_AIRCANDI_MEDIA + CandiConstants.S3_BUCKET_IMAGES + "/" + imageKey;
							mUser.creatorId = mUser.id;
							mUser.ownerId = mUser.id;
							mUser.modifierId = mUser.id;
							mUser.password = null;
							serviceRequest = new ServiceRequest();
							serviceRequest.setUri(mUser.getEntryUri())
									.setRequestType(RequestType.Update)
									.setRequestBody(ProxibaseService.convertObjectToJson((Object) mUser, GsonType.ProxibaseService))
									.setResponseFormat(ResponseFormat.Json);

							/* Doing an insert so we don't need anything back */
							serviceResponse = NetworkManager.getInstance().request(serviceRequest);
						}
					}
				}
				return serviceResponse;
			}

			@Override
			protected void onPostExecute(Object response) {

				ServiceResponse serviceResponse = (ServiceResponse) response;
				if (serviceResponse.responseCode == ResponseCode.Success) {

					Tracker.trackEvent("User", "Insert", null, 0);
					mCommon.showProgressDialog(false, null);
					Logger.i(SignUpForm.this, "Inserted new user: " + mUser.name + " (" + mUser.id + ")");

					AircandiCommon.showAlertDialog(R.drawable.icon_app, getResources().getString(R.string.signup_alert_new_user_title),
							getResources().getString(R.string.signup_alert_new_user_message),
							SignUpForm.this, android.R.string.ok, null, new OnClickListener() {

								public void onClick(DialogInterface dialog, int which) {
									setResult(CandiConstants.RESULT_PROFILE_INSERTED);
									finish();
								}
							});
				}
				else {
					/*
					 * This could have been caused any problem while inserting/updating the user and the user image. We look
					 * first for ones that are known responses from the service.
					 * 
					 * - 400: password not strong enough
					 * - 400: email not valid
					 * - 400: email not unique
					 */
					if (serviceResponse.exception.getHttpStatusCode() == HttpStatus.SC_BAD_REQUEST) {
						String message = serviceResponse.exception.getResponseMessage();
						//String message = getString(R.string.signin_alert_invalid_signin);
						AircandiCommon.showAlertDialog(R.drawable.icon_app, null, message,
								SignUpForm.this, android.R.string.ok, null, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {}
								});
					}
					else {
						mCommon.handleServiceError(serviceResponse);
					}
				}
			}
		}.execute();

	}

	private boolean isValid() {
		/*
		 * Client validation logic is handle here. The service may still reject based on
		 * additional validation rules.
		 */
		if (mTextFullname.getText().length() == 0) {
			return false;
		}
		if (mTextEmail.getText().length() == 0) {
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