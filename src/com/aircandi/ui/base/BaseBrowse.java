package com.aircandi.ui.base;

import android.os.Bundle;
import android.view.WindowManager;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.components.EntityManager;
import com.aircandi.components.ProximityManager;
import com.aircandi.service.objects.User;

public abstract class BaseBrowse extends BaseActivity {

	protected Number	mEntityModelRefreshDate;
	protected Number	mEntityModelActivityDate;
	protected User		mEntityModelUser;

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
			onDatabind(mForceRefresh);
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
		mEntityModelUser = Aircandi.getInstance().getUser();
	}

	@Override
	protected void configureActionBar() {
		super.configureActionBar();
		if (mActionBar != null) {
			mActionBar.setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	public void onDatabind(final Boolean refreshProposed) {}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	public void onRefresh() {
		onDatabind(true); // Called from Routing
	}

	// --------------------------------------------------------------------------------------------
	// UI
	// --------------------------------------------------------------------------------------------

	protected void draw() {}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------
	
	protected void synchronize() {
		mEntityModelRefreshDate = ProximityManager.getInstance().getLastBeaconLoadDate();
		mEntityModelActivityDate = EntityManager.getEntityCache().getLastActivityDate();
		mEntityModelUser = Aircandi.getInstance().getUser();		
	}

	protected Boolean unsynchronized() {
		Number lastBeaconLoadDate = ProximityManager.getInstance().getLastBeaconLoadDate();
		Number lastActivityDate = EntityManager.getEntityCache().getLastActivityDate();

		if (Aircandi.getInstance().getUser() != null 
				&& mEntityModelUser != null
				&& !mEntityModelUser.id.equals(Aircandi.getInstance().getUser().id)) {
			return true;
		}

		if (lastBeaconLoadDate != null
				&& mEntityModelRefreshDate != null
				&& lastBeaconLoadDate.longValue() != mEntityModelRefreshDate.longValue()) {
			return true;
		}

		if (lastActivityDate != null
				&& mEntityModelActivityDate != null
				&& lastActivityDate.longValue() != mEntityModelActivityDate.longValue()) {
			return true;
		}

		return false;
	}

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