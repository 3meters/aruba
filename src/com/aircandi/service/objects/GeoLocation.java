package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.Map;

import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class GeoLocation extends ServiceObject implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 455904759787968585L;

	@Expose
	public Number				lat;
	@Expose
	public Number				lng;
	@Expose
	public Number				altitude;
	@Expose
	public Number				accuracy;
	@Expose
	public Number				bearing;
	@Expose
	public Number				speed;

	public GeoLocation() {}

	public GeoLocation(Number lat, Number lng) {
		this.lat = lat;
		this.lng = lng;
	}

	@Override
	public GeoLocation clone() {
		try {
			final GeoLocation clone = (GeoLocation) super.clone();
			return clone;
		}
		catch (final CloneNotSupportedException ex) {
			throw new AssertionError();
		}
	}
	
	public static GeoLocation setPropertiesFromMap(GeoLocation geoLocation, Map map) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		geoLocation.lat = (Number) map.get("lat");
		geoLocation.lng = (Number) map.get("lng");
		geoLocation.altitude = (Number) map.get("altitude");
		geoLocation.accuracy = (Number) map.get("accuracy");
		geoLocation.bearing = (Number) map.get("bearing");
		geoLocation.speed = (Number) map.get("speed");
		return geoLocation;
	}
}