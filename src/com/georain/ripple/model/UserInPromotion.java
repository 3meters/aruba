package com.georain.ripple.model;

import com.georain.ripple.controller.Ripple;
import com.google.gson.annotations.Expose;

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
		String root = Ripple.URL_RIPPLESERVICE_ODATA;
		String entity = "UsersInPromotions";
		String uri = root + entity + "(guid'" + this.userInPromotionId + "')";
		return uri;
	}
}