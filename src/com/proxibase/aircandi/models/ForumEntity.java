package com.proxibase.aircandi.models;

import com.google.gson.annotations.Expose;

public class ForumEntity extends BaseEntity {

	/* Annotation syntax: @Expose (serialize = false, deserialize = false) */
	@Expose
	public Metadata	__metadata	= new Metadata();

	//---------------------------------------------------------------------------------------------
	// For client use only
	//---------------------------------------------------------------------------------------------

	public ForumEntity() {
		__metadata.type = "Aircandi.Forum";
	}
}