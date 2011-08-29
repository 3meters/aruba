package com.proxibase.aircandi.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.proxibase.aircandi.models.BaseEntity.SubType;
import com.proxibase.aircandi.utils.AircandiUI;
import com.proxibase.sdk.android.proxi.consumer.Beacon;
import com.proxibase.sdk.android.proxi.consumer.EntityProxy;
import com.proxibase.sdk.android.proxi.consumer.User;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;

public abstract class AircandiActivity extends Activity {

	protected ImageView		mProgressIndicator;
	protected ImageView		mButtonRefresh;
	protected TextView		mContextButton;

	protected Verb			mVerb;
	protected SubType		mSubType;
	protected Integer		mParentEntityId;
	protected Beacon		mBeacon;
	protected Boolean		mBeaconUnregistered;
	protected EntityProxy	mEntityProxy;
	protected User			mUser;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		super.setContentView(this.getLayoutID());

		unpackIntent(getIntent());
		configure();
	}

	private void unpackIntent(Intent intent) {

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mVerb = (Verb) extras.get(getString(R.string.EXTRA_VERB));
			mSubType = (SubType) extras.get(getString(R.string.EXTRA_SUBTYPE));
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

	protected int getLayoutID() {
		return 0;
	}

	private void configure() {

		mProgressIndicator = (ImageView) findViewById(R.id.Application_ProgressIndicator);
		if (mProgressIndicator != null)
			mProgressIndicator.setVisibility(View.INVISIBLE);

		mButtonRefresh = (ImageView) findViewById(R.id.Application_Button_Refresh);
		if (mButtonRefresh != null)
			mButtonRefresh.setVisibility(View.VISIBLE);

		mContextButton = (TextView) findViewById(R.id.Context_Button);
		if (mContextButton != null)
			mContextButton.setVisibility(View.VISIBLE);

		if (mEntityProxy != null && mContextButton != null)
			showBackButton(true);

	}

	public void showBackButton(boolean show) {
		if (show) {
			mContextButton.setText("Back");
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

	// Titlebar refresh
	public void onRefreshClick(View view) {
		return;
	}

	// Titlebar search
	public void onSearchClick(View view) {
		AircandiUI.showToastNotification(this, "Unimplemented...", Toast.LENGTH_SHORT);
		return;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);

		// Hide the sign out option if we don't have a current session
		MenuItem item = menu.findItem(R.id.signout);
		item.setVisible(false);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		// Hide the sign out option if we don't have a current session
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