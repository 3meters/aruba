package com.aircandi.components;

@SuppressWarnings("ucd")
public class WifiChangedEvent {
	public final Integer	wifiState;

	public WifiChangedEvent(Integer wifiState) {
		this.wifiState = wifiState;
	}
}
