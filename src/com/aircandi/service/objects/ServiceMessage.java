package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.Map;

import com.aircandi.service.Expose;

public class ServiceMessage extends ActivityBase implements Cloneable, Serializable {

	private static final long	serialVersionUID	= -6550475791491989605L;

	@Expose
	public Number				sentDate;

	public ServiceMessage() {
		super();
	}

	public static ServiceMessage setPropertiesFromMap(ServiceMessage serviceMessage, Map map, Boolean nameMapping) {
		
		serviceMessage = (ServiceMessage) ActivityBase.setPropertiesFromMap(serviceMessage, map, nameMapping);
		serviceMessage.sentDate = (Number) map.get("sentDate");

		return serviceMessage;
	}

}
