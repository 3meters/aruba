package com.georain.ripple.model;

import com.georain.ripple.controller.Ripple;
import com.google.gson.annotations.Expose;

/**
 * @author Jayma
 */
public class Page {
	@Expose
	public String	pageId;
	@Expose
	public String	title;
	@Expose
	public String	description;
	@Expose
	public String	pageContent;
	@Expose
	public String	keywords;
	@Expose
	public String	dateCreated;
	@Expose
	public String	dateModified;
	@Expose
	public Boolean	isPublished;
	@Expose
	public String	entityId;

	public String getUriOdata()
	{
		String root = Ripple.URL_RIPPLESERVICE_ODATA;
		String entity = "Pages";
		String uri = root + entity + "(guid'" + this.pageId + "')";
		return uri;
	}
	
	public Page() {}
}
