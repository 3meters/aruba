package com.aircandi.service.objects;

/**
 * @author Jayma
 */
public class LinkSettings {

	public String	type;
	public Boolean	links		= false;
	public Boolean	load		= false;
	public Boolean	count		= true;
	public String	where;
	public Number	limit;
	public String	direction	= "both";

	public LinkSettings() {}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

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

	public LinkSettings(String type, Boolean links, Boolean load, Boolean count, Number limit, String where) {
		this.type = type;
		this.links = links;
		this.load = load;
		this.count = count;
		this.where = where;
		this.limit = limit;
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

	public String getWhere() {
		return where;
	}

	public LinkSettings setWhere(String where) {
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