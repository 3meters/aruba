package com.georain.ripple.controller;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;

@SuppressWarnings("unused")
public class Preferences extends PreferenceActivity
{

	SharedPreferences			prefs;

	public static final String	PREF_RIPPLE_SPOTS_ONLY		= "Pref_Ripple_Spots_Only";
	public static final String	PREF_DISPLAY_EXTRAS			= "Pref_Display_Extras";
	public static final String	PREF_LEVEL_CUTOFF			= "Pref_Level_Cutoff";
	public static final String	PREF_AUTOSCAN				= "Pref_Autoscan";
	public static final String	PREF_AUTOSCAN_INTERVAL		= "Pref_Autoscan_Interval";
	public static final String	PREF_SHOW_CONFIG_MENU		= "Pref_Show_Config_Menu";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);
	}
}
