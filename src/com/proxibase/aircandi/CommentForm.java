package com.proxibase.aircandi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.proxibase.aircandi.models.Comment;
import com.proxibase.aircandi.utils.NetworkManager;
import com.proxibase.aircandi.utils.NetworkManager.ResultCode;
import com.proxibase.aircandi.utils.NetworkManager.ServiceResponse;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ServiceRequest;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;

public class CommentForm extends EntityBaseForm {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		bindEntity();
		super.drawEntity();
	}

	@Override
	protected void bindEntity() {

		/* We handle all the elements that are different than the base entity. */
		if (mCommand.verb.equals("new")) {
			mEntity = new Comment();
		}
		else if (mCommand.verb.equals("edit")) {
			ServiceResponse serviceResponse = NetworkManager.getInstance().request(
					new ServiceRequest(mEntityProxy.getEntryUri(), RequestType.Get, ResponseFormat.Json));

			if (serviceResponse.resultCode != ResultCode.Success) {
				setResult(Activity.RESULT_CANCELED);
				finish();
				overridePendingTransition(R.anim.hold, R.anim.fade_out_medium);
			}
			else {
				String jsonResponse = (String) serviceResponse.data;
				mEntity = (Comment) ProxibaseService.convertJsonToObject(jsonResponse, Comment.class, GsonType.ProxibaseService);
				GoogleAnalyticsTracker.getInstance().dispatch();
			}
		}

		super.bindEntity();

	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutID() {
		return R.layout.comment_form;

	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}
}