package com.aircandi.ui;

import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.aircandi.Aircandi;
import com.aircandi.R;
import com.aircandi.components.AircandiCommon;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.FontManager;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.ProxiExplorer.ModelResult;
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
	private Button		mButtonSave;
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

		mButtonSave = (Button) findViewById(R.id.button_save);
		mButtonSave.setEnabled(false);

		FontManager.getInstance().setTypefaceDefault(mContent);
		FontManager.getInstance().setTypefaceDefault(mButtonSave);
		FontManager.getInstance().setTypefaceDefault((TextView) findViewById(R.id.button_cancel));

		mContent.addTextChangedListener(new SimpleTextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				mButtonSave.setEnabled(enableSave());
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

	@SuppressWarnings("ucd")
	public void onSaveButtonClick(View view) {
		doSave();
	}

	@Override
	public void onCancelButtonClick(View view) {
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
		AlertDialog dialog = AircandiCommon.showAlertDialog(null
				, getResources().getString(R.string.alert_feedback_dirty_exit_title)
				, getResources().getString(R.string.alert_feedback_dirty_exit_message)
				, null
				, this
				, android.R.string.ok
				, android.R.string.cancel
				, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == Dialog.BUTTON_POSITIVE) {
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
		/*
		 * This is where we would do any heavier validation before saving.
		 */
		return true;
	}

	private void insertFeedback() {

		if (validate()) {

			mDocument.data.put("message", (Object) mContent.getText().toString().trim());

			Logger.i(this, "Insert feedback");

			new AsyncTask() {

				@Override
				protected void onPreExecute() {
					mCommon.showBusy(R.string.progress_sending);
				}

				@Override
				protected Object doInBackground(Object... params) {
					Thread.currentThread().setName("InsertFeedback");
					mDocument.createdDate = DateUtils.nowDate().getTime();
					ModelResult result = ProxiExplorer.getInstance().getEntityModel().insertDocument(mDocument);
					return result;
				}

				@Override
				protected void onPostExecute(Object response) {
					ModelResult result = (ModelResult) response;

					if (result.serviceResponse.responseCode == ResponseCode.Success) {
						Tracker.sendEvent("ui_action", "send_feedback", null, 0);
						mCommon.hideBusy(false);
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

	private boolean enableSave() {
		if (mContent.getText().toString().length() > 0) {
			return true;
		}
		return false;
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutID() {
		return R.layout.feedback_form;
	}
}