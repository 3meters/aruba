package com.aircandi.components;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.conn.ConnectTimeoutException;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.FragmentTransaction;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.BadTokenException;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.ProxiConstants;
import com.aircandi.ScanService;
import com.aircandi.beta.BuildConfig;
import com.aircandi.beta.R;
import com.aircandi.components.NetworkManager.ResponseCode;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.ProximityManager.ModelResult;
import com.aircandi.components.ProximityManager.WifiScanResult;
import com.aircandi.service.HttpServiceException;
import com.aircandi.service.HttpServiceException.ErrorType;
import com.aircandi.service.WalledGardenException;
import com.aircandi.service.objects.AirLocation;
import com.aircandi.service.objects.AirNotification;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.ServiceEntry;
import com.aircandi.service.objects.Shortcut;
import com.aircandi.service.objects.User;
import com.aircandi.ui.EntityList;
import com.aircandi.ui.EntityList.ListMode;
import com.aircandi.ui.HelpForm;
import com.aircandi.ui.Preferences;
import com.aircandi.ui.RadarForm;
import com.aircandi.ui.SplashForm;
import com.aircandi.ui.WatchForm;
import com.aircandi.ui.base.BaseEntityView;
import com.aircandi.ui.edit.FeedbackEdit;
import com.aircandi.ui.helpers.TemplatePicker;
import com.aircandi.ui.user.UserEdit;
import com.aircandi.ui.user.UserForm;
import com.aircandi.utilities.AnimUtils;
import com.aircandi.utilities.AnimUtils.TransitionType;
import com.aircandi.utilities.DateUtils;
import com.aircandi.utilities.ImageUtils;
import com.google.android.gcm.GCMRegistrar;
import com.squareup.otto.Subscribe;

public class AircandiCommon implements ActionBar.TabListener {

	private final Context		mContext;
	private final Activity		mActivity;

	/* Parameters */
	public String				mParentId;
	public String				mEntityId;
	public String				mEntitySchema;
	public String				mMessage;
	public Entity				mEntity;
	public String				mUserId;

	/* Theme */
	public String				mPrefTheme;
	public String				mThemeTone;
	private Boolean				mIsDialog;

	/* UI */
	private TextView			mBeaconIndicator;
	private View				mAccuracyIndicator;
	private View				mRefreshImage;
	private View				mRefreshProgress;
	private MenuItem			mMenuItemRefresh;
	public MenuItem				mMenuItemEdit;
	public MenuItem				mMenuItemDelete;
	public MenuItem				mMenuItemAdd;
	private MenuItem			mMenuItemBeacons;
	public Menu					mMenu;
	private ProgressDialog		mProgressDialog;
	public ActionBar			mActionBar;
	private ViewFlipper			mViewFlipper;

	/* Other */
	private final String		mPageName;
	private String				mDebugWifi;
	private String				mDebugLocation	= "--";
	private final AtomicInteger	mBusyCount		= new AtomicInteger(0);

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

		/* Make sure we have successfully registered this device with aircandi service */
		com.aircandi.components.NotificationManager.getInstance().registerDeviceWithAircandi();

		Logger.i(this, "Activity created: " + mPageName);

		/* Stash the action bar */
		mActionBar = ((SherlockActivity) mActivity).getSupportActionBar();

		/* Fonts */
		final Integer titleId = getActionBarTitleId();
		FontManager.getInstance().setTypefaceDefault((TextView) mActivity.findViewById(titleId));

		/* Theme info */
		final TypedValue resourceName = new TypedValue();
		if (mActivity.getTheme().resolveAttribute(R.attr.themeTone, resourceName, true)) {
			mThemeTone = (String) resourceName.coerceToString();
		}

		/* Tabs: setup tabs if appropriate */
		manageTabs();

		/* Default sizing if this is a dialog */
		if (mIsDialog) {
			setDialogSize(mActivity.getResources().getConfiguration());
		}
	}

	public void unpackIntent() {

		final Bundle extras = mActivity.getIntent().getExtras();
		if (extras != null) {

			mParentId = extras.getString(Constants.EXTRA_ENTITY_PARENT_ID);
			mEntitySchema = extras.getString(Constants.EXTRA_ENTITY_SCHEMA);
			mEntityId = extras.getString(Constants.EXTRA_ENTITY_ID);
			mUserId = extras.getString(Constants.EXTRA_USER_ID);
			mMessage = extras.getString(Constants.EXTRA_MESSAGE);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Event routines
	// --------------------------------------------------------------------------------------------

	@Subscribe
	@SuppressWarnings("ucd")
	public void onWifiScanReceived(MonitoringWifiScanReceivedEvent event) {
		updateDevIndicator(event.wifiList, null);
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onWifiQueryReceived(QueryWifiScanReceivedEvent event) {
		updateDevIndicator(event.wifiList, null);
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onLocationReceived(LocationReceivedEvent event) {
		//updateDevIndicator(null, event.location);
	}

	@Subscribe
	@SuppressWarnings("ucd")
	public void onLocationLocked(LocationLockedEvent event) {
		updateDevIndicator(null, event.location);
	}

	public void doEditUserClick() {
		final IntentBuilder intentBuilder = new IntentBuilder(mActivity, UserEdit.class);
		final Intent intent = intentBuilder.create();
		mActivity.startActivity(intent);
		AnimUtils.doOverridePendingTransition(mActivity, TransitionType.PageToForm);
	}

	private void doFeedbackClick() {
		final IntentBuilder intentBuilder = new IntentBuilder(mActivity, FeedbackEdit.class);
		final Intent intent = intentBuilder.create();
		mActivity.startActivity(intent);
		AnimUtils.doOverridePendingTransition(mActivity, TransitionType.PageToForm);
	}

	public void doAttachedToWindow() {
		final Window window = mActivity.getWindow();
		window.setFormat(PixelFormat.RGBA_8888);
	}

	private void doBeaconIndicatorClick() {
		if (mBeaconIndicator != null) {
			final StringBuilder beaconMessage = new StringBuilder(500);
			List<WifiScanResult> wifiList = ProximityManager.getInstance().getWifiList();
			synchronized (wifiList) {
				if (Aircandi.getInstance().getUser() != null
						&& Aircandi.settings.getBoolean(Constants.PREF_ENABLE_DEV, Constants.PREF_ENABLE_DEV_DEFAULT)
						&& Aircandi.getInstance().getUser().developer != null
						&& Aircandi.getInstance().getUser().developer) {
					if (Aircandi.wifiCount > 0) {
						for (WifiScanResult wifi : wifiList) {
							if (!wifi.SSID.equals("candi_feed")) {
								beaconMessage.append(wifi.SSID + ": (" + String.valueOf(wifi.level) + ") " + wifi.BSSID + System.getProperty("line.separator"));
							}
						}
						beaconMessage.append(System.getProperty("line.separator"));
						beaconMessage.append("Wifi fix: " + DateUtils.intervalSince(ProximityManager.getInstance().mLastWifiUpdate, DateUtils.nowDate()));
					}

					final Location location = LocationManager.getInstance().getLocationLocked();
					if (location != null) {
						final Date fixDate = new Date(location.getTime());
						beaconMessage.append(System.getProperty("line.separator") + "Location fix: " + DateUtils.intervalSince(fixDate, DateUtils.nowDate()));
						beaconMessage.append(System.getProperty("line.separator") + "Location accuracy: " + String.valueOf(location.getAccuracy()));
						beaconMessage.append(System.getProperty("line.separator") + "Location provider: " + location.getProvider());
					}
					else {
						beaconMessage.append(System.getProperty("line.separator") + "Location fix: none");
					}
				}
				else {
					return;
				}
			}
			AircandiCommon.showAlertDialog(R.drawable.ic_launcher
					, mActivity.getString(R.string.alert_beacons_title)
					, beaconMessage.toString()
					, null
					, mActivity, android.R.string.ok, null, null, new
					DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {}
					}, null);
		}
	}

	public void doRefreshClick() {
		/* Show busy indicator */
		startActionbarBusyIndicator();

		if (mPageName.equals("RadarForm")) {
			((RadarForm) mActivity).doRefresh();
		}
		else if (mPageName.equals("PlaceForm") || mPageName.equals("PostForm")) {
			((BaseEntityView) mActivity).doRefresh();
		}
		else if (mPageName.equals("UserForm")) {
			((UserForm) mActivity).doRefresh();
		}
		else if (mPageName.equals("EntityList")) {
			((EntityList) mActivity).doRefresh();
		}
	}

	public void doConfigurationChanged(Configuration newConfig) {
		if (mIsDialog) {
			setDialogSize(newConfig);
		}
	}

	public void doUserClick(User user) {
		if (user != null) {
			Intent intent = new Intent(mActivity, UserForm.class);
			intent.putExtra(Constants.EXTRA_USER_ID, user.id);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			mActivity.startActivity(intent);
			AnimUtils.doOverridePendingTransition(mActivity, TransitionType.RadarToPage);
		}
	}

	public void doWatchingClick() {
		Intent intent = new Intent(mActivity, WatchForm.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		mActivity.startActivity(intent);
		AnimUtils.doOverridePendingTransition(mActivity, TransitionType.RadarToPage);
	}

	public void doHelpClick() {
		Intent intent = new Intent(mActivity, HelpForm.class);
		if (mPageName.equals("RadarForm")) {
			intent.putExtra(Constants.EXTRA_HELP_ID, R.layout.radar_help);
		}
		else if (mPageName.equals("PlaceForm") && mEntitySchema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
			intent.putExtra(Constants.EXTRA_HELP_ID, R.layout.place_help);
		}
		mActivity.startActivity(intent);
		AnimUtils.doOverridePendingTransition(mActivity, TransitionType.PageToHelp);
	}

	// --------------------------------------------------------------------------------------------
	// General routines
	// --------------------------------------------------------------------------------------------

	private void signout() {
		mActivity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				new AsyncTask() {

					@Override
					protected void onPreExecute() {
						showBusy(R.string.progress_signing_out, true);
					}

					@Override
					protected Object doInBackground(Object... params) {
						Thread.currentThread().setName("SignOut");
						final ModelResult result = EntityManager.getInstance().signout();
						return result;
					}

					@SuppressLint("NewApi")
					@Override
					protected void onPostExecute(Object response) {
						final ModelResult result = (ModelResult) response;
						/* We continue on even if the service call failed. */
						if (result.serviceResponse.responseCode == ResponseCode.Success) {
							Logger.i(this, "User signed out: " + Aircandi.getInstance().getUser().name + " (" + Aircandi.getInstance().getUser().id + ")");
						}
						else {
							Logger.w(this, "User signed out, service call failed: " + Aircandi.getInstance().getUser().id);
						}

						/* Stop the current tracking session. Starts again when a user logs in. */
						Tracker.stopSession(Aircandi.getInstance().getUser());

						/* Clear the user and session that is tied into auto-signin */
						com.aircandi.components.NotificationManager.getInstance().unregisterDeviceWithAircandi(
								GCMRegistrar.getRegistrationId(Aircandi.applicationContext));
						Aircandi.getInstance().setUser(null);

						Aircandi.settingsEditor.putString(Constants.SETTING_USER, null);
						Aircandi.settingsEditor.putString(Constants.SETTING_USER_SESSION, null);
						Aircandi.settingsEditor.commit();

						/* Make sure onPrepareOptionsMenu gets called */
						((SherlockActivity) mActivity).invalidateOptionsMenu();

						/* Notify interested parties */
						ImageUtils.showToastNotification(mActivity.getString(R.string.toast_signed_out), Toast.LENGTH_SHORT);
						hideBusy(true);
						final Intent intent = new Intent(mActivity, SplashForm.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						mActivity.startActivity(intent);
						mActivity.finish();
						AnimUtils.doOverridePendingTransition(mActivity, TransitionType.FormToPage);
					}
				}.execute();

			}
		});
	}

	public void routeShortcut(Shortcut shortcut, Entity hostEntity) {

		Tracker.sendEvent("ui_action", "browse_source", shortcut.app, 0, Aircandi.getInstance().getUser());
		if (shortcut.app.equals(Constants.TYPE_APPLINK_TWITTER)) {
			AndroidManager.getInstance().callTwitterActivity(mActivity, (shortcut.appId != null) ? shortcut.appId : shortcut.appUrl);
		}
		else if (shortcut.app.equals(Constants.TYPE_APPLINK_FOURSQUARE)) {
			AndroidManager.getInstance().callFoursquareActivity(mActivity, (shortcut.appId != null) ? shortcut.appId : shortcut.appUrl);
		}
		else if (shortcut.app.equals(Constants.TYPE_APPLINK_FACEBOOK)) {
			AndroidManager.getInstance().callFacebookActivity(mActivity, (shortcut.appId != null) ? shortcut.appId : shortcut.appUrl);
		}
		else if (shortcut.app.equals(Constants.TYPE_APPLINK_MAP) && hostEntity != null) {
			Tracker.sendEvent("ui_action", "map_place", null, 0, Aircandi.getInstance().getUser());
			final AirLocation location = hostEntity.getLocation();
			AndroidManager.getInstance().callMapActivity(mActivity, String.valueOf(location.lat.doubleValue())
					, String.valueOf(location.lng.doubleValue())
					, hostEntity.name);
		}
		else if (shortcut.app.equals(Constants.TYPE_APPLINK_YELP)) {
			AndroidManager.getInstance().callYelpActivity(mActivity, shortcut.appId, shortcut.appUrl);
		}
		else if (shortcut.app.equals(Constants.TYPE_APPLINK_OPENTABLE)) {
			AndroidManager.getInstance().callOpentableActivity(mActivity, shortcut.appId, shortcut.appUrl);
		}
		else if (shortcut.app.equals(Constants.TYPE_APPLINK_WEBSITE)) {
			AndroidManager.getInstance().callBrowserActivity(mActivity, (shortcut.appUrl != null) ? shortcut.appUrl : shortcut.appId);
		}
		else if (shortcut.app.equals(Constants.TYPE_APPLINK_EMAIL)) {
			AndroidManager.getInstance().callSendToActivity(mActivity, shortcut.name, shortcut.appId, null, null);
		}
		else if (shortcut.app.equals(Constants.TYPE_APPLINK_COMMENT) && hostEntity != null) {
			final IntentBuilder intentBuilder = new IntentBuilder(mActivity, EntityList.class)
					.setEntityId(hostEntity.id)
					.setListMode(ListMode.EntitiesForEntity)
					.setListItemResId(R.layout.temp_listitem_comment)
					.setListNewEnabled(true)
					.setListSchema(Constants.SCHEMA_ENTITY_COMMENT);

			final Intent intent = intentBuilder.create();
			mActivity.startActivity(intent);
			AnimUtils.doOverridePendingTransition(mActivity, TransitionType.PageToPage);
		}
		else if (shortcut.app.equals(Constants.TYPE_APPLINK_LIKE)) {
			/*
			 * We don't do anything right now. Goal is to show a form with
			 * more detail on the likes.
			 */
		}
		else if (shortcut.app.equals(Constants.TYPE_APPLINK_WATCH)) {
			/*
			 * We don't do anything right now. Goal is to show a form with
			 * more detail on the watchers.
			 */
		}
		else if (shortcut.app.equals(Constants.TYPE_APPLINK_POST) && hostEntity != null) {
			IntentBuilder intentBuilder = null;
			intentBuilder = new IntentBuilder(mActivity, EntityList.class)
					.setEntityId(hostEntity.id)
					.setListMode(ListMode.EntitiesForEntity)
					.setListItemResId(R.layout.temp_listitem_candi)
					.setListNewEnabled(true)
					.setListSchema(Constants.SCHEMA_ENTITY_POST);

			final Intent intent = intentBuilder.create();
			mActivity.startActivity(intent);
			AnimUtils.doOverridePendingTransition(mActivity, TransitionType.PageToPage);
		}
		else if (shortcut.app.equals(Constants.SCHEMA_ENTITY_PLACE) && hostEntity != null) {
			IntentBuilder intentBuilder = null;
			intentBuilder = new IntentBuilder(mActivity, EntityList.class)
					.setEntityId(hostEntity.id)
					.setListMode(ListMode.EntitiesForEntity)
					.setListItemResId(R.layout.temp_listitem_candi)
					.setListNewEnabled(true)
					.setListSchema(Constants.SCHEMA_ENTITY_POST);

			final Intent intent = intentBuilder.create();
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			mActivity.startActivity(intent);
			AnimUtils.doOverridePendingTransition(mActivity, TransitionType.PageToPage);
		}
		else if (shortcut.app.equals(Constants.SCHEMA_ENTITY_USER) && hostEntity != null) {
			IntentBuilder intentBuilder = null;
			if (shortcut.group != null && shortcut.group.size() > 1) {
				intentBuilder = new IntentBuilder(mActivity, EntityList.class)
						.setEntityId(hostEntity.id)
						.setListMode(ListMode.EntitiesForEntity)
						.setListItemResId(R.layout.temp_listitem_candi)
						.setListNewEnabled(true)
						.setListSchema(Constants.SCHEMA_ENTITY_USER);
			}
			else {
				intentBuilder = new IntentBuilder(mActivity, UserForm.class).setEntityId(shortcut.id);
			}

			final Intent intent = intentBuilder.create();
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			mActivity.startActivity(intent);
			AnimUtils.doOverridePendingTransition(mActivity, TransitionType.PageToPage);
		}
		else {
			AndroidManager.getInstance().callGenericActivity(mActivity, (shortcut.appUrl != null) ? shortcut.appUrl : shortcut.appId);
		}
	}

	public void handleServiceError(ServiceResponse serviceResponse, ServiceOperation serviceOperation) {

		Context context = mActivity;
		final ErrorType errorType = serviceResponse.exception.getErrorType();
		final String errorMessage = serviceResponse.exception.getMessage();
		final Float statusCode = serviceResponse.exception.getStatusCode();

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
		if (errorType == ErrorType.Client) {

			if (serviceResponse.exception.getInnerException() instanceof IOException) {
				/*
				 * We have a bad network connection.
				 * 
				 * This could be any of these:
				 * 
				 * Handled above
				 * - SocketException: thrown during socket creation or setting options
				 * - ConnectTimeoutException: timeout expired trying to connect to service
				 * - SocketTimeoutException: timeout expired on a socket
				 * 
				 * Still left
				 * - NoHttpResponseException: target server failed to respond with a valid HTTP response
				 * - UnknownHostException: hostname didn't exist in the dns system
				 */
				if (serviceResponse.exception.getInnerException() instanceof SocketException) {
					/*
					 * We don't have a network connection.
					 */
					final Intent intent = new Intent(mContext, RadarForm.class);
					AirNotification airNotification = new AirNotification();
					airNotification.title = mActivity.getString(R.string.error_connection_none_notification_title);
					airNotification.subtitle = mActivity.getString(R.string.error_connection_none_notification_message);
					airNotification.intent = intent;
					airNotification.type = "network";
					NotificationManager.getInstance().showNotification(airNotification, context);

					showAlertDialogSimple(null, mActivity.getString(R.string.error_connection_none));
				}
				else if (serviceResponse.exception.getInnerException() instanceof WalledGardenException) {
					/*
					 * We have a connection but user is locked in a walled garden until they sign-in, pay, etc.
					 */
					showAlertDialogSimple(null, mActivity.getString(R.string.error_connection_walled_garden));
				}
				else if (serviceResponse.exception.getInnerException() instanceof ConnectTimeoutException) {
					/*
					 * This exception signals that HttpClient is unable to establish a connection with the target server
					 * or proxy server within the given period of time.
					 */
					ImageUtils.showToastNotification(mActivity.getString(R.string.error_service_unavailable), Toast.LENGTH_SHORT);
				}
				else if (serviceResponse.exception.getInnerException() instanceof SocketTimeoutException) {
					/*
					 * We have a connection but got tired of waiting for data. Could be a
					 * poor connection or service is slow.
					 */
					ImageUtils.showToastNotification(mActivity.getString(R.string.error_connection_poor), Toast.LENGTH_SHORT);
				}
				else if (serviceResponse.exception.getInnerException() instanceof UnknownHostException) {
					/*
					 * We have a connection but got tired of waiting for data. Could be a
					 * poor connection or service is slow.
					 */
					showAlertDialogSimple(null, mActivity.getString(R.string.error_client_unknown_host));
				}
				else if (serviceResponse.exception.getInnerException() instanceof ClientProtocolException) {
					/*
					 * Something wrong with the request. In most cases, this is a bug and
					 * not something that a user should cause unless they provided a bad uri.
					 */
					ImageUtils.showToastNotification(mActivity.getString(R.string.error_client_request_error), Toast.LENGTH_SHORT);
				}
				else {
					ImageUtils.showToastNotification(mActivity.getString(R.string.error_connection_poor), Toast.LENGTH_SHORT);
				}
			}
			else {
				/*
				 * Something wrong with the request. In most cases, this is a bug and
				 * not something that a user should cause unless they provided a bad uri.
				 */
				if (serviceResponse.exception.getInnerException() instanceof URISyntaxException) {
					ImageUtils.showToastNotification(mActivity.getString(R.string.error_client_request_error), Toast.LENGTH_SHORT);
				}
				else {
					/* Something without special handling */
					ImageUtils.showToastNotification(serviceResponse.exception.getMessage(), Toast.LENGTH_SHORT);
				}
			}
		}
		else if (errorType == ErrorType.Service) {

			if (serviceResponse.exception.getInnerException() instanceof HttpServiceException.NotFoundException) {
				/*
				 * Reached the service but requested something that doesn't exist. This is a bug and
				 * not something that a user should cause.
				 */
				ImageUtils.showToastNotification(mActivity.getString(R.string.error_client_request_not_found), Toast.LENGTH_SHORT);
			}
			else if (serviceResponse.exception.getInnerException() instanceof HttpServiceException.UnauthorizedException) {
				/*
				 * Reached the service but requested something that the user can't access.
				 */
				ImageUtils.showToastNotification(mActivity.getString(R.string.error_service_unauthorized), Toast.LENGTH_SHORT);
			}
			else if (serviceResponse.exception.getInnerException() instanceof HttpServiceException.ForbiddenException) {
				/*
				 * Reached the service but request was invalid per service policy.
				 */
				ImageUtils.showToastNotification(mActivity.getString(R.string.error_service_forbidden), Toast.LENGTH_SHORT);
			}
			else if (serviceResponse.exception.getInnerException() instanceof HttpServiceException.GatewayTimeoutException) {
				/*
				 * Reached the service but request was invalid per service policy.
				 */
				ImageUtils.showToastNotification(mActivity.getString(R.string.error_service_gateway_timeout), Toast.LENGTH_SHORT);
			}
			else if (serviceResponse.exception.getInnerException() instanceof HttpServiceException.ClientVersionException) {
				/*
				 * Reached the service but a more current client version is required.
				 */
				Aircandi.applicationUpdateRequired = true;
				final Intent intent = new Intent(mActivity, SplashForm.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				mActivity.startActivity(intent);
				mActivity.finish();
				AnimUtils.doOverridePendingTransition(mActivity, TransitionType.FormToPage);
			}
			else {
				String title = null;
				String message = null;
				if (statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
					/*
					 * Reached the service with a good call but the service failed for an unknown reason. Examples
					 * are service bugs like missing indexes causing mongo queries to throw errors.
					 * 
					 * - 500: Something bad and unknown has happened in the service.
					 */
					ImageUtils.showToastNotification(mActivity.getString(R.string.error_service_unknown), Toast.LENGTH_SHORT);
				}
				else {
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
					if (statusCode == ProxiConstants.HTTP_STATUS_CODE_UNAUTHORIZED_SESSION_EXPIRED) {
						title = mActivity.getString(R.string.error_session_expired_title);
						message = mActivity.getString(R.string.error_session_expired);
						/*
						 * Make sure the user is logged out
						 */
						signout();

					}
					else if (statusCode == ProxiConstants.HTTP_STATUS_CODE_UNAUTHORIZED_CREDENTIALS) {
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
					else if (statusCode == ProxiConstants.HTTP_STATUS_CODE_UNAUTHORIZED_WHITELIST) {
						message = mActivity.getString(R.string.error_whitelist_unauthorized);
					}
					else if (statusCode == ProxiConstants.HTTP_STATUS_CODE_UNAUTHORIZED_UNVERIFIED) {
						message = mActivity.getString(R.string.error_unverified_unauthorized);
					}
					else if (statusCode == ProxiConstants.HTTP_STATUS_CODE_FORBIDDEN_USER_PASSWORD_WEAK) {
						message = mActivity.getString(R.string.error_signup_password_weak);
					}
					else if (statusCode == ProxiConstants.HTTP_STATUS_CODE_FORBIDDEN_DUPLICATE) {
						message = mActivity.getString(R.string.error_signup_email_taken);
					}
					else {
						ImageUtils.showToastNotification(serviceResponse.exception.getMessage(), Toast.LENGTH_SHORT);
					}
				}

				if (message != null) {
					showAlertDialogSimple(title, message);
				}
			}
		}

		Logger.w(context, "Service error: " + errorMessage);
	}

	@SuppressWarnings("ucd")
	public void startScanService(int scanInterval) {

		/* Start first scan right away */
		Logger.d(this, "Starting wifi scan service");
		final Intent scanIntent = new Intent(Aircandi.applicationContext, ScanService.class);
		mActivity.startService(scanIntent);

		/* Setup a scanning schedule */
		if (scanInterval > 0) {
			final AlarmManager alarmManager = (AlarmManager) mActivity.getSystemService(Service.ALARM_SERVICE);
			final PendingIntent pendingIntent = PendingIntent.getService(Aircandi.applicationContext, 0, scanIntent, 0);
			alarmManager.cancel(pendingIntent);
			alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP
					, SystemClock.elapsedRealtime() + scanInterval
					, scanInterval, pendingIntent);
		}
	}

	@SuppressWarnings("ucd")
	public void stopScanService() {
		final AlarmManager alarmManager = (AlarmManager) mActivity.getSystemService(Service.ALARM_SERVICE);
		final Intent scanIntent = new Intent(Aircandi.applicationContext, ScanService.class);
		final PendingIntent pendingIntent = PendingIntent.getService(Aircandi.applicationContext, 0, scanIntent, 0);
		alarmManager.cancel(pendingIntent);
		Logger.d(this, "Stopped wifi scan service");
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
		final Intent intent = new Intent(mActivity, TemplatePicker.class);
		mActivity.startActivityForResult(intent, Constants.ACTIVITY_TEMPLATE_PICK);
		AnimUtils.doOverridePendingTransition(mActivity, TransitionType.PageToForm);
	}

	public void showCandiFormForEntity(String entityId, Class<?> clazz) {

		final IntentBuilder intentBuilder = new IntentBuilder(mActivity, clazz)
				.setEntityId(entityId)
				.setEntitySchema(ServiceEntry.getSchemaFromId(entityId));

		Entity entity = EntityManager.getEntity(entityId);
		if (entity != null) {
			intentBuilder.setEntitySchema(entity.schema);
			if (entity.toId != null) {
				intentBuilder.setEntityParentId(entity.getParent().id);
			}
		}

		final Intent intent = intentBuilder.create();

		mActivity.startActivity(intent);
		AnimUtils.doOverridePendingTransition(mActivity, TransitionType.PageToPage);
	}

	public void updateAccuracyIndicator() {

		final Location location = LocationManager.getInstance().getLocationLocked();

		if (mAccuracyIndicator != null) {

			mActivity.runOnUiThread(new Runnable() {

				@Override
				public void run() {

					int sizeDip = 35;

					if (location != null && location.hasAccuracy()) {

						sizeDip = 35;

						if (location.getAccuracy() <= 100) {
							sizeDip = 25;
						}
						if (location.getAccuracy() <= 50) {
							sizeDip = 13;
						}
						if (location.getAccuracy() <= 30) {
							sizeDip = 7;
						}
						Logger.v(this, "Location accuracy: >>> " + String.valueOf(sizeDip));
					}

					final int sizePixels = ImageUtils.getRawPixels(mActivity, sizeDip);
					final FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(sizePixels, sizePixels, Gravity.CENTER);
					mAccuracyIndicator.setLayoutParams(layoutParams);
					mAccuracyIndicator.setBackgroundResource(R.drawable.bg_accuracy_indicator);
				}
			});
		}
	}

	public void updateDevIndicator(final List<WifiScanResult> scanList, Location location) {

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
					}
				});

			}
		}

		if (location != null) {
			Location locationLocked = LocationManager.getInstance().getLocationLocked();
			if (locationLocked != null) {
				if (location.getProvider().equals(locationLocked.getProvider()) && (int) location.getAccuracy() == (int) locationLocked.getAccuracy()) {
					mBeaconIndicator.setTextColor(Aircandi.getInstance().getResources().getColor(R.color.accent_blue));
				}
				else {
					if (mThemeTone.equals("dark")) {
						mBeaconIndicator.setTextColor(Aircandi.getInstance().getResources().getColor(R.color.text_dark));
					}
					else {
						mBeaconIndicator.setTextColor(Aircandi.getInstance().getResources().getColor(R.color.text_light));
					}
				}
			}

			String debugLocation = location.getProvider().substring(0, 1).toUpperCase(Locale.ROOT);
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

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	public static AlertDialog showAlertDialog(Integer iconResource // $codepro.audit.disable largeNumberOfParameters
			, String titleText
			, String message
			, View customView
			, Context context
			, Integer okButtonId
			, Integer cancelButtonId
			, Integer neutralButtonId
			, OnClickListener listenerClick
			, OnCancelListener listenerCancel) {

		final AlertDialog.Builder builder = new AlertDialog.Builder(context);

		if (iconResource != null) {
			builder.setIcon(iconResource);
		}

		if (titleText != null) {
			final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View title = inflater.inflate(R.layout.temp_dialog_title, null);
			((TextView) title.findViewById(R.id.name)).setText(titleText);
			builder.setCustomTitle(title);
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

		if (cancelButtonId != null) {
			builder.setNegativeButton(cancelButtonId, listenerClick);
		}

		if (neutralButtonId != null) {
			builder.setNeutralButton(neutralButtonId, listenerClick);
		}

		if (listenerCancel != null) {
			builder.setOnCancelListener(listenerCancel);
		}

		final AlertDialog alert = builder.create();
		alert.show();

		/* Hardcoded size for body text in the alert */
		final TextView textView = (TextView) alert.findViewById(android.R.id.message);
		if (textView != null) {
			textView.setTextSize(14);
		}

		/* Prevent dimming the background */
		if (Constants.SUPPORTS_ICE_CREAM_SANDWICH) {
			alert.getWindow().setDimAmount(Constants.DIALOGS_DIM_AMOUNT);
		}

		return alert;
	}

	public void showAlertDialogSimple(final String titleText, final String message) {
		if (!mActivity.isFinishing()) {
			mActivity.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					AircandiCommon.showAlertDialog(R.drawable.ic_launcher
							, titleText
							, message
							, null
							, mContext
							, android.R.string.ok
							, null
							, null
							, null
							, null);
				}
			});
		}
	}

	public void setTheme(Integer themeResId, Boolean isDialog, Boolean isTransparent) {
		mPrefTheme = Aircandi.settings.getString(Constants.PREF_THEME, Constants.PREF_THEME_DEFAULT);
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
		Integer themeId = themeResId;
		if (themeId == null) {
			themeId = mContext.getApplicationContext().getResources().getIdentifier(mPrefTheme, "style", mContext.getPackageName());
			if (isDialog) {
				themeId = R.style.aircandi_theme_dialog_dark;
				if (mPrefTheme.equals("aircandi_theme_snow")) {
					themeId = R.style.aircandi_theme_dialog_light;
				}
			}
			else if (isTransparent) {
				themeId = R.style.aircandi_theme_midnight_transparent;
				if (mPrefTheme.equals("aircandi_theme_snow")) {
					themeId = R.style.aircandi_theme_snow_transparent;
				}
			}
		}

		((Activity) mContext).setTheme(themeId);
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
			if (BuildConfig.DEBUG) {
				e.printStackTrace();
			}
		}
		return actionBarTitleId;
	}

	@SuppressLint("NewApi")
	private void setDialogSize(Configuration newConfig) {

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB_MR2) {
			//			final android.view.WindowManager.LayoutParams params = mActivity.getWindow().getAttributes();
			//			final int height = Math.min(newConfig.screenHeightDp, 450);
			//			final int width = Math.min(newConfig.screenWidthDp, 350);
			//			params.height = ImageUtils.getRawPixels(mActivity, height);
			//			params.width = ImageUtils.getRawPixels(mActivity, width);
			//			mActivity.getWindow().setAttributes(params);
		}
	}

	// --------------------------------------------------------------------------------------------
	// UI progress and notifications
	// --------------------------------------------------------------------------------------------

	public void showBusy(Boolean reset) {
		showBusy(null, reset);
	}

	public void showBusy(Integer messageResId, Boolean reset) {

		if (reset) {
			mBusyCount.set(1);
		}
		else {
			mBusyCount.getAndIncrement();
		}
		Logger.v(this, "Busy count: " + String.valueOf(mBusyCount.get()));

		try {

			if (mBusyCount.get() == 1) {
				startActionbarBusyIndicator();
			}

			if (messageResId != null) {
				final ProgressDialog progressDialog = getProgressDialog();
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
		catch (BadTokenException e) {
			/*
			 * Sometimes the activity has been destroyed out from under us
			 * so we trap this and continue.
			 */
			e.printStackTrace();
		}
	}

	public void hideBusy(Boolean force) {
		final ProgressDialog progressDialog = getProgressDialog();
		if (progressDialog.isShowing() && progressDialog.getWindow().getWindowManager() != null) {
			try {
				progressDialog.dismiss();
			}
			catch (Exception e) {
				/*
				 * Sometime we get a harmless exception that the view is not attached to window manager.
				 * It could be that the activity is getting destroyed before the dismiss can happen.
				 * We catch it and move on.
				 */
				Logger.v(this, e.getMessage());
			}
		}

		mBusyCount.getAndDecrement();
		Logger.v(this, "Busy count: " + String.valueOf(mBusyCount.get()));

		if (mBusyCount.get() <= 0 || force) {
			stopActionbarBusyIndicator();
			stopBodyBusyIndicator();
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

	public void startBodyBusyIndicator() {
		final View progress = mActivity.findViewById(R.id.progress);
		if (progress != null) {
			progress.setVisibility(View.VISIBLE);
		}
	}

	private void stopBodyBusyIndicator() {
		final View progress = mActivity.findViewById(R.id.progress);
		if (progress != null) {
			progress.setVisibility(View.GONE);
		}
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
		final SherlockActivity activity = (SherlockActivity) mActivity;
		if (mPageName.equals("RadarForm")) {
			activity.getSupportMenuInflater().inflate(R.menu.menu_radar, menu);

			/* Beacon indicator */
			mMenuItemBeacons = menu.findItem(R.id.beacons);
			if (mMenuItemBeacons != null) {

				/* Only show beacon indicator if user is a developer */
				if (!Aircandi.settings.getBoolean(Constants.PREF_ENABLE_DEV, Constants.PREF_ENABLE_DEV_DEFAULT)
						|| Aircandi.getInstance().getUser() == null
						|| Aircandi.getInstance().getUser().developer == null
						|| !Aircandi.getInstance().getUser().developer) {
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
				}
			}
		}
		else if (mPageName.equals("PlaceForm") || mPageName.equals("PostForm")) {
			activity.getSupportMenuInflater().inflate(R.menu.menu_candi, menu);
			activity.getSupportMenuInflater().inflate(R.menu.menu_base, menu);
		}
		else if (mPageName.equals("EntityList")) {
			activity.getSupportMenuInflater().inflate(R.menu.menu_entity_list, menu);
			activity.getSupportMenuInflater().inflate(R.menu.menu_base, menu);
		}
		else if (mPageName.equals("UserForm")) {
			activity.getSupportMenuInflater().inflate(R.menu.menu_user, menu);

			/* Hide user edit menu item if not the current user */
			MenuItem menuItem = menu.findItem(R.id.edit);
			if (menuItem != null && Aircandi.getInstance().getUser() != null && !Aircandi.getInstance().getUser().id.equals(mUserId)) {
				menuItem.setVisible(false);
			}
		}
		else if (mPageName.equals("HelpForm")) {
			activity.getSupportMenuInflater().inflate(R.menu.menu_help, menu);
		}

		/* Editing */

		else if (mPageName.equals("CommentEdit")) {
			activity.getSupportMenuInflater().inflate(R.menu.menu_comment_form, menu);
		}
		else if (mPageName.equals("FeedbackEdit")) {
			activity.getSupportMenuInflater().inflate(R.menu.menu_send, menu);
		}
		else if (mPageName.equals("TuningEdit")) {
			activity.getSupportMenuInflater().inflate(R.menu.menu_tuning_wizard, menu);
		}
		else if (mPageName.equals("ApplinkEdit") || mPageName.equals("ApplinksEdit")) {
			activity.getSupportMenuInflater().inflate(R.menu.menu_builder, menu);
		}
		else if (mPageName.equals("UserEdit") || mPageName.equals("PasswordEdit")) {
			activity.getSupportMenuInflater().inflate(R.menu.menu_builder, menu);
		}
		else if (mPageName.contains("Edit")) {
			activity.getSupportMenuInflater().inflate(R.menu.menu_entity, menu);
		}
		else if (mPageName.contains("Builder")) {
			activity.getSupportMenuInflater().inflate(R.menu.menu_builder, menu);
		}

		/* Cache edit and delete menus because we need to toggle it later */
		mMenuItemEdit = menu.findItem(R.id.edit);
		mMenuItemDelete = menu.findItem(R.id.delete);

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
			updateAccuracyIndicator();
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

		Intent intent = null;

		switch (menuItem.getItemId()) {
			case android.R.id.home:
				mActivity.onBackPressed();
				return;
			case R.id.home:
				intent = new Intent(mActivity, RadarForm.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				mActivity.finish();
				mActivity.startActivity(intent);
				AnimUtils.doOverridePendingTransition(mActivity, TransitionType.PageToPage);
				return;
			case R.id.settings:
				mActivity.startActivityForResult(new Intent(mActivity, Preferences.class), Constants.ACTIVITY_PREFERENCES);
				AnimUtils.doOverridePendingTransition(mActivity, TransitionType.PageToForm);
				return;
			case R.id.signout:
				Tracker.sendEvent("ui_action", "signout_user", null, 0, Aircandi.getInstance().getUser());
				signout();
				return;
			case R.id.feedback:
				doFeedbackClick();
				return;
			case R.id.profile:
				doUserClick(Aircandi.getInstance().getUser());
				return;
			case R.id.watching:
				doWatchingClick();
				return;
			case R.id.help:
				doHelpClick();
				return;
			case R.id.cancel:
				mActivity.setResult(Activity.RESULT_CANCELED);
				mActivity.finish();
				AnimUtils.doOverridePendingTransition(mActivity, TransitionType.FormToPage);
				return;
			case R.id.cancel_help:
				mActivity.setResult(Activity.RESULT_CANCELED);
				mActivity.finish();
				AnimUtils.doOverridePendingTransition(mActivity, TransitionType.HelpToPage);
				return;
			default:
				return;
		}
	}

	// --------------------------------------------------------------------------------------------
	// Tab routines
	// --------------------------------------------------------------------------------------------

	private void manageTabs() {
		Logger.v(this, "Building tabs: " + mPageName);
		if (mPageName.equals("UserForm")) {
			addTabsToActionBar(this, Constants.TABS_PROFILE_FORM_ID);
		}
		else if (mPageName.equals("PlaceEdit") || mPageName.equals("PostEdit")) {
			if (mEntityId != null) {
				/* Editing */
				mEntity = EntityManager.getEntity(mEntityId);
				if (mEntity != null) {
					if (mEntity.ownerId != null && (mEntity.ownerId.equals(Aircandi.getInstance().getUser().id))) {
						addTabsToActionBar(this, Constants.TABS_ENTITY_FORM_ID);
					}
				}
			}
			else {
				/* Making something new */
				addTabsToActionBar(this, Constants.TABS_ENTITY_FORM_ID);
			}
		}
	}

	private void addTabsToActionBar(ActionBar.TabListener tabListener, int tabsId)
	{
		mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		if (tabsId == Constants.TABS_ENTITY_FORM_ID) {

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
		else if (tabsId == Constants.TABS_PROFILE_FORM_ID) {

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
	}

	public void doDestroy() {}

	public void doPause() {
		BusProvider.getInstance().unregister(this);
		synchronized (GCMIntentService.lock) {
			GCMIntentService.currentActivity = null;
		}
	}

	public void doStop() {
		Tracker.activityStop(mActivity, Aircandi.getInstance().getUser());
	}

	public void doStart() {
		Tracker.activityStart(mActivity, Aircandi.getInstance().getUser());
	}

	public void doResume() {
		BusProvider.getInstance().register(this);
		synchronized (GCMIntentService.lock) {
			GCMIntentService.currentActivity = mActivity;
		}
	}

	public void doSaveInstanceState(Bundle savedInstanceState) {
		/*
		 * This gets called from comment, profile and entity forms
		 */
		if (mActionBar != null && mActionBar.getTabCount() > 0) {
			savedInstanceState.putInt("tab_index", (mActionBar.getSelectedTab() != null) ? mActionBar.getSelectedTab().getPosition() : 0);
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
		CandiUser, PlaceSearch
	}

}
