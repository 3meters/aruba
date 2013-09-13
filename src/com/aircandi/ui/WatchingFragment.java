package com.aircandi.ui;

import android.os.Bundle;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.service.objects.LinkOptions.LinkProfile;
import com.aircandi.ui.base.BaseShortcutFragment;

@SuppressWarnings("ucd")
public class WatchingFragment extends BaseShortcutFragment {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mLinkProfiles = LinkProfile.LINKS_FOR_USER;
		mShortcutType = Constants.TYPE_LINK_WATCH;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mEntityId = Aircandi.getInstance().getUser().id;
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.shortcut_fragment;
	}

}