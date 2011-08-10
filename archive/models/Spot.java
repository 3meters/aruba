package com.proxibase.aircandi.models;

import com.google.gson.annotations.Expose;
import com.proxibase.sdk.android.util.ProxiConstants;

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
		String root = ProxiConstants.URL_PROXIBASE_SERVICE_ODATA;
		String entity = "Spots";
		String uri = root + entity + "(guid'" + this.spotId + "')";
		return uri;
	}
}