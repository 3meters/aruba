package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.Map;

import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Icon extends ServiceObject implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 455904759787968585L;

	@Expose
	public String				prefix;
	@Expose
	public String				suffix;

	public Icon() {}
	
	@Override
	public Icon clone() {
		try {
			final Icon icon = (Icon) super.clone();
			return icon;
		}
		catch (final CloneNotSupportedException ex) {
			throw new AssertionError();
		}
	}	
	

	public static Icon setPropertiesFromMap(Icon icon, Map map) {
		icon.prefix = (String) map.get("prefix");
		icon.suffix = (String) map.get("suffix");
		return icon;
	}
	
	public String getIconUri(int width, int height) {
		return prefix + String.valueOf(width) + "x" + String.valueOf(height) + suffix;
	}
}