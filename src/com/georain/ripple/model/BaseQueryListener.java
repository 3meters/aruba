package com.georain.ripple.model;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import org.apache.http.client.ClientProtocolException;
import com.georain.ripple.model.RippleRunner.QueryListener;
import android.util.Log;

/**
 * Skeleton base class for QueryListeners, providing default error handling. Applications should handle these error
 * conditions.
 */
public abstract class BaseQueryListener implements QueryListener
{

	public void onClientProtocolException(ClientProtocolException e)
	{
		Log.e("Ripple", e.getMessage());
		e.printStackTrace();
	}

	public void onRippleError(RippleError e)
	{
		Log.e("Ripple", e.getMessage());
		e.printStackTrace();
	}

	public void onFileNotFoundException(FileNotFoundException e)
	{
		Log.e("Ripple", e.getMessage());
		e.printStackTrace();
	}

	public void onIOException(IOException e)
	{
		Log.e("Ripple", e.getMessage());
		e.printStackTrace();
	}

	public void onMalformedURLException(MalformedURLException e)
	{
		Log.e("Ripple", e.getMessage());
		e.printStackTrace();
	}

	public void onURISyntaxException(URISyntaxException e)
	{
		Log.e("Ripple", e.getMessage());
		e.printStackTrace();
	}
}
