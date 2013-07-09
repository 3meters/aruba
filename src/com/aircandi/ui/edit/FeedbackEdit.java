package com.aircandi.ui.edit;

import java.util.HashMap;

import android.os.AsyncTask;
import android.widget.Toast;

import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi;
import com.aircandi.beta.R;
import com.aircandi.components.EntityManager;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.Tracker;
import com.aircandi.service.objects.Document;
import com.aircandi.ui.base.BaseEntityEdit;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Dialogs;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.UI;

public class FeedbackEdit extends BaseEntityEdit {

	private Document	mDocument;

	@Override
	protected void bind() {
		/*
		 * We are always creating a new comment.
		 */
		mDocument = new Document();
		mDocument.type = "feedback";
		mDocument.name = "aircandi";
		mDocument.data = new HashMap<String, Object>();

	}

	@Override
	protected void draw() {
		((UserView) findViewById(R.id.created_by)).bindToUser(Aircandi.getInstance().getUser(), null);
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	private void doSave() {
		insertFeedback();
	}

	@Override
	protected boolean validate() {
		if (super.validate()) {
			if (mDescription.getText().length() == 0) {
				Dialogs.showAlertDialog(android.R.drawable.ic_dialog_alert
						, null
						, getResources().getString(R.string.error_missing_message)
						, null
						, this
						, android.R.string.ok
						, null, null, null, null);
				return false;
			}
		}
		return true;
	}

	private void insertFeedback() {

		if (validate()) {

			mDocument.data.put("message", (Object) mDescription.getText().toString().trim());

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
	}

	// --------------------------------------------------------------------------------------------
	// Application menu routines (settings)
	// --------------------------------------------------------------------------------------------

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.accept) {
			doSave();
			return true;
		}

		/* In case we add general menu items later */
		super.onOptionsItemSelected(item);
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.feedback_edit;
	}
}