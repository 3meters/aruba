package com.proxibase.aircandi.components;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
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

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.proxibase.aircandi.Aircandi;
import com.proxibase.aircandi.CandiSearchActivity;
import com.proxibase.aircandi.Preferences;
import com.proxibase.aircandi.ProfileForm;
import com.proxibase.aircandi.R;
import com.proxibase.aircandi.SignInForm;
import com.proxibase.aircandi.components.Command.CommandVerb;
import com.proxibase.aircandi.components.ImageManager.ImageRequest;
import com.proxibase.aircandi.components.ImageManager.ImageRequest.ImageShape;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.widgets.ActionsWindow;
import com.proxibase.aircandi.widgets.WebImageView;
import com.proxibase.sdk.android.proxi.consumer.Beacon;
import com.proxibase.sdk.android.proxi.consumer.Comment;
import com.proxibase.sdk.android.proxi.consumer.Entity;
import com.proxibase.sdk.android.proxi.consumer.User;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.GsonType;

public class AircandiCommon {

	public Context			mContext;
	public Activity			mActivity;
	public LayoutInflater	mLayoutInflater;
	public String			mPrefTheme;

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

		mProgressDialog = new Dialog(mContext, R.style.progress_body);
		mProgressDialog.setTitle(null);
		mProgressDialog.setCancelable(true);
		mProgressDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

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

		Intent intent = new Intent(mContext, CandiSearchActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		mContext.startActivity(intent);
	}

	public void doCommand(final Command command) {

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
			}
			else {
				/* It might not be in the big model if this is part my candi */
				Entity entity = ProxiExplorer.getInstance().getEntityById(command.entityId);
				IntentBuilder intentBuilder = new IntentBuilder(mContext, clazz);
				intentBuilder.setCommand(command);
				if (entity != null) {
					intentBuilder.setEntity(entity);
				}
				else {
					intentBuilder.setEntityId(command.entityId);
				}
				intentBuilder.setEntityType(command.entityType);
				Intent intent = intentBuilder.create();
				((Activity) mContext).startActivityForResult(intent, CandiConstants.ACTIVITY_ENTITY_HANDLER);
			}
		}
		catch (ClassNotFoundException exception) {
			exception.printStackTrace();
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
			ImageRequest imageRequest = new ImageRequest(imageUri, linkUri, ImageShape.Square, false, false,
					CandiConstants.IMAGE_WIDTH_USER_SMALL, false, true, true, 1, this, null);
			imageView.setImageRequest(imageRequest, null);
		}
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

	// --------------------------------------------------------------------------------------------
	// Utility routines
	// --------------------------------------------------------------------------------------------

	public void recycleImageViewDrawable(int resourceId) {
		ImageView imageView = ((ImageView) mActivity.findViewById(resourceId));
		if (imageView.getDrawable() != null) {
			BitmapDrawable bitmapDrawable = (BitmapDrawable) imageView.getDrawable();
			if (bitmapDrawable != null && bitmapDrawable.getBitmap() != null) {
				bitmapDrawable.getBitmap().recycle();
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Lifecycle routines
	// --------------------------------------------------------------------------------------------

	public void doDestroy() {
	/* Nothing right now but stubbed for later. */
	}

	// --------------------------------------------------------------------------------------------
	// Inner classes
	// --------------------------------------------------------------------------------------------

	public enum ActionButtonSet {
		Radar, CandiForm, CandiList, CommentList
	}

	public static class IntentBuilder {

		private Context		mContext;
		private Entity		mEntity;
		private Integer		mEntityId;
		private Integer		mParentEntityId;
		private String		mEntityType;
		private String		mMessage;
		private Command		mCommand;
		private String		mBeaconId;
		private Boolean		mStripChildEntities	= true;
		private Class<?>	mClass;

		public IntentBuilder() {}

		public IntentBuilder(Context context, Class<?> clazz) {
			this.mContext = context;
			this.mClass = clazz;
		}

		public Intent create() {
			Intent intent = new Intent(mContext, mClass);

			/* We want to make sure that any child entities don't get serialized */
			GsonBuilder gsonb = new GsonBuilder();

			gsonb.setExclusionStrategies(new ExclusionStrategy() {

				@Override
				public boolean shouldSkipClass(Class<?> clazz) {
					return false;
					//return (clazz == (Class<List<Entity>>) (Class<?>) List.class);
				}

				@Override
				public boolean shouldSkipField(FieldAttributes f) {
					/* We always skip these fields because they produce circular references */
					boolean skip = (f.getDeclaringClass() == Beacon.class && f.getName().equals("entities"))
									|| (f.getDeclaringClass() == Command.class && f.getName().equals("entity"));

					if (mStripChildEntities) {
						skip = skip || (f.getDeclaringClass() == Entity.class && f.getName().equals("children"));
					}
					return skip;
					//return (f.getDeclaredType() == (Class<List<Entity>>) (Class<?>) List.class);
				}
			});

			Gson gson = gsonb.create();

			if (mCommand != null) {
				String jsonCommand = gson.toJson(mCommand);
				intent.putExtra(mContext.getString(R.string.EXTRA_COMMAND), jsonCommand);
			}

			if (mParentEntityId != null) {
				intent.putExtra(mContext.getString(R.string.EXTRA_PARENT_ENTITY_ID), mParentEntityId);
			}

			if (mEntityType != null) {
				intent.putExtra(mContext.getString(R.string.EXTRA_ENTITY_TYPE), mEntityType);
			}

			if (mBeaconId != null) {
				intent.putExtra(mContext.getString(R.string.EXTRA_BEACON_ID), mBeaconId);
			}

			if (mMessage != null) {
				intent.putExtra(mContext.getString(R.string.EXTRA_MESSAGE), mMessage);
			}

			if (mEntity != null) {
				String jsonEntity = gson.toJson(mEntity);
				if (jsonEntity.length() > 0) {
					intent.putExtra(mContext.getString(R.string.EXTRA_ENTITY), jsonEntity);
				}
			}

			if (mEntityId != null) {
				intent.putExtra(mContext.getString(R.string.EXTRA_ENTITY_ID), mEntityId);
			}
			return intent;
		}

		public IntentBuilder setContext(Context context) {
			this.mContext = context;
			return this;
		}

		public IntentBuilder setEntity(Entity entity) {
			this.mEntity = entity;
			return this;
		}

		public IntentBuilder setParentEntityId(Integer parentEntityId) {
			this.mParentEntityId = parentEntityId;
			return this;
		}

		public IntentBuilder setEntityType(String entityType) {
			this.mEntityType = entityType;
			return this;
		}

		public IntentBuilder setMessage(String message) {
			this.mMessage = message;
			return this;
		}

		public IntentBuilder setCommand(Command command) {
			this.mCommand = command;
			return this;
		}

		public IntentBuilder setBeaconId(String beaconId) {
			this.mBeaconId = beaconId;
			return this;
		}

		public IntentBuilder setClass(Class<?> _class) {
			this.mClass = _class;
			return this;
		}

		public IntentBuilder setEntityId(Integer entityId) {
			this.mEntityId = entityId;
			return this;
		}
	}

	public static class EventHandler {

		public void onEvent(Object data) {}
	}
}
