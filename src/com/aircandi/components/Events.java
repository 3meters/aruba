package com.aircandi.components;

import java.util.ArrayList;
import java.util.List;

public class Events {

	public static class EventBus {

		public static List<EventHandler>	wifiScanReceived		= new ArrayList<EventHandler>();
		public static List<EventHandler>	beaconsLocked			= new ArrayList<EventHandler>();
		public static List<EventHandler>	locationChanged			= new ArrayList<EventHandler>();
		public static List<EventHandler>	locationEntitiesLoaded	= new ArrayList<EventHandler>();
		public static List<EventHandler>	beaconEntitiesLoaded	= new ArrayList<EventHandler>();
		public static List<EventHandler>	syntheticsLoaded		= new ArrayList<EventHandler>();

		public static void onWifiScanReceived(Object data) {
			synchronized (wifiScanReceived) {
				List<EventHandler> eventHandlers = (List<EventHandler>) ((ArrayList<EventHandler>) wifiScanReceived).clone();
				for (EventHandler eventHandler : eventHandlers) {
					if (eventHandler != null) {
						eventHandler.onEvent(data);
					}
				}
			}
		}

		public static void onBeaconsLocked(Object data) {
			synchronized (beaconsLocked) {
				List<EventHandler> eventHandlers = (List<EventHandler>) ((ArrayList<EventHandler>) beaconsLocked).clone();
				for (EventHandler eventHandler : eventHandlers) {
					if (eventHandler != null) {
						eventHandler.onEvent(data);
					}
				}
			}
		}

		public static void onLocationChanged(Object data) {
			synchronized (locationChanged) {
				List<EventHandler> eventHandlers = (List<EventHandler>) ((ArrayList<EventHandler>) locationChanged).clone();
				for (EventHandler eventHandler : eventHandlers) {
					if (eventHandler != null) {
						eventHandler.onEvent(data);
					}
				}
			}
		}

		public static void onBeaconEntitiesLoaded(Object data) {
			synchronized (beaconEntitiesLoaded) {
				List<EventHandler> eventHandlers = (List<EventHandler>) ((ArrayList<EventHandler>) beaconEntitiesLoaded).clone();
				for (EventHandler eventHandler : eventHandlers) {
					if (eventHandler != null) {
						eventHandler.onEvent(data);
					}
				}
			}
		}

		public static void onLocationEntitiesLoaded(Object data) {
			synchronized (locationEntitiesLoaded) {
				List<EventHandler> eventHandlers = (List<EventHandler>) ((ArrayList<EventHandler>) locationEntitiesLoaded).clone();
				for (EventHandler eventHandler : eventHandlers) {
					if (eventHandler != null) {
						eventHandler.onEvent(data);
					}
				}
			}
		}

		public static void onSyntheticsLoaded(Object data) {
			synchronized (syntheticsLoaded) {
				List<EventHandler> eventHandlers = (List<EventHandler>) ((ArrayList<EventHandler>) syntheticsLoaded).clone();
				for (EventHandler eventHandler : eventHandlers) {
					if (eventHandler != null) {
						eventHandler.onEvent(data);
					}
				}
			}
		}
	}

	public static class EventHandler {
		public void onEvent(Object data) {}
	}
}
