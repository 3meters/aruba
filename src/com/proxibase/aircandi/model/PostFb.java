package com.proxibase.aircandi.model;

import com.google.gson.annotations.Expose;
import com.proxibase.sdk.android.core.UserFb;

public class PostFb
{
	@Expose
	public String		id;

	@Expose
	public UserFb		from;

	@Expose
	public Integer		likes;

	@Expose
	public String		message;

	@Expose
	public String		picture;

	@Expose
	public String		link;

	@Expose
	public String		name;

	@Expose
	public String		caption;

	@Expose
	public String		description;

	@Expose
	public String		source;

	@Expose
	public String		icon;

	@Expose
	public String		attribution;

	@Expose
	public LinkFb		actions;

	@Expose
	public String		type;

	@Expose
	public String		createdTime;

	@Expose
	public String		updatedTime;

	public PostFb() {}
}
