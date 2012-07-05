package com.proxibase.aircandi;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.entity.scene.Scene;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;

import com.proxibase.aircandi.components.AircandiCommon;
import com.proxibase.aircandi.components.Logger;

public abstract class AircandiGameActivity extends LayoutGameActivity {

	private AircandiCommon	mCommon;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		mCommon = new AircandiCommon(this);
		mCommon.setTheme(null);
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
		mCommon.doProfileClick(view);
	}

	public void onRefresh() {}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Logger.i(this, "Configuration changed");
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

	public AircandiCommon getCommon() {
		return mCommon;
	}

	public void setCommon(AircandiCommon common) {
		mCommon = common;
	}
}