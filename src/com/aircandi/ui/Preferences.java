package com.aircandi.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.text.InputType;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.Animate.TransitionType;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.Routing.Route;

@SuppressWarnings("deprecation")
public class Preferences extends SherlockPreferenceActivity implements OnSharedPreferenceChangeListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		/*
		 * We need to set the theme so ActionBarSherlock behaves correctly on API < V14
		 * TODO: Switch over to using the preferenceStyle attribute for the current theme.
		 */
		setTheme();
		super.onCreate(savedInstanceState);

		/* Load preferences layout */
		addPreferencesFromResource(R.xml.preferences);
		if (Aircandi.getInstance().getCurrentUser() != null
				&& Aircandi.getInstance().getCurrentUser().developer != null
				&& Aircandi.getInstance().getCurrentUser().developer) {
			addPreferencesFromResource(R.xml.preferences_dev);
		}

		initialize();
	}

	private void initialize() {

		/* Configure action bar */
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowHomeEnabled(true);

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
			pref.setTitle("Version: " + Aircandi.getVersionName(this, Preferences.class));
			pref.setSummary("Terms of Service, Privacy Policy, Licenses");

			pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {
					Routing.route(Preferences.this, Route.ABOUT);
					return true;
				}
			});
		}

		/* Listen for feedback click */
		pref = findPreference("Pref_Feedback");
		if (pref != null) {
			pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {
					Routing.route(Preferences.this, Route.FEEDBACK);
					return true;
				}
			});
		}

		/* Listen for dev toggle */
		pref = findPreference("Pref_Enable_Dev");
		if (pref != null) {
			Boolean enabled = Aircandi.settings.getBoolean(Constants.PREF_ENABLE_DEV, Constants.PREF_ENABLE_DEV_DEFAULT);
			enableDeveloper(enabled);

			pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					Boolean enabled = (Boolean) newValue;
					enableDeveloper(enabled);
					return true;
				}
			});
		}

	}

	private void enableDeveloper(Boolean enable) {
		findPreference("Pref_Testing_Screen").setEnabled(enable);
		findPreference("Pref_Entity_Fencing").setEnabled(enable);
		findPreference("Pref_Testing_Self_Notify").setEnabled(enable);
		Aircandi.tracker.enableDeveloper(enable);
	}

	private void handleAnonymous() {
		/* Hide notification item if anonymous */
		Preference pref = findPreference("Pref_Notifications_Screen");
		if (pref != null) {
			pref.setShouldDisableView(true);
			pref.setEnabled(!(Aircandi.getInstance().getCurrentUser().isAnonymous()));
		}
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Preference pref = findPreference(key);

		if (pref instanceof ListPreference) {
			ListPreference listPref = (ListPreference) pref;
			pref.setSummary(listPref.getEntry());
		}
		else if (pref instanceof EditTextPreference) {
			if (((EditTextPreference) pref).getEditText().getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
				EditTextPreference etPref = (EditTextPreference) pref;
				String maskedPw = "";
				if (etPref.getText() != null) {
					for (int j = 0; j < etPref.getText().length(); j++) {
						maskedPw = maskedPw + "*";
					}
					pref.setSummary(maskedPw);
				}
			}
			else {
				EditTextPreference etPref = (EditTextPreference) pref;
				pref.setSummary(etPref.getText());
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	private void setSummaries(PreferenceGroup prefGroup) {
		/*
		 * Walk and set the current pref values in the UI
		 */
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		for (int i = 0; i < prefGroup.getPreferenceCount(); i++) {
			Preference pref = prefGroup.getPreference(i);
			if (pref instanceof ListPreference) {
				ListPreference listPref = (ListPreference) pref;
				pref.setSummary(listPref.getEntry());
			}
			else if (pref instanceof EditTextPreference) {
				if (((EditTextPreference) pref).getEditText().getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
					EditTextPreference etPref = (EditTextPreference) pref;
					String maskedPw = "";
					if (etPref.getText() != null) {
						for (int j = 0; j < etPref.getText().length(); j++) {
							maskedPw = maskedPw + "*";
						}
						pref.setSummary(maskedPw);
					}
				}
				else {
					EditTextPreference etPref = (EditTextPreference) pref;
					pref.setSummary(etPref.getText());
				}
			}
			else if (pref instanceof PreferenceGroup) {
				setSummaries((PreferenceGroup) pref);
			}
		}
	}

	private void setTheme() {
		final String prefTheme = Aircandi.settings.getString(Constants.PREF_THEME, Constants.PREF_THEME_DEFAULT);
		if (prefTheme.equals("aircandi_theme_snow")) {
			setTheme(R.style.aircandi_theme_light);
		}
		else {
			setTheme(R.style.aircandi_theme_dark);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			/*
			 * We aren't using Routing because pref activity doesn't derive
			 * from BaseActivity.
			 */
			setResult(Activity.RESULT_CANCELED);
			finish();
			Animate.doOverridePendingTransition(this, TransitionType.PAGE_TO_PAGE);
		}
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onResume() {
		super.onResume();
		setSummaries((PreferenceGroup) getPreferenceScreen());
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onStop() {
		super.onStop();
		Aircandi.tracker.activityStop(this);
	}

	@Override
	protected void onStart() {
		super.onStart();
		Aircandi.tracker.activityStart(this);
		handleAnonymous();
	}
}
