package com.aircandi.components;

import java.util.List;

import com.aircandi.components.ProxiManager.WifiScanResult;



@SuppressWarnings("ucd")
public class QueryWifiScanReceivedEvent {
	public final List<WifiScanResult>	wifiList;

	public QueryWifiScanReceivedEvent(List<WifiScanResult> wifiList) {
		this.wifiList = wifiList;
	}
}
