package com.aircandi.service.objects;

import com.aircandi.service.Expose;
import com.aircandi.service.SerializedName;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Observation extends ServiceEntryBase {

	private static final long	serialVersionUID	= 5247954279209162831L;

	@Expose
	@SerializedName("_beacon")
	public String				beaconId;

	@Expose
	@SerializedName("_entity")
	public String				entityId;

	@Expose
	public Number				levelDb;
	@Expose
	public Number				latitude;
	@Expose
	public Number				longitude;
	@Expose
	public Number				altitude;
	@Expose
	public Number				accuracy;
	@Expose
	public Number				bearing;
	@Expose
	public Number				speed;

	@Expose(serialize = false, deserialize = true)
	public Beacon				beacon;

	@Expose(serialize = false, deserialize = true)
	public Entity				entity;

	/* Client only */
	public Number				time;
	public String				provider;

	public Observation() {}

	public Observation(Number pLatitude, Number pLongitude) {
		latitude = pLatitude;
		longitude = pLongitude;
	}

	@Override
	public String getCollection() {
		return "observations";
	}
}