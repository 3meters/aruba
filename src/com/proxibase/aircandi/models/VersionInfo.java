package com.proxibase.aircandi.models;

import com.google.gson.annotations.Expose;

/**
 * @author Jayma
 */
public class VersionInfo {

	/* Annotation syntax: @Expose (serialize = false, deserialize = false) */
	@Expose
	public int		id;
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

	/* For client use only */

	public VersionInfo() {}
}