package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.aircandi.Constants;
import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Action extends ServiceObject implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 4362288672244719448L;

	@Expose
	private String				event;										// insert, move, expand, refresh, insert_place, move_candigram, etc
	@Expose
	public User					user;										// can be null
	@Expose
	public Entity				entity;
	@Expose
	public Entity				toEntity;
	@Expose
	public Entity				fromEntity;

	public Action() {}

	public static Action setPropertiesFromMap(Action action, Map map, Boolean nameMapping) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		action.event = (String) map.get("event");

		if (map.get("entity") != null) {
			Map<String, Object> entityMap = (HashMap<String, Object>) map.get("entity");
			String schema = (String) entityMap.get("schema");
			if (schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
				action.entity = Place.setPropertiesFromMap(new Place(), entityMap, nameMapping);
			}
			else if (schema.equals(Constants.SCHEMA_ENTITY_BEACON)) {
				action.entity = Beacon.setPropertiesFromMap(new Beacon(), entityMap, nameMapping);
			}
			else if (schema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {
				action.entity = Post.setPropertiesFromMap(new Post(), entityMap, nameMapping);
			}
			else if (schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)) {
				action.entity = Candigram.setPropertiesFromMap(new Candigram(), entityMap, nameMapping);
			}
			else if (schema.equals(Constants.SCHEMA_ENTITY_APPLINK)) {
				action.entity = Applink.setPropertiesFromMap(new Applink(), entityMap, nameMapping);
			}
			else if (schema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
				action.entity = Comment.setPropertiesFromMap(new Comment(), entityMap, nameMapping);
			}
			else if (schema.equals(Constants.SCHEMA_ENTITY_USER)) {
				action.entity = User.setPropertiesFromMap(new User(), entityMap, nameMapping);
			}
		}

		if (map.get("toEntity") != null) {
			Map<String, Object> entityMap = (HashMap<String, Object>) map.get("toEntity");
			String schema = (String) entityMap.get("schema");
			if (schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
				action.toEntity = Place.setPropertiesFromMap(new Place(), entityMap, nameMapping);
			}
			else if (schema.equals(Constants.SCHEMA_ENTITY_BEACON)) {
				action.toEntity = Beacon.setPropertiesFromMap(new Beacon(), entityMap, nameMapping);
			}
			else if (schema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {
				action.toEntity = Post.setPropertiesFromMap(new Post(), entityMap, nameMapping);
			}
			else if (schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)) {
				action.toEntity = Candigram.setPropertiesFromMap(new Candigram(), entityMap, nameMapping);
			}
			else if (schema.equals(Constants.SCHEMA_ENTITY_APPLINK)) {
				action.toEntity = Applink.setPropertiesFromMap(new Applink(), entityMap, nameMapping);
			}
			else if (schema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
				action.toEntity = Comment.setPropertiesFromMap(new Comment(), entityMap, nameMapping);
			}
			else if (schema.equals(Constants.SCHEMA_ENTITY_USER)) {
				action.toEntity = User.setPropertiesFromMap(new User(), entityMap, nameMapping);
			}
		}

		if (map.get("fromEntity") != null) {
			Map<String, Object> entityMap = (HashMap<String, Object>) map.get("fromEntity");
			String schema = (String) entityMap.get("schema");
			if (schema.equals(Constants.SCHEMA_ENTITY_PLACE)) {
				action.fromEntity = Place.setPropertiesFromMap(new Place(), entityMap, nameMapping);
			}
			else if (schema.equals(Constants.SCHEMA_ENTITY_BEACON)) {
				action.fromEntity = Beacon.setPropertiesFromMap(new Beacon(), entityMap, nameMapping);
			}
			else if (schema.equals(Constants.SCHEMA_ENTITY_PICTURE)) {
				action.fromEntity = Post.setPropertiesFromMap(new Post(), entityMap, nameMapping);
			}
			else if (schema.equals(Constants.SCHEMA_ENTITY_CANDIGRAM)) {
				action.fromEntity = Candigram.setPropertiesFromMap(new Candigram(), entityMap, nameMapping);
			}
			else if (schema.equals(Constants.SCHEMA_ENTITY_APPLINK)) {
				action.fromEntity = Applink.setPropertiesFromMap(new Applink(), entityMap, nameMapping);
			}
			else if (schema.equals(Constants.SCHEMA_ENTITY_COMMENT)) {
				action.fromEntity = Comment.setPropertiesFromMap(new Comment(), entityMap, nameMapping);
			}
			else if (schema.equals(Constants.SCHEMA_ENTITY_USER)) {
				action.fromEntity = User.setPropertiesFromMap(new User(), entityMap, nameMapping);
			}
		}

		if (map.get("user") != null) {
			action.user = User.setPropertiesFromMap(new User(), (HashMap<String, Object>) map.get("user"), nameMapping);
		}

		return action;
	}

	public String getEventCategory() {
		if (this.event.contains("insert")) return EventType.INSERT;
		if (this.event.contains("move")) return EventType.MOVE;
		if (this.event.contains("expand")) return EventType.EXPAND;
		if (this.event.contains("refresh")) return EventType.REFRESH;
		return null;
	}

	// --------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------

	public static class EventType {
		public static String	INSERT	= "insert";
		public static String	MOVE	= "move";
		public static String	EXPAND	= "expand";
		public static String	REFRESH	= "refresh";
	}
}