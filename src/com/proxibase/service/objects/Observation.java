package com.proxibase.service.objects;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * @author Jayma
 */
public class Observation extends ServiceEntry {

	@Expose
	@SerializedName("_beacon")
	public String	beaconId;

	@Expose
	@SerializedName("_entity")
	public String	entityId;

	@Expose
	public Number	levelDb;
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

	@Expose(serialize = false, deserialize = true)
	public Beacon	beacon;

	@Expose(serialize = false, deserialize = true)
	public Entity	entity;

	public Observation() {}

	public String getCollection() {
		return "observations";
	}
}