package com.aircandi.ui;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.aircandi.Aircandi;
import com.aircandi.CandiConstants;
import com.aircandi.R;
import com.aircandi.components.AircandiCommon;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;

public class Preferences extends SherlockPreferenceActivity {

	private AircandiCommon		mCommon;

	/* Prefs - users */
	public static final String	PREF_SEARCH_RADIUS			= "Pref_Search_Radius";
	public static final String	PREF_SOUND_EFFECTS			= "Pref_Sound_Effects";
	public static final String	PREF_THEME					= "Pref_Theme";

	/* Prefs - dev only */
	public static final String	PREF_ENABLE_DEV				= "Pref_Enable_Dev";
	public static final String	PREF_ENTITY_FENCING			= "Pref_Entity_Fencing";
	public static final String	PREF_SHOW_PLACE_RANK_SCORE	= "Pref_Show_Place_Rank_Score";
	public static final String	PREF_TESTING_BEACONS		= "Pref_Testing_Beacons";
	public static final String	PREF_TESTING_LOCATION		= "Pref_Testing_Location";
	public static final String	PREF_TESTING_PLACE_PROVIDER	= "Pref_Testing_Place_Provider";

	/* Settings */
	public static final String	SETTING_USER				= "Setting_User";
	public static final String	SETTING_USER_SESSION		= "Setting_User_Session";
	public static final String	SETTING_PICTURE_SEARCH		= "Setting_Picture_Search";
	public static final String	SETTING_LAST_EMAIL			= "Setting_Last_Email";

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		/*
		 * We need to set the theme so ActionBarSherlock behaves correctly on API < V14
		 */
		mCommon = new AircandiCommon(this, savedInstanceState);

		/*
		 * TODO: Switch over to using the preferenceStyle attribute for the current theme.
		 */
		final String prefTheme = Aircandi.settings.getString(Preferences.PREF_THEME, CandiConstants.THEME_DEFAULT);
		if (prefTheme.equals("aircandi_theme_snow")) {
			setTheme(R.style.aircandi_theme_light);
		}
		else {
			setTheme(R.style.aircandi_theme_dark);
		}

		super.onCreate(savedInstanceState);
		if (Aircandi.getInstance().getUser() != null
				&& Aircandi.getInstance().getUser().isDeveloper != null
				&& Aircandi.getInstance().getUser().isDeveloper) {
			addPreferencesFromResource(R.xml.preferences_dev);
		}
		else {
			addPreferencesFromResource(R.xml.preferences);
		}

		final Preference myPref = findPreference("Pref_Theme");

		myPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Aircandi.settingsEditor.putString(Preferences.PREF_THEME, (String) newValue);
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
