package com.proxibase.aircandi.components;

/**
 * Encapsulates everything required to execute a command including any data
 * that has to be passed to another activity. In some cases, the data included
 * is used to lookup data that is required.
 * 
 * @author Jayma
 */
public class Command {

	public CommandType	type	= CommandType.View;
	public String		label;
	public String		entityId;
	public String		entityType;
	public String		entityParentId;
	public String		activityName;
	public Integer		iconResourceId;

	public Command() {}

	public Command(CommandType type) {
		this.type = type;
	}

	public Command(CommandType type, String label, String activityName, String entityType, String entityId, String entityParentId, Integer iconResourceId) {
		this.type = type;
		this.label = label;
		this.activityName = activityName;
		this.entityType = entityType;
		this.entityId = entityId;
		this.entityParentId = entityParentId;
		this.iconResourceId = iconResourceId;
	}

	public static enum CommandType {
		None,
		View,
		Edit,
		New,
		Dialog,
		ChunkEntities,
		ChunkChildEntities
	}
}