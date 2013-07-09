package com.aircandi.utilities;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.conn.ConnectTimeoutException;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.ProxiConstants;
import com.aircandi.beta.R;
import com.aircandi.components.AndroidManager;
import com.aircandi.components.IntentBuilder;
import com.aircandi.components.Logger;
import com.aircandi.components.NetworkManager.ServiceResponse;
import com.aircandi.components.NotificationManager;
import com.aircandi.components.Tracker;
import com.aircandi.service.HttpServiceException;
import com.aircandi.service.HttpServiceException.ErrorType;
import com.aircandi.service.WalledGardenException;
import com.aircandi.service.objects.AirLocation;
import com.aircandi.service.objects.AirNotification;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.Shortcut;
import com.aircandi.ui.RadarForm;
import com.aircandi.ui.SplashForm;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.ui.base.BaseActivity.ServiceOperation;
import com.aircandi.ui.base.BaseEntityList;
import com.aircandi.ui.base.BaseEntityList.ListMode;
import com.aircandi.ui.user.UserForm;
import com.aircandi.utilities.Animate.TransitionType;

public final class Routing {

	public static void shortcut(final Activity activity, Shortcut shortcut, Entity hostEntity) {

		Tracker.sendEvent("ui_action", "browse_source", shortcut.app, 0, Aircandi.getInstance().getUser());
		if (shortcut.app.equals(Constants.TYPE_APPLINK_TWITTER)) {
			AndroidManager.getInstance().callTwitterActivity(activity, (shortcut.appId != null) ? shortcut.appId : shortcut.appUrl);
		}
		else if (shortcut.app.equals(Constants.TYPE_APPLINK_FOURSQUARE)) {
			AndroidManager.getInstance().callFoursquareActivity(activity, (shortcut.appId != null) ? shortcut.appId : shortcut.appUrl);
		}
		else if (shortcut.app.equals(Constants.TYPE_APPLINK_FACEBOOK)) {
			AndroidManager.getInstance().callFacebookActivity(activity, (shortcut.appId != null) ? shortcut.appId : shortcut.appUrl);
		}
		else if (shortcut.app.equals(Constants.TYPE_APPLINK_MAP) && hostEntity != null) {
			Tracker.sendEvent("ui_action", "map_place", null, 0, Aircandi.getInstance().getUser());
			final AirLocation location = hostEntity.getLocation();
			AndroidManager.getInstance().callMapActivity(activity, String.valueOf(location.lat.doubleValue())
					, String.valueOf(location.lng.doubleValue())
					, hostEntity.name);
		}
		else if (shortcut.app.equals(Constants.TYPE_APPLINK_YELP)) {
			AndroidManager.getInstance().callYelpActivity(activity, shortcut.appId, shortcut.appUrl);
		}
		else if (shortcut.app.equals(Constants.TYPE_APPLINK_OPENTABLE)) {
			AndroidManager.getInstance().callOpentableActivity(activity, shortcut.appId, shortcut.appUrl);
		}
		else if (shortcut.app.equals(Constants.TYPE_APPLINK_WEBSITE)) {
			AndroidManager.getInstance().callBrowserActivity(activity, (shortcut.appUrl != null) ? shortcut.appUrl : shortcut.appId);
		}
		else if (shortcut.app.equals(Constants.TYPE_APPLINK_EMAIL)) {
			AndroidManager.getInstance().callSendToActivity(activity, shortcut.name, shortcut.appId, null, null);
		}
		else if (shortcut.app.equals(Constants.TYPE_APPLINK_COMMENT) && hostEntity != null) {
			final IntentBuilder intentBuilder = new IntentBuilder(activity, BaseEntityList.class)
					.setEntityId(hostEntity.id)
					.setListMode(ListMode.EntitiesForEntity)
					.setListItemResId(R.layout.temp_listitem_comment)
					.setListNewEnabled(true)
					.setListSchema(Constants.SCHEMA_ENTITY_COMMENT);

			final Intent intent = intentBuilder.create();
			activity.startActivity(intent);
			Animate.doOverridePendingTransition(activity, TransitionType.PageToPage);
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
			intentBuilder = new IntentBuilder(activity, BaseEntityList.class)
					.setEntityId(hostEntity.id)
					.setListMode(ListMode.EntitiesForEntity)
					.setListItemResId(R.layout.temp_listitem_candi)
					.setListNewEnabled(true)
					.setListSchema(Constants.SCHEMA_ENTITY_POST);

			final Intent intent = intentBuilder.create();
			activity.startActivity(intent);
			Animate.doOverridePendingTransition(activity, TransitionType.PageToPage);
		}
		else if (shortcut.app.equals(Constants.SCHEMA_ENTITY_PLACE) && hostEntity != null) {
			IntentBuilder intentBuilder = null;
			intentBuilder = new IntentBuilder(activity, BaseEntityList.class)
					.setEntityId(hostEntity.id)
					.setListMode(ListMode.EntitiesForEntity)
					.setListItemResId(R.layout.temp_listitem_candi)
					.setListNewEnabled(true)
					.setListSchema(Constants.SCHEMA_ENTITY_POST);

			final Intent intent = intentBuilder.create();
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			activity.startActivity(intent);
			Animate.doOverridePendingTransition(activity, TransitionType.PageToPage);
		}
		else if (shortcut.app.equals(Constants.SCHEMA_ENTITY_USER) && hostEntity != null) {
			IntentBuilder intentBuilder = null;
			if (shortcut.group != null && shortcut.group.size() > 1) {
				intentBuilder = new IntentBuilder(activity, BaseEntityList.class)
						.setEntityId(hostEntity.id)
						.setListMode(ListMode.EntitiesForEntity)
						.setListItemResId(R.layout.temp_listitem_candi)
						.setListNewEnabled(true)
						.setListSchema(Constants.SCHEMA_ENTITY_USER);
			}
			else {
				intentBuilder = new IntentBuilder(activity, UserForm.class).setEntityId(shortcut.id);
			}

			final Intent intent = intentBuilder.create();
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			activity.startActivity(intent);
			Animate.doOverridePendingTransition(activity, TransitionType.PageToPage);
		}
		else {
			AndroidManager.getInstance().callGenericActivity(activity, (shortcut.appUrl != null) ? shortcut.appUrl : shortcut.appId);
		}
	}

	public static void serviceError(final Activity activity, ServiceResponse serviceResponse) {
		serviceError(activity, serviceResponse, null);
	}

	public static void serviceError(final Activity activity, ServiceResponse serviceResponse, ServiceOperation serviceOperation) {

		final ErrorType errorType = serviceResponse.exception.getErrorType();
		final String errorMessage = serviceResponse.exception.getMessage();
		final Float statusCode = serviceResponse.exception.getStatusCode();

		/* We always make sure the progress indicator has been stopped */
		activity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				((BaseActivity) activity).mBusyManager.hideBusy();
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
					final Intent intent = new Intent(activity, RadarForm.class);
					AirNotification airNotification = new AirNotification();
					airNotification.title = activity.getString(R.string.error_connection_none_notification_title);
					airNotification.subtitle = activity.getString(R.string.error_connection_none_notification_message);
					airNotification.intent = intent;
					airNotification.type = "network";
					NotificationManager.getInstance().showNotification(airNotification, activity);

					Dialogs.showAlertDialogSimple(activity, null, activity.getString(R.string.error_connection_none));
				}
				else if (serviceResponse.exception.getInnerException() instanceof WalledGardenException) {
					/*
					 * We have a connection but user is locked in a walled garden until they sign-in, pay, etc.
					 */
					Dialogs.showAlertDialogSimple(activity, null, activity.getString(R.string.error_connection_walled_garden));
				}
				else if (serviceResponse.exception.getInnerException() instanceof ConnectTimeoutException) {
					/*
					 * This exception signals that HttpClient is unable to establish a connection with the target server
					 * or proxy server within the given period of time.
					 */
					UI.showToastNotification(activity.getString(R.string.error_service_unavailable), Toast.LENGTH_SHORT);
				}
				else if (serviceResponse.exception.getInnerException() instanceof SocketTimeoutException) {
					/*
					 * We have a connection but got tired of waiting for data. Could be a
					 * poor connection or service is slow.
					 */
					UI.showToastNotification(activity.getString(R.string.error_connection_poor), Toast.LENGTH_SHORT);
				}
				else if (serviceResponse.exception.getInnerException() instanceof UnknownHostException) {
					/*
					 * We have a connection but got tired of waiting for data. Could be a
					 * poor connection or service is slow.
					 */
					Dialogs.showAlertDialogSimple(activity, null, activity.getString(R.string.error_client_unknown_host));
				}
				else if (serviceResponse.exception.getInnerException() instanceof ClientProtocolException) {
					/*
					 * Something wrong with the request. In most cases, this is a bug and
					 * not something that a user should cause unless they provided a bad uri.
					 */
					UI.showToastNotification(activity.getString(R.string.error_client_request_error), Toast.LENGTH_SHORT);
				}
				else {
					UI.showToastNotification(activity.getString(R.string.error_connection_poor), Toast.LENGTH_SHORT);
				}
			}
			else {
				/*
				 * Something wrong with the request. In most cases, this is a bug and
				 * not something that a user should cause unless they provided a bad uri.
				 */
				if (serviceResponse.exception.getInnerException() instanceof URISyntaxException) {
					UI.showToastNotification(activity.getString(R.string.error_client_request_error), Toast.LENGTH_SHORT);
				}
				else {
					/* Something without special handling */
					UI.showToastNotification(serviceResponse.exception.getMessage(), Toast.LENGTH_SHORT);
				}
			}
		}
		else if (errorType == ErrorType.Service) {

			if (serviceResponse.exception.getInnerException() instanceof HttpServiceException.NotFoundException) {
				/*
				 * Reached the service but requested something that doesn't exist. This is a bug and
				 * not something that a user should cause.
				 */
				UI.showToastNotification(activity.getString(R.string.error_client_request_not_found), Toast.LENGTH_SHORT);
			}
			else if (serviceResponse.exception.getInnerException() instanceof HttpServiceException.UnauthorizedException) {
				/*
				 * Reached the service but requested something that the user can't access.
				 */
				UI.showToastNotification(activity.getString(R.string.error_service_unauthorized), Toast.LENGTH_SHORT);
			}
			else if (serviceResponse.exception.getInnerException() instanceof HttpServiceException.ForbiddenException) {
				/*
				 * Reached the service but request was invalid per service policy.
				 */
				UI.showToastNotification(activity.getString(R.string.error_service_forbidden), Toast.LENGTH_SHORT);
			}
			else if (serviceResponse.exception.getInnerException() instanceof HttpServiceException.GatewayTimeoutException) {
				/*
				 * Reached the service but request was invalid per service policy.
				 */
				UI.showToastNotification(activity.getString(R.string.error_service_gateway_timeout), Toast.LENGTH_SHORT);
			}
			else if (serviceResponse.exception.getInnerException() instanceof HttpServiceException.ClientVersionException) {
				/*
				 * Reached the service but a more current client version is required.
				 */
				Aircandi.applicationUpdateRequired = true;
				final Intent intent = new Intent(activity, SplashForm.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				activity.startActivity(intent);
				activity.finish();
				Animate.doOverridePendingTransition(activity, TransitionType.FormToPage);
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
					UI.showToastNotification(activity.getString(R.string.error_service_unknown), Toast.LENGTH_SHORT);
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
						title = activity.getString(R.string.error_session_expired_title);
						message = activity.getString(R.string.error_session_expired);
						/*
						 * Make sure the user is logged out
						 */
						((BaseActivity) activity).signout();

					}
					else if (statusCode == ProxiConstants.HTTP_STATUS_CODE_UNAUTHORIZED_CREDENTIALS) {
						message = activity.getString(R.string.error_session_invalid);
						if (serviceOperation != null) {
							if (serviceOperation == ServiceOperation.PasswordChange) {
								message = activity.getString(R.string.error_change_password_unauthorized);
							}
							else if (serviceOperation == ServiceOperation.Signin) {
								message = activity.getString(R.string.error_signin_invalid_signin);
							}
						}
						else {
							((BaseActivity) activity).signout();
						}
					}
					else if (statusCode == ProxiConstants.HTTP_STATUS_CODE_UNAUTHORIZED_WHITELIST) {
						message = activity.getString(R.string.error_whitelist_unauthorized);
					}
					else if (statusCode == ProxiConstants.HTTP_STATUS_CODE_UNAUTHORIZED_UNVERIFIED) {
						message = activity.getString(R.string.error_unverified_unauthorized);
					}
					else if (statusCode == ProxiConstants.HTTP_STATUS_CODE_FORBIDDEN_USER_PASSWORD_WEAK) {
						message = activity.getString(R.string.error_signup_password_weak);
					}
					else if (statusCode == ProxiConstants.HTTP_STATUS_CODE_FORBIDDEN_DUPLICATE) {
						message = activity.getString(R.string.error_signup_email_taken);
					}
					else {
						UI.showToastNotification(serviceResponse.exception.getMessage(), Toast.LENGTH_SHORT);
					}
				}

				if (message != null) {
					Dialogs.showAlertDialogSimple(activity, title, message);
				}
			}
		}

		Logger.w(activity, "Service error: " + errorMessage);
	}

}