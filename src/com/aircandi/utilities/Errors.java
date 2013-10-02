package com.aircandi.utilities;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.conn.ConnectTimeoutException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.R;
import com.aircandi.ServiceConstants;
import com.aircandi.components.NetworkManager;
import com.aircandi.service.ClientVersionException;
import com.aircandi.service.ServiceResponse;
import com.aircandi.ui.SplashForm;
import com.aircandi.ui.base.BaseActivity;
import com.aircandi.utilities.Animate.TransitionType;

public final class Errors {

	public static void handleError(final Activity activity, ServiceResponse serviceResponse) {

		final ErrorResponse errorResponse = serviceResponse.errorResponse;
		/*
		 * First show any required UI
		 */
		if (errorResponse.errorResponseType == ResponseType.DIALOG) {
			Aircandi.mainThreadHandler.post(new Runnable() {

				@Override
				public void run() {
					Dialogs.alertDialog(R.drawable.ic_launcher
							, null
							, errorResponse.errorMessage
							, null
							, activity
							, android.R.string.ok
							, null
							, null
							, null
							, null);
				}
			});
		}
		else if (errorResponse.errorResponseType == ResponseType.TOAST) {
			UI.showToastNotification(errorResponse.errorMessage, Toast.LENGTH_SHORT);
		}
		/*
		 * Perform any follow-up actions.
		 */
		if (errorResponse.signout) {
			BaseActivity.signout(activity, true);
		}
		else if (errorResponse.splash) {
			/*
			 * Mostly because a more current client version is required.
			 */
			Aircandi.applicationUpdateRequired = true;
			final Intent intent = new Intent(activity, SplashForm.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			activity.startActivity(intent);
			activity.finish();
			Animate.doOverridePendingTransition(activity, TransitionType.FORM_TO_PAGE);
		}
	}

	public static final ErrorResponse getErrorResponse(Context context, ServiceResponse serviceResponse) {

		if (serviceResponse.statusCode != null) {
			/*
			 * Status code based error
			 */
			if (serviceResponse.statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
				/*
				 * Reached the service with a good call but the service failed for an unknown reason. Examples
				 * are service bugs like missing indexes causing mongo queries to throw errors.
				 * 
				 * - 500: Something bad and unknown has happened in the service.
				 */
				if (Aircandi.settings.getBoolean(Constants.PREF_ENABLE_DEV, Constants.PREF_ENABLE_DEV_DEFAULT)
						&& Type.isTrue(Aircandi.getInstance().getUser().developer)) {
					return new ErrorResponse(ResponseType.TOAST, context.getString(R.string.error_service_unknown_status));
				}
				return new ErrorResponse(ResponseType.TOAST, context.getString(R.string.error_service_unknown));
			}
			else if (serviceResponse.statusCode == HttpStatus.SC_NOT_FOUND) {
				return new ErrorResponse(ResponseType.TOAST, context.getString(R.string.error_client_request_not_found));
			}
			else if (serviceResponse.statusCode == HttpStatus.SC_FORBIDDEN) {
				if (serviceResponse.statusCodeService != null) {
					if (serviceResponse.statusCodeService == ServiceConstants.HTTP_STATUS_CODE_FORBIDDEN_USER_PASSWORD_WEAK) {
						return new ErrorResponse(ResponseType.DIALOG, context.getString(R.string.error_signup_password_weak));
					}
					else if (serviceResponse.statusCodeService == ServiceConstants.HTTP_STATUS_CODE_FORBIDDEN_DUPLICATE) {
						return new ErrorResponse(ResponseType.DIALOG, context.getString(R.string.error_signup_email_taken));
					}
				}
			}
			else if (serviceResponse.statusCode == HttpStatus.SC_GATEWAY_TIMEOUT) {
				return new ErrorResponse(ResponseType.TOAST, context.getString(R.string.error_service_gateway_timeout));
			}
			else if (serviceResponse.statusCode == HttpStatus.SC_UNAUTHORIZED) {
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
				if (serviceResponse.statusCodeService != null) {
					if (serviceResponse.statusCodeService == ServiceConstants.HTTP_STATUS_CODE_UNAUTHORIZED_SESSION_EXPIRED) {
						ErrorResponse errorResponse = new ErrorResponse(ResponseType.DIALOG
								, context.getString(R.string.error_session_expired)
								, context.getString(R.string.error_session_expired_title));
						errorResponse.signout = true;
						return errorResponse;
					}
					else if (serviceResponse.statusCodeService == ServiceConstants.HTTP_STATUS_CODE_UNAUTHORIZED_CREDENTIALS) {
						if (serviceResponse.activityName != null) {
							if (serviceResponse.activityName.equals("PasswordEdit")) {
								return new ErrorResponse(ResponseType.DIALOG, context.getString(R.string.error_change_password_unauthorized));
							}
							else if (serviceResponse.activityName.equals("SignInEdit")) {
								return new ErrorResponse(ResponseType.DIALOG, context.getString(R.string.error_signin_invalid_signin));
							}
						}
						ErrorResponse errorResponse = new ErrorResponse(ResponseType.DIALOG, context.getString(R.string.error_session_invalid));
						errorResponse.signout = true;
						return errorResponse;
					}
					else if (serviceResponse.statusCodeService == ServiceConstants.HTTP_STATUS_CODE_UNAUTHORIZED_WHITELIST) {
						return new ErrorResponse(ResponseType.DIALOG, context.getString(R.string.error_whitelist_unauthorized));
					}
					else if (serviceResponse.statusCodeService == ServiceConstants.HTTP_STATUS_CODE_UNAUTHORIZED_UNVERIFIED) {
						return new ErrorResponse(ResponseType.DIALOG, context.getString(R.string.error_unverified_unauthorized));
					}
				}
			}
			return new ErrorResponse(ResponseType.TOAST, "Unhandled status error: " + serviceResponse.statusCode);
		}
		else {
			/*
			 * Exception based error
			 */
			Exception exception = serviceResponse.exception;
			if (exception instanceof ClientVersionException) {
				/*
				 * This gets returned by any network call to service where this aircandi version
				 * is not allowed to access the service api.
				 */
				ErrorResponse errorResponse = new ErrorResponse(ResponseType.DIALOG, context.getString(R.string.dialog_update_message));
				errorResponse.splash = true;
				return errorResponse;
			}
			else if (exception instanceof IOException) {
				/*
				 * We get an UnknownHostException when mobile data and wifi are is disabled. Also fails fast
				 * instead of waiting for socket/connection timeout.
				 * 
				 * In airplane mode, we spin until a socket/connection timeout drops us into here.
				 */
				if (!NetworkManager.getInstance().isConnected()) {
					if (NetworkManager.isAirplaneMode(context)) {
						return new ErrorResponse(ResponseType.DIALOG, context.getString(R.string.error_connection_airplane_mode));
					}
					else {
						return new ErrorResponse(ResponseType.DIALOG, context.getString(R.string.error_connection_none));
					}
				}
				else {
					/* We have a network connection so now check for a walled garden */
					if (!NetworkManager.getInstance().isMobileNetwork()) {
						if (NetworkManager.getInstance().isWalledGardenConnection()) {
							return new ErrorResponse(ResponseType.DIALOG, context.getString(R.string.error_connection_walled_garden));
						}
					}
				}

				/*
				 * Network request failed for some reason:
				 * 
				 * - ConnectException: Couldn't connect to the service host.
				 * - ConnectTimeoutException: Timeout trying to establish connection to service host.
				 * - SocketException: thrown during socket creation or setting options, we don't have a connection
				 * - SocketTimeoutException: Timeout trying to send/receive data to the service. Service might not be
				 * up.
				 * - WalledGardenException: have a connection but user was taken to a different host than requested
				 * - UnknownHostException: The ip address of the host could not be determined.
				 * - ClientProtocolException: malformed request and a bug.
				 * - NotFoundException: Reached service but requested something that isn't there.
				 * - UnauthorizedException: Reached service but user doesn't have needed permissions.
				 * - ForbiddenException: Reached service but request invalid per service policy.
				 * - GatewayTimeoutException: ?
				 * 
				 * Still left
				 * - NoHttpResponseException: target server failed to respond with a valid HTTP response
				 * - UnknownHostException: hostname didn't exist in the dns system
				 */
				if (exception instanceof ConnectTimeoutException) {
					return new ErrorResponse(ResponseType.TOAST, context.getString(R.string.error_service_unavailable));
				}
				else if (exception instanceof SocketTimeoutException) {
					return new ErrorResponse(ResponseType.TOAST, context.getString(R.string.error_connection_poor));
				}
				if (exception instanceof ConnectException) {
					return new ErrorResponse(ResponseType.TOAST, context.getString(R.string.error_service_unavailable));
				}
				else if (exception instanceof SocketException) {
					return new ErrorResponse(ResponseType.DIALOG, context.getString(R.string.error_service_unavailable));
				}
				else if (exception instanceof UnknownHostException) {
					return new ErrorResponse(ResponseType.DIALOG, context.getString(R.string.error_client_unknown_host));
				}
				else if (exception instanceof FileNotFoundException) {
					return new ErrorResponse(ResponseType.DIALOG, context.getString(R.string.error_service_file_not_found));
				}
				else if (exception instanceof ClientProtocolException) {
					return new ErrorResponse(ResponseType.TOAST, context.getString(R.string.error_client_request_error));
				}
				else if (exception instanceof EOFException) {
					return new ErrorResponse(ResponseType.TOAST, context.getString(R.string.error_client_request_stream_error));
				}
				else {
					if (Aircandi.settings.getBoolean(Constants.PREF_ENABLE_DEV, Constants.PREF_ENABLE_DEV_DEFAULT)
							&& Type.isTrue(Aircandi.getInstance().getUser().developer)) {
						return new ErrorResponse(ResponseType.TOAST, context.getString(R.string.error_service_unknown_exception) + ": "
								+ exception.getClass().getSimpleName());
					}
					return new ErrorResponse(ResponseType.TOAST, context.getString(R.string.error_service_unknown));
				}
			}
			return new ErrorResponse(ResponseType.TOAST, exception.getMessage());
		}
	}

	public static Boolean isNetworkError(ServiceResponse serviceResponse) {
		return (serviceResponse.statusCode == null && serviceResponse.exception != null && serviceResponse.exception instanceof IOException);
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------

	public static enum ResponseType {
		TOAST,
		DIALOG,
		NONE
	}

	public static class ErrorResponse {
		public String		errorMessage;
		public String		errorTitle;
		public ResponseType	errorResponseType;
		public Boolean		signout	= false;
		public Boolean		splash	= false;

		public ErrorResponse(ResponseType responseType) {
			this(responseType, null);
		}

		public ErrorResponse(ResponseType responseType, String errorMessage) {
			this(responseType, errorMessage, null);
		}

		public ErrorResponse(ResponseType responseType, String errorMessage, String errorTitle) {
			this.errorMessage = errorMessage;
			this.errorTitle = errorTitle;
			this.errorResponseType = responseType;
		}
	}
}