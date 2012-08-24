package com.proxibase.aircandi;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.proxibase.aircandi.components.AnimUtils;
import com.proxibase.aircandi.components.CommandType;
import com.proxibase.aircandi.components.DateUtils;
import com.proxibase.aircandi.components.ImageUtils;
import com.proxibase.aircandi.components.IntentBuilder;
import com.proxibase.aircandi.components.Logger;
import com.proxibase.aircandi.components.NetworkManager;
import com.proxibase.aircandi.components.ProxiExplorer;
import com.proxibase.aircandi.components.AnimUtils.TransitionType;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.components.Tracker;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.widgets.AuthorBlock;
import com.proxibase.service.ProxiConstants;
import com.proxibase.service.ProxibaseService;
import com.proxibase.service.ProxibaseService.GsonType;
import com.proxibase.service.ProxibaseService.RequestType;
import com.proxibase.service.ProxibaseService.ResponseFormat;
import com.proxibase.service.ServiceRequest;
import com.proxibase.service.objects.Comment;
import com.proxibase.service.objects.User;

public class CommentForm extends FormActivity {

	private EditText	mContent;
	private Button		mButtonSave;
	private Comment		mComment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/*
		 * Two sign in cases:
		 * 
		 * - Currently anonymous.
		 * - Session expired.
		 */
		User user = Aircandi.getInstance().getUser();
		Boolean expired = false;
		Integer messageResId = R.string.signin_message_comment_new;
		if (user != null) {
			Boolean userAnonymous = user.anonymous;
			if (user.session != null) {
				expired = user.session.renewSession(DateUtils.nowDate().getTime());
			}
			if (userAnonymous || expired) {
				if (expired) {
					messageResId = R.string.signin_message_session_expired;
				}
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
		mContent = (EditText) findViewById(R.id.text_content);
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
		((AuthorBlock) findViewById(R.id.block_author)).bindToUser(Aircandi.getInstance().getUser(), null);
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
					mCommon.showProgressDialog(true, getString(R.string.progress_saving));
				}

				@Override
				protected Object doInBackground(Object... params) {

					// Construct entity, link, and observation
					Bundle parameters = new Bundle();
					parameters.putString("entityId", mCommon.mParentId);
					parameters.putString("comment", "object:" + ProxibaseService.convertObjectToJson(mComment, GsonType.ProxibaseService));

					ServiceRequest serviceRequest = new ServiceRequest();
					serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_METHOD + "insertComment")
							.setRequestType(RequestType.Method)
							.setParameters(parameters)
							.setSession(Aircandi.getInstance().getUser().session)
							.setResponseFormat(ResponseFormat.Json);

					ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);
					return serviceResponse;
				}

				@Override
				protected void onPostExecute(Object response) {

					ServiceResponse serviceResponse = (ServiceResponse) response;
					if (serviceResponse.responseCode == ResponseCode.Success) {
						Tracker.trackEvent("Comment", "Insert", null, 0);
						
						/* We need to push the comment into the entity model. */
						ProxiExplorer.getInstance().getEntityModel().insertCommentEverywhere(mComment, mCommon.mParentId);
						ProxiExplorer.getInstance().getEntityModel().setLastActivityDate(DateUtils.nowDate().getTime());
						
						mCommon.showProgressDialog(false, null);
						ImageUtils.showToastNotification(getString(R.string.alert_inserted), Toast.LENGTH_SHORT);
						setResult(CandiConstants.RESULT_COMMENT_INSERTED);
						finish();
					}
					else {
						mCommon.handleServiceError(serviceResponse);
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