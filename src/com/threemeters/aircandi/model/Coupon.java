package com.threemeters.aircandi.model;

import com.google.gson.annotations.Expose;
import com.threemeters.aircandi.controller.Aircandi;

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
		String root = Aircandi.URL_RIPPLESERVICE_ODATA;
		String entity = "Coupons";
		String uri = root + entity + "(guid'" + this.couponId + "')";
		return uri;
	}
}