package com.proxibase.aircandi;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.models.ForumEntity;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;

public class ForumForm extends EntityBaseForm {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		bindEntity();
		drawEntity();
	}

	@Override
	protected void bindEntity() {

		/* We handle all the elements that are different than the base entity. */
		if (mCommand.verb.equals("new")) {
			mEntity = new ForumEntity();
			((ForumEntity) mEntity).entityType = CandiConstants.TYPE_CANDI_FORUM;
		}
		else if (mCommand.verb.equals("edit")) {
			String jsonResponse = null;
			try {
				jsonResponse = (String) ProxibaseService.getInstance().select(mEntityProxy.getEntryUri(), ResponseFormat.Json);
				mEntity = (ForumEntity) ProxibaseService.convertJsonToObject(jsonResponse, ForumEntity.class, GsonType.ProxibaseService);
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

		((TextView) findViewById(R.id.txt_header_title)).setText(getResources().getString(R.string.form_title_topic));

		if (findViewById(R.id.chk_locked) != null) {
			((CheckBox) findViewById(R.id.chk_locked)).setVisibility(View.VISIBLE);
			((CheckBox) findViewById(R.id.chk_locked)).setChecked(((ForumEntity) mEntity).locked);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected void doSave() {
		super.doSave();

		/* Delete or upload images to S3 as needed. */
		updateImages();

		if (mCommand.verb.equals("new")) {
			insertEntity();
		}
		else if (mCommand.verb.equals("edit")) {
			updateEntity();
		}
	}

	@Override
	protected void insertEntity() {
		final ForumEntity entity = (ForumEntity) mEntity;
		entity.locked = ((CheckBox) findViewById(R.id.chk_locked)).isChecked();
		super.insertEntity();

	}

	@Override
	protected void updateEntity() {
		final ForumEntity entity = (ForumEntity) mEntity;
		entity.locked = ((CheckBox) findViewById(R.id.chk_locked)).isChecked();
		super.updateEntity();
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutID() {
		return R.layout.forum_form;
	}
}