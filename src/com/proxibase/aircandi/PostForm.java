package com.proxibase.aircandi;

import android.widget.TextView;

import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.models.PostEntity;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;

public class PostForm extends EntityBaseForm {

	@Override
	protected void bindEntity() {

		/* We handle all the elements that are different than the base entity. */
		if (mCommand.verb.equals("new")) {
			PostEntity entity = new PostEntity();
			entity.entityType = CandiConstants.TYPE_CANDI_POST;
			if (mParentEntityId != 0) {
				entity.parentEntityId = mParentEntityId;
			}
			else {
				entity.parentEntityId = null;
			}
			entity.enabled = true;
			mEntity = entity;
		}
		else if (mCommand.verb.equals("edit")) {
			String jsonResponse = null;
			try {
				jsonResponse = (String) ProxibaseService.getInstance().select(mEntityProxy.getEntryUri(), ResponseFormat.Json);
				mEntity = (PostEntity) ProxibaseService.convertJsonToObject(jsonResponse, PostEntity.class, GsonType.ProxibaseService);
			}
			catch (ProxibaseException exception) {
				exception.printStackTrace();
			}
		}
		super.bindEntity();
	}

	@Override
	protected void drawEntity() {
		super.drawEntity();

		((TextView) findViewById(R.id.txt_header_title)).setText(getResources().getString(R.string.form_title_post));
	}

	@Override
	protected void doSave(boolean updateImages) {
		super.doSave(true);
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutID() {
		return R.layout.post_form;
	}
}