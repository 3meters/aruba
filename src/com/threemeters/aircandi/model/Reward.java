package com.threemeters.aircandi.model;

import com.google.gson.annotations.Expose;
import com.threemeters.sdk.android.core.RippleService;

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
		String root = RippleService.URL_RIPPLE_SERVICE_ODATA;
		String entity = "Rewards";
		String uri = root + entity + "(guid'" + this.rewardId + "')";
		return uri;
	}
}