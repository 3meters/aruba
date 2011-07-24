package com.proxibase.aircandi.activities;

import com.proxibase.aircandi.controllers.R;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Preferences extends PreferenceActivity {

	SharedPreferences			prefs;

	public static final String	PREF_BEACON_WITH_ENTITIES_ONLY			= "Pref_Beacon_With_Entities_Only";
	public static final String	PREF_BEACON_LEVEL_CUTOFF_UNREGISTERED	= "Pref_Beacon_Level_Cutoff_Unregistered";
	public static final String	PREF_BEACON_SHOW_HIDDEN					= "Pref_Beacon_Show_Hidden";

	public static final String	PREF_DISPLAY_EXTRAS						= "Pref_Display_Extras";
	public static final String	PREF_ENTITY_FENCING						= "Pref_Entity_Fencing";
	public static final String	PREF_AUTOSCAN							= "Pref_Autoscan";
	public static final String	PREF_AUTOSCAN_INTERVAL					= "Pref_Autoscan_Interval";
	public static final String	PREF_SHOW_CONFIG_MENU					= "Pref_Show_Config_Menu";
	public static final String	PREF_TILE_SCALE							= "Pref_Tile_Scale";
	public static final String	PREF_TILE_ROTATE						= "Pref_Tile_Rotate";
	public static final String	PREF_SOUND_EFFECTS						= "Pref_Sound_Effects";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}
}
