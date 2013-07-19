package com.aircandi.ui.base;

import android.os.Bundle;
import android.view.WindowManager;

import com.aircandi.Constants;
import com.aircandi.components.BusyManager;

public abstract class BaseBrowse extends BaseActivity {

	/* Inputs */
	protected Boolean	mForceRefresh	= false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		if (!isFinishing()) {
			unpackIntent();
			initialize(savedInstanceState);
			configureActionBar();
			databind(mForceRefresh);
		}
	}

	@Override
	protected void unpackIntent() {
		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mForceRefresh = extras.getBoolean(Constants.EXTRA_REFRESH_FORCE);
		}
	}

	protected void initialize(Bundle savedInstanceState) {
		if (mActionBar != null) {
			mActionBar.setDisplayHomeAsUpEnabled(true);
		}
		mBusyManager = new BusyManager(this);
	}

	protected void databind(final Boolean refreshProposed) {}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	public void onRefresh() {
		databind(true); // Called from Routing
	}

	// --------------------------------------------------------------------------------------------
	// UI
	// --------------------------------------------------------------------------------------------

	protected void draw() {}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

}