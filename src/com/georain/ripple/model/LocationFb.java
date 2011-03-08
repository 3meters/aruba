package com.georain.ripple.model;

import com.google.gson.annotations.Expose;

public class LocationFb
{
	@Expose
	public String	street;
	@Expose
	public String	city;
	@Expose
	public String	state;
	@Expose
	public String	country;
	@Expose
	public String	zip;

	public LocationFb() {}

}
