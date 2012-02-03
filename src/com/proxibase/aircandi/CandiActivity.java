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
import com.proxibase.aircandi.components.Logger;
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
		Logger.i(this, "CandiActivity created");
	}

	protected void bind() {
		if (Aircandi.getInstance().getCandiTask() == CandiTask.RadarCandi) {
			mCommon.setActiveTab(((ViewGroup) findViewById(R.id.image_tab_host)).getChildAt(0));
		}
		else if (Aircandi.getInstance().getCandiTask() == CandiTask.MyCandi) {
			mCommon.setActiveTab(((ViewGroup) findViewById(R.id.image_tab_host)).getChildAt(2));
		}
		else if (Aircandi.getInstance().getCandiTask() == CandiTask.Map) {
			mCommon.setActiveTab(((ViewGroup) findViewById(R.id.image_tab_host)).getChildAt(4));
		}
	}

//	@Override
//	protected void onNewIntent(Intent intent) {
//		super.onNewIntent(intent);
//		Logger.i(this, "CandiActivity got new intent");
//		setContentView(getLayoutId());
//	}

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
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			startActivity(intent);
			overridePendingTransition(R.anim.fade_in_short, R.anim.fade_out_short);
		}
		else if (view.getTag().equals("mycandi")) {
			Aircandi.getInstance().setCandiTask(CandiTask.MyCandi);
			IntentBuilder intentBuilder = new IntentBuilder(this, CandiList.class);
			Intent intent = intentBuilder.create();
			startActivity(intent);
			overridePendingTransition(R.anim.fade_in_medium, R.anim.hold);
		}
		else if (view.getTag().equals("map")) {
			Aircandi.getInstance().setCandiTask(CandiTask.Map);
			IntentBuilder intentBuilder = new IntentBuilder(this, CandiMap.class);
			Intent intent = intentBuilder.create();
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
		overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
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
		Logger.d(this, "CandiActivity restarting");
		super.onRestart();

		if (!mCommon.mPrefTheme.equals(Aircandi.settings.getString(Preferences.PREF_THEME, "aircandi_theme_midnight"))) {
			Logger.d(this, "Restarting because of theme change");
			mCommon.mPrefTheme = Aircandi.settings.getString(Preferences.PREF_THEME, "aircandi_theme_midnight");
			mCommon.reload();
		}
		else {

			/* User could have changed */
			if (findViewById(R.id.image_user) != null && Aircandi.getInstance().getUser() != null) {
				User user = Aircandi.getInstance().getUser();
				mCommon.setUserPicture(user.imageUri, user.linkUri, (WebImageView) findViewById(R.id.image_user));
			}

			/* Currrent tab could have changed */
			if (Aircandi.getInstance().getCandiTask() == CandiTask.RadarCandi) {
				mCommon.setActiveTab(((ViewGroup) findViewById(R.id.image_tab_host)).getChildAt(0));
			}
			else if (Aircandi.getInstance().getCandiTask() == CandiTask.MyCandi) {
				mCommon.setActiveTab(((ViewGroup) findViewById(R.id.image_tab_host)).getChildAt(2));
			}
			else if (Aircandi.getInstance().getCandiTask() == CandiTask.Map) {
				mCommon.setActiveTab(((ViewGroup) findViewById(R.id.image_tab_host)).getChildAt(4));
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	protected int getLayoutId() {
		return 0;
	}

	protected void onDestroy() {
		/* This activity gets destroyed everytime we leave using back or finish(). */
		super.onDestroy();
		mCommon.doDestroy();
		Logger.d(this, "CandiActivity destroyed");
	}
}