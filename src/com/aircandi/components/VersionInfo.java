package com.aircandi.components;

import com.aircandi.service.objects.ServiceEntry;
import com.google.gson.annotations.Expose;

/**
 * @author Jayma
 */
public class VersionInfo extends ServiceEntry {

	private static final long	serialVersionUID	= 6537038067017071976L;
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
	@Expose
	public String	updateUri;

	public VersionInfo() {}

	@Override
	public String getCollection() {
		return "documents";
	}
}