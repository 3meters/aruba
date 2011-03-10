package com.threemeters.aircandi.model;

import com.google.gson.annotations.Expose;
import com.threemeters.aircandi.controller.Aircandi;

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
		String root = Aircandi.URL_RIPPLESERVICE_ODATA;
		String entity = "Spots";
		String uri = root + entity + "(guid'" + this.spotId + "')";
		return uri;
	}
}