package com.proxibase.aircandi.models;

import android.graphics.Bitmap;

import com.google.gson.annotations.Expose;

public class WebEntity extends BaseEntity {

	/* Annotation syntax: @Expose (serialize = false, deserialize = false) */
	@Expose
	public String	contentUri;
	
	@Expose
	public Metadata	__metadata	= new Metadata();

	//---------------------------------------------------------------------------------------------
	// For client use only
	//---------------------------------------------------------------------------------------------

	public Bitmap	mediaBitmap;

	public WebEntity() {
		__metadata.type = "Aircandi.Web";
	}
}