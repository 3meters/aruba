package com.proxibase.aircandi.components;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.proxibase.aircandi.Aircandi;
import com.proxibase.aircandi.CandiRadar;
import com.proxibase.aircandi.Preferences;
import com.proxibase.aircandi.ProfileForm;
import com.proxibase.aircandi.R;
import com.proxibase.aircandi.SignInForm;
import com.proxibase.aircandi.components.Command.CommandVerb;
import com.proxibase.aircandi.components.NetworkManager.ResponseCode;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.widgets.ActionsWindow;
import com.proxibase.aircandi.widgets.WebImageView;
import com.proxibase.sdk.android.proxi.consumer.Comment;
import com.proxibase.sdk.android.proxi.consumer.Entity;
import com.proxibase.sdk.android.proxi.consumer.User;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.Query;
import com.proxibase.sdk.android.proxi.service.ServiceRequest;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.RequestType;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ResponseFormat;
import com.proxibase.sdk.android.util.ProxiConstants;

public class AircandiCommon {

	public static final int	MENU_ITEM_NEW_POST_ID		= 1;
	public static final int	MENU_ITEM_NEW_PICTURE_ID	= 2;
	public static final int	MENU_ITEM_NEW_LINK_ID		= 3;

	public Context			mContext;
	public Activity			mActivity;
	public LayoutInflater	mLayoutInflater;

	public Command			mCommand;
	public Integer			mParent;
	public Entity			mEntity;
	public Integer			mEntityId;
	public String			mEntityType;
	public List<Entity>		mEntities;
	public Comment			mComment;
	public String			mMessage;
	public String			mBeaconId;

	private int				mTextColorFocused;
	private int				mTextColorUnfocused;
	private int				mHeightActive;
	private int				mHeightInactive;
	private String			mThemeTone;
	private int				mIconPost;
	private int				mIconPicture;
	private int				mIconLink;

	protected ImageView		mProgressIndicator;
	protected ImageView		mButtonRefresh;
	public Dialog		mProgressDialog;
	public ActionsWindow	mActionsWindow;
	public String			mPrefTheme;
	public IconContextMenu	mIconContextMenu			= null;
	public Integer			mTabIndex;

	public AircandiCommon(Context context) {
		mContext = context;
		mActivity = (Activity) context;
	}

	public void initialize() {

		mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		/* Tabs */
		TypedValue resourceName = new TypedValue();
		if (mActivity.getTheme().resolveAttribute(R.attr.textColorFocused, resourceName, true)) {
			mTextColorFocused = Color.parseColor((String) resourceName.coerceToString());
		}

		if (mActivity.getTheme().resolveAttribute(R.attr.textColorUnfocused, resourceName, true)) {
			mTextColorUnfocused = Color.parseColor((String) resourceName.coerceToString());
		}

		if (mActivity.getTheme().resolveAttribute(R.attr.themeTone, resourceName, true)) {
			mThemeTone = (String) resourceName.coerceToString();
			if (mThemeTone.equals("dark")) {
				mIconPost = R.drawable.icon_post;
				mIconPicture = R.drawable.icon_picture;
				mIconLink = R.drawable.icon_link;
			}
			else if (mThemeTone.equals("light")) {
				mIconPost = R.drawable.icon_post;
				mIconPicture = R.drawable.icon_picture;
				mIconLink = R.drawable.icon_link;
			}
		}

		mHeightActive = ImageUtils.getRawPixelsForDisplayPixels(6);
		mHeightInactive = ImageUtils.getRawPixelsForDisplayPixels(1);

		/* Get view references */
		mProgressIndicator = (ImageView) mActivity.findViewById(R.id.image_progress_indicator);
		if (mProgressIndicator != null) {
			mProgressIndicator.setVisibility(View.INVISIBLE);
		}

		mButtonRefresh = (ImageView) mActivity.findViewById(R.id.image_refresh_button);
		if (mButtonRefresh != null) {
			mButtonRefresh.setVisibility(View.VISIBLE);
		}

		if (mActivity.findViewById(R.id.image_user) != null && Aircandi.getInstance().getUser() != null) {
			User user = Aircandi.getInstance().getUser();
			setUserPicture(user.imageUri, user.linkUri, (WebImageView) mActivity.findViewById(R.id.image_user));
		}

		/* Dialogs */
		mProgressDialog = new Dialog(mContext, R.style.progress_body);
		mProgressDialog.setTitle(null);
		mProgressDialog.setCancelable(true);
		mProgressDialog.setCanceledOnTouchOutside(true);
		mProgressDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		mProgressDialog.setOnDismissListener(new OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				final ImageView image = (ImageView) mProgressDialog.findViewById(R.id.image_body_progress_indicator);
				image.setBackgroundResource(0);
			}
		});
	}

	public void initializeDialogs() {

		/* New candi menu */
		mIconContextMenu = new IconContextMenu(mActivity, CandiConstants.DIALOG_NEW_CANDI_ID);
		Resources resources = mActivity.getResources();
		mIconContextMenu.addItem(resources, resources.getString(R.string.dialog_new_post), mIconPost, MENU_ITEM_NEW_POST_ID);
		mIconContextMenu.addItem(resources, resources.getString(R.string.dialog_new_picture), mIconPicture, MENU_ITEM_NEW_PICTURE_ID);
		mIconContextMenu.addItem(resources, resources.getString(R.string.dialog_new_link), mIconLink, MENU_ITEM_NEW_LINK_ID);

		//set onclick listener for context menu
		mIconContextMenu.setOnClickListener(new IconContextMenu.IconContextMenuOnClickListener() {

			@Override
			public void onClick(int menuId) {

				Command command = null;

				if (menuId == MENU_ITEM_NEW_POST_ID) {
					command = new Command(CommandVerb.New, "Post", "EntityForm", CandiConstants.TYPE_CANDI_POST, null, mEntityId, null);
				}
				else if (menuId == MENU_ITEM_NEW_PICTURE_ID) {
					command = new Command(CommandVerb.New, "Picture", "EntityForm", CandiConstants.TYPE_CANDI_PICTURE, null, mEntityId, null);
				}
				else if (menuId == MENU_ITEM_NEW_LINK_ID) {
					command = new Command(CommandVerb.New, "Link", "EntityForm", CandiConstants.TYPE_CANDI_LINK, null, mEntityId, null);
				}
				doCommand(command);
			}
		});
	}

	public void unpackIntent() {

		Bundle extras = mActivity.getIntent().getExtras();
		if (extras != null) {

			mParent = extras.getInt(mContext.getString(R.string.EXTRA_PARENT_ENTITY_ID));
			mBeaconId = extras.getString(mContext.getString(R.string.EXTRA_BEACON_ID));
			mEntityType = extras.getString(mContext.getString(R.string.EXTRA_ENTITY_TYPE));
			mEntityId = extras.getInt(mContext.getString(R.string.EXTRA_ENTITY_ID));
			mMessage = extras.getString(mContext.getString(R.string.EXTRA_MESSAGE));

			String json = extras.getString(mContext.getString(R.string.EXTRA_ENTITY));
			if (json != null && json.length() > 0) {
				mEntity = ProxibaseService.getGson(GsonType.Internal).fromJson(json, Entity.class);
				mEntityId = mEntity.id;
			}

			json = extras.getString(mContext.getString(R.string.EXTRA_ENTITY_LIST));
			if (json != null && json.length() > 0) {
				mEntities = (List<Entity>) (List<?>) ProxibaseService.convertJsonToObjects(json, Entity.class, GsonType.Internal);
			}

			json = extras.getString(mContext.getString(R.string.EXTRA_COMMAND));
			if (json != null && !json.equals("")) {
				mCommand = ProxibaseService.getGson(GsonType.Internal).fromJson(json, Command.class);
			}

			json = extras.getString(mContext.getString(R.string.EXTRA_COMMENT));
			if (json != null && json.length() > 0) {
				mComment = ProxibaseService.getGson(GsonType.Internal).fromJson(json, Comment.class);
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------	
	public void doHomeClick(View view) {

		Intent intent = new Intent(mContext, CandiRadar.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		mContext.startActivity(intent);
	}

	public void doCommand(final Command command) {

		if (command.verb == CommandVerb.Dialog) {
			String dialogName = command.activityName;
			if (dialogName.toLowerCase().equals("newcandi")) {
				mActivity.showDialog(CandiConstants.DIALOG_NEW_CANDI_ID);
			}
		}
		else {
			try {
				Class clazz = Class.forName(CandiConstants.APP_PACKAGE_NAME + command.activityName, false, mContext.getClass().getClassLoader());
				if (command.verb == CommandVerb.New) {
					String beaconId = ProxiExplorer.getInstance().getStrongestBeacon().id;
					IntentBuilder intentBuilder = new IntentBuilder(mContext, clazz);
					intentBuilder.setCommand(command);
					intentBuilder.setParentEntityId(command.entityParentId);
					intentBuilder.setBeaconId(beaconId);
					intentBuilder.setEntityType(command.entityType);
					Intent intent = intentBuilder.create();
					((Activity) mContext).startActivityForResult(intent, CandiConstants.ACTIVITY_ENTITY_HANDLER);
					((Activity) mContext).overridePendingTransition(R.anim.form_in, R.anim.browse_out);
				}
				else {
					/*
					 * We could try and pass the entity instead of the entity id so we don't have to
					 * load it again but that would require handling cases where the entity doesn't always
					 * exist in the proxi model.
					 */
					IntentBuilder intentBuilder = new IntentBuilder(mContext, clazz);
					intentBuilder.setCommand(command);
					intentBuilder.setEntityId(command.entityId);
					intentBuilder.setEntityType(command.entityType);
					Intent intent = intentBuilder.create();
					((Activity) mContext).startActivityForResult(intent, CandiConstants.ACTIVITY_ENTITY_HANDLER);
					((Activity) mContext).overridePendingTransition(R.anim.form_in, R.anim.browse_out);
				}
			}
			catch (ClassNotFoundException exception) {
				exception.printStackTrace();
			}
		}
	}

	public void doProfileClick(View view) {

		if (Aircandi.getInstance().getUser().anonymous) {
			mActivity.startActivityForResult(new Intent(mContext, SignInForm.class), CandiConstants.ACTIVITY_SIGNIN);
		}
		else {
			IntentBuilder intentBuilder = new IntentBuilder(mContext, ProfileForm.class);
			intentBuilder.setCommand(new Command(CommandVerb.Edit));
			Intent intent = intentBuilder.create();
			mActivity.startActivityForResult(intent, CandiConstants.ACTIVITY_PROFILE);
		}
		mActivity.overridePendingTransition(R.anim.form_in, R.anim.browse_out);

	}

	public void doRefreshClick(View view) {}

	public void doAttachedToWindow() {
		Window window = mActivity.getWindow();
		window.setFormat(PixelFormat.RGBA_8888);
	}

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	public void showProgressDialog(boolean visible, String message) {

		if (visible) {
			mProgressDialog.setContentView(R.layout.dialog_progress);
			final ImageView image = (ImageView) mProgressDialog.findViewById(R.id.image_body_progress_indicator);
			TextView text = (TextView) mProgressDialog.findViewById(R.id.text_progress_message);
			text.setText(message == null ? "Loading..." : message);

			mProgressDialog.show();
			image.post(new Runnable() {

				@Override
				public void run() {
					image.setBackgroundResource(R.drawable.busy_anim_dark);
					final AnimationDrawable animation = (AnimationDrawable) image.getBackground();
					animation.start();
				}
			});

		}
		else {
			mProgressDialog.dismiss();
		}
	}

	public void startTitlebarProgress() {
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

	public void stopTitlebarProgress() {
		if (mProgressIndicator != null) {
			mProgressIndicator.setAnimation(null);
			mButtonRefresh.setVisibility(View.VISIBLE);
			mButtonRefresh.bringToFront();
			mProgressIndicator.setVisibility(View.INVISIBLE);
		}
	}

	public void setUserPicture(String imageUri, String linkUri, final WebImageView imageView) {
		if (imageUri != null && imageUri.length() != 0) {
			ImageRequestBuilder builder = new ImageRequestBuilder(imageView);
			builder.setFromUris(imageUri, linkUri);
			ImageRequest imageRequest = builder.create();
			imageView.setImageRequest(imageRequest);
		}
	}

	public void updateUserPicture() {
		User user = Aircandi.getInstance().getUser();
		setUserPicture(user.imageUri, user.linkUri, (WebImageView) mActivity.findViewById(R.id.image_user));
	}

	public static void showAlertDialog(int iconResource, String title, String message, Context context, OnClickListener listener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(title);
		builder.setMessage(message);
		builder.setIcon(iconResource);
		if (listener != null) {
			builder.setPositiveButton(android.R.string.ok, listener);
		}
		builder.show();
	}

	public void setTheme() {
		mPrefTheme = Aircandi.settings.getString(Preferences.PREF_THEME, "aircandi_theme_midnight");
		int themeResourceId = mContext.getApplicationContext().getResources().getIdentifier(mPrefTheme, "style", mContext.getPackageName());
		((Activity) mContext).setTheme(themeResourceId);
	}

	public void setActiveTab(View view) {
		ViewGroup tabHost = (ViewGroup) view.getParent();
		for (int i = 0; i < tabHost.getChildCount(); i++) {
			View tab = tabHost.getChildAt(i);
			TextView label = (TextView) tab.findViewById(R.id.image_tab_label);
			ImageView image = (ImageView) tab.findViewById(R.id.image_tab_image);
			if (label != null) {
				if (tab == view) {
					mTabIndex = i;
					label.setTextColor(mTextColorFocused);
					RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, mHeightActive);
					params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
					image.setLayoutParams(params);
				}
				else {
					label.setTextColor(mTextColorUnfocused);
					RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, mHeightInactive);
					params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
					image.setLayoutParams(params);
				}
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Menu routines
	// --------------------------------------------------------------------------------------------

	public void doCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = mActivity.getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
	}

	public void doPrepareOptionsMenu(Menu menu) {
		/* Hide the sign out option if we don't have a current session */
		MenuItem itemOut = menu.findItem(R.id.signinout);
		MenuItem itemProfile = menu.findItem(R.id.profile);
		if (Aircandi.getInstance().getUser() != null && !Aircandi.getInstance().getUser().anonymous) {
			itemOut.setTitle("Sign Out");
			itemProfile.setVisible(true);
		}
		else {
			itemOut.setTitle("Sign In");
			itemProfile.setVisible(false);
		}
	}

	public boolean doOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.settings :
				mActivity.startActivityForResult(new Intent(mActivity, Preferences.class), CandiConstants.ACTIVITY_PREFERENCES);
				mActivity.overridePendingTransition(R.anim.form_in, R.anim.browse_out);
				return false;
			case R.id.profile :
				doProfileClick(null);
				return false;
			case R.id.signinout :
				if (Aircandi.getInstance().getUser() != null && !Aircandi.getInstance().getUser().anonymous) {
					showProgressDialog(true, "Signing out...");
					Query query = new Query("Users").filter("Email eq 'anonymous@3meters.com'");

					ServiceResponse serviceResponse = NetworkManager.getInstance().request(
							new ServiceRequest(ProxiConstants.URL_AIRCANDI_SERVICE_ODATA, query, RequestType.Get, ResponseFormat.Json));

					if (serviceResponse.responseCode == ResponseCode.Success) {

						String jsonResponse = (String) serviceResponse.data;

						Aircandi.getInstance().setUser(
								(User) ProxibaseService.convertJsonToObject(jsonResponse, User.class, GsonType.ProxibaseService));
						Aircandi.getInstance().getUser().anonymous = true;

						Aircandi.settingsEditor.putString(Preferences.PREF_USERNAME, null);
						Aircandi.settingsEditor.putString(Preferences.PREF_PASSWORD, null);
						Aircandi.settingsEditor.commit();

						if (mActivity.findViewById(R.id.image_user) != null) {
							User user = Aircandi.getInstance().getUser();
							setUserPicture(user.imageUri, user.linkUri, (WebImageView) mActivity.findViewById(R.id.image_user));
						}
						showProgressDialog(false, null);
						ImageUtils.showToastNotification("Signed out.", Toast.LENGTH_SHORT);
						return true;
					}
					return false;
				}
				else {
					mActivity.startActivityForResult(new Intent(mActivity, SignInForm.class), CandiConstants.ACTIVITY_SIGNIN);
					mActivity.overridePendingTransition(R.anim.form_in, R.anim.browse_out);
					return false;
				}
		}
		return false;
	}

	// --------------------------------------------------------------------------------------------
	// Utility routines
	// --------------------------------------------------------------------------------------------

	public void recycleImageViewDrawable(int resourceId) {
		WebImageView imageView = ((WebImageView) mActivity.findViewById(resourceId));
		if (imageView != null) {
			imageView.onDestroy();
		}
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle routines
	// --------------------------------------------------------------------------------------------

	public void reload() {
		/*
		 * If the activity was called using startActivityForResult,
		 * the ActivityResult will ripple back down the chain.
		 * This process seems to kill the previous activities since their
		 * work appears to be completed. The back stack still exists though
		 * so hitting the back button launches new activities instead of
		 * bring the existing ones to the front. User also sees forward
		 * slide animation and loading just like a forward launching
		 * sequence.
		 */
		Intent intent = mActivity.getIntent();
		mActivity.finish();
		mActivity.startActivity(intent);
	}

	public void doDestroy() {
	/* Nothing right now but stubbed for later. */
	}

	// --------------------------------------------------------------------------------------------
	// Inner classes
	// --------------------------------------------------------------------------------------------

	public static enum ActionButtonSet {
		Radar, CandiForm, CandiList, CommentList
	}

	public static class EventHandler {

		public void onEvent(Object data) {}
	}
}
