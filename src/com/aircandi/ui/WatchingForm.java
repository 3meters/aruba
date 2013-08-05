package com.aircandi.ui;

import android.os.Bundle;
import android.view.View;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.beta.R;
import com.aircandi.service.objects.LinkOptions.DefaultType;
import com.aircandi.ui.base.BaseShortcutForm;

@SuppressWarnings("ucd")
public class WatchingForm extends BaseShortcutForm {

	@Override
	protected void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		mLinkOptions = DefaultType.LinksForWatching;
		mShortcutType = Constants.TYPE_LINK_WATCH;
		Aircandi.getInstance().setNavigationDrawerCurrentView(WatchingForm.class);
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	@SuppressWarnings("ucd")
	public void onMenuItemClick(View view) {
		Integer id = view.getId();
		if (id != R.id.watching) {
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