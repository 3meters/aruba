package com.proxibase.aircandi.models;

import java.util.Date;

import com.google.gson.annotations.Expose;

public class FriendAtPoint
{
	@Expose
	public String	id;
	@Expose
	public String	name;
	@Expose
	public String	label;
	@Expose
	public Date		hookupDate;
	@Expose
	public String	userId;
	@Expose
	public String	entityId;

	// Client only fields

	public FriendAtPoint() {}
}