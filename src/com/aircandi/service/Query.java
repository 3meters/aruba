package com.aircandi.service;

import java.net.URLEncoder;

@SuppressWarnings("ucd")
public class Query {

	public String	entityName;
	public String	filter;
	public Integer	topCount	= 0;
	public Boolean	lookups		= false;
	public String	orderBy;

	public Query() {}

	public Query(String entityName) {
		this.entityName = entityName;
	}

	public Query filter(String filter) {
		this.filter = filter;
		return this;
	}

	public Query orderBy(String orderBy) {
		this.orderBy = orderBy;
		return this;
	}

	public Query lookups(Boolean lookups) {
		this.lookups = lookups;
		return this;
	}

	public Query top(Integer topCount) {
		this.topCount = topCount;
		return this;
	}

	@SuppressWarnings("deprecation")
	public String queryString() {
		String query = entityName;
		Boolean atRoot = true;

		if (filter != null) {
			query += "?find=" + URLEncoder.encode(filter);
			atRoot = false;
		}

		if (lookups) {
			if (atRoot) {
				query += "?lookups=true";
			}
			else {
				query += "&lookups=true";
			}
			atRoot = false;
		}

		if (orderBy != null) {
			if (atRoot) {
				query += "?$orderby=" + URLEncoder.encode(orderBy);
			}
			else {
				query += "&$orderby=" + URLEncoder.encode(orderBy);
			}
			atRoot = false;
		}

		if (topCount != 0) {
			if (atRoot) {
				query += "?$top=" + URLEncoder.encode(topCount.toString());
			}
			else {
				query += "&$top=" + URLEncoder.encode(topCount.toString());
			}
			atRoot = false;
		}
		return query;
	}
}
