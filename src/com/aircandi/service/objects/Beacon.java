package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.aircandi.components.ProxiExplorer;
import com.aircandi.components.ProxiExplorer.ModelResult;
import com.aircandi.service.Expose;

/**
 * @author Jayma
 */

public class Beacon extends ServiceEntry implements Cloneable, Serializable {

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
	public Number				level;

	/* Synthetic service fields */

	@Expose(serialize = false, deserialize = true)
	public Integer				entityCount;
	@Expose(serialize = false, deserialize = true)
	public Integer				pictureCount;
	@Expose(serialize = false, deserialize = true)
	public Integer				postCount;
	@Expose(serialize = false, deserialize = true)
	public Integer				linkCount;
	@Expose(serialize = false, deserialize = true)
	public Integer				folderCount;

	// For client use only
	public int					signalLevel;
	public Boolean				global				= false;
	public Boolean				radarHit			= false;
	public Date					discoveryTime;
	public Boolean				detectedLastPass	= false;
	public BeaconState			state				= BeaconState.Normal;
	public int					scanMisses			= 0;
	public List<Integer>		scanPasses			= new ArrayList<Integer>();

	public Beacon() {}

	public Beacon(String bssid, String ssid, String label, int levelDb, Date discoveryTime, Boolean test) {
		this.id = "0003:" + bssid;
		this.ssid = ssid;
		this.bssid = bssid;
		this.label = label;
		this.signalLevel = levelDb;
		this.discoveryTime = discoveryTime;
	}

	public Integer getAvgBeaconLevel() {
		int scanHits = scanPasses.size();
		if (scanHits == 0) {
			return -80;
		}
		else {
			int scanLevelSum = 0;
			for (int scanLevel : this.scanPasses) {
				scanLevelSum += scanLevel;
			}
			return Math.round(scanLevelSum / scanHits);
		}
	}

	public void addScanPass(int level) {
		scanPasses.add(0, level);
		while (scanPasses.size() > 1) {
			scanPasses.remove(1);
		}
	}

	public static void copyProperties(Beacon from, Beacon to) {
		/*
		 * Properties are copied from one beacon to another.
		 * 
		 * Local state properties we intentionally don't overwrite:
		 * 
		 * - scanLevel;
		 * - global
		 * - radarHit
		 * - discoveryTime
		 * - detectedLastPass
		 * - state
		 * - scanMisses
		 * - scanPasses
		 */
		ServiceEntry.copyProperties(from, to);

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

		to.entityCount = from.entityCount;
		to.folderCount = from.folderCount;
		to.linkCount = from.linkCount;
		to.pictureCount = from.pictureCount;
		to.postCount = from.postCount;

		to.locked = from.locked;
		to.visibility = from.visibility;

	}

	public static Beacon setPropertiesFromMap(Beacon beacon, HashMap map) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		beacon = (Beacon) ServiceEntry.setPropertiesFromMap(beacon, map);

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

		beacon.entityCount = (Integer) map.get("entityCount");
		beacon.pictureCount = (Integer) map.get("pictureCount");
		beacon.postCount = (Integer) map.get("postCount");
		beacon.linkCount = (Integer) map.get("linkCount");
		beacon.folderCount = (Integer) map.get("collectionCount");

		return beacon;
	}

	public GeoLocation getLocation() {
		GeoLocation location = null;
		if (latitude != null && longitude != null) {
			location = new GeoLocation(latitude, longitude);
		}
		return location;
	}

	@Override
	public String getCollection() {
		return "beacons";
	}

	public float getLevelPcnt() {

		int avgZoneLevel = Math.abs(this.getAvgBeaconLevel());
		float levelPcnt = 1f;

		if (avgZoneLevel >= 90)
			levelPcnt = .1f;
		else if (avgZoneLevel >= 80)
			levelPcnt = .3f;
		else if (avgZoneLevel >= 70)
			levelPcnt = .5f;
		else if (avgZoneLevel >= 60)
			levelPcnt = .7f;
		else if (avgZoneLevel >= 50)
			levelPcnt = .8f;

		return levelPcnt;
	}

	public List<Entity> getEntities() {
		ModelResult result = ProxiExplorer.getInstance().getEntityModel().getBeaconEntities(this.id, false);
		return (List<Entity>) result.data;
	}

	public static enum BeaconType {
		Fixed, Mobile, Temporary
	}

	public static enum BeaconState {
		New, Normal
	}
}