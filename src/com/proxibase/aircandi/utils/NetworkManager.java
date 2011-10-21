package com.proxibase.aircandi.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

public class NetworkManager {

	private static NetworkManager	singletonObject;

	private Context					mContext;
	private ConnectionReceiver		mConnectionReceiver	= new ConnectionReceiver();
	private WifiManager				mWifiManager;
	private boolean					mConnected			= false;

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

	public boolean isNetworkAvailable() {
		ConnectivityManager connectivity = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity != null) {
			NetworkInfo[] info = connectivity.getAllNetworkInfo();
			if (info != null) {
				for (int i = 0; i < info.length; i++) {
					if (info[i].getState() == NetworkInfo.State.CONNECTED) {
						mConnected = true;
						return true;
					}
				}
			}
		}
		mConnected = false;
		return false;
	}

	public boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		return cm.getActiveNetworkInfo().isConnectedOrConnecting();
	}

	public void initialize() {
		mContext.registerReceiver(mConnectionReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
		isNetworkAvailable();
	}

	// --------------------------------------------------------------------------------------------
	// WIFI routines
	// --------------------------------------------------------------------------------------------

	public Boolean isWifiEnabled() {
		return (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED);
	}

	public void enableWifi() {
		mWifiManager.setWifiEnabled(true);
	}

	public void onPause() {
		try {
			/* Unregister for network connectivity broadcasts */
			if (mContext != null && mConnectionReceiver != null)
				mContext.unregisterReceiver(mConnectionReceiver);
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
	}

	// --------------------------------------------------------------------------------------------
	// Inner classes
	// --------------------------------------------------------------------------------------------

	public void setConnected(boolean connected) {
		this.mConnected = connected;
	}

	public boolean isConnected() {
		return mConnected;
	}

	public class ConnectionReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {

			NetworkInfo currentNetworkInfo = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
			NetworkInfo otherNetworkInfo = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_OTHER_NETWORK_INFO);

			if (currentNetworkInfo.isConnected()) {
				mConnected = true;
			}
			else if (otherNetworkInfo != null && otherNetworkInfo.isConnected()) {
				mConnected = true;
			}
			else {
				mConnected = false;
			}
		}
	};
}
