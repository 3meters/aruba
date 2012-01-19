package com.proxibase.aircandi.components;

import java.util.ArrayList;
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
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
	public Integer			mParentEntityId;
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
	private int				mIconComment;

	protected ImageView		mProgressIndicator;
	protected ImageView		mButtonRefresh;
	protected Dialog		mProgressDialog;
	public ActionsWindow	mActionsWindow;
	public String			mPrefTheme;
	public IconContextMenu	mIconContextMenu			= null;

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
				mIconPost = R.drawable.post_dark;
				mIconPicture = R.drawable.picture_dark;
				mIconLink = R.drawable.link_dark;
				mIconComment = R.drawable.post_dark;
			}
			else if (mThemeTone.equals("light")) {
				mIconPost = R.drawable.post_light;
				mIconPicture = R.drawable.picture_light;
				mIconLink = R.drawable.link_light;
				mIconComment = R.drawable.post_light;
			}
		}

		mHeightActive = ImageUtils.getRawPixelsForDisplayPixels(6);
		mHeightInactive = ImageUtils.getRawPixelsForDisplayPixels(2);

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
		mProgressDialog.setCancelable(false);
		mProgressDialog.setCanceledOnTouchOutside(false);
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

			mParentEntityId = extras.getInt(mContext.getString(R.string.EXTRA_PARENT_ENTITY_ID));
			mBeaconId = extras.getString(mContext.getString(R.string.EXTRA_BEACON_ID));
			mEntityType = extras.getString(mContext.getString(R.string.EXTRA_ENTITY_TYPE));
			mEntityId = extras.getInt(mContext.getString(R.string.EXTRA_ENTITY_ID));
			mMessage = extras.getString(mContext.getString(R.string.EXTRA_MESSAGE));

			String json = extras.getString(mContext.getString(R.string.EXTRA_ENTITY));
			if (json != null && json.length() > 0) {
				mEntity = ProxibaseService.getGson(GsonType.Internal).fromJson(json, Entity.class);
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
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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

	public void doActionsClick(View view, boolean includeEntityId, ActionButtonSet actionButtonSet) {

		if (mActionsWindow == null) {
			mActionsWindow = new ActionsWindow(mContext);
		}
		else {
			long dismissInterval = System.currentTimeMillis() - mActionsWindow.getActionStripToggleTime();
			if (dismissInterval <= 200) {
				return;
			}
		}

		int[] coordinates = { 0, 0 };

		view.getLocationInWindow(coordinates);
		final Rect rect = new Rect(coordinates[0], coordinates[1], coordinates[0] + view.getWidth(), coordinates[1] + view.getHeight());
		View content = configureActionButtonSet(actionButtonSet, mContext, includeEntityId ? mEntity.id : null);

		mActionsWindow.show(rect, content, view, 0, -10, -72);
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
			imageView.setImageRequest(imageRequest, null);
		}
	}

	public void updateUserPicture() {
		User user = Aircandi.getInstance().getUser();
		setUserPicture(user.imageUri, user.linkUri, (WebImageView) mActivity.findViewById(R.id.image_user));
	}

	public ViewGroup configureActionButtonSet(ActionButtonSet actionButtonSet, Context context, Integer entityId) {
		List<Command> commands = new ArrayList<Command>();
		if (actionButtonSet == ActionButtonSet.Radar
				|| actionButtonSet == ActionButtonSet.CandiForm
				|| actionButtonSet == ActionButtonSet.CandiList) {
			commands.add(new Command(CommandVerb.New, "Post", "EntityForm", CandiConstants.TYPE_CANDI_POST, null, entityId, mIconPost));
			commands.add(new Command(CommandVerb.New, "Picture", "EntityForm", CandiConstants.TYPE_CANDI_PICTURE, null, entityId, mIconPicture));
			commands.add(new Command(CommandVerb.New, "Link", "EntityForm", CandiConstants.TYPE_CANDI_LINK, null, entityId, mIconLink));
		}
		else if (actionButtonSet == ActionButtonSet.CommentList) {
			commands.add(new Command(CommandVerb.New, "Comment", "CommentForm", CandiConstants.TYPE_CANDI_LINK, null, entityId, mIconComment));
		}

		ViewGroup viewGroup = configureActionButtons(commands, null, context, null);
		return viewGroup;
	}

	public ViewGroup configureActionButtons(List<Command> commands, Entity entity, Context context, User user) {

		if (commands == null || commands.size() == 0) {
			return null;
		}
		/* Get the table we use for grouping and clear it */
		ViewGroup viewGroup = new LinearLayout(context);

		/* Loop the commands */
		for (Command command : commands) {
			/*
			 * TODO: This is a temporary hack. The service shouldn't pass commands
			 * that this user doesn't have sufficient permissions for.
			 */
			if (command.verb == CommandVerb.Edit && user != null) {
				if (entity.createdById != null && !entity.createdById.toString().equals(user.id)) {
					continue;
				}
			}

			/* Make a button and configure it */
			Button commandButton = (Button) mLayoutInflater.inflate(R.layout.temp_actionstrip_button, null);
			commandButton.setText(command.label);

			Drawable icon = context.getResources().getDrawable(command.iconResourceId);
			if (command.verb == CommandVerb.Edit && user != null) {
				icon = context.getResources().getDrawable(R.drawable.icon_edit_dark);
			}

			icon.setBounds(0, 0, 40, 40);
			commandButton.setCompoundDrawables(null, icon, null, null);

			commandButton.setTag(command);
			viewGroup.addView(commandButton);
		}

		return viewGroup;
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
			if (tab == view) {
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

	@SuppressWarnings("unused")
	private void showNewCandiDialog(final Integer entityId) {

		final CharSequence[] items = {
												mActivity.getResources().getString(R.string.dialog_new_picture),
												mActivity.getResources().getString(R.string.dialog_new_post),
												mActivity.getResources().getString(R.string.dialog_new_link) };
		AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
		builder.setTitle(mActivity.getResources().getString(R.string.dialog_new_message));
		builder.setCancelable(true);
		builder.setNegativeButton(mActivity.getResources().getString(R.string.dialog_new_negative), new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {}
		});
		builder.setItems(items, new DialogInterface.OnClickListener() {

			public void onClick(final DialogInterface dialog, int item) {
				Command command = null;
				if (item == 0) {
					command = new Command(CommandVerb.New, "Post", "EntityForm", CandiConstants.TYPE_CANDI_POST, null, entityId, null);
				}
				else if (item == 1) {
					command = new Command(CommandVerb.New, "Picture", "EntityForm", CandiConstants.TYPE_CANDI_PICTURE, null, entityId, null);
				}
				else if (item == 2) {
					command = new Command(CommandVerb.New, "Link", "EntityForm", CandiConstants.TYPE_CANDI_LINK, null, entityId, null);
				}
				doCommand(command);
			}
		});
		AlertDialog alert = builder.create();
		alert.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		alert.show();
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

	public void doOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.settings :
				mActivity.startActivityForResult(new Intent(mActivity, Preferences.class), CandiConstants.ACTIVITY_PREFERENCES);
				mActivity.overridePendingTransition(R.anim.form_in, R.anim.browse_out);
				return;
			case R.id.profile :
				doProfileClick(null);
				return;
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
					}
				}
				else {
					mActivity.startActivityForResult(new Intent(mActivity, SignInForm.class), CandiConstants.ACTIVITY_SIGNIN);
					mActivity.overridePendingTransition(R.anim.form_in, R.anim.browse_out);

				}
				return;
			default :
				return;
		}
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
		Intent intent = mActivity.getIntent();
		//intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		mActivity.finish();
		mActivity.startActivity(intent);
	}

	public void doDestroy() {
	/* Nothing right now but stubbed for later. */
	}

	// --------------------------------------------------------------------------------------------
	// Inner classes
	// --------------------------------------------------------------------------------------------

	public enum ActionButtonSet {
		Radar, CandiForm, CandiList, CommentList
	}

	public static class EventHandler {

		public void onEvent(Object data) {}
	}
}
