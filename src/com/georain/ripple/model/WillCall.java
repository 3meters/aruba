package com.georain.ripple.model;

import com.georain.ripple.controller.Ripple;
import com.google.gson.annotations.Expose;

/**
 * @author Jayma
 */
public class WillCall
{
	// Annotation syntax: @Expose (serialize = false, deserialize = false)
	@Expose
	public String	willCallId;
	@Expose
	public String	referrerId;
	@Expose
	public String	promotionId;
	@Expose
	public String	willCallMax;

	public WillCall() {}

	public String getUriOdata()
	{
		String root = Ripple.URL_RIPPLESERVICE_ODATA;
		String entity = "WillCalls";
		String uri = root + entity + "(guid'" + this.willCallId + "')";
		return uri;
	}
}