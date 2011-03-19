/*
 * Copyright 2010 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.android;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;

import com.facebook.android.Facebook.DialogListener;
import com.threemeters.aircandi.controller.Aircandi;
import com.threemeters.aircandi.utilities.Utilities;

/**
 * A sample implementation of asynchronous API requests. This class provides the ability to execute API methods and have
 * the call return immediately, without blocking the calling thread. This is necessary when accessing the API in the UI
 * thread, for instance. The request response is returned to the caller via a callback interface, which the developer
 * must implement. This sample implementation simply spawns a new thread for each request, and makes the API call
 * immediately. This may work in many applications, but more sophisticated users may re-implement this behavior using a
 * thread pool, a network thread, a request queue, or other mechanism. Advanced functionality could be built, such as
 * rate-limiting of requests, as per a specific application's needs.
 * 
 * @see RequestListener The callback interface.
 * @author ssoneff@facebook.com
 */
public class FacebookRunner
{
	public static final String		KEY			= "facebook-credentials";
	public static final String		APP_ID		= "107345299332080";
	public static final String[]	PERMISSIONS	= new String[] { "user_about_me", "user_status", "publish_stream", "read_stream", "offline_access" };
	public final Facebook			facebook;
	private final Context			mContextUi;
	private final Context			mContextCredentials;
	private DialogListener			mLoginListener;

	public FacebookRunner(Context contextUi, Context contextCredentials) {
		this.facebook = new Facebook();
		this.mContextUi = contextUi;
		this.mContextCredentials = contextCredentials;

		// Loads up the cached access token if we have one
		restoreCredentials(mContextCredentials, facebook);
	}

	/**
	 * Starts a dialog which prompts the user to log in to Facebook and grant the requested permissions to the given
	 * application. This method implements the OAuth 2.0 User-Agent flow to retrieve an access token for use in API
	 * requests. In this flow, the user credentials are handled by Facebook in an embedded WebView, not by the client
	 * application. As such, the dialog makes a network request and renders HTML content rather than a native UI. The
	 * access token is retrieved from a redirect to a special URL that the WebView handles. Note that User credentials
	 * could be handled natively using the OAuth 2.0 Username and Password Flow, but this is not supported by this SDK.
	 * See http://developers.facebook.com/docs/authentication/ and http://wiki.oauth.net/OAuth-2 for more details. Note
	 * that this method is asynchronous and the callback will be invoked in the original calling thread (not in a
	 * background thread). Also note that requests may be made to the API without calling authorize first, in which case
	 * only public information is returned.
	 * 
	 * @param context
	 *            The Android context in which we want to display the authorization dialog
	 * @param applicationId
	 *            The Facebook application identifier e.g. "350685531728"
	 * @param permissions
	 *            A list of permission required for this application: e.g. "read_stream", "publish_stream",
	 *            "offline_access", etc. see http://developers.facebook.com/docs/authentication/permissions This
	 *            parameter should not be null -- if you do not require any permissions, then pass in an empty String
	 *            array.
	 * @param listener
	 *            Callback interface for notifying the calling application when the dialog has completed, failed, or
	 *            been canceled.
	 */
	public void authorize(final DialogListener listener)
	{
		// Logout should have always be called before authorize if this is being reused.
		mLoginListener = listener;
		facebook.setAccessToken(null);
		facebook.setAccessExpires(0);
		facebook.authorize(mContextUi, FacebookRunner.APP_ID, FacebookRunner.PERMISSIONS, new LoginDialogListener());
	}

	private final class LoginDialogListener implements DialogListener
	{
		public void onComplete(Bundle values)
		{
			saveCredentials(mContextCredentials, facebook);
			mLoginListener.onComplete(values);
			mLoginListener = null;
		}

		public void onFacebookError(FacebookError error)
		{
			mLoginListener.onFacebookError(error);
		}

		public void onError(DialogError error)
		{
			mLoginListener.onError(error);
		}

		public void onCancel()
		{
			mLoginListener.onCancel();
		}
	}

	/**
	 * Invalidate the current user session by removing the access token in memory, clearing the browser cookies, and
	 * calling auth.expireSession through the API. The application will be notified when logout is complete via the
	 * callback interface. Note that this method is asynchronous and the callback will be invoked in a background
	 * thread; operations that affect the UI will need to be posted to the UI thread or an appropriate handler.
	 * 
	 * @param context
	 *            The Android context in which the logout should be called: it should be the same context in which the
	 *            login occurred in order to clear any stored cookies
	 * @param listener
	 *            Callback interface to notify the application when the request has completed.
	 */
	public void logout(final RequestListener listener)
	{
		new Thread() {
			@Override
			public void run()
			{
				try
				{
					// Parent clears cookies for the context, calls facebook graph, and clears the token
					// from the object but not from shared preferences. Call blocks waiting for network response
					// so should not be made on the main UI thread.
					String response = facebook.logout(mContextUi);
					if (response.length() == 0 || response.equals("false"))
					{
						listener.onFacebookError(new FacebookError("auth.expireSession failed"));
						return;
					}
					// Make sure we have cleared out token and expires info that is cached in
					// shared preferences
					clearCredentials(mContextCredentials);
					listener.onComplete(response);
				}
				catch (FileNotFoundException e)
				{
					listener.onFileNotFoundException(e);
				}
				catch (MalformedURLException e)
				{
					listener.onMalformedURLException(e);
				}
				catch (IOException e)
				{
					listener.onIOException(e);
				}
			}
		}.start();
	}

	/**
	 * Make a request to Facebook's old (pre-graph) API with the given parameters. One of the parameter keys must be
	 * "method" and its value should be a valid REST server API method. See
	 * http://developers.facebook.com/docs/reference/rest/ Note that this method is asynchronous and the callback will
	 * be invoked in a background thread; operations that affect the UI will need to be posted to the UI thread or an
	 * appropriate handler. Example: <code>
	 *  Bundle parameters = new Bundle();
	 *  parameters.putString("method", "auth.expireSession", new Listener());
	 *  String response = request(parameters);
     * </code>
	 * 
	 * @param parameters
	 *            Key-value pairs of parameters to the request. Refer to the documentation: one of the parameters must
	 *            be "method".
	 * @param listener
	 *            Callback interface to notify the application when the request has completed.
	 */
	public void request(Bundle parameters, RequestListener listener)
	{
		request(null, parameters, "GET", listener);
	}

	/**
	 * Make a request to the Facebook Graph API without any parameters. See http://developers.facebook.com/docs/api Note
	 * that this method is asynchronous and the callback will be invoked in a background thread; operations that affect
	 * the UI will need to be posted to the UI thread or an appropriate handler.
	 * 
	 * @param graphPath
	 *            Path to resource in the Facebook graph, e.g., to fetch data about the currently logged authenticated
	 *            user, provide "me", which will fetch http://graph.facebook.com/me
	 * @param listener
	 *            Callback interface to notify the application when the request has completed.
	 */
	public void request(String graphPath, RequestListener listener)
	{
		request(graphPath, new Bundle(), "GET", listener);
	}

	/**
	 * Make a request to the Facebook Graph API with the given string parameters using an HTTP GET (default method). See
	 * http://developers.facebook.com/docs/api Note that this method is asynchronous and the callback will be invoked in
	 * a background thread; operations that affect the UI will need to be posted to the UI thread or an appropriate
	 * handler.
	 * 
	 * @param graphPath
	 *            Path to resource in the Facebook graph, e.g., to fetch data about the currently logged authenticated
	 *            user, provide "me", which will fetch http://graph.facebook.com/me
	 * @param parameters
	 *            key-value string parameters, e.g. the path "search" with parameters "q" : "facebook" would produce a
	 *            query for the following graph resource: https://graph.facebook.com/search?q=facebook
	 * @param listener
	 *            Callback interface to notify the application when the request has completed.
	 */
	public void request(String graphPath, Bundle parameters, RequestListener listener)
	{
		request(graphPath, parameters, "GET", listener);
	}

	/**
	 * Make a request to the Facebook Graph API with the given HTTP method and string parameters. Note that binary data
	 * parameters (e.g. pictures) are not yet supported by this helper function. See
	 * http://developers.facebook.com/docs/api Note that this method is asynchronous and the callback will be invoked in
	 * a background thread; operations that affect the UI will need to be posted to the UI thread or an appropriate
	 * handler.
	 * 
	 * @param graphPath
	 *            Path to resource in the Facebook graph, e.g., to fetch data about the currently logged authenticated
	 *            user, provide "me", which will fetch http://graph.facebook.com/me
	 * @param parameters
	 *            key-value string parameters, e.g. the path "search" with parameters {"q" : "facebook"} would produce a
	 *            query for the following graph resource: https://graph.facebook.com/search?q=facebook
	 * @param httpMethod
	 *            http verb, e.g. "POST", "DELETE"
	 * @param listener
	 *            Callback interface to notify the application when the request has completed.
	 */
	public void request(final String graphPath, final Bundle parameters, final String httpMethod, final RequestListener listener)
	{
		new Thread() {
			@Override
			public void run()
			{
				try
				{
					Utilities.Log(Aircandi.APP_NAME, "FacebookRunner", "Starting thread for facebook graph request: '" + graphPath + "'");
					String resp = facebook.request(graphPath, parameters, httpMethod);
					listener.onComplete(resp);
				}
				catch (FileNotFoundException e)
				{
					listener.onFileNotFoundException(e);
				}
				catch (MalformedURLException e)
				{
					listener.onMalformedURLException(e);
				}
				catch (IOException e)
				{
					listener.onIOException(e);
				}
			}
		}.start();
	}
	
	public boolean isSessionValid()
	{
		return facebook.isSessionValid();
	}

	public boolean saveCredentials(Context contextCredentials, Facebook facebook)
	{
		Editor editor = contextCredentials.getSharedPreferences(KEY, Context.MODE_PRIVATE).edit();
		editor.putString(Facebook.TOKEN, facebook.getAccessToken());
		editor.putLong(Facebook.EXPIRES, facebook.getAccessExpires());
		return editor.commit();
	}

	public boolean restoreCredentials(Context contextCredentials, Facebook facebook)
	{
		SharedPreferences sharedPreferences = contextCredentials.getSharedPreferences(KEY, Context.MODE_PRIVATE);
		facebook.setAccessToken(sharedPreferences.getString(Facebook.TOKEN, null));
		facebook.setAccessExpires(sharedPreferences.getLong(Facebook.EXPIRES, 0));
		return facebook.isSessionValid();
	}

	private void clearCredentials(Context contextCredentials)
	{
		Editor editor = contextCredentials.getSharedPreferences(KEY, Context.MODE_PRIVATE).edit();
		editor.clear();
		editor.commit();
	}

	/**
	 * Callback interface for API requests.
	 */
	public static interface RequestListener
	{

		/**
		 * Called when a request completes with the given response. Executed by a background thread: do not update the
		 * UI in this method.
		 */
		public void onComplete(String response);

		/**
		 * Called when a request has a network or request error. Executed by a background thread: do not update the UI
		 * in this method.
		 */
		public void onIOException(IOException e);

		/**
		 * Called when a request fails because the requested resource is invalid or does not exist. Executed by a
		 * background thread: do not update the UI in this method.
		 */
		public void onFileNotFoundException(FileNotFoundException e);

		/**
		 * Called if an invalid graph path is provided (which may result in a malformed URL). Executed by a background
		 * thread: do not update the UI in this method.
		 */
		public void onMalformedURLException(MalformedURLException e);

		/**
		 * Called when the server-side Facebook method fails. Executed by a background thread: do not update the UI in
		 * this method.
		 */
		public void onFacebookError(FacebookError e);

	}

}
