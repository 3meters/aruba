package com.proxibase.aircandi;

import java.util.List;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.ui.activity.LayoutGameActivity;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.proxibase.aircandi.components.ImageUtils;
import com.proxibase.aircandi.components.ImageManager.ImageRequest;
import com.proxibase.aircandi.components.ImageManager.ImageRequest.ImageShape;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.widgets.WebImageView;
import com.proxibase.sdk.android.proxi.consumer.Beacon;
import com.proxibase.sdk.android.proxi.consumer.Command;
import com.proxibase.sdk.android.proxi.consumer.Comment;
import com.proxibase.sdk.android.proxi.consumer.EntityProxy;
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
	protected Integer				mParentEntityId;
	protected EntityProxy			mEntityProxy;
	protected List<EntityProxy>		mListEntities;
	protected Beacon				mBeacon;
	protected Comment				mComment;
	protected Dialog				mProgressDialog;
	protected String				mPrefTheme;
	protected Context				mContext;

	protected AircandiTab			mActiveTab			= AircandiTab.Radar;
	private int						mTextColorFocused;
	private int						mTextColorUnfocused;
	private int						mHeightActive;
	private int						mHeightInactive;
	private ImageView				mImageViewContent;
	private ImageView				mImageViewSettings;
	private TextView				mTextViewContent;
	private TextView				mTextViewSettings;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		setTheme();
		super.onCreate(savedInstanceState);
		unpackIntent(getIntent());
		configure();
	}

	protected void unpackIntent(Intent intent) {

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mParentEntityId = extras.getInt(getString(R.string.EXTRA_PARENT_ENTITY_ID));

			String json = extras.getString(getString(R.string.EXTRA_ENTITY));
			if (json != null && json.length() > 0) {
				mEntityProxy = ProxibaseService.getGson(GsonType.Internal).fromJson(json, EntityProxy.class);
			}

			json = extras.getString(getString(R.string.EXTRA_ENTITY_LIST));
			if (json != null && json.length() > 0) {
				mListEntities = (List<EntityProxy>) (List<?>) ProxibaseService.convertJsonToObjects(json, EntityProxy.class, GsonType.Internal);
			}

			json = extras.getString(getString(R.string.EXTRA_COMMAND));
			if (json != null && json.length() > 0) {
				mCommand = ProxibaseService.getGson(GsonType.Internal).fromJson(json, Command.class);
				if (mCommand.verb == null || mCommand.verb.length() == 0) {
					throw new IllegalStateException("A command passed to an activity must include a verb");
				}
			}
			else {
				throw new IllegalStateException("A command must be passed when calling an Aircandi activity");
			}

			json = extras.getString(getString(R.string.EXTRA_BEACON));
			if (json != null && json.length() > 0) {
				mBeacon = ProxibaseService.getGson(GsonType.Internal).fromJson(json, Beacon.class);
			}

			json = extras.getString(getString(R.string.EXTRA_USER));
			if (json != null && json.length() > 0) {
				mUser = ProxibaseService.getGson(GsonType.Internal).fromJson(json, User.class);
			}

			json = extras.getString(getString(R.string.EXTRA_COMMENT));
			if (json != null && json.length() > 0) {
				mComment = ProxibaseService.getGson(GsonType.Internal).fromJson(json, Comment.class);
			}
		}
	}

	private void configure() {

		/* Tabs */
		mImageViewContent = (ImageView) findViewById(R.id.image_tab_content);
		mImageViewSettings = (ImageView) findViewById(R.id.image_tab_settings);
		mTextViewContent = (TextView) findViewById(R.id.text_tab_content);
		mTextViewSettings = (TextView) findViewById(R.id.text_tab_settings);
		mTextViewContent.setText(R.string.search_tab_radar);
		mTextViewSettings.setText(R.string.search_tab_mycandi);

		TypedValue resourceName = new TypedValue();
		if (this.getTheme().resolveAttribute(R.attr.textColorFocused, resourceName, true)) {
			mTextColorFocused = Color.parseColor((String) resourceName.coerceToString());
		}

		if (this.getTheme().resolveAttribute(R.attr.textColorUnfocused, resourceName, true)) {
			mTextColorUnfocused = Color.parseColor((String) resourceName.coerceToString());
		}

		mHeightActive = ImageUtils.getRawPixelsForDisplayPixels(6);
		mHeightInactive = ImageUtils.getRawPixelsForDisplayPixels(2);
		setActiveTab(AircandiTab.Radar);

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
				if (jsonStream != null && jsonStream.length() > 0)
					mCommand = ProxibaseService.getGson(GsonType.Internal).fromJson(getIntent().getExtras().getString("stream"), Command.class);
			}
		}

		mContext = this;

	}

	private void setTheme() {
		mPrefTheme = Aircandi.settings.getString(Preferences.PREF_THEME, "aircandi_theme_midnight");
		int themeResourceId = getApplicationContext().getResources().getIdentifier(mPrefTheme, "style", getPackageName());
		this.setTheme(themeResourceId);
	}

	// --------------------------------------------------------------------------------------------
	// Events routines
	// --------------------------------------------------------------------------------------------

	public void onHomeClick(View view) {

		Intent intent = new Intent(this, CandiSearchActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	public void onSearchClick(View view) {
		ImageUtils.showToastNotification("Unimplemented...", Toast.LENGTH_SHORT);
		return;
	}

	@Override
	public void onAttachedToWindow() {

		super.onAttachedToWindow();
		Window window = getWindow();
		window.setFormat(PixelFormat.RGBA_8888);
	}

	public void onRadarTabClick(View view) {
		if (mActiveTab != AircandiTab.Radar) {
			setActiveTab(AircandiTab.Radar);
		}
	}

	public void onMyCandiTabClick(View view) {
		if (mActiveTab != AircandiTab.MyCandi) {
			setActiveTab(AircandiTab.MyCandi);
		}
	}

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------
	protected void setActiveTab(AircandiTab formTab) {
		if (formTab == AircandiTab.Radar) {
			mTextViewContent.setTextColor(mTextColorFocused);
			mTextViewSettings.setTextColor(mTextColorUnfocused);

			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, mHeightActive);
			params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
			mImageViewContent.setLayoutParams(params);

			params = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, mHeightInactive);
			params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
			mImageViewSettings.setLayoutParams(params);

		}
		else if (formTab == AircandiTab.MyCandi) {
			mTextViewContent.setTextColor(mTextColorUnfocused);
			mTextViewSettings.setTextColor(mTextColorFocused);

			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, mHeightActive);
			params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
			mImageViewSettings.setLayoutParams(params);

			params = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, mHeightInactive);
			params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
			mImageViewContent.setLayoutParams(params);

		}
		mActiveTab = formTab;
	}

	protected void showProgressDialog(boolean visible, String message) {

		if (visible) {
			mProgressDialog = new Dialog(mContext, R.style.progress_body);
			mProgressDialog.setTitle(null);
			mProgressDialog.setCancelable(true);
			mProgressDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

			mProgressDialog.setContentView(R.layout.dialog_progress);
			final ImageView image = (ImageView) mProgressDialog.findViewById(R.id.image_body_progress_indicator);
			TextView text = (TextView) mProgressDialog.findViewById(R.id.text_progress_message);
			text.setText(message == null ? "Loading..." : message);

			mProgressDialog.show();
			image.post(new Runnable() {

				@Override
				public void run() {
					image.setBackgroundResource(R.drawable.busy_anim);
					final AnimationDrawable animation = (AnimationDrawable) image.getBackground();
					animation.start();
				}
			});
		}
		else {
			mProgressDialog.dismiss();
		}
	}

	protected void startTitlebarProgress() {
		if (mProgressIndicator != null) {
			mProgressIndicator.setVisibility(View.VISIBLE);
			mButtonRefresh.setVisibility(View.INVISIBLE);
			mProgressIndicator.bringToFront();

			mProgressIndicator.post(new Runnable() {

				@Override
				public void run() {
					AnimationDrawable animation = (AnimationDrawable) mProgressIndicator.getBackground();
					animation.setOneShot(false);
					animation.start();
				}
			});
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

	protected void setUserPicture(String imageUri, final WebImageView imageView) {
		if (imageUri != null && imageUri.length() != 0) {
			ImageRequest imageRequest = new ImageRequest(imageUri, ImageShape.Square, "binary", false,
					CandiConstants.IMAGE_WIDTH_USER_SMALL, false, true, true, 1, this, null);
			imageView.setImageRequest(imageRequest, null);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle routines
	// --------------------------------------------------------------------------------------------
	@Override
	protected void onResume() {
		super.onResume();
		setActiveTab(AircandiTab.Radar);
	}

	protected void onDestroy() {
		super.onDestroy();

		/* This activity gets destroyed everytime we leave using back or finish(). */
		mEntityProxy = null;
		mListEntities = null;
		mUser = null;
		mBeacon = null;
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

	protected enum AircandiTab {
		Radar,
		MyCandi
	}

}