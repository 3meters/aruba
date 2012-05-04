package com.proxibase.service.objects;

import com.google.gson.annotations.Expose;

/**
 * @author Jayma
 */
public class ServiceData {

	@Expose
	public Object	data;
	@Expose
	public Number	date;
	@Expose
	public Number	count;
	@Expose
	public Boolean	more;
	@Expose
	public String	info;

	public ServiceData() {}
}