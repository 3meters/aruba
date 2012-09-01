package com.aircandi.service.objects;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * @author Jayma
 */
public class Link extends ServiceEntry {

	private static final long	serialVersionUID	= 8839291281700760437L;

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