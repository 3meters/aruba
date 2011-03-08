package com.georain.ripple.model;

import java.net.URLEncoder;
import com.facebook.android.Facebook;
import com.georain.ripple.model.RippleService.UrlEncodingType;

public class Query
{
	public String			entityName		= "";
	public String			filter			= "";
	public Integer			topCount		= 0;
	public String			orderBy			= "";
	public Facebook			facebook;
	public UrlEncodingType	urlEncodingType	= UrlEncodingType.SpacesOnly;

	public Query() {}

	public Query(String entityName) {
		this.entityName = entityName;
	}

	public Query(String entityName, Facebook facebook) {
		this.entityName = entityName;
		this.facebook = facebook;
	}

	public Query filter(String filter)
	{
		this.filter = filter;
		return this;
	}

	public Query orderBy(String orderBy)
	{
		this.orderBy = orderBy;
		return this;
	}

	public Query top(Integer topCount)
	{
		this.topCount = topCount;
		return this;
	}

	public String queryString()
	{
		String query = this.entityName;
		Boolean atRoot = true;

		if (this.filter != "")
		{
			query += "?$filter=" + this.filter;
			atRoot = false;
		}

		if (this.orderBy != "")
		{
			if (atRoot)
				query += "?$orderby=" + this.orderBy;
			else
				query += "&$orderby=" + this.orderBy;
			atRoot = false;
		}

		if (this.topCount != 0)
		{
			if (atRoot)
				query += "?$top=" + this.topCount.toString();
			else
				query += "&$top=" + this.topCount.toString();
			atRoot = false;
		}

		if (this.urlEncodingType == UrlEncodingType.All)
			query = URLEncoder.encode(query);
		else if (this.urlEncodingType == UrlEncodingType.SpacesOnly)
			query = query.replaceAll(" ", "%20");

		return query;
	}
	
	public String queryStringFb()
	{
		//https://graph.facebook.com/696942623/friends?access_token=107345299332080|8030daa7dfbf566204883d64-696942623|FHPSQ1R-qQPaRaU8KfFSGMM7b-w&sdk=android&format=json
		String query = this.entityName;
		query += "?access_tokey=" + facebook.getAccessToken();
		query += "&sdk=android&format=json";
		return query;
	}
}
