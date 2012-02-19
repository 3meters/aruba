package com.proxibase.aircandi.components;

import java.util.ArrayList;
import java.util.List;

public class Events {

	public static class EventBus {

		public static List<EventHandler>	wifiScanReceived	= new ArrayList<EventHandler>();
		public static List<EventHandler>	entitiesLoaded		= new ArrayList<EventHandler>();
		public static List<EventHandler>	beaconScanComplete	= new ArrayList<EventHandler>();
		public static List<EventHandler>	locationChanged		= new ArrayList<EventHandler>();

		public static void onWifiScanReceived(Object data) {
			for (EventHandler eventHandler : wifiScanReceived) {
				if (eventHandler != null) {
					eventHandler.onEvent(data);
				}
			}
		}

		public static void onEntitiesLoaded(Object data) {
			for (EventHandler eventHandler : entitiesLoaded) {
				if (eventHandler != null) {
					eventHandler.onEvent(data);
				}
			}
		}

		public static void onBeaconScanComplete(Object data) {
			for (EventHandler eventHandler : beaconScanComplete) {
				if (eventHandler != null) {
					eventHandler.onEvent(data);
				}
			}
		}

		public static void onLocationChanged(Object data) {
			for (EventHandler eventHandler : locationChanged) {
				if (eventHandler != null) {
					eventHandler.onEvent(data);
				}
			}
		}
	}

	public static class EventHandler {

		public void onEvent(Object data) {}
	}

}
