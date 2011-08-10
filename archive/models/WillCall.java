package com.proxibase.aircandi.models;

import com.google.gson.annotations.Expose;
import com.proxibase.sdk.android.util.ProxiConstants;

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
		String root = ProxiConstants.URL_PROXIBASE_SERVICE_ODATA;
		String entity = "WillCalls";
		String uri = root + entity + "(guid'" + this.willCallId + "')";
		return uri;
	}
}