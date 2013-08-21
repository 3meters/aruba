package com.aircandi.ui.edit;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Contacts;
import android.text.Editable;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.aircandi.Aircandi;
import com.aircandi.R;
import com.aircandi.components.EntityManager;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.Tracker;
import com.aircandi.ui.base.BaseEntityEdit;
import com.aircandi.ui.widgets.AirEditText;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.UI;

public class InviteEdit extends BaseEntityEdit {

	private AirEditText			mEmail;
	private static final int	CONTACT_PICKER_RESULT	= 1001;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void initialize(Bundle savedInstanceState) {
		/*
		 * Feedback are not really an entity type so we have to handle
		 * all the expected initialization.
		 */
		mDescription = (AirEditText) findViewById(R.id.description);
		mEmail = (AirEditText) findViewById(R.id.email);

		if (mDescription != null) {
			mDescription.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					mDirty = false;
					if (s.toString() != null || !s.toString().equals("")) {
						mDirty = true;
					}
				}
			});
		}

		if (mEmail != null) {
			mEmail.addTextChangedListener(new SimpleTextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					mDirty = false;
					if (s.toString() != null || !s.toString().equals("")) {
						mDirty = true;
					}
				}
			});
		}
	}

	@Override
	protected void draw() {
		((UserView) findViewById(R.id.created_by)).databind(Aircandi.getInstance().getUser(), null);
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("ucd")
	public void onSearchButtonClick(View view) {
		Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
		startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != Activity.RESULT_CANCELED) {
			switch (requestCode) {
				case CONTACT_PICKER_RESULT:
					Cursor cursor = null;
					String email = "";
					try {
						Uri result = data.getData();
						Logger.v(this, "Got a contact result: " + result.toString());

						// get the contact id from the Uri
						String id = result.getLastPathSegment();

						// query for everything email
						cursor = getContentResolver().query(Email.CONTENT_URI
								, null
								, Email.CONTACT_ID + "=?"
								, new String[] { id }
								, null);

						int emailIdx = cursor.getColumnIndex(Email.DATA);

						// let's just get the first email
						if (cursor.moveToFirst()) {
							email = cursor.getString(emailIdx);
							Logger.v(this, "Got email: " + email);
						}
						else {
							Logger.w(this, "No results");
						}
					}
					catch (Exception e) {
						Logger.e(this, "Failed to get email data", e);
					}
					finally {
						if (cursor != null) {
							cursor.close();
						}
						if (email.length() == 0) {
							UI.showToastNotification("No email found for contact.", Toast.LENGTH_LONG);
						}
						else {
							String currentEmail = mEmail.getEditableText().toString().trim();
							Boolean existingEmail = (currentEmail != null && !currentEmail.equals(""));
							mEmail.getEditableText().append(existingEmail ? ", " + email : email);
						}
					}
					break;
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	@Override
	public void onAccept() {
		if (validate()) {
			super.onAccept();
		}
	}

	@Override
	protected String getLinkType() {
		return null;
	}

	@Override
	protected boolean validate() {
		if (!super.validate()) {
			return false;
		}
		if (mEmail.getText().length() == 0) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_missing_invite_email)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}
		return true;
	}

	@Override
	protected void gather() {
		/*
		 * Do nothing
		 */
	}

	@Override
	protected void insert() {
		Logger.i(this, "Send invite");

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusyManager.showBusy(R.string.progress_sending);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("SendInvite");
				String email = mEmail.getEditableText().toString();
				String fullname = Aircandi.getInstance().getUser().name;
				String message = mDescription.getEditableText().toString();
				final ModelResult result = EntityManager.getInstance().sendInvite(email, fullname, message);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					Tracker.sendEvent("ui_action", "send_invite", null, 0, Aircandi.getInstance().getUser());
					mBusyManager.hideBusy();
					UI.showToastNotification(getString(R.string.alert_invite_sent), Toast.LENGTH_SHORT);
					finish();
				}
				else {
					Routing.serviceError(InviteEdit.this, result.serviceResponse);
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
		return R.layout.invite_edit;
	}
}