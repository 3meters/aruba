package com.aircandi.ui;

import android.app.Activity;
import android.content.Intent;
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

import com.aircandi.Aircandi;
import com.aircandi.CandiConstants;
import com.aircandi.R;
import com.aircandi.components.AircandiCommon;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.CommandType;
import com.aircandi.components.FontManager;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.ProxiExplorer.ModelResult;
import com.aircandi.components.Tracker;
import com.aircandi.components.images.BitmapRequest;
import com.aircandi.components.images.BitmapRequest.ImageResponse;
import com.aircandi.components.images.BitmapRequestBuilder;
import com.aircandi.service.ProxibaseService;
import com.aircandi.service.ProxibaseService.RequestListener;
import com.aircandi.service.ProxibaseService.ServiceDataType;
import com.aircandi.service.objects.User;
import com.aircandi.ui.base.FormActivity;
import com.aircandi.ui.widgets.WebImageView;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;
import com.aircandi.utilities.DateUtils;
import com.aircandi.utilities.ImageUtils;
import com.aircandi.utilities.MiscUtils;

public class ProfileForm extends FormActivity {

	private ViewFlipper		mViewFlipper;
	private WebImageView	mImageUser;
	private EditText		mTextFullname;
	private EditText		mTextBio;
	private EditText		mTextLink;
	private EditText		mTextLocation;
	private EditText		mTextEmail;
	private Button			mButtonSave;
	private User			mUser;
	private Bitmap			mUserBitmap;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initialize();
		bind();
	}

	private void initialize() {

		mUser = Aircandi.getInstance().getUser();

		if (mUser == null) {
			throw new IllegalStateException("Current user required by ProfileForm");
		}

		mViewFlipper = (ViewFlipper) findViewById(R.id.flipper_form);
		mCommon.setViewFlipper(mViewFlipper);

		mImageUser = (WebImageView) findViewById(R.id.image_picture);
		mTextFullname = (EditText) findViewById(R.id.text_fullname);
		mTextBio = (EditText) findViewById(R.id.text_bio);
		mTextLink = (EditText) findViewById(R.id.text_link);
		mTextLocation = (EditText) findViewById(R.id.text_location);
		mTextEmail = (EditText) findViewById(R.id.text_email);
		mButtonSave = (Button) findViewById(R.id.btn_save);

		FontManager.getInstance().setTypefaceDefault(mTextFullname);
		FontManager.getInstance().setTypefaceDefault(mTextBio);
		FontManager.getInstance().setTypefaceDefault(mTextLink);
		FontManager.getInstance().setTypefaceDefault(mTextLocation);
		FontManager.getInstance().setTypefaceDefault(mTextEmail);
		FontManager.getInstance().setTypefaceDefault(mButtonSave);

		mTextFullname.addTextChangedListener(new SimpleTextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				mButtonSave.setEnabled(enableSave());
			}
		});

		mTextEmail.addTextChangedListener(new SimpleTextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				mButtonSave.setEnabled(enableSave());
			}
		});

	}

	private void bind() {
		/*
		 * This form is always for editing. We always reload the user to make sure
		 * we have the freshest data.
		 */
		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showBusy();
			}

			@Override
			protected Object doInBackground(Object... params) {
				ModelResult result = ProxiExplorer.getInstance().getEntityModel().getUser(mUser.id);
				return result;
			}

			@Override
			protected void onPostExecute(Object modelResult) {
				ModelResult result = (ModelResult) modelResult;
				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					String jsonResponse = (String) result.serviceResponse.data;
					mUser = (User) ProxibaseService.convertJsonToObjectSmart(jsonResponse, ServiceDataType.User).data;

					/* We got fresh user data but we want to hook up the old session. */
					mUser.session = Aircandi.getInstance().getUser().session;
					mImageUriOriginal = mUser.getImageUri();

					mCommon.hideBusy();
					draw();
				}
				else {
					mCommon.handleServiceError(result.serviceResponse, ServiceOperation.ProfileBrowse);
				}
			}
		}.execute();
	}

	private void draw() {

		mTextFullname.setText(mUser.name);
		mTextBio.setText(mUser.bio);
		mTextLink.setText(mUser.webUri);
		mTextLocation.setText(mUser.location);
		mTextEmail.setText(mUser.email);

		if (mUser.getImageUri() != null && mUser.getImageUri().length() > 0) {
			if (mUserBitmap != null) {
				ImageUtils.showImageInImageView(mUserBitmap, mImageUser.getImageView(), true, AnimUtils.fadeInMedium());
			}
			else {
				BitmapRequestBuilder builder = new BitmapRequestBuilder(mImageUser);
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
							mCommon.handleServiceError(serviceResponse, ServiceOperation.PictureBrowse);
						}
					}
				});

				BitmapRequest imageRequest = builder.create();
				mImageUser.setBitmapRequest(imageRequest);
			}
		}
		((ViewGroup) findViewById(R.id.flipper_form)).setVisibility(View.VISIBLE);

	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onSaveButtonClick(View view) {
		updateProfile();
	}

	public void onChangePictureButtonClick(View view) {
//		showChangePictureDialog(false, null, null, null, mImageUser, new RequestListener() {
//
//			@Override
//			public void onComplete(Object response, String imageUri, String linkUri, Bitmap imageBitmap, String title, String description) {
//
//				ServiceResponse serviceResponse = (ServiceResponse) response;
//				if (serviceResponse.responseCode == ResponseCode.Success) {
//					mUser.getPhoto().setImageUri(imageUri);
//					mUserBitmap = imageBitmap;
//				}
//			}
//		});
	}

	public void onChangePasswordButtonClick(View view) {
		IntentBuilder intentBuilder = new IntentBuilder(this, PasswordForm.class);
		intentBuilder.setCommandType(CommandType.Edit);
		Intent intent = intentBuilder.create();
		startActivity(intent);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {

		if (requestCode == CandiConstants.ACTIVITY_SIGNIN) {
			if (resultCode == Activity.RESULT_CANCELED) {
				setResult(resultCode);
				finish();
			}
			else {
				initialize();
				bind();
				draw();
			}
		}
		else {
			super.onActivityResult(requestCode, resultCode, intent);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	private void updateProfile() {

		if (validate()) {

			mUser.email = mTextEmail.getText().toString().trim();
			mUser.name = mTextFullname.getText().toString().trim();
			mUser.bio = mTextBio.getText().toString().trim();
			mUser.location = mTextLocation.getText().toString().trim();
			mUser.webUri = mTextLink.getText().toString().trim();

			new AsyncTask() {

				@Override
				protected void onPreExecute() {
					mCommon.showBusy(R.string.progress_saving);
				}

				@Override
				protected Object doInBackground(Object... params) {
					ModelResult result = ProxiExplorer.getInstance().getEntityModel().updateUser(mUser, mUserBitmap, false);
					return result;
				}

				@Override
				protected void onPostExecute(Object response) {
					ModelResult result = (ModelResult) response;

					if (result.serviceResponse.responseCode == ResponseCode.Success) {
						Logger.i(this, "Updated user profile: " + mUser.name + " (" + mUser.id + ")");
						Tracker.trackEvent("User", "Update", null, 0);
						mCommon.hideBusy();
						/*
						 * We treat updating the profile like a change to an entity in the entity model. This forces
						 * UI to update itself and pickup the changes like a new profile name, picture, etc.
						 */
						ProxiExplorer.getInstance().getEntityModel().setLastActivityDate(DateUtils.nowDate().getTime());

						/* Update the global user */
						Aircandi.getInstance().setUser(mUser);
						/*
						 * We also need to update the user that has been persisted for auto sign in.
						 */
						String jsonUser = ProxibaseService.convertObjectToJsonSmart(mUser, true, false);
						Aircandi.settingsEditor.putString(Preferences.PREF_USER, jsonUser);
						Aircandi.settingsEditor.commit();

						ImageUtils.showToastNotification(getString(R.string.alert_updated), Toast.LENGTH_SHORT);
						setResult(CandiConstants.RESULT_PROFILE_UPDATED);
						finish();
					}
					else {
						mCommon.handleServiceError(result.serviceResponse, ServiceOperation.ProfileUpdate);
					}
				}
			}.execute();
		}
	}

	private boolean validate() {
		if (!MiscUtils.validEmail(mTextEmail.getText().toString())) {
			mCommon.showAlertDialogSimple(null, getString(R.string.error_invalid_email));
			return false;
		}
		if (mTextLink.getText().toString() != null && !mTextLink.getText().toString().equals("")) {
			if (!MiscUtils.validWebUri(mTextLink.getText().toString())) {
				AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert
						, null
						, getResources().getString(R.string.error_weburi_invalid)
						, null
						, this
						, android.R.string.ok
						, null, null, null);
			}
			return false;
		}
		return true;
	}

	private boolean enableSave() {
		if (mTextFullname.getText().length() == 0) {
			return false;
		}
		if (mTextEmail.getText().length() == 0) {
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
		return R.layout.profile_form;
	}
}