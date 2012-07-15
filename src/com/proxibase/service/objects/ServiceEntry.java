package com.proxibase.service.objects;

import java.io.Serializable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.proxibase.service.ProxiConstants;

/**
 * Bound by default to the proxibase odata service.
 * 
 * @author Jayma
 */
public abstract class ServiceEntry  implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 5341986472204947191L;

	protected String	mServiceUri	= ProxiConstants.URL_PROXIBASE_SERVICE;

	/*
	 * Annotation syntax:
	 * 
	 * @Expose (serialize = false, deserialize = false)
	 * 
	 * @SerializedName("_nametoserialize")
	 */
	@Expose
	@SerializedName("_id")
	public String		id;

	/* Lookup Ids */

	@Expose
	@SerializedName("_owner")
	public String		ownerId;

	@Expose
	@SerializedName("_creator")
	public String		creatorId;

	@Expose
	@SerializedName("_modifier")
	public String		modifierId;

	/* Lookups */

	@Expose(serialize = false, deserialize = true)
	public User			owner;

	@Expose(serialize = false, deserialize = true)
	public User			creator;

	@Expose(serialize = false, deserialize = true)
	public User			modifier;

	/* Dates */

	@Expose(serialize = false, deserialize = true)
	public Number		createdDate;

	@Expose(serialize = false, deserialize = true)
	public Number		modifiedDate;

	/* Client use only */
	public String		timeSince;

	public String getEntryUri() {
		String root = mServiceUri;
		String entity = this.getCollection();
		String uri = root + entity + "/ids:" + id;
		return uri;
	}

	public abstract String getCollection();

	public void setServiceUri(String serviceUri) {
		this.mServiceUri = serviceUri;
	}

	public String getServiceUri() {
		return mServiceUri;
	}
}
