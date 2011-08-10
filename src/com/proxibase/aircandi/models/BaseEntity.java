package com.proxibase.aircandi.models;

import com.google.gson.annotations.Expose;

/**
 * @author Jayma
 */
public class BaseEntity extends AircandiEntity {

	// Annotation syntax: @Expose (serialize = false, deserialize = false)
	@Expose
	public int		entityId;
	@Expose
	public String	entityType	= "";
	@Expose
	public String	beaconId;
	@Expose
	public String	label;
	@Expose
	public String	title;
	@Expose
	public String	subtitle;
	@Expose
	public String	description;
	@Expose
	public String	imageUri;
	@Expose
	public Float	signalFence	= -200f;
	@Expose
	public Float	createdLatitude;
	@Expose
	public Float	createdLongitude;
	@Expose
	public String	createdById;
	@Expose
	public String	createdDate;

	// For client use only
	public boolean	dataBound	= true;

	public BaseEntity() {}
	
	@Override
	public String getId() {
		return String.valueOf(this.entityId);
	}

	@Override
	public String getCollection() {
		return "Entities";
	}

	@Override
	public String getIdType() {
		return "";
	}
}