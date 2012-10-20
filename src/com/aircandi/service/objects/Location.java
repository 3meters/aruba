package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.HashMap;

import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
public class Location implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 455904759787968585L;

	@Expose
	public String				address;
	@Expose
	public String				crossStreet;
	@Expose
	public String				postalCode;
	@Expose
	public String				city;
	@Expose
	public String				state;
	@Expose
	public String				country;
	@Expose
	public String				cc;
	@Expose
	public Number				lat;
	@Expose
	public Number				lng;
	/*
	 * Distance is measured in meters. Some venues have their locations intentionally hidden for privacy reasons (such
	 * as private residences). If this is the case, the parameter isFuzzed will be set to true, and the lat/lng
	 * parameters will have reduced precision.
	 */
	@Expose
	public Number				distance;
	@Expose
	public Boolean				isFuzzed;

	public Location() {}

	public static Location setFromPropertiesFromMap(Location location, HashMap map) {

		location.address = (String) map.get("address");
		location.crossStreet = (String) map.get("crossStreet");
		location.postalCode = (String) map.get("postalCode");
		location.city = (String) map.get("city");
		location.state = (String) map.get("state");
		location.country = (String) map.get("country");
		location.cc = (String) map.get("cc");
		location.lat = (Number) map.get("lat");
		location.lng = (Number) map.get("lng");
		location.distance = (Number) map.get("distance");
		location.isFuzzed = (Boolean) map.get("isFuzzed");

		return location;
	}

}