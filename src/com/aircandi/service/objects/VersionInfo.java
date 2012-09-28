package com.aircandi.service.objects;

import java.util.HashMap;

import com.aircandi.service.Expose;


/**
 * @author Jayma
 */
public class VersionInfo extends ServiceEntry {

	private static final long	serialVersionUID	= 6537038067017071976L;
	@Expose
	public String				type;
	@Expose
	public String				target;
	@Expose
	public Integer				major;
	@Expose
	public Integer				minor;
	@Expose
	public Integer				revision;
	@Expose
	public String				versionName;
	@Expose
	public Integer				versionCode;
	@Expose
	public Boolean				updateRequired;
	@Expose
	public Boolean				enabled;
	@Expose
	public String				updateUri;

	public VersionInfo() {}

	@Override
	public String getCollection() {
		return "documents";
	}

	public static VersionInfo setFromPropertiesFromMap(VersionInfo versionInfo, HashMap map) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		versionInfo.type = (String) map.get("type");
		versionInfo.target = (String) map.get("target");
		versionInfo.major = (Integer) map.get("major");
		versionInfo.minor = (Integer) map.get("minor");
		versionInfo.revision = (Integer) map.get("revision");
		versionInfo.versionName = (String) map.get("versionName");
		versionInfo.versionCode = (Integer) map.get("versionCode");
		versionInfo.updateRequired = (Boolean) map.get("updateRequired");
		versionInfo.enabled = (Boolean) map.get("enabled");
		versionInfo.updateUri = (String) map.get("updateUri");

		return versionInfo;
	}

}