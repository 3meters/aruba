package com.aircandi.ui.base;

import android.os.Bundle;
import android.view.WindowManager;

import com.actionbarsherlock.view.Menu;
import com.aircandi.Aircandi;
import com.aircandi.R;
import com.aircandi.service.objects.CacheStamp;

public abstract class BaseBrowse extends BaseActivity {

	protected CacheStamp	mCacheStamp;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		if (!isFinishing()) {
			unpackIntent();
			initialize(savedInstanceState);
			configureActionBar();
		}
	}

	@Override
	public void initialize(Bundle savedInstanceState) {}

	@Override
	protected void configureActionBar() {
		super.configureActionBar();
		if (mActionBar != null) {
			mActionBar.setDisplayHomeAsUpEnabled(true);
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
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		/*
		 * Setup menu items that are common for entity forms.
		 */
		mMenuItemSignin = menu.findItem(R.id.signin);
		mMenuItemProfile = menu.findItem(R.id.profile);
		if (mMenuItemProfile != null) {
			mMenuItemProfile.setVisible(!(Aircandi.getInstance().getCurrentUser().isAnonymous()));
		}
		if (mMenuItemSignin != null) {
			mMenuItemSignin.setVisible(Aircandi.getInstance().getCurrentUser().isAnonymous());
		}
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		if (mMenuItemProfile != null) {
			mMenuItemProfile.setVisible(!Aircandi.getInstance().getCurrentUser().isAnonymous());
		}
		if (mMenuItemSignin != null) {
			mMenuItemSignin.setVisible(Aircandi.getInstance().getCurrentUser().isAnonymous());
		}
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

}