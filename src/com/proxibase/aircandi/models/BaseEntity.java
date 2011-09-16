package com.proxibase.aircandi.models;

import android.graphics.Bitmap;

import com.google.gson.annotations.Expose;

/**
 * @author Jayma
 */
public class BaseEntity extends AircandiEntity {

	// Annotation syntax: @Expose (serialize = false, deserialize = false)
	@Expose
	public int			id;
	@Expose
	public Integer		parentEntityId;
	@Expose
	public String		entityUri;
	@Expose
	public String		entityType;
	@Expose
	public String		beaconId;
	@Expose
	public String		label;
	@Expose
	public String		title;
	@Expose
	public String		subtitle;
	@Expose
	public String		description;
	@Expose
	public String		imageUri;
	@Expose
	public String		imageFormat;
	@Expose
	public Float		signalFence	= -200f;
	@Expose
	public Float		createdLatitude;
	@Expose
	public Float		createdLongitude;
	@Expose
	public String		createdById;
	@Expose
	public String		createdDate;
	@Expose
	public Integer		visibility;
	@Expose
	public String		password;
	@Expose
	public boolean		enabled;

	/*
	 * For client use only
	 */
	public Bitmap		imageBitmap;

	public BaseEntity() {}

	@Override
	public String getId() {
		return String.valueOf(this.id);
	}

	@Override
	public String getCollection() {
		return "Entities";
	}

	@Override
	public String getIdType() {
		return null;
	}

	public enum SubType {
		Topic, Comment
	}
}