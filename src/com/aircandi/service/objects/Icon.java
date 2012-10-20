package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.HashMap;

import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
public class Icon implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 455904759787968585L;

	@Expose
	public String				prefix;
	@Expose
	public String				suffix;

	public Icon() {}

	public static Icon setFromPropertiesFromMap(Icon icon, HashMap map) {
		icon.prefix = (String) map.get("prefix");
		icon.suffix = (String) map.get("suffix");
		return icon;
	}
	
	public String getIconUri(int width, int height) {
		return prefix + String.valueOf(width) + "x" + String.valueOf(height) + suffix;
	}
}