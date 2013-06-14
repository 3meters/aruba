package com.aircandi.components;

import java.util.List;

import com.aircandi.components.ProximityManager.WifiScanResult;

@SuppressWarnings("ucd")
public class MonitoringWifiScanReceivedEvent {
	public final List<WifiScanResult>	wifiList;

	public MonitoringWifiScanReceivedEvent(List<WifiScanResult> wifiList) {
		this.wifiList = wifiList;
	}
}
