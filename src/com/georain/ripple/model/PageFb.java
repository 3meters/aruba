package com.georain.ripple.model;

import com.google.gson.annotations.Expose;

public class PageFb
{
	@Expose
	public String		type;
	@Expose
	public String		id;
	@Expose
	public String		name;
	@Expose
	public String		category;
	@Expose
	public String		fanCount;
	@Expose
	public LocationFb	location;
	@Expose
	public String		phone;

	@Expose
	public String		picture;
	@Expose
	public String		link;
	@Expose
	public String		website;
	@Expose
	public String		userName;

	@Expose
	public String		attire;
	@Expose
	public String		priceRange;
	@Expose
	public String		generalManager;

	@Expose
	public String		founded;
	@Expose
	public String		companyOverview;
	@Expose
	public String		mission;
	@Expose
	public String		products;

	public PageFb() {}

}
