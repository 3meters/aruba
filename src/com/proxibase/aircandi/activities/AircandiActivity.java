package com.proxibase.aircandi.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.proxibase.aircandi.utils.ImageUtils;
import com.proxibase.sdk.android.proxi.consumer.Beacon;
import com.proxibase.sdk.android.proxi.consumer.EntityProxy;
import com.proxibase.sdk.android.proxi.consumer.User;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;

public abstract class AircandiActivity extends Activity {

	protected ImageView		mProgressIndicator;
	protected ImageView		mButtonRefresh;
	protected Button		mContextButton;

	protected Verb			mVerb;
	protected Integer		mParentEntityId;
	protected Beacon		mBeacon;
	protected Boolean		mBeaconUnregistered;
	protected EntityProxy	mEntityProxy;
	protected User			mUser;
	protected String		mPrefTheme;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		/*
		 * Theme has to be set before any UI is constructed. We also
		 * have to do it for each activity so they pickup our custom
		 * style attributes.
		 */
		setTheme();
		super.onCreate(savedInstanceState);
		super.setContentView(this.getLayoutID());

		unpackIntent(getIntent());
		configure();
	}

	private void unpackIntent(Intent intent) {

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mVerb = (Verb) extras.get(getString(R.string.EXTRA_VERB));
			mParentEntityId = extras.getInt(getString(R.string.EXTRA_PARENT_ENTITY_ID));

			String json = extras.getString(getString(R.string.EXTRA_ENTITY));
			if (json != null && !json.equals("")) {
				mEntityProxy = ProxibaseService.getGson(GsonType.Internal).fromJson(json, EntityProxy.class);
			}

			json = extras.getString(getString(R.string.EXTRA_BEACON));
			if (json != null && !json.equals("")) {
				mBeacon = ProxibaseService.getGson(GsonType.Internal).fromJson(json, Beacon.class);
			}

			json = extras.getString(getString(R.string.EXTRA_USER));
			if (json != null && !json.equals("")) {
				mUser = ProxibaseService.getGson(GsonType.Internal).fromJson(json, User.class);
			}
		}
	}

	private void setTheme() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		if (prefs != null) {
			mPrefTheme = prefs.getString(Preferences.PREF_THEME, "aircandi_theme.blueray");
			int themeResourceId = getApplicationContext().getResources().getIdentifier(mPrefTheme, "style", getPackageName());
			this.setTheme(themeResourceId);
		}
	}

	protected int getLayoutID() {
		return 0;
	}

	private void configure() {

		mProgressIndicator = (ImageView) findViewById(R.id.img_progress_indicator);
		if (mProgressIndicator != null)
			mProgressIndicator.setVisibility(View.INVISIBLE);

		mButtonRefresh = (ImageView) findViewById(R.id.img_refresh_button);
		if (mButtonRefresh != null)
			mButtonRefresh.setVisibility(View.VISIBLE);

		mContextButton = (Button) findViewById(R.id.btn_context);
		if (mContextButton != null)
			mContextButton.setVisibility(View.INVISIBLE);

		if ((mVerb == Verb.New || mEntityProxy != null) && mContextButton != null)
			showBackButton(true, getString(R.string.post_back_button));

	}

	public void showBackButton(boolean show, String backButtonText) {
		if (show) {
			mContextButton.setVisibility(View.VISIBLE);
			mContextButton.setText(backButtonText);
			mContextButton.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					doBackPressed();
				}
			});
		}
	}

	protected void startTitlebarProgress() {
		if (mProgressIndicator != null) {
			mProgressIndicator.setVisibility(View.VISIBLE);
			mButtonRefresh.setVisibility(View.INVISIBLE);
			mProgressIndicator.bringToFront();
			AnimationDrawable animation = (AnimationDrawable) mProgressIndicator.getBackground();
			animation.start();
			mProgressIndicator.invalidate();
		}
	}

	protected void stopTitlebarProgress() {
		if (mProgressIndicator != null) {
			mProgressIndicator.setAnimation(null);
			mButtonRefresh.setVisibility(View.VISIBLE);
			mButtonRefresh.bringToFront();
			mProgressIndicator.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	public void onBackPressed() {
		doBackPressed();
	}

	public void doBackPressed() {
		startTitlebarProgress();
		setResult(Activity.RESULT_OK);
		super.onBackPressed();
		overridePendingTransition(R.anim.hold, R.anim.fade_out_medium);
	}

	@Override
	public void onAttachedToWindow() {

		super.onAttachedToWindow();
		Window window = getWindow();
		window.setFormat(PixelFormat.RGBA_8888);
	}

	public void onHomeClick(View view) {
		Intent intent = new Intent(this, CandiSearchActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	/* Titlebar refresh */
	public void onRefreshClick(View view) {
		return;
	}

	/* Titlebar search */
	public void onSearchClick(View view) {
		ImageUtils.showToastNotification(this, "Unimplemented...", Toast.LENGTH_SHORT);
		return;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);

		/* Hide the sign out option if we don't have a current session */
		MenuItem item = menu.findItem(R.id.signout);
		item.setVisible(false);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		/* Hide the sign out option if we don't have a current session */
		MenuItem item = menu.findItem(R.id.signout);
		item.setVisible(false);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
			case R.id.settings :
				startActivity(new Intent(this, Preferences.class));
				return (true);
			default :
				return (super.onOptionsItemSelected(item));
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	public static enum Verb {
		New, Edit, Delete
	}
}