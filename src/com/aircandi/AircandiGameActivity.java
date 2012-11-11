package com.aircandi;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.entity.scene.Scene;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;

import com.aircandi.components.AircandiCommon;
import com.aircandi.components.Logger;

public abstract class AircandiGameActivity extends LayoutGameActivity {

	protected AircandiCommon	mCommon;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		mCommon = new AircandiCommon(this, savedInstanceState);
		mCommon.setTheme(null, false);
		super.onCreate(savedInstanceState);
		mCommon.initialize();
	}

	// --------------------------------------------------------------------------------------------
	// Events routines
	// --------------------------------------------------------------------------------------------

	public void onHomeClick(View view) {}

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		mCommon.doAttachedToWindow();
	}

	public void onProfileClick(View view) {
		mCommon.doProfileClick();
	}

	public void onRefresh() {}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Logger.d(this, "Configuration changed");
		super.onConfigurationChanged(newConfig);
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle routines
	// --------------------------------------------------------------------------------------------
	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected int getRenderSurfaceViewID() {
		return 0;
	}

	@Override
	public void onLoadComplete() {}

	@Override
	public Engine onLoadEngine() {
		return null;
	}

	@Override
	public void onLoadResources() {}

	@Override
	public Scene onLoadScene() {
		return null;
	}
}