package com.proxibase.aircandi;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;

import com.proxibase.aircandi.R;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.models.AlbumEntity;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;

public class AlbumForm extends EntityBase {

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
			mEntity = new AlbumEntity();
		}
		else if (mCommand.verb.equals("edit")) {
			String jsonResponse = null;
			try {
				jsonResponse = (String) ProxibaseService.getInstance().select(mEntityProxy.getEntryUri(), ResponseFormat.Json);
			}
			catch (ProxibaseException exception) {
				exception.printStackTrace();
			}

			mEntity = (AlbumEntity) ProxibaseService.convertJsonToObject(jsonResponse, AlbumEntity.class, GsonType.ProxibaseService);
		}

		super.bindEntity();

		final AlbumEntity entity = (AlbumEntity) mEntity;

		if (mCommand.verb.equals("new")) {
			entity.entityType = CandiConstants.TYPE_CANDI_FORUM;
		}
	}

	@Override
	protected void drawEntity() {
		super.drawEntity();

		final AlbumEntity entity = (AlbumEntity) mEntity;

		if (findViewById(R.id.chk_locked) != null) {
			((CheckBox) findViewById(R.id.chk_locked)).setVisibility(View.VISIBLE);
			((CheckBox) findViewById(R.id.chk_locked)).setChecked(entity.locked);
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
		final AlbumEntity entity = (AlbumEntity) mEntity;
		entity.locked = ((CheckBox) findViewById(R.id.chk_locked)).isChecked();
		super.insertEntity();

	}

	@Override
	protected void updateEntity() {
		final AlbumEntity entity = (AlbumEntity) mEntity;
		entity.locked = ((CheckBox) findViewById(R.id.chk_locked)).isChecked();
		super.updateEntity();
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------
	
	@Override
	protected int getLayoutID() {
		return R.layout.album_form;
	}
}