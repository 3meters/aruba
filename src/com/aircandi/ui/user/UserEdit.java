package com.aircandi.ui.user;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
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

import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.beta.R;
import com.aircandi.components.AircandiCommon;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.EntityManager;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.Tracker;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequestBuilder;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.RequestListener;
import com.aircandi.service.objects.LinkOptions;
import com.aircandi.service.objects.LinkOptions.DefaultType;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Photo.PhotoSource;
import com.aircandi.service.objects.User;
import com.aircandi.ui.base.BaseEntityEdit;
import com.aircandi.ui.widgets.WebImageView;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;
import com.aircandi.utilities.DateUtils;
import com.aircandi.utilities.ImageUtils;
import com.aircandi.utilities.MiscUtils;

public class UserEdit extends BaseEntityEdit {

	private User			mEntity;
	
	private ViewFlipper		mViewFlipper;
	private WebImageView	mPhoto;
	private EditText		mName;
	private EditText		mBio;
	private EditText		mWebUri;
	private EditText		mArea;
	private EditText		mEmail;
	private CheckBox		mDoNotTrack;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		if (!isFinishing()) {
			initialize();
			bind();
		}
	}

	@Override
	protected void initialize() {

		mCommon.mActionBar.setDisplayHomeAsUpEnabled(true);
		mEntity = Aircandi.getInstance().getUser();

		if (mEntity == null) {
			throw new IllegalStateException("Current user required by ProfileForm");
		}

		mViewFlipper = (ViewFlipper) findViewById(R.id.flipper_form);
		mCommon.setViewFlipper(mViewFlipper);

		mPhoto = (WebImageView) findViewById(R.id.photo);
		mName = (EditText) findViewById(R.id.name);
		mBio = (EditText) findViewById(R.id.bio);
		mWebUri = (EditText) findViewById(R.id.web_uri);
		mArea = (EditText) findViewById(R.id.area);
		mEmail = (EditText) findViewById(R.id.email);
		mDoNotTrack = (CheckBox) findViewById(R.id.chk_do_not_track);

		if (mName != null) {
			mName.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					if (!s.toString().equals(mEntity.name)) {
						mDirty = true;
					}
				}
			});
		}
		if (mBio != null) {
			mBio.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					if (!s.toString().equals(mEntity.bio)) {
						mDirty = true;
					}
				}
			});
		}
		if (mWebUri != null) {
			mWebUri.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					if (!s.toString().equals(mEntity.webUri)) {
						mDirty = true;
					}
				}
			});
		}
		if (mArea != null) {
			mArea.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					if (!s.toString().equals(mEntity.location)) {
						mDirty = true;
					}
				}
			});
		}
		if (mEmail != null) {
			mEmail.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					if (!s.toString().equals(mEntity.email)) {
						mDirty = true;
					}
				}
			});
		}
		if (mDoNotTrack != null) {
			mDoNotTrack.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (mEntity.doNotTrack != isChecked) {
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
	protected void bind() {
		/*
		 * This form is always for editing. We always reload the user to make sure
		 * we have the freshest data.
		 */
		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showBusy(true);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("GetUser");
				ModelResult result = EntityManager.getInstance().getEntity(mEntity.id, true, LinkOptions.getDefault(DefaultType.LinksUserWatching));
				return result;
			}

			@Override
			protected void onPostExecute(Object modelResult) {
				final ModelResult result = (ModelResult) modelResult;
				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					mEntity = (User) result.serviceResponse.data;

					/* We got fresh user data but we want to hook up the old session. */
					mEntity.session = Aircandi.getInstance().getUser().session;
					mImageUriOriginal = mEntity.getPhotoUri();

					mCommon.hideBusy(true);
					draw();
				}
				else {
					mCommon.handleServiceError(result.serviceResponse, ServiceOperation.ProfileBrowse);
				}
			}
		}.execute();
	}

	@Override
	protected void draw() {

		drawPhoto();

		mName.setText(mEntity.name);
		mBio.setText(mEntity.bio);
		mWebUri.setText(mEntity.webUri);
		mArea.setText(mEntity.area);
		mEmail.setText(mEntity.email);
		if (mEntity.doNotTrack == null) {
			mEntity.doNotTrack = false;
		}
		mDoNotTrack.setChecked(mEntity.doNotTrack);
		if (mEntity.doNotTrack) {
			((TextView) findViewById(R.id.do_not_track_hint)).setText(R.string.form_do_not_track_on_hint);
		}
		else {
			((TextView) findViewById(R.id.do_not_track_hint)).setText(R.string.form_do_not_track_off_hint);
		}

		((ViewGroup) findViewById(R.id.flipper_form)).setVisibility(View.VISIBLE);

	}

	@Override
	protected void drawPhoto() {
		if (mPhoto != null) {
			if (mEntity.photo != null && mEntity.photo.hasBitmap()) {
				mPhoto.hideLoading();
				ImageUtils.showImageInImageView(mEntity.photo.getBitmap(), mPhoto.getImageView(), true, AnimUtils.fadeInMedium());
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
	public void onChangePictureButtonClick(View view) {

		//mCommon.showPhotoSourcePicker(mEntity.id, mEntity.schema, mEntity.type);
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
		AnimUtils.doOverridePendingTransition(this, TransitionType.PageToForm);
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
								defaultSearch = MiscUtils.emptyAsNull(mName.getText().toString().trim());
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
		mDirty = true;
		
		if (mEntity.photo != null) {
			mEntity.photo.removeBitmap();
			mEntity.photo = null;
		}
		mEntity.photo = mEntity.getDefaultPhoto();
		drawPhoto();
		Tracker.sendEvent("ui_action", "set_user_picture_to_default", null, 0, Aircandi.getInstance().getUser());
	}

	private void confirmDirtyExit() {
		final AlertDialog dialog = AircandiCommon.showAlertDialog(null
				, getResources().getString(R.string.alert_entity_dirty_exit_title)
				, getResources().getString(R.string.alert_entity_dirty_exit_message)
				, null
				, this
				, R.string.alert_dirty_save
				, android.R.string.cancel
				, R.string.alert_dirty_discard
				, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == Dialog.BUTTON_POSITIVE) {
							updateProfile();
						}
						else if (which == Dialog.BUTTON_NEUTRAL) {
							setResult(Activity.RESULT_CANCELED);
							finish();
							AnimUtils.doOverridePendingTransition(UserEdit.this, TransitionType.FormToPage);
						}
					}
				}
				, null);
		dialog.setCanceledOnTouchOutside(false);
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	private void updateProfile() {

		if (validate()) {

			mEntity.email = mEmail.getText().toString().trim();
			mEntity.name = mName.getText().toString().trim();
			mEntity.bio = mBio.getText().toString().trim();
			mEntity.area = mArea.getText().toString().trim();
			mEntity.webUri = mWebUri.getText().toString().trim();
			mEntity.doNotTrack = mDoNotTrack.isChecked();

			new AsyncTask() {

				@Override
				protected void onPreExecute() {
					mCommon.showBusy(R.string.progress_saving, true);
				}

				@Override
				protected Object doInBackground(Object... params) {
					Thread.currentThread().setName("UpdateUser");
					final ModelResult result = EntityManager.getInstance().updateUser(mEntity, mEntity.photo.getBitmap());
					return result;
				}

				@Override
				protected void onPostExecute(Object response) {
					final ModelResult result = (ModelResult) response;

					if (result.serviceResponse.responseCode == ResponseCode.Success) {
						Logger.i(this, "Updated user profile: " + mEntity.name + " (" + mEntity.id + ")");
						Tracker.sendEvent("ui_action", "update_user", null, 0, Aircandi.getInstance().getUser());
						mCommon.hideBusy(true);
						/*
						 * We treat updating the profile like a change to an entity in the entity model. This forces
						 * UI to update itself and pickup the changes like a new profile name, picture, etc.
						 */
						EntityManager.getEntityCache().setLastActivityDate(DateUtils.nowDate().getTime());

						/* Update the global user */
						Aircandi.getInstance().setUser(mEntity);
						/*
						 * We also need to update the user that has been persisted for auto sign in.
						 */
						final String jsonUser = HttpService.objectToJson(mEntity);
						Aircandi.settingsEditor.putString(Constants.SETTING_USER, jsonUser);
						Aircandi.settingsEditor.commit();

						ImageUtils.showToastNotification(getString(R.string.alert_updated), Toast.LENGTH_SHORT);
						setResult(Constants.RESULT_PROFILE_UPDATED);
						finish();
					}
					else {
						mCommon.handleServiceError(result.serviceResponse, ServiceOperation.ProfileUpdate);
					}
				}
			}.execute();
		}
	}

	@Override
	protected boolean validate() {
		if (!MiscUtils.validEmail(mEmail.getText().toString())) {
			mCommon.showAlertDialogSimple(null, getString(R.string.error_invalid_email));
			return false;
		}
		if (mWebUri.getText().toString() != null && !mWebUri.getText().toString().equals("")) {
			if (!MiscUtils.validWebUri(mWebUri.getText().toString())) {
				AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert
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
	// Application menu routines (settings)
	// --------------------------------------------------------------------------------------------

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.accept) {
			updateProfile();
			return true;
		}
		else if (item.getItemId() == R.id.cancel) {
			if (isDirty()) {
				confirmDirtyExit();
			}
			else {
				setResult(Activity.RESULT_CANCELED);
				finish();
				AnimUtils.doOverridePendingTransition(UserEdit.this, TransitionType.FormToPage);
			}
			return true;
		}

		/* In case we add general menu items later */
		mCommon.doOptionsItemSelected(item);
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.user_edit;
	}
}