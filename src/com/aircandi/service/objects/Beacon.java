package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.aircandi.components.ProxiExplorer;
import com.aircandi.core.CandiConstants;
import com.aircandi.service.Expose;

/**
 * @author Jayma
 */

public class Beacon extends ServiceEntryBase implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 694133954499515095L;

	@Expose
	public String				ssid;
	@Expose
	public String				bssid;
	@Expose
	public String				label;
	@Expose
	public Boolean				locked;
	@Expose
	public String				visibility;
	@Expose
	public String				beaconType;
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
	@Expose
	public Number				level;										// Used to evaluate location accuracy 

	// For client use only
	public Boolean				global				= false;
	public BeaconState			state				= BeaconState.Normal;
	public Boolean				test				= false;

	public Beacon() {}

	public Beacon(String bssid, String ssid, String label, int levelDb, Date discoveryTime, Boolean test) {
		this.id = "0008." + bssid;
		this.ssid = ssid;
		this.bssid = bssid;
		this.label = label;
		this.level = levelDb;
		this.test = test;
	}

	public static void copyProperties(Beacon from, Beacon to) {
		/*
		 * Properties are copied from one beacon to another.
		 * 
		 * Local state properties we intentionally don't overwrite:
		 * 
		 * - global
		 * - state
		 * - test
		 */
		ServiceEntryBase.copyProperties(from, to);

		to.ssid = from.ssid;
		to.bssid = from.bssid;
		to.label = from.label;
		to.beaconType = from.beaconType;
		to.latitude = from.latitude;
		to.longitude = from.longitude;
		to.altitude = from.altitude;
		to.accuracy = from.accuracy;
		to.speed = from.speed;
		to.bearing = from.bearing;
		to.level = from.level;

		to.locked = from.locked;
		to.visibility = from.visibility;

	}

	public static Beacon setPropertiesFromMap(Beacon beacon, HashMap map) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		beacon = (Beacon) ServiceEntryBase.setPropertiesFromMap(beacon, map);

		beacon.ssid = (String) map.get("ssid");
		beacon.bssid = (String) map.get("bssid");
		beacon.label = (String) map.get("label");
		beacon.locked = (Boolean) map.get("locked");
		beacon.visibility = (String) map.get("visibility");
		beacon.beaconType = (String) map.get("beaconType");
		beacon.latitude = (Number) map.get("latitude");
		beacon.longitude = (Number) map.get("longitude");
		beacon.altitude = (Number) map.get("altitude");
		beacon.accuracy = (Number) map.get("accuracy");
		beacon.speed = (Number) map.get("speed");
		beacon.bearing = (Number) map.get("bearing");

		return beacon;
	}

	public GeoLocation getLocation() {
		GeoLocation location = null;
		if (latitude != null && longitude != null) {
			location = new GeoLocation(latitude.doubleValue(), longitude.doubleValue());
		}
		return location;
	}

	@Override
	public String getCollection() {
		return "beacons";
	}

	public float getLevelPcnt() {

		float levelPcnt = 1f;

		if (this.level.intValue() >= 90)
			levelPcnt = .1f;
		else if (this.level.intValue() >= 80)
			levelPcnt = .3f;
		else if (this.level.intValue() >= 70)
			levelPcnt = .5f;
		else if (this.level.intValue() >= 60)
			levelPcnt = .7f;
		else if (this.level.intValue() >= 50)
			levelPcnt = .8f;

		return levelPcnt;
	}

	public List<Entity> getEntities() {
		return (ProxiExplorer.getInstance().getEntityModel().getBeaconEntities(this.id));
	}

	public static class SortBeaconsBySignalLevel implements Comparator<Beacon> {

		@Override
		public int compare(Beacon object1, Beacon object2) {
			if ((object1.level.intValue() / CandiConstants.RADAR_BEACON_SIGNAL_BUCKET_SIZE)
			> (object2.level.intValue() / CandiConstants.RADAR_BEACON_SIGNAL_BUCKET_SIZE)) {
				return -1;
			}
			else if ((object1.level.intValue() / CandiConstants.RADAR_BEACON_SIGNAL_BUCKET_SIZE)
			< (object2.level.intValue() / CandiConstants.RADAR_BEACON_SIGNAL_BUCKET_SIZE)) {
				return 1;
			}
			else {
				return 0;
			}
		}
	}

	public static enum BeaconType {
		Fixed, Mobile, Temporary
	}

	public static enum BeaconState {
		New, Normal, Gone
	}
}