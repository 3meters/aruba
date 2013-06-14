package com.aircandi.service.objects;

/**
 * @author Jayma
 */
public class Cursor {

	public String	where;
	public String	sort;
	public Number	skip	= 0;
	public Number	limit;

	public Cursor() {}

	public String getWhere() {
		return where;
	}

	public Cursor(String sort, Number skip, Number limit, String where) {
		this.sort = sort;
		this.skip = skip;
		this.limit = limit;
		this.where = where;
	}

	public Cursor(String sort, Number skip, Number limit) {
		this.sort = sort;
		this.skip = skip;
		this.limit = limit;
	}

	public Cursor setWhere(String where) {
		this.where = where;
		return this;
	}

	public String getSort() {
		return sort;
	}

	public Cursor setSort(String sort) {
		this.sort = sort;
		return this;
	}

	public Number getSkip() {
		return skip;
	}

	public Cursor setSkip(Number skip) {
		this.skip = skip;
		return this;
	}

	public Number getLimit() {
		return limit;
	}

	public Cursor setLimit(Number limit) {
		this.limit = limit;
		return this;
	}

}