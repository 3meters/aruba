package com.aircandi.service.objects;

import com.aircandi.service.objects.Link.Direction;

/**
 * @author Jayma
 */
public class ShortcutSettings {

	public String		linkType;
	public String		targetSchema;
	public Direction	direction;
	public Boolean		synthetic	= false;

	public ShortcutSettings() {}

	public ShortcutSettings(String linkType, String targetSchema, Direction direction, Boolean synthetic) {
		this.linkType = linkType;
		this.targetSchema = targetSchema;
		this.direction = direction;
		this.synthetic = synthetic;
	}

}