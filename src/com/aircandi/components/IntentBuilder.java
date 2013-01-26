package com.aircandi.components;

import android.content.Context;
import android.content.Intent;

import com.aircandi.CandiConstants;
import com.aircandi.components.ProxiExplorer.EntityListType;

public class IntentBuilder {

	private Context			mContext;
	private Class<?>		mClass;

	private String			mEntityId;
	private String			mParentEntityId;
	private String			mEntityType;
	private CommandType		mCommandType;
	private String			mUserId;

	private String			mMessage;
	private EntityListType	mEntityListType;
	private String			mCollectionId;

	public IntentBuilder() {}

	public IntentBuilder(Context context, Class<?> clazz) {
		this.mContext = context;
		this.mClass = clazz;
	}

	public Intent create() {
		Intent intent = new Intent(mContext, mClass);

		if (mEntityId != null) {
			intent.putExtra(CandiConstants.EXTRA_ENTITY_ID, mEntityId);
		}

		if (mCommandType != null) {
			intent.putExtra(CandiConstants.EXTRA_COMMAND_TYPE, mCommandType.name());
		}

		if (mEntityType != null) {
			intent.putExtra(CandiConstants.EXTRA_ENTITY_TYPE, mEntityType);
		}

		if (mParentEntityId != null) {
			intent.putExtra(CandiConstants.EXTRA_PARENT_ENTITY_ID, mParentEntityId);
		}

		if (mMessage != null) {
			intent.putExtra(CandiConstants.EXTRA_MESSAGE, mMessage);
		}

		if (mUserId != null) {
			intent.putExtra(CandiConstants.EXTRA_USER_ID, mUserId);
		}

		if (mCollectionId != null) {
			intent.putExtra(CandiConstants.EXTRA_COLLECTION_ID, mCollectionId);
		}

		if (mEntityListType != null) {
			intent.putExtra(CandiConstants.EXTRA_LIST_TYPE, mEntityListType.name());
		}

		return intent;
	}

	public IntentBuilder setParentEntityId(String parentEntityId) {
		this.mParentEntityId = parentEntityId;
		return this;
	}

	public IntentBuilder setEntityType(String entityType) {
		this.mEntityType = entityType;
		return this;
	}

	public IntentBuilder setEntityId(String entityId) {
		this.mEntityId = entityId;
		return this;
	}

	public IntentBuilder setCollectionId(String collectionId) {
		mCollectionId = collectionId;
		return this;
	}

	public IntentBuilder setCommandType(CommandType commandType) {
		mCommandType = commandType;
		return this;
	}

	public IntentBuilder setUserId(String userId) {
		mUserId = userId;
		return this;
	}

	public IntentBuilder setEntityListType(EntityListType entityListType) {
		mEntityListType = entityListType;
		return this;
	}
}
