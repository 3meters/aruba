package com.proxibase.aircandi.models;

import android.graphics.Bitmap;

import com.google.gson.annotations.Expose;

/**
 * @author Jayma
 */
public class Comment extends AircandiEntity {

	/* Annotation syntax: @Expose (serialize = false, deserialize = false) */
	@Expose
	public int		id;
	@Expose
	public Integer	entityId;
	@Expose
	public String	title;
	@Expose
	public String	comment;
	@Expose
	public String	imageUri;
	@Expose
	public String	imageFormat;
	@Expose
	public String	createdById;
	@Expose
	public String	createdDate;

	/* For client use only */
	public Bitmap	imageBitmap;

	public Comment() {}

	@Override
	public String getId() {
		return String.valueOf(this.id);
	}

	@Override
	public String getCollection() {
		return "Comments";
	}

	@Override
	public String getIdType() {
		return null;
	}
}