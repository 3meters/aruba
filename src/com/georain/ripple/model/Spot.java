package com.georain.ripple.model;

import com.georain.ripple.controller.Ripple;
import com.google.gson.annotations.Expose;

/**
 * @author Jayma
 */
public class Spot
{
	// Annotation syntax: @Expose (serialize = false, deserialize = false)
	@Expose
	public String	spotId;
	@Expose
	public String	name;
	@Expose
	public String	nameShort;
	@Expose
	public String	managerId;
	@Expose
	public String	logo;
	@Expose
	public String	logoSmall;
	@Expose
	public String	facebookId;
	

	public Spot() {}

	public String getUriOdata()
	{
		String root = Ripple.URL_RIPPLESERVICE_ODATA;
		String entity = "Spots";
		String uri = root + entity + "(guid'" + this.spotId + "')";
		return uri;
	}
}