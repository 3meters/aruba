package com.aircandi.components;

import android.content.Context;
import android.content.Intent;

import com.aircandi.components.CommandType;
import com.aircandi.components.ProxiExplorer.EntityTree;
import com.aircandi.service.objects.Beacon;
import com.aircandi.service.objects.Entity;
import com.aircandi.service.objects.GeoLocation;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.aircandi.R;

public class IntentBuilder {

	private Context		mContext;
	private String		mEntityId;
	private String		mParentEntityId;
	private GeoLocation	mEntityLocation;
	private String		mEntityType;
	private String		mMessage;
	private CommandType	mCommandType;
	private String		mBeaconId;
	private Boolean		mStripChildEntities	= true;
	private Boolean		mNavigationTop		= false;
	private Class<?>	mClass;
	private String		mCollectionId;
	private EntityTree	mEntityTree;

	public IntentBuilder() {}

	public IntentBuilder(Context context, Class<?> clazz) {
		this.mContext = context;
		this.mClass = clazz;
	}

	public Intent create() {
		Intent intent = new Intent(mContext, mClass);

		/* We want to make sure that any child entities don't get serialized */
		GsonBuilder gsonb = new GsonBuilder();

		gsonb.setExclusionStrategies(new ExclusionStrategy() {

			@Override
			public boolean shouldSkipClass(Class<?> clazz) {
				return false;
			}

			@Override
			public boolean shouldSkipField(FieldAttributes f) {
				/* We always skip these fields because they produce circular references */
				boolean skip = (f.getDeclaringClass() == Beacon.class && f.getName().equals("entities"));
				if (mStripChildEntities) {
					skip = skip || (f.getDeclaringClass() == Entity.class && f.getName().equals("children"));
				}
				return skip;
			}
		});

		Gson gson = gsonb.create();

		if (mEntityLocation != null) {
			String json = gson.toJson(mEntityLocation);
			intent.putExtra(mContext.getString(R.string.EXTRA_ENTITY_LOCATION), json);
		}

		if (mParentEntityId != null) {
			intent.putExtra(mContext.getString(R.string.EXTRA_PARENT_ENTITY_ID), mParentEntityId);
		}

		if (mEntityType != null) {
			intent.putExtra(mContext.getString(R.string.EXTRA_ENTITY_TYPE), mEntityType);
		}

		if (mBeaconId != null) {
			intent.putExtra(mContext.getString(R.string.EXTRA_BEACON_ID), mBeaconId);
		}

		if (mMessage != null) {
			intent.putExtra(mContext.getString(R.string.EXTRA_MESSAGE), mMessage);
		}

		if (mEntityId != null) {
			intent.putExtra(mContext.getString(R.string.EXTRA_ENTITY_ID), mEntityId);
		}

		if (mCollectionId != null) {
			intent.putExtra(mContext.getString(R.string.EXTRA_COLLECTION_ID), mCollectionId);
		}

		if (mEntityTree != null) {
			intent.putExtra(mContext.getString(R.string.EXTRA_ENTITY_TREE), mEntityTree.name());
		}

		if (mCommandType != null) {
			intent.putExtra(mContext.getString(R.string.EXTRA_COMMAND_TYPE), mCommandType.name());
		}

		if (mNavigationTop != null) {
			intent.putExtra(mContext.getString(R.string.EXTRA_NAVIGATION_TOP), mNavigationTop);
		}

		return intent;
	}

	public IntentBuilder setContext(Context context) {
		this.mContext = context;
		return this;
	}

	public IntentBuilder setParentEntityId(String parentEntityId) {
		this.mParentEntityId = parentEntityId;
		return this;
	}

	public IntentBuilder setEntityType(String entityType) {
		this.mEntityType = entityType;
		return this;
	}

	public IntentBuilder setMessage(String message) {
		this.mMessage = message;
		return this;
	}

	public IntentBuilder setBeaconId(String beaconId) {
		this.mBeaconId = beaconId;
		return this;
	}

	public IntentBuilder setClass(Class<?> _class) {
		this.mClass = _class;
		return this;
	}

	public IntentBuilder setEntityId(String entityId) {
		this.mEntityId = entityId;
		return this;
	}

	public IntentBuilder setEntityLocation(GeoLocation entityLocation) {
		this.mEntityLocation = entityLocation;
		return this;
	}

	public IntentBuilder setCollectionId(String collectionId) {
		mCollectionId = collectionId;
		return this;
	}

	public IntentBuilder setEntityTree(EntityTree entityTree) {
		mEntityTree = entityTree;
		return this;
	}

	public IntentBuilder setCommandType(CommandType commandType) {
		mCommandType = commandType;
		return this;
	}

	public IntentBuilder setNavigationTop(Boolean navigationTop) {
		mNavigationTop = navigationTop;
		return this;
	}
}
