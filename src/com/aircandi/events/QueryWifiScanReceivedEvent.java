package com.aircandi.events;

import java.util.List;

import com.aircandi.components.ProximityManager.WifiScanResult;



@SuppressWarnings("ucd")
public class QueryWifiScanReceivedEvent {
	public final List<WifiScanResult>	wifiList;

	public QueryWifiScanReceivedEvent(List<WifiScanResult> wifiList) {
		this.wifiList = wifiList;
	}
}
