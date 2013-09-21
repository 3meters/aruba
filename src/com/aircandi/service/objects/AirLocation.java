package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.Map;

import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class AirLocation extends ServiceObject implements Cloneable, Serializable {

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
	@Expose
	public String				provider;
	@Expose
	public Boolean				zombie = false ;

	public AirLocation() {}

	public AirLocation(Number lat, Number lng) {
		this.lat = lat;
		this.lng = lng;
	}

	@Override
	public AirLocation clone() {
		try {
			final AirLocation clone = (AirLocation) super.clone();
			return clone;
		}
		catch (final CloneNotSupportedException ex) {
			throw new AssertionError();
		}
	}
	
	public static AirLocation setPropertiesFromMap(AirLocation location, Map map, Boolean nameMapping) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		location.lat = (Number) map.get("lat");
		location.lng = (Number) map.get("lng");
		location.altitude = (Number) map.get("altitude");
		location.accuracy = (Number) map.get("accuracy");
		location.bearing = (Number) map.get("bearing");
		location.speed = (Number) map.get("speed");
		location.provider = (String) map.get("provider");
		location.zombie = (Boolean) map.get("zombie");
		return location;
	}
	
	public Float distanceTo(AirLocation location) {
		
		Float distance = 0f;

		final android.location.Location locationThis = new android.location.Location(this.provider);
		locationThis.setLatitude(this.lat.doubleValue());
		locationThis.setLongitude(this.lng.doubleValue());

		final android.location.Location locationTo = new android.location.Location("place");
		locationTo.setLatitude(location.lat.doubleValue());
		locationTo.setLongitude(location.lng.doubleValue());

		distance = locationThis.distanceTo(locationTo);
		return distance;
	}
}