package com.georain.ripple.model;

import com.georain.ripple.controller.Ripple;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

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
		return String.format("{0}post.aspx?id={1}", Ripple.URL_RIPPLEBLOGSERVICE, this.postId);
	}

	public String getUriOdata()
	{
		String root = Ripple.URL_RIPPLESERVICE_ODATA;
		String entity = "Posts";
		String uri = root + entity + "(guid'" + this.postId + "')";
		return uri;
	}

	public Post() {}
}
