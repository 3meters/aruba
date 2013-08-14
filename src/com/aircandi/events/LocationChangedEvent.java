package com.aircandi.events;

import android.location.Location;

@SuppressWarnings("ucd")
public class LocationChangedEvent {
	public final Location	location;

	public LocationChangedEvent(Location location) {
		this.location = location;
	}
}
