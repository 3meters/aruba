package com.proxibase.aircandi.model;

import com.google.gson.annotations.Expose;
import com.proxibase.sdk.android.core.ProxibaseService;

/**
 * @author Jayma
 */
public class Promotion
{
	// Annotation syntax: @Expose (serialize = false, deserialize = false)
	@Expose
	public String	promotionId;
	@Expose
	public String	spotId;
	@Expose
	public String	title;
	@Expose
	public String	description;
	@Expose
	public String	image;
	@Expose
	public String	promotionType;
	

	public Promotion() {}

	public String getUriOdata()
	{
		String root = ProxibaseService.URL_PROXIBASE_SERVICE_ODATA;
		String entity = "Promotions";
		String uri = root + entity + "(guid'" + this.promotionId + "')";
		return uri;
	}
}