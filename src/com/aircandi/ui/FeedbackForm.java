package com.aircandi.ui;

import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi;
import com.aircandi.beta.R;
import com.aircandi.components.AircandiCommon;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.EntityManager;
import com.aircandi.components.FontManager;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.Tracker;
import com.aircandi.service.objects.Document;
import com.aircandi.ui.base.FormActivity;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;
import com.aircandi.utilities.DateUtils;
import com.aircandi.utilities.ImageUtils;

public class FeedbackForm extends FormActivity {

	private EditText	mContent;
	private Document	mDocument;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!isFinishing()) {
			initialize();
			bind();
			draw();
		}
	}

	private void initialize() {
		mContent = (EditText) findViewById(R.id.description);
		FontManager.getInstance().setTypefaceDefault(mContent);

		mContent.setImeOptions(EditorInfo.IME_ACTION_SEND);
		mContent.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_SEND) {
					insertFeedback();
					return true;
				}
				return false;
			}
		});
	}

	private void bind() {
		/*
		 * We are always creating a new comment.
		 */

		mDocument = new Document();
		mDocument.type = "feedback";
		mDocument.name = "aircandi";
		mDocument.data = new HashMap<String, Object>();

	}

	private void draw() {
		/* Author */
		((UserView) findViewById(R.id.author)).bindToUser(Aircandi.getInstance().getUser(), null);
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
			finish();
			AnimUtils.doOverridePendingTransition(this, TransitionType.FormToPage);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	private void doSave() {
		insertFeedback();
	}

	private Boolean isDirty() {
		if (mContent.getText().toString().length() > 0) {
			return true;
		}
		return false;
	}

	private void confirmDirtyExit() {
		final AlertDialog dialog = AircandiCommon.showAlertDialog(null
				, getResources().getString(R.string.alert_feedback_dirty_exit_title)
				, getResources().getString(R.string.alert_feedback_dirty_exit_message)
				, null
				, this
				, R.string.alert_dirty_save
				, android.R.string.cancel
				, R.string.alert_dirty_discard
				, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == Dialog.BUTTON_POSITIVE) {
							doSave();
						}
						else if (which == Dialog.BUTTON_NEUTRAL) {
							setResult(Activity.RESULT_CANCELED);
							finish();
							AnimUtils.doOverridePendingTransition(FeedbackForm.this, TransitionType.FormToPage);
						}
					}
				}
				, null);
		dialog.setCanceledOnTouchOutside(false);
	}

	private boolean validate() {
		if (mContent.getText().length() == 0) {
			AircandiCommon.showAlertDialog(android.R.drawable.ic_dialog_alert
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

	private void insertFeedback() {

		if (validate()) {

			mDocument.data.put("message", (Object) mContent.getText().toString().trim());

			Logger.i(this, "Insert feedback");

			new AsyncTask() {

				@Override
				protected void onPreExecute() {
					mCommon.showBusy(R.string.progress_sending, true);
				}

				@Override
				protected Object doInBackground(Object... params) {
					Thread.currentThread().setName("InsertFeedback");
					mDocument.createdDate = DateUtils.nowDate().getTime();
					final ModelResult result = EntityManager.getInstance().insertDocument(mDocument);
					return result;
				}

				@Override
				protected void onPostExecute(Object response) {
					final ModelResult result = (ModelResult) response;

					if (result.serviceResponse.responseCode == ResponseCode.Success) {
						Tracker.sendEvent("ui_action", "send_feedback", null, 0, Aircandi.getInstance().getUser());
						mCommon.hideBusy(true);
						ImageUtils.showToastNotification(getString(R.string.alert_feedback_sent), Toast.LENGTH_SHORT);
						finish();
					}
					else {
						mCommon.handleServiceError(result.serviceResponse, ServiceOperation.FeedbackSave);
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
		if (item.getItemId() == R.id.save) {
			doSave();
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
		return R.layout.feedback_form;
	}
}