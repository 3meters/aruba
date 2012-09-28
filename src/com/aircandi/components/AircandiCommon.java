package com.aircandi.components;

import java.io.File;
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
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v4.app.FragmentTransaction;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.app.SherlockMapActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi;
import com.aircandi.Aircandi.CandiTask;
import com.aircandi.CandiForm;
import com.aircandi.CandiList;
import com.aircandi.CandiMap;
import com.aircandi.CandiPicker;
import com.aircandi.CandiRadar;
import com.aircandi.CandiRadar.RefreshType;
import com.aircandi.CommentList;
import com.aircandi.FormActivity;
import com.aircandi.HelpForm;
import com.aircandi.MapCandiForm;
import com.aircandi.MapCandiList;
import com.aircandi.Preferences;
import com.aircandi.ProfileForm;
import com.aircandi.R;
import com.aircandi.ScanService;
import com.aircandi.SignInForm;
import com.aircandi.SignUpForm;
import com.aircandi.TemplatePicker;
import com.aircandi.UserCandiForm;
import com.aircandi.UserCandiList;
import com.aircandi.candi.models.CandiPatchModel;
import com.aircandi.candi.presenters.CandiPatchPresenter;
import com.aircandi.components.AnimUtils.TransitionType;
import com.aircandi.components.Events.EventHandler;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProxiExplorer.EntityTree;
import com.aircandi.components.ProxiExplorer.ModelResult;
import com.aircandi.components.ProxiExplorer.WifiScanResult;
import com.aircandi.core.CandiConstants;
import com.aircandi.service.ProxiConstants;
import com.aircandi.service.ProxibaseService;
import com.aircandi.service.ProxibaseService.ServiceDataType;
import com.aircandi.service.ProxibaseServiceException.ErrorCode;
import com.aircandi.service.ProxibaseServiceException.ErrorType;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.GeoLocation;
import com.aircandi.service.objects.Session;
import com.aircandi.service.objects.User;
import com.aircandi.widgets.WebImageView;

public class AircandiCommon implements ActionBar.TabListener {

	public static final int				MENU_ITEM_NEW_POST_ID		= 1;
	public static final int				MENU_ITEM_NEW_PICTURE_ID	= 2;
	public static final int				MENU_ITEM_NEW_LINK_ID		= 3;

	public Context						mContext;
	public Activity						mActivity;
	public String						mActivityName;
	public static NotificationManager	mNotificationManager;
	public static LayoutInflater		mLayoutInflater;

	/* Parameters */
	public CommandType					mCommandType;
	public String						mParentId;
	public String						mEntityId;
	public String						mEntityType;
	public GeoLocation					mEntityLocation;
	public List<Entity>					mEntities;
	public String						mMessage;
	public String						mBeaconId;
	public String						mCollectionId;
	public EntityTree					mEntityTree;

	/* Theme */
	public String						mThemeTone;
	public Integer						mThemeId;
	public Integer						mThemeBusyIndicatorResId;
	public Integer						mThemeDialogResId;

	/* UI */
	protected ImageView					mProgressIndicator;
	public TextView						mBeaconIndicator;
	protected TextView					mTitle;
	protected ImageView					mButtonRefresh;
	private Dialog						mProgressDialog;
	public String						mPrefTheme;
	public Boolean						mUsingCustomTheme			= false;
	public Integer						mTabIndex;
	public ActionBar					mActionBar;
	private ViewFlipper					mViewFlipper;
	@SuppressWarnings("unused")
	private Integer						mActionBarHomeImageView;

	/* Other */
	public EventHandler					mEventScanReceived;
	private EventHandler				mEventLocationChanged;
	public String						mPageName;
	public Boolean						mNavigationTop				= false;
	public Boolean						mConfigChange				= false;
	private CandiPatchModel				mCandiPatchModel;
	private CandiPatchPresenter			mCandiPatchPresenter;

	public AircandiCommon(Context context, Bundle savedInstanceState) {
		mContext = context;
		mActivity = (Activity) context;
		mPageName = mActivity.getClass().getSimpleName();
		/*
		 * ActionBarSherlock takes over if version < 4.0 (Ice Cream Sandwich).
		 */
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			mActivity.requestWindowFeature(Window.FEATURE_NO_TITLE);
		}

		/*
		 * Base activity class handles restoring view state as long as they have
		 * an id property. We handle ourselves any other state that needs to
		 * survive activity death.
		 */
		if (savedInstanceState != null) {
			doRestoreInstanceState(savedInstanceState);
		}
	}

	public void initialize() {

		mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mNotificationManager = (NotificationManager) mContext.getSystemService(Service.NOTIFICATION_SERVICE);

		Logger.i(this, "Activity created: " + mPageName);
		Logger.d(this, "Started from radar flag: " + String.valueOf(Aircandi.getInstance().getLaunchedFromRadar()));

		/* Stash the action bar */
		if (mPageName.equals("CandiMap") || mPageName.equals("MapBrowse")) {
			mActionBar = ((SherlockMapActivity) mActivity).getSupportActionBar();
		}
		else {
			mActionBar = ((SherlockActivity) mActivity).getSupportActionBar();
		}
		mActionBarHomeImageView = android.R.id.home;
		if (mActivity.findViewById(android.R.id.home) == null) {
			mActionBarHomeImageView = R.id.abs__home;
		}

		/* Theme info */
		TypedValue resourceName = new TypedValue();
		if (mActivity.getTheme().resolveAttribute(R.attr.themeTone, resourceName, true)) {
			mThemeTone = (String) resourceName.coerceToString();
		}
		if (mContext.getTheme().resolveAttribute(R.attr.busy, resourceName, true)) {
			mThemeBusyIndicatorResId = (Integer) resourceName.resourceId;
		}

		mTitle = (TextView) mActivity.findViewById(R.id.text_title);

		/* Tabs: setup tabs if appropriate */
		manageTabs();

		/* Beacon indicator */
		mBeaconIndicator = (TextView) mActivity.findViewById(R.id.beacon_indicator);
		if (mBeaconIndicator != null) {
			updateBeaconIndicator(ProxiExplorer.getInstance().mWifiList);
			//			mEventScanReceived = new EventHandler() {
			//
			//				@Override
			//				public void onEvent(Object data) {
			//					List<WifiScanResult> scanList = (List<WifiScanResult>) data;
			//					updateBeaconIndicator(scanList);
			//				}
			//			};
		}

		mEventLocationChanged = new EventHandler() {

			@Override
			public void onEvent(Object data) {}
		};

		mButtonRefresh = null;
		if (mButtonRefresh != null) {
			mButtonRefresh.setVisibility(View.VISIBLE);
		}
	}

	public void unpackIntent() {

		Bundle extras = mActivity.getIntent().getExtras();
		if (extras != null) {

			mParentId = extras.getString(mContext.getString(R.string.EXTRA_PARENT_ENTITY_ID));
			mBeaconId = extras.getString(mContext.getString(R.string.EXTRA_BEACON_ID));
			mEntityType = extras.getString(mContext.getString(R.string.EXTRA_ENTITY_TYPE));
			mEntityId = extras.getString(mContext.getString(R.string.EXTRA_ENTITY_ID));
			mMessage = extras.getString(mContext.getString(R.string.EXTRA_MESSAGE));
			mThemeId = extras.getInt(mContext.getString(R.string.EXTRA_THEME_ID));
			mCollectionId = extras.getString(mContext.getString(R.string.EXTRA_COLLECTION_ID));
			mNavigationTop = extras.getBoolean(mContext.getString(R.string.EXTRA_NAVIGATION_TOP));

			String entityTree = extras.getString(mContext.getString(R.string.EXTRA_ENTITY_TREE));
			if (entityTree != null) {
				mEntityTree = ProxiExplorer.EntityTree.valueOf(entityTree);
			}

			String commandType = extras.getString(mContext.getString(R.string.EXTRA_COMMAND_TYPE));
			if (commandType != null) {
				mCommandType = CommandType.valueOf(commandType);
			}

			String json = extras.getString(mContext.getString(R.string.EXTRA_ENTITY_LOCATION));
			if (json != null && !json.equals("")) {
				mEntityLocation = (GeoLocation) ProxibaseService.convertJsonToObjectInternalSmart(json, ServiceDataType.GeoLocation);
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	public void doHomeClick(View view) {
		startRadarActivity();
	}

	public void doProfileClick() {
		IntentBuilder intentBuilder = new IntentBuilder(mContext, ProfileForm.class);
		intentBuilder.setCommandType(CommandType.View);
		Intent intent = intentBuilder.create();
		mActivity.startActivity(intent);
		AnimUtils.doOverridePendingTransition(mActivity, TransitionType.CandiPageToForm);
	}

	public void doInfoClick() {
		String title = mActivity.getString(R.string.alert_about_title);
		String message = mActivity.getString(R.string.alert_about_message) + " "
				+ Aircandi.getVersionName(mContext, CandiRadar.class) + "\n"
				+ mActivity.getString(R.string.dialog_info);
		AircandiCommon.showAlertDialog(R.drawable.icon_app
				, title
				, message
				, null
				, mActivity, android.R.string.ok, null, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {}
				}, null);
		Tracker.trackEvent("DialogAbout", "Open", null, 0);

	}

	public void doRefreshClick(View view) {}

	public void doAttachedToWindow() {
		Window window = mActivity.getWindow();
		window.setFormat(PixelFormat.RGBA_8888);
	}

	public void doBeaconIndicatorClick() {
		if (mBeaconIndicator != null) {
			int messageId = R.string.alert_beacons_zero;
			String beaconMessage = mActivity.getString(messageId);
			synchronized (ProxiExplorer.getInstance().mWifiList) {
				if (Aircandi.wifiCount > 0) {
					messageId = R.string.alert_beacons_available;
					if (Aircandi.getInstance().getUser() != null
							&& Aircandi.getInstance().getUser().isDeveloper != null
							&& Aircandi.getInstance().getUser().isDeveloper) {
						beaconMessage = mActivity.getString(messageId) + "\n\n";
						for (WifiScanResult wifi : ProxiExplorer.getInstance().mWifiList) {
							if (!wifi.SSID.equals("candi_feed")) {
								beaconMessage += wifi.SSID + ": (" + String.valueOf(wifi.level) + ") " + wifi.BSSID + "\n";
							}
						}
					}
					else {
						beaconMessage = mActivity.getString(messageId);
					}
				}
			}
			AircandiCommon.showAlertDialog(R.drawable.icon_app
					, mActivity.getString(R.string.alert_beacons_title)
					, beaconMessage
					, null
					, mActivity, android.R.string.ok, null, new
					DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {}
					}, null);
			Tracker.trackEvent("DialogBeacon", "Open", null, 0);
		}
	}

	// --------------------------------------------------------------------------------------------
	// UI routines
	// --------------------------------------------------------------------------------------------

	public void showTemplatePicker(Boolean isRoot) {
		/*
		 * Dialogs
		 * 
		 * To get the dialog activity to overlay the calling activity, the theme needs to be set on the activity in the
		 * manifest in order to get the correct window features. The theme can then be modified by passing the desired
		 * theme id to the dialog activity.
		 */
		Aircandi.returningFromDialog = true;
		Intent intent = new Intent(mActivity, TemplatePicker.class);
		intent.putExtra(mActivity.getString(R.string.EXTRA_ENTITY_IS_ROOT), isRoot);
		intent.putExtra(mActivity.getString(R.string.EXTRA_THEME_ID), mThemeTone.equals("dark") ? R.style.Theme_Sherlock_Dialog
				: R.style.Theme_Sherlock_Light_Dialog);
		mActivity.startActivityForResult(intent, CandiConstants.ACTIVITY_TEMPLATE_PICK);
		AnimUtils.doOverridePendingTransition(mActivity, TransitionType.CandiPageToForm);
	}

	public void showHelp(int helpResId) {
		/*
		 * Dialogs
		 * 
		 * To get the dialog activity to overlay the calling activity, the theme needs to be set on the activity in the
		 * manifest in order to get the correct window features. The theme can then be modified by passing the desired
		 * theme id to the dialog activity.
		 */
		Aircandi.returningFromDialog = true;
		Intent intent = new Intent(mActivity, HelpForm.class);
		intent.putExtra(mActivity.getString(R.string.EXTRA_STRING_ID), R.string.help_radar);
		intent.putExtra(mActivity.getString(R.string.EXTRA_THEME_ID), mThemeTone.equals("dark") ? R.style.Theme_Sherlock_Dialog
				: R.style.Theme_Sherlock_Light_Dialog);
		mActivity.startActivity(intent);
		AnimUtils.doOverridePendingTransition(mActivity, TransitionType.HelpShow);
	}

	public void updateBeaconIndicator(final List<WifiScanResult> scanList) {

		synchronized (scanList) {
			/*
			 * In case we get called from a background thread.
			 */
			mActivity.runOnUiThread(new Runnable() {

				@SuppressWarnings("deprecation")
				@Override
				public void run() {
					Drawable drawable = mActivity.getResources().getDrawable(R.drawable.beacon_indicator_stop);

					mBeaconIndicator.setText(String.valueOf(scanList.size()));

					WifiScanResult wifiStrongest = null;

					int wifiCount = 0;
					for (WifiScanResult wifi : scanList) {
						if (wifi.global) {
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

					Aircandi.wifiCount = wifiCount;

					mBeaconIndicator.setText(String.valueOf(wifiCount));
					if (wifiCount > 0) {
						drawable = mActivity.getResources().getDrawable(R.drawable.beacon_indicator_caution);
					}
					if (wifiStrongest != null && wifiStrongest.level > CandiConstants.RADAR_BEACON_INDICATOR_CAUTION) {
						drawable = mActivity.getResources().getDrawable(R.drawable.beacon_indicator_go);
					}
					mBeaconIndicator.setBackgroundDrawable(drawable);

				}
			});

		}
	}

	public void handleServiceError(ServiceResponse serviceResponse, ServiceOperation serviceOperation) {
		handleServiceError(serviceResponse, serviceOperation, mActivity);
	}

	public void handleServiceError(ServiceResponse serviceResponse, ServiceOperation serviceOperation, Context context) {

		ErrorType errorType = serviceResponse.exception.getErrorType();
		ErrorCode errorCode = serviceResponse.exception.getErrorCode();
		String errorMessage = serviceResponse.exception.getMessage();

		/* We always make sure the progress indicator has been stopped */
		showProgressDialog(false, null);

		/*
		 * Client errors occur when we are unable to get a response from a service, or when the client is
		 * unable to understand a response from a service. This includes protocol, network and timeout errors.
		 */
		if (errorType == ErrorType.Client && errorCode == ErrorCode.ConnectionException) {
			/*
			 * We don't have a network connection.
			 */
			showNotification(mActivity.getString(R.string.error_connection_title), mActivity.getString(R.string.error_connection_notification), context);
			showAlertDialogSimple(null, mActivity.getString(R.string.error_connection));
		}
		else if (errorType == ErrorType.Client && errorCode == ErrorCode.IOException) {
			/*
			 * We have a bad network connection.
			 * 
			 * This could be any of these:
			 * - ConnectTimeoutException: timeout expired trying to connect to service
			 * - SocketTimeoutException: timeout expired on a socket
			 * - NoHttpResponseException: target server failed to respond with a valid HTTP response
			 * - UnknownHostException: hostname didn't exist in the dns system
			 */
			ImageUtils.showToastNotification(mActivity.getString(R.string.error_connection_poor), Toast.LENGTH_SHORT);
		}
		else if (errorType == ErrorType.Client && errorCode == ErrorCode.SocketException) {
			/* Connection could be good but the remote service has a problem with our request */
			ImageUtils.showToastNotification(mActivity.getString(R.string.error_client_request_socket_error), Toast.LENGTH_SHORT);
		}
		else if (errorType == ErrorType.Client) {
			/*
			 * Something wrong with the request. In most cases, this is a bug and
			 * not something that a user should cause unless they provided a bad uri.
			 */
			if (errorCode == ErrorCode.UnknownHostException) {
				showAlertDialogSimple(null, mActivity.getString(R.string.error_client_unknown_host));
			}
			else {
				if (errorCode == ErrorCode.ClientProtocolException
						|| errorCode == ErrorCode.URISyntaxException) {
					ImageUtils.showToastNotification(mActivity.getString(R.string.error_client_request_error), Toast.LENGTH_SHORT);
				}
				else {
					/* Something without special handling */
					ImageUtils.showToastNotification(serviceResponse.exception.getMessage(), Toast.LENGTH_SHORT);
				}
			}
		}
		else if (errorType == ErrorType.Service && errorCode == ErrorCode.NotFoundException) {
			/*
			 * Reached the service but requested something that doesn't exist. This is a bug and
			 * not something that a user should cause.
			 */
			ImageUtils.showToastNotification(mActivity.getString(R.string.error_client_request_not_found), Toast.LENGTH_SHORT);
		}
		else if (errorType == ErrorType.Service && errorCode == ErrorCode.UnauthorizedException) {
			/* Reached the service but requested something that the user can't access. */
			ImageUtils.showToastNotification(mActivity.getString(R.string.error_service_unauthorized), Toast.LENGTH_SHORT);
		}
		else if (errorType == ErrorType.Service && errorCode == ErrorCode.ForbiddenException) {
			/* Reached the service but request was invalid per service policy. */
			ImageUtils.showToastNotification(mActivity.getString(R.string.error_service_forbidden), Toast.LENGTH_SHORT);
		}
		else if (errorType == ErrorType.Service && errorCode == ErrorCode.GatewayTimeoutException) {
			/* Reached the service but request was invalid per service policy. */
			ImageUtils.showToastNotification(mActivity.getString(R.string.error_service_gateway_timeout), Toast.LENGTH_SHORT);
		}
		else if (errorType == ErrorType.Service) {
			/*
			 * Reached the service with a good call but failed for a well known reason.
			 * 
			 * This could have been caused by any problem while inserting/updating.
			 * We look first for ones that are known responses from the service.
			 * 
			 * - 403.x: password not strong enough
			 * - 403.x: email not unique
			 * - 401.2: expired session
			 * - 401.1: invalid or missing session
			 */
			String title = null;
			String message = null;
			if (serviceResponse.exception.getHttpStatusCode() == ProxiConstants.HTTP_STATUS_CODE_UNAUTHORIZED_SESSION_EXPIRED) {
				title = mActivity.getString(R.string.error_session_expired_title);
				message = mActivity.getString(R.string.error_session_expired);
				/*
				 * Make sure the user is logged out
				 */
				signout();

			}
			else if (serviceResponse.exception.getHttpStatusCode() == ProxiConstants.HTTP_STATUS_CODE_UNAUTHORIZED_CREDENTIALS) {
				message = mActivity.getString(R.string.error_session_invalid);
				if (serviceOperation == ServiceOperation.PasswordChange) {
					message = mActivity.getString(R.string.error_change_password_unauthorized);
				}
				else if (serviceOperation == ServiceOperation.Signin) {
					message = mActivity.getString(R.string.error_signin_invalid_signin);
				}
				else {
					signout();
				}
			}
			else if (serviceResponse.exception.getHttpStatusCode() == ProxiConstants.HTTP_STATUS_CODE_FORBIDDEN_USER_PASSWORD_WEAK) {
				message = mActivity.getString(R.string.error_signup_password_weak);
			}
			else if (serviceResponse.exception.getHttpStatusCode() == ProxiConstants.HTTP_STATUS_CODE_FORBIDDEN_USER_EMAIL_NOT_UNIQUE) {
				message = mActivity.getString(R.string.error_signup_email_taken);
			}
			else {
				ImageUtils.showToastNotification(serviceResponse.exception.getMessage(), Toast.LENGTH_SHORT);
			}

			if (message != null) {
				showAlertDialogSimple(title, message);
			}
		}
		Logger.w(context, "Service error: " + errorMessage);
	}

	public void showNotification(String title, String message, Context context) {
		@SuppressWarnings("deprecation")
		Notification note = new Notification(R.drawable.icon_app_status
				, title
				, System.currentTimeMillis());

		RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.custom_notification);
		contentView.setImageViewResource(R.id.image, R.drawable.icon_app);
		contentView.setTextViewText(R.id.title, title);
		contentView.setTextViewText(R.id.text, message);
		note.contentView = contentView;

		Intent intent = new Intent(mContext, CandiRadar.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
		note.contentIntent = pendingIntent;

		mNotificationManager.notify(CandiConstants.NOTIFICATION_NETWORK, note);
	}

	public void showProgressDialog(boolean visible, String message) {
		showProgressDialog(visible, message, null);
	}

	public void showProgressDialog(boolean visible, String message, Activity ownerActivity) {

		Dialog progressDialog = getProgressDialog();
		if (visible) {
			if (progressDialog.isShowing()) {
				TextView text = (TextView) progressDialog.findViewById(R.id.text_progress_message);
				if (message == null) {
					text.setVisibility(View.GONE);
				}
				else {
					text.setVisibility(View.VISIBLE);
					text.setText(message);
				}
			}
			else {
				if (ownerActivity != null) {
					progressDialog.setOwnerActivity(ownerActivity);
				}
				progressDialog.setContentView(R.layout.dialog_progress);
				final ImageView image = (ImageView) progressDialog.findViewById(R.id.image_body_progress_indicator);
				TextView text = (TextView) progressDialog.findViewById(R.id.text_progress_message);
				if (message == null) {
					text.setVisibility(View.GONE);
				}
				else {
					text.setVisibility(View.VISIBLE);
					text.setText(message);
				}

				/* Prevent dismissing the indicator with the back key */
				progressDialog.setCancelable(false);

				progressDialog.show();
				image.post(new Runnable() {

					@Override
					public void run() {
						image.setBackgroundResource(mThemeBusyIndicatorResId);
						final AnimationDrawable animation = (AnimationDrawable) image.getBackground();
						animation.start();
					}
				});
			}
		}
		else {
			if (progressDialog.isShowing() && progressDialog.getWindow().getWindowManager() != null) {
				progressDialog.dismiss();
			}
		}
	}

	public boolean isProgressDialogShowing() {
		Dialog progressDialog = getProgressDialog();
		return (progressDialog.isShowing());
	}

	public static AlertDialog showAlertDialog(Integer iconResource, String titleText, String message, View customView, Context context, Integer okButtonId,
			Integer cancelButtonId,
			OnClickListener listenerClick, OnCancelListener listenerCancel) {

		AlertDialog.Builder builder = new AlertDialog.Builder(context)
				.setIcon(iconResource)
				.setTitle(titleText);

		if (customView == null) {
			builder.setMessage(message);
		}
		else {
			builder.setView(customView);
		}

		if (okButtonId != null) {
			builder.setPositiveButton(okButtonId, listenerClick);
		}

		if (listenerCancel != null) {
			builder.setOnCancelListener(listenerCancel);
		}

		if (cancelButtonId != null) {
			builder.setNegativeButton(cancelButtonId, listenerClick);
		}

		AlertDialog alert = builder.show();

		/* Hardcoded size for body text in the alert */
		TextView textView = (TextView) alert.findViewById(android.R.id.message);
		if (textView != null) {
			textView.setTextSize(14);
		}

		/* Prevent dimming the background */
		alert.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

		return alert;
	}

	public void showAlertDialogSimple(final String titleText, final String message) {
		mActivity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				AircandiCommon.showAlertDialog(R.drawable.icon_app
						, titleText
						, message
						, null
						, mContext, android.R.string.ok, null, new OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {}
						}, null);
			}
		});
	}

	public void setTheme(Boolean isDialog, Boolean isForm) {
		mPrefTheme = Aircandi.settings.getString(Preferences.PREF_THEME, CandiConstants.THEME_DEFAULT);
		Integer themeResId = mContext.getApplicationContext().getResources().getIdentifier(mPrefTheme, "style", mContext.getPackageName());
		if (isDialog) {
			themeResId = R.style.aircandi_theme_dialog_dark;
			if (mPrefTheme.equals("aircandi_theme_snow")
					|| mPrefTheme.equals("aircandi_theme_serene")
					|| mPrefTheme.equals("aircandi_theme_lagoon")
					|| mPrefTheme.equals("aircandi_theme_blueray")) {
				themeResId = R.style.aircandi_theme_dialog_light;
			}
		}
		else if (isForm) {
			themeResId = R.style.aircandi_theme_form_light;
		}
		((Activity) mContext).setTheme(themeResId);
	}

	public void signinAuto() {
		Logger.i(this, "Auto sign in...");

		/* Use anonymous user as initial default */
		User user = (User) ProxibaseService.convertJsonToObjectInternalSmart(CandiConstants.USER_ANONYMOUS, ServiceDataType.User);

		String jsonUser = Aircandi.settings.getString(Preferences.PREF_USER, null);
		String jsonSession = Aircandi.settings.getString(Preferences.PREF_USER_SESSION, null);

		if (jsonUser != null && jsonSession != null) {
			user = (User) ProxibaseService.convertJsonToObjectInternalSmart(jsonUser, ServiceDataType.User);
			user.session = (Session) ProxibaseService.convertJsonToObjectInternalSmart(jsonSession, ServiceDataType.Session);
			ImageUtils.showToastNotification(mActivity.getString(R.string.alert_signed_in)
					+ " " + user.name, Toast.LENGTH_SHORT);
			Tracker.startNewSession();
			Tracker.trackEvent("User", "AutoSignin", null, 0);
		}

		Aircandi.getInstance().setUser(user);

		/* Make sure onPrepareOptionsMenu gets called (since api 11) */
		((SherlockActivity) mActivity).invalidateOptionsMenu();
	}

	public void signup() {
		IntentBuilder intentBuilder = new IntentBuilder(mActivity, SignUpForm.class);
		intentBuilder.setCommandType(CommandType.New);
		Intent intent = intentBuilder.create();
		mActivity.startActivity(intent);
		AnimUtils.doOverridePendingTransition(mActivity, TransitionType.CandiPageToForm);
	}

	public void signout() {
		if (Aircandi.getInstance().getUser() != null && !Aircandi.getInstance().getUser().isAnonymous()) {

			new AsyncTask() {

				@Override
				protected void onPreExecute() {
					showProgressDialog(true, mActivity.getString(R.string.progress_signing_out));
				}

				@Override
				protected Object doInBackground(Object... params) {

					ModelResult result = ProxiExplorer.getInstance().getEntityModel().signout();
					return result;
				}

				@Override
				protected void onPostExecute(Object response) {
					ModelResult result = (ModelResult) response;
					/*
					 * We continue on even if the service call failed.
					 */
					if (result.serviceResponse.responseCode == ResponseCode.Success) {
						Logger.i(this, "User signed out: " + Aircandi.getInstance().getUser().name + " (" + Aircandi.getInstance().getUser().id + ")");
					}
					else {
						Logger.w(this, "User signed out, service call failed: " + Aircandi.getInstance().getUser().id);
					}

					User user = (User) ProxibaseService.convertJsonToObjectInternalSmart(CandiConstants.USER_ANONYMOUS, ServiceDataType.User);
					Aircandi.getInstance().setUser(user);

					/* Clear the user and session that is tied into auto-signin */
					Aircandi.settingsEditor.putString(Preferences.PREF_USER, null);
					Aircandi.settingsEditor.putString(Preferences.PREF_USER_SESSION, null);
					Aircandi.settingsEditor.commit();

					/* Make sure onPrepareOptionsMenu gets called */
					if (mPageName.equals("CandiMap")) {
						((SherlockMapActivity) mActivity).invalidateOptionsMenu();
					}
					else {
						((SherlockActivity) mActivity).invalidateOptionsMenu();
					}

					ImageUtils.showToastNotification(mActivity.getString(R.string.toast_signed_out), Toast.LENGTH_SHORT);
					Tracker.trackEvent("User", "Signout", null, 0);

					showProgressDialog(false, null);
				}
			}.execute();
		}
	}

	public void signin(Integer messageResId) {
		IntentBuilder intentBuilder = new IntentBuilder(mActivity, SignInForm.class);
		if (messageResId != null) {
			intentBuilder.setMessage(mActivity.getString(messageResId));
		}
		Intent intent = intentBuilder.create();
		mActivity.startActivityForResult(intent, CandiConstants.ACTIVITY_SIGNIN);
		AnimUtils.doOverridePendingTransition(mActivity, TransitionType.CandiPageToForm);
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
		AnimUtils.doOverridePendingTransition(mActivity, TransitionType.CandiPageBack);
	}

	// --------------------------------------------------------------------------------------------
	// Menu routines
	// --------------------------------------------------------------------------------------------

	public void doCreateOptionsMenu(Menu menu) {

		if (mPageName.equals("CandiMap")) {
			SherlockMapActivity mapActivity = (SherlockMapActivity) mActivity;
			mapActivity.getSupportMenuInflater().inflate(mThemeTone.equals("light") ? R.menu.menu_primary_light : R.menu.menu_primary_dark, menu);
		}
		else if (mPageName.equals("SignInForm")) {
			SherlockActivity mapActivity = (SherlockActivity) mActivity;
			mapActivity.getSupportMenuInflater().inflate(R.menu.menu_signin, menu);
		}
		else {
			SherlockActivity activity = (SherlockActivity) mActivity;
			activity.getSupportMenuInflater().inflate(mThemeTone.equals("light") ? R.menu.menu_primary_light : R.menu.menu_primary_dark, menu);
		}

		/* Beacon indicator */
		MenuItem menuItem = menu.findItem(R.id.beacons);
		if (menuItem != null) {
			mBeaconIndicator = (TextView) menuItem.getActionView().findViewById(R.id.beacon_indicator);
			menuItem.getActionView().findViewById(R.id.beacon_frame).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					doBeaconIndicatorClick();
				}
			});

			updateBeaconIndicator(ProxiExplorer.getInstance().mWifiList);
			mEventScanReceived = new EventHandler() {

				@Override
				public void onEvent(Object data) {
					List<WifiScanResult> scanList = (List<WifiScanResult>) data;
					updateBeaconIndicator(scanList);
				}
			};

		}

		/* Refresh with action mode support */
		menuItem = menu.findItem(R.id.refresh);
		if (menuItem != null) {
			menuItem.getActionView().findViewById(R.id.refresh_frame).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (mPageName.equals("CandiRadar")) {
						((CandiRadar) mActivity).doRefresh(RefreshType.Standard);
					}
					else if (mPageName.equals("CandiList")) {
						((CandiList) mActivity).doRefresh();
					}
					else if (mPageName.equals("CandiForm")) {
						((CandiForm) mActivity).doRefresh();
					}
					else if (mPageName.equals("UserCandiList")) {
						((UserCandiList) mActivity).doRefresh();
					}
					else if (mPageName.equals("UserCandiForm")) {
						((UserCandiForm) mActivity).doRefresh();
					}
					else if (mPageName.equals("MapCandiList")) {
						((MapCandiList) mActivity).doRefresh();
					}
					else if (mPageName.equals("MapCandiForm")) {
						((MapCandiForm) mActivity).doRefresh();
					}
					else if (mPageName.equals("CandiMap")) {
						((CandiMap) mActivity).doRefresh();
					}
					else if (mPageName.equals("CommentList")) {
						((CommentList) mActivity).doRefresh();
					}
				}
			});
			menuItem.getActionView().findViewById(R.id.refresh_frame).setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View view) {
					if (mPageName.equals("CandiMap")) {
						SherlockMapActivity mapActivity = (SherlockMapActivity) mActivity;
						mapActivity.startActionMode(new ActionModeRefresh());
					}
					else {
						SherlockActivity activity = (SherlockActivity) mActivity;
						activity.startActionMode(new ActionModeRefresh());
					}
					return true;
				}
			});
		}
	}

	public void doPrepareOptionsMenu(Menu menu) {
		/* Hide the sign out option if we don't have a current session */
		if (!mPageName.equals("SignInForm")) {
			if (Aircandi.getInstance().getUser() != null && !Aircandi.getInstance().getUser().isAnonymous()) {
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
	}

	public void doOptionsItemSelected(MenuItem menuItem) {

		switch (menuItem.getItemId()) {
			case android.R.id.home:
				/*
				 * If this doesn't get handled directly by the activity then
				 * it falls through to here and we want to go to the top of the app.
				 */
				mActivity.onBackPressed();
				return;
			case R.id.settings:
				mActivity.startActivityForResult(new Intent(mActivity, Preferences.class), CandiConstants.ACTIVITY_PREFERENCES);
				AnimUtils.doOverridePendingTransition(mActivity, TransitionType.CandiPageToForm);
				return;
			case R.id.profile:
				doProfileClick();
				return;
			case R.id.signout:
				signout();
				return;
			case R.id.signin:
				signin(null);
				return;
			case R.id.signup:
				signup();
				return;
			case R.id.about:
				doInfoClick();
				return;
		}
	}

	public final class ActionModeRefresh implements ActionMode.Callback {

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			((SherlockActivity) mActivity).getSupportMenuInflater().inflate(
					mThemeTone.equals("light") ? R.menu.menu_context_refresh_light : R.menu.menu_context_refresh_dark, menu);
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			if (mPageName.equals("CandiRadar")) {
				if (item.getItemId() == R.id.refresh) {
					((CandiRadar) mActivity).doRefresh(RefreshType.Standard);
				}
				else if (item.getItemId() == R.id.refresh_all) {
					((CandiRadar) mActivity).doRefresh(RefreshType.FullBuild);
				}
			}
			else if (item.getItemId() == R.id.refresh || item.getItemId() == R.id.refresh_all) {
				if (mPageName.equals("CandiList")) {
					((CandiList) mActivity).doRefresh();
				}
				else if (mPageName.equals("CandiForm")) {
					((CandiForm) mActivity).doRefresh();
				}
				else if (mPageName.equals("UserCandiList")) {
					((UserCandiList) mActivity).doRefresh();
				}
				else if (mPageName.equals("UserCandiForm")) {
					((UserCandiForm) mActivity).doRefresh();
				}
				else if (mPageName.equals("MapCandiList")) {
					((MapCandiList) mActivity).doRefresh();
				}
				else if (mPageName.equals("MapCandiForm")) {
					((MapCandiForm) mActivity).doRefresh();
				}
				else if (mPageName.equals("CandiMap")) {
					((CandiMap) mActivity).doRefresh();
				}
				else if (mPageName.equals("CommentList")) {
					((CommentList) mActivity).doRefresh();
				}
			}
			mode.finish();
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {}
	}

	// --------------------------------------------------------------------------------------------
	// Tab routines
	// --------------------------------------------------------------------------------------------

	public void manageTabs() {
		Logger.v(this, "Building tabs: " + mPageName);
		if (mPageName.equals("CandiRadar")) {
			addTabsToActionBar(this, CandiConstants.TABS_PRIMARY_ID);
			setActiveTab(0);
		}
		else if (mPageName.equals("CandiList")) {
			addTabsToActionBar(this, CandiConstants.TABS_PRIMARY_ID);
			setActiveTab(0);
		}
		else if (mPageName.equals("UserCandiList")) {
			addTabsToActionBar(this, CandiConstants.TABS_PRIMARY_ID);
			setActiveTab(1);
		}
		else if (mPageName.equals("MapCandiList")) {
			addTabsToActionBar(this, CandiConstants.TABS_PRIMARY_ID);
			setActiveTab(2);
		}
		else if (mPageName.equals("CandiMap")) {
			addTabsToActionBar(this, CandiConstants.TABS_PRIMARY_ID);
			setActiveTab(2);
		}
		else if (mPageName.equals("CandiForm")) {
			addTabsToActionBar(this, CandiConstants.TABS_PRIMARY_ID);
			setActiveTab(0);
		}
		else if (mPageName.equals("UserCandiForm")) {
			addTabsToActionBar(this, CandiConstants.TABS_PRIMARY_ID);
			setActiveTab(1);
		}
		else if (mPageName.equals("MapCandiForm")) {
			addTabsToActionBar(this, CandiConstants.TABS_PRIMARY_ID);
			setActiveTab(2);
		}
		else if (mPageName.equals("CommentList")) {
			addTabsToActionBar(this, CandiConstants.TABS_PRIMARY_ID);
			if (Aircandi.getInstance().getCandiTask() == CandiTask.RadarCandi) {
				setActiveTab(0);
			}
			else if (Aircandi.getInstance().getCandiTask() == CandiTask.MyCandi) {
				setActiveTab(1);
			}
		}
		else if (mPageName.equals("ProfileForm")) {
			addTabsToActionBar(this, CandiConstants.TABS_PROFILE_FORM_ID);
		}
		else if (mPageName.equals("EntityForm")) {
			addTabsToActionBar(this, CandiConstants.TABS_ENTITY_FORM_ID);
		}
		else if (mPageName.equals("CandiPicker")) {
			/*
			 * We let candi picker handle tab changes because there
			 * is extra work to do.
			 */
			addTabsToActionBar((CandiPicker) mActivity, CandiConstants.TABS_CANDI_PICKER_ID);
		}		
	}

	public void addTabsToActionBar(ActionBar.TabListener tabListener, int tabsId)
	{
		mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		if (tabsId == CandiConstants.TABS_PRIMARY_ID) {

			ActionBar.Tab tab = mActionBar.newTab();
			tab.setText(R.string.radar_tab_radar);
			tab.setTag(R.string.radar_tab_radar);
			tab.setTabListener(tabListener);
			mActionBar.addTab(tab, false);

			tab = mActionBar.newTab();
			tab.setText(R.string.radar_tab_mycandi);
			tab.setTag(R.string.radar_tab_mycandi);
			tab.setTabListener(tabListener);
			mActionBar.addTab(tab, false);

			tab = mActionBar.newTab();
			tab.setText(R.string.radar_tab_map);
			tab.setTag(R.string.radar_tab_map);
			tab.setTabListener(tabListener);
			mActionBar.addTab(tab, false);
		}
		else if (tabsId == CandiConstants.TABS_ENTITY_FORM_ID) {

			ActionBar.Tab tab = mActionBar.newTab();
			tab.setText(R.string.form_tab_content);
			tab.setTag(R.string.form_tab_content);
			tab.setTabListener(tabListener);
			mActionBar.addTab(tab, false);

			tab = mActionBar.newTab();
			tab.setText(R.string.form_tab_settings);
			tab.setTag(R.string.form_tab_settings);
			tab.setTabListener(tabListener);
			mActionBar.addTab(tab, false);
		}
		else if (tabsId == CandiConstants.TABS_PROFILE_FORM_ID) {

			ActionBar.Tab tab = mActionBar.newTab();
			tab.setText(R.string.profile_tab_profile);
			tab.setTag(R.string.profile_tab_profile);
			tab.setTabListener(tabListener);
			mActionBar.addTab(tab, false);

			tab = mActionBar.newTab();
			tab.setText(R.string.profile_tab_account);
			tab.setTag(R.string.profile_tab_account);
			tab.setTabListener(tabListener);
			mActionBar.addTab(tab, false);
		}
		else if (tabsId == CandiConstants.TABS_CANDI_PICKER_ID) {

			ActionBar.Tab tab = mActionBar.newTab();
			tab.setText(R.string.candi_picker_tab_radar);
			tab.setTag(R.string.candi_picker_tab_radar);
			tab.setTabListener(tabListener);
			mActionBar.addTab(tab, false);

			tab = mActionBar.newTab();
			tab.setText(R.string.candi_picker_tab_mycandi);
			tab.setTag(R.string.candi_picker_tab_mycandi);
			tab.setTabListener(tabListener);
			mActionBar.addTab(tab, false);
		}
	}

	public void setActiveTab(int position) {
		Logger.v(this, "Setting active tab: " + String.valueOf(position));
		if ((mActionBar.getSelectedTab() == null || mActionBar.getSelectedTab().getPosition() != position)
				&& mActionBar.getTabCount() >= (position - 1)) {
			mActionBar.getTabAt(position).select();
		}
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		Logger.v(this, "onTabSelected: " + tab.getTag());
		if (tab.getTag().equals(R.string.radar_tab_radar)) {
			if (Aircandi.getInstance().getCandiTask() != CandiTask.RadarCandi) {
				Aircandi.getInstance().setCandiTask(CandiTask.RadarCandi);

				IntentBuilder intentBuilder = new IntentBuilder(mActivity, CandiRadar.class);
				intentBuilder.setNavigationTop(true);
				Intent intent = intentBuilder.create();
				/*
				 * Radar is the root task so the back button will always end here. From radar the back
				 * button kills the application and returns to the launcher (home).
				 * 
				 * The flags let us use existing instance of radar if its already around.
				 */
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
				mActivity.startActivity(intent);
				AnimUtils.doOverridePendingTransition(mActivity, TransitionType.CandiPageToCandiRadar);
			}
		}
		else if (tab.getTag().equals(R.string.radar_tab_mycandi)) {
			if (Aircandi.getInstance().getCandiTask() != CandiTask.MyCandi) {
				Aircandi.getInstance().setCandiTask(CandiTask.MyCandi);

				IntentBuilder intentBuilder = new IntentBuilder(mActivity, UserCandiList.class);
				intentBuilder.setNavigationTop(true)
						.setEntityTree(ProxiExplorer.EntityTree.User)
						.setCollectionId(ProxiConstants.ROOT_COLLECTION_ID);
				Intent intent = intentBuilder.create();
				/*
				 * These flags put this on the stack as a new task and hitting back will
				 * take the user back the top of the previous task.
				 */
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				mActivity.startActivity(intent);
				AnimUtils.doOverridePendingTransition(mActivity, TransitionType.CandiPageToMyCandi);
			}
		}
		else if (tab.getTag().equals(R.string.radar_tab_map)) {
			if (Aircandi.getInstance().getCandiTask() != CandiTask.Map) {
				Aircandi.getInstance().setCandiTask(CandiTask.Map);

				IntentBuilder intentBuilder = new IntentBuilder(mActivity, CandiMap.class);
				intentBuilder.setNavigationTop(true);
				Intent intent = intentBuilder.create();
				/*
				 * These flags put this on the stack as a new task and hitting back will
				 * take the user back the top of the previous task.
				 */
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				mActivity.startActivity(intent);
				AnimUtils.doOverridePendingTransition(mActivity, TransitionType.CandiPageToCandiMap);
			}
		}
		else {
			/* Currently handles tab switching in all forms with view flippers */
			if (mViewFlipper != null) {
				mViewFlipper.setDisplayedChild(tab.getPosition());
			}
		}
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {

	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
		Logger.v(this, "onTabReselected: " + tab.getText() + ", Top: " + String.valueOf(mNavigationTop));

		//		/*
		//		 * Reselecting a tab should take the user to the top of the
		//		 * hierarchy but not refresh data.
		//		 * 
		//		 * This seems to get fired without user interaction when first
		//		 * displayed in landscape mode.
		//		 */
		//		if (tab.getTag().equals(R.string.radar_tab_radar)) {
		//			if (!mNavigationTop) {
		//				IntentBuilder intentBuilder = new IntentBuilder(mActivity, CandiRadar.class);
		//				intentBuilder.setNavigationTop(true);
		//				Intent intent = intentBuilder.create();
		//				/* Flags let us use existing instance of radar if its already around */
		//				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		//				intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		//				mActivity.startActivity(intent);
		//				AnimUtils.doOverridePendingTransition(mActivity, TransitionType.CandiPageBack);
		//			}
		//		}
		//		else if (tab.getTag().equals(R.string.radar_tab_mycandi)) {
		//			/*
		//			 * This seems to get fired without user interaction when first
		//			 * displayed in landscape mode so we check to see if we are already at the top.
		//			 */
		//			
		//			/*
		//			 * Problem: this gets fired when we are on candi form on change to landscape.
		//			 * We then navigate back to candi list. When in candiform, how can we tell the 
		//			 * different between tabReselected by the user vs tabReselected by system because
		//			 * of change to landscape?
		//			 * 
		//			 * - onTabReselected fires on first orientation change but not subsequent ones.
		//			 * 
		//			 */
		//			if (!mNavigationTop) {
		//				IntentBuilder intentBuilder = new IntentBuilder(mActivity, CandiList.class);
		//				intentBuilder.setNavigationTop(true);
		//				intentBuilder.setCollectionType(ProxiExplorer.CollectionType.CandiByUser);
		//				intentBuilder.setCollectionId(ProxiConstants.ROOT_COLLECTION_ID);
		//				Intent intent = intentBuilder.create();
		//
		//				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		//				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		//				mActivity.startActivity(intent);
		//				AnimUtils.doOverridePendingTransition(mActivity, TransitionType.CandiPageBack);
		//			}
		//		}
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

	public void doDestroy() {}

	public void doPause() {
		synchronized (Events.EventBus.locationChanged) {
			Events.EventBus.locationChanged.remove(mEventLocationChanged);
		}
		synchronized (Events.EventBus.wifiScanReceived) {
			Events.EventBus.wifiScanReceived.remove(mEventScanReceived);
		}
	}

	public void doStop() {
		Tracker.activityStop(mActivity);
	}

	public void doStart() {
		Tracker.activityStart(mActivity);
	}

	public void doResume() {
		synchronized (Events.EventBus.locationChanged) {
			Events.EventBus.locationChanged.add(mEventLocationChanged);
		}
		synchronized (Events.EventBus.wifiScanReceived) {
			Events.EventBus.wifiScanReceived.add(mEventScanReceived);
		}
	}

	public void doSaveInstanceState(Bundle savedInstanceState) {
		/*
		 * This gets called from comment, profile and entity forms
		 */
		if (mActionBar != null && mActionBar.getTabCount() > 0) {
			savedInstanceState.putInt("tab_index", mActionBar.getSelectedTab() != null ? mActionBar.getSelectedTab().getPosition() : 0);
		}
	}

	public void doRestoreInstanceState(Bundle savedInstanceState) {
		/*
		 * This gets everytime Common is created and savedInstanceState bundle is
		 * passed to the constructor.
		 * 
		 * This gets called from comment, profile and entity forms
		 */
		if (savedInstanceState != null) {
			if (mActionBar != null && mActionBar.getTabCount() > 0) {
				setActiveTab(savedInstanceState.getInt("tab_index"));
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Inner classes
	// --------------------------------------------------------------------------------------------

	public CandiPatchModel getCandiPatchModel() {
		return mCandiPatchModel;
	}

	public void setCandiPatchModel(CandiPatchModel candiPatchModel) {
		mCandiPatchModel = candiPatchModel;
	}

	public CandiPatchPresenter getCandiPatchPresenter() {
		return mCandiPatchPresenter;
	}

	public void setCandiPatchPresenter(CandiPatchPresenter candiPatchPresenter) {
		mCandiPatchPresenter = candiPatchPresenter;
	}

	public ViewFlipper getViewFlipper() {
		return mViewFlipper;
	}

	public void setViewFlipper(ViewFlipper viewFlipper) {
		mViewFlipper = viewFlipper;
	}

	public File getTempFile(FormActivity formActivity, String tempFileName) {
		File path = new File(Environment.getExternalStorageDirectory(), formActivity.getPackageName());
		if (!path.exists()) {
			path.mkdir();
		}
		return new File(path, tempFileName);
	}

	public Dialog getProgressDialog() {

		if (mProgressDialog == null) {
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
		return mProgressDialog;
	}

	public enum ServiceOperation {
		Signin,
		Signout,
		Signup,
		PasswordChange,
		ProfileBrowse,
		ProfileUpdate,
		BeaconScan,
		CandiForm,
		CandiList,
		CandiBrowse,
		CandiSave,
		CandiMove,
		CandiDelete,
		ImageLoad,
		CommentBrowse,
		CommentSave,
		PictureBrowse,
		PictureSearch,
		MapBrowse,
		LinkLookup,
		Unknown,
		PickBookmark,
		PickCandi,
		CheckUpdate
	}

	public static enum ActionButtonSet {
		Radar, CandiForm, CandiList, CommentList
	}

}
