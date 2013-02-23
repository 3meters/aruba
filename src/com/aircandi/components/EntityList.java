package com.aircandi.components;

import java.util.ArrayList;

public class EntityList<T> extends ArrayList<T> {

	private static final long	serialVersionUID	= -2567383399125318332L;

	/*
	 * CollectionEntity is the entity that owns this collection. If CollectionEntity
	 * is null then this is a top level entity collection.
	 */

	public EntityList() {}

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
		final EntityList<T> entityList = (EntityList<T>) super.clone();
		return entityList;
	}
}
