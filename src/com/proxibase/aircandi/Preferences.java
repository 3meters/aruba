package com.proxibase.aircandi;

import android.content.SharedPreferences;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.proxibase.aircandi.components.AircandiCommon;
import com.proxibase.aircandi.components.AnimUtils;
import com.proxibase.aircandi.components.AnimUtils.TransitionType;

public class Preferences extends SherlockPreferenceActivity {

	SharedPreferences			prefs;
	protected AircandiCommon	mCommon;

	public static final String	PREF_AUTOSCAN			= "Pref_Autoscan";
	public static final String	PREF_AUTOSCAN_INTERVAL	= "Pref_Autoscan_Interval";
	public static final String	PREF_DEMO_MODE			= "Pref_Demo_Mode";
	public static final String	PREF_GLOBAL_BEACONS		= "Pref_Global_Beacons";
	public static final String	PREF_DISPLAY_EXTRAS		= "Pref_Display_Extras";
	public static final String	PREF_ENTITY_FENCING		= "Pref_Entity_Fencing";
	public static final String	PREF_SHOW_DEBUG			= "Pref_Show_Debug";
	public static final String	PREF_SOUND_EFFECTS		= "Pref_Sound_Effects";
	public static final String	PREF_THEME				= "Pref_Theme";
	public static final String	PREF_USER				= "Pref_User";
	public static final String	PREF_USER_SESSION		= "Pref_User_Session";
	public static final String	SETTING_VERSION_NAME	= "Setting_Version_Name";
	public static final String	SETTING_PICTURE_SEARCH	= "Setting_Picture_Search";

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
		String prefTheme = Aircandi.settings.getString(Preferences.PREF_THEME, "aircandi_theme_midnight");
		if (prefTheme.equals("aircandi_theme_snow") 
				|| prefTheme.equals("aircandi_theme_serene")
				|| prefTheme.equals("aircandi_theme_lagoon")
				|| prefTheme.equals("aircandi_theme_blueray")) {
			setTheme(R.style.aircandi_theme_form_light);
		}
		else {
			setTheme(R.style.aircandi_theme_form_dark);
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
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		AnimUtils.doOverridePendingTransition(this, TransitionType.FormToCandiPage);
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

	public static enum PrefResponse {
		None, Change, Refresh, Restart
	}
}
