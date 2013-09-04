package com.aircandi.ui.base;

import android.os.Bundle;
import android.view.WindowManager;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.components.EntityManager;
import com.aircandi.components.Logger;
import com.aircandi.components.ProximityManager;
import com.aircandi.service.objects.User;
import com.aircandi.utilities.DateTime;

public abstract class BaseBrowse extends BaseActivity {

	protected Number	mEntityModelRefreshDate;
	protected Number	mEntityModelActivityDate;
	protected User		mEntityModelUser;

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
	protected void unpackIntent() {
		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mRefreshFromService = extras.getBoolean(Constants.EXTRA_REFRESH_FROM_SERVICE);
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

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	public void onRefresh() {
		mRefreshFromService	= true;
		databind(); // Called from Routing
	}

	// --------------------------------------------------------------------------------------------
	// UI
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	protected void synchronize() {
		mEntityModelRefreshDate = ProximityManager.getInstance().getLastBeaconLoadDate();
		mEntityModelActivityDate = EntityManager.getEntityCache().getLastActivityDate();
		mEntityModelUser = Aircandi.getInstance().getUser();
	}

	protected Boolean unsynchronized() {
		
		if (mEntityModelActivityDate == null) {
			Logger.v(this, "Unsynchronized: no local activity date");
			return true;
		}

		if (mEntityModelRefreshDate == null) {
			Logger.v(this, "Unsynchronized: no local beacon date");
			return true;
		}
		
		Number lastBeaconLoadDate = ProximityManager.getInstance().getLastBeaconLoadDate();
		Number lastActivityDate = EntityManager.getEntityCache().getLastActivityDate();

		if (Aircandi.getInstance().getUser() != null
				&& mEntityModelUser != null
				&& !mEntityModelUser.id.equals(Aircandi.getInstance().getUser().id)) {
			Logger.v(this, "Unsynchronized: user change");
			return true;
		}

		if (lastBeaconLoadDate != null
				&& mEntityModelRefreshDate != null
				&& lastBeaconLoadDate.longValue() != mEntityModelRefreshDate.longValue()) {
			Logger.v(this, "Unsynchronized: beacon load date has changed");
			Logger.v(this, "Unsynchronized: beacon date old: " + DateTime.dateString(mEntityModelRefreshDate.longValue(), DateTime.DATE_FORMAT_DETAILED));
			Logger.v(this, "Unsynchronized: beacon date new: " + DateTime.dateString(lastBeaconLoadDate.longValue(), DateTime.DATE_FORMAT_DETAILED));
			return true;
		}

		if (lastActivityDate != null
				&& mEntityModelActivityDate != null
				&& lastActivityDate.longValue() != mEntityModelActivityDate.longValue()) {
			Logger.v(this, "Unsynchronized: model activity date has changed");
			Logger.v(this, "Unsynchronized: activity date old: " + DateTime.dateString(mEntityModelActivityDate.longValue(), DateTime.DATE_FORMAT_DETAILED));
			Logger.v(this, "Unsynchronized: activity date new: " + DateTime.dateString(lastActivityDate.longValue(), DateTime.DATE_FORMAT_DETAILED));
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