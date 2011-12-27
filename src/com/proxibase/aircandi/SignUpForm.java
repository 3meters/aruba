package com.proxibase.aircandi;

import android.app.Activity;
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

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.proxibase.aircandi.components.DateUtils;
import com.proxibase.aircandi.components.Exceptions;
import com.proxibase.aircandi.components.ImageUtils;
import com.proxibase.aircandi.components.Logger;
import com.proxibase.aircandi.components.NetworkManager;
import com.proxibase.aircandi.components.S3;
import com.proxibase.aircandi.components.ImageManager.ImageRequest;
import com.proxibase.aircandi.components.ImageManager.ImageRequest.ImageShape;
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

public class SignUpForm extends AircandiActivity {

	private EditText		mTextFullname;
	private EditText		mTextEmail;
	private EditText		mTextPassword;
	private EditText		mTextPasswordConfirm;
	private WebImageView	mImageUser;
	private Button			mButtonSignUp;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);

		configure();
		bind();
		draw();
		GoogleAnalyticsTracker.getInstance().trackPageView("/SignUpForm");
	}

	protected void configure() {
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
		GoogleAnalyticsTracker.getInstance().dispatch();

	}

	protected void draw() {
		/*
		 * We only want to enable the save button when there is something in all
		 * the required fields: fullname, email, password
		 */
		if (mUser.imageUri != null && mUser.imageUri.length() > 0) {
			if (mUser.imageBitmap != null) {
				mImageUser.setImageBitmap(mUser.imageBitmap);
			}
			else {
				ImageRequest imageRequest = new ImageRequest(mUser.imageUri, ImageShape.Square, "binary", false,
						CandiConstants.IMAGE_WIDTH_SEARCH_MAX, false, true, true, 1, this, new RequestListener() {

							@Override
							public void onComplete(Object response) {
								ServiceResponse serviceResponse = (ServiceResponse) response;
								if (serviceResponse.responseCode == ResponseCode.Success) {
									mUser.imageBitmap = (Bitmap) serviceResponse.data;
								}
							}
						});

				mImageUser.setImageRequest(imageRequest, null);
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onSignUpButtonClick(View view) {
		startTitlebarProgress();
		doSave();
	}

	public void onCancelButtonClick(View view) {
		setResult(Activity.RESULT_CANCELED);
		finish();
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
		insertUser();
	}

	private boolean validate() {
		if (!mTextPassword.getText().toString().equals(mTextPasswordConfirm.getText().toString())) {
			Aircandi.showAlertDialog(android.R.drawable.ic_dialog_alert, getResources().getString(R.string.signup_alert_missmatched_passwords_title),
					getResources().getString(R.string.signup_alert_missmatched_passwords_message), this, null);
			return false;
		}
		return true;
	}

	protected void insertUser() {

		mUser.email = mTextEmail.getText().toString().trim();
		mUser.fullname = mTextFullname.getText().toString().trim();
		mUser.password = mTextPassword.getText().toString().trim();
		mUser.createdDate = DateUtils.nowString();

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
				if (serviceResponse.responseCode != ResponseCode.Success) {
					ImageUtils.showToastNotification(getString(R.string.alert_insert_failed), Toast.LENGTH_SHORT);
					return;
				}
				else {
					/* Load the just insert user to get the user id */
					Query query = new Query("Users").filter("Email eq '" + mUser.email + "'");
					serviceResponse = NetworkManager.getInstance().request(
							new ServiceRequest(ProxiConstants.URL_AIRCANDI_SERVICE_ODATA, query, RequestType.Get, ResponseFormat.Json));

					if (serviceResponse.responseCode != ResponseCode.Success) {
						/* Jayma: We might have succeeded inserting user but not the user picture */
						return;
					}
					else
					{
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
						stopTitlebarProgress();
						Aircandi.showAlertDialog(R.drawable.icon_app, getResources().getString(R.string.signup_alert_new_user_title),
										getResources().getString(R.string.signup_alert_new_user_message),
										SignUpForm.this, new OnClickListener() {

											public void onClick(DialogInterface dialog, int which) {
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
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutID() {
		return R.layout.signup_form;
	}
}