package com.aircandi.service.objects;

import java.util.Map;

import com.aircandi.ServiceConstants;
import com.aircandi.service.Expose;
import com.aircandi.service.objects.Link.Direction;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class LinkSettings extends ServiceObject {

	private static final long	serialVersionUID	= 4371355790668325686L;

	@Expose
	public String				type;
	@Expose
	public Boolean				links				= false;
	@Expose
	public Boolean				load				= false;
	@Expose
	public Boolean				count				= true;
	@Expose
	public Boolean				inactive			= false;
	@Expose
	public Map					where;
	@Expose
	public Number				limit;
	@Expose
	public String				direction			= "BOTH";

	public LinkSettings() {}

	public LinkSettings(String type, Boolean links, Boolean load, Boolean count) {
		this(type, links, load, count, false); 
	}
	
	public LinkSettings(String type, Boolean links, Boolean load, Boolean count, Boolean inactive) {
		this(type, links, load, count, inactive, ServiceConstants.LIMIT_LINKS_DEFAULT); 
	}

	public LinkSettings(String type, Boolean links, Boolean load, Boolean count, Boolean inactive, Number limit) {
		this(type, links, load, count, inactive, limit, null); 
	}

	public LinkSettings(String type, Boolean links, Boolean load, Boolean count, Boolean inactive, Number limit, Map where) {
		this.type = type;
		this.links = links;
		this.load = load;
		this.count = count;
		this.inactive = inactive;
		this.where = where;
		this.limit = limit;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Boolean getLinks() {
		return links;
	}

	public LinkSettings setLinks(Boolean links) {
		this.links = links;
		return this;
	}

	public Boolean getLoad() {
		return load;
	}

	public LinkSettings setLoad(Boolean load) {
		this.load = load;
		return this;
	}

	public Boolean getCount() {
		return count;
	}

	public LinkSettings setCount(Boolean count) {
		this.count = count;
		return this;
	}

	public Object getWhere() {
		return where;
	}

	public LinkSettings setWhere(Map where) {
		this.where = where;
		return this;
	}

	public Number getLimit() {
		return limit;
	}

	public LinkSettings setLimit(Number limit) {
		this.limit = limit;
		return this;
	}

	public Direction getDirection() {
		return Direction.valueOf(direction);
	}

	public LinkSettings setDirection(Direction direction) {
		this.direction = direction.name();
		return this;
	}

	public Boolean getInactive() {
		return inactive;
	}

	public LinkSettings setInactive(Boolean inactive) {
		this.inactive = inactive;
		return this;
	}
}