package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.aircandi.service.Expose;
import com.aircandi.service.SerializedName;

/**
 * @author Jayma
 */

@SuppressWarnings("ucd")
public class Device extends ServiceEntryBase implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 694133954499515095L;
	public static final String	collectionId		= "devices";

	@Expose
	@SerializedName("_user")
	public String				userId;
	@Expose
	public String				registrationId;
	@Expose
	public Number				clientVersionCode;
	@Expose
	public String				clientVersionName;
	@Expose
	public List<Beacon>			beacons;
	@Expose
	public Number				beaconsDate;

	public Device() {}

	public Device(String userId, String registrationId) {
		this.userId = userId;
		this.registrationId = registrationId;
	}

	public static Device setPropertiesFromMap(Device device, Map map) {

		device = (Device) ServiceEntryBase.setPropertiesFromMap(device, map);
		device.userId = (String) map.get("_user");
		device.registrationId = (String) map.get("registrationId");
		device.clientVersionCode = (Number) map.get("clientVersionCode");
		device.clientVersionName = (String) map.get("clientVersionName");
		device.beaconsDate = (Number) map.get("beaconsDate");

		if (map.get("beacons") != null) {
			device.beacons = new ArrayList<Beacon>();
			final List<LinkedHashMap<String, Object>> beaconMaps = (List<LinkedHashMap<String, Object>>) map.get("beacons");
			for (Map<String, Object> beaconMap : beaconMaps) {
				device.beacons.add(Beacon.setPropertiesFromMap(new Beacon(), beaconMap));
			}
		}

		return device;
	}

	@Override
	public String getCollection() {
		return collectionId;
	}
}