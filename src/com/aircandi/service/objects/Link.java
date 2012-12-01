package com.aircandi.service.objects;

import java.util.HashMap;

import com.aircandi.service.Expose;
import com.aircandi.service.SerializedName;

/**
 * @author Jayma
 */
public class Link extends ServiceEntryBase {

	private static final long	serialVersionUID	= 8839291281700760437L;

	@Expose
	@SerializedName("_from")
	public String				fromId;
	@Expose
	@SerializedName("_to")
	public String				toId;
	@Expose
	public Number				signal;
	@Expose
	public Boolean				primary;

	@Expose(serialize = false, deserialize = true)
	public String				fromCollectionId;
	@Expose(serialize = false, deserialize = true)
	public String				toCollectionId;
	@Expose(serialize = false, deserialize = true)
	public Number				tuneCount;

	public Link() {}

	public Link(String toId, String fromId) {
		this.toId = toId;
		this.fromId = fromId;
	}

	@Override
	public Link clone() {
		try {
			final Link link = (Link) super.clone();
			return link;
		}
		catch (final CloneNotSupportedException ex) {
			throw new AssertionError();
		}
	}

	public static Link setPropertiesFromMap(Link link, HashMap map) {

		link = (Link) ServiceEntryBase.setPropertiesFromMap(link, map);

		link.fromId = (String) map.get("_from");
		link.toId = (String) map.get("_to");
		link.signal = (Number) map.get("signal");
		link.primary = (Boolean) map.get("primary");

		link.fromCollectionId = (String) map.get("fromCollectionId");
		link.toCollectionId = (String) map.get("toCollectionId");
		link.tuneCount = (Number) map.get("tuneCount");
		return link;
	}

	public String getCollection() {
		return "links";
	}
}