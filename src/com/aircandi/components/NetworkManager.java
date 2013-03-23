package com.aircandi.components;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiManager;
import android.widget.Toast;

import com.aircandi.CandiConstants;
import com.aircandi.ProxiConstants;
import com.aircandi.beta.BuildConfig;
import com.aircandi.service.ProxibaseService;
import com.aircandi.service.ProxibaseServiceException;
import com.aircandi.service.ServiceRequest;
import com.aircandi.service.WalledGardenException;
import com.aircandi.utilities.ImageUtils;

/**
 * Designed as a singleton. The private Constructor prevents any other class from instantiating.
 */

public class NetworkManager {

	private static NetworkManager			singletonObject;

	/* monitor platform changes */
	private IntentFilter					mNetworkStateChangedFilter;
	@SuppressWarnings("unused")
	private BroadcastReceiver				mNetworkStateIntentReceiver;

	private Context							mApplicationContext;
	private final WifiStateChangedReceiver	mWifiStateChangedReceiver	= new WifiStateChangedReceiver();
	private Integer							mWifiState;
	private WifiManager						mWifiManager;
	private ConnectivityManager				mConnectivityManager;
	private ConnectedState					mConnectedState				= ConnectedState.Normal;

	public static synchronized NetworkManager getInstance() {
		if (singletonObject == null) {
			singletonObject = new NetworkManager();
		}
		return singletonObject;
	}

	private NetworkManager() {}

	public void setContext(Context applicationContext) {
		mApplicationContext = applicationContext;
	}

	public void initialize() {
		mWifiManager = (WifiManager) mApplicationContext.getSystemService(Context.WIFI_SERVICE);
		mConnectivityManager = (ConnectivityManager) mApplicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		mApplicationContext.registerReceiver(mWifiStateChangedReceiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));

		/*
		 * Enables registration for changes in network status from http stack
		 */
		mNetworkStateChangedFilter = new IntentFilter();
		mNetworkStateChangedFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		mNetworkStateIntentReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(final Context context, final Intent intent) {
				if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {

					final NetworkInfo wifi = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
					final NetworkInfo mobile = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
					boolean noConnection = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);

					if (noConnection) {
						ImageUtils.showToastNotification("Lost network connection", Toast.LENGTH_SHORT);
					}
					if (wifi.isAvailable()) {
						ImageUtils.showToastNotification("Wifi network is available", Toast.LENGTH_SHORT);
					}
					if (mobile.isAvailable()) {
						ImageUtils.showToastNotification("Mobile network is available", Toast.LENGTH_SHORT);
					}
				}
			}
		};
		//mApplicationContext.registerReceiver(mNetworkStateIntentReceiver, mNetworkStateChangedFilter);
	}

	public ServiceResponse request(ServiceRequest serviceRequest) {
		/*
		 * Don't assume this is being called from the UI thread.
		 */
		ServiceResponse serviceResponse = new ServiceResponse();
		if (AircandiCommon.mNotificationManager != null) {
			AircandiCommon.mNotificationManager.cancel(CandiConstants.NOTIFICATION_NETWORK);
		}

		/* Determine what kind of network connection we have */
		checkConnectedState();

		if (mConnectedState == ConnectedState.None) {
			final Exception exception = new ConnectException();
			final ProxibaseServiceException proxibaseException = ProxibaseService.makeProxibaseServiceException(null, exception);
			serviceResponse = new ServiceResponse(ResponseCode.Failed, null, proxibaseException);
		}
		else if (mConnectedState == ConnectedState.WalledGarden) {
			final Exception exception = new WalledGardenException();
			final ProxibaseServiceException proxibaseException = ProxibaseService.makeProxibaseServiceException(null, exception);
			serviceResponse = new ServiceResponse(ResponseCode.Failed, null, proxibaseException);
		}
		else if (mConnectedState == ConnectedState.Normal) {
			/*
			 * We have a network connection so give it a try. Request processing
			 * will retry using an exponential backoff scheme if needed and possible.
			 */
			try {
				/* Could be string, input stream, or array of bytes */
				final Object response = ProxibaseService.getInstance().request(serviceRequest);
				serviceResponse = new ServiceResponse(ResponseCode.Success, response, null);
			}
			catch (ProxibaseServiceException exception) {
				/*
				 * We got a service side error that either stopped us in our tracks or
				 * we gave up after performing a series of retries.
				 */
				serviceResponse = new ServiceResponse(ResponseCode.Failed, null, exception);
			}
		}
		return serviceResponse;
	}

	// --------------------------------------------------------------------------------------------
	// Connectivity routines
	// --------------------------------------------------------------------------------------------

	public ConnectedState checkConnectedState() {
		int attempts = 0;

		/*
		 * We create a little time for a connection process to complete
		 * Max attempt time = CONNECT_TRIES * CONNECT_WAIT
		 */
		ConnectedState connectedState = ConnectedState.Normal;
		while (!NetworkManager.getInstance().isConnected()) {
			attempts++;
			Logger.v(this, "No network connection: attempt: " + String.valueOf(attempts));

			if (attempts >= ProxiConstants.CONNECT_TRIES) {
				connectedState = ConnectedState.None;
				break;
			}
			try {
				Thread.sleep(ProxiConstants.CONNECT_WAIT);
			}
			catch (InterruptedException exception) {
				connectedState = ConnectedState.None;
				break;
			}
		}

		/* We have a network connection so now check for a walled garden */
		if (isWalledGardenConnection()) {
			connectedState = ConnectedState.WalledGarden;
		}

		synchronized (mConnectedState) {
			mConnectedState = connectedState;
		}
		return mConnectedState;
	}

	private boolean isConnected() {
		if (mApplicationContext != null) {
			if (mConnectivityManager != null) {
				final NetworkInfo[] info = mConnectivityManager.getAllNetworkInfo();
				if (info != null) {
					for (int i = 0; i < info.length; i++) {
						if (info[i].getState() == State.CONNECTED) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	@SuppressWarnings("unused")
	private boolean isConnectedOrConnecting() {
		final ConnectivityManager cm = (ConnectivityManager) mApplicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm != null) {
			final NetworkInfo[] info = cm.getAllNetworkInfo();
			if (info != null) {
				for (int i = 0; i < info.length; i++) {
					if (info[i].getState() == State.CONNECTED || info[i].getState() == State.CONNECTING) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@SuppressWarnings("ucd")
	protected Boolean isMobileNetwork() {
		/* Check if we're connected to a data network, and if so - if it's a mobile network. */
		Boolean isMobileNetwork = null;
		if (mConnectivityManager != null) {
			final NetworkInfo activeNetwork = mConnectivityManager.getActiveNetworkInfo();
			isMobileNetwork = activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE;
		}
		return isMobileNetwork;
	}

	/**
	 * 
	 * @return Based on ConnectivityManager.TYPE_*. Can return null.
	 */
	@SuppressWarnings("ucd")
	protected Integer getNetworkTypeId() {
		/* Check if we're connected to a data network, and if so - if it's a mobile network. */
		Integer networkTypeId = null;
		if (mConnectivityManager != null) {
			final NetworkInfo activeNetwork = mConnectivityManager.getActiveNetworkInfo();
			if (activeNetwork != null) {
				networkTypeId = activeNetwork.getType();
			}
		}
		return networkTypeId;
	}

	public boolean isWalledGardenConnection() {
		HttpURLConnection urlConnection = null;
		try {
			URL url = new URL(ProxiConstants.WALLED_GARDEN_URI);
			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setInstanceFollowRedirects(false);
			urlConnection.setConnectTimeout(ProxiConstants.WALLED_GARDEN_SOCKET_TIMEOUT_MS);
			urlConnection.setReadTimeout(ProxiConstants.WALLED_GARDEN_SOCKET_TIMEOUT_MS);
			urlConnection.setUseCaches(false);
			urlConnection.getInputStream();
			// We got a valid response, but not from the real google
			return urlConnection.getResponseCode() != 204;
		}
		catch (IOException e) {
			if (BuildConfig.DEBUG) {
				Logger.w(this, "Walled garden check - probably not a portal: exception " + e);
			}
			return false;
		}
		finally {
			if (urlConnection != null) {
				urlConnection.disconnect();
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Wifi routines
	// --------------------------------------------------------------------------------------------

	public Boolean isWifiEnabled() {
		Boolean wifiEnabled = null;
		if (mWifiManager != null) {
			wifiEnabled = mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED;
		}
		return wifiEnabled;
	}

	public boolean isWifiTethered() {
		/*
		 * We use reflection because the method is hidden and unpublished.
		 */
		Boolean isTethered = false;
		if (mWifiManager != null) {
			final Method[] wmMethods = mWifiManager.getClass().getDeclaredMethods();
			for (Method method : wmMethods) {
				if (method.getName().equals("isWifiApEnabled")) {
					try {
						isTethered = (Boolean) method.invoke(mWifiManager);
					}
					catch (IllegalArgumentException e) {
						if (BuildConfig.DEBUG) {
							e.printStackTrace();
						}
					}
					catch (IllegalAccessException e) {
						if (BuildConfig.DEBUG) {
							e.printStackTrace();
						}
					}
					catch (InvocationTargetException e) {
						if (BuildConfig.DEBUG) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		return isTethered;
	}

	// --------------------------------------------------------------------------------------------
	// Inner classes
	// --------------------------------------------------------------------------------------------

	public Integer getWifiState() {
		return mWifiState;
	}

	private void setWifiState(Integer wifiState) {
		mWifiState = wifiState;
	}

	public ConnectedState getConnectedState() {
		return mConnectedState;
	}

	private class WifiStateChangedReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(final Context context, Intent intent) {

			/* This is on the main UI thread */
			setWifiState(intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN));

			if (getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
				Logger.d(this, "Wifi state enabled");
			}
			else if (getWifiState() == WifiManager.WIFI_STATE_ENABLING) {
				Logger.d(this, "Wifi state enabling");
			}
			else {
				switch (getWifiState()) {
					case WifiManager.WIFI_STATE_DISABLED:
						Logger.d(this, "Wifi state disabled");
						break;
					case WifiManager.WIFI_STATE_DISABLING:
						Logger.d(this, "Wifi state disabling");
						break;
					case WifiManager.WIFI_STATE_UNKNOWN:
						Logger.d(this, "Wifi state unknown");
						break;
					default:
						return;
				}
			}
			BusProvider.getInstance().post(new WifiChangedEvent(getWifiState()));
		}
	}

	@SuppressWarnings("ucd")
	public static class ServiceResponse {

		public Object						data;
		public ResponseCode					responseCode	= ResponseCode.Success;

		public ProxibaseServiceException	exception;

		public ServiceResponse() {}

		public ServiceResponse(ResponseCode resultCode, Object data, ProxibaseServiceException exception) {
			responseCode = resultCode;
			this.data = data;
			this.exception = exception;
		}
	}

	public static enum ResponseCode {
		Success, Failed
	}

	public static enum ConnectedState {
		None, Normal, WalledGarden,
	}
}
