package com.georain.ripple.model;

import com.google.gson.annotations.Expose;

/**
 * @author Jayma
 */
public class PromotionsAtPoint extends Promotion
{
	// Annotation syntax: @Expose (serialize = false, deserialize = false)
	@Expose
	public Boolean	enrolled;
	@Expose
	public String	status;
	@Expose
	public Integer	couponCount;

	public PromotionsAtPoint() {}

}