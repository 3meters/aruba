package com.aircandi.ui.user;

import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.beta.R;
import com.aircandi.components.EntityManager;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.Tracker;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequestBuilder;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.RequestListener;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Photo.PhotoSource;
import com.aircandi.service.objects.User;
import com.aircandi.ui.base.BaseEntityEdit;
import com.aircandi.ui.widgets.WebImageView;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.Animate.TransitionType;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.UI;
import com.aircandi.utilities.Utilities;

public class RegisterEdit extends BaseEntityEdit {

	private EditText		mTextFullname;
	private EditText		mTextEmail;
	private EditText		mTextPassword;
	private EditText		mTextPasswordConfirm;
	private WebImageView	mPhoto;
	private User			mEntity;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!isFinishing()) {
			initialize(savedInstanceState);
			bind();
			draw();
		}
	}

	@Override
	protected void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		mPhoto = (WebImageView) findViewById(R.id.photo);
		mTextFullname = (EditText) findViewById(R.id.name);
		mTextEmail = (EditText) findViewById(R.id.email);
		mTextPassword = (EditText) findViewById(R.id.password);
		mTextPasswordConfirm = (EditText) findViewById(R.id.password_confirm);

		mTextPasswordConfirm.setImeOptions(EditorInfo.IME_ACTION_GO);
		mTextPasswordConfirm.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_GO) {
					doSave();
					return true;
				}
				return false;
			}
		});
	}

	@Override
	protected void bind() {
		mEntity = new User();
	}

	@Override
	protected void draw() {
		drawPhoto();
	}

	@Override
	protected void drawPhoto() {
		if (mPhoto != null) {
			if (mEntity.photo != null && mEntity.photo.hasBitmap()) {
				mPhoto.hideLoading();
				UI.showImageInImageView(mEntity.photo.getBitmap(), mPhoto.getImageView(), true, Animate.fadeInMedium());
				mPhoto.setVisibility(View.VISIBLE);
			}
			else {
				final BitmapRequestBuilder builder = new BitmapRequestBuilder(mPhoto);
				builder.setImageUri(mEntity.getPhotoUri());
				final BitmapRequest imageRequest = builder.create();
				mPhoto.setBitmapRequest(imageRequest);
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("ucd")
	public void onViewTermsButtonClick(View view) {
		doViewTerms();
	}

	@SuppressWarnings("ucd")
	public void onChangePictureButtonClick(View view) {

		//showPhotoSourcePicker(mEntity.id, mEntity.schema, mEntity.type);
		mImageRequestWebImageView = mPhoto;
		mImageRequestListener = new RequestListener() {

			@Override
			public void onComplete(Object response, Photo photo, String photoUri, Bitmap imageBitmap, String title, String description, Boolean bitmapLocalOnly) {

				final ServiceResponse serviceResponse = (ServiceResponse) response;
				if (serviceResponse.responseCode == ResponseCode.Success) {

					/* Could get set to null if we are using the default */
					if (photo != null) {
						mEntity.photo = photo;
					}
					else if (photoUri != null) {
						mEntity.photo = new Photo(photoUri, null, null, null, PhotoSource.aircandi);
					}
					if (imageBitmap != null) {
						final String imageKey = mEntity.schema + "_" + mEntity.id + ".jpg";
						mEntity.photo = new Photo(null, null, null, null, PhotoSource.cache);
						mEntity.photo.setBitmap(imageKey, imageBitmap); // Could get set to null if we are using the default 
						mEntity.photo.setBitmapLocalOnly(bitmapLocalOnly);
					}
					drawPhoto();
				}
			}
		};
	}

	@SuppressWarnings("ucd")
	public void onRegisterButtonClick(View view) {
		doSave();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {

		if (resultCode == Activity.RESULT_OK) {
			if (requestCode == Constants.ACTIVITY_PICTURE_SOURCE_PICK) {
				if (intent != null && intent.getExtras() != null) {
					final Bundle extras = intent.getExtras();
					final String pictureSource = extras.getString(Constants.EXTRA_PICTURE_SOURCE);
					if (pictureSource != null && !pictureSource.equals("")) {
						if (pictureSource.equals(Constants.PHOTO_SOURCE_SEARCH)) {
							String defaultSearch = null;
							if (mTextFullname != null) {
								defaultSearch = Utilities.emptyAsNull(mTextFullname.getText().toString().trim());
							}
							pictureSearch(defaultSearch);
						}
						else if (pictureSource.equals(Constants.PHOTO_SOURCE_GALLERY)) {
							pictureFromGallery();
						}
						else if (pictureSource.equals(Constants.PHOTO_SOURCE_CAMERA)) {
							pictureFromCamera();
						}
						else if (pictureSource.equals(Constants.PHOTO_SOURCE_DEFAULT)) {
							usePictureDefault();
						}
					}
				}
			}
			else {
				super.onActivityResult(requestCode, resultCode, intent);
			}
		}
	}

	private void usePictureDefault() {
		/*
		 * Setting the photo to null will trigger correct default handling.
		 */
		if (mEntity.photo != null) {
			mEntity.photo.removeBitmap();
			mEntity.photo = null;
		}
		mEntity.photo = mEntity.getDefaultPhoto();
		drawPhoto();
		Tracker.sendEvent("ui_action", "set_user_picture_to_default", null, 0, Aircandi.getInstance().getUser());
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	private void doViewTerms() {
		Tracker.sendEvent("ui_action", "view_terms", null, 0, Aircandi.getInstance().getUser());
		final Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
		intent.setData(Uri.parse(Constants.URL_AIRCANDI_TERMS));
		startActivity(intent);
		Animate.doOverridePendingTransition(this, TransitionType.PageToForm);

	}

	@Override
	protected boolean validate() {
		if (mTextFullname.getText().length() == 0) {
			Dialogs.showAlertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_missing_fullname)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}
		if (mTextEmail.getText().length() == 0) {
			Dialogs.showAlertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_missing_email)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}
		if (mTextPassword.getText().length() < 6) {
			Dialogs.showAlertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_missing_password)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}
		if (mTextPasswordConfirm.getText().length() < 6) {
			Dialogs.showAlertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_missing_password_confirmation)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}
		if (!Utilities.validEmail(mTextEmail.getText().toString())) {
			Dialogs.showAlertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_invalid_email)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}
		if (!mTextPassword.getText().toString().equals(mTextPasswordConfirm.getText().toString())) {
			Dialogs.showAlertDialog(android.R.drawable.ic_dialog_alert
					, getResources().getString(R.string.error_signup_missmatched_passwords_title)
					, getResources().getString(R.string.error_signup_missmatched_passwords_message)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			mTextPasswordConfirm.setText("");
			return false;
		}
		return true;
	}

	private void doSave() {

		if (validate()) {

			mEntity.email = mTextEmail.getText().toString().trim().toLowerCase(Locale.US);
			mEntity.name = mTextFullname.getText().toString().trim();
			mEntity.password = mTextPassword.getText().toString().trim();

			Tracker.sendEvent("ui_action", "register_user", null, 0, Aircandi.getInstance().getUser());
			Logger.d(this, "Inserting user: " + mEntity.name);

			new AsyncTask() {

				@Override
				protected void onPreExecute() {
					mBusyManager.showBusy(R.string.progress_signing_up);
				}

				@Override
				protected Object doInBackground(Object... params) {
					Thread.currentThread().setName("InsertUser");
					final ModelResult result = EntityManager.getInstance().insertUser(mEntity, null, mEntity.photo.getBitmap());
					return result;
				}

				@Override
				protected void onPostExecute(Object response) {
					final ModelResult result = (ModelResult) response;

					if (result.serviceResponse.responseCode == ResponseCode.Success) {
						/*
						 * mUser has been set to the user and session we got back from
						 * the service when it was inserted. We now consider the user
						 * signed in.
						 */
						final User insertedUser = (User) result.data;
						Aircandi.getInstance().setUser(insertedUser);

						mBusyManager.hideBusy();
						Logger.i(RegisterEdit.this, "Inserted new user: " + mEntity.name + " (" + mEntity.id + ")");

						UI.showToastNotification(getResources().getString(R.string.alert_signed_in)
								+ " " + Aircandi.getInstance().getUser().name, Toast.LENGTH_SHORT);

						final String jsonUser = HttpService.objectToJson(insertedUser);
						final String jsonSession = HttpService.objectToJson(insertedUser.session);

						Aircandi.settingsEditor.putString(Constants.SETTING_USER, jsonUser);
						Aircandi.settingsEditor.putString(Constants.SETTING_USER_SESSION, jsonSession);
						Aircandi.settingsEditor.putString(Constants.SETTING_LAST_EMAIL, insertedUser.email);
						Aircandi.settingsEditor.commit();

						setResult(Constants.RESULT_USER_SIGNED_IN);
						finish();
						Animate.doOverridePendingTransition(RegisterEdit.this, TransitionType.FormToPage);
					}
					else {
						/*
						 * TODO: Need to handle AmazonClientException.
						 * Does clearing the password fields always make sense?
						 */
						Routing.serviceError(RegisterEdit.this, result.serviceResponse);
					}
				}
			}.execute();
		}
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.register_edit;
	}
}