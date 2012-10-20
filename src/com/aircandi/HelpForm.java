package com.aircandi;

import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import com.aircandi.components.AnimUtils;
import com.aircandi.components.AnimUtils.TransitionType;

public class HelpForm extends FormActivity {

	private Integer	mStringId;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		android.view.WindowManager.LayoutParams params = getWindow().getAttributes();
		params.width = Aircandi.displayMetrics.widthPixels - 50;
		params.height = Aircandi.displayMetrics.heightPixels - 140;
		getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mStringId = extras.getInt(getResources().getString(R.string.EXTRA_STRING_ID));
		}

		initialize();
	}

	private void initialize() {
		String helpHtml = getString(mStringId);
		((TextView) findViewById(R.id.text_message)).setText(Html.fromHtml(helpHtml));
	}

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
		return R.layout.help_form;
	}
}