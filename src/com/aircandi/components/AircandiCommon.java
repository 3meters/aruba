package com.aircandi.components;

import java.util.Date;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.FragmentTransaction;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi;
import com.aircandi.CandiConstants;
import com.aircandi.ProxiConstants;
import com.aircandi.R;
import com.aircandi.ScanService;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProxiExplorer.ModelResult;
import com.aircandi.components.ProxiExplorer.WifiScanResult;
import com.aircandi.service.ProxibaseServiceException.ErrorCode;
import com.aircandi.service.ProxibaseServiceException.ErrorType;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Observation;
import com.aircandi.ui.CandiForm;
import com.aircandi.ui.CandiList;
import com.aircandi.ui.CandiRadar;
import com.aircandi.ui.CommentForm;
import com.aircandi.ui.CommentList;
import com.aircandi.ui.EntityForm;
import com.aircandi.ui.FeedbackForm;
import com.aircandi.ui.Preferences;
import com.aircandi.ui.SplashForm;
import com.aircandi.ui.builders.PictureSourcePicker;
import com.aircandi.ui.builders.TemplatePicker;
import com.aircandi.ui.user.CandiUser;
import com.aircandi.ui.user.ProfileForm;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;
import com.aircandi.utilities.DateUtils;
import com.aircandi.utilities.ImageUtils;
import com.squareup.otto.Subscribe;

public class AircandiCommon implements ActionBar.TabListener {

	private Context				mContext;
	private Activity			mActivity;

	static NotificationManager	mNotificationManager;

	/* Parameters */
	public CommandType			mCommandType;
	public String				mParentId;
	public String				mEntityId;
	public String				mEntityType;
	public String				mMessage;
	public Entity				mEntity;
	public String				mUserId;
	public String				mCollectionId;

	/* Theme */
	public String				mThemeTone;
	public Boolean				mIsDialog;

	/* UI */
	private TextView			mBeaconIndicator;
	private View				mAccuracyIndicator;
	private View				mRefreshImage;
	private View				mRefreshProgress;
	private MenuItem			mMenuItemRefresh;
	private MenuItem			mMenuItemBeacons;
	public Menu					mMenu;
	private ProgressDialog		mProgressDialog;
	public String				mPrefTheme;
	public ActionBar			mActionBar;
	private ViewFlipper			mViewFlipper;

	/* Other */
	private String				mPageName;
	private String				mDebugWifi;
	private String				mDebugLocation;
	private Integer				mBusyCount	= 0;

	public AircandiCommon(Context context, Bundle savedInstanceState) {
		mContext = context;
		mActivity = (Activity) context;
		mPageName = mActivity.getClass().getSimpleName();
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

		mNotificationManager = (NotificationManager) mContext.getSystemService(Service.NOTIFICATION_SERVICE);

		Logger.i(this, "Activity created: " + mPageName);
		Logger.d(this, "Started from radar flag: " + String.valueOf(Aircandi.getInstance().wasLaunchedNormally()));

		/* Stash the action bar */
		mActionBar = ((SherlockActivity) mActivity).getSupportActionBar();

		/* Fonts */
		Integer titleId = getActionBarTitleId();
		FontManager.getInstance().setTypefaceDefault((TextView) mActivity.findViewById(titleId));

		/* Theme info */
		TypedValue resourceName = new TypedValue();
		if (mActivity.getTheme().resolveAttribute(R.attr.themeTone, resourceName, true)) {
			mThemeTone = (String) resourceName.coerceToString();
		}

		/* Tabs: setup tabs if appropriate */
		manageTabs();

		/* Default sizing if this is a dialog */
		if (mIsDialog) {
			WindowManager.LayoutParams params = mActivity.getWindow().getAttributes();
			params.height = ImageUtils.getRawPixels(mActivity, 500);
			DisplayMetrics metrics = mActivity.getResources().getDisplayMetrics();
			if (metrics != null) {
				int displayWidth = metrics.widthPixels;
				int desiredWidth = ImageUtils.getRawPixels(mActivity, 350);
				params.width = Math.min(desiredWidth, displayWidth);
			}
			else {
				params.width = ImageUtils.getRawPixels(mActivity, 300);
			}

			mActivity.getWindow().setAttributes(params);
		}
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onWifiScanReceived(MonitoringWifiScanReceivedEvent event) {
		updateDevIndicator(event.wifiList, null);
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onWifiScanReceived(QueryWifiScanReceivedEvent event) {
		updateDevIndicator(event.wifiList, null);
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onLocationReceived(LocationReceivedEvent event) {
		updateDevIndicator(null, event.location);
	}

	public int getActionBarTitleId() {
		Integer actionBarTitleId = null;
		try {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
				actionBarTitleId = Class.forName("com.actionbarsherlock.R$id").getField("abs__action_bar_title").getInt(null);
			}
			else {
				// Use reflection to get the actionbar title TextView and set the custom font. May break in updates.
				actionBarTitleId = Class.forName("com.android.internal.R$id").getField("action_bar_title").getInt(null);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return actionBarTitleId;
	}

	public void unpackIntent() {

		Bundle extras = mActivity.getIntent().getExtras();
		if (extras != null) {

			mParentId = extras.getString(CandiConstants.EXTRA_PARENT_ENTITY_ID);
			mEntityType = extras.getString(CandiConstants.EXTRA_ENTITY_TYPE);
			mEntityId = extras.getString(CandiConstants.EXTRA_ENTITY_ID);
			mUserId = extras.getString(CandiConstants.EXTRA_USER_ID);
			mMessage = extras.getString(CandiConstants.EXTRA_MESSAGE);
			mCollectionId = extras.getString(CandiConstants.EXTRA_COLLECTION_ID);

			String commandType = extras.getString(CandiConstants.EXTRA_COMMAND_TYPE);
			if (commandType != null) {
				mCommandType = CommandType.valueOf(commandType);
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	private void doProfileClick() {
		IntentBuilder intentBuilder = new IntentBuilder(mContext, ProfileForm.class);
		intentBuilder.setCommandType(CommandType.View);
		Intent intent = intentBuilder.create();
		mActivity.startActivity(intent);
		AnimUtils.doOverridePendingTransition(mActivity, TransitionType.PageToForm);
	}

	public void doEditCandiClick() {
		IntentBuilder intentBuilder = new IntentBuilder(mActivity, EntityForm.class)
				.setCommandType(CommandType.Edit)
				.setEntityId(((CandiForm) mActivity).getEntity().id)
				.setParentEntityId(((CandiForm) mActivity).getEntity().parentId)
				.setEntityType(((CandiForm) mActivity).getEntity().type);
		Intent intent = intentBuilder.create();
		mActivity.startActivityForResult(intent, CandiConstants.ACTIVITY_ENTITY_EDIT);
		AnimUtils.doOverridePendingTransition(mActivity, TransitionType.PageToForm);
	}

	private void doFeedbackClick() {
		IntentBuilder intentBuilder = new IntentBuilder(mActivity, FeedbackForm.class);
		Intent intent = intentBuilder.create();
		mActivity.startActivity(intent);
		AnimUtils.doOverridePendingTransition(mActivity, TransitionType.PageToForm);
	}

	private void doInfoClick() {
		String title = mActivity.getString(R.string.alert_about_title);
		String message = mActivity.getString(R.string.alert_about_message) + " "
				+ Aircandi.getVersionName(mContext, CandiRadar.class) + "\n"
				+ mActivity.getString(R.string.dialog_info);
		AircandiCommon.showAlertDialog(R.drawable.ic_app
				, title
				, message
				, null
				, mActivity, android.R.string.ok, null, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {}
				}, null);
		Tracker.sendEvent("ui_action", "open_dialog", "about", 0);

	}

	public void doAttachedToWindow() {
		Window window = mActivity.getWindow();
		window.setFormat(PixelFormat.RGBA_8888);
	}

	private void doBeaconIndicatorClick() {
		if (mBeaconIndicator != null) {
			String beaconMessage = "";
			synchronized (ProxiExplorer.getInstance().mWifiList) {
				if (Aircandi.getInstance().getUser() != null
						&& Aircandi.settings.getBoolean(Preferences.PREF_ENABLE_DEV, true)
						&& Aircandi.getInstance().getUser().isDeveloper != null
						&& Aircandi.getInstance().getUser().isDeveloper) {
					if (Aircandi.wifiCount > 0) {
						for (WifiScanResult wifi : ProxiExplorer.getInstance().mWifiList) {
							if (!wifi.SSID.equals("candi_feed")) {
								beaconMessage += wifi.SSID + ": (" + String.valueOf(wifi.level) + ") " + wifi.BSSID + "\n";
							}
						}
						beaconMessage += "\n";
						beaconMessage += "Wifi fix: " + DateUtils.intervalSince(ProxiExplorer.getInstance().mLastWifiUpdate, DateUtils.nowDate());
					}

					Observation observation = LocationManager.getInstance().getObservation();
					if (observation != null) {
						Date fixDate = new Date(observation.time.longValue());
						beaconMessage += "\nLocation fix: " + DateUtils.intervalSince(fixDate, DateUtils.nowDate());
						beaconMessage += "\nLocation accuracy: " + String.valueOf(observation.accuracy);
						beaconMessage += "\nLocation provider: " + observation.provider;
					}
					else {
						beaconMessage += "\nLocation fix: none";
					}
				}
				else {
					return;
				}
			}
			AircandiCommon.showAlertDialog(R.drawable.ic_app
					, mActivity.getString(R.string.alert_beacons_title)
					, beaconMessage
					, null
					, mActivity, android.R.string.ok, null, new
					DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {}
					}, null);
		}
	}

	private void doRefreshClick() {
		/* Show busy indicator */
		startActionbarBusyIndicator();

		if (mPageName.equals("CandiRadar")) {
			((CandiRadar) mActivity).doRefresh();
		}
		else if (mPageName.equals("CandiList")) {
			((CandiList) mActivity).doRefresh();
		}
		else if (mPageName.equals("CandiForm")) {
			((CandiForm) mActivity).doRefresh();
		}
		else if (mPageName.equals("CandiUser")) {
			((CandiUser) mActivity).doRefresh();
		}
		else if (mPageName.equals("CommentList")) {
			((CommentList) mActivity).doRefresh();
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
		Intent intent = new Intent(mActivity, TemplatePicker.class);
		intent.putExtra(CandiConstants.EXTRA_ENTITY_IS_ROOT, isRoot);
		mActivity.startActivityForResult(intent, CandiConstants.ACTIVITY_TEMPLATE_PICK);
		AnimUtils.doOverridePendingTransition(mActivity, TransitionType.PageToForm);
	}

	public void showPictureSourcePicker(String entityId) {
		Intent intent = new Intent(mActivity, PictureSourcePicker.class);
		if (entityId != null) {
			intent.putExtra(CandiConstants.EXTRA_ENTITY_ID, entityId);
		}
		mActivity.startActivityForResult(intent, CandiConstants.ACTIVITY_PICTURE_SOURCE_PICK);
		AnimUtils.doOverridePendingTransition(mActivity, TransitionType.PageToForm);
	}

	public void showCandiFormForEntity(Entity entity, Class<?> clazz) {

		IntentBuilder intentBuilder = new IntentBuilder(mActivity, clazz);
		intentBuilder.setCommandType(CommandType.View)
				.setEntityId(entity.id)
				.setParentEntityId(entity.parentId)
				.setEntityType(entity.type);

		if (entity.parentId != null) {
			intentBuilder.setCollectionId(entity.getParent().id);
		}

		Intent intent = intentBuilder.create();

		mActivity.startActivity(intent);
		AnimUtils.doOverridePendingTransition(mActivity, TransitionType.PageToPage);
	}

	public void updateAccuracyIndicator(final Location location) {

		mActivity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (location == null) {
					mAccuracyIndicator.setBackgroundResource(R.drawable.accuracy_indicator_none);
					Logger.v(this, "Location accuracy: none");
				}
				else if (location.hasAccuracy()) {

					int sizeDip = 40;

					if (location.getAccuracy() <= 100) {
						sizeDip = 25;
					}
					if (location.getAccuracy() <= 50) {
						sizeDip = 12;
					}
					if (location.getAccuracy() <= 30) {
						sizeDip = 8;
					}

					Logger.v(this, "Location accuracy: >>> " + String.valueOf(sizeDip));
					int sizePixels = ImageUtils.getRawPixels(mActivity, sizeDip);
					FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(sizePixels, sizePixels, Gravity.CENTER);
					mAccuracyIndicator.setLayoutParams(layoutParams);
					mAccuracyIndicator.setBackgroundResource(R.drawable.accuracy_indicator);
				}
			}
		});
	}

	private void updateDevIndicator(final List<WifiScanResult> scanList, Location location) {

		if (mBeaconIndicator == null) return;

		if (scanList != null) {

			synchronized (scanList) {
				/*
				 * In case we get called from a background thread.
				 */
				mActivity.runOnUiThread(new Runnable() {

					@Override
					public void run() {

						WifiScanResult wifiStrongest = null;
						int wifiCount = 0;
						for (WifiScanResult wifi : scanList) {
							wifiCount++;
							if (wifiStrongest == null) {
								wifiStrongest = wifi;
							}
							else if (wifi.level > wifiStrongest.level) {
								wifiStrongest = wifi;
							}
						}

						Aircandi.wifiCount = wifiCount;
						mDebugWifi = String.valueOf(wifiCount);

						//						Drawable drawable = mActivity.getResources().getDrawable(R.drawable.beacon_indicator_stop);
						//						if (wifiCount > 0) {
						//							//						if (mMenuItemBeacons != null) {
						//							//							mMenuItemBeacons.setVisible(true);
						//							//						}
						//							drawable = mActivity.getResources().getDrawable(R.drawable.beacon_indicator_caution);
						//						}
						//						else {
						//							//						if (mMenuItemBeacons != null) {
						//							//							mMenuItemBeacons.setVisible(false);
						//							//						}
						//						}
						//
						//						if (wifiStrongest != null && wifiStrongest.level > CandiConstants.RADAR_BEACON_INDICATOR_CAUTION) {
						//							drawable = mActivity.getResources().getDrawable(R.drawable.beacon_indicator_go);
						//						}
						//						mBeaconIndicator.setBackgroundDrawable(drawable);

					}
				});

			}
		}

		if (location != null) {
			String debugLocation = location.getProvider().substring(0, 1).toUpperCase();
			if (location.hasAccuracy()) {
				debugLocation += String.valueOf((int) location.getAccuracy());
			}
			else {
				debugLocation += "--";
			}
			mDebugLocation = debugLocation;
		}

		mBeaconIndicator.setText(mDebugWifi + ":" + mDebugLocation);
	}

	public void handleServiceError(ServiceResponse serviceResponse, ServiceOperation serviceOperation) {
		handleServiceError(serviceResponse, serviceOperation, mActivity);
	}

	public void handleServiceError(ServiceResponse serviceResponse, ServiceOperation serviceOperation, Context context) {

		ErrorType errorType = serviceResponse.exception.getErrorType();
		ErrorCode errorCode = serviceResponse.exception.getErrorCode();
		String errorMessage = serviceResponse.exception.getMessage();

		/* We always make sure the progress indicator has been stopped */
		mActivity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				hideBusy(true);
			}
		});

		/*
		 * Client errors occur when we are unable to get a response from a service, or when the client is
		 * unable to understand a response from a service. This includes protocol, network and timeout errors.
		 */
		if (errorType == ErrorType.Client && errorCode == ErrorCode.ConnectionException) {
			/*
			 * We don't have a network connection.
			 */
			Intent intent = new Intent(mContext, CandiRadar.class);
			showNotification(mActivity.getString(R.string.error_connection_title), mActivity.getString(R.string.error_connection_notification), context,
					intent, CandiConstants.NOTIFICATION_NETWORK);
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

	public static AlertDialog showAlertDialog(Integer iconResource
			, String titleText
			, String message
			, View customView
			, Context context
			, Integer okButtonId
			, Integer cancelButtonId
			, OnClickListener listenerClick
			, OnCancelListener listenerCancel) {

		AlertDialog.Builder builder = new AlertDialog.Builder(context);

		if (iconResource != null) {
			builder.setIcon(iconResource);
		}

		if (titleText != null) {
			builder.setTitle(titleText);
		}

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

		AlertDialog alert = builder.create();
		alert.show();

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
		if (!mActivity.isFinishing()) {
			mActivity.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					AircandiCommon.showAlertDialog(R.drawable.ic_app
							, titleText
							, message
							, null
							, mContext
							, android.R.string.ok
							, null
							, null
							, null);
				}
			});
		}
	}

	public void setTheme(Integer themeResId, Boolean isDialog) {
		mPrefTheme = Aircandi.settings.getString(Preferences.PREF_THEME, CandiConstants.THEME_DEFAULT);
		mIsDialog = isDialog;
		/*
		 * ActionBarSherlock takes over the title area if version < 4.0 (Ice Cream Sandwich).
		 */
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			mActivity.requestWindowFeature(Window.FEATURE_NO_TITLE);
		}
		/*
		 * Need to use application context so our app level themes and attributes are available to actionbarsherlock
		 */
		if (themeResId == null) {
			themeResId = mContext.getApplicationContext().getResources().getIdentifier(mPrefTheme, "style", mContext.getPackageName());
			if (isDialog) {
				themeResId = R.style.aircandi_theme_dialog_dark;
				if (mPrefTheme.equals("aircandi_theme_snow")) {
					themeResId = R.style.aircandi_theme_dialog_light;
				}
			}
		}

		((Activity) mContext).setTheme(themeResId);
	}

	private void signout() {
		mActivity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				new AsyncTask() {

					@Override
					protected void onPreExecute() {
						showBusy(R.string.progress_signing_out);
					}

					@Override
					protected Object doInBackground(Object... params) {
						Thread.currentThread().setName("SignOut");
						ModelResult result = ProxiExplorer.getInstance().getEntityModel().signout();
						return result;
					}

					@SuppressLint("NewApi")
					@Override
					protected void onPostExecute(Object response) {
						ModelResult result = (ModelResult) response;
						/* We continue on even if the service call failed. */
						if (result.serviceResponse.responseCode == ResponseCode.Success) {
							Logger.i(this, "User signed out: " + Aircandi.getInstance().getUser().name + " (" + Aircandi.getInstance().getUser().id + ")");
						}
						else {
							Logger.w(this, "User signed out, service call failed: " + Aircandi.getInstance().getUser().id);
						}

						/* Clear the user and session that is tied into auto-signin */
						Aircandi.settingsEditor.putString(Preferences.SETTING_USER, null);
						Aircandi.settingsEditor.putString(Preferences.SETTING_USER_SESSION, null);
						Aircandi.settingsEditor.commit();

						/* Make sure onPrepareOptionsMenu gets called */
						((SherlockActivity) mActivity).invalidateOptionsMenu();

						/* Notify interested parties */
						ImageUtils.showToastNotification(mActivity.getString(R.string.toast_signed_out), Toast.LENGTH_SHORT);
						hideBusy(false);
						Intent intent = new Intent(mActivity, SplashForm.class);
						mActivity.startActivity(intent);
						mActivity.finish();
						AnimUtils.doOverridePendingTransition(mActivity, TransitionType.FormToPage);
					}
				}.execute();

			}
		});
	}

	@SuppressWarnings("ucd")
	public void startScanService(int scanInterval) {

		/* Start first scan right away */
		Logger.d(this, "Starting wifi scan service");
		Intent scanIntent = new Intent(Aircandi.applicationContext, ScanService.class);
		mActivity.startService(scanIntent);

		/* Setup a scanning schedule */
		if (scanInterval > 0) {
			AlarmManager alarmManager = (AlarmManager) mActivity.getSystemService(Service.ALARM_SERVICE);
			PendingIntent pendingIntent = PendingIntent.getService(Aircandi.applicationContext, 0, scanIntent, 0);
			alarmManager.cancel(pendingIntent);
			alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP
					, SystemClock.elapsedRealtime() + scanInterval
					, scanInterval, pendingIntent);
		}
	}

	@SuppressWarnings("ucd")
	public void stopScanService() {
		AlarmManager alarmManager = (AlarmManager) mActivity.getSystemService(Service.ALARM_SERVICE);
		Intent scanIntent = new Intent(Aircandi.applicationContext, ScanService.class);
		PendingIntent pendingIntent = PendingIntent.getService(Aircandi.applicationContext, 0, scanIntent, 0);
		alarmManager.cancel(pendingIntent);
		Logger.d(this, "Stopped wifi scan service");
	}

	// --------------------------------------------------------------------------------------------
	// UI progress and notifications
	// --------------------------------------------------------------------------------------------

	public void showBusy() {

		showBusy(null);
	}

	public void showBusy(Integer messageResId) {

		synchronized (mBusyCount) {
			mBusyCount++;
			Logger.v(this, "Busy count: " + String.valueOf(mBusyCount));
		}

		if (mBusyCount == 1) {
			startActionbarBusyIndicator();
		}

		if (messageResId != null) {
			ProgressDialog progressDialog = getProgressDialog();
			progressDialog.setMessage(mActivity.getString(messageResId));
			if (!progressDialog.isShowing()) {
				progressDialog.setCancelable(false);
				progressDialog.show();
				if (Aircandi.displayMetrics != null) {
					progressDialog.getWindow().setLayout((int) (Aircandi.displayMetrics.widthPixels * 0.7), WindowManager.LayoutParams.WRAP_CONTENT);
				}
			}
		}
	}

	public void hideBusy(Boolean force) {
		ProgressDialog progressDialog = getProgressDialog();
		if (progressDialog.isShowing() && progressDialog.getWindow().getWindowManager() != null) {
			progressDialog.dismiss();
		}

		synchronized (mBusyCount) {
			mBusyCount--;
			Logger.v(this, "Busy count: " + String.valueOf(mBusyCount));
		}

		if (mBusyCount == 0 || force) {
			stopActionbarBusyIndicator();
			stopBusyIndicator();
		}
	}

	private void startActionbarBusyIndicator() {
		if (mRefreshImage != null) {
			mRefreshImage.setVisibility(View.GONE);
			mRefreshProgress.setVisibility(View.VISIBLE);
		}
	}

	private void stopActionbarBusyIndicator() {
		if (mRefreshImage != null) {
			mRefreshProgress.setVisibility(View.GONE);
			mRefreshImage.setVisibility(View.VISIBLE);
		}
	}

	@SuppressWarnings("unused")
	private void startBusyIndicator() {
		View progress = (View) mActivity.findViewById(R.id.progress);
		if (progress != null) {
			progress.setVisibility(View.VISIBLE);
		}
	}

	private void stopBusyIndicator() {
		View progress = (View) mActivity.findViewById(R.id.progress);
		if (progress != null) {
			progress.setVisibility(View.GONE);
		}
	}

	public void showNotification(String title, String message, Context context, Intent intent, int notificationType) {
		@SuppressWarnings("deprecation")
		Notification note = new Notification(R.drawable.ic_app_status_bw
				, title
				, System.currentTimeMillis());

		RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.custom_notification);
		contentView.setImageViewResource(R.id.image, R.drawable.ic_app);
		contentView.setTextViewText(R.id.title, title);
		contentView.setTextViewText(R.id.text, message);
		note.contentView = contentView;

		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
		note.contentIntent = pendingIntent;

		mNotificationManager.notify(notificationType, note);
	}

	// --------------------------------------------------------------------------------------------
	// Menu routines
	// --------------------------------------------------------------------------------------------

	public void doCreateOptionsMenu(Menu menu) {
		/*
		 * Android 2.3 or lower: called when user hits the menu button for the first time.
		 * Android 3.0 or higher: called when activity is first started.
		 * 
		 * Behavior might be modified because we are using ABS.
		 */
		SherlockActivity activity = (SherlockActivity) mActivity;
		if (mPageName.equals("CandiUser")) {
			activity.getSupportMenuInflater().inflate(R.menu.menu_user, menu);
		}
		else if (mPageName.equals("CommentList")) {
			activity.getSupportMenuInflater().inflate(R.menu.menu_comment, menu);
		}
		else if (mPageName.equals("SourcesBuilder")) {
			activity.getSupportMenuInflater().inflate(R.menu.menu_sources_builder, menu);
		}
		else {
			activity.getSupportMenuInflater().inflate(R.menu.menu_primary, menu);
		}

		/* Hide add comment menu item if not in commentlist */
		MenuItem menuItem = menu.findItem(R.id.add_comment);
		if (menuItem != null && !mPageName.equals("CommentList")) {
			if (mEntity != null && mEntity.locked) {
				menuItem.setVisible(false);
			}
		}

		/* Hide add custom place menuitem if this is not the CandiRadar activity */
		menuItem = menu.findItem(R.id.add_custom_place);
		if (menuItem != null && !mPageName.equals("CandiRadar")) {
			menuItem.setVisible(false);
		}

		/* Show update menuitem if one is needed */
		menuItem = menu.findItem(R.id.update);
		if (menuItem != null && !mPageName.equals("SignInForm")) {
			if (!Aircandi.applicationUpdateNeeded) {
				menuItem.setVisible(false);
			}
		}

		/* Beacon indicator */
		mMenuItemBeacons = menu.findItem(R.id.beacons);
		if (mMenuItemBeacons != null) {

			/* Only show beacon indicator if user is a developer */
			if (!Aircandi.settings.getBoolean(Preferences.PREF_ENABLE_DEV, true)
					|| Aircandi.getInstance().getUser() == null
					|| Aircandi.getInstance().getUser().isDeveloper == null
					|| !Aircandi.getInstance().getUser().isDeveloper) {
				mMenuItemBeacons.setVisible(false);
			}
			else {
				mBeaconIndicator = (TextView) mMenuItemBeacons.getActionView().findViewById(R.id.beacon_indicator);
				mMenuItemBeacons.getActionView().findViewById(R.id.beacon_frame).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						doBeaconIndicatorClick();
					}
				});
				updateDevIndicator(ProxiExplorer.getInstance().mWifiList, LocationManager.getInstance().getLocation());
			}
		}

		/* Cache refresh menu item for later ui updates */
		mMenuItemRefresh = menu.findItem(R.id.refresh);
		if (mMenuItemRefresh != null) {

			mRefreshImage = mMenuItemRefresh.getActionView().findViewById(R.id.refresh_image);
			mRefreshProgress = mMenuItemRefresh.getActionView().findViewById(R.id.refresh_progress);
			mAccuracyIndicator = mMenuItemRefresh.getActionView().findViewById(R.id.accuracy_indicator);
			mMenuItemRefresh.getActionView().findViewById(R.id.refresh_frame).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					doRefreshClick();
				}
			});

			updateAccuracyIndicator(null);
		}

		mMenu = menu;
	}

	public void doPrepareOptionsMenu(Menu menu) {
		/*
		 * Android 2.3 or lower: called every time the user hits the menu button.
		 * Android 3.0 or higher: called when invalidateOptionsMenu is called.
		 * 
		 * Behavior might be modified because we are using ABS.
		 */
	}

	public void doOptionsItemSelected(MenuItem menuItem) {

		IntentBuilder intentBuilder = null;
		Intent intent = null;

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
				AnimUtils.doOverridePendingTransition(mActivity, TransitionType.PageToForm);
				return;
			case R.id.signout:
				Tracker.sendEvent("ui_action", "signout_user", null, 0);
				signout();
				return;
			case R.id.edit_user:
				doProfileClick();
				return;
			case R.id.update:
				Logger.d(this, "Update menu item: navigating to install page");
				intent = new Intent(android.content.Intent.ACTION_VIEW);
				intent.setData(Uri.parse(Aircandi.applicationUpdateUri));
				mActivity.startActivity(intent);
				AnimUtils.doOverridePendingTransition(mActivity, TransitionType.PageToSource);
				return;
			case R.id.feedback:
				doFeedbackClick();
				return;
			case R.id.profile:
				intentBuilder = new IntentBuilder(mActivity, CandiUser.class)
						.setUserId(Aircandi.getInstance().getUser().id);
				intent = intentBuilder.create();
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
				mActivity.startActivity(intent);
				AnimUtils.doOverridePendingTransition(mActivity, TransitionType.RadarToPage);
				return;
			case R.id.add_comment:
				/*
				 * We assume the new comment button wouldn't be visible if the
				 * entity is locked.
				 */
				intentBuilder = new IntentBuilder(mActivity, CommentForm.class);
				intentBuilder.setEntityId(null);
				intentBuilder.setParentEntityId(mEntityId);
				mActivity.startActivityForResult(intentBuilder.create(), CandiConstants.ACTIVITY_COMMENT);
				AnimUtils.doOverridePendingTransition(mActivity, TransitionType.PageToForm);
				return;
			case R.id.add_custom_place:
				if (Aircandi.getInstance().getUser() != null) {
					intentBuilder = new IntentBuilder(mActivity, EntityForm.class)
							.setCommandType(CommandType.New)
							.setEntityId(null)
							.setEntityType(CandiConstants.TYPE_CANDI_PLACE);

					mActivity.startActivity(intentBuilder.create());
					AnimUtils.doOverridePendingTransition(mActivity, TransitionType.PageToForm);
				}
				return;
			case R.id.about:
				doInfoClick();
				return;
		}
	}

	// --------------------------------------------------------------------------------------------
	// Tab routines
	// --------------------------------------------------------------------------------------------

	private void manageTabs() {
		Logger.v(this, "Building tabs: " + mPageName);
		if (mPageName.equals("ProfileForm")) {
			addTabsToActionBar(this, CandiConstants.TABS_PROFILE_FORM_ID);
		}
		else if (mPageName.equals("EntityForm")) {
			if (mEntityId != null) {
				mEntity = ProxiExplorer.getInstance().getEntityModel().getCacheEntity(mEntityId);
				if (mEntity != null) {
					if (mEntity.ownerId != null && (mEntity.ownerId.equals(Aircandi.getInstance().getUser().id))) {
						addTabsToActionBar(this, CandiConstants.TABS_ENTITY_FORM_ID);
					}
				}
			}
		}
	}

	private void addTabsToActionBar(ActionBar.TabListener tabListener, int tabsId)
	{
		mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		if (tabsId == CandiConstants.TABS_ENTITY_FORM_ID) {

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
	}

	public void setActiveTab(int position) {
		if (mActionBar.getTabCount() == 0) {
			return;
		}
		Logger.v(this, "Setting active tab: " + String.valueOf(position));
		if ((mActionBar.getSelectedTab() == null || mActionBar.getSelectedTab().getPosition() != position)
				&& mActionBar.getTabCount() >= (position - 1)) {
			mActionBar.getTabAt(position).select();
		}
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		Logger.v(this, "onTabSelected: " + tab.getTag());
		/* Currently handles tab switching in all forms with view flippers */
		if (mViewFlipper != null) {
			mViewFlipper.setDisplayedChild(tab.getPosition());
		}
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {}

	// --------------------------------------------------------------------------------------------
	// Utility routines
	// --------------------------------------------------------------------------------------------

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
		BusProvider.getInstance().unregister(this);
	}

	public void doStop() {
		Tracker.activityStop(mActivity);
	}

	public void doStart() {
		Tracker.activityStart(mActivity);
	}

	public void doResume() {
		BusProvider.getInstance().register(this);
	}

	public void doSaveInstanceState(Bundle savedInstanceState) {
		/*
		 * This gets called from comment, profile and entity forms
		 */
		if (mActionBar != null && mActionBar.getTabCount() > 0) {
			savedInstanceState.putInt("tab_index", mActionBar.getSelectedTab() != null ? mActionBar.getSelectedTab().getPosition() : 0);
		}
	}

	private void doRestoreInstanceState(Bundle savedInstanceState) {
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

	public ViewFlipper getViewFlipper() {
		return mViewFlipper;
	}

	public void setViewFlipper(ViewFlipper viewFlipper) {
		mViewFlipper = viewFlipper;
	}

	public ProgressDialog getProgressDialog() {

		if (mProgressDialog == null) {
			/* Dialogs */
			mProgressDialog = new ProgressDialog(mContext);
			mProgressDialog.setIndeterminate(true);
			mProgressDialog.setCancelable(true);
			mProgressDialog.setCanceledOnTouchOutside(false);
			mProgressDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
			mProgressDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
					WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
		}

		return mProgressDialog;
	}

	public enum ServiceOperation {
		Signin,
		Signup,
		PasswordChange,
		ProfileBrowse,
		ProfileUpdate,
		CandiForm,
		CandiList,
		CandiSave,
		CandiDelete,
		CommentBrowse,
		CommentSave,
		FeedbackSave,
		PictureSearch,
		PickBookmark,
		CheckUpdate,
		Tuning,
		CandiUser
	}

}
