package com.aircandi.ui.user;

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
import android.widget.TextView;
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
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequestBuilder;
import com.aircandi.service.ProxibaseService;
import com.aircandi.service.ProxibaseService.RequestListener;
import com.aircandi.service.ProxibaseService.ServiceDataType;
import com.aircandi.service.objects.User;
import com.aircandi.ui.Preferences;
import com.aircandi.ui.base.FormActivity;
import com.aircandi.ui.widgets.WebImageView;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;
import com.aircandi.utilities.DateUtils;
import com.aircandi.utilities.ImageUtils;
import com.aircandi.utilities.MiscUtils;

public class ProfileForm extends FormActivity {

	private ViewFlipper		mViewFlipper;
	private WebImageView	mImage;
	private EditText		mTextFullname;
	private EditText		mTextBio;
	private EditText		mTextLink;
	private EditText		mTextLocation;
	private EditText		mTextEmail;
	private Button			mButtonSave;
	private User			mUser;
	private Bitmap			mBitmap;

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

		mImage = (WebImageView) findViewById(R.id.image_picture);
		mTextFullname = (EditText) findViewById(R.id.text_fullname);
		mTextBio = (EditText) findViewById(R.id.text_bio);
		mTextLink = (EditText) findViewById(R.id.text_link);
		mTextLocation = (EditText) findViewById(R.id.text_location);
		mTextEmail = (EditText) findViewById(R.id.text_email);
		mButtonSave = (Button) findViewById(R.id.button_save);

		FontManager.getInstance().setTypefaceDefault(mTextFullname);
		FontManager.getInstance().setTypefaceDefault(mTextBio);
		FontManager.getInstance().setTypefaceDefault(mTextLink);
		FontManager.getInstance().setTypefaceDefault(mTextLocation);
		FontManager.getInstance().setTypefaceDefault(mTextEmail);
		FontManager.getInstance().setTypefaceDefault(mButtonSave);
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_cancel));
		

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
				Thread.currentThread().setName("GetUser");
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
					mImageUriOriginal = mUser.getUserPhotoUri();

					mCommon.hideBusy(false);
					draw();
				}
				else {
					mCommon.handleServiceError(result.serviceResponse, ServiceOperation.ProfileBrowse);
				}
			}
		}.execute();
	}

	private void draw() {

		drawImage(mUser);

		mTextFullname.setText(mUser.name);
		mTextBio.setText(mUser.bio);
		mTextLink.setText(mUser.webUri);
		mTextLocation.setText(mUser.location);
		mTextEmail.setText(mUser.email);

		((ViewGroup) findViewById(R.id.flipper_form)).setVisibility(View.VISIBLE);

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
	public void onSaveButtonClick(View view) {
		updateProfile();
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

	@SuppressWarnings("ucd")
	public void onChangePasswordButtonClick(View view) {
		IntentBuilder intentBuilder = new IntentBuilder(this, PasswordForm.class);
		intentBuilder.setCommandType(CommandType.Edit);
		Intent intent = intentBuilder.create();
		startActivity(intent);
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
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
					Thread.currentThread().setName("UpdateUser");
					ModelResult result = ProxiExplorer.getInstance().getEntityModel().updateUser(mUser, mBitmap, false);
					return result;
				}

				@Override
				protected void onPostExecute(Object response) {
					ModelResult result = (ModelResult) response;

					if (result.serviceResponse.responseCode == ResponseCode.Success) {
						Logger.i(this, "Updated user profile: " + mUser.name + " (" + mUser.id + ")");
						Tracker.trackEvent("User", "Update", null, 0);
						mCommon.hideBusy(false);
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
						Aircandi.settingsEditor.putString(Preferences.SETTING_USER, jsonUser);
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

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutID() {
		return R.layout.profile_form;
	}
}