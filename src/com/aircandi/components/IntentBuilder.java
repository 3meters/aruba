package com.aircandi.components;

import android.content.Context;
import android.content.Intent;

import com.aircandi.Constants;
import com.aircandi.components.ProxiManager.ArrayListType;


public class IntentBuilder {

	private Context			mContext;
	private Class<?>		mClass;

	private String			mEntityId;
	private String			mParentEntityId;
	private String			mEntityType;
	private CommandType		mCommandType;
	private String			mUserId;
	private Boolean			mForceRefresh;

	private String			mMessage;
	private ArrayListType	mArrayListType;
	private String			mCollectionId;

	public IntentBuilder() {}

	public IntentBuilder(Context context, Class<?> clazz) {
		mContext = context;
		mClass = clazz;
	}

	public Intent create() {
		final Intent intent = new Intent(mContext, mClass);

		if (mEntityId != null) {
			intent.putExtra(Constants.EXTRA_ENTITY_ID, mEntityId);
		}

		if (mCommandType != null) {
			intent.putExtra(Constants.EXTRA_COMMAND_TYPE, mCommandType.name());
		}

		if (mEntityType != null) {
			intent.putExtra(Constants.EXTRA_ENTITY_TYPE, mEntityType);
		}

		if (mParentEntityId != null) {
			intent.putExtra(Constants.EXTRA_PARENT_ENTITY_ID, mParentEntityId);
		}

		if (mMessage != null) {
			intent.putExtra(Constants.EXTRA_MESSAGE, mMessage);
		}

		if (mUserId != null) {
			intent.putExtra(Constants.EXTRA_USER_ID, mUserId);
		}

		if (mCollectionId != null) {
			intent.putExtra(Constants.EXTRA_COLLECTION_ID, mCollectionId);
		}

		if (mArrayListType != null) {
			intent.putExtra(Constants.EXTRA_LIST_TYPE, mArrayListType.name());
		}

		if (mForceRefresh != null) {
			intent.putExtra(Constants.EXTRA_REFRESH_FORCE, mForceRefresh);
		}
		
		return intent;
	}

	public IntentBuilder setParentEntityId(String parentEntityId) {
		mParentEntityId = parentEntityId;
		return this;
	}

	public IntentBuilder setEntityType(String entityType) {
		mEntityType = entityType;
		return this;
	}

	public IntentBuilder setEntityId(String entityId) {
		mEntityId = entityId;
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

	@SuppressWarnings("ucd")
	public IntentBuilder setArrayListType(ArrayListType arrayListType) {
		mArrayListType = arrayListType;
		return this;
	}

	public Boolean getForceRefresh() {
		return mForceRefresh;
	}

	public IntentBuilder setForceRefresh(Boolean forceRefresh) {
		mForceRefresh = forceRefresh;
		return this;
	}
}
