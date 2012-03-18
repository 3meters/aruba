package com.proxibase.service.objects;

import android.graphics.Bitmap;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * @author Jayma
 */
public class Comment {

	@Expose
	public String	title;
	@Expose
	public String	description;
	@Expose
	public String	name;
	@Expose
	public String	location;
	@Expose
	public String	imageUri;
	@Expose
	@SerializedName("_creator")
	public String	creatorId;
	@Expose
	public Number	createdDate;

	/* For client use only */
	public Bitmap	imageBitmap;

	public Comment() {}
}