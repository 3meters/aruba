package com.proxibase.aircandi;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.proxibase.aircandi.components.DateUtils;
import com.proxibase.aircandi.components.Exceptions;
import com.proxibase.aircandi.components.ImageRequest;
import com.proxibase.aircandi.components.ImageRequestBuilder;
import com.proxibase.aircandi.components.ImageUtils;
import com.proxibase.aircandi.components.Logger;
import com.proxibase.aircandi.components.NetworkManager;
import com.proxibase.aircandi.components.S3;
import com.proxibase.aircandi.components.Tracker;
import com.proxibase.aircandi.components.Command.CommandVerb;
import com.proxibase.aircandi.components.ImageRequest.ImageResponse;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ResponseCodeDetail;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.widgets.WebImageView;
import com.proxibase.sdk.android.proxi.consumer.User;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseServiceException;
import com.proxibase.sdk.android.proxi.service.Query;
import com.proxibase.sdk.android.proxi.service.ServiceRequest;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestListener;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;
import com.proxibase.sdk.android.util.ProxiConstants;

public class ProfileForm extends FormActivity {

	private ViewFlipper		mViewFlipper;
	private WebImageView	mImageUser;
	private EditText		mTextFullname;
	private EditText		mTextEmail;
	private EditText		mTextPassword;
	private EditText		mTextPasswordConfirm;
	@SuppressWarnings("unused")
	private EditText		mTextPasswordOld;
	private Button			mButtonSave;
	private User			mUser;
	private Integer			mUserId;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);

		initialize();

		/* Restore current tab */
		if (savedInstanceState != null) {
			if (findViewById(R.id.image_tab_host) != null) {
				mCommon.setActiveTab(((ViewGroup) findViewById(R.id.image_tab_host)).getChildAt(savedInstanceState.getInt("tab_index")));
				mViewFlipper.setDisplayedChild(mCommon.mTabIndex);
			}
		}

		bind();
		Tracker.trackPageView("/ProfileForm");

		if (savedInstanceState != null) {
			doRestoreInstanceState(savedInstanceState);
		}
	}

	protected void initialize() {

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mUserId = extras.getInt(getString(R.string.EXTRA_USER_ID));
		}

		mUser = Aircandi.getInstance().getUser();

		if (mUser == null && mUserId == null) {
			throw new IllegalStateException("User or userId must be passed to ProfileForm");
		}

		mViewFlipper = (ViewFlipper) findViewById(R.id.flipper_form);

		mImageUser = (WebImageView) findViewById(R.id.image_picture);
		mTextFullname = (EditText) findViewById(R.id.text_fullname);
		mTextEmail = (EditText) findViewById(R.id.text_email);
		mTextPassword = (EditText) findViewById(R.id.text_password);
		mTextPasswordOld = (EditText) findViewById(R.id.text_password_old);
		mTextPasswordConfirm = (EditText) findViewById(R.id.text_password_confirm);
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

		mTextPassword.addTextChangedListener(new SimpleTextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				mButtonSave.setEnabled(isValid());
			}
		});

		mTextPasswordConfirm.addTextChangedListener(new SimpleTextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				mButtonSave.setEnabled(isValid());
			}
		});

		if (mViewFlipper != null) {
			if (mCommon.mCommand.verb == CommandVerb.New) {
				mCommon.setActiveTab(((ViewGroup) findViewById(R.id.image_tab_host)).getChildAt(2));
			}
			else {
				mCommon.setActiveTab(((ViewGroup) findViewById(R.id.image_tab_host)).getChildAt(0));
			}
		}
	}

	protected void bind() {
		/*
		 * This form is always for editing. We always reload the user to make sure
		 * we have the freshest data.
		 */
		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showProgressDialog(true, "Loading...");
			}

			@Override
			protected Object doInBackground(Object... params) {

				Query query = new Query("Users").filter("Id eq '" + String.valueOf(mUserId) + "'");
				if (mUser != null) {
					query = new Query("Users").filter("Email eq '" + ((User) mUser).email + "'");
				}

				ServiceResponse serviceResponse = NetworkManager.getInstance().request(
						new ServiceRequest(ProxiConstants.URL_PROXIBASE_SERVICE, query, RequestType.Get, ResponseFormat.Json));
				return serviceResponse;
			}

			@Override
			protected void onPostExecute(Object result) {
				ServiceResponse serviceResponse = (ServiceResponse) result;
				if (serviceResponse.responseCode == ResponseCode.Success) {
					String jsonResponse = (String) serviceResponse.data;
					mUser = (User) ProxibaseService.convertJsonToObject(jsonResponse, User.class, GsonType.ProxibaseService);
					mImageUriOriginal = mUser.imageUri;
					mCommon.showProgressDialog(false, null);
					mCommon.stopTitlebarProgress();					
					draw();
					Tracker.dispatch();
				}
				else {
					mCommon.handleServiceError(serviceResponse);
				}
			}
		}.execute();
	}

	protected void draw() {

		mTextFullname.setText(mUser.name);
		mTextEmail.setText(mUser.email);

		if (mUser.imageUri != null && mUser.imageUri.length() > 0) {
			if (mUser.imageBitmap != null) {
				ImageUtils.showImageInImageView(mUser.imageBitmap, mImageUser.getImageView());
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

	public void onTabClick(View view) {
		mCommon.setActiveTab(view);
		if (view.getTag().equals("profile")) {
			mViewFlipper.setDisplayedChild(0);
		}
		else if (view.getTag().equals("account")) {
			mViewFlipper.setDisplayedChild(1);
		}
	}

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

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	protected void doSave() {

		if (validate()) {

			new AsyncTask() {

				@Override
				protected void onPreExecute() {
					mCommon.showProgressDialog(true, "Saving profile...");
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
					if (mUser.imageBitmap != null) {
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
						mUser.modifiedDate = (int) (DateUtils.nowDate().getTime() / 1000L);
						mUser.modifierId = Aircandi.getInstance().getUser().id;

						if (mTextPassword.getText().toString().length() != 0) {
							mUser.password = mTextPassword.getText().toString().trim();
						}

						Logger.i(this, "Updating user: " + mUser.name);

						ServiceRequest serviceRequest = new ServiceRequest();
						serviceRequest.setUri(mUser.getEntryUri());
						serviceRequest.setRequestType(RequestType.Update);
						serviceRequest.setRequestBody(ProxibaseService.convertObjectToJson((Object) mUser, GsonType.ProxibaseService));
						serviceRequest.setResponseFormat(ResponseFormat.Json);

						serviceResponse = NetworkManager.getInstance().request(serviceRequest);
					}
					return serviceResponse;
				}

				@Override
				protected void onPostExecute(Object response) {
					ServiceResponse serviceResponse = (ServiceResponse) response;

					if (serviceResponse.responseCode == ResponseCode.Success) {
						mCommon.showProgressDialog(false, null);
						Aircandi.getInstance().setUser(mUser);
						ImageUtils.showToastNotification(getString(R.string.alert_updated), Toast.LENGTH_SHORT);
						setResult(CandiConstants.RESULT_PROFILE_UPDATED);
						finish();
					}
					else {
						mCommon.handleServiceError(serviceResponse);
					}
				}
			}.execute();
		}
	}

	private boolean validate() {
		String alertMessage = "";
		if (mTextPassword.getText().toString().length() > 0) {
			if (mTextPassword.getText().toString().length() == 0) {
				alertMessage = getResources().getString(R.string.profile_alert_missing_old_password);
			}
			if (!mTextPassword.getText().toString().equals(mTextPasswordConfirm.getText().toString())) {
				alertMessage = getResources().getString(R.string.profile_alert_missmatched_passwords);
			}
			if (alertMessage.length() != 0) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(alertMessage).setCancelable(true).show();
				return false;
			}
		}
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
	// UI routines
	// --------------------------------------------------------------------------------------------

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
		if (findViewById(R.id.image_tab_host) != null) {
			savedInstanceState.putInt("tab_index", mCommon.mTabIndex);
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

	@Override
	protected int getLayoutID() {
		return R.layout.profile_form;
	}
}