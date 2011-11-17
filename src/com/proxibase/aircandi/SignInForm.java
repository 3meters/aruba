package com.proxibase.aircandi;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class SignInForm extends EntityBaseForm {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		bindEntity();
		drawEntity();
	}
	
	public void onSignUpButtonClick(View view) {
		Toast.makeText(this, "Not implemented yet.", Toast.LENGTH_SHORT).show();
	}
	
	public void onSignInButtonClick(View view) {
		Toast.makeText(this, "Not implemented yet.", Toast.LENGTH_SHORT).show();
	}
	
	@Override
	public void onCancelButtonClick(View view) {
		setResult(Activity.RESULT_CANCELED);
		finish();
	}
	
	
	// --------------------------------------------------------------------------------------------
	// Service routines
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Misc routines
	// --------------------------------------------------------------------------------------------
	
	@Override
	protected int getLayoutID() {
		return R.layout.signin_form;
	}
}