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
import com.aircandi.components.CommandType;
import com.aircandi.components.FontManager;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProxiManager;
import com.aircandi.components.ProxiManager.ModelResult;
import com.aircandi.components.Tracker;
import com.aircandi.components.bitmaps.BitmapRequest;
import com.aircandi.components.bitmaps.BitmapRequestBuilder;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpService.RequestListener;
import com.aircandi.service.objects.Photo;
import com.aircandi.service.objects.Photo.PhotoSource;
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
	private WebImageView	mImage;
	private EditText		mTextFullname;
	private CheckBox		mCheckBrowseInPrivate;
	private EditText		mTextBio;
	private EditText		mTextLink;
	private EditText		mTextLocation;
	private EditText		mTextEmail;
	private User			mUser;
	private Bitmap			mBitmap;
	private Boolean			mDirty	= false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		if (!isFinishing()) {
			initialize();
			bind();
		}
	}

	private void initialize() {

		mCommon.mActionBar.setDisplayHomeAsUpEnabled(true);
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
		mCheckBrowseInPrivate = (CheckBox) findViewById(R.id.chk_do_not_track);

		FontManager.getInstance().setTypefaceDefault(mTextFullname);
		FontManager.getInstance().setTypefaceDefault(mTextBio);
		FontManager.getInstance().setTypefaceDefault(mTextLink);
		FontManager.getInstance().setTypefaceDefault(mTextLocation);
		FontManager.getInstance().setTypefaceDefault(mTextEmail);
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_change_image));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_change_password));
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.do_not_track_hint));

		if (mTextFullname != null) {
			mTextFullname.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					if (!s.toString().equals(mUser.name)) {
						mDirty = true;
					}
				}
			});
		}
		if (mTextBio != null) {
			mTextBio.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					if (!s.toString().equals(mUser.bio)) {
						mDirty = true;
					}
				}
			});
		}
		if (mTextLink != null) {
			mTextLink.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					if (!s.toString().equals(mUser.webUri)) {
						mDirty = true;
					}
				}
			});
		}
		if (mTextLocation != null) {
			mTextLocation.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					if (!s.toString().equals(mUser.location)) {
						mDirty = true;
					}
				}
			});
		}
		if (mTextEmail != null) {
			mTextEmail.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					if (!s.toString().equals(mUser.email)) {
						mDirty = true;
					}
				}
			});
		}
		if (mCheckBrowseInPrivate != null) {
			mCheckBrowseInPrivate.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (mUser.doNotTrack != isChecked) {
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

	private void bind() {
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
				final ModelResult result = ProxiManager.getInstance().getEntityModel().getUser(mUser.id, true);
				return result;
			}

			@Override
			protected void onPostExecute(Object modelResult) {
				final ModelResult result = (ModelResult) modelResult;
				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					mUser = (User) result.serviceResponse.data;

					/* We got fresh user data but we want to hook up the old session. */
					mUser.session = Aircandi.getInstance().getUser().session;
					mImageUriOriginal = mUser.getPhotoUri();

					mCommon.hideBusy(true);
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
		mTextLocation.setText(mUser.area);
		mTextEmail.setText(mUser.email);
		if (mUser.doNotTrack == null) {
			mUser.doNotTrack = false;
		}
		mCheckBrowseInPrivate.setChecked(mUser.doNotTrack);
		if (mUser.doNotTrack) {
			((TextView) findViewById(R.id.do_not_track_hint)).setText(R.string.form_do_not_track_on_hint);
		}
		else {
			((TextView) findViewById(R.id.do_not_track_hint)).setText(R.string.form_do_not_track_off_hint);
		}

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
				final BitmapRequestBuilder builder = new BitmapRequestBuilder(mImage);
				builder.setImageUri(user.getPhotoUri());
				final BitmapRequest imageRequest = builder.create();
				mImage.setBitmapRequest(imageRequest);
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------
	@Override
	public void onBackPressed() {
		if (isDirty()) {
			confirmDirtyExit();
		}
		else {
			setResult(Activity.RESULT_CANCELED);
			finish();
			AnimUtils.doOverridePendingTransition(ProfileForm.this, TransitionType.FormToPage);
		}
	}

	@SuppressWarnings("ucd")
	public void onChangePictureButtonClick(View view) {

		mCommon.showPictureSourcePicker(null, null);
		mImageRequestWebImageView = mImage;
		mImageRequestListener = new RequestListener() {

			@Override
			public void onComplete(Object response, Photo photo, String imageUri, Bitmap imageBitmap, String title, String description, Boolean bitmapLocalOnly) {

				final ServiceResponse serviceResponse = (ServiceResponse) response;
				if (serviceResponse.responseCode == ResponseCode.Success) {
					/* Could get set to null if we are using the default */
					mDirty = true;
					mBitmap = imageBitmap;
					if (photo != null) {
						mUser.photo = photo;
					}
					else if (imageUri != null) {
						mUser.photo = new Photo(imageUri, null, null, null, PhotoSource.aircandi);
					}
					drawImage(mUser);
				}
			}
		};
	}

	@SuppressWarnings("ucd")
	public void onChangePasswordButtonClick(View view) {
		final IntentBuilder intentBuilder = new IntentBuilder(this, PasswordForm.class);
		intentBuilder.setCommandType(CommandType.Edit);
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
						if (pictureSource.equals("search")) {
							String defaultSearch = null;
							if (mTextFullname != null) {
								defaultSearch = MiscUtils.emptyAsNull(mTextFullname.getText().toString().trim());
							}
							pictureSearch(defaultSearch);
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
		mDirty = true;
		if (user.photo != null) {
			user.photo.setBitmap(null);
			user.photo = null;
		}
		mBitmap = null;
		drawImage(user);
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
							AnimUtils.doOverridePendingTransition(ProfileForm.this, TransitionType.FormToPage);
						}
					}
				}
				, null);
		dialog.setCanceledOnTouchOutside(false);
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	private Boolean isDirty() {
		return mDirty;
	}

	private void updateProfile() {

		if (validate()) {

			mUser.email = mTextEmail.getText().toString().trim();
			mUser.name = mTextFullname.getText().toString().trim();
			mUser.bio = mTextBio.getText().toString().trim();
			mUser.area = mTextLocation.getText().toString().trim();
			mUser.webUri = mTextLink.getText().toString().trim();
			mUser.doNotTrack = mCheckBrowseInPrivate.isChecked();

			new AsyncTask() {

				@Override
				protected void onPreExecute() {
					mCommon.showBusy(R.string.progress_saving, true);
				}

				@Override
				protected Object doInBackground(Object... params) {
					Thread.currentThread().setName("UpdateUser");
					final ModelResult result = ProxiManager.getInstance().getEntityModel().updateUser(mUser, mBitmap, false);
					return result;
				}

				@Override
				protected void onPostExecute(Object response) {
					final ModelResult result = (ModelResult) response;

					if (result.serviceResponse.responseCode == ResponseCode.Success) {
						Logger.i(this, "Updated user profile: " + mUser.name + " (" + mUser.id + ")");
						Tracker.sendEvent("ui_action", "update_user", null, 0, Aircandi.getInstance().getUser());
						mCommon.hideBusy(true);
						/*
						 * We treat updating the profile like a change to an entity in the entity model. This forces
						 * UI to update itself and pickup the changes like a new profile name, picture, etc.
						 */
						ProxiManager.getInstance().getEntityModel().setLastActivityDate(DateUtils.nowDate().getTime());

						/* Update the global user */
						Aircandi.getInstance().setUser(mUser);
						/*
						 * We also need to update the user that has been persisted for auto sign in.
						 */
						final String jsonUser = HttpService.convertObjectToJsonSmart(mUser, true, false);
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
				AnimUtils.doOverridePendingTransition(ProfileForm.this, TransitionType.FormToPage);
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
		return R.layout.profile_form;
	}
}