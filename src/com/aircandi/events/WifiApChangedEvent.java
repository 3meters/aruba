package com.aircandi.events;

@SuppressWarnings("ucd")
public class WifiApChangedEvent {
	public final Integer	wifiState;

	public WifiApChangedEvent(Integer wifiState) {
		this.wifiState = wifiState;
	}
}
