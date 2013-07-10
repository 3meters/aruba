package com.aircandi.ui.user;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.beta.R;
import com.aircandi.components.EntityManager;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.TabManager;
import com.aircandi.components.Tracker;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.RequestListener;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Photo.PhotoSource;
import com.aircandi.service.objects.User;
import com.aircandi.ui.base.BaseEntityEdit;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.Animate.TransitionType;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.UI;
import com.aircandi.utilities.Utilities;

public class UserEdit extends BaseEntityEdit {

	private EditText	mBio;
	private EditText	mWebUri;
	private EditText	mArea;
	private EditText	mEmail;
	private CheckBox	mDoNotTrack;

	private TabManager	mTabManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mEntity = Aircandi.getInstance().getUser();

		if (mEntity == null) {
			throw new IllegalStateException("Current user required by ProfileForm");
		}

		mTabManager = new TabManager(Constants.TABS_USER_FORM_ID, mActionBar, (ViewFlipper) findViewById(R.id.flipper_form));
		mTabManager.initialize();
		mTabManager.doRestoreInstanceState(savedInstanceState);

		mBio = (EditText) findViewById(R.id.bio);
		mWebUri = (EditText) findViewById(R.id.web_uri);
		mArea = (EditText) findViewById(R.id.area);
		mEmail = (EditText) findViewById(R.id.email);
		mDoNotTrack = (CheckBox) findViewById(R.id.chk_do_not_track);

		if (mBio != null) {
			mBio.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					if (!s.toString().equals(((User) mEntity).bio)) {
						mDirty = true;
					}
				}
			});
		}
		if (mWebUri != null) {
			mWebUri.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					if (!s.toString().equals(((User) mEntity).webUri)) {
						mDirty = true;
					}
				}
			});
		}
		if (mArea != null) {
			mArea.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					if (!s.toString().equals(((User) mEntity).area)) {
						mDirty = true;
					}
				}
			});
		}
		if (mEmail != null) {
			mEmail.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					if (!s.toString().equals(((User) mEntity).email)) {
						mDirty = true;
					}
				}
			});
		}
		if (mDoNotTrack != null) {
			mDoNotTrack.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (((User) mEntity).doNotTrack != isChecked) {
						mDirty = true;
					}

					if (isChecked) {
						((TextView) findViewById(R.id.do_not_track_hint)).setText(R.string.form_do_not_track_on_hint);
					}
					else {
						((TextView) findViewById(R.id.do_not_track_hint)).setText(R.string.form_do_not_track_off_hint);
					}
				}
			});
		}

	}

	@Override
	protected void draw() {
		super.draw();

		User user = (User) mEntity;

		mBio.setText(user.bio);
		mWebUri.setText(user.webUri);
		mArea.setText(user.area);
		mEmail.setText(user.email);
		if (user.doNotTrack == null) {
			user.doNotTrack = false;
		}
		mDoNotTrack.setChecked(user.doNotTrack);
		if (user.doNotTrack) {
			((TextView) findViewById(R.id.do_not_track_hint)).setText(R.string.form_do_not_track_on_hint);
		}
		else {
			((TextView) findViewById(R.id.do_not_track_hint)).setText(R.string.form_do_not_track_off_hint);
		}

		((ViewGroup) findViewById(R.id.flipper_form)).setVisibility(View.VISIBLE);

	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

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
						mEntity.photo.setBitmap(imageKey, imageBitmap); // Could get set to null if we are using the default 
						mEntity.photo.setBitmapLocalOnly(bitmapLocalOnly);
					}
					drawPhoto();
				}
			}
		};
	}

	@SuppressWarnings("ucd")
	public void onChangePasswordButtonClick(View view) {
		final IntentBuilder intentBuilder = new IntentBuilder(this, PasswordEdit.class);
		final Intent intent = intentBuilder.create();
		startActivity(intent);
		Animate.doOverridePendingTransition(this, TransitionType.PageToForm);
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
							if (mName != null) {
								defaultSearch = Utilities.emptyAsNull(mName.getText().toString().trim());
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

	@Override
	protected void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		mTabManager.doSaveInstanceState(savedInstanceState);
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	@Override
	protected void gather() {
		super.gather();

		User user = (User) mEntity;
		user.email = mEmail.getText().toString().trim();
		user.bio = mBio.getText().toString().trim();
		user.area = mArea.getText().toString().trim();
		user.webUri = mWebUri.getText().toString().trim();
		user.doNotTrack = mDoNotTrack.isChecked();
	}

	@Override
	protected boolean validate() {
		if (!super.validate()) {
			return false;
		}

		if (!Utilities.validEmail(mEmail.getText().toString())) {
			Dialogs.alertDialogSimple(this, null, getString(R.string.error_invalid_email));
			return false;
		}
		if (mWebUri.getText().toString() != null && !mWebUri.getText().toString().equals("")) {
			if (!Utilities.validWebUri(mWebUri.getText().toString())) {
				Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
						, null
						, getResources().getString(R.string.error_weburi_invalid)
						, null
						, this
						, android.R.string.ok
						, null, null, null, null);
				return false;
			}
		}
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Services
	// --------------------------------------------------------------------------------------------

	@Override
	protected void update() {

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusyManager.showBusy(R.string.progress_saving);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("UpdateUser");
				final ModelResult result = EntityManager.getInstance().updateUser((User) mEntity, mEntity.photo.getBitmap());
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					Logger.i(this, "Updated user profile: " + mEntity.name + " (" + mEntity.id + ")");
					Tracker.sendEvent("ui_action", "update_user", null, 0, Aircandi.getInstance().getUser());
					mBusyManager.hideBusy();
					/*
					 * We treat updating the profile like a change to an entity in the entity model. This forces
					 * UI to update itself and pickup the changes like a new profile name, picture, etc.
					 */
					EntityManager.getEntityCache().setLastActivityDate(DateTime.nowDate().getTime());

					/* Update the global user */
					Aircandi.getInstance().setUser((User) mEntity);
					/*
					 * We also need to update the user that has been persisted for auto sign in.
					 */
					final String jsonUser = HttpService.objectToJson(mEntity);
					Aircandi.settingsEditor.putString(Constants.SETTING_USER, jsonUser);
					Aircandi.settingsEditor.commit();

					UI.showToastNotification(getString(R.string.alert_updated), Toast.LENGTH_SHORT);
					setResult(Constants.RESULT_PROFILE_UPDATED);
					finish();
				}
				else {
					Routing.serviceError(UserEdit.this, result.serviceResponse);
				}
			}
		}.execute();
	}

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.user_edit;
	}
}