package com.threemeters.aircandi.model;

import com.google.gson.annotations.Expose;

/**
 * @author Jayma
 */
public class StringResult
{
	// Annotation syntax: @Expose (serialize = false, deserialize = false)
	@Expose
	public String	result;
	@Expose
	public String	type;

	public StringResult() {}	
}