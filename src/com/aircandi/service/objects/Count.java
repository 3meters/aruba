package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.Map;

import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
public class Count extends ServiceObject implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 455904759787968585L;

	@Expose
	public String				type;
	@Expose
	public String				schema;
	@Expose
	public Boolean				inactive = false;
	@Expose
	public Number				count;

	public Count() {}

	public Count(String type, String schema, Number count) {
		this.type = type;
		this.schema = schema;
		this.count = count;
	}

	@Override
	public Count clone() {
		try {
			final Count count = (Count) super.clone();
			return count;
		}
		catch (final CloneNotSupportedException ex) {
			throw new AssertionError();
		}
	}

	public static Count setPropertiesFromMap(Count stat, Map map, Boolean nameMapping) {
		stat.type = (String) map.get("type");
		if (stat.type == null && map.get("event") != null) {
			stat.type = (String) map.get("event");
		}
		stat.schema = (String) map.get("schema");
		stat.inactive = (Boolean) map.get("inactive");
		stat.count = (Number) map.get("count");
		if (stat.count == null && map.get("countBy") != null) {
			stat.count = (Number) map.get("countBy");
		}
		return stat;
	}
}