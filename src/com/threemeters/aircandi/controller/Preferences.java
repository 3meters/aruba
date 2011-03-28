package com.threemeters.aircandi.controller;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Preferences extends PreferenceActivity {

	SharedPreferences			prefs;

	public static final String	PREF_TAGS_WITH_ENTITIES_ONLY	= "Pref_Tags_With_Entities_Only";
	public static final String	PREF_DISPLAY_EXTRAS				= "Pref_Display_Extras";
	public static final String	PREF_TAG_LEVEL_CUTOFF			= "Pref_Tag_Level_Cutoff";
	public static final String	PREF_AUTOSCAN					= "Pref_Autoscan";
	public static final String	PREF_AUTOSCAN_INTERVAL			= "Pref_Autoscan_Interval";
	public static final String	PREF_SHOW_CONFIG_MENU			= "Pref_Show_Config_Menu";
	public static final String	PREF_TILE_SCALE					= "Pref_Tile_Scale";
	public static final	String	PREF_TILE_ROTATE				= "Pref_Tile_Rotate";
	public static final	String	PREF_SOUND_EFFECTS				= "Pref_Sound_Effects";


	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);
	}
}
