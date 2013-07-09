package com.aircandi.ui;

import android.os.Bundle;
import android.view.View;

import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.beta.R;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.Animate.TransitionType;

public class HelpForm extends BaseActivity {

	private Integer	mHelpResId;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!isFinishing()) {
			initialize();
			bind();
			draw();
		}
	}

	private void initialize() {}

	private void bind() {}

	private void draw() {}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("ucd")
	public void onHelpClick(View view) {
		updateRunOnce();
		finish();
		Animate.doOverridePendingTransition(this, TransitionType.HelpToPage);
	}

	@Override
	public void onBackPressed() {
		updateRunOnce();
		finish();
		Animate.doOverridePendingTransition(this, TransitionType.HelpToPage);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.cancel) {
			updateRunOnce();
		}

		/* In case we add general menu items later */
		super.onOptionsItemSelected(item);
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------
	
	private void updateRunOnce() {
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

	@Override
	protected Boolean isTransparent() {
		return true;
	}

	@Override
	protected int getLayoutId() {
		if (mHelpResId == null) {
			final Bundle extras = this.getIntent().getExtras();
			if (extras != null) {
				mHelpResId = extras.getInt(Constants.EXTRA_HELP_ID);
			}
		}
		return mHelpResId;
	}
}