package com.proxibase.service.objects;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * @author Jayma
 */
public class Session {

	/* syntax: @Expose (serialize = false, deserialize = false) */

	@Expose
	@SerializedName("_id")
	public String	id;

	@Expose
	@SerializedName("_owner")
	public String	ownerId;

	@Expose
	public String	key;

	/* Dates */

	@Expose(serialize = false, deserialize = true)
	public Number	createdDate;

	@Expose(serialize = false, deserialize = true)
	public Number	modifiedDate;
	
	@Expose(serialize = false, deserialize = true)
	public Number	expirationDate;
	
	public Session() {}
}