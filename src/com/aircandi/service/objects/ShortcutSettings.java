package com.aircandi.service.objects;

import com.aircandi.service.objects.Link.Direction;

/**
 * @author Jayma
 */
public class ShortcutSettings {

	public String		linkType;
	public String		linkSchema;
	public Direction	direction;
	public Boolean		synthetic		= false;
	public Boolean		groupedByApp	= false;
	public Class<?>		appClass;

	public ShortcutSettings() {}

	public ShortcutSettings(String linkType, String linkSchema, Direction direction, Boolean synthetic, Boolean groupedByApp) {
		this.linkType = linkType;
		this.linkSchema = linkSchema;
		this.direction = direction;
		this.synthetic = synthetic;
		this.groupedByApp = groupedByApp;
	}	
}