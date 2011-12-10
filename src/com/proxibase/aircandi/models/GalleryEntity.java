package com.proxibase.aircandi.models;

import com.google.gson.annotations.Expose;

public class GalleryEntity extends BaseEntity {

	/* Annotation syntax: @Expose (serialize = false, deserialize = false) */
	@Expose
	public Metadata	__metadata	= new Metadata();

	//---------------------------------------------------------------------------------------------
	// For client use only
	//---------------------------------------------------------------------------------------------

	public GalleryEntity() {
		__metadata.type = "Aircandi.Album";
	}
}