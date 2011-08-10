package com.proxibase.aircandi.models;

import com.google.gson.annotations.Expose;
import com.proxibase.sdk.android.util.ProxiConstants;

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
		String root = ProxiConstants.URL_PROXIBASE_SERVICE_ODATA;
		String entity = "Coupons";
		String uri = root + entity + "(guid'" + this.couponId + "')";
		return uri;
	}
}