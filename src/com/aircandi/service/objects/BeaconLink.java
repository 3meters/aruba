package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.HashMap;

import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
public class BeaconLink extends ServiceObject implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 455904759787968585L;

	@Expose
	public String				beaconId;
	@Expose
	public String				linkId;
	@Expose
	public Number				latitude;
	@Expose
	public Number				longitude;

	public BeaconLink() {}

	public BeaconLink(String beaconId, String linkId) {
		this.beaconId = beaconId;
		this.linkId = linkId;
	}
	
	@Override
	public BeaconLink clone() {
		try {
			final BeaconLink beaconLink = (BeaconLink) super.clone();
			return beaconLink;
		}
		catch (final CloneNotSupportedException ex) {
			throw new AssertionError();
		}
	}

	public static BeaconLink setPropertiesFromMap(BeaconLink beaconLink, HashMap map) {
		beaconLink.beaconId = (String) map.get("beaconId");
		beaconLink.linkId = (String) map.get("linkId");
		beaconLink.latitude = (Number) map.get("latitude");
		beaconLink.longitude = (Number) map.get("longitude");
		return beaconLink;
	}
}