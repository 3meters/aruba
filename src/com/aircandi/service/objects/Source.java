package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;

import com.aircandi.ProxiConstants;
import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
public class Source extends ServiceObject implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 4362288672245719448L;

	@Expose
	public String				name;
	@Expose
	public String				source;
	@Expose
	public String				id;
	@Expose
	public String				url;
	@Expose
	public String				icon;
	@Expose
	public String				iconInverse;
	@Expose
	public String				origin;

	public Boolean				checked;
	public Integer				position;
	public Boolean				custom;

	public Source() {}

	public Source(String source, String id) {
		this.source = source;
		this.id = id;
	}

	public static Source setPropertiesFromMap(Source source, HashMap map) {
		source.name = (String) map.get("name");
		source.source = (String) map.get("source");
		source.id = (String) map.get("id");
		source.url = (String) map.get("url");
		source.icon = (String) map.get("icon");
		source.iconInverse = (String) map.get("iconInverse");
		source.origin = (String) map.get("origin");
		return source;
	}

	public String getImageUri() {
		String imageUri = icon;
		if (imageUri != null && !imageUri.startsWith("resource:")) {
			imageUri = ProxiConstants.URL_PROXIBASE_SERVICE_ASSETS_ICONS + icon;
		}
		return imageUri;
	}

	public static class SortSourcesBySourcePosition implements Comparator<Source> {

		@Override
		public int compare(Source source1, Source source2) {
			if (source1.position < source2.position) {
				return -1;
			}
			if (source1.position == source2.position) {
				return 0;
			}
			return 1;
		}
	}
}