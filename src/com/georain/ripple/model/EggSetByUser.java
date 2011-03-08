package com.georain.ripple.model;

import com.google.gson.annotations.Expose;

/**
 * @author Jayma
 */
public class EggSetByUser
{
	// Annotation syntax: @Expose (serialize = false, deserialize = false)
	@Expose
	public String	eggSetName;

	@Expose
	public Integer	setCount;

	@Expose
	public Integer	setMax;
	
	@Expose
	public String	eggSetResourceId;
	

	public EggSetByUser() {}
}