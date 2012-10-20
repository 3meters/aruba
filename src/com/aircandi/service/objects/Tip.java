package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.HashMap;

import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
public class Tip implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 455904759787968585L;

	@Expose
	public Number				createdAt;
	@Expose
	public String				text;
	/*
	 * More info, could be an article/site/blog that contained the tip. 
	 */
	@Expose
	public String				uri;
	/*
	 * If there is a photo for this tip and the tip is not already container 
	 * inside of a photo element, details about the photo.
	 */
	@Expose
	public Photo				photo;

	public Tip() {}

	public static Tip setFromPropertiesFromMap(Tip tip, HashMap map) {

		tip.createdAt = (Number) map.get("createdAt");
		tip.text = (String) map.get("text");
		tip.uri = (String) map.get("url");

		return tip;
	}
}