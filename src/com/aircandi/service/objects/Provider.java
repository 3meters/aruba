package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.Map;

import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Provider extends ServiceObject implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 4362288672245719998L;

	@Expose
	public String				id;
	@Expose
	public String				type;

	public Provider() {}
	
	public Provider(String id, String type) {
		this.id = id;
		this.type = type;
	}

	public static Provider setPropertiesFromMap(Provider provider, Map map) {
		provider.id = (String) map.get("id");
		provider.type = (String) map.get("type");
		return provider;
	}
}