package com.proxibase.aircandi.model;

import com.google.gson.annotations.Expose;
import com.proxibase.sdk.android.core.ProxibaseService;

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
		String root = ProxibaseService.URL_PROXIBASE_SERVICE_ODATA;
		String entity = "Rewards";
		String uri = root + entity + "(guid'" + this.rewardId + "')";
		return uri;
	}
}