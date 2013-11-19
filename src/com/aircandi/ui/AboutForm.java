package com.aircandi.ui;

import java.util.Calendar;
import java.util.Locale;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.TextView;

import com.aircandi.Aircandi;
import com.aircandi.R;
import com.aircandi.ui.base.BaseBrowse;
import com.aircandi.utilities.Animate;
import com.aircandi.utilities.Animate.TransitionType;
import com.aircandi.utilities.DateTime;
import com.aircandi.utilities.Routing;
import com.aircandi.utilities.Routing.Route;
import com.aircandi.utilities.Utilities;
import com.aircandi.utilities.Utilities.ScreenSize;

public class AboutForm extends BaseBrowse {

	private TextView	mVersion;
	private TextView	mCopyright;
	private String		mVersionName;

	@Override
	public void initialize(Bundle savedInstanceState) {
		super.initialize(savedInstanceState);
		mVersion = (TextView) findViewById(R.id.version);
		mCopyright = (TextView) findViewById(R.id.copyright);

		final String year = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
		final String company = getString(R.string.name_company);
		final String copyrightSymbol = getString(R.string.symbol_copyright);

		mVersionName = Aircandi.getVersionName(this, AircandiForm.class);
		final String version = getString(R.string.about_label_version) + ": " + mVersionName
				+ " (" + String.valueOf(Aircandi.getVersionCode(this, AircandiForm.class)) + ")";

		final String copyright = copyrightSymbol + year + " " + company;

		mVersion.setText(version);
		mCopyright.setText(copyright);

		DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
		Integer screenWidthDp = Math.round(displayMetrics.widthPixels / displayMetrics.density);
		Integer screenHeightDp = Math.round(displayMetrics.heightPixels / displayMetrics.density);

		ScreenSize screenSize = Utilities.getScreenSize();

		((TextView) findViewById(R.id.install_id)).setText(Aircandi.getInstallationId());
		((TextView) findViewById(R.id.install_type)).setText(Aircandi.getInstallType());
		((TextView) findViewById(R.id.install_date)).setText(DateTime.dateString(Aircandi.getInstallDate(), DateTime.DATE_FORMAT_DEFAULT));
		((TextView) findViewById(R.id.screen_info)).setText("Screen size: " + screenSize.name().toLowerCase(Locale.US)
				+ ", height: " + String.valueOf(screenHeightDp)
				+ "dp, width: " + String.valueOf(screenWidthDp) + "dp");
	}

	@Override
	protected void configureActionBar() {
		super.configureActionBar();

		setActivityTitle(getString(R.string.about_title));
	}

	// --------------------------------------------------------------------------------------------
	// Events
	// --------------------------------------------------------------------------------------------

	@Override
	public void onCancel(Boolean force) {
		setResultCode(Activity.RESULT_CANCELED);
		finish();
		Animate.doOverridePendingTransition(this, TransitionType.PAGE_TO_PAGE);
	}

	@SuppressWarnings("ucd")
	public void onTermsButtonClick(View view) {
		Routing.route(this, Route.TERMS);
	}

	@SuppressWarnings("ucd")
	public void onPrivacyButtonClick(View view) {
		Routing.route(this, Route.PRIVACY);
	}

	@SuppressWarnings("ucd")
	public void onLegalButtonClick(View view) {
		Routing.route(this, Route.LEGAL);
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onStart() {
		super.onStart();
		if (!isFinishing()) {
			if (mActionBar != null) {
				mActionBar.setSubtitle(mVersionName);
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Menus
	// --------------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	// Misc
	// --------------------------------------------------------------------------------------------

	@Override
	protected int getLayoutId() {
		return R.layout.about_form;
	}
}