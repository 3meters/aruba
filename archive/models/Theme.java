package com.proxibase.aircandi.models;

import com.google.gson.annotations.Expose;

/**
 * @author Jayma
 */
public class Theme
{
	// Annotation syntax: @Expose (serialize = false, deserialize = false)
	@Expose
	public String				themeId;
	@Expose
	public String				themeName;
	@Expose
	public String				headerColor;
	@Expose
	public String				headerBackColor;
	@Expose
	public String				headerImage;
	@Expose
	public String				bodyColor;
	@Expose
	public String				bodyBackColor;
	@Expose
	public String				bodyTitleColor;

	public Theme() {}
}