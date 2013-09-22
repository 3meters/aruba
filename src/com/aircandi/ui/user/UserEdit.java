package com.aircandi.ui.user;

import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.components.TabManager;
import com.aircandi.service.objects.User;
import com.aircandi.ui.base.BaseEntityEdit;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.Routing.Route;
import com.aircandi.utilities.Type;
import com.aircandi.utilities.Utilities;

public class UserEdit extends BaseEntityEdit {

	private EditText	mBio;
	private EditText	mWebUri;
	private EditText	mArea;
	private EditText	mEmail;
	private CheckBox	mDoNotTrack;

	private TabManager	mTabManager;

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);

		mTabManager = new TabManager(Constants.TABS_USER_EDIT_ID, mActionBar, (ViewFlipper) findViewById(R.id.flipper_form));
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
						if (!mFirstDraw) {
							mDirty = true;
						}
					}
				}
			});
		}
		if (mWebUri != null) {
			mWebUri.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					if (!s.toString().equals(((User) mEntity).webUri)) {
						if (!mFirstDraw) {
							mDirty = true;
						}
					}
				}
			});
		}
		if (mArea != null) {
			mArea.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					if (!s.toString().equals(((User) mEntity).area)) {
						if (!mFirstDraw) {
							mDirty = true;
						}
					}
				}
			});
		}
		if (mEmail != null) {
			mEmail.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					if (!s.toString().equals(((User) mEntity).email)) {
						if (!mFirstDraw) {
							mDirty = true;
						}
					}
				}
			});
		}
		if (mDoNotTrack != null) {
			mDoNotTrack.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (((User) mEntity).doNotTrack != isChecked) {
						if (!mFirstDraw) {
							mDirty = true;
						}
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
	public void draw() {
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
	public void onChangePasswordButtonClick(View view) {
		Routing.route(this, Route.PASSWORD_CHANGE);
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
	protected String getLinkType() {
		return null;
	}

	@Override
	protected void gather() {
		super.gather();

		User user = (User) mEntity;
		user.email = Type.emptyAsNull(mEmail.getText().toString().trim());;
		user.bio = Type.emptyAsNull(mBio.getText().toString().trim());
		user.area = Type.emptyAsNull(mArea.getText().toString().trim());
		user.webUri = Type.emptyAsNull(mWebUri.getText().toString().trim());
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

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.user_edit;
	}
}