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
	public String				type;
	@Expose
	public String				title;
	@Expose
	public String				subtitle;
	@Expose
	public String				message;
	@Expose
	public Entity				entity;
	@Expose
	public Comment				comment;
	@Expose
	public User					user;
	@Expose
	public Number				sentDate;
	
	/* Client only */
	public Intent 				intent;
	

	public AirNotification() {}

	public static AirNotification setPropertiesFromMap(AirNotification notification, Map map) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		notification.title = (String) map.get("title");
		notification.subtitle = (String) map.get("subtitle");
		notification.type = (String) map.get("type");
		notification.message = (String) map.get("message");
		notification.sentDate = (Number) map.get("sentDate");

		if (map.get("entity") != null) {
			notification.entity = Entity.setPropertiesFromMap(new Entity(), (HashMap<String, Object>) map.get("entity"));
		}

		if (map.get("comment") != null) {
			notification.comment = Comment.setPropertiesFromMap(new Comment(), (HashMap<String, Object>) map.get("comment"));
		}

		if (map.get("user") != null) {
			notification.user = User.setPropertiesFromMap(new User(), (HashMap<String, Object>) map.get("user"));
		}
		return notification;
	}

}