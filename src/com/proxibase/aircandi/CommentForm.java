package com.proxibase.aircandi;

import android.content.Intent;
import android.os.Bundle;

import com.proxibase.aircandi.R;
import com.proxibase.aircandi.models.Comment;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;
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
			String jsonResponse = null;
			try {
				jsonResponse = (String) ProxibaseService.getInstance().select(mEntityProxy.getEntryUri(), ResponseFormat.Json);
			}
			catch (ProxibaseException exception) {
				exception.printStackTrace();
			}

			mEntity = (Comment) ProxibaseService.convertJsonToObject(jsonResponse, Comment.class, GsonType.ProxibaseService);
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