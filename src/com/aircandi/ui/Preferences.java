package com.aircandi.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.beta.R;
import com.aircandi.components.AircandiCommon;
import com.aircandi.components.Tracker;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;

public class Preferences extends SherlockPreferenceActivity {

	private AircandiCommon	mCommon;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		/*
		 * We need to set the theme so ActionBarSherlock behaves correctly on API < V14
		 */
		mCommon = new AircandiCommon(this, savedInstanceState);
		/*
		 * Set theme.
		 * TODO: Switch over to using the preferenceStyle attribute for the current theme.
		 */
		final String prefTheme = Aircandi.settings.getString(Constants.PREF_THEME, Constants.PREF_THEME_DEFAULT);
		if (prefTheme.equals("aircandi_theme_snow")) {
			setTheme(R.style.aircandi_theme_light);
		}
		else {
			setTheme(R.style.aircandi_theme_dark);
		}

		super.onCreate(savedInstanceState);

		/* Load preferences layout */
		if (Aircandi.getInstance().getUser() != null
				&& Aircandi.getInstance().getUser().developer != null
				&& Aircandi.getInstance().getUser().developer) {
			addPreferencesFromResource(R.xml.preferences_dev);
		}
		else {
			addPreferencesFromResource(R.xml.preferences);
		}

		initialize();
	}

	@SuppressWarnings("deprecation")
	private void initialize() {
		/* Listen for theme change */
		Preference pref = findPreference("Pref_Theme");
		if (pref != null) {
			pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					Aircandi.settingsEditor.putString(Constants.PREF_THEME, (String) newValue);
					Aircandi.settingsEditor.commit();

					final Intent intent = getIntent();
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
					finish();
					overridePendingTransition(0, 0);
					startActivity(intent);
					return false;
				}
			});
		}

		/* Listen for privacy change */
		pref = findPreference("Pref_Browse_In_Private");
		if (pref != null) {
			pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					/*
					 * Update user property and push update to service
					 */
					return true; // we handled it
				}
			});
		}

		/* Listen for clear history click */
		pref = findPreference("Pref_Button_Clear_History");
		if (pref != null) {
			pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {
					/*
					 * Alert and then clear browse history
					 */
					return true; // we handled it
				}
			});
		}

		/* Listen for about click */
		pref = findPreference("Pref_About");
		if (pref != null) {
			pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {
					doInfoClick();
					return true;
				}
			});
		}
	}

	private void doInfoClick() {
		final String title = getString(R.string.alert_about_title);
		final String message = getString(R.string.alert_about_label_version) + " "
				+ Aircandi.getVersionName(this, CandiRadar.class) + System.getProperty("line.separator")
				+ getString(R.string.alert_about_label_code) + " "
				+ String.valueOf(Aircandi.getVersionCode(this, CandiRadar.class)) + System.getProperty("line.separator")
				+ getString(R.string.dialog_about_copyright);
		AircandiCommon.showAlertDialog(R.drawable.ic_launcher
				, title
				, message
				, null
				, this, android.R.string.ok, null, null, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {}
				}, null);
		Tracker.sendEvent("ui_action", "open_dialog", "about", 0, Aircandi.getInstance().getUser());

	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		AnimUtils.doOverridePendingTransition(this, TransitionType.FormToPage);
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
}
