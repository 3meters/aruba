package com.aircandi.components;

@SuppressWarnings("ucd")
public class WifiApChangedEvent {
	public final Integer	wifiState;

	public WifiApChangedEvent(Integer wifiState) {
		this.wifiState = wifiState;
	}
}
