package com.aircandi.components;

import java.util.ArrayList;

import com.aircandi.service.objects.Entity;

public class EntityList<T> extends ArrayList<T> {

	private static final long	serialVersionUID	= -2567383399125318332L;

	/*
	 * CollectionEntity is the entity that owns this collection. If CollectionEntity
	 * is null then this is a top level entity collection.
	 */

	public EntityList() {}

	public EntityList(final int capacity) {
		super(capacity);
	}

	public boolean containsKey(String key) {

		for (int i = 0; i < this.size(); i++) {
			Entity entity = (Entity) this.get(i);
			if (entity.id.equals(key)) {
				return true;
			}
		}
		return false;
	}

	public Entity getByKey(String key) {

		for (int i = 0; i < this.size(); i++) {
			Entity entity = (Entity) this.get(i);
			if (entity.id.equals(key)) {
				return entity;
			}
		}
		return null;
	}

	/*
	 * The list created is a new object but the objects in the list
	 * are the same ones from the cloned list by reference. Make a change
	 * and it will effect the object in both lists.
	 * 
	 * (non-Javadoc)
	 * 
	 * @see java.util.ArrayList#clone()
	 */
	@Override
	public EntityList<T> clone() {
		EntityList<T> entityList = (EntityList<T>) super.clone();
		return entityList;
	}

	/*
	 * The list created is a new object and the entity objects in the list
	 * are new objects. Make a change to an object in the new list and
	 * it will not effect the object in the old list.
	 */
	public EntityList<Entity> copy() {
		EntityList<Entity> entityList = new EntityList(this.size());
		for (int i = 0; i < this.size(); i++) {
			Entity entity = (Entity) this.get(i);
			entityList.add(entity.clone());
		}
		return entityList;
	}

	/*
	 * The list created is a new object and the entity objects in the list
	 * are completely new objects including all their object properties.
	 * Note: serialization is sloooooow.
	 */
	public EntityList<Entity> deepCopy() {
		EntityList<Entity> entityList = new EntityList(this.size());
		for (int i = 0; i < this.size(); i++) {
			Entity entity = (Entity) this.get(i);
			entityList.add(entity.deepCopy());
		}
		return entityList;
	}
}
