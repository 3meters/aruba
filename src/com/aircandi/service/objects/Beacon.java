package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.aircandi.Constants;
import com.aircandi.components.LocationManager;
import com.aircandi.service.Copy;
import com.aircandi.service.Expose;

/**
 * @author Jayma
 */

@SuppressWarnings("ucd")
public class Beacon extends Entity implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 694133954499515095L;
	public static final String	collectionId		= "beacons";

	@Expose
	public String				ssid;
	@Expose
	public String				bssid;
	@Expose
	public Number				signal;									// Used to evaluate location accuracy 
	@Expose
	public GeoLocation			location;

	// For client use only
	@Copy(exclude = true)
	public Boolean				test				= false;
	@Copy(exclude = true)
	public Float				distance;

	public Beacon() {}

	public Beacon(String bssid, String ssid, String label, int levelDb, Boolean test) { // $codepro.audit.disable largeNumberOfParameters
		id = "0011." + bssid;
		this.ssid = ssid;
		this.bssid = bssid;
		this.name = label;
		this.signal = levelDb;
		this.test = test;
	}

	// --------------------------------------------------------------------------------------------
	// Set and get
	// --------------------------------------------------------------------------------------------	

	public Float getDistance() {

		Float distance = 0f;

		if (signal.intValue() >= -40) {
			distance = 1f;
		}
		else if (signal.intValue() >= -50) {
			distance = 2f;
		}
		else if (signal.intValue() >= -55) {
			distance = 3f;
		}
		else if (signal.intValue() >= -60) {
			distance = 5f;
		}
		else if (signal.intValue() >= -65) {
			distance = 7f;
		}
		else if (signal.intValue() >= -70) {
			distance = 10f;
		}
		else if (signal.intValue() >= -75) {
			distance = 15f;
		}
		else if (signal.intValue() >= -80) {
			distance = 20f;
		}
		else if (signal.intValue() >= -85) {
			distance = 30f;
		}
		else if (signal.intValue() >= -90) {
			distance = 40f;
		}
		else if (signal.intValue() >= -95) {
			distance = 60f;
		}
		else {
			distance = 80f;
		}

		return distance * LocationManager.FeetToMetersConversion;
	}

	@Override
	public String getCollection() {
		return collectionId;
	}

	// --------------------------------------------------------------------------------------------
	// Copy and serialization
	// --------------------------------------------------------------------------------------------

	public static Beacon setPropertiesFromMap(Beacon entity, Map map) {

		synchronized (entity) {
			entity = (Beacon) Entity.setPropertiesFromMap(entity, map);
			entity.ssid = (String) map.get("ssid");
			entity.bssid = (String) map.get("bssid");
			entity.signal = (Number) map.get("signal");

			if (map.get("location") != null) {
				entity.location = GeoLocation.setPropertiesFromMap(new GeoLocation(), (HashMap<String, Object>) map.get("location"));
			}
		}
		return entity;
	}

	@Override
	public Beacon clone() {
		final Beacon entity = (Beacon) super.clone();
		return entity;
	}

	// --------------------------------------------------------------------------------------------
	// Inner classes
	// --------------------------------------------------------------------------------------------

	public static class SortBeaconsBySignalLevel implements Comparator<Beacon> {

		@Override
		public int compare(Beacon object1, Beacon object2) {
			if ((object1.signal.intValue() / Constants.RADAR_BEACON_SIGNAL_BUCKET_SIZE)
			> (object2.signal.intValue() / Constants.RADAR_BEACON_SIGNAL_BUCKET_SIZE)) {
				return -1;
			}
			else if ((object1.signal.intValue() / Constants.RADAR_BEACON_SIGNAL_BUCKET_SIZE)
			< (object2.signal.intValue() / Constants.RADAR_BEACON_SIGNAL_BUCKET_SIZE)) {
				return 1;
			}
			else {
				return 0;
			}
		}
	}

}