package com.threemeters.aircandi.model;

import com.google.gson.annotations.Expose;
import com.threemeters.aircandi.controller.Aircandi;

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
		String root = Aircandi.URL_RIPPLESERVICE_ODATA;
		String entity = "Rewards";
		String uri = root + entity + "(guid'" + this.rewardId + "')";
		return uri;
	}
}