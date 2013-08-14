package com.aircandi.events;

import java.util.List;

import com.aircandi.service.objects.Entity;

@SuppressWarnings("ucd")
public class EntitiesChangedEvent {
	
	public final List<Entity> entities;

	public EntitiesChangedEvent(List<Entity> entities) {
		this.entities = entities;
	}
}
