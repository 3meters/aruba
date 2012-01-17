package com.proxibase.aircandi;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.ui.activity.LayoutGameActivity;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.proxibase.aircandi.components.AircandiCommon;
import com.proxibase.aircandi.components.AircandiCommon.ActionButtonSet;

public abstract class AircandiGameActivity extends LayoutGameActivity {

	protected AircandiCommon	mCommon;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		mCommon = new AircandiCommon(this);
		mCommon.setTheme();
		super.onCreate(savedInstanceState);
		mCommon.initialize();
		mCommon.initializeDialogs();		
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

	public void onActionsClick(View view) {
		mCommon.doActionsClick(view, false, ActionButtonSet.Radar);
	}

	public void onProfileClick(View view) {
		mCommon.doProfileClick(view);
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle routines
	// --------------------------------------------------------------------------------------------
	@Override
	protected void onResume() {
		super.onResume();
		mCommon.setActiveTab(((ViewGroup) findViewById(R.id.image_tab_host)).getChildAt(0));
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