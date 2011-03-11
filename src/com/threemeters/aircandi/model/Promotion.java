package com.threemeters.aircandi.model;

import com.google.gson.annotations.Expose;
import com.threemeters.sdk.android.core.RippleService;

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
		String root = RippleService.URL_RIPPLE_SERVICE_ODATA;
		String entity = "Promotions";
		String uri = root + entity + "(guid'" + this.promotionId + "')";
		return uri;
	}
}