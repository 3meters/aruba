package com.proxibase.aircandi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.proxibase.aircandi.Aircandi.CandiTask;
import com.proxibase.aircandi.components.AircandiCommon;
import com.proxibase.aircandi.components.Command;
import com.proxibase.aircandi.components.IntentBuilder;
import com.proxibase.aircandi.widgets.WebImageView;
import com.proxibase.sdk.android.proxi.consumer.User;

public abstract class CandiActivity extends Activity {

	protected int				mLastResultCode	= Activity.RESULT_OK;
	protected AircandiCommon	mCommon;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		mCommon = new AircandiCommon(this);
		mCommon.setTheme();
		mCommon.unpackIntent();
		setContentView(getLayoutId());
		super.onCreate(savedInstanceState);
		mCommon.initialize();
		mCommon.initializeDialogs();
	}

	protected void bind() {
		if (Aircandi.getInstance().getCandiTask() == CandiTask.RadarCandi) {
			mCommon.setActiveTab(((ViewGroup) findViewById(R.id.image_tab_host)).getChildAt(0));
		}
		else {
			mCommon.setActiveTab(((ViewGroup) findViewById(R.id.image_tab_host)).getChildAt(1));
		}
	}

	// --------------------------------------------------------------------------------------------
	// Events routines
	// --------------------------------------------------------------------------------------------

	public void onHomeClick(View view) {
		mCommon.doHomeClick(view);
	}

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		mCommon.doAttachedToWindow();
	}

	public void onTabClick(View view) {
		mCommon.setActiveTab(view);
		if (view.getTag().equals("radar")) {
			Aircandi.getInstance().setCandiTask(CandiTask.RadarCandi);
			Intent intent = new Intent(this, CandiRadar.class);
			startActivity(intent);
			overridePendingTransition(R.anim.fade_in_short, R.anim.fade_out_short);
		}
		else if (view.getTag().equals("mycandi")) {
			Aircandi.getInstance().setCandiTask(CandiTask.MyCandi);
			IntentBuilder intentBuilder = new IntentBuilder(this, CandiList.class);
			Intent intent = intentBuilder.create();
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			startActivity(intent);
			overridePendingTransition(R.anim.fade_in_medium, R.anim.hold);
		}
	}

	public void onProfileClick(View view) {
		mCommon.doProfileClick(view);
	}

	public void onBackPressed() {
		setResult(mLastResultCode);
		super.onBackPressed();
	}

	public void onCommandButtonClick(View view) {
		if (mCommon.mActionsWindow != null) {
			mCommon.mActionsWindow.dismiss();
		}
		Command command = (Command) view.getTag();
		mCommon.doCommand(command);
	}

	public void onRefreshClick(View view) {
		mCommon.doRefreshClick(view);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {}

	// --------------------------------------------------------------------------------------------
	// Application menu routines (settings)
	// --------------------------------------------------------------------------------------------

	public boolean onCreateOptionsMenu(Menu menu) {
		mCommon.doCreateOptionsMenu(menu);
		return true;
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		mCommon.doPrepareOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		mCommon.doOptionsItemSelected(item);
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle routines
	// --------------------------------------------------------------------------------------------

	@Override
	protected void onRestart() {
		super.onRestart();
		if (findViewById(R.id.image_user) != null && Aircandi.getInstance().getUser() != null) {
			User user = Aircandi.getInstance().getUser();
			mCommon.setUserPicture(user.imageUri, user.linkUri, (WebImageView) findViewById(R.id.image_user));
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (!mCommon.mPrefTheme.equals(Aircandi.settings.getString(Preferences.PREF_THEME, "aircandi_theme_midnight"))) {
			mCommon.mPrefTheme = Aircandi.settings.getString(Preferences.PREF_THEME, "aircandi_theme_midnight");
			mCommon.reload();
		}
	}

	protected int getLayoutId() {
		return 0;
	}

	protected void onDestroy() {
		/* This activity gets destroyed everytime we leave using back or finish(). */
		super.onDestroy();
		mCommon.doDestroy();
	}
}