package com.aircandi.service.objects;

import java.util.HashMap;

import com.aircandi.service.Expose;


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

	public static ServiceError setPropertiesFromMap(ServiceError serviceError, HashMap map) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		serviceError.name = (String) map.get("name");
		serviceError.code = (Number) map.get("code");
		serviceError.message = (String) map.get("message");
		serviceError.appStack = (String) map.get("appStack");
		serviceError.stack = (String) map.get("stack");

		return serviceError;
	}
}