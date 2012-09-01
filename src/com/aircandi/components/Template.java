package com.aircandi.components;

public class Template {

	public Integer	iconResId;
	public String	title;
	public String	description;
	public String	type;

	public Template() {}

	public Template(Integer iconResId, String title, String description, String type) {
		this.iconResId = iconResId;
		this.title = title;
		this.description = description;
		this.type = type;
	}
}
