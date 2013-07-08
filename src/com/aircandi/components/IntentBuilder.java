package com.aircandi.components;

import android.content.Context;
import android.content.Intent;

import com.aircandi.Constants;
import com.aircandi.service.HttpService;
import com.aircandi.service.objects.Entity;
import com.aircandi.ui.EntityList;
import com.aircandi.ui.EntityList.ListMode;

public class IntentBuilder {

	private Context		mContext;
	private Class<?>	mClass;

	private String		mUserId;
	private Entity		mEntity;
	private String		mEntityId;
	private String		mEntityParentId;
	private String		mEntitySchema;

	private String		mListSchema;
	private Boolean		mListNewEnabled;
	private Integer		mListItemResId;
	private ListMode	mListMode;

	private Boolean		mForceRefresh;
	private String		mMessage;

	public IntentBuilder() {}

	public IntentBuilder(Context context, Class<?> clazz) {
		mContext = context;
		mClass = clazz;
	}

	public Intent create() {
		Intent intent = new Intent();
		if (mContext != null && mClass != null) {
			intent = new Intent(mContext, mClass);
		}

		if (mUserId != null) {
			intent.putExtra(Constants.EXTRA_USER_ID, mUserId);
		}

		if (mEntityId != null) {
			intent.putExtra(Constants.EXTRA_ENTITY_ID, mEntityId);
		}

		if (mEntity != null) {
			final String jsonEntity = HttpService.objectToJson(mEntity);
			intent.putExtra(Constants.EXTRA_ENTITY, jsonEntity);
		}

		if (mEntitySchema != null) {
			intent.putExtra(Constants.EXTRA_ENTITY_SCHEMA, mEntitySchema);
		}

		if (mEntityParentId != null) {
			intent.putExtra(Constants.EXTRA_ENTITY_PARENT_ID, mEntityParentId);
		}

		if (mListMode != null) {
			intent.putExtra(Constants.EXTRA_LIST_MODE, mListMode.name());
		}

		if (mListSchema != null) {
			intent.putExtra(Constants.EXTRA_LIST_SCHEMA, mListSchema);
		}

		if (mListItemResId != null) {
			intent.putExtra(Constants.EXTRA_LIST_ITEM_RESID, mListItemResId);
		}

		if (mListNewEnabled != null) {
			intent.putExtra(Constants.EXTRA_LIST_NEW_ENABLED, mListNewEnabled);
		}

		if (mForceRefresh != null) {
			intent.putExtra(Constants.EXTRA_REFRESH_FORCE, mForceRefresh);
		}

		if (mMessage != null) {
			intent.putExtra(Constants.EXTRA_MESSAGE, mMessage);
		}

		return intent;
	}

	public IntentBuilder setEntityParentId(String entityParentId) {
		mEntityParentId = entityParentId;
		return this;
	}

	public IntentBuilder setEntitySchema(String entitySchema) {
		mEntitySchema = entitySchema;
		return this;
	}

	public IntentBuilder setEntityId(String entityId) {
		mEntityId = entityId;
		return this;
	}

	public IntentBuilder setUserId(String userId) {
		mUserId = userId;
		return this;
	}

	public IntentBuilder setListMode(EntityList.ListMode arrayListType) {
		mListMode = arrayListType;
		return this;
	}

	public IntentBuilder setForceRefresh(Boolean forceRefresh) {
		mForceRefresh = forceRefresh;
		return this;
	}

	public IntentBuilder setListSchema(String listSchema) {
		mListSchema = listSchema;
		return this;
	}

	public IntentBuilder setListNewEnabled(Boolean listNewEnabled) {
		mListNewEnabled = listNewEnabled;
		return this;
	}

	public IntentBuilder setListItemResId(Integer listItemResId) {
		mListItemResId = listItemResId;
		return this;
	}

	public void setClass(Class<?> clazz) {
		mClass = clazz;
	}

	public IntentBuilder setEntity(Entity entity) {
		mEntity = entity;
		return this;
	}
}
