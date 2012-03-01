package com.proxibase.sdk.android.proxi.consumer;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * @author Jayma
 */
public class EntityPoint {

	@Expose
	@SerializedName("_id")
	public Integer	id;

	@Expose
	public String	label;
	@Expose
	public Number	latitude;
	@Expose
	public Number	longitude;
	@Expose
	public String	imagePreviewUri;
	@Expose
	public String	linkUri;

	public EntityPoint() {}

}