package com.proxibase.sdk.android.proxi.consumer;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * @author Jayma
 */
public class Drop extends ServiceEntry {

	@Expose
	@SerializedName("_beacon")
	public String	beaconId;

	@Expose(serialize = false, deserialize = true)
	public Beacon	beacon;

	@Expose
	@SerializedName("_entity")
	public String	entityId;

	@Expose(serialize = false, deserialize = true)
	public Entity	entity;

	@Expose
	public Number	latitude;
	@Expose
	public Number	longitude;
	@Expose
	public Number	altitude;
	@Expose
	public Number	accuracy;
	@Expose
	public Number	bearing;
	@Expose
	public Number	speed;

	public Drop() {}

	public String getCollection() {
		return "drops";
	}
}