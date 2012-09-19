package com.aircandi;

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

import com.aircandi.components.AircandiCommon;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.AnimUtils;
import com.aircandi.components.AnimUtils.TransitionType;
import com.aircandi.components.CommandType;
import com.aircandi.components.DateUtils;
import com.aircandi.components.ImageRequest;
import com.aircandi.components.ImageRequest.ImageResponse;
import com.aircandi.components.ImageRequestBuilder;
import com.aircandi.components.ImageUtils;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.S3;
import com.aircandi.components.Tracker;
import com.aircandi.components.Utilities;
import com.aircandi.core.CandiConstants;
import com.aircandi.service.ProxiConstants;
import com.aircandi.service.ProxibaseService;
import com.aircandi.service.ProxibaseService.RequestListener;
import com.aircandi.service.ProxibaseService.RequestType;
import com.aircandi.service.ProxibaseService.ResponseFormat;
import com.aircandi.service.ProxibaseService.ServiceDataType;
import com.aircandi.service.ProxibaseServiceException;
import com.aircandi.service.Query;
import com.aircandi.service.ServiceRequest;
import com.aircandi.service.objects.User;
import com.aircandi.widgets.WebImageView;

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
	private Integer			mUserId;
	private Bitmap			mUserBitmap;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/*
		 * You can't display the profile form unless you are signed in so the only sign in case
		 * is if the session expired. That could change if we start letting users view profiles
		 * for other Aircandi users.
		 */
		User user = Aircandi.getInstance().getUser();
		if (user != null) {
			if (user.session != null) {
				Boolean expired = user.session.renewSession(DateUtils.nowDate().getTime());
				if (expired) {
					IntentBuilder intentBuilder = new IntentBuilder(this, SignInForm.class);
					intentBuilder.setCommandType(CommandType.Edit);
					intentBuilder.setMessage(getString(R.string.signin_message_session_expired));
					Intent intent = intentBuilder.create();
					startActivityForResult(intent, CandiConstants.ACTIVITY_SIGNIN);
					AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
					return;
				}
			}
		}
		initialize();
		bind();
		draw();
	}

	private void initialize() {

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mUserId = extras.getInt(getString(R.string.EXTRA_USER_ID));
		}

		mUser = Aircandi.getInstance().getUser();

		if (mUser == null && mUserId == null) {
			throw new IllegalStateException("User or userId must be passed to ProfileForm");
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
				mCommon.showProgressDialog(true, getString(R.string.progress_loading));
			}

			@Override
			protected Object doInBackground(Object... params) {
				Query query = new Query("users");
				if (mUser != null) {
					query.filter("{\"email\":\"" + ((User) mUser).email + "\"}");
				}
				else {
					query.filter("{\"_id\":\"" + String.valueOf(mUserId) + "\"}");
				}

				ServiceRequest serviceRequest = new ServiceRequest()
						.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_REST)
						.setRequestType(RequestType.Get)
						.setQuery(query)
						.setSession(Aircandi.getInstance().getUser().session)
						.setResponseFormat(ResponseFormat.Json);

				ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);
				return serviceResponse;
			}

			@Override
			protected void onPostExecute(Object result) {
				ServiceResponse serviceResponse = (ServiceResponse) result;
				if (serviceResponse.responseCode == ResponseCode.Success) {
					String jsonResponse = (String) serviceResponse.data;
					mUser = (User) ProxibaseService.convertJsonToObjectSmart(jsonResponse, ServiceDataType.User).data;

					/* We got fresh user data but we want to hook up the old session. */
					mUser.session = Aircandi.getInstance().getUser().session;
					mImageUriOriginal = mUser.imageUri;

					mCommon.showProgressDialog(false, null);
				}
				else {
					mCommon.handleServiceError(serviceResponse, ServiceOperation.ProfileBrowse);
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

		if (mUser.imageUri != null && mUser.imageUri.length() > 0) {
			if (mUserBitmap != null) {
				ImageUtils.showImageInImageView(mUserBitmap, mImageUser.getImageView(), true, AnimUtils.fadeInMedium());
			}
			else {
				ImageRequestBuilder builder = new ImageRequestBuilder(mImageUser);
				builder.setFromUris(mUser.imageUri, null);
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

				ImageRequest imageRequest = builder.create();
				mImageUser.setImageRequest(imageRequest);
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

			new AsyncTask() {

				@Override
				protected void onPreExecute() {
					mCommon.showProgressDialog(true, getString(R.string.progress_saving));
				}

				@Override
				protected Object doInBackground(Object... params) {

					/*
					 * TODO: We are going with a garbage collection scheme for orphaned images. We
					 * need to use an extended property on S3 items that is set to a date when
					 * collection is ok. This allows downloaded entities to keep working even if
					 * an image for entity has changed.
					 */
					ServiceResponse serviceResponse = new ServiceResponse();

					/* Put image to S3 if we have a new one. */
					if (mUserBitmap != null && !mUserBitmap.isRecycled()) {
						try {
							String imageKey = String.valueOf(((User) mUser).id) + "_"
									+ String.valueOf(DateUtils.nowString(DateUtils.DATE_NOW_FORMAT_FILENAME))
									+ ".jpg";
							S3.putImage(imageKey, mUserBitmap);
							mUser.imageUri = imageKey;
						}
						catch (ProxibaseServiceException exception) {
							serviceResponse = new ServiceResponse(ResponseCode.Failed, null, exception);
						}
					}

					if (serviceResponse.responseCode == ResponseCode.Success) {

						mUser.email = mTextEmail.getText().toString().trim();
						mUser.name = mTextFullname.getText().toString().trim();
						mUser.bio = mTextBio.getText().toString().trim();
						mUser.location = mTextLocation.getText().toString().trim();
						mUser.webUri = mTextLink.getText().toString().trim();
						/*
						 * Service handles modifiedId and modifiedDate based
						 * on the session info passed with request.
						 */

						ServiceRequest serviceRequest = new ServiceRequest()
								.setUri(mUser.getEntryUri())
								.setRequestType(RequestType.Update)
								.setRequestBody(ProxibaseService.convertObjectToJsonSmart(mUser, true))
								.setSession(Aircandi.getInstance().getUser().session)
								.setResponseFormat(ResponseFormat.Json);

						serviceResponse = NetworkManager.getInstance().request(serviceRequest);
					}
					return serviceResponse;
				}

				@Override
				protected void onPostExecute(Object response) {
					ServiceResponse serviceResponse = (ServiceResponse) response;

					if (serviceResponse.responseCode == ResponseCode.Success) {
						Logger.i(this, "Updated user profile: " + mUser.name + " (" + mUser.id + ")");
						Tracker.trackEvent("User", "Update", null, 0);
						mCommon.showProgressDialog(false, null);
						/*
						 * We treat updating the profile like a change to an entity in the entity model. This forces
						 * UI to update itself and pickup the changes like a new profile name, picture, etc.
						 */
						ProxiExplorer.getInstance().getEntityModel().setLastActivityDate(DateUtils.nowDate().getTime());

						/* Update the global user */
						Aircandi.getInstance().setUser(mUser);

						/*
						 * entity.creator is what we show for entity authors. To make the entity model consistent
						 * with this update to the profile we walk all the entities and update where creator.id
						 * equals the signed in user.
						 */
						ProxiExplorer.getInstance().getEntityModel().updateUser(mUser);

						/*
						 * We also need to update the user that has been persisted for auto sign in.
						 */
						String jsonUser = ProxibaseService.convertObjectToJsonSmart(mUser, true);
						Aircandi.settingsEditor.putString(Preferences.PREF_USER, jsonUser);
						Aircandi.settingsEditor.commit();

						ImageUtils.showToastNotification(getString(R.string.alert_updated), Toast.LENGTH_SHORT);
						setResult(CandiConstants.RESULT_PROFILE_UPDATED);
						finish();
					}
					else {
						mCommon.handleServiceError(serviceResponse, ServiceOperation.ProfileUpdate);
					}
				}
			}.execute();
		}
	}

	private boolean validate() {
		if (!Utilities.validEmail(mTextEmail.getText().toString())) {
			mCommon.showAlertDialogSimple(null, getString(R.string.error_invalid_email));
			return false;
		}
		if (mTextLink.getText().toString() != null && !mTextLink.getText().toString().equals("")) {
			if (!Utilities.validWebUri(mTextLink.getText().toString())) {
				AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert
						, null
						, getResources().getString(R.string.error_weburi_invalid)
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