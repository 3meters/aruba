package com.proxibase.aircandi.components;

import android.content.Context;
import android.content.Intent;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.proxibase.aircandi.R;
import com.proxibase.service.consumer.Beacon;
import com.proxibase.service.consumer.Entity;

public class IntentBuilder {

	private Context		mContext;
	private Entity		mEntity;
	private String		mEntityId;
	private String		mParentEntityId;
	private String		mEntityType;
	private String		mMessage;
	private Command		mCommand;
	private String		mBeaconId;
	private Boolean		mStripChildEntities	= true;
	private Class<?>	mClass;

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
				//return (clazz == (Class<List<Entity>>) (Class<?>) List.class);
			}

			@Override
			public boolean shouldSkipField(FieldAttributes f) {
				/* We always skip these fields because they produce circular references */
				boolean skip = (f.getDeclaringClass() == Beacon.class && f.getName().equals("entities"))
								|| (f.getDeclaringClass() == Command.class && f.getName().equals("entity"));

				if (mStripChildEntities) {
					skip = skip || (f.getDeclaringClass() == Entity.class && f.getName().equals("children"));
				}
				return skip;
				//return (f.getDeclaredType() == (Class<List<Entity>>) (Class<?>) List.class);
			}
		});

		Gson gson = gsonb.create();

		if (mCommand != null) {
			String jsonCommand = gson.toJson(mCommand);
			intent.putExtra(mContext.getString(R.string.EXTRA_COMMAND), jsonCommand);
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

		if (mEntity != null) {
			String jsonEntity = gson.toJson(mEntity);
			if (jsonEntity.length() > 0) {
				intent.putExtra(mContext.getString(R.string.EXTRA_ENTITY), jsonEntity);
			}
		}

		if (mEntityId != null) {
			intent.putExtra(mContext.getString(R.string.EXTRA_ENTITY_ID), mEntityId);
		}
		return intent;
	}

	public IntentBuilder setContext(Context context) {
		this.mContext = context;
		return this;
	}

	public IntentBuilder setEntity(Entity entity) {
		this.mEntity = entity;
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

	public IntentBuilder setCommand(Command command) {
		this.mCommand = command;
		return this;
	}

	public IntentBuilder setBeaconName(String beaconId) {
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
}
