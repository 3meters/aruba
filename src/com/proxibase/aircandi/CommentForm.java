package com.proxibase.aircandi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.proxibase.aircandi.components.DateUtils;
import com.proxibase.aircandi.components.ImageUtils;
import com.proxibase.aircandi.components.Logger;
import com.proxibase.aircandi.components.NetworkManager;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.widgets.AuthorBlock;
import com.proxibase.sdk.android.proxi.consumer.Comment;
import com.proxibase.sdk.android.proxi.consumer.User;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ServiceRequest;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestListener;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;
import com.proxibase.sdk.android.util.ProxiConstants;

public class CommentForm extends CandiActivity {

	private EditText	mContent;
	private Button		mButtonSave;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);

		configure();
		bind();
		draw();
		GoogleAnalyticsTracker.getInstance().trackPageView("/SignUpForm");
	}

	protected void configure() {
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
		mComment = new Comment();
		mComment.createdById = String.valueOf(((User) mUser).id);
		mComment.entityId = mParentEntityId;
	}

	protected void draw() {
		/* Author */
		if (mUser != null) {
			((AuthorBlock) findViewById(R.id.block_author)).bindToUser(mUser, mComment.createdDate != null ? DateUtils
					.wcfToDate(mComment.createdDate) : null);
		}
		else {
			((AuthorBlock) findViewById(R.id.block_author)).setVisibility(View.GONE);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onSaveButtonClick(View view) {
		startTitlebarProgress();
		doSave();
	}

	public void onCancelButtonClick(View view) {
		setResult(Activity.RESULT_CANCELED);
		finish();
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	protected void doSave() {
		if (!validate()) {
			return;
		}
		insertComment();
	}

	private boolean validate() {
		if (mContent.getText().toString().length() > 0) {
			return true;
		}
		return true;
	}

	protected void insertComment() {

		mComment.description = mContent.getText().toString().trim();
		mComment.createdDate = DateUtils.nowString();

		Logger.i(this, "Insert comment for: " + String.valueOf(mComment.entityId));

		ServiceRequest serviceRequest = new ServiceRequest();
		serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_ODATA + mComment.getCollection());
		serviceRequest.setRequestType(RequestType.Insert);
		serviceRequest.setRequestBody(ProxibaseService.convertObjectToJson((Object) mComment, GsonType.ProxibaseService));
		serviceRequest.setResponseFormat(ResponseFormat.Json);
		serviceRequest.setRequestListener(new RequestListener() {

			@Override
			public void onComplete(Object response) {

				ServiceResponse serviceResponse = (ServiceResponse) response;
				if (serviceResponse.responseCode != ResponseCode.Success) {
					ImageUtils.showToastNotification(getString(R.string.alert_insert_failed), Toast.LENGTH_SHORT);
				}
				else {
					ImageUtils.showToastNotification(getString(R.string.alert_inserted), Toast.LENGTH_SHORT);
					Intent intent = new Intent();

					/* We are editing so set the dirty flag */
					intent.putExtra(getString(R.string.EXTRA_ENTITY_DIRTY), mComment.entityId);
					intent.putExtra(getString(R.string.EXTRA_RESULT_VERB), Verb.Edit);
					setResult(Activity.RESULT_FIRST_USER, intent);
					finish();
				}

			}
		});

		NetworkManager.getInstance().requestAsync(serviceRequest);
	}

	private boolean isValid() {
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