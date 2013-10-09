package com.aircandi.service.objects;

import com.aircandi.service.objects.Link.Direction;

/**
 * @author Jayma
 */
public class ShortcutSettings {

	public String		linkType;
	public String		linkTargetSchema;
	public Direction	direction;
	public Boolean		linkInactive;
	public Boolean		synthetic		= false;
	public Boolean		groupedByApp	= false;
	public Class<?>		appClass;

	public ShortcutSettings() {}

	public ShortcutSettings(String linkType, String linkTargetSchema, Direction direction, Boolean linkInactive, Boolean synthetic, Boolean groupedByApp) {
		this.linkType = linkType;
		this.linkTargetSchema = linkTargetSchema;
		this.linkInactive = linkInactive;
		this.direction = direction;
		this.synthetic = synthetic;
		this.groupedByApp = groupedByApp;
	}
}