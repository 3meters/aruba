package com.aircandi.service.objects;

import java.util.HashMap;
import java.util.Map;

import com.aircandi.service.Expose;
import com.aircandi.service.SerializedName;

/**
 * @author Jayma
 */

@SuppressWarnings("ucd")
public class Link extends ServiceBase {

	private static final long	serialVersionUID	= 8839291281700760437L;
	public static final String	collectionId		= "links";

	@Expose
	public String				schema;
	@Expose
	@SerializedName(name = "_from")
	public String				fromId;
	@Expose
	@SerializedName(name = "_to")
	public String				toId;
	@Expose
	public Proximity			proximity;

	@Expose(serialize = false, deserialize = true)
	public String				fromCollectionId;
	@Expose(serialize = false, deserialize = true)
	public String				toCollectionId;
	@Expose(serialize = false, deserialize = true)
	public Number				tuneCount			= 0;

	public Link() {}

	public Link(String toId, String fromId) {
		this.toId = toId;
		this.fromId = fromId;
	}

	// --------------------------------------------------------------------------------------------
	// Set and get
	// --------------------------------------------------------------------------------------------	

	@Override
	public String getCollection() {
		return collectionId;
	}

	// --------------------------------------------------------------------------------------------
	// Copy and serialization
	// --------------------------------------------------------------------------------------------

	public static Link setPropertiesFromMap(Link link, Map map) {

		link = (Link) ServiceBase.setPropertiesFromMap(link, map);

		link.schema = (String) map.get("schema");
		link.fromId = (String) map.get("_from");
		link.toId = (String) map.get("_to");
		link.fromCollectionId = (String) map.get("fromCollectionId");
		link.toCollectionId = (String) map.get("toCollectionId");
		link.tuneCount = (Number) map.get("tuneCount");

		if (map.get("proximity") != null) {
			link.proximity = Proximity.setPropertiesFromMap(new Proximity(), (HashMap<String, Object>) map.get("proximity"));
		}

		return link;
	}

	@Override
	public Link clone() {
		final Link link = (Link) super.clone();
		return link;
	}

	// --------------------------------------------------------------------------------------------
	// Inner classes
	// --------------------------------------------------------------------------------------------	

	public enum Direction {
		in,
		out,
		both
	}
}
