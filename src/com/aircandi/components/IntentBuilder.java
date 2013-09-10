package com.aircandi.components;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.aircandi.Constants;
import com.aircandi.service.HttpService;
import com.aircandi.service.objects.Entity;

public class IntentBuilder {

	private Context		mContext;
	private Class<?>	mClass;
	private String		mAction;
	private Uri			mData;

	private Entity		mEntity;
	private String		mEntityId;
	private String		mEntityParentId;
	private String		mEntitySchema;

	private String		mListLinkSchema;
	private String		mListLinkType;
	private String		mListLinkDirection;
	private String		mListTitle;
	private Boolean		mListNewEnabled;
	private Integer		mListItemResId;

	private Bundle		mExtras;

	private Boolean		mForceRefresh;
	private String		mMessage;

	public IntentBuilder() {}

	public IntentBuilder(Context context, Class<?> clazz) {
		mContext = context;
		mClass = clazz;
	}

	public IntentBuilder(String action) {
		mAction = action;
	}

	public Intent create() {
		Intent intent = new Intent();
		if (mContext != null && mClass != null) {
			intent = new Intent(mContext, mClass);
		}
		else if (mAction != null) {
			intent = new Intent(mAction);
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

		if (mListLinkType != null) {
			intent.putExtra(Constants.EXTRA_LIST_LINK_TYPE, mListLinkType);
		}

		if (mListLinkSchema != null) {
			intent.putExtra(Constants.EXTRA_LIST_LINK_SCHEMA, mListLinkSchema);
		}

		if (mListLinkDirection != null) {
			intent.putExtra(Constants.EXTRA_LIST_LINK_DIRECTION, mListLinkDirection);
		}

		if (mListItemResId != null) {
			intent.putExtra(Constants.EXTRA_LIST_ITEM_RESID, mListItemResId);
		}

		if (mListNewEnabled != null) {
			intent.putExtra(Constants.EXTRA_LIST_NEW_ENABLED, mListNewEnabled);
		}
		
		if (mListTitle != null) {
			intent.putExtra(Constants.EXTRA_LIST_TITLE, mListTitle);
		}

		if (mForceRefresh != null) {
			intent.putExtra(Constants.EXTRA_REFRESH_FROM_SERVICE, mForceRefresh);
		}

		if (mMessage != null) {
			intent.putExtra(Constants.EXTRA_MESSAGE, mMessage);
		}

		if (mExtras != null) {
			intent.putExtras(mExtras);
		}

		if (mData != null) {
			intent.setData(mData);
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

	public IntentBuilder setForceRefresh(Boolean forceRefresh) {
		mForceRefresh = forceRefresh;
		return this;
	}

	public IntentBuilder setListLinkSchema(String listLinkSchema) {
		mListLinkSchema = listLinkSchema;
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

	public Bundle getExtras() {
		return mExtras;
	}

	public IntentBuilder setExtras(Bundle extras) {
		mExtras = extras;
		return this;
	}

	public IntentBuilder setData(Uri data) {
		mData = data;
		return this;
	}

	public IntentBuilder setListLinkType(String listLinkType) {
		mListLinkType = listLinkType;
		return this;
	}

	public IntentBuilder setListLinkDirection(String listLinkDirection) {
		mListLinkDirection = listLinkDirection;
		return this;
	}

	public String getListTitle() {
		return mListTitle;
	}

	public IntentBuilder setListTitle(String listTitle) {
		mListTitle = listTitle;
		return this;
	}

}
