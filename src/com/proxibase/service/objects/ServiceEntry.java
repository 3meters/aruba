package com.proxibase.service.objects;

import java.io.Serializable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.proxibase.service.ProxiConstants;

/*
 * Initial user settings (owner,creator):
 * 
 * entities: user, user
 * links: user, user
 * sessions: user, user
 * users: user, admin
 * 
 * beacons: admin, user
 * documents: admin, admin
 * observations: admin, user
 */

public abstract class ServiceEntry implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 5341986472204947191L;

	/*
	 * Annotation syntax:
	 * 
	 * @Expose (serialize = false, deserialize = false)
	 * 
	 * @SerializedName("_nametoserialize")
	 */
	@Expose
	@SerializedName("_id")
	public String				id;

	/* User ids */

	@Expose
	@SerializedName("_owner")
	public String				ownerId;

	@Expose
	@SerializedName("_creator")
	public String				creatorId;

	@Expose
	@SerializedName("_modifier")
	public String				modifierId;

	/* Dates */

	@Expose(serialize = false, deserialize = true)
	public Number				createdDate;

	@Expose(serialize = false, deserialize = true)
	public Number				modifiedDate;

	/* Users (client) */

	@Expose(serialize = false, deserialize = true)
	public User					owner;

	@Expose(serialize = false, deserialize = true)
	public User					creator;

	@Expose(serialize = false, deserialize = true)
	public User					modifier;

	/* Client use only */
	public String				timeSince;

	public String getEntryUri() {
		String root = ProxiConstants.URL_PROXIBASE_SERVICE_REST;
		String entity = this.getCollection();
		String uri = root + entity + "/ids:" + id;
		return uri;
	}

	public abstract String getCollection();
}
