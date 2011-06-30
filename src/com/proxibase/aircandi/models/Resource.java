package com.proxibase.aircandi.models;

import com.google.gson.annotations.Expose;
import com.proxibase.sdk.android.util.ProxiConstants;

/**
 * @author Jayma
 */
public class Resource
{
	// Annotation syntax: @Expose (serialize = false, deserialize = false)
	@Expose
	public String	resourceId;
	@Expose
	public String	entityId;
	@Expose
	public String	resourceName;
	@Expose
	public String	resourceType;
	@Expose
	public String	title;
	@Expose
	public String	artist;

	public Resource() {}
	
	public String getUriOdata()
	{
		String root = ProxiConstants.URL_PROXIBASE_SERVICE_ODATA;
		String entity = "Resources";
		String uri = root + entity + "(guid'" + this.resourceId + "')";
		return uri;
	}
}