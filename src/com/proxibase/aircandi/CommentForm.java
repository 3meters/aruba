package com.proxibase.aircandi;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.proxibase.aircandi.components.Command;
import com.proxibase.aircandi.components.DateUtils;
import com.proxibase.aircandi.components.ImageUtils;
import com.proxibase.aircandi.components.IntentBuilder;
import com.proxibase.aircandi.components.Logger;
import com.proxibase.aircandi.components.NetworkManager;
import com.proxibase.aircandi.components.Tracker;
import com.proxibase.aircandi.components.Command.CommandVerb;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.widgets.AuthorBlock;
import com.proxibase.service.ProxibaseService;
import com.proxibase.service.ServiceRequest;
import com.proxibase.service.ProxibaseService.GsonType;
import com.proxibase.service.ProxibaseService.RequestType;
import com.proxibase.service.ProxibaseService.ResponseFormat;
import com.proxibase.service.consumer.Comment;
import com.proxibase.service.consumer.User;
import com.proxibase.service.util.ProxiConstants;

public class CommentForm extends FormActivity {

	private EditText	mContent;
	private Button		mButtonSave;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);

		User user = Aircandi.getInstance().getUser();
		if (user != null && user.anonymous) {

			IntentBuilder intentBuilder = new IntentBuilder(this, SignInForm.class);
			intentBuilder.setCommand(new Command(CommandVerb.Edit));
			intentBuilder.setMessage(getString(R.string.signin_message_new_candi));
			Intent intent = intentBuilder.create();

			startActivityForResult(intent, CandiConstants.ACTIVITY_SIGNIN);
			overridePendingTransition(R.anim.form_in, R.anim.browse_out);

		}

		initialize();
		bind();
		draw();

		/* Can overwrite any values from the original intent */
		if (savedInstanceState != null) {
			doRestoreInstanceState(savedInstanceState);
		}

		Tracker.trackPageView("/SignUpForm");
	}

	protected void initialize() {
		mContent = (EditText) findViewById(R.id.text_content);
		mButtonSave = (Button) findViewById(R.id.button_save);
		mButtonSave.setEnabled(false);

		mContent.addTextChangedListener(new SimpleTextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				mButtonSave.setEnabled(isValid());
			}
		});
	}

	protected void bind() {
		mCommon.mComment = new Comment();
		mCommon.mComment.creatorId = Aircandi.getInstance().getUser().id;
		mCommon.mComment.modifierId = Aircandi.getInstance().getUser().id;
		mCommon.mComment.entityId = mCommon.mParent;
	}

	protected void draw() {
		/* Author */
		((AuthorBlock) findViewById(R.id.block_author)).bindToUser(Aircandi.getInstance().getUser(), null);
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onSaveButtonClick(View view) {
		mCommon.startTitlebarProgress();
		doSave();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == CandiConstants.ACTIVITY_SIGNIN) {
			if (resultCode == Activity.RESULT_CANCELED) {
				setResult(resultCode);
				finish();
			}
			else {
				initialize();
				bind();
				draw();
				Tracker.trackPageView("/CommentForm");
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	protected void doSave() {
		if (!validate()) {
			return;
		}
		insert();
	}

	private boolean validate() {
		if (mContent.getText().toString().length() > 0) {
			return true;
		}
		return true;
	}

	protected void insert() {

		mCommon.mComment.description = mContent.getText().toString().trim();
		mCommon.mComment.createdDate = (int) (DateUtils.nowDate().getTime() / 1000L);
		mCommon.mComment.modifiedDate = mCommon.mComment.createdDate;

		Logger.i(this, "Insert comment for: " + String.valueOf(mCommon.mComment.entity));

		new AsyncTask() {

			@Override
			protected void onPreExecute() {
				mCommon.showProgressDialog(true, "Saving...");
			}

			@Override
			protected Object doInBackground(Object... params) {

				ServiceRequest serviceRequest = new ServiceRequest();
				serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE + mCommon.mComment.getCollection());
				serviceRequest.setRequestType(RequestType.Insert);
				serviceRequest.setRequestBody(ProxibaseService.convertObjectToJson((Object) mCommon.mComment, GsonType.ProxibaseService));
				serviceRequest.setResponseFormat(ResponseFormat.Json);

				ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest);
				return serviceResponse;
			}

			@Override
			protected void onPostExecute(Object response) {

				ServiceResponse serviceResponse = (ServiceResponse) response;
				if (serviceResponse.responseCode == ResponseCode.Success) {
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

	private boolean isValid() {
		if (mContent.getText().toString().length() > 0) {
			return true;
		}
		return false;
	}

	// --------------------------------------------------------------------------------------------
	// Persistence routines
	// --------------------------------------------------------------------------------------------

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		Logger.d(this, "onSaveInstanceState called");

		if (mContent != null) {
			savedInstanceState.putString("content", mContent.getText().toString());
		}
	}

	private void doRestoreInstanceState(Bundle savedInstanceState) {
		Logger.d(this, "Restoring previous state");

		if (mContent != null) {
			mContent.setText(savedInstanceState.getString("content"));
		}
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutID() {
		return R.layout.comment_form;
	}
}