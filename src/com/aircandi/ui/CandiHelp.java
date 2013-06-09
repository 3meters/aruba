package com.aircandi.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.beta.R;
import com.aircandi.components.FontManager;
import com.aircandi.ui.base.FormActivity;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;

public class CandiHelp extends FormActivity {

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

	private void initialize() {
		FontManager.getInstance().setTypefaceLightItalic((TextView) findViewById(R.id.text_radar_add_places));
		FontManager.getInstance().setTypefaceLightItalic((TextView) findViewById(R.id.text_radar_browse_places));
		FontManager.getInstance().setTypefaceLightItalic((TextView) findViewById(R.id.text_candi_add_candigrams));
		FontManager.getInstance().setTypefaceLightItalic((TextView) findViewById(R.id.text_candi_tune_places));
	}

	private void bind() {}

	private void draw() {}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("ucd")
	public void onHelpClick(View view) {
		updateRunOnce();
		finish();
		AnimUtils.doOverridePendingTransition(this, TransitionType.HelpToPage);
	}

	@Override
	public void onBackPressed() {
		updateRunOnce();
		finish();
		AnimUtils.doOverridePendingTransition(this, TransitionType.HelpToPage);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.cancel) {
			updateRunOnce();
		}

		/* In case we add general menu items later */
		mCommon.doOptionsItemSelected(item);
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------
	
	private void updateRunOnce() {
		if (mHelpResId == R.layout.help_radar) {
			final Boolean runOnce = Aircandi.settings.getBoolean(Constants.SETTING_RUN_ONCE_HELP_RADAR, false);
			if (!runOnce) {
				Aircandi.settingsEditor.putBoolean(Constants.SETTING_RUN_ONCE_HELP_RADAR, true);
				Aircandi.settingsEditor.commit();					
			}
		}
		else if (mHelpResId == R.layout.help_candi_place) {
			final Boolean runOnce = Aircandi.settings.getBoolean(Constants.SETTING_RUN_ONCE_HELP_CANDI_PLACE, false);
			if (!runOnce) {
				Aircandi.settingsEditor.putBoolean(Constants.SETTING_RUN_ONCE_HELP_CANDI_PLACE, true);
				Aircandi.settingsEditor.commit();					
			}
		}
	}

	@Override
	protected Boolean isDialog() {
		return false;
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