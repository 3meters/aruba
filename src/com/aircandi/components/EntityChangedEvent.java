package com.aircandi.components;

import com.aircandi.service.objects.Entity;

@SuppressWarnings("ucd")
public class EntityChangedEvent {

	public final Entity	entity;

	public EntityChangedEvent(Entity entity) {
		this.entity = entity;
	}
}