package com.aircandi.service.objects;


/**
 * @author Jayma
 */
public class CursorSettings {

	public Object	where;
	public Object	sort;
	public Number	skip = 0;
	public Number	limit;

	public CursorSettings() {}
	
	public Object getWhere() {
		return where;
	}

	public CursorSettings setWhere(Object where) {
		this.where = where;
		return this;
	}

	public Object getSort() {
		return sort;
	}

	public CursorSettings setSort(Object sort) {
		this.sort = sort;
		return this;
	}

	public Number getSkip() {
		return skip;
	}

	public CursorSettings setSkip(Number skip) {
		this.skip = skip;
		return this;
	}

	public Number getLimit() {
		return limit;
	}

	public CursorSettings setLimit(Number limit) {
		this.limit = limit;
		return this;
	}

	public CursorSettings(Object sort, Number limit, Number skip) {
		this.sort = sort;
		this.limit = limit;
		this.skip = skip;
	}

	public CursorSettings(Object sort, Number limit, Number skip, Object where) {
		this.sort = sort;
		this.limit = limit;
		this.skip = skip;
		this.where = where;
	}

}