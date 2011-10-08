package com.proxibase.aircandi.activities;

import com.proxibase.aircandi.activities.R;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Preferences extends PreferenceActivity {

	SharedPreferences			prefs;

	public static final String	PREF_AUTOSCAN			= "Pref_Autoscan";
	public static final String	PREF_AUTOSCAN_INTERVAL	= "Pref_Autoscan_Interval";
	public static final String	PREF_DEMO_MODE			= "Pref_Demo_Mode";
	public static final String	PREF_DISPLAY_EXTRAS		= "Pref_Display_Extras";
	public static final String	PREF_ENTITY_FENCING		= "Pref_Entity_Fencing";
	public static final String	PREF_SHOW_MEMORY		= "Pref_Show_Memory";
	public static final String	PREF_SOUND_EFFECTS		= "Pref_Sound_Effects";
	public static final String	PREF_THEME				= "Pref_Theme";
	public static final String	PREF_FULLSCREEN			= "Pref_Fullscreen";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}
}
