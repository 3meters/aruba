package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import android.content.Intent;

import com.aircandi.Constants;
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
	public String				type;	// watch, nearby, network
	@Expose
	public Entity				entity;
	@Expose
	public User					user;
	@Expose
	public Entity				toEntity;
	@Expose
	public Number				sentDate;

	/* Client only */
	public Intent				intent;
	public String				title;
	public String				subtitle;

	public AirNotification() {}

	public static AirNotification setPropertiesFromMap(AirNotification notification, Map map, Boolean nameMapping) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		notification.action = (String) map.get("action");
		notification.type = (String) map.get("type");
		notification.title = (String) map.get("title");
		notification.subtitle = (String) map.get("subtitle");
		notification.sentDate = (Number) map.get("sentDate");

		if (map.get("entity") != null) {
			Map<String, Object> entityMap = (HashMap<String, Object>) map.get("entity");
			String schema = (String) entityMap.get("schema");
			if (schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
				notification.entity = Place.setPropertiesFromMap(new Place(), entityMap, nameMapping);
			}
			else if (schema.equals(Constants.SCHEMA_ENTITY_BEACON)) {
				notification.entity = Beacon.setPropertiesFromMap(new Beacon(), entityMap, nameMapping);
			}
			else if (schema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {
				notification.entity = Post.setPropertiesFromMap(new Post(), entityMap, nameMapping);
			}
			else if (schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)) {
				notification.entity = Candigram.setPropertiesFromMap(new Candigram(), entityMap, nameMapping);
			}
			else if (schema.equals(Constants.SCHEMA_ENTITY_APPLINK)) {
				notification.entity = Applink.setPropertiesFromMap(new Applink(), entityMap, nameMapping);
			}
			else if (schema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
				notification.entity = Comment.setPropertiesFromMap(new Comment(), entityMap, nameMapping);
			}
			else if (schema.equals(Constants.SCHEMA_ENTITY_USER)) {
				notification.entity = User.setPropertiesFromMap(new User(), entityMap, nameMapping);
			}
		}

		if (map.get("toEntity") != null) {
			Map<String, Object> entityMap = (HashMap<String, Object>) map.get("toEntity");
			String schema = (String) entityMap.get("schema");
			if (schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
				notification.toEntity = Place.setPropertiesFromMap(new Place(), entityMap, nameMapping);
			}
			else if (schema.equals(Constants.SCHEMA_ENTITY_BEACON)) {
				notification.toEntity = Beacon.setPropertiesFromMap(new Beacon(), entityMap, nameMapping);
			}
			else if (schema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {
				notification.toEntity = Post.setPropertiesFromMap(new Post(), entityMap, nameMapping);
			}
			else if (schema.equals(Constants.SCHEMA_ENTITY_APPLINK)) {
				notification.toEntity = Applink.setPropertiesFromMap(new Applink(), entityMap, nameMapping);
			}
			else if (schema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
				notification.toEntity = Comment.setPropertiesFromMap(new Comment(), entityMap, nameMapping);
			}
			else if (schema.equals(Constants.SCHEMA_ENTITY_USER)) {
				notification.toEntity = User.setPropertiesFromMap(new User(), entityMap, nameMapping);
			}
		}
		
		if (map.get("user") != null) {
			notification.user = User.setPropertiesFromMap(new User(), (HashMap<String, Object>) map.get("user"), nameMapping);
		}

		return notification;
	}

}