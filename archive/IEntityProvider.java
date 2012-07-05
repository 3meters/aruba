package com.proxibase.aircandi.components;

import com.proxibase.service.objects.Entity;

public interface IEntityProvider {
	public EntityList<Entity> loadEntities();

	public EntityList<Entity> getEntities();

	public Entity getCollectionEntity();
}