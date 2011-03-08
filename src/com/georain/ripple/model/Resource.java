package com.georain.ripple.model;

import com.georain.ripple.controller.Ripple;
import com.google.gson.annotations.Expose;

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
		String root = Ripple.URL_RIPPLESERVICE_ODATA;
		String entity = "Resources";
		String uri = root + entity + "(guid'" + this.resourceId + "')";
		return uri;
	}
}