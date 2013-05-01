package com.aircandi.service.objects;

import java.util.Map;

/**
 * @author Jayma
 */
public class ServiceEntry extends ServiceEntryBase {

	private static final long	serialVersionUID	= -4468666434251114969L;

	public ServiceEntry() {}

	public static ServiceEntry setPropertiesFromMap(ServiceEntry serviceEntry, Map map) {

		serviceEntry = (ServiceEntry) ServiceEntryBase.setPropertiesFromMap(serviceEntry, map);
		return serviceEntry;
	}

	@Override
	public String getCollection() {
		return null;
	}

	public static String getTypeFromId(String id) {

		String typeId = id.substring(0, 4);
		String type = null;
		if (typeId.equals("0001")) type = "users";
		if (typeId.equals("0002")) type = "accounts";
		if (typeId.equals("0003")) type = "sessions";
		if (typeId.equals("0004")) type = "entities";
		if (typeId.equals("0005")) type = "links";
		if (typeId.equals("0006")) type = "actions";
		if (typeId.equals("0007")) type = "documents";
		if (typeId.equals("0008")) type = "beacons";
		if (typeId.equals("0009")) type = "devices";

		return type;
	}

	public static String getIdFromType(String type) {

		String typeId = null;
		if (type.equals("users")) typeId = "0001";
		if (type.equals("accounts")) typeId = "0002";
		if (type.equals("sessions")) typeId = "0003";
		if (type.equals("entities")) typeId = "0004";
		if (type.equals("links")) typeId = "0005";
		if (type.equals("actions")) typeId = "0006";
		if (type.equals("documents")) typeId = "0007";
		if (type.equals("beacons")) typeId = "0008";
		if (type.equals("devices")) typeId = "0009";

		return typeId;
	}
}