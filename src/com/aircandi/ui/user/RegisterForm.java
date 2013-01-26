package com.aircandi.ui.user;

import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.aircandi.Aircandi;
import com.aircandi.CandiConstants;
import com.aircandi.R;
import com.aircandi.components.AircandiCommon;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.FontManager;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.ProxiExplorer.ModelResult;
import com.aircandi.components.Tracker;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequestBuilder;
import com.aircandi.service.ProxibaseService;
import com.aircandi.service.ProxibaseService.RequestListener;
import com.aircandi.service.objects.User;
import com.aircandi.ui.Preferences;
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
	private WebImageView	mImage;
	private Button			mButtonSignUp;
	private User			mUser;
	private Bitmap			mBitmap;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initialize();
		bind();
		draw();
	}

	private void initialize() {
		mImage = (WebImageView) findViewById(R.id.image_picture);
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

	private void bind() {
		mUser = new User();
	}

	private void draw() {
		drawImage(mUser);
	}

	private void drawImage(User user) {
		if (mImage != null) {
			if (mBitmap != null) {
				mImage.hideLoading();
				ImageUtils.showImageInImageView(mBitmap, mImage.getImageView(), true, AnimUtils.fadeInMedium());
				mImage.setVisibility(View.VISIBLE);
			}
			else {
				BitmapRequestBuilder builder = new BitmapRequestBuilder(mImage);
				builder.setImageUri(user.getUserPhotoUri());
				BitmapRequest imageRequest = builder.create();
				mImage.setBitmapRequest(imageRequest);
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("ucd")
	public void onSignUpButtonClick(View view) {
		doSave();
	}

	@SuppressWarnings("ucd")
	public void onViewTermsButtonClick(View view) {
		doViewTerms();
	}

	@SuppressWarnings("ucd")
	public void onChangePictureButtonClick(View view) {

		mCommon.showPictureSourcePicker(null);
		mImageRequestWebImageView = mImage;
		mImageRequestListener = new RequestListener() {

			@Override
			public void onComplete(Object response, String imageUri, Bitmap imageBitmap, String title, String description) {

				ServiceResponse serviceResponse = (ServiceResponse) response;
				if (serviceResponse.responseCode == ResponseCode.Success) {
					/* Could get set to null if we are using the default */
					mBitmap = imageBitmap;
					if (imageUri != null) {
						mUser.getPhotoForSet().setImageUri(imageUri);
					}
					drawImage(mUser);
				}
			}
		};
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {

		if (resultCode == Activity.RESULT_OK) {
			if (requestCode == CandiConstants.ACTIVITY_PICTURE_SOURCE_PICK) {
				if (intent != null && intent.getExtras() != null) {
					Bundle extras = intent.getExtras();
					final String pictureSource = extras.getString(CandiConstants.EXTRA_PICTURE_SOURCE);
					if (pictureSource != null && !pictureSource.equals("")) {
						if (pictureSource.equals("search")) {
							pictureSearch();
						}
						else if (pictureSource.equals("gallery")) {
							pictureFromGallery();
						}
						else if (pictureSource.equals("camera")) {
							pictureFromCamera();
						}
						else if (pictureSource.equals("default")) {
							usePictureDefault(mUser);
						}
					}
				}
			}
			else {
				super.onActivityResult(requestCode, resultCode, intent);
			}
		}
	}

	private void usePictureDefault(User user) {
		/*
		 * Setting the photo to null will trigger correct default handling.
		 */
		if (user.photo != null) {
			user.photo.setBitmap(null);
			user.photo = null;
		}
		mBitmap = null;
		drawImage(user);
		Tracker.trackEvent("User", "DefaultPicture", "None", 0);
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	private void doViewTerms() {

		Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
		intent.setData(Uri.parse(CandiConstants.URL_AIRCANDI_TERMS));
		startActivity(intent);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);

	}

	private void doSave() {
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

	private void insert() {

		if (validate()) {

			mUser.email = mTextEmail.getText().toString().trim().toLowerCase(Locale.US);
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
					Thread.currentThread().setName("InsertUser");
					ModelResult result = ProxiExplorer.getInstance().getEntityModel().insertUser(mUser, mBitmap);
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
						User insertedUser = (User) result.data;
						Aircandi.getInstance().setUser(insertedUser);

						Tracker.trackEvent("User", "Insert", null, 0);
						mCommon.hideBusy(false);
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
						
						ImageUtils.showToastNotification(getResources().getString(R.string.alert_signed_in)
								+ " " + Aircandi.getInstance().getUser().name, Toast.LENGTH_SHORT);

						String jsonUser = ProxibaseService.convertObjectToJsonSmart(insertedUser, false, true);
						String jsonSession = ProxibaseService.convertObjectToJsonSmart(insertedUser.session, false, true);

						Aircandi.settingsEditor.putString(Preferences.SETTING_USER, jsonUser);
						Aircandi.settingsEditor.putString(Preferences.SETTING_USER_SESSION, jsonSession);
						Aircandi.settingsEditor.putString(Preferences.SETTING_LAST_EMAIL, insertedUser.email);
						Aircandi.settingsEditor.commit();

						setResult(CandiConstants.RESULT_USER_SIGNED_IN);
						finish();
						AnimUtils.doOverridePendingTransition(RegisterForm.this, TransitionType.FormToCandiPage);
					}
					else {
						/*
						 * TODO: Need to handle AmazonClientException.
						 * Does clearing the password fields always make sense?
						 */
						mTextPassword.setText("");
						mTextPasswordConfirm.setText("");
						mCommon.handleServiceError(result.serviceResponse, ServiceOperation.Signup);
					}
				}
			}.execute();
		}
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle events
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutID() {
		return R.layout.register_form;
	}
}