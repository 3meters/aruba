package com.georain.ripple.model;

import com.georain.ripple.controller.Ripple;
import com.google.gson.annotations.Expose;

/**
 * @author Jayma
 */
public class Coupon
{
	// Annotation syntax: @Expose (serialize = false, deserialize = false)
	@Expose
	public String	couponId;
	@Expose
	public String	userId;
	@Expose
	public String	promotionId;
	@Expose
	public String	couponDate;
	@Expose
	public String	used;
	@Expose
	public String	usedDate;
	

	public Coupon() {}

	public String getUriOdata()
	{
		String root = Ripple.URL_RIPPLESERVICE_ODATA;
		String entity = "Coupons";
		String uri = root + entity + "(guid'" + this.couponId + "')";
		return uri;
	}
}