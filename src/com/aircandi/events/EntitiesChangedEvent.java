package com.aircandi.events;

import java.util.List;

import com.aircandi.service.objects.Entity;

@SuppressWarnings("ucd")
public class EntitiesChangedEvent {

	public final List<Entity>	entities;
	public final String			changeSource;

	public EntitiesChangedEvent(List<Entity> entities, String changeSource) {
		this.entities = entities;
		this.changeSource = changeSource;
	}
}
