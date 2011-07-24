package com.proxibase.aircandi.activities;

import org.anddev.andengine.ui.activity.LayoutGameActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.drawable.AnimationDrawable;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.proxibase.aircandi.candi.models.CandiModel;
import com.proxibase.aircandi.controllers.R;
import com.proxibase.aircandi.models.Post;
import com.proxibase.sdk.android.proxi.consumer.Command;
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

		configureAppHeader();

		if (mCommand != null) {
			// Configure the activity header
			configureHeader(mCommand);

			// Configure the activity footer
			configureFooter(mCommand);
		}
	}

	private void configureAppHeader() {

		TableRow row = (TableRow) findViewById(R.id.Texture_Row);
		if (row != null) {
			// Drawable bgDrawable = row.getBackground().mutate();
			// if (bgDrawable != null)
			// bgDrawable.setAlpha(64);
			//
			// if (bgDrawable != null)
			// {
			// bgDrawable.setColorFilter(null);
			// int colorFilter = getResources().getColor(R.color.point_ripple_filter);
			// bgDrawable.setColorFilter(colorFilter, PorterDuff.Mode.MULTIPLY);
			// }

		}
	}

	private void configureHeader(Command command) {

//		if (command.showHeader) {
//			// Title
//			TextView title = (TextView) findViewById(R.id.Activity_Title);
//			if (title != null)
//				title.setText(command.headerTitle);
//
//			// Icon
//			ImageView icon = (ImageView) findViewById(R.id.Activity_Icon);
//			if (icon != null)
//				if (!command.headerIconResource.equals("none"))
//					icon.setBackgroundResource(this.getResources().getIdentifier(command.headerIconResource, "drawable", this.getPackageName()));
//				else
//					icon.setVisibility(View.GONE);
//		}
//		else {
//			TableLayout activityHeader = (TableLayout) findViewById(R.id.Activity_Header);
//			if (activityHeader != null)
//				activityHeader.setVisibility(View.GONE);
//		}
	}

	private void configureFooter(Command command) {

//		if (command.showFooter) {
//		}
//		else {
//			// Layout might have a default footer in it or it might not
//			TableLayout activityFooter = (TableLayout) findViewById(R.id.Activity_Footer);
//			if (activityFooter != null)
//				activityFooter.setVisibility(View.GONE);
//		}
	}

	/*
	 * Routines required by the titlebar
	 */

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

	/*
	 * These need to be overridden by activity sub classes
	 */

	protected void onActivityButtonClick(View view) {

		AircandiUI.showToastNotification(this, "Unimplemented...", Toast.LENGTH_SHORT);
		return;
	}

	protected void onItemButtonClick(View view) {

		AircandiUI.showToastNotification(this, "Unimplemented...", Toast.LENGTH_SHORT);
		return;
	}

	protected void onItemClick(View view) {

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

	protected void setCurrentLocation(Location currentLocation) {

		((Aircandi) getApplicationContext()).currentLocation = currentLocation;
	}

	protected Location getCurrentLocation() {

		return ((Aircandi) getApplicationContext()).currentLocation;
	}

	protected void setCurrentPost(Post post) {

		((Aircandi) getApplicationContext()).currentPostX = post;
	}

	protected Post getCurrentPost() {

		return ((Aircandi) getApplicationContext()).currentPostX;
	}

	protected void setCurrentCandiModel(CandiModel currentCandiModel) {

		((Aircandi) getApplicationContext()).currentCandiModel = currentCandiModel;
	}

	protected CandiModel getCurrentCandiModel() {

		return ((Aircandi) getApplicationContext()).currentCandiModel;
	}

	public static class ImageHolder {

		public ImageView	imageView;
		public String		imageUrl;
		public String		imageId;
		public String		imageShape		= "native";
		public Bitmap		image;
		public boolean		showReflection	= false;
		public Object		data;
		public Object		containerView;
	}

	public static class ViewHolder {

		public ImageView	itemIcon;
		public TextView		itemTitle;
		public TextView		itemBody;
		public Button		itemButton;
		public LinearLayout	itemSidebarContainer;
		public TextView		itemSidebarText;
		public String		itemIconUrl;
		public Object		data;
		public String		userId;
		public String		imageFormat	= "large";
	}

	public static class ViewHolderEgg {

		public TextView		itemTitle;

		public Button		itemButton;
		public String		itemButtonImageUrl;
		public ImageView	itemFoundImage;
		public String		itemFoundImageUrl;
		public TextView		itemFoundText;
		public LinearLayout	itemFoundRow;

		public Object		data;
		public String		foundUserId;
		public String		imageFormat	= "large";
	}
}