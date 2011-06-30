package com.proxibase.aircandi.models;

import com.google.gson.annotations.Expose;
import com.proxibase.sdk.android.util.ProxiConstants;

/**
 * @author Jayma
 */
public class Reward
{
	// Annotation syntax: @Expose (serialize = false, deserialize = false)
	@Expose
	public String	rewardId;
	@Expose
	public String	entityId;
	@Expose
	public String	title;
	@Expose
	public String	description;

	public Reward() {}
	
	public String getUriOdata()
	{
		String root = ProxiConstants.URL_PROXIBASE_SERVICE_ODATA;
		String entity = "Rewards";
		String uri = root + entity + "(guid'" + this.rewardId + "')";
		return uri;
	}
}