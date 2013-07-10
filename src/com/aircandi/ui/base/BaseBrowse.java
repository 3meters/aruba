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
			bind(mForceRefresh);
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
		mActionBar.setDisplayHomeAsUpEnabled(true);
		mBusyManager = new BusyManager(this);
	}

	protected abstract void configureActionBar();

	protected abstract void bind(final Boolean refreshProposed);

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	public void onRefresh() {
		bind(true); // Called from AircandiCommon
	}

	public abstract void onAdd();

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	protected abstract void draw();

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
	// Misc routines
	// --------------------------------------------------------------------------------------------

}