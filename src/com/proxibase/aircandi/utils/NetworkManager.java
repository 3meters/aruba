package com.proxibase.aircandi.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.widget.Toast;

import com.proxibase.aircandi.R;
import com.proxibase.sdk.android.proxi.service.ProxibaseService;
import com.proxibase.sdk.android.proxi.service.ServiceRequest;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxiErrorCode;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.ProxibaseException;

public class NetworkManager {

	private static NetworkManager		singletonObject;

	private Context						mContext;
	private ConnectionReceiver			mConnectionReceiver			= new ConnectionReceiver();
	private WifiStateChangedReceiver	mWifiStateChangedReceiver	= new WifiStateChangedReceiver();
	private IConnectivityListener		mConnectivityListener;
	private WifiManager					mWifiManager;
	private boolean						mIsConnectedFailurePrevious	= false;

	public static synchronized NetworkManager getInstance() {
		if (singletonObject == null) {
			singletonObject = new NetworkManager();
		}
		return singletonObject;
	}

	/**
	 * Designed as a singleton. The private Constructor prevents any other class from instantiating.
	 */
	private NetworkManager() {}

	public void setContext(Context context) {
		mContext = context;
	}

	public boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		return cm.getActiveNetworkInfo().isConnectedOrConnecting();
	}

	public void reset() {
		mIsConnectedFailurePrevious = false;
	}

	public void initialize() {
		mContext.registerReceiver(mConnectionReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		mContext.registerReceiver(mWifiStateChangedReceiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
		mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
	}

	public void requestAsync(final ServiceRequest serviceRequest) {

		new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				ServiceResponse serviceResponse = request(serviceRequest);
				return serviceResponse;
			}

			@Override
			protected void onPostExecute(Object result) {
				ServiceResponse serviceResponse = (ServiceResponse) result;
				serviceRequest.getRequestListener().onComplete(serviceResponse);
			}

		}.execute();
	}

	public ServiceResponse request(ServiceRequest serviceRequest) {
		/*
		 * Don't assume this is being called from the UI thread.
		 */

		/* Make sure we have a network connection */
		if (!verifyIsConnected()) {
			if (!mIsConnectedFailurePrevious) {
				mIsConnectedFailurePrevious = true;
				if (!serviceRequest.isSuppressUI()) {
					ImageUtils.showToastNotification(R.string.network_message_connection_notready, Toast.LENGTH_LONG);
				}
			}
			Logger.d(this, "Connection exception: " + serviceRequest.getUri());
			return new ServiceResponse(ResultCode.Recoverable, ResultCodeDetail.ConnectionException, null, null);
		}

		/* We have a network connection so give it a try */
		try {
			Object response = ProxibaseService.getInstance().request(serviceRequest);
			return new ServiceResponse(ResultCode.Success, ResultCodeDetail.Success, response, null);
		}
		catch (ProxibaseException exception) {

			if (exception.getErrorCode() == ProxiErrorCode.IOException) {
				/* Recoverable if the connection improves */
				if (!serviceRequest.isSuppressUI()) {
					ImageUtils.showToastNotification(R.string.network_message_connection_poor, Toast.LENGTH_LONG);
				}
				Logger.d(this, "Transport exception: " + serviceRequest.getUri());
				return new ServiceResponse(ResultCode.Recoverable, ResultCodeDetail.TransportException, null, exception);

			}
			else if (exception.getErrorCode() == ProxiErrorCode.NotFoundException) {
				/* Recoverable if the service is down and on the way back */
				if (!serviceRequest.isSuppressUI()) {
					ImageUtils.showToastNotification(R.string.network_message_service_notready, Toast.LENGTH_LONG);
				}
				Logger.d(this, "Service not founr exception: " + serviceRequest.getUri());
				return new ServiceResponse(ResultCode.Recoverable, ResultCodeDetail.ServiceNotFoundException, null, exception);
			}
			else if (exception.getErrorCode() == ProxiErrorCode.AircandiServiceException) {
				/* Unrecoverable */
				Logger.d(this, "Service exception: " + serviceRequest.getUri());
				return new ServiceResponse(ResultCode.Unrecoverable, ResultCodeDetail.ServiceException, null, exception);
			}
			else if (exception.getErrorCode() == ProxiErrorCode.ClientProtocolException) {
				/* Unrecoverable */
				Exceptions.Handle(exception);
				Logger.d(this, "Protocol exception: " + serviceRequest.getUri());
				return new ServiceResponse(ResultCode.Unrecoverable, ResultCodeDetail.ProtocolException, null, exception);
			}
			else if (exception.getErrorCode() == ProxiErrorCode.URISyntaxException) {
				/* Unrecoverable */
				Exceptions.Handle(exception);
				Logger.d(this, "URI syntax exception: " + serviceRequest.getUri());
				return new ServiceResponse(ResultCode.Unrecoverable, ResultCodeDetail.ProtocolException, null, exception);
			}
			else {
				/* Unrecoverable */
				Exceptions.Handle(exception);
				Logger.d(this, "Unknown exception: " + serviceRequest.getUri());
				return new ServiceResponse(ResultCode.Unrecoverable, ResultCodeDetail.UnknownException, null, exception);
			}
		}
	}

	private boolean verifyIsConnected() {
		int attempts = 0;
		while (!NetworkManager.getInstance().isConnected()) {
			if (mIsConnectedFailurePrevious) {
				return false;
			}
			attempts++;
			Logger.v(this, "No network connection: attempt: " + String.valueOf(attempts));

			if (attempts >= 10) {
				return false;
			}
			try {
				Thread.sleep(500);
			}
			catch (InterruptedException exception) {
				return false;
			}
		}
		mIsConnectedFailurePrevious = false;
		return true;
	}

	// --------------------------------------------------------------------------------------------
	// WIFI routines
	// --------------------------------------------------------------------------------------------

	public Boolean isWifiEnabled() {
		return (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED);
	}

	public void enableWifi(boolean enable) {
		mWifiManager.setWifiEnabled(enable);
	}

	public void onPause() {
		try {
			/* Unregister for network connectivity broadcasts */
			if (mContext != null) {
				if (mConnectionReceiver != null) {
					mContext.unregisterReceiver(mConnectionReceiver);
				}
				if (mWifiStateChangedReceiver != null) {
					mContext.unregisterReceiver(mWifiStateChangedReceiver);
				}
			}
		}
		catch (Exception exception) {
			/*
			 * Jayma: For some insane reason, unregisterReceiver always throws an exception
			 * so we catch it and move on.
			 */
		}
	}

	public void onDestroy() {}

	public void onResume() {
		/*
		 * This is a placeholder for any future work that should be done
		 * when a parent activity is resumed.
		 */
		if (mContext != null) {
			mContext.registerReceiver(mConnectionReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
			mContext.registerReceiver(mWifiStateChangedReceiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
		}
	}

	// --------------------------------------------------------------------------------------------
	// Inner classes
	// --------------------------------------------------------------------------------------------

	public boolean isConnected() {
		ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm != null) {
			NetworkInfo[] info = cm.getAllNetworkInfo();
			if (info != null) {
				for (int i = 0; i < info.length; i++) {
					if (info[i].getState() == State.CONNECTED) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean isConnectedOrConnecting() {
		ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm != null) {
			NetworkInfo[] info = cm.getAllNetworkInfo();
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

	public void setConnectivityListener(IConnectivityListener connectivityListener) {
		this.mConnectivityListener = connectivityListener;
	}

	public interface IConnectivityListener {

		void onWifiStateChanged(int wifiState);

		void onConnectivityStateChanged(NetworkInfo.State networkInfoState);
	}

	public interface IWifiReadyListener {

		void onWifiReady();

		void onWifiFailed();
	}

	public interface NetworkRequestListener {

		void onRequestComplete();

		void onRequestFailed();
	}

	public class ConnectionReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {

			NetworkInfo currentNetworkInfo = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
			NetworkInfo otherNetworkInfo = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_OTHER_NETWORK_INFO);

			if (!currentNetworkInfo.isConnected() && currentNetworkInfo.isConnectedOrConnecting()) {
				Logger.d(this, "Network connecting: " + currentNetworkInfo.getTypeName());
				if (mConnectivityListener != null) {
					mConnectivityListener.onConnectivityStateChanged(State.CONNECTING);
				}
			}
			else if (currentNetworkInfo.isConnected()) {
				Logger.d(this, "Network connected: " + currentNetworkInfo.getTypeName());
				if (mConnectivityListener != null) {
					mConnectivityListener.onConnectivityStateChanged(State.CONNECTED);
				}
			}
			else if (otherNetworkInfo != null && otherNetworkInfo.isConnected()) {
				Logger.d(this, "Alt network connected: " + currentNetworkInfo.getTypeName());
				if (mConnectivityListener != null) {
					mConnectivityListener.onConnectivityStateChanged(State.CONNECTED);
				}
			}
			else {
				Logger.d(this, "Network disconnected");
				if (mConnectivityListener != null) {
					mConnectivityListener.onConnectivityStateChanged(State.DISCONNECTED);
				}
			}
		}
	};

	class WifiStateChangedReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(final Context context, Intent intent) {

			/* This is on the main UI thread */
			int extraWifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
			if (mConnectivityListener != null) {
				mConnectivityListener.onWifiStateChanged(extraWifiState);
			}

			if (extraWifiState == WifiManager.WIFI_STATE_ENABLED) {
				Logger.d(this, "Wifi state enabled");
			}
			else if (extraWifiState == WifiManager.WIFI_STATE_ENABLING) {
				Logger.d(this, "Wifi state enabling");
			}
			else {
				switch (extraWifiState) {
					case WifiManager.WIFI_STATE_DISABLED :
						Logger.d(this, "Wifi state disabled");
						break;
					case WifiManager.WIFI_STATE_DISABLING :
						Logger.d(this, "Wifi state disabling");
						//ImageUtils.showToastNotification(CandiSearchActivity.this, "Wifi state disabling...", Toast.LENGTH_SHORT);
						break;
					case WifiManager.WIFI_STATE_UNKNOWN :
						Logger.d(this, "Wifi state unknown");
						break;
				}
			}
		}
	}

	public static class ServiceResponse {

		public Object				data;
		public ResultCode			resultCode			= ResultCode.Success;
		public ResultCodeDetail		resultCodeDetail	= ResultCodeDetail.Success;
		public ProxibaseException	exception;

		public ServiceResponse() {}

		public ServiceResponse(ResultCode resultCode, ResultCodeDetail resultCodeDetail, Object data, ProxibaseException exception) {
			this.resultCode = resultCode;
			this.resultCodeDetail = resultCodeDetail;
			this.data = data;
			this.exception = exception;
		}
	}

	public enum ResultCodeDetail {
		Success, ServiceException, ServiceNotFoundException, ConnectionException, TransportException, ProtocolException, UnknownException
	}

	public enum ResultCode {
		Success, Recoverable, Unrecoverable
	}
}
