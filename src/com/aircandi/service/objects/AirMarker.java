package com.aircandi.service.objects;

import java.util.Map;

import com.aircandi.service.Expose;

public class AirMarker {

	@Expose
	public String	title;
	@Expose
	public String	snippet;
	@Expose
	public String	id;
	@Expose
	public Number	lat;
	@Expose
	public Number	lng;
	@Expose
	public Integer	iconResId;
	@Expose
	public Boolean	current;

	public AirMarker() {}

	public AirMarker(String id, String title, String snippet, Number lat, Number lng, Boolean current, Integer iconResId) {
		this.id = id;
		this.title = title;
		this.snippet = snippet;
		this.lat = lat;
		this.lng = lng;
		this.current = current;
		this.iconResId = iconResId;
	}

	public static AirMarker setPropertiesFromMap(AirMarker marker, Map map, Boolean nameMapping) {

		marker.id = (String) map.get("id");
		marker.title = (String) map.get("title");
		marker.snippet = (String) map.get("snippet");
		marker.lat = (Number) map.get("lat");
		marker.lng = (Number) map.get("lng");
		marker.iconResId = (Integer) map.get("iconResId");
		marker.current = (Boolean) map.get("current");

		return marker;
	}
}
