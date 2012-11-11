package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.HashMap;

import com.aircandi.service.Expose;


/**
 * @author Jayma
 */
public class GeoLocation extends ServiceObject implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 455904759787968585L;

	@Expose
	public Number				latitude;
	@Expose
	public Number				longitude;

	public GeoLocation() {}

	public GeoLocation(Number latitude, Number longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public static GeoLocation setPropertiesFromMap(GeoLocation geoLocation, HashMap map) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		geoLocation.latitude = (Number) map.get("latitude");
		geoLocation.longitude = (Number) map.get("longitude");
		return geoLocation;
	}

}