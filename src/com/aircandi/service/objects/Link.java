package com.aircandi.service.objects;

import com.aircandi.service.Expose;
import com.aircandi.service.SerializedName;

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