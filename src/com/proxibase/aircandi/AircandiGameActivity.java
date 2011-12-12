package com.proxibase.aircandi;

import org.anddev.andengine.ui.activity.LayoutGameActivity;

import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.proxibase.aircandi.utils.ImageUtils;
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
	protected Button				mContextButton;
	protected ImageView				mLogo;
	protected ContextButtonState	mContextButtonState	= ContextButtonState.Default;
	protected Command				mCommand;
	protected User					mUser;
	protected String				mPrefTheme;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		setTheme();
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

		/* Get view references */
		mProgressIndicator = (ImageView) findViewById(R.id.image_progress_indicator);
		if (mProgressIndicator != null)
			mProgressIndicator.setVisibility(View.INVISIBLE);

		mButtonRefresh = (ImageView) findViewById(R.id.image_refresh_button);
		if (mButtonRefresh != null)
			mButtonRefresh.setVisibility(View.VISIBLE);

		mContextButton = (Button) findViewById(R.id.btn_context);
		if (mContextButton != null)
			mContextButton.setVisibility(View.GONE);

		mLogo = (ImageView) findViewById(R.id.btn_logo);
		if (mLogo != null)
			mLogo.setVisibility(View.VISIBLE);

		/*
		 * If mStream wasn't set by a sub class then check to see if there is something
		 * we can do to create it.
		 */
		if (mCommand == null) {
			if (getIntent() != null && getIntent().getExtras() != null) {
				String jsonStream = getIntent().getExtras().getString("stream");
				if (jsonStream != null && !jsonStream.equals(""))
					mCommand = ProxibaseService.getGson(GsonType.Internal).fromJson(getIntent().getExtras().getString("stream"), Command.class);
			}
		}
	}

	private void setTheme() {
		mPrefTheme = Aircandi.settings.getString(Preferences.PREF_THEME, "aircandi_theme_blueray");
		int themeResourceId = getApplicationContext().getResources().getIdentifier(mPrefTheme, "style", getPackageName());
		this.setTheme(themeResourceId);
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
		ImageUtils.showToastNotification(this, "Unimplemented...", Toast.LENGTH_SHORT);
		return;
	}

	protected void startTitlebarProgress(final boolean oneShot) {
		if (mProgressIndicator != null) {
			mProgressIndicator.setVisibility(View.VISIBLE);
			mButtonRefresh.setVisibility(View.INVISIBLE);
			mProgressIndicator.bringToFront();

			mProgressIndicator.post(new Runnable() {

				@Override
				public void run() {
					AnimationDrawable animation = (AnimationDrawable) mProgressIndicator.getBackground();
					animation.setOneShot(oneShot);
					animation.start();

				}
			});

			//mProgressIndicator.invalidate();
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