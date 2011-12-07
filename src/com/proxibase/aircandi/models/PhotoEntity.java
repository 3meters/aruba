package com.proxibase.aircandi.models;

import android.graphics.Bitmap;

import com.google.gson.annotations.Expose;

public class PhotoEntity extends BaseEntity {

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

	public PhotoEntity() {
		__metadata.type = "Aircandi.Photo";
	}
}