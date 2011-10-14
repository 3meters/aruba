package com.proxibase.aircandi.models;

import android.graphics.Bitmap;

import com.google.gson.annotations.Expose;
import com.proxibase.aircandi.core.CandiConstants;
import com.proxibase.sdk.android.proxi.consumer.EntityProxy;

public class PostEntity extends BaseEntity {

	/* Annotation syntax: @Expose (serialize = false, deserialize = false) */
	@Expose
	public String	mediaUri;
	@Expose
	public String	mediaFormat;
	@Expose
	public boolean	locked;
	@Expose
	public Metadata	__metadata	= new Metadata();

	//---------------------------------------------------------------------------------------------
	// For client use only
	//---------------------------------------------------------------------------------------------

	public Bitmap	mediaBitmap;

	public PostEntity() {
		__metadata.type = "Aircandi.Post";
	}

	@Override
	public EntityProxy getEntityProxy() {
		EntityProxy entityProxy = new EntityProxy();
		entityProxy.beaconId = beaconId;
		entityProxy.entityUri = getEntryUri();
		entityProxy.entityType = CandiConstants.TYPE_CANDI_POST;
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