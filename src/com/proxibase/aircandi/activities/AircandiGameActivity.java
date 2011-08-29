package com.proxibase.aircandi.activities;

import org.anddev.andengine.ui.activity.LayoutGameActivity;

import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.proxibase.aircandi.utils.AircandiUI;
import com.proxibase.sdk.android.proxi.consumer.Command;
import com.proxibase.sdk.android.proxi.consumer.User;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;

public abstract class AircandiGameActivity extends LayoutGameActivity {

	protected enum ContextButtonState {
		Default, NavigateBack, HideSummary
	}

	protected ImageView				mProgressIndicator;
	protected ImageView				mButtonRefresh;
	protected TextView				mContextButton;
	protected ContextButtonState	mContextButtonState	= ContextButtonState.Default;
	protected Command				mCommand;
	protected User					mUser;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		configure();
	}

	@Override
	public void onAttachedToWindow() {

		super.onAttachedToWindow();
		Window window = getWindow();
		window.setFormat(PixelFormat.RGBA_8888);
	}

	private void configure() {

		// Get view references
		mProgressIndicator = (ImageView) findViewById(R.id.Application_ProgressIndicator);
		if (mProgressIndicator != null)
			mProgressIndicator.setVisibility(View.INVISIBLE);

		mButtonRefresh = (ImageView) findViewById(R.id.Application_Button_Refresh);
		if (mButtonRefresh != null)
			mButtonRefresh.setVisibility(View.VISIBLE);

		mContextButton = (TextView) findViewById(R.id.Context_Button);
		if (mContextButton != null)
			mContextButton.setVisibility(View.VISIBLE);

		// If mStream wasn't set by a sub class then check to see if there is something
		// we can do to create it.
		if (mCommand == null) {
			if (getIntent() != null && getIntent().getExtras() != null) {
				String jsonStream = getIntent().getExtras().getString("stream");
				if (jsonStream != null && !jsonStream.equals(""))
					mCommand = ProxibaseService.getGson(GsonType.Internal).fromJson(getIntent().getExtras().getString("stream"), Command.class);
			}
		}
	}

	public void onHomeClick(View view) {

		Intent intent = new Intent(this, CandiSearchActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	public void onRefreshClick(View view) {
		return;
	}

	public void onSearchClick(View view) {
		AircandiUI.showToastNotification(this, "Unimplemented...", Toast.LENGTH_SHORT);
		return;
	}

	protected void startTitlebarProgress() {
		if (mProgressIndicator != null) {
			mProgressIndicator.setVisibility(View.VISIBLE);
			mButtonRefresh.setVisibility(View.INVISIBLE);
			mProgressIndicator.bringToFront();
			AnimationDrawable rippleAnimation = (AnimationDrawable) mProgressIndicator.getBackground();
			rippleAnimation.start();
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
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

}