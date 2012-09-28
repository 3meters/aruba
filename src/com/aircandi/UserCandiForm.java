package com.aircandi;

import com.aircandi.components.ProxiExplorer.EntityTree;

import android.os.Bundle;
import android.view.View;

public class UserCandiForm extends CandiFormBase {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!isFinishing()) {
			bind(false);
		}
	}

	public void bind(Boolean refresh) {
		doBind(refresh, true, EntityTree.User);
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void onChildrenButtonClick(View v) {
		showChildrenForEntity(UserCandiList.class);
	}
}