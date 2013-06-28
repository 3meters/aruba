package com.aircandi.service.objects;

import java.util.Map;

import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
public class Cursor extends ServiceObject {

	private static final long	serialVersionUID	= -8424707925181657940L;

	@Expose
	public Map					where;
	@Expose
	public Map					sort;
	@Expose
	public Number				skip				= 0;
	@Expose
	public Number				limit;

	public Cursor() {}

	public Cursor(Map sort, Number skip, Number limit, Map where) {
		this.sort = sort;
		this.skip = skip;
		this.limit = limit;
		this.where = where;
	}

	public Cursor(Map sort, Number skip, Number limit) {
		this.sort = sort;
		this.skip = skip;
		this.limit = limit;
	}

	public Map getWhere() {
		return where;
	}

	public Cursor setWhere(Map where) {
		this.where = where;
		return this;
	}

	public Map getSort() {
		return sort;
	}

	public Cursor setSort(Map sort) {
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