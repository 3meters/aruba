package com.aircandi.components;

import android.location.Location;

@SuppressWarnings("ucd")
public class LocationLockedEvent {
	public final Location	location;

	public LocationLockedEvent(Location location) {
		this.location = location;
	}
}
