package com.aircandi.service.objects;

import java.io.Serializable;

import com.google.gson.annotations.Expose;

/**
 * @author Jayma
 */
public class GeoLocation implements Cloneable, Serializable{

	private static final long	serialVersionUID	= 455904759787968585L;
	
	@Expose
	public Number	latitude;
	@Expose
	public Number	longitude;

	public GeoLocation() {}
}