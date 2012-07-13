package com.proxibase.service.objects;

import java.util.List;

import com.google.gson.annotations.Expose;

/**
 * @author Jayma
 */
public class ServiceData {

	@Expose
	public Object		data;
	@Expose
	public List<String>	cursor;
	@Expose
	public Number		date;
	@Expose
	public Number		count;
	@Expose
	public Boolean		more;
	@Expose
	public String		info;

	@Expose
	public User			user;
	@Expose
	public ServiceError	error;
	@Expose
	public Session		session;
	@Expose
	public Number		time;

	public ServiceData() {}
}