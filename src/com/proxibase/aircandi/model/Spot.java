package com.proxibase.aircandi.model;

import com.google.gson.annotations.Expose;
import com.proxibase.sdk.android.core.ProxibaseService;

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
		String root = ProxibaseService.URL_PROXIBASE_SERVICE_ODATA;
		String entity = "Spots";
		String uri = root + entity + "(guid'" + this.spotId + "')";
		return uri;
	}
}