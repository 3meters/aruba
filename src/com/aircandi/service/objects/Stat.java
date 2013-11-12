package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.Map;

import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
public class Stat extends ServiceObject implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 455904759787968585L;

	@Expose
	public String				event;
	@Expose
	public Number				countBy;

	public Stat() {}

	
	@Override
	public Stat clone() {
		try {
			final Stat stat = (Stat) super.clone();
			return stat;
		}
		catch (final CloneNotSupportedException ex) {
			throw new AssertionError();
		}
	}

	public static Stat setPropertiesFromMap(Stat stat, Map map, Boolean nameMapping) {
		stat.event = (String) map.get("event");
		stat.countBy = (Number) map.get("countBy");
		return stat;
	}
}