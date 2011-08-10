package com.proxibase.aircandi.models;

import com.google.gson.annotations.Expose;
import com.proxibase.aircandi.candi.utils.CandiConstants;
import com.proxibase.sdk.android.proxi.consumer.EntityProxy;

/**
 * Entity as described by the proxi protocol standards.
 * 
 * @author Jayma
 */
public class ClaimEntity extends BaseEntity {

	// Annotation syntax: @Expose (serialize = false, deserialize = false)
	@Expose
	public String	claimedById;
	@Expose
	public String	claimedDate;
	@Expose
	public Metadata	__metadata	= new Metadata();

	public ClaimEntity() {
		__metadata.type = "Aircandi.Claim";
	}

	@Override
	public EntityProxy getEntityProxy() {
		EntityProxy entityProxy = new EntityProxy();
		entityProxy.beaconId = beaconId;
		entityProxy.entityUri = getEntryUri();
		entityProxy.entityType = CandiConstants.TYPE_CANDI_CLAIM;
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