package com.aircandi.ui.edit;

import android.os.Bundle;
import android.view.WindowManager;

import com.aircandi.beta.R;
import com.aircandi.ui.base.BaseEntityEdit;

public class PostEdit extends BaseEntityEdit {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		super.onCreate(savedInstanceState);
	}

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.post_edit;
	}
}