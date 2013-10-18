package com.aircandi.components;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import com.aircandi.BuildConfig;
import com.aircandi.Constants;
import com.aircandi.ServiceConstants;
import com.aircandi.service.BaseConnection;
import com.aircandi.service.OkHttpUrlConnection;
import com.aircandi.service.RequestType;
import com.aircandi.service.ResponseFormat;
import com.aircandi.service.ServiceRequest;
import com.aircandi.service.ServiceResponse;
import com.aircandi.utilities.Errors;
import com.aircandi.utilities.Reporting;
import com.aircandi.utilities.UI;

/**
 * Designed as a singleton. The private Constructor prevents any other class from instantiating.
 */

@SuppressWarnings("unused")
public class NetworkManager {
	/*
	 * Http 1.1 Status Codes (subset)
	 * 
	 * - 200: OK
	 * - 201: Created
	 * - 202: Accepted
	 * - 203: Non-authoritative information
	 * - 204: Request fulfilled but no content returned (message body empty).
	 * - 3xx: Redirection
	 * - 400: Bad request. Malformed syntax.
	 * - 401: Unauthorized. Request requires user authentication.
	 * - 403: Forbidden
	 * - 404: Not found
	 * - 405: Method not allowed
	 * - 408: Request timeout
	 * - 415: Unsupported media type
	 * - 500: Internal server error
	 * - 503: Service unavailable. Caused by temporary overloading or maintenance.
	 * 
	 * Notes:
	 * 
	 * - We get a 403 from amazon when trying to fetch something from S3 that isn't there.
	 */

	/*
	 * Timeouts
	 * 
	 * - Connection timeout is the max time allowed to make initial connection with the remote server.
	 * - Sockettimeout is the max inactivity time allowed between two consecutive data packets.
	 * - AndroidHttpClient sets both to 60 seconds.
	 */

	/*
	 * Exceptions when executing HTTP methods using HttpClient
	 * 
	 * - IOException: Generic transport exceptions (unreliable connection, socket timeout, generally non-fatal.
	 * ClientProtocolException, SocketException and InterruptedIOException are sub classes of IOException.
	 * 
	 * - HttpException: Protocol exceptions. These tend to be fatal and suggest something fundamental is wrong with the
	 * request such as a violation of the http protocol.
	 */

	/* monitor platform changes */
	private IntentFilter					mNetworkStateChangedFilter;
	private BroadcastReceiver				mNetworkStateIntentReceiver;

	private Context							mApplicationContext;
	private final WifiStateChangedReceiver	mWifiStateChangedReceiver		= new WifiStateChangedReceiver();
	private Integer							mWifiState;
	private Integer							mWifiApState;
	private WifiManager						mWifiManager;
	private ConnectivityManager				mConnectivityManager;
	private ConnectedState					mConnectedState					= ConnectedState.NORMAL;
	private BaseConnection					mConnection;

	public static final String				EXTRA_WIFI_AP_STATE				= "wifi_state";
	public static final String				WIFI_AP_STATE_CHANGED_ACTION	= "android.net.wifi.WIFI_AP_STATE_CHANGED";
	public static final int					WIFI_AP_STATE_ENABLED			= 3;

	private NetworkManager() {
		mConnection = new OkHttpUrlConnection();
	}

	private static class NetworkManagerHolder {
		public static final NetworkManager	instance	= new NetworkManager();
	}

	public static NetworkManager getInstance() {
		return NetworkManagerHolder.instance;

	}

	public void setContext(Context applicationContext) {
		mApplicationContext = applicationContext;
	}

	public void initialize() {

		mWifiManager = (WifiManager) mApplicationContext.getSystemService(Context.WIFI_SERVICE);
		mConnectivityManager = (ConnectivityManager) mApplicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);

		/*
		 * Setting system properties needed for HttpUrlConnection
		 */
		System.setProperty("http.maxConnections", String.valueOf(ServiceConstants.DEFAULT_MAX_CONNECTIONS));
		System.setProperty("http.keepAlive", Constants.SUPPORTS_FROYO ? "true" : "false");

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		intentFilter.addAction(WIFI_AP_STATE_CHANGED_ACTION);
		mApplicationContext.registerReceiver(mWifiStateChangedReceiver, intentFilter);
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
						UI.showToastNotification("Lost network connection", Toast.LENGTH_SHORT);
					}
					if (wifi.isAvailable()) {
						UI.showToastNotification("Wifi network is available", Toast.LENGTH_SHORT);
					}
					if (mobile.isAvailable()) {
						UI.showToastNotification("Mobile network is available", Toast.LENGTH_SHORT);
					}
				}
			}
		};
	}

	public ServiceResponse request(final ServiceRequest serviceRequest, final Stopwatch stopwatch) {
		ServiceResponse serviceResponse = (ServiceResponse) mConnection.request(serviceRequest, stopwatch);

		if (serviceResponse.responseCode == ResponseCode.FAILED) {
			serviceResponse.errorResponse = Errors.getErrorResponse(mApplicationContext, serviceResponse);
			if (stopwatch != null) {
				stopwatch.segmentTime("Service call failed");
			}
			if (serviceResponse.exception != null) {
				Logger.w(this, "Service exception: " + serviceResponse.exception.getClass().getSimpleName());
				Logger.w(this, "Service exception: " + serviceResponse.exception.getLocalizedMessage());
			}
			else {
				Logger.w(this, "Service error: (code: " + String.valueOf(serviceResponse.statusCode) + ") " + serviceResponse.statusMessage);
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
		ConnectedState connectedState = ConnectedState.NORMAL;
		while (!NetworkManager.getInstance().isConnected()) {
			attempts++;
			Logger.v(this, "No network connection: attempt: " + String.valueOf(attempts));

			if (attempts >= ServiceConstants.CONNECT_TRIES) {
				connectedState = ConnectedState.NONE;
				break;
			}
			try {
				Thread.sleep(ServiceConstants.CONNECT_WAIT);
			}
			catch (InterruptedException exception) {
				connectedState = ConnectedState.NONE;
				break;
			}
		}

		/* We have a network connection so now check for a walled garden */
		if (!isMobileNetwork()) {
			if (isWalledGardenConnection()) {
				connectedState = ConnectedState.WALLED_GARDEN;
			}
		}

		synchronized (mConnectedState) {
			mConnectedState = connectedState;
		}
		return mConnectedState;
	}

	@SuppressWarnings("deprecation")
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	public static boolean isAirplaneMode(Context context) {
		ContentResolver cr = context.getContentResolver();
		if (Constants.SUPPORTS_JELLY_BEAN_MR1) {
			return Settings.Global.getInt(cr, Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
		}
		else {
			return Settings.System.getInt(cr, Settings.System.AIRPLANE_MODE_ON, 0) != 0;
		}
	}

	public boolean isConnected() {
		NetworkInfo activeNetwork = mConnectivityManager.getActiveNetworkInfo();
		return (activeNetwork != null && activeNetwork.isConnectedOrConnecting());
	}

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
	public Boolean isMobileNetwork() {
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
	public Integer getNetworkTypeId() {
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

	@SuppressWarnings("ucd")
	public String getNetworkType() {
		/* Check if we're connected to a data network, and if so - if it's a mobile network. */
		Integer networkTypeId = null;
		if (mConnectivityManager != null) {
			final NetworkInfo activeNetwork = mConnectivityManager.getActiveNetworkInfo();
			if (activeNetwork != null) {
				networkTypeId = activeNetwork.getType();
			}
		}
		return getNetworkTypeLabel(networkTypeId);
	}

	public boolean isWalledGardenConnection() {

		final ServiceRequest serviceRequest = new ServiceRequest()
				.setUri(ServiceConstants.WALLED_GARDEN_URI)
				.setRequestType(RequestType.GET)
				.setResponseFormat(ResponseFormat.NONE);

		final ServiceResponse serviceResponse = NetworkManager.getInstance().request(serviceRequest, null);
		if (serviceResponse.responseCode == ResponseCode.SUCCESS) {
			return serviceResponse.statusCode != 204;
		}
		else {
			if (BuildConfig.DEBUG) {
				Logger.w(this, "Walled garden check - probably not a portal: exception " + serviceResponse.exception);
			}
			return false;
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

	private String getNetworkTypeLabel(Integer networkTypeId) {

		String networkTypeLabel = null;
		if (networkTypeId == null) {
			networkTypeLabel = "None";
		}
		else if (networkTypeId == 0) {
			networkTypeLabel = "Mobile";
		}
		else if (networkTypeId == 1) {
			networkTypeLabel = "Wifi";
		}
		else if (networkTypeId == 2) {
			networkTypeLabel = "Mobile MMS";
		}
		else if (networkTypeId == 3) {
			networkTypeLabel = "Mobile SUPL";
		}
		else if (networkTypeId == 4) {
			networkTypeLabel = "Mobile DUN";
		}
		else if (networkTypeId == 5) {
			networkTypeLabel = "Mobile HiPri";
		}
		else if (networkTypeId == 6) {
			networkTypeLabel = "WiMax";
		}
		else if (networkTypeId == 7) {
			networkTypeLabel = "Bluetooth";
		}
		else if (networkTypeId == 8) {
			networkTypeLabel = "Dummy";
		}
		else if (networkTypeId == 9) {
			networkTypeLabel = "Ethernet";
		}
		return networkTypeLabel;

	}

	// --------------------------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------------------------

	public Integer getWifiState() {
		return mWifiState;
	}

	public Integer getWifiApState() {
		return mWifiApState;
	}

	public ConnectedState getConnectedState() {
		return mConnectedState;
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------

	private class WifiStateChangedReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(final Context context, Intent intent) {

			/* This is on the main UI thread */
			String action = intent.getAction();
			if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
				mWifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
			}
			else if (action.equals(WIFI_AP_STATE_CHANGED_ACTION)) {
				mWifiApState = intent.getIntExtra(EXTRA_WIFI_AP_STATE, WifiManager.WIFI_STATE_UNKNOWN);
			}
			Reporting.updateCrashKeys();
		}
	}

	public static enum ResponseCode {
		SUCCESS,
		FAILED
	}

	public static enum ConnectedState {
		NONE,
		NORMAL,
		WALLED_GARDEN,
	}
}
