package com.aircandi.components;

import java.util.ArrayList;
import java.util.Comparator;

import com.aircandi.core.CandiConstants;
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

	public static class SortEntitiesByModifiedDate implements Comparator<Entity> {

		@Override
		public int compare(Entity object1, Entity object2) {
			if (object1.modifiedDate.longValue() < object2.modifiedDate.longValue()) {
				return 1;
			}
			else if (object1.modifiedDate.longValue() == object2.modifiedDate.longValue()) {
				return 0;
			}
			return -1;
		}
	}

	public static class SortEntitiesBySignalLevelDiscoveryTimeModifiedDate implements Comparator<Entity> {

		@Override
		public int compare(Entity entity1, Entity entity2) {

			/* global versus user */
			if (!entity1.global && entity2.global) {
				return -1;
			}
			if (entity1.global && !entity2.global) {
				return 1;
			}
			else {
				/* synthetics */
				if (!entity1.synthetic && entity2.synthetic) {
					return -1;
				}
				if (entity1.synthetic && !entity2.synthetic) {
					return 1;
				}
				else {
					/*
					 * Signal level
					 * 
					 * Rounded to produce buckets for more sorting stability.
					 */
					if (entity1.synthetic) {
						if (entity1.place.location.distance.intValue() > entity2.place.location.distance.intValue()) {
							return -1;
						}
						else if (entity1.place.location.distance.intValue() < entity2.place.location.distance.intValue()) {
							return 1;
						}
						else {
							return 0;
						}
					}
					else {
						if ((entity1.getBeacon().getAvgBeaconLevel() / CandiConstants.RADAR_BEACON_SIGNAL_BUCKET_SIZE) > (entity2.getBeacon()
								.getAvgBeaconLevel() / CandiConstants.RADAR_BEACON_SIGNAL_BUCKET_SIZE)) {
							return -1;
						}
						else if ((entity1.getBeacon().getAvgBeaconLevel() / CandiConstants.RADAR_BEACON_SIGNAL_BUCKET_SIZE) < (entity2.getBeacon()
								.getAvgBeaconLevel() / CandiConstants.RADAR_BEACON_SIGNAL_BUCKET_SIZE)) {
							return 1;
						}
						else {
							/*
							 * Discovery time
							 * 
							 * Rounded to produce a 5 second bucket that will get further sorted by recent activity
							 */
							if (entity1.discoveryTime != null && entity2.discoveryTime != null) {
								if ((entity1.discoveryTime.getTime() / 1000) > (entity2.discoveryTime.getTime() / 1000)) {
									return -1;
								}
								else if ((entity1.discoveryTime.getTime() / 1000) < (entity2.discoveryTime.getTime() / 1000)) {
									return 1;
								}
								else {
									/* Modified date */
									if (entity1.modifiedDate.longValue() > entity2.modifiedDate.longValue()) {
										return -1;
									}
									else if (entity1.modifiedDate.longValue() < entity2.modifiedDate.longValue()) {
										return 1;
									}
									else {
										return 0;
									}
								}
							}
							else {
								/* Modified date */
								if (entity1.modifiedDate.longValue() > entity2.modifiedDate.longValue()) {
									return -1;
								}
								else if (entity1.modifiedDate.longValue() < entity2.modifiedDate.longValue()) {
									return 1;
								}
								else {
									return 0;
								}
							}
						}
					}
				}
			}
		}
	}
}
