package com.proxibase.aircandi.components;

/**
 * Encapsulates everything required to execute a command including any data
 * that has to be passed to another activity. In some cases, the data included
 * is used to lookup data that is required.
 * 
 * @author Jayma
 */
public class Command {

	public CommandVerb	verb	= CommandVerb.View;
	public String		label;
	public Integer		entityId;
	public String		entityType;
	public Integer		entityParentId;
	public String		activityName;
	public Integer		iconResourceId;

	public Command() {}

	public Command(CommandVerb verb) {
		this.verb = verb;
	}

	public Command(CommandVerb verb, String label, String activityName, String entityType, Integer entityId, Integer entityParentId, Integer iconResourceId) {
		this.verb = verb;
		this.label = label;
		this.activityName = activityName;
		this.entityType = entityType;
		this.entityId = entityId;
		this.entityParentId = entityParentId;
		this.iconResourceId = iconResourceId;
	}

	public static enum CommandVerb {
		None, View, Edit, New, Dialog
	}
}