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
public class Install extends ServiceBase implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 694133954499515095L;
	public static final String	collectionId		= "installs";

	@Expose
	@SerializedName(name = "_user")
	public String				userId;
	@Expose
	public String				registrationId;
	@Expose
	public String				installationId;
	@Expose
	public Number				clientVersionCode;
	@Expose
	public String				clientVersionName;
	@Expose
	public List<Beacon>			beacons;
	@Expose
	public Number				beaconsDate;

	public Install() {
	}

	public Install(String userId, String registrationId, String installationId) {
		this.userId = userId;
		this.registrationId = registrationId;
		this.installationId = installationId;
	}

	public static Install setPropertiesFromMap(Install install, Map map, Boolean nameMapping) {

		install = (Install) ServiceBase.setPropertiesFromMap(install, map, nameMapping);
		install.userId = (String) (nameMapping ? map.get("_user") : map.get("userId"));
		install.registrationId = (String) map.get("registrationId");
		install.installationId = (String) map.get("installationId");
		install.clientVersionCode = (Number) map.get("clientVersionCode");
		install.clientVersionName = (String) map.get("clientVersionName");
		install.beaconsDate = (Number) map.get("beaconsDate");

		if (map.get("beacons") != null) {
			install.beacons = new ArrayList<Beacon>();
			final List<LinkedHashMap<String, Object>> beaconMaps = (List<LinkedHashMap<String, Object>>) map.get("beacons");
			for (Map<String, Object> beaconMap : beaconMaps) {
				install.beacons.add(Beacon.setPropertiesFromMap(new Beacon(), beaconMap, nameMapping));
			}
		}

		return install;
	}

	@Override
	public String getCollection() {
		return collectionId;
	}
}