package com.proxibase.aircandi.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiManager;

import com.proxibase.aircandi.core.CandiConstants;

public class NetworkManager {

	private static NetworkManager		singletonObject;

	private Context						mContext;
	private ConnectionReceiver			mConnectionReceiver			= new ConnectionReceiver();
	private WifiStateChangedReceiver	mWifiStateChangedReceiver	= new WifiStateChangedReceiver();
	private IConnectivityListener		mConnectivityListener;
	private WifiManager					mWifiManager;

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

	public void initialize() {
		mContext.registerReceiver(mConnectionReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		mContext.registerReceiver(mWifiStateChangedReceiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
		mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
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
		ConnectivityManager connectivity = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity != null) {
			NetworkInfo[] info = connectivity.getAllNetworkInfo();
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
		ConnectivityManager connectivity = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity != null) {
			NetworkInfo[] info = connectivity.getAllNetworkInfo();
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

	public interface IConnectivityReadyListener {

		void onConnectivityReady();
	}

	public class ConnectionReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {

			NetworkInfo currentNetworkInfo = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
			NetworkInfo otherNetworkInfo = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_OTHER_NETWORK_INFO);

			if (!currentNetworkInfo.isConnected() && currentNetworkInfo.isConnectedOrConnecting()) {
				Logger.d(CandiConstants.APP_NAME, this.getClass().getSimpleName(), "Network connecting: " + currentNetworkInfo.getTypeName());
				if (mConnectivityListener != null) {
					mConnectivityListener.onConnectivityStateChanged(State.CONNECTING);
				}
			}
			else if (currentNetworkInfo.isConnected()) {
				Logger.d(CandiConstants.APP_NAME, this.getClass().getSimpleName(), "Network connected: " + currentNetworkInfo.getTypeName());
				if (mConnectivityListener != null) {
					mConnectivityListener.onConnectivityStateChanged(State.CONNECTED);
				}
			}
			else if (otherNetworkInfo != null && otherNetworkInfo.isConnected()) {
				Logger.d(CandiConstants.APP_NAME, this.getClass().getSimpleName(), "Alt network connected: " + currentNetworkInfo.getTypeName());
				if (mConnectivityListener != null) {
					mConnectivityListener.onConnectivityStateChanged(State.CONNECTED);
				}
			}
			else {
				Logger.d(CandiConstants.APP_NAME, this.getClass().getSimpleName(), "Network disconnected");
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
				Logger.d(CandiConstants.APP_NAME, this.getClass().getSimpleName(), "Wifi state enabled");
			}
			else if (extraWifiState == WifiManager.WIFI_STATE_ENABLING) {
				Logger.d(CandiConstants.APP_NAME, this.getClass().getSimpleName(), "Wifi state enabling");
			}
			else {
				switch (extraWifiState) {
					case WifiManager.WIFI_STATE_DISABLED :
						Logger.d(CandiConstants.APP_NAME, this.getClass().getSimpleName(), "Wifi state disabled");
						break;
					case WifiManager.WIFI_STATE_DISABLING :
						Logger.d(CandiConstants.APP_NAME, this.getClass().getSimpleName(), "Wifi state disabling");
						//ImageUtils.showToastNotification(CandiSearchActivity.this, "Wifi state disabling...", Toast.LENGTH_SHORT);
						break;
					case WifiManager.WIFI_STATE_UNKNOWN :
						Logger.d(CandiConstants.APP_NAME, this.getClass().getSimpleName(), "Wifi state unknown");
						break;
				}
			}
		}
	}

}
