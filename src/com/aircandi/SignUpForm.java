package com.aircandi;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.aircandi.components.AircandiCommon;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.AnimUtils;
import com.aircandi.components.AnimUtils.TransitionType;
import com.aircandi.components.ImageRequest;
import com.aircandi.components.ImageRequest.ImageResponse;
import com.aircandi.components.ImageRequestBuilder;
import com.aircandi.components.ImageUtils;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.ProxiExplorer.ModelResult;
import com.aircandi.components.Tracker;
import com.aircandi.components.Utilities;
import com.aircandi.core.CandiConstants;
import com.aircandi.service.ProxibaseService.RequestListener;
import com.aircandi.service.objects.User;
import com.aircandi.widgets.WebImageView;

public class SignUpForm extends FormActivity {

	private EditText		mTextFullname;
	private EditText		mTextEmail;
	private EditText		mTextPassword;
	private EditText		mTextPasswordConfirm;
	private WebImageView	mImageUser;
	private Button			mButtonSignUp;
	private User			mUser;
	private Bitmap			mUserBitmap;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initialize();
		bind();
		draw();
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
		mUser.imageUri = "resource:placeholder_logo";
	}

	protected void draw() {
		/*
		 * We only want to enable the save button when there is something in all
		 * the required fields: fullname, email, password
		 */
		if (mUser.imageUri != null && !mUser.imageUri.equals("")) {
			if (mUserBitmap != null) {
				ImageUtils.showImageInImageView(mUserBitmap, mImageUser.getImageView(), true, AnimUtils.fadeInMedium());
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
							mUserBitmap = imageResponse.bitmap;
						}
						else {
							mImageUser.getImageView().setImageResource(R.drawable.image_broken);
							mCommon.handleServiceError(serviceResponse, ServiceOperation.Signup);
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
		doSave();
	}

	public void onViewTermsButtonClick(View view) {
		doViewTerms();
	}

	public void onChangePictureButtonClick(View view) {

		showChangePictureDialog(true, mImageUser, new RequestListener() {

			@Override
			public void onComplete(Object response, String imageUri, String linkUri, Bitmap imageBitmap, String title, String description) {

				ServiceResponse serviceResponse = (ServiceResponse) response;
				if (serviceResponse.responseCode == ResponseCode.Success) {
					mUser.imageUri = imageUri;
					mUser.linkUri = linkUri;
					mUserBitmap = imageBitmap;
				}
			}
		});
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	protected void doViewTerms() {

		Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
		intent.setData(Uri.parse(CandiConstants.URL_AIRCANDI_TERMS));
		startActivity(intent);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);

	}

	protected void doSave() {
		insert();
	}

	private boolean validate() {
		if (!Utilities.validEmail(mTextEmail.getText().toString())) {
			AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_invalid_email)
					, null
					, this
					, android.R.string.ok
					, null, null, null);
			return false;
		}
		if (!mTextPassword.getText().toString().equals(mTextPasswordConfirm.getText().toString())) {
			AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert
					, getResources().getString(R.string.error_signup_missmatched_passwords_title)
					, getResources().getString(R.string.error_signup_missmatched_passwords_message)
					, null
					, this
					, android.R.string.ok
					, null, null, null);
			mTextPasswordConfirm.setText("");
			return false;
		}
		return true;
	}

	protected void insert() {

		if (validate()) {

			mUser.email = mTextEmail.getText().toString().trim().toLowerCase();
			mUser.name = mTextFullname.getText().toString().trim();
			mUser.password = mTextPassword.getText().toString().trim();

			Logger.d(this, "Inserting user: " + mUser.name);

			new AsyncTask() {

				@Override
				protected void onPreExecute() {
					mCommon.showProgressDialog(true, getString(R.string.progress_signing_up));
				}

				@Override
				protected Object doInBackground(Object... params) {
					ModelResult result = ProxiExplorer.getInstance().getEntityModel().insertUser(mUser, mUserBitmap);
					return result;
				}

				@Override
				protected void onPostExecute(Object response) {
					ModelResult result = (ModelResult) response;

					if (result.serviceResponse.responseCode == ResponseCode.Success) {

						Tracker.trackEvent("User", "Insert", null, 0);
						mCommon.showProgressDialog(false, null);
						Logger.i(SignUpForm.this, "Inserted new user: " + mUser.name + " (" + mUser.id + ")");

						AircandiCommon.showAlertDialog(R.drawable.icon_app
								, getResources().getString(R.string.alert_signup_new_user_title)
								, getResources().getString(R.string.alert_signup_new_user_message)
								, null
								, SignUpForm.this, android.R.string.ok, null, new OnClickListener() {

									public void onClick(DialogInterface dialog, int which) {
										setResult(CandiConstants.RESULT_PROFILE_INSERTED);
										finish();
									}
								}, null);
					}
					else {
						mTextPassword.setText("");
						mCommon.handleServiceError(result.serviceResponse, ServiceOperation.Signup);
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
	// Lifecycle events
	// --------------------------------------------------------------------------------------------

	protected void onDestroy() {

		/* This activity gets destroyed everytime we leave using back or finish(). */
		super.onDestroy();
		if (mUserBitmap != null && !mUserBitmap.isRecycled()) {
			mUserBitmap.recycle();
			mUserBitmap = null;
		}
		System.gc();
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutID() {
		return R.layout.signup_form;
	}
}