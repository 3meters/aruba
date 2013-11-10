package com.aircandi.events;

import com.aircandi.service.objects.Activity;

public class MessageEvent {
	public final Activity	activity;

	public MessageEvent(Activity activity) {
		this.activity = activity;
	}
}
