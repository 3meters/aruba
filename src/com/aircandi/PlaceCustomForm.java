package com.aircandi;

import android.os.Bundle;
import android.view.View;

import com.aircandi.components.AnimUtils;
import com.aircandi.components.AnimUtils.TransitionType;

public class PlaceCustomForm extends FormActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		android.view.WindowManager.LayoutParams params = getWindow().getAttributes();
		params.width = Aircandi.displayMetrics.widthPixels - 50;
		params.height = Aircandi.displayMetrics.heightPixels - 140;
		getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);

		initialize();
	}

	private void initialize() {}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		AnimUtils.doOverridePendingTransition(this, TransitionType.HelpHide);
	}

	public void onCancelButtonClick(View view) {
		/*
		 * Activity is already gone using default animation by the time we get something going.
		 */
		finish();
		AnimUtils.doOverridePendingTransition(this, TransitionType.HelpHide);
	}

	@Override
	protected Boolean isDialog() {
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected int getLayoutID() {
		return R.layout.place_custom_form;
	}
}