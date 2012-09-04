package com.aircandi;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.aircandi.components.AircandiCommon;
import com.aircandi.components.AnimUtils;
import com.aircandi.components.Logger;
import com.aircandi.components.AnimUtils.TransitionType;
import com.aircandi.core.CandiConstants;

public abstract class CandiActivity extends SherlockActivity {

	protected int				mLastResultCode	= Activity.RESULT_OK;
	protected AircandiCommon	mCommon;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (!Aircandi.getInstance().getLaunchedFromRadar()) {
			/* Try to detect case where this is being created after a crash and bail out. */
			super.onCreate(savedInstanceState);
			finish();
		}
		else {
			mCommon = new AircandiCommon(this, savedInstanceState);
			mCommon.setTheme(false, false);
			mCommon.unpackIntent();
			setContentView(getLayoutId());
			super.onCreate(savedInstanceState);
			mCommon.initialize();

			Logger.d(this, "Started from radar flag: " + String.valueOf(Aircandi.getInstance().getLaunchedFromRadar()));
		}
	}

	// --------------------------------------------------------------------------------------------
	// Events routines
	// --------------------------------------------------------------------------------------------

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		mCommon.doAttachedToWindow();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		AnimUtils.doOverridePendingTransition(this, TransitionType.CandiPageBack);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Logger.d(this, "Configuration changed");
		mCommon.mConfigChange = true;
		super.onConfigurationChanged(newConfig);
	}

	// --------------------------------------------------------------------------------------------
	// Application menu routines (settings)
	// --------------------------------------------------------------------------------------------

	public boolean onCreateOptionsMenu(Menu menu) {
		mCommon.doCreateOptionsMenu(menu);
		return true;
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		mCommon.doPrepareOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		mCommon.doOptionsItemSelected(item);
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onRestart() {
		Logger.d(this, "CandiActivity restarting");
		super.onRestart();

		if (!mCommon.mPrefTheme.equals(Aircandi.settings.getString(Preferences.PREF_THEME, CandiConstants.THEME_DEFAULT))) {
			Logger.d(this, "Restarting because of theme change");
			mCommon.mPrefTheme = Aircandi.settings.getString(Preferences.PREF_THEME, CandiConstants.THEME_DEFAULT);
			mCommon.reload();
		}
	}

	@Override
	protected void onResume() {
		mCommon.doResume();
		super.onResume();
	}

	@Override
	protected void onStop() {
		super.onStop();
		mCommon.doStop();
	}

	@Override
	protected void onStart() {
		super.onStart();
		mCommon.doStart();
	}

	@Override
	protected void onPause() {
		mCommon.doPause();
		super.onPause();
	}

	protected int getLayoutId() {
		return 0;
	}

	protected void onDestroy() {
		/* This activity gets destroyed everytime we leave using back or finish(). */
		super.onDestroy();
		if (mCommon != null) {
			mCommon.doDestroy();
		}
		Logger.d(this, "CandiActivity destroyed");
	}
}