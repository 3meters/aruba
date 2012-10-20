package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.HashMap;

import com.aircandi.service.Expose;


/**
 * @author Jayma
 */
public class Photo implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 455904759787968585L;

	@Expose
	public Number				createdAt;
	@Expose
	public String				prefix;
	@Expose
	public String				suffix;
	@Expose
	public Number				width;
	@Expose
	public Number				height;

	public Photo() {}

	public static Photo setFromPropertiesFromMap(Photo photo, HashMap map) {
		
		photo.createdAt = (Number) map.get("createdAt");
		photo.prefix = (String) map.get("prefix");
		photo.suffix = (String) map.get("suffix");
		photo.width = (Number) map.get("width");
		photo.height = (Number) map.get("height");
		
		return photo;
	}

}