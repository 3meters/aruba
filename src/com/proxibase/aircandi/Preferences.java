package com.proxibase.aircandi;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.proxibase.aircandi.components.Tracker;

public class Preferences extends PreferenceActivity {

	SharedPreferences			prefs;

	public static final String	PREF_AUTOSCAN			= "Pref_Autoscan";
	public static final String	PREF_AUTOSCAN_INTERVAL	= "Pref_Autoscan_Interval";
	public static final String	PREF_DEMO_MODE			= "Pref_Demo_Mode";
	public static final String	PREF_GLOBAL_BEACONS		= "Pref_Global_Beacons";
	public static final String	PREF_DISPLAY_EXTRAS		= "Pref_Display_Extras";
	public static final String	PREF_ENTITY_FENCING		= "Pref_Entity_Fencing";
	public static final String	PREF_SHOW_DEBUG			= "Pref_Show_Debug";
	public static final String	PREF_SOUND_EFFECTS		= "Pref_Sound_Effects";
	public static final String	PREF_THEME				= "Pref_Theme";
	public static final String	PREF_USERNAME			= "Pref_Username";
	public static final String	PREF_PASSWORD			= "Pref_Password";
	public static final String	SETTING_VERSION_NAME	= "Setting_Version_Name";
	public static final String	SETTING_PICTURE_SEARCH	= "Setting_Picture_Search";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (Aircandi.getInstance().getUser().isDeveloper) {
			addPreferencesFromResource(R.xml.preferences_dev);
			Tracker.trackPageView("/Preferences");
		}
		else {
			addPreferencesFromResource(R.xml.preferences_dev);
			Tracker.trackPageView("/PreferencesDev");
		}
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		overridePendingTransition(R.anim.browse_in, R.anim.form_out);
	}

	public static enum PrefResponse {
		None, Change, Refresh, Restart
	}
}
