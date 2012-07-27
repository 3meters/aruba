package com.proxibase.aircandi.components;

import java.io.File;
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
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
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
import com.proxibase.aircandi.Aircandi;
import com.proxibase.aircandi.Aircandi.CandiTask;
import com.proxibase.aircandi.CandiForm;
import com.proxibase.aircandi.CandiList;
import com.proxibase.aircandi.CandiMap;
import com.proxibase.aircandi.CandiPicker;
import com.proxibase.aircandi.CandiRadar;
import com.proxibase.aircandi.CandiRadar.RefreshType;
import com.proxibase.aircandi.CommentList;
import com.proxibase.aircandi.FormActivity;
import com.proxibase.aircandi.Preferences;
import com.proxibase.aircandi.ProfileForm;
import com.proxibase.aircandi.R;
import com.proxibase.aircandi.ScanService;
import com.proxibase.aircandi.SignInForm;
import com.proxibase.aircandi.TemplatePicker;
import com.proxibase.aircandi.UserCandiForm;
import com.proxibase.aircandi.UserCandiList;
import com.proxibase.aircandi.candi.models.CandiPatchModel;
import com.proxibase.aircandi.candi.presenters.CandiPatchPresenter;
import com.proxibase.aircandi.components.AnimUtils.TransitionType;
import com.proxibase.aircandi.components.Events.EventHandler;
import com.proxibase.aircandi.components.NetworkManager.ServiceResponse;
import com.proxibase.aircandi.components.ProxiExplorer.EntityTree;
import com.proxibase.aircandi.components.ProxiExplorer.WifiScanResult;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.aircandi.widgets.WebImageView;
import com.proxibase.service.ProxiConstants;
import com.proxibase.service.ProxibaseService;
import com.proxibase.service.ProxibaseService.GsonType;
import com.proxibase.service.ProxibaseService.RequestType;
import com.proxibase.service.ProxibaseService.ResponseFormat;
import com.proxibase.service.ProxibaseServiceException.ErrorCode;
import com.proxibase.service.ProxibaseServiceException.ErrorType;
import com.proxibase.service.ServiceRequest;
import com.proxibase.service.objects.Entity;
import com.proxibase.service.objects.GeoLocation;
import com.proxibase.service.objects.Session;
import com.proxibase.service.objects.User;

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
	private Integer						mThemeBusyIndicatorResId;
	public Integer						mThemeDialogResId;

	/* UI */
	protected ImageView					mProgressIndicator;
	public TextView						mBeaconIndicator;
	protected TextView					mTitle;
	protected ImageView					mButtonRefresh;
	public Dialog						mProgressDialog;
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

		/* Get view references */
		mProgressIndicator = null;
		if (mProgressIndicator != null) {
			mProgressIndicator.setVisibility(View.INVISIBLE);
		}

		mTitle = (TextView) mActivity.findViewById(R.id.text_title);
		showLocationAccuracy();

		/* Tabs: setup tabs if appropriate */
		manageTabs();


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

		mButtonRefresh = null;
		if (mButtonRefresh != null) {
			mButtonRefresh.setVisibility(View.VISIBLE);
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
				mEntityLocation = ProxibaseService.getGson(GsonType.Internal).fromJson(json, GeoLocation.class);
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
		AircandiCommon.showAlertDialog(R.drawable.icon_app, title, message,
				mActivity, android.R.string.ok, null, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {}
				});
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
				int beaconCount = ProxiExplorer.getInstance().mWifiList.size();
				if (beaconCount > 0) {
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
			AircandiCommon.showAlertDialog(R.drawable.icon_app, mActivity.getString(R.string.alert_beacons_title), beaconMessage, mActivity,
					android.R.string.ok, null, new
					DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {}
					});
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
		Intent intent = new Intent(mActivity, TemplatePicker.class);
		intent.putExtra(mActivity.getString(R.string.EXTRA_ENTITY_IS_ROOT), isRoot);
		intent.putExtra(mActivity.getString(R.string.EXTRA_THEME_ID), mThemeTone.equals("dark") ? R.style.Theme_Sherlock_Dialog
				: R.style.Theme_Sherlock_Light_Dialog);
		mActivity.startActivityForResult(intent, CandiConstants.ACTIVITY_TEMPLATE_PICK);
		AnimUtils.doOverridePendingTransition(mActivity, TransitionType.CandiPageToForm);
	}

	@SuppressWarnings("unused")
	public void showLocationAccuracy() {
		final Location location = GeoLocationManager.getInstance().getCurrentLocation();
		// if (location != null
		// && Aircandi.getInstance().getUser() != null
		// && Aircandi.getInstance().getUser().isDeveloper != null
		// && Aircandi.getInstance().getUser().isDeveloper) {
		// if (location.hasAccuracy()) {
		// TextView textView = (TextView) mActivity.findViewById(R.id.text_header_debug);
		// if (textView != null) {
		// textView.setVisibility(View.VISIBLE);
		// textView.setText(String.valueOf(location.getAccuracy()));
		// }
		// else if (mTitle != null) {
		// final String title = mActivity.getString(R.string.app_name);
		// mActivity.runOnUiThread(new Runnable() {
		//
		// @Override
		// public void run() {
		// mTitle.setText(title + "  " + location.getProvider().substring(0, 1).toUpperCase()
		// + String.valueOf(location.getAccuracy()));
		// }
		// });
		// }
		// }
		// }
		// else {
		// /* Clear location info */
		// TextView textView = (TextView) mActivity.findViewById(R.id.text_header_debug);
		// if (textView != null) {
		// textView.setVisibility(View.GONE);
		// }
		// else if (mTitle != null) {
		// final String title = mActivity.getString(R.string.app_name);
		// mActivity.runOnUiThread(new Runnable() {
		//
		// @Override
		// public void run() {
		// mTitle.setText(title);
		// }
		// });
		// }
		// }
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
						if (wifi.global || wifi.demo) {
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
			});

		}
	}

	public void handleServiceError(ServiceResponse serviceResponse) {
		handleServiceError(serviceResponse, ServiceOperation.Unknown, null);
	}

	@SuppressWarnings("deprecation")
	public void handleServiceError(ServiceResponse serviceResponse, ServiceOperation serviceOperation, Context context) {

		ErrorType errorType = serviceResponse.exception.getErrorType();
		ErrorCode errorCode = serviceResponse.exception.getErrorCode();
		String errorMessage = serviceResponse.exception.getMessage();
		String responseMessage = serviceResponse.exception.getResponseMessage();
		String logMessage = null;
		String friendlyMessage = mActivity.getString(R.string.error_service_general);
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
					friendlyMessage = mActivity.getString(R.string.error_client_unknown_host);
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
				friendlyMessage = mActivity.getString(R.string.error_client_unknown_host);
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
			AircandiCommon.showAlertDialog(R.drawable.icon_app, mActivity.getString(R.string.alert_error_title),
					friendlyMessage, context, android.R.string.ok, null, new
					DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {}
					});
		}
		else {
			if (isDeveloper) {
				friendlyMessage = responseMessage;
			}

			Notification note = new Notification(R.drawable.icon_app, mActivity.getString(R.string.notification_error_network_message),
					System.currentTimeMillis());

			RemoteViews contentView = new RemoteViews(mActivity.getPackageName(), R.layout.custom_notification);
			contentView.setImageViewResource(R.id.image, R.drawable.icon_app);
			if (isDeveloper) {
				contentView.setViewVisibility(R.id.title, View.GONE);
			}
			contentView.setTextViewText(R.id.title, mActivity.getString(R.string.name_app));
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
			text.setText(message == null ? mActivity.getString(R.string.progress_loading) : message);

			/* Prevent dismissing the indicator with the back key */
			mProgressDialog.setCancelable(false);

			mProgressDialog.show();
			image.post(new Runnable() {

				@Override
				public void run() {
					image.setBackgroundResource(mThemeBusyIndicatorResId);
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

	public static void showAlertDialog(Integer iconResource, String titleText, String message, Context context, Integer okButtonId, Integer cancelButtonId,
			OnClickListener listener) {

		AlertDialog.Builder builder = new AlertDialog.Builder(context).setIcon(iconResource).setTitle(titleText).setMessage(message);

		if (okButtonId != null) {
			builder.setPositiveButton(okButtonId, listener);
		}

		if (cancelButtonId != null) {
			builder.setNegativeButton(cancelButtonId, listener);
		}

		AlertDialog alert = builder.show();

		/* Hardcoded size for body text in the alert */
		TextView textView = (TextView) alert.findViewById(android.R.id.message);
		textView.setTextSize(14);

		/* Prevent dimming the background */
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

	public void setTheme(Boolean isDialog) {
		mPrefTheme = Aircandi.settings.getString(Preferences.PREF_THEME, "aircandi_theme_midnight");
		Integer themeResId = mContext.getApplicationContext().getResources()
				.getIdentifier(mPrefTheme, "style", mContext.getPackageName());
		if (isDialog) {
			themeResId = R.style.aircandi_theme_dialog;
			if (mPrefTheme.equals("aircandi_theme_snow")
					|| mPrefTheme.equals("aircandi_theme_serene")) {
				themeResId = R.style.aircandi_theme_light_dialog;
			}
		}
		((Activity) mContext).setTheme(themeResId);
	}

	public void signinAuto() {
		Logger.i(this, "Auto sign in...");

		/* Use anonymous user as initial default */
		User user = ProxibaseService.getGson(GsonType.Internal).fromJson(CandiConstants.USER_ANONYMOUS, User.class);

		String jsonUser = Aircandi.settings.getString(Preferences.PREF_USER, null);
		String jsonSession = Aircandi.settings.getString(Preferences.PREF_USER_SESSION, null);

		if (jsonUser != null && jsonSession != null) {

			user = ProxibaseService.getGson(GsonType.Internal).fromJson(jsonUser, User.class);
			user.session = ProxibaseService.getGson(GsonType.Internal).fromJson(jsonSession, Session.class);
			/*
			 * If user is about to expire, we trigger the sign in process. We go back to the anonymous user
			 * as the default in case the user cancels out of the sign in form.
			 */
			if (user.session != null && user.session.expirationDate != null && user.session.renewSession(DateUtils.nowDate().getTime())) {
				user = ProxibaseService.getGson(GsonType.Internal).fromJson(CandiConstants.USER_ANONYMOUS, User.class);
				Aircandi.getInstance().setUser(user);
				signin(R.string.signin_message_session_expired);
				return;
			}
			Tracker.startNewSession();
			Tracker.trackEvent("User", "AutoSignin", null, 0);
		}

		/* We fall to here if we didn't find a good user or session */
		Aircandi.getInstance().setUser(user);
		ImageUtils.showToastNotification(mActivity.getString(R.string.toast_signed_in) + " " + Aircandi.getInstance().getUser().name, Toast.LENGTH_SHORT);

		/* Make sure onPrepareOptionsMenu gets called (since api 11) */
		((SherlockActivity) mActivity).invalidateOptionsMenu();
	}

	public void signout() {
		if (Aircandi.getInstance().getUser() != null && !Aircandi.getInstance().getUser().anonymous) {

			new AsyncTask() {

				@Override
				protected void onPreExecute() {
					showProgressDialog(true, mActivity.getString(R.string.progress_signing_out));
				}

				@Override
				protected Object doInBackground(Object... params) {

					/*
					 * Sign out with the service so the session can be cleaned up.
					 */
					User user = Aircandi.getInstance().getUser();
					ServiceResponse serviceResponse = new ServiceResponse();

					if (user.session != null) {
						Bundle parameters = new Bundle();
						ServiceRequest serviceRequest = new ServiceRequest();

						parameters.putString("user", "object:{"
								+ "\"_id\":\"" + user.id + "\","
								+ "\"session\":\"" + user.session.key + "\""
								+ "}");

						serviceRequest.setUri(ProxiConstants.URL_PROXIBASE_SERVICE_AUTH + "signout")
								.setRequestType(RequestType.Method)
								.setParameters(parameters)
								.setResponseFormat(ResponseFormat.Json);

						serviceResponse = NetworkManager.getInstance().request(serviceRequest);
					}

					return serviceResponse;
				}

				@Override
				protected void onPostExecute(Object response) {
					/*
					 * We continue on even if the service call failed.
					 */
					User user = ProxibaseService.getGson(GsonType.Internal).fromJson(CandiConstants.USER_ANONYMOUS, User.class);
					user.anonymous = true;
					Aircandi.getInstance().setUser(user);

					/* Clear the user and session that is tied into auto-signin */
					Aircandi.settingsEditor.putString(Preferences.PREF_USER, null);
					Aircandi.settingsEditor.putString(Preferences.PREF_USER_SESSION, null);
					Aircandi.settingsEditor.commit();

					/* Make sure onPrepareOptionsMenu gets called */
					((SherlockActivity) mActivity).invalidateOptionsMenu();

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

	public void doOptionsItemSelected(MenuItem menuItem) {

		switch (menuItem.getItemId()) {
			case android.R.id.home:
				/*
				 * If this doesn't get handled directly by the activity then
				 * it falls through to here and we want to go to the top of the app.
				 */
				mActivity.onBackPressed();
				// Intent intent = new Intent(mActivity, CandiRadar.class);
				// intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				// intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				// mActivity.startActivity(intent);
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
		Logger.i(this, "Building tabs: " + mPageName);
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
		Logger.i(this, "Setting active tab: " + String.valueOf(position));
		if (mActionBar.getSelectedTab() == null || mActionBar.getSelectedTab().getPosition() != position) {
			mActionBar.getTabAt(position).select();
		}
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		Logger.i(this, "onTabSelected: " + tab.getTag());
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
		Logger.i(this, "onTabReselected: " + tab.getText() + ", Top: " + String.valueOf(mNavigationTop));

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
		stopTitlebarProgress();
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

	public enum ServiceOperation {
		Login,
		Logout,
		BeaconScan,
		Chunking,
		ProfileBrowse,
		ProfileSave,
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
		Unknown
	}

	public static enum ActionButtonSet {
		Radar, CandiForm, CandiList, CommentList
	}

}
