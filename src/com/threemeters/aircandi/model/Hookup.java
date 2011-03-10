package com.threemeters.aircandi.model;

import com.google.gson.annotations.Expose;
import com.threemeters.aircandi.controller.Aircandi;

/**
 * @author Jayma
 */
public class Hookup
{
	// Annotation syntax: @Expose (serialize = false, deserialize = false)
	@Expose
	public String	hookupId;
	@Expose
	public String	userId;
	@Expose
	public String	entityId;
	@Expose
	public String	hookupDate;
	@Expose
	public boolean	confirmed				= false;
	@Expose
	public String	confirmedDate;
	

	public Hookup() {}
	
	public Hookup(String entityId, String userId, String hookupDate)
	{
		this.entityId = entityId;
		this.userId = userId;
		this.hookupDate = hookupDate;
	}

	public String getUriOdata()
	{
		String root = Aircandi.URL_RIPPLESERVICE_ODATA;
		String entity = "Hookups";
		String uri = root + entity + "(guid'" + this.hookupId + "')";
		return uri;
	}
}