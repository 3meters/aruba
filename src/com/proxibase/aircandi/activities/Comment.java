package com.proxibase.aircandi.activities;

import android.content.Intent;
import android.os.Bundle;

import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.models.CommentEntity;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;

public class Comment extends EntityBase {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		bindEntity();
		super.drawEntity();
	}

	@Override
	protected void bindEntity() {
		/*
		 * We handle all the elements that are different than the base entity.
		 */
		if (mVerb == Verb.New) {
			mEntity = new CommentEntity();
		}
		else if (mVerb == Verb.Edit) {
			String jsonResponse = null;
			try {
				jsonResponse = (String) ProxibaseService.getInstance().select(mEntityProxy.getEntryUri(), ResponseFormat.Json);
			}
			catch (ProxibaseException exception) {
				exception.printStackTrace();
			}

			mEntity = (CommentEntity) ProxibaseService.convertJsonToObject(jsonResponse, CommentEntity.class, GsonType.ProxibaseService);
		}

		super.bindEntity();

		if (mVerb == Verb.New) {
			final CommentEntity entity = (CommentEntity) mEntity;
			entity.entityType = CandiConstants.TYPE_CANDI_COMMENT;
		}
		else if (mVerb == Verb.Edit) {
		}
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutID() {
		return R.layout.comment;

	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}
}