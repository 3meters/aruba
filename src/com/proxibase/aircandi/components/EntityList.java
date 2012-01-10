package com.proxibase.aircandi.components;

import java.util.ArrayList;

import com.proxibase.sdk.android.proxi.consumer.Entity;

public class EntityList<T> extends ArrayList<T> {

	private static final long	serialVersionUID	= -2567383399125318332L;

	public EntityList() {}

	public EntityList(final int capacity) {
		super(capacity);
	}

	public boolean containsKey(Integer key) {

		for (int i = 0; i < this.size(); i++) {
			Entity entity = (Entity) this.get(i);
			if (entity.id.equals(key)) {
				return true;
			}
		}
		return false;
	}

	public Entity getByKey(Integer key) {

		for (int i = 0; i < this.size(); i++) {
			Entity entity = (Entity) this.get(i);
			if (entity.id.equals(key)) {
				return entity;
			}
		}
		return null;
	}
}
