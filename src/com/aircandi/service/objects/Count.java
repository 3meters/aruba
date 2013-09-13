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
	public Number				count;

	public Count() {}

	public Count(String type, Number count) {
		this.type = type;
		this.count = count;
	}

	@Override
	public Count clone() {
		try {
			final Count stat = (Count) super.clone();
			return stat;
		}
		catch (final CloneNotSupportedException ex) {
			throw new AssertionError();
		}
	}

	public static Count setPropertiesFromMap(Count stat, Map map, Boolean nameMapping) {
		stat.type = (String) map.get("TYPE");
		stat.count = (Number) map.get("count");
		return stat;
	}
}