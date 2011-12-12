package com.proxibase.aircandi;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.utils.DateUtils;
import com.proxibase.aircandi.utils.Exceptions;
import com.proxibase.aircandi.utils.ImageManager;
import com.proxibase.aircandi.utils.ImageUtils;
import com.proxibase.aircandi.utils.Logger;
import com.proxibase.aircandi.utils.S3;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest;
import com.proxibase.aircandi.utils.ImageManager.ImageRequestListener;
import com.proxibase.aircandi.utils.ImageManager.ImageRequest.ImageShape;
import com.proxibase.aircandi.widgets.WebImageView;
import com.proxibase.sdk.android.proxi.consumer.User;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.Query;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.IQueryListener;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;
import com.proxibase.sdk.android.util.ProxiConstants;

public class ProfileForm extends AircandiActivity {

	private FormTab			mActiveTab	= FormTab.Profile;
	private ViewFlipper		mViewFlipper;
	private int				mTextColorFocused;
	private int				mTextColorUnfocused;
	private int				mHeightActive;
	private int				mHeightInactive;
	private ImageView		mImageProfileTab;
	private ImageView		mImageAccountTab;
	private TextView		mTextProfileTab;
	private TextView		mTextAccountTab;

	private WebImageView	mImageUser;
	private EditText		mTextFullname;
	private EditText		mTextEmail;
	private EditText		mTextPassword;
	private EditText		mTextPasswordConfirm;
	@SuppressWarnings("unused")
	private EditText		mTextPasswordOld;
	private Button			mButtonSave;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);

		configure();
		bind();
		draw();
	}

	protected void configure() {
		mViewFlipper = (ViewFlipper) findViewById(R.id.flipper_form);
		mImageProfileTab = (ImageView) findViewById(R.id.image_tab_profile);
		mImageAccountTab = (ImageView) findViewById(R.id.image_tab_account);
		mTextProfileTab = (TextView) findViewById(R.id.text_tab_profile);
		mTextAccountTab = (TextView) findViewById(R.id.text_tab_account);

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

		TypedValue resourceName = new TypedValue();
		if (this.getTheme().resolveAttribute(R.attr.textColorFocused, resourceName, true)) {
			mTextColorFocused = Color.parseColor((String) resourceName.coerceToString());
		}

		if (this.getTheme().resolveAttribute(R.attr.textColorUnfocused, resourceName, true)) {
			mTextColorUnfocused = Color.parseColor((String) resourceName.coerceToString());
		}

		mHeightActive = ImageUtils.getRawPixelsForDisplayPixels(6);
		mHeightInactive = ImageUtils.getRawPixelsForDisplayPixels(2);
		if (mViewFlipper != null) {
			if (mCommand.verb.equals("new")) {
				setActiveTab(FormTab.Account);
			}
			else {
				setActiveTab(FormTab.Profile);
			}
		}
	}

	protected void bind() {
		/*
		 * This form is always for editing. We always reload the user to make sure
		 * we have the freshest data.
		 */
		String jsonResponse = null;
		try {
			Query query = new Query("Users").filter("Email eq '" + ((User) mUser).email + "'");
			jsonResponse = (String) ProxibaseService.getInstance().selectUsingQuery(query, ProxiConstants.URL_AIRCANDI_SERVICE_ODATA);
			mUser = (User) ProxibaseService.convertJsonToObject(jsonResponse, User.class, GsonType.ProxibaseService);
			mImageUriOriginal = mUser.imageUri;
		}
		catch (ProxibaseException exception) {
			Exceptions.Handle(exception);
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
				ImageRequest imageRequest = new ImageRequest(mUser.imageUri, ImageShape.Square, "binary", false,
						CandiConstants.IMAGE_WIDTH_MAX, false, true, true, 1, this, new ImageRequestListener() {

							@Override
							public void onImageReady(Bitmap bitmap) {
								mUser.imageBitmap = bitmap;
							}
						});

				mImageUser.setImageRequest(imageRequest, null);
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onProfileTabClick(View view) {
		if (mActiveTab != FormTab.Profile) {
			setActiveTab(FormTab.Profile);
		}
	}

	public void onAccountTabClick(View view) {
		if (mActiveTab != FormTab.Account) {
			setActiveTab(FormTab.Account);
		}
	}

	public void onSaveButtonClick(View view) {
		startTitlebarProgress();
		doSave();
	}

	public void onCancelButtonClick(View view) {
		setResult(Activity.RESULT_CANCELED);
		finish();
	}

	public void onChangePictureButtonClick(View view) {
		showChangePictureDialog((mUser.facebookId != null && mUser.facebookId.length() != 0), new ImageRequestListener() {

			@Override
			public void onImageReady(final Bitmap bitmap) {
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						if (bitmap == null) {
							mUser.imageUri = "resource:placeholder_user";
							mUser.imageBitmap = ImageManager.getInstance().loadBitmapFromResources(R.attr.placeholder_user);
						}
						else {
							mUser.imageUri = "updated";
							mUser.imageBitmap = bitmap;
						}
						ImageUtils.showImageInImageView(bitmap, mImageUser);
					}
				});
			}

			@Override
			public void onProxibaseException(ProxibaseException exception) {
				/* Do nothing */
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
		updateEntity();
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
					ImageUtils.showToastNotification(this, getString(R.string.alert_update_failed), Toast.LENGTH_SHORT);
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
					ImageUtils.showToastNotification(this, getString(R.string.alert_update_failed), Toast.LENGTH_SHORT);
					exception.printStackTrace();
				}
				mUser.imageUri = CandiConstants.URL_AIRCANDI_MEDIA + CandiConstants.S3_BUCKET_IMAGES + "/" + imageKey;
			}
		}
	}

	protected void updateEntity() {

		mUser.email = mTextEmail.getText().toString().trim();
		mUser.fullname = mTextFullname.getText().toString().trim();
		if (mTextPassword.getText().toString().length() != 0) {
			mUser.password = mTextPassword.getText().toString().trim();
		}

		Logger.i(this, "Updating user: " + mUser.fullname);
		mUser.updateAsync(new IQueryListener() {

			@Override
			public void onComplete(String response) {

				/* Post the processed result back to the UI thread */
				runOnUiThread(new Runnable() {

					public void run() {
						stopTitlebarProgress();
						ImageUtils.showToastNotification(getApplicationContext(), getString(R.string.alert_updated), Toast.LENGTH_SHORT);
						Intent intent = new Intent();
						//mUser.imageBitmap.recycle();
						mUser.imageBitmap = null;
						String jsonUser = ProxibaseService.getGson(GsonType.Internal).toJson(mUser);
						if (!jsonUser.equals("")) {
							intent.putExtra(getString(R.string.EXTRA_USER), jsonUser);
						}

						setResult(Activity.RESULT_FIRST_USER, intent);
						finish();
					}
				});
			}

			@Override
			public void onProxibaseException(ProxibaseException exception) {
				if (!Exceptions.Handle(exception)) {
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							ImageUtils.showToastNotification(getApplicationContext(), getString(R.string.alert_update_failed),
									Toast.LENGTH_SHORT);
						}
					});
				}
			}
		});

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

	private void setActiveTab(FormTab formTab) {

		mTextProfileTab.setTextColor(mTextColorUnfocused);
		mTextAccountTab.setTextColor(mTextColorUnfocused);

		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, mHeightInactive);
		params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);

		mImageProfileTab.setLayoutParams(params);
		mImageAccountTab.setLayoutParams(params);

		params = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, mHeightActive);
		params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);

		if (formTab == FormTab.Profile) {
			mTextProfileTab.setTextColor(mTextColorFocused);
			mImageProfileTab.setLayoutParams(params);
		}
		else if (formTab == FormTab.Account) {
			mTextAccountTab.setTextColor(mTextColorFocused);
			mImageAccountTab.setLayoutParams(params);
		}
		mViewFlipper.setDisplayedChild(formTab.ordinal());
		mActiveTab = formTab;
	}

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