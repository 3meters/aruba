package com.proxibase.aircandi.components;

import com.google.gson.annotations.Expose;
import com.proxibase.sdk.android.proxi.consumer.ServiceEntry;

/**
 * @author Jayma
 */
public class VersionInfo extends ServiceEntry {

	@Expose
	public String	type;
	@Expose
	public String	target;
	@Expose
	public Integer	major;
	@Expose
	public Integer	minor;
	@Expose
	public Integer	revision;
	@Expose
	public String	versionName;
	@Expose
	public Integer	versionCode;
	@Expose
	public boolean	updateRequired;

	public VersionInfo() {}

	@Override
	public String getCollection() {
		return "documents";
	}
}