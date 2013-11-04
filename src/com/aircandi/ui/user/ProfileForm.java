package com.aircandi.ui.user;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.aircandi.R;

public class ProfileForm extends UserForm {

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuItem profile = menu.findItem(R.id.profile);
		if (profile != null) {
			profile.setVisible(false);
		}
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		
		MenuItem profile = menu.findItem(R.id.profile);
		if (profile != null) {
			profile.setVisible(false);
		}
		return true;
	}	
}