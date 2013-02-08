package com.aircandi.components;

import android.location.Location;

@SuppressWarnings("ucd")
public class LocationReceivedEvent {
	public final Location	location;

	public LocationReceivedEvent(Location location) {
		this.location = location;
	}
}
