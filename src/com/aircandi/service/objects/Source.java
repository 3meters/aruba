package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.aircandi.ProxiConstants;
import com.aircandi.components.AndroidManager;
import com.aircandi.service.Expose;

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
	public String				caption;									// remove after switch to label
	@Expose
	public String				label;
	@Expose
	public String				url;
	@Expose
	public String				icon;
	@Expose
	public String				packageName;
	@Expose
	public Map<String, Object>	data; // origin
	@Expose
	public Boolean				hidden;

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
		source.caption = (String) map.get("caption");
		source.label = (String) map.get("label");
		source.hidden = (Boolean) map.get("hidden");
		source.url = (String) map.get("url");
		source.icon = (String) map.get("icon");
		source.packageName = (String) map.get("packageName");
		source.data = (HashMap<String, Object>) map.get("data");
		return source;
	}

	public String getImageUri() {
		if (icon == null) {
			return null;
		}
		else if (icon.toLowerCase(Locale.US).startsWith("resource:")) {
			return icon;
		}
		return ProxiConstants.URL_PROXIBASE_SERVICE + icon;
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
}