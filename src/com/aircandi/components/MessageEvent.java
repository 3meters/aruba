package com.aircandi.components;

import com.aircandi.service.objects.AirNotification;

public class MessageEvent {
	public final AirNotification	notification;

	public MessageEvent(AirNotification notification) {
		this.notification = notification;
	}
}
