package com.georain.ripple.model;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import org.apache.http.client.ClientProtocolException;
import android.os.Bundle;
import android.util.Log;
import com.facebook.android.FacebookRunner.RequestListener;
import com.georain.ripple.model.RippleService.QueryFormat;

/**
 * Implementation of asynchronous API requests. This class provides the ability to execute API methods and have the call
 * return immediately, without blocking the calling thread. This is necessary when accessing the API in the UI thread,
 * for instance. The request response is returned to the caller via a callback interface, which the developer must
 * implement. This implementation simply spawns a new thread for each request, and makes the API call immediately.
 * 
 * @see RequestListener The callback interface.
 * @author ssoneff@facebook.com
 */
public class RippleRunner
{
	public final RippleService	rippleService;

	public RippleRunner() {
		this.rippleService = new RippleService();
	}

	public void select(final Query query, final Class objClass, final QueryListener listener)
	{
		new Thread() {
			@Override
			public void run()
			{
				try
				{
					Log.d("Ripple", "RippleRunner: starting thread for ripple select query: '" + query.queryString() + "'");
					String response = rippleService.select(query, objClass);
					listener.onComplete(response);
				}
				catch (ClientProtocolException e)
				{
					listener.onClientProtocolException(e);
				}
				catch (IOException e)
				{
					listener.onIOException(e);
				}
			}
		}.start();
	}

	public void insert(final Object object, final String entityName, final ModifyListener listener)
	{
		new Thread() {
			@Override
			public void run()
			{
				try
				{
					rippleService.insert(object, entityName);
					listener.onComplete();
				}
				catch (ClientProtocolException e)
				{
					listener.onClientProtocolException(e);
				}
				catch (IOException e)
				{
					listener.onIOException(e);
				}
				catch (URISyntaxException e)
				{
					listener.onURISyntaxException(e);
				}
				catch (RippleError e)
				{
					listener.onRippleError(e);
				}
			}
		}.start();
	}

	public void update(final Object object, final String uriString, final ModifyListener listener)
	{
		new Thread() {
			@Override
			public void run()
			{
				try
				{
					rippleService.update(object, uriString);
					listener.onComplete();
				}
				catch (ClientProtocolException e)
				{
					listener.onClientProtocolException(e);
				}
				catch (IOException e)
				{
					listener.onIOException(e);
				}
				catch (URISyntaxException e)
				{
					listener.onURISyntaxException(e);
				}
				catch (RippleError e)
				{
					listener.onRippleError(e);
				}
			}
		}.start();
	}

	public void delete(final String uriString, final ModifyListener listener)
	{
		new Thread() {
			@Override
			public void run()
			{
				try
				{
					rippleService.delete(uriString);
					listener.onComplete();
				}
				catch (ClientProtocolException e)
				{
					listener.onClientProtocolException(e);
				}
				catch (IOException e)
				{
					listener.onIOException(e);
				}
				catch (URISyntaxException e)
				{
					listener.onURISyntaxException(e);
				}
				catch (RippleError e)
				{
					listener.onRippleError(e);
				}
			}
		}.start();
	}

	public void get(final String url, final QueryFormat queryFormat, final QueryListener listener)
	{
		new Thread() {
			@Override
			public void run()
			{
				try
				{
					String response = rippleService.getString(url, queryFormat);
					listener.onComplete(response);
				}
				catch (ClientProtocolException e)
				{
					listener.onClientProtocolException(e);
				}
				catch (IOException e)
				{
					listener.onIOException(e);
				}
			}
		}.start();
	}

	public void post(final String methodName, final Bundle parameters, final QueryFormat queryFormat, final QueryListener listener)
	{
		new Thread() {
			@Override
			public void run()
			{
				try
				{
					String response = rippleService.post(methodName, parameters, queryFormat);
					if (listener != null)
						listener.onComplete(response);
				}
				catch (ClientProtocolException e)
				{
					if (listener != null)
						listener.onClientProtocolException(e);
				}
				catch (IOException e)
				{
					if (listener != null)
						listener.onIOException(e);
				}
				catch (URISyntaxException e)
				{
					if (listener != null)
						listener.onURISyntaxException(e);
				}
			}
		}.start();
	}

	/**
	 * Callback interface for Ripple query requests.
	 */
	public static interface QueryListener
	{
		/**
		 * Called when a request completes with the given response. Executed by a background thread: do not update the
		 * UI in this method.
		 */
		public void onComplete(String response);

		/**
		 * Called when a request has a network or request e. Executed by a background thread: do not update the UI in
		 * this method.
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
		 * Called when the server-side Ripple method fails. Executed by a background thread: do not update the UI in
		 * this method.
		 */
		public void onRippleError(RippleError e);

		/**
		 * Called when the server-side Ripple method fails. Executed by a background thread: do not update the UI in
		 * this method.
		 */
		public void onClientProtocolException(ClientProtocolException e);
		
		/**
		 * Called if an invalid graph path is provided (which may result in a malformed URL). Executed by a background
		 * thread: do not update the UI in this method.
		 */
		public void onURISyntaxException(URISyntaxException e);
	}

	/**
	 * Callback interface for Ripple insert/update/delete requests.
	 */
	public static interface ModifyListener
	{
		/**
		 * Called when a request completes with the given response. Executed by a background thread: do not update the
		 * UI in this method.
		 */
		public void onComplete();

		/**
		 * Called when a request has a network or request e. Executed by a background thread: do not update the UI in
		 * this method.
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
		 * Called when the server-side Ripple method fails. Executed by a background thread: do not update the UI in
		 * this method.
		 */
		public void onRippleError(RippleError e);

		/**
		 * Called when the server-side Ripple method fails. Executed by a background thread: do not update the UI in
		 * this method.
		 */
		public void onClientProtocolException(ClientProtocolException e);

		/**
		 * Called when the uri syntax is incorrect. Executed by a background thread: do not update the UI in this
		 * method.
		 */
		public void onURISyntaxException(URISyntaxException e);
	}

}