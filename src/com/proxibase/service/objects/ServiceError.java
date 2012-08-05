package com.proxibase.service.objects;


/**
 * @author Jayma
 */
public class ServiceError {

	public String	name;
	public Number	code;
	public String	message;
	public String	appStack;	// optional
	public String 	stack;

	public ServiceError() {}
}