package com.proxibase.aircandi.model;

import com.google.gson.annotations.Expose;

/**
 * @author Jayma
 */
public class Basket
{
	// Annotation syntax: @Expose (serialize = false, deserialize = false)
	@Expose
	public String	basketId;

	@Expose
	public String	entityId;

	@Expose
	public String	label;

	@Expose
	public double	latitude;

	@Expose
	public double	longitude;

	@Expose
	public String	dropName;

	@Expose
	public String	dropId;

	@Expose
	public Integer	count;

	public Basket() {}
}