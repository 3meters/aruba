package com.proxibase.aircandi;

import org.apache.http.HttpStatus;

import android.app.AlertDialog;
import android.content.DialogInterface;
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

import com.proxibase.aircandi.components.AircandiCommon;
import com.proxibase.aircandi.components.CommandType;
import com.proxibase.aircandi.components.DateUtils;
import com.proxibase.aircandi.components.Exceptions;
import com.proxibase.aircandi.components.ImageRequest;
import com.proxibase.aircandi.components.IntentBuilder;
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
import com.proxibase.service.Query;
import com.proxibase.service.ServiceRequest;
import com.proxibase.service.objects.User;

@SuppressWarnings("unused")
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

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initialize();
		bind();

		if (savedInstanceState != null) {
			doRestoreInstanceState(savedInstanceState);
		}
	}

	protected void initialize() {

		mCommon.track();
		mCommon.mActionBar.setTitle(R.string.form_title_profile);
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
				mButtonSave.setEnabled(isValid());
			}
		});

		mTextEmail.addTextChangedListener(new SimpleTextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				mButtonSave.setEnabled(isValid());
			}
		});

	}

	protected void bind() {
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
				Query query = new Query("users").filter("{\"_id\":\"" + String.valueOf(mUserId) + "\"}");
				if (mUser != null) {
					query = new Query("users").filter("{\"email\":\"" + ((User) mUser).email + "\"}");
				}

				ServiceRequest serviceRequest = new ServiceRequest();
				serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE)
						.setRequestType(RequestType.Get)
						.setQuery(query)
						.setResponseFormat(ResponseFormat.Json);

				ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);
				return serviceResponse;
			}

			@Override
			protected void onPostExecute(Object result) {
				ServiceResponse serviceResponse = (ServiceResponse) result;
				if (serviceResponse.responseCode == ResponseCode.Success) {
					String jsonResponse = (String) serviceResponse.data;
					mUser = (User) ProxibaseService.convertJsonToObject(jsonResponse, User.class, GsonType.ProxibaseService).data;
					mImageUriOriginal = mUser.imageUri;
					mCommon.showProgressDialog(false, null);
					mCommon.stopTitlebarProgress();
					draw();
				}
				else {
					mCommon.handleServiceError(serviceResponse);
				}
			}
		}.execute();
	}

	protected void draw() {

		mTextFullname.setText(mUser.name);
		mTextBio.setText(mUser.bio);
		mTextLink.setText(mUser.webUri);
		mTextLocation.setText(mUser.location);
		mTextEmail.setText(mUser.email);

		if (mUser.imageUri != null && mUser.imageUri.length() > 0) {
			if (mUser.imageBitmap != null) {
				ImageUtils.showImageInImageView(mUser.imageBitmap, mImageUser.getImageView(), true, R.anim.fade_in_medium);
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
							mUser.imageBitmap = imageResponse.bitmap;
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
		doSave();
	}

	public void onChangePictureButtonClick(View view) {
		showChangePictureDialog(true, mImageUser, new RequestListener() {

			@Override
			public void onComplete(Object response, String imageUri, String linkUri, Bitmap imageBitmap) {

				ServiceResponse serviceResponse = (ServiceResponse) response;
				if (serviceResponse.responseCode == ResponseCode.Success) {
					mUser.imageUri = imageUri;
					mUser.imageBitmap = imageBitmap;
				}
			}
		});
	}

	public void onChangePasswordButtonClick(View view) {
		IntentBuilder intentBuilder = new IntentBuilder(this, PasswordForm.class);
		intentBuilder.setCommandType(CommandType.Edit);
		Intent intent = intentBuilder.create();
		startActivity(intent);
		overridePendingTransition(R.anim.form_in, R.anim.browse_out);
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	protected void doSave() {

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
					if (mUser.imageBitmap != null && !mUser.imageBitmap.isRecycled()) {
						try {
							String imageKey = String.valueOf(((User) mUser).id) + "_"
									+ String.valueOf(DateUtils.nowString(DateUtils.DATE_NOW_FORMAT_FILENAME))
									+ ".jpg";
							S3.putImage(imageKey, mUser.imageBitmap);
							mUser.imageUri = CandiConstants.URL_AIRCANDI_MEDIA + CandiConstants.S3_BUCKET_IMAGES + "/" + imageKey;
						}
						catch (ProxibaseServiceException exception) {
							serviceResponse = new ServiceResponse(ResponseCode.Failed, ResponseCodeDetail.ServiceException, null, exception);
						}
					}

					if (serviceResponse.responseCode == ResponseCode.Success) {

						mUser.email = mTextEmail.getText().toString().trim();
						mUser.name = mTextFullname.getText().toString().trim();
						mUser.bio = mTextBio.getText().toString().trim();
						mUser.location = mTextLocation.getText().toString().trim();
						mUser.webUri = mTextLink.getText().toString().trim();
						mUser.modifiedDate = DateUtils.nowDate().getTime();
						mUser.modifierId = Aircandi.getInstance().getUser().id;

						Logger.i(this, "Updating user profile: " + mUser.name);

						ServiceRequest serviceRequest = new ServiceRequest();
						serviceRequest.setUri(mUser.getEntryUri())
								.setRequestType(RequestType.Update)
								.setRequestBody(ProxibaseService.convertObjectToJson((Object) mUser, GsonType.ProxibaseService))
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
						Tracker.trackEvent("User", "Update", null, 0);
						mCommon.showProgressDialog(false, null);
						Aircandi.getInstance().setUser(mUser);
						ImageUtils.showToastNotification(getString(R.string.alert_updated), Toast.LENGTH_SHORT);
						setResult(CandiConstants.RESULT_PROFILE_UPDATED);
						finish();
					}
					else {
						/*
						 * This could have been caused any problem while updating the user and the user image. We look
						 * first for ones that are known responses from the service.
						 * 
						 * - 400: email not valid
						 * - 400: email not unique
						 */
						if (serviceResponse.exception.getHttpStatusCode() == HttpStatus.SC_BAD_REQUEST) {
							String message = serviceResponse.exception.getResponseMessage();
							AircandiCommon.showAlertDialog(R.drawable.icon_app, null, message,
									ProfileForm.this, android.R.string.ok, null, new DialogInterface.OnClickListener() {
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
	}

	private boolean validate() {
		/*
		 * Might want to do extra validation:
		 * - valid web link
		 * - valid email address
		 */
		return true;
	}

	private boolean isValid() {
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
		try {
			if (mUser != null && mUser.imageBitmap != null) {
				mUser.imageBitmap.recycle();
				mUser.imageBitmap = null;
			}
		}
		catch (Exception exception) {
			Exceptions.Handle(exception);
		}
		finally {
			super.onDestroy();
		}
	}

	// --------------------------------------------------------------------------------------------
	// Persistence routines
	// --------------------------------------------------------------------------------------------

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		Logger.d(this, "onSaveInstanceState called");

		mCommon.doSaveInstanceState(savedInstanceState);
		if (mTextFullname != null) {
			savedInstanceState.putString("fullname", mTextFullname.getText().toString());
		}
		if (mTextEmail != null) {
			savedInstanceState.putString("email", mTextEmail.getText().toString());
		}
	}

	private void doRestoreInstanceState(Bundle savedInstanceState) {
		Logger.d(this, "Restoring previous state");

		mCommon.doRestoreInstanceState(savedInstanceState);
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
		return R.layout.profile_form;
	}
}