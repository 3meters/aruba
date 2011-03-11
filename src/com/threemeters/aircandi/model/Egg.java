package com.threemeters.aircandi.model;

import android.graphics.Bitmap;

import com.google.gson.annotations.Expose;
import com.threemeters.sdk.android.core.RippleService;

/**
 * @author Jayma
 */
public class Egg
{
	// Annotation syntax: @Expose (serialize = false, deserialize = false)
	@Expose
	public String	eggId;

	@Expose
	public String	eggFinderId;

	@Expose
	public String	eggFoundDate;

	@Expose
	public Boolean	eggUnwrapped;

	@Expose
	public String	eggOwnerId;

	@Expose
	public String	eggClassId;
	
	@Expose
	public String	eggName;

	@Expose
	public String	eggResourceId;

	@Expose
	public String	eggSetName;

	@Expose
	public String	basketDropperId;

	@Expose
	public String	basketDropDate;

	@Expose
	public String	eggDropperName;

	@Expose
	public String	eggFinderName;

	@Expose
	public String	entityId;
	
	@Expose
	public String	basketMessage;
	
	@Expose
	public String	sponsorName;
	
	@Expose
	public String	sponsorMessage;
	
	@Expose
	public String	couponTitle;
	
	@Expose
	public String	couponDesc;
	
	@Expose
	public String	couponImage;
	
	@Expose
	public String	categoryId;
	
	public Bitmap	eggImage;
	public Boolean	addedToCollection = false;

	public Egg() {}

	public String getUriOdata()
	{
		String root = RippleService.URL_RIPPLESERVICE_ODATA;
		String entity = "Eggs";
		String uri = root + entity + "(guid'" + this.eggId + "')";
		return uri;
	}
}