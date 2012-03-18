package com.proxibase.service.objects;

import com.google.gson.annotations.Expose;

/**
 * @author Jayma
 */
public class GeoLocation {

	@Expose
	public Number	latitude;
	@Expose
	public Number	longitude;

	public GeoLocation() {}
}