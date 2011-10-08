package com.proxibase.aircandi.models;

import com.google.gson.annotations.Expose;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.sdk.android.proxi.consumer.EntityProxy;

public class CommentEntity extends BaseEntity {

	// Annotation syntax: @Expose (serialize = false, deserialize = false)
	@Expose
	public Metadata		__metadata	= new Metadata();

	public CommentEntity() {
		__metadata.type = "Aircandi.Comment";
	}

	@Override
	public EntityProxy getEntityProxy() {
		EntityProxy entityProxy = new EntityProxy();
		entityProxy.beaconId = beaconId;
		entityProxy.entityUri = getEntryUri();
		entityProxy.entityType = CandiConstants.TYPE_CANDI_COMMENT;
		entityProxy.label = label;
		entityProxy.title = title;
		entityProxy.subtitle = subtitle;
		entityProxy.description = description;
		entityProxy.imageUri = imageUri;
		entityProxy.signalFence = signalFence;
		return entityProxy;
	}

	@Override
	public String getCollection() {
		return "Entities";
	}
}