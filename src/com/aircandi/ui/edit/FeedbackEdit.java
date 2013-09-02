package com.aircandi.ui.edit;

import java.util.HashMap;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.widget.Toast;

import com.aircandi.Aircandi;
import com.aircandi.R;
import com.aircandi.components.EntityManager;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.Tracker;
import com.aircandi.service.objects.Document;
import com.aircandi.ui.base.BaseEntityEdit;
import com.aircandi.ui.widgets.AirEditText;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.UI;

public class FeedbackEdit extends BaseEntityEdit {

	private Document	mDocument;

	@Override
	protected void initialize(Bundle savedInstanceState) {
		/*
		 * Feedback are not really an entity type so we handle
		 * all the expected initialization.
		 */
		mDescription = (AirEditText) findViewById(R.id.description);

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
		databind(null);
		draw();
	}

	@Override
	public void databind(Boolean refresh) {
		/*
		 * We are always creating a new comment.
		 */
		mDocument = new Document();
		mDocument.type = "feedback";
		mDocument.name = "aircandi";
		mDocument.data = new HashMap<String, Object>();
	}

	@Override
	public void draw() {
		((UserView) findViewById(R.id.created_by)).databind(Aircandi.getInstance().getUser(), null);
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
		mDocument.data.put("message", (Object) mDescription.getText().toString().trim());
	}

	// --------------------------------------------------------------------------------------------
	// Services
	// --------------------------------------------------------------------------------------------

	@Override
	protected boolean validate() {
		if (!super.validate()) {
			return false;
		}
		if (mDescription.getText().length() == 0) {
			Dialogs.alertDialog(android.R.drawable.ic_dialog_alert
					, null
					, getResources().getString(R.string.error_missing_message)
					, null
					, this
					, android.R.string.ok
					, null, null, null, null);
			return false;
		}
		return true;
	}

	@Override
	protected void insert() {
		Logger.i(this, "Insert feedback");

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mBusyManager.showBusy(R.string.progress_sending);
			}

			@Override
			protected Object doInBackground(Object... params) {
				Thread.currentThread().setName("InsertFeedback");
				mDocument.createdDate = DateTime.nowDate().getTime();
				final ModelResult result = EntityManager.getInstance().insertDocument(mDocument);
				return result;
			}

			@Override
			protected void onPostExecute(Object response) {
				final ModelResult result = (ModelResult) response;

				if (result.serviceResponse.responseCode == ResponseCode.Success) {
					Tracker.sendEvent("ui_action", "send_feedback", null, 0, Aircandi.getInstance().getUser());
					mBusyManager.hideBusy();
					UI.showToastNotification(getString(R.string.alert_feedback_sent), Toast.LENGTH_SHORT);
					finish();
				}
				else {
					Routing.serviceError(FeedbackEdit.this, result.serviceResponse);
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
		return R.layout.feedback_edit;
	}
}