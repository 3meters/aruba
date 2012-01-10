package com.proxibase.aircandi;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
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
import com.proxibase.aircandi.components.ImageManager;
import com.proxibase.aircandi.components.ImageUtils;
import com.proxibase.aircandi.components.Logger;
import com.proxibase.aircandi.components.NetworkManager;
import com.proxibase.aircandi.components.S3;
import com.proxibase.aircandi.components.Tracker;
import com.proxibase.aircandi.components.Command.CommandVerb;
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
		bind();
		draw();
		Tracker.trackPageView("/ProfileForm");
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
				mCommon.setActiveTab(((ViewGroup) findViewById(R.id.image_tab_host)).getChildAt(1));		
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
		Query query = null;
		if (mUser != null) {
			query = new Query("Users").filter("Email eq '" + ((User) mUser).email + "'");
		}
		else {
			query = new Query("Users").filter("Id eq '" + String.valueOf(mUserId) + "'");
		}

		ServiceResponse serviceResponse = NetworkManager.getInstance().request(
				new ServiceRequest(ProxiConstants.URL_AIRCANDI_SERVICE_ODATA, query, RequestType.Get, ResponseFormat.Json));

		if (serviceResponse.responseCode != ResponseCode.Success) {
			setResult(Activity.RESULT_CANCELED);
			finish();
			overridePendingTransition(R.anim.hold, R.anim.fade_out_medium);
		}
		else {
			String jsonResponse = (String) serviceResponse.data;
			mUser = (User) ProxibaseService.convertJsonToObject(jsonResponse, User.class, GsonType.ProxibaseService);
			mImageUriOriginal = mUser.imageUri;
			Tracker.dispatch();
		}
	}

	protected void draw() {

		mTextFullname.setText(mUser.fullname);
		mTextEmail.setText(mUser.email);

		if (mUser.imageUri != null && mUser.imageUri.length() > 0) {
			if (mUser.imageBitmap != null) {
				mImageUser.setImageBitmap(mUser.imageBitmap);
			}
			else {
				ImageRequest imageRequest = new ImageRequest(mUser.imageUri, null, ImageShape.Square, false, false,
						CandiConstants.IMAGE_WIDTH_SEARCH_MAX, false, true, true, 1, this, new RequestListener() {

							@Override
							public void onComplete(Object response) {

								ServiceResponse serviceResponse = (ServiceResponse) response;
								if (serviceResponse.responseCode != ResponseCode.Success) {
									return;
								}
								else {
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
		mCommon.startTitlebarProgress();
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
		updateImages();
		update();
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

	protected void updateImages() {

		/* Delete image from S3 if it has been orphaned */
		if (mImageUriOriginal != null && !ImageManager.isLocalImage(mImageUriOriginal)) {
			if (!mUser.imageUri.equals(mImageUriOriginal)) {
				try {
					S3.deleteImage(mImageUriOriginal.substring(mImageUriOriginal.lastIndexOf("/") + 1));
					ImageManager.getInstance().deleteImage(mImageUriOriginal);
					ImageManager.getInstance().deleteImage(mImageUriOriginal + ".reflection");
				}
				catch (ProxibaseException exception) {
					ImageUtils.showToastNotification(getString(R.string.alert_update_failed), Toast.LENGTH_SHORT);
					exception.printStackTrace();
				}
			}
		}

		/* Put image to S3 if we have a new one. */
		if (mUser.imageUri != null && !ImageManager.isLocalImage(mUser.imageUri)) {
			if (!mUser.imageUri.equals(mImageUriOriginal) && mUser.imageBitmap != null) {
				String imageKey = String.valueOf(((User) mUser).id) + "_"
									+ String.valueOf(DateUtils.nowString(DateUtils.DATE_NOW_FORMAT_FILENAME))
									+ ".jpg";
				try {
					S3.putImage(imageKey, mUser.imageBitmap);
				}
				catch (ProxibaseException exception) {
					ImageUtils.showToastNotification(getString(R.string.alert_update_failed), Toast.LENGTH_SHORT);
					exception.printStackTrace();
				}
				mUser.imageUri = CandiConstants.URL_AIRCANDI_MEDIA + CandiConstants.S3_BUCKET_IMAGES + "/" + imageKey;
			}
		}
	}

	protected void update() {

		mUser.email = mTextEmail.getText().toString().trim();
		mUser.fullname = mTextFullname.getText().toString().trim();
		if (mTextPassword.getText().toString().length() != 0) {
			mUser.password = mTextPassword.getText().toString().trim();
		}

		Logger.i(this, "Updating user: " + mUser.fullname);

		ServiceRequest serviceRequest = new ServiceRequest();
		serviceRequest.setUri(mUser.getEntryUri());
		serviceRequest.setRequestType(RequestType.Update);
		serviceRequest.setRequestBody(ProxibaseService.convertObjectToJson((Object) mUser, GsonType.ProxibaseService));
		serviceRequest.setResponseFormat(ResponseFormat.Json);
		serviceRequest.setRequestListener(new RequestListener() {

			@Override
			public void onComplete(Object response) {

				mCommon.stopTitlebarProgress();
				ServiceResponse serviceResponse = (ServiceResponse) response;
				if (serviceResponse.responseCode != ResponseCode.Success) {
					ImageUtils.showToastNotification(getString(R.string.alert_update_failed), Toast.LENGTH_SHORT);
					return;
				}
				else {
					ImageUtils.showToastNotification(getString(R.string.alert_updated), Toast.LENGTH_SHORT);
					Intent intent = new Intent();
					mUser.imageBitmap = null;

					String jsonUser = ProxibaseService.getGson(GsonType.Internal).toJson(mUser);
					if (jsonUser.length() > 0) {
						intent.putExtra(getString(R.string.EXTRA_USER), jsonUser);
					}

					setResult(CandiConstants.RESULT_PROFILE_UPDATED);
					finish();
				}
			}
		});

		NetworkManager.getInstance().requestAsync(serviceRequest);
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
	// Misc routines
	// --------------------------------------------------------------------------------------------

	protected void onDestroy() {

		/* This activity gets destroyed everytime we leave using back or finish(). */
		try {
			if (mUser != null && mUser.imageBitmap != null) {
				mUser.imageBitmap.recycle();
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

	protected enum FormTab {
		Profile,
		Account
	}
}