package com.aircandi.ui;

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
import android.widget.Toast;
import android.widget.TextView.OnEditorActionListener;

import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi;
import com.aircandi.CandiConstants;
import com.aircandi.beta.R;
import com.aircandi.components.AircandiCommon;
import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.FontManager;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProxiManager;
import com.aircandi.components.ProxiManager.ModelResult;
import com.aircandi.components.Tracker;
import com.aircandi.service.objects.Comment;
import com.aircandi.ui.base.FormActivity;
import com.aircandi.ui.widgets.UserView;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;
import com.aircandi.utilities.ImageUtils;

public class CommentForm extends FormActivity {

	private EditText	mContent;
	private Comment		mComment;

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
					insertComment();
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
		mComment = new Comment();
		mComment.creatorId = Aircandi.getInstance().getUser().id;
		mComment.location = Aircandi.getInstance().getUser().location;
		mComment.imageUri = Aircandi.getInstance().getUser().getPhoto().getUri();
		mComment.name = Aircandi.getInstance().getUser().name;
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
			setResult(Activity.RESULT_CANCELED);
			finish();
			AnimUtils.doOverridePendingTransition(this, TransitionType.FormToPage);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	private void doSave() {
		insertComment();
	}

	private Boolean isDirty() {
		if (mContent.getText().toString().length() > 0) {
			return true;
		}
		return false;
	}

	private void confirmDirtyExit() {
		final AlertDialog dialog = AircandiCommon.showAlertDialog(null
				, getResources().getString(R.string.alert_comment_dirty_exit_title)
				, getResources().getString(R.string.alert_comment_dirty_exit_message)
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
							AnimUtils.doOverridePendingTransition(CommentForm.this, TransitionType.FormToPage);
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
					, null, null, null);
			return false;
		}
		return true;
	}

	private void insertComment() {

		if (validate()) {

			/* TODO: Add title */
			mComment.description = mContent.getText().toString().trim();

			Logger.i(this, "Insert comment for: " + String.valueOf(mCommon.mParentId));

			new AsyncTask() {

				@Override
				protected void onPreExecute() {
					mCommon.showBusy(R.string.progress_saving, true);
				}

				@Override
				protected Object doInBackground(Object... params) {
					Thread.currentThread().setName("InsertComment");
					final ModelResult result = ProxiManager.getInstance().getEntityModel().insertComment(mCommon.mParentId, mComment, false);
					return result;
				}

				@Override
				protected void onPostExecute(Object response) {
					final ModelResult result = (ModelResult) response;

					if (result.serviceResponse.responseCode == ResponseCode.Success) {
						Tracker.sendEvent("ui_action", "add_comment", null, 0);
						mCommon.hideBusy(true);
						ImageUtils.showToastNotification(getString(R.string.alert_inserted), Toast.LENGTH_SHORT);
						setResult(CandiConstants.RESULT_COMMENT_INSERTED);
						finish();
					}
					else {
						mCommon.handleServiceError(result.serviceResponse, ServiceOperation.CommentSave);
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
	protected int getLayoutID() {
		return R.layout.comment_form;
	}
}