package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.aircandi.ProxiConstants;
import com.aircandi.components.AndroidManager;
import com.aircandi.service.Expose;
import com.aircandi.service.objects.Photo.PhotoSource;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Source extends ServiceObject implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 4362288672245719448L;

	@Expose
	public String				id;
	@Expose
	public String				type;
	@Expose
	public String				name;
	@Expose
	public String				label;
	@Expose
	public String				url;
	@Expose
	public Photo				photo;
	@Expose
	public String				packageName;
	@Expose
	public Map<String, Object>	data;										// treat as opaque but roundtrip
	@Expose
	public Boolean				system;

	/* Client use only */
	public Boolean				checked;
	public Integer				position;
	public Boolean				custom;
	public Boolean				intentSupport;
	public Boolean				installDeclined;

	public Source() {}

	public Source(Boolean intentSupport, Boolean installDeclined) {
		this.intentSupport = intentSupport;
		this.installDeclined = installDeclined;
	}

	public static Source setPropertiesFromMap(Source source, Map map) {
		source.id = (String) map.get("id");
		source.type = (String) map.get("type");
		source.name = (String) map.get("name");
		source.label = (String) map.get("label");
		source.system = (Boolean) map.get("system");
		source.url = (String) map.get("url");
		source.packageName = (String) map.get("packageName");
		source.data = (HashMap<String, Object>) map.get("data");
		if (map.get("photo") != null) {
			source.photo = Photo.setPropertiesFromMap(new Photo(), (HashMap<String, Object>) map.get("photo"));
		}
		return source;
	}

	public Photo getPhoto() {
		if (photo != null) {
			return photo;
		}
		else {
			return new Photo(getDefaultIcon(type), null, null, null, PhotoSource.assets);
		}
	}

	public static String getDefaultIcon(String sourceType) {
		String icon = ProxiConstants.PATH_PROXIBASE_SERVICE_ASSETS_SOURCE_ICONS + sourceType + ".png";
		return icon;
	}

	public static String getPackageName(String sourceType) {
		String packageName = null;
		if (sourceType.equals("facebook")) {
			packageName = "com.facebook.katana";
		}
		else if (sourceType.equals("twitter")) {
			packageName = "com.twitter.android";
		}
		return packageName;
	}

	public static class SortSourcesBySourcePosition implements Comparator<Source> {

		@Override
		public int compare(Source source1, Source source2) {
			if (source1.position < source2.position) {
				return -1;
			}
			if (source1.position.equals(source2.position)) {
				return 0;
			}
			return 1;
		}
	}

	public Boolean appExists() {
		return packageName != null;
	}

	public Boolean appInstalled() {
		final Boolean exists = AndroidManager.getInstance().doesPackageExist(packageName);
		return exists;
	}

	public static class SourceType {
		public static String	foursquare	= "foursquare";
		public static String	facebook	= "facebook";
		public static String	twitter		= "twitter";
		public static String	website		= "website";
	}
}