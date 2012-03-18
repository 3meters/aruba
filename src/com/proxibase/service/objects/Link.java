package com.proxibase.service.objects;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * @author Jayma
 */
public class Link extends ServiceEntry {

	@Expose
	@SerializedName("_from")
	public String	fromId;

	@Expose
	@SerializedName("_to")
	public String	toId;

	public Link() {}

	public String getCollection() {
		return "links";
	}
}