package com.aircandi.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.aircandi.Aircandi;
import com.aircandi.CandiConstants;
import com.aircandi.R;
import com.aircandi.components.AircandiCommon;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.FontManager;
import com.aircandi.components.ImageRequest;
import com.aircandi.components.ImageRequest.ImageResponse;
import com.aircandi.components.ImageRequestBuilder;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.ProxiExplorer.ModelResult;
import com.aircandi.components.Tracker;
import com.aircandi.service.ProxibaseService.RequestListener;
import com.aircandi.service.objects.User;
import com.aircandi.ui.base.FormActivity;
import com.aircandi.ui.widgets.WebImageView;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;
import com.aircandi.utilities.ImageUtils;
import com.aircandi.utilities.MiscUtils;

public class RegisterForm extends FormActivity {

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
		mButtonSignUp = (Button) findViewById(R.id.button_register);
		mButtonSignUp.setEnabled(true);
		
		FontManager.getInstance().setTypefaceDefault(mTextFullname);
		FontManager.getInstance().setTypefaceDefault(mTextEmail);
		FontManager.getInstance().setTypefaceDefault(mTextPassword);
		FontManager.getInstance().setTypefaceDefault(mTextPasswordConfirm);
		FontManager.getInstance().setTypefaceDefault(mButtonSignUp);
		
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.terms));
		FontManager.getInstance().setTypefaceDefault((Button) findViewById(R.id.button_change_image));
		FontManager.getInstance().setTypefaceDefault((Button) findViewById(R.id.button_view_terms));
		FontManager.getInstance().setTypefaceDefault((Button) findViewById(R.id.button_cancel));
		
	}

	protected void bind() {
		mUser = new User();
		mUser.getPhoto().setImageUri("resource:placeholder_logo");
	}

	protected void draw() {
		/*
		 * We only want to enable the save button when there is something in all
		 * the required fields: fullname, email, password
		 */
		if (mUser.getImageUri() != null && !mUser.getImageUri().equals("")) {
			if (mUserBitmap != null) {
				ImageUtils.showImageInImageView(mUserBitmap, mImageUser.getImageView(), true, AnimUtils.fadeInMedium());
			}
			else {

				ImageRequestBuilder builder = new ImageRequestBuilder(mImageUser);
				builder.setFromUris(mUser.getImageUri(), null);
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

		showChangePictureDialog(false, null, null, null, mImageUser, new RequestListener() {

			@Override
			public void onComplete(Object response, String imageUri, String linkUri, Bitmap imageBitmap, String title, String description) {

				ServiceResponse serviceResponse = (ServiceResponse) response;
				if (serviceResponse.responseCode == ResponseCode.Success) {
					mUser.getPhoto().setImageUri(imageUri);
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
		if (mTextFullname.getText().length() == 0) {
			AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_missing_fullname)
					, null
					, this
					, android.R.string.ok
					, null, null, null);
			return false;
		}
		if (mTextEmail.getText().length() == 0) {
			AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_missing_email)
					, null
					, this
					, android.R.string.ok
					, null, null, null);
			return false;
		}
		if (mTextPassword.getText().length() < 6) {
			AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_missing_password)
					, null
					, this
					, android.R.string.ok
					, null, null, null);
			return false;
		}
		if (mTextPasswordConfirm.getText().length() < 6) {
			AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_missing_password_confirmation)
					, null
					, this
					, android.R.string.ok
					, null, null, null);
			return false;
		}
		if (!MiscUtils.validEmail(mTextEmail.getText().toString())) {
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
					mCommon.showBusy(R.string.progress_signing_up);
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
						/*
						 * mUser has been set to the user and session we got back from
						 * the service when it was inserted. We now consider the user
						 * signed in.
						 */
						Aircandi.getInstance().setUser(mUser);

						Tracker.trackEvent("User", "Insert", null, 0);
						mCommon.hideBusy();
						Logger.i(RegisterForm.this, "Inserted new user: " + mUser.name + " (" + mUser.id + ")");

//						AircandiCommon.showAlertDialog(R.drawable.ic_app
//								, getResources().getString(R.string.alert_signup_new_user_title)
//								, getResources().getString(R.string.alert_signup_new_user_message)
//								, null
//								, SignUpForm.this, android.R.string.ok, null, new OnClickListener() {
//
//									public void onClick(DialogInterface dialog, int which) {
//										setResult(CandiConstants.RESULT_USER_SIGNED_IN);
//										finish();
//									}
//								}, null);
						
						setResult(CandiConstants.RESULT_USER_SIGNED_IN);
						finish();
						AnimUtils.doOverridePendingTransition(RegisterForm.this, TransitionType.FormToCandiPage);
					}
					else {
						mTextPassword.setText("");
						mCommon.handleServiceError(result.serviceResponse, ServiceOperation.Signup);
					}
				}
			}.execute();
		}
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
		return R.layout.register_form;
	}
}