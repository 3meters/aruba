package com.proxibase.aircandi.components;

import java.net.UnknownHostException;
import java.util.List;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
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
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import com.proxibase.aircandi.Aircandi;
import com.proxibase.aircandi.CandiRadar;
import com.proxibase.aircandi.Preferences;
import com.proxibase.aircandi.ProfileForm;
import com.proxibase.aircandi.R;
import com.proxibase.aircandi.ScanService;
import com.proxibase.aircandi.SignInForm;
import com.proxibase.aircandi.components.Command.CommandVerb;
import com.proxibase.aircandi.components.Events.EventHandler;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.components.ProxiExplorer.WifiScanResult;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.widgets.ActionsWindow;
import com.proxibase.aircandi.widgets.WebImageView;
import com.proxibase.service.ProxibaseService;
import com.proxibase.service.ProxibaseService.GsonType;
import com.proxibase.service.ProxibaseServiceException.ErrorCode;
import com.proxibase.service.ProxibaseServiceException.ErrorType;
import com.proxibase.service.objects.Comment;
import com.proxibase.service.objects.Entity;
import com.proxibase.service.objects.User;
import com.proxibase.service.objects.GeoLocation;

public class AircandiCommon {

	public static final int				MENU_ITEM_NEW_POST_ID		= 1;
	public static final int				MENU_ITEM_NEW_PICTURE_ID	= 2;
	public static final int				MENU_ITEM_NEW_LINK_ID		= 3;

	public Context						mContext;
	public Activity						mActivity;
	public static NotificationManager	mNotificationManager;
	public static LayoutInflater		mLayoutInflater;

	/* Parameters */
	public Command						mCommand;
	public String						mParentId;
	public Entity						mEntity;
	public String						mEntityId;
	public String						mEntityType;
	public GeoLocation					mEntityLocation;
	public List<Entity>					mEntities;
	public Comment						mComment;
	public String						mMessage;
	public String						mBeaconId;
	public Boolean						mNewCandiIsRoot				= true;

	/* Theme */
	private int							mTextColorFocused;
	private int							mTextColorUnfocused;
	private int							mHeightActive;
	private int							mHeightInactive;
	private String						mThemeTone;
	private int							mIconPost;
	private int							mIconPicture;
	private int							mIconLink;

	/* UI */
	protected ImageView					mProgressIndicator;
	protected TextView					mBeaconIndicator;
	protected TextView					mTitle;
	protected ImageView					mButtonRefresh;
	public Dialog						mProgressDialog;
	public ActionsWindow				mActionsWindow;
	public String						mPrefTheme;
	public IconContextMenu				mIconContextMenu			= null;
	public Integer						mTabIndex;

	/* Other */
	private EventHandler				mEventScanReceived;
	private EventHandler				mEventLocationChanged;
	private String						mPageName;

	public AircandiCommon(Context context) {
		mContext = context;
		mActivity = (Activity) context;
		mPageName = this.getClass().getSimpleName();
	}

	public void initialize() {

		mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mNotificationManager = (NotificationManager) mContext.getSystemService(Service.NOTIFICATION_SERVICE);

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

		mTitle = (TextView) mActivity.findViewById(R.id.text_title);
		showLocationAccuracy();

		/* Beacon indicator */
		mBeaconIndicator = (TextView) mActivity.findViewById(R.id.beacon_indicator);
		if (mBeaconIndicator != null) {
			updateBeaconIndicator(ProxiExplorer.getInstance().mWifiList);
			mEventScanReceived = new EventHandler() {

				@Override
				public void onEvent(Object data) {
					List<WifiScanResult> scanList = (List<WifiScanResult>) data;
					updateBeaconIndicator(scanList);
				}
			};
		}

		mEventLocationChanged = new EventHandler() {

			@Override
			public void onEvent(Object data) {
				showLocationAccuracy();
			}
		};

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
		mProgressDialog.setCanceledOnTouchOutside(false);
		mProgressDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		mProgressDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
				WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
		mProgressDialog.setOnDismissListener(new OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				final ImageView image = (ImageView) mProgressDialog.findViewById(R.id.image_body_progress_indicator);
				image.setBackgroundResource(0);
			}
		});

	}

	public void track() {
		track(null);
	}

	public void track(String pageName) {
		if (pageName != null) {
			mPageName = pageName;
		}
		Tracker.trackPageView("/" + mPageName);
		Tracker.dispatch();
	}

	public void initializeDialogs() {

		/* New candi menu */
		mIconContextMenu = new IconContextMenu(mActivity, CandiConstants.DIALOG_NEW_CANDI_ID);
		Resources resources = mActivity.getResources();
		mIconContextMenu.addItem(resources, resources.getString(R.string.dialog_new_post), mIconPost, MENU_ITEM_NEW_POST_ID);
		mIconContextMenu.addItem(resources, resources.getString(R.string.dialog_new_picture), mIconPicture, MENU_ITEM_NEW_PICTURE_ID);
		mIconContextMenu.addItem(resources, resources.getString(R.string.dialog_new_link), mIconLink, MENU_ITEM_NEW_LINK_ID);

		// set onclick listener for context menu
		mIconContextMenu.setOnClickListener(new IconContextMenu.IconContextMenuOnClickListener() {

			@Override
			public void onClick(int menuId) {

				Command command = null;

				String parentId = mNewCandiIsRoot ? null : mEntityId;
				if (menuId == MENU_ITEM_NEW_POST_ID) {
					command = new Command(CommandVerb.New, "Post", "EntityForm", CandiConstants.TYPE_CANDI_POST, null,
							parentId, null);
				}
				else if (menuId == MENU_ITEM_NEW_PICTURE_ID) {
					command = new Command(CommandVerb.New, "Picture", "EntityForm", CandiConstants.TYPE_CANDI_PICTURE,
							null, parentId, null);
				}
				else if (menuId == MENU_ITEM_NEW_LINK_ID) {
					command = new Command(CommandVerb.New, "Link", "EntityForm", CandiConstants.TYPE_CANDI_LINK, null,
							parentId, null);
				}
				doCommand(command);
			}
		});
	}

	public void unpackIntent() {

		Bundle extras = mActivity.getIntent().getExtras();
		if (extras != null) {

			mParentId = extras.getString(mContext.getString(R.string.EXTRA_PARENT_ENTITY_ID));
			mBeaconId = extras.getString(mContext.getString(R.string.EXTRA_BEACON_ID));
			mEntityType = extras.getString(mContext.getString(R.string.EXTRA_ENTITY_TYPE));
			mEntityId = extras.getString(mContext.getString(R.string.EXTRA_ENTITY_ID));
			mMessage = extras.getString(mContext.getString(R.string.EXTRA_MESSAGE));

			String json = extras.getString(mContext.getString(R.string.EXTRA_ENTITY));
			if (json != null && json.length() > 0) {
				mEntity = ProxibaseService.getGson(GsonType.Internal).fromJson(json, Entity.class);
				mEntityId = mEntity.id;
			}

			json = extras.getString(mContext.getString(R.string.EXTRA_ENTITY_LIST));
			if (json != null && json.length() > 0) {
				mEntities = (List<Entity>) (List<?>) ProxibaseService.convertJsonToObjects(json, Entity.class,
						GsonType.Internal);
			}

			json = extras.getString(mContext.getString(R.string.EXTRA_ENTITY_LOCATION));
			if (json != null && !json.equals("")) {
				mEntityLocation = ProxibaseService.getGson(GsonType.Internal).fromJson(json, GeoLocation.class);
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
		startRadarActivity();
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
				Class clazz = Class.forName(CandiConstants.APP_PACKAGE_NAME + command.activityName, false, mContext
						.getClass().getClassLoader());
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
					 * We could try and pass the entity instead of the entity id
					 * so we don't have to load it again but that would require
					 * handling cases where the entity doesn't always exist in
					 * the proxi model.
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
		if (Aircandi.getInstance().getUser() != null) {
			if (Aircandi.getInstance().getUser().anonymous) {
				mActivity
						.startActivityForResult(new Intent(mContext, SignInForm.class), CandiConstants.ACTIVITY_SIGNIN);
			}
			else {
				IntentBuilder intentBuilder = new IntentBuilder(mContext, ProfileForm.class);
				intentBuilder.setCommand(new Command(CommandVerb.Edit));
				Intent intent = intentBuilder.create();
				mActivity.startActivityForResult(intent, CandiConstants.ACTIVITY_PROFILE);
			}
			mActivity.overridePendingTransition(R.anim.form_in, R.anim.browse_out);
		}

	}

	public void doInfoClick() {
		AircandiCommon.showAlertDialog(R.drawable.icon_app, "About", mActivity.getString(R.string.dialog_info),
				mActivity, new
				DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {}
				});
	}

	public void doRefreshClick(View view) {}

	public void doAttachedToWindow() {
		Window window = mActivity.getWindow();
		window.setFormat(PixelFormat.RGBA_8888);
	}

	public void doBeaconIndicatorClick(View view) {
		if (mBeaconIndicator != null) {
			int messageId = R.string.alert_beacons_zero;
			String beaconMessage = mActivity.getString(messageId);
			synchronized (ProxiExplorer.getInstance().mWifiList) {
				int beaconCount = ProxiExplorer.getInstance().mWifiList.size();
				if (beaconCount > 0) {
					messageId = R.string.alert_beacons_available;
					if (Aircandi.getInstance().getUser() != null
							&& Aircandi.getInstance().getUser().isDeveloper != null
							&& Aircandi.getInstance().getUser().isDeveloper) {
						beaconMessage = mActivity.getString(messageId) + "\n\n";
						for (WifiScanResult wifi : ProxiExplorer.getInstance().mWifiList) {
							if (!wifi.SSID.equals("candi_feed")) {
								beaconMessage += wifi.SSID + ": " + wifi.BSSID + "\n";
							}
						}
					}
					else {
						beaconMessage = mActivity.getString(messageId);
					}
				}
			}
			AircandiCommon.showAlertDialog(R.drawable.icon_app, "Aircandi beacons", beaconMessage, mActivity, new
					DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {}
					});
		}
	}

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	public void showLocationAccuracy() {
		final Location location = GeoLocationManager.getInstance().getCurrentLocation();
		if (location != null
				&& Aircandi.getInstance().getUser() != null
				&& Aircandi.getInstance().getUser().isDeveloper != null
				&& Aircandi.getInstance().getUser().isDeveloper) {
			if (location.hasAccuracy()) {
				TextView textView = (TextView) mActivity.findViewById(R.id.text_header_debug);
				if (textView != null) {
					textView.setVisibility(View.VISIBLE);
					textView.setText(String.valueOf(location.getAccuracy()));
				}
				else if (mTitle != null) {
					final String title = mActivity.getString(R.string.app_name);
					mActivity.runOnUiThread(new Runnable() {

						@Override
						public void run() {
							mTitle.setText(title + "  " + location.getProvider().substring(0, 1).toUpperCase()
									+ String.valueOf(location.getAccuracy()));
						}
					});
				}
			}
		}
		else {
			/* Clear location info */
			TextView textView = (TextView) mActivity.findViewById(R.id.text_header_debug);
			if (textView != null) {
				textView.setVisibility(View.GONE);
			}
			else if (mTitle != null) {
				final String title = mActivity.getString(R.string.app_name);
				mActivity.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						mTitle.setText(title);
					}
				});
			}
		}
	}

	public void updateBeaconIndicator(List<WifiScanResult> scanList) {

		synchronized (scanList) {
			Drawable drawable = mActivity.getResources().getDrawable(R.drawable.beacon_indicator_stop);
			mBeaconIndicator.setText(String.valueOf(scanList.size()));

			WifiScanResult wifiStrongest = null;

			int wifiCount = 0;
			for (WifiScanResult wifi : scanList) {
				if (wifi.global || wifi.demo ) {
					continue;
				}
				else {
					wifiCount++;
					if (wifiStrongest == null) {
						wifiStrongest = wifi;
					}
					else if (wifi.level > wifiStrongest.level) {
						wifiStrongest = wifi;
					}
				}
			}

			if (wifiCount > 0) {
				mBeaconIndicator.setText(String.valueOf(wifiCount));
				drawable = mActivity.getResources().getDrawable(R.drawable.beacon_indicator_caution);
			}
			if (wifiStrongest != null && wifiStrongest.level > -80) {
				drawable = mActivity.getResources().getDrawable(R.drawable.beacon_indicator_go);
			}
			mBeaconIndicator.setBackgroundDrawable(drawable);
		}
	}

	public void handleServiceError(ServiceResponse serviceResponse) {
		handleServiceError(serviceResponse, ServiceOperation.Unknown, null);
	}

	public void handleServiceError(ServiceResponse serviceResponse, ServiceOperation serviceOperation, Context context) {

		ErrorType errorType = serviceResponse.exception.getErrorType();
		ErrorCode errorCode = serviceResponse.exception.getErrorCode();
		String errorMessage = serviceResponse.exception.getMessage();
		String responseMessage = serviceResponse.exception.getResponseMessage();
		String logMessage = null;
		String friendlyMessage = "Network is unavailable or busy";
		Boolean showAlert = false;
		Boolean isDeveloper = false;
		if (Aircandi.getInstance().getUser().isDeveloper != null && Aircandi.getInstance().getUser().isDeveloper) {
			isDeveloper = true;
		}

		if (errorType == ErrorType.Service) {
			if (errorCode == ErrorCode.NotFoundException) {
				logMessage = "Service not found exception: " + errorMessage;
			}
			else {
				logMessage = "Service exception: " + errorMessage;
			}
		}
		/*
		 * Client errors occur when we are unable to get a response from a
		 * service, or when the client is unable to understand a response from a
		 * service. This includes protocol, network and timeout errors.
		 */
		else if (errorType == ErrorType.Client) {
			if (errorCode == ErrorCode.IOException) {
				if (serviceResponse.exception.getCause() instanceof UnknownHostException) {
					logMessage = "Unknown host exception: " + errorMessage;
					friendlyMessage = "The website you entered can't be found.";
					showAlert = true;
				}
				else {
					logMessage = "Transport exception: " + errorMessage;
				}
			}
			else if (errorCode == ErrorCode.ConnectionException) {
				logMessage = "Connection not ready: " + errorMessage;
			}
			else if (errorCode == ErrorCode.UnknownHostException) {
				logMessage = "Unknown host: " + errorMessage;
				friendlyMessage = "The website you entered can't be found.";
			}
			else if (errorCode == ErrorCode.ClientProtocolException) {
				logMessage = "Protocol exception: " + errorMessage;
			}
			else if (errorCode == ErrorCode.URISyntaxException) {
				logMessage = "Uri syntax exception: " + errorMessage;
			}
			else {
				logMessage = "Request exception: " + errorMessage;
			}
		}
		Logger.w(context, logMessage);
		if (showAlert && context != null) {
			showProgressDialog(false, null);
			stopTitlebarProgress();
			AircandiCommon.showAlertDialog(R.drawable.icon_app, "Candi service",
					friendlyMessage, context, new
					DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {}
					});
		}
		else {
			if (isDeveloper) {
				friendlyMessage = responseMessage;
			}

			Notification note = new Notification(R.drawable.icon_app, "Network problem", System.currentTimeMillis());

			RemoteViews contentView = new RemoteViews(mActivity.getPackageName(), R.layout.custom_notification);
			contentView.setImageViewResource(R.id.image, R.drawable.icon_app);
			if (isDeveloper) {
				contentView.setViewVisibility(R.id.title, View.GONE);
			}
			contentView.setTextViewText(R.id.title, "Aircandi");
			contentView.setTextViewText(R.id.text, friendlyMessage);
			note.contentView = contentView;

			Intent intent = new Intent(mContext, CandiRadar.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
			note.contentIntent = pendingIntent;

			ImageUtils.showToastNotification(friendlyMessage, Toast.LENGTH_SHORT);
			showProgressDialog(false, null);
			stopTitlebarProgress();
			mNotificationManager.notify(CandiConstants.NOTIFICATION_NETWORK, note);
		}
	}

	public void showProgressDialog(boolean visible, String message) {
		showProgressDialog(visible, message, null);
	}

	public void showProgressDialog(boolean visible, String message, Activity ownerActivity) {

		if (visible) {
			if (ownerActivity != null) {
				mProgressDialog.setOwnerActivity(ownerActivity);
			}
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
			if (mProgressDialog.isShowing() && mProgressDialog.getWindow().getWindowManager() != null) {
				mProgressDialog.dismiss();
			}
		}
	}

	public static void showAlertDialog(Integer iconResource, String titleText, String message, Context context,
			OnClickListener listener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);

		View titleView = ((Activity) context).getLayoutInflater().inflate(R.layout.temp_dialog_title, null);
		((TextView) titleView.findViewById(R.id.dialog_title_text)).setText(titleText);
		if (iconResource != null) {
			Drawable icon = context.getResources().getDrawable(iconResource);
			((ImageView) titleView.findViewById(R.id.dialog_title_image)).setImageDrawable(icon);
		}
		builder.setCustomTitle(titleView);

		View bodyView = ((Activity) context).getLayoutInflater().inflate(R.layout.temp_dialog_body, null);
		((TextView) bodyView.findViewById(R.id.dialog_body_text)).setText(message);
		builder.setView(bodyView);
		builder.setInverseBackgroundForced(true);

		if (listener != null) {
			builder.setPositiveButton(android.R.string.ok, listener);
		}
		AlertDialog alert = builder.show();
		alert.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
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

	public void setTheme() {
		mPrefTheme = Aircandi.settings.getString(Preferences.PREF_THEME, "aircandi_theme_midnight");
		int themeResourceId = mContext.getApplicationContext().getResources()
				.getIdentifier(mPrefTheme, "style", mContext.getPackageName());
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
					RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT,
							mHeightActive);
					params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
					image.setLayoutParams(params);
				}
				else {
					label.setTextColor(mTextColorUnfocused);
					RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT,
							mHeightInactive);
					params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
					image.setLayoutParams(params);
				}
			}
		}
	}

	public void signinAuto() {
		String jsonUser = Aircandi.settings.getString(Preferences.PREF_USER, null);
		Logger.i(this, "Auto sign in...");

		User user = null;
		if (jsonUser != null) {
			user = (User) ProxibaseService.convertJsonToObject(jsonUser, User.class, GsonType.ProxibaseService);
		}
		else {
			jsonUser = CandiConstants.USER_ANONYMOUS;
			user = (User) ProxibaseService.convertJsonToObject(jsonUser, User.class, GsonType.ProxibaseService);
			user.anonymous = true;
		}
		Aircandi.getInstance().setUser(user);
		ImageUtils.showToastNotification("Signed in as " + Aircandi.getInstance().getUser().name, Toast.LENGTH_SHORT);

		if (mActivity.findViewById(R.id.image_user) != null) {
			mActivity.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					User user = Aircandi.getInstance().getUser();
					setUserPicture(user.imageUri, user.linkUri, (WebImageView) mActivity.findViewById(R.id.image_user));
				}
			});
		}
	}

	public void signout() {
		if (Aircandi.getInstance().getUser() != null && !Aircandi.getInstance().getUser().anonymous) {
			showProgressDialog(true, "Signing out...");
			User user = (User) ProxibaseService.convertJsonToObject(CandiConstants.USER_ANONYMOUS, User.class,
					GsonType.ProxibaseService);
			user.anonymous = true;
			Aircandi.getInstance().setUser(user);
			if (mActivity.findViewById(R.id.image_user) != null) {
				setUserPicture(user.imageUri, user.linkUri, (WebImageView) mActivity.findViewById(R.id.image_user));
			}
			ImageUtils.showToastNotification("Signed out.", Toast.LENGTH_SHORT);
			Tracker.trackEvent("User", "Signout", null, 0);
			showProgressDialog(false, null);
		}
	}

	public void signin() {
		mActivity.startActivityForResult(new Intent(mActivity, SignInForm.class), CandiConstants.ACTIVITY_SIGNIN);
		mActivity.overridePendingTransition(R.anim.form_in, R.anim.browse_out);
	}

	public void startScanService() {

		/* Start first scan right away */
		Logger.d(this, "Starting wifi scan service");
		Intent scanIntent = new Intent(Aircandi.applicationContext, ScanService.class);
		mActivity.startService(scanIntent);

		/* Setup a scanning schedule */
		if (CandiConstants.INTERVAL_SCAN > 0) {
			AlarmManager alarmManager = (AlarmManager) mActivity.getSystemService(Service.ALARM_SERVICE);
			PendingIntent pendingIntent = PendingIntent.getService(Aircandi.applicationContext, 0, scanIntent, 0);
			alarmManager.cancel(pendingIntent);
			alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
					SystemClock.elapsedRealtime() + CandiConstants.INTERVAL_SCAN,
					CandiConstants.INTERVAL_SCAN, pendingIntent);
		}
	}

	public void stopScanService() {
		AlarmManager alarmManager = (AlarmManager) mActivity.getSystemService(Service.ALARM_SERVICE);
		Intent scanIntent = new Intent(Aircandi.applicationContext, ScanService.class);
		PendingIntent pendingIntent = PendingIntent.getService(Aircandi.applicationContext, 0, scanIntent, 0);
		alarmManager.cancel(pendingIntent);
		Logger.d(this, "Stopped wifi scan service");
	}

	public void startRadarActivity() {
		Intent intent = new Intent(mContext, CandiRadar.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		mContext.startActivity(intent);
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
		if (Aircandi.getInstance().getUser() != null && !Aircandi.getInstance().getUser().anonymous) {
			((MenuItem) menu.findItem(R.id.signin)).setVisible(false);
			((MenuItem) menu.findItem(R.id.signout)).setVisible(true);
			((MenuItem) menu.findItem(R.id.profile)).setVisible(true);
		}
		else {
			((MenuItem) menu.findItem(R.id.signin)).setVisible(true);
			((MenuItem) menu.findItem(R.id.signout)).setVisible(false);
			((MenuItem) menu.findItem(R.id.profile)).setVisible(false);
		}
	}

	public boolean doOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.settings:
			mActivity.startActivityForResult(new Intent(mActivity, Preferences.class),
					CandiConstants.ACTIVITY_PREFERENCES);
			mActivity.overridePendingTransition(R.anim.form_in, R.anim.browse_out);
			return false;
		case R.id.profile:
			doProfileClick(null);
			return false;
		case R.id.signout:
			signout();
			return false;
		case R.id.signin:
			signin();
			return false;
		case R.id.about:
			doInfoClick();
			return false;
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
		 * If the activity was called using startActivityForResult, the
		 * ActivityResult will ripple back down the chain. This process seems to
		 * kill the previous activities since their work appears to be
		 * completed. The back stack still exists though so hitting the back
		 * button launches new activities instead of bring the existing ones to
		 * the front. User also sees forward slide animation and loading just
		 * like a forward launching sequence.
		 */
		Intent intent = mActivity.getIntent();
		mActivity.finish();
		mActivity.startActivity(intent);
	}

	public void doDestroy() {
		if (mEntity != null && mEntity.imageBitmap != null) {
			mEntity.imageBitmap.recycle();
		}
		System.gc();
	}

	public void doPause() {
		stopTitlebarProgress();
		synchronized (Events.EventBus.locationChanged) {
			Events.EventBus.locationChanged.remove(mEventLocationChanged);
		}
		synchronized (Events.EventBus.wifiScanReceived) {
			Events.EventBus.wifiScanReceived.remove(mEventScanReceived);
		}
	}

	public void doResume() {
		synchronized (Events.EventBus.locationChanged) {
			Events.EventBus.locationChanged.add(mEventLocationChanged);
		}
		synchronized (Events.EventBus.wifiScanReceived) {
			Events.EventBus.wifiScanReceived.add(mEventScanReceived);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Inner classes
	// --------------------------------------------------------------------------------------------

	public String getPageName() {
		return mPageName;
	}

	public void setPageName(String pageName) {
		mPageName = pageName;
	}

	public enum ServiceOperation {
		Login,
		Logout,
		BeaconScan,
		ProfileBrowse,
		ProfileSave,
		CandiForm,
		CandiList,
		CandiBrowse,
		CandiSave,
		CandiDelete,
		ImageLoad,
		CommentBrowse,
		CommentSave,
		PictureBrowse,
		PictureSearch,
		MapBrowse,
		LinkLookup,
		Unknown
	}

	public static enum ActionButtonSet {
		Radar, CandiForm, CandiList, CommentList
	}

}
