package com.threemeters.aircandi.model;

import com.google.gson.annotations.Expose;

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
