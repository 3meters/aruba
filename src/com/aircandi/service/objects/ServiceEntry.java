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

}