package com.proxibase.aircandi.models;

import com.google.gson.annotations.Expose;
import com.proxibase.sdk.android.util.ProxiConstants;

/**
 * @author Jayma
 */
public class UserInPromotion
{
	// Annotation syntax: @Expose (serialize = false, deserialize = false)
	@Expose
	public String	userInPromotionId;
	@Expose
	public String	userId;
	@Expose
	public String	promotionId;
	@Expose
	public String	referrerId;
	@Expose
	public Boolean	deleted;

	public UserInPromotion() {}

	public String getUriOdata()
	{
		String root = ProxiConstants.URL_PROXIBASE_SERVICE_ODATA;
		String entity = "UsersInPromotions";
		String uri = root + entity + "(guid'" + this.userInPromotionId + "')";
		return uri;
	}
}