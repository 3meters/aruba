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
	public String				type;
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

	public static Stat setPropertiesFromMap(Stat stat, Map map) {
		stat.type = (String) map.get("type");
		stat.countBy = (Number) map.get("countBy");
		return stat;
	}
}