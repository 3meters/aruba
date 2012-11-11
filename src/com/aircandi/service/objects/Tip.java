package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.HashMap;

import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
public class Tip extends ServiceObject implements Cloneable, Serializable {

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
	@Expose
	public User					user;

	public Tip() {}

	@Override
	public Tip clone() {
		try {
			final Tip tip = (Tip) super.clone();
			return tip;
		}
		catch (final CloneNotSupportedException ex) {
			throw new AssertionError();
		}
	}	
	
	public static Tip setPropertiesFromMap(Tip tip, HashMap map) {

		tip.createdAt = (Number) map.get("createdAt");
		tip.text = (String) map.get("text");
		tip.uri = (String) map.get("url");
		
		if (map.get("photo") != null) {
			tip.photo = (Photo) Photo.setPropertiesFromMap(new Photo(), (HashMap<String, Object>) map.get("photo"));
		}
		
		if (map.get("user") != null) {
			tip.user = (User) User.setPropertiesFromMap(new User(), (HashMap<String, Object>) map.get("user"));
		}

		return tip;
	}
}