package com.aircandi.service.objects;

import java.io.Serializable;

import com.aircandi.service.objects.ServiceBase.UpdateScope;

public class ServiceObject implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 5341986472204947192L;
	public UpdateScope			updateScope			= UpdateScope.Property;
}
