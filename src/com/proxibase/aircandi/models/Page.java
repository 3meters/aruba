package com.proxibase.aircandi.models;

import com.google.gson.annotations.Expose;
import com.proxibase.sdk.android.util.ProxiConstants;

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
		String root = ProxiConstants.URL_PROXIBASE_SERVICE_ODATA;
		String entity = "Pages";
		String uri = root + entity + "(guid'" + this.pageId + "')";
		return uri;
	}
	
	public Page() {}
}
