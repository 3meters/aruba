package com.aircandi.components;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ConnectException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiManager;

import com.aircandi.BuildConfig;
import com.aircandi.CandiConstants;
import com.aircandi.service.ProxibaseService;
import com.aircandi.service.ProxibaseServiceException;
import com.aircandi.service.ServiceRequest;

/**
 * Designed as a singleton. The private Constructor prevents any other class from instantiating.
 */

public class NetworkManager {

	private static NetworkManager		singletonObject;
	public static int					CONNECT_TRIES				= 10;
	public static int					CONNECT_WAIT				= 500;

	private Context						mApplicationContext;
	private final WifiStateChangedReceiver	mWifiStateChangedReceiver	= new WifiStateChangedReceiver();
	private Integer						mWifiState;
	private WifiManager					mWifiManager;
	private ConnectivityManager			mConnectivityManager;

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
	}

	public ServiceResponse request(ServiceRequest serviceRequest) {
		final ServiceResponse serviceResponse = request(serviceRequest, null);
		return serviceResponse;
	}

	private ServiceResponse request(ServiceRequest serviceRequest, ServiceResponse testServiceResponse) {
		if (testServiceResponse != null) {
			return testServiceResponse;
		}
		/*
		 * Don't assume this is being called from the UI thread.
		 */
		ServiceResponse serviceResponse = new ServiceResponse();
		if (AircandiCommon.mNotificationManager != null) {
			AircandiCommon.mNotificationManager.cancel(CandiConstants.NOTIFICATION_NETWORK);
		}

		/* Make sure we have a network connection */
		if (!verifyIsConnected()) {
			final Exception exception = new ConnectException();
			final ProxibaseServiceException proxibaseException = ProxibaseService.makeProxibaseServiceException(null, exception);
			serviceResponse = new ServiceResponse(ResponseCode.Failed, null, proxibaseException);
		}
		else {
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

	private boolean verifyIsConnected() {
		int attempts = 0;
		while (!NetworkManager.getInstance().isConnected()) {
			attempts++;
			Logger.v(this, "No network connection: attempt: " + String.valueOf(attempts));

			if (attempts >= CONNECT_TRIES) {
				return false;
			}
			try {
				Thread.sleep(CONNECT_WAIT);
			}
			catch (InterruptedException exception) {
				return false;
			}
		}
		return true;
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
	protected boolean isMobileNetwork() {
		/* Check if we're connected to a data network, and if so - if it's a mobile network. */
		final NetworkInfo activeNetwork = mConnectivityManager.getActiveNetworkInfo();
		final Boolean mobileNetwork = activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE;
		return mobileNetwork;
	}

	// --------------------------------------------------------------------------------------------
	// Wifi routines
	// --------------------------------------------------------------------------------------------

	public Boolean isWifiEnabled() {
		return mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED;
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
}
