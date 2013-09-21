package com.aircandi.ui.base;

import android.os.Bundle;

import com.aircandi.Constants;
import com.aircandi.service.objects.Entity;
import com.aircandi.utilities.Json;

public abstract class BasePicker extends BaseBrowse implements IList {

	protected Entity	mEntity;

	@Override
	public void unpackIntent() {
		super.unpackIntent();

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			final String jsonEntity = extras.getString(Constants.EXTRA_ENTITY);
			if (jsonEntity != null) {
				mEntity = (Entity) Json.jsonToObject(jsonEntity, Json.ObjectType.ENTITY);
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// UI
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onResume() {
		super.onResume();
		if (!isFinishing()) {
			bind(BindingMode.AUTO);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

}