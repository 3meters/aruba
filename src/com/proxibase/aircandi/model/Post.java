package com.proxibase.aircandi.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.proxibase.aircandi.controller.Aircandi;
import com.proxibase.sdk.android.core.ProxibaseService;

/**
 * @author Jayma
 */
public class Post
{
	@Expose
	public String	postId;
	@Expose
	public String	title = "";
	@Expose
	public String	description = "";
	@Expose
	public String	postContent = "";
	@Expose
	public String	dateCreated;
	@Expose
	public String	dateModified;
	@Expose
	@SerializedName("Author")
	public String	authorId = "";
	@Expose
	public Boolean	isPublished = false;
	@Expose
	public Boolean	isCommentEnabled = false;
	@Expose
	public Integer	raters = 0;
	@Expose
	public Float	rating = 0f;
	@Expose
	public String	entityId = "";
	
	public String 	getPermaLink()
	{
		return String.format("{0}post.aspx?id={1}", Aircandi.URL_AIRCANDI_BLOG, this.postId);
	}

	public String getUriOdata()
	{
		String root = ProxibaseService.URL_PROXIBASE_SERVICE_ODATA;
		String entity = "Posts";
		String uri = root + entity + "(guid'" + this.postId + "')";
		return uri;
	}

	public Post() {}
}
