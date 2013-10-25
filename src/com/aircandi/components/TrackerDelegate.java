package com.aircandi.components;

import android.app.Activity;
import android.support.v4.app.Fragment;

public interface TrackerDelegate {
	public void sendEvent(String category, String action, String target, long value);

	public void sendTiming(String category, Long timing, String name, String label);

	public void sendException(Exception exception);

	public void sendError(String category, String name);
	
	public void fragmentStart(Fragment fragment);

	public void activityStart(Activity activity);

	public void activityStop(Activity activity);

	public void applicationStart();

	public static enum Action {
		ENTITY_KICK,
		ENTITY_DELETE

	}
}