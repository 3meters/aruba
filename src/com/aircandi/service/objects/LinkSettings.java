package com.aircandi.service.objects;

import java.util.Map;

import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
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
	public Map					where;
	@Expose
	public Number				limit;
	@Expose
	public String				direction			= "both";

	public LinkSettings() {}

	public LinkSettings(String type, Boolean links, Boolean load, Boolean count) {
		this.type = type;
		this.links = links;
		this.load = load;
		this.count = count;
	}

	public LinkSettings(String type, Boolean links, Boolean load, Boolean count, Number limit) {
		this.type = type;
		this.links = links;
		this.load = load;
		this.count = count;
		this.limit = limit;
	}

	public LinkSettings(String type, Boolean links, Boolean load, Boolean count, Number limit, Map where) {
		this.type = type;
		this.links = links;
		this.load = load;
		this.count = count;
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

	public String getDirection() {
		return direction;
	}

	public LinkSettings setDirection(String direction) {
		this.direction = direction;
		return this;
	}
}