package com.georain.ripple.controller;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.android.BaseRequestListener;
import com.georain.ripple.model.FriendsFb;
import com.georain.ripple.model.Post;
import com.georain.ripple.model.UserFb;
import com.threemeters.sdk.android.core.Entity;
import com.threemeters.sdk.android.core.RippleService;
import com.threemeters.sdk.android.core.Stream;
import com.threemeters.sdk.android.core.RippleService.GsonType;

public abstract class RippleActivityBackup extends Activity
{
	protected ImageView				mProgressIndicator;
	protected ImageView				mButtonRefresh;
	private static final int		PREF_UPDATE	= 9;
	private Stream					mStream; 
	
	protected void setCurrentFriends(FriendsFb currentFriends)
	{
		((Ripple) getApplicationContext()).currentFriendsX = currentFriends;
	}

	protected FriendsFb getCurrentFriends()
	{
		return ((Ripple) getApplicationContext()).currentFriendsX;
	}

	protected void setCurrentPost(Post post)
	{
		((Ripple) getApplicationContext()).currentPostX = post;
	}

	protected Post getCurrentPost()
	{
		return ((Ripple) getApplicationContext()).currentPostX;
	}

	protected void setCurrentUser(UserFb currentUser)
	{
		((Ripple) getApplicationContext()).currentUserX = currentUser;
	}

	protected UserFb getCurrentUser()
	{
		return ((Ripple) getApplicationContext()).currentUserX;
	}

	protected void setCurrentPoint(Entity currentPoint)
	{
		((Ripple) getApplicationContext()).currentEntityX = currentPoint;
	}

	protected Entity getCurrentPoint()
	{
		return ((Ripple) getApplicationContext()).currentEntityX;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		init();
	}

	/*
	 * A current stream has always been set before we get called with one exception and that is
	 * for the radar.
	 */

	public void init()
	{
		// Get view references
		mProgressIndicator = (ImageView) findViewById(R.id.Application_ProgressIndicator);
		mProgressIndicator.setVisibility(View.INVISIBLE);
		mButtonRefresh = (ImageView) findViewById(R.id.Application_Button_Refresh);
		mButtonRefresh.setVisibility(View.VISIBLE);
		
		// Get configuration object for this activity
		String className = this.getClass().getSimpleName();
		String resourceName = "json_activity_" + className.toLowerCase();
		int i = this.getResources().getIdentifier(resourceName, "string", this.getPackageName());
		String json = this.getString(i);
		ActivityConfig activity = RippleService.getGson(GsonType.Internal).fromJson(json, ActivityConfig.class);

		// Configure the activity header
		configureHeader(activity);

		// Configure the activity footer
		configureFooter(activity);
	}

	private void configureHeader(ActivityConfig activityConfig)
	{
		if (activityConfig.showHeader)
		{
			// Title
			TextView title = (TextView) findViewById(R.id.Activity_Title);
			title.setText(activityConfig.titleText);

			// Icon
			if (!activityConfig.iconResource.equals("none"))
			{
				ImageView icon = (ImageView) findViewById(R.id.Activity_Icon);
				icon.setBackgroundResource(this.getResources().getIdentifier(activityConfig.iconResource, "drawable", this.getPackageName()));
			}

			// Header button
			if (activityConfig.headerButton1Visible)
			{
				TextView button = (TextView) findViewById(R.id.Activity_Header_Button1);
				button.setText(activityConfig.headerButton1Text);
				button.setTag(activityConfig.headerButton1Tag);
			}
		}
		else
		{
			TableLayout activityHeader = (TableLayout) findViewById(R.id.Activity_Header);
			if (activityHeader != null)
				activityHeader.setVisibility(View.GONE);
		}
	}

	private void configureFooter(ActivityConfig activityConfig)
	{
		if (activityConfig.showFooter)
		{
			// Header button
			if (activityConfig.footerButton1Visible)
			{
				Button button = (Button) findViewById(R.id.Activity_Footer_Button1);
				button.setText(activityConfig.footerButton1Text);
				button.setTag(activityConfig.footerButton1Tag);
			}
		}
		else
		{
			// Layout might have a default footer in it or it might not
			TableLayout activityFooter = (TableLayout) findViewById(R.id.Activity_Footer);
			if (activityFooter != null)
				activityFooter.setVisibility(View.GONE);
		}
	}

	/*
	 * Routines required by the titlebar
	 */

	public void onHomeClick(View view)
	{
		Intent intent = new Intent(this, Radar.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	// Titlebar refresh
	public void onRefreshClick(View view)
	{
		return;
	}

	// Titlebar search
	public void onSearchClick(View view)
	{
		RippleUI.showToastNotification(this, "Unimplemented...", Toast.LENGTH_SHORT);
		// Intent intent = new Intent(this, SearchList.class);
		// startActivity(intent);
	}

	/*
	 * These need to be overridden by activity sub classes
	 */

	public void onActivityButtonClick(View view)
	{
		RippleUI.showToastNotification(this, "Unimplemented...", Toast.LENGTH_SHORT);
		return;
	}

	public void onItemButtonClick(View view)
	{
		RippleUI.showToastNotification(this, "Unimplemented...", Toast.LENGTH_SHORT);
		return;
	}

	public void onItemClick(View view)
	{
		RippleUI.showToastNotification(this, "Unimplemented...", Toast.LENGTH_SHORT);
		return;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);

		// Hide the sign out option if we don't have a current session
		if (!FacebookService.facebookRunner.isSessionValid())
		{
			MenuItem item = menu.findItem(R.id.signout);
			item.setVisible(false);
		}
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		// Hide the sign out option if we don't have a current session
		if (!FacebookService.facebookRunner.isSessionValid())
		{
			MenuItem item = menu.findItem(R.id.signout);
			item.setVisible(false);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId()) {
			case R.id.settings:
				startActivity(new Intent(this, Preferences.class));
				return (true);
			case R.id.signout:
				signOut();
				return (true);
			default:
				return (super.onOptionsItemSelected(item));
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == PREF_UPDATE)
			setOptionText();
	}
	
	private void setOptionText()
	{
		finish();
	}

	public void signOut()
	{
		// User clicked OK so sign out
		FacebookService.facebookRunner.logout(new LogoutRequestListener());
	}

	private final class LogoutRequestListener extends BaseRequestListener
	{
		public void onComplete(String response)
		{
			RippleActivityBackup.this.runOnUiThread(new Runnable() {
				public void run()
				{
					// Clear out the facebook objects
					setCurrentUser(null);
					setCurrentFriends(null);
					
					// Feedback
					RippleUI.showToastNotification(RippleActivityBackup.this, "Signed out", Toast.LENGTH_SHORT);

					// Jump to the sign in activity
					Intent intent = new Intent(getApplicationContext(), RippleLogin.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
					startActivity(intent);
				}
			});
		}
	}
	
	

	public void startProgress()
	{
		mProgressIndicator.setVisibility(View.VISIBLE);
		mButtonRefresh.setVisibility(View.INVISIBLE);
		mProgressIndicator.bringToFront();
		AnimationDrawable rippleAnimation = (AnimationDrawable) mProgressIndicator.getBackground();
		rippleAnimation.start();
		mProgressIndicator.invalidate();
	}

	public void stopProgress()
	{
		mProgressIndicator.setAnimation(null);
		mButtonRefresh.setVisibility(View.VISIBLE);
		mButtonRefresh.bringToFront();
		mProgressIndicator.setVisibility(View.INVISIBLE);
	}

	@Override
	protected void onPause()
	{
		super.onPause();
	}

	@Override
	protected void onResume()
	{
		super.onResume();
	}
}