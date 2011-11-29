package com.proxibase.aircandi;

import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.models.AlbumEntity;
import com.proxibase.aircandi.utils.Exceptions;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;

public class AlbumForm extends EntityBaseForm {

	@Override
	protected void bindEntity() {

		/* We handle all the elements that are different than the base entity. */
		if (mCommand.verb.equals("new")) {
			AlbumEntity entity = new AlbumEntity();
			entity.entityType = CandiConstants.TYPE_CANDI_ALBUM;
			entity.parentEntityId = null;
			entity.enabled = true;
			mEntity = entity;
		}
		else if (mCommand.verb.equals("edit")) {
			String jsonResponse = null;
			try {
				jsonResponse = (String) ProxibaseService.getInstance().select(mEntityProxy.getEntryUri(), ResponseFormat.Json);
				mEntity = (AlbumEntity) ProxibaseService.convertJsonToObject(jsonResponse, AlbumEntity.class, GsonType.ProxibaseService);
			}
			catch (ProxibaseException exception) {
				Exceptions.Handle(exception);
			}
		}
		super.bindEntity();
	}

	@Override
	protected void drawEntity() {
		super.drawEntity();
		
		((TextView) findViewById(R.id.txt_header_title)).setText(getResources().getString(R.string.form_title_album));

		if (findViewById(R.id.chk_locked) != null) {
			((CheckBox) findViewById(R.id.chk_locked)).setVisibility(View.VISIBLE);
			((CheckBox) findViewById(R.id.chk_locked)).setChecked(((AlbumEntity) mEntity).locked);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected void doSave(boolean updateImages) {
		super.doSave(true);
	}
	
	@Override
	protected void gather() {
		/*
		 * Handle properties that are not part of the base entity
		 */
		final AlbumEntity entity = (AlbumEntity) mEntity;
		entity.locked = ((CheckBox) findViewById(R.id.chk_locked)).isChecked();
		super.gather();
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------
	
	@Override
	protected int getLayoutID() {
		return R.layout.album_form;
	}
}