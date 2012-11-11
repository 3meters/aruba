package com.aircandi;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.aircandi.components.AircandiCommon.ServiceOperation;
import com.aircandi.components.AnimUtils;
import com.aircandi.components.AnimUtils.TransitionType;
import com.aircandi.components.CommandType;
import com.aircandi.components.ImageUtils;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.ProxiExplorer.ModelResult;
import com.aircandi.components.Tracker;
import com.aircandi.core.CandiConstants;
import com.aircandi.service.objects.Comment;
import com.aircandi.service.objects.User;
import com.aircandi.widgets.UserView;

public class CommentForm extends FormActivity {

	private EditText	mContent;
	private Button		mButtonSave;
	private Comment		mComment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/*
		 * Signin required if user is anonymous
		 */
		User user = Aircandi.getInstance().getUser();
		Integer messageResId = R.string.signin_message_comment_new;
		if (user != null) {
			Boolean userAnonymous = user.isAnonymous();
			if (userAnonymous) {
				IntentBuilder intentBuilder = new IntentBuilder(this, SignInForm.class);
				intentBuilder.setCommandType(CommandType.Edit);
				intentBuilder.setMessage(getString(messageResId));
				Intent intent = intentBuilder.create();
				startActivityForResult(intent, CandiConstants.ACTIVITY_SIGNIN);
				AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageToForm);
				return;
			}
		}
		initialize();
		bind();
		draw();
	}

	private void initialize() {
		mContent = (EditText) findViewById(R.id.description);
		mButtonSave = (Button) findViewById(R.id.button_save);
		mButtonSave.setEnabled(false);

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
		mComment = new Comment();
		mComment.creatorId = Aircandi.getInstance().getUser().id;
		mComment.location = Aircandi.getInstance().getUser().location;
		mComment.imageUri = Aircandi.getInstance().getUser().imageUri;
		mComment.name = Aircandi.getInstance().getUser().name;
	}

	private void draw() {
		/* Author */
		((UserView) findViewById(R.id.author)).bindToUser(Aircandi.getInstance().getUser(), null);
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onSaveButtonClick(View view) {
		doSave();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {

		if (requestCode == CandiConstants.ACTIVITY_SIGNIN) {
			if (resultCode == Activity.RESULT_CANCELED) {
				setResult(resultCode);
				finish();
			}
			else {
				initialize();
				bind();
				draw();
			}
		}
		else {
			super.onActivityResult(requestCode, resultCode, intent);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	private void doSave() {
		insertComment();
	}

	private boolean validate() {
		/*
		 * This is where we would do any heavier validation before saving.
		 */
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
					mCommon.showProgressDialog(getString(R.string.progress_saving), true);
				}

				@Override
				protected Object doInBackground(Object... params) {

					ModelResult result = ProxiExplorer.getInstance().getEntityModel().insertComment(mCommon.mParentId, mComment, false);
					return result;
				}

				@Override
				protected void onPostExecute(Object response) {
					ModelResult result = (ModelResult) response;

					if (result.serviceResponse.responseCode == ResponseCode.Success) {
						Tracker.trackEvent("Comment", "Insert", null, 0);
						mCommon.hideProgressDialog();
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
		return R.layout.comment_form;
	}
}