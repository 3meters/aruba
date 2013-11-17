package com.aircandi.events;

import com.aircandi.service.objects.ServiceMessage;

public class MessageEvent {
	public final ServiceMessage	message;

	public MessageEvent(ServiceMessage message) {
		this.message = message;
	}
}
