package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.HashMap;

import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
public class Source extends ServiceObject implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 4362288672245719448L;

	@Expose
	public String				name;
	@Expose
	public String				source;
	@Expose
	public String				id;
	@Expose
	public String				url;

	public Source() {}
	
	public Source(String source, String id) {
		this.source = source;
		this.id = id;
	}

	public static Source setPropertiesFromMap(Source source, HashMap map) {
		source.name = (String) map.get("name");
		source.source = (String) map.get("source");
		source.id = (String) map.get("id");
		source.url = (String) map.get("url");
		return source;
	}
}