package com.proxibase.aircandi.components;

import java.util.List;

import com.proxibase.service.objects.Entity;

public interface IEntityProvider {
	public List<Entity> loadEntities();

	public List<Entity> getEntities();

	public Boolean isMore();

	public void setMore(boolean more);
	
	public void reset();

	public Entity getParentEntity();
}