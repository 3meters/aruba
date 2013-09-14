package com.aircandi.service.objects;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.aircandi.Constants;
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
	@SerializedName(name = "_from")
	public String				fromId;
	@Expose
	@SerializedName(name = "_to")
	public String				toId;
	@Expose(serialize = false, deserialize = true)
	public String				fromCollectionId;
	@Expose(serialize = false, deserialize = true)
	public String				toCollectionId;
	@Expose
	public Boolean				strong;
	@Expose
	public Boolean				inactive = false;

	@Expose
	public Proximity			proximity;
	@Expose(serialize = false, deserialize = true)
	public Shortcut				shortcut;
	@Expose(serialize = false, deserialize = true)
	public List<Count>			stats;

	public Link() {}

	public Link(String toId, String type, Boolean strong) {
		this.toId = toId;
		this.type = type;
		this.strong = strong;
	}

	public Link(String toId, String type, Boolean strong, String fromId) {
		this.toId = toId;
		this.type = type;
		this.strong = strong;
		this.fromId = fromId;
	}

	// --------------------------------------------------------------------------------------------
	// Set and get
	// --------------------------------------------------------------------------------------------

	public Integer getProximityScore() {
		Integer score = 0;
		if (this.stats != null) {
			for (Count count : stats) {
				if (count.type.equals(Constants.TYPE_COUNT_LINK_PROXIMITY)) {
					score += count.count.intValue();
				}
				else if (count.type.equals(Constants.TYPE_COUNT_LINK_PROXIMITY_MINUS)) {
					score -= count.count.intValue();
				}
			}
		}
		return score;
	}

	public Count getStat(String type) {
		if (this.stats != null) {
			for (Count count : stats) {
				if (count.type.equals(type)) {
					return count;
				}
			}
		}
		return null;
	}

	public Count incrementStat(String type) {
		Count count = null;
		if (this.stats == null) {
			this.stats = new ArrayList<Count>();
		}
		if (getStat(type) == null) {
			count = new Count(type, 1);
			this.stats.add(count);
		}
		else {
			count = getStat(type);
			count.count = count.count.intValue() + 1;
		}
		return count;
	}

	public Count decrementStat(String type) {
		Count count = null;
		if (this.stats != null) {
			count = getStat(type);
			if (count != null) {
				count.count = count.count.intValue() - 1;
				if (count.count.intValue() <= 0) {
					this.stats.remove(count);
				}
			}
		}
		return count;
	}

	@Override
	public String getCollection() {
		return collectionId;
	}

	// --------------------------------------------------------------------------------------------
	// Copy and serialization
	// --------------------------------------------------------------------------------------------

	public static Link setPropertiesFromMap(Link link, Map map, Boolean nameMapping) {

		link = (Link) ServiceBase.setPropertiesFromMap(link, map, nameMapping);

		link.fromId = (String) (nameMapping ? map.get("_from") : map.get("fromId"));
		link.toId = (String) (nameMapping ? map.get("_to") : map.get("toId"));
		link.fromCollectionId = (String) map.get("fromCollectionId");
		link.toCollectionId = (String) map.get("toCollectionId");
		link.strong = (Boolean) map.get("strong");
		link.inactive = (Boolean) map.get("inactive");

		if (map.get("proximity") != null) {
			link.proximity = Proximity.setPropertiesFromMap(new Proximity(), (HashMap<String, Object>) map.get("proximity"), nameMapping);
		}

		if (map.get("stats") != null) {
			final List<LinkedHashMap<String, Object>> statMaps = (List<LinkedHashMap<String, Object>>) map.get("stats");

			link.stats = new ArrayList<Count>();
			for (Map<String, Object> statMap : statMaps) {
				link.stats.add(Count.setPropertiesFromMap(new Count(), statMap, nameMapping));
			}
		}

		if (map.get("shortcut") != null) {
			link.shortcut = Shortcut.setPropertiesFromMap(new Shortcut(), (HashMap<String, Object>) map.get("shortcut"), nameMapping);
		}

		return link;
	}

	@Override
	public Link clone() {
		final Link link = (Link) super.clone();
		if (stats != null) {
			link.stats = (List<Count>) ((ArrayList) stats).clone();
		}

		return link;
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------

	public static class SortByModifiedDate implements Comparator<Link> {

		@Override
		public int compare(Link object1, Link object2) {

			if (object1.modifiedDate.longValue() < object2.modifiedDate.longValue()) {
				return 1;
			}
			else if (object1.modifiedDate.longValue() == object2.modifiedDate.longValue()) {
				return 0;
			}
			return -1;
		}
	}

	public enum Direction {
		in,
		out,
		both
	}
}
