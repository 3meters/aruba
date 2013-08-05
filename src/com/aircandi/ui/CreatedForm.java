package com.aircandi.ui;

import android.os.Bundle;
import android.view.View;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.beta.R;
import com.aircandi.service.objects.LinkOptions.DefaultType;
import com.aircandi.ui.base.BaseShortcutForm;

@SuppressWarnings("ucd")
public class CreatedForm extends BaseShortcutForm {

	@Override
	protected void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		mLinkOptions = DefaultType.LinksForCreated;
		mShortcutType = Constants.TYPE_LINK_CREATE;
		Aircandi.getInstance().setNavigationDrawerCurrentView(CreatedForm.class);
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	@SuppressWarnings("ucd")
	public void onMenuItemClick(View view) {
		Integer id = view.getId();
		if (id != R.id.created) {
			super.onMenuItemClick(view);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.shortcut_form;
	}
}