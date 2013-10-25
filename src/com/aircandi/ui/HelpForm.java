package com.aircandi.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.ui.base.BaseBrowse;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.Animate.TransitionType;

public class HelpForm extends BaseBrowse {

	private Integer	mHelpResId;

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("ucd")
	public void onHelpClick(View view) {
		onCancel(false);
	}

	@Override
	public void onCancel(Boolean force) {
		updateRunOnce();
		setResultCode(Activity.RESULT_CANCELED);
		finish();
		Animate.doOverridePendingTransition(this, TransitionType.HELP_TO_PAGE);
	}

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	private void updateRunOnce() {
		if (mHelpResId != null) {
			if (mHelpResId == R.layout.radar_help) {
				final Boolean runOnce = Aircandi.settings.getBoolean(Constants.SETTING_RUN_ONCE_HELP_RADAR, false);
				if (!runOnce) {
					Aircandi.settingsEditor.putBoolean(Constants.SETTING_RUN_ONCE_HELP_RADAR, true);
					Aircandi.settingsEditor.commit();
				}
			}
			else if (mHelpResId == R.layout.place_help) {
				final Boolean runOnce = Aircandi.settings.getBoolean(Constants.SETTING_RUN_ONCE_HELP_CANDI_PLACE, false);
				if (!runOnce) {
					Aircandi.settingsEditor.putBoolean(Constants.SETTING_RUN_ONCE_HELP_CANDI_PLACE, true);
					Aircandi.settingsEditor.commit();
				}
			}
		}
	}

	@Override
	protected Boolean isTransparent() {
		return true;
	}

	@Override
	protected int getLayoutId() {
		Integer mHelpResId = 0;
		final Bundle extras = this.getIntent().getExtras();
		if (extras != null) {
			mHelpResId = extras.getInt(Constants.EXTRA_HELP_ID);
		}
		return mHelpResId;
	}
}