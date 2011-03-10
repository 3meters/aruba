package com.threemeters.aircandi.controller;

import android.app.TabActivity;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class Eggs extends TabActivity
{
	protected ImageView	mProgressIndicator;
	protected ImageView	mButtonRefresh;
	protected TabHost	mTabHost;
	protected String	mTabId	= "";

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		setContentView(R.layout.eggs);
		super.onCreate(savedInstanceState);

		mTabHost = getTabHost(); // The activity TabHost
		// mTabHost.getTabWidget().setDividerDrawable(R.drawable.divider);
		TabHost.TabSpec spec; // Resusable TabSpec for each tab
		Intent intent; // Reusable Intent for each tab

		if (getIntent().getExtras() != null && getIntent().getExtras().containsKey("TabId"))
			mTabId = getIntent().getExtras().getString("TabId");

		// Create an Intent to launch an Activity for the tab (to be reused)
		intent = new Intent().setClass(this, EggsGrid.class);
		spec = mTabHost.newTabSpec("egghunt").setIndicator("Egg Hunt").setContent(intent);
		mTabHost.addTab(spec);

		// Do the same for the other tabs
		intent = new Intent().setClass(this, EggsMap.class);
		intent.putExtra("layoutForTab", true);
		spec = mTabHost.newTabSpec("eggsmap").setIndicator("Map").setContent(intent);
		mTabHost.addTab(spec);

		if (!mTabId.equals(""))
			mTabHost.setCurrentTabByTag(mTabId);
		else
			mTabHost.setCurrentTab(0);

		mTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
			@Override
			public void onTabChanged(String tabId)
			{
				setTabColors(mTabHost);
			}
		});
		setTabColors(mTabHost);
		configureAppHeader();
		configure();
	}

	private void configureAppHeader()
	{
		TableRow row = (TableRow) findViewById(R.id.Texture_Row);
		if (row != null)
		{
			Drawable bgDrawable = row.getBackground().mutate();
			if (bgDrawable != null)
				bgDrawable.setAlpha(64);
		}
	}

	private void setTabColors(final TabHost tabHost)
	{
		tabHost.getTabWidget().setStripEnabled(false);
		for (int i = 0; i < tabHost.getTabWidget().getChildCount(); i++)
		{
			View view = tabHost.getTabWidget().getChildAt(i);
			if (view instanceof RelativeLayout)
			{
				tabHost.getTabWidget().getChildAt(i).getLayoutParams().height = 60;
				((TextView) ((RelativeLayout) getTabWidget().getChildAt(i)).getChildAt(1)).setTextColor(R.color.tab_inactive_text);
				tabHost.getTabWidget().getChildAt(i).setBackgroundResource(R.drawable.tab_inactive_bg);
			}
			else if (view instanceof ImageView)
			{
				tabHost.getTabWidget().getChildAt(i).getLayoutParams().height = 60;
			}
		}
		((TextView) ((RelativeLayout) getTabWidget().getChildAt(tabHost.getCurrentTab())).getChildAt(1)).setTextColor(R.color.tab_active_text);
		tabHost.getTabWidget().getChildAt(tabHost.getCurrentTab()).setBackgroundResource(R.drawable.tab_active_bg);
	}

	private void configure()
	{
		// Get view references
		mProgressIndicator = (ImageView) findViewById(R.id.Application_ProgressIndicator);
		if (mProgressIndicator != null)
			mProgressIndicator.setVisibility(View.INVISIBLE);
		mButtonRefresh = (ImageView) findViewById(R.id.Application_Button_Refresh);
		if (mButtonRefresh != null)
			mButtonRefresh.setVisibility(View.VISIBLE);
	}

	protected void startProgress()
	{
		if (mProgressIndicator != null)
		{
			mProgressIndicator.setVisibility(View.VISIBLE);
			mButtonRefresh.setVisibility(View.INVISIBLE);
			mProgressIndicator.bringToFront();
			AnimationDrawable rippleAnimation = (AnimationDrawable) mProgressIndicator.getBackground();
			rippleAnimation.start();
			mProgressIndicator.invalidate();
		}
	}

	protected void stopProgress()
	{
		if (mProgressIndicator != null)
		{
			mProgressIndicator.setAnimation(null);
			mButtonRefresh.setVisibility(View.VISIBLE);
			mButtonRefresh.bringToFront();
			mProgressIndicator.setVisibility(View.INVISIBLE);
		}
	}

	public void onHomeClick(View view)
	{
		Intent intent = new Intent(this, Dashboard.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	// Titlebar refresh
	public void onRefreshClick(View view)
	{
		if (mTabHost.getCurrentTab() == 0)
			EggsGrid.mSelf.onRefreshClick(null);
		else if (mTabHost.getCurrentTab() == 1)
			EggsMap.mSelf.onRefreshClick(null);
	}

	// Titlebar search
	public void onSearchClick(View view)
	{
		AircandiUI.showToastNotification(this, "Unimplemented...", Toast.LENGTH_SHORT);
		return;
	}

}