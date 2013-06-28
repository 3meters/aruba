package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import android.content.Intent;

import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class AirNotification extends ServiceObject implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 4362288672244719448L;

	@Expose
	public String				action;
	@Expose
	public Entity				entity;
	@Expose
	public User					user;
	@Expose
	public String				type;
	@Expose
	public String				title;
	@Expose
	public String				subtitle;
	@Expose
	public String				message;
	@Expose
	public Number				sentDate;
	
	/* Client only */
	public Intent 				intent;
	

	public AirNotification() {}

	public static AirNotification setPropertiesFromMap(AirNotification notification, Map map, Boolean nameMapping) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		notification.action = (String) map.get("action");
		
		if (map.get("entity") != null) {
			String type = (String) map.get("type");
			if (type.equals("place")) {
				notification.entity = Place.setPropertiesFromMap(new Place(), (HashMap<String, Object>) map.get("entity"), nameMapping);
			}
			else if (type.equals("beacon")) {
				notification.entity = Beacon.setPropertiesFromMap(new Beacon(), (HashMap<String, Object>) map.get("entity"), nameMapping);
			}
			else if (type.equals("post")) {
				notification.entity = Post.setPropertiesFromMap(new Post(), (HashMap<String, Object>) map.get("entity"), nameMapping);
			}
			else if (type.equals("applink")) {
				notification.entity = Applink.setPropertiesFromMap(new Applink(), (HashMap<String, Object>) map.get("entity"), nameMapping);
			}
			else if (type.equals("comment")) {
				notification.entity = Comment.setPropertiesFromMap(new Comment(), (HashMap<String, Object>) map.get("entity"), nameMapping);
			}
			else if (type.equals("user")) {
				notification.entity = User.setPropertiesFromMap(new User(), (HashMap<String, Object>) map.get("entity"), nameMapping);
			}			
		}

		if (map.get("user") != null) {
			notification.user = User.setPropertiesFromMap(new User(), (HashMap<String, Object>) map.get("user"), nameMapping);
		}
		
		notification.type = (String) map.get("type");
		notification.title = (String) map.get("title");
		notification.subtitle = (String) map.get("subtitle");
		notification.message = (String) map.get("message");
		notification.sentDate = (Number) map.get("sentDate");

		return notification;
	}

}