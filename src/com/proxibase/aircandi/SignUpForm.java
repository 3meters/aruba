package com.proxibase.aircandi;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.utils.DateUtils;
import com.proxibase.aircandi.utils.Exceptions;
import com.proxibase.aircandi.utils.ImageManager;
import com.proxibase.aircandi.utils.ImageUtils;
import com.proxibase.aircandi.utils.Logger;
import com.proxibase.aircandi.utils.S3;
import com.proxibase.aircandi.utils.ImageLoader.ImageProfile;
import com.proxibase.aircandi.utils.ImageManager.ImageRequestListener;
import com.proxibase.sdk.android.proxi.consumer.User;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.IQueryListener;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;

public class SignUpForm extends AircandiActivity {

	private EditText	mTextFullname;
	private EditText	mTextEmail;
	private EditText	mTextPassword;
	private EditText	mTextPasswordConfirm;
	private ImageView	mImageUser;
	private Button		mButtonSignUp;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);

		configure();
		bind();
		draw();
	}

	protected void configure() {
		mImageUser = (ImageView) findViewById(R.id.img_public_image);
		mTextFullname = (EditText) findViewById(R.id.txt_fullname);
		mTextEmail = (EditText) findViewById(R.id.txt_email);
		mTextPassword = (EditText) findViewById(R.id.txt_password);
		mTextPasswordConfirm = (EditText) findViewById(R.id.txt_password_confirm);
		mButtonSignUp = (Button) findViewById(R.id.btn_signup);
		mButtonSignUp.setEnabled(false);

		mTextFullname.addTextChangedListener(new SimpleTextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				mButtonSignUp.setEnabled(isValid());
			}
		});

		mTextEmail.addTextChangedListener(new SimpleTextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				mButtonSignUp.setEnabled(isValid());
			}

		});

		mTextPassword.addTextChangedListener(new SimpleTextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				mButtonSignUp.setEnabled(isValid());
			}

		});

		mTextPasswordConfirm.addTextChangedListener(new SimpleTextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				mButtonSignUp.setEnabled(isValid());
			}

		});
	}

	protected void bind() {
		mUser = new User();
		mUser.imageUri = "resource:placeholder_user";
		mUser.imageBitmap = ImageManager.getInstance().loadBitmapFromResources(R.attr.placeholder_user);
	}

	protected void draw() {
		/*
		 * We only want to enable the save button when there is something in all
		 * the required fields: fullname, email, password
		 */
		if (mUser.imageUri != null && mUser.imageUri.length() > 0) {
			if (mUser.imageBitmap != null) {
				mImageUser.setImageBitmap(mUser.imageBitmap);
			}
			else {
				ImageManager.getInstance().getImageLoader().fetchImageByProfile(ImageProfile.SquareTile, mUser.imageUri, new ImageRequestListener() {

					@Override
					public void onImageReady(Bitmap bitmap) {
						Logger.d(SignUpForm.this, "User picture fetched: " + mUser.imageUri);
						mUser.imageBitmap = bitmap;
						showPicture(bitmap, R.id.img_public_image);
					}
				});
			}
		}

	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onSignUpButtonClick(View view) {
		startTitlebarProgress();
		doSave();
	}

	public void onCancelButtonClick(View view) {
		setResult(Activity.RESULT_CANCELED);
		finish();
	}

	public void onChangePictureButtonClick(View view) {
		showChangePictureDialog(false, new ImageRequestListener() {

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
						showPicture(mUser.imageBitmap, R.id.img_public_image);
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
		insertEntity();
	}

	private boolean validate() {
		if (!mTextPassword.getText().toString().equals(mTextPasswordConfirm.getText().toString())) {
			Aircandi.showAlertDialog(android.R.drawable.ic_dialog_alert, "Password", "Passwords do not match", this, null);
			return false;
		}
		return true;
	}

	protected void updateImages() {
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
					ImageUtils.showToastNotification(this, getString(R.string.post_update_failed_toast), Toast.LENGTH_SHORT);
					exception.printStackTrace();
				}
				mUser.imageUri = CandiConstants.URL_AIRCANDI_MEDIA + CandiConstants.S3_BUCKET_IMAGES + "/" + imageKey;
			}
		}
	}

	protected void insertEntity() {

		mUser.email = mTextEmail.getText().toString().trim();
		mUser.fullname = mTextFullname.getText().toString().trim();
		mUser.password = mTextPassword.getText().toString().trim();
		mUser.createdDate = DateUtils.nowString();

		mUser.insertAsync(new IQueryListener() {

			@Override
			public void onComplete(String jsonResponse) {

				/* Load the just insert user to get the user id */
				try {
					mUser = ProxibaseService.getInstance().loadUser(mUser.email);

					/* Delete or upload images to S3 as needed. */
					if (mUser.imageUri != null && !mUser.imageUri.contains("resource:") && mUser.imageBitmap != null) {
						String imageKey = String.valueOf(mUser.id) + "_"
													+ String.valueOf(DateUtils.nowString(DateUtils.DATE_NOW_FORMAT_FILENAME))
													+ ".jpg";
						try {
							S3.putImage(imageKey, mUser.imageBitmap);
						}
						catch (ProxibaseException exception) {
							if (!Exceptions.Handle(exception)) {
								ImageUtils.showToastNotification(SignUpForm.this, getString(R.string.post_update_failed_toast), Toast.LENGTH_SHORT);
							}
						}
						mUser.imageUri = CandiConstants.URL_AIRCANDI_MEDIA + CandiConstants.S3_BUCKET_IMAGES + "/" + imageKey;
						mUser.update(); /* Need to update the user to capture the uri for the image we saved */
					}

					runOnUiThread(new Runnable() {

						public void run() {
							Logger.i(SignUpForm.this, "Inserted new user: " + mUser.fullname
																							+ " ("
																							+ mUser.id
																							+ ")");
							stopTitlebarProgress();
							Aircandi.showAlertDialog(R.drawable.icon_app, "New user created",
									"A message has been sent to your email address with instructions on how to activate your account.",
									SignUpForm.this, new
									OnClickListener() {

										public void onClick(DialogInterface dialog, int which) {
											finish();
										}
									});

						}
					});
				}
				catch (ProxibaseException exception) {
					Exceptions.Handle(exception);
				}
			}

			@Override
			public void onProxibaseException(ProxibaseException exception) {
				if (!Exceptions.Handle(exception)) {
					ImageUtils.showToastNotification(getApplicationContext(), getString(R.string.post_insert_failed_toast), Toast.LENGTH_SHORT);
				}
			}
		});
	}

	private boolean isValid() {
		/*
		 * Could be either a check for a new user or an update to an existing user
		 */
		if (mTextFullname.getText().length() == 0) {
			return false;
		}
		if (mTextEmail.getText().length() == 0) {
			return false;
		}
		if (mTextPassword.getText().length() == 0) {
			return false;
		}
		if (mTextPasswordConfirm.getText().length() == 0) {
			return false;
		}
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutID() {
		return R.layout.signup_form;
	}
}