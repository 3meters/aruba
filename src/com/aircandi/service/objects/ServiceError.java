package com.aircandi.service.objects;

import com.google.gson.annotations.Expose;

/**
 * @author Jayma
 */
public class ServiceError {

	@Expose
	public String	name;
	@Expose
	public Number	code;
	@Expose
	public String	message;
	@Expose
	public String	errors;
	@Expose
	public String	appStack;	// optional
	@Expose
	public String	stack;

	public ServiceError() {}
}