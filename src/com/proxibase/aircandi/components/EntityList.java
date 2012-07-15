package com.proxibase.aircandi.components;

import java.util.ArrayList;
import java.util.List;

import com.proxibase.aircandi.components.ProxiExplorer.CollectionType;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.service.objects.Entity;

public class EntityList<T> extends ArrayList<T> {

	private static final long	serialVersionUID	= -2567383399125318332L;
	private List<String>		mCursorIds;
	private Integer				mCursorIndex		= 0;
	private CollectionType		mCollectionType;
	private Entity				mCollectionEntity;

	/*
	 * CollectionEntity is the entity that owns this collection. If CollectionEntity
	 * is null then this is a top level entity collection.
	 */

	public EntityList() {}

	public EntityList(CollectionType collectionType) {
		super();
		mCollectionType = collectionType;
	}

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

	/*
	 * The list created is a new object but the objects in the list
	 * are the same ones from the cloned list by reference. Make a change
	 * and it will effect the object in both lists. 
	 *
	 * (non-Javadoc)
	 * @see java.util.ArrayList#clone()
	 */
	@Override
	public EntityList<T> clone() {
		EntityList<T> entityList = (EntityList<T>) super.clone();
		entityList.setCollectionType(mCollectionType);
		entityList.setCursorIds(mCursorIds);
		entityList.setCursorIndex(mCursorIndex);
		entityList.setCollectionEntity(mCollectionEntity);
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
		entityList.setCollectionType(mCollectionType);
		entityList.setCursorIds(mCursorIds);
		entityList.setCursorIndex(mCursorIndex);
		entityList.setCollectionEntity(mCollectionEntity);
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
		entityList.setCollectionType(mCollectionType);
		entityList.setCursorIds(mCursorIds);
		entityList.setCursorIndex(mCursorIndex);
		entityList.setCollectionEntity(mCollectionEntity);
		return entityList;
	}

	public Boolean isMore() {
		/*
		 * CursorIndex should be advanced whenever another set
		 * of entities are chunked in using the cursor ids.
		 */
		Boolean more = false;
		if (mCursorIndex == null) {
			mCursorIndex = this.size();
		}
		if (mCursorIds != null && mCursorIds.size() > mCursorIndex) {
			more = true;
		}
		return more;
	}

	public int getStartIndex() {
		int startIndex = this.size();
		if (mCursorIndex != null) {
			startIndex = mCursorIndex;
		}
		return startIndex;
	}

	public int getEndIndex() {
		int endIndex = this.size() + CandiConstants.RADAR_ENTITY_LIMIT;
		if (mCursorIndex != null) {
			endIndex = mCursorIndex + CandiConstants.RADAR_ENTITY_LIMIT;
		}
		if (endIndex > mCursorIds.size()) {
			endIndex = mCursorIds.size();
		}
		return endIndex;
	}

	public CollectionType getCollectionType() {
		return mCollectionType;
	}

	public void setCollectionType(CollectionType collectionType) {
		mCollectionType = collectionType;
	}

	public Entity getCollectionEntity() {
		return mCollectionEntity;
	}

	public void setCollectionEntity(Entity collectionEntity) {
		mCollectionEntity = collectionEntity;
	}

	public List<String> getCursorIds() {
		return mCursorIds;
	}

	public void setCursorIds(List<String> cursorIds) {
		mCursorIds = cursorIds;
		mCursorIndex = null;
	}

	public Integer getCursorIndex() {
		return mCursorIndex;
	}

	public void setCursorIndex(Integer cursorIndex) {
		mCursorIndex = cursorIndex;
	}
}
