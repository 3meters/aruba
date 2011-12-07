package com.proxibase.aircandi.models;

import android.graphics.Bitmap;

import com.google.gson.annotations.Expose;

public class PostEntity extends BaseEntity {

	/* Annotation syntax: @Expose (serialize = false, deserialize = false) */
	@Expose
	public String	mediaUri;
	@Expose
	public String	mediaFormat;
	@Expose
	public Metadata	__metadata	= new Metadata();

	//---------------------------------------------------------------------------------------------
	// For client use only
	//---------------------------------------------------------------------------------------------

	public Bitmap	mediaBitmap;

	public PostEntity() {
		__metadata.type = "Aircandi.Post";
	}
}