package com.proxibase.sdk.android.proxi.consumer;

import android.graphics.Bitmap;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * @author Jayma
 */

public class Comment extends ServiceEntry {

	@Expose
	@SerializedName("_entity")
	public String	entityId;

	@Expose(serialize = false, deserialize = true)
	public User		entity;

	@Expose
	public String	title;
	@Expose
	public String	description;

	/* For client use only */
	public Bitmap	imageBitmap;

	public Comment() {}

	@Override
	public String getCollection() {
		return "comments";
	}
}